import { zodResolver } from "@hookform/resolvers/zod"
import { Controller, useForm } from "react-hook-form"
import { useNavigate, useParams } from "react-router"
import { useMutation } from "urql"
import { z } from "zod"

import { Button } from "@/components/ui/button"
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card"
import {
  Field,
  FieldError,
  FieldGroup,
  FieldLabel,
} from "@/components/ui/field"
import { Input } from "@/components/ui/input"
import { Spinner } from "@/components/ui/spinner"
import { FormError } from "@/features/auth/auth-layout"
import { useCompanies } from "@/features/tenants/tenant-context"
import { graphql } from "@/gql"
import type { CreateTenantInput, UpdateTenantInput } from "@/gql/graphql"
import { errorMessage } from "@/lib/errors"
import { tenantContext } from "@/lib/tenant"

const CreateTenantMutation = graphql(`
  mutation CreateTenant($input: CreateTenantInput!) {
    createTenant(input: $input) {
      tenant {
        id
        name
        active
        inactivatedAt
      }
    }
  }
`)

const UpdateTenantMutation = graphql(`
  mutation UpdateTenant($input: UpdateTenantInput!) {
    updateTenant(input: $input) {
      tenant {
        id
        name
        active
        inactivatedAt
      }
    }
  }
`)

// Both inputs are `{ name: String! }`; `satisfies` fails to compile if the schema drifts.
const nameSchema = z.object({
  name: z.string().trim().min(1, "Name is required"),
})
nameSchema satisfies z.ZodType<CreateTenantInput>
nameSchema satisfies z.ZodType<UpdateTenantInput>

/** Creates a company, or renames the one in `/companies/:id/edit`. */
export function CompanyFormPage() {
  const { id } = useParams()
  const editing = id !== undefined
  const navigate = useNavigate()
  const { companies, loading, select, refresh } = useCompanies()
  const company = companies.find((candidate) => candidate.id === id)
  const [creation, create] = useMutation(CreateTenantMutation)
  const [update, rename] = useMutation(UpdateTenantMutation)

  const form = useForm<CreateTenantInput>({
    resolver: zodResolver(nameSchema),
    values: { name: company?.name ?? "" },
  })

  async function submit(input: CreateTenantInput) {
    if (editing) {
      const result = await rename({ input }, tenantContext(id))
      if (result.error) return
      await refresh()
      void navigate("/companies")
      return
    }
    const result = await create({ input })
    const created = result.data?.createTenant.tenant
    if (!created) return
    select(created.id)
    await refresh()
    void navigate("/companies")
  }

  const { fetching, error } = editing ? update : creation

  if (editing && !loading && !company) {
    return <p className="text-sm text-muted-foreground">Company not found.</p>
  }

  return (
    <div className="mx-auto w-full max-w-lg">
      <Card>
        <CardHeader>
          <CardTitle>{editing ? "Edit company" : "Create company"}</CardTitle>
          <CardDescription>
            {editing
              ? "Rename this company."
              : "You become its first member, with every permission."}
          </CardDescription>
        </CardHeader>
        <CardContent>
          <form onSubmit={form.handleSubmit(submit)} noValidate>
            <FieldGroup>
              <Controller
                name="name"
                control={form.control}
                render={({ field, fieldState }) => (
                  <Field data-invalid={fieldState.invalid}>
                    <FieldLabel htmlFor="name">Name</FieldLabel>
                    <Input
                      {...field}
                      id="name"
                      autoComplete="organization"
                      aria-invalid={fieldState.invalid}
                    />
                    {fieldState.invalid && (
                      <FieldError errors={[fieldState.error]} />
                    )}
                  </Field>
                )}
              />
              <FormError message={errorMessage(error)} />
              <Field orientation="horizontal">
                <Button type="submit" disabled={fetching}>
                  {fetching && <Spinner data-icon="inline-start" />}
                  {editing ? "Save" : "Create Company"}
                </Button>
                <Button
                  type="button"
                  variant="outline"
                  onClick={() => void navigate("/companies")}
                >
                  Cancel
                </Button>
              </Field>
            </FieldGroup>
          </form>
        </CardContent>
      </Card>
    </div>
  )
}
