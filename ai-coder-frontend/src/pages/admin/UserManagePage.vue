<template>
  <div id="userManagePage">
    <!-- 搜索表单 -->
    <a-form layout="inline" :model="searchParams" @finish="doSearch">
      <a-form-item label="账号">
        <a-input v-model:value="searchParams.userAccount" placeholder="输入账号" />
      </a-form-item>
      <a-form-item label="用户名">
        <a-input v-model:value="searchParams.userName" placeholder="输入用户名" />
      </a-form-item>
      <a-form-item>
        <a-button type="primary" html-type="submit">搜索</a-button>
      </a-form-item>
    </a-form>
    <a-divider />
    <!-- 表格 -->
  </div>

  <a-table :dataSource="data" :columns="columns" :pagination="pagination" @change="doTableChange">
    <!-- 表格单元格插槽 -->
    <template #bodyCell="{ column, record }">
      <!-- 头像：统一大小 + 不变形 -->
      <template v-if="column.dataIndex === 'userAvatar'">
        <a-image :src="record.userAvatar" :width="60" :height="60" preview
          style="object-fit: cover; border-radius: 4px" />
      </template>

      <!-- 用户角色 -->
      <template v-else-if="column.dataIndex === 'userRole'">
        <a-tag v-if="record.userRole === 'admin'" color="green">管理员</a-tag>
        <a-tag v-else color="blue">普通用户</a-tag>
      </template>

      <!-- 创建时间 -->
      <template v-else-if="column.dataIndex === 'createTime'">
        {{ dayjs(record.createTime).format('YYYY-MM-DD HH:mm:ss') }}
      </template>

      <!-- 操作 -->
      <template v-else-if="column.key === 'action'">
        <a-button danger @click="doDelete(record.id)">删除</a-button>
      </template>
    </template>
  </a-table>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue';
import { message } from 'ant-design-vue';
import { deleteUser, listUserVoByPage } from '@/api/userController';
import dayjs from 'dayjs';

// 数据
const data = ref<API.UserVO[]>([]);
const total = ref(0);

// 搜索条件
const searchParams = reactive<API.UserQueryRequest>({
  pageNum: 1,
  pageSize: 10,
});

// 获取数据
const fetchData = async () => {
  try {
    const res = await listUserVoByPage({ ...searchParams });
    if (res.data.code === 0) {
      data.value = res.data.data.records ?? [];
      total.value = res.data.data.totalRow ?? 0;
    } else {
      message.error('获取数据失败：' + res.data.message);
    }
  } catch (error) {
    message.error('请求异常');
    console.error(error);
  }
};

// 分页参数
const pagination = computed(() => {
  return {
    current: searchParams.pageNum ?? 1,
    pageSize: searchParams.pageSize ?? 10,
    total: total.value,
    showSizeChanger: true,
    showTotal: (total: number) => `共 ${total} 条`,
  }
})
// 表格变化处理
const doTableChange = (page: any) => {
  searchParams.pageNum = page.current
  searchParams.pageSize = page.pageSize
  fetchData()
}

// 获取搜索数据
const doSearch = () => {
  // 重置页码
  searchParams.pageNum = 1
  fetchData()
}

// 删除数据
const doDelete = async (id: string) => {
  if (!id) {
    return
  }
  const res = await deleteUser({ id })
  if (res.data.code === 0) {
    message.success('删除成功')
    // 刷新数据
    fetchData()
  } else {
    message.error('删除失败')
  }
}



onMounted(() => {
  fetchData();
});

// 表格列
const columns = [
  { title: 'id', dataIndex: 'id' },
  { title: '账号', dataIndex: 'userAccount' },
  { title: '用户名', dataIndex: 'userName' },
  { title: '头像', dataIndex: 'userAvatar' },
  { title: '简介', dataIndex: 'userProfile' },
  { title: '用户角色', dataIndex: 'userRole' },
  { title: '创建时间', dataIndex: 'createTime' },
  { title: '操作', key: 'action' },
];
</script>
