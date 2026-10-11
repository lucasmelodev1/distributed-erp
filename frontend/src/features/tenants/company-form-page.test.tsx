import { render, screen } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { MemoryRouter, Route, Routes } from "react-router"
import { beforeEach, describe, expect, it, vi } from "vitest"

import { CompanyFormPage } from "@/features/tenants/company-form-page"

const create = vi.fn()
const select = vi.fn()
const refresh = vi.fn().mockResolvedValue(undefined)

vi.mock("urql", async (importOriginal) => ({
  ...(await importOriginal<typeof import("urql")>()),
  useMutation: () => [{ fetching: false, error: undefined }, create],
}))
vi.mock("@/features/tenants/tenant-context", () => ({
  useCompanies: () => ({
    loading: false,
    companies: [],
    current: null,
    select,
    refresh,
  }),
}))

function renderPage() {
  return render(
    <MemoryRouter initialEntries={["/companies/new"]}>
      <Routes>
        <Route path="/companies/new" element={<CompanyFormPage />} />
        <Route path="/companies" element={<p>companies list</p>} />
      </Routes>
    </MemoryRouter>
  )
}

describe("CompanyFormPage (create)", () => {
  beforeEach(() => vi.clearAllMocks())

  it("rejects an empty name without calling the API", async () => {
    renderPage()
    await userEvent.click(
      screen.getByRole("button", { name: "Create Company" })
    )
    expect(await screen.findByText("Name is required")).toBeInTheDocument()
    expect(create).not.toHaveBeenCalled()
  })

  it("creates the company, selects it and goes back to the list", async () => {
    create.mockResolvedValue({
      data: { createTenant: { tenant: { id: "9", name: "Acme" } } },
    })
    renderPage()
    await userEvent.type(screen.getByLabelText("Name"), "  Acme ")
    await userEvent.click(
      screen.getByRole("button", { name: "Create Company" })
    )
    expect(await screen.findByText("companies list")).toBeInTheDocument()
    expect(create).toHaveBeenCalledWith({ input: { name: "Acme" } })
    expect(select).toHaveBeenCalledWith("9")
    expect(refresh).toHaveBeenCalled()
  })
})
