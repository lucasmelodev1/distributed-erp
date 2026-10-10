import { useState } from "react"
import { Link, useNavigate } from "react-router"
import { useMutation } from "urql"

import { FieldDescription } from "@/components/ui/field"
import { AuthLayout } from "@/features/auth/auth-layout"
import { CodeStep } from "@/features/auth/code-step"
import { useSession } from "@/features/auth/session"
import { EmailStep, PasswordStep } from "@/features/auth/steps"
import { graphql } from "@/gql"
import { errorMessage } from "@/lib/errors"

const RequestEmailVerificationMutation = graphql(`
  mutation RequestEmailVerification($input: RequestEmailVerificationInput!) {
    requestEmailVerification(input: $input) {
      authTokenId
    }
  }
`)

const VerifyEmailMutation = graphql(`
  mutation VerifyEmail($input: VerifyEmailInput!) {
    verifyEmail(input: $input) {
      token
    }
  }
`)

const RegisterUserMutation = graphql(`
  mutation RegisterUser($input: RegisterUserInput!) {
    registerUser(input: $input) {
      id
    }
  }
`)

type Step =
  | { name: "email" }
  | { name: "code"; email: string }
  | { name: "account"; token: string }

const DESCRIPTIONS = {
  email: "Enter your email and we'll send you a verification code",
  code: "Check your inbox",
  account: "Almost done. Choose your name and password",
}

/** Registration: email, then the emailed code, then name and password. */
export function RegisterPage() {
  const [step, setStep] = useState<Step>({ name: "email" })
  const [request, requestCode] = useMutation(RequestEmailVerificationMutation)
  const [verify, verifyEmail] = useMutation(VerifyEmailMutation)
  const [register, registerUser] = useMutation(RegisterUserMutation)
  const { refresh } = useSession()
  const navigate = useNavigate()

  return (
    <AuthLayout title="Create an account" description={DESCRIPTIONS[step.name]}>
      {step.name === "email" && (
        <EmailStep
          submitLabel="Send code"
          pending={request.fetching}
          error={errorMessage(request.error)}
          onSubmit={async (email) => {
            const result = await requestCode({ input: { email } })
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
            const result = await verifyEmail({ input: { token } })
            if (result.data) {
              setStep({ name: "account", token: result.data.verifyEmail.token })
            }
          }}
          onBack={() => setStep({ name: "email" })}
        />
      )}
      {step.name === "account" && (
        <PasswordStep
          withName
          submitLabel="Create account"
          pending={register.fetching}
          error={errorMessage(register.error)}
          onSubmit={async ({ name, password }) => {
            const result = await registerUser({
              input: { name, password, token: step.token },
            })
            if (result.error) return
            await refresh()
            void navigate("/dashboard", { replace: true })
          }}
        />
      )}
      <FieldDescription className="mt-6 text-center">
        Already have an account? <Link to="/login">Login</Link>
      </FieldDescription>
    </AuthLayout>
  )
}
