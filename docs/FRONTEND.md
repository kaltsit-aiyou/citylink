# CityLink 城邻云｜账户前端使用与学习说明

- 版本：V0.1
- 范围：注册、登录、个人资料、退出登录
- 技术：HTML、CSS、原生 JavaScript，页面由 Spring Boot 提供

## 1. 如何打开

1. 确认本机 MySQL 服务正在运行。
2. 在 IDEA 中打开 `BackendApplication.java`，点击类旁的绿色运行按钮。若旧程序已启动，先点击“停止”，再运行，目的是加载新的页面和安全配置。
3. 启动日志出现 `Started BackendApplication` 后，在浏览器打开 `http://localhost:8080`。
4. 如果页面仍旧，按 `Ctrl+F5` 重新加载。

IDEA 的运行环境需要已有的 `CITYLINK_DB_PASSWORD` 和 `CITYLINK_JWT_SECRET`。这两个值只供后端使用，前端不需要也不应得到它们。

必须通过上述 HTTP 地址访问页面。直接双击 HTML 文件会使用 `file:` 地址，页面无法按当前配置访问后端。前端不需要安装 Node.js，也不需要另外启动一个前端服务。

## 2. 各个文件负责什么

下面的路径从项目根目录 `citylink` 开始。

| 文件 | 用途 |
| --- | --- |
| `backend/src/main/resources/static/index.html` | 页面结构：文字、登录与注册表单、个人资料表单 |
| `backend/src/main/resources/static/assets/app.css` | 颜色、字号、布局，以及手机宽度下的显示方式 |
| `backend/src/main/resources/static/assets/app.js` | 表单校验、接口请求、登录状态、成功与错误提示 |
| `backend/src/main/resources/static/assets/mark.svg` | 城邻云标志 |
| `backend/src/main/java/io/github/kaltsitaiyou/citylink/auth/SecurityConfig.java` | 放行首页和静态资源，继续保护个人资料接口 |

Spring Boot 会把 `static/index.html` 作为首页。页面中 `/assets/app.js` 的开头斜杠代表当前网站的根地址，不是 Windows 磁盘路径。

## 3. 前后端如何交互

浏览器打开页面后，JavaScript 用 `fetch` 发送 HTTP 请求。后端 Controller 接收请求，UserService 处理业务，UserMapper 访问 MySQL，再把结果以 JSON 返回给页面。

| 页面操作 | 请求 | 成功后页面行为 |
| --- | --- | --- |
| 创建账户 | `POST /api/auth/register` | 提示注册成功，切换到登录表单并保留用户名 |
| 登录 | `POST /api/auth/login` | 保存本标签页的登录状态，然后读取个人资料 |
| 进入或刷新个人中心 | `GET /api/users/me` | 显示数据库中的用户名、昵称和头像地址 |
| 保存修改 | `PUT /api/users/me` | 保存昵称和头像地址，并更新页面 |
| 退出登录 | 清除浏览器中的登录状态 | 清空显示的个人资料并回到登录页 |

登录接口返回 `accessToken`、`tokenType` 和 `expiresIn`。受保护请求会附带 `Authorization: Bearer <accessToken>` 请求头。页面与接口使用相同的地址和端口，所以不需要额外的跨域配置。

浏览器按 UTF-8 发送和读取 JSON，因此中文昵称不需要手动转码。头像留空会发送 `null`，表示清除头像地址；当前只支持填写图片网址，不包含图片上传。

## 4. 代码中常见语法

| 代码 | 意义 |
| --- | --- |
| `document.querySelector('#username')` | 找到 HTML 中 `id="username"` 的元素 |
| `.value` | 读取或设置输入框内容 |
| `.textContent` | 显示普通文字，不把昵称当成 HTML 执行 |
| `addEventListener('submit', ...)` | 当用户提交表单时执行指定函数 |
| `event.preventDefault()` | 阻止表单默认刷新页面，让 JavaScript 发送请求 |
| `async` / `await` | 等待网络操作完成，同时保持页面可以响应用户 |
| `JSON.stringify(...)` | 把 JavaScript 对象转换为发送给后端的 JSON 文本 |
| `try` / `catch` / `finally` | 分别处理正常结果、异常以及必须执行的收尾操作 |
| `sessionStorage` | 为当前标签页保存登录信息，支持刷新后继续使用 |
| `@media` | 按屏幕宽度切换 CSS 布局 |

建议按 `index.html` → `app.css` → `app.js` 的顺序阅读，再对照后端 Controller 的路径查看请求去向。

## 5. 如何自己验收

1. 注册一个新用户名。用户名只能包含英文字母、数字或下划线；昵称可以使用中文。注册成功后应回到登录表单。
2. 使用同一个用户名再注册，应显示“这个用户名已经被使用”。
3. 输入错误密码登录，应显示“用户名或密码不正确”。
4. 使用正确密码登录，应进入个人中心，并显示自己的用户名。
5. 把昵称改为中文，点击“保存修改”，随后刷新页面，中文昵称应保留。
6. 将头像地址留空并保存，应显示昵称首字头像；填写可访问的图片地址并保存，应显示图片，图片无法加载时回退为昵称头像。
7. 点击“退出登录”，应回到登录页。此时直接访问 `http://localhost:8080/#profile` 也应回到登录页。
8. 缩小浏览器窗口，页面应改为上下排列，表单不应横向溢出。

想观察请求时，在浏览器按 `F12`，打开“网络（Network）”，选择 `Fetch/XHR`，再点击页面按钮。查看请求的地址、方法、状态码、请求数据和响应，可以看到页面与 Controller 的对应关系。

## 6. 登录状态的范围

令牌有效期为两小时；页面会在到期或后端返回 401 时要求重新登录。令牌保存在当前标签页的 `sessionStorage`，页面代码不保存用户密码。普通关闭标签页会结束该标签页会话，浏览器恢复会话时的行为可能不同，因此共用设备时应主动点击退出登录。

退出登录会删除前端保存的令牌。当前后端没有令牌撤销机制，因此已经复制出去的令牌在到期前仍可能有效。服务端强制退出、刷新令牌和多设备会话管理属于后续功能。

## 7. 本次验证记录

2026-11-25 已通过真实浏览器验证：注册、密码确认不一致提示、重复注册提示、错误密码提示、登录后读取资料、保存中文昵称、刷新后资料保留、显示头像图片、清除头像、退出登录后清空旧账户显示资料、退出后访问个人中心跳回登录页。

已检查 390 像素宽度下的布局，无横向溢出；首页、CSS 和 JavaScript 匿名访问返回 200，个人资料接口无令牌或无效令牌访问返回 401。

原有两个 Maven 测试通过。本次自动执行环境对测试工具的动态加载有限制，验证时通过命令行提前加载 Mockito agent；没有修改项目的 Maven 配置。你在 IDEA 中仍可按原方式运行 Maven 的 `test`。

联调使用了独立测试账户 `citylink_ui_20261125_1615`，未修改已有用户资料。头像图片加载失败、网络超时和令牌到期的处理已经实现，仍可按上述步骤进一步体验；本次没有等待两小时验证真实自然过期。
