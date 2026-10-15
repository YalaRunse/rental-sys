import { createStore } from "vuex";
import request from "@/api";
// 只保存服务端返回的安全身份信息，权限以服务端检查为准。
export default createStore({
  state: { user: null },
  getters: { can: state => permission => !!state.user && (state.user.permissions.includes("*") || state.user.permissions.includes(permission)) },
  mutations: { SET_USER(state, user) { state.user = user; } },
  actions: {
    async LOGIN({ commit }, credentials) {
      const result = await request.post("/auth/login", credentials);
      sessionStorage.setItem("rental.token", result.token);
      sessionStorage.setItem("rental.user", JSON.stringify(result.user));
      commit("SET_USER", result.user);
    },
    async RESTORE({ commit }) { const user = await request.get("/auth/me"); commit("SET_USER", user); },
    async LOGOUT({ commit }) {
      try { await request.post("/auth/logout"); }
      finally { sessionStorage.removeItem("rental.token"); sessionStorage.removeItem("rental.user"); commit("SET_USER", null); }
    }
  }
});