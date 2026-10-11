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
import type { UpdateTenantUserInput } from "@/gql/graphql"
import { errorMessage } from "@/lib/errors"

const UpdateTenantUserMutation = graphql(`
  mutation UpdateTenantUser($input: UpdateTenantUserInput!) {
    updateTenantUser(input: $input) {
      tenantUser {
        roles {
          id
          name
        }
      }
    }
  }
`)

const manageSchema = z.object({
  tenantRoleIds: z.array(z.string()).min(1, "Select at least one role"),
})
manageSchema satisfies z.ZodType<Omit<UpdateTenantUserInput, "userId">>

type ManageForm = z.infer<typeof manageSchema>

export type ManagedMember = {
  userId: string
  name: string
  email: string
  roleIds: string[]
}

type ManageUserDrawerProps = {
  member: ManagedMember
  open: boolean
  onOpenChange: (open: boolean) => void
  /** Called after the roles were saved, e.g. to reload the list. */
  onSaved: () => void
}

/** Side drawer to change the roles of a member; their name and email are shown but not editable. */
export function ManageUserDrawer({
  member,
  open,
  onOpenChange,
  onSaved,
}: ManageUserDrawerProps) {
  // All roles, so the ones the member holds are shown even if inactive (they can be removed, not added).
  const [{ data, fetching: loadingRoles, error: rolesError }] = useQuery({
    query: TenantRolesQuery,
    variables: { first: ROLES_LIMIT },
    pause: !open,
    requestPolicy: "cache-and-network",
  })
  const [update, save] = useMutation(UpdateTenantUserMutation)

  const initial: ManageForm = { tenantRoleIds: member.roleIds }
  const form = useForm<ManageForm>({
    resolver: zodResolver(manageSchema),
    defaultValues: initial,
  })
  // Read during render: react-hook-form only tracks the form state that render subscribed to.
  const { isDirty, isSubmitted } = form.formState

  const roles = data?.tenantRoles.edges.map((edge) => edge.node) ?? []
  const chosen = useWatch({ control: form.control, name: "tenantRoleIds" })

  return (
    <FormDrawer
      open={open}
      onOpenChange={onOpenChange}
      title="Manage user"
      description="Choose the roles this person has in the selected company."
      formId="manage-user-form"
      submitLabel="Save"
      pending={update.fetching}
      dirty={isDirty}
      onClosed={() => form.reset(initial)}
    >
      {({ close }) => (
        <form
          id="manage-user-form"
          noValidate
          onSubmit={form.handleSubmit(async (values) => {
            const result = await save({
              input: { userId: member.userId, ...values },
            })
            if (!result.data) return
            toast.add({
              title: "User updated",
              description: `${member.name}'s roles were saved.`,
              type: "success",
            })
            onSaved()
            close()
          })}
        >
          <FieldGroup>
            <Field>
              <FieldLabel htmlFor="manage-name">Name</FieldLabel>
              <Input id="manage-name" value={member.name} readOnly />
            </Field>
            <Field>
              <FieldLabel htmlFor="manage-email">Email</FieldLabel>
              <Input id="manage-email" value={member.email} readOnly />
            </Field>
            <Controller
              name="tenantRoleIds"
              control={form.control}
              render={({ field, fieldState }) => (
                <FieldSet data-invalid={fieldState.invalid}>
                  <FieldLegend variant="label">Roles</FieldLegend>
                  <FieldDescription>
                    Inactive roles can be removed but not added.
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
            <FormError message={errorMessage(update.error ?? rolesError)} />
          </FieldGroup>
        </form>
      )}
    </FormDrawer>
  )
}
