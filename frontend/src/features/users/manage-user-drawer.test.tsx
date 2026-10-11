import { render, screen } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { useState } from "react"
import { beforeEach, describe, expect, it, vi } from "vitest"

import { ManageUserDrawer } from "@/features/users/manage-user-drawer"

const save = vi.fn()
const onSaved = vi.fn()

vi.mock("urql", async (importOriginal) => ({
  ...(await importOriginal<typeof import("urql")>()),
  useQuery: () => [
    {
      data: {
        tenantRoles: {
          edges: [
            {
              node: {
                id: "r1",
                name: "Recruiter",
                active: true,
                builtIn: false,
                permissions: [
                  { code: "tenants:invite", description: null, level: "EDIT" },
                ],
              },
            },
            {
              node: {
                id: "r2",
                name: "Retired",
                active: false,
                builtIn: false,
                permissions: [],
              },
            },
            {
              node: {
                id: "r3",
                name: "Reader",
                active: true,
                builtIn: false,
                permissions: [
                  {
                    code: "tenant_roles:read",
                    description: null,
                    level: "VIEW",
                  },
                ],
              },
            },
          ],
        },
      },
      fetching: false,
    },
  ],
  useMutation: () => [{ fetching: false, error: undefined }, save],
}))

const MEMBER = {
  userId: "u1",
  name: "Alice Smith",
  email: "alice@example.com",
  roleIds: ["r1", "r2"],
}

function Host() {
  const [open, setOpen] = useState(true)
  return (
    <>
      <p>{open ? "drawer open" : "drawer closed"}</p>
      <ManageUserDrawer
        member={MEMBER}
        open={open}
        onOpenChange={setOpen}
        onSaved={onSaved}
      />
    </>
  )
}

describe("ManageUserDrawer", () => {
  beforeEach(() => vi.clearAllMocks())

  it("shows the name and email as read-only and the current roles ticked", () => {
    render(<Host />)

    expect(screen.getByLabelText("Name")).toHaveValue("Alice Smith")
    expect(screen.getByLabelText("Name")).toHaveAttribute("readonly")
    expect(screen.getByLabelText("Email")).toHaveValue("alice@example.com")
    expect(screen.getByLabelText("Email")).toHaveAttribute("readonly")
    expect(screen.getByRole("checkbox", { name: "Recruiter" })).toBeChecked()
    expect(screen.getByRole("checkbox", { name: /Retired/ })).toBeChecked()
    expect(screen.getByText("Invite people")).toBeInTheDocument()
  })

  it("does not let an inactive role be added once removed", async () => {
    render(<Host />)

    await userEvent.click(screen.getByRole("checkbox", { name: /Retired/ }))

    expect(screen.getByRole("checkbox", { name: /Retired/ })).not.toBeChecked()
    expect(screen.getByRole("checkbox", { name: /Retired/ })).toHaveAttribute(
      "aria-disabled",
      "true"
    )
  })

  it("saves the chosen role ids", async () => {
    save.mockResolvedValue({
      data: { updateTenantUser: { tenantUser: { roles: [] } } },
    })
    render(<Host />)
    await userEvent.click(screen.getByRole("checkbox", { name: "Reader" }))

    await userEvent.click(screen.getByRole("button", { name: "Save" }))

    expect(save).toHaveBeenCalledWith({
      input: { userId: "u1", tenantRoleIds: ["r1", "r2", "r3"] },
    })
    expect(await screen.findByText("drawer closed")).toBeInTheDocument()
    expect(onSaved).toHaveBeenCalled()
  })

  it("keeps at least one role and asks before discarding changes", async () => {
    render(<Host />)
    await userEvent.click(screen.getByRole("checkbox", { name: "Recruiter" }))
    await userEvent.click(screen.getByRole("checkbox", { name: /Retired/ }))

    await userEvent.click(screen.getByRole("button", { name: "Save" }))
    expect(
      await screen.findByText("Select at least one role")
    ).toBeInTheDocument()
    expect(save).not.toHaveBeenCalled()

    await userEvent.click(screen.getByRole("button", { name: "Cancel" }))
    expect(screen.getByText("Discard your changes?")).toBeInTheDocument()
  })
})
