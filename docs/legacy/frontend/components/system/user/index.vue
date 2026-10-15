<template>
  <el-form :inline="true" :model="queryParams" class="demo-form-inline">
    <el-form-item label="用户名">
      <el-input
        v-model="queryParams.userName"
        placeholder="请输入用户名"
        clearable
      />
    </el-form-item>

    <el-form-item label="昵称">
      <el-input
        v-model="queryParams.nickName"
        placeholder="请输入昵称"
        clearable
      />
    </el-form-item>

    <el-form-item>
      <el-button type="primary" @click="onSubmit">查询</el-button>
    </el-form-item>
  </el-form>

  <el-table
    ref="tableRef"
    row-key="userName"
    :data="tableData"
    style="width: 100%"
  >
    <el-table-column
      prop="userName"
      label="用户名"
      width="180"
    ></el-table-column>
    <el-table-column prop="nickName" label="昵称" width="180"></el-table-column>
    <el-table-column prop="email" label="邮箱"></el-table-column>
  </el-table>

  <el-pagination
    background
    layout="prev, pager, next"
    :total="total"
    :page-sizes="[10, 20, 50, 100]"
    :page-size="queryParams.size"
    @current-change="changePage"
  />
</template>

<script setup>
import { ref } from "vue";
import { listUser } from "@/api/user";
import { onMounted } from "vue";

const queryParams = ref({
  size: 10,
  page: 0,
  userName: "",
  nickName: "",
});

const tableData = ref([]);

const total = ref(0);

const getList = () => {
  listUser(queryParams.value).then((res) => {
    tableData.value = res.data.content;
    console.log(tableData);
    total.value = res.data.totalElements;
    queryParams.value.size = res.data.size;
  });
};

const changePage = async function (current) {
  queryParams.value.page = current - 1;
  getList();
};

onMounted(() => {
  getList();
});

const onSubmit = function () {
  getList();
};
</script>
