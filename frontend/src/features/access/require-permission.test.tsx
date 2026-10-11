import { render, screen } from "@testing-library/react"
import { MemoryRouter, Route, Routes } from "react-router"
import { describe, expect, it, vi } from "vitest"

import { PERMISSIONS } from "@/features/access/permissions"
import { RequirePermission } from "@/features/access/require-permission"

const state = vi.hoisted(() => ({
  loading: false,
  permissions: [] as string[],
}))

vi.mock("@/features/tenants/tenant-context", () => ({
  useCompanies: () => ({
    loading: state.loading,
    current: { id: "a" },
    companies: [{ id: "a", active: true, permissions: state.permissions }],
  }),
}))

function renderGuard() {
  return render(
    <MemoryRouter>
      <Routes>
        <Route
          element={<RequirePermission permission={PERMISSIONS.tenantsInvite} />}
        >
          <Route path="/" element={<p>secret page</p>} />
        </Route>
      </Routes>
    </MemoryRouter>
  )
}

describe("RequirePermission", () => {
  it("renders the page when the permission is held", () => {
    state.loading = false
    state.permissions = [PERMISSIONS.tenantsInvite]
    renderGuard()
    expect(screen.getByText("secret page")).toBeInTheDocument()
  })

  it("shows no access when it is missing", () => {
    state.loading = false
    state.permissions = []
    renderGuard()
    expect(screen.queryByText("secret page")).toBeNull()
    expect(screen.getByText("No access")).toBeInTheDocument()
  })

  it("shows neither while permissions are loading", () => {
    state.loading = true
    state.permissions = []
    renderGuard()
    expect(screen.queryByText("secret page")).toBeNull()
    expect(screen.queryByText("No access")).toBeNull()
  })
})
