import { render, screen } from "@testing-library/react"
import { MemoryRouter, Route, Routes } from "react-router"
import { type Client, Provider } from "urql"
import { describe, expect, it } from "vitest"

import { PublicOnly, RequireAuth } from "@/features/auth/guards"
import { SessionProvider } from "@/features/auth/session"

function clientWithMe(me: unknown): Client {
  return {
    query: () => ({ toPromise: async () => ({ data: { me } }) }),
  } as unknown as Client
}

function renderAt(path: string, me: unknown) {
  return render(
    <Provider value={clientWithMe(me)}>
      <SessionProvider>
        <MemoryRouter initialEntries={[path]}>
          <Routes>
            <Route element={<PublicOnly />}>
              <Route path="/login" element={<p>login page</p>} />
            </Route>
            <Route element={<RequireAuth />}>
              <Route path="/dashboard" element={<p>dashboard page</p>} />
            </Route>
          </Routes>
        </MemoryRouter>
      </SessionProvider>
    </Provider>
  )
}

const user = { id: "1", name: "Ada Lovelace", email: "ada@example.com" }

describe("route guards", () => {
  it("sends an anonymous visitor from the dashboard to login", async () => {
    renderAt("/dashboard", null)
    expect(await screen.findByText("login page")).toBeInTheDocument()
  })

  it("lets a signed-in user reach the dashboard", async () => {
    renderAt("/dashboard", user)
    expect(await screen.findByText("dashboard page")).toBeInTheDocument()
  })

  it("sends a signed-in user away from login", async () => {
    renderAt("/login", user)
    expect(await screen.findByText("dashboard page")).toBeInTheDocument()
  })
})
