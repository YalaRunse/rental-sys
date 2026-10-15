import axios from "axios";
// API 使用同源代理，换端口或部署地址时无需修改源码。
const request = axios.create({ baseURL: "/admin/api", timeout: 15000 });
request.interceptors.request.use(config => {
  const token = sessionStorage.getItem("rental.token");
  if (token) config.headers.Authorization = "Bearer " + token;
  return config;
});
request.interceptors.response.use(response => response.data.data, error => {
  if (error.response?.status === 401) {
    sessionStorage.removeItem("rental.token");
    sessionStorage.removeItem("rental.user");
    if (location.pathname !== "/login") location.assign("/login");
  }
  const data = error.response?.data;
  const message = data?.message || "服务暂不可用，请稍后重试";
  const failure = new Error(message + (data?.requestId ? "（请求编号：" + data.requestId.slice(0, 8) + "）" : ""));
  failure.status = error.response?.status;
  return Promise.reject(failure);
});
export default request;