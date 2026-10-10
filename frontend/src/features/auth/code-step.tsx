import { useState } from "react"

import { Button } from "@/components/ui/button"
import { Field, FieldDescription, FieldGroup } from "@/components/ui/field"
import {
  InputOTP,
  InputOTPGroup,
  InputOTPSlot,
} from "@/components/ui/input-otp"
import { Spinner } from "@/components/ui/spinner"
import { FormError } from "@/features/auth/auth-layout"

const CODE_LENGTH = 6

/** Asks for the code that was emailed to `email`. */
export function CodeStep({
  email,
  pending,
  error,
  onSubmit,
  onBack,
}: {
  email: string
  pending: boolean
  error?: string
  onSubmit: (code: string) => void
  onBack: () => void
}) {
  const [code, setCode] = useState("")

  function submit(value: string) {
    if (value.length === CODE_LENGTH && !pending) onSubmit(value)
  }

  return (
    <form
      onSubmit={(event) => {
        event.preventDefault()
        submit(code)
      }}
    >
      <FieldGroup>
        <Field>
          <InputOTP
            maxLength={CODE_LENGTH}
            value={code}
            onChange={setCode}
            onComplete={submit}
            disabled={pending}
            autoFocus
            aria-label="Verification code"
          >
            <InputOTPGroup className="mx-auto">
              {Array.from({ length: CODE_LENGTH }, (_, index) => (
                <InputOTPSlot key={index} index={index} />
              ))}
            </InputOTPGroup>
          </InputOTP>
          <FieldDescription className="text-center">
            Enter the {CODE_LENGTH}-character code we sent to {email}.
          </FieldDescription>
        </Field>
        <FormError message={error} />
        <Field>
          <Button type="submit" disabled={pending || code.length < CODE_LENGTH}>
            {pending && <Spinner data-icon="inline-start" />}
            Verify
          </Button>
          <Button type="button" variant="ghost" onClick={onBack}>
            Use a different email
          </Button>
        </Field>
      </FieldGroup>
    </form>
  )
}
