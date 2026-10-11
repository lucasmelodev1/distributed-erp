import { zodResolver } from "@hookform/resolvers/zod"
import { Controller, useForm } from "react-hook-form"
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
import { PermissionTree } from "@/features/roles/permission-tree"
import { graphql } from "@/gql"
import type { Role } from "@/features/roles/roles-query"
import type {
  CreateTenantRoleInput,
  UpdateTenantRoleInput,
} from "@/gql/graphql"
import { errorMessage } from "@/lib/errors"

const PermissionsQuery = graphql(`
  query Permissions {
    permissions {
      code
      description
      level
    }
  }
`)

const CreateTenantRoleMutation = graphql(`
  mutation CreateTenantRole($input: CreateTenantRoleInput!) {
    createTenantRole(input: $input) {
      tenantRole {
        id
        name
      }
    }
  }
`)

const UpdateTenantRoleMutation = graphql(`
  mutation UpdateTenantRole($input: UpdateTenantRoleInput!) {
    updateTenantRole(input: $input) {
      tenantRole {
        id
        name
      }
    }
  }
`)

const roleSchema = z.object({
  name: z.string().trim().min(1, "Name is required"),
  permissionCodes: z.array(z.string()).min(1, "Select at least one permission"),
})
roleSchema satisfies z.ZodType<CreateTenantRoleInput>
roleSchema satisfies z.ZodType<Omit<UpdateTenantRoleInput, "id">>

type RoleForm = z.infer<typeof roleSchema>

const EMPTY: RoleForm = { name: "", permissionCodes: [] }

type RoleDrawerProps = {
  /** The role to edit; without one the drawer creates a role. */
  role?: Role
  open: boolean
  onOpenChange: (open: boolean) => void
  /** Called after the role was saved, e.g. to reload the list. */
  onSaved: () => void
}

/** Side drawer to create a role, or edit one, from a name and a set of permissions. */
export function RoleDrawer({
  role,
  open,
  onOpenChange,
  onSaved,
}: RoleDrawerProps) {
  const editing = role !== undefined
  const [{ data, fetching: loading, error: permissionsError }] = useQuery({
    query: PermissionsQuery,
    pause: !open,
    requestPolicy: "cache-and-network",
  })
  const [creation, create] = useMutation(CreateTenantRoleMutation)
  const [update, save] = useMutation(UpdateTenantRoleMutation)

  const initial: RoleForm = role
    ? { name: role.name, permissionCodes: role.permissions.map((p) => p.code) }
    : EMPTY

  const form = useForm<RoleForm>({
    resolver: zodResolver(roleSchema),
    defaultValues: initial,
  })
  // Read during render: react-hook-form only tracks the form state that render subscribed to.
  const { isDirty, isSubmitted } = form.formState

  return (
    <FormDrawer
      open={open}
      onOpenChange={onOpenChange}
      title={editing ? "Edit role" : "Create role"}
      description="A role is a named set of permissions you can give to people."
      formId="role-form"
      submitLabel={editing ? "Save" : "Create role"}
      pending={(editing ? update : creation).fetching}
      dirty={isDirty}
      onClosed={() => form.reset(initial)}
    >
      {({ close }) => (
        <form
          id="role-form"
          noValidate
          onSubmit={form.handleSubmit(async (values) => {
            const saved = editing
              ? (await save({ input: { id: role.id, ...values } })).data
                  ?.updateTenantRole.tenantRole
              : (await create({ input: values })).data?.createTenantRole
                  .tenantRole
            if (!saved) return
            toast.add({
              title: editing ? "Role updated" : "Role created",
              description: `${saved.name} was saved.`,
              type: "success",
            })
            onSaved()
            close()
          })}
        >
          <FieldGroup>
            <Controller
              name="name"
              control={form.control}
              render={({ field, fieldState }) => (
                <Field data-invalid={fieldState.invalid}>
                  <FieldLabel htmlFor="role-name">Name</FieldLabel>
                  <Input
                    {...field}
                    id="role-name"
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
              name="permissionCodes"
              control={form.control}
              render={({ field, fieldState }) => (
                <FieldSet data-invalid={fieldState.invalid}>
                  <FieldLegend variant="label">Permissions</FieldLegend>
                  <FieldDescription>
                    Ticking an area ticks everything in it.
                  </FieldDescription>
                  {loading && !data ? (
                    <div className="flex flex-col gap-2">
                      <Skeleton className="h-5 w-full" />
                      <Skeleton className="h-5 w-full" />
                      <Skeleton className="h-5 w-full" />
                    </div>
                  ) : (
                    <PermissionTree
                      permissions={data?.permissions ?? []}
                      value={field.value}
                      onChange={(codes) =>
                        form.setValue("permissionCodes", codes, {
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
            <FormError
              message={errorMessage(
                (editing ? update : creation).error ?? permissionsError
              )}
            />
          </FieldGroup>
        </form>
      )}
    </FormDrawer>
  )
}
