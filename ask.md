# Nexus BBS — 前后端接口对接文档

---

## 1. 项目基础信息

| 项目 | 说明 |
|------|------|
| 项目名称 | Nexus BBS 论坛系统 |
| 后端框架 | Spring Boot 3.2.5 + MyBatis-Plus 3.5.9 |
| 前端框架 | Vue 3 (Vite) + Element-Plus + Vue-Router |
| 后端地址 | `http://localhost:8081` |
| 前端地址 | `http://localhost:5173` |

### 1.1 接口统一响应格式

除少数早期接口外，绝大部分接口返回统一的 JSON 结构 `Result<T>`：

```json
{
  "code": 200,
  "message": "success",
  "data": { ... }
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `code` | int | 业务状态码。`200` 表示成功，`400` 表示业务错误，`401` 表示未登录，`403` 表示无权限，`500` 表示服务器错误 |
| `message` | string | 提示信息，可直接展示给用户 |
| `data` | any | 响应数据，可能为对象、数组、`null` |

**前端处理建议：** 所有接口响应后先判断 `code === 200`，不为 200 时使用 `ElMessage.error(message)` 提示用户。

### 1.2 认证方式

本项目使用 **JWT（JSON Web Token）** 进行身份认证。

| 项目 | 说明 |
|------|------|
| 获取方式 | 调用登录接口，响应中的 `token` 字段即为 JWT |
| 传递方式 | 请求头 `Authorization: Bearer <token>` |
| 有效期 | 7 天（604800 秒） |
| 无需认证的接口 | `/user/login`、`/user/register` |

**JWT 载荷内容：**

| 字段 | 说明 |
|------|------|
| `userId` | 用户 ID（Long） |
| `globalRole` | 全局角色：`USER`（普通用户）或 `SYS_ADMIN`（系统管理员） |

### 1.3 跨域说明

后端已配置全局 CORS，允许任意来源跨域请求，支持携带 Cookie/Authorization 头。前端直接请求即可，无需配置代理。

---

## 2. 接口清单

---

### 2.1 用户模块

#### 2.1.1 用户注册

| 项目 | 内容 |
|------|------|
| 接口名称 | 用户注册 |
| 请求方式 | **POST** |
| 接口地址 | `/user/register` |
| Content-Type | `application/x-www-form-urlencoded` |
| 是否需要 Token | 否 |

**请求参数：**

| 字段名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| `username` | String | 是 | 用户名，不可重复 |
| `password` | String | 是 | 密码，明文传输，服务端明文存储 |

**成功响应（200）：**

```
注册成功
```

> **注意：** 此接口返回纯文本，非 `Result<T>` 格式。前端判断可用 `String(res.data).includes('成功')`。

**失败响应示例：**

```json
{
  "code": 400,
  "message": "用户名已存在",
  "data": null
}
```

---

#### 2.1.2 用户登录

| 项目 | 内容 |
|------|------|
| 接口名称 | 用户登录 |
| 请求方式 | **POST** |
| 接口地址 | `/user/login` |
| Content-Type | `application/x-www-form-urlencoded` |
| 是否需要 Token | 否 |

**请求参数：**

| 字段名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| `username` | String | 是 | 用户名 |
| `password` | String | 是 | 密码 |

**成功响应（200）：**

```json
{
  "token": "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzM4NCJ9...",
  "user": {
    "id": 5,
    "username": "testuser1",
    "password": null,
    "email": null,
    "phone": null,
    "jobNature": null,
    "location": null,
    "points": 0,
    "globalRole": "USER",
    "status": "ACTIVE",
    "createdAt": "2026-05-14T23:24:24",
    "updatedAt": "2026-05-14T23:24:24"
  }
}
```

> **注意：** 此接口返回格式为 `{ token, user }`，非 `Result<T>` 格式。前端应将 `token` 和 `user` 都存入 `localStorage`。

**失败响应示例：**

```json
{
  "code": 401,
  "message": "账号或密码错误",
  "data": null
}
```

---

#### 2.1.3 查询用户详情

| 项目 | 内容 |
|------|------|
| 接口名称 | 查询用户详情 |
| 请求方式 | **GET** |
| 接口地址 | `/user/detail` |
| 是否需要 Token | 是 |

**请求参数（Query）：**

| 字段名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| `id` | Long | 是 | 用户 ID |

**成功响应（200）：**

```json
{
  "id": 1,
  "username": "YellowToTruth",
  "password": "123456",
  "email": null,
  "phone": null,
  "jobNature": null,
  "location": null,
  "points": 9999,
  "globalRole": "SYS_ADMIN",
  "status": "ACTIVE",
  "createdAt": "2026-05-10T22:29:38",
  "updatedAt": "2026-05-10T22:29:38"
}
```

> **注意：** 此接口返回 User 对象本身，非 `Result<T>` 格式。**响应中包含 `password` 字段，前端不应展示密码。**

---

### 2.2 板块模块

#### 2.2.1 查询板块列表

| 项目 | 内容 |
|------|------|
| 接口名称 | 查询板块列表 |
| 请求方式 | **GET** |
| 接口地址 | `/board/list` |
| 是否需要 Token | 否 |

**请求参数：** 无

**成功响应（200）：**

```json
[
  {
    "id": 1,
    "name": "Java技术交流",
    "description": "讨论Java、Spring Boot相关技术",
    "creatorId": 1,
    "status": "ACTIVE",
    "createdAt": "2026-05-10T22:30:21",
    "updatedAt": "2026-05-10T22:30:21"
  },
  {
    "id": 2,
    "name": "技术交流",
    "description": "编程技术讨论区",
    "creatorId": 1,
    "status": "ACTIVE",
    "createdAt": "2026-05-14T23:03:52",
    "updatedAt": "2026-05-14T23:03:52"
  }
]
```

> **注意：** 此接口返回数组，非 `Result<T>` 格式。

---

#### 2.2.2 创建板块

| 项目 | 内容 |
|------|------|
| 接口名称 | 创建板块 |
| 请求方式 | **POST** |
| 接口地址 | `/board/create` |
| Content-Type | `application/x-www-form-urlencoded` |
| 是否需要 Token | 否 |

**请求参数：**

| 字段名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| `name` | String | 是 | 板块名称 |
| `description` | String | 否 | 板块描述 |
| `creatorId` | Long | 是 | 创建者用户 ID |

**成功响应（200）：**

```json
{
  "id": 3,
  "name": "技术交流",
  "description": "编程技术讨论区",
  "creatorId": 1,
  "status": "ACTIVE",
  "createdAt": "2026-05-14T23:26:03",
  "updatedAt": "2026-05-14T23:26:03"
}
```

> **注意：** 此接口返回 Board 对象，非 `Result<T>` 格式。

---

### 2.3 帖子模块

#### 2.3.1 发布帖子

| 项目 | 内容 |
|------|------|
| 接口名称 | 发布帖子 |
| 请求方式 | **POST** |
| 接口地址 | `/post` |
| Content-Type | `application/json` |
| 是否需要 Token | 是 |

**请求参数（JSON Body）：**

| 字段名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| `title` | String | 是 | 标题，最长 100 字符 |
| `content` | String | 是 | 正文内容 |
| `boardId` | Long | 否 | 板块 ID，不传默认 `1` |
| `type` | String | 否 | 帖子类型：`NORMAL`（普通）/ `REWARD`（悬赏），不传默认 `NORMAL` |
| `rewardPoints` | Integer | 否 | 悬赏积分，仅 type=REWARD 时有意义，发帖时从用户余额扣除 |

**请求示例：**

```json
{
  "title": "Spring Boot 启动报错求助",
  "content": "项目启动时报 UnsatisfiedDependencyException ...",
  "boardId": 1,
  "type": "NORMAL"
}
```

**成功响应（200）：**

```json
{
  "code": 200,
  "message": "发帖成功",
  "data": {
    "id": 4,
    "boardId": 1,
    "authorId": 5,
    "title": "Spring Boot 启动报错求助",
    "content": "项目启动时报 UnsatisfiedDependencyException ...",
    "type": "NORMAL",
    "rewardPoints": 0,
    "isPinned": 0,
    "isGlobalPinned": 0,
    "isFeatured": 0,
    "status": "NORMAL",
    "createdAt": "2026-05-14T23:30:00",
    "updatedAt": "2026-05-14T23:30:00"
  }
}
```

**失败响应：**

```json
{
  "code": 400,
  "message": "标题不能为空",
  "data": null
}
```

```json
{
  "code": 400,
  "message": "积分不足",
  "data": null
}
```

---

#### 2.3.2 帖子列表

| 项目 | 内容 |
|------|------|
| 接口名称 | 帖子列表（分页） |
| 请求方式 | **GET** |
| 接口地址 | `/post` |
| 是否需要 Token | 是 |

**请求参数（Query）：**

| 字段名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| `page` | Integer | 否 | 页码，默认 `1` |
| `size` | Integer | 否 | 每页条数，默认 `10` |
| `boardId` | Long | 否 | 按板块筛选 |
| `keyword` | String | 否 | 关键词搜索（匹配标题和正文） |
| `status` | String | 否 | 按状态筛选（不传则自动排除已删除帖子） |
| `type` | String | 否 | 按类型筛选：`NORMAL` / `REWARD` |

**成功响应（200）：**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [
      {
        "id": 4,
        "boardId": 1,
        "authorId": 5,
        "title": "Spring Boot 启动报错求助",
        "content": "项目启动时报 UnsatisfiedDependencyException ...",
        "type": "NORMAL",
        "rewardPoints": 0,
        "isPinned": 0,
        "isGlobalPinned": 0,
        "isFeatured": 0,
        "status": "NORMAL",
        "createdAt": "2026-05-14T23:30:00",
        "updatedAt": "2026-05-14T23:30:00"
      }
    ],
    "total": 1,
    "size": 10,
    "current": 1,
    "pages": 1
  }
}
```

| 分页字段 | 类型 | 说明 |
|----------|------|------|
| `records` | Array | 当前页数据 |
| `total` | Long | 总记录数 |
| `size` | Integer | 每页条数 |
| `current` | Long | 当前页码 |
| `pages` | Long | 总页数 |

---

#### 2.3.3 帖子详情

| 项目 | 内容 |
|------|------|
| 接口名称 | 帖子详情 |
| 请求方式 | **GET** |
| 接口地址 | `/post/{id}` |
| 是否需要 Token | 是 |

**路径参数：**

| 字段名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| `id` | Long | 是 | 帖子 ID |

**成功响应（200）：**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 4,
    "boardId": 1,
    "authorId": 5,
    "title": "Spring Boot 启动报错求助",
    "content": "...",
    "type": "NORMAL",
    "rewardPoints": 0,
    "isPinned": 0,
    "isGlobalPinned": 0,
    "isFeatured": 0,
    "status": "NORMAL",
    "createdAt": "2026-05-14T23:30:00",
    "updatedAt": "2026-05-14T23:30:00"
  }
}
```

**失败响应：**

```json
{
  "code": 400,
  "message": "帖子不存在",
  "data": null
}
```

---

#### 2.3.4 修改帖子

| 项目 | 内容 |
|------|------|
| 接口名称 | 修改帖子 |
| 请求方式 | **PUT** |
| 接口地址 | `/post/{id}` |
| Content-Type | `application/json` |
| 是否需要 Token | 是 |

**路径参数：**

| 字段名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| `id` | Long | 是 | 帖子 ID |

**请求参数（JSON Body）：**

| 字段名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| `title` | String | 是 | 标题，最长 100 字符 |
| `content` | String | 是 | 正文内容 |

**请求示例：**

```json
{
  "title": "修改后的标题",
  "content": "修改后的内容。"
}
```

**成功响应（200）：**

```json
{
  "code": 200,
  "message": "修改成功",
  "data": null
}
```

> **权限说明：** 仅帖子作者本人或管理员可以修改。

---

#### 2.3.5 删除帖子（软删除）

| 项目 | 内容 |
|------|------|
| 接口名称 | 删除帖子 |
| 请求方式 | **DELETE** |
| 接口地址 | `/post/{id}` |
| 是否需要 Token | 是 |

**路径参数：**

| 字段名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| `id` | Long | 是 | 帖子 ID |

**成功响应（200）：**

```json
{
  "code": 200,
  "message": "删除成功",
  "data": null
}
```

> **说明：** 此接口为软删除，仅将状态标记为 `DELETED`，普通列表查询不会展示。仅帖子作者本人或管理员可操作。

---

#### 2.3.6 板块置顶

| 项目 | 内容 |
|------|------|
| 接口名称 | 板块置顶 |
| 请求方式 | **PUT** |
| 接口地址 | `/post/{id}/pin` |
| 是否需要 Token | 是（管理员） |

**路径参数：**

| 字段名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| `id` | Long | 是 | 帖子 ID |

**请求参数（Query）：**

| 字段名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| `status` | Integer | 是 | `1` = 置顶，`0` = 取消置顶 |

**成功响应（200）：**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": null
}
```

---

#### 2.3.7 全局置顶

| 项目 | 内容 |
|------|------|
| 接口名称 | 全局置顶 |
| 请求方式 | **PUT** |
| 接口地址 | `/post/{id}/global-pin` |
| 是否需要 Token | 是（管理员） |

**请求参数（Query）：**

| 字段名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| `status` | Integer | 是 | `1` = 全局置顶，`0` = 取消 |

---

#### 2.3.8 设置精华

| 项目 | 内容 |
|------|------|
| 接口名称 | 设置精华 |
| 请求方式 | **PUT** |
| 接口地址 | `/post/{id}/feature` |
| 是否需要 Token | 是（管理员） |

**请求参数（Query）：**

| 字段名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| `status` | Integer | 是 | `1` = 设精，`0` = 取消 |

---

### 2.4 评论模块

#### 2.4.1 发表评论

| 项目 | 内容 |
|------|------|
| 接口名称 | 发表评论 |
| 请求方式 | **POST** |
| 接口地址 | `/comment` |
| Content-Type | `application/json` |
| 是否需要 Token | 是 |

**请求参数（JSON Body）：**

| 字段名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| `postId` | Long | 是 | 帖子 ID |
| `content` | String | 是 | 评论内容 |
| `parentCommentId` | Long | 否 | 父评论 ID，用于回复某条评论 |

**请求示例（直接评论）：**

```json
{
  "postId": 4,
  "content": "检查一下 application.properties 里的数据库配置是否正确"
}
```

**请求示例（回复评论）：**

```json
{
  "postId": 4,
  "content": "谢谢，已解决！",
  "parentCommentId": 1
}
```

**成功响应（200）：**

```json
{
  "code": 200,
  "message": "评论成功",
  "data": {
    "id": 2,
    "postId": 4,
    "authorId": 5,
    "content": "检查一下 application.properties 里的数据库配置是否正确",
    "parentCommentId": null,
    "isAccepted": 0,
    "status": "NORMAL",
    "createdAt": "2026-05-14T23:35:00",
    "updatedAt": "2026-05-14T23:35:00"
  }
}
```

---

#### 2.4.2 查询评论列表

| 项目 | 内容 |
|------|------|
| 接口名称 | 查询评论列表 |
| 请求方式 | **GET** |
| 接口地址 | `/comment` |
| 是否需要 Token | 是 |

**请求参数（Query）：**

| 字段名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| `postId` | Long | 是 | 帖子 ID |

**成功响应（200）：**

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "postId": 4,
      "authorId": 5,
      "content": "检查一下 application.properties 里的数据库配置是否正确",
      "parentCommentId": null,
      "isAccepted": 0,
      "status": "NORMAL",
      "createdAt": "2026-05-14T23:35:00",
      "updatedAt": "2026-05-14T23:35:00"
    },
    {
      "id": 2,
      "postId": 4,
      "authorId": 6,
      "content": "谢谢，已解决！",
      "parentCommentId": 1,
      "isAccepted": 0,
      "status": "NORMAL",
      "createdAt": "2026-05-14T23:36:00",
      "updatedAt": "2026-05-14T23:36:00"
    }
  ]
}
```

> **前端提示：** `parentCommentId` 不为 null 表示这是对某条评论的回复。可通过此字段构建评论嵌套或 @回复 样式。

---

#### 2.4.3 采纳评论

| 项目 | 内容 |
|------|------|
| 接口名称 | 采纳评论 |
| 请求方式 | **PUT** |
| 接口地址 | `/comment/{id}/accept` |
| 是否需要 Token | 是 |

**路径参数：**

| 字段名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| `id` | Long | 是 | 评论 ID |

**成功响应（200）：**

```json
{
  "code": 200,
  "message": "采纳成功",
  "data": null
}
```

> **说明：** 采纳评论后，评论者将获得对应帖子的悬赏积分。

---

#### 2.4.4 删除评论（软删除）

| 项目 | 内容 |
|------|------|
| 接口名称 | 删除评论 |
| 请求方式 | **DELETE** |
| 接口地址 | `/comment/{id}` |
| 是否需要 Token | 是 |

**路径参数：**

| 字段名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| `id` | Long | 是 | 评论 ID |

> **权限说明：** 仅评论作者本人或管理员可以删除。

---

### 2.5 管理员模块

> **权限要求：** 所有管理员接口需要 `globalRole = SYS_ADMIN`，普通用户调用返回 403。

#### 2.5.1 管理员查询帖子

| 项目 | 内容 |
|------|------|
| 接口名称 | 管理员查询帖子（含已删除） |
| 请求方式 | **GET** |
| 接口地址 | `/admin/post` |
| 是否需要 Token | 是（管理员） |

**请求参数（Query）：**

| 字段名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| `page` | Integer | 否 | 页码，默认 `1` |
| `size` | Integer | 否 | 每页条数，默认 `10` |
| `keyword` | String | 否 | 关键词搜索 |
| `status` | String | 否 | 按状态筛选（可查 `DELETED` 帖子） |
| `type` | String | 否 | 按类型筛选 |
| `boardId` | Long | 否 | 按板块筛选 |

---

#### 2.5.2 管理员修改帖子状态

| 项目 | 内容 |
|------|------|
| 接口名称 | 管理员修改帖子状态 |
| 请求方式 | **PUT** |
| 接口地址 | `/admin/post/{id}/status` |
| 是否需要 Token | 是（管理员） |

**请求参数（Query）：**

| 字段名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| `status` | String | 是 | 目标状态：`ACTIVE` / `DELETED` 等 |

---

#### 2.5.3 管理员物理删除帖子

| 项目 | 内容 |
|------|------|
| 接口名称 | 管理员物理删除帖子 |
| 请求方式 | **DELETE** |
| 接口地址 | `/admin/post/{id}` |
| 是否需要 Token | 是（管理员） |

> **说明：** 此操作从数据库彻底删除帖子，不可恢复。与普通删除（软删除）不同。

---

#### 2.5.4 管理员查询评论

| 项目 | 内容 |
|------|------|
| 接口名称 | 管理员查询评论 |
| 请求方式 | **GET** |
| 接口地址 | `/admin/comment` |
| 是否需要 Token | 是（管理员） |

**请求参数（Query）：**

| 字段名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| `page` | Integer | 否 | 页码，默认 `1` |
| `size` | Integer | 否 | 每页条数，默认 `10` |
| `keyword` | String | 否 | 关键词搜索 |
| `status` | String | 否 | 按状态筛选 |
| `postId` | Long | 否 | 按帖子筛选 |

---

#### 2.5.5 管理员物理删除评论

| 项目 | 内容 |
|------|------|
| 接口名称 | 管理员物理删除评论 |
| 请求方式 | **DELETE** |
| 接口地址 | `/admin/comment/{id}` |
| 是否需要 Token | 是（管理员） |

---

## 3. DTO 与数据结构说明

### 3.1 请求 DTO

#### PostCreateDTO（发帖请求）

| 字段名 | 类型 | 校验规则 | 说明 |
|--------|------|----------|------|
| `title` | String | `@NotBlank`，最长 100 字符 | 帖子标题 |
| `content` | String | `@NotBlank` | 帖子正文 |
| `boardId` | Long | 可选 | 板块 ID，默认 1 |
| `type` | String | 可选 | `NORMAL` / `REWARD` |
| `rewardPoints` | Integer | 可选 | 悬赏积分 |

#### PostUpdateDTO（修改帖子请求）

| 字段名 | 类型 | 校验规则 | 说明 |
|--------|------|----------|------|
| `title` | String | `@NotBlank`，最长 100 字符 | 帖子标题 |
| `content` | String | `@NotBlank` | 帖子正文 |

#### CommentCreateDTO（评论请求）

| 字段名 | 类型 | 校验规则 | 说明 |
|--------|------|----------|------|
| `postId` | Long | `@NotNull` | 帖子 ID |
| `content` | String | `@NotBlank` | 评论内容 |
| `parentCommentId` | Long | 可选 | 父评论 ID（回复时传入） |

### 3.2 响应实体字段说明

#### User（用户信息）

| 字段名 | 类型 | 说明 |
|--------|------|------|
| `id` | Long | 用户 ID |
| `username` | String | 用户名 |
| `password` | String | 密码（**前端请勿展示，生产环境应排除此字段**） |
| `email` | String | 邮箱 |
| `phone` | String | 手机号 |
| `jobNature` | String | 职业 |
| `location` | String | 所在地 |
| `points` | Integer | 积分余额 |
| `globalRole` | String | 角色：`USER` / `SYS_ADMIN` |
| `status` | String | 状态：`ACTIVE` / 其他 |
| `createdAt` | String | 注册时间（ISO 8601） |
| `updatedAt` | String | 更新时间（ISO 8601） |

#### Post（帖子）

| 字段名 | 类型 | 说明 |
|--------|------|------|
| `id` | Long | 帖子 ID |
| `boardId` | Long | 所属板块 ID |
| `authorId` | Long | 作者用户 ID |
| `title` | String | 标题 |
| `content` | String | 正文 |
| `type` | String | 类型：`NORMAL` / `REWARD` |
| `rewardPoints` | Integer | 悬赏积分 |
| `isPinned` | Integer | 板块置顶：`1`=是，`0`=否 |
| `isGlobalPinned` | Integer | 全局置顶：`1`=是，`0`=否 |
| `isFeatured` | Integer | 精华：`1`=是，`0`=否 |
| `status` | String | 状态：`NORMAL` / `DELETED` |
| `createdAt` | String | 发布时间（ISO 8601） |
| `updatedAt` | String | 更新时间（ISO 8601） |

#### Comment（评论）

| 字段名 | 类型 | 说明 |
|--------|------|------|
| `id` | Long | 评论 ID |
| `postId` | Long | 所属帖子 ID |
| `authorId` | Long | 作者用户 ID |
| `content` | String | 评论内容 |
| `parentCommentId` | Long | 父评论 ID，`null` 表示直接评论帖子 |
| `isAccepted` | Integer | 是否被采纳：`1`=是，`0`=否 |
| `status` | String | 状态：`NORMAL` / `DELETED` |
| `createdAt` | String | 发布时间（ISO 8601） |
| `updatedAt` | String | 更新时间（ISO 8601） |

#### Board（板块）

| 字段名 | 类型 | 说明 |
|--------|------|------|
| `id` | Long | 板块 ID |
| `name` | String | 板块名称 |
| `description` | String | 板块描述 |
| `creatorId` | Long | 创建者用户 ID |
| `status` | String | 状态 |
| `createdAt` | String | 创建时间 |
| `updatedAt` | String | 更新时间 |

---

## 4. 错误码与异常说明

| code | 含义 | 触发场景 | 前端处理建议 |
|------|------|----------|-------------|
| **200** | 成功 | 正常响应 | 读取 `data` 字段展示 |
| **400** | 业务错误 / 参数校验失败 | 用户名重复、帖子不存在、积分不足、校验不通过等 | `ElMessage.error(message)` |
| **401** | 未登录 / Token 无效 | Token 过期、未传 Token、账号密码错误 | 跳转登录页，清除本地 token |
| **403** | 无权限 | 普通用户调用管理员接口、非作者修改/删除帖子 | 提示"无权限" |
| **500** | 服务器内部错误 | 未预期的异常 | 提示"服务器错误，请稍后重试" |

**常见的 `message` 内容：**

| message | 说明 |
|---------|------|
| `用户名已存在` | 注册时用户名重复 |
| `账号或密码错误` | 登录失败（401） |
| `请先登录` | 未携带有效 Token（401） |
| `无管理员权限` | 非管理员调用管理接口（403） |
| `无权修改` / `无权删除` | 非作者操作他人帖子/评论（403） |
| `帖子不存在` | 帖子已删除或不存在（400） |
| `积分不足` | 发悬赏帖时余额不够（400） |
| `标题不能为空` / `内容不能为空` | 参数校验不通过（400） |

---

## 5. 前端开发注意事项

### 5.1 JWT Token 管理

```javascript
// 登录成功后保存
const res = await axios.post('/user/login', params)
localStorage.setItem('token', res.data.token)
localStorage.setItem('user', JSON.stringify(res.data.user))

// 请求拦截器自动携带
axios.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})
```

### 5.2 Token 过期处理

Token 有效期 7 天。过期后后端不会主动返回 401（当前实现中，无效 Token 会被静默忽略，接口表现同未登录）。

建议在前端做以下处理：

```javascript
axios.interceptors.response.use(
  response => response,
  error => {
    if (error.response?.data?.code === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      router.push('/auth')
      ElMessage.error('登录已过期，请重新登录')
    }
    return Promise.reject(error)
  }
)
```

### 5.3 统一响应格式处理

大部分接口返回 `Result<T>` 格式，但以下接口例外：

| 例外接口 | 返回格式 | 处理方式 |
|----------|----------|----------|
| `/user/register` | 纯文本 `"注册成功"` | `String(res.data).includes('成功')` |
| `/user/login` | `{ token, user }` | 直接取 `res.data.token` |
| `/user/detail` | User 对象 | 直接取 `res.data` |
| `/board/list` | Board 数组 | 直接取 `res.data` |
| `/board/create` | Board 对象 | 直接取 `res.data` |

**建议封装一个统一的响应处理函数：**

```javascript
// 对 /user/login 和 /user/register 单独处理
// 其余接口统一用 Result 格式解析
function handleResponse(res) {
  if (res.data && typeof res.data.code === 'number') {
    // Result<T> 格式
    if (res.data.code === 200) return res.data.data
    ElMessage.error(res.data.message)
    return null
  }
  return res.data // 非 Result 格式，直接返回
}
```

### 5.4 权限控制

- 帖子列表中的 `isPinned`、`isGlobalPinned`、`isFeatured` 字段用于展示置顶/精华标签
- 管理员操作（置顶、精华、物理删除等）的按钮应根据 `user.globalRole === 'SYS_ADMIN'` 来控制显示
- 帖子的修改/删除按钮应根据 `post.authorId === user.id` 或 `user.globalRole === 'SYS_ADMIN'` 来控制显示

### 5.5 软删除说明

- 普通用户删除帖子/评论为软删除（仅标记 `status = DELETED`）
- 管理员通过 `/admin/` 前缀的接口可进行物理删除（彻底删除，不可恢复）
- 列表接口自动过滤 `status = DELETED` 的记录，除非显式传入 `status` 参数

---

## 6. 接口速查表

| 序号 | 方法 | 路径 | 说明 | Token | Admin |
|------|------|------|------|-------|-------|
| 1 | POST | `/user/register` | 注册 | — | — |
| 2 | POST | `/user/login` | 登录获取 Token | — | — |
| 3 | GET | `/user/detail?id=` | 用户详情 | 需要 | — |
| 4 | GET | `/board/list` | 板块列表 | — | — |
| 5 | POST | `/board/create` | 创建板块 | — | — |
| 6 | POST | `/post` | 发布帖子 | 需要 | — |
| 7 | GET | `/post` | 帖子列表（分页） | 需要 | — |
| 8 | GET | `/post/{id}` | 帖子详情 | 需要 | — |
| 9 | PUT | `/post/{id}` | 修改帖子 | 需要 | — |
| 10 | DELETE | `/post/{id}` | 删除帖子（软删） | 需要 | — |
| 11 | PUT | `/post/{id}/pin` | 板块置顶 | 需要 | 需要 |
| 12 | PUT | `/post/{id}/global-pin` | 全局置顶 | 需要 | 需要 |
| 13 | PUT | `/post/{id}/feature` | 设置精华 | 需要 | 需要 |
| 14 | POST | `/comment` | 发表评论 | 需要 | — |
| 15 | GET | `/comment?postId=` | 评论列表 | 需要 | — |
| 16 | PUT | `/comment/{id}/accept` | 采纳评论 | 需要 | — |
| 17 | DELETE | `/comment/{id}` | 删除评论（软删） | 需要 | — |
| 18 | GET | `/admin/post` | 管理员查帖子 | 需要 | 需要 |
| 19 | PUT | `/admin/post/{id}/status` | 修改帖子状态 | 需要 | 需要 |
| 20 | DELETE | `/admin/post/{id}` | 物理删除帖子 | 需要 | 需要 |
| 21 | GET | `/admin/comment` | 管理员查评论 | 需要 | 需要 |
| 22 | DELETE | `/admin/comment/{id}` | 物理删除评论 | 需要 | 需要 |

---

*文档说明：此文档为 Nexus BBS 当前版本的完整接口约定，后续接口变更会同步更新。如有疑问请联系后端开发。*
