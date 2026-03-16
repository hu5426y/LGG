# Windows 本机启动说明

这份文档适用于 `Windows 10/11` 本机开发，不依赖 `WSL` 和 `Docker`。目标是把下面三个部分跑通：

- Spring Boot 后端 `backend/`
- Vue 管理后台 `admin-web/`
- 微信小程序 `miniapp/`

## 1. 环境清单

必须安装这些环境：

1. `Git for Windows`
2. `JDK 17`
3. `Maven 3.9+`
4. `Node.js 20 LTS`
5. `MySQL 8.0`
6. `Redis 兼容服务`
7. `微信开发者工具`
8. `PowerShell 5.1+`

如果你不想手动一个个安装，仓库里已经补了自动引导脚本：

- [scripts/bootstrap-windows-env.ps1](/home/huge/dev/LJJ/scripts/bootstrap-windows-env.ps1)

这个脚本会优先用 `winget` 逐项检查并安装：

1. `Git for Windows`
2. `JDK 17`
3. `Apache Maven`
4. `Node.js LTS`
5. `MySQL Server`
6. `Memurai Developer`
7. 可选 `微信开发者工具`

然后继续做：

1. 修正 `JAVA_HOME`
2. 自动解压并配置 `Maven`
3. 自动初始化 `MySQL80` 服务、数据目录和 `root` 密码
4. 创建 `campus_run` 库和 `campus / campus123` 账号
5. 安装或启动 `Memurai` Windows 服务
6. 输出版本检查结果

项目当前要求来自这些配置文件：

- [backend/src/main/resources/application-dev.yml](/home/huge/dev/LJJ/backend/src/main/resources/application-dev.yml)
- [admin-web/vite.config.js](/home/huge/dev/LJJ/admin-web/vite.config.js)
- [miniapp/config.example.js](/home/huge/dev/LJJ/miniapp/config.example.js)

## 2. 安装前先检查 PowerShell 执行策略

先打开 `PowerShell`，执行：

```powershell
Get-ExecutionPolicy -List
```

如果 `CurrentUser` 是 `Restricted`，执行：

```powershell
Set-ExecutionPolicy -Scope CurrentUser RemoteSigned
```

如果你不想修改系统策略，也可以每次都这样运行脚本：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\bootstrap-windows-env.ps1
```

如果后面需要由脚本自动启动 `MySQL` 或 `Redis` Windows 服务，建议用“以管理员身份运行”的 PowerShell。

## 3. 先执行自动安装脚本

建议先用管理员权限打开 `PowerShell`，在项目根目录执行：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\bootstrap-windows-env.ps1
```

如果你还想顺手安装微信开发者工具：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\bootstrap-windows-env.ps1 -IncludeWeixinDevTools
```

如果你暂时只想装基础环境，不让脚本改 MySQL 或 Memurai：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\bootstrap-windows-env.ps1 -SkipMySqlConfiguration -SkipMemuraiConfiguration
```

说明：

- 脚本必须用管理员权限运行
- 脚本依赖 `winget`
- 脚本日志会写到 `scripts/.runtime/windows/bootstrap.log`
- 第一次配置 MySQL 时，脚本会要求你输入本地 `root` 密码，或者设置一个新的 `root` 密码

如果自动脚本跑完了，本节后面的手动安装内容可以只当排障手册看。

## 3.1 新电脑从零部署的推荐顺序

这部分是这次实测后整理出的“最短可落地流程”。新电脑第一次部署时，建议严格按这个顺序做，不要一上来同时开多个终端。

1. 用管理员权限打开 `PowerShell`
2. 进入项目根目录
3. 执行：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\bootstrap-windows-env.ps1
```

4. 脚本提示输入 `MySQL root` 密码时，输入一个你自己记得住的本地开发密码，例如 `root123`
5. 等到看到 `Bootstrap finished.`
6. 再执行一次版本检查：

```powershell
git --version
java -version
mvn -v
node -v
npm -v
```

7. 第一次启动后端时，优先手动启动，便于直接看报错：

```powershell
cd .\backend
$env:SPRING_PROFILES_ACTIVE="dev"
$env:SPRING_DATASOURCE_URL="jdbc:mysql://localhost:3306/campus_run?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai"
$env:SPRING_DATASOURCE_USERNAME="campus"
$env:SPRING_DATASOURCE_PASSWORD="campus123"
$env:SPRING_DATA_REDIS_HOST="localhost"
$env:SPRING_DATA_REDIS_PORT="6379"
$env:CAMPUSRUN_SWAGGER_ENABLED="true"
$env:CAMPUSRUN_RUN_ALLOW_SIMULATED_RUNS="true"
mvn spring-boot:run
```

8. 等到后端日志出现 `Started CampusRunApplication`
9. 另开一个 `PowerShell` 窗口，到 [admin-web](/home/huge/dev/LJJ/admin-web) 执行：

```powershell
cd .\admin-web
npm.cmd run dev -- --host 127.0.0.1
```

10. 浏览器打开 `http://127.0.0.1:5173`
11. 如果手动启动确认没有问题，再回过头使用 `dev-up-windows.ps1`

如果你希望全程走脚本，建议至少等第一次手动启动成功后再切回脚本模式，这样后续排障成本会低很多。

## 4. Git for Windows

检查是否已安装：

```powershell
git --version
```

如果提示命令不存在：

1. 安装 `Git for Windows`
2. 安装时选择 `Git from the command line and also from 3rd-party software`
3. 安装完成后关闭当前终端，重新打开 PowerShell
4. 再次执行 `git --version`

如果已经安装但版本过旧，直接覆盖安装新版即可。

## 5. JDK 17

项目要求 `Java 17`。检查当前版本：

```powershell
java -version
```

正确示例：

```text
openjdk version "17.x.x"
```

如果出现以下情况：

- 没有 `java` 命令
- 版本不是 `17`

按这个步骤处理：

1. 安装一个 `JDK 17` 发行版，例如 `Temurin 17`
2. 安装时勾选把 Java 加入 `PATH`
3. 安装后确认系统环境变量 `JAVA_HOME` 指向 JDK 17
4. 关闭当前 PowerShell，重新打开
5. 再次执行：

```powershell
java -version
```

如果电脑里同时装了多个 JDK，必须确保终端里实际生效的是 `17`。

## 6. Maven 3.9+

检查是否已安装：

```powershell
mvn -v
```

正常输出里应该同时看到：

- Maven 版本
- `Java version: 17`

如果没有 `mvn`：

1. 安装 `Apache Maven 3.9+`
2. 配置 `MAVEN_HOME`
3. 把 `%MAVEN_HOME%\bin` 加入 `PATH`
4. 关闭并重新打开 PowerShell
5. 再次执行 `mvn -v`

如果 `mvn -v` 显示的 Java 不是 17，先修正 `JAVA_HOME`，再重新打开终端。

## 7. Node.js 20 LTS

检查版本：

```powershell
node -v
npm -v
```

如果 `node` 不存在，或者版本明显过旧：

1. 安装 `Node.js 20 LTS`
2. 安装时保持 `npm` 一起安装
3. 关闭当前终端，重新打开
4. 再次执行：

```powershell
node -v
npm -v
```

## 8. MySQL 8.0

### 8.1 检查是否已安装

```powershell
Get-Service MySQL80 -ErrorAction SilentlyContinue
```

如果返回为空，说明常见的 `MySQL80` 服务还没有装好。也可以直接检查端口：

```powershell
Test-NetConnection -ComputerName 127.0.0.1 -Port 3306
```

### 8.2 安装建议

安装 `MySQL Community Server 8.0`，建议：

1. 选择 `Developer Default` 或 `Server only`
2. 记住 `root` 密码
3. 保持默认端口 `3306`
4. 安装为 Windows 服务，并设置自动启动

### 8.3 初始化数据库和业务账号

安装完成后，用 `mysql` 命令行、`MySQL Shell` 或 `Workbench` 执行：

```sql
CREATE DATABASE campus_run CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'campus'@'localhost' IDENTIFIED BY 'campus123';
GRANT ALL PRIVILEGES ON campus_run.* TO 'campus'@'localhost';
FLUSH PRIVILEGES;
```

如果 `campus` 用户已存在，可以改成：

```sql
ALTER USER 'campus'@'localhost' IDENTIFIED BY 'campus123';
GRANT ALL PRIVILEGES ON campus_run.* TO 'campus'@'localhost';
FLUSH PRIVILEGES;
```

## 9. Redis 兼容服务

这个项目依赖 Redis。Windows 下建议装一个原生的 Redis 兼容服务，例如 `Memurai`。

### 9.1 检查是否已安装

先检查服务：

```powershell
Get-Service Memurai -ErrorAction SilentlyContinue
```

再检查端口：

```powershell
Test-NetConnection -ComputerName 127.0.0.1 -Port 6379
```

### 9.2 安装建议

1. 安装一个 Redis 兼容服务
2. 保持监听端口 `6379`
3. 安装为 Windows 服务，并设置自动启动

只要本机 `127.0.0.1:6379` 可访问，这个项目就能连上。

## 10. 微信开发者工具

安装微信开发者工具，然后在后续步骤里导入 [miniapp](/home/huge/dev/LJJ/miniapp) 目录。

说明：

- 开发者工具里调试本机接口可以直接用 `http://127.0.0.1:8080/api`
- 真机调试不适合直接用 `127.0.0.1`
- 真机需要手机能访问的 `HTTPS` 地址

## 11. 首次启动前的版本总检查

在项目根目录执行：

```powershell
git --version
java -version
mvn -v
node -v
npm -v
```

如果其中任何一个命令报错，先不要启动项目，先把对应环境装好。

如果你已经跑过自动安装脚本，也建议再手动执行一次这组命令确认环境真的可用。

## 12. Windows 启动脚本

项目已经提供了 Windows 脚本：

- [scripts/dev-up-windows.ps1](/home/huge/dev/LJJ/scripts/dev-up-windows.ps1)
- [scripts/dev-down-windows.ps1](/home/huge/dev/LJJ/scripts/dev-down-windows.ps1)

启动命令：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\dev-up-windows.ps1
```

脚本会做这些事：

1. 检查 `java`、`mvn`、`node`、`npm`
2. 校验 Java 主版本是否为 `17`
3. 检查 MySQL `3306` 和 Redis `6379`
4. 如果发现常见 Windows 服务名存在但未启动，会尝试自动启动
5. 自动生成 `miniapp/config.js`
6. 如果 `admin-web/node_modules` 不存在，会自动执行 `npm install`
7. 启动后端和管理后台
8. 在 `scripts/.runtime/windows/` 下写日志和状态文件

如果你把 MySQL 或 Redis 安装在非默认端口，可以这样传参：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\dev-up-windows.ps1 -MySqlPort 3307 -RedisPort 6380
```

如果你想给小程序写入其他接口地址：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\dev-up-windows.ps1 -ApiBaseUrl "http://127.0.0.1:8080/api"
```

说明：

- 第一次在新电脑上执行 `dev-up-windows.ps1` 时，后端可能会先下载大量 Maven 依赖
- 在依赖还没下载完之前，`admin-web` 可以先打开，但登录接口可能报 `500` 或代理错误
- 这时候不要急着判断账号密码有问题，先等后端真正启动完成
- 可以用下面的命令确认后端是否已经就绪：

```powershell
Get-Content .\scripts\.runtime\windows\backend.log -Wait
curl http://127.0.0.1:8080/actuator/health
```

只有当健康检查返回 `{"status":"UP"}` 后，再去浏览器登录后台。

## 13. Windows 关闭脚本

关闭命令：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\dev-down-windows.ps1
```

关闭脚本会：

1. 停掉启动脚本拉起的 `backend` 和 `admin-web`
2. 停掉由启动脚本自动启动的 MySQL/Redis 服务
3. 清理运行状态文件

如果 MySQL/Redis 原本就是你自己手动启动的，关闭脚本不会动它们。

## 14. 启动成功后的访问地址

- 管理后台：`http://127.0.0.1:5173`
- Swagger：`http://127.0.0.1:8080/swagger-ui.html`
- 健康检查：`http://127.0.0.1:8080/actuator/health`

默认账号：

- 管理员：`admin / admin123`
- 学生：`20230001 / 123456`
- 学生：`20230002 / 123456`

这些账号来自 [backend/src/main/resources/db/devdata/R__seed_demo_data.sql](/home/huge/dev/LJJ/backend/src/main/resources/db/devdata/R__seed_demo_data.sql)。

前提：

- 后端已经成功启动
- `Flyway` 迁移已经执行完成
- 演示数据已经正常导入

## 15. 小程序导入方式

1. 打开微信开发者工具
2. 导入 [miniapp](/home/huge/dev/LJJ/miniapp)
3. 确认本地已经生成 `miniapp/config.js`
4. 直接使用上面的测试账号登录

## 16. 常见问题

### 16.1 `java -version` 不是 17

这是硬性要求。不要继续启动项目，先把 `JAVA_HOME` 和 `PATH` 修正到 `JDK 17`。

### 16.2 `mvn -v` 能运行，但显示的 Java 版本不对

说明 Maven 读到的不是你想要的 JDK。先修正 `JAVA_HOME`，再开新终端执行 `mvn -v`。

### 16.3 MySQL 端口不通

先确认 Windows 服务已启动，再确认数据库 `campus_run` 和用户 `campus` 已创建。

### 16.4 Redis 端口不通

先确认 Redis 兼容服务已经安装并启动，监听在 `6379`。

### 16.5 管理后台能打开，但接口报错

通常是后端没起来。先看：

- `http://127.0.0.1:8080/actuator/health`
- `scripts/.runtime/windows/backend.log`

如果 `admin-web.log` 里看到：

```text
http proxy error: /api/auth/login
AggregateError [ECONNREFUSED]
```

说明前端已经起来了，但后端 `8080` 当时还没有完成启动。先等后端依赖下载和 Spring Boot 启动结束，再刷新页面重试。

### 16.6 小程序真机请求失败

不要直接用 `127.0.0.1` 做真机接口地址。真机需要手机能访问的 `HTTPS` 公网地址。

### 16.7 `winget` 不存在

先安装或更新 `App Installer`。`winget` 是微软官方的 Windows 包管理器，自动安装脚本依赖它。

### 16.8 自动脚本已经装了 MySQL，但 MySQL80 服务没出来

旧版本脚本曾经依赖 `MySQL Installer Console`。当前脚本已经改成直接检测 `mysqld.exe/mysql.exe`，并自动：

- 初始化数据目录
- 注册 `MySQL80` Windows 服务
- 设置 `root` 密码
- 创建 `campus_run` 和 `campus / campus123`

如果你仍然遇到 MySQL 相关问题，先看：

- `scripts/.runtime/windows/bootstrap.log`

再确认下面三项：

```powershell
Get-Service MySQL80
& "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe" -h localhost -P 3306 -u root "-p你的root密码" -e "SELECT VERSION();"
& "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe" -h localhost -P 3306 -u campus "-pcampus123" -e "SHOW DATABASES;"
```

如果 `campus` 账号登录失败，可以手动重建：

```powershell
& "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe" -h localhost -P 3306 -u root "-p你的root密码" -e "CREATE DATABASE IF NOT EXISTS campus_run CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci; CREATE USER IF NOT EXISTS 'campus'@'localhost' IDENTIFIED BY 'campus123'; ALTER USER 'campus'@'localhost' IDENTIFIED BY 'campus123'; GRANT ALL PRIVILEGES ON campus_run.* TO 'campus'@'localhost'; FLUSH PRIVILEGES;"
```

### 16.9 `npm` 在 PowerShell 里报执行策略错误

如果你在 PowerShell 里看到类似：

```text
npm.ps1，因为在此系统上禁止运行脚本
```

优先用下面这两种方式之一：

```powershell
npm.cmd run dev -- --host 127.0.0.1
```

或者：

```powershell
Set-ExecutionPolicy -Scope Process Bypass
npm run dev -- --host 127.0.0.1
```

### 16.10 自动脚本执行后，当前终端还是找不到新命令

关闭 PowerShell，重新打开一个管理员 PowerShell，再执行：

```powershell
git --version
java -version
mvn -v
node -v
npm -v
```

如果是运行 [scripts/dev-up-windows.ps1](/home/huge/dev/LJJ/scripts/dev-up-windows.ps1) 时才出现这个问题，先更新到当前仓库版本。当前脚本已经会在启动前主动刷新注册表中的 `PATH`、`JAVA_HOME`、`MAVEN_HOME`。

### 16.11 后端报 `Access denied for user 'campus'@'localhost'`

这说明：

- MySQL 已经启动
- 但业务账号 `campus` 不存在，或密码不对，或授权不对

先验证 root：

```powershell
& "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe" -h localhost -P 3306 -u root "-p你的root密码" -e "SELECT VERSION();"
```

再重建业务账号：

```powershell
& "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe" -h localhost -P 3306 -u root "-p你的root密码" -e "CREATE DATABASE IF NOT EXISTS campus_run CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci; CREATE USER IF NOT EXISTS 'campus'@'localhost' IDENTIFIED BY 'campus123'; ALTER USER 'campus'@'localhost' IDENTIFIED BY 'campus123'; GRANT ALL PRIVILEGES ON campus_run.* TO 'campus'@'localhost'; FLUSH PRIVILEGES;"
```

再验证：

```powershell
& "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe" -h localhost -P 3306 -u campus "-pcampus123" -e "SHOW DATABASES;"
```

### 16.12 使用脚本启动后登录接口返回 `500`

先不要立刻判断是账号密码错误，先确认后端是否真的已经完成启动。

如果 [scripts/.runtime/windows/admin-web.log](/home/huge/dev/LJJ/scripts/.runtime/windows/admin-web.log) 里出现：

```text
http proxy error: /api/auth/login
AggregateError [ECONNREFUSED]
```

这通常表示：

- `admin-web` 已经起来了
- 但 `backend` 还在下载 Maven 依赖，或者 Spring Boot 还没监听 `8080`

先执行：

```powershell
curl http://127.0.0.1:8080/actuator/health
```

等后端返回 `UP` 再登录。
