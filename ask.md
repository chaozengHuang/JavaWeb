### 第一步：后端重构指令（复制发给 Agent）

*请先确保你已经在本地 MySQL 里执行了我上一条回复中的那些 `ALTER TABLE` 和 `CREATE TABLE` 的 SQL 语句。*

**👇 复制以下完整内容发给你的后端 Agent：**

> 你好，我是后端开发。当前项目是 Spring Boot 3 + MyBatis，运行在 8081 端口。为了满足 BBS 论坛的新需求，我的数据库表结构已经进行了升级。因为你无法访问我的数据库，请参考以下最新的表结构：
> 
> ```sql
> -- 1. 用户表 (新增了phone, job_nature, work_location, points积分, role角色)
> CREATE TABLE `user` (
>   `id` int NOT NULL AUTO_INCREMENT PRIMARY KEY,
>   `username` varchar(50) NOT NULL UNIQUE,
>   `password` varchar(255) NOT NULL,
>   `phone` varchar(20),
>   `job_nature` varchar(50),
>   `work_location` varchar(100),
>   `points` int DEFAULT 100,
>   `role` int DEFAULT 0 -- 0普通用户，1管理员
> );
> 
> -- 2. 帖子表 (新增了board_id所属板块, is_top置顶, is_elite加精, reward_points悬赏积分)
> CREATE TABLE `post` (
>   `id` int NOT NULL AUTO_INCREMENT PRIMARY KEY,
>   `user_id` int NOT NULL,
>   `title` varchar(100) NOT NULL,
>   `content` text NOT NULL,
>   `board_id` int NOT NULL DEFAULT 1,
>   `is_top` tinyint(1) DEFAULT 0,
>   `is_elite` tinyint(1) DEFAULT 0,
>   `reward_points` int DEFAULT 0
> );
> 
> -- 3. 评论表 (新增了is_accepted采纳状态)
> CREATE TABLE `comment` (
>   `id` int NOT NULL AUTO_INCREMENT PRIMARY KEY,
>   `post_id` int NOT NULL,
>   `user_id` int NOT NULL,
>   `content` text NOT NULL,
>   `is_accepted` tinyint(1) DEFAULT 0
> );
> 
> -- 4. 板块表 (全新表)
> CREATE TABLE `board` (
>   `id` int NOT NULL AUTO_INCREMENT PRIMARY KEY,
>   `name` varchar(50) NOT NULL,
>   `description` varchar(255)
> );
> ```
> 
> 请帮我修改并补充现有的 `entity`, `mapper`, `service`, `controller` 代码：
> 
> **任务 1：实体类更新**
> 更新 User, Post, Comment 实体类，加入上述新增字段，并创建 Board 实体类。生成对应的 Getter/Setter。
> 
> **任务 2：Board 模块开发 (板块管理)**
> 创建 BoardMapper, BoardService, BoardController。提供获取所有板块列表的接口 `GET /board/list`，以及添加板块的接口 `POST /board/create`。
> 
> **任务 3：User 模块增强 (个人资料)**
> 在 UserMapper 和 UserController 中新增 `POST /user/updateProfile` 接口，接收 id, phone, jobNature, workLocation 参数，更新用户资料。
> 
> **任务 4：Post 模块核心重构 (发帖、修改、置顶加精)**
> 1. 重构发帖接口：`POST /post/create` 需接收 boardId 和 rewardPoints。在 Service 层判断，如果 rewardPoints > 0，需要同时扣除该用户的 points 积分。
> 2. 新增修改接口：`POST /post/update`，接收 postId, userId, title, content。在 Service 层必须判断：只有当操作的 userId 等于帖子的 user_id，或者该 userId 的 role == 1 时，才允许修改。
> 3. 新增管理员操作接口：`POST /post/setTop` 和 `POST /post/setElite`（接收 postId 和 status状态值）。
> 
> **任务 5：Comment 模块增强 (悬赏采纳)**
> 新增采纳接口 `POST /comment/accept`。接收 commentId。在 Service 层逻辑为：将该评论 is_accepted 设为 1，并根据该评论对应帖子的 reward_points，将积分增加给该评论的作者 (user_id)。
> 
> *注意：所有的 Mapper 依然使用注解编写 SQL；所有 POST 请求的参数依然使用 `@RequestParam` 接收（非 JSON）。*请一步步给出修改后的代码。

---

### 第二步：前端重构指令（后端跑通后再发）

前端的改动量比较大，主要是增加了“导航栏”来切换不同页面（论坛、个人中心、管理员）。

**👇 复制以下完整内容发给你的前端 Agent：**

> 你好，我是前端开发。后端的 BBS 论坛接口已经重构完毕（运行在 localhost:8081，接口传参一律使用 `URLSearchParams` 表单格式）。现在请帮我重构 Vue 3 + Element-Plus 的前端项目：
> 
> **任务 1：全局布局与路由配置**
> 1. 修改 `App.vue`，加入顶部的 `el-menu` 导航栏，包含菜单项：【论坛大厅】、【个人中心】。
> 2. 修改 `src/router/index.js`，配置以下路由：
>    - `/auth` -> 登录注册页 (AuthView.vue)
>    - `/forum` -> 论坛大厅 (ForumView.vue)
>    - `/profile` -> 个人中心 (ProfileView.vue)
> *注：为了方便测试，当前登录的用户 ID 和 Role 请暂时使用 `localStorage` 保存并在各个页面读取。登录成功后将用户信息存入 localStorage。*
> 
> **任务 2：重构论坛大厅 (ForumView.vue)**
> 1. **板块筛选**：在页面左侧或顶部增加一个板块列表（调用 `GET /board/list`），点击不同板块可切换帖子列表。
> 2. **帖子列表展示优化**：在 `el-card` 上，根据返回的数据展示 `el-tag`，如果 `is_top == 1` 显示红色的“置顶”，`is_elite == 1` 显示黄色的“精华”。如果是悬赏贴，展示“悬赏：XX积分”。
> 3. **发帖功能**：发帖弹窗的表单需增加“选择板块(el-select)”和“悬赏积分(el-input-number)”字段。
> 4. **文章修改与管理**：在帖子卡片下方，如果当前登录的 user.id == post.userId 或者 role == 1，显示“编辑”按钮。如果 role == 1，额外显示“置顶”和“加精”按钮。
> 5. **评论采纳**：在查看评论的弹窗/抽屉中，如果当前登录用户是该帖子的作者，且该帖子有悬赏分，在每条评论旁显示“采纳”按钮（调用 `/comment/accept`）。已被采纳的评论显示绿色标记。
> 
> **任务 3：新建个人中心 (ProfileView.vue)**
> 创建该页面，使用 `el-descriptions` 或 `el-form` 展示用户的基本信息、当前剩余积分。并提供一个表单，允许用户修改：联系电话、工作性质、工作地点。点击保存发送请求到 `/user/updateProfile`。
> 
> 请分步骤提供 `router/index.js`, `App.vue`, `ForumView.vue`, 以及新建的 `ProfileView.vue` 的完整 Vue 3 代码。记得处理 axios 的 `URLSearchParams` 以避免 400 错误。

---