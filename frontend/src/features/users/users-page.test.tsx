import { render, screen, waitFor } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { beforeEach, describe, expect, it, vi } from "vitest"

import { PERMISSIONS } from "@/features/access/permissions"
import { UsersPage } from "@/features/users/users-page"

// The permissions the signed-in user holds in the selected company.
const held = vi.hoisted(() => ({ permissions: [] as string[] }))
const useQuery = vi.fn()
const reload = vi.fn()
const inactivate = vi.fn()
const reactivate = vi.fn()

vi.mock("urql", async (importOriginal) => ({
  ...(await importOriginal<typeof import("urql")>()),
  useQuery: (args: unknown) => useQuery(args),
  // Order of the useMutation calls in the page: inactivate, then reactivate.
  useMutation: vi.fn().mockImplementation(() => [{ fetching: false }, vi.fn()]),
}))
vi.mock("@/features/tenants/tenant-context", () => ({
  useCompanies: () => ({
    current: { id: "tenant-1" },
    companies: [
      { id: "tenant-1", permissions: held.permissions, active: true },
    ],
  }),
}))
vi.mock("@/features/users/manage-user-drawer", () => ({
  ManageUserDrawer: ({ member }: { member: { name: string } }) => (
    <p>managing {member.name}</p>
  ),
}))
vi.mock("@/features/auth/session", () => ({
  useSession: () => ({ user: { id: "me" } }),
}))

const node = (id: string, name: string, active: boolean) => ({
  node: {
    active,
    user: { id, name, email: `${name.toLowerCase()}@example.com` },
    roles: [{ id: "r", name: "Member" }],
  },
})

function lastVariables() {
  return useQuery.mock.lastCall![0].variables
}

async function setupMutations() {
  const { useMutation } = await import("urql")
  vi.mocked(useMutation)
    .mockReset()
    .mockImplementation(((document: {
      definitions: { name?: { value: string } }[]
    }) => {
      const name = document.definitions[0]?.name?.value
      return [
        { fetching: false },
        name === "InactivateTenantUser" ? inactivate : reactivate,
      ]
    }) as never)
}

describe("UsersPage", () => {
  beforeEach(async () => {
    vi.clearAllMocks()
    held.permissions = Object.values(PERMISSIONS)
    await setupMutations()
    inactivate.mockResolvedValue({ data: {} })
    reactivate.mockResolvedValue({ data: {} })
    useQuery.mockReturnValue([
      {
        fetching: false,
        data: {
          tenantUsers: {
            edges: [
              node("me", "Myself", true),
              node("u1", "Alice", true),
              node("u2", "Bob", false),
            ],
            pageInfo: { hasNextPage: true, endCursor: "cursor-1" },
          },
        },
      },
      reload,
    ])
  })

  it("sends the search text and the status filter to the server", async () => {
    render(<UsersPage />)

    await userEvent.type(screen.getByLabelText("Search users"), "ali")
    await waitFor(() => expect(lastVariables().query).toBe("ali"))

    await userEvent.click(
      screen.getByRole("combobox", { name: "Filter by status" })
    )
    await userEvent.click(
      await screen.findByRole("option", { name: "Inactive" })
    )
    await waitFor(() => expect(lastVariables().active).toBe(false))
  })

  it("pages forward with the end cursor", async () => {
    render(<UsersPage />)

    await userEvent.click(screen.getByRole("button", { name: /Next/ }))

    expect(lastVariables().after).toBe("cursor-1")
  })

  it("has no actions on the signed-in user's own row", () => {
    render(<UsersPage />)

    expect(
      screen.queryByRole("button", { name: "Actions for Myself" })
    ).toBeNull()
    expect(
      screen.getByRole("button", { name: "Actions for Alice" })
    ).toBeInTheDocument()
  })

  it("inactivates a member only after confirming", async () => {
    render(<UsersPage />)

    await userEvent.click(
      screen.getByRole("button", { name: "Actions for Alice" })
    )
    await userEvent.click(
      await screen.findByRole("menuitem", { name: "Inactivate" })
    )
    expect(inactivate).not.toHaveBeenCalled()

    await userEvent.click(screen.getByRole("button", { name: "Inactivate" }))

    expect(inactivate).toHaveBeenCalledWith({ input: { userId: "u1" } })
    await waitFor(() => expect(reload).toHaveBeenCalled())
  })

  it("reactivates an inactive member straight away", async () => {
    render(<UsersPage />)

    await userEvent.click(
      screen.getByRole("button", { name: "Actions for Bob" })
    )
    await userEvent.click(
      await screen.findByRole("menuitem", { name: "Reactivate" })
    )

    expect(reactivate).toHaveBeenCalledWith({ input: { userId: "u2" } })
  })

  it("opens the manage drawer for the chosen member", async () => {
    render(<UsersPage />)

    await userEvent.click(
      screen.getByRole("button", { name: "Actions for Alice" })
    )
    await userEvent.click(
      await screen.findByRole("menuitem", { name: "Manage" })
    )

    expect(screen.getByText("managing Alice")).toBeInTheDocument()
  })

  it("shows only the actions the user may do", async () => {
    held.permissions = [PERMISSIONS.tenantUsersInactivate]
    render(<UsersPage />)

    await userEvent.click(
      screen.getByRole("button", { name: "Actions for Alice" })
    )

    expect(
      await screen.findByRole("menuitem", { name: "Inactivate" })
    ).toBeInTheDocument()
    expect(screen.queryByRole("menuitem", { name: "Manage" })).toBeNull()
  })

  it("hides the actions menu when the user may do none of them", () => {
    held.permissions = [PERMISSIONS.tenantUsersRead]
    render(<UsersPage />)

    expect(screen.queryByRole("button", { name: /^Actions for/ })).toBeNull()
  })
})
