# 工时管理系统（Work Hours System）

一个基于 **Spring Boot** 的企业级工时申报与审批系统，支持管理员、部门经理、普通员工三种角色，实现「工时填写 → 提交 → 经理审批 → 通过/驳回」的完整业务闭环，并包含角色权限控制、数据统计、操作日志等功能。

## 技术栈

| 分类 | 技术 |
|------|------|
| 后端框架 | Spring Boot 3.4.1（Java 21） |
| 安全框架 | Spring Security（RBAC 权限控制 + BCrypt 密码加密） |
| 持久层 | MyBatis-Plus 3.5.7 |
| 数据库 | SQL Server |
| 模板引擎 | Thymeleaf（含 Layout Dialect 布局） |
| 构建工具 | Maven |

## 功能模块

### 1. 登录认证
- 基于 Spring Security 的表单登录
- 自定义算式验证码过滤器，登录前校验验证码（存于 Session，校验后即清除防重用）
- 密码使用 BCrypt 加密存储

### 2. 角色与权限（RBAC）
| 角色 | 权限范围 |
|------|---------|
| 管理员（ADMIN） | 部门 / 员工 / 项目 / 工时 / 操作日志的全局管理 |
| 部门经理（MANAGER） | 审批本部门员工的工时申报、查看部门统计、员工项目授权 |
| 普通员工（EMPLOYEE） | 填写并提交本人工时、查看个人统计 |

### 3. 工时管理
- 员工按「项目 + 日期」申报工时（草稿 DRAFT / 已提交 SUBMITTED）
- 经理审批：通过（APPROVED）或驳回（REJECTED，必须填写原因）
- 业务规则：
  - 经理只能审批本部门员工的工时
  - 工时对应项目必须属于本部门
  - 驳回必须填写原因
  - 单条审批与批量审批（部分失败不影响已成功的）
- 状态流转：`DRAFT → SUBMITTED → APPROVED / REJECTED`

### 4. 数据统计与仪表盘
- 管理员：员工/部门/项目总数、本月全局工时、待审批数、最近记录
- 经理：本部门员工/项目数、待审批数、部门总工时、按员工/项目分布统计
- 员工：本月工时、今日工时、参与项目数、个人工时分布

### 5. 其他
- 统一业务异常（`BizException`）+ 全局异常处理
- 操作日志记录（操作人、IP、模块、动作、详情）
- 逻辑删除（`is_deleted` 字段）
- 分页查询

## 项目结构

```
src/main/java/com/workhours/
├── WorkHoursApplication.java   # 启动类
├── common/                     # 通用类（统一返回、业务异常）
├── config/                     # 配置类（Security、验证码过滤器、MyBatis-Plus 等）
├── controller/                 # 控制层（Login/Admin/Manager/Employee）
├── dto/                        # 数据传输对象
├── entity/                     # 实体类（与数据库表对应）
├── mapper/                     # MyBatis-Plus Mapper（含自定义 SQL）
└── service/                    # 业务逻辑层
src/main/resources/
├── application.yml             # 配置文件
├── static/                     # 静态资源（CSS）
└── templates/                  # Thymeleaf 页面模板
sql/
└── init.sql                    # 数据库初始化脚本（建库建表 + 示例数据）
```

## 快速开始

### 环境要求
- JDK 21
- Maven 3.6+
- SQL Server（本地实例）

### 步骤

1. **初始化数据库**

   在 SQL Server 中执行 `sql/init.sql`，脚本会自动创建数据库 `work_hours_db`、6 张表和示例数据。

2. **修改数据库连接配置**

   编辑 `src/main/resources/application.yml`，将 `url`、`username`、`password` 改为你的 SQL Server 连接信息：

   ```yaml
   spring:
     datasource:
       url: 'jdbc:sqlserver://localhost:12198;databaseName=work_hours_db;encrypt=false'
       username: sa
       password: Sa123456
   ```

3. **启动项目**

   ```bash
   mvn spring-boot:run
   ```

   或直接运行 `WorkHoursApplication` 主类。

4. **访问系统**

   浏览器打开：<http://localhost:8081>

### 演示账号

所有账号密码均为 `123456`：

| 角色 | 手机号（登录名） | 姓名 |
|------|-----------------|------|
| 管理员 | 13800000001 | 系统管理员 |
| 部门经理 | 13800000002 | 张经理 |
| 部门经理 | 13800000003 | 李经理 |
| 普通员工 | 13800000004 | 张三 |
| 普通员工 | 13800000005 | 李四 |
| 普通员工 | 13800000006 | 王五 |

## 核心业务流程

```
员工填写工时(草稿) → 提交审批 → 经理审核
                                   ├── 通过 → 计入统计
                                   └── 驳回 → 填写原因，员工可修改后重新提交
```

## 数据库设计（6 张表）

| 表名 | 说明 |
|------|------|
| department | 部门表 |
| employee | 员工表（含角色、所属部门） |
| project | 项目表 |
| employee_project | 员工-项目授权表（多对多） |
| work_log | 工时申报单表（含状态、审批信息） |
| operation_log | 操作日志表 |

## 后续可优化方向

- [ ] 验证码改用 Redis 存储 + 图形验证码
- [ ] 数据库密码改为环境变量 / 配置中心，避免明文
- [ ] 补充 JUnit 单元测试与接口测试
- [ ] 开启 CSRF 防护
- [ ] `ORDER BY` 字段白名单校验，消除 SQL 注入风险
- [ ] 支持 MySQL 作为可选数据库
