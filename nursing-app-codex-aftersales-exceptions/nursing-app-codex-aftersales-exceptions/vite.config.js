import { defineConfig } from 'vite'
import uni from '@dcloudio/vite-plugin-uni'

export default defineConfig({
  plugins: [uni()],
  server: {
    host: '127.0.0.1',
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
  css: {
    preprocessorOptions: {
      scss: {
        // uni.scss 由 Uni-app 框架自动注入，不要重复 import
        additionalData: `
          $u-primary: #3A7BF7;
          $u-success: #00B8A9;
          $u-warning: #FF8A5C;
          $u-error: #FF5252;
          $u-info: #8E9DAE;
          // 页面级设计 Token：确保每个 Vue SFC 的 SCSS 都能访问
          $page-gradient: linear-gradient(180deg, #f8fbff 0%, #f6f8fc 42%, #f1f5fb 100%);
          $surface-gradient: linear-gradient(145deg, rgba(255,255,255,0.86), rgba(247,250,255,0.68));
          @import "uview-plus/theme.scss";
        `,
      },
    },
  },
})
