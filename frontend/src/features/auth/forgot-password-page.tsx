import { useState } from "react"
import { Link, useNavigate } from "react-router"
import { useMutation } from "urql"

import { FieldDescription } from "@/components/ui/field"
import { AuthLayout } from "@/features/auth/auth-layout"
import { CodeStep } from "@/features/auth/code-step"
import { EmailStep, PasswordStep } from "@/features/auth/steps"
import { graphql } from "@/gql"
import { errorMessage } from "@/lib/errors"

const RequestPasswordResetMutation = graphql(`
  mutation RequestPasswordReset($input: RequestPasswordResetInput!) {
    requestPasswordReset(input: $input) {
      authTokenId
    }
  }
`)

const VerifyPasswordResetMutation = graphql(`
  mutation VerifyPasswordReset($input: VerifyPasswordResetInput!) {
    verifyPasswordReset(input: $input) {
      token
    }
  }
`)

const ResetPasswordMutation = graphql(`
  mutation ResetPassword($input: ResetPasswordInput!) {
    resetPassword(input: $input) {
      success
    }
  }
`)

type Step =
  | { name: "email" }
  | { name: "code"; email: string }
  | { name: "password"; email: string; token: string }

const DESCRIPTIONS = {
  email: "Enter your email and we'll send you a reset code",
  code: "Check your inbox",
  password: "Choose a new password",
}

/** Password reset: email, then the emailed code, then the new password. */
export function ForgotPasswordPage() {
  const [step, setStep] = useState<Step>({ name: "email" })
  const [request, requestReset] = useMutation(RequestPasswordResetMutation)
  const [verify, verifyReset] = useMutation(VerifyPasswordResetMutation)
  const [reset, resetPassword] = useMutation(ResetPasswordMutation)
  const navigate = useNavigate()

  return (
    <AuthLayout
      title="Reset your password"
      description={DESCRIPTIONS[step.name]}
    >
      {step.name === "email" && (
        <EmailStep
          submitLabel="Send code"
          pending={request.fetching}
          error={errorMessage(request.error)}
          onSubmit={async (email) => {
            // The API never reveals whether the account exists, so neither do we.
            const result = await requestReset({ input: { email } })
            if (!result.error) setStep({ name: "code", email })
          }}
        />
      )}
      {step.name === "code" && (
        <CodeStep
          email={step.email}
          pending={verify.fetching}
          error={errorMessage(verify.error)}
          onSubmit={async (token) => {
            const result = await verifyReset({ input: { token } })
            if (result.data) {
              setStep({
                name: "password",
                email: step.email,
                token: result.data.verifyPasswordReset.token,
              })
            }
          }}
          onBack={() => setStep({ name: "email" })}
        />
      )}
      {step.name === "password" && (
        <PasswordStep
          submitLabel="Reset password"
          pending={reset.fetching}
          error={errorMessage(reset.error)}
          onSubmit={async ({ password }) => {
            const result = await resetPassword({
              input: { email: step.email, token: step.token, password },
            })
            if (!result.error) void navigate("/login", { replace: true })
          }}
        />
      )}
      <FieldDescription className="mt-6 text-center">
        Remembered it? <Link to="/login">Back to login</Link>
      </FieldDescription>
    </AuthLayout>
  )
}
