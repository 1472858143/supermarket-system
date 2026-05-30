export function hasAnyRole(userRoles = [], allowedRoles = []) {
  if (!allowedRoles || allowedRoles.length === 0) {
    return true
  }
  return allowedRoles.some((role) => userRoles.includes(role))
}
