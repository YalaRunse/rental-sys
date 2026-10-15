<template>
  <div class="app-shell">
    <aside class="sidebar">
      <router-link to="/dashboard" class="brand"><span class="brand-mark">栖</span><div>栖居<small>RENTAL DESK</small></div></router-link>
      <div class="nav-label">租赁业务</div>
      <router-link v-for="item in menus" :key="item.path" :to="item.path" class="nav-item"><span class="nav-glyph">{{item.icon}}</span>{{item.label}}<span class="nav-arrow">›</span></router-link>
      <template v-if="store.getters.can('system:manage')"><div class="nav-label system-label">系统管理</div><router-link v-for="item in system" :key="item.path" :to="item.path" class="nav-item"><span class="nav-glyph">{{item.icon}}</span>{{item.label}}<span class="nav-arrow">›</span></router-link></template>
      <div class="sidebar-bottom"><span class="live-dot"></span>租赁管理演示环境<small>v2.0 · 数据可追踪</small></div>
    </aside>
    <main class="main-shell">
      <header class="topbar"><div class="breadcrumb">工作台 <span>/</span> <b>{{route.meta.title}}</b></div><div class="profile"><span class="profile-role">{{role}}</span><span class="avatar">{{store.state.user?.nickname?.slice(0,1)}}</span><span>{{store.state.user?.nickname}}</span><button class="text-button" @click="logout">退出</button></div></header>
      <div class="content"><router-view :key="route.path" /></div>
      <footer class="app-footer">栖居租赁管理 · 学习与演示项目 <router-link to="/api-docs">查看接口说明 ↗</router-link></footer>
    </main>
  </div>
</template>
<script setup>
import { computed } from "vue"; import { useStore } from "vuex"; import { useRoute, useRouter } from "vue-router"; import { ElMessage } from "element-plus";
const store = useStore(); const route = useRoute(); const router = useRouter();
// 导航与权限联动；真正的权限校验仍在服务端。
const menus = [{path:"/dashboard",label:"经营概览",icon:"◫"},{path:"/houses",label:"房源档案",icon:"⌂"},{path:"/tenants",label:"租客档案",icon:"♙"},{path:"/contracts",label:"租赁合同",icon:"▤"},{path:"/bills",label:"账单与收款",icon:"¥"}];
const system = [{path:"/users",label:"账号管理",icon:"♧"},{path:"/roles",label:"角色权限",icon:"◇"},{path:"/audits",label:"操作日志",icon:"☷"}];
const role = computed(() => store.state.user?.roles.includes("admin") ? "管理员" : store.state.user?.roles.includes("operator") ? "租赁运营" : "只读观察员");
async function logout() {try {await store.dispatch("LOGOUT");await router.push("/login");}catch(error){ElMessage.error(error.message);await router.push("/login");}}
</script>