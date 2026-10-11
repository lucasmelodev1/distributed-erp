import { useState } from "react"
import { Link, useNavigate, useSearchParams } from "react-router"
import { useMutation, useQuery } from "urql"

import { Button } from "@/components/ui/button"
import { Field, FieldGroup, FieldLabel } from "@/components/ui/field"
import { Input } from "@/components/ui/input"
import { Spinner } from "@/components/ui/spinner"
import { toast } from "@/components/ui/toast"
import { AuthLayout, FormError } from "@/features/auth/auth-layout"
import { useSession } from "@/features/auth/session"
import { PasswordStep } from "@/features/auth/steps"
import { graphql } from "@/gql"
import { errorCode, errorMessage } from "@/lib/errors"
import { setTenantId } from "@/lib/tenant"

const CheckInvitedUserQuery = graphql(`
  query CheckInvitedUser($code: String!) {
    checkInvitedUser(code: $code) {
      nextAction
      email
      tenantName
    }
  }
`)

const AcceptTenantInvitationMutation = graphql(`
  mutation AcceptTenantInvitation($input: AcceptTenantInvitationInput!) {
    acceptTenantInvitation(input: $input) {
      tenant {
        id
        name
      }
    }
  }
`)

const RegisterUserFromInvitationMutation = graphql(`
  mutation RegisterUserFromInvitation(
    $input: RegisterUserFromInvitationInput!
  ) {
    registerUserFromInvitation(input: $input) {
      id
    }
  }
`)

const InvitationLoginMutation = graphql(`
  mutation InvitationLogin($input: LoginInput!) {
    login(input: $input) {
      id
    }
  }
`)

/** The page behind the link in the invitation email: `/accept-invitation?code=...`. */
export function AcceptInvitationPage() {
  const code = useSearchParams()[0].get("code")?.trim()
  const { loading, user } = useSession()

  if (loading) return <Message title="Invitation" busy />
  if (!code) return <InvalidInvitation />
  // Remount when the session changes (after login or logout) so the check is asked again.
  return <Invitation key={user?.id ?? "anonymous"} code={code} />
}

function Invitation({ code }: { code: string }) {
  const [{ data, error, fetching }] = useQuery({
    query: CheckInvitedUserQuery,
    variables: { code },
    requestPolicy: "network-only",
  })

  if (fetching && !data) return <Message title="Invitation" busy />
  if (errorCode(error) === "NOT_FOUND") return <InvalidInvitation />
  if (!data) {
    return (
      <AuthLayout title="Invitation" description="Something went wrong">
        <FormError message={errorMessage(error)} />
      </AuthLayout>
    )
  }

  const { nextAction, email, tenantName } = data.checkInvitedUser
  switch (nextAction) {
    case "CREATE_ACCOUNT":
      return <CreateAccount code={code} email={email} tenantName={tenantName} />
    case "LOGIN":
      return <Login email={email} tenantName={tenantName} />
    case "ACCEPT":
      return <Accept code={code} email={email} tenantName={tenantName} />
    case "WRONG_ACCOUNT":
      return <WrongAccount email={email} />
  }
}

function Message({
  title,
  description = "One moment",
  busy,
  children,
}: {
  title: string
  description?: string
  busy?: boolean
  children?: React.ReactNode
}) {
  return (
    <AuthLayout title={title} description={description}>
      {busy && <Spinner />}
      {children}
    </AuthLayout>
  )
}

function InvalidInvitation() {
  return (
    <Message
      title="Invitation not valid"
      description="This invitation link is invalid, expired or was already used. Ask for a new one."
    >
      <Button variant="outline" render={<Link to="/login" />}>
        Go to login
      </Button>
    </Message>
  )
}

type InvitationProps = { email: string; tenantName: string }

function CreateAccount({
  code,
  email,
  tenantName,
}: InvitationProps & { code: string }) {
  const [{ fetching, error }, register] = useMutation(
    RegisterUserFromInvitationMutation
  )
  const { refresh } = useSession()
  const navigate = useNavigate()

  return (
    <AuthLayout
      title={`Join ${tenantName}`}
      description={`Create your account for ${email}`}
    >
      <PasswordStep
        withName
        submitLabel="Create account and join"
        pending={fetching}
        error={errorMessage(error)}
        onSubmit={async ({ name, password }) => {
          const result = await register({ input: { code, name, password } })
          if (result.error) return
          // A new account has only this company, which the dashboard then selects.
          setTenantId(null)
          await refresh()
          toast.add({
            title: `Welcome to ${tenantName}`,
            description: "Your account was created.",
            type: "success",
          })
          void navigate("/dashboard", { replace: true })
        }}
      />
    </AuthLayout>
  )
}

function Login({ email, tenantName }: InvitationProps) {
  const [{ fetching, error }, login] = useMutation(InvitationLoginMutation)
  const { refresh } = useSession()
  const [password, setPassword] = useState("")

  return (
    <AuthLayout
      title={`Join ${tenantName}`}
      description={`Log in as ${email} to accept the invitation`}
    >
      <form
        onSubmit={async (event) => {
          event.preventDefault()
          const result = await login({ input: { email, password } })
          if (!result.error) await refresh()
        }}
      >
        <FieldGroup>
          <Field>
            <FieldLabel htmlFor="password">Password</FieldLabel>
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
          </Field>
        </FieldGroup>
      </form>
    </AuthLayout>
  )
}

function Accept({
  code,
  email,
  tenantName,
}: InvitationProps & { code: string }) {
  const [{ fetching, error }, accept] = useMutation(
    AcceptTenantInvitationMutation
  )
  const navigate = useNavigate()

  async function submit() {
    const result = await accept({ input: { code } })
    const tenant = result.data?.acceptTenantInvitation.tenant
    if (!tenant) return
    setTenantId(tenant.id)
    toast.add({
      title: `You joined ${tenant.name}`,
      type: "success",
    })
    void navigate("/dashboard", { replace: true })
  }

  return (
    <AuthLayout
      title={`Join ${tenantName}`}
      description={`You are invited to join as ${email}`}
    >
      <FieldGroup>
        <FormError message={errorMessage(error)} />
        <Field>
          <Button onClick={() => void submit()} disabled={fetching}>
            {fetching && <Spinner data-icon="inline-start" />}
            Accept invitation
          </Button>
        </Field>
      </FieldGroup>
    </AuthLayout>
  )
}

function WrongAccount({ email }: { email: string }) {
  const { logout } = useSession()
  return (
    <AuthLayout
      title="Wrong account"
      description={`This invitation was sent to ${email}, but you are signed in as someone else.`}
    >
      <FieldGroup>
        <Field>
          <Button onClick={() => void logout()}>Log out and continue</Button>
          <Button variant="outline" render={<Link to="/dashboard" />}>
            Go to the dashboard
          </Button>
        </Field>
      </FieldGroup>
    </AuthLayout>
  )
}
