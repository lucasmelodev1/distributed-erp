import { render, screen, waitFor } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { beforeEach, describe, expect, it, vi } from "vitest"

import { InvitationsPage } from "@/features/users/invitations-page"

const useQuery = vi.fn()
const resend = vi.fn()
const revoke = vi.fn()

vi.mock("urql", async (importOriginal) => ({
  ...(await importOriginal<typeof import("urql")>()),
  useQuery: (args: unknown) => useQuery(args),
  useMutation: (document: { definitions: { name?: { value: string } }[] }) => [
    { fetching: false, error: undefined },
    document.definitions[0]?.name?.value === "ResendInvitation"
      ? resend
      : revoke,
  ],
}))
vi.mock("@/features/tenants/tenant-context", () => ({
  useCompanies: () => ({ current: { id: "tenant-1" } }),
}))
vi.mock("@/features/users/invite-drawer", () => ({
  InviteDrawer: ({ open }: { open: boolean }) => (open ? <p>drawer</p> : null),
}))

function lastVariables() {
  return useQuery.mock.lastCall![0].variables
}

describe("InvitationsPage", () => {
  beforeEach(() => {
    vi.clearAllMocks()
    useQuery.mockReturnValue([
      {
        fetching: false,
        data: {
          tenantInvitations: {
            edges: [
              {
                node: {
                  id: "1",
                  email: "a@example.com",
                  status: "PENDING",
                  expiresAt: null,
                  roles: [{ id: "r", name: "Member" }],
                },
              },
              {
                node: {
                  id: "2",
                  email: "b@example.com",
                  status: "EXPIRED",
                  expiresAt: null,
                  roles: [],
                },
              },
              {
                node: {
                  id: "3",
                  email: "c@example.com",
                  status: "ACCEPTED",
                  expiresAt: null,
                  roles: [],
                },
              },
            ],
            pageInfo: { hasNextPage: true, endCursor: "cursor-1" },
          },
        },
      },
      vi.fn(),
    ])
  })

  it("lists invitations and opens the drawer", async () => {
    render(<InvitationsPage />)

    expect(screen.getByText("a@example.com")).toBeInTheDocument()
    expect(screen.getByText("Pending")).toBeInTheDocument()

    await userEvent.click(
      screen.getByRole("button", { name: "Send Invitation" })
    )
    expect(screen.getByText("drawer")).toBeInTheDocument()
  })

  it("resends the email of a pending or expired invitation, not an accepted one", async () => {
    resend.mockResolvedValue({ data: {} })
    render(<InvitationsPage />)

    expect(
      screen.queryByRole("button", { name: "Actions for c@example.com" })
    ).toBeNull()

    await userEvent.click(
      screen.getByRole("button", { name: "Actions for b@example.com" })
    )
    expect(screen.queryByRole("menuitem", { name: "Revoke" })).toBeNull()
    await userEvent.click(
      await screen.findByRole("menuitem", { name: "Resend email" })
    )

    expect(resend).toHaveBeenCalledWith({ input: { invitationId: "2" } })
  })

  it("offers revoking only for pending invitations", async () => {
    render(<InvitationsPage />)

    await userEvent.click(
      screen.getByRole("button", { name: "Actions for a@example.com" })
    )

    expect(
      await screen.findByRole("menuitem", { name: "Resend email" })
    ).toBeInTheDocument()
    expect(screen.getByRole("menuitem", { name: "Revoke" })).toBeInTheDocument()
  })

  it("sends the search text to the server after a pause", async () => {
    render(<InvitationsPage />)

    await userEvent.type(screen.getByLabelText("Search invitations"), "bob")

    await waitFor(() => expect(lastVariables().query).toBe("bob"))
  })

  it("filters by status", async () => {
    render(<InvitationsPage />)

    await userEvent.click(
      screen.getByRole("combobox", { name: "Filter by status" })
    )
    await userEvent.click(
      await screen.findByRole("option", { name: "Revoked" })
    )

    await waitFor(() => expect(lastVariables().status).toBe("REVOKED"))
  })

  it("pages forward with the end cursor and back again", async () => {
    render(<InvitationsPage />)
    expect(lastVariables().after).toBeNull()

    await userEvent.click(screen.getByRole("button", { name: /Next/ }))
    expect(lastVariables().after).toBe("cursor-1")

    await userEvent.click(screen.getByRole("button", { name: /Previous/ }))
    expect(lastVariables().after).toBeNull()
  })
})
