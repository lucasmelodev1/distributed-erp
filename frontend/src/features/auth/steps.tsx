import { useState } from "react"

import { Button } from "@/components/ui/button"
import { Field, FieldGroup, FieldLabel } from "@/components/ui/field"
import { Input } from "@/components/ui/input"
import { Spinner } from "@/components/ui/spinner"
import { FormError } from "@/features/auth/auth-layout"

export function EmailStep({
  submitLabel,
  pending,
  error,
  onSubmit,
}: {
  submitLabel: string
  pending: boolean
  error?: string
  onSubmit: (email: string) => void
}) {
  const [email, setEmail] = useState("")
  return (
    <form
      onSubmit={(event) => {
        event.preventDefault()
        onSubmit(email)
      }}
    >
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
        <FormError message={error} />
        <Field>
          <Button type="submit" disabled={pending}>
            {pending && <Spinner data-icon="inline-start" />}
            {submitLabel}
          </Button>
        </Field>
      </FieldGroup>
    </form>
  )
}

/** Final step: a new password, and optionally the user's name (registration). */
export function PasswordStep({
  withName,
  submitLabel,
  pending,
  error,
  onSubmit,
}: {
  withName?: boolean
  submitLabel: string
  pending: boolean
  error?: string
  onSubmit: (values: { name: string; password: string }) => void
}) {
  const [name, setName] = useState("")
  const [password, setPassword] = useState("")
  return (
    <form
      onSubmit={(event) => {
        event.preventDefault()
        onSubmit({ name, password })
      }}
    >
      <FieldGroup>
        {withName && (
          <Field>
            <FieldLabel htmlFor="name">Name</FieldLabel>
            <Input
              id="name"
              autoComplete="name"
              value={name}
              onChange={(event) => setName(event.target.value)}
              required
            />
          </Field>
        )}
        <Field>
          <FieldLabel htmlFor="password">Password</FieldLabel>
          <Input
            id="password"
            type="password"
            autoComplete="new-password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            required
          />
        </Field>
        <FormError message={error} />
        <Field>
          <Button type="submit" disabled={pending}>
            {pending && <Spinner data-icon="inline-start" />}
            {submitLabel}
          </Button>
        </Field>
      </FieldGroup>
    </form>
  )
}
