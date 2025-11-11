<template>
  <div class="order-task-query-container">
    <h2>订单任务查询</h2>
    
    <!-- 查询表单 -->
    <div class="query-form">
      <div class="form-item">
        <label>订单名称：</label>
        <input v-model="queryParams.orderName" type="text" placeholder="请输入订单名称" />
      </div>
      
      <div class="form-item">
        <label>计划开始时间：</label>
        <input v-model="queryParams.startTime" type="date" />
      </div>
      
      <div class="form-item">
        <label>计划结束时间：</label>
        <input v-model="queryParams.endTime" type="date" />
      </div>
      
      <div class="form-item">
        <label>任务状态：</label>
        <select v-model="queryParams.status" multiple style="height: 100px;">
          <option value="生产中">生产中</option>
          <option value="已完成">已完成</option>
          <option value="已暂停">已暂停</option>
          <option value="未开始">未开始</option>
        </select>
      </div>
      
      <div class="form-item">
        <label>每页显示条数：</label>
        <select v-model="pageSize" @change="handlePageSizeChange">
          <option value="10">10</option>
          <option value="20">20</option>
          <option value="50">50</option>
          <option value="100">100</option>
        </select>
      </div>
      
      <div class="form-actions">
        <button @click="queryOrderTasks" class="btn-primary">查询</button>
        <button @click="resetQuery" class="btn-secondary">重置</button>
      </div>
    </div>
    
    <!-- 查询结果 -->
    <div class="result-container">
      <div class="result-header">
        <h3>查询结果</h3>
        <span class="result-count">共 {{ totalCount }} 条记录</span>
      </div>
      
      <!-- 错误提示 -->
      <div v-if="error" class="error-message">
        {{ error }}
      </div>
      
      <div class="table-wrapper">
        <table class="data-table">
          <thead>
            <tr>
              <th>任务编号</th>
              <th>订单编号</th>
              <th>订单名称</th>
              <th>工艺路线</th>
              <th>计划数量</th>
              <th>任务状态</th>
              <th>计划开始日期</th>
              <th>计划结束日期</th>
              <th>实际开始日期</th>
              <th>实际结束日期</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="task in orderTasks" :key="task.taskNo">
              <td>{{ task.taskNo }}</td>
              <td>{{ task.orderNo }}</td>
              <td>{{ task.orderName || '-' }}</td>
              <td>{{ task.routeSeq }}</td>
              <td>{{ task.planQuantity }}</td>
              <td :class="getTaskStatusClass(task.taskStatus)">{{ task.taskStatus }}</td>
              <td>{{ formatDate(task.planStartDate) }}</td>
              <td>{{ formatDate(task.planEndDate) }}</td>
              <td>{{ formatDate(task.factStartDate) }}</td>
              <td>{{ formatDate(task.factEndDate) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
      
      <!-- 无数据提示 -->
      <div v-if="orderTasks.length === 0 && !loading" class="no-data">
        暂无数据
      </div>
      
      <!-- 加载中提示 -->
      <div v-if="loading" class="loading">
        加载中...
      </div>
      
      <!-- 分页控件 -->
      <div v-if="totalCount > 0 && !loading" class="pagination">
        <div class="pagination-info">
          显示 {{ (pageNum - 1) * pageSize + 1 }}-{{ Math.min(pageNum * pageSize, totalCount) }} 条，共 {{ totalCount }} 条
        </div>
        <div class="pagination-controls">
          <button 
            class="pagination-btn"
            :disabled="pageNum === 1"
            @click="goToPage(1)"
          >
            首页
          </button>
          <button 
            class="pagination-btn"
            :disabled="pageNum === 1"
            @click="goToPage(pageNum - 1)"
          >
            上一页
          </button>
          
          <!-- 页码按钮 -->
          <button 
            v-for="page in visiblePages" 
            :key="page"
            class="pagination-btn"
            :class="{ active: page === pageNum }"
            @click="goToPage(page)"
          >
            {{ page }}
          </button>
          
          <button 
            class="pagination-btn"
            :disabled="pageNum === totalPages"
            @click="goToPage(pageNum + 1)"
          >
            下一页
          </button>
          <button 
            class="pagination-btn"
            :disabled="pageNum === totalPages"
            @click="goToPage(totalPages)"
          >
            末页
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { factorySchedulingAPI } from '../api/service.js'

export default {
  name: 'OrderTaskQueryView',
  data() {
    return {
      queryParams: {
        orderName: '',
        startTime: '',
        endTime: '',
        status: []
      },
      orderTasks: [],
      loading: false,
      error: null,
      // 分页相关
      pageNum: 1,
      pageSize: 20,
      totalCount: 0,
      totalPages: 0
    }
  },
  computed: {
    // 计算可见的页码范围
    visiblePages() {
      const pages = [];
      const total = this.totalPages;
      const current = this.pageNum;
      
      // 显示当前页附近的页码，最多显示7个
      let startPage = Math.max(1, current - 3);
      let endPage = Math.min(total, startPage + 6);
      
      // 调整起始页码以确保显示足够的页码
      if (endPage - startPage < 6 && startPage > 1) {
        startPage = Math.max(1, endPage - 6);
      }
      
      for (let i = startPage; i <= endPage; i++) {
        pages.push(i);
      }
      
      return pages;
    }
  },
  methods: {
    async queryOrderTasks() {
      // 重置到第一页
      this.pageNum = 1;
      this.loading = true;
      this.error = null;
      
      try {
          // 构建查询参数
          const params = {
            orderName: this.queryParams.orderName,
            startTime: this.queryParams.startTime,
            endTime: this.queryParams.endTime,
            statusList: this.queryParams.status && this.queryParams.status.length > 0 ? this.queryParams.status : undefined,
            pageNum: this.pageNum,
            pageSize: this.pageSize
          };
          
          // 过滤空值参数
          const filteredParams = Object.fromEntries(Object.entries(params).filter(([_, v]) => v !== undefined && v !== ''));
          
          // 使用API服务发送请求到新的分页接口
          const response = await factorySchedulingAPI.mesOrders.getOrderTasksWithPagination(filteredParams);
          
          // 处理分页数据 - 注意：后端返回的格式是包含records数组的对象
          this.orderTasks = response.records || [];
          this.totalCount = response.total || 0;
          this.totalPages = response.totalPages || 0;
      } catch (err) {
        this.error = '查询失败：' + (err.message || '未知错误');
        console.error('查询订单任务失败:', err);
      } finally {
        this.loading = false;
      }
    },
    
    // 跳转到指定页码
    async goToPage(page) {
      if (page < 1 || page > this.totalPages || page === this.pageNum || this.loading) {
        return;
      }
      
      this.pageNum = page;
      this.loading = true;
      
      try {
          // 构建查询参数
          const params = {
            orderName: this.queryParams.orderName,
            startTime: this.queryParams.startTime,
            endTime: this.queryParams.endTime,
            statusList: this.queryParams.status && this.queryParams.status.length > 0 ? this.queryParams.status : undefined,
            pageNum: this.pageNum,
            pageSize: this.pageSize
          };
          
          // 过滤空值参数
          const filteredParams = Object.fromEntries(Object.entries(params).filter(([_, v]) => v !== undefined && v !== ''));
          
          const response = await factorySchedulingAPI.mesOrders.getOrderTasksWithPagination(filteredParams);
          
          this.orderTasks = response.records || [];
      } catch (err) {
        this.error = '获取分页数据失败：' + (err.message || '未知错误');
        console.error('获取分页数据失败:', err);
      } finally {
        this.loading = false;
      }
    },
    
    // 处理每页显示条数变化
    handlePageSizeChange() {
      this.queryOrderTasks(); // 重新查询第一页
    },
    
    resetQuery() {
      this.queryParams = {
        orderName: '',
        startTime: '',
        endTime: '',
        status: []
      };
      this.orderTasks = [];
      this.totalCount = 0;
      this.totalPages = 0;
      this.pageNum = 1;
    },
    
    formatDate(dateString) {
      if (!dateString) return '-';
      
      // 尝试格式化日期
      try {
        const date = new Date(dateString);
        if (isNaN(date.getTime())) {
          return dateString; // 如果日期无效，返回原始字符串
        }
        
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const day = String(date.getDate()).padStart(2, '0');
        const hours = String(date.getHours()).padStart(2, '0');
        const minutes = String(date.getMinutes()).padStart(2, '0');
        
        return `${year}-${month}-${day} ${hours}:${minutes}`;
      } catch (e) {
        return dateString;
      }
    },
    
    getTaskStatusClass(status) {
      // 确保状态值是字符串类型
      const statusStr = String(status || '');
      switch (statusStr) {
        case '生产中':
        case 'PRODUCTION':
          return 'status-processing';
        case '已完成':
        case 'COMPLETED':
          return 'status-completed';
        case '已暂停':
        case 'PAUSED':
          return 'status-paused';
        case '未开始':
        case 'PENDING':
          return 'status-pending';
        default:
          return '';
      }
    }
  }
}
</script>

<style scoped>
.order-task-query-container {
  padding: 20px;
}

h2 {
  margin-bottom: 20px;
  color: var(--text-primary, #333);
}

.query-form {
  background: #fff;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  margin-bottom: 20px;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 15px;
  align-items: end;
}

.form-item {
  display: flex;
  flex-direction: column;
}

.form-item label {
  margin-bottom: 5px;
  font-weight: 500;
  color: var(--text-secondary, #666);
}

.form-item input,
.form-item select {
  padding: 8px 12px;
  border: 1px solid var(--border-color, #ddd);
  border-radius: 4px;
  font-size: 14px;
}

.form-item input:focus,
.form-item select:focus {
  outline: none;
  border-color: var(--primary-color, #409eff);
}

.form-actions {
  display: flex;
  gap: 10px;
}

.btn-primary,
.btn-secondary {
  padding: 8px 20px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  transition: background-color 0.3s;
}

.btn-primary {
  background-color: var(--primary-color, #409eff);
  color: white;
}

.btn-primary:hover {
  background-color: #0056b3;
}

.btn-secondary {
  background-color: #f0f0f0;
  color: var(--text-primary, #333);
  border: 1px solid var(--border-color, #ddd);
}

.btn-secondary:hover {
  background-color: #e0e0e0;
}

.result-container {
  background: #fff;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.result-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.result-header h3 {
  margin: 0;
  color: var(--text-primary, #333);
}

.result-count {
  color: var(--text-secondary, #666);
  font-size: 14px;
}

/* 错误提示 */
.error-message {
  background-color: #fee;
  color: #c33;
  padding: 10px 15px;
  border-radius: 4px;
  margin-bottom: 15px;
  border-left: 4px solid #c33;
  font-size: 14px;
}

.table-wrapper {
  overflow-x: auto;
  margin-bottom: 15px;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
}

.data-table th,
.data-table td {
  padding: 12px;
  text-align: left;
  border-bottom: 1px solid var(--border-color, #ddd);
}

.data-table th {
  background-color: #f5f5f5;
  font-weight: 600;
  color: var(--text-primary, #333);
  position: sticky;
  top: 0;
  z-index: 10;
  white-space: nowrap;
}

.data-table td {
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.data-table tbody tr:hover {
  background-color: #f9f9f9;
}

.status-processing {
  color: #ff9800;
  font-weight: 500;
}

.status-completed {
  color: #4caf50;
  font-weight: 500;
}

.status-paused {
  color: #9e9e9e;
  font-weight: 500;
}

.status-pending {
  color: #2196f3;
  font-weight: 500;
}

.no-data,
.loading {
  text-align: center;
  padding: 40px;
  color: var(--text-secondary, #666);
}

.loading {
  color: var(--primary-color, #409eff);
}

/* 分页控件样式 */
.pagination {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid var(--border-color, #ddd);
  flex-wrap: wrap;
  gap: 15px;
}

.pagination-info {
  color: var(--text-secondary, #666);
  font-size: 14px;
}

.pagination-controls {
  display: flex;
  align-items: center;
  gap: 5px;
  flex-wrap: wrap;
}

.pagination-btn {
  padding: 6px 12px;
  border: 1px solid var(--border-color, #ddd);
  background-color: #fff;
  color: var(--text-primary, #333);
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s ease;
  min-width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.pagination-btn:hover:not(:disabled) {
  background-color: #f0f0f0;
  border-color: var(--primary-color, #409eff);
}

.pagination-btn.active {
  background-color: var(--primary-color, #409eff);
  color: white;
  border-color: var(--primary-color, #409eff);
}

.pagination-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  background-color: #f5f5f5;
  color: #999;
}

/* 变量定义 */
:root {
  --primary-color: #409eff;
  --text-primary: #333;
  --text-secondary: #666;
  --border-color: #ddd;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .query-form {
    grid-template-columns: 1fr;
  }
  
  .data-table {
    font-size: 12px;
  }
  
  .data-table th,
  .data-table td {
    padding: 8px;
  }
  
  .pagination {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .pagination-controls {
    width: 100%;
    justify-content: center;
  }
}
</style>
