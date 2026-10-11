import { MoreHorizontalIcon, UsersIcon } from "lucide-react"
import { useState } from "react"
import { useMutation, useQuery } from "urql"

import { ConfirmDialog } from "@/components/confirm-dialog"
import { CursorPager } from "@/components/cursor-pager"
import { ListToolbar } from "@/components/list-toolbar"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuGroup,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
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
import { FormError } from "@/features/auth/auth-layout"
import {
  ManageUserDrawer,
  type ManagedMember,
} from "@/features/users/manage-user-drawer"
import { useSession } from "@/features/auth/session"
import { useCompanies } from "@/features/tenants/tenant-context"
import { graphql } from "@/gql"
import {
  ACTIVE_STATUS_ITEMS,
  activeFilter,
  useListControls,
} from "@/hooks/use-list-controls"
import { errorMessage } from "@/lib/errors"

const TenantUsersQuery = graphql(`
  query TenantUsers(
    $first: Int
    $after: String
    $query: String
    $active: Boolean
  ) {
    tenantUsers(first: $first, after: $after, query: $query, active: $active) {
      edges {
        node {
          active
          user {
            id
            name
            email
          }
          roles {
            id
            name
          }
        }
      }
      pageInfo {
        hasNextPage
        endCursor
      }
    }
  }
`)

const InactivateTenantUserMutation = graphql(`
  mutation InactivateTenantUser($input: TenantUserIdInput!) {
    inactivateTenantUser(input: $input) {
      tenantUser {
        active
        inactivatedAt
      }
    }
  }
`)

const ReactivateTenantUserMutation = graphql(`
  mutation ReactivateTenantUser($input: TenantUserIdInput!) {
    reactivateTenantUser(input: $input) {
      tenantUser {
        active
        inactivatedAt
      }
    }
  }
`)

export function UsersPage() {
  const { current } = useCompanies()
  // Remount per company so nothing from the previous one is shown or kept.
  return <Members key={current?.id ?? "none"} companyId={current?.id} />
}

function Members({ companyId }: { companyId?: string }) {
  const { user: me } = useSession()
  const list = useListControls()
  const [managing, setManaging] = useState<ManagedMember | null>(null)
  const [manageOpen, setManageOpen] = useState(false)
  const [toInactivate, setToInactivate] = useState<{
    id: string
    name: string
  } | null>(null)

  const [{ data, fetching, error }, reload] = useQuery({
    query: TenantUsersQuery,
    variables: {
      ...list.pageVariables,
      active: activeFilter(list.status),
    },
    requestPolicy: "network-only",
    pause: !companyId,
  })
  const [inactivation, inactivate] = useMutation(InactivateTenantUserMutation)
  const [reactivation, reactivate] = useMutation(ReactivateTenantUserMutation)

  const edges = data?.tenantUsers.edges ?? []
  const pageInfo = data?.tenantUsers.pageInfo

  function changeAccess(
    run: (variables: {
      input: { userId: string }
    }) => Promise<{ error?: unknown }>,
    userId: string,
    title: string,
    description: string
  ) {
    void run({ input: { userId } }).then((result) => {
      if (result.error) return
      reload({ requestPolicy: "network-only" })
      toast.add({ title, description, type: "success" })
    })
  }

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-col gap-1">
        <h1 className="text-2xl font-semibold tracking-tight">Users</h1>
        <p className="text-sm text-muted-foreground">
          The people who belong to the selected company.
        </p>
      </div>

      <ListToolbar
        searchLabel="Search users"
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
      ) : edges.length === 0 ? (
        <Empty className="border">
          <EmptyHeader>
            <EmptyMedia variant="icon">
              <UsersIcon />
            </EmptyMedia>
            <EmptyTitle>{list.filtered ? "No results" : "No users"}</EmptyTitle>
            <EmptyDescription>
              {list.filtered
                ? "No user matches your filters."
                : "Invite people from the Invitations page."}
            </EmptyDescription>
          </EmptyHeader>
        </Empty>
      ) : (
        <div className="rounded-lg border">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Name</TableHead>
                <TableHead>Email</TableHead>
                <TableHead>Roles</TableHead>
                <TableHead>Status</TableHead>
                <TableHead className="w-12">
                  <span className="sr-only">Actions</span>
                </TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {edges.map(({ node }, index) => {
                const member = node.user
                const isMe = member?.id === me?.id
                return (
                  <TableRow key={member?.id ?? index} className="h-12">
                    <TableCell className="font-medium">
                      {member?.name ?? "Unknown user"}
                    </TableCell>
                    <TableCell>{member?.email}</TableCell>
                    <TableCell>
                      <div className="flex flex-wrap gap-1">
                        {node.roles.map((role) => (
                          <Badge key={role.id} variant="secondary">
                            {role.name}
                          </Badge>
                        ))}
                      </div>
                    </TableCell>
                    <TableCell>
                      <Badge variant={node.active ? "outline" : "secondary"}>
                        {node.active ? "Active" : "Inactive"}
                      </Badge>
                    </TableCell>
                    <TableCell>
                      {member && !isMe && (
                        <DropdownMenu>
                          <DropdownMenuTrigger
                            render={<Button variant="ghost" size="icon-sm" />}
                            aria-label={`Actions for ${member.name}`}
                          >
                            <MoreHorizontalIcon />
                          </DropdownMenuTrigger>
                          <DropdownMenuContent align="end">
                            <DropdownMenuGroup>
                              <DropdownMenuItem
                                onClick={() => {
                                  setManaging({
                                    userId: member.id,
                                    name: member.name,
                                    email: member.email,
                                    roleIds: node.roles.map((role) => role.id),
                                  })
                                  setManageOpen(true)
                                }}
                              >
                                Manage
                              </DropdownMenuItem>
                              {node.active ? (
                                <DropdownMenuItem
                                  variant="destructive"
                                  onClick={() =>
                                    setToInactivate({
                                      id: member.id,
                                      name: member.name,
                                    })
                                  }
                                >
                                  Inactivate
                                </DropdownMenuItem>
                              ) : (
                                <DropdownMenuItem
                                  onClick={() =>
                                    changeAccess(
                                      reactivate,
                                      member.id,
                                      "Member reactivated",
                                      `${member.name} can enter this company again.`
                                    )
                                  }
                                >
                                  Reactivate
                                </DropdownMenuItem>
                              )}
                            </DropdownMenuGroup>
                          </DropdownMenuContent>
                        </DropdownMenu>
                      )}
                    </TableCell>
                  </TableRow>
                )
              })}
            </TableBody>
          </Table>
        </div>
      )}

      <CursorPager {...list.pagerProps(pageInfo, fetching)} />

      {managing && (
        <ManageUserDrawer
          key={managing.userId}
          member={managing}
          open={manageOpen}
          onOpenChange={setManageOpen}
          onSaved={() => reload({ requestPolicy: "network-only" })}
        />
      )}

      <ConfirmDialog
        open={toInactivate !== null}
        onOpenChange={(open) => !open && setToInactivate(null)}
        title={`Inactivate ${toInactivate?.name}?`}
        description="They lose access to this company until you reactivate them. Their roles are kept."
        confirmLabel="Inactivate"
        onConfirm={() => {
          if (!toInactivate) return
          const { id, name } = toInactivate
          setToInactivate(null)
          changeAccess(
            inactivate,
            id,
            "Member inactivated",
            `${name} can no longer enter this company.`
          )
        }}
      />
    </div>
  )
}
