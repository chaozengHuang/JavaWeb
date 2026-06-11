-- MySQL dump 10.13  Distrib 8.0.43, for Win64 (x86_64)
--
-- Host: 127.0.0.1    Database: javaweb
-- ------------------------------------------------------
-- Server version	8.0.43

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `admin_log`
--

DROP TABLE IF EXISTS `admin_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `admin_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `admin_id` bigint NOT NULL COMMENT '操作管理员ID',
  `admin_username` varchar(50) NOT NULL COMMENT '操作管理员用户名',
  `action` varchar(50) NOT NULL COMMENT '操作类型: RESET_PASSWORD/BAN_USER/UNBAN_USER/UPDATE_POST_STATUS/UPDATE_COMMENT_STATUS/BATCH_UPDATE_POST_STATUS/BATCH_UPDATE_COMMENT_STATUS',
  `target_type` varchar(20) NOT NULL COMMENT '操作对象类型: USER/POST/COMMENT',
  `target_id` bigint DEFAULT NULL COMMENT '操作对象ID(批量操作时为NULL)',
  `detail` varchar(500) DEFAULT NULL COMMENT '操作详情描述',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`id`),
  KEY `idx_admin_id` (`admin_id`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB AUTO_INCREMENT=21 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='管理员操作日志表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `admin_log`
--

LOCK TABLES `admin_log` WRITE;
/*!40000 ALTER TABLE `admin_log` DISABLE KEYS */;
INSERT INTO `admin_log` VALUES (1,1,'fx','RESET_PASSWORD','USER',1,'重置用户 fx 的密码','2026-05-27 16:24:04'),(2,1,'fx','BAN_USER','USER',3,'封禁用户 扫福瑞','2026-05-27 16:26:48'),(3,1,'fx','UNBAN_USER','USER',3,'解封用户 扫福瑞','2026-05-27 16:32:21'),(4,1,'fx','BAN_USER','USER',3,'封禁用户 扫福瑞','2026-05-27 16:32:23'),(5,1,'fx','BAN_USER','USER',4,'封禁用户 testban','2026-05-27 16:43:47'),(6,1,'fx','UNBAN_USER','USER',4,'解封用户 testban','2026-05-27 19:51:50'),(7,1,'fx','UPDATE_POST_STATUS','POST',1,'将帖子 这是一个帖子 状态修改为 DELETED','2026-06-01 10:33:01'),(8,1,'fx','UPDATE_POST_STATUS','POST',1,'将帖子 这是一个帖子 状态修改为 NORMAL','2026-06-01 10:33:07'),(9,1,'fx','UPDATE_POST_STATUS','POST',1,'将帖子 这是一个帖子 状态修改为 BLOCKED','2026-06-01 10:33:09'),(10,1,'fx','UPDATE_POST_STATUS','POST',1,'将帖子 这是一个帖子 状态修改为 NORMAL','2026-06-01 10:33:11'),(11,1,'fx','UPDATE_POST_STATUS','POST',1,'将帖子 这是一个帖子 状态修改为 BLOCKED','2026-06-01 10:33:16'),(12,1,'fx','UPDATE_POST_STATUS','POST',1,'将帖子 这是一个帖子 状态修改为 DELETED','2026-06-01 10:33:32'),(13,1,'fx','UPDATE_POST_STATUS','POST',1,'将帖子 这是一个帖子 状态修改为 NORMAL','2026-06-01 10:33:43'),(14,1,'fx','RESET_PASSWORD','USER',3,'重置用户 扫福瑞 的密码','2026-06-03 14:47:00'),(15,1,'fx','UNBAN_USER','USER',3,'解封用户 扫福瑞','2026-06-03 14:47:35'),(16,1,'fx','RESTORE_BOARD','BOARD',4,'恢复贴吧 孙吧','2026-06-03 18:23:07'),(17,1,'fx','RESTORE_BOARD','BOARD',4,'恢复贴吧 孙吧','2026-06-04 19:41:49'),(18,2,'admin','UPDATE_POST_STATUS','POST',14,'将帖子 测试系统管理员后台屏蔽 状态修改为 BLOCKED','2026-06-04 21:28:06'),(19,2,'admin','UPDATE_COMMENT_STATUS','COMMENT',19,'将评论#19 状态修改为 BLOCKED','2026-06-04 21:41:03'),(20,2,'admin','UPDATE_POST_STATUS','POST',14,'将帖子 测试系统管理员后台屏蔽 状态修改为 ACTIVE','2026-06-04 21:46:56');
/*!40000 ALTER TABLE `admin_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `board`
--

DROP TABLE IF EXISTS `board`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `board` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(50) NOT NULL COMMENT '板块名称',
  `description` varchar(255) DEFAULT NULL COMMENT '板块描述/规则',
  `creator_id` bigint NOT NULL COMMENT '创建者(版主)ID',
  `status` varchar(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE(正常), HIDDEN(隐藏)',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `avatar` varchar(500) DEFAULT NULL COMMENT '吧头像URL',
  PRIMARY KEY (`id`),
  KEY `idx_creator` (`creator_id`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='板块表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `board`
--

LOCK TABLES `board` WRITE;
/*!40000 ALTER TABLE `board` DISABLE KEYS */;
INSERT INTO `board` VALUES (1,'这是一个吧','',1,'ACTIVE','2026-06-01 10:29:34','2026-06-01 10:29:34',NULL),(2,'孙笑川吧','',5,'ACTIVE','2026-06-03 13:35:21','2026-06-03 13:35:21',NULL),(3,'测试吧是否可以管理调整','测试',6,'ACTIVE','2026-06-03 13:43:17','2026-06-03 13:43:17',NULL),(4,'孙吧','',6,'ACTIVE','2026-06-03 14:15:36','2026-06-04 20:24:40','/uploads/avatars/board_4_32ae2ec2.jpg'),(5,'测试吧的信息维护','这是一个测试吧描述的测试',5,'ACTIVE','2026-06-03 15:16:27','2026-06-04 20:22:33','/uploads/avatars/board_5_54caf073.jpg'),(6,'南昌大学吧','',8,'ACTIVE','2026-06-04 20:36:36','2026-06-07 20:34:40','/uploads/avatars/board_6_5bb629c4.jpg'),(7,'巨人吧','',5,'ACTIVE','2026-06-10 20:17:03','2026-06-11 10:57:39','/uploads/avatars/board_7_9569f2d6.jpg');
/*!40000 ALTER TABLE `board` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `browse_history`
--

DROP TABLE IF EXISTS `browse_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `browse_history` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `post_id` bigint NOT NULL COMMENT '帖子ID',
  `browse_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '浏览时间',
  PRIMARY KEY (`id`),
  KEY `idx_history_user` (`user_id`,`browse_time` DESC),
  KEY `idx_history_post` (`post_id`)
) ENGINE=InnoDB AUTO_INCREMENT=96 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='浏览历史表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `browse_history`
--

LOCK TABLES `browse_history` WRITE;
/*!40000 ALTER TABLE `browse_history` DISABLE KEYS */;
INSERT INTO `browse_history` VALUES (1,5,1,'2026-06-03 14:28:41'),(2,5,1,'2026-06-03 14:28:45'),(3,5,2,'2026-06-03 14:32:24'),(4,6,3,'2026-06-03 14:34:49'),(5,6,2,'2026-06-03 14:35:04'),(6,5,5,'2026-06-03 14:43:57'),(7,6,5,'2026-06-03 14:44:50'),(8,1,5,'2026-06-03 14:45:31'),(9,1,5,'2026-06-03 14:47:43'),(10,1,5,'2026-06-03 14:53:23'),(11,5,5,'2026-06-03 14:53:45'),(12,5,5,'2026-06-03 14:53:51'),(13,5,5,'2026-06-03 14:54:04'),(14,5,5,'2026-06-03 14:54:10'),(15,5,5,'2026-06-03 14:56:06'),(16,5,6,'2026-06-03 14:56:31'),(17,5,6,'2026-06-03 14:56:42'),(18,6,7,'2026-06-03 15:22:26'),(19,5,7,'2026-06-03 15:22:49'),(20,5,8,'2026-06-03 16:13:12'),(21,6,9,'2026-06-03 16:14:57'),(22,6,8,'2026-06-03 16:21:43'),(23,6,8,'2026-06-03 16:21:48'),(24,6,9,'2026-06-03 16:24:16'),(25,6,8,'2026-06-03 16:24:19'),(26,6,8,'2026-06-03 16:28:46'),(27,5,9,'2026-06-03 16:35:42'),(28,5,8,'2026-06-03 16:35:46'),(29,5,8,'2026-06-03 16:41:15'),(30,1,9,'2026-06-03 16:50:57'),(31,1,8,'2026-06-03 16:54:48'),(32,1,6,'2026-06-03 18:29:31'),(33,1,6,'2026-06-03 18:30:09'),(34,5,5,'2026-06-03 18:44:01'),(35,5,9,'2026-06-04 19:38:39'),(36,5,9,'2026-06-04 19:39:31'),(37,3,11,'2026-06-04 19:48:29'),(38,8,11,'2026-06-04 20:23:56'),(39,8,12,'2026-06-04 20:30:48'),(40,8,12,'2026-06-04 20:31:19'),(41,8,12,'2026-06-04 20:34:28'),(42,8,13,'2026-06-04 20:48:57'),(43,6,13,'2026-06-04 20:49:22'),(50,6,13,'2026-06-04 21:33:31'),(54,7,11,'2026-06-07 20:09:40'),(55,9,11,'2026-06-07 20:21:56'),(56,1,13,'2026-06-07 20:33:36'),(57,1,13,'2026-06-07 20:34:06'),(58,5,8,'2026-06-10 19:27:39'),(59,5,8,'2026-06-10 20:17:45'),(60,5,8,'2026-06-10 20:17:48'),(61,5,13,'2026-06-10 20:18:05'),(62,6,15,'2026-06-10 20:19:46'),(63,6,15,'2026-06-10 20:27:16'),(64,6,15,'2026-06-10 20:29:41'),(65,6,15,'2026-06-10 20:31:31'),(66,5,15,'2026-06-10 20:41:09'),(67,5,16,'2026-06-10 20:44:19'),(68,5,16,'2026-06-10 20:48:35'),(69,5,16,'2026-06-10 20:52:15'),(70,5,7,'2026-06-10 20:54:21'),(71,5,8,'2026-06-10 20:54:24'),(72,5,16,'2026-06-10 20:55:00'),(73,5,15,'2026-06-10 20:58:13'),(74,5,15,'2026-06-10 20:59:19'),(75,8,17,'2026-06-10 21:01:12'),(76,8,17,'2026-06-10 21:01:19'),(77,8,17,'2026-06-10 21:01:20'),(78,8,17,'2026-06-10 21:01:23'),(79,8,18,'2026-06-10 21:01:38'),(80,8,19,'2026-06-10 21:02:28'),(81,8,19,'2026-06-10 21:03:40'),(82,8,17,'2026-06-10 21:03:43'),(83,5,19,'2026-06-10 21:11:25'),(84,8,16,'2026-06-10 21:14:02'),(85,5,16,'2026-06-10 21:17:24'),(86,5,16,'2026-06-10 21:17:29'),(87,5,16,'2026-06-10 21:21:40'),(88,5,16,'2026-06-10 21:21:43'),(89,5,19,'2026-06-10 21:22:01'),(90,5,18,'2026-06-10 21:22:04'),(91,5,16,'2026-06-10 21:22:06'),(92,5,16,'2026-06-10 21:25:31'),(93,5,16,'2026-06-10 21:48:45'),(94,5,16,'2026-06-10 21:48:48'),(95,5,20,'2026-06-10 21:49:34');
/*!40000 ALTER TABLE `browse_history` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `comment`
--

DROP TABLE IF EXISTS `comment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `comment` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `post_id` bigint NOT NULL COMMENT '所属帖子ID',
  `author_id` bigint NOT NULL COMMENT '评论人ID',
  `content` text NOT NULL COMMENT '评论内容',
  `parent_comment_id` bigint DEFAULT NULL COMMENT '父评论ID(如果为空则是直接回复帖子，有值则是楼中楼回复)',
  `is_accepted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否被题主采纳(仅针对悬赏帖，0-否, 1-是)',
  `status` varchar(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE(正常), DELETED(已删除)',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '评论时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_author_id` (`author_id`),
  KEY `idx_post_id` (`post_id`)
) ENGINE=InnoDB AUTO_INCREMENT=31 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='评论回复表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `comment`
--

LOCK TABLES `comment` WRITE;
/*!40000 ALTER TABLE `comment` DISABLE KEYS */;
INSERT INTO `comment` VALUES (1,1,5,'你好',NULL,0,'ACTIVE','2026-06-01 17:11:04','2026-06-04 21:45:38'),(2,1,6,'你好',NULL,0,'ACTIVE','2026-06-03 13:38:29','2026-06-04 21:45:38'),(3,4,5,'你好',NULL,0,'ACTIVE','2026-06-03 13:47:13','2026-06-04 21:45:38'),(4,4,6,'你好',NULL,0,'ACTIVE','2026-06-03 13:47:34','2026-06-04 21:45:38'),(5,5,6,'测试接受悬赏',NULL,1,'ACTIVE','2026-06-03 14:44:59','2026-06-04 21:45:38'),(6,6,5,'你好',NULL,1,'ACTIVE','2026-06-03 14:56:35','2026-06-04 21:45:38'),(7,8,5,'测试活跃度更新',NULL,0,'ACTIVE','2026-06-03 16:13:22','2026-06-04 21:45:38'),(8,8,6,'测试',NULL,0,'ACTIVE','2026-06-03 16:21:54','2026-06-04 21:45:38'),(9,11,3,'你好',NULL,0,'DELETED','2026-06-04 19:48:37','2026-06-04 19:48:43'),(10,11,3,'你好',NULL,0,'ACTIVE','2026-06-04 19:48:46','2026-06-04 21:45:38'),(11,12,8,'测试禁言是否可以评论',NULL,0,'ACTIVE','2026-06-04 20:31:28','2026-06-04 21:45:38'),(12,12,8,'测试禁言以后是否可以评论',NULL,0,'ACTIVE','2026-06-04 20:34:37','2026-06-04 21:45:38'),(13,13,8,'测试楼中楼',NULL,0,'ACTIVE','2026-06-04 20:49:02','2026-06-04 21:45:38'),(14,13,8,'测试楼中楼',13,0,'ACTIVE','2026-06-04 20:49:10','2026-06-04 21:45:38'),(15,13,8,'测试',13,0,'ACTIVE','2026-06-04 20:49:14','2026-06-04 21:45:38'),(16,13,6,'你好',13,0,'ACTIVE','2026-06-04 20:49:28','2026-06-07 20:34:05'),(17,13,6,'你好',NULL,0,'ACTIVE','2026-06-04 20:49:32','2026-06-04 21:45:38'),(18,2,2,'测试系统管理员后台屏蔽',NULL,0,'ACTIVE','2026-06-04 21:27:47','2026-06-04 21:45:38'),(19,13,6,'nb兄弟\n',NULL,0,'ACTIVE','2026-06-04 21:33:39','2026-06-04 21:46:29'),(20,15,6,'牛逼\n',NULL,0,'ACTIVE','2026-06-10 20:27:27','2026-06-10 20:27:27'),(21,15,5,'\n![图片](/uploads/images/images_8e94eb50.jpg)\n',NULL,0,'ACTIVE','2026-06-10 20:41:17','2026-06-10 20:41:17'),(22,8,5,'\n![图片](/uploads/images/images_6d3e3902.jpg)\n',NULL,0,'ACTIVE','2026-06-10 20:54:29','2026-06-10 20:54:29'),(23,16,5,'\n![图片](/uploads/images/images_9e26a927.jpg)\n',NULL,0,'ACTIVE','2026-06-10 20:55:04','2026-06-10 20:55:04'),(24,15,5,'❌\n![图片](/uploads/images/images_e201e44d.jpg)\n',NULL,0,'ACTIVE','2026-06-10 20:58:23','2026-06-10 20:58:23'),(25,15,5,'👎\n![图片](/uploads/images/images_c13d32fd.jpg)\n',21,0,'ACTIVE','2026-06-10 20:59:29','2026-06-10 20:59:29'),(26,17,8,'\n![图片](/uploads/images/images_d8dab6ba.jpg)\n',NULL,0,'ACTIVE','2026-06-10 21:01:29','2026-06-10 21:01:29'),(27,19,5,'测试回复',NULL,0,'ACTIVE','2026-06-10 21:11:34','2026-06-10 21:11:34'),(28,16,8,'你好\n',NULL,0,'ACTIVE','2026-06-10 21:14:07','2026-06-10 21:14:07'),(29,16,8,'你好',23,0,'ACTIVE','2026-06-10 21:14:14','2026-06-10 21:14:14'),(30,16,5,'😇\n![图片](/uploads/images/images_24fa8646.jpg)\n',NULL,0,'ACTIVE','2026-06-10 21:49:00','2026-06-10 21:49:00');
/*!40000 ALTER TABLE `comment` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `daily_check_in`
--

DROP TABLE IF EXISTS `daily_check_in`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `daily_check_in` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '鐢ㄦ埛ID',
  `check_in_date` date NOT NULL COMMENT '绛惧埌鏃ユ湡',
  `points_awarded` int NOT NULL DEFAULT '0' COMMENT '鑾峰緱绉?垎',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '绛惧埌鏃堕棿',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_date` (`user_id`,`check_in_date`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='姣忔棩绛惧埌琛';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `daily_check_in`
--

LOCK TABLES `daily_check_in` WRITE;
/*!40000 ALTER TABLE `daily_check_in` DISABLE KEYS */;
INSERT INTO `daily_check_in` VALUES (1,6,'2026-06-03',15,'2026-06-03 14:14:53'),(2,5,'2026-06-03',11,'2026-06-03 14:28:36'),(3,1,'2026-06-03',14,'2026-06-03 18:21:28'),(4,3,'2026-06-03',13,'2026-06-03 18:46:55'),(5,5,'2026-06-04',9,'2026-06-04 19:38:15'),(6,6,'2026-06-04',5,'2026-06-04 19:39:48'),(7,1,'2026-06-04',15,'2026-06-04 19:41:57'),(8,3,'2026-06-04',7,'2026-06-04 19:51:28'),(9,8,'2026-06-04',12,'2026-06-04 20:24:13'),(10,2,'2026-06-04',7,'2026-06-04 21:30:12'),(11,1,'2026-06-07',11,'2026-06-07 20:34:12'),(12,5,'2026-06-07',6,'2026-06-07 20:55:30'),(13,5,'2026-06-10',12,'2026-06-10 19:26:53'),(14,8,'2026-06-10',8,'2026-06-10 21:03:17'),(15,5,'2026-06-11',14,'2026-06-11 10:38:22');
/*!40000 ALTER TABLE `daily_check_in` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `friend_relation`
--

DROP TABLE IF EXISTS `friend_relation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `friend_relation` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '鍙戣捣鏂圭敤鎴稩D',
  `friend_id` bigint NOT NULL COMMENT '鎺ユ敹鏂圭敤鎴稩D',
  `status` varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT '鐘舵?: PENDING/ACCEPTED/REJECTED',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_friend` (`user_id`,`friend_id`),
  KEY `idx_friend_id` (`friend_id`)
) ENGINE=InnoDB AUTO_INCREMENT=45 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='濂藉弸鍏崇郴琛';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `friend_relation`
--

LOCK TABLES `friend_relation` WRITE;
/*!40000 ALTER TABLE `friend_relation` DISABLE KEYS */;
INSERT INTO `friend_relation` VALUES (11,1,7,'ACCEPTED','2026-06-03 18:21:30','2026-06-03 18:21:30'),(12,7,1,'ACCEPTED','2026-06-03 18:21:30','2026-06-03 18:21:30'),(13,6,7,'ACCEPTED','2026-06-03 18:24:41','2026-06-03 18:24:41'),(14,7,6,'ACCEPTED','2026-06-03 18:24:41','2026-06-03 18:24:41'),(19,6,2,'ACCEPTED','2026-06-03 18:24:50','2026-06-03 18:24:50'),(20,2,6,'ACCEPTED','2026-06-03 18:24:50','2026-06-03 18:24:50'),(23,5,7,'ACCEPTED','2026-06-03 18:42:59','2026-06-03 18:42:59'),(24,7,5,'ACCEPTED','2026-06-03 18:42:59','2026-06-03 18:42:59'),(25,3,7,'ACCEPTED','2026-06-03 18:46:06','2026-06-03 18:46:06'),(26,7,3,'ACCEPTED','2026-06-03 18:46:06','2026-06-03 18:46:06'),(33,6,5,'ACCEPTED','2026-06-04 20:15:18','2026-06-04 20:15:10'),(34,5,6,'ACCEPTED','2026-06-04 20:15:20','2026-06-04 20:15:20'),(35,8,7,'ACCEPTED','2026-06-04 20:22:57','2026-06-04 20:22:57'),(36,7,8,'ACCEPTED','2026-06-04 20:22:57','2026-06-04 20:22:57'),(37,6,8,'ACCEPTED','2026-06-04 20:23:27','2026-06-04 20:23:27'),(38,8,6,'ACCEPTED','2026-06-04 20:23:33','2026-06-04 20:23:33'),(39,2,7,'ACCEPTED','2026-06-04 21:26:03','2026-06-04 21:26:03'),(40,7,2,'ACCEPTED','2026-06-04 21:26:03','2026-06-04 21:26:03'),(41,5,3,'ACCEPTED','2026-06-07 19:37:26','2026-06-07 19:33:17'),(42,3,5,'ACCEPTED','2026-06-07 19:37:50','2026-06-07 19:37:50'),(43,5,8,'ACCEPTED','2026-06-10 19:28:00','2026-06-10 19:28:00'),(44,8,5,'ACCEPTED','2026-06-10 21:00:28','2026-06-10 21:00:28');
/*!40000 ALTER TABLE `friend_relation` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `message`
--

DROP TABLE IF EXISTS `message`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `message` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `sender_id` bigint NOT NULL COMMENT '发送者ID',
  `receiver_id` bigint NOT NULL COMMENT '接收者ID',
  `content` text NOT NULL COMMENT '消息内容',
  `type` tinyint NOT NULL DEFAULT '1' COMMENT '消息类型: 1-文本',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '消息状态: 0-未读, 1-已读',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
  PRIMARY KEY (`id`),
  KEY `idx_sender_receiver` (`sender_id`,`receiver_id`),
  KEY `idx_receiver_status` (`receiver_id`,`status`)
) ENGINE=InnoDB AUTO_INCREMENT=73 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='私聊消息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `message`
--

LOCK TABLES `message` WRITE;
/*!40000 ALTER TABLE `message` DISABLE KEYS */;
INSERT INTO `message` VALUES (1,6,5,'你好，我是ytt1',1,1,'2026-06-03 14:41:25'),(2,5,6,'你好，我是ytt',1,1,'2026-06-03 14:42:10'),(3,5,1,'你好，我是ytt',1,1,'2026-06-03 14:43:02'),(4,1,5,'你好',1,1,'2026-06-03 14:53:36'),(5,6,5,'测试',1,1,'2026-06-03 16:22:15'),(6,1,6,'您在吧中的帖子「测试活跃度」已被管理员隐藏',1,1,'2026-06-03 16:54:41'),(7,1,5,'您在吧中的帖子「测试活跃度更新」已被管理员隐藏',1,1,'2026-06-03 17:57:05'),(8,1,6,'您在吧中的帖子「测试置顶」已被管理员隐藏',1,1,'2026-06-03 18:01:08'),(9,1,6,'您在吧中的帖子「测试活跃度」已被管理员删除',1,1,'2026-06-03 18:02:54'),(10,6,6,'贴吧「孙吧」已被解散',1,1,'2026-06-03 18:03:58'),(11,6,5,'贴吧「孙吧」已被解散',1,1,'2026-06-03 18:03:58'),(12,7,6,'贴吧「孙吧」已被解散',1,1,'2026-06-03 18:15:26'),(13,7,5,'贴吧「孙吧」已被解散',1,1,'2026-06-03 18:15:26'),(14,6,5,'你好',1,1,'2026-06-03 18:25:03'),(15,7,6,'您在吧中的帖子「测试置顶」已被管理员删除',1,1,'2026-06-03 18:42:31'),(16,7,5,'您在吧中的帖子「测试活跃度更新」已被管理员删除',1,1,'2026-06-03 18:42:34'),(17,3,5,'第一条消息测试（未加好友）',1,1,'2026-06-03 18:45:23'),(18,3,7,'你好',1,1,'2026-06-03 18:46:13'),(19,3,6,'测试第一条消息发送（未加好友）',1,1,'2026-06-03 18:50:58'),(20,7,6,'贴吧「孙吧」已被解散',1,1,'2026-06-04 19:41:20'),(21,7,5,'贴吧「孙吧」已被解散',1,1,'2026-06-04 19:41:20'),(22,7,3,'贴吧「孙吧」已被解散',1,1,'2026-06-04 19:41:20'),(23,3,7,'ok',1,1,'2026-06-04 19:42:51'),(24,3,1,'你好',1,1,'2026-06-04 19:48:59'),(25,5,6,'你好',1,1,'2026-06-04 20:06:45'),(26,6,5,'你好',1,1,'2026-06-04 20:07:06'),(27,6,5,'你好',1,1,'2026-06-04 20:14:21'),(28,5,6,'你好1',1,1,'2026-06-04 20:14:26'),(29,5,6,'你好',1,1,'2026-06-04 20:15:36'),(30,5,6,'nihao',1,1,'2026-06-04 20:15:37'),(31,5,6,'你好',1,1,'2026-06-04 20:15:42'),(32,8,6,'你好',1,1,'2026-06-04 20:23:18'),(33,6,8,'你好',1,1,'2026-06-04 20:23:21'),(34,6,8,'你好',1,1,'2026-06-04 20:23:39'),(35,7,6,'您在吧中的帖子「测试管理员管理操作」已被管理员隐藏',1,1,'2026-06-04 20:30:18'),(36,7,8,'有人回复了您的评论：你好',1,1,'2026-06-04 20:49:28'),(37,7,8,'有人回复了您的评论：你好',1,1,'2026-06-04 20:49:32'),(38,7,6,'您在吧中的帖子「测试活跃度」已被管理员删除',1,1,'2026-06-04 21:26:53'),(39,7,5,'有人回复了您的评论：测试系统管理员后台屏蔽',1,1,'2026-06-04 21:27:47'),(40,7,2,'系统管理员已将您的帖子「测试系统管理员后台屏蔽」屏蔽',1,1,'2026-06-04 21:28:06'),(41,7,8,'有人回复了您的评论：nb兄弟\n',1,1,'2026-06-04 21:33:39'),(42,6,7,'nb兄弟',1,1,'2026-06-04 21:33:55'),(43,7,6,'系统管理员已将您的评论屏蔽',1,1,'2026-06-04 21:41:03'),(44,3,5,'你好',1,1,'2026-06-07 19:37:56'),(45,7,9,'lj你好，欢迎来到Nexus贴吧',1,1,'2026-06-07 20:21:23'),(46,7,8,'ytt 加入了贴吧「南昌大学吧」',1,1,'2026-06-07 20:23:40'),(47,5,8,'你好',1,1,'2026-06-10 19:28:05'),(48,5,3,'😅',1,0,'2026-06-10 19:38:51'),(49,5,3,'![图片](/uploads/images/images_6c307b76.jpg)',1,0,'2026-06-10 19:38:58'),(50,5,3,'[文件](/uploads/files/files_4495082f.ipynb)  - ceshi.ipynb',1,0,'2026-06-10 19:39:16'),(51,5,3,'😗',1,0,'2026-06-10 19:43:25'),(52,5,3,'[文件](/uploads/files/files_bed3b2c3.cpp)  - ceshi.cpp',1,0,'2026-06-10 19:43:49'),(53,7,5,'ytt1 加入了贴吧「巨人吧」',1,1,'2026-06-10 20:19:42'),(54,6,5,'😃',1,1,'2026-06-10 20:21:16'),(55,6,5,'🥺',1,1,'2026-06-10 20:21:24'),(56,6,5,'你好😃',1,1,'2026-06-10 20:21:29'),(57,7,6,'有人回复了您的评论：\n![图片](/uploads/images/images_...',1,1,'2026-06-10 20:41:17'),(58,5,3,'![图片](/uploads/images/images_5eaa70d0.jpg)',1,0,'2026-06-10 20:55:14'),(59,7,6,'有人回复了您的评论：❌\n![图片](/uploads/images/images...',1,1,'2026-06-10 20:58:23'),(60,8,5,'![图片](/uploads/images/images_63f12e85.jpg)',1,1,'2026-06-10 21:00:39'),(61,7,10,'ytt3你好，欢迎来到Nexus贴吧',1,1,'2026-06-10 21:07:09'),(62,7,6,'您在吧中的帖子「测试」已被管理员隐藏',1,1,'2026-06-10 21:07:48'),(63,7,5,'ytt1 加入了贴吧「巨人吧」',1,1,'2026-06-10 21:10:51'),(64,7,8,'ytt 加入了贴吧「南昌大学吧」',1,1,'2026-06-10 21:11:33'),(65,7,6,'ytt 加入了贴吧「南昌大学吧」',1,1,'2026-06-10 21:11:33'),(66,7,8,'有人回复了您的评论：测试回复',1,1,'2026-06-10 21:11:34'),(67,7,5,'ytt2 回复了您的帖子，点击查看：/post/16',1,1,'2026-06-10 21:14:07'),(68,7,5,'ytt2 回复了您的评论，点击查看：/post/16',1,1,'2026-06-10 21:14:14'),(69,5,6,'😃你好',1,1,'2026-06-10 21:49:12'),(70,5,6,'![图片](/uploads/images/images_45bf3897.jpg)',1,1,'2026-06-10 21:49:15'),(71,5,5,'你是谁',1,1,'2026-06-10 21:54:15'),(72,6,5,'![图片](/uploads/images/images_4990c6d4.jpg)',1,0,'2026-06-11 10:58:43');
/*!40000 ALTER TABLE `message` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `post`
--

DROP TABLE IF EXISTS `post`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `post` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `board_id` bigint NOT NULL COMMENT '所属板块ID',
  `author_id` bigint NOT NULL COMMENT '发帖人ID',
  `title` varchar(150) NOT NULL COMMENT '帖子标题',
  `content` longtext NOT NULL COMMENT '帖子正文',
  `type` varchar(20) NOT NULL DEFAULT 'NORMAL' COMMENT '帖子类型: NORMAL(普通贴), REWARD(需求悬赏贴)',
  `reward_points` int NOT NULL DEFAULT '0' COMMENT '悬赏积分(普通贴为0)',
  `is_pinned` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否板块内置顶 (0-否, 1-是)',
  `is_global_pinned` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否全局置顶 (0-否, 1-是)',
  `is_featured` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否精华帖 (0-否, 1-是)',
  `status` varchar(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE(正常), DELETED(已删除)',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发帖时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_author_id` (`author_id`),
  KEY `idx_board_id` (`board_id`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB AUTO_INCREMENT=21 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='帖子表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `post`
--

LOCK TABLES `post` WRITE;
/*!40000 ALTER TABLE `post` DISABLE KEYS */;
INSERT INTO `post` VALUES (1,1,5,'这是一个帖子','rt','NORMAL',0,0,0,0,'NORMAL','2026-06-01 10:30:31','2026-06-01 10:30:31'),(2,2,5,'你好','你好啊','NORMAL',0,0,0,0,'NORMAL','2026-06-03 13:35:31','2026-06-03 13:35:31'),(3,1,6,'测试','测试ing\n','NORMAL',0,0,0,0,'NORMAL','2026-06-03 13:42:41','2026-06-03 13:42:41'),(4,3,5,'你好','你好','NORMAL',0,0,0,0,'NORMAL','2026-06-03 13:47:09','2026-06-03 13:47:09'),(5,1,5,'测试','测试悬赏帖子','REWARD',0,0,0,0,'ACTIVE','2026-06-03 14:43:55','2026-06-03 14:53:46'),(6,2,5,'测试悬赏','测试悬赏','REWARD',0,0,0,0,'ACTIVE','2026-06-03 14:56:24','2026-06-03 14:56:43'),(7,5,6,'测试置顶','测试','NORMAL',0,0,0,1,'ACTIVE','2026-06-03 15:22:24','2026-06-04 21:47:43'),(8,5,5,'测试活跃度更新','测试','NORMAL',0,0,0,0,'ACTIVE','2026-06-03 16:13:07','2026-06-04 21:26:58'),(9,5,6,'测试活跃度','测试','NORMAL',0,0,0,0,'ACTIVE','2026-06-03 16:14:50','2026-06-04 21:26:57'),(10,1,3,'测试未加入','测试未加入','NORMAL',0,0,0,0,'ACTIVE','2026-06-04 19:48:13','2026-06-04 19:48:13'),(11,1,3,'测试未加入悬赏','测试未加入悬赏','REWARD',4,0,0,0,'ACTIVE','2026-06-04 19:48:28','2026-06-04 19:48:28'),(12,4,6,'测试管理员管理操作','测试','NORMAL',0,1,0,0,'ACTIVE','2026-06-04 20:25:17','2026-06-04 20:30:21'),(13,6,8,'测试楼中楼','测试','NORMAL',0,0,0,0,'ACTIVE','2026-06-04 20:48:56','2026-06-04 20:48:56'),(14,2,2,'测试系统管理员后台屏蔽','测试','NORMAL',0,0,0,0,'ACTIVE','2026-06-04 21:27:38','2026-06-04 21:27:38'),(15,7,6,'测试','\n![图片](/uploads/images/images_7d4c413d.jpg)\n','NORMAL',0,0,0,0,'HIDDEN','2026-06-10 20:19:44','2026-06-10 21:07:48'),(16,6,5,'你好','你们好\n![图片](/uploads/images/images_8f58de34.jpg)\n','NORMAL',0,0,0,0,'ACTIVE','2026-06-10 20:44:18','2026-06-10 20:44:18'),(17,6,8,'大家好','😁😁大家好\n![图片](/uploads/images/images_57b66544.jpg)\n','NORMAL',0,0,0,0,'ACTIVE','2026-06-10 21:01:11','2026-06-10 21:01:18'),(18,6,8,'测试','😄','NORMAL',0,0,0,0,'ACTIVE','2026-06-10 21:01:37','2026-06-10 21:01:37'),(19,6,8,'测试','测试','REWARD',2,0,0,0,'ACTIVE','2026-06-10 21:02:27','2026-06-10 21:02:27'),(20,6,5,'1','😁测试\n![图片](/uploads/images/images_15c94d35.jpg)\n','NORMAL',0,0,0,0,'ACTIVE','2026-06-10 21:49:33','2026-06-10 21:49:33');
/*!40000 ALTER TABLE `post` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `post_favorite`
--

DROP TABLE IF EXISTS `post_favorite`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `post_favorite` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `post_id` bigint NOT NULL COMMENT '帖子ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_fav_user_post` (`user_id`,`post_id`),
  KEY `idx_fav_user_id` (`user_id`),
  KEY `idx_fav_post_id` (`post_id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='帖子收藏表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `post_favorite`
--

LOCK TABLES `post_favorite` WRITE;
/*!40000 ALTER TABLE `post_favorite` DISABLE KEYS */;
INSERT INTO `post_favorite` VALUES (1,5,1,'2026-06-01 22:07:17'),(2,5,8,'2026-06-03 16:13:13'),(5,6,15,'2026-06-10 20:29:49');
/*!40000 ALTER TABLE `post_favorite` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `post_like`
--

DROP TABLE IF EXISTS `post_like`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `post_like` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `post_id` bigint NOT NULL COMMENT '帖子ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_like_user_post` (`user_id`,`post_id`),
  KEY `idx_like_user_id` (`user_id`),
  KEY `idx_like_post_id` (`post_id`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='帖子点赞表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `post_like`
--

LOCK TABLES `post_like` WRITE;
/*!40000 ALTER TABLE `post_like` DISABLE KEYS */;
INSERT INTO `post_like` VALUES (1,5,1,'2026-06-01 22:07:10'),(2,6,1,'2026-06-03 13:38:30'),(3,6,3,'2026-06-03 14:34:51'),(4,5,8,'2026-06-03 16:13:13'),(5,6,9,'2026-06-03 16:14:58');
/*!40000 ALTER TABLE `post_like` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `recharge_order`
--

DROP TABLE IF EXISTS `recharge_order`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `recharge_order` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '鐢ㄦ埛ID',
  `order_no` varchar(32) NOT NULL COMMENT '璁㈠崟鍙',
  `amount` decimal(10,2) NOT NULL COMMENT '鍏呭?閲戦?(鍏?',
  `points` int NOT NULL COMMENT '鑾峰緱绉?垎',
  `status` varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT '鐘舵?: PENDING/COMPLETED/CANCELLED',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
  `completed_at` datetime DEFAULT NULL COMMENT '瀹屾垚鏃堕棿',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='鍏呭?璁㈠崟琛';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `recharge_order`
--

LOCK TABLES `recharge_order` WRITE;
/*!40000 ALTER TABLE `recharge_order` DISABLE KEYS */;
INSERT INTO `recharge_order` VALUES (1,6,'RC1780467296719_6_2689',10.00,100,'COMPLETED','2026-06-03 14:14:57','2026-06-03 14:14:57'),(2,6,'RC1780467311007_6_6669',100.00,1000,'COMPLETED','2026-06-03 14:15:11','2026-06-03 14:15:11'),(3,5,'RC1780468117444_5_6472',10.00,100,'COMPLETED','2026-06-03 14:28:37','2026-06-03 14:28:37'),(4,5,'RC1780836945592_5_5332',10.00,100,'COMPLETED','2026-06-07 20:55:46','2026-06-07 20:55:46'),(5,5,'RC1780836946099_5_8939',10.00,100,'COMPLETED','2026-06-07 20:55:46','2026-06-07 20:55:46'),(6,5,'RC1780836946623_5_3778',10.00,100,'COMPLETED','2026-06-07 20:55:47','2026-06-07 20:55:47'),(7,5,'RC1780836948091_5_6182',10.00,100,'COMPLETED','2026-06-07 20:55:48','2026-06-07 20:55:48'),(8,5,'RC1780836957820_5_8493',1.00,10,'COMPLETED','2026-06-07 20:55:58','2026-06-07 20:55:58');
/*!40000 ALTER TABLE `recharge_order` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user`
--

DROP TABLE IF EXISTS `user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(50) NOT NULL COMMENT '用户名',
  `password` varchar(255) NOT NULL COMMENT '密码(加密)',
  `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
  `phone` varchar(20) DEFAULT NULL COMMENT '联系方式',
  `job_nature` varchar(50) DEFAULT NULL COMMENT '工作性质',
  `location` varchar(100) DEFAULT NULL COMMENT '工作地点',
  `points` int NOT NULL DEFAULT '0' COMMENT '用户积分(用于发悬赏)',
  `global_role` varchar(20) NOT NULL DEFAULT 'USER' COMMENT '全局角色: USER(普通用户), SYS_ADMIN(系统管理员)',
  `status` varchar(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE(正常), BANNED(封禁)',
  `avatar` varchar(500) DEFAULT NULL COMMENT '头像URL',
  `bio` varchar(500) DEFAULT NULL COMMENT '个人简介',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `background` varchar(500) DEFAULT NULL COMMENT '个人主页背景URL',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user`
--

LOCK TABLES `user` WRITE;
/*!40000 ALTER TABLE `user` DISABLE KEYS */;
INSERT INTO `user` VALUES (1,'fx','$2a$10$VFcSDdlOqS2nSrELQ6Uqp.Px6dog/6laE2pUGOUyDpukSyeFKh.fC',NULL,NULL,NULL,NULL,40,'SYS_ADMIN','ACTIVE',NULL,'我是一个扫福瑞','2026-05-26 22:25:13','2026-05-27 16:22:08',NULL),(2,'admin','$2a$10$I35Fn0fJHtiIWMHHqpgMFux017nZesF3wb0V4EmXwpamUFrvCt21.',NULL,NULL,NULL,NULL,10006,'SYS_ADMIN','ACTIVE','/uploads/avatars/avatar_2_edf18fad.jpg',NULL,'2026-05-27 16:13:45','2026-06-04 21:26:01',NULL),(3,'扫福瑞','$2a$10$GCGnUcPgIDk9ye.gyEbO5.yqGNy2D/M4BLXGBIXPU.U5kKY9uHqhq',NULL,NULL,NULL,NULL,16,'USER','ACTIVE',NULL,'我是火车站','2026-05-27 16:24:49','2026-05-27 16:25:30',NULL),(4,'testban','$2a$10$BW3M202Qz.a5qpuPMAuCs.WlmBUtAVhZj0psnJ961wUOBZUCi4PLe',NULL,NULL,NULL,NULL,0,'USER','ACTIVE',NULL,NULL,'2026-05-27 16:40:46','2026-05-27 16:40:46',NULL),(5,'ytt','$2a$10$Beed0hw7oxviy2LY3KUdBeU46TmL2ksnLov3unfzBe4cvIJ4BfO4e','123','123','进击的巨人','帕拉迪亚岛',561,'USER','ACTIVE','/uploads/images/avatar_5_ec997262.jpg','你可以叫我YellowToTruth','2026-05-27 19:50:10','2026-06-11 10:38:08','/uploads/images/bg_5_72017f46.jpg'),(6,'ytt1','$2a$10$acC3hx5wawQm3.RJzX8vzuDcIi9sH0LL897HlJ/Q7WIegpli8Y8qq','123','123','调查兵团','南昌',1121,'USER','ACTIVE','/uploads/images/avatar_6_9ef7749b.jpg','喜欢玩电脑','2026-06-03 13:37:12','2026-06-11 10:58:29','/uploads/images/bg_6_2a3de7e6.jpg'),(7,'系统通知','$2a$10$IEQK4BcO232AwXj88WEf5O4lRQmYZxPvT.mqeYy3QWLa2IVt7/TIG','','','','',0,'NOTIFY_ADMIN','ACTIVE','/uploads/avatars/avatar_7_12e5744a.jpg','我是YellowToTruth','2026-06-03 18:15:07','2026-06-04 21:21:55',NULL),(8,'ytt2','$2a$10$lgAAv0y2Ubz35iB9oTsw/.iaoPIwWTangoloq73e7rAafgVJoCqTi',NULL,NULL,NULL,NULL,18,'USER','ACTIVE','/uploads/avatars/avatar_8_b9f0cdf4.jpg',NULL,'2026-06-04 20:22:49','2026-06-10 21:03:11','/uploads/images/bg_8_f72dbf23.jpg'),(9,'lj','$2a$10$gkV9AYFPmSpkjfUl55Bn6.EfeMki9z8CkKGrswtrvtqyc0ZzpToUS',NULL,NULL,NULL,NULL,0,'USER','ACTIVE',NULL,NULL,'2026-06-07 20:21:23','2026-06-07 20:21:23',NULL),(10,'ytt3','$2a$10$Ml2IaeacyrvIhxIVt6O1yuKN4BUH1S9/.1pVsaLSdjbY.g4IioR42',NULL,NULL,NULL,NULL,0,'USER','ACTIVE',NULL,NULL,'2026-06-10 21:07:09','2026-06-10 21:07:09',NULL);
/*!40000 ALTER TABLE `user` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user_board_relation`
--

DROP TABLE IF EXISTS `user_board_relation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_board_relation` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `board_id` bigint NOT NULL,
  `board_role` varchar(20) NOT NULL DEFAULT 'MEMBER' COMMENT '板块角色: OWNER(版主), ADMIN(小吧), MEMBER(成员), MUTED(禁言)',
  `join_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
  `activity_points` int NOT NULL DEFAULT '0' COMMENT '活跃度积分',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_board` (`user_id`,`board_id`),
  KEY `idx_board_id` (`board_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=22 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户与板块权限关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user_board_relation`
--

LOCK TABLES `user_board_relation` WRITE;
/*!40000 ALTER TABLE `user_board_relation` DISABLE KEYS */;
INSERT INTO `user_board_relation` VALUES (1,6,4,'OWNER','2026-06-03 14:15:36',5),(2,5,5,'OWNER','2026-06-03 15:16:27',9),(3,6,5,'MEMBER','2026-06-03 16:14:41',10),(5,5,4,'MEMBER','2026-06-03 18:02:32',0),(6,3,4,'MEMBER','2026-06-03 18:47:04',0),(7,5,2,'MEMBER','2026-06-04 20:16:27',0),(8,8,5,'MEMBER','2026-06-04 20:24:07',0),(9,8,4,'MUTED','2026-06-04 20:24:51',4),(10,8,6,'OWNER','2026-06-04 20:36:36',32),(11,6,6,'ADMIN','2026-06-04 20:36:44',6),(13,9,1,'MEMBER','2026-06-07 20:22:03',0),(14,9,2,'MEMBER','2026-06-07 20:22:09',0),(16,9,3,'MEMBER','2026-06-07 20:22:26',0),(18,5,7,'OWNER','2026-06-10 20:17:03',6),(20,6,7,'MEMBER','2026-06-10 21:10:51',0),(21,5,6,'MEMBER','2026-06-10 21:11:33',9);
/*!40000 ALTER TABLE `user_board_relation` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-06-11 15:47:20
