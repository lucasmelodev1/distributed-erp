import { render, screen } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { describe, expect, it, vi } from "vitest"

import { CodeStep } from "@/features/auth/code-step"

function setup(props: Partial<React.ComponentProps<typeof CodeStep>> = {}) {
  const onSubmit = vi.fn()
  render(
    <CodeStep
      email="ada@example.com"
      pending={false}
      onSubmit={onSubmit}
      onBack={() => {}}
      {...props}
    />
  )
  return { onSubmit }
}

describe("CodeStep", () => {
  it("names the address the code was sent to", () => {
    setup()
    expect(screen.getByText(/ada@example.com/)).toBeInTheDocument()
  })

  it("keeps Verify disabled until the code is complete", () => {
    setup()
    expect(screen.getByRole("button", { name: "Verify" })).toBeDisabled()
  })

  it("submits the code exactly as typed once all characters are entered", async () => {
    const { onSubmit } = setup()
    await userEvent.type(screen.getByLabelText("Verification code"), "ab12cd")
    expect(onSubmit).toHaveBeenCalledWith("ab12cd")
  })

  it("shows the server's error", () => {
    setup({ error: "Invalid or expired token" })
    expect(screen.getByRole("alert")).toHaveTextContent(
      "Invalid or expired token"
    )
  })
})
