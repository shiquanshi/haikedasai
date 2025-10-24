import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: [
      { find: '@', replacement: resolve(__dirname, 'src') },
      // 解决element-plus模块解析问题的别名配置
      { find: /^element-plus(\/(es|lib))?$/, replacement: 'element-plus' },
      { find: /^element-plus\/(es|lib)\/(.*)$/, replacement: 'element-plus/$2' }
    ]
  },
  define: {
    global: 'globalThis'
  },
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      '/share': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      '/minio': {
        target: 'https://nohavebug.preview.huawei-zeabur.cn',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/minio/, '')
      }
    }
  }
})