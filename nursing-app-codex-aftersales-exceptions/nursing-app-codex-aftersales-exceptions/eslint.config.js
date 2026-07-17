const js = require('@eslint/js')
const vue = require('eslint-plugin-vue')
const prettier = require('eslint-config-prettier')

module.exports = [
  // 忽略目录
  {
    ignores: [
      'dist/**',
      'node_modules/**',
      'unpackage/**',
      '*.min.js',
    ],
  },

  // 基础 ESLint 推荐规则
  js.configs.recommended,

  // Vue 3 推荐规则
  ...vue.configs['flat/recommended'],

  // Prettier（禁用与 Prettier 冲突的规则）
  prettier,

  // 项目自定义规则
  {
    languageOptions: {
      ecmaVersion: 'latest',
      sourceType: 'module',
      globals: {
        uni: 'readonly',
        getApp: 'readonly',
        getCurrentPages: 'readonly',
        // 浏览器 / Node 全局
        console: 'readonly',
        setTimeout: 'readonly',
        clearTimeout: 'readonly',
        setInterval: 'readonly',
        clearInterval: 'readonly',
        document: 'readonly',
        window: 'readonly',
        localStorage: 'readonly',
        sessionStorage: 'readonly',
        navigator: 'readonly',
        location: 'readonly',
        fetch: 'readonly',
        Promise: 'readonly',
        process: 'readonly',
        module: 'readonly',
        require: 'readonly',
        __dirname: 'readonly',
        __filename: 'readonly',
      },
    },
    rules: {
      'vue/multi-word-component-names': 'off',
      'vue/no-v-html': 'off',
      'no-console': process.env.NODE_ENV === 'production' ? 'warn' : 'off',
      'no-debugger': process.env.NODE_ENV === 'production' ? 'warn' : 'off',
      'no-unused-vars': ['warn', { argsIgnorePattern: '^_' }],
    },
  },
]
