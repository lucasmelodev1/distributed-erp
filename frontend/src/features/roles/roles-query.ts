import { graphql } from "@/gql"
import type { TenantRolesQuery as TenantRolesResult } from "@/gql/graphql"

/** Roles with their permissions. Unpaged on purpose: the first 100 are enough for now. */
export const TenantRolesQuery = graphql(`
  query TenantRoles($first: Int, $query: String, $active: Boolean) {
    tenantRoles(first: $first, query: $query, active: $active) {
      edges {
        node {
          id
          name
          active
          builtIn
          permissions {
            code
            description
            level
          }
        }
      }
    }
  }
`)

export type Role = TenantRolesResult["tenantRoles"]["edges"][number]["node"]

export const ROLES_LIMIT = 100
