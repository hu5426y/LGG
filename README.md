# 校园乐跑单校 MVP

本项目面向单校内部上线场景，提供一套可真实交付的校园跑系统，包含：

- 微信小程序学生端 `miniapp/`
- Spring Boot 后端 `backend/`
- Vue 管理后台 `admin-web/`
- 部署与研发文档 `docs/`

当前版本的产品目标是：

- 学生端以微信登录为主，并与学号体系绑定
- 跑步数据基于真实 GPS 轨迹采集与校验
- 管理端支持学生名单导入、启停用、重置微信绑定
- 后端按 `dev / prod` 配置隔离，支持云托管 MySQL / Redis

## 目录结构

```text
LJJ/
├── admin-web/            # 运营后台
├── backend/              # Spring Boot API
├── docs/                 # 部署、说明、开发文档
├── miniapp/              # 微信小程序
├── docker-compose.yml    # 本地开发环境
├── scripts/              # 启动与配置脚本
└── .env.example          # 生产环境变量示例
```

## 技术栈

- 学生端：原生微信小程序
- 后端：Spring Boot 3.5、Spring Security、JPA、Flyway、Redis、MySQL
- 管理端：Vue 3、Vite、Element Plus
- 开发环境：Docker Compose
- 生产部署：容器镜像 + 托管 MySQL / Redis

## 本地开发启动

推荐先安装以下环境：

- Docker / Docker Compose
- JDK 17
- Node.js 20+
- 微信开发者工具

推荐直接启动完整开发环境：

```bash
git clone git@github.com:hu5426y/LGG.git
cd LGG
chmod +x scripts/*.sh
./scripts/dev-up.sh
```

查看状态：

```bash
./scripts/dev-status.sh
```

停止环境：

```bash
./scripts/dev-down.sh
```

默认开发端口：

- MySQL：`localhost:3307`
- Redis：`localhost:6380`
- Backend API：`http://127.0.0.1:8080`
- Actuator Health：`http://127.0.0.1:8080/actuator/health`
- Swagger：`http://127.0.0.1:8080/swagger-ui.html`
- Admin Web：`http://127.0.0.1:5173`

说明：

- `dev` 环境会自动加载 `db/devdata` 下的开发种子数据。
- `prod` 环境不会自动导入演示账号，也不会默认开放 Swagger。
- `./scripts/dev-up.sh` 会自动生成本地小程序配置文件 `miniapp/config.js`，该文件不纳入 Git 管理。

## 小程序接入

1. 用微信开发者工具导入 `miniapp/`
2. 首次 clone 后先执行 `./scripts/dev-up.sh` 或 `./scripts/configure-miniapp-api.sh` 生成 `miniapp/config.js`
3. 确认 `miniapp/config.js` 指向正确的后端地址
4. 如果开发者工具运行在 Windows、后端运行在 WSL，优先执行：

```bash
./scripts/configure-miniapp-api.sh
```

5. 在微信公众平台配置合法服务器域名
6. 为小程序后台补齐 `AppID / AppSecret`

当前登录流程：

- 学生先调用微信登录
- 若 `openid` 已绑定学生账号，则直接登录
- 若未绑定，则输入学号和密码完成首次绑定

## 学生导入

管理端支持 `.csv` 和 `.xlsx` 导入。推荐字段：

- `student_no`
- `display_name`
- `college`
- `class_name`
- `username`
- `password`
- `status`

可直接使用模板：

- [student-import-template.csv](./docs/student-import-template.csv)

说明：

- 新导入学生必须提供初始密码
- 已存在学生可通过再次导入进行更新
- `status` 仅支持 `ACTIVE` 或 `DISABLED`

## 生产部署

生产环境请使用环境变量注入配置，示例见：

- [`.env.example`](./.env.example)
- [`docs/04-部署说明.md`](./docs/04-部署说明.md)

核心生产配置包括：

- `SPRING_DATASOURCE_*`
- `SPRING_DATA_REDIS_*`
- `CAMPUSRUN_JWT_SECRET`
- `CAMPUSRUN_WECHAT_APP_ID`
- `CAMPUSRUN_WECHAT_APP_SECRET`
- `CAMPUSRUN_BOOTSTRAP_ADMIN_*`

建议：

- `SPRING_PROFILES_ACTIVE=prod`
- `CAMPUSRUN_SWAGGER_ENABLED=false`
- `CAMPUSRUN_RUN_ALLOW_SIMULATED_RUNS=false`

## 当前已实现能力

- 学生端：微信登录绑定、首页、真实跑步、动态、活动、个人中心
- 后端：JWT 鉴权、微信登录适配、跑步有效性校验、排行榜缓存、后台审核、审计日志、健康检查
- 管理端：登录、总览、学生导入、学生状态管理、微信绑定重置、内容审核、活动/教程/勋章维护、日志查看

## 文档入口

- [任务清单](./docs/01-任务清单.md)
- [开发文档](./docs/02-开发文档.md)
- [开发日志](./docs/03-开发日志.md)
- [部署说明](./docs/04-部署说明.md)
- [项目说明](./docs/05-项目说明.md)
