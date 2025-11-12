import axios from 'axios';
import type { AxiosResponse } from 'axios';

// 创建axios实例
const apiClient = axios.create({
  baseURL: 'http://localhost:8081',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// 响应拦截器
apiClient.interceptors.response.use(
  (response: AxiosResponse) => {
    return response.data;
  },
  (error) => {
    console.error('API请求错误:', error);
    return Promise.reject(error);
  }
);

// 订单数据类型定义 (兼容旧格式)
export interface Order {
  taskId: string;
  orderNo: string;
  erpStatus?: string;
  orderStatus?: string;
  planStartDate?: string;
  planEndDate?: string;
  factStartDate?: string;
  factEndDate?: string;
}

// 新的订单任务数据类型定义
export interface OrderTask {
  taskNo: string;
  orderNo: string;
  orderName: string;
  routeSeq: string | null;
  planQuantity: number;
  taskStatus: string;
  planStartDate: string;
  planEndDate: string;
  factStartDate: string | null;
  factEndDate: string | null;
  orderPlanQuantity: number;
  orderStatus: string;
  contractNum: string;
}

// 查询参数接口
export interface OrderTaskQueryParams {
  orderName?: string;
  startTime?: string;
  endTime?: string;
  statusList?: string[];
  pageNum?: number;
  pageSize?: number;
}

// API响应接口
export interface ApiResponse<T> {
  code: number;
  msg: string;
  data: T;
  total?: number;
  reqId: string;
}

// 新的分页响应接口
export interface PageResponse {
  total: number;
  records: OrderTask[];
  totalPages: number;
  pageSize: number;
  pageNum: number;
}

// Mock数据 - 订单任务列表
const mockOrderTasks: OrderTask[] = []

// 获取所有订单（保留旧方法以兼容）
export const getAllOrders = async (): Promise<ApiResponse<Order[]>> => {
  try {
    const response = await apiClient.get('/api/orders');
    return response as unknown as ApiResponse<Order[]>;
  } catch (error) {
    console.error('获取订单列表失败:', error);
    // 返回mock数据
    return {
      code: 200,
      msg: 'success',
      reqId: 'mock-' + Date.now(),
      data: []
    };
  }
};

// 根据条件分页查询订单任务
export const queryOrderTasksWithPagination = async (params: OrderTaskQueryParams): Promise<PageResponse> => {
  try {
    // 构建查询参数
    console.log('查询参数:', params);
    const queryParams = {
      orderName: params.orderName,
      startTime: params.startTime,
      endTime: params.endTime,
      statusList: params.statusList?.join(','),
      pageNum: params.pageNum || 1,
      pageSize: params.pageSize || 20
    };
    
    // 调用新的API接口
    const response = await apiClient.get('/api/mesOrders/orderTasks/page', { params: queryParams });
    return response as unknown as PageResponse;
  } catch (error) {
    console.error('分页查询订单任务失败:', error);
    
    // 返回mock数据
    const pageNum = params.pageNum || 1;
    const pageSize = params.pageSize || 20;
    const startIndex = (pageNum - 1) * pageSize;
    const endIndex = startIndex + pageSize;
    
    // 模拟搜索过滤
    let filteredTasks = [...mockOrderTasks];
    if (params.orderName) {
      filteredTasks = filteredTasks.filter(task => 
        task.orderName.includes(params.orderName!) || task.orderNo.includes(params.orderName!)
      );
    }
    
    // 模拟时间范围过滤
    if (params.startTime) {
      filteredTasks = filteredTasks.filter(task => task.planStartDate >= params.startTime!);
    }
    if (params.endTime) {
      filteredTasks = filteredTasks.filter(task => task.planEndDate <= params.endTime!);
    }
    
    // 模拟状态过滤
    if (params.statusList && params.statusList.length > 0) {
      filteredTasks = filteredTasks.filter(task => 
        params.statusList!.includes(task.taskStatus)
      );
    }
    
    // 模拟分页
    const paginatedTasks = filteredTasks.slice(startIndex, endIndex);
    const total = filteredTasks.length;
    const totalPages = Math.ceil(total / pageSize);
    
    return {
      total,
      records: paginatedTasks,
      totalPages,
      pageSize,
      pageNum
    };
  }
};

// 分页获取订单（保留旧方法但内部调用新方法）
export const getOrdersByPage = async (page: number = 1, pageSize: number = 10): Promise<ApiResponse<Order[]>> => {
  try {
    // 调用新的查询方法并转换格式
    const response = await queryOrderTasksWithPagination({ pageNum: page, pageSize });
    
    // 转换为旧的Order格式
    const convertedOrders: Order[] = response.records.map(task => ({
      taskId: task.taskNo,
      orderNo: task.orderNo,
      orderStatus: task.orderStatus,
      planStartDate: task.planStartDate,
      planEndDate: task.planEndDate,
      factStartDate: task.factStartDate || undefined,
      factEndDate: task.factEndDate || undefined
    }));
    
    return {
      code: 200,
      msg: 'success',
      reqId: 'mock-' + Date.now(),
      data: convertedOrders,
      total: response.total
    };
  } catch (error) {
    console.error('分页查询订单失败:', error);
    throw error;
  }
};

// 同步订单数据到MES系统
export const syncOrderData = async (orderNos: string[]): Promise<void> => {
  try {
    await apiClient.post('/api/mesOrders/syncData', orderNos);
    console.log('同步订单数据成功，订单编号:', orderNos);
  } catch (error) {
    console.error('同步订单数据失败:', error);
    throw error;
  }
};
