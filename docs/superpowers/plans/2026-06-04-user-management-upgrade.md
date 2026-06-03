# User Management Upgrade Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Upgrade user management with generated employee numbers, email, required contact phone, enabled/disabled management, and successful-login timestamp tracking.

**Architecture:** Extend the existing `user` table and current `/api/users` plus `/api/auth/login` flows. Keep the current Vue user management page and JDBC-based Spring Boot service style.

**Tech Stack:** Spring Boot 3, Spring JDBC, MySQL 8, Vue 3, Vite.

---

## Constraints

- User-confirmed employee number format: `EMP + yyyy + 4-digit sequence`, for example `EMP20260001`.
- User-confirmed statuses: only enabled and disabled.
- User-confirmed required fields: contact phone required, email optional.
- User-confirmed history behavior: existing users must receive generated employee numbers.
- User-confirmed login behavior: update `last_login_time` only after successful login.
- User requested no repeated test runs during development; run one final verification after all implementation work is complete.

## File Structure

- Modify `system/sql/00_终版.sql`, `system/sql/market.sql`, `system/sql/01_market.sql`: add user columns to fresh database snapshots.
- Create `system/sql/12_user_management_upgrade.sql`: alter existing databases and backfill employee numbers.
- Modify `system/backend/src/main/java/com/supermarket/inventory/user/entity/User.java`: add user fields.
- Modify `system/backend/src/main/java/com/supermarket/inventory/user/dto/UserCreateRequest.java`: add email and contact phone.
- Modify `system/backend/src/main/java/com/supermarket/inventory/user/dto/UserUpdateRequest.java`: add email and contact phone.
- Modify `system/backend/src/main/java/com/supermarket/inventory/user/vo/UserVO.java`: return new fields.
- Modify `system/backend/src/main/java/com/supermarket/inventory/user/mapper/UserMapper.java`: map, query, insert, update, max employee number, and last login update SQL.
- Modify `system/backend/src/main/java/com/supermarket/inventory/user/service/UserService.java`: generate employee number, validate email/contact/status, expose fields.
- Modify `system/backend/src/main/java/com/supermarket/inventory/auth/service/AuthService.java`: update last login after successful password verification.
- Modify `system/backend/src/main/java/com/supermarket/inventory/config/BootstrapDataInitializer.java`: provide contact phone for demo accounts and rely on service-compatible insert fields.
- Modify `system/frontend/src/views/user/UsersView.vue`: update table, dialog, validation payload, and status toggle.

## Tasks

### Task 1: Database Scripts

- [ ] Add `employee_no`, `email`, `contact_phone`, and `last_login_time` to each full schema user table.
- [ ] Add unique key `uk_user_employee_no`.
- [ ] Create `system/sql/12_user_management_upgrade.sql`.
- [ ] In the upgrade script, add nullable columns first, backfill `employee_no` by year and `id`, backfill `contact_phone`, then apply `NOT NULL` and unique key.

### Task 2: Backend User Model And Mapper

- [ ] Add fields and accessors to `User`, `UserCreateRequest`, `UserUpdateRequest`, and `UserVO`.
- [ ] Extend `UserMapper.userRowMapper` for new columns.
- [ ] Extend keyword search to employee number, username, real name, email, and contact phone.
- [ ] Extend insert/update SQL for employee number, email, and contact phone.
- [ ] Add `findMaxEmployeeNo(String pattern)` and `updateLastLoginTime(Long id)`.

### Task 3: Backend Business Rules

- [ ] In `UserService.create`, generate `EMPyyyyNNNN` before insert.
- [ ] In `UserService.update`, keep username and employee number immutable.
- [ ] Normalize email to null when blank and validate simple email format when present.
- [ ] Require contact phone and validate it against digits, spaces, `+`, and `-`.
- [ ] Reject user status values other than `0` or `1`.
- [ ] Include new fields in `toVO`.
- [ ] In `AuthService.login`, call `userMapper.updateLastLoginTime(user.getId())` only after password verification succeeds.

### Task 4: Bootstrap Compatibility

- [ ] Update default demo account creation so inserted users include generated employee numbers and non-empty contact phones.
- [ ] Keep existing behavior that does not overwrite already existing accounts.

### Task 5: Frontend User Management

- [ ] Add table columns for employee number, contact phone, email, and last login time.
- [ ] Update search placeholder to `工号、用户名、姓名、邮箱或联系方式`.
- [ ] Add contact phone and optional email fields to create/edit dialog.
- [ ] Show employee number as automatic hint on create and readonly field on edit.
- [ ] Include email and contact phone in create/update payloads.
- [ ] Add direct status toggle button that reuses `updateUser`.
- [ ] Show `-` when email or last login time is empty.

### Task 6: Final Verification

- [ ] Run one backend verification command after all backend and frontend changes are complete:

```powershell
mvn test
```

- [ ] Run one frontend verification command after all backend and frontend changes are complete:

```powershell
npm run build
```

- [ ] Review final `git diff` to ensure only user-management upgrade files plus the plan/spec docs were changed by this task.
