import { render, screen } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { useState } from "react"
import { beforeEach, describe, expect, it, vi } from "vitest"

import { RoleDrawer } from "@/features/roles/role-drawer"

const create = vi.fn()
const save = vi.fn()
const onSaved = vi.fn()

vi.mock("urql", async (importOriginal) => ({
  ...(await importOriginal<typeof import("urql")>()),
  useQuery: () => [
    {
      data: {
        permissions: [
          {
            code: "tenants:invite",
            description: "Invite new tenant members",
            level: "EDIT",
          },
          {
            code: "tenants:update",
            description: "Update tenant information",
            level: "EDIT",
          },
        ],
      },
      fetching: false,
    },
  ],
  useMutation: (document: { definitions: { name?: { value: string } }[] }) => [
    { fetching: false, error: undefined },
    document.definitions[0]?.name?.value === "UpdateTenantRole" ? save : create,
  ],
}))

import type { Role } from "@/features/roles/roles-query"

const ROLE: Role = {
  id: "r1",
  name: "Recruiter",
  active: true,
  builtIn: false,
  permissions: [{ code: "tenants:invite", description: null, level: "EDIT" }],
}

function Host({ role }: { role?: Role }) {
  const [open, setOpen] = useState(true)
  return (
    <>
      <p>{open ? "drawer open" : "drawer closed"}</p>
      <RoleDrawer
        role={role}
        open={open}
        onOpenChange={setOpen}
        onSaved={onSaved}
      />
    </>
  )
}

describe("RoleDrawer", () => {
  beforeEach(() => vi.clearAllMocks())

  it("requires a name and at least one permission", async () => {
    render(<Host />)

    await userEvent.click(screen.getByRole("button", { name: "Create role" }))

    expect(await screen.findByText("Name is required")).toBeInTheDocument()
    expect(
      screen.getByText("Select at least one permission")
    ).toBeInTheDocument()
    expect(create).not.toHaveBeenCalled()
  })

  it("creates the role with the ticked permissions", async () => {
    create.mockResolvedValue({
      data: {
        createTenantRole: { tenantRole: { id: "1", name: "Recruiter" } },
      },
    })
    render(<Host />)
    await userEvent.type(screen.getByLabelText("Name"), "Recruiter")
    await userEvent.click(screen.getByRole("button", { name: "Edit" }))

    await userEvent.click(screen.getByRole("button", { name: "Create role" }))

    expect(create).toHaveBeenCalledWith({
      input: {
        name: "Recruiter",
        permissionCodes: expect.arrayContaining([
          "tenants:invite",
          "tenants:update",
        ]),
      },
    })
    expect(await screen.findByText("drawer closed")).toBeInTheDocument()
    expect(onSaved).toHaveBeenCalled()
  })

  it("asks before discarding what was typed", async () => {
    render(<Host />)
    await userEvent.type(screen.getByLabelText("Name"), "Draft")

    await userEvent.click(screen.getByRole("button", { name: "Cancel" }))

    expect(screen.getByText("Discard your changes?")).toBeInTheDocument()
    expect(screen.getByText("drawer open")).toBeInTheDocument()
  })

  describe("editing a role", () => {
    it("starts from the role's name and permissions", () => {
      render(<Host role={ROLE} />)

      expect(screen.getByText("Edit role")).toBeInTheDocument()
      expect(screen.getByLabelText("Name")).toHaveValue("Recruiter")
      expect(
        screen.getByRole("checkbox", { name: "Invite people" })
      ).toBeChecked()
      expect(
        screen.getByRole("checkbox", { name: "Rename the company" })
      ).not.toBeChecked()
    })

    it("saves the changes to the same role", async () => {
      save.mockResolvedValue({
        data: {
          updateTenantRole: { tenantRole: { id: "r1", name: "Hiring" } },
        },
      })
      render(<Host role={ROLE} />)
      await userEvent.clear(screen.getByLabelText("Name"))
      await userEvent.type(screen.getByLabelText("Name"), "Hiring")
      await userEvent.click(
        screen.getByRole("checkbox", { name: "Rename the company" })
      )

      await userEvent.click(screen.getByRole("button", { name: "Save" }))

      expect(save).toHaveBeenCalledWith({
        input: {
          id: "r1",
          name: "Hiring",
          permissionCodes: expect.arrayContaining([
            "tenants:invite",
            "tenants:update",
          ]),
        },
      })
      expect(create).not.toHaveBeenCalled()
      expect(await screen.findByText("drawer closed")).toBeInTheDocument()
      expect(onSaved).toHaveBeenCalled()
    })

    it("closes without asking when nothing was changed", async () => {
      render(<Host role={ROLE} />)

      await userEvent.click(screen.getByRole("button", { name: "Cancel" }))

      expect(screen.getByText("drawer closed")).toBeInTheDocument()
    })
  })
})
