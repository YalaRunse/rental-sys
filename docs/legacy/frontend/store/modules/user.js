import { getInfo, login, logout } from "@/api/user.js";
import storage from "@/util/storage";

const user = {
  state: {
    username: "",
    nickname: "",
    token: "",
    roles: [],
    permissions: [],
  },
  getters: {
    ISLOGIN: (state) => {
      return state.username !== "" && state.token !== "";
    },
  },
  mutations: {
    SAVE_USER(state, username) {
      state.username = username;
    },
    SAVE_NICKNAME(state, nickname) {
      state.nickname = nickname;
    },
    SAVE_TOKEN(state, token) {
      state.token = token;
    },
    SAVE_ROLES(state, roles) {
      state.roles = roles;
    },
    SAVE_PERMISSIONS(state, permissions) {
      state.permissions = permissions;
    },
  },
  actions: {
    LOGIN({ commit }, user) {
      return new Promise((resolve) => {
        login(user).then((res) => {
          //需要将获取的数据保存
          commit("SAVE_USER", res.data.user.userName);
          storage.saveSessionString("username", res.data.user.userName);
          commit("SAVE_NICKNAME", res.data.user.nickName);
          storage.saveSessionString("nickname", res.data.user.nickName);
          commit("SAVE_TOKEN", res.data.token);
          storage.saveSessionObject("loginUser", res.data);
          resolve(res);
        });
      });
    },

    LOGOUT({ commit }) {
      return new Promise((resolve) => {
        logout().then((res) => {
          //需要将获取的数据保存
          commit("SAVE_USER", "");
          commit("SAVE_NICKNAME", "");
          commit("SAVE_TOKEN", "");
          storage.remove("loginUser");
          resolve(res);
        });
      });
    },

    RECOVERY_USER({ commit }) {
      //从storage中获得数据
      let loginUser = storage.getSessionObject("loginUser");
      if (loginUser) {
        commit("SAVE_USER", loginUser.user.userName);
        commit("SAVE_NICKNAME", loginUser.user.nickName);
        commit("SAVE_TOKEN", loginUser.token);
      }
    },

    GET_INFO({ commit }) {
      return new Promise((resolve) => {
        getInfo().then((res) => {
          console.log(res);
          commit("SAVE_ROLES", res.data.roles);
          commit("SAVE_PERMISSIONS", res.data.perms);
          resolve();
        });
      });
    },
  },
};

export default user;
