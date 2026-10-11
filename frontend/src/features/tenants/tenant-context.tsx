import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
} from "react"
import { useClient, useQuery } from "urql"

import { graphql } from "@/gql"
import type { MyTenantsQuery as MyTenantsResult } from "@/gql/graphql"
import { getTenantId, setTenantId } from "@/lib/tenant"

export const MyTenantsQuery = graphql(`
  query MyTenants {
    myTenants {
      id
      name
      active
      inactivatedAt
    }
  }
`)

export type Company = MyTenantsResult["myTenants"][number]

type Companies = {
  loading: boolean
  companies: Company[]
  /** The company tenant-aware requests are sent for, if the user has any usable one. */
  current: Company | null
  select: (id: string) => void
  /** Re-reads the list from the server; call after changing a company. */
  refresh: () => Promise<void>
}

const CompaniesContext = createContext<Companies | null>(null)

/** Picks the stored company if the user still has it, else the first active one. */
function pick(companies: Company[], storedId: string | null) {
  return (
    companies.find((company) => company.id === storedId) ??
    companies.find((company) => company.active) ??
    null
  )
}

export function CompaniesProvider({ children }: { children: React.ReactNode }) {
  const client = useClient()
  const [{ data, fetching }, reexecute] = useQuery({ query: MyTenantsQuery })
  const [selectedId, setSelectedId] = useState(getTenantId)

  const companies = useMemo(() => data?.myTenants ?? [], [data])
  const current = useMemo(
    () => pick(companies, selectedId),
    [companies, selectedId]
  )

  // Keeps the `X-Tenant-Id` header in step with what the sidebar shows.
  useEffect(() => {
    if (!fetching) setTenantId(current?.id ?? null)
  }, [current, fetching])

  const select = useCallback((id: string) => {
    setTenantId(id)
    setSelectedId(id)
  }, [])

  // Fetch fresh, then let the mounted query pick the result up from the cache.
  const refresh = useCallback(async () => {
    await client
      .query(MyTenantsQuery, {}, { requestPolicy: "network-only" })
      .toPromise()
    reexecute({ requestPolicy: "cache-first" })
  }, [client, reexecute])

  const value = useMemo(
    () => ({
      loading: fetching && !data,
      companies,
      current,
      select,
      refresh,
    }),
    [fetching, data, companies, current, select, refresh]
  )
  return <CompaniesContext value={value}>{children}</CompaniesContext>
}

export function useCompanies() {
  const companies = useContext(CompaniesContext)
  if (!companies)
    throw new Error("useCompanies must be used inside CompaniesProvider")
  return companies
}
