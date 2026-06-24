# springboot-todo-demo

## 1. 项目介绍

这是一个用于练习 Spring Boot、MySQL、Redis 和 RocketMQ 的简易 Todo 任务管理系统。项目只提供新增、查询、完成和删除四个接口，重点展示常见后端分层和中间件的基础用法。不包含 Spring Security、登录系统或前端页面。

本地运行需要准备 MySQL、Redis、RocketMQ。

所有 `/todos` 和 `/todos/**` 请求都必须携带 `userId` 请求头。缺少、格式错误或小于等于 0 时，拦截器会直接返回 HTTP 401。

任务状态：`0` 表示未完成，`1` 表示已完成。

## 2. 技术栈

- Java 17
- Spring Boot 3.2.5
- Maven
- Spring Web / Spring MVC
- Spring AOP
- MyBatis 3（XML Mapper）
- MySQL 8
- Spring Data Redis
- Redisson
- RocketMQ Spring Boot Starter
- Lombok
- Docker Compose

## 3. 项目结构

```text
springboot-todo-demo
├── pom.xml
├── docker-compose.yml
├── README.md
└── src/main
    ├── java/com/example/todo
    │   ├── TodoApplication.java
    │   ├── aspect
    │   │   └── ControllerTimeAspect.java
    │   ├── config
    │   │   ├── RedissonConfig.java
    │   │   └── WebMvcConfig.java
    │   ├── controller
    │   │   └── TodoController.java
    │   ├── dto
    │   │   └── CreateTodoRequest.java
    │   ├── entity
    │   │   ├── Todo.java
    │   │   └── TodoLog.java
    │   ├── interceptor
    │   │   └── UserIdInterceptor.java
    │   ├── mapper
    │   │   ├── TodoMapper.java
    │   │   └── TodoLogMapper.java
    │   ├── mq
    │   │   ├── TodoMessageConsumer.java
    │   │   └── TodoMessageProducer.java
    │   ├── service
    │   │   ├── TodoService.java
    │   │   └── impl
    │   │       └── TodoServiceImpl.java
    └── resources
        ├── application.yml
        ├── schema.sql
        └── mapper
            ├── TodoMapper.xml
            └── TodoLogMapper.xml
```

## 4. 数据库建表 SQL

完整文件位于 `src/main/resources/schema.sql`。

```sql
CREATE TABLE IF NOT EXISTS todo (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0-未完成，1-已完成',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_todo_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS todo_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    todo_id BIGINT NOT NULL,
    action VARCHAR(32) NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_todo_log_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

`idx_todo_user_id` 和 `idx_todo_log_user_id` 是普通索引，用于加快按用户查询。MyBatis XML 中包含 `INSERT`、`SELECT`、`UPDATE`、`DELETE` 和 `COUNT` 等基础 SQL。

## 5. 启动 MySQL 和 Redis

确保已安装 Docker Desktop。先通过环境变量提供数据库密码，然后在项目根目录启动容器。

```bash
$env:MYSQL_PASSWORD = "<your-password>"
docker compose up -d
docker compose ps
```

Compose 文件只包含 MySQL 和 Redis：

- MySQL：`localhost:3306/todo_demo`，用户名 `root`，密码从 `MYSQL_PASSWORD` 环境变量读取
- Redis：`localhost:6379`

停止容器：
```bash
docker compose down
```

## 6. 启动 RocketMQ 和 Spring Boot

新增任务时，`TodoServiceImpl` 在写入 `todo` 和 `todo_log` 后调用 `TodoMessageProducer`，由它通过 `RocketMQTemplate` 向 `todo-topic` 发送 todoId。`TodoMessageConsumer` 监听同一个 topic，收到消息后在日志中打印 todoId。

编译并启动项目：

```bash
mvn clean package
mvn spring-boot:run
```

应用默认监听 `http://localhost:8080`。启动 Maven 或 JAR 时也必须提供 `MYSQL_PASSWORD` 环境变量；数据库、Redis 和 RocketMQ 的地址可在 `src/main/resources/application.yml` 中修改。

## 7. 测试接口

### 7.1 新增任务

```bash
curl.exe -i -X POST "http://localhost:8080/todos" -H "Content-Type: application/json" -H "userId: 1" -d "{\"title\":\"学习 Spring Boot\"}"
```
成功返回 HTTP 201。相同用户重复提交同名任务返回 HTTP 409。

### 7.2 查询当前用户任务列表

```bash
curl.exe -i "http://localhost:8080/todos" -H "userId: 1"
```

### 7.3 完成任务

将下面的 `1` 替换为实际 todo id：

```bash
curl.exe -i -X PUT "http://localhost:8080/todos/1/finish" -H "userId: 1"
```
成功返回 HTTP 204。

### 7.4 删除任务

```bash
curl.exe -i -X DELETE "http://localhost:8080/todos/1" -H "userId: 1"
```
成功返回 HTTP 204。

### 7.5 验证拦截器

```bash
curl.exe -i "http://localhost:8080/todos"
```
没有 `userId` 请求头时返回 HTTP 401。

### 7.6 查看 Redis 数据

```bash
docker exec -it todo-redis redis-cli GET todo:count:1
docker exec -it todo-redis redis-cli LRANGE todo:recent:1 0 -1
```


