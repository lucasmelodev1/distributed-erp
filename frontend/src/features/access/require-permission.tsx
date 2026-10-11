import { Outlet } from "react-router"

import { NoAccess } from "@/features/access/no-access"
import type { Permission } from "@/features/access/permissions"
import { useCan } from "@/features/access/use-can"
import { useCompanies } from "@/features/tenants/tenant-context"
import { Spinner } from "@/components/ui/spinner"

/** Layout route: renders the page only when the selected company grants the permission. */
export function RequirePermission({ permission }: { permission: Permission }) {
  const { loading } = useCompanies()
  const can = useCan()
  if (loading) return <Spinner />
  return can(permission) ? <Outlet /> : <NoAccess />
}
