# 用户管理页面升级设计

## 背景

当前用户管理只维护用户名、姓名、角色、启用状态、创建时间和密码重置，不支持工号、邮箱、联系方式与最近登录时间展示。后端 `user` 表已有 `status` 字段，登录时已禁止禁用用户登录。本次升级在现有用户表和现有接口上做扩展，避免引入新的用户档案表或大范围权限改造。

## 目标

- 新增自动生成工号，格式为 `EMP + 年份 + 四位流水号`，例如 `EMP20260001`。
- 新增邮箱字段，允许为空；填写时校验邮箱格式。
- 新增联系方式字段，必填；只允许数字、空格、`+`、`-`。
- 状态管理暂时保持 `启用 / 禁用` 两种状态。
- 记录最近一次成功登录时间；密码错误或禁用用户登录失败时不更新。
- 已有用户通过升级 SQL 补齐工号，按 `create_time` 年份分组，同年内按 `id` 升序生成流水号。

## 非目标

- 不扩展 `离职`、`锁定` 等更多用户状态。
- 不新增独立用户档案表。
- 不允许前端或管理员手工编辑工号。
- 不引入日期库或前端复杂时间格式化。
- 不改变现有 `ADMIN` 用户管理权限边界。

## 数据库设计

在 `user` 表新增字段：

```sql
employee_no VARCHAR(20) NOT NULL COMMENT '工号，格式 EMPyyyyNNNN',
email VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
contact_phone VARCHAR(30) NOT NULL COMMENT '联系方式',
last_login_time DATETIME DEFAULT NULL COMMENT '最近一次登录时间',
UNIQUE KEY uk_user_employee_no (employee_no)
```

全量初始化脚本 `system/sql/00_终版.sql`、`system/sql/market.sql`、`system/sql/01_market.sql` 同步更新用户表结构。默认演示账号需要在初始化数据或启动补齐逻辑中具备工号和联系方式。

增量升级脚本新增一个独立 SQL 文件，例如 `system/sql/12_user_management_upgrade.sql`：

- 添加 `employee_no`、`email`、`contact_phone`、`last_login_time` 字段。
- 对历史用户按 `YEAR(create_time)` 分组、同年按 `id` 升序补齐 `employee_no`。
- 现有历史用户联系方式可先补默认值，例如 `00000000000`，后续管理员在页面维护真实联系方式。
- 补齐后将 `employee_no` 和 `contact_phone` 调整为 `NOT NULL`，并添加工号唯一索引。

## 后端设计

保留现有接口路径：

- `GET /api/users`：返回分页用户，新增 `employeeNo`、`email`、`contactPhone`、`lastLoginTime`。
- `POST /api/users`：新增用户时后端自动生成工号；请求体不接收工号。
- `PUT /api/users/{id}`：允许更新姓名、邮箱、联系方式、状态、角色和新密码；用户名、工号不可修改。
- `DELETE /api/users/{id}`：行为保持不变。
- `POST /api/auth/login`：仅在状态启用且密码正确后更新 `last_login_time = now()`。

涉及文件：

- `system/backend/src/main/java/com/supermarket/inventory/user/entity/User.java`
- `system/backend/src/main/java/com/supermarket/inventory/user/dto/UserCreateRequest.java`
- `system/backend/src/main/java/com/supermarket/inventory/user/dto/UserUpdateRequest.java`
- `system/backend/src/main/java/com/supermarket/inventory/user/vo/UserVO.java`
- `system/backend/src/main/java/com/supermarket/inventory/user/mapper/UserMapper.java`
- `system/backend/src/main/java/com/supermarket/inventory/user/service/UserService.java`
- `system/backend/src/main/java/com/supermarket/inventory/auth/service/AuthService.java`
- `system/backend/src/main/java/com/supermarket/inventory/config/BootstrapDataInitializer.java`

工号生成规则：

- 固定前缀：`EMP`。
- 年份：以后端当前日期年份为准。
- 流水号：当年从 `0001` 开始。
- 查询当前年份最大工号，例如 `EMP20260009`，下一号为 `EMP20260010`。
- 如果最大工号不符合 `EMPyyyyNNNN` 格式，抛出业务异常“用户工号序号异常”。
- 数据库唯一索引兜底防止重复工号。

校验规则：

- 联系方式为空时抛出“联系方式不能为空”。
- 联系方式包含非法字符时抛出“联系方式只能包含数字、空格、+或-”。
- 邮箱为空时保存为 `NULL`。
- 邮箱非空但格式不正确时抛出“邮箱格式不正确”。
- 状态只接受 `0` 或 `1`；其他值抛出“用户状态不正确”。

关键词搜索扩展到：

- 工号
- 用户名
- 姓名
- 邮箱
- 联系方式

## 前端设计

用户管理页面继续使用现有 `BaseTable`、`BaseDialog`、`PageToolbar`、`StatusTag`、`PermissionButton`，保持后台工具型界面。

表格列调整为：

- 工号
- 用户名
- 姓名
- 联系方式
- 邮箱
- 角色
- 状态
- 最近登录
- 创建时间
- 操作

搜索框占位文案改为“工号、用户名、姓名、邮箱或联系方式”。

新增用户弹窗：

- 用户名：必填。
- 密码：必填。
- 姓名：可填。
- 联系方式：必填。
- 邮箱：可选。
- 状态：启用 / 禁用。
- 角色：至少选择一个。
- 工号不允许输入，显示只读提示“工号保存后自动生成”。

编辑用户弹窗：

- 显示只读工号。
- 用户名保持只读。
- 重置密码留空表示不修改密码。
- 可修改姓名、联系方式、邮箱、状态和角色。

状态管理入口：

- 表格中继续使用状态标签展示启用或禁用。
- 操作区新增“启用 / 禁用”按钮，直接切换当前用户状态。
- 状态切换复用 `PUT /api/users/{id}`，提交当前用户原有角色、姓名、邮箱、联系方式和目标状态，避免新增接口。

时间展示：

- `lastLoginTime` 为空时显示 `-`。
- 非空时沿用接口返回字符串直接展示。

## 数据流

新增用户：

1. 管理员填写用户名、密码、联系方式、邮箱、状态和角色。
2. 前端调用 `POST /api/users`，不传工号。
3. 后端校验联系方式、邮箱、状态和角色。
4. 后端生成当年下一个工号并保存用户。
5. 前端刷新列表，展示生成后的工号。

编辑用户：

1. 管理员打开编辑弹窗。
2. 前端展示只读工号和用户名。
3. 管理员修改资料、状态、角色或密码。
4. 前端调用 `PUT /api/users/{id}`。
5. 后端更新可变字段并返回最新用户信息。

登录成功：

1. 用户提交用户名和密码。
2. 后端先校验用户存在、状态启用、密码正确。
3. 校验通过后更新 `last_login_time`。
4. 后端生成 JWT 并返回登录信息。

## 错误处理

- 前端继续使用当前页面顶部消息条展示后端业务错误。
- 后端复用 `BusinessException` 返回业务错误。
- 工号生成异常、邮箱格式错误、联系方式缺失或非法、状态非法都使用明确中文错误信息。
- 登录失败时保持现有错误语义，不暴露用户是否存在或密码是否错误的额外细节。

## 测试设计

后端新增或扩展单元测试：

- `UserServiceTest`：创建用户时生成 `EMPyyyyNNNN` 工号。
- `UserServiceTest`：同一年无历史工号时生成 `EMPyyyy0001`。
- `UserServiceTest`：已有最大工号时生成下一流水号。
- `UserServiceTest`：联系方式为空时报错。
- `UserServiceTest`：联系方式包含非法字符时报错。
- `UserServiceTest`：邮箱为空可通过，邮箱格式错误时报错。
- `UserServiceTest`：状态只接受 `0` 或 `1`。
- `AuthServiceTest`：登录成功后更新最近登录时间。
- `AuthServiceTest`：密码错误或禁用用户不更新最近登录时间。

前端验证：

- 运行 `npm run build` 确认用户页面编译通过。
- 手动打开用户管理页，检查列表列宽、弹窗字段、状态按钮、空最近登录展示。

后端验证：

- 运行 `mvn test`。
- 如果只需局部验证，先运行用户和认证相关测试，再运行完整测试。

## 实施顺序

1. 写后端测试，覆盖工号生成、字段校验、状态校验和最近登录时间更新。
2. 扩展数据库 SQL。
3. 扩展后端用户实体、DTO、VO、Mapper 和 Service。
4. 扩展登录成功后的 `last_login_time` 更新。
5. 更新前端用户管理列表、弹窗和状态切换按钮。
6. 运行后端测试和前端构建。

## 风险与取舍

- 工号生成使用查询最大工号加唯一索引兜底，符合当前系统规模；高并发新增用户时可能需要序列表或数据库锁。
- 历史联系方式需要默认值补齐，默认值不是用户真实联系电话，需要管理员后续维护。
- 状态切换复用更新接口会提交当前角色列表，前端必须确保角色数据完整，否则可能误改角色。
