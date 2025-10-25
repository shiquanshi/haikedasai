import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src')
    }
  },
  server: {
    port: 3000,
    historyApiFallback: true,
    proxy: {
      // 处理已有/api前缀的请求
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      // 处理没有/api前缀的直接路径请求
      '/share': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        rewrite: (path) => `/api${path}`
      },
      '/minio': {
        target: 'https://nohavebug.preview.huawei-zeabur.cn',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/minio/, '')
      },
      '/question-bank': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        rewrite: (path) => `/api${path}`
      },
      '/user': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        rewrite: (path) => `/api${path}`
      },
      '/image': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        rewrite: (path) => `/api${path}`
      },
      '/battle': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        rewrite: (path) => `/api${path}`
      },
      '/study-record': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        rewrite: (path) => `/api${path}`
      },
      '/tts': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        rewrite: (path) => `/api${path}`
      },
      // WebSocket相关路径
      '/ws-battle': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        ws: true,
        rewrite: (path) => `/api${path}`
      }
    }
  }
})