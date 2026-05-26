# Nexus论坛项目开发提示词（登录注册完善+即时私聊模块）
## 一、项目基础信息
### 1. 项目概述
这是一个基于**Vue3+SpringBoot+MyBatis-Plus**的前后端分离论坛Web应用（项目名：Nexus），已完成核心的帖子发布、评论回复、板块管理功能。本次需要**完善用户登录注册模块**并**新增一对一即时私聊功能**，严格遵循现有项目架构和代码风格进行开发。

### 2. 技术栈
| 层级 | 技术栈 | 版本 |
|------|--------|------|
| 前端 | Vue3 + Vite + Vue Router 4 + Axios | 最新稳定版 |
| 后端 | SpringBoot 3.2.5 + MyBatis-Plus 3.5.9 | 3.2.5 / 3.5.9 |
| 数据库 | MySQL 8.0 + HikariCP连接池 | 8.0+ |
| 认证 | JWT 0.12.5 | 0.12.5 |
| 即时通讯 | Spring WebSocket | 与SpringBoot版本一致 |

### 3. 项目结构（严格遵循此结构开发）
#### 后端（nexus-backend）
```
com.hcz.nexusbackend
├── config/          # 现有配置（CorsConfig、JwtInterceptorConfig、MybatisPlusConfig）
├── controller/      # 接口层，新增UserController扩展、MessageController
├── service/         # 业务层，新增UserService扩展、MessageService
├── mapper/          # 数据访问层，新增MessageMapper
├── entity/          # 实体类，新增Message实体
├── dto/             # 数据传输对象，新增登录/注册/消息相关DTO
├── interceptor/     # 现有JWT拦截器，直接复用
├── util/            # 现有工具类（JWT工具、结果封装），直接复用
├── exception/       # 现有全局异常处理，直接复用
└── NexusBackendApplication.java  # 启动类
```

#### 前端（nexus-frontend）
```
src
├── api/             # 接口封装，新增user.js扩展、message.js
├── router/          # 路由配置，新增私聊页面路由
├── views/           # 页面组件，新增ChatView.vue，完善AuthView.vue
├── components/      # 公共组件，新增ChatMessage.vue、ChatList.vue
├── App.vue
└── main.js
```

### 4. 已完成功能（直接复用，无需重复开发）
- 用户基础登录注册接口（JWT生成与校验）
- 帖子CRUD、评论CRUD、板块管理
- 全局统一结果封装（Result类）、全局异常处理
- JWT拦截器（已配置拦截除登录注册外的所有接口）
- 跨域配置（CorsConfig）、MyBatis-Plus自动填充配置
- 前端基础页面框架（AuthView登录注册页、ForumView论坛首页）
- 前端Axios请求封装（request.js，自动携带token）

## 二、待开发任务清单
### 任务1：完善用户登录注册模块
#### 后端开发
1. **完善UserService业务逻辑**
   - 密码加密：使用Spring Security BCryptPasswordEncoder加密存储密码（现有代码未加密，必须补充）
   - 用户名唯一性校验：注册时检查用户名是否已存在
   - 登录逻辑增强：验证密码正确性，生成JWT令牌返回
   - 用户信息查询：根据token获取当前登录用户信息
   - 退出登录：前端清除token即可，后端无需额外处理
2. **完善UserController接口**
   - POST /api/user/register：用户注册接口，接收UserRegisterDTO
   - POST /api/user/login：用户登录接口，接收UserLoginDTO，返回token和用户信息
   - GET /api/user/info：获取当前登录用户信息接口（需JWT认证）
3. **新增DTO类**
   - UserLoginDTO：包含username、password字段
   - UserRegisterDTO：包含username、password、confirmPassword字段
4. **权限处理**
   - 登录注册接口放行（已在JwtInterceptorConfig配置）
   - 其他用户接口添加@PreAuthorize("isAuthenticated()")注解

#### 前端开发
1. **完善AuthView.vue登录注册页面**
   - 表单验证：用户名非空、密码长度≥6、确认密码与密码一致
   - 登录成功：保存token到localStorage，跳转到论坛首页
   - 注册成功：自动跳转到登录页
   - 错误提示：统一使用Element Plus的ElMessage组件
2. **完善api/user.js接口封装**
   - 封装login、register、getUserInfo三个接口
3. **全局路由守卫**
   - 在router/index.js中添加路由守卫，未登录用户自动跳转到登录页
   - 已登录用户禁止访问登录注册页

### 任务2：新增一对一即时私聊模块
#### 后端开发
1. **数据库设计**
   - 新建message表，执行以下SQL（可添加到database/javaweb_init.sql）：
     ```sql
     CREATE TABLE message (
         id BIGINT AUTO_INCREMENT PRIMARY KEY,
         sender_id BIGINT NOT NULL COMMENT '发送者ID',
         receiver_id BIGINT NOT NULL COMMENT '接收者ID',
         content TEXT NOT NULL COMMENT '消息内容',
         type TINYINT DEFAULT 1 COMMENT '消息类型：1-文本',
         status TINYINT DEFAULT 0 COMMENT '消息状态：0-未读，1-已读',
         create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
         INDEX idx_sender_receiver (sender_id, receiver_id),
         INDEX idx_receiver_status (receiver_id, status)
     ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
     ```
2. **新增实体类Message**
   - 对应message表字段，使用MyBatis-Plus注解
3. **新增MessageMapper**
   - 继承BaseMapper<Message>
   - 自定义方法：查询两个用户之间的历史消息、查询用户未读消息数
4. **新增MessageService**
   - 发送消息：保存消息到数据库
   - 获取历史消息：分页查询两个用户之间的聊天记录
   - 标记消息已读：将接收者的未读消息标记为已读
   - 获取未读消息数：查询当前用户的未读消息总数
5. **新增WebSocket配置**
   - 创建WebSocketConfig类，配置WebSocket端点
   - 创建WebSocketHandler类，处理连接建立、消息收发、连接关闭
   - 维护在线用户映射（userId -> Session）
6. **新增MessageController**
   - GET /api/message/history/{userId}：获取与指定用户的历史消息
   - PUT /api/message/read/{userId}：标记与指定用户的消息为已读
   - GET /api/message/unread：获取当前用户未读消息总数

#### 前端开发
1. **新增api/message.js接口封装**
   - 封装getHistory、markAsRead、getUnreadCount三个接口
2. **新增ChatView.vue私聊页面**
   - 左侧：聊天列表，显示所有有过聊天记录的用户及未读消息数
   - 右侧：聊天窗口，显示历史消息、输入框、发送按钮
3. **新增公共组件**
   - ChatMessage.vue：单条消息组件，区分发送者和接收者样式
   - ChatList.vue：聊天列表组件
4. **WebSocket连接**
   - 在App.vue中建立WebSocket连接，监听消息推送
   - 收到新消息时，更新聊天列表和聊天窗口，显示未读提示
5. **路由配置**
   - 在router/index.js中添加/chat路由，指向ChatView.vue
   - 添加导航栏入口，跳转到私聊页面

## 三、开发规范与要求
### 1. 后端开发规范
- 命名规范：类名使用大驼峰，方法名和变量名使用小驼峰，数据库表和字段使用下划线
- 接口规范：遵循RESTful风格，统一返回Result类（code=200成功，500失败）
- 异常处理：业务异常抛出BusinessException，由全局异常处理器统一处理
- 日志记录：关键业务逻辑添加日志（使用Slf4j注解）
- 注释规范：类、方法、复杂逻辑添加清晰注释

### 2. 前端开发规范
- 组件命名：使用大驼峰，页面组件以View结尾，公共组件以功能命名
- 代码风格：使用Vue3组合式API（<script setup>）
- 样式规范：使用Scoped CSS，避免全局样式污染
- 接口调用：统一使用api文件夹下的封装方法，禁止直接使用axios
- 错误处理：请求失败时使用ElMessage提示错误信息

### 3. 特别注意事项
- **严格复用现有代码**：不要重复编写JWT工具、结果封装、异常处理等已有的功能
- **权限控制**：所有私聊相关接口必须经过JWT认证，只能操作自己的消息
- **数据安全**：用户密码必须加密存储，禁止明文传输和存储
- **实时性**：私聊消息必须通过WebSocket实时推送，保证消息即时到达
- **兼容性**：代码兼容现有项目的依赖版本，不要引入新的不必要依赖

## 四、输出要求
1. 按项目结构输出完整的代码文件，每个文件单独标注路径
2. 代码包含必要的注释，关键逻辑说明清晰
3. 提供详细的运行步骤说明，包括数据库执行、前后端启动
4. 说明功能测试方法，验证登录注册和私聊功能是否正常
5. 如有需要修改的现有文件，标注修改位置和修改内容