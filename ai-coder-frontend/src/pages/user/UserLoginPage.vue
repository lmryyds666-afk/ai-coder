<template>

  <div class="UserLoginPage">
    <h2 class="title">灵码云-用户登录</h2>
    <div class="desc">
      一句话，呈所想，与AI对话轻松创建应用和网站
    </div>
    <a-form
      :model="formState"
      name="basic"
      autocomplete="off"
      @finish="handleSubmit"
    >
      <a-form-item
        label="用户账号"
        name="userAccount"
        :rules="[{ required: true, message: '请输入账号' }]"
      >
        <a-input v-model:value="formState.userAccount" />
      </a-form-item>

      <a-form-item
        label="用户密码"
        name="userPassword"
        :rules="[{ required: true, message: '请输入登录密码，不得少于8位' },
        {min: 8,message: '登录密码不得少于8位'}]"
      >
        <a-input-password v-model:value="formState.userPassword" />
      </a-form-item>


      <div class="tips">
        没有账号？
        <router-link to="/user/register">去注册</router-link>
      </div>


      <a-form-item :wrapper-col="{ offset: 8, span: 16 }">
        <a-button type="primary" html-type="登录">登录</a-button>
      </a-form-item>
    </a-form>
  </div>
</template>

<script setup lang="ts">
import { reactive } from 'vue';
import { userLogin } from '@/api/userController.ts'
import { message } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import { useLoginUserStore } from '@/stores/loginUser.ts'


const formState = reactive<API.UserLoginRequest>({
  userAccount: '',
  userPassword: '',

});

const router =useRouter()
const loginUserStore = useLoginUserStore()
const handleSubmit =async (values: any) => {
  const res =await userLogin( values)
  //登录成功，把登录用户信息保存到全局状态中
  if(res.data.code === 0 && res.data.data){
    await loginUserStore.fetchLoginUser ()
    message.success('登录成功')
    router.push({
      path: '/',
      replace: true
    })
  }
};


</script>

<style scoped>
#userLoginPage {
  max-width: 360px;
  margin: 0 auto;
}

.title {
  text-align: center;
  margin-bottom: 16px;
}

.desc {
  text-align: center;
  color: #bbb;
  margin-bottom: 16px;
}

.tips {
  margin-bottom: 16px;
  color: #bbb;
  font-size: 13px;
  text-align: right;
}

</style>
