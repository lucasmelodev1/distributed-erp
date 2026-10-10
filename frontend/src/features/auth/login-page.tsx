import { useState } from "react"
import { Link, useLocation, useNavigate } from "react-router"
import { useMutation } from "urql"

import { Button } from "@/components/ui/button"
import {
  Field,
  FieldDescription,
  FieldGroup,
  FieldLabel,
} from "@/components/ui/field"
import { Input } from "@/components/ui/input"
import { Spinner } from "@/components/ui/spinner"
import { AuthLayout, FormError } from "@/features/auth/auth-layout"
import { useSession } from "@/features/auth/session"
import { graphql } from "@/gql"
import { errorMessage } from "@/lib/errors"

const LoginMutation = graphql(`
  mutation Login($input: LoginInput!) {
    login(input: $input) {
      id
    }
  }
`)

export function LoginPage() {
  const [{ fetching, error }, login] = useMutation(LoginMutation)
  const { refresh } = useSession()
  const navigate = useNavigate()
  const location = useLocation()
  const [email, setEmail] = useState("")
  const [password, setPassword] = useState("")

  async function submit(event: React.FormEvent) {
    event.preventDefault()
    const result = await login({ input: { email, password } })
    if (result.error) return
    await refresh()
    const from = (location.state as { from?: { pathname: string } } | null)
      ?.from?.pathname
    void navigate(from ?? "/dashboard", { replace: true })
  }

  return (
    <AuthLayout
      title="Login to your account"
      description="Enter your email and password to continue"
    >
      <form onSubmit={submit}>
        <FieldGroup>
          <Field>
            <FieldLabel htmlFor="email">Email</FieldLabel>
            <Input
              id="email"
              type="email"
              autoComplete="email"
              placeholder="m@example.com"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              required
            />
          </Field>
          <Field>
            <div className="flex items-center">
              <FieldLabel htmlFor="password">Password</FieldLabel>
              <Link
                to="/forgot-password"
                className="ml-auto inline-block text-sm underline-offset-4 hover:underline"
              >
                Forgot your password?
              </Link>
            </div>
            <Input
              id="password"
              type="password"
              autoComplete="current-password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              required
            />
          </Field>
          <FormError message={errorMessage(error)} />
          <Field>
            <Button type="submit" disabled={fetching}>
              {fetching && <Spinner data-icon="inline-start" />}
              Login
            </Button>
            <FieldDescription className="text-center">
              Don&apos;t have an account? <Link to="/register">Sign up</Link>
            </FieldDescription>
          </Field>
        </FieldGroup>
      </form>
    </AuthLayout>
  )
}
