package com.kennyouchou.author.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.kennyouchou.author.bo.QueryUserListBo;
import com.kennyouchou.author.bo.RegisterBO;
import com.kennyouchou.author.entity.Role;
import com.kennyouchou.author.entity.User;
import com.kennyouchou.author.mapper.UserMapper;
import com.kennyouchou.author.service.RoleResourceService;
import com.kennyouchou.author.service.RoleService;
import com.kennyouchou.author.service.UserRoleService;
import com.kennyouchou.author.service.UserService;
import com.kennyouchou.author.shiro.utils.ShiroUserUtils;
import com.kennyouchou.commons.config.SystemProperties;
import com.kennyouchou.commons.encrypt_decrypt.factory.LEncryptDecryptFactory;
import com.kennyouchou.commons.enums.*;
import com.kennyouchou.commons.exception.LException;
import com.kennyouchou.commons.exception.LMailException;
import com.kennyouchou.commons.pojo.RoleVo;
import com.kennyouchou.commons.pojo.ShiroUser;
import com.kennyouchou.commons.utils.BaseEntityUtil;
import com.kennyouchou.commons.utils.ShiroUtils;
import com.kennyouchou.community.entity.CommunityGreyCode;
import com.kennyouchou.community.entity.CommunityHouseholdUser;
import com.kennyouchou.community.service.CommunityGreenCodeService;
import com.kennyouchou.community.service.CommunityGreyCodeService;
import com.kennyouchou.community.service.CommunityHouseholdUserService;
import com.kennyouchou.community.vo.UserHouseholdVo;
import com.kennyouchou.mail.bo.BaseSenderMessageBo;
import com.kennyouchou.mail.service.CommunityMessageService;

import org.apache.commons.collections.CollectionUtils;
import org.junit.platform.commons.util.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * <p>
 * ????????? ???????????????
 * </p>
 *
 * @author kennyouchou
 * @since 2022-10-23
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Resource
    private LEncryptDecryptFactory lEncryptDecryptFactory;

    @Resource
    private UserRoleService userRoleService;

    @Resource
    private CommunityHouseholdUserService communityHouseholdUserService;

    @Resource
    private CommunityMessageService communityMessageService;

    @Resource
    private CommunityGreenCodeService communityGreenCodeService;

    @Resource
    private CommunityGreyCodeService communityGreyCodeService;

    @Resource
    private SystemProperties systemProperties;

    @Resource
    private RoleService roleService;

    @Resource
    private RoleResourceService roleResourceService;



    /**
     * ??????????????????????????????
     * @param user ????????????
     * @return java.util.List<com.kennyouchou.author.entity.User>
     * @author kennyouchou
     * @since 2022/10/23 16:38
     **/
    @Override
    public List<User> listByUser(User user) {
        return baseMapper.listByUser(user);
    }

    /**
     * ????????????
     *
     * @param registerBO ??????????????????
     * @return java.lang.Integer
     * @author kennyouchou
     * @since 2022/10/23 16:39
     **/
    @Override
    public Integer addUser(RegisterBO registerBO) {
        User user = new User();
        BeanUtils.copyProperties(registerBO, user);
        // -1??????????????????
        user.setCreateUser("-1");
        user.setCreateTime(LocalDateTime.now());
        return this.baseMapper.insert(user);
    }

    /**
     * ??????????????????
     *
     * @param oldPassword    ?????????
     * @param newPassword    ?????????
     * @param repeatPassword ????????????
     * @author kennyouchou
     * @since 2022/10/31 11:56
     **/
    @Override
    public void updatePassword(String oldPassword, String newPassword, String repeatPassword) {
        if (! newPassword.equals(repeatPassword)){
            throw new RuntimeException("???????????????????????????");
        }
        ShiroUser shiroUser = ShiroUserUtils.getShiroUser();
        if (! oldPassword.equals(shiroUser.getPassword())){
            throw new RuntimeException("?????????????????????");
        }
        LambdaUpdateWrapper<User> wrapper = new LambdaUpdateWrapper<>();
        wrapper.set(User::getPassword, lEncryptDecryptFactory.getEncryptDecryptService().encrypt(newPassword))
                .eq(User::getId, shiroUser.getId());
        this.baseMapper.update(null, wrapper);
    }

    /**
     * ???????????????????????????????????????
     *
     * @param householdId ??????id
     * @author kennyouchou
     * @since 2022/11/8 19:14
     **/
    @Override
    public void bindRoleAndHousehold(String householdId) {
        String userId = ShiroUtils.getUserId();
        List<Role> roleList = roleService.findRoleIdByLabel(RoleTypeEnum.USER_FUNCTION.getValue(), RoleTypeEnum.USER.getValue());
        if (CollectionUtils.isNotEmpty(roleList)){
            List<String> roleIds = roleList.stream().map(Role::getId).collect(Collectors.toList());
            // ????????????(???????????????????????????)????????????
            userRoleService.bind(userId, roleIds);
        }
        // ??????????????????
        communityHouseholdUserService.bind(userId, householdId);
    }

    /**
     * ??????????????????
     *
     * @param userId ??????id
     * @author kennyouchou
     * @since 2022/11/2 10:02
     **/
    @Override
    public void userMoveOut(String userId) {
        // ??????????????????
        this.removeById(userId);
        // ????????????????????????????????????
        this.communityHouseholdUserService
                .remove(new LambdaQueryWrapper<CommunityHouseholdUser>()
                        .eq(CommunityHouseholdUser::getUserId, userId));
    }

    /**
     * ??????????????????????????????
     *
     * @param userId            ??????id
     * @param examinationStatus ????????????
     * @author kennyouchou
     * @since 2022/11/2 9:36
     **/
    @Override
    public void updateExamination(String userId, String examinationStatus) throws LMailException {
        LambdaUpdateWrapper<CommunityHouseholdUser> householdUserLambdaUpdateWrapper = new LambdaUpdateWrapper<>();
        householdUserLambdaUpdateWrapper.eq(CommunityHouseholdUser::getUserId, userId);
        // ????????????????????????????????????
        if (BooleanTypeEnum.NO.getCode().toString().equals(examinationStatus)) {
            communityHouseholdUserService.remove(householdUserLambdaUpdateWrapper);
            return;
        }
        householdUserLambdaUpdateWrapper.set(CommunityHouseholdUser::getIsEnable, BooleanTypeEnum.YES.getCode());
        communityHouseholdUserService.update(householdUserLambdaUpdateWrapper);
        String periodId = communityHouseholdUserService.findPeriodIdByUserId(userId);
        // ?????????????????????????????????
        LambdaUpdateWrapper<User> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(User::getId, userId)
                .set(User::getIsEnable, BooleanTypeEnum.YES.getCode())
                .set(User::getPeriodId, periodId);
        this.update(wrapper);
        // ????????????????????????
        userRoleService.activeRole(userId, RoleTypeEnum.USER.getValue(), RoleTypeEnum.USER_FUNCTION.getValue());
        /*
         * ?????????????????????????????????????????????
         *  1. ????????????????????????????????????
         *  2. ?????????????????????
         **/
        communityGreenCodeService.invalidateAll(userId);
        CommunityGreyCode communityGreyCode = new CommunityGreyCode();
        BaseEntityUtil.add(ShiroUtils.getUserId(), communityGreyCode);
        communityGreyCode.setUserId(userId);
        communityGreyCodeService.save(communityGreyCode);
        // ??????????????????
        BaseSenderMessageBo baseSenderMessageBo = new BaseSenderMessageBo();
        baseSenderMessageBo.setTo(Collections.singletonList(userId));
        baseSenderMessageBo.setSubject("xx????????????????????????");
        User user = this.getById(userId);
        String sex = user.getSex().equals(SexTypeEnum.MALE.getCode()) ? " ??????":" ??????";
        String content = "?????????"+this.getById(userId).getRealName()+ sex +"???????????????" + ExamineStatusEnum.isSuccessEnum(Integer.parseInt(examinationStatus));
        if (Objects.equals(Integer.parseInt(examinationStatus), ExamineStatusEnum.YES.getCode())){
            content += "???????????????????????????????????????????????????";
        }else {
            content += "????????????????????????????????????";
        }
        baseSenderMessageBo.setContent(content);
        baseSenderMessageBo.setIsSenderEmail(BooleanTypeEnum.YES.getCode());
        communityMessageService.sendSimpleMessage(baseSenderMessageBo);
    }

    /**
     * ??????????????????
     *
     * @param queryUserBo ????????????
     * @param pageNum     ??????
     * @param pageSize    ?????????
     * @return com.github.pagehelper.PageInfo<com.kennyouchou.community.vo.UserHouseholdVo>
     * @author kennyouchou
     * @since 2022/11/3 22:39
     **/
    @Override
    public PageInfo<UserHouseholdVo> findUserList(QueryUserListBo queryUserBo, Integer pageNum, Integer pageSize) {
        if (queryUserBo.getStatus().equals(UserTypeEnum.COMMUNITY_USER.getCode())) {
            // ????????????
            queryUserBo.setIsEnableUser(BooleanTypeEnum.YES.getCode());
            queryUserBo.setIsEnableHouseHold(BooleanTypeEnum.YES.getCode());
        }else if(queryUserBo.getStatus().equals(UserTypeEnum.SYSTEM_USER.getCode())){
            // ?????????
            queryUserBo.setIsEnableUser(BooleanTypeEnum.YES.getCode());
        }else if(queryUserBo.getStatus().equals(UserTypeEnum.OTHER_USER.getCode())){
            // ???????????????
            queryUserBo.setIsEnableUser(BooleanTypeEnum.NO.getCode());
        }else if (queryUserBo.getStatus().equals(UserTypeEnum.SYSTEM_OTHER_USER.getCode())){
            // ?????????  ???????????????
        }
        List<String> periodIds = roleResourceService.findUserPeriodId(ShiroUtils.getUserId());
        queryUserBo.setPeriodIds(periodIds);
        // ????????????
        PageHelper.startPage(pageNum, pageSize);
        PageInfo<UserHouseholdVo> userHouseholdVoPageInfo = new PageInfo<>(this.baseMapper.findUserList(queryUserBo));
        List<UserHouseholdVo> userHouseholdVoList = userHouseholdVoPageInfo.getList();
        userHouseholdVoList = userHouseholdVoList.stream().peek(item -> {
            // ??????????????????
            List<Role> roleList = roleService.findRoleByUserId(item.getUserId());
            if (CollectionUtils.isNotEmpty(roleList)){
                List<RoleVo> roleVoList = roleList.stream().map(role -> {
                    RoleVo roleVo = new RoleVo();
                    BeanUtils.copyProperties(role, roleVo);
                    return roleVo;
                }).collect(Collectors.toList());
                ShiroUser user = item.getUser();
                user.setRoleVoList(roleVoList);
                item.setUser(user);
            }
            AtomicReference<String> roleTypeName = new AtomicReference<>("??????????????????");
            List<RoleVo> roleVoList = item.getUser().getRoleVoList();
            if (CollectionUtils.isNotEmpty(roleVoList)) {
                roleVoList.forEach(role -> {
                    if (Objects.equals(RoleTypeEnum.ADMIN.getValue(), role.getLabel()) || Objects.equals(RoleTypeEnum.COMMUNITY_ADMIN.getValue(), role.getLabel())) {
                        if (!roleTypeName.get().equals(RoleTypeEnum.ADMIN.getName())) {
                            roleTypeName.set(role.getRoleName());
                        }
                    }
                });
            }
            item.setRoleTypeName(roleTypeName.get());
        }).collect(Collectors.toList());
        userHouseholdVoPageInfo.setList(userHouseholdVoList);
        return userHouseholdVoPageInfo;
    }

    /**
     * ??????????????????
     *
     * @param userId ??????id
     * @author kennyouchou
     * @since 2022/11/4 12:27
     **/
    @Override
    public void resetPassword(String userId) throws  LException {
        // ????????????
        User user = this.getById(userId);
        if (Objects.isNull(user)) {
            throw new LException("???????????????");
        }
        // ????????????
        user.setPassword(systemProperties.getSystemPassword());
        // ??????????????????
        this.updateById(user);
    }

    /**
     * ????????????
     *
     * @param userId ??????id
     * @author kennyouchou
     * @since 2022/11/4 12:27
     **/
    @Override
    public void deleteUser(String userId) throws  LException {
        // ????????????
        User user = this.getById(userId);
        if (Objects.isNull(user)) {
            throw new LException("???????????????");
        }
        // ????????????
        this.removeById(userId);
        // ???????????????????????????
        communityHouseholdUserService.deleteByUser(userId);
        // ???????????????
        communityGreenCodeService.invalidateAll(userId);
    }

    /**
     * ??????????????????
     *
     * @param pageNum  ??????
     * @param pageSize ????????????
     * @param name     ?????????
     * @return com.github.pagehelper.PageInfo<com.kennyouchou.author.entity.User>
     * @author kennyouchou
     * @since 2022/11/5 18:40
     **/
    @Override
    public PageInfo<User> findAllUser(Integer pageNum, Integer pageSize, String name) {
        PageHelper.startPage(pageNum, pageSize);
        LambdaUpdateWrapper<User> wrapper = new LambdaUpdateWrapper<>();
        wrapper.like(StringUtils.isNotBlank(name),User::getRealName, name);
        List<User> userList = this.list(wrapper);
        return new PageInfo<>(userList);
    }
}
