import { createRouter, createWebHistory } from "vue-router";
import store from "@/store";
// 所有菜单都有明确页面，刷新时先确认会话有效再放行。
const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: "/login", name: "login", component: () => import("@/views/Login.vue") },
    { path: "/", component: () => import("@/views/Shell.vue"), children: [
      { path: "", redirect: "/dashboard" },
      { path: "main", redirect: "/dashboard" },
      { path: "dashboard", component: () => import("@/views/Dashboard.vue"), meta: { title: "经营概览" } },
      ...["houses","tenants","contracts","bills","users","roles","audits"].map(name => ({
        path: name, component: () => import("@/views/Workspace.vue"), meta: { section: name, title: { houses:"房源档案",tenants:"租客档案",contracts:"租赁合同",bills:"账单与收款",users:"账号管理",roles:"角色权限",audits:"操作日志" }[name], admin: ["users","roles","audits"].includes(name) }
      })),
      { path: "user", redirect: "/users" },
      { path: "api-docs", component: () => import("@/views/ApiDocs.vue"), meta: { title: "接口说明" } }
    ]},
    { path: "/:pathMatch(.*)*", redirect: "/dashboard" }
  ]
});
router.beforeEach(async to => {
  if (to.path === "/login") return true;
  if (!sessionStorage.getItem("rental.token")) return { path: "/login" };
  try {
    if (!store.state.user) await store.dispatch("RESTORE");
    if (to.meta.admin && !store.getters.can("system:manage")) return "/dashboard";
    return true;
  } catch { return { path: "/login" }; }
});
export default router;