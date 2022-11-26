package com.kennyouchou.community.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.github.pagehelper.PageInfo;
import com.kennyouchou.community.entity.CommunityPeriod;
import com.kennyouchou.community.vo.AddressVo;
import com.kennyouchou.community.vo.CascadeVo;
import com.kennyouchou.community.vo.PeriodInfoVo;

import java.util.List;

/**
 * <p>
 * 小区-期 服务类
 * </p>
 *
 * @author kennyouchou
 * @since 2022-10-01
 */
public interface CommunityPeriodService extends IService<CommunityPeriod> {

    /**
     * 添加期信息
     * @param period 期信息
     * @return CommunityPeriod
     * @author kennyouchou
     * @since 2022/10/4 15:31
     **/
    CommunityPeriod add(CommunityPeriod period);

    /**
     * 查询期
     * @return java.util.List<com.kennyouchou.community.entity.CommunityPeriod>
     * @author kennyouchou
     * @since 2022/10/4 17:05
     **/
    List<CommunityPeriod> find();

    /**
     * 查询所有期
     * @return java.util.List<com.kennyouchou.community.entity.CommunityPeriod>
     * @author kennyouchou
     * @since 2022/10/5 19:46
     **/
    List<CommunityPeriod> findAll();


    /**
     * 根据期id得到小区信息
     *
     * @param id        期id
     * @param pageSize  页面大小
     * @param pageNum   起始页
     * @return com.github.pagehelper.PageInfo<com.kennyouchou.community.vo.CommunityInfo>
     * @author kennyouchou
     * @since 2022/10/6 18:30
     **/
    PageInfo<PeriodInfoVo> getInfoByPeriodId(String id, Integer pageSize, Integer pageNum);

    /**
     * 获取社区级联选择结构
     * @return java.util.List<com.kennyouchou.community.vo.CascadeVo>
     * @author kennyouchou
     * @since 2022/10/8 6:06
     **/
    List<CascadeVo> getCascadeVo();

    /**
     * 获取当前登录人的住址信息
     * @return AddressVo
     * @author kennyouchou
     * @since 2022/10/9 21:24
     **/
    AddressVo findInfoByUser();

    /**
     * 查询登录用户是否含有住址信息
     * @return java.lang.Integer
     * @author kennyouchou
     * @since 2022/10/22 16:12
     **/
    Integer checkUserHasAddress();

}
