<template>
  <div class="login-page">
    <section class="login-story">
      <div class="brand"><span class="brand-mark">栖</span><div>栖居 <small>RENTAL DESK</small></div></div>
      <div class="story-content"><div class="eyebrow">把每一次出租，管理得有条有理</div><h1>从一间好房，<br>到一份安心的租约。</h1><p>房源、租客、合同与账单在这里衔接。<br>让状态清晰，让每笔收款都有据可查。</p>
        <div class="story-building"><i v-for="n in 18" :key="n"></i></div>
      </div>
      <footer>独立学习与演示项目 · Java / Spring MVC / MyBatis / Redis</footer>
    </section>
    <section class="login-form-area"><div class="login-form">
      <div class="eyebrow">WELCOME BACK</div><h2>登录管理工作台</h2><p class="muted">欢迎回来，开始今天的租赁管理。</p>
      <el-form @submit.prevent="submit" label-position="top" :model="form">
        <el-form-item label="账号"><el-input v-model="form.username" size="large" placeholder="请输入账号" autocomplete="username" /></el-form-item>
        <el-form-item label="密码"><el-input v-model="form.password" size="large" type="password" show-password placeholder="请输入密码" autocomplete="current-password" @keyup.enter="submit" /></el-form-item>
        <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon class="login-error" />
        <el-button type="primary" size="large" :loading="loading" class="login-submit" @click="submit">进入工作台 <span>→</span></el-button>
      </el-form>
      <div class="demo-note"><b>演示账号</b><span>admin / 111111</span><small>样例数据均为虚构，系统支持管理员、运营与只读角色。</small></div>
    </div><div class="login-footer">栖居 · 租赁管理系统 <span>v2.0</span></div></section>
  </div>
</template>
<script setup>
import { reactive, ref } from "vue";
import { useStore } from "vuex";
import { useRouter } from "vue-router";
// 显式展示失败并恢复按钮状态，避免旧版 Promise 失败后页面无反馈。
const form = reactive({ username: "admin", password: "111111" });
const loading = ref(false); const error = ref(""); const store = useStore(); const router = useRouter();
async function submit() {
  if (loading.value) return;
  if (!form.username || !form.password) { error.value = "请填写账号和密码"; return; }
  loading.value = true; error.value = "";
  try { await store.dispatch("LOGIN", form); await router.push("/dashboard"); }
  catch (failure) { error.value = failure.message; }
  finally { loading.value = false; }
}
</script>