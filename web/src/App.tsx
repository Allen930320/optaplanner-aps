
import React from 'react';
import { Layout, Menu } from 'antd';
import { Routes, Route, Link, BrowserRouter } from 'react-router-dom';
import { HomeOutlined, FileTextOutlined, BarChartOutlined, SettingOutlined } from '@ant-design/icons';
import HomePage from './components/HomePage';
import OrderQueryPage from './components/OrderQueryPage';
import 'antd/dist/reset.css';
import './App.css';

const { Header, Content, Sider } = Layout;

// 菜单配置项
const menuItems = [
  {
    key: '1',
    icon: <HomeOutlined />,
    label: <Link to="/">首页</Link>,
  },
  {
    key: '2',
    icon: <FileTextOutlined />,
    label: <Link to="/order-query">订单查询</Link>,
  },
  {
    key: '3',
    icon: <BarChartOutlined />,
    label: <Link to="/reports">数据报表</Link>,
  },
  {
    key: '4',
    icon: <SettingOutlined />,
    label: <Link to="/settings">系统设置</Link>,
  },
];

// 应用内部布局组件
const AppContent = () => {
  return (
    <Layout className="app-layout">
      <Header className="header">
        <div className="logo">生产计划管理系统</div>
      </Header>
      <Layout>
        <Sider width={200} className="sider" theme="light">
          <Menu
            mode="inline"
            items={menuItems}
            defaultSelectedKeys={['1']}
            className="menu"
          />
        </Sider>
        <Layout className="content-wrapper">
          <Content className="content">
            <Routes>
              <Route path="/" element={<HomePage />} />
              <Route path="/order-query" element={<OrderQueryPage />} />
              {/* 其他路由可以在这里添加 */}
            </Routes>
          </Content>
        </Layout>
      </Layout>
    </Layout>
  );
};

function App() {
  return (
    <BrowserRouter>
      <AppContent />
    </BrowserRouter>
  );
}

export default App
