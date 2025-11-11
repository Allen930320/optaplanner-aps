import axios from 'axios'

// 创建axios实例
const apiClient = axios.create({
  baseURL: 'http://localhost:8081',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器
apiClient.interceptors.request.use(
  config => {
    // 可以在这里添加token等认证信息
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

// 响应拦截器
apiClient.interceptors.response.use(
  response => {
    return response.data
  },
  error => {
    console.error('API请求错误:', error)
    return Promise.reject(error)
  }
)

// API接口封装
export const factorySchedulingAPI = {
  // 调度管理接口
  scheduling: {
    solve: (problemId, orderNos) => apiClient.post(`/api/scheduling/solve/${problemId}`, orderNos),
    stop: (problemId) => apiClient.post(`/api/scheduling/stop/${problemId}`),
    solution: (problemId) => apiClient.get(`/api/scheduling/solution/${problemId}`),
    score: (problemId) => apiClient.get(`/api/scheduling/score/${problemId}`),
    status: (problemId) => apiClient.get(`/api/scheduling/status/${problemId}`),
    feasible: (problemId) => apiClient.get(`/api/scheduling/feasible/${problemId}`),
    update: (problemId, solution) => apiClient.put(`/api/scheduling/update/${problemId}`, solution),
    explain: (problemId) => apiClient.get(`/api/scheduling/explain/${problemId}`),
    updateTimeslot: (procedureRequest) => apiClient.post('/api/scheduling/update', procedureRequest),
    deleteAll: () => apiClient.get('/api/scheduling/deleteAll'),
    validation: (solution) => apiClient.post('/api/scheduling/validation', solution),
    receiveTimeslot: (timeslots) => apiClient.post('/api/scheduling/receiveTimeslot', timeslots)
  },
  
  // 设备管理接口
  machines: {
    getAll: () => apiClient.get('/api/machines'),
    getById: (id) => apiClient.get(`/api/machines/${id}`),
    create: (workCenters) => apiClient.post('/api/machines', workCenters),
    update: (id, workCenter) => apiClient.put(`/api/machines/${id}`, workCenter),
    delete: (id) => apiClient.delete(`/api/machines/${id}`)
  },
  
  // 订单管理接口
  orders: {
    getAll: () => apiClient.get('/api/orders'),
    getById: (id) => apiClient.get(`/api/orders/${id}`),
    create: (orders) => apiClient.post('/api/orders', orders),
    delete: (id) => apiClient.delete(`/api/orders/${id}`)
  },
  
  // MES订单管理接口
  mesOrders: {
    syncData: (orderNos) => apiClient.post('/api/mesOrders/syncData', orderNos),
    getOrderTasks: (params) => apiClient.get('/api/mesOrders/orderTasks', { params }),
    getOrderTasksWithPagination: (params) => {
      // 确保参数名与后端保持一致
      // 如果传入的是status，将其转换为statusList
      const adjustedParams = { ...params };
      if (adjustedParams.status && !adjustedParams.statusList) {
        adjustedParams.statusList = adjustedParams.status;
        delete adjustedParams.status;
      }
      
      return apiClient.get('/api/mesOrders/orderTasks/page', { params: adjustedParams })
    }
  },
  
  // 维护管理接口
  maintenance: {
    autoGenerate: () => apiClient.post('/api/maintenance/auto'),
    saveAll: (maintenances) => apiClient.post('/api/maintenance/all', maintenances),
    updateAll: (maintenances) => apiClient.post('/api/maintenance/updateAll', maintenances)
  },
  
  // 工序管理接口
  procedure: {
    create: (procedures) => apiClient.post('/api/procedure', procedures),
    createList: (procedures) => apiClient.post('/api/procedure/list', procedures)
  },
  
  // 工作日历管理接口
  workCalendar: {
    createAll: (request) => apiClient.post('/api/work-calendar/create-all', request)
  }
}

export default factorySchedulingAPI