-- ----------------------------
-- Table structure for email_code
-- ----------------------------
create table email_code
(
    email       varchar(150) not null comment '邮箱',
    code        varchar(5)   not null comment '编号',
    create_time datetime     null comment '创建时间',
    status      tinyint(1)   null comment '0:未使用  1:已使用',
    ip_address  varchar(50)  null comment '用户请求发送邮件时的ip地址',
    primary key (email, code)
)
    comment '邮箱验证码';


-- ----------------------------
-- Table structure for file_info
-- ----------------------------
CREATE TABLE `file_info` (
                             `file_id` varchar(10) NOT NULL COMMENT '文件ID',
                             `user_id` varchar(10) NOT NULL COMMENT '用户ID',
                             `file_md5` varchar(32) DEFAULT NULL COMMENT 'md5值，第一次上传记录',
                             `file_pid` varchar(10) DEFAULT NULL COMMENT '父级ID',
                             `file_size` bigint(20) DEFAULT NULL COMMENT '文件大小',
                             `file_name` varchar(200) DEFAULT NULL COMMENT '文件名称',
                             `file_cover` varchar(100) DEFAULT NULL COMMENT '封面',
                             `file_path` varchar(100) DEFAULT NULL COMMENT '文件路径',
                             `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                             `last_update_time` datetime DEFAULT NULL COMMENT '最后更新时间',
                             `folder_type` tinyint(1) DEFAULT NULL COMMENT '0:文件 1:目录',
                             `file_category` tinyint(1) DEFAULT NULL COMMENT '1:视频 2:音频  3:图片 4:文档 5:其他',
                             `file_type` tinyint(1) DEFAULT NULL COMMENT ' 1:视频 2:音频  3:图片 4:pdf 5:doc 6:excel 7:txt 8:code 9:zip 10:其他',
                             `status` tinyint(1) DEFAULT NULL COMMENT '0:转码中 1转码失败 2:转码成功',
                             `recovery_time` datetime DEFAULT NULL COMMENT '回收站时间',
                             `del_flag` tinyint(1) DEFAULT '2' COMMENT '删除标记 0:删除  1:回收站  2:正常',
                             PRIMARY KEY (`file_id`,`user_id`),
                             KEY `idx_create_time` (`create_time`),
                             KEY `idx_user_id` (`user_id`),
                             KEY `idx_md5` (`file_md5`) USING BTREE,
                             KEY `idx_file_pid` (`file_pid`),
                             KEY `idx_del_flag` (`del_flag`),
                             KEY `idx_recovery_time` (`recovery_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件信息';

-- ----------------------------
-- Table structure for file_share
-- ----------------------------
CREATE TABLE `file_share` (
                              `share_id` varchar(20) NOT NULL COMMENT '分享ID',
                              `file_id` varchar(10) NOT NULL COMMENT '文件ID',
                              `user_id` varchar(10) NOT NULL COMMENT '用户ID',
                              `valid_type` tinyint(1) DEFAULT NULL COMMENT '有效期类型 0:1天 1:7天 2:30天 3:永久有效',
                              `expire_time` datetime DEFAULT NULL COMMENT '失效时间',
                              `share_time` datetime DEFAULT NULL COMMENT '分享时间',
                              `code` varchar(5) DEFAULT NULL COMMENT '提取码',
                              `show_count` int(11) DEFAULT '0' COMMENT '浏览次数',
                              PRIMARY KEY (`share_id`),
                              KEY `idx_file_id` (`file_id`),
                              KEY `idx_user_id` (`user_id`),
                              KEY `idx_share_time` (`share_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分享信息';

-- ----------------------------
-- Table structure for user_info
-- ----------------------------
create table user_info
(
    user_id         varchar(10)      not null comment '用户ID'
        primary key,
    nick_name       varchar(20)      null comment '昵称',
    email           varchar(150)     null comment '邮箱',
    qq_open_id      varchar(35)      null comment 'qqOpenID',
    qq_avatar       varchar(150)     null comment 'qq头像',
    password        varchar(50)      null comment '密码',
    join_time       datetime         null comment '加入时间',
    last_login_time datetime         null comment '最后登录时间',
    status          tinyint          null comment '0:禁用 1:正常',
    use_space       bigint default 0 null comment '使用空间单位byte',
    total_space     bigint           null comment '总空间',
    ip_address      varchar(50)      null comment '用户ip地址',
    constraint key_email
        unique (email),
    constraint key_nick_name
        unique (nick_name),
    constraint key_qq_open_id
        unique (qq_open_id)
)
    comment '用户信息';

INSERT INTO `user_info` VALUES ('3178033358', '管理员', 'test@test.com', null, null, '47ec2dd791e31e2ef2076caf64ed9b3d', '2023-09-28 13:54:01', '2023-12-28 13:54:01', '1', '238302835', '10737418240','10.2.22.97');
