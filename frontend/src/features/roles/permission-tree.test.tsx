import { render, screen, within } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { useState } from "react"
import { describe, expect, it } from "vitest"

import { groupPermissions } from "@/features/roles/permission-labels"
import { PermissionTree } from "@/features/roles/permission-tree"
import type { PermissionLevel } from "@/gql/graphql"

const perm = (code: string, level: PermissionLevel, description?: string) => ({
  code,
  level,
  description,
})

const PERMISSIONS = [
  perm("tenant_roles:inactivate", "SENSITIVE"),
  perm("tenant_roles:read", "VIEW"),
  perm("tenant_roles:create", "EDIT"),
  perm("tenants:expel", "SENSITIVE"),
  perm("tenants:invite", "EDIT"),
  perm("reports:view", "VIEW", "View reports"),
  perm("reports:export", "EDIT"),
]

function Harness({ initial = [] }: { initial?: string[] }) {
  const [value, setValue] = useState(initial)
  return (
    <>
      <PermissionTree
        permissions={PERMISSIONS}
        value={value}
        onChange={setValue}
      />
      <output>{[...value].sort().join(",")}</output>
    </>
  )
}

function levelButton(area: string, name: string) {
  return within(
    screen.getByRole("group", { name: `${area} access` })
  ).getByRole("button", { name })
}

const selection = () => screen.getByRole("status").textContent

describe("groupPermissions", () => {
  it("uses friendly names, falling back to the description, then the code", () => {
    const groups = groupPermissions(PERMISSIONS)

    expect(groups.map((group) => group.label)).toEqual([
      "Company",
      "Roles",
      "Reports",
    ])
    expect(groups[2]!.permissions.map((p) => p.label)).toEqual([
      "View reports",
      "reports:export",
    ])
  })

  it("orders each area by level: view, edit, then sensitive", () => {
    const [company, roles] = groupPermissions(PERMISSIONS)

    expect(company!.permissions.map((p) => p.level)).toEqual([
      "EDIT",
      "SENSITIVE",
    ])
    expect(roles!.permissions.map((p) => p.level)).toEqual([
      "VIEW",
      "EDIT",
      "SENSITIVE",
    ])
  })
})

describe("PermissionTree", () => {
  it("shows the icon of each permission's level", () => {
    render(<Harness />)

    expect(screen.getAllByRole("img", { name: "View" })).toHaveLength(2)
    expect(screen.getAllByRole("img", { name: "Edit" })).toHaveLength(3)
    expect(
      screen.getAllByRole("img", { name: "Sensitive actions" })
    ).toHaveLength(2)
  })

  it("selects everything up to the clicked level and lights the buttons below it", async () => {
    render(<Harness />)

    await userEvent.click(levelButton("Roles", "Edit"))

    expect(selection()).toBe("tenant_roles:create,tenant_roles:read")
    expect(levelButton("Roles", "View")).toHaveAttribute("aria-pressed", "true")
    expect(levelButton("Roles", "Edit")).toHaveAttribute("aria-pressed", "true")
    expect(levelButton("Roles", "Sensitive actions")).toHaveAttribute(
      "aria-pressed",
      "false"
    )

    await userEvent.click(levelButton("Roles", "Sensitive actions"))
    expect(levelButton("Roles", "Sensitive actions")).toHaveAttribute(
      "aria-pressed",
      "true"
    )
    expect(
      screen.getByRole("checkbox", { name: "Inactivate or reactivate roles" })
    ).toBeChecked()
  })

  it("lowers the selection when a lower level is clicked, and clears it from the lowest", async () => {
    render(
      <Harness
        initial={[
          "tenant_roles:read",
          "tenant_roles:create",
          "tenant_roles:inactivate",
        ]}
      />
    )

    await userEvent.click(levelButton("Roles", "View"))
    expect(selection()).toBe("tenant_roles:read")

    await userEvent.click(levelButton("Roles", "View"))
    expect(selection()).toBe("")
  })

  it("lights the buttons when the boxes are ticked by hand", async () => {
    render(<Harness />)

    await userEvent.click(screen.getByRole("checkbox", { name: "View roles" }))
    expect(levelButton("Roles", "View")).toHaveAttribute("aria-pressed", "true")
    expect(levelButton("Roles", "Edit")).toHaveAttribute(
      "aria-pressed",
      "false"
    )

    await userEvent.click(
      screen.getByRole("checkbox", { name: "Create roles" })
    )
    expect(levelButton("Roles", "Edit")).toHaveAttribute("aria-pressed", "true")
  })

  it("disables the level an area does not have and leaves other areas alone", async () => {
    render(<Harness initial={["reports:view"]} />)

    expect(levelButton("Company", "View")).toBeDisabled()

    await userEvent.click(levelButton("Company", "Edit"))
    expect(selection()).toBe("reports:view,tenants:invite")
  })
})
