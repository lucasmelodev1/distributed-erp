import { render, screen } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { useState } from "react"
import { beforeEach, describe, expect, it, vi } from "vitest"

import { InviteDrawer } from "@/features/users/invite-drawer"

const invite = vi.fn()
const onInvited = vi.fn()
const useQuery = vi.fn()

vi.mock("urql", async (importOriginal) => ({
  ...(await importOriginal<typeof import("urql")>()),
  useQuery: (args: unknown) => useQuery(args),
  useMutation: () => [{ fetching: false, error: undefined }, invite],
}))

const role = (id: string, name: string, codes: string[]) => ({
  node: {
    id,
    name,
    active: true,
    builtIn: false,
    permissions: codes.map((code) => ({
      code,
      description: null,
      level: code === "tenant_roles:read" ? "VIEW" : "EDIT",
    })),
  },
})

function Host() {
  const [open, setOpen] = useState(true)
  return (
    <>
      <p>{open ? "drawer open" : "drawer closed"}</p>
      <InviteDrawer open={open} onOpenChange={setOpen} onInvited={onInvited} />
    </>
  )
}

describe("InviteDrawer", () => {
  beforeEach(() => {
    vi.clearAllMocks()
    useQuery.mockReturnValue([
      {
        data: {
          tenantRoles: {
            edges: [
              role("r1", "Recruiter", ["tenants:invite"]),
              role("r2", "Reader", ["tenant_roles:read", "tenants:invite"]),
            ],
          },
        },
        fetching: false,
      },
    ])
  })

  it("only asks for active roles", () => {
    render(<Host />)

    expect(useQuery.mock.lastCall![0].variables.active).toBe(true)
  })

  it("closes straight away when nothing was filled in", async () => {
    render(<Host />)

    await userEvent.click(screen.getByRole("button", { name: "Cancel" }))

    expect(screen.getByText("drawer closed")).toBeInTheDocument()
    expect(screen.queryByText("Discard your changes?")).not.toBeInTheDocument()
  })

  it("asks before discarding what was typed, and keeps it on 'Keep editing'", async () => {
    render(<Host />)
    await userEvent.type(screen.getByLabelText("Email"), "new@example.com")

    await userEvent.click(screen.getByRole("button", { name: "Cancel" }))
    expect(screen.getByText("Discard your changes?")).toBeInTheDocument()
    expect(screen.getByText("drawer open")).toBeInTheDocument()

    await userEvent.click(screen.getByRole("button", { name: "Keep editing" }))
    expect(screen.getByLabelText("Email")).toHaveValue("new@example.com")

    await userEvent.click(screen.getByRole("button", { name: "Cancel" }))
    await userEvent.click(screen.getByRole("button", { name: "Discard" }))
    expect(screen.getByText("drawer closed")).toBeInTheDocument()
  })

  it("asks before discarding a ticked role too", async () => {
    render(<Host />)
    await userEvent.click(screen.getByRole("checkbox", { name: "Recruiter" }))

    await userEvent.click(screen.getByRole("button", { name: "Cancel" }))

    expect(screen.getByText("Discard your changes?")).toBeInTheDocument()
  })

  it("shows the permissions of the chosen roles added together", async () => {
    render(<Host />)
    expect(
      screen.getByText("The selected roles allow nothing.")
    ).toBeInTheDocument()

    await userEvent.click(screen.getByRole("checkbox", { name: "Recruiter" }))
    expect(screen.getByText("Invite people")).toBeInTheDocument()
    expect(screen.queryByText("View roles")).not.toBeInTheDocument()

    await userEvent.click(screen.getByRole("checkbox", { name: "Reader" }))
    expect(screen.getAllByText("Invite people")).toHaveLength(1)
    expect(screen.getByText("View roles")).toBeInTheDocument()
  })

  it("validates before calling the API", async () => {
    render(<Host />)

    await userEvent.click(
      screen.getByRole("button", { name: "Send invitation" })
    )

    expect(await screen.findByText("Email is required")).toBeInTheDocument()
    expect(screen.getByText("Select at least one role")).toBeInTheDocument()
    expect(invite).not.toHaveBeenCalled()
  })

  it("sends the email and role ids, then closes without asking", async () => {
    invite.mockResolvedValue({
      data: {
        inviteUserToTenant: {
          invitation: { id: "1", email: "new@example.com" },
        },
      },
    })
    render(<Host />)
    await userEvent.type(screen.getByLabelText("Email"), "new@example.com")
    await userEvent.click(screen.getByRole("checkbox", { name: "Recruiter" }))
    await userEvent.click(screen.getByRole("checkbox", { name: "Reader" }))

    await userEvent.click(
      screen.getByRole("button", { name: "Send invitation" })
    )

    expect(invite).toHaveBeenCalledWith({
      input: { email: "new@example.com", tenantRoleIds: ["r1", "r2"] },
    })
    expect(await screen.findByText("drawer closed")).toBeInTheDocument()
    expect(onInvited).toHaveBeenCalled()
  })
})
