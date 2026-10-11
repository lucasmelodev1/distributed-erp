import { render, screen } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { MemoryRouter, Route, Routes } from "react-router"
import { beforeEach, describe, expect, it, vi } from "vitest"

import { AcceptInvitationPage } from "@/features/invitations/accept-invitation-page"
import { getTenantId } from "@/lib/tenant"

const useQuery = vi.fn()
const register = vi.fn()
const accept = vi.fn()
const login = vi.fn()
const refresh = vi.fn().mockResolvedValue(undefined)
const logout = vi.fn().mockResolvedValue(undefined)
let session: { loading: boolean; user: { id: string } | null }

vi.mock("urql", async (importOriginal) => ({
  ...(await importOriginal<typeof import("urql")>()),
  useQuery: (args: unknown) => useQuery(args),
  useMutation: (document: { definitions: { name?: { value: string } }[] }) => {
    const name = document.definitions[0]?.name?.value
    const run =
      name === "RegisterUserFromInvitation"
        ? register
        : name === "AcceptTenantInvitation"
          ? accept
          : login
    return [{ fetching: false, error: undefined }, run]
  },
}))
vi.mock("@/features/auth/session", () => ({
  useSession: () => ({ ...session, refresh, logout }),
}))

function check(nextAction: string) {
  useQuery.mockReturnValue([
    {
      fetching: false,
      data: {
        checkInvitedUser: {
          nextAction,
          email: "new@example.com",
          tenantName: "Acme",
        },
      },
    },
  ])
}

function renderPage(url = "/accept-invitation?code=abc") {
  return render(
    <MemoryRouter initialEntries={[url]}>
      <Routes>
        <Route path="/accept-invitation" element={<AcceptInvitationPage />} />
        <Route path="/dashboard" element={<p>dashboard</p>} />
      </Routes>
    </MemoryRouter>
  )
}

describe("AcceptInvitationPage", () => {
  beforeEach(() => {
    vi.clearAllMocks()
    localStorage.clear()
    session = { loading: false, user: null }
  })

  it("asks the server about the code from the link", () => {
    check("CREATE_ACCOUNT")
    renderPage()

    expect(useQuery.mock.lastCall![0].variables).toEqual({ code: "abc" })
  })

  it("never calls the API without a code", () => {
    renderPage("/accept-invitation")

    expect(screen.getByText("Invitation not valid")).toBeInTheDocument()
    expect(useQuery).not.toHaveBeenCalled()
  })

  it("explains that an unknown code is not valid", () => {
    useQuery.mockReturnValue([
      {
        fetching: false,
        error: { graphQLErrors: [{ extensions: { code: "NOT_FOUND" } }] },
      },
    ])
    renderPage()

    expect(screen.getByText("Invitation not valid")).toBeInTheDocument()
  })

  it("waits for the session before checking", () => {
    session = { loading: true, user: null }
    renderPage()

    expect(useQuery).not.toHaveBeenCalled()
  })

  it("creates the account from the invitation and enters the app", async () => {
    check("CREATE_ACCOUNT")
    register.mockResolvedValue({
      data: { registerUserFromInvitation: { id: "1" } },
    })
    renderPage()

    expect(
      screen.getByText("Create your account for new@example.com")
    ).toBeInTheDocument()
    await userEvent.type(screen.getByLabelText("Name"), "New Person")
    await userEvent.type(screen.getByLabelText("Password"), "secret-password")
    await userEvent.click(
      screen.getByRole("button", { name: "Create account and join" })
    )

    expect(register).toHaveBeenCalledWith({
      input: { code: "abc", name: "New Person", password: "secret-password" },
    })
    expect(await screen.findByText("dashboard")).toBeInTheDocument()
    expect(refresh).toHaveBeenCalled()
  })

  it("logs an existing account in with the invited email", async () => {
    check("LOGIN")
    login.mockResolvedValue({ data: { login: { id: "1" } } })
    renderPage()

    await userEvent.type(screen.getByLabelText("Password"), "secret-password")
    await userEvent.click(screen.getByRole("button", { name: "Login" }))

    expect(login).toHaveBeenCalledWith({
      input: { email: "new@example.com", password: "secret-password" },
    })
    expect(refresh).toHaveBeenCalled()
  })

  it("accepts for a signed-in invitee and selects the company", async () => {
    session = { loading: false, user: { id: "u" } }
    check("ACCEPT")
    accept.mockResolvedValue({
      data: { acceptTenantInvitation: { tenant: { id: "t-1", name: "Acme" } } },
    })
    renderPage()

    await userEvent.click(
      screen.getByRole("button", { name: "Accept invitation" })
    )

    expect(accept).toHaveBeenCalledWith({ input: { code: "abc" } })
    expect(await screen.findByText("dashboard")).toBeInTheDocument()
    expect(getTenantId()).toBe("t-1")
  })

  it("lets someone signed in as another account log out", async () => {
    session = { loading: false, user: { id: "other" } }
    check("WRONG_ACCOUNT")
    renderPage()

    await userEvent.click(
      screen.getByRole("button", { name: "Log out and continue" })
    )

    expect(logout).toHaveBeenCalled()
    expect(accept).not.toHaveBeenCalled()
  })
})
