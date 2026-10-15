import { createApp } from "vue";
import App from "./App.vue";
import router from "@/router";
import store from "@/store";
import ElementPlus from "element-plus";
import zhCn from "element-plus/es/locale/lang/zh-cn";
import "element-plus/dist/index.css";
import "@/assets/style/common.css";
// Element Plus 负责表单、表格与确认框；布局样式集中管理。
createApp(App).use(ElementPlus, { locale: zhCn }).use(store).use(router).mount("#app");
