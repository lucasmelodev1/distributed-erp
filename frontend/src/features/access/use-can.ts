import { useCallback } from "react"

import { PERMISSIONS, type Permission } from "@/features/access/permissions"
import { useCompanies } from "@/features/tenants/tenant-context"

/**
 * The one place that answers "may the user do this?". Permissions come with `myTenants`, which
 * is refetched on every page entry. `can` is true only when every given permission is held in
 * `companyId` (default: the selected company), and false while loading. In an inactive
 * company only `tenants:inactivate` (reactivation) counts, as on the backend.
 */
export function useCan() {
  const { companies, current } = useCompanies()
  return useCallback(
    (permission: Permission | Permission[], companyId = current?.id) => {
      const company = companies.find((candidate) => candidate.id === companyId)
      if (!company) return false
      return [permission].flat().every(
        (code) =>
          company.permissions.includes(code) &&
          // An inactive company rejects everything except reactivating it.
          (company.active || code === PERMISSIONS.tenantsInactivate)
      )
    },
    [companies, current]
  )
}
