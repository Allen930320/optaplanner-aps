import { createRouter, createWebHistory } from 'vue-router'
import SchedulingView from '../views/SchedulingView.vue'
import MachineView from '../views/MachineView.vue'
import OrderView from '../views/OrderView.vue'
import OrderTaskQueryView from '../views/OrderTaskQueryView.vue'

const routes = [
  {
    path: '/',
    name: 'home',
    redirect: '/scheduling'
  },
  {
    path: '/scheduling',
    name: 'scheduling',
    component: SchedulingView,
    meta: {
      title: '调度管理'
    }
  },
  {
    path: '/machines',
    name: 'machines',
    component: MachineView,
    meta: {
      title: '设备管理'
    }
  },
  {
    path: '/orders',
    name: 'orders',
    component: OrderView,
    meta: {
      title: '订单管理'
    }
  },
  {
    path: '/order-tasks',
    name: 'orderTasks',
    component: OrderTaskQueryView,
    meta: {
      title: '订单任务查询'
    }
  }
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes
})

// 全局前置守卫，用于设置页面标题
router.beforeEach((to, from, next) => {
  if (to.meta.title) {
    document.title = `工厂调度系统 - ${to.meta.title}`
  } else {
    document.title = '工厂调度系统'
  }
  next()
})

export default router