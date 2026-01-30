import type {AxiosResponse} from 'axios';
import axios from 'axios';
import type {
    ApiResponse,
    LoginResponse,
    OrderTaskQueryParams,
    ProcedureQueryDTO,
    RegisterRequest,
    RegisterResponse,
    SpringDataPage,
    Task,
    UserInfo,
    WorkCenterDetail,
    WorkCenterMaintenance
} from './model';

// 创建axios实例
const apiClient = axios.create({
  baseURL: 'http://localhost:8080',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
  withCredentials: true // 允许跨域请求携带cookies
});

// 请求拦截器 - 添加JWT令牌
apiClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// 响应拦截器
apiClient.interceptors.response.use(
  (response: AxiosResponse) => {
    return response.data;
  },
  (error) => {

    // 如果返回401未授权，清除本地存储并跳转到登录页
    if (error.response?.status === 401) {
      localStorage.removeItem('userInfo');
      localStorage.removeItem('isLoggedIn');
      localStorage.removeItem('token');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

// 同步订单数据到MES系统
export const syncOrderData = async (taskNos: string[]): Promise<void> => {
  const response:ApiResponse<string> = await apiClient.post('/api/mesOrders/syncData', taskNos);
  // 检查响应状态，确保同步成功
  if (response.code !== 200) {
    throw new Error(`同步失败: ${response.msg || '未知错误'}`);
  }
};

// 调用OrderController.queryTasks接口获取任务数据
export const queryTasks = async (params?: OrderTaskQueryParams): Promise<ApiResponse<SpringDataPage<Task>>> => {
  // 构建查询参数对象
    const queryParams: Record<string, string | string[]> = {};
    
    // 添加基本参数
    queryParams.orderName = params?.orderName || '';
    queryParams.orderNo = params?.orderNo || '';
    queryParams.contractNum = params?.contractNum || '';
    queryParams.productCode = params?.productCode || '';
    queryParams.productName = params?.productName || '';
    queryParams.startTime = params?.startTime || '';
    queryParams.endTime = params?.endTime || '';
    queryParams.pageNum = (params?.pageNum || 1).toString();
    queryParams.pageSize = (params?.pageSize || 20).toString();
    
    // 处理statusList参数
    if (params?.statusList && params.statusList.length > 0) {
        // 将statusList设置为数组
        queryParams.statusList = params.statusList;
    }
  
  // 适配新的API返回格式：ApiResponse<SpringDataPage<Task>>
  const result: ApiResponse<SpringDataPage<Task>> = await apiClient.get('/api/mesOrders/orderTasks/page', {
    params: queryParams,
    // 自定义参数序列化器，处理重复的参数名
    paramsSerializer: function(params: Record<string, string | string[]>) {
      let result = '';
      Object.keys(params).forEach(key => {
        const value = params[key];
        if (Array.isArray(value)) {
          // 对于数组，为每个元素创建一个同名参数
          value.forEach(item => {
            result += `${key}=${encodeURIComponent(item)}&`;
          });
        } else {
          // 对于单个值，创建一个参数
          result += `${key}=${encodeURIComponent(value)}&`;
        }
      });
      // 移除末尾的&
      return result.slice(0, -1);
    }
  });
    if (!result || result.code !== 200) {
    throw new Error(`API调用失败: ${result?.msg || '未知错误'}`);
  }
  return result;
};

// 开始任务调度
export const startTasks = async (orderNos: string[]): Promise<string> => {
  // 检查订单编号列表是否为空
  if (!orderNos || orderNos.length === 0) {
    throw new Error('订单编号列表不能为空');
  }
  // 根据时间戳生成problemId
  const problemId = `${Date.now()}`;
  // 保存服务器响应（响应拦截器已经返回了response.data）
  const response:ApiResponse<string> = await apiClient.post(`/api/scheduling/solve/${problemId}`, orderNos);
  if (response.code === 200) {
    return response.data || '';
  } else {
    throw new Error(`API调用失败: ${response.msg || '未知错误'}`);
  }
};

// ====================================
// 认证相关API函数
// ====================================

// 登录API函数
export const login = async (username: string, password: string): Promise<LoginResponse> => {
  // 调用真实API
  const response:ApiResponse<LoginResponse> = await apiClient.post('/api/auth/login', { username, password });

  if (response.code === 200) {
    const userInfo: UserInfo = {
      username: response.data.username,
      name: response.data.username,
      permissions: ['user'],
      isLoggedIn: true
    };
    // 保存用户信息到localStorage
    localStorage.setItem('userInfo', JSON.stringify(userInfo));
    localStorage.setItem('isLoggedIn', 'true');
    localStorage.setItem('token', response.data.token);
    
    return response.data;
  } else {
    throw new Error(response.msg || '登录失败');
  }
};

// 注册API函数
export const register = async (registerData: RegisterRequest): Promise<RegisterResponse> => {
  try {
    const response:ApiResponse<RegisterResponse> = await apiClient.post('/api/auth/register', registerData);
    // 此时response已经是{code: number, msg: string, data: string}的格式
    return response.data;
  } catch {
    throw new Error('注册失败，请重试');
  }
};

// 登出API函数
export const logout = async (): Promise<void> => {
  try {
    // 调用真实API
    await apiClient.post('/api/auth/logout');
    // 清除本地存储的用户信息
    localStorage.removeItem('userInfo');
    localStorage.removeItem('isLoggedIn');
    localStorage.removeItem('token');
    
    // 刷新页面或跳转到登录页
    window.location.href = '/login';
  } catch {
    // 即使API调用失败，也清除本地状态
    localStorage.removeItem('userInfo');
    localStorage.removeItem('isLoggedIn');
    localStorage.removeItem('token');
    window.location.href = '/login';
  }
};

// 创建时间槽
export const createTimeslot = async (procedureId: string, time: number = 0.5, slice: number = 0): Promise<void> => {
  const params = new URLSearchParams();
  // 添加taskNo参数
  params.append('time', time !== null && time !== undefined ? time.toString() : '30');
  params.append('slice', slice !== null && slice !== undefined ? slice.toString() : '0');
  params.append('procedureId', procedureId);
  const response: ApiResponse<void> = await apiClient.post('/api/timeslot/create', params, {
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded'
    }
  });
  
  if (response.code !== 200) {
    throw new Error(`创建时间槽失败: ${response.msg || '未知错误'}`);
  }
};

// 获取时间槽列表
export const getTimeslotList = async () => {
    return await apiClient.get('/api/timeslot/list');
};

// 获取时间槽列表
export const getTimeslotByTaskNo = async (taskNo: string) => {
    return await apiClient.get(`/api/timeslot/${taskNo}/list`);
};

// 分页查询时间槽列表
export const queryTimeslots = async (params: {
  productName?: string;
  productCode?: string;
  contractNum?: string;
  startTime?: string;
  endTime?: string;
  taskNo?: string;
  pageNum?: number;
  pageSize?: number;
}): Promise<ApiResponse<SpringDataPage<any>>> => {
  // 构建查询参数
  const queryParams = {
    productName: params.productName || '',
    productCode: params.productCode || '',
    contractNum: params.contractNum || '',
    startTime: params.startTime || '',
    endTime: params.endTime || '',
    taskNo: params.taskNo || '',
    pageNum: params.pageNum || 1,
    pageSize: params.pageSize || 20
  };

  // 调用后端接口
  const result: ApiResponse<SpringDataPage<any>> = await apiClient.get('/api/timeslot/page', {
    params: queryParams
  });

  if (!result || result.code !== 200) {
    throw new Error(`API调用失败: ${result?.msg || '未知错误'}`);
  }
  return result;
};

// 分页查询生产人员专用时间槽列表
export const queryProductUserTimeslots = async (params: {
  productName?: string;
  productCode?: string;
  contractNum?: string;
  startTime?: string;
  endTime?: string;
  taskNo?: string;
  pageNum?: number;
  pageSize?: number;
}): Promise<ApiResponse<SpringDataPage<any>>> => {
  // 构建查询参数
  const queryParams = {
    productName: params.productName || '',
    productCode: params.productCode || '',
    contractNum: params.contractNum || '',
    startTime: params.startTime || '',
    endTime: params.endTime || '',
    taskNo: params.taskNo || '',
    pageNum: params.pageNum || 1,
    pageSize: params.pageSize || 20
  };

  // 调用后端接口
  const result: ApiResponse<SpringDataPage<any>> = await apiClient.get('/api/timeslot/pageOfProductUser', {
    params: queryParams
  });

  if (!result || result.code !== 200) {
    throw new Error(`API调用失败: ${result?.msg || '未知错误'}`);
  }
  return result;
};

// 分页查询工序列表（适配新接口 /api/mesOrders/procedure/page）
export const queryProcedures = async (params: {
  orderName?: string;
  taskNo?: string;
  contractNum?: string;
  productCode?: string;
  statusList?: string[];
  startDate?: string;
  endDate?: string;
  pageNum?: number;
  pageSize?: number;
}): Promise<ApiResponse<SpringDataPage<ProcedureQueryDTO>>> => {
  // 构建查询参数
  const queryParams = {
    orderName: params.orderName || '',
    taskNo: params.taskNo || '',
    contractNum: params.contractNum || '',
    productCode: params.productCode || '',
    startDate: params.startDate || '',
    endDate: params.endDate || '',
    pageNum: params.pageNum || 1,
    pageSize: params.pageSize || 20
  };
  
  // 处理statusList参数
    const paramsSerializer = function(params: Record<string, string | string[]>) {
        let result = '';
        Object.keys(params).forEach(key => {
            const value = params[key];
            if (Array.isArray(value)) {
                // 对于数组，为每个元素创建一个同名参数
                value.forEach(item => {
                    result += `${key}=${encodeURIComponent(item)}&`;
                });
            } else if (value) {
                // 对于单个值，创建一个参数
                result += `${key}=${encodeURIComponent(value)}&`;
            }
        });
        // 移除末尾的&
        return result.slice(0, -1);
    };
  
  // 调用后端接口
  const result: ApiResponse<SpringDataPage<ProcedureQueryDTO>> = await apiClient.get('/api/mesOrders/procedure/page', {
    params: {
      ...queryParams,
      statusList: params.statusList
    },
    paramsSerializer
  });
  
  if (!result || result.code !== 200) {
    throw new Error(`API调用失败: ${result?.msg || '未知错误'}`);
  }
  return result;
};

// 获取工作中心列表
export const getWorkCenterList = async (): Promise<WorkCenterDetail[]> => {
  const response: ApiResponse<WorkCenterDetail[]> = await apiClient.get('/api/mes_work_center/list');
  if (response.code !== 200) {
    throw new Error(`获取工作中心列表失败: ${response.msg || '未知错误'}`);
  }
  return response.data;
};

// 获取工作中心维护计划
export const getWorkCenterMaintenance = async (
  workCenterCode: string,
  startDate: string,
  endDate: string
): Promise<WorkCenterMaintenance[]> => {
  const params = {
    workCenterCode,
    startDate,
    endDate
  };
  const response: ApiResponse<WorkCenterMaintenance[]> = await apiClient.get('/api/mes_work_center/maintenance/list', { params });
  if (response.code !== 200) {
    throw new Error(`获取工作中心维护计划失败: ${response.msg || '未知错误'}`);
  }
  // 确保返回的是数组
  return Array.isArray(response.data) ? response.data : [];
};

// 更新工作中心维护计划
export const updateWorkCenterMaintenance = async (
  data: Partial<WorkCenterMaintenance>
): Promise<ApiResponse<void>> => {
  const response: ApiResponse<void> = await apiClient.post('/api/work-calendar/update', data);
  if (response.code !== 200) {
    throw new Error(`更新工作中心维护计划失败: ${response.msg || '未知错误'}`);
  }
  return response;
};

// 外协工序时间槽拆分
export const splitOutsourcingTimeslot = async (timeslotId: string, days: number): Promise<void> => {
  const response: ApiResponse<void> = await apiClient.post(`/api/timeslot/${timeslotId}/split`, null, {
    params: { days }
  });
  if (response.code !== 200) {
    throw new Error(`拆分时间槽失败: ${response.msg || '未知错误'}`);
  }
};
