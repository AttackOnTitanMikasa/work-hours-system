-- ============================================
-- 工时管理系统 - 数据库初始化脚本
-- 适用: SQL Server 2008 R2 (10.50.1600.1)
-- ============================================

-- 创建数据库
IF NOT EXISTS (SELECT name FROM sys.databases WHERE name = N'work_hours_db')
BEGIN
    CREATE DATABASE work_hours_db;
END
GO

USE work_hours_db;
GO

-- ============================================
-- 1. 部门表
-- ============================================
IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='department' AND xtype='U')
CREATE TABLE department (
    id          INT IDENTITY(1,1) PRIMARY KEY,
    name        VARCHAR(50)  NOT NULL,
    code        VARCHAR(20)  NOT NULL UNIQUE,
    create_time DATETIME2    DEFAULT GETDATE(),
    is_deleted  TINYINT      DEFAULT 0
);
GO

-- ============================================
-- 2. 员工表
-- ============================================
IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='employee' AND xtype='U')
CREATE TABLE employee (
    id          INT IDENTITY(1,1) PRIMARY KEY,
    name        VARCHAR(50)  NOT NULL,
    phone       VARCHAR(20)  NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    email       VARCHAR(100),
    role        VARCHAR(20)  NOT NULL CHECK (role IN ('ADMIN','MANAGER','EMPLOYEE')),
    dept_id     INT          FOREIGN KEY REFERENCES department(id),
    create_time DATETIME2    DEFAULT GETDATE(),
    is_deleted  TINYINT      DEFAULT 0
);
GO

-- ============================================
-- 3. 项目表
-- ============================================
IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='project' AND xtype='U')
CREATE TABLE project (
    id          INT IDENTITY(1,1) PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    dept_id     INT          FOREIGN KEY REFERENCES department(id),
    status      TINYINT      DEFAULT 1,  -- 1:启用 0:禁用
    create_time DATETIME2    DEFAULT GETDATE(),
    is_deleted  TINYINT      DEFAULT 0
);
GO

-- ============================================
-- 4. 员工-项目授权表
-- ============================================
IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='employee_project' AND xtype='U')
CREATE TABLE employee_project (
    id         INT IDENTITY(1,1) PRIMARY KEY,
    emp_id     INT NOT NULL FOREIGN KEY REFERENCES employee(id),
    project_id INT NOT NULL FOREIGN KEY REFERENCES project(id),
    CONSTRAINT UQ_emp_project UNIQUE (emp_id, project_id)
);
GO

-- ============================================
-- 5. 操作日志表
-- ============================================
IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='operation_log' AND xtype='U')
CREATE TABLE operation_log (
    id            INT IDENTITY(1,1) PRIMARY KEY,
    operator_id   INT          NOT NULL,
    operator_name VARCHAR(50)  NOT NULL,
    operator_role VARCHAR(20)  NOT NULL,
    module        VARCHAR(50)  NOT NULL,
    action        VARCHAR(20)  NOT NULL,
    detail        VARCHAR(500),
    ip            VARCHAR(50),
    create_time   DATETIME2    DEFAULT GETDATE()
);
GO

-- ============================================
-- 6. 工时申报单表
-- ============================================
IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='work_log' AND xtype='U')
CREATE TABLE work_log (
    id            INT IDENTITY(1,1) PRIMARY KEY,
    emp_id        INT          NOT NULL FOREIGN KEY REFERENCES employee(id),
    project_id    INT          NOT NULL FOREIGN KEY REFERENCES project(id),
    work_date     DATE         NOT NULL,
    hours         DECIMAL(3,1) NOT NULL,
    description   VARCHAR(500),
    status        VARCHAR(20)  DEFAULT 'DRAFT',
    reject_reason VARCHAR(500),
    reviewer_id   INT          FOREIGN KEY REFERENCES employee(id),
    review_time   DATETIME2,
    create_time   DATETIME2    DEFAULT GETDATE(),
    update_time   DATETIME2    DEFAULT GETDATE(),
    is_deleted    TINYINT      DEFAULT 0,
    CONSTRAINT UQ_emp_project_date UNIQUE (emp_id, project_id, work_date),
    CONSTRAINT CK_hours CHECK (hours >= 0.5 AND hours <= 24)
);
GO

-- ============================================
-- 示例数据 (密码: 123456, BCrypt加密)
-- ============================================

INSERT INTO department (name, code) VALUES (N'Web系统研发部', 'WEB');
INSERT INTO department (name, code) VALUES (N'移动端研发部', 'MOBILE');
INSERT INTO department (name, code) VALUES (N'Windows应用研发部', 'WINDOWS');

-- 管理员 (密码: 123456)
INSERT INTO employee (name, phone, password, email, role, dept_id)
VALUES (N'系统管理员', '13800000001', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'admin@work.com', 'ADMIN', 1);

-- 部门经理 (密码: 123456)
INSERT INTO employee (name, phone, password, email, role, dept_id)
VALUES (N'张经理', '13800000002', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'zhang@work.com', 'MANAGER', 1);
INSERT INTO employee (name, phone, password, email, role, dept_id)
VALUES (N'李经理', '13800000003', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'li@work.com', 'MANAGER', 2);

-- 普通员工 (密码: 123456)
INSERT INTO employee (name, phone, password, email, role, dept_id)
VALUES (N'张三', '13800000004', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'zhangsan@work.com', 'EMPLOYEE', 1);
INSERT INTO employee (name, phone, password, email, role, dept_id)
VALUES (N'李四', '13800000005', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'lisi@work.com', 'EMPLOYEE', 1);
INSERT INTO employee (name, phone, password, email, role, dept_id)
VALUES (N'王五', '13800000006', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'wangwu@work.com', 'EMPLOYEE', 2);

-- 项目
INSERT INTO project (name, dept_id, status) VALUES (N'XX后台管理系统', 1, 1);
INSERT INTO project (name, dept_id, status) VALUES (N'企业官网重构', 1, 1);
INSERT INTO project (name, dept_id, status) VALUES (N'移动端App开发', 2, 1);

-- 员工项目授权
INSERT INTO employee_project (emp_id, project_id) VALUES (4, 1);  -- 张三 -> XX后台管理系统
INSERT INTO employee_project (emp_id, project_id) VALUES (4, 2);  -- 张三 -> 企业官网重构
INSERT INTO employee_project (emp_id, project_id) VALUES (5, 1);  -- 李四 -> XX后台管理系统
INSERT INTO employee_project (emp_id, project_id) VALUES (6, 3);  -- 王五 -> 移动端App开发

-- 工时记录
-- 张三(4) - XX后台管理系统(1) - 已提交待审批
INSERT INTO work_log (emp_id, project_id, work_date, hours, description, status) VALUES (4, 1, '2026-07-01', 8.0, '完成用户管理模块开发', 'SUBMITTED');
INSERT INTO work_log (emp_id, project_id, work_date, hours, description, status) VALUES (4, 1, '2026-07-02', 6.5, '修复登录验证Bug', 'SUBMITTED');
INSERT INTO work_log (emp_id, project_id, work_date, hours, description, status) VALUES (4, 1, '2026-07-03', 7.0, '编写单元测试', 'SUBMITTED');

-- 张三(4) - 企业官网重构(2) - 已通过
INSERT INTO work_log (emp_id, project_id, work_date, hours, description, status, reviewer_id, review_time) VALUES (4, 2, '2026-06-25', 8.0, '首页设计稿切图', 'APPROVED', 2, '2026-06-26 10:00:00');
INSERT INTO work_log (emp_id, project_id, work_date, hours, description, status, reviewer_id, review_time) VALUES (4, 2, '2026-06-26', 7.5, '响应式布局适配', 'APPROVED', 2, '2026-06-27 09:30:00');
INSERT INTO work_log (emp_id, project_id, work_date, hours, description, status, reviewer_id, review_time) VALUES (4, 2, '2026-06-27', 6.0, '联系页面开发', 'APPROVED', 2, '2026-06-28 11:00:00');

-- 张三(4) - 企业官网重构(2) - 草稿
INSERT INTO work_log (emp_id, project_id, work_date, hours, description, status) VALUES (4, 2, '2026-07-07', 5.0, 'SEO优化调整', 'DRAFT');

-- 李四(5) - XX后台管理系统(1) - 已提交待审批
INSERT INTO work_log (emp_id, project_id, work_date, hours, description, status) VALUES (5, 1, '2026-07-01', 7.0, '数据库表结构设计', 'SUBMITTED');
INSERT INTO work_log (emp_id, project_id, work_date, hours, description, status) VALUES (5, 1, '2026-07-02', 8.0, 'API接口开发', 'SUBMITTED');
INSERT INTO work_log (emp_id, project_id, work_date, hours, description, status) VALUES (5, 1, '2026-07-03', 5.5, '接口联调测试', 'SUBMITTED');
INSERT INTO work_log (emp_id, project_id, work_date, hours, description, status) VALUES (5, 1, '2026-07-04', 6.0, '权限控制模块开发', 'SUBMITTED');

-- 李四(5) - XX后台管理系统(1) - 已通过
INSERT INTO work_log (emp_id, project_id, work_date, hours, description, status, reviewer_id, review_time) VALUES (5, 1, '2026-06-25', 8.0, '项目环境搭建', 'APPROVED', 2, '2026-06-26 10:30:00');
INSERT INTO work_log (emp_id, project_id, work_date, hours, description, status, reviewer_id, review_time) VALUES (5, 1, '2026-06-26', 7.0, '基础框架搭建', 'APPROVED', 2, '2026-06-27 14:00:00');

-- 李四(5) - XX后台管理系统(1) - 被驳回
INSERT INTO work_log (emp_id, project_id, work_date, hours, description, status, reject_reason, reviewer_id, review_time) VALUES (5, 1, '2026-06-30', 10.0, '加班调试', 'REJECTED', '单日工时超过8小时需额外说明', 2, '2026-07-01 09:00:00');

-- 王五(6) - 移动端App开发(3) - 已提交待审批
INSERT INTO work_log (emp_id, project_id, work_date, hours, description, status) VALUES (6, 3, '2026-07-01', 6.0, 'UI界面原型设计', 'SUBMITTED');
INSERT INTO work_log (emp_id, project_id, work_date, hours, description, status) VALUES (6, 3, '2026-07-02', 7.5, '首页组件开发', 'SUBMITTED');

-- 王五(6) - 移动端App开发(3) - 已通过
INSERT INTO work_log (emp_id, project_id, work_date, hours, description, status, reviewer_id, review_time) VALUES (6, 3, '2026-06-25', 8.0, '开发环境配置', 'APPROVED', 3, '2026-06-26 11:00:00');
INSERT INTO work_log (emp_id, project_id, work_date, hours, description, status, reviewer_id, review_time) VALUES (6, 3, '2026-06-26', 6.5, '项目架构设计', 'APPROVED', 3, '2026-06-27 10:00:00');
INSERT INTO work_log (emp_id, project_id, work_date, hours, description, status, reviewer_id, review_time) VALUES (6, 3, '2026-06-27', 7.0, '登录模块开发', 'APPROVED', 3, '2026-06-28 09:00:00');
INSERT INTO work_log (emp_id, project_id, work_date, hours, description, status, reviewer_id, review_time) VALUES (6, 3, '2026-06-30', 8.0, '推送通知集成', 'APPROVED', 3, '2026-07-01 10:00:00');

-- 王五(6) - 移动端App开发(3) - 草稿
INSERT INTO work_log (emp_id, project_id, work_date, hours, description, status) VALUES (6, 3, '2026-07-07', 4.0, '性能优化', 'DRAFT');

GO
PRINT N'数据库初始化完成！';
GO
