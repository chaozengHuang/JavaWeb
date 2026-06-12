import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

export default defineConfig({
  plugins: [
    vue(),
    vueDevTools(),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    },
  },
  server: {
    proxy: {
      '/api': { target: 'http://localhost:8081', changeOrigin: true },
      '/post': {
        target: 'http://localhost:8081',
        changeOrigin: true,
        bypass: (req, res) => {
          if (req.headers.accept && req.headers.accept.includes('text/html')) {
            return '/index.html'
          }
        },
      },
      '/board': { target: 'http://localhost:8081', changeOrigin: true },
      '/comment': { target: 'http://localhost:8081', changeOrigin: true },
      '/admin': {
        target: 'http://localhost:8081',
        changeOrigin: true,
        bypass: (req, res) => {
          if (req.headers.accept && req.headers.accept.includes('text/html')) {
            return '/index.html'
          }
        },
      },
      '/uploads': { target: 'http://localhost:8081', changeOrigin: true },
      '/ws': { target: 'ws://localhost:8081', ws: true, changeOrigin: true },
    },
  },
})