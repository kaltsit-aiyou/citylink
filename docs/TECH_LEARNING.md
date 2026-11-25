# CityLink 技术学习与八股复盘

- 文档版本：V0.1
- 对应项目蓝图：阶段 1——工程搭建与用户系统
- 记录日期：2026-11-25
- 范围：当前**已经使用**的技术，以及与代码直接相关的后端面试知识
- 更新原则：开发过程中记录疑问；每完成一个项目阶段，核对实际代码、补充学习内容，再统一更新本文件。计划使用但尚未引入的技术，不写成“已经掌握或实现”。

## 1. 先记住一条完整调用链

以“登录后查看个人资料”为例：

**浏览器页面 → JavaScript `fetch` 请求 → HTTP → Spring Security 检查 JWT → Spring MVC 的 `UserController` → `UserService` → MyBatis 的 `UserMapper` → JDBC 数据库连接 → MySQL `app_user` 表 → JSON 响应 → JavaScript 更新页面。**

学习八股时，先问“这个概念位于调用链的哪一段”，再回答“它解决了什么问题”。这样能把理论和自己写过的代码连起来。

当前 `pom.xml` 使用 **Java 17、Spring Boot 4.1.2、MyBatis Spring Boot Starter 4.1.0**。早期项目蓝图写过 Spring Boot 3.x；学习**当前代码**时，以实际 `pom.xml` 为准。

## 2. 当前技术清单与职责

| 技术 | 在当前项目中做什么 | 到哪里看 |
| --- | --- | --- |
| Java 17 | 编写启动类、接口、业务类、数据类和测试；使用 `record`、注解、lambda | `BackendApplication`、`UserService`、测试类 |
| Maven | 读取 `pom.xml`，管理依赖，编译并运行测试 | `pom.xml`、Maven 工具窗口 |
| Spring Boot | 创建 Spring 容器，自动配置 Web、数据源等组件，启动应用 | `BackendApplication` |
| Spring MVC | 将 URL 和 HTTP 方法映射到 Controller，处理 JSON 请求与响应 | `AuthController`、`UserController` |
| Bean Validation | 在业务执行前检查用户名、密码、昵称等输入 | `@Valid`、`@NotBlank`、`@Size`、`@Pattern` |
| Spring Security | 决定匿名请求和登录后请求能访问哪些资源 | `SecurityConfig` |
| BCrypt、JWT | 保存密码摘要；登录后签发、验证身份令牌 | `UserService`、`SecurityConfig` |
| MyBatis、JDBC | 将 Java 方法与 SQL 连接起来，并访问数据库 | `UserMapper` |
| MySQL 8 | 持久保存账号和个人资料，用唯一约束防止重名 | `app_user` 表 |
| HTML、CSS、JavaScript | 展示注册、登录、资料页面，并调用 `/api` 接口 | `static` 目录中的页面与资源 |
| JUnit 5、Spring Boot Test | 检查应用能启动、Mapper 能查询数据库 | `BackendApplicationTests` |
| Git、GitHub | 记录阶段成果，并把本地提交同步到远程仓库 | `git status`、`git commit`、`git push` |

本阶段**没有实际引入** Redis、Kafka、Elasticsearch、微服务、Vue 或 Nginx。它们出现在长期蓝图中，不应在介绍当前项目经历时说成已实现。

## 3. 从浏览器到 Controller：HTTP 与计算机网络

浏览器访问 `http://localhost:8080` 时，`localhost` 指这台电脑，`8080` 是后端监听的端口。如果后端没有启动，浏览器可能显示“连接被拒绝”；这意味着尚未建立到应用的连接，不等于 Controller 返回了 404。

当前接口对应关系：

| 操作 | HTTP 请求 | 成功结果 |
| --- | --- | --- |
| 注册 | `POST /api/auth/register` | `201 Created` |
| 登录 | `POST /api/auth/login` | `200 OK`，返回令牌 |
| 查看资料 | `GET /api/users/me` | `200 OK`，返回资料 |
| 修改资料 | `PUT /api/users/me` | `200 OK`，返回更新后的资料 |

**面试联系：**

- HTTP 请求由方法、路径、请求头和可选的请求体组成。注册、登录的 JSON 放在请求体中；登录后的身份令牌放在 `Authorization: Bearer ...` 请求头中。
- `400` 通常表示请求参数不合要求；本项目用 `401` 表示没有有效身份；`403` 表示有身份但无权执行；`404` 表示资源未找到；注册用户名冲突在业务代码中约定返回 `409`。
- TCP 负责建立连接并可靠传输；HTTP 规定应用层如何请求与响应。本地的 `http://` 适合开发演示，未来公开部署涉及密码和令牌时应使用 HTTPS。
- 页面与 API 都由同一个 Spring Boot 应用提供，使用相同的协议、主机和端口，因此当前页面调用 `/api` 不需要额外的跨域配置。

阅读顺序：[小林 Coding：HTTP 常见面试题](https://xiaolincoding.com/network/2_http/http_interview.html) → [计算机网络面试题](https://xiaolincoding.com/interview/network.html)。读的时候用浏览器开发者工具的“网络”面板，对照自己发出的请求、状态码和响应。

## 4. Spring、Spring Boot 与 Java：对象从哪里来

`BackendApplication` 上的 `@SpringBootApplication` 标记应用入口。启动时，Spring 从入口类所在的包及其子包查找相关组件。`AuthController`、`UserController` 是接口层，`UserService` 是业务层，`UserMapper` 是数据库访问层。

**IoC（控制反转）**：对象的创建与管理主要交给 Spring 容器。**DI（依赖注入）**：例如 `AuthController` 的构造方法需要 `UserService`，Spring 找到对应 Bean 后把它传入。`@Service` 让 `UserService` 成为可管理的组件；如果缺少这个 Bean，Controller 就无法完成构造注入。

**自动配置**：引入对应依赖、提供必要配置后，Spring Boot 能按条件创建基础组件。例如项目配置了数据源地址和账号，相关依赖会参与建立数据库访问能力。自动配置并不意味着任何类都无需声明；自己的业务类仍要放在扫描范围内并使用适当注解。

**Java 语法在本项目中的作用：**

- `interface UserMapper`：声明数据库操作方法，MyBatis 为其提供实现。
- `record RegisterRequest(...)`：定义一组请求数据，`request.username()` 是读取字段的方法。
- `@RestController`、`@Service`、`@Mapper`：注解是给框架识别和处理的元信息，不是普通注释。
- `() -> userMapper.countUsers()`：lambda 把“稍后执行的操作”传给测试断言；`assertDoesNotThrow` 是 JUnit 方法，不是 Java 语言关键字。
- `private final UserService userService`：对象创建后不再重新给该字段赋值；构造方法负责提供必需依赖。

结合 [小林 Coding：Spring 面试题](https://xiaolincoding.com/interview/spring.html) 复习 IoC、Bean、构造注入、自动配置和 Spring MVC；Java 语法可结合其 [Java 基础面试题](https://xiaolincoding.com/interview/java.html)。项目中的构造注入也可对照 [Spring 官方说明](https://docs.spring.io/spring-framework/reference/7.0/core/beans/dependencies/factory-collaborators.html)。

## 5. Spring MVC：请求为什么能变成 Java 对象

`@RequestMapping("/api/auth")` 给 Controller 设置共同路径，`@PostMapping("/login")` 指定具体的 POST 路径。`@RequestBody` 把请求体中的 JSON 读成 Java 请求对象；`@Valid` 触发字段上的校验注解。校验不通过时，请求不应继续执行正常注册逻辑。

Controller 应负责接收请求、选择返回状态和调用业务层；注册规则、密码核对、资料修改等放在 `UserService`。这使接口层和业务规则各有职责。

注册返回的 `UserProfile` 只包含公开资料，不包含 `passwordHash`。这里要记住：**数据库对象有什么字段，不代表接口就应该把这些字段全部返回给浏览器。**

## 6. 登录与授权：密码、JWT、安全规则

注册时，`UserService` 使用 BCrypt 生成密码摘要，数据库保存 `password_hash`，不保存明文密码。登录时使用 `matches` 核对用户输入。**摘要不是可解密后取回原密码的加密文本。** 当前代码还限制密码不超过 72 个 UTF-8 字节，以符合所用 BCrypt 实现的输入限制。

登录成功后，服务端签发 JWT。当前令牌包含签发者、用户 ID、签发时间和到期时间，有效期为 **7200 秒**。客户端访问 `/api/users/me` 时携带 Bearer 令牌；后端验证签名和有效期，再从令牌的 `sub` 取得用户 ID。**JWT 的签名用于验证内容未被篡改；不要把令牌载荷当作加密的秘密存储空间。**

当前安全规则允许匿名读取首页与静态资源、访问注册和登录接口；个人资料接口需要有效令牌。页面把令牌放在当前标签页的 `sessionStorage`，刷新页面仍可读取；退出登录会删除本地令牌。当前没有服务端令牌撤销功能，因此“点击退出”并不等于已复制出去的令牌立即失效。

结合 [Spring Security 官方 JWT 文档](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html) 理解 Bearer 请求头、签名验证和身份信息。以后如改用 Cookie 登录，应重新审视当前的 CSRF 配置。

## 7. MyBatis、MySQL：数据为什么可靠

`UserMapper` 上的 `@Mapper` 让 MyBatis 识别接口。`@Select`、`@Insert`、`@Update` 指定 SQL；`@Param` 将方法参数命名。`#{username}` 会把值作为预编译 SQL 的参数绑定，不能把用户输入直接拼到 SQL 文本里。查询中的 `AS passwordHash`、`AS avatarUrl` 用于把数据库列名对应到 Java 属性名。[MyBatis 官方参数说明](https://mybatis.org/mybatis-3/sqlmap-xml.html)

`app_user` 表的关键设计：

- `id`：自增主键，用于稳定标识用户。
- `username`：非空，且具有唯一索引 `uk_app_user_username`。
- `password_hash`：保存密码摘要。
- `nickname`、`avatar_url`：个人资料；头像地址可以为空。
- `created_at`、`updated_at`：记录创建和修改时间。

注册前用 `countByUsername` 查询，可以尽早给出提示；**最终防重仍依赖数据库唯一约束**。因为两个人同时注册相同用户名时，二者都可能先查到“尚未存在”，只有数据库约束能在写入时阻止重复。代码捕获重复键异常并转换为业务响应。

学习索引时，先从本项目的“按用户名查找”和“用户名必须唯一”出发，再阅读 [小林 Coding：索引常见面试题](https://xiaolincoding.com/mysql/index/index_interview.html)。事务的 ACID、隔离级别和 MVCC 是下一步理解并发写入的基础，可阅读 [事务隔离级别是怎么实现的](https://xiaolincoding.com/mysql/transaction/mvcc.html)。当前账户代码**没有自行实现一套复杂的多步骤事务流程**，不要把读过的事务原理说成项目里已经全部用过。

## 8. Maven、测试与 Git：如何得到可复现的成果

### Maven

`pom.xml` 定义项目坐标、父 POM、Java 版本、依赖和插件。Spring Boot 父 POM 管理许多依赖版本，因此部分依赖不用逐项写版本。`mysql-connector-j` 标为 `runtime`，表示它主要在运行和测试时需要；测试依赖标为 `test`，不会作为正常运行依赖使用。项目的 Maven Wrapper 配置了 Maven **3.9.16**；`wrapperVersion=3.3.4` 是 Wrapper 脚本版本，两者不是一回事。

在 `backend` 目录运行 `.\mvnw.cmd test` 时，Maven会按构建生命周期先处理资源和编译，再执行测试。IDEA 的“Maven 重新加载项目”是让 IDE 重新读取依赖模型；它与实际运行 `test` 是两件事。学习资料：[Maven POM](https://maven.apache.org/guides/introduction/introduction-to-the-pom.html)、[依赖范围](https://maven.apache.org/guides/introduction/introduction-to-dependency-mechanism.html)、[构建生命周期](https://maven.apache.org/guides/introduction/introduction-to-the-lifecycle.html)。

### 测试

`@SpringBootTest` 会启动应用上下文。现有两个测试分别检查应用能加载，以及 `UserMapper.countUsers()` 查询不抛异常；后者需要数据库可连接且表存在。**这不等于注册、登录、权限的所有分支都有自动化测试。** 浏览器联调补充了真实用户流程，后续阶段应逐步增加针对业务规则的测试。[Spring Boot 测试说明](https://docs.spring.io/spring-boot/reference/testing/spring-boot-applications.html)

### Git

按“工作区 → 暂存区 → 本地提交 → GitHub”的顺序理解：

1. `git status --short`：看哪些文件被修改（`M`）或尚未跟踪（`??`）。
2. `git add <文件>`：选择这一次提交包含什么；它本身不创建提交。
3. `git diff --cached --check`：检查已暂存内容中的空白等问题。
4. `git commit -m "说明"`：把暂存内容固定为**本地**版本。
5. `git push`：把本地新提交上传到已关联的远程分支。`origin` 是远程仓库的常用简称，`main` 是分支名；出现 `ahead 1` 表示本地比对应远程分支多一个提交。

不要把真实数据库密码、JWT 密钥、构建输出或 IDEA 本地配置加入提交。参考 [Git 官方：记录更改](https://git-scm.com/book/en/v2/Git-Basics-Recording-Changes-to-the-Repository)和[远程仓库](https://git-scm.com/book/en/v2/Git-Basics-Working-with-Remotes)。

## 9. 用自己的项目回答八股：自测题

| 问题 | 应能结合项目答出的要点 |
| --- | --- |
| IoC 和 DI 有什么关系？ | Spring 管理 Bean 是 IoC；通过构造方法把 `UserService` 交给 Controller 是 DI。 |
| Spring Boot 为什么引入依赖后能减少手动配置？ | Starter 提供依赖，自动配置根据类路径、配置和条件创建合适的基础 Bean；自己的业务 Bean 仍要被扫描。 |
| `@RequestBody` 和 `@Valid` 分别做什么？ | 前者读取 JSON 请求体；后者触发字段约束校验。 |
| `#{username}` 与直接拼 SQL 有什么区别？ | 前者绑定预编译参数；直接拼接不可信输入可能产生 SQL 注入。 |
| 注册前已查重，为什么还要唯一索引？ | 并发请求可能同时查到“不存在”；数据库写入约束是最终防线。 |
| BCrypt 与 JWT 各解决什么问题？ | BCrypt 用于密码摘要与核对；JWT 表示并验证登录身份。两者不能互相替代。 |
| 401 和 403 有什么区别？ | 401 关注身份凭证无效或缺失；403 关注身份已确认但权限不足。 |
| `@SpringBootTest` 通过代表什么？ | 应用上下文和当前测试覆盖的操作可运行；不代表所有业务路径都正确。 |
| `git add`、`commit`、`push` 有什么区别？ | 选择内容、建立本地提交、上传远程，分别发生在不同步骤。 |
| 为什么浏览器访问 `localhost:8080` 曾被拒绝？ | 当时没有服务监听该端口；请求还没进入 Controller，不能当作接口返回 404。 |

复习方法：先遮住右栏，用自己的话回答；再打开当前代码指出对应类、方法或 SQL。说不出代码位置时，说明还需要把概念与实践连起来。