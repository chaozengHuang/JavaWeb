create table admin_log
(
    id             bigint auto_increment
        primary key,
    admin_id       bigint                             not null comment '操作管理员ID',
    admin_username varchar(50)                        not null comment '操作管理员用户名',
    action         varchar(50)                        not null comment '操作类型: RESET_PASSWORD/BAN_USER/UNBAN_USER/UPDATE_POST_STATUS/UPDATE_COMMENT_STATUS/BATCH_UPDATE_POST_STATUS/BATCH_UPDATE_COMMENT_STATUS',
    target_type    varchar(20)                        not null comment '操作对象类型: USER/POST/COMMENT',
    target_id      bigint                             null comment '操作对象ID(批量操作时为NULL)',
    detail         varchar(500)                       null comment '操作详情描述',
    created_at     datetime default CURRENT_TIMESTAMP not null comment '操作时间'
)
    comment '管理员操作日志表';

create index idx_admin_id
    on admin_log (admin_id);

create index idx_created_at
    on admin_log (created_at);

create table board
(
    id          bigint auto_increment
        primary key,
    name        varchar(50)                           not null comment '板块名称',
    description varchar(255)                          null comment '板块描述/规则',
    creator_id  bigint                                not null comment '创建者(版主)ID',
    status      varchar(20) default 'ACTIVE'          not null comment '状态: ACTIVE(正常), HIDDEN(隐藏)',
    created_at  datetime    default CURRENT_TIMESTAMP not null comment '创建时间',
    updated_at  datetime    default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间'
)
    comment '板块表';

create index idx_creator
    on board (creator_id);

create table browse_history
(
    id          bigint auto_increment
        primary key,
    user_id     bigint                             not null comment '用户ID',
    post_id     bigint                             not null comment '帖子ID',
    browse_time datetime default CURRENT_TIMESTAMP not null comment '浏览时间'
)
    comment '浏览历史表';

create index idx_history_post
    on browse_history (post_id);

create index idx_history_user
    on browse_history (user_id asc, browse_time desc);

create table comment
(
    id                bigint auto_increment
        primary key,
    post_id           bigint                                not null comment '所属帖子ID',
    author_id         bigint                                not null comment '评论人ID',
    content           text                                  not null comment '评论内容',
    parent_comment_id bigint                                null comment '父评论ID(如果为空则是直接回复帖子，有值则是楼中楼回复)',
    is_accepted       tinyint(1)  default 0                 not null comment '是否被题主采纳(仅针对悬赏帖，0-否, 1-是)',
    status            varchar(20) default 'ACTIVE'          not null comment '状态: ACTIVE(正常), DELETED(已删除)',
    created_at        datetime    default CURRENT_TIMESTAMP not null comment '评论时间',
    updated_at        datetime    default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间'
)
    comment '评论回复表';

create index idx_author_id
    on comment (author_id);

create index idx_post_id
    on comment (post_id);

create table message
(
    id          bigint auto_increment
        primary key,
    sender_id   bigint                             not null comment '发送者ID',
    receiver_id bigint                             not null comment '接收者ID',
    content     text                               not null comment '消息内容',
    type        tinyint  default 1                 not null comment '消息类型: 1-文本',
    status      tinyint  default 0                 not null comment '消息状态: 0-未读, 1-已读',
    create_time datetime default CURRENT_TIMESTAMP not null comment '发送时间'
)
    comment '私聊消息表';

create index idx_receiver_status
    on message (receiver_id, status);

create index idx_sender_receiver
    on message (sender_id, receiver_id);

create table post
(
    id               bigint auto_increment
        primary key,
    board_id         bigint                                not null comment '所属板块ID',
    author_id        bigint                                not null comment '发帖人ID',
    title            varchar(150)                          not null comment '帖子标题',
    content          longtext                              not null comment '帖子正文',
    type             varchar(20) default 'NORMAL'          not null comment '帖子类型: NORMAL(普通贴), REWARD(需求悬赏贴)',
    reward_points    int         default 0                 not null comment '悬赏积分(普通贴为0)',
    is_pinned        tinyint(1)  default 0                 not null comment '是否板块内置顶 (0-否, 1-是)',
    is_global_pinned tinyint(1)  default 0                 not null comment '是否全局置顶 (0-否, 1-是)',
    is_featured      tinyint(1)  default 0                 not null comment '是否精华帖 (0-否, 1-是)',
    status           varchar(20) default 'ACTIVE'          not null comment '状态: ACTIVE(正常), DELETED(已删除)',
    created_at       datetime    default CURRENT_TIMESTAMP not null comment '发帖时间',
    updated_at       datetime    default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间'
)
    comment '帖子表';

create index idx_author_id
    on post (author_id);

create index idx_board_id
    on post (board_id);

create index idx_created_at
    on post (created_at);

create table post_favorite
(
    id          bigint auto_increment
        primary key,
    user_id     bigint                             not null comment '用户ID',
    post_id     bigint                             not null comment '帖子ID',
    create_time datetime default CURRENT_TIMESTAMP not null comment '收藏时间',
    constraint uk_fav_user_post
        unique (user_id, post_id)
)
    comment '帖子收藏表';

create index idx_fav_post_id
    on post_favorite (post_id);

create index idx_fav_user_id
    on post_favorite (user_id);

create table post_like
(
    id          bigint auto_increment
        primary key,
    user_id     bigint                             not null comment '用户ID',
    post_id     bigint                             not null comment '帖子ID',
    create_time datetime default CURRENT_TIMESTAMP not null comment '点赞时间',
    constraint uk_like_user_post
        unique (user_id, post_id)
)
    comment '帖子点赞表';

create index idx_like_post_id
    on post_like (post_id);

create index idx_like_user_id
    on post_like (user_id);

create table user
(
    id          bigint auto_increment
        primary key,
    username    varchar(50)                           not null comment '用户名',
    password    varchar(255)                          not null comment '密码(加密)',
    email       varchar(100)                          null comment '邮箱',
    phone       varchar(20)                           null comment '联系方式',
    job_nature  varchar(50)                           null comment '工作性质',
    location    varchar(100)                          null comment '工作地点',
    points      int         default 0                 not null comment '用户积分(用于发悬赏)',
    global_role varchar(20) default 'USER'            not null comment '全局角色: USER(普通用户), SYS_ADMIN(系统管理员)',
    status      varchar(20) default 'ACTIVE'          not null comment '状态: ACTIVE(正常), BANNED(封禁)',
    avatar      varchar(500)                          null comment '头像URL',
    bio         varchar(500)                          null comment '个人简介',
    created_at  datetime    default CURRENT_TIMESTAMP not null comment '注册时间',
    updated_at  datetime    default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    constraint uk_username
        unique (username)
)
    comment '用户表';

create table user_board_relation
(
    id         bigint auto_increment
        primary key,
    user_id    bigint                                not null,
    board_id   bigint                                not null,
    board_role varchar(20) default 'MEMBER'          not null comment '板块角色: OWNER(版主), ADMIN(小吧), MEMBER(成员), MUTED(禁言)',
    join_time  datetime    default CURRENT_TIMESTAMP not null comment '加入时间',
    constraint uk_user_board
        unique (user_id, board_id)
)
    comment '用户与板块权限关联表';

create index idx_board_id
    on user_board_relation (board_id);

create index idx_user_id
    on user_board_relation (user_id);

