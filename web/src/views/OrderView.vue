<template>
  <div class="order-view">
    <h2>订单管理</h2>
    
    <!-- 操作按钮 -->
    <div class="action-buttons">
      <button @click="toggleAddForm">
        {{ showAddForm ? '取消添加' : '添加订单' }}
      </button>
    </div>
    
    <!-- 添加订单表单 -->
    <div class="form-container" v-if="showAddForm">
      <form @submit.prevent="createOrder">
        <div class="form-group">
          <label for="orderNumber">订单编号：</label>
          <input 
            id="orderNumber" 
            v-model="newOrder.orderNumber" 
            type="text" 
            required
            placeholder="请输入订单编号"
          >
        </div>
        
        <div class="form-group">
          <label for="productName">产品名称：</label>
          <input 
            id="productName" 
            v-model="newOrder.productName" 
            type="text" 
            required
            placeholder="请输入产品名称"
          >
        </div>
        <div class="form-group">
          <label for="quantity">订单数量：</label>
          <input 
            id="quantity" 
            v-model.number="newOrder.quantity" 
            type="number" 
            min="1"
            required
            placeholder="请输入订单数量"
          >
        </div>
        <div class="form-group">
          <label for="dueDate">交货日期：</label>
          <input 
            id="dueDate" 
            v-model="newOrder.dueDate" 
            type="date" 
            required
          >
        </div>
        <div class="form-group">
          <label for="priority">优先级：</label>
          <select id="priority" v-model.number="newOrder.priority">
            <option v-for="n in 10" :key="n" :value="n">{{ n }}</option>
          </select>
        </div>
        <div class="form-group">
          <label for="status">订单状态：</label>
          <select id="status" v-model="newOrder.status">
            <option value="PENDING">待处理</option>
            <option value="PROCESSING">处理中</option>
            <option value="COMPLETED">已完成</option>
            <option value="CANCELLED">已取消</option>
          </select>
        </div>
        <div class="form-actions">
          <button type="submit" class="submit-btn">保存</button>
          <button type="button" @click="toggleAddForm" class="cancel-btn">取消</button>
        </div>
      </form>
    </div>
    
    <!-- 编辑订单表单 -->
    <div class="form-container" v-if="editingOrder">
      <form @submit.prevent="updateOrder">
        <div class="form-group">
          <label>ID：</label>
          <span>{{ editingOrder.id }}</span>
        </div>
        <div class="form-group">
          <label for="editOrderNumber">订单编号：</label>
          <input 
            id="editOrderNumber" 
            v-model="editingOrder.orderNumber" 
            type="text" 
            required
            placeholder="请输入订单编号"
          >
        </div>
        <div class="form-group">
          <label for="editProductName">产品名称：</label>
          <input 
            id="editProductName" 
            v-model="editingOrder.productName" 
            type="text" 
            required
            placeholder="请输入产品名称"
          >
        </div>
        <div class="form-group">
          <label for="editQuantity">订单数量：</label>
          <input 
            id="editQuantity" 
            v-model.number="editingOrder.quantity" 
            type="number" 
            min="1"
            required
            placeholder="请输入订单数量"
          >
        </div>
        <div class="form-group">
          <label for="editDueDate">交货日期：</label>
          <input 
            id="editDueDate" 
            v-model="editingOrder.dueDate" 
            type="date" 
            required
          >
        </div>
        <div class="form-group">
          <label for="editPriority">优先级：</label>
          <select id="editPriority" v-model.number="editingOrder.priority">
            <option v-for="n in 10" :key="n" :value="n">{{ n }}</option>
          </select>
        </div>
        <div class="form-group">
          <label for="editStatus">订单状态：</label>
          <select id="editStatus" v-model="editingOrder.status">
            <option value="PENDING">待处理</option>
            <option value="PROCESSING">处理中</option>
            <option value="COMPLETED">已完成</option>
            <option value="CANCELLED">已取消</option>
          </select>
        </div>
        <div class="form-actions">
          <button type="submit" class="submit-btn">更新</button>
          <button type="button" @click="editingOrder = null" class="cancel-btn">取消</button>
        </div>
      </form>
    </div>
    
    <!-- 订单列表 -->
    <div class="table-container">
      <table>
        <thead>
          <tr>
            <th>ID</th>
            <th>订单编号</th>
            <th>产品名称</th>
            <th>数量</th>
            <th>交货日期</th>
            <th>优先级</th>
            <th>状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="order in orders" :key="order.id">
            <td>{{ order.id }}</td>
            <td>{{ order.orderNumber }}</td>
            <td>{{ order.productName }}</td>
            <td>{{ order.quantity }}</td>
            <td>{{ formatDate(order.dueDate) }}</td>
            <td>{{ order.priority }}</td>
            <td :class="getStatusClass(order.status)">{{ getStatusText(order.status) }}</td>
            <td>
              <button @click="editOrder(order)">编辑</button>
              <button @click="deleteOrder(order.id)" class="delete-btn">删除</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
    
    <div class="loading" v-if="isLoading">
      <p>加载中...</p>
    </div>
    
    <div class="message" v-if="message">
      <p :class="messageType">{{ message }}</p>
    </div>
  </div>
</template>

<script>
import { factorySchedulingAPI } from '../api/service'

export default {
  name: 'OrderView',
  data() {
    return {
      orders: [],
      showAddForm: false,
      isLoading: false,
      message: '',
      messageType: 'success',
      newOrder: {
        orderNumber: '',
        productName: '',
        quantity: 1,
        dueDate: '',
        priority: 5,
        status: 'PENDING'
      },
      editingOrder: null
    }
  },
  mounted() {
    this.getAllOrders()
  },
  methods: {
    async getAllOrders() {
      this.isLoading = true
      try {
        const response = await factorySchedulingAPI.orders.getAll()
        this.orders = response.data || []
      } catch (error) {
        this.showMessage('获取订单列表失败', 'error', error)
      } finally {
        this.isLoading = false
      }
    },
    
    toggleAddForm() {
      this.showAddForm = !this.showAddForm
      if (this.showAddForm) {
        this.resetNewOrder()
      }
    },
    
    resetNewOrder() {
      this.newOrder = {
        orderNumber: '',
        productName: '',
        quantity: 1,
        dueDate: '',
        priority: 5,
        status: 'PENDING'
      }
    },
    
    async createOrder() {
      if (!this.validateOrderForm(this.newOrder)) {
        return
      }
      
      this.isLoading = true
      try {
        const response = await factorySchedulingAPI.orders.create(this.newOrder)
        this.orders.push(response.data)
        this.showMessage('订单创建成功')
        this.toggleAddForm()
      } catch (error) {
        this.showMessage('订单创建失败', 'error', error)
      } finally {
        this.isLoading = false
      }
    },
    
    async updateOrder() {
      if (!this.validateOrderForm(this.editingOrder)) {
        return
      }
      
      this.isLoading = true
      try {
        const response = await factorySchedulingAPI.orders.update(this.editingOrder.id, this.editingOrder)
        const index = this.orders.findIndex(o => o.id === this.editingOrder.id)
        if (index !== -1) {
          this.orders[index] = response.data
        }
        this.showMessage('订单更新成功')
        this.editingOrder = null
      } catch (error) {
        this.showMessage('订单更新失败', 'error', error)
      } finally {
        this.isLoading = false
      }
    },
    
    async deleteOrder(id) {
      if (!confirm('确定要删除该订单吗？')) {
        return
      }
      
      this.isLoading = true
      try {
        await factorySchedulingAPI.orders.delete(id)
        this.orders = this.orders.filter(o => o.id !== id)
        this.showMessage('订单删除成功')
      } catch (error) {
        this.showMessage('订单删除失败', 'error', error)
      } finally {
        this.isLoading = false
      }
    },
    
    editOrder(order) {
      this.editingOrder = { ...order }
      this.showAddForm = false
    },
    
    validateOrderForm(order) {
      if (!order.orderNumber || !order.orderNumber.trim()) {
        this.showMessage('订单编号不能为空', 'error')
        return false
      }
      
      if (!order.productName || !order.productName.trim()) {
        this.showMessage('产品名称不能为空', 'error')
        return false
      }
      
      if (!order.quantity || order.quantity <= 0) {
        this.showMessage('订单数量必须大于0', 'error')
        return false
      }
      
      if (!order.dueDate) {
        this.showMessage('交货日期不能为空', 'error')
        return false
      }
      
      return true
    },
    
    showMessage(message, type = 'success', error = null) {
      this.message = message
      this.messageType = type
      
      if (error && process.env.NODE_ENV !== 'production') {
        console.error('Error details:', error)
      }
      
      setTimeout(() => {
        this.message = ''
      }, 3000)
    },
    
    formatDate(dateString) {
      if (!dateString) return ''
      const date = new Date(dateString)
      return date.toLocaleDateString('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit'
      })
    },
    
    getStatusClass(status) {
      switch(status) {
        case 'PENDING': return 'status-pending'
        case 'PROCESSING': return 'status-processing'
        case 'COMPLETED': return 'status-completed'
        case 'CANCELLED': return 'status-cancelled'
        default: return ''
      }
    },
    
    getStatusText(status) {
      const statusMap = {
        'PENDING': '待处理',
        'PROCESSING': '处理中',
        'COMPLETED': '已完成',
        'CANCELLED': '已取消'
      }
      return statusMap[status] || status
    }
  }
}
</script>

<style scoped>
.order-view {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

h2 {
  color: #333;
  margin-bottom: 20px;
  font-size: 24px;
}

.action-buttons {
  margin-bottom: 20px;
}

.action-buttons button {
  padding: 10px 20px;
  background-color: #409eff;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 16px;
}

.action-buttons button:hover {
  background-color: #66b1ff;
}

.form-container {
  background-color: #f5f7fa;
  padding: 20px;
  border-radius: 8px;
  margin-bottom: 20px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.form-group {
  margin-bottom: 15px;
}

.form-group label {
  display: inline-block;
  width: 100px;
  font-weight: bold;
  color: #606266;
}

.form-group input,
.form-group select {
  padding: 8px 12px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  width: calc(100% - 120px);
  font-size: 14px;
}

.form-group input:focus,
.form-group select:focus {
  outline: none;
  border-color: #409eff;
}

.form-actions {
  margin-top: 20px;
  text-align: right;
}

.submit-btn {
  padding: 8px 20px;
  background-color: #409eff;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  margin-right: 10px;
}

.submit-btn:hover {
  background-color: #66b1ff;
}

.cancel-btn {
  padding: 8px 20px;
  background-color: #909399;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.cancel-btn:hover {
  background-color: #a6a9ad;
}

.table-container {
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

table {
  width: 100%;
  border-collapse: collapse;
}

th, td {
  padding: 12px 15px;
  text-align: left;
  border-bottom: 1px solid #ebeef5;
}

th {
  background-color: #f5f7fa;
  color: #606266;
  font-weight: bold;
  white-space: nowrap;
}

td {
  color: #303133;
}

tr:hover {
  background-color: #f5f7fa;
}

td button {
  padding: 4px 12px;
  margin-right: 8px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}

.delete-btn {
  background-color: #f56c6c;
  color: white;
}

.delete-btn:hover {
  background-color: #f78989;
}

.loading {
  text-align: center;
  padding: 40px;
  color: #909399;
}

.message {
  position: fixed;
  top: 20px;
  right: 20px;
  padding: 10px 20px;
  border-radius: 4px;
  z-index: 1000;
}

.message p {
  margin: 0;
  padding: 5px 0;
}

.success {
  background-color: #f0f9eb;
  color: #67c23a;
  border: 1px solid #e1f3d8;
}

.error {
  background-color: #fef0f0;
  color: #f56c6c;
  border: 1px solid #fde2e2;
}

.status-pending {
  color: #e6a23c;
}

.status-processing {
  color: #409eff;
}

.status-completed {
  color: #67c23a;
}

.status-cancelled {
  color: #909399;
}
</style>