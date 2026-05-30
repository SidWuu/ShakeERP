# ShakeERP 金东进销存

适用于自家小型工厂的局域网 Web 进销存系统。部署在工厂内一台电脑上，PC 和手机通过浏览器访问同一地址。

## 技术栈

| 层 | 技术 |
|---|---|
| 后端 | Java 17 + Spring Boot 3 + Maven + spring-boot-devtools（热部署） |
| 前端 | Vue 3 + Vite 5 + Element Plus + Pinia + Vue Router |
| 数据库 | SQLite（`data/factory-erp.db`） |
| 扫码 | html5-qrcode（浏览器摄像头实时识别 1D/2D 条码） |
| 文件存储 | 本地 `uploads/` 目录 |

## 快速启动

```bash
./startup/start-all.sh          # HTTP 模式，一键启动前后端
./startup/start-backend.sh      # 单独启动后端 http://localhost:8080
./startup/start-frontend.sh     # 单独启动前端 http://localhost:5173
./startup/start-https.sh        # HTTPS 模式，手机扫码用 https://电脑IP:8443
```

## 项目结构

```
.
├── README.md                   # 项目说明
├── startup/                    # 启动脚本
│   ├── start-all.sh
│   ├── start-backend.sh
│   ├── start-frontend.sh
│   └── start-https.sh
├── backend/                    # Spring Boot 后端
├── frontend/                   # Vue 3 前端
├── data/                       # SQLite 数据库（git 忽略）
├── uploads/                    # 上传文件（git 忽略）
└── docs/                       # 文档
```

## 后端架构

```
backend/src/main/java/com/factory/erp/
├── FactoryErpApplication.java              # 启动入口
├── common/
│   ├── ApiResponse.java                    # 统一响应结构
│   ├── BaseService.java                    # Service 基类（编码生成、日期过滤、模糊匹配）
│   ├── config/
│   │   ├── WebConfig.java                  # CORS + 静态资源映射 + 鉴权拦截器注册
│   │   └── AuthInterceptor.java            # API 鉴权拦截器（校验 token）
│   └── exception/
│       ├── ApiExceptionHandler.java        # 全局异常处理器
│       ├── BusinessException.java          # 业务异常
│       └── NotFoundException.java          # 资源未找到异常
├── auth/                                   # 登录认证
├── customer/                               # 客户管理
├── product/                                # 商品管理
├── inventory/                              # 库存管理
├── scanner/                                # 扫码查询
├── stock/                                  # 入库/出库/台账
├── upload/                                 # 文件上传
└── admin/                                  # 系统管理（备份/恢复）
```

## 前端架构

```
frontend/src/
├── main.js                         # 入口
├── App.vue                         # 应用壳
├── api.js                          # API 调用
├── router/index.js                 # 路由
├── stores/app.js                   # Pinia 状态
├── utils/                          # 工具函数
├── components/                     # 公共组件（扫码、多图上传）
└── views/                          # 页面视图
```

## 已完成功能

- 登录认证（三角色 + 密码加密 + token 鉴权 + HTTPS）
- 商品管理（CRUD + 多图上传 + 重复条码提示）
- 库存管理（入库/出库/调整一体化 + 扫码 + 客户关联）
- 客户管理（CRUD + 查询）
- 库存台账（变动日志 + 客户列）
- 系统管理（备份/恢复/导出 + 每日自动备份）
- 摄像头实时扫码
- 一键启动脚本

## 后续扩展方向

- SQLite → MySQL
- 多用户并发
- 条码枪接入
- 报表与打印
- 细粒度按钮级权限控制
- 审批流、微信通知

## 相关文档

- [启动指南](docs/startup-guide.md)（本地查看，未纳入 git）
