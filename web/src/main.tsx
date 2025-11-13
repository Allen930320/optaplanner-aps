// 导入polyfill以支持Chrome 75等旧版浏览器
import 'core-js/stable';
import 'regenerator-runtime/runtime';

import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import App from './App.tsx'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
