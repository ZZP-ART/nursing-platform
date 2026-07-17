import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    host: '127.0.0.1',
    port: 5174,
    watch: {
      ignored: ['**/.codex-tmp/**'],
    },
    proxy: {
      '/backend': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/backend/, ''),
      },
    },
  },
  build: {
    outDir: 'dist',
  },
})
