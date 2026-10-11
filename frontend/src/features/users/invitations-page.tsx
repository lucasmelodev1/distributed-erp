import { MailIcon, MoreHorizontalIcon, PlusIcon } from "lucide-react"
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
import { useCompanies } from "@/features/tenants/tenant-context"
import { InviteDrawer } from "@/features/users/invite-drawer"
import { graphql } from "@/gql"
import type { TenantInvitationStatus } from "@/gql/graphql"
import { ALL, useListControls } from "@/hooks/use-list-controls"
import { errorMessage } from "@/lib/errors"

const TenantInvitationsQuery = graphql(`
  query TenantInvitations(
    $first: Int
    $after: String
    $status: TenantInvitationStatus
    $query: String
  ) {
    tenantInvitations(
      first: $first
      after: $after
      status: $status
      query: $query
    ) {
      edges {
        node {
          id
          email
          status
          expiresAt
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

const ResendInvitationMutation = graphql(`
  mutation ResendInvitation($input: ResendInvitationInput!) {
    resendInvitation(input: $input) {
      invitation {
        id
        status
        expiresAt
      }
    }
  }
`)

const RevokeInvitationMutation = graphql(`
  mutation RevokeInvitation($input: RevokeInvitationInput!) {
    revokeInvitation(input: $input) {
      invitation {
        id
        status
      }
    }
  }
`)

const STATUS_ITEMS = [
  { value: ALL, label: "All statuses" },
  { value: "PENDING", label: "Pending" },
  { value: "ACCEPTED", label: "Accepted" },
  { value: "REVOKED", label: "Revoked" },
  { value: "EXPIRED", label: "Expired" },
]

const STATUS_LABELS = new Map(STATUS_ITEMS.map((i) => [i.value, i.label]))

function statusVariant(status: TenantInvitationStatus) {
  if (status === "PENDING") return "outline"
  if (status === "ACCEPTED") return "default"
  return "secondary"
}

export function InvitationsPage() {
  const { current } = useCompanies()
  // Remount per company so nothing from the previous one is shown or kept.
  return <Invitations key={current?.id ?? "none"} companyId={current?.id} />
}

function Invitations({ companyId }: { companyId?: string }) {
  const list = useListControls()
  const [drawerOpen, setDrawerOpen] = useState(false)
  const [toRevoke, setToRevoke] = useState<{
    id: string
    email: string
  } | null>(null)

  const [{ data, fetching, error }, reload] = useQuery({
    query: TenantInvitationsQuery,
    variables: {
      ...list.pageVariables,
      status:
        list.status === ALL ? null : (list.status as TenantInvitationStatus),
    },
    requestPolicy: "network-only",
    pause: !companyId,
  })
  const [revocation, revoke] = useMutation(RevokeInvitationMutation)
  const [resending, resend] = useMutation(ResendInvitationMutation)

  const edges = data?.tenantInvitations.edges ?? []
  const pageInfo = data?.tenantInvitations.pageInfo

  function resendEmail(id: string, email: string) {
    void resend({ input: { invitationId: id } }).then((result) => {
      if (result.error) return
      reloadList()
      toast.add({
        title: "Email sent again",
        description: `${email} got a new link. The previous one no longer works.`,
        type: "success",
      })
    })
  }

  function reloadList() {
    reload({ requestPolicy: "network-only" })
  }

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div className="flex flex-col gap-1">
          <h1 className="text-2xl font-semibold tracking-tight">Invitations</h1>
          <p className="text-sm text-muted-foreground">
            People invited to join the selected company.
          </p>
        </div>
        <Button onClick={() => setDrawerOpen(true)}>
          <PlusIcon data-icon="inline-start" />
          Send Invitation
        </Button>
      </div>

      <ListToolbar
        searchLabel="Search invitations"
        searchPlaceholder="Search by email..."
        statusItems={STATUS_ITEMS}
        {...list.toolbar}
      />

      <FormError
        message={errorMessage(error ?? revocation.error ?? resending.error)}
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
              <MailIcon />
            </EmptyMedia>
            <EmptyTitle>
              {list.filtered ? "No results" : "No invitations yet"}
            </EmptyTitle>
            <EmptyDescription>
              {list.filtered
                ? "No invitation matches your filters."
                : "Send an invitation to add people to this company."}
            </EmptyDescription>
          </EmptyHeader>
        </Empty>
      ) : (
        <div className="rounded-lg border">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Email</TableHead>
                <TableHead>Status</TableHead>
                <TableHead>Roles</TableHead>
                <TableHead>Expires</TableHead>
                <TableHead className="w-12">
                  <span className="sr-only">Actions</span>
                </TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {edges.map(({ node }) => (
                <TableRow key={node.id} className="h-12">
                  <TableCell className="font-medium">{node.email}</TableCell>
                  <TableCell>
                    <Badge variant={statusVariant(node.status)}>
                      {STATUS_LABELS.get(node.status) ?? node.status}
                    </Badge>
                  </TableCell>
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
                    {node.expiresAt
                      ? new Date(node.expiresAt).toLocaleDateString()
                      : "Never"}
                  </TableCell>
                  <TableCell>
                    {(node.status === "PENDING" ||
                      node.status === "EXPIRED") && (
                      <DropdownMenu>
                        <DropdownMenuTrigger
                          render={<Button variant="ghost" size="icon-sm" />}
                          aria-label={`Actions for ${node.email}`}
                        >
                          <MoreHorizontalIcon />
                        </DropdownMenuTrigger>
                        <DropdownMenuContent align="end">
                          <DropdownMenuGroup>
                            <DropdownMenuItem
                              onClick={() => resendEmail(node.id, node.email)}
                            >
                              Resend email
                            </DropdownMenuItem>
                            {node.status === "PENDING" && (
                              <DropdownMenuItem
                                variant="destructive"
                                onClick={() =>
                                  setToRevoke({
                                    id: node.id,
                                    email: node.email,
                                  })
                                }
                              >
                                Revoke
                              </DropdownMenuItem>
                            )}
                          </DropdownMenuGroup>
                        </DropdownMenuContent>
                      </DropdownMenu>
                    )}
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
      )}

      <CursorPager {...list.pagerProps(pageInfo, fetching)} />

      <InviteDrawer
        open={drawerOpen}
        onOpenChange={setDrawerOpen}
        onInvited={reloadList}
      />

      <ConfirmDialog
        open={toRevoke !== null}
        onOpenChange={(open) => !open && setToRevoke(null)}
        title="Revoke the invitation?"
        description={`The link sent to ${toRevoke?.email} stops working. You can invite them again later.`}
        confirmLabel="Revoke"
        onConfirm={() => {
          if (!toRevoke) return
          const { id, email } = toRevoke
          setToRevoke(null)
          void revoke({ input: { invitationId: id } }).then((result) => {
            if (result.error) return
            reloadList()
            toast.add({
              title: "Invitation revoked",
              description: `${email} can no longer use the link.`,
              type: "success",
            })
          })
        }}
      />
    </div>
  )
}
