import {
  BuildingIcon,
  MoreHorizontalIcon,
  PlusIcon,
  SearchIcon,
} from "lucide-react"
import { useState } from "react"
import { Link, useNavigate } from "react-router"
import { useMutation } from "urql"

import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog"
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
import {
  InputGroup,
  InputGroupAddon,
  InputGroupInput,
} from "@/components/ui/input-group"
import { Skeleton } from "@/components/ui/skeleton"
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import { FormError } from "@/features/auth/auth-layout"
import { type Company, useCompanies } from "@/features/tenants/tenant-context"
import { graphql } from "@/gql"
import { errorMessage } from "@/lib/errors"
import { tenantContext } from "@/lib/tenant"

const InactivateTenantMutation = graphql(`
  mutation InactivateTenant {
    inactivateTenant {
      tenant {
        id
        name
        active
        inactivatedAt
      }
    }
  }
`)

const ReactivateTenantMutation = graphql(`
  mutation ReactivateTenant {
    reactivateTenant {
      tenant {
        id
        name
        active
        inactivatedAt
      }
    }
  }
`)

export function CompaniesPage() {
  const { companies, current, loading, select, refresh } = useCompanies()
  const navigate = useNavigate()
  const [search, setSearch] = useState("")
  const [toInactivate, setToInactivate] = useState<Company | null>(null)
  const [inactivation, inactivate] = useMutation(InactivateTenantMutation)
  const [reactivation, reactivate] = useMutation(ReactivateTenantMutation)

  const query = search.trim().toLowerCase()
  const visible = companies.filter((company) =>
    company.name.toLowerCase().includes(query)
  )
  const error = inactivation.error ?? reactivation.error

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div className="flex flex-col gap-1">
          <h1 className="text-2xl font-semibold tracking-tight">Companies</h1>
          <p className="text-sm text-muted-foreground">
            The companies you belong to. Switch between them from the menu in
            the sidebar.
          </p>
        </div>
        <Button render={<Link to="/companies/new" />}>
          <PlusIcon data-icon="inline-start" />
          Create Company
        </Button>
      </div>

      <InputGroup className="max-w-sm">
        <InputGroupAddon>
          <SearchIcon />
        </InputGroupAddon>
        <InputGroupInput
          aria-label="Search companies"
          placeholder="Search companies..."
          value={search}
          onChange={(event) => setSearch(event.target.value)}
        />
      </InputGroup>

      <FormError message={errorMessage(error)} />

      {loading ? (
        <div className="flex flex-col gap-2">
          <Skeleton className="h-10 w-full" />
          <Skeleton className="h-10 w-full" />
        </div>
      ) : visible.length === 0 ? (
        <Empty className="border">
          <EmptyHeader>
            <EmptyMedia variant="icon">
              <BuildingIcon />
            </EmptyMedia>
            <EmptyTitle>
              {companies.length === 0 ? "No companies yet" : "No results"}
            </EmptyTitle>
            <EmptyDescription>
              {companies.length === 0
                ? "Create your first company to get started."
                : "No company matches your search."}
            </EmptyDescription>
          </EmptyHeader>
        </Empty>
      ) : (
        <div className="rounded-lg border">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Name</TableHead>
                <TableHead>Status</TableHead>
                <TableHead className="w-12">
                  <span className="sr-only">Actions</span>
                </TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {visible.map((company) => (
                <TableRow key={company.id}>
                  <TableCell className="font-medium">
                    {company.name}
                    {company.id === current?.id && (
                      <Badge variant="secondary" className="ml-2">
                        Selected
                      </Badge>
                    )}
                  </TableCell>
                  <TableCell>
                    <Badge variant={company.active ? "outline" : "secondary"}>
                      {company.active ? "Active" : "Inactive"}
                    </Badge>
                  </TableCell>
                  <TableCell>
                    <DropdownMenu>
                      <DropdownMenuTrigger
                        render={<Button variant="ghost" size="icon" />}
                        aria-label={`Actions for ${company.name}`}
                      >
                        <MoreHorizontalIcon />
                      </DropdownMenuTrigger>
                      <DropdownMenuContent align="end">
                        <DropdownMenuGroup>
                          {company.active && (
                            <DropdownMenuItem
                              onClick={() => select(company.id)}
                            >
                              Switch to this company
                            </DropdownMenuItem>
                          )}
                          <DropdownMenuItem
                            onClick={() =>
                              void navigate(`/companies/${company.id}/edit`)
                            }
                          >
                            Edit
                          </DropdownMenuItem>
                          {company.active ? (
                            <DropdownMenuItem
                              variant="destructive"
                              onClick={() => setToInactivate(company)}
                            >
                              Inactivate
                            </DropdownMenuItem>
                          ) : (
                            <DropdownMenuItem
                              onClick={() =>
                                void reactivate(
                                  {},
                                  tenantContext(company.id)
                                ).then(refresh)
                              }
                            >
                              Reactivate
                            </DropdownMenuItem>
                          )}
                        </DropdownMenuGroup>
                      </DropdownMenuContent>
                    </DropdownMenu>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
      )}

      <AlertDialog
        open={toInactivate !== null}
        onOpenChange={(open) => !open && setToInactivate(null)}
      >
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>
              Inactivate {toInactivate?.name}?
            </AlertDialogTitle>
            <AlertDialogDescription>
              All its members lose access until the company is reactivated.
              Nothing is deleted.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Cancel</AlertDialogCancel>
            <AlertDialogAction
              variant="destructive"
              onClick={() => {
                if (!toInactivate) return
                // AlertDialogAction is a plain Button here, so it does not close the dialog itself.
                setToInactivate(null)
                void inactivate({}, tenantContext(toInactivate.id)).then(
                  refresh
                )
              }}
            >
              Inactivate
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  )
}
