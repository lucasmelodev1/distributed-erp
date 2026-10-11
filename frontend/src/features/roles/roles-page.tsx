import { PlusIcon, ShieldIcon } from "lucide-react"
import { useState } from "react"
import { useMutation, useQuery } from "urql"

import { ActionsMenu } from "@/components/actions-menu"
import { ConfirmDialog } from "@/components/confirm-dialog"
import { ListToolbar } from "@/components/list-toolbar"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { DropdownMenuItem } from "@/components/ui/dropdown-menu"
import {
  Empty,
  EmptyDescription,
  EmptyHeader,
  EmptyMedia,
  EmptyTitle,
} from "@/components/ui/empty"
import { Skeleton } from "@/components/ui/skeleton"
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import { toast } from "@/components/ui/toast"
import { Can } from "@/features/access/can"
import { PERMISSIONS } from "@/features/access/permissions"
import { useCan } from "@/features/access/use-can"
import { FormError } from "@/features/auth/auth-layout"
import { RoleDrawer } from "@/features/roles/role-drawer"
import {
  ROLES_LIMIT,
  type Role,
  TenantRolesQuery,
} from "@/features/roles/roles-query"
import { useCompanies } from "@/features/tenants/tenant-context"
import { graphql } from "@/gql"
import {
  ACTIVE_STATUS_ITEMS,
  activeFilter,
  useListControls,
} from "@/hooks/use-list-controls"
import { errorMessage } from "@/lib/errors"

const InactivateTenantRoleMutation = graphql(`
  mutation InactivateTenantRole($input: TenantRoleIdInput!) {
    inactivateTenantRole(input: $input) {
      tenantRole {
        id
        active
      }
    }
  }
`)

const ReactivateTenantRoleMutation = graphql(`
  mutation ReactivateTenantRole($input: TenantRoleIdInput!) {
    reactivateTenantRole(input: $input) {
      tenantRole {
        id
        active
      }
    }
  }
`)

export function RolesPage() {
  const { current } = useCompanies()
  // Remount per company so nothing from the previous one is shown or kept.
  return <Roles key={current?.id ?? "none"} companyId={current?.id} />
}

function Roles({ companyId }: { companyId?: string }) {
  const can = useCan()
  const canUpdate = can(PERMISSIONS.tenantRolesUpdate)
  const canToggle = can(PERMISSIONS.tenantRolesInactivate)
  const list = useListControls()
  const [drawerOpen, setDrawerOpen] = useState(false)
  // The role being edited; none means the drawer creates one.
  const [editing, setEditing] = useState<Role | undefined>()
  const [toInactivate, setToInactivate] = useState<{
    id: string
    name: string
  } | null>(null)

  // No pager for now: the first ROLES_LIMIT roles are listed.
  const [{ data, fetching, error }, reload] = useQuery({
    query: TenantRolesQuery,
    variables: {
      first: ROLES_LIMIT,
      query: list.pageVariables.query,
      active: activeFilter(list.status),
    },
    requestPolicy: "network-only",
    pause: !companyId,
  })
  const [inactivation, inactivate] = useMutation(InactivateTenantRoleMutation)
  const [reactivation, reactivate] = useMutation(ReactivateTenantRoleMutation)

  const roles = data?.tenantRoles.edges.map((edge) => edge.node) ?? []

  function changeStatus(
    run: (variables: { input: { id: string } }) => Promise<{ error?: unknown }>,
    id: string,
    title: string,
    description: string
  ) {
    void run({ input: { id } }).then((result) => {
      if (result.error) return
      reload({ requestPolicy: "network-only" })
      toast.add({ title, description, type: "success" })
    })
  }

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div className="flex flex-col gap-1">
          <h1 className="text-2xl font-semibold tracking-tight">Roles</h1>
          <p className="text-sm text-muted-foreground">
            A role is a set of permissions. People can have several roles and
            get everything they allow together.
          </p>
        </div>
        <Can permission={PERMISSIONS.tenantRolesCreate}>
          <Button
            onClick={() => {
              setEditing(undefined)
              setDrawerOpen(true)
            }}
          >
            <PlusIcon data-icon="inline-start" />
            Create Role
          </Button>
        </Can>
      </div>

      <ListToolbar
        searchLabel="Search roles"
        searchPlaceholder="Search by name..."
        statusItems={ACTIVE_STATUS_ITEMS}
        {...list.toolbar}
      />

      <FormError
        message={errorMessage(
          error ?? inactivation.error ?? reactivation.error
        )}
      />

      {fetching && !data ? (
        <div className="flex flex-col gap-2">
          <Skeleton className="h-10 w-full" />
          <Skeleton className="h-10 w-full" />
        </div>
      ) : roles.length === 0 ? (
        <Empty className="border">
          <EmptyHeader>
            <EmptyMedia variant="icon">
              <ShieldIcon />
            </EmptyMedia>
            <EmptyTitle>
              {list.filtered ? "No results" : "No roles yet"}
            </EmptyTitle>
            <EmptyDescription>
              {list.filtered
                ? "No role matches your filters."
                : "Create a role to give people access."}
            </EmptyDescription>
          </EmptyHeader>
        </Empty>
      ) : (
        <div className="rounded-lg border">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Name</TableHead>
                <TableHead>Permissions</TableHead>
                <TableHead>Status</TableHead>
                <TableHead className="w-12">
                  <span className="sr-only">Actions</span>
                </TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {roles.map((role) => (
                <TableRow key={role.id} className="h-12">
                  <TableCell className="font-medium">
                    {role.name}
                    {role.builtIn && (
                      <Badge variant="secondary" className="ml-2">
                        Default
                      </Badge>
                    )}
                  </TableCell>
                  <TableCell>
                    {role.permissions.length === 1
                      ? "1 permission"
                      : `${role.permissions.length} permissions`}
                  </TableCell>
                  <TableCell>
                    <Badge variant={role.active ? "outline" : "secondary"}>
                      {role.active ? "Active" : "Inactive"}
                    </Badge>
                  </TableCell>
                  <TableCell>
                    <ActionsMenu
                      label={`Actions for ${role.name}`}
                      show={!role.builtIn && (canUpdate || canToggle)}
                    >
                      {canUpdate && (
                        <DropdownMenuItem
                          onClick={() => {
                            setEditing(role)
                            setDrawerOpen(true)
                          }}
                        >
                          Manage
                        </DropdownMenuItem>
                      )}
                      {canToggle && role.active && (
                        <DropdownMenuItem
                          variant="destructive"
                          onClick={() =>
                            setToInactivate({ id: role.id, name: role.name })
                          }
                        >
                          Inactivate
                        </DropdownMenuItem>
                      )}
                      {canToggle && !role.active && (
                        <DropdownMenuItem
                          onClick={() =>
                            changeStatus(
                              reactivate,
                              role.id,
                              "Role reactivated",
                              `${role.name} can be chosen again.`
                            )
                          }
                        >
                          Reactivate
                        </DropdownMenuItem>
                      )}
                    </ActionsMenu>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
      )}

      <RoleDrawer
        key={editing?.id ?? "new"}
        role={editing}
        open={drawerOpen}
        onOpenChange={setDrawerOpen}
        onSaved={() => reload({ requestPolicy: "network-only" })}
      />

      <ConfirmDialog
        open={toInactivate !== null}
        onOpenChange={(open) => !open && setToInactivate(null)}
        title={`Inactivate ${toInactivate?.name}?`}
        description="People who have it keep it, but it grants nothing and cannot be given to anyone new."
        confirmLabel="Inactivate"
        onConfirm={() => {
          if (!toInactivate) return
          const { id, name } = toInactivate
          setToInactivate(null)
          changeStatus(
            inactivate,
            id,
            "Role inactivated",
            `${name} can no longer be given to anyone.`
          )
        }}
      />
    </div>
  )
}
