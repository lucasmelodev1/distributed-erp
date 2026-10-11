import { render, screen } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { MemoryRouter, Route, Routes } from "react-router"
import { describe, expect, it, vi } from "vitest"

import { CompanySwitcher } from "@/components/company-switcher"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"

const select = vi.fn()
const acme = { id: "1", name: "Acme", active: true, inactivatedAt: null }
const globex = { id: "2", name: "Globex", active: true, inactivatedAt: null }

vi.mock("@/features/tenants/tenant-context", () => ({
  useCompanies: () => ({
    loading: false,
    companies: [acme, globex],
    current: acme,
    select,
    refresh: vi.fn(),
  }),
}))

function renderSwitcher() {
  return render(
    <MemoryRouter initialEntries={["/dashboard"]}>
      <Routes>
        <Route
          path="/dashboard"
          element={
            <DropdownMenu defaultOpen>
              <DropdownMenuTrigger>menu</DropdownMenuTrigger>
              <DropdownMenuContent>
                <CompanySwitcher />
              </DropdownMenuContent>
            </DropdownMenu>
          }
        />
        <Route path="/companies/new" element={<p>create page</p>} />
      </Routes>
    </MemoryRouter>
  )
}

describe("CompanySwitcher", () => {
  it("marks only the selected company with a check", async () => {
    renderSwitcher()
    const acmeItem = await screen.findByRole("menuitem", { name: /Acme/ })
    const globexItem = screen.getByRole("menuitem", { name: /Globex/ })
    expect(acmeItem.querySelector('[aria-label="Selected"]')).not.toBeNull()
    expect(globexItem.querySelector('[aria-label="Selected"]')).toBeNull()
  })

  it("selects a company when it is clicked", async () => {
    renderSwitcher()
    await userEvent.click(await screen.findByText("Globex"))
    expect(select).toHaveBeenCalledWith("2")
  })

  it("opens the creation screen", async () => {
    renderSwitcher()
    await userEvent.click(await screen.findByText("Create Company"))
    expect(await screen.findByText("create page")).toBeInTheDocument()
  })
})
