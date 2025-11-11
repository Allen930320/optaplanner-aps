<template>
  <div class="scheduling-container">
    <h1>工厂调度管理</h1>
    
    <div class="scheduling-controls">
      <div class="input-group">
        <label for="problemId">问题ID:</label>
        <input type="number" id="problemId" v-model="problemId" placeholder="请输入问题ID">
      </div>
      
      <div class="input-group">
        <label for="orderNos">订单编号列表:</label>
        <textarea id="orderNos" v-model="orderNos" placeholder="订单编号，用逗号分隔" rows="3"></textarea>
      </div>
      
      <div class="button-group">
        <button @click="startScheduling" :disabled="isLoading">启动调度</button>
        <button @click="stopScheduling" :disabled="isLoading">停止调度</button>
        <button @click="getStatus" :disabled="isLoading">获取状态</button>
        <button @click="getSolution" :disabled="isLoading">获取方案</button>
        <button @click="getScore" :disabled="isLoading">获取评分</button>
        <button @click="checkFeasible" :disabled="isLoading">检查可行性</button>
      </div>
    </div>
    
    <div class="scheduling-result" v-if="result">
      <h3>操作结果</h3>
      <pre>{{ JSON.stringify(result, null, 2) }}</pre>
    </div>
    
    <div class="scheduling-status" v-if="status">
      <h3>调度状态</h3>
      <p>当前状态: {{ status }}</p>
    </div>
    
    <div class="loading" v-if="isLoading">
      <p>处理中...</p>
    </div>
  </div>
</template>

<script>
import { factorySchedulingAPI } from '../api/service'

export default {
  name: 'SchedulingView',
  data() {
    return {
      problemId: '',
      orderNos: '',
      result: null,
      status: null,
      isLoading: false
    }
  },
  methods: {
    async startScheduling() {
      if (!this.validateInput()) return
      
      this.isLoading = true
      try {
        const orderList = this.orderNos.split(',').map(no => no.trim()).filter(no => no)
        const response = await factorySchedulingAPI.scheduling.solve(this.problemId, orderList)
        this.result = response
        this.showMessage('调度启动成功')
      } catch (error) {
        this.showError('调度启动失败', error)
      } finally {
        this.isLoading = false
      }
    },
    
    async stopScheduling() {
      if (!this.problemId) {
        alert('请输入问题ID')
        return
      }
      
      this.isLoading = true
      try {
        const response = await factorySchedulingAPI.scheduling.stop(this.problemId)
        this.result = response
        this.showMessage('调度停止成功')
      } catch (error) {
        this.showError('调度停止失败', error)
      } finally {
        this.isLoading = false
      }
    },
    
    async getStatus() {
      if (!this.problemId) {
        alert('请输入问题ID')
        return
      }
      
      this.isLoading = true
      try {
        const response = await factorySchedulingAPI.scheduling.status(this.problemId)
        this.status = response.data
        this.result = response
      } catch (error) {
        this.showError('获取状态失败', error)
      } finally {
        this.isLoading = false
      }
    },
    
    async getSolution() {
      if (!this.problemId) {
        alert('请输入问题ID')
        return
      }
      
      this.isLoading = true
      try {
        const response = await factorySchedulingAPI.scheduling.solution(this.problemId)
        this.result = response
      } catch (error) {
        this.showError('获取方案失败', error)
      } finally {
        this.isLoading = false
      }
    },
    
    async getScore() {
      if (!this.problemId) {
        alert('请输入问题ID')
        return
      }
      
      this.isLoading = true
      try {
        const response = await factorySchedulingAPI.scheduling.score(this.problemId)
        this.result = response
      } catch (error) {
        this.showError('获取评分失败', error)
      } finally {
        this.isLoading = false
      }
    },
    
    async checkFeasible() {
      if (!this.problemId) {
        alert('请输入问题ID')
        return
      }
      
      this.isLoading = true
      try {
        const response = await factorySchedulingAPI.scheduling.feasible(this.problemId)
        this.result = response
        this.showMessage(response.data ? '方案可行' : '方案不可行')
      } catch (error) {
        this.showError('检查可行性失败', error)
      } finally {
        this.isLoading = false
      }
    },
    
    validateInput() {
      if (!this.problemId) {
        alert('请输入问题ID')
        return false
      }
      if (!this.orderNos.trim()) {
        alert('请输入订单编号列表')
        return false
      }
      return true
    },
    
    showMessage(message) {
      // 简单的消息提示，实际项目中可以使用Element UI等组件库
      alert(message)
    },
    
    showError(message, error) {
      console.error(error)
      alert(`${message}: ${error.message || '未知错误'}`)
    }
  }
}
</script>

<style scoped>
.scheduling-container {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

h1 {
  color: #333;
  margin-bottom: 30px;
}

.scheduling-controls {
  background-color: #f5f5f5;
  padding: 20px;
  border-radius: 8px;
  margin-bottom: 20px;
}

.input-group {
  margin-bottom: 15px;
}

label {
  display: block;
  margin-bottom: 5px;
  font-weight: bold;
}

input[type="number"],
textarea {
  width: 100%;
  padding: 8px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
}

.button-group {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

button {
  padding: 10px 20px;
  background-color: #409eff;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}

button:hover:not(:disabled) {
  background-color: #66b1ff;
}

button:disabled {
  background-color: #ccc;
  cursor: not-allowed;
}

.scheduling-result,
.scheduling-status {
  margin-top: 20px;
  padding: 20px;
  background-color: #f9f9f9;
  border-radius: 8px;
  border: 1px solid #e0e0e0;
}

pre {
  white-space: pre-wrap;
  word-break: break-all;
  font-family: 'Courier New', monospace;
  background-color: #f0f0f0;
  padding: 10px;
  border-radius: 4px;
  max-height: 400px;
  overflow-y: auto;
}

.loading {
  text-align: center;
  padding: 20px;
  font-size: 16px;
  color: #409eff;
}
</style>