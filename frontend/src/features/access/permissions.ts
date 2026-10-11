/**
 * Every permission code the UI checks. The checks go through `useCan`, so a typo here or at a
 * call site fails to compile. The backend stays the authority; this only decides what to show.
 */
export const PERMISSIONS = {
  tenantsUpdate: "tenants:update",
  tenantsInactivate: "tenants:inactivate",
  tenantsInvite: "tenants:invite",
  tenantUsersRead: "tenant_users:read",
  tenantUsersUpdate: "tenant_users:update",
  tenantUsersInactivate: "tenant_users:inactivate",
  tenantRolesRead: "tenant_roles:read",
  tenantRolesCreate: "tenant_roles:create",
  tenantRolesUpdate: "tenant_roles:update",
  tenantRolesInactivate: "tenant_roles:inactivate",
} as const

export type Permission = (typeof PERMISSIONS)[keyof typeof PERMISSIONS]
