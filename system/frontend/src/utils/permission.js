export function hasAnyRole(userRoles = [], allowedRoles = []) {
  if (!allowedRoles || allowedRoles.length === 0) {
    return true
  }
  return allowedRoles.some((role) => userRoles.includes(role))
}

export function hasAnyPermission(userPermissions = [], allowedPermissions = []) {
  if (!allowedPermissions || allowedPermissions.length === 0) {
    return true
  }
  return allowedPermissions.some((permission) => userPermissions.includes(permission))
}

export function hasRouteAccess(userRoles = [], userPermissions = [], meta = {}) {
  const hasRoleRules = Array.isArray(meta.roles) && meta.roles.length > 0
  const hasPermissionRules = Array.isArray(meta.permissions) && meta.permissions.length > 0
  if (!hasRoleRules && !hasPermissionRules) {
    return true
  }
  return (
    (hasRoleRules && hasAnyRole(userRoles, meta.roles)) ||
    (hasPermissionRules && hasAnyPermission(userPermissions, meta.permissions))
  )
}
