import type { Permission } from "@/features/access/permissions"
import { useCan } from "@/features/access/use-can"

/** Renders its children only when the user holds the permission(s) in the company. */
export function Can({
  permission,
  companyId,
  children,
}: {
  permission: Permission | Permission[]
  companyId?: string
  children: React.ReactNode
}) {
  const can = useCan()
  return can(permission, companyId) ? children : null
}
