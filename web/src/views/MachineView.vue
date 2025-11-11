<template>
  <div class="machine-view">
    <h2>设备管理</h2>
    
    <!-- 操作按钮 -->
    <div class="action-buttons">
      <button @click="toggleAddForm">
        {{ showAddForm ? '取消添加' : '添加设备' }}
      </button>
    </div>
    
    <!-- 添加设备表单 -->
    <div class="form-container" v-if="showAddForm">
      <form @submit.prevent="createMachine">
        <div class="form-group">
          <label for="workCenterCode">工位代码：</label>
          <input 
            id="workCenterCode" 
            v-model="newMachine.workCenterCode" 
            type="text" 
            required
            placeholder="请输入工位代码"
          >
        </div>
        
        <div class="form-group">
          <label for="name">设备名称：</label>
          <input 
            id="name" 
            v-model="newMachine.name" 
            type="text" 
            required
            placeholder="请输入设备名称"
          >
        </div>
        
        <div class="form-group">
          <label for="status">设备状态：</label>
          <select id="status" v-model="newMachine.status">
            <option value="AVAILABLE">可用</option>
            <option value="MAINTENANCE">维护中</option>
            <option value="BROKEN">故障</option>
            <option value="OFFLINE">离线</option>
          </select>
        </div>
        
        <div class="form-actions">
          <button type="submit" class="submit-btn">保存</button>
          <button type="button" @click="toggleAddForm" class="cancel-btn">取消</button>
        </div>
      </form>
    </div>
    
    <!-- 编辑设备表单 -->
    <div class="form-container" v-if="editingMachine">
      <form @submit.prevent="updateMachine">
        <div class="form-group">
          <label>ID：</label>
          <span>{{ editingMachine.id }}</span>
        </div>
        
        <div class="form-group">
          <label for="editWorkCenterCode">工位代码：</label>
          <input 
            id="editWorkCenterCode" 
            v-model="editingMachine.workCenterCode" 
            type="text" 
            required
            placeholder="请输入工位代码"
          >
        </div>
        
        <div class="form-group">
          <label for="editName">设备名称：</label>
          <input 
            id="editName" 
            v-model="editingMachine.name" 
            type="text" 
            required
            placeholder="请输入设备名称"
          >
        </div>
        
        <div class="form-group">
          <label for="editStatus">设备状态：</label>
          <select id="editStatus" v-model="editingMachine.status">
            <option value="AVAILABLE">可用</option>
            <option value="MAINTENANCE">维护中</option>
            <option value="BROKEN">故障</option>
            <option value="OFFLINE">离线</option>
          </select>
        </div>
        
        <div class="form-actions">
          <button type="submit" class="submit-btn">更新</button>
          <button type="button" @click="editingMachine = null" class="cancel-btn">取消</button>
        </div>
      </form>
    </div>
    
    <!-- 设备列表 -->
    <div class="table-container">
      <table>
        <thead>
          <tr>
            <th>ID</th>
            <th>工位代码</th>
            <th>设备名称</th>
            <th>状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="machine in machines" :key="machine.id">
            <td>{{ machine.id }}</td>
            <td>{{ machine.workCenterCode }}</td>
            <td>{{ machine.name }}</td>
            <td :class="getStatusClass(machine.status)">{{ getStatusText(machine.status) }}</td>
            <td>
              <button @click="editMachine(machine)">编辑</button>
              <button @click="deleteMachine(machine.id)" class="delete-btn">删除</button>
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
  name: 'MachineView',
  data() {
    return {
      machines: [],
      showAddForm: false,
      isLoading: false,
      message: '',
      messageType: 'success',
      newMachine: {
        workCenterCode: '',
        name: '',
        status: 'AVAILABLE'
      },
      editingMachine: null
    }
  },
  mounted() {
    this.getAllMachines()
  },
  methods: {
    async getAllMachines() {
      this.isLoading = true
      try {
        const response = await factorySchedulingAPI.machines.getAll()
        this.machines = response.data || []
      } catch (error) {
        this.showMessage('获取设备列表失败', 'error', error)
      } finally {
        this.isLoading = false
      }
    },
    
    toggleAddForm() {
      this.showAddForm = !this.showAddForm
      if (this.showAddForm) {
        this.resetNewMachine()
      }
    },
    
    resetNewMachine() {
      this.newMachine = {
        workCenterCode: '',
        name: '',
        status: 'AVAILABLE'
      }
    },
    
    async createMachine() {
      if (!this.validateMachineForm(this.newMachine)) {
        return
      }
      
      this.isLoading = true
      try {
        const response = await factorySchedulingAPI.machines.create(this.newMachine)
        this.machines.push(response.data)
        this.showMessage('设备创建成功')
        this.toggleAddForm()
      } catch (error) {
        this.showMessage('设备创建失败', 'error', error)
      } finally {
        this.isLoading = false
      }
    },
    
    async updateMachine() {
      if (!this.validateMachineForm(this.editingMachine)) {
        return
      }
      
      this.isLoading = true
      try {
        const response = await factorySchedulingAPI.machines.update(this.editingMachine.id, this.editingMachine)
        const index = this.machines.findIndex(m => m.id === this.editingMachine.id)
        if (index !== -1) {
          this.machines[index] = response.data
        }
        this.showMessage('设备更新成功')
        this.editingMachine = null
      } catch (error) {
        this.showMessage('设备更新失败', 'error', error)
      } finally {
        this.isLoading = false
      }
    },
    
    async deleteMachine(id) {
      if (!confirm('确定要删除该设备吗？')) {
        return
      }
      
      this.isLoading = true
      try {
        await factorySchedulingAPI.machines.delete(id)
        this.machines = this.machines.filter(m => m.id !== id)
        this.showMessage('设备删除成功')
      } catch (error) {
        this.showMessage('设备删除失败', 'error', error)
      } finally {
        this.isLoading = false
      }
    },
    
    editMachine(machine) {
      this.editingMachine = { ...machine }
      this.showAddForm = false
    },
    
    validateMachineForm(machine) {
      if (!machine.workCenterCode || !machine.workCenterCode.trim()) {
        this.showMessage('工位代码不能为空', 'error')
        return false
      }
      
      if (!machine.name || !machine.name.trim()) {
        this.showMessage('设备名称不能为空', 'error')
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
    
    getStatusClass(status) {
      switch(status) {
        case 'AVAILABLE': return 'status-available'
        case 'MAINTENANCE': return 'status-maintenance'
        case 'BROKEN': return 'status-broken'
        case 'OFFLINE': return 'status-offline'
        default: return ''
      }
    },
    
    getStatusText(status) {
      const statusMap = {
        'AVAILABLE': '可用',
        'MAINTENANCE': '维护中',
        'BROKEN': '故障',
        'OFFLINE': '离线'
      }
      return statusMap[status] || status
    }
  }
}
</script>

<style scoped>
.machine-view {
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

.status-available {
  color: #67c23a;
}

.status-maintenance {
  color: #e6a23c;
}

.status-broken {
  color: #f56c6c;
}

.status-offline {
  color: #909399;
}
</style>