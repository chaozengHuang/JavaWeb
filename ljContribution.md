# 工作清单 — 2026/05/26

## 一、完善用户登录注册模块

### 后端
| 文件 | 操作 | 说明 |
|------|------|------|
| `pom.xml` | 修改 | 新增 `spring-security-crypto` 依赖，提供 BCrypt 加密 |
| `dto/UserLoginDTO.java` | 新增 | 登录请求体，含 `@NotBlank` 校验 |
| `dto/UserRegisterDTO.java` | 新增 | 注册请求体，含密码长度 ≥ 6 校验 |
| `service/UserService.java` | 重写 | BCrypt 密码加密存储与验证、`getCurrentUser()` 从 ThreadLocal 获取当前用户 |
| `controller/UserController.java` | 重写 | 改用 `@RequestBody` + `@Valid` 接收 DTO、路径改为 `/api/user/*`、新增 `GET /api/user/info` |
| `config/JwtInterceptorConfig.java` | 修改 | 放行 `/api/user/login`、`/api/user/register` |

### 前端
| 文件 | 操作 | 说明 |
|------|------|------|
| `api/user.js` | 重写 | 改为 JSON body 传参、路径更新为 `/api/user/*`、新增 `getUserInfo()` |
| `views/AuthView.vue` | 重写 | 逐字段校验、修复 localStorage 存储、登录成功 emit `login-success` 事件 |
| `router/index.js` | 修改 | 新增 `beforeEach` 路由守卫（未登录→/auth，已登录→/forum） |

---

## 二、新增一对一即时私聊模块

### 后端
| 文件 | 操作 | 说明 |
|------|------|------|
| `entity/Message.java` | 新增 | 私聊消息实体，映射 `message` 表 |
| `mapper/MessageMapper.java` | 新增 | 历史消息查询、未读数统计、聊天用户列表、标记已读 |
| `service/MessageService.java` | 新增 | 发送消息、历史记录、标记已读、未读统计、聊天列表 |
| `controller/MessageController.java` | 新增 | `GET /api/message/history/{userId}`、`PUT /api/message/read/{userId}`、`GET /api/message/unread`、`GET /api/message/chat-list` |
| `config/WebSocketConfig.java` | 新增 | WebSocket 端点 `/ws/chat` |
| `handler/ChatWebSocketHandler.java` | 新增 | 连接管理、消息收发推送、在线用户 `ConcurrentHashMap` |
| `interceptor/WebSocketAuthInterceptor.java` | 新增 | WebSocket 握手时从 URL 参数校验 JWT token |
| `config/MyMetaObjectHandler.java` | 修改 | 新增 `createTime` 字段自动填充 |
| `database/javaweb_init.sql` | 修改 | 新增 `message` 表建表语句 |

### 前端
| 文件 | 操作 | 说明 |
|------|------|------|
| `api/message.js` | 新增 | `getHistory`、`markAsRead`、`getUnreadCount`、`getChatList` |
| `views/ChatView.vue` | 新增 | 左侧聊天列表 + 右侧聊天窗口，WebSocket 实时消息 |
| `components/ChatMessage.vue` | 新增 | 单条消息气泡组件，区分自己/他人样式 |
| `components/ChatList.vue` | 新增 | 聊天列表组件，显示未读 badge |
| `App.vue` | 重写 | WebSocket 连接（自动重连）、未读消息轮询、导航栏新增"私聊"入口 |

---

## 三、新增个人中心模块

### 数据库
| 变更 | 说明 |
|------|------|
| `user` 表新增 `avatar`、`bio` 列 | 头像 URL + 个人简介 |
| 新建 `post_favorite` 表 | 帖子收藏（user_id + post_id，UNIQUE） |
| 新建 `post_like` 表 | 帖子点赞（user_id + post_id，UNIQUE） |
| 新建 `browse_history` 表 | 浏览历史（user_id + post_id + browse_time） |

### 后端
| 文件 | 操作 | 说明 |
|------|------|------|
| `entity/User.java` | 修改 | 新增 `avatar`、`bio` 字段 |
| `entity/PostFavorite.java` | 新增 | 收藏实体 |
| `entity/PostLike.java` | 新增 | 点赞实体 |
| `entity/BrowseHistory.java` | 新增 | 浏览历史实体 |
| `mapper/PostFavoriteMapper.java` | 新增 | 收藏 DAO，含自定义查询方法 |
| `mapper/PostLikeMapper.java` | 新增 | 点赞 DAO，含自定义查询方法 |
| `mapper/BrowseHistoryMapper.java` | 新增 | 浏览历史 DAO，含去重排序查询 |
| `service/ProfileService.java` | 新增 | 接口定义（头像、简介、收藏、点赞、历史） |
| `service/impl/ProfileServiceImpl.java` | 新增 | 接口实现，完整的业务逻辑 |
| `controller/ProfileController.java` | 新增 | `GET/PUT /api/profile`、`POST/DELETE/GET /api/profile/favorites`、`POST/DELETE/GET /api/profile/likes`、`POST/GET /api/profile/history` |
| `config/FileUploadConfig.java` | 新增 | 静态资源映射 `/uploads/**`，头像 URL 可访问 |
| `application.properties` | 修改 | 文件上传大小 10MB + 上传目录配置 |
| `config/MyMetaObjectHandler.java` | 修改 | 新增 `browseTime` 自动填充 |

### 前端
| 文件 | 操作 | 说明 |
|------|------|------|
| `api/profile.js` | 新增 | 全部个人中心 API 封装 |
| `views/ProfileView.vue` | 重写 | 四个选项卡（基本信息 / 收藏 / 点赞 / 历史）、头像上传、简介编辑、分页列表、空状态 |

---

## 四、Bug 修复

| 问题 | 原因 | 修复 |
|------|------|------|
| `BrowseHistoryMapper` SQL 报 `No value specified for parameter 1` | `@Select` 中 `?` 占位符与 `@Param` 不兼容 | 改回 `#{userId}` 命名参数 |
| `Unknown column 'avatar' in 'field list'` | 数据库 `user` 表缺少 `avatar`、`bio` 列 | 执行 `ALTER TABLE user ADD COLUMN avatar/bio` |

---

## 五、API 接口汇总

### 用户模块 (`/api/user`)
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/user/register` | 注册（JSON body） |
| POST | `/api/user/login` | 登录（JSON body），返回 token + 用户信息 |
| GET | `/api/user/info` | 获取当前登录用户信息（需认证） |

### 私聊模块 (`/api/message`)
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/message/history/{userId}` | 获取与指定用户的历史消息 |
| PUT | `/api/message/read/{userId}` | 标记消息已读 |
| GET | `/api/message/unread` | 获取未读消息总数 |
| GET | `/api/message/chat-list` | 获取聊天用户列表 |
| WS | `ws://localhost:8080/ws/chat?token=xxx` | WebSocket 实时通讯 |

### 个人中心模块 (`/api/profile`)
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/profile` | 获取个人资料 + 统计 |
| PUT | `/api/profile/bio` | 更新个人简介 |
| POST | `/api/profile/avatar` | 上传头像（multipart） |
| POST | `/api/profile/favorites/{postId}` | 收藏/取消收藏 |
| GET | `/api/profile/favorites` | 分页获取收藏列表 |
| POST | `/api/profile/likes/{postId}` | 点赞/取消点赞 |
| GET | `/api/profile/likes` | 分页获取点赞列表 |
| POST | `/api/profile/history/{postId}` | 记录浏览历史 |
| GET | `/api/profile/history` | 分页获取浏览历史 |

---

## 六、新增路由

| 路径 | 页面 | 认证 |
|------|------|------|
| `/chat` | ChatView | 需登录 |
| `/chat/:userId` | ChatView（指定用户） | 需登录 |
| `/profile` | ProfileView（四个选项卡） | 需登录 |
