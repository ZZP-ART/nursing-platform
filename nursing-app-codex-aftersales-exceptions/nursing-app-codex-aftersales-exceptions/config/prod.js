/**
 * 生产环境配置
 * 由 CI/CD 部署时写入实际域名
 */
export default {
  BASE_URL: 'https://api.nursing.com', // 部署时替换为实际域名
  ENV: 'prod',
}
