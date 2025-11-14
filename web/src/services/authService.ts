import axios from 'axios';

// 创建axios实例
const apiClient = axios.create({
  baseURL: 'http://localhost:8082',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
  withCredentials: true // 允许跨域请求携带cookies
});

// 登录响应接口
export interface LoginResponse {
  success: boolean;
  message?: string;
  token?: string;
  user?: {
    username: string;
    name?: string;
    permissions?: string[];
  };
}

// 注册请求接口
export interface RegisterRequest {
  username: string;
  password: string;
  email: string;
  phone: string;
}

// 注册响应接口
export interface RegisterResponse {
  code: number;
  msg: string;
  data: string;
}

// 登录请求接口
export interface LoginRequest {
  username: string;
  password: string;
}

// 用户信息接口
export interface UserInfo {
  username: string;
  name?: string;
  permissions?: string[];
  isLoggedIn: boolean;
}

// 登录API函数
export const login = async (username: string, password: string): Promise<LoginResponse> => {
  try {
    // 调用真实API
    const response = await apiClient.post('/api/auth/login', { username, password });
    
    if (response.data.code === 200) {
      const userInfo: UserInfo = {
        username: response.data.data.username,
        name: response.data.data.username,
        permissions: ['user'],
        isLoggedIn: true
      };
      
      // 保存用户信息到localStorage
      localStorage.setItem('userInfo', JSON.stringify(userInfo));
      localStorage.setItem('isLoggedIn', 'true');
      localStorage.setItem('token', response.data.data.token);
      
      return {
        success: true,
        message: response.data.msg,
        token: response.data.data.token,
        user: userInfo
      };
    } else {
      return {
        success: false,
        message: response.data.msg || '登录失败'
      };
    }
  } catch (error) {
    console.error('登录错误:', error);
    return {
      success: false,
      message: '登录失败，请重试'
    };
  }
};

// 注册API函数
export const register = async (registerData: RegisterRequest): Promise<RegisterResponse> => {
  try {
    const response = await apiClient.post('/api/auth/register', registerData);
    return response.data;
  } catch (error) {
    console.error('注册错误:', error);
    throw new Error('注册失败，请重试');
  }
};

// 登出函数
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
  } catch (error) {
    console.error('登出错误:', error);
    // 即使API调用失败，也清除本地状态
    localStorage.removeItem('userInfo');
    localStorage.removeItem('isLoggedIn');
    localStorage.removeItem('token');
    window.location.href = '/login';
  }
};

// 获取当前用户信息
export const getCurrentUser = (): UserInfo | null => {
  try {
    const userInfoStr = localStorage.getItem('userInfo');
    if (userInfoStr) {
      return JSON.parse(userInfoStr);
    }
    return null;
  } catch (error) {
    console.error('获取用户信息错误:', error);
    return null;
  }
};

// 检查用户是否已登录
export const isLoggedIn = (): boolean => {
  try {
    const loggedIn = localStorage.getItem('isLoggedIn');
    return loggedIn === 'true';
  } catch (error) {
    console.error('检查登录状态错误:', error);
    return false;
  }
};
