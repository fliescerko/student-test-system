# 学生成绩查询系统（2025 课程作业）

> **版本**：0.0.2（与课堂演示保持一致）  
> **技术栈**：Spring Boot 3.3 + Spring Security 6 + Spring Data JPA (Hibernate) + Thymeleaf + HikariCP + MySQL 8  
> **部署环境**：AWS Elastic Beanstalk（Amazon Linux 2023，内置 Nginx 反向代理），RDS MySQL。

---

## 1. 系统架构图（文字版）

```
[浏览器]
   └─ 学生 / 教师 / 管理员
        │
        ▼
[Nginx (EB 反向代理, 80→5000)]
        │
        ▼
[Spring Boot Web 应用 (Tomcat 10.1, 端口 5000)]
        ├─ Web 层（Controller：/student/**, /teacher/**, /admin/**, /login, /dashboard）
        ├─ 业务层（Service：成绩计算、时间窗校验、导入校验、用户与角色逻辑）
        ├─ 数据访问层（Repository：UserRepo / StudentRepo / TeacherRepo / CourseRepo / GradeRepo ...）
        └─ 视图层（Thymeleaf 模版：/templates/**）
        │
        ▼
[MySQL（RDS 或本地）]
        ├─ users / roles / students / teachers
        ├─ courses（含成绩可见开始/结束时间）
        └─ grades  (student×course 的成绩明细)
```

- **安全**：基于 Spring Security 的表单登录与角色访问控制（`ROLE_STUDENT`、`ROLE_TEACHER`、`ROLE_ADMIN`）。
- **数据源**：`application.properties` 中开启 `spring.jpa.hibernate.ddl-auto=update`，首次运行自动建表。
- **连接池**：HikariCP。

---

## 2. 角色与功能对照

### 学生
- 登录后在 **「我的成绩」** 页面查看：课程名称、分数、学期等信息。
- 仅能在课程 **开放查询时间窗** 内（`course.grade_view_start` ~ `course.grade_view_end`）查看；超时自动不可见。
- 可查看但**不可修改**个人基本信息。

### 教师
- **成绩录入**：手动录入或 **Excel/CSV 导入** 学生成绩：
    - Excel（`.xlsx`）：**每一行**记录包含：`学号, 学生姓名, 课程, 分数, 学期`。
    - CSV（`.csv`）：**逗号分隔**并与上面一致。
    - 系统会校验格式与学号存在性，发现异常会在页面提示可下载错误报告。
- **成绩可见时间窗管理**：设置/修改某门课的 `grade_view_start` 与 `grade_view_end`。
- **成绩查看**：查看所授课程的成绩概览与导入历史。

### 管理员
- 学生信息管理：新增/修改/删除学生（学号、姓名、班级、年级等）与班级信息。
- 用户与角色：可创建新教师/学生/管理员账号，重置密码。

> 以上功能均可在 `templates/` 下对应页面找到：例如 `teacher/upload.html`、`teacher/input-grade.html`、`student/grades.html`、`admin/student/list.html` 等。

---

## 3. 关键页面与接口（节选）

- 登录：`GET /login`（表单提交登录）
- 仪表盘：`GET /dashboard`（根据角色跳转）

### 学生（`/student`）
- `GET /student/grades`：查看本人成绩（在课程开放窗内）。

### 教师（`/teacher`）
- `GET /teacher/upload`：导入页面（Excel/CSV）。
- `POST /teacher/upload`：上传并导入成绩文件。
- `GET /teacher/input-grade`：手动录入页面。
- `POST /teacher/input-grade`：提交单条成绩。
- `GET /teacher/open`：成绩开放时间设置页面。
- `POST /teacher/open`：提交课程的开放起止时间。

> 以上映射可在 `TeacherController` 中找到；学生端映射见 `StudentController`。

---

## 4. 成绩导入文件格式

### 4.1 Excel（.xlsx）
- **表头（第一行）**：`学号, 姓名, 课程, 分数, 学期`
- **数据样例**
  ```
  20230001, 张三, 数学, 88, 2025春
  20230002, 李四, 英语, 92, 2025春
  ```

### 4.2 CSV（.csv）
- 与 Excel 字段一致，**UTF-8** 编码，逗号分隔，首行可为表头。

**导入校验**：系统会校验学号是否存在、课程是否存在、分数是否为 0~100 的合法数值；若校验失败会在导入结果页面给出失败行及原因。

---

## 5. 成绩查询时间窗

- 每门 **课程（course）** 记录两个字段：`grade_view_start` 与 `grade_view_end`。
- 学生端查询时会判断 `now` 是否在时间窗内；**仅在开放时间内可见**。
- 教师可在 **「成绩开放时间」** 页面对所属课程进行配置。

---

## 6. 安全性设计

- **认证**：基于表单登录（/login），使用持久化的用户与角色表。
- **授权**：
    - `/student/**` 仅对 `ROLE_STUDENT` 开放；
    - `/teacher/**` 仅对 `ROLE_TEACHER` 开放；
    - `/admin/**` 仅对 `ROLE_ADMIN` 开放。
- **敏感信息**：数据库账号密码通过 **环境变量** 注入，避免硬编码。
- **防护**：Nginx 统一入口 + Spring Security 过滤器链。

---

## 7. 本地开发与运行

### 7.1 准备 MySQL（推荐 8.x）
新建数据库（如 `grades_db`），并创建账号（如 `grades_user`）。授予最少权限：`CREATE, ALTER, INSERT, UPDATE, SELECT`。

### 7.2 配置 `src/main/resources/application.properties`
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/grades_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&characterEncoding=UTF-8
spring.datasource.username=grades_user
spring.datasource.password=<your_password>

server.port=5000
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.thymeleaf.cache=false
```

### 7.3 运行
```bash
# 使用 Gradle
./gradlew bootRun          # Windows 下可用 gradlew.bat bootRun
# 或打包运行
./gradlew bootJar && java -jar build/libs/*.jar
```
首次运行会自动建表。若在控制台看到“数据初始化开始/完成”的日志，表示初始化脚本成功执行。

> 如果你看到与 **JDBC Dialect** 相关的异常，通常是数据库连接失败或用户名/密码错误，请先确认连接再重试。

---

## 8. 在 AWS 上部署（Elastic Beanstalk + RDS）

1. **创建 RDS MySQL** 实例（建议启用 **公有访问关闭**，通过 EB 子网访问）。
2. **在 EB 环境**（Tomcat on Amazon Linux 2023 或 Java SE 平台）部署可执行 JAR。保证应用监听 `5000` 端口（Nginx 会反代）。
3. **在 EB 控制台 → 配置 → 软件** 中添加以下环境变量：
    - `SPRING_DATASOURCE_URL=jdbc:mysql://<rds-endpoint>:3306/grades_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&characterEncoding=UTF-8`
    - `SPRING_DATASOURCE_USERNAME=<db_user>`
    - `SPRING_DATASOURCE_PASSWORD=<db_pass>`
4. 部署完成后访问环境域名（如 `http://<env>.elasticbeanstalk.com`）。
5. 若 Nginx 报 `connect() failed (111: Connection refused) while connecting to upstream 127.0.0.1:5000`：
    - 检查应用是否成功启动并监听 5000；
    - 查看 `/var/log/web.stdout.log` 是否有数据库权限 `Access denied for user` 提示；
    - 确认 RDS 安全组允许来自 EB 实例所在私有子网/安全组的 3306 入站。

---

## 9. 账户初始化（演示环境）

系统会在首次启动时**尝试自动初始化**演示数据（创建 `admin / teacher1~3 / student1~10` 等）。  
默认口令在 `DataInit` 与 `SecurityConfig` 中由密码编码器统一处理。若无法登录：
- 使用管理员账号登录后台重置口令；或
- 在数据库中对 `users` 表的指定用户重置 `password` 字段（BCrypt）。

> 出于安全考虑，**请务必**在对外演示前修改默认密码。

---


