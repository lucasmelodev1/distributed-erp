import { zodResolver } from "@hookform/resolvers/zod"
import { Controller, useForm, useWatch } from "react-hook-form"
import { useMutation, useQuery } from "urql"
import { z } from "zod"

import { FormDrawer } from "@/components/form-drawer"
import {
  Field,
  FieldDescription,
  FieldError,
  FieldGroup,
  FieldLabel,
  FieldLegend,
  FieldSet,
} from "@/components/ui/field"
import { Input } from "@/components/ui/input"
import { Skeleton } from "@/components/ui/skeleton"
import { toast } from "@/components/ui/toast"
import { FormError } from "@/features/auth/auth-layout"
import { PermissionSummary } from "@/features/roles/permission-summary"
import { RolePicker } from "@/features/roles/role-picker"
import { ROLES_LIMIT, TenantRolesQuery } from "@/features/roles/roles-query"
import { graphql } from "@/gql"
import type { InviteUserToTenantInput } from "@/gql/graphql"
import { errorMessage } from "@/lib/errors"

const InviteUserToTenantMutation = graphql(`
  mutation InviteUserToTenant($input: InviteUserToTenantInput!) {
    inviteUserToTenant(input: $input) {
      invitation {
        id
        email
        status
      }
    }
  }
`)

const inviteSchema = z.object({
  email: z
    .string()
    .trim()
    .min(1, "Email is required")
    .email("Enter a valid email"),
  tenantRoleIds: z.array(z.string()).min(1, "Select at least one role"),
})
inviteSchema satisfies z.ZodType<InviteUserToTenantInput>

type InviteForm = z.infer<typeof inviteSchema>

const EMPTY: InviteForm = { email: "", tenantRoleIds: [] }

type InviteDrawerProps = {
  open: boolean
  onOpenChange: (open: boolean) => void
  /** Called after an invitation was sent, e.g. to reload the list. */
  onInvited: () => void
}

/** Side drawer to invite someone with one or more roles; asks before throwing away what was typed. */
export function InviteDrawer({
  open,
  onOpenChange,
  onInvited,
}: InviteDrawerProps) {
  // Inactive roles are not offered: the server refuses them for new invitations too.
  const [{ data, fetching: loadingRoles, error: rolesError }] = useQuery({
    query: TenantRolesQuery,
    variables: { first: ROLES_LIMIT, active: true },
    pause: !open,
    requestPolicy: "cache-and-network",
  })
  const [invitation, invite] = useMutation(InviteUserToTenantMutation)

  const form = useForm<InviteForm>({
    resolver: zodResolver(inviteSchema),
    defaultValues: EMPTY,
  })
  // Read during render: react-hook-form only tracks the form state that render subscribed to.
  const { isDirty, isSubmitted } = form.formState

  const roles = data?.tenantRoles.edges.map((edge) => edge.node) ?? []
  const chosen = useWatch({ control: form.control, name: "tenantRoleIds" })

  return (
    <FormDrawer
      open={open}
      onOpenChange={onOpenChange}
      title="Send invitation"
      description="They get an email with a link to join this company with the roles you pick."
      formId="invite-form"
      submitLabel="Send invitation"
      pending={invitation.fetching}
      dirty={isDirty}
      onClosed={() => form.reset(EMPTY)}
    >
      {({ close }) => (
        <form
          id="invite-form"
          noValidate
          onSubmit={form.handleSubmit(async (values) => {
            const result = await invite({ input: values })
            const sent = result.data?.inviteUserToTenant.invitation
            if (!sent) return
            toast.add({
              title: "Invitation sent",
              description: `An email was sent to ${sent.email}.`,
              type: "success",
            })
            onInvited()
            close()
          })}
        >
          <FieldGroup>
            <Controller
              name="email"
              control={form.control}
              render={({ field, fieldState }) => (
                <Field data-invalid={fieldState.invalid}>
                  <FieldLabel htmlFor="invite-email">Email</FieldLabel>
                  <Input
                    {...field}
                    id="invite-email"
                    type="email"
                    autoComplete="off"
                    aria-invalid={fieldState.invalid}
                  />
                  {fieldState.invalid && (
                    <FieldError errors={[fieldState.error]} />
                  )}
                </Field>
              )}
            />
            <Controller
              name="tenantRoleIds"
              control={form.control}
              render={({ field, fieldState }) => (
                <FieldSet data-invalid={fieldState.invalid}>
                  <FieldLegend variant="label">Roles</FieldLegend>
                  <FieldDescription>
                    You can only give roles whose permissions you have yourself.
                  </FieldDescription>
                  {loadingRoles && !data ? (
                    <Skeleton className="h-5 w-full" />
                  ) : (
                    <RolePicker
                      roles={roles}
                      value={field.value}
                      onChange={(ids) =>
                        form.setValue("tenantRoleIds", ids, {
                          shouldDirty: true,
                          shouldValidate: isSubmitted,
                        })
                      }
                    />
                  )}
                  {fieldState.invalid && (
                    <FieldError errors={[fieldState.error]} />
                  )}
                </FieldSet>
              )}
            />
            <FieldSet>
              <FieldLegend variant="label">Permissions</FieldLegend>
              <FieldDescription>
                Everything the selected roles allow together.
              </FieldDescription>
              <PermissionSummary
                roles={roles.filter((role) => chosen.includes(role.id))}
              />
            </FieldSet>
            <FormError message={errorMessage(invitation.error ?? rolesError)} />
          </FieldGroup>
        </form>
      )}
    </FormDrawer>
  )
}
