const STORAGE_KEY = "tenantId"

function read(): string | null {
  try {
    return localStorage.getItem(STORAGE_KEY)
  } catch {
    return null
  }
}

let currentTenantId = read()

/** The company every tenant-aware request is sent for (the `X-Tenant-Id` header). */
export function getTenantId() {
  return currentTenantId
}

export function setTenantId(id: string | null) {
  currentTenantId = id
  try {
    if (id) localStorage.setItem(STORAGE_KEY, id)
    else localStorage.removeItem(STORAGE_KEY)
  } catch {
    // Storage can be unavailable (private mode); the id then lives for this page only.
  }
}

export function tenantFetchOptions(id: string | null): RequestInit {
  return {
    credentials: "include",
    headers: id ? { "X-Tenant-Id": id } : {},
  }
}

/**
 * Per-operation urql context that targets a specific company instead of the selected one.
 * Pass it as the second argument of an `executeMutation` / `useQuery` call.
 */
export function tenantContext(id: string) {
  return { fetchOptions: tenantFetchOptions(id) }
}
