import { render, screen, waitFor } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { beforeEach, describe, expect, it, vi } from "vitest"

import { RolesPage } from "@/features/roles/roles-page"

const useQuery = vi.fn()
const reload = vi.fn()
const inactivate = vi.fn()
const reactivate = vi.fn()

vi.mock("urql", async (importOriginal) => ({
  ...(await importOriginal<typeof import("urql")>()),
  useQuery: (args: unknown) => useQuery(args),
  useMutation: (document: { definitions: { name?: { value: string } }[] }) => [
    { fetching: false, error: undefined },
    document.definitions[0]?.name?.value === "InactivateTenantRole"
      ? inactivate
      : reactivate,
  ],
}))
vi.mock("@/features/tenants/tenant-context", () => ({
  useCompanies: () => ({ current: { id: "tenant-1" } }),
}))
vi.mock("@/features/roles/role-drawer", () => ({
  RoleDrawer: ({ open, role }: { open: boolean; role?: { name: string } }) =>
    open ? <p>drawer {role?.name ?? "new"}</p> : null,
}))

const role = (id: string, name: string, active: boolean, builtIn = false) => ({
  node: {
    id,
    name,
    active,
    builtIn,
    permissions: [{ code: "tenants:invite", description: null, level: "EDIT" }],
  },
})

function lastVariables() {
  return useQuery.mock.lastCall![0].variables
}

describe("RolesPage", () => {
  beforeEach(() => {
    vi.clearAllMocks()
    inactivate.mockResolvedValue({ data: {} })
    reactivate.mockResolvedValue({ data: {} })
    useQuery.mockReturnValue([
      {
        fetching: false,
        data: {
          tenantRoles: {
            edges: [
              role("1", "Admin", true, true),
              role("2", "Recruiter", true),
              role("3", "Retired", false),
            ],
          },
        },
      },
      reload,
    ])
  })

  it("lists roles, marks the default one and gives it no actions", () => {
    render(<RolesPage />)

    expect(screen.getByText("Admin")).toBeInTheDocument()
    expect(screen.getByText("Default")).toBeInTheDocument()
    expect(
      screen.queryByRole("button", { name: "Actions for Admin" })
    ).toBeNull()
    expect(
      screen.getByRole("button", { name: "Actions for Recruiter" })
    ).toBeInTheDocument()
  })

  it("asks for the first 100 roles and sends the search and status filter", async () => {
    render(<RolesPage />)
    expect(lastVariables().first).toBe(100)

    await userEvent.type(screen.getByLabelText("Search roles"), "rec")
    await waitFor(() => expect(lastVariables().query).toBe("rec"))

    await userEvent.click(
      screen.getByRole("combobox", { name: "Filter by status" })
    )
    await userEvent.click(
      await screen.findByRole("option", { name: "Inactive" })
    )
    await waitFor(() => expect(lastVariables().active).toBe(false))
  })

  it("inactivates only after confirming", async () => {
    render(<RolesPage />)

    await userEvent.click(
      screen.getByRole("button", { name: "Actions for Recruiter" })
    )
    await userEvent.click(
      await screen.findByRole("menuitem", { name: "Inactivate" })
    )
    expect(inactivate).not.toHaveBeenCalled()

    await userEvent.click(screen.getByRole("button", { name: "Inactivate" }))

    expect(inactivate).toHaveBeenCalledWith({ input: { id: "2" } })
    await waitFor(() => expect(reload).toHaveBeenCalled())
  })

  it("reactivates an inactive role straight away", async () => {
    render(<RolesPage />)

    await userEvent.click(
      screen.getByRole("button", { name: "Actions for Retired" })
    )
    await userEvent.click(
      await screen.findByRole("menuitem", { name: "Reactivate" })
    )

    expect(reactivate).toHaveBeenCalledWith({ input: { id: "3" } })
  })

  it("opens the create-role drawer", async () => {
    render(<RolesPage />)

    await userEvent.click(screen.getByRole("button", { name: "Create Role" }))

    expect(screen.getByText("drawer new")).toBeInTheDocument()
  })

  it("opens the drawer on the role to manage, but not for the default role", async () => {
    render(<RolesPage />)

    await userEvent.click(
      screen.getByRole("button", { name: "Actions for Recruiter" })
    )
    await userEvent.click(
      await screen.findByRole("menuitem", { name: "Manage" })
    )

    expect(screen.getByText("drawer Recruiter")).toBeInTheDocument()
    expect(
      screen.queryByRole("button", { name: "Actions for Admin" })
    ).toBeNull()
  })
})
