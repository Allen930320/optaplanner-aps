<script setup>
import { ref, onMounted } from 'vue'
import { RouterView, useRoute } from 'vue-router'

const route = useRoute()
const isSidebarCollapsed = ref(false)
const isMobileMenuOpen = ref(false)

// 切换侧边栏展开/收起状态
const toggleSidebar = () => {
  isSidebarCollapsed.value = !isSidebarCollapsed.value
}

// 切换移动端菜单
const toggleMobileMenu = () => {
  isMobileMenuOpen.value = !isMobileMenuOpen.value
}

// 监听窗口大小变化
onMounted(() => {
  const handleResize = () => {
    if (window.innerWidth > 768) {
      isMobileMenuOpen.value = false
    }
  }
  
  window.addEventListener('resize', handleResize)
  handleResize() // 初始调用一次
  
  return () => {
    window.removeEventListener('resize', handleResize)
  }
})

// 菜单项定义
const menuItems = [
  {
    path: '/scheduling',
    name: 'scheduling',
    label: '调度管理',
    icon: '📋'
  },
  {
    path: '/machines',
    name: 'machines',
    label: '设备管理',
    icon: '⚙️'
  },
  {
    path: '/orders',
    name: 'orders',
    label: '订单管理',
    icon: '📦'
  },
  {
    path: '/order-tasks',
    name: 'orderTasks',
    label: '订单任务查询',
    icon: '🔍'
  }
]
</script>

<template>
  <div class="app-container">
    <!-- 移动端顶部导航栏 -->
    <header class="mobile-header" v-if="isMobileMenuOpen">
      <div class="mobile-header-content">
        <h2>工厂调度系统</h2>
        <button class="close-btn" @click="toggleMobileMenu">×</button>
      </div>
      <nav class="mobile-nav">
        <ul class="mobile-nav-list">
          <li 
            v-for="item in menuItems" 
            :key="item.path"
            :class="{ active: route.path === item.path }"
            @click="toggleMobileMenu"
          >
            <router-link :to="item.path">
              <span class="menu-icon">{{ item.icon }}</span>
              <span class="menu-label">{{ item.label }}</span>
            </router-link>
          </li>
        </ul>
      </nav>
    </header>

    <!-- 主布局容器 -->
    <div class="main-layout">
      <!-- 左侧侧边栏 -->
      <aside 
        class="app-sidebar" 
        :class="{ 
          'collapsed': isSidebarCollapsed,
          'mobile-open': isMobileMenuOpen && window.innerWidth <= 768
        }"
      >
        <!-- 侧边栏头部 -->
        <div class="sidebar-header">
          <div class="logo" :class="{ 'collapsed': isSidebarCollapsed }">
            <h1 v-if="!isSidebarCollapsed">工厂调度系统</h1>
            <div class="logo-icon" v-else>🎯</div>
          </div>
          <button class="toggle-btn" @click="toggleSidebar">
            {{ isSidebarCollapsed ? '→' : '←' }}
          </button>
        </div>
        
        <!-- 侧边栏菜单 -->
        <nav class="sidebar-nav">
          <ul class="nav-list">
            <li 
              v-for="item in menuItems" 
              :key="item.path"
              :class="{ active: route.path === item.path }"
            >
              <router-link :to="item.path">
                <span class="menu-icon">{{ item.icon }}</span>
                <span class="menu-label" :class="{ 'hidden': isSidebarCollapsed }">
                  {{ item.label }}
                </span>
              </router-link>
            </li>
          </ul>
        </nav>
      </aside>
      
      <!-- 主内容区域 -->
      <div class="content-wrapper">
        <!-- 顶部信息栏 -->
        <div class="top-bar">
          <button class="mobile-menu-btn" @click="toggleMobileMenu">☰</button>
          <div class="current-page" v-if="!isSidebarCollapsed">
            {{ menuItems.find(item => item.path === route.path)?.label || '系统首页' }}
          </div>
        </div>
        
        <!-- 页面内容 -->
        <main class="app-main">
          <RouterView />
        </main>
        
        <!-- 底部信息 -->
        <footer class="app-footer">
          <p>© 2024 工厂调度系统 - 基于OptaPlanner实现的高级排程系统</p>
        </footer>
      </div>
    </div>
  </div>
</template>

<style>
/* 全局样式重置 */
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, sans-serif;
  background-color: #f5f7fa;
  color: #303133;
  overflow-x: hidden;
}

/* 应用容器 */
.app-container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

/* 主布局容器 */
.main-layout {
  display: flex;
  height: 100vh;
  width: 100%;
}

/* 侧边栏样式 */
.app-sidebar {
  width: 240px;
  background-color: #1f2937;
  color: white;
  box-shadow: 2px 0 6px rgba(0, 0, 0, 0.1);
  transition: all 0.3s ease;
  position: fixed;
  height: 100vh;
  top: 0;
  left: 0;
  z-index: 1000;
  display: flex;
  flex-direction: column;
}

/* 侧边栏收起状态 */
.app-sidebar.collapsed {
  width: 64px;
}

/* 侧边栏头部 */
.sidebar-header {
  display: flex;
  align-items: center;
  padding: 20px 16px;
  border-bottom: 1px solid #374151;
}

.logo h1 {
  font-size: 20px;
  font-weight: 600;
  margin: 0;
  white-space: nowrap;
}

.logo-icon {
  font-size: 24px;
  display: none;
}

.logo.collapsed .logo-icon {
  display: block;
}

.logo.collapsed h1 {
  display: none;
}

/* 侧边栏切换按钮 */
.toggle-btn {
  background: transparent;
  border: none;
  color: white;
  cursor: pointer;
  font-size: 18px;
  margin-left: auto;
  padding: 4px;
  border-radius: 4px;
  transition: background-color 0.3s;
}

.toggle-btn:hover {
  background-color: rgba(255, 255, 255, 0.1);
}

/* 侧边栏菜单 */
.sidebar-nav {
  flex: 1;
  padding: 20px 0;
  overflow-y: auto;
}

.nav-list {
  list-style: none;
  padding: 0;
}

.nav-list li {
  margin: 4px 8px;
  border-radius: 8px;
  transition: background-color 0.3s;
}

.nav-list li a {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  color: #e5e7eb;
  text-decoration: none;
  transition: color 0.3s;
}

.nav-list li:hover a {
  color: white;
}

.nav-list li.active {
  background-color: #3b82f6;
}

.nav-list li.active a {
  color: white;
}

/* 菜单项样式 */
.menu-icon {
  font-size: 18px;
  margin-right: 12px;
  min-width: 24px;
  text-align: center;
}

.menu-label {
  font-size: 15px;
  transition: opacity 0.3s;
}

.menu-label.hidden {
  display: none;
}

/* 内容包装器 */
.content-wrapper {
  flex: 1;
  margin-left: 240px;
  transition: margin-left 0.3s ease;
  display: flex;
  flex-direction: column;
  height: 100vh;
}

.app-sidebar.collapsed + .content-wrapper {
  margin-left: 64px;
}

/* 顶部信息栏 */
.top-bar {
  background-color: white;
  padding: 16px 24px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
  display: flex;
  align-items: center;
  gap: 16px;
}

.mobile-menu-btn {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  display: none;
}

.current-page {
  font-size: 18px;
  font-weight: 500;
  color: #1f2937;
}

/* 主要内容区域 */
.app-main {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
}

/* 底部信息 */
.app-footer {
  background-color: white;
  padding: 16px 24px;
  border-top: 1px solid #e5e7eb;
  color: #6b7280;
  text-align: center;
  font-size: 14px;
}

/* 移动端菜单 */
.mobile-header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  background-color: #1f2937;
  color: white;
  z-index: 2000;
  height: 100vh;
  overflow-y: auto;
}

.mobile-header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  border-bottom: 1px solid #374151;
}

.mobile-header-content h2 {
  margin: 0;
  font-size: 20px;
}

.close-btn {
  background: none;
  border: none;
  color: white;
  font-size: 28px;
  cursor: pointer;
  padding: 0;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.mobile-nav-list {
  list-style: none;
  padding: 20px;
}

.mobile-nav-list li {
  margin-bottom: 8px;
  border-radius: 8px;
  overflow: hidden;
}

.mobile-nav-list li a {
  display: flex;
  align-items: center;
  padding: 16px;
  color: white;
  text-decoration: none;
  font-size: 16px;
}

.mobile-nav-list li.active {
  background-color: #3b82f6;
}

.mobile-nav-list .menu-icon {
  font-size: 20px;
  margin-right: 16px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .app-sidebar {
    transform: translateX(-100%);
  }
  
  .app-sidebar.mobile-open {
    transform: translateX(0);
  }
  
  .content-wrapper {
    margin-left: 0;
  }
  
  .app-sidebar.collapsed + .content-wrapper {
    margin-left: 0;
  }
  
  .mobile-menu-btn {
    display: block;
  }
  
  .top-bar {
    padding: 12px 16px;
  }
  
  .current-page {
    font-size: 16px;
  }
  
  .app-main {
    padding: 16px;
  }
}

/* 滚动条样式 */
.sidebar-nav::-webkit-scrollbar {
  width: 6px;
}

.sidebar-nav::-webkit-scrollbar-track {
  background: transparent;
}

.sidebar-nav::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.2);
  border-radius: 3px;
}

.sidebar-nav::-webkit-scrollbar-thumb:hover {
  background: rgba(255, 255, 255, 0.3);
}
</style>
