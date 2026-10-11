import { describe, expect, it } from "vitest"

import { isLevelOn, selectLevel } from "@/features/roles/permission-levels"
import type { PermissionLevel } from "@/gql/graphql"

const perm = (code: string, level: PermissionLevel) => ({ code, level })

const ROLES = [
  perm("read", "VIEW"),
  perm("create", "EDIT"),
  perm("update", "EDIT"),
  perm("inactivate", "SENSITIVE"),
]
// An area with nothing to view.
const COMPANY = [perm("update", "EDIT"), perm("expel", "SENSITIVE")]

const set = (...codes: string[]) => new Set(codes)

describe("isLevelOn", () => {
  it("is on only when everything up to the level is selected", () => {
    expect(isLevelOn(ROLES, "VIEW", set("read"))).toBe(true)
    expect(isLevelOn(ROLES, "EDIT", set("read"))).toBe(false)
    expect(isLevelOn(ROLES, "EDIT", set("read", "create", "update"))).toBe(true)
    expect(isLevelOn(ROLES, "SENSITIVE", set("read", "create", "update"))).toBe(
      false
    )
  })

  it("follows boxes ticked by hand", () => {
    expect(isLevelOn(ROLES, "VIEW", set("create"))).toBe(false)
    expect(isLevelOn(ROLES, "VIEW", set("read", "create"))).toBe(true)
  })

  it("is never on for a level the area does not have", () => {
    expect(isLevelOn(COMPANY, "VIEW", set())).toBe(false)
    expect(isLevelOn(COMPANY, "EDIT", set("update"))).toBe(true)
  })
})

describe("selectLevel", () => {
  it("selects everything up to the clicked level", () => {
    expect(selectLevel(ROLES, "VIEW", set())).toEqual(["read"])
    expect(selectLevel(ROLES, "EDIT", set())).toEqual([
      "read",
      "create",
      "update",
    ])
    expect(selectLevel(ROLES, "SENSITIVE", set())).toEqual([
      "read",
      "create",
      "update",
      "inactivate",
    ])
  })

  it("lowers the selection when a lower level is clicked", () => {
    const all = set("read", "create", "update", "inactivate")
    expect(selectLevel(ROLES, "VIEW", all)).toEqual(["read"])
    expect(selectLevel(ROLES, "EDIT", all)).toEqual([
      "read",
      "create",
      "update",
    ])
  })

  it("steps one level down when the highest level that is on is clicked", () => {
    expect(selectLevel(ROLES, "EDIT", set("read", "create", "update"))).toEqual(
      ["read"]
    )
    expect(selectLevel(ROLES, "VIEW", set("read"))).toEqual([])
    expect(selectLevel(COMPANY, "EDIT", set("update"))).toEqual([])
  })
})
