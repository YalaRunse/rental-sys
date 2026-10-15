<template>
  <div class="page-head"><div><div class="eyebrow">OPERATIONS OVERVIEW</div><h1>经营概览</h1><p>让房源状态与收款进度，一目了然。</p></div><div class="head-date">{{date}}<span class="badge soft">实时业务数据</span></div></div>
  <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" />
  <section class="stats-grid" v-loading="loading">
    <div v-for="card in cards" :key="card.label" class="stat-card"><div class="stat-top">{{card.label}}<span>{{card.icon}}</span></div><div class="stat-number">{{card.value}}<small>{{card.unit}}</small></div><div class="stat-note">{{card.note}}</div></div>
  </section>
  <div class="dashboard-grid">
    <section class="panel"><div class="panel-heading"><div><h3>房源出租情况</h3><p>当前在管房源的实时状态</p></div><router-link to="/houses">房源管理 →</router-link></div>
      <div class="occupancy"><div class="donut" :style="{'--rate':rate+'%'}"><div><strong>{{rate}}<small>%</small></strong><span>出租率</span></div></div><div class="occupancy-legend"><div><i class="legend-dot green"></i>已出租<strong>{{summary.occupied||0}} 套</strong></div><div><i class="legend-dot light"></i>空置中<strong>{{summary.vacant||0}} 套</strong></div><div><i class="legend-dot grey"></i>已下架<strong>{{Number(summary.houses||0)-Number(summary.occupied||0)-Number(summary.vacant||0)}} 套</strong></div><p>下架房源不参与可租房源分配。</p></div></div>
    </section>
    <section class="panel"><div class="panel-heading"><div><h3>应收与待办</h3><p>按当前账单与实收记录统计</p></div><span class="badge warn">账单跟进</span></div><div class="due-summary"><div><span>账单剩余应收</span><strong>¥ {{money(summary.receivable)}}</strong><small>包含未到期账单</small></div><div><span>已逾期未收</span><strong class="amber">¥ {{money(summary.overdue)}}</strong><small>到期日早于今天</small></div></div><router-link to="/bills" class="panel-action">查看账单，安排本次收款 <span>→</span></router-link></section>
  </div>
  <section class="panel recent-panel"><div class="panel-heading"><div><h3>最近签订的合同</h3><p>每份合同关联房源、租客和每月账单</p></div><router-link to="/contracts">全部合同 →</router-link></div>
    <el-table :data="contracts" stripe><el-table-column prop="contractNo" label="合同编号" min-width="210"/><el-table-column prop="houseTitle" label="房源" min-width="170"/><el-table-column prop="tenantName" label="租客" width="110"/><el-table-column label="租期" min-width="205"><template #default="{row}">{{row.startDate}} — {{row.endDate}}</template></el-table-column><el-table-column label="月租金" width="130"><template #default="{row}">¥ {{money(row.monthlyRent)}}</template></el-table-column><el-table-column label="状态" width="100"><template #default="{row}"><span class="badge" :class="row.status==='ACTIVE'?'soft':'neutral'">{{row.status==='ACTIVE'?'生效中':'已结束'}}</span></template></el-table-column></el-table>
  </section>
  <div class="workflow-note"><b>业务闭环</b><span>① 房源建档</span><i>→</i><span>② 租客签约</span><i>→</i><span>③ 月账单收款</span><i>→</i><span>④ 结清退租</span><small>合同、金额与状态变更均由服务端校验</small></div>
</template>
<script setup>
import { computed, onMounted, ref } from "vue"; import api from "@/api";
const loading=ref(true);const error=ref("");const summary=ref({});const contracts=ref([]);
const date=new Intl.DateTimeFormat("zh-CN",{year:"numeric",month:"long",day:"numeric",weekday:"long"}).format(new Date());
const money=value=>Number(value||0).toLocaleString("zh-CN",{minimumFractionDigits:2,maximumFractionDigits:2});
const rate=computed(()=>summary.value.houses?Math.round(Number(summary.value.occupied)/Number(summary.value.houses)*100):0);
// 所有数字直接来自真实接口，空数据展示为零。
const cards=computed(()=>[{label:"在管房源",value:summary.value.houses||0,unit:"套",note:"房源档案统一管理",icon:"⌂"},{label:"生效合同",value:summary.value.activeContracts||0,unit:"份",note:"每套房源最多一份生效租约",icon:"▤"},{label:"本月实收",value:money(summary.value.monthIncome),unit:"元",note:"仅统计实际收款流水",icon:"¥"},{label:"可出租房源",value:summary.value.vacant||0,unit:"套",note:"空置房源可直接签约",icon:"◇"}]);
onMounted(async()=>{try{const [overview,list]=await Promise.all([api.get("/dashboard"),api.get("/contracts",{params:{page:1,size:5}})]);summary.value=overview;contracts.value=list.records;}catch(failure){error.value=failure.message;}finally{loading.value=false;}});
</script>