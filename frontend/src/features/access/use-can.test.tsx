import { renderHook } from "@testing-library/react"
import { describe, expect, it, vi } from "vitest"

import { PERMISSIONS } from "@/features/access/permissions"
import { useCan } from "@/features/access/use-can"

const companies = [
  {
    id: "a",
    active: true,
    permissions: [PERMISSIONS.tenantsInvite, PERMISSIONS.tenantsUpdate],
  },
  { id: "b", permissions: [] as string[], active: true },
  {
    id: "off",
    active: false,
    permissions: [PERMISSIONS.tenantsUpdate, PERMISSIONS.tenantsInactivate],
  },
]
const state = vi.hoisted(() => ({ currentId: "a" as string | null }))

vi.mock("@/features/tenants/tenant-context", () => ({
  useCompanies: () => ({
    companies,
    current: state.currentId ? { id: state.currentId } : null,
  }),
}))

describe("useCan", () => {
  it("checks the selected company", () => {
    state.currentId = "a"
    const { result } = renderHook(() => useCan())

    expect(result.current(PERMISSIONS.tenantsInvite)).toBe(true)
    expect(result.current(PERMISSIONS.tenantRolesRead)).toBe(false)
  })

  it("needs every permission of a list", () => {
    state.currentId = "a"
    const { result } = renderHook(() => useCan())

    expect(
      result.current([PERMISSIONS.tenantsInvite, PERMISSIONS.tenantsUpdate])
    ).toBe(true)
    expect(
      result.current([PERMISSIONS.tenantsInvite, PERMISSIONS.tenantRolesRead])
    ).toBe(false)
  })

  it("can check another company", () => {
    state.currentId = "b"
    const { result } = renderHook(() => useCan())

    expect(result.current(PERMISSIONS.tenantsInvite)).toBe(false)
    expect(result.current(PERMISSIONS.tenantsInvite, "a")).toBe(true)
  })

  it("denies when there is no company or it is unknown", () => {
    state.currentId = null
    const { result } = renderHook(() => useCan())

    expect(result.current(PERMISSIONS.tenantsInvite)).toBe(false)
    expect(result.current(PERMISSIONS.tenantsInvite, "zzz")).toBe(false)
  })

  it("only allows reactivating an inactive company", () => {
    state.currentId = "off"
    const { result } = renderHook(() => useCan())

    expect(result.current(PERMISSIONS.tenantsUpdate)).toBe(false)
    expect(result.current(PERMISSIONS.tenantsInactivate)).toBe(true)
  })
})
