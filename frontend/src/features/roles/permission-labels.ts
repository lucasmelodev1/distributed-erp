import { levelRank } from "@/features/roles/permission-levels"
import type { PermissionLevel } from "@/gql/graphql"

/**
 * Friendly names for the permission tree. The API only stores a code and a short English
 * description, so the groups and labels live here. Unknown codes fall back to the stored
 * description (then the code itself), so a new backend permission still shows up.
 */
const GROUP_LABELS: Record<string, string> = {
  tenants: "Company",
  tenant_users: "Members",
  tenant_user_roles: "Member roles",
  tenant_roles: "Roles",
}

const PERMISSION_LABELS: Record<string, string> = {
  "tenants:update": "Rename the company",
  "tenants:delete": "Delete the company",
  "tenants:invite": "Invite people",
  "tenants:expel": "Remove members",
  "tenants:inactivate": "Inactivate or reactivate the company",
  "tenant_users:read": "View members",
  "tenant_users:update": "Change the roles of members",
  "tenant_users:inactivate": "Inactivate or reactivate members",
  "tenant_user_roles:read": "View the roles of members",
  "tenant_user_roles:grant": "Grant roles to members",
  "tenant_user_roles:revoke": "Revoke roles from members",
  "tenant_roles:read": "View roles",
  "tenant_roles:create": "Create roles",
  "tenant_roles:update": "Edit roles",
  "tenant_roles:delete": "Delete roles",
  "tenant_roles:inactivate": "Inactivate or reactivate roles",
}

const GROUP_ORDER = Object.keys(GROUP_LABELS)

export type PermissionInfo = {
  code: string
  description?: string | null
  level: PermissionLevel
}

export type PermissionGroup = {
  key: string
  label: string
  /** Ordered by level: view, then edit, then sensitive. */
  permissions: { code: string; label: string; level: PermissionLevel }[]
}

export function groupKey(code: string) {
  return code.split(":")[0]!
}

function titleCase(key: string) {
  const words = key.replace(/_/g, " ")
  return words.charAt(0).toUpperCase() + words.slice(1)
}

export function permissionLabel(permission: PermissionInfo) {
  return (
    PERMISSION_LABELS[permission.code] ??
    permission.description ??
    permission.code
  )
}

/** Groups permissions by the part of the code before the colon, known groups first. */
export function groupPermissions(
  permissions: readonly PermissionInfo[]
): PermissionGroup[] {
  const groups = new Map<string, PermissionGroup>()
  for (const permission of permissions) {
    const key = groupKey(permission.code)
    let group = groups.get(key)
    if (!group) {
      group = {
        key,
        label: GROUP_LABELS[key] ?? titleCase(key),
        permissions: [],
      }
      groups.set(key, group)
    }
    group.permissions.push({
      code: permission.code,
      label: permissionLabel(permission),
      level: permission.level,
    })
  }
  for (const group of groups.values()) {
    group.permissions.sort((a, b) => levelRank(a.level) - levelRank(b.level))
  }
  const rank = (key: string) => {
    const index = GROUP_ORDER.indexOf(key)
    return index === -1 ? GROUP_ORDER.length : index
  }
  return [...groups.values()].sort(
    (a, b) => rank(a.key) - rank(b.key) || a.label.localeCompare(b.label)
  )
}
