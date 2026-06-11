<template>
  <div class="user-permission-page">
    <!-- Page Header -->
    <div class="page-head">
      <div>
        <h1>用户与权限</h1>
        <div class="greet-sub">
          <b>{{ allUsers.length }}</b> 名员工 · 
          <b>{{ roles.length }}</b> 个角色 · 
          <b>{{ totalPermissionCount }}</b> 个权限项 · 
          上次同步 <b>刚刚</b>
        </div>
      </div>
      <div class="quick-actions">
        <button class="btn" type="button" @click="exportUsersCSV">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg>
          导出名册
        </button>
        <button class="btn" type="button" @click="openInviteModal">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="8.5" cy="7" r="4"/><line x1="20" y1="8" x2="20" y2="14"/><line x1="23" y1="11" x2="17" y2="11"/></svg>
          邀请用户
        </button>
        <button class="btn primary" type="button" @click="openCreateUser">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
          新建用户
        </button>
      </div>
    </div>

    <!-- KPI Row -->
    <section class="kpi-row">
      <div class="kpi" style="--accent:#4d9bff" @click="activeTab = 'users'">
        <div class="label">总用户</div>
        <div class="value">{{ allUsers.length }}<span class="unit">人</span></div>
        <div>
          <span class="delta"><svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round"><polyline points="6 15 12 9 18 15"/></svg>+{{ Math.max(1, Math.floor(allUsers.length * 0.1)) }}</span>
          <span class="sub">本月新增</span>
        </div>
        <div class="icon-wrap">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>
        </div>
      </div>
      <div class="kpi" style="--accent:#34d399">
        <div class="label">7 日活跃</div>
        <div class="value">{{ Math.ceil(allUsers.length * 0.85) }}<span class="unit">人</span></div>
        <div>
          <span class="delta"><svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round"><polyline points="6 15 12 9 18 15"/></svg>85.7%</span>
          <span class="sub">活跃率</span>
        </div>
        <div class="icon-wrap">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><polyline points="22 12 18 12 15 21 9 3 6 12 2 12"/></svg>
        </div>
      </div>
      <div class="kpi" style="--accent:#c084fc" @click="activeTab = 'roles'">
        <div class="label">角色</div>
        <div class="value">{{ roles.length }}<span class="unit">个</span></div>
        <div>
          <span class="delta"><svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round"><polyline points="6 15 12 9 18 15"/></svg>+1</span>
          <span class="sub">含 {{ roles.filter(r => r.roleCode.startsWith('CUSTOM_')).length }} 个自定义</span>
        </div>
        <div class="icon-wrap">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M12 1l3 6 6 .75-4.5 4.25L18 18l-6-3-6 3 1.5-6L3 7.75 9 7z"/></svg>
        </div>
      </div>
      <div class="kpi" style="--accent:#fbbf24">
        <div class="label">待处理</div>
        <div class="value">{{ allUsers.filter(u => Number(u.status) === 0).length }}<span class="unit">条</span></div>
        <div>
          <span class="delta down"><svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round"><polyline points="6 9 12 15 18 9"/></svg>+2</span>
          <span class="sub">账号状态待复核</span>
        </div>
        <div class="icon-wrap">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
        </div>
      </div>
    </section>

    <!-- Sub Navigation Tabs -->
    <nav class="ip-tabs">
      <button type="button" :class="{ on: activeTab === 'users' }" @click="activeTab = 'users'">
        <svg class="ico" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>
        <span>用户与权限</span>
        <span class="ct">{{ allUsers.length }}</span>
      </button>
      <button type="button" :class="{ on: activeTab === 'roles' }" @click="activeTab = 'roles'">
        <svg class="ico" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg>
        <span>角色与权限</span>
        <span class="ct">{{ roles.length }}</span>
      </button>
      <button v-if="showOperationLog" type="button" :class="{ on: activeTab === 'log' }" @click="activeTab = 'log'">
        <svg class="ico" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
        <span>操作日志</span>
        <span class="ct">{{ filteredLogs.length }}</span>
      </button>
    </nav>

    <!-- TAB 1: 用户管理 -->
    <section v-show="activeTab === 'users'" class="tab-pane-container">
      <div class="filter-bar">
        <label class="filter-search">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
          <input v-model.trim="userFilters.keyword" placeholder="按姓名 / 工号 / 邮箱 / 手机号搜索..." />
        </label>
        <span class="field-label">角色</span>
        <select v-model="userFilters.role" class="select">
          <option value="">全部角色</option>
          <option v-for="r in roles" :key="r.id" :value="r.roleCode">{{ r.roleName }}</option>
        </select>
        <span class="field-label">门店</span>
        <select v-model="userFilters.store" class="select">
          <option value="">全部门店</option>
          <option>陆家嘴店</option>
          <option>张江店</option>
          <option>世纪公园店</option>
          <option>静安店</option>
          <option>总部</option>
        </select>
        <span class="field-label">状态</span>
        <select v-model="userFilters.status" class="select">
          <option value="">全部</option>
          <option value="1">在职</option>
          <option value="0">停用</option>
        </select>
        <button class="btn sm" type="button" @click="resetUserFilters">重置</button>
      </div>

      <article class="card" style="padding:0; overflow:hidden">
        <div class="table-host">
          <table class="t">
            <thead>
              <tr>
                <th class="col-check"><input type="checkbox" class="check" :checked="isAllSelected" @change="toggleSelectAll" /></th>
                <th>工号</th>
                <th>用户</th>
                <th>角色</th>
                <th>门店 / 部门</th>
                <th>联系方式</th>
                <th>状态</th>
                <th>最近登录</th>
                <th>2FA</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="user in paginatedUsers" :key="user.id" @click="openUserDrawer(user)">
                <td class="col-check" @click.stop>
                  <input type="checkbox" class="check" :checked="selectedUserIds.includes(user.id)" @change="toggleSelectUser(user.id)" />
                </td>
                <td>
                  <span class="user-emp-no">{{ user.employeeNo }}</span>
                </td>
                <td>
                  <div class="uc-cell">
                    <div class="uc-avatar" :style="`background: linear-gradient(135deg, ${getUserAvatarColor(user)[0]}, ${getUserAvatarColor(user)[1]})`">
                      <span>{{ user.realName ? user.realName[0] : (user.username ? user.username[0] : 'U') }}</span>
                      <span v-if="user.roles?.some(r => r.roleCode === 'ADMIN')" class="crown">★</span>
                    </div>
                    <div class="uc-info">
                      <div class="nm">{{ user.realName || user.username }}</div>
                      <div class="em">{{ user.email || '未绑定邮箱' }}</div>
                    </div>
                  </div>
                </td>
                <td>
                  <span v-for="r in user.roles || []" :key="r.id" class="role-pill" :class="getRoleClass(r.roleCode)">
                    <span class="dot"></span>
                    {{ r.roleName }}
                  </span>
                  <span v-if="!user.roles?.length" class="role-pill viewer"><span class="dot"></span>普通用户</span>
                </td>
                <td>
                  <div class="user-dept">{{ getUserStoreDept(user).dept }}</div>
                </td>
                <td>
                  <div class="user-phone">{{ user.contactPhone || '-' }}</div>
                </td>
                <td>
                  <span class="st-pill" :class="Number(user.status) === 1 ? 'on' : 'off'">
                    <span class="dot"></span>
                    {{ Number(user.status) === 1 ? '在职' : '已停用' }}
                  </span>
                </td>
                <td>
                  <div class="user-time">{{ user.lastLoginTime ? formatDateTime(user.lastLoginTime).split(' ')[0] : '-' }}</div>
                  <div v-if="user.lastLoginTime" class="user-ip">{{ formatDateTime(user.lastLoginTime).split(' ')[1] }} · {{ mockIpAddress(user.id) }}</div>
                </td>
                <td>
                  <span v-if="user.id % 2 === 1 || user.username === 'admin'" class="tfa-enabled">
                    <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><path d="M12 1l3 6 6 .75-4.5 4.25L18 18l-6-3-6 3 1.5-6L3 7.75 9 7z"/></svg>已启用
                  </span>
                  <span v-else class="tfa-disabled">
                    <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><circle cx="12" cy="12" r="10"/><line x1="4.93" y1="4.93" x2="19.07" y2="19.07"/></svg>未启用
                  </span>
                </td>
                <td @click.stop>
                  <div class="row-actions">
                    <button type="button" title="编辑" @click="openUserDrawer(user)">
                      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 20h9"/><path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z"/></svg>
                    </button>
                    <button type="button" title="重置密码" @click="resetPasswordDirect(user)">
                      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="11" width="18" height="11" rx="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>
                    </button>
                    <button type="button" :title="Number(user.status) === 1 ? '禁用' : '启用'" @click="toggleStatusDirect(user)">
                      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M18.36 6.64a9 9 0 1 1-12.73 0"/><line x1="12" y1="2" x2="12" y2="12"/></svg>
                    </button>
                  </div>
                </td>
              </tr>
              <tr v-if="paginatedUsers.length === 0">
                <td colspan="10" style="text-align: center; padding: 30px; color: var(--text-mute)">暂无符合过滤条件的用户记录</td>
              </tr>
            </tbody>
          </table>
        </div>
        <div class="page-foot">
          <div>共 <b>{{ filteredUsers.length }}</b> 名用户 · 已选 <b>{{ selectedUserIds.length }}</b></div>
          <div class="pager">
            <button type="button" :disabled="userPage <= 1" @click="userPage = 1">«</button>
            <button type="button" :disabled="userPage <= 1" @click="userPage--">&lt;</button>
            <button v-for="p in totalUserPages" :key="p" type="button" :class="{ on: userPage === p }" @click="userPage = p">{{ p }}</button>
            <button type="button" :disabled="userPage >= totalUserPages" @click="userPage++">&gt;</button>
            <button type="button" :disabled="userPage >= totalUserPages" @click="userPage = totalUserPages">»</button>
          </div>
        </div>
      </article>
    </section>

    <!-- TAB 2: 角色与权限 -->
    <section v-show="activeTab === 'roles'" class="tab-pane-container">
      <div class="filter-bar">
        <div style="font-size:13px; color:var(--text-dim)">
          系统预置 {{ roles.filter(r => !r.roleCode.startsWith('CUSTOM_')).length }} 个角色，自定义角色 {{ roles.filter(r => r.roleCode.startsWith('CUSTOM_')).length }} 个。点击卡片或「配置权限」可调整角色的功能权限。
        </div>
        <button class="btn sm" style="margin-left:auto" type="button" @click="cloneRoleTemplate">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="9" y="9" width="13" height="13" rx="2"/><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/></svg>
          克隆角色
        </button>
        <button class="btn primary sm" type="button" @click="openCreateRole">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
          新建角色
        </button>
      </div>

      <div class="roles-grid">
        <div v-for="role in roles" :key="role.id" class="role-card" :style="`--rc: ${getRoleMeta(role.roleCode).color}`" @click="openPermissionDrawer(role)">
          <div class="rc-head">
            <div class="rc-icon">
              <!-- SVG Icons mapping -->
              <svg v-if="getRoleMeta(role.roleCode).icon === 'crown'" viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polygon points="2 7 6 11 12 5 18 11 22 7 20 17 4 17"/></svg>
              <svg v-else-if="getRoleMeta(role.roleCode).icon === 'shield'" viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg>
              <svg v-else-if="getRoleMeta(role.roleCode).icon === 'cart'" viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="9" cy="21" r="1"/><circle cx="20" cy="21" r="1"/><path d="M1 1h4l2.7 13.4a2 2 0 0 0 2 1.6h9.7a2 2 0 0 0 2-1.6L23 6H6"/></svg>
              <svg v-else-if="getRoleMeta(role.roleCode).icon === 'chart'" viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="22 12 18 12 15 21 9 3 6 12 2 12"/></svg>
              <svg v-else-if="getRoleMeta(role.roleCode).icon === 'box'" viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"/><polyline points="3.27 6.96 12 12.01 20.73 6.96"/><line x1="12" y1="22.08" x2="12" y2="12"/></svg>
              <svg v-else-if="getRoleMeta(role.roleCode).icon === 'wallet'" viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 12V7a2 2 0 0 0-2-2H5a2 2 0 0 0-2 2v10a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-5"/><path d="M17 12h4"/><circle cx="17" cy="12" r="1.5"/></svg>
              <svg v-else-if="getRoleMeta(role.roleCode).icon === 'yen'" viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="12" y1="1" x2="12" y2="23"/><path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"/></svg>
              <svg v-else viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>
            </div>
            <div style="min-width:0">
              <div class="rc-name">
                {{ role.roleName }}
                <svg v-if="getRoleMeta(role.roleCode).locked" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" style="margin-left:4px; color:var(--text-faint); vertical-align:-1px"><rect x="3" y="11" width="18" height="11" rx="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>
              </div>
              <div class="rc-code">{{ role.roleCode }}</div>
            </div>
          </div>
          <div class="rc-desc">{{ role.remark || getRoleMeta(role.roleCode).desc }}</div>
          <div class="rc-stats">
            <div class="it"><div class="l">成员</div><div class="v">{{ role.userCount || 0 }}</div></div>
            <div class="it"><div class="l">权限</div><div class="v">{{ (role.permissionCodes || []).length }}<span style="font-size:11px; color:var(--text-mute); margin-left:3px">/{{ totalPermissionCount }}</span></div></div>
            <div class="it"><div class="l">范围</div><div class="v" style="font-size:13px; line-height:1.4; padding-top:4px">{{ getRoleMeta(role.roleCode).scope }}</div></div>
          </div>
          <div class="rc-foot" @click.stop>
            <button class="btn primary" type="button" @click="openPermissionDrawer(role)">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/><rect x="14" y="14" width="7" height="7"/></svg>
              配置权限
            </button>
            <button class="btn" type="button" :disabled="getRoleMeta(role.roleCode).locked" @click="cloneSingleRole(role)">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="9" y="9" width="13" height="13" rx="2"/><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/></svg>
              克隆
            </button>
          </div>
        </div>

        <div class="role-card add-card" @click="openCreateRole">
          <div class="plus"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg></div>
          <div style="font-size:14px; font-weight:500">新建角色</div>
          <div style="font-size:11.5px; color:var(--text-faint)">从空白或现有模板继承</div>
        </div>
      </div>
    </section>

    <!-- TAB 3: 操作日志 -->
    <section v-if="showOperationLog && activeTab === 'log'" class="tab-pane-container">
      <div class="log-insight-grid">
        <article class="log-insight-card" style="--accent:#4d9bff">
          <div class="lic-label">日志总量</div>
          <div class="lic-value">{{ logInsights.total }}</div>
          <div class="lic-note">当前筛选范围 · {{ logFilters.timeRange }}</div>
        </article>
        <article class="log-insight-card" style="--accent:#34d399">
          <div class="lic-label">成功率</div>
          <div class="lic-value">{{ logInsights.successRate }}%</div>
          <div class="lic-note">{{ logInsights.failCount }} 条失败操作</div>
        </article>
        <article class="log-insight-card" style="--accent:#ff6b6b" @click="showRiskLogs">
          <div class="lic-label">风险日志</div>
          <div class="lic-value">{{ logInsights.riskCount }}</div>
          <div class="lic-note">高/中风险与失败登录</div>
        </article>
        <article class="log-insight-card" style="--accent:#fbbf24">
          <div class="lic-label">高频模块</div>
          <div class="lic-value text">{{ logInsights.topModule || '-' }}</div>
          <div class="lic-note">最活跃操作员：{{ logInsights.topOperator || '-' }}</div>
        </article>
      </div>

      <div class="log-analysis-board">
        <article class="log-panel">
          <div class="log-panel-head">
            <div>
              <h3>风险事件</h3>
              <p>自动识别失败登录、删除、跨网段访问和权限变更</p>
            </div>
            <button class="btn xs ghost" type="button" @click="applyRiskFilter">{{ logFilters.riskOnly ? '查看全部' : '只看异常' }}</button>
          </div>
          <div class="risk-list">
            <button
              v-for="log in highRiskLogs"
              :key="log.id"
              class="risk-item"
              type="button"
              @click="showLogDetail(log)"
            >
              <span class="risk-dot" :class="log.riskLevel"></span>
              <span class="risk-main">
                <b>{{ log.op }}</b>
                <small>{{ log.obj }}</small>
              </span>
              <span class="risk-meta">{{ getRiskName(log.riskLevel) }}</span>
            </button>
            <div v-if="highRiskLogs.length === 0" class="risk-empty">当前筛选范围内暂无异常风险</div>
          </div>
        </article>

        <article class="log-panel">
          <div class="log-panel-head">
            <div>
              <h3>操作类型分布</h3>
              <p>用于判断权限、登录和导出行为是否异常集中</p>
            </div>
          </div>
          <div class="type-bars">
            <div v-for="item in logTypeStats" :key="item.type" class="type-bar">
              <span class="log-op-tag" :class="item.type">{{ getLogTypeName(item.type) }}</span>
              <div class="bar-track">
                <i :style="{ width: item.percent + '%' }"></i>
              </div>
              <b>{{ item.count }}</b>
            </div>
          </div>
        </article>

        <article class="log-panel compact">
          <div class="log-panel-head">
            <div>
              <h3>操作员风险排行</h3>
              <p>按异常数、失败数和操作量排序</p>
            </div>
          </div>
          <div class="operator-rank">
            <div v-for="item in operatorRiskRanking" :key="item.operator" class="rank-row">
              <span class="rank-avatar">{{ item.operator[0] }}</span>
              <span class="rank-name">{{ item.operator }}</span>
              <span class="rank-score">{{ item.riskCount }} 风险 / {{ item.total }} 次</span>
            </div>
          </div>
        </article>
      </div>

      <div class="filter-bar">
        <label class="filter-search">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
          <input v-model.trim="logFilters.keyword" placeholder="按对象 / 操作员 / IP / 风险原因搜索..." />
        </label>
        <span class="field-label">操作员</span>
        <select v-model="logFilters.operator" class="select">
          <option value="">全部</option>
          <option v-for="op in logOperators" :key="op">{{ op }}</option>
        </select>
        <span class="field-label">模块</span>
        <select v-model="logFilters.module" class="select">
          <option value="">全部模块</option>
          <option v-for="m in logModules" :key="m">{{ m }}</option>
        </select>
        <span class="field-label">类型</span>
        <select v-model="logFilters.type" class="select">
          <option value="">全部</option>
          <option value="create">新建</option>
          <option value="update">修改</option>
          <option value="delete">删除</option>
          <option value="login">登录</option>
          <option value="export">导出</option>
          <option value="approve">审批</option>
        </select>
        <span class="field-label">结果</span>
        <select v-model="logFilters.result" class="select">
          <option value="">全部</option>
          <option value="ok">成功</option>
          <option value="fail">失败</option>
        </select>
        <span class="field-label">时间</span>
        <select v-model="logFilters.timeRange" class="select">
          <option>全部时间</option>
          <option>近 24 小时</option>
          <option>近 7 天</option>
          <option>近 30 天</option>
        </select>
        <button class="btn sm" type="button" @click="resetLogFilters">重置</button>
        <button class="btn sm" type="button" @click="exportLogs">导出</button>
      </div>

      <article class="card" style="padding:0; overflow:hidden">
        <div class="table-host">
          <table class="t">
            <thead>
              <tr>
                <th>时间</th>
                <th>操作员</th>
                <th>模块</th>
                <th>操作类型</th>
                <th>对象</th>
                <th>IP / 终端</th>
                <th>风险</th>
                <th>结果</th>
                <th>详情</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="log in paginatedLogs" :key="log.id">
                <td class="log-time">{{ formatLogClock(log.t) }}<span class="d">{{ formatLogDateLabel(log.t) }}</span></td>
                <td>
                  <div class="uc-cell">
                    <div class="uc-avatar" style="width:28px; height:28px; font-size:11px; background:linear-gradient(135deg, #a4adc2, #7b86a0)">
                      <span>{{ log.op[0] }}</span>
                    </div>
                    <div>
                      <div style="font-size:12.5px; font-weight:500">{{ log.op }}</div>
                      <div style="font-size:10.5px; color:var(--text-faint); margin-top:2px">{{ getLogRoleName(log.role) }}</div>
                    </div>
                  </div>
                </td>
                <td><span style="font-size:12.5px; color:var(--text-dim)">{{ log.mod }}</span></td>
                <td><span class="log-op-tag" :class="log.type">{{ getLogTypeName(log.type) }}</span></td>
                <td>
                  <span style="font-size:12.5px; color:var(--text-dim)">{{ log.obj }}</span>
                  <div v-if="log.traceId" class="trace-id">{{ log.traceId }}</div>
                </td>
                <td>
                  <div class="log-ip">{{ log.ip }}</div>
                  <div style="font-size:10.5px; color:var(--text-faint); margin-top:2px">{{ log.term }}</div>
                </td>
                <td>
                  <span class="risk-badge" :class="log.riskLevel">
                    <span></span>
                    {{ getRiskName(log.riskLevel) }}
                  </span>
                </td>
                <td>
                  <span class="log-result-tag" :class="log.result">
                    <svg v-if="log.result === 'ok'" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><polyline points="20 6 9 17 4 12"/></svg>
                    <svg v-else width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg>
                    {{ log.result === 'ok' ? '成功' : '失败' }}
                  </span>
                </td>
                <td>
                  <button class="btn xs ghost" type="button" @click="showLogDetail(log)">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>
                    查看
                  </button>
                </td>
              </tr>
              <tr v-if="paginatedLogs.length === 0">
                <td colspan="9" style="text-align:center; padding:30px; color:var(--text-mute)">没有符合筛选条件的操作日志记录</td>
              </tr>
            </tbody>
          </table>
        </div>
        <div class="page-foot">
          <div>共 <b>{{ filteredLogs.length }}</b> 条记录 · {{ logInsights.riskCount }} 条需复核 · 保留 90 天</div>
          <div class="pager">
            <button type="button" :disabled="logPage <= 1" @click="logPage = 1">«</button>
            <button type="button" :disabled="logPage <= 1" @click="logPage--">&lt;</button>
            <button v-for="p in totalLogPages" :key="p" type="button" :class="{ on: logPage === p }" @click="logPage = p">{{ p }}</button>
            <button type="button" :disabled="logPage >= totalLogPages" @click="logPage++">&gt;</button>
            <button type="button" :disabled="logPage >= totalLogPages" @click="logPage = totalLogPages">»</button>
          </div>
        </div>
      </article>
    </section>

    <!-- RIGHT DRAWER: USER DETAILS & EDITING -->
    <div class="drawer-mask" :class="{ show: userDrawerVisible }" @click="closeUserDrawer"></div>
    <aside class="drawer-panel" :class="{ show: userDrawerVisible }">
      <div class="drawer-head">
        <h2>用户详情</h2>
        <button class="x" type="button" @click="closeUserDrawer">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
        </button>
      </div>
      <div v-if="selectedUser" class="drawer-body">
        <div class="drawer-hero">
          <div class="uc-avatar" style="width: 56px; height: 56px; font-size: 20px" :style="`background: linear-gradient(135deg, ${getUserAvatarColor(selectedUser)[0]}, ${getUserAvatarColor(selectedUser)[1]})`">
            <span>{{ selectedUser.realName ? selectedUser.realName[0] : (selectedUser.username ? selectedUser.username[0] : 'U') }}</span>
            <span v-if="selectedUser.roles?.some(r => r.roleCode === 'ADMIN')" class="crown" style="top: -4px; right: -4px; width: 18px; height: 18px; font-size: 10px">★</span>
          </div>
          <div>
            <div class="hi-nm">{{ selectedUser.realName || selectedUser.username }}</div>
            <div class="hi-sub">{{ selectedUser.employeeNo }} · {{ selectedUser.email || '未绑定邮箱' }}</div>
            <div style="margin-top:8px; display:flex; gap:6px">
              <span v-for="r in selectedUser.roles || []" :key="r.id" class="role-pill" :class="getRoleClass(r.roleCode)">
                <span class="dot"></span>
                {{ r.roleName }}
              </span>
              <span class="st-pill" :class="Number(selectedUser.status) === 1 ? 'on' : 'off'">
                <span class="dot"></span>
                {{ Number(selectedUser.status) === 1 ? '在职' : '已停用' }}
              </span>
            </div>
          </div>
        </div>

        <div class="stat-row">
          <div class="st">
            <div class="l">入职时长</div>
            <div class="v">{{ calcMonths(selectedUser.createTime) }}<span style="font-size:11px; color:var(--text-mute); margin-left:3px">个月</span></div>
          </div>
          <div class="st">
            <div class="l">7 日操作</div>
            <div class="v">{{ 40 + (selectedUser.id * 17) % 200 }}</div>
          </div>
          <div class="st">
            <div class="l">登录异常</div>
            <div class="v" :style="`color: ${Number(selectedUser.status) === 0 ? '#b91c1c' : 'var(--text)'}`">
              {{ Number(selectedUser.status) === 0 ? 3 : 0 }}
            </div>
          </div>
        </div>

        <div style="margin-top:22px">
          <div style="font-size:12px; color:var(--text-mute); letter-spacing:1px; text-transform:uppercase; font-family:'Orbitron',monospace; margin-bottom:10px">账号信息</div>
          <div class="field-row">
            <div class="label"><span class="req">*</span>姓名</div>
            <div class="input-wrap"><input v-model.trim="userForm.realName" class="input" /></div>
          </div>
          <div class="field-row">
            <div class="label">工号</div>
            <div class="input-wrap"><input :value="selectedUser.employeeNo" class="input" disabled style="font-family:'DM Mono',monospace; opacity:0.7" /></div>
          </div>
          <div class="field-row">
            <div class="label">用户名</div>
            <div class="input-wrap"><input :value="selectedUser.username" class="input" disabled style="opacity:0.7" /></div>
          </div>
          <div class="field-row">
            <div class="label">重置密码</div>
            <div class="input-wrap"><input v-model="userForm.password" class="input" type="password" placeholder="留空代表不修改密码" /></div>
          </div>
          <div class="field-row">
            <div class="label">邮箱</div>
            <div class="input-wrap"><input v-model.trim="userForm.email" class="input" style="font-family:'DM Mono',monospace" /></div>
          </div>
          <div class="field-row">
            <div class="label"><span class="req">*</span>手机号</div>
            <div class="input-wrap"><input v-model.trim="userForm.contactPhone" class="input" style="font-family:'DM Mono',monospace" /></div>
          </div>
        </div>

        <div style="margin-top:18px">
          <div style="font-size:12px; color:var(--text-mute); letter-spacing:1px; text-transform:uppercase; font-family:'Orbitron',monospace; margin-bottom:10px">权限与归属</div>
          <div class="field-row">
            <div class="label"><span class="req">*</span>所属角色</div>
            <div class="input-wrap">
              <div class="check-grid" style="grid-template-columns: 1fr">
                <label v-for="r in roles" :key="r.id" class="check-item" style="padding: 6px 10px">
                  <input v-model="userForm.roleIds" type="checkbox" :value="r.id" />
                  <span style="font-size:12.5px">{{ r.roleName }}</span>
                  <small>{{ r.roleCode }}</small>
                </label>
              </div>
            </div>
          </div>
          <div class="field-row">
            <div class="label">归属门店</div>
            <div class="input-wrap"><input v-model="userForm.store" class="input" /></div>
          </div>
          <div class="field-row">
            <div class="label">部门</div>
            <div class="input-wrap"><input v-model="userForm.dept" class="input" /></div>
          </div>
        </div>

        <div style="margin-top:18px">
          <div style="font-size:12px; color:var(--text-mute); letter-spacing:1px; text-transform:uppercase; font-family:'Orbitron',monospace; margin-bottom:10px">安全设置</div>
          <div class="field-row">
            <div class="label">两步验证<span class="hint">TOTP (Google Authenticator)</span></div>
            <div class="input-wrap" style="flex-direction:row; align-items:center; gap:12px">
              <label class="switch">
                <input v-model="userForm.tfa" type="checkbox" />
                <span class="slider"></span>
              </label>
              <span style="font-size:12.5px; color:var(--text-mute)">{{ userForm.tfa ? '已绑定TOTP设备' : '未开启TOTP限制' }}</span>
            </div>
          </div>
          <div class="field-row">
            <div class="label">登录限制</div>
            <div class="input-wrap">
              <div class="radio-chips">
                <label><input v-model="userForm.loginLimit" type="radio" value="work" /><span>仅工作时间</span></label>
                <label><input v-model="userForm.loginLimit" type="radio" value="none" /><span>无限制</span></label>
                <label><input v-model="userForm.loginLimit" type="radio" value="ip" /><span>IP 白名单</span></label>
              </div>
            </div>
          </div>
          <div class="field-row" style="border-bottom:0">
            <div class="label">会话超时</div>
            <div class="input-wrap">
              <div class="row" style="gap:6px">
                <input v-model="userForm.timeout" class="input" style="width:80px; font-family:'DM Mono',monospace" />
                <span style="font-size:12.5px; color:var(--text-mute)">分钟</span>
              </div>
            </div>
          </div>
        </div>
      </div>
      <div v-if="selectedUser" class="drawer-foot">
        <button class="btn danger" type="button" @click="toggleStatusDirect(selectedUser)">
          {{ Number(selectedUser.status) === 1 ? '停用账号' : '启用账号' }}
        </button>
        <button class="btn" type="button" @click="resetPasswordDirect(selectedUser)">重置密码</button>
        <button class="btn primary" type="button" :disabled="submitting" @click="saveUserDrawer">
          {{ submitting ? '保存中...' : '保存修改' }}
        </button>
      </div>
    </aside>

    <!-- RIGHT DRAWER: ROLE PERMISSION MATRIX CONFIGURATION -->
    <div class="drawer-mask" :class="{ show: permDrawerVisible }" @click="closePermDrawer"></div>
    <aside id="permDrawer" class="drawer-panel perm-drawer" :class="{ show: permDrawerVisible }">
      <div v-if="selectedRole" class="perm-dr-head">
        <div class="perm-dr-top">
          <div class="pdi">
            <svg v-if="getRoleMeta(selectedRole.roleCode).icon === 'crown'" viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="2"><polygon points="2 7 6 11 12 5 18 11 22 7 20 17 4 17"/></svg>
            <svg v-else-if="getRoleMeta(selectedRole.roleCode).icon === 'shield'" viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg>
            <svg v-else-if="getRoleMeta(selectedRole.roleCode).icon === 'cart'" viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="9" cy="21" r="1"/><circle cx="20" cy="21" r="1"/><path d="M1 1h4l2.7 13.4a2 2 0 0 0 2 1.6h9.7a2 2 0 0 0 2-1.6L23 6H6"/></svg>
            <svg v-else-if="getRoleMeta(selectedRole.roleCode).icon === 'chart'" viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="22 12 18 12 15 21 9 3 6 12 2 12"/></svg>
            <svg v-else-if="getRoleMeta(selectedRole.roleCode).icon === 'box'" viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"/><polyline points="3.27 6.96 12 12.01 20.73 6.96"/><line x1="12" y1="22.08" x2="12" y2="12"/></svg>
            <svg v-else-if="getRoleMeta(selectedRole.roleCode).icon === 'wallet'" viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 12V7a2 2 0 0 0-2-2H5a2 2 0 0 0-2 2v10a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-5"/><path d="M17 12h4"/><circle cx="17" cy="12" r="1.5"/></svg>
            <svg v-else-if="getRoleMeta(selectedRole.roleCode).icon === 'yen'" viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="2"><line x1="12" y1="1" x2="12" y2="23"/><path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"/></svg>
            <svg v-else viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="2"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>
          </div>
          <div style="min-width:0">
            <div class="pd-name">
              {{ selectedRole.roleName }}
              <svg v-if="getRoleMeta(selectedRole.roleCode).locked" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><rect x="3" y="11" width="18" height="11" rx="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>
            </div>
            <div class="pd-code">{{ selectedRole.roleCode }}</div>
            <div class="pd-desc">{{ selectedRole.remark || getRoleMeta(selectedRole.roleCode).desc }}</div>
          </div>
          <button class="x" type="button" @click="closePermDrawer">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
          </button>
        </div>
        <div class="perm-dr-meta">
          <div class="m"><div class="l">成员</div><div class="v">{{ selectedRole.userCount || 0 }}<small>人</small></div></div>
          <div class="m"><div class="l">数据范围</div><div class="v" style="font-size:15px">{{ getRoleMeta(selectedRole.roleCode).scope }}</div></div>
          <div class="m"><div class="l">已授权</div><div class="v">{{ selectedRolePermissionCodes.length }}<small>/{{ totalPermissionCount }}</small></div></div>
        </div>
      </div>

      <div class="perm-dr-toolbar">
        <label class="filter-search" style="flex:1">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
          <input v-model="permSearchKeyword" placeholder="搜索权限项 / 编码..." />
        </label>
        <div class="perm-legend">
          <span class="lg"><span class="perm-dot y"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg></span>允许</span>
          <span class="lg"><span class="perm-dot partial"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round"><line x1="5" y1="12" x2="19" y2="12"/></svg></span>部分</span>
          <span class="lg"><span class="perm-dot n">·</span>拒绝</span>
        </div>
      </div>

      <div class="drawer-body" style="background:#f6f8fc">
        <div v-if="getRoleMeta(selectedRole?.roleCode).locked" class="perm-lock-note">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><rect x="3" y="11" width="18" height="11" rx="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>
          超级管理员拥有系统全部 192 项权限，且不可被收回。
        </div>

        <div v-for="(module, mi) in filteredPermissionModules" :key="module.moduleCode" class="perm-mod">
          <div class="perm-mod-head">
            <span class="mic">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="7" height="9"/><rect x="14" y="3" width="7" height="5"/><rect x="14" y="12" width="7" height="9"/><rect x="3" y="16" width="7" height="5"/></svg>
            </span>
            <span class="mn">{{ module.moduleName }}</span>
            <span class="mc">{{ module.permissions.length }} 项</span>
            <span class="mod-count">{{ getModuleGrantedCount(module) }}/{{ module.permissions.length }}</span>
            <div v-if="!getRoleMeta(selectedRole?.roleCode).locked" class="mod-all">
              <button type="button" @click="setModuleAll(module, true)">全允许</button>
              <button type="button" @click="setModuleAll(module, false)">全拒绝</button>
            </div>
          </div>
          <div v-for="perm in module.permissions" :key="perm.code" class="perm-item">
            <div class="pi-info">
              <div class="pin">{{ perm.name }}</div>
              <div class="pic">{{ perm.code }}</div>
            </div>
            <div class="seg">
              <button type="button" class="y" :class="{ on: isPermissionEnabled(perm.code) }" :disabled="getRoleMeta(selectedRole?.roleCode).locked" @click="setPermissionState(perm.code, true)">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg>
                允许
              </button>
              <button type="button" class="p" :class="{ on: false }" :disabled="getRoleMeta(selectedRole?.roleCode).locked" @click="setPermissionState(perm.code, true)">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round"><line x1="5" y1="12" x2="19" y2="12"/></svg>
                部分
              </button>
              <button type="button" class="n" :class="{ on: !isPermissionEnabled(perm.code) }" :disabled="getRoleMeta(selectedRole?.roleCode).locked" @click="setPermissionState(perm.code, false)">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round"><line x1="5" y1="5" x2="19" y2="19"/><line x1="19" y1="5" x2="5" y2="19"/></svg>
                拒绝
              </button>
            </div>
          </div>
        </div>
      </div>

      <div class="drawer-foot">
        <div style="margin-right:auto; font-size:12.5px; color:var(--text-mute); align-self:center">
          共 <b>{{ totalPermissionCount }}</b> 项 · 
          <span style="color:#0f766e">允许 {{ selectedRolePermissionCodes.length }}</span> · 
          <span style="color:#64748b">拒绝 {{ totalPermissionCount - selectedRolePermissionCodes.length }}</span>
        </div>
        <button class="btn" type="button" @click="closePermDrawer">取消</button>
        <button v-if="selectedRole && !getRoleMeta(selectedRole.roleCode).locked" class="btn primary" type="button" :disabled="submitting" @click="saveRolePermissions">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><polyline points="20 6 9 17 4 12"/></svg>
          保存权限配置
        </button>
      </div>
    </aside>

    <!-- DIALOG: CREATE USER MODAL -->
    <div class="modal-mask" :class="{ show: createUserModalVisible }" @click="createUserModalVisible = false">
      <div class="modal-card" style="width:min(580px, calc(100% - 32px))" @click.stop>
        <div class="modal-head">
          <h3>新建用户</h3>
          <button class="close-x" type="button" @click="createUserModalVisible = false">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
          </button>
        </div>
        <div class="modal-body">
          <div class="field-row">
            <div class="label"><span class="req">*</span>用户名</div>
            <div class="input-wrap"><input v-model.trim="newUserForm.username" class="input" placeholder="用于登录系统，例: chensq" /></div>
          </div>
          <div class="field-row">
            <div class="label"><span class="req">*</span>初始密码</div>
            <div class="input-wrap"><input v-model="newUserForm.password" class="input" type="password" placeholder="例: ******" /></div>
          </div>
          <div class="field-row">
            <div class="label"><span class="req">*</span>姓名</div>
            <div class="input-wrap"><input v-model.trim="newUserForm.realName" class="input" placeholder="例：陈思琪" /></div>
          </div>
          <div class="field-row">
            <div class="label">工号</div>
            <div class="input-wrap"><input class="input" placeholder="保存后自动生成" disabled style="font-family:'DM Mono',monospace; opacity:0.7" /></div>
          </div>
          <div class="field-row">
            <div class="label"><span class="req">*</span>邮箱</div>
            <div class="input-wrap"><input v-model.trim="newUserForm.email" class="input" placeholder="name@company.com" style="font-family:'DM Mono',monospace" /></div>
          </div>
          <div class="field-row">
            <div class="label"><span class="req">*</span>手机号</div>
            <div class="input-wrap"><input v-model.trim="newUserForm.contactPhone" class="input" placeholder="138 0000 0000" style="font-family:'DM Mono',monospace" /></div>
          </div>
          <div class="field-row">
            <div class="label"><span class="req">*</span>角色</div>
            <div class="input-wrap">
              <div class="check-grid">
                <label v-for="r in roles" :key="r.id" class="check-item" style="padding: 6px 10px">
                  <input v-model="newUserForm.roleIds" type="checkbox" :value="r.id" />
                  <span style="font-size:12px">{{ r.roleName }}</span>
                </label>
              </div>
            </div>
          </div>
          <div class="field-row">
            <div class="label">归属门店</div>
            <div class="input-wrap">
              <select v-model="newUserForm.store" class="select">
                <option>陆家嘴店</option>
                <option>张江店</option>
                <option>世纪公园店</option>
                <option>静安店</option>
                <option>总部</option>
              </select>
            </div>
          </div>
          <div class="field-row">
            <div class="label">登录方式</div>
            <div class="input-wrap">
              <div class="radio-chips">
                <label><input v-model="newUserForm.loginMethod" type="radio" value="pwd" /><span>设置初始密码</span></label>
                <label><input v-model="newUserForm.loginMethod" type="radio" value="email" /><span>邮件邀请</span></label>
                <label><input v-model="newUserForm.loginMethod" type="radio" value="ding" /><span>钉钉扫码</span></label>
              </div>
            </div>
          </div>
          <div class="field-row" style="border-bottom:0">
            <div class="label">两步验证</div>
            <div class="input-wrap" style="flex-direction:row; align-items:center; gap:12px">
              <label class="switch">
                <input v-model="newUserForm.tfa" type="checkbox" />
                <span class="slider"></span>
              </label>
              <span style="font-size:12.5px; color:var(--text-mute)">首次登录后强制绑定 TOTP</span>
            </div>
          </div>
        </div>
        <div class="modal-foot">
          <button class="btn" type="button" @click="createUserModalVisible = false">取消</button>
          <button class="btn primary" type="button" :disabled="submitting" @click="submitCreateUser">确认创建</button>
        </div>
      </div>
    </div>

    <!-- DIALOG: INVITE USER MODAL -->
    <div class="modal-mask" :class="{ show: inviteModalVisible }" @click="inviteModalVisible = false">
      <div class="modal-card" style="width:min(520px, calc(100% - 32px))" @click.stop>
        <div class="modal-head">
          <h3>邀请用户</h3>
          <button class="close-x" type="button" @click="inviteModalVisible = false">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
          </button>
        </div>
        <div class="modal-body">
          <div class="field-row">
            <div class="label"><span class="req">*</span>邮箱（每行一个）</div>
            <div class="input-wrap">
              <textarea v-model="inviteForm.emails" class="textarea" placeholder="user1@company.com&#10;user2@company.com" style="min-height:100px; font-family:'DM Mono',monospace; font-size:12.5px"></textarea>
            </div>
          </div>
          <div class="field-row">
            <div class="label">默认角色</div>
            <div class="input-wrap">
              <select v-model="inviteForm.roleId" class="select">
                <option v-for="r in roles" :key="r.id" :value="r.id">{{ r.roleName }}</option>
              </select>
            </div>
          </div>
          <div class="field-row" style="border-bottom:0">
            <div class="label">邀请有效期</div>
            <div class="input-wrap">
              <div class="radio-chips">
                <label><input v-model="inviteForm.expiry" type="radio" value="24h" /><span>24 小时</span></label>
                <label><input v-model="inviteForm.expiry" type="radio" value="7d" /><span>7 天</span></label>
                <label><input v-model="inviteForm.expiry" type="radio" value="30d" /><span>30 天</span></label>
              </div>
            </div>
          </div>
        </div>
        <div class="modal-foot">
          <button class="btn" type="button" @click="inviteModalVisible = false">取消</button>
          <button class="btn primary" type="button" @click="submitInvite">发送邀请</button>
        </div>
      </div>
    </div>

    <!-- DIALOG: CREATE ROLE MODAL -->
    <div class="modal-mask" :class="{ show: createRoleModalVisible }" @click="createRoleModalVisible = false">
      <div class="modal-card" style="width:min(520px, calc(100% - 32px))" @click.stop>
        <div class="modal-head">
          <h3>{{ roleFormTitle }}</h3>
          <button class="close-x" type="button" @click="createRoleModalVisible = false">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
          </button>
        </div>
        <div class="modal-body">
          <div v-if="roleFormMode === 'clone'" class="field-row">
            <div class="label"><span class="req">*</span>克隆模板</div>
            <div class="input-wrap">
              <select v-model="newRoleForm.templateRoleId" class="select" @change="syncCloneTemplateFields">
                <option disabled value="">请选择角色</option>
                <option v-for="role in cloneableRoles" :key="role.id" :value="role.id">
                  {{ role.roleName }} · {{ (role.permissionCodes || []).length }} 项权限
                </option>
              </select>
            </div>
          </div>
          <div class="field-row">
            <div class="label"><span class="req">*</span>角色名称</div>
            <div class="input-wrap"><input v-model.trim="newRoleForm.roleName" class="input" placeholder="例：区域经理" /></div>
          </div>
          <div class="field-row">
            <div class="label">数据范围</div>
            <div class="input-wrap">
              <div class="radio-chips">
                <label><input v-model="newRoleForm.scope" type="radio" value="本店" /><span>仅本店</span></label>
                <label><input v-model="newRoleForm.scope" type="radio" value="所属区域" /><span>所属区域</span></label>
                <label><input v-model="newRoleForm.scope" type="radio" value="全部门店" /><span>全部门店</span></label>
              </div>
            </div>
          </div>
          <div class="field-row" style="border-bottom:0">
            <div class="label">描述</div>
            <div class="input-wrap">
              <textarea v-model.trim="newRoleForm.remark" class="textarea" placeholder="简要说明该角色的职责范围..." style="min-height:80px"></textarea>
            </div>
          </div>
        </div>
        <div class="modal-foot">
          <button class="btn" type="button" @click="createRoleModalVisible = false">取消</button>
          <button class="btn primary" type="button" :disabled="submitting" @click="submitCreateRole">{{ roleFormSubmitText }}</button>
        </div>
      </div>
    </div>

    <!-- DIALOG: LOG DETAILS MODAL -->
    <div v-if="showOperationLog" class="modal-mask" :class="{ show: logDetailModalVisible }" @click="logDetailModalVisible = false">
      <div class="modal-card" style="width:min(500px, calc(100% - 32px))" @click.stop>
        <div class="modal-head">
          <h3>日志详情</h3>
          <button class="close-x" type="button" @click="logDetailModalVisible = false">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
          </button>
        </div>
        <div v-if="selectedLog" class="modal-body log-detail-body">
          <div class="detail-grid">
            <div><b>操作时间</b><span>{{ selectedLog.t }} · {{ formatLogDateLabel(selectedLog.t) }}</span></div>
            <div><b>操作人</b><span>{{ selectedLog.op }} · {{ getLogRoleName(selectedLog.role) }}</span></div>
            <div><b>业务模块</b><span>{{ selectedLog.mod }}</span></div>
            <div><b>动作类型</b><span class="log-op-tag" :class="selectedLog.type">{{ getLogTypeName(selectedLog.type) }}</span></div>
            <div><b>操作对象</b><span>{{ selectedLog.obj }}</span></div>
            <div><b>IP 地址</b><span>{{ selectedLog.ip }}</span></div>
            <div><b>客户端终端</b><span>{{ selectedLog.term }}</span></div>
            <div><b>门店区域</b><span>{{ selectedLog.area }}</span></div>
            <div><b>追踪编号</b><span>{{ selectedLog.traceId }}</span></div>
            <div>
              <b>执行结果</b>
              <span :class="selectedLog.result === 'ok' ? 'detail-ok' : 'detail-fail'">
                {{ selectedLog.result === 'ok' ? '成功' : '失败' }}
              </span>
            </div>
            <div>
              <b>风险等级</b>
              <span class="risk-badge" :class="selectedLog.riskLevel">
                <span></span>
                {{ getRiskName(selectedLog.riskLevel) }}
              </span>
            </div>
          </div>
          <div class="risk-reason">
            <b>分析结论</b>
            <p>{{ selectedLog.riskReason || '常规业务操作，暂无异常信号。' }}</p>
          </div>
        </div>
        <div class="modal-foot">
          <button class="btn primary" type="button" @click="logDetailModalVisible = false">关闭</button>
        </div>
      </div>
    </div>

    <!-- TOAST HOST -->
    <Teleport to="body">
      <div v-if="toastMessage" class="toast-host">
        <div class="toast" :class="toastMessage.type">
          <div class="ic">
            <svg v-if="toastMessage.type === 'ok'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><polyline points="20 6 9 17 4 12"/></svg>
            <svg v-else-if="toastMessage.type === 'warn'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
            <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg>
          </div>
          <div class="body">
            <div class="title">{{ toastMessage.title }}</div>
            <div v-if="toastMessage.desc" class="desc">{{ toastMessage.desc }}</div>
          </div>
          <button class="close" type="button" @click="toastMessage = null">×</button>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import {
  createRole,
  createUser,
  deleteUser,
  listPermissions,
  listRoles,
  listUsers,
  updateRolePermissions,
  updateUser
} from '../../api/user'

// Active Tab
const activeTab = ref('users')
const showOperationLog = false

// API Data
const allUsers = ref([])
const roles = ref([])
const permissionModules = ref([])
const loading = ref(false)
const submitting = ref(false)

// Select / Checkbox State
const selectedUserIds = ref([])

// Filters
const userFilters = reactive({
  keyword: '',
  role: '',
  store: '',
  status: ''
})

const logFilters = reactive({
  keyword: '',
  operator: '',
  module: '',
  type: '',
  result: '',
  riskOnly: false,
  timeRange: '近 7 天'
})

// Pagination
const userPage = ref(1)
const userPageSize = ref(10)

const logPage = ref(1)
const logPageSize = ref(10)

// Toast
const toastMessage = ref(null)
function showToast(title, desc = '', type = 'ok') {
  toastMessage.value = { title, desc, type }
  setTimeout(() => {
    toastMessage.value = null
  }, 4000)
}

// User Drawer State
const userDrawerVisible = ref(false)
const selectedUser = ref(null)
const userForm = reactive({
  realName: '',
  email: '',
  contactPhone: '',
  password: '',
  status: 1,
  roleIds: [],
  store: '',
  dept: '',
  tfa: false,
  loginLimit: 'none',
  timeout: '30'
})

// Invite Modal State
const inviteModalVisible = ref(false)
const inviteForm = reactive({
  emails: '',
  roleId: '',
  expiry: '7d'
})

// Create User Modal State
const createUserModalVisible = ref(false)
const newUserForm = reactive({
  username: '',
  password: '',
  realName: '',
  email: '',
  contactPhone: '',
  roleIds: [],
  store: '陆家嘴店',
  loginMethod: 'pwd',
  tfa: false
})

// Create Role Modal State
const createRoleModalVisible = ref(false)
const roleFormMode = ref('create')
const newRoleForm = reactive({
  roleName: '',
  scope: '本店',
  remark: '',
  templateRoleId: ''
})

// Permission Configuration Drawer State
const permDrawerVisible = ref(false)
const selectedRole = ref(null)
const selectedRolePermissionCodes = ref([])
const permSearchKeyword = ref('')

// Log Details Modal State
const logDetailModalVisible = ref(false)
const selectedLog = ref(null)

// static role configuration configurations matching the prototype
const ROLE_CONFIGS = {
  'ADMIN': { color: '#d97706', icon: 'crown', desc: '系统最高权限，可访问所有模块和数据。', scope: '全部门店', locked: true },
  'STORE_MGR': { color: '#7c3aed', icon: 'shield', desc: '负责所属门店的日常运营、库存、销售、人员管理。', scope: '本店', locked: false },
  'PURCHASE': { color: '#1e63e0', icon: 'cart', desc: '管理采购订单、供应商档案、合同价格、入库审核。', scope: '全部门店', locked: false },
  'SALES': { color: '#0e7490', icon: 'chart', desc: '管理销售订单、退款、促销活动、会员体系。', scope: '所属区域', locked: false },
  'STOCK': { color: '#0f766e', icon: 'box', desc: '负责门店库存盘点、调拨、出入库流水核对。', scope: '本店', locked: false },
  'CASHIER': { color: '#be185d', icon: 'wallet', desc: '前台收银、扫码出库、退换货操作、会员积分录入。', scope: '本店 · 收银台', locked: false },
  'FINANCE': { color: '#92400e', icon: 'yen', desc: '应付对账、利润报表、发票管理、对外结算审批。', scope: '全部门店', locked: false },
  'VIEWER': { color: '#64748b', icon: 'eye', desc: '仅查看权限，无法新建、编辑或删除。适用于外部顾问。', scope: '全部门店', locked: false }
}

const BASIC_ROLE_PERMISSION_CODES = ['dashboard:view', 'product:view', 'stock:view', 'report:view']

function getRoleMeta(roleCode) {
  const code = roleCode ? roleCode.toUpperCase() : '';
  if (ROLE_CONFIGS[code]) return ROLE_CONFIGS[code];
  for (const k in ROLE_CONFIGS) {
    if (code.includes(k)) return ROLE_CONFIGS[k];
  }
  return { color: '#64748b', icon: 'eye', desc: '自定义角色，可自由配置系统权限范围。', scope: '全部门店', locked: false };
}

// User Metadata Map
function getUserStoreDept(user) {
  const roleCode = user.roles?.[0]?.roleCode || '';
  if (user.username === 'admin' || roleCode === 'ADMIN') {
    return { store: '总部', dept: '总部 / 运营中心' }
  }
  if (roleCode.includes('STORE_MGR') || roleCode.includes('MGR')) {
    return { store: '陆家嘴店', dept: '陆家嘴店 / 经营部' }
  } else if (roleCode.includes('PURCHASE')) {
    return { store: '总部', dept: '总部 / 供应链部' }
  } else if (roleCode.includes('SALES')) {
    return { store: '张江店', dept: '张江店 / 销售部' }
  } else if (roleCode.includes('STOCK')) {
    return { store: '陆家嘴店', dept: '陆家嘴店 / 仓储部' }
  } else if (roleCode.includes('CASHIER')) {
    return { store: '世纪公园店', dept: '世纪公园店 / 前场' }
  } else if (roleCode.includes('FINANCE')) {
    return { store: '总部', dept: '总部 / 财务部' }
  } else if (roleCode.includes('VIEWER')) {
    return { store: '总部', dept: '审计组 / 外部' }
  }
  return { store: '总部', dept: '运营部' }
}

function getUserAvatarColor(user) {
  const colors = [
    ['#fbbf24', '#d97706'], // admin
    ['#c084fc', '#7c3aed'], // manager
    ['#4d9bff', '#1e63e0'], // purchase
    ['#22d3ee', '#0e7490'], // sales
    ['#34d399', '#0f766e'], // stock
    ['#f472b6', '#be185d'], // cashier
    ['#fbbf24', '#92400e'], // finance
    ['#94a3b8', '#475569']  // viewer
  ];
  return colors[user.id % colors.length];
}

function getRoleClass(roleCode) {
  if (!roleCode) return 'viewer';
  const code = roleCode.toLowerCase();
  if (code === 'admin') return 'admin';
  if (code.includes('mgr') || code.includes('manager')) return 'manager';
  if (code.includes('purchase')) return 'purchase';
  if (code.includes('sales')) return 'sales';
  if (code.includes('stock')) return 'stock';
  if (code.includes('cashier')) return 'cashier';
  if (code.includes('finance')) return 'finance';
  return 'viewer';
}

function mockIpAddress(userId) {
  const ips = ['192.168.1.12', '10.20.1.46', '192.168.1.34', '10.21.2.18', '10.20.1.88', '10.22.3.04', '192.168.1.55', '10.23.4.21'];
  return ips[userId % ips.length];
}

// Operation log sample data and analysis helpers
const LOG_TYPE_NAMES = {
  create: '新建',
  update: '修改',
  delete: '删除',
  login: '登录',
  export: '导出',
  approve: '审批'
}

const LOG_ROLE_NAMES = {
  admin: '超级管理员',
  manager: '店长',
  purchase: '采购专员',
  sales: '销售主管',
  stock: '库存专员',
  cashier: '收银员',
  finance: '财务',
  viewer: '审计访客'
}

const RISK_NAMES = {
  high: '高风险',
  medium: '中风险',
  low: '低风险'
}

const STATIC_LOGS = [
  { id: 'AUD-24001', t: makeLogTime(12), op: '林志远', role: 'admin', mod: '用户与权限', type: 'update', obj: '修改角色权限 ROLE_PURCHASE', ip: '192.168.1.12', term: 'Mac · Chrome', result: 'ok', area: '总部 / 运营中心', riskLevel: 'high', riskReason: '采购角色权限发生变更，属于高权限配置动作，建议复核变更范围。' },
  { id: 'AUD-24002', t: makeLogTime(18), op: '陈思琪', role: 'sales', mod: '用户与权限', type: 'login', obj: '账号登录（密码错误）', ip: '10.21.2.55', term: 'Win · Chrome', result: 'fail', area: '张江店 / 销售部', riskLevel: 'medium', riskReason: '同一账号出现密码错误，需要关注是否为本人误输或撞库尝试。' },
  { id: 'AUD-24003', t: makeLogTime(24), op: '陈思琪', role: 'sales', mod: '用户与权限', type: 'login', obj: '账号登录（密码错误）', ip: '10.21.2.55', term: 'Win · Chrome', result: 'fail', area: '张江店 / 销售部', riskLevel: 'medium', riskReason: '短时间重复失败登录，建议结合账号锁定策略处理。' },
  { id: 'AUD-24004', t: makeLogTime(47), op: '周晓琳', role: 'manager', mod: '库存中心', type: 'create', obj: '盘点单 ST-2026-0518', ip: '10.20.1.46', term: 'Mac · Safari', result: 'ok', area: '陆家嘴店 / 经营部', riskLevel: 'low', riskReason: '常规盘点创建动作。' },
  { id: 'AUD-24005', t: makeLogTime(66), op: '张志伟', role: 'purchase', mod: '采购订单', type: 'approve', obj: 'PO-2026-0866 (¥ 48,260)', ip: '192.168.1.34', term: 'Win · Edge', result: 'ok', area: '总部 / 供应链部', riskLevel: 'medium', riskReason: '大额采购审批完成，建议保留审批链路与入库结果核对。' },
  { id: 'AUD-24006', t: makeLogTime(94), op: '陈思琪', role: 'sales', mod: '销售管理', type: 'create', obj: '促销活动 端午粽情礼遇', ip: '10.21.2.18', term: 'iPad · Safari', result: 'ok', area: '张江店 / 销售部', riskLevel: 'low', riskReason: '常规促销活动创建动作。' },
  { id: 'AUD-24007', t: makeLogTime(128), op: '梁雅婷', role: 'sales', mod: '销售管理', type: 'update', obj: 'SO-2026-1284 退款金额', ip: '10.23.4.32', term: 'Win · Chrome', result: 'ok', area: '世纪公园店 / 销售部', riskLevel: 'medium', riskReason: '退款金额变更会影响财务对账，建议与退款单据一致性核对。' },
  { id: 'AUD-24008', t: makeLogTime(180), op: '吴俊辉', role: 'purchase', mod: '供应商', type: 'update', obj: '合同 CT-2024-0042 续签', ip: '192.168.1.41', term: 'Mac · Chrome', result: 'ok', area: '总部 / 供应链部', riskLevel: 'low', riskReason: '供应商合同续签记录正常。' },
  { id: 'AUD-24009', t: makeLogTime(245), op: '罗韵芝', role: 'finance', mod: '数据报表', type: 'export', obj: '5 月利润报表.xlsx', ip: '192.168.1.58', term: 'Win · Chrome', result: 'ok', area: '总部 / 财务部', riskLevel: 'medium', riskReason: '财务报表导出涉及敏感经营数据，建议确认导出用途。' },
  { id: 'AUD-24010', t: makeLogTime(420), op: '张志伟', role: 'purchase', mod: '采购订单', type: 'create', obj: 'PO-2026-0867', ip: '192.168.1.34', term: 'Win · Edge', result: 'ok', area: '总部 / 供应链部', riskLevel: 'low', riskReason: '常规采购订单创建动作。' },
  { id: 'AUD-24011', t: makeLogTime(780), op: '孙佳怡', role: 'manager', mod: '商品管理', type: 'update', obj: 'SKU-887234 价格调整 ¥ 36.80', ip: '10.23.4.21', term: 'Mac · Safari', result: 'ok', area: '世纪公园店 / 经营部', riskLevel: 'medium', riskReason: '价格调整会影响销售毛利，建议抽查审批依据。' },
  { id: 'AUD-24012', t: makeLogTime(990), op: '王建国', role: 'stock', mod: '库存中心', type: 'create', obj: '调拨单 TR-2026-0102', ip: '10.20.1.88', term: 'Mac · Chrome', result: 'ok', area: '陆家嘴店 / 仓储部', riskLevel: 'low', riskReason: '常规库存调拨创建动作。' },
  { id: 'AUD-24013', t: makeLogTime(1220), op: '何嘉诚', role: 'stock', mod: '库存中心', type: 'update', obj: '盘点单 ST-2026-0517 提交', ip: '10.22.3.18', term: 'Android · Chrome', result: 'ok', area: '静安店 / 仓储部', riskLevel: 'low', riskReason: '盘点提交动作正常。' },
  { id: 'AUD-24014', t: makeLogTime(1480), op: '徐子轩', role: 'cashier', mod: '用户与权限', type: 'login', obj: '账号登录（连续 3 次失败 → 锁定）', ip: '10.21.2.55', term: 'Win · Chrome', result: 'fail', area: '世纪公园店 / 前场', riskLevel: 'high', riskReason: '账号已触发连续失败锁定，应联系本人确认并检查来源终端。' },
  { id: 'AUD-24015', t: makeLogTime(1660), op: '赵立新', role: 'finance', mod: '供应商', type: 'approve', obj: '对账单 RC-2026-0086 (¥ 86,420)', ip: '192.168.1.55', term: 'Win · Chrome', result: 'ok', area: '总部 / 财务部', riskLevel: 'medium', riskReason: '大额对账审批完成，建议与供应商结算记录联查。' },
  { id: 'AUD-24016', t: makeLogTime(2120), op: '潘子默', role: 'viewer', mod: '数据报表', type: 'export', obj: '审计样本 2026Q1.csv', ip: '203.94.18.7', term: 'Mac · Firefox', result: 'ok', area: '外部审计 / 远程', riskLevel: 'high', riskReason: '外部公网 IP 导出审计样本，建议核对授权窗口和导出范围。' },
  { id: 'AUD-24017', t: makeLogTime(2860), op: '林志远', role: 'admin', mod: '系统设置', type: 'update', obj: '修改打印模板「销售小票」', ip: '192.168.1.12', term: 'Mac · Chrome', result: 'ok', area: '总部 / 运营中心', riskLevel: 'low', riskReason: '系统模板维护动作正常。' },
  { id: 'AUD-24018', t: makeLogTime(4300), op: '黄梓涵', role: 'sales', mod: '销售管理', type: 'delete', obj: '促销活动 母亲节礼盒（已结束）', ip: '10.20.1.92', term: 'Mac · Chrome', result: 'ok', area: '陆家嘴店 / 销售部', riskLevel: 'medium', riskReason: '删除类操作不可逆，建议确认对象已结束且无关联未结算单据。' },
  { id: 'AUD-24019', t: makeLogTime(6200), op: '林志远', role: 'admin', mod: '用户与权限', type: 'create', obj: '新建用户 梁雅婷', ip: '192.168.1.12', term: 'Mac · Chrome', result: 'ok', area: '总部 / 运营中心', riskLevel: 'medium', riskReason: '新增用户需要确认所属角色、门店与入职信息一致。' },
  { id: 'AUD-24020', t: makeLogTime(9300), op: '林志远', role: 'admin', mod: '用户与权限', type: 'delete', obj: '删除停用账号 old_cashier_03', ip: '192.168.1.12', term: 'Mac · Chrome', result: 'ok', area: '总部 / 运营中心', riskLevel: 'high', riskReason: '账号删除会影响审计追溯，建议优先停用并保留历史记录。' }
]

const operationLogs = computed(() => {
  return STATIC_LOGS.map((log, index) => ({
    traceId: `TRACE-${String(index + 1).padStart(4, '0')}`,
    area: '总部',
    riskLevel: deriveRiskLevel(log),
    riskReason: deriveRiskReason(log),
    ...log
  }))
})

const logOperators = computed(() => {
  const ops = new Set(operationLogs.value.map(l => l.op))
  return Array.from(ops)
})

const logModules = computed(() => {
  const mods = new Set(operationLogs.value.map(l => l.mod))
  return Array.from(mods)
})

function getLogTypeName(type) {
  return LOG_TYPE_NAMES[type] || type
}

function getLogRoleName(role) {
  return LOG_ROLE_NAMES[role] || role || '-'
}

function getRiskName(level) {
  return RISK_NAMES[level] || '低风险'
}

function pad2(value) {
  return String(value).padStart(2, '0')
}

function formatLocalDate(date) {
  return `${date.getFullYear()}-${pad2(date.getMonth() + 1)}-${pad2(date.getDate())}`
}

function formatLocalDateTime(date) {
  return `${formatLocalDate(date)} ${pad2(date.getHours())}:${pad2(date.getMinutes())}:${pad2(date.getSeconds())}`
}

function makeLogTime(minutesAgo) {
  const date = new Date()
  date.setMinutes(date.getMinutes() - minutesAgo)
  return formatLocalDateTime(date)
}

function parseLogTime(value) {
  return new Date(String(value).replace(' ', 'T')).getTime()
}

function formatLogClock(value) {
  const [, clock = ''] = String(value).split(' ')
  return clock
}

function formatLogDateLabel(value) {
  const date = new Date(String(value).replace(' ', 'T'))
  const today = new Date()
  const yesterday = new Date()
  yesterday.setDate(today.getDate() - 1)
  const dateOnly = formatLocalDate(date)
  if (dateOnly === formatLocalDate(today)) {
    return '今天'
  }
  if (dateOnly === formatLocalDate(yesterday)) {
    return '昨天'
  }
  return dateOnly.slice(5)
}

function isLogInRange(log, range) {
  if (range === '全部时间') {
    return true
  }
  const windows = {
    '近 24 小时': 24 * 60 * 60 * 1000,
    '近 7 天': 7 * 24 * 60 * 60 * 1000,
    '近 30 天': 30 * 24 * 60 * 60 * 1000
  }
  const windowMs = windows[range] || windows['近 7 天']
  return Date.now() - parseLogTime(log.t) <= windowMs
}

function deriveRiskLevel(log) {
  if (log.result === 'fail' || log.type === 'delete') {
    return 'high'
  }
  if (log.type === 'update' && log.mod === '用户与权限') {
    return 'high'
  }
  if (log.type === 'export' || log.type === 'approve') {
    return 'medium'
  }
  return 'low'
}

function deriveRiskReason(log) {
  if (log.result === 'fail') {
    return '失败操作需要复核账号、来源 IP 与终端环境。'
  }
  if (log.type === 'delete') {
    return '删除动作会影响业务追溯，应确认对象已完成归档或停用。'
  }
  if (log.type === 'export') {
    return '导出动作可能涉及敏感数据，建议核对导出范围和用途。'
  }
  return '常规业务操作，暂无异常信号。'
}

// Dynamic Computeds
const totalPermissionCount = computed(() => {
  return permissionModules.value.reduce((sum, mod) => sum + mod.permissions.length, 0)
})

const cloneableRoles = computed(() => {
  return roles.value.filter(role => !getRoleMeta(role.roleCode).locked)
})

const selectedTemplateRole = computed(() => {
  return cloneableRoles.value.find(role => Number(role.id) === Number(newRoleForm.templateRoleId)) || null
})

const roleFormTitle = computed(() => roleFormMode.value === 'clone' ? '克隆角色' : '新建角色')
const roleFormSubmitText = computed(() => roleFormMode.value === 'clone' ? '确认克隆' : '确认创建')

// Client-side Users Filter
const filteredUsers = computed(() => {
  let list = allUsers.value
  const kw = (userFilters.keyword || '').trim().toLowerCase()
  
  if (kw) {
    list = list.filter(u => 
      (u.employeeNo || '').toLowerCase().includes(kw) ||
      (u.username || '').toLowerCase().includes(kw) ||
      (u.realName || '').toLowerCase().includes(kw) ||
      (u.email || '').toLowerCase().includes(kw) ||
      (u.contactPhone || '').toLowerCase().includes(kw)
    )
  }
  
  if (userFilters.role) {
    list = list.filter(u => (u.roles || []).some(r => r.roleCode === userFilters.role))
  }
  
  if (userFilters.store) {
    list = list.filter(u => getUserStoreDept(u).store === userFilters.store)
  }
  
  if (userFilters.status !== '') {
    list = list.filter(u => Number(u.status) === Number(userFilters.status))
  }
  
  return list
})

const paginatedUsers = computed(() => {
  const start = (userPage.value - 1) * userPageSize.value
  return filteredUsers.value.slice(start, start + userPageSize.value)
})

const totalUserPages = computed(() => {
  return Math.ceil(filteredUsers.value.length / userPageSize.value) || 1
})

// Multi-select user mapping
const isAllSelected = computed(() => {
  const pageItems = paginatedUsers.value
  if (pageItems.length === 0) return false
  return pageItems.every(u => selectedUserIds.value.includes(u.id))
})

function toggleSelectAll(e) {
  const checked = e.target.checked
  const pageIds = paginatedUsers.value.map(u => u.id)
  if (checked) {
    selectedUserIds.value = Array.from(new Set([...selectedUserIds.value, ...pageIds]))
  } else {
    selectedUserIds.value = selectedUserIds.value.filter(id => !pageIds.includes(id))
  }
}

function toggleSelectUser(id) {
  const idx = selectedUserIds.value.indexOf(id)
  if (idx > -1) {
    selectedUserIds.value.splice(idx, 1)
  } else {
    selectedUserIds.value.push(id)
  }
}

// Client-side Logs Filter
const filteredLogs = computed(() => {
  let list = operationLogs.value
  const kw = (logFilters.keyword || '').trim().toLowerCase()
  
  if (kw) {
    list = list.filter(l => 
      (l.obj || '').toLowerCase().includes(kw) ||
      (l.op || '').toLowerCase().includes(kw) ||
      (l.ip || '').toLowerCase().includes(kw) ||
      (l.riskReason || '').toLowerCase().includes(kw)
    )
  }
  if (logFilters.operator) {
    list = list.filter(l => l.op === logFilters.operator)
  }
  if (logFilters.module) {
    list = list.filter(l => l.mod === logFilters.module)
  }
  if (logFilters.type) {
    list = list.filter(l => l.type === logFilters.type)
  }
  if (logFilters.result) {
    list = list.filter(l => l.result === logFilters.result)
  }
  if (logFilters.riskOnly) {
    list = list.filter(l => l.riskLevel !== 'low' || l.result === 'fail')
  }
  list = list.filter(l => isLogInRange(l, logFilters.timeRange))
  return [...list].sort((a, b) => parseLogTime(b.t) - parseLogTime(a.t))
})

const logInsights = computed(() => {
  const list = filteredLogs.value
  const total = list.length
  const failCount = list.filter(l => l.result === 'fail').length
  const riskCount = list.filter(l => l.riskLevel === 'high' || l.riskLevel === 'medium' || l.result === 'fail').length
  const successRate = total === 0 ? 0 : Math.round(((total - failCount) / total) * 100)
  return {
    total,
    failCount,
    riskCount,
    successRate,
    topModule: topValue(list, 'mod'),
    topOperator: topValue(list, 'op')
  }
})

const highRiskLogs = computed(() => {
  return filteredLogs.value
    .filter(log => log.riskLevel !== 'low' || log.result === 'fail')
    .slice(0, 5)
})

const logTypeStats = computed(() => {
  const counts = filteredLogs.value.reduce((acc, log) => {
    acc[log.type] = (acc[log.type] || 0) + 1
    return acc
  }, {})
  const max = Math.max(1, ...Object.values(counts))
  return Object.keys(LOG_TYPE_NAMES).map(type => ({
    type,
    count: counts[type] || 0,
    percent: Math.round(((counts[type] || 0) / max) * 100)
  })).filter(item => item.count > 0)
})

const operatorRiskRanking = computed(() => {
  const map = new Map()
  filteredLogs.value.forEach(log => {
    if (!map.has(log.op)) {
      map.set(log.op, { operator: log.op, total: 0, riskCount: 0, failCount: 0 })
    }
    const item = map.get(log.op)
    item.total += 1
    if (log.riskLevel !== 'low') {
      item.riskCount += 1
    }
    if (log.result === 'fail') {
      item.failCount += 1
    }
  })
  return Array.from(map.values())
    .sort((a, b) => b.riskCount - a.riskCount || b.failCount - a.failCount || b.total - a.total)
    .slice(0, 5)
})

function topValue(list, key) {
  const counts = list.reduce((acc, item) => {
    const value = item[key]
    if (value) {
      acc[value] = (acc[value] || 0) + 1
    }
    return acc
  }, {})
  return Object.entries(counts).sort((a, b) => b[1] - a[1])[0]?.[0] || ''
}

const paginatedLogs = computed(() => {
  const start = (logPage.value - 1) * logPageSize.value
  return filteredLogs.value.slice(start, start + logPageSize.value)
})

const totalLogPages = computed(() => {
  return Math.ceil(filteredLogs.value.length / logPageSize.value) || 1
})

// Permissions Filter Inside Config Drawer
const filteredPermissionModules = computed(() => {
  const kw = permSearchKeyword.value.trim().toLowerCase()
  if (!kw) return permissionModules.value
  return permissionModules.value.map(mod => {
    return {
      ...mod,
      permissions: mod.permissions.filter(p => 
        (p.name || '').toLowerCase().includes(kw) ||
        (p.code || '').toLowerCase().includes(kw)
      )
    }
  }).filter(mod => mod.permissions.length > 0)
})

// Load Data from APIs
async function loadData() {
  loading.value = true
  try {
    const data = await listUsers({ page: 1, pageSize: 1000 })
    allUsers.value = data.items || []
  } catch (error) {
    showToast('获取用户失败', error.message, 'err')
  } finally {
    loading.value = false
  }
}

async function loadRoles() {
  try {
    roles.value = await listRoles()
  } catch (error) {
    showToast('获取角色失败', error.message, 'err')
  }
}

async function loadPermissionModules() {
  try {
    permissionModules.value = await listPermissions()
  } catch (error) {
    showToast('获取权限包失败', error.message, 'err')
  }
}

// Reset User Filters
function resetUserFilters() {
  userFilters.keyword = ''
  userFilters.role = ''
  userFilters.store = ''
  userFilters.status = ''
  userPage.value = 1
}

// Reset Log Filters
function resetLogFilters() {
  logFilters.keyword = ''
  logFilters.operator = ''
  logFilters.module = ''
  logFilters.type = ''
  logFilters.result = ''
  logFilters.riskOnly = false
  logFilters.timeRange = '近 7 天'
  logPage.value = 1
}

function applyRiskFilter() {
  logFilters.riskOnly = !logFilters.riskOnly
  logFilters.result = ''
  logFilters.timeRange = '近 7 天'
  logPage.value = 1
}

function showRiskLogs() {
  logFilters.riskOnly = true
  logFilters.result = ''
  logFilters.timeRange = '近 7 天'
  logPage.value = 1
}

// Calculate months joined
function calcMonths(createTime) {
  if (!createTime) return 1
  const joinedDate = new Date(createTime)
  const currentDate = new Date()
  const diffTime = Math.abs(currentDate - joinedDate)
  const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24))
  return Math.max(1, Math.ceil(diffDays / 30))
}

// Formats
function formatDateTime(value) {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 19)
}

function formatShortDate(value) {
  if (!value) return '-'
  return String(value).slice(0, 10)
}

// CSV Export
function exportUsersCSV() {
  let csvContent = 'data:text/csv;charset=utf-8,\uFEFF'
  csvContent += '工号,用户名,姓名,角色,门店,部门,联系方式,状态,注册时间\n'
  
  allUsers.value.forEach(u => {
    const rNames = (u.roles || []).map(r => r.roleName).join(';')
    const sd = getUserStoreDept(u)
    const statusText = Number(u.status) === 1 ? '在职' : '停用'
    const row = [
      u.employeeNo || '',
      u.username || '',
      u.realName || '',
      rNames || '普通用户',
      sd.store,
      sd.dept.split('/').pop().trim(),
      u.contactPhone || '',
      statusText,
      formatShortDate(u.createTime)
    ].map(val => `"${String(val).replace(/"/g, '""')}"`).join(',')
    csvContent += row + '\n'
  })
  
  const encodedUri = encodeURI(csvContent)
  const link = document.createElement('a')
  link.setAttribute('href', encodedUri)
  link.setAttribute('download', `员工名册_${formatShortDate(new Date())}.csv`)
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  
  showToast('已导出员工名册', `${allUsers.value.length} 条记录 · CSV`, 'ok')
}

// Invite Modals
function openInviteModal() {
  inviteForm.emails = ''
  inviteForm.roleId = roles.value[0]?.id || ''
  inviteForm.expiry = '7d'
  inviteModalVisible.value = true
}

function submitInvite() {
  if (!inviteForm.emails.trim()) {
    showToast('请输入电子邮箱', '每行支持一个邮箱地址', 'warn')
    return
  }
  const count = inviteForm.emails.split('\n').filter(e => e.trim().includes('@')).length
  inviteModalVisible.value = false
  showToast(`已发送 ${count} 封邀请邮件`, '受邀人员点击邮件即可注册并登录', 'ok')
}

// User Creation Modal
function openCreateUser() {
  newUserForm.username = ''
  newUserForm.password = ''
  newUserForm.realName = ''
  newUserForm.email = ''
  newUserForm.contactPhone = ''
  newUserForm.roleIds = []
  newUserForm.store = '陆家嘴店'
  newUserForm.loginMethod = 'pwd'
  newUserForm.tfa = false
  
  createUserModalVisible.value = true
}

async function submitCreateUser() {
  if (!newUserForm.username || !newUserForm.password || !newUserForm.contactPhone || newUserForm.roleIds.length === 0) {
    showToast('请填写必填项', '用户名、密码、联系方式和所属角色为必填', 'warn')
    return
  }
  submitting.value = true
  try {
    await createUser({
      username: newUserForm.username.trim(),
      password: newUserForm.password,
      realName: newUserForm.realName.trim(),
      email: newUserForm.email.trim() || null,
      contactPhone: newUserForm.contactPhone.trim(),
      status: 1,
      roleIds: newUserForm.roleIds
    })
    createUserModalVisible.value = false
    showToast('新用户创建成功', '系统工号已自动分配', 'ok')
    await loadData()
  } catch (error) {
    showToast('创建用户失败', error.message, 'err')
  } finally {
    submitting.value = false
  }
}

// User Right Drawer Details
function openUserDrawer(user) {
  selectedUser.value = user
  const sd = getUserStoreDept(user)
  
  userForm.realName = user.realName || ''
  userForm.email = user.email || ''
  userForm.contactPhone = user.contactPhone || ''
  userForm.password = ''
  userForm.status = user.status
  userForm.roleIds = (user.roles || []).map(r => r.id)
  userForm.store = sd.store
  userForm.dept = sd.dept.split('/').pop().trim()
  userForm.tfa = user.id % 2 === 1 || user.username === 'admin'
  userForm.loginLimit = 'none'
  userForm.timeout = '30'
  
  userDrawerVisible.value = true
}

function closeUserDrawer() {
  userDrawerVisible.value = false
  selectedUser.value = null
}

async function saveUserDrawer() {
  if (!selectedUser.value) return
  if (!userForm.contactPhone || userForm.roleIds.length === 0) {
    showToast('必填项缺失', '联系方式与角色不能为空', 'warn')
    return
  }
  submitting.value = true
  try {
    await updateUser(selectedUser.value.id, {
      realName: userForm.realName,
      email: userForm.email || null,
      contactPhone: userForm.contactPhone,
      status: userForm.status,
      roleIds: userForm.roleIds,
      newPassword: userForm.password || null
    })
    closeUserDrawer()
    showToast('用户资料更新成功', '所有修改已保存至服务器', 'ok')
    await loadData()
  } catch (error) {
    showToast('更新失败', error.message, 'err')
  } finally {
    submitting.value = false
  }
}

async function toggleStatusDirect(user) {
  const nextStatus = Number(user.status) === 1 ? 0 : 1
  const actionText = nextStatus === 1 ? '启用' : '禁用'
  if (!window.confirm(`确定要${actionText}用户 ${user.realName || user.username} 吗？`)) {
    return
  }
  try {
    await updateUser(user.id, {
      realName: user.realName,
      email: user.email,
      contactPhone: user.contactPhone,
      status: nextStatus,
      roleIds: (user.roles || []).map(r => r.id),
      newPassword: null
    })
    showToast(`用户账号${actionText}成功`, user.realName || user.username, 'ok')
    if (selectedUser.value && selectedUser.value.id === user.id) {
      userForm.status = nextStatus
      selectedUser.value.status = nextStatus
    }
    await loadData()
  } catch (error) {
    showToast('操作失败', error.message, 'err')
  }
}

async function resetPasswordDirect(user) {
  if (!window.confirm(`确定要重置用户 ${user.realName || user.username} 的密码吗？`)) {
    return
  }
  try {
    // Generates a temporary random password and sends via API
    const tempPassword = 'Reset_' + Math.floor(100000 + Math.random() * 900000)
    await updateUser(user.id, {
      realName: user.realName,
      email: user.email,
      contactPhone: user.contactPhone,
      status: user.status,
      roleIds: (user.roles || []).map(r => r.id),
      newPassword: tempPassword
    })
    showToast('密码重置成功', `临时密码 ${tempPassword} 已生成并可通过邮件通知`, 'ok')
  } catch (error) {
    showToast('重置密码失败', error.message, 'err')
  }
}

// Role Creation Modal
function openCreateRole() {
  roleFormMode.value = 'create'
  newRoleForm.roleName = ''
  newRoleForm.remark = ''
  newRoleForm.scope = '本店'
  newRoleForm.templateRoleId = ''
  createRoleModalVisible.value = true
}

function openCloneRole(sourceRole = null) {
  const templateRole = sourceRole && !getRoleMeta(sourceRole.roleCode).locked
    ? sourceRole
    : cloneableRoles.value[0]
  if (!templateRole) {
    showToast('暂无可克隆角色', '请先创建可作为模板的角色', 'warn')
    return
  }
  roleFormMode.value = 'clone'
  newRoleForm.templateRoleId = templateRole.id
  syncCloneTemplateFields()
  createRoleModalVisible.value = true
}

function syncCloneTemplateFields() {
  const templateRole = selectedTemplateRole.value
  if (!templateRole) {
    return
  }
  newRoleForm.roleName = buildUniqueCloneRoleName(templateRole.roleName)
  newRoleForm.scope = normalizeRoleScope(getRoleMeta(templateRole.roleCode).scope)
  newRoleForm.remark = `克隆自 ${templateRole.roleName}`
}

function buildUniqueCloneRoleName(templateRoleName) {
  const baseName = `${templateRoleName || '角色'} 副本`
  const existingNames = new Set(roles.value.map(role => role.roleName))
  let candidate = truncateRoleName(baseName)
  if (!existingNames.has(candidate)) {
    return candidate
  }
  for (let index = 2; index < 100; index += 1) {
    candidate = truncateRoleName(baseName, ` ${index}`)
    if (!existingNames.has(candidate)) {
      return candidate
    }
  }
  return truncateRoleName(baseName, ` ${Date.now().toString().slice(-4)}`)
}

function truncateRoleName(baseName, suffix = '') {
  const maxBaseLength = Math.max(1, 50 - suffix.length)
  return `${String(baseName).slice(0, maxBaseLength)}${suffix}`.trim()
}

function normalizeRoleScope(scope) {
  const text = String(scope || '')
  if (text.includes('全部')) {
    return '全部门店'
  }
  if (text.includes('区域')) {
    return '所属区域'
  }
  return '本店'
}

function buildRoleRemark() {
  const remark = (newRoleForm.remark || '').trim()
  return `数据范围: ${newRoleForm.scope}。${remark}`.trim()
}

function getDefaultRolePermissionCodes() {
  const userRole = roles.value.find(role => role.roleCode === 'USER')
  if (userRole?.permissionCodes?.length) {
    return [...userRole.permissionCodes]
  }
  return [...BASIC_ROLE_PERMISSION_CODES]
}

function getRolePermissionCodesForSubmit() {
  if (roleFormMode.value === 'clone') {
    return [...(selectedTemplateRole.value?.permissionCodes || [])]
  }
  return getDefaultRolePermissionCodes()
}

async function submitCreateRole() {
  const roleName = newRoleForm.roleName.trim()
  if (!roleName) {
    showToast('请输入角色名称', '', 'warn')
    return
  }
  if (roleName.length > 50) {
    showToast('角色名称过长', '最多 50 个字符', 'warn')
    return
  }
  if (roleFormMode.value === 'clone' && !selectedTemplateRole.value) {
    showToast('请选择克隆模板', '', 'warn')
    return
  }
  const permissionCodes = getRolePermissionCodesForSubmit()
  if (!permissionCodes.length) {
    showToast('权限模板为空', '请选择包含权限的角色模板', 'warn')
    return
  }
  const remark = buildRoleRemark()
  if (remark.length > 100) {
    showToast('角色描述过长', '最多 100 个字符', 'warn')
    return
  }
  submitting.value = true
  try {
    await createRole({
      roleName,
      remark,
      permissionCodes: getRolePermissionCodesForSubmit()
    })
    createRoleModalVisible.value = false
    showToast(
      roleFormMode.value === 'clone' ? '角色克隆成功' : '角色创建成功',
      roleFormMode.value === 'clone' ? `已从 ${selectedTemplateRole.value?.roleName || '模板角色'} 复制权限` : '请在卡片上配置具体权限',
      'ok'
    )
    await loadRoles()
  } catch (error) {
    showToast(roleFormMode.value === 'clone' ? '克隆角色失败' : '创建角色失败', error.message, 'err')
  } finally {
    submitting.value = false
  }
}

// Permission configuration Drawer
function openPermissionDrawer(role) {
  selectedRole.value = role
  selectedRolePermissionCodes.value = [...(role.permissionCodes || [])]
  permSearchKeyword.value = ''
  permDrawerVisible.value = true
}

function closePermDrawer() {
  permDrawerVisible.value = false
  selectedRole.value = null
}

function isPermissionEnabled(code) {
  return selectedRolePermissionCodes.value.includes(code)
}

function setPermissionState(code, enable) {
  const index = selectedRolePermissionCodes.value.indexOf(code)
  if (enable) {
    if (index === -1) selectedRolePermissionCodes.value.push(code)
  } else {
    if (index > -1) selectedRolePermissionCodes.value.splice(index, 1)
  }
}

function getModuleGrantedCount(module) {
  return module.permissions.filter(p => isPermissionEnabled(p.code)).length
}

function setModuleAll(module, enable) {
  module.permissions.forEach(p => {
    setPermissionState(p.code, enable)
  })
}

async function saveRolePermissions() {
  if (!selectedRole.value) return
  submitting.value = true
  try {
    await updateRolePermissions(selectedRole.value.id, {
      permissionCodes: selectedRolePermissionCodes.value
    })
    showToast('权限配置保存成功', selectedRole.value.roleName, 'ok')
    closePermDrawer()
    await loadRoles()
    await loadData() // Refresh counts
  } catch (error) {
    showToast('保存权限失败', error.message, 'err')
  } finally {
    submitting.value = false
  }
}

// Cloning Roles
function cloneRoleTemplate() {
  openCloneRole()
}

function cloneSingleRole(role) {
  openCloneRole(role)
}

// Log view modals
function showLogDetail(log) {
  selectedLog.value = log
  logDetailModalVisible.value = true
}

function exportLogs() {
  let csvContent = 'data:text/csv;charset=utf-8,\uFEFF'
  csvContent += '日志编号,操作时间,操作员,角色,模块,类型,对象,IP,终端,门店区域,结果,风险等级,分析结论,追踪编号\n'
  filteredLogs.value.forEach(log => {
    const row = [
      log.id,
      log.t,
      log.op,
      getLogRoleName(log.role),
      log.mod,
      getLogTypeName(log.type),
      log.obj,
      log.ip,
      log.term,
      log.area,
      log.result === 'ok' ? '成功' : '失败',
      getRiskName(log.riskLevel),
      log.riskReason,
      log.traceId
    ].map(value => `"${String(value || '').replace(/"/g, '""')}"`).join(',')
    csvContent += row + '\n'
  })

  const encodedUri = encodeURI(csvContent)
  const link = document.createElement('a')
  link.setAttribute('href', encodedUri)
  link.setAttribute('download', `操作日志分析_${formatLocalDate(new Date())}.csv`)
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  showToast('已导出日志分析', `CSV · ${filteredLogs.value.length} 条日志`, 'ok')
}

onMounted(async () => {
  await Promise.all([loadPermissionModules(), loadRoles(), loadData()])
})
</script>

<style scoped>
/* Page Layout */
.user-permission-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

/* Header */
.page-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 16px;
}
.page-head h1 {
  margin: 0;
  font-size: 24px;
  font-weight: 600;
  letter-spacing: 1px;
}
.page-head .greet-sub {
  margin-top: 4px;
  font-size: 13px;
  color: var(--text-mute);
}
.page-head .greet-sub b {
  color: var(--brand);
  font-weight: 600;
}
.quick-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

/* Buttons Styling */
.btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 36px;
  padding: 0 14px;
  border-radius: 8px;
  border: 1px solid var(--line-strong);
  background: #fff;
  color: var(--text);
  font-size: 13px;
  cursor: pointer;
  transition: all .18s;
  box-shadow: 0 1px 2px rgba(20,40,80,0.04);
}
.btn:hover {
  border-color: var(--brand-bright);
  color: var(--brand);
  background: var(--brand-soft);
}
.btn.primary {
  border-color: transparent;
  background: linear-gradient(135deg, var(--brand-bright), var(--brand-glow));
  color: #fff;
  box-shadow: 0 6px 16px rgba(30,99,224,0.28);
}
.btn.primary:hover {
  filter: brightness(1.08);
}
.btn.danger {
  color: var(--danger);
  border-color: rgba(255,107,107,0.4);
  background: #fff;
}
.btn.danger:hover {
  background: rgba(255,107,107,0.08);
  border-color: var(--danger);
}
.btn.sm {
  height: 30px;
  padding: 0 10px;
  font-size: 12px;
}
.btn.xs {
  height: 26px;
  padding: 0 8px;
  font-size: 11.5px;
}
.btn.ghost {
  background: transparent;
  border-color: transparent;
  box-shadow: none;
}
.btn.ghost:hover {
  background: var(--bg-hover);
  color: var(--brand);
}
.btn svg {
  width: 14px;
  height: 14px;
  flex-shrink: 0;
}
.btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* KPI Panel */
.kpi-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}
.kpi {
  position: relative;
  padding: 18px 20px;
  border-radius: var(--radius);
  background: var(--bg-card);
  border: 1px solid var(--line);
  overflow: hidden;
  transition: transform .25s ease, border-color .2s, box-shadow .25s;
  cursor: pointer;
  box-shadow: 0 1px 2px rgba(20,40,80,0.04), 0 8px 24px rgba(20,40,80,0.05);
}
.kpi:hover {
  transform: translateY(-2px);
  border-color: color-mix(in oklab, var(--accent, #4d9bff) 40%, var(--line));
  box-shadow: 0 12px 32px rgba(20,40,80,0.10);
}
.kpi::before {
  content: "";
  position: absolute;
  top: 0;
  left: 16px;
  right: 16px;
  height: 2px;
  background: linear-gradient(90deg, transparent, var(--accent, var(--brand-bright)), transparent);
  opacity: 0.7;
}
.kpi::after {
  content: "";
  position: absolute;
  top: 8px;
  right: 8px;
  width: 12px;
  height: 12px;
  border-top: 1.5px solid var(--accent, var(--brand-bright));
  border-right: 1.5px solid var(--accent, var(--brand-bright));
  opacity: 0.5;
}
.kpi .label {
  font-size: 12px;
  color: var(--text-mute);
  letter-spacing: 1px;
}
.kpi .icon-wrap {
  position: absolute;
  bottom: 16px;
  right: 18px;
  width: 44px;
  height: 44px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: color-mix(in oklab, var(--accent, #4d9bff) 14%, transparent);
  color: var(--accent);
}
.kpi .icon-wrap svg {
  width: 22px;
  height: 22px;
}
.kpi .value {
  margin-top: 8px;
  font-family: "Orbitron", monospace;
  font-size: 28px;
  font-weight: 600;
  letter-spacing: 1px;
  color: var(--text);
  line-height: 1.1;
}
.kpi .value .unit {
  font-size: 13px;
  color: var(--text-mute);
  margin-left: 4px;
  font-weight: 400;
}
.kpi .delta {
  margin-top: 10px;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  background: rgba(34,153,98,0.10);
  color: #15803d;
  padding: 2px 7px;
  border-radius: 4px;
}
.kpi .delta.down {
  background: rgba(220,38,38,0.10);
  color: #b91c1c;
}
.kpi .sub {
  margin-left: 8px;
  font-size: 11px;
  color: var(--text-faint);
}

/* Tabs Switcher Styles */
.ip-tabs {
  display: flex;
  align-items: center;
  gap: 2px;
  padding: 0 4px;
  border-bottom: 1px solid var(--line);
}
.ip-tabs button {
  appearance: none;
  background: none;
  border: 0;
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 14px 16px;
  font: inherit;
  font-size: 13.5px;
  color: var(--text-mute);
  border-bottom: 2px solid transparent;
  cursor: pointer;
  transition: color .15s, border-color .15s;
  letter-spacing: 0.3px;
}
.ip-tabs button:hover {
  color: var(--text);
}
.ip-tabs button.on {
  color: var(--brand);
  border-bottom-color: var(--brand);
  font-weight: 500;
}
.ip-tabs button .ct {
  font-family: "DM Mono", monospace;
  font-size: 10px;
  padding: 1px 6px;
  border-radius: 999px;
  background: var(--bg-elev);
  color: var(--text-mute);
}
.ip-tabs button.on .ct {
  background: var(--brand-soft);
  color: var(--brand);
}
.ip-tabs button .ico {
  width: 14px;
  height: 14px;
  opacity: 0.85;
}

.tab-pane-container {
  display: flex;
  flex-direction: column;
  gap: 18px;
  animation: rise .35s cubic-bezier(.2,.7,.2,1) both;
}

/* Filters Bar Styles */
.filter-bar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
  background: var(--bg-card);
  border: 1px solid var(--line);
  border-radius: var(--radius);
  box-shadow: 0 1px 2px rgba(20,40,80,0.04);
}
.filter-bar .filter-search {
  flex: 1;
  min-width: 220px;
  display: flex;
  align-items: center;
  gap: 6px;
  height: 32px;
  padding: 0 12px;
  background: var(--bg-elev);
  border: 1px solid var(--line);
  border-radius: 6px;
}
.filter-bar .filter-search input {
  background: transparent;
  border: 0;
  outline: 0;
  flex: 1;
  color: inherit;
  font: inherit;
  font-size: 12.5px;
}
.filter-bar .filter-search svg {
  width: 14px;
  height: 14px;
  color: var(--text-mute);
}
.filter-bar .select {
  height: 32px;
  font-size: 12.5px;
  min-width: 120px;
  width: auto;
}
.field-label {
  font-size: 12px;
  color: var(--text-mute);
}

/* Card Styling */
.card {
  position: relative;
  background: var(--bg-card);
  border: 1px solid var(--line);
  border-radius: var(--radius);
  padding: 18px 20px;
  box-shadow: 0 1px 2px rgba(20,40,80,0.04), 0 8px 24px rgba(20,40,80,0.04);
}

/* User Avatar Table Cell */
.uc-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 13px;
  flex-shrink: 0;
  position: relative;
  overflow: hidden;
  box-shadow: inset 0 1px 0 rgba(255,255,255,0.25), 0 1px 4px rgba(20,40,80,0.16);
}
.uc-avatar::after {
  content: "";
  position: absolute;
  inset: 0;
  background: radial-gradient(circle at 22% 22%, rgba(255,255,255,0.30), transparent 55%);
}
.uc-avatar > span {
  position: relative;
  z-index: 1;
}
.uc-avatar .crown {
  position: absolute;
  top: -2px;
  right: -2px;
  width: 14px;
  height: 14px;
  background: #fbbf24;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 0 0 2px #fff;
  z-index: 2;
  font-size: 8px;
  color: #fff;
}
.uc-cell {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}
.uc-cell .uc-info .nm {
  font-size: 13px;
  font-weight: 500;
  color: var(--text);
}
.uc-cell .uc-info .em {
  font-size: 11px;
  color: var(--text-faint);
  font-family: "DM Mono", monospace;
  margin-top: 2px;
}

/* Table Design */
.table-host {
  overflow-x: auto;
}
table.t {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}
table.t th, table.t td {
  text-align: left;
  padding: 12px 14px;
  border-bottom: 1px solid var(--line);
  vertical-align: middle;
}
table.t th {
  font-weight: 500;
  color: var(--text-mute);
  font-size: 11px;
  letter-spacing: 1.5px;
  text-transform: uppercase;
  background: #fafbfe;
  position: sticky;
  top: 0;
  z-index: 2;
}
table.t tbody tr {
  transition: background .15s;
  cursor: pointer;
}
table.t tbody tr:hover {
  background: var(--bg-hover);
}
table.t td {
  color: var(--text-dim);
}
table.t .col-check {
  width: 36px;
  padding-right: 0;
}

.user-emp-no {
  font-family: "DM Mono", monospace;
  font-size: 11.5px;
  color: var(--text-mute);
}
.user-dept {
  font-size: 12.5px;
  color: var(--text-dim);
}
.user-phone {
  font-family: "DM Mono", monospace;
  font-size: 12px;
  color: var(--text-mute);
}
.user-time {
  font-family: "DM Mono", monospace;
  font-size: 11.5px;
  color: var(--text-dim);
}
.user-ip {
  font-family: "DM Mono", monospace;
  font-size: 10.5px;
  color: var(--text-faint);
  margin-top: 2px;
}

/* Checkbox Style */
.check {
  appearance: none;
  width: 16px;
  height: 16px;
  border: 1.5px solid var(--line-strong);
  border-radius: 4px;
  background: transparent;
  cursor: pointer;
  position: relative;
  flex-shrink: 0;
  transition: all .15s;
  vertical-align: middle;
}
.check:hover {
  border-color: var(--brand-bright);
}
.check:checked {
  background: var(--brand);
  border-color: var(--brand);
}
.check:checked::after {
  content: "";
  position: absolute;
  left: 4px;
  top: 1px;
  width: 5px;
  height: 9px;
  border: solid #fff;
  border-width: 0 2px 2px 0;
  transform: rotate(45deg);
}

/* Role pill tag styling */
.role-pill {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 3px 9px;
  font-size: 11.5px;
  border-radius: 4px;
  font-weight: 500;
  letter-spacing: 0.3px;
  margin-right: 4px;
  margin-bottom: 2px;
}
.role-pill.admin    { background: linear-gradient(135deg, #fbbf24, #d97706); color: #fff; }
.role-pill.manager  { background: rgba(192,132,252,0.15); color: #7c3aed; border: 1px solid rgba(192,132,252,0.4); }
.role-pill.purchase { background: rgba(77,155,255,0.15); color: var(--brand); border: 1px solid rgba(77,155,255,0.4); }
.role-pill.sales    { background: rgba(34,211,238,0.15); color: #0e7490; border: 1px solid rgba(34,211,238,0.4); }
.role-pill.stock    { background: rgba(52,211,153,0.15); color: #0f766e; border: 1px solid rgba(52,211,153,0.4); }
.role-pill.cashier  { background: rgba(244,114,182,0.15); color: #be185d; border: 1px solid rgba(244,114,182,0.4); }
.role-pill.finance  { background: rgba(251,191,36,0.15); color: #92400e; border: 1px solid rgba(251,191,36,0.4); }
.role-pill.viewer   { background: var(--bg-elev); color: var(--text-mute); border: 1px solid var(--line); }
.role-pill .dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: currentColor;
}

/* Status Pill Tag */
.st-pill {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 500;
}
.st-pill.on {
  background: rgba(52,211,153,0.14);
  color: #15803d;
}
.st-pill.off {
  background: rgba(120,128,148,0.16);
  color: #64748b;
}
.st-pill .dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: currentColor;
}

/* 2FA styling */
.tfa-enabled {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 11.5px;
  color: #15803d;
}
.tfa-disabled {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 11.5px;
  color: var(--text-faint);
}
.tfa-enabled svg, .tfa-disabled svg {
  flex-shrink: 0;
}

/* Row Action items */
.row-actions {
  display: inline-flex;
  gap: 4px;
}
.row-actions button {
  width: 28px;
  height: 28px;
  border-radius: 6px;
  border: 1px solid var(--line);
  background: transparent;
  color: var(--text-mute);
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition: all .15s;
}
.row-actions button:hover {
  color: var(--brand);
  border-color: var(--brand-bright);
  background: var(--brand-soft);
}

/* Pager Navigation */
.page-foot {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 16px;
  border-top: 1px solid var(--line);
  font-size: 12.5px;
  color: var(--text-mute);
}
.pager {
  display: inline-flex;
  gap: 4px;
  margin-left: auto;
}
.pager button {
  min-width: 32px;
  height: 32px;
  border: 1px solid var(--line);
  background: #fff;
  color: var(--text-dim);
  border-radius: 6px;
  cursor: pointer;
  padding: 0 8px;
  font-family: "DM Mono", monospace;
  font-size: 12.5px;
}
.pager button.on {
  background: var(--brand);
  border-color: var(--brand);
  color: #fff;
}
.pager button:hover:not(.on):not(:disabled) {
  border-color: var(--brand-bright);
  color: var(--brand);
}

/* Roles grid list cards layout */
.roles-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}
.role-card {
  position: relative;
  background: var(--bg-card);
  border: 1px solid var(--line);
  border-radius: var(--radius);
  padding: 18px 18px 16px;
  box-shadow: 0 1px 2px rgba(20,40,80,0.04), 0 8px 24px rgba(20,40,80,0.04);
  overflow: hidden;
  transition: transform .25s, border-color .2s, box-shadow .25s;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  min-height: 240px;
}
.role-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 32px rgba(20,40,80,0.10);
  border-color: color-mix(in oklab, var(--rc, var(--brand)) 35%, var(--line));
}
.role-card::before {
  content: "";
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: linear-gradient(90deg, var(--rc, var(--brand)), color-mix(in oklab, var(--rc) 60%, #fff));
}
.role-card .rc-head {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}
.role-card .rc-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--rc, var(--brand));
  background: color-mix(in oklab, var(--rc, var(--brand)) 14%, transparent);
  flex-shrink: 0;
}
.role-card .rc-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--text);
}
.role-card .rc-code {
  font-size: 11px;
  font-family: "DM Mono", monospace;
  color: var(--text-mute);
  margin-top: 2px;
  letter-spacing: 0.5px;
}
.role-card .rc-desc {
  margin: 12px 0 14px;
  font-size: 12.5px;
  color: var(--text-dim);
  line-height: 1.55;
  min-height: 36px;
}
.role-card .rc-stats {
  display: flex;
  gap: 12px;
  padding-top: 12px;
  border-top: 1px dashed var(--line);
}
.role-card .rc-stats .it {
  flex: 1;
}
.role-card .rc-stats .it .l {
  font-size: 10.5px;
  color: var(--text-mute);
  letter-spacing: 1px;
  text-transform: uppercase;
  font-family: "Orbitron", monospace;
}
.role-card .rc-stats .it .v {
  font-family: "Orbitron", monospace;
  font-size: 18px;
  font-weight: 600;
  color: var(--text);
  margin-top: 2px;
}
.role-card .rc-foot {
  display: flex;
  gap: 6px;
  margin-top: auto;
  padding-top: 14px;
}
.role-card .rc-foot .btn {
  flex: 1;
  justify-content: center;
  height: 30px;
  font-size: 12px;
  padding: 0 8px;
}

.role-card.add-card {
  border-style: dashed;
  background: transparent;
  box-shadow: none;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  gap: 8px;
  min-height: 240px;
  color: var(--text-mute);
}
.role-card.add-card::before {
  display: none;
}
.role-card.add-card:hover {
  border-color: var(--brand-bright);
  color: var(--brand);
  background: var(--brand-soft);
}
.role-card.add-card .plus {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: var(--bg-elev);
  display: flex;
  align-items: center;
  justify-content: center;
}
.role-card.add-card svg {
  width: 22px;
  height: 22px;
}

/* Operation Audit Logs Styles */
.log-insight-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}
.log-insight-card {
  position: relative;
  min-height: 112px;
  padding: 16px 18px;
  border: 1px solid var(--line);
  border-radius: var(--radius);
  background: var(--bg-card);
  overflow: hidden;
  cursor: pointer;
  box-shadow: 0 1px 2px rgba(20,40,80,0.04), 0 8px 22px rgba(20,40,80,0.04);
}
.log-insight-card::before {
  content: "";
  position: absolute;
  inset: 0 auto 0 0;
  width: 3px;
  background: var(--accent);
}
.log-insight-card::after {
  content: "";
  position: absolute;
  right: -28px;
  top: -34px;
  width: 96px;
  height: 96px;
  border-radius: 50%;
  background: color-mix(in oklab, var(--accent) 16%, transparent);
}
.log-insight-card .lic-label {
  color: var(--text-mute);
  font-size: 12px;
}
.log-insight-card .lic-value {
  margin-top: 8px;
  color: var(--text);
  font-family: "Orbitron", monospace;
  font-size: 28px;
  font-weight: 700;
  line-height: 1;
}
.log-insight-card .lic-value.text {
  max-width: calc(100% - 22px);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-family: inherit;
  font-size: 20px;
}
.log-insight-card .lic-note {
  margin-top: 10px;
  color: var(--text-dim);
  font-size: 12px;
}
.log-analysis-board {
  display: grid;
  grid-template-columns: minmax(0, 1.25fr) minmax(0, 1fr) minmax(260px, .9fr);
  gap: 14px;
}
.log-panel {
  min-width: 0;
  padding: 16px;
  border: 1px solid var(--line);
  border-radius: var(--radius);
  background: var(--bg-card);
  box-shadow: 0 1px 2px rgba(20,40,80,0.04);
}
.log-panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}
.log-panel-head h3 {
  margin: 0;
  color: var(--text);
  font-size: 14px;
  font-weight: 650;
}
.log-panel-head p {
  margin: 4px 0 0;
  color: var(--text-mute);
  font-size: 11.5px;
  line-height: 1.45;
}
.risk-list {
  display: grid;
  gap: 8px;
}
.risk-item {
  display: grid;
  grid-template-columns: 10px minmax(0, 1fr) auto;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 9px 10px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: var(--bg-elev);
  color: inherit;
  text-align: left;
  cursor: pointer;
}
.risk-item:hover {
  border-color: var(--brand-bright);
  background: var(--brand-soft);
}
.risk-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #94a3b8;
}
.risk-dot.high { background: var(--danger); }
.risk-dot.medium { background: var(--warn); }
.risk-dot.low { background: var(--ok); }
.risk-main {
  min-width: 0;
}
.risk-main b {
  display: block;
  color: var(--text);
  font-size: 12.5px;
}
.risk-main small {
  display: block;
  margin-top: 2px;
  overflow: hidden;
  color: var(--text-mute);
  font-size: 11.5px;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.risk-meta {
  color: var(--text-dim);
  font-size: 11.5px;
}
.risk-empty {
  padding: 18px 10px;
  color: var(--text-mute);
  font-size: 12.5px;
  text-align: center;
}
.type-bars {
  display: grid;
  gap: 10px;
}
.type-bar {
  display: grid;
  grid-template-columns: 62px minmax(0, 1fr) 28px;
  align-items: center;
  gap: 10px;
}
.bar-track {
  height: 8px;
  border-radius: 999px;
  background: var(--bg-elev);
  overflow: hidden;
}
.bar-track i {
  display: block;
  height: 100%;
  min-width: 8px;
  border-radius: inherit;
  background: linear-gradient(90deg, var(--brand-bright), #34d399);
}
.type-bar b {
  color: var(--text-dim);
  font-family: "DM Mono", monospace;
  font-size: 12px;
  text-align: right;
}
.operator-rank {
  display: grid;
  gap: 8px;
}
.rank-row {
  display: grid;
  grid-template-columns: 28px minmax(0, 1fr) auto;
  align-items: center;
  gap: 9px;
  padding: 8px 0;
  border-bottom: 1px dashed var(--line);
}
.rank-row:last-child {
  border-bottom: 0;
}
.rank-avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: #edf3ff;
  color: var(--brand);
  font-size: 12px;
  font-weight: 700;
}
.rank-name {
  min-width: 0;
  overflow: hidden;
  color: var(--text);
  font-size: 12.5px;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.rank-score {
  color: var(--text-mute);
  font-size: 11.5px;
}
.log-time {
  font-family: "DM Mono", monospace;
  font-size: 11.5px;
  color: var(--text-mute);
  white-space: nowrap;
}
.log-time .d {
  display: block;
  color: var(--text-faint);
  font-size: 10.5px;
  margin-top: 1px;
}
.log-op-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 8px;
  font-size: 11px;
  border-radius: 4px;
  font-weight: 500;
}
.log-op-tag.create  { background: rgba(52,211,153,0.14); color: #0f766e; }
.log-op-tag.update  { background: rgba(77,155,255,0.14); color: var(--brand); }
.log-op-tag.delete  { background: rgba(255,107,107,0.14); color: #b91c1c; }
.log-op-tag.login   { background: rgba(192,132,252,0.14); color: #7c3aed; }
.log-op-tag.export  { background: rgba(34,211,238,0.14); color: #0e7490; }
.log-op-tag.approve { background: rgba(251,191,36,0.14); color: #92400e; }

.risk-badge {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  white-space: nowrap;
  color: var(--text-mute);
  font-size: 11.5px;
}
.risk-badge span {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #94a3b8;
}
.risk-badge.low {
  color: #15803d;
}
.risk-badge.low span {
  background: var(--ok);
}
.risk-badge.medium {
  color: #92400e;
}
.risk-badge.medium span {
  background: var(--warn);
}
.risk-badge.high {
  color: #b91c1c;
}
.risk-badge.high span {
  background: var(--danger);
}
.log-result-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
}
.log-result-tag.ok {
  color: #15803d;
}
.log-result-tag.fail {
  color: #b91c1c;
}
.log-ip {
  font-family: "DM Mono", monospace;
  font-size: 11.5px;
  color: var(--text-mute);
}
.trace-id {
  margin-top: 2px;
  color: var(--text-faint);
  font-family: "DM Mono", monospace;
  font-size: 10.5px;
}
.log-detail-body {
  font-size: 13px;
  line-height: 1.55;
}
.detail-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px 14px;
}
.detail-grid > div {
  min-width: 0;
  padding-bottom: 10px;
  border-bottom: 1px dashed #e4e8f1;
}
.detail-grid b {
  display: block;
  color: #7b86a0;
  font-size: 11px;
  font-weight: 500;
}
.detail-grid span {
  display: inline-flex;
  margin-top: 3px;
  color: #18243d;
}
.detail-ok {
  color: #15803d !important;
}
.detail-fail {
  color: #b91c1c !important;
}
.risk-reason {
  margin-top: 14px;
  padding: 12px;
  border: 1px solid rgba(77,155,255,0.22);
  border-radius: 8px;
  background: rgba(77,155,255,0.06);
}
.risk-reason b {
  color: #1e63e0;
  font-size: 12px;
}
.risk-reason p {
  margin: 6px 0 0;
  color: #3f4a61;
  font-size: 12.5px;
}

/* Form Styles */
.field-row {
  display: grid;
  grid-template-columns: 130px 1fr;
  gap: 14px;
  align-items: start;
  padding: 14px 0;
  border-bottom: 1px dashed var(--line);
}
.field-row:last-child {
  border-bottom: 0;
}
.field-row > .label {
  font-size: 13px;
  color: var(--text-dim);
  line-height: 32px;
}
.field-row > .label .req {
  color: var(--danger);
  margin-right: 3px;
}
.field-row > .label .hint {
  display: block;
  font-size: 11px;
  color: var(--text-faint);
  font-weight: 400;
  margin-top: 1px;
  line-height: 1.4;
}
.field-row > .input-wrap {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.input, .textarea, .select {
  height: 36px;
  padding: 0 12px;
  background: var(--bg-elev);
  border: 1px solid var(--line);
  border-radius: 6px;
  color: var(--text);
  font: inherit;
  font-size: 13px;
  transition: border-color .15s, box-shadow .15s;
  width: 100%;
  outline: 0;
}
.input::placeholder, .textarea::placeholder {
  color: var(--text-mute);
}
.input:hover, .textarea:hover, .select:hover {
  border-color: var(--line-strong);
}
.input:focus, .textarea:focus, .select:focus {
  border-color: var(--brand-bright);
  box-shadow: 0 0 0 3px var(--brand-soft);
}
.textarea {
  padding: 10px 12px;
  height: auto;
  min-height: 80px;
  resize: vertical;
  line-height: 1.5;
}
.select {
  appearance: none;
  padding-right: 28px;
  cursor: pointer;
  background-image: url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 24 24' fill='none' stroke='%237b86a0' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'><polyline points='6 9 12 15 18 9'/></svg>");
  background-repeat: no-repeat;
  background-position: right 8 center;
}

.check-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}
.check-item {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  padding: 8px 10px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: #fbfcff;
  cursor: pointer;
}
.check-item span {
  min-width: 0;
  color: var(--text);
  font-size: 13px;
  font-weight: 600;
}
.check-item small {
  margin-left: auto;
  color: var(--text-faint);
  font-size: 11px;
  font-family: ui-monospace, SFMono-Regular, Consolas, monospace;
}

/* Switch Styles */
.switch {
  position: relative;
  width: 38px;
  height: 22px;
  display: inline-block;
}
.switch input {
  opacity: 0;
  width: 0;
  height: 0;
}
.switch .slider {
  position: absolute;
  inset: 0;
  background: var(--bg-elev);
  border: 1px solid var(--line);
  border-radius: 999px;
  cursor: pointer;
  transition: all .2s;
}
.switch .slider::before {
  content: "";
  position: absolute;
  left: 2px;
  top: 2px;
  width: 16px;
  height: 16px;
  background: #fff;
  border-radius: 50%;
  transition: transform .25s cubic-bezier(.2,.7,.2,1);
  box-shadow: 0 1px 3px rgba(0,0,0,0.18);
}
.switch input:checked + .slider {
  background: var(--brand);
  border-color: var(--brand);
}
.switch input:checked + .slider::before {
  transform: translateX(16px);
}

/* Radio chips styling */
.radio-chips {
  display: inline-flex;
  flex-wrap: wrap;
  gap: 6px;
}
.radio-chips label {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  font-size: 12.5px;
  border: 1px solid var(--line);
  border-radius: 6px;
  background: var(--bg-elev);
  color: var(--text-dim);
  cursor: pointer;
  transition: all .15s;
}
.radio-chips label:hover {
  color: var(--text);
  border-color: var(--line-strong);
}
.radio-chips input {
  display: none;
}
.radio-chips input:checked + span {
  color: var(--brand);
}
.radio-chips label:has(input:checked) {
  border-color: var(--brand);
  background: var(--brand-soft);
  color: var(--brand);
}

/* Right Detail Drawer & Mask Layout styles */
.drawer-mask {
  position: fixed;
  inset: 0;
  background: rgba(4,8,24,0.55);
  backdrop-filter: blur(4px);
  z-index: 80;
  opacity: 0;
  pointer-events: none;
  transition: opacity .2s;
}
.drawer-mask.show {
  opacity: 1;
  pointer-events: auto;
}
.drawer-panel {
  position: fixed;
  right: 0;
  top: 0;
  bottom: 0;
  width: min(640px, calc(100% - 60px));
  background: #fff;
  color: #18243d;
  z-index: 81;
  box-shadow: -18px 0 56px rgba(2,10,32,0.30);
  transform: translateX(100%);
  transition: transform .3s cubic-bezier(.2,.7,.2,1);
  display: flex;
  flex-direction: column;
}
.drawer-panel.show {
  transform: translateX(0);
}

.drawer-head {
  padding: 18px 22px;
  border-bottom: 1px solid #e4e8f1;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.drawer-head h2 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
}
.drawer-head .x {
  width: 28px;
  height: 28px;
  background: transparent;
  border: 0;
  border-radius: 6px;
  color: #7b86a0;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}
.drawer-head .x:hover {
  background: #f1f4fa;
  color: #18243d;
}

.drawer-body {
  padding: 22px;
  overflow-y: auto;
  flex: 1;
}

.drawer-foot {
  padding: 14px 22px;
  border-top: 1px solid #e4e8f1;
  display: flex;
  gap: 10px;
  justify-content: flex-end;
  background: #fafbfe;
}

.drawer-hero {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 18px;
  background: linear-gradient(135deg, rgba(77,155,255,0.08), rgba(192,132,252,0.06));
  border-radius: 12px;
  border: 1px solid var(--line);
}
.drawer-hero .uc-avatar {
  box-shadow: inset 0 1px 0 rgba(255,255,255,0.25), 0 1px 4px rgba(20,40,80,0.16);
}
.drawer-hero .hi-nm {
  font-size: 18px;
  font-weight: 600;
  color: var(--text);
}
.drawer-hero .hi-sub {
  font-size: 12.5px;
  color: var(--text-mute);
  margin-top: 2px;
  font-family: "DM Mono", monospace;
}

.stat-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
  margin-top: 16px;
}
.stat-row .st {
  padding: 12px;
  background: var(--bg-elev);
  border-radius: 8px;
}
.stat-row .st .l {
  font-size: 10.5px;
  color: var(--text-mute);
  letter-spacing: 1px;
  text-transform: uppercase;
  font-family: "Orbitron", monospace;
}
.stat-row .st .v {
  font-family: "Orbitron", monospace;
  font-size: 18px;
  font-weight: 600;
  color: var(--text);
  margin-top: 4px;
}

/* Modals Box styles */
.modal-mask {
  position: fixed;
  inset: 0;
  background: rgba(4,8,24,0.55);
  backdrop-filter: blur(4px);
  z-index: 80;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  pointer-events: none;
  transition: opacity .2s;
}
.modal-mask.show {
  opacity: 1;
  pointer-events: auto;
}
.modal-card {
  background: #fff;
  color: #18243d;
  width: min(520px, calc(100% - 32px));
  border-radius: 14px;
  box-shadow: 0 28px 64px rgba(2,10,32,0.40);
  transform: translateY(20px) scale(.98);
  transition: transform .25s cubic-bezier(.2,.7,.2,1);
  overflow: hidden;
}
.modal-mask.show .modal-card {
  transform: translateY(0) scale(1);
}
.modal-head {
  padding: 16px 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #e4e8f1;
}
.modal-head h3 {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
}
.modal-head .close-x {
  width: 28px;
  height: 28px;
  border-radius: 6px;
  background: transparent;
  border: 0;
  color: #7b86a0;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}
.modal-head .close-x:hover {
  background: #f1f4fa;
  color: #18243d;
}
.modal-body {
  padding: 18px 20px;
  max-height: 70vh;
  overflow-y: auto;
}
.modal-foot {
  padding: 14px 20px;
  display: flex;
  gap: 10px;
  justify-content: flex-end;
  border-top: 1px solid #e4e8f1;
  background: #fafbfe;
}

/* Tri-state Permissions Config drawer specific styles */
.perm-drawer {
  width: min(760px, calc(100% - 60px));
}
.perm-dr-head {
  position: relative;
  padding: 22px;
  color: #fff;
  overflow: hidden;
  flex-shrink: 0;
}
.perm-dr-head::before {
  content: "";
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, var(--rc, var(--brand)), color-mix(in oklab, var(--rc, var(--brand)) 55%, #1a2740));
}
.perm-dr-head::after {
  content: "";
  position: absolute;
  inset: 0;
  background: radial-gradient(circle at 88% -10%, rgba(255,255,255,0.22), transparent 55%);
}
.perm-dr-head > * {
  position: relative;
  z-index: 1;
}
.perm-dr-top {
  display: flex;
  align-items: flex-start;
  gap: 14px;
}
.perm-dr-top .pdi {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: rgba(255,255,255,0.18);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: inset 0 0 0 1px rgba(255,255,255,0.25);
}
.perm-dr-top .pdi svg {
  width: 24px;
  height: 24px;
}
.perm-dr-top .pd-name {
  font-size: 19px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 8px;
}
.perm-dr-top .pd-code {
  font-family: "DM Mono", monospace;
  font-size: 11.5px;
  opacity: 0.82;
  margin-top: 3px;
  letter-spacing: 0.5px;
}
.perm-dr-top .pd-desc {
  font-size: 12.5px;
  opacity: 0.9;
  margin-top: 8px;
  line-height: 1.55;
  max-width: 540px;
}
.perm-dr-top .x {
  margin-left: auto;
  width: 30px;
  height: 30px;
  border-radius: 8px;
  background: rgba(255,255,255,0.14);
  border: 0;
  color: #fff;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: background .15s;
}
.perm-dr-top .x:hover {
  background: rgba(255,255,255,0.28);
}
.perm-dr-meta {
  display: flex;
  gap: 26px;
  margin-top: 18px;
}
.perm-dr-meta .m .l {
  font-size: 10px;
  letter-spacing: 1.5px;
  text-transform: uppercase;
  font-family: "Orbitron", monospace;
  opacity: 0.78;
}
.perm-dr-meta .m .v {
  font-family: "Orbitron", monospace;
  font-size: 20px;
  font-weight: 600;
  margin-top: 3px;
}
.perm-dr-meta .m .v small {
  font-size: 12px;
  opacity: 0.75;
  font-weight: 400;
  margin-left: 2px;
}

.perm-dr-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 20px;
  border-bottom: 1px solid var(--line);
  background: #fafbfe;
  flex-shrink: 0;
}
.perm-dr-toolbar .filter-search {
  display: flex;
  align-items: center;
  gap: 6px;
  height: 32px;
  padding: 0 12px;
  background: #fff;
  border: 1px solid var(--line);
  border-radius: 6px;
}
.perm-dr-toolbar .filter-search input {
  background: transparent;
  border: 0;
  outline: 0;
  flex: 1;
  color: inherit;
  font: inherit;
  font-size: 12.5px;
}
.perm-dr-toolbar .filter-search svg {
  width: 14px;
  height: 14px;
  color: var(--text-mute);
}
.perm-dr-toolbar .perm-legend {
  padding: 6px 12px;
}
.perm-legend {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 14px;
  font-size: 12px;
  color: var(--text-dim);
  background: var(--bg-elev);
  border-radius: 8px;
  border: 1px solid var(--line);
}
.perm-legend .lg {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
.perm-legend .lg .perm-dot {
  width: 18px;
  height: 18px;
}
.perm-legend .lg .perm-dot svg {
  width: 10px;
  height: 10px;
}

.perm-mod {
  border: 1px solid var(--line);
  border-radius: 10px;
  margin-bottom: 14px;
  overflow: hidden;
  background: #fff;
}
.perm-mod-head {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 11px 14px;
  background: #fafbfe;
  border-bottom: 1px solid var(--line);
}
.perm-mod-head .mic {
  width: 26px;
  height: 26px;
  border-radius: 7px;
  background: var(--brand-soft);
  color: var(--brand);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.perm-mod-head .mic svg {
  width: 14px;
  height: 14px;
}
.perm-mod-head .mn {
  font-size: 13.5px;
  font-weight: 600;
  color: var(--text);
}
.perm-mod-head .mc {
  font-family: "DM Mono", monospace;
  font-size: 10.5px;
  color: var(--text-faint);
  margin-left: 2px;
}
.perm-mod-head .mod-count {
  margin-left: auto;
  font-size: 11.5px;
  color: var(--text-mute);
  font-family: "DM Mono", monospace;
}
.perm-mod-head .mod-all {
  display: inline-flex;
  gap: 4px;
}
.perm-mod-head .mod-all button {
  appearance: none;
  border: 1px solid var(--line);
  background: #fff;
  color: var(--text-mute);
  font: inherit;
  font-size: 11px;
  padding: 3px 9px;
  border-radius: 5px;
  cursor: pointer;
  transition: all .15s;
}
.perm-mod-head .mod-all button:hover {
  border-color: var(--brand-bright);
  color: var(--brand);
  background: var(--brand-soft);
}

.perm-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 11px 14px;
  border-bottom: 1px solid var(--line);
}
.perm-item:last-child {
  border-bottom: 0;
}
.perm-item .pi-info {
  min-width: 0;
  flex: 1;
}
.perm-item .pi-info .pin {
  font-size: 13px;
  color: var(--text-dim);
}
.perm-item .pi-info .pic {
  font-size: 10.5px;
  color: var(--text-faint);
  font-family: "DM Mono", monospace;
  margin-top: 2px;
}

/* Tri-state Segmented control */
.seg {
  display: inline-flex;
  border: 1px solid var(--line);
  border-radius: 7px;
  overflow: hidden;
  background: var(--bg-elev);
  flex-shrink: 0;
}
.seg button {
  appearance: none;
  border: 0;
  border-right: 1px solid var(--line);
  background: transparent;
  padding: 6px 13px;
  font: inherit;
  font-size: 11.5px;
  color: var(--text-mute);
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 5px;
  transition: all .14s;
}
.seg button:last-child {
  border-right: 0;
}
.seg button svg {
  width: 11px;
  height: 11px;
}
.seg button:hover:not(.on) {
  background: var(--bg-hover);
  color: var(--text-dim);
}
.seg button.on.y {
  background: rgba(52,211,153,0.18);
  color: #0f766e;
  font-weight: 600;
  box-shadow: inset 0 0 0 1px rgba(52,211,153,0.4);
}
.seg button.on.p {
  background: rgba(251,191,36,0.20);
  color: #92400e;
  font-weight: 600;
  box-shadow: inset 0 0 0 1px rgba(251,191,36,0.45);
}
.seg button.on.n {
  background: rgba(120,128,148,0.16);
  color: #64748b;
  font-weight: 600;
  box-shadow: inset 0 0 0 1px rgba(120,128,148,0.35);
}

.perm-lock-note {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 11px 14px;
  margin-bottom: 14px;
  border-radius: 10px;
  background: rgba(251,191,36,0.12);
  border: 1px solid rgba(251,191,36,0.35);
  color: #92400e;
  font-size: 12.5px;
}
.perm-lock-note svg {
  width: 16px;
  height: 16px;
  flex-shrink: 0;
}

/* Toast Notification Styles */
.toast-host {
  position: fixed;
  top: 24px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 100;
  display: flex;
  flex-direction: column;
  gap: 10px;
  pointer-events: none;
}
.toast {
  pointer-events: auto;
  min-width: 280px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px 12px 14px;
  border-radius: 10px;
  background: #fff;
  border: 1px solid var(--line);
  box-shadow: 0 12px 32px rgba(2,10,32,0.18);
  color: #18243d;
  font-size: 13.5px;
  animation: toast-in .35s cubic-bezier(.2,.7,.2,1) both;
  position: relative;
  overflow: hidden;
}
.toast::before {
  content: "";
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 3px;
  background: var(--brand);
}
.toast.ok::before { background: var(--ok); }
.toast.warn::before { background: var(--warn); }
.toast.err::before { background: var(--danger); }
.toast.info::before { background: var(--info); }
.toast .ic {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
}
.toast.ok .ic { background: var(--ok); }
.toast.warn .ic { background: var(--warn); }
.toast.err .ic { background: var(--danger); }
.toast.info .ic { background: var(--brand); }
.toast .ic svg {
  width: 13px;
  height: 13px;
}
.toast .body {
  flex: 1;
  line-height: 1.35;
}
.toast .body .title {
  font-weight: 500;
}
.toast .body .desc {
  font-size: 12px;
  color: #7b86a0;
  margin-top: 2px;
}
.toast .close {
  background: transparent;
  border: 0;
  color: #7b86a0;
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
  font-size: 18px;
  line-height: 1;
}
.toast .close:hover {
  background: #f1f4fa;
  color: #18243d;
}

@keyframes toast-in {
  from { opacity: 0; transform: translateY(-12px) scale(.96); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}

/* Perm segment dot representation */
.perm-dot {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border-radius: 6px;
  transition: all .15s;
  user-select: none;
}
.perm-dot.y { background: rgba(52,211,153,0.18); color: #0f766e; }
.perm-dot.n { background: var(--bg-elev); color: var(--text-faint); }
.perm-dot.partial { background: rgba(251,191,36,0.18); color: #92400e; }
.perm-dot svg { width: 12px; height: 12px; }

/* Responsive styles overrides */
@media (max-width: 1100px) {
  .kpi-row {
    grid-template-columns: repeat(2, 1fr);
  }
  .log-insight-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .log-analysis-board {
    grid-template-columns: 1fr;
  }
}
@media (max-width: 760px) {
  .roles-grid,
  .check-grid {
    grid-template-columns: 1fr;
  }
  .log-insight-grid {
    grid-template-columns: 1fr;
  }
  .detail-grid {
    grid-template-columns: 1fr;
  }
  .filter-bar {
    align-items: stretch;
    flex-direction: column;
  }
  .kpi-row {
    grid-template-columns: 1fr;
  }
  .perm-dr-top {
    flex-direction: column;
    gap: 10px;
  }
}
</style>
