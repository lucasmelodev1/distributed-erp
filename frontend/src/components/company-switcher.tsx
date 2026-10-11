import { CheckIcon, PlusIcon } from "lucide-react"
import { useNavigate } from "react-router"

import {
  DropdownMenuGroup,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
} from "@/components/ui/dropdown-menu"
import { useCompanies } from "@/features/tenants/tenant-context"

/** The "Companies" section of the user dropdown: switch company or create one. */
export function CompanySwitcher() {
  const { companies, current, select } = useCompanies()
  const navigate = useNavigate()
  return (
    <>
      <DropdownMenuGroup>
        <DropdownMenuLabel className="text-xs text-muted-foreground">
          Companies
        </DropdownMenuLabel>
        {companies.map((company) => (
          <DropdownMenuItem
            key={company.id}
            // An inactive company rejects every request; reactivate it from the Companies page.
            disabled={!company.active}
            onClick={() => select(company.id)}
            className="gap-2 p-2"
          >
            <span className="truncate">{company.name}</span>
            {company.id === current?.id && (
              <CheckIcon className="ml-auto" aria-label="Selected" />
            )}
          </DropdownMenuItem>
        ))}
      </DropdownMenuGroup>
      {companies.length > 0 && <DropdownMenuSeparator />}
      <DropdownMenuGroup>
        <DropdownMenuItem
          className="gap-2 p-2"
          onClick={() => void navigate("/companies/new")}
        >
          <PlusIcon />
          <span className="font-medium text-muted-foreground">
            Create Company
          </span>
        </DropdownMenuItem>
      </DropdownMenuGroup>
      <DropdownMenuSeparator />
    </>
  )
}
