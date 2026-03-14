# 校园乐跑本地联调版

这个仓库只保留本地开发和演示所需的内容：

- 微信小程序学生端 `miniapp/`
- Spring Boot 后端 `backend/`
- Vue 管理后台 `admin-web/`
- Docker 本地依赖环境 `docker-compose.yml`

项目默认使用账号密码登录，不包含真实微信授权登录和生产部署配置。

## 环境要求

- Docker Desktop 或 Docker Engine + Docker Compose
- 微信开发者工具

## 一键启动

```bash
git clone git@github.com:hu5426y/LGG.git
cd LGG
chmod +x scripts/*.sh
./scripts/dev-up.sh
```

启动完成后：

- 管理后台：`http://127.0.0.1:5173`
- 后端 Swagger：`http://127.0.0.1:8080/swagger-ui.html`
- 后端健康检查：`http://127.0.0.1:8080/actuator/health`

查看状态：

```bash
./scripts/dev-status.sh
```

停止环境：

```bash
./scripts/dev-down.sh
```

如果你想彻底清空旧数据再重启：

```bash
docker compose down -v
./scripts/dev-up.sh
```

## 默认账号

- 管理员：`admin / admin123`
- 学生：`20230001 / 123456`
- 学生：`20230002 / 123456`

这些账号来自开发种子数据，首次启动会自动导入。

## 小程序联调

1. 运行 `./scripts/dev-up.sh`，它会自动生成 `miniapp/config.js`
2. 用微信开发者工具导入 `miniapp/`
3. 使用上面的测试账号直接登录

说明：

- `miniapp/config.js` 是本地生成文件，不纳入 Git 管理
- 每台电脑都需要在本地重新生成自己的 `miniapp/config.js`，不会绑定到某一台机器
- 如果微信开发者工具运行在 Windows、后端运行在 WSL，可单独执行：

```bash
./scripts/configure-miniapp-api.sh
```

## 学生导入

管理后台支持 `.csv` 和 `.xlsx` 导入，推荐字段：

- `student_no`
- `display_name`
- `college`
- `class_name`
- `username`
- `password`
- `status`

模板文件见 [docs/student-import-template.csv](./docs/student-import-template.csv)。

更多本地说明见：

- [docs/01-本地联调说明.md](./docs/01-本地联调说明.md)
- [docs/02-账号与数据说明.md](./docs/02-账号与数据说明.md)

## 常见问题

### 1. 页面打不开

先执行：

```bash
./scripts/dev-status.sh
```

确认 `mysql`、`redis`、`backend`、`admin` 四个容器都处于运行状态。

### 2. 小程序提示网络不可达

检查 `miniapp/config.js` 指向的接口地址是否可访问。开发者工具和后端不在同一网络环境时，重新执行：

```bash
./scripts/configure-miniapp-api.sh
```

### 3. 想重新初始化数据库

执行：

```bash
docker compose down -v
./scripts/dev-up.sh
```

这会删除本地 MySQL 和 Redis 卷，并重新导入开发数据。
