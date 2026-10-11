import { LEVEL_INFO } from "@/features/roles/permission-levels"
import type { PermissionLevel } from "@/gql/graphql"

/** The icon that stands for a permission's level: small, and the same colour for every level. */
export function PermissionLevelIcon({ level }: { level: PermissionLevel }) {
  const { icon: Icon, label } = LEVEL_INFO[level]
  return (
    <Icon role="img" aria-label={label} className="size-3 text-foreground" />
  )
}
