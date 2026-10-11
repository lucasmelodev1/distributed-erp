import { PermissionLevelIcon } from "@/features/roles/permission-level-icon"
import { groupPermissions } from "@/features/roles/permission-labels"
import type { Role } from "@/features/roles/roles-query"

/** Read-only list of everything the chosen roles allow together, grouped by area. */
export function PermissionSummary({
  roles,
}: {
  roles: readonly Pick<Role, "active" | "permissions">[]
}) {
  // Only active roles grant anything, so inactive ones add nothing to the total.
  const byCode = new Map(
    roles
      .filter((role) => role.active)
      .flatMap((role) => role.permissions)
      .map((permission) => [permission.code, permission])
  )
  const groups = groupPermissions([...byCode.values()])

  if (groups.length === 0) {
    return (
      <p className="text-sm text-muted-foreground">
        The selected roles allow nothing.
      </p>
    )
  }
  return (
    <div className="flex flex-col gap-3 text-sm">
      {groups.map((group) => (
        <div key={group.key} className="flex flex-col gap-1">
          <span className="font-medium">{group.label}</span>
          <ul className="flex flex-col gap-1 text-muted-foreground">
            {group.permissions.map((permission) => (
              <li key={permission.code} className="flex items-center gap-2">
                <PermissionLevelIcon level={permission.level} />
                {permission.label}
              </li>
            ))}
          </ul>
        </div>
      ))}
    </div>
  )
}
