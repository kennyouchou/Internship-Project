<template>
  <div class="box">
    <form action="/">
      <van-search v-model="query.subject" placeholder="请输入搜索关键词" />
    </form>
    <van-pull-refresh class="box" v-model="refreshing" @refresh="onRefresh">
      <van-list v-model="loading" :immediate-check="true" :error.sync="error" :finished="finished" finished-text="没有更多了" error-text="请求失败，点击重新加载" @load="onLoad">
        <message-item v-for="item in list" :key="item.emailId" :active="active" :messageInfo="item" />
      </van-list>
    </van-pull-refresh>
    <van-dialog v-model="show" :title="selectMessageInfo.subject">
      <div class="box infoBox">
        <van-field v-model="selectMessageInfo.fromUserName" label="发件人" readonly />
        <van-field v-model="selectMessageInfo.toUserName" label="收件人" readonly />
        <van-field v-model="isEmailStr" label="是否邮件提醒" readonly />
        <van-field v-model="selectMessageInfo.emailCreateDate" label="发件时间" readonly />
        <van-field v-model="selectMessageInfo.subject" label="主题" readonly />
        <van-field v-model="selectMessageInfo.content" rows="3" type="textarea" label="内容" readonly />
      </div>
    </van-dialog>
  </div>
</template>

<script>
import { findMessageReq, readMessageReq } from '../../../../utils/api'
import MessageItem from './MessageItem.vue'
export default {
  props: {
    active: {
      type: Number,//类型
      required: false,//必要性
      default: 0 //默认值
    }
  },
  name: "ListGroup",
  components: { MessageItem },
  data() {
    return {
      show: false,
      refreshing: false,
      loading: false,
      finished: false,
      error: false,
      pageInfo: {
        pageSize: 6,
        pageNum: 1,
        hasNextPage: false, // 是否有下一页
        hasPreviousPage: false, // 是否有上一页
        isFirstPage: true, // 是否第一页
        isLastPage: true, // 是否最后一页
      },
      list: [], // 页面列表信息
      query: {
        isEnable: 0,
        subject: '',
      },
      selectMessageInfo: {

      }
    }
  },
  watch: {
    query: {
      deep: true, // 深度监视
      immediate: false, //初始化时让handler调用一下
      handler(newValue, oldVale) { //handler什么时候调用？当isHot发生改变时。
        this.list.splice(0, this.list.length)
        this.handlPage()
        this.findMessage() // 查询列表
      }
    },
    active: {
      immediate: true,
      handler(newValue, oldVale) {
        this.query.isEnable = newValue
      }
    }
  },
  computed: {
    isEmailStr() {
      return this.selectMessageInfo.isEmail == 0 ? '否' : '是'
    }
  },
  created() {
    this.findMessage()
  },
  mounted() {

    this.$bus.$on("showInfo", (messageInfo) => { //在总线上创建自定义事件用于接收数据
      this.selectMessageInfo = messageInfo
      this.show = true
      this.readMessage(1)
      if (this.active == 0) {
        this.list.forEach((value, index) => {
          if (value.emailId == this.selectMessageInfo.emailId) {
            this.list.splice(index, 1)
          }
        })
      }
    })

    this.$bus.$on("noRead", (emailId) => { //在总线上创建自定义事件用于接收数据
      this.selectMessageInfo.emailId = emailId
      this.readMessage(0)
      this.list.forEach((value, index) => {
        if (value.emailId == emailId) {
          this.list.splice(index, 1)
        }
      })
    })
  },
  beforeDestroy() {
    this.$bus.$off("showInfo") //生命周期结束，销毁自定义事件
    this.$bus.$off("noRead")
  },
  methods: {
    readMessage(isEnable) {
      let params = {
        emailId: this.selectMessageInfo.emailId,
        isEnable: isEnable
      }
      readMessageReq(params).then(res => {
        if (res.code == 200) {
          console.log("消息读取成功")
        } else {
          this.$toast("消息读取失败")
        }

      })
    },
    findMessage() {
      let params = {
        pageSize: this.pageInfo.pageSize,
        pageNum: this.pageInfo.pageNum,
        isEnable: this.query.isEnable,
        subject: this.query.subject
      }
      findMessageReq(params).then(res => {
        if (res.code == 200) {
          this.handlQuery(res.data) // 添加页面信息
          if (res.data.list.length > 0) {
            res.data.list.forEach(value => this.list.push(value))
          }
        } else {
          this.$toast(res.message)
        }
        // 结束刷新状态
        this.refreshing = false
        // 加载状态结束
        this.loading = false;
        if (res.data.hasNextPage) {
          this.finished = false
        } else {
          this.finished = true
        }
      })
    },
    onRefresh() {
      this.refreshing = true // 加载状态
      this.list.splice(0, this.list.length) // 清空列表
      this.pageInfo.pageSize = 6
      this.pageInfo.pageNum = 1
      this.findMessage() // 查询列表
    },
    handlQuery(data) {
      this.pageInfo.hasNextPage = data.hasNextPage // 是否有下一页
      this.pageInfo.hasPreviousPage = data.hasPreviousPage // 是否有上一页
      this.pageInfo.isFirstPage = data.isFirstPage // 是否第一页
      this.pageInfo.isLastPage = data.isLastPage // 是否最后一页
    },
    onLoad() {
      // 判断是否有下一页
      if (this.pageInfo.hasNextPage) {
        this.loading = true
        // 页码+1
        this.pageInfo.pageNum++
        // 调用查询方法
        this.findMessage()
      } else {
        // 加载状态结束
        this.loading = false;
        // 数据全部加载完成
        this.finished = true;
      }
    },
    handlPage() {
      this.pageInfo = {
        pageSize: 6,
        pageNum: 1,
        hasNextPage: false, // 是否有下一页
        hasPreviousPage: false, // 是否有上一页
        isFirstPage: true, // 是否第一页
        isLastPage: true, // 是否最后一页
      }
    }
  }
}
</script>

<style scoped>
.infoBox {
  margin: 6px;
  height: 200px;
  overflow: auto;
}
</style>