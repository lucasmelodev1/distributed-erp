import { SearchIcon } from "lucide-react"

import {
  InputGroup,
  InputGroupAddon,
  InputGroupInput,
} from "@/components/ui/input-group"
import {
  Select,
  SelectContent,
  SelectGroup,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"

type ListToolbarProps = {
  searchLabel: string
  searchPlaceholder: string
  search: string
  onSearchChange: (search: string) => void
  statusItems: { value: string; label: string }[]
  status: string
  onStatusChange: (status: string) => void
}

/** Search box and status dropdown above a list. */
export function ListToolbar({
  searchLabel,
  searchPlaceholder,
  search,
  onSearchChange,
  statusItems,
  status,
  onStatusChange,
}: ListToolbarProps) {
  return (
    <div className="flex flex-wrap gap-2">
      <InputGroup className="max-w-sm">
        <InputGroupAddon>
          <SearchIcon />
        </InputGroupAddon>
        <InputGroupInput
          aria-label={searchLabel}
          placeholder={searchPlaceholder}
          value={search}
          onChange={(event) => onSearchChange(event.target.value)}
        />
      </InputGroup>
      <Select
        items={statusItems}
        value={status}
        onValueChange={(value) => value && onStatusChange(value)}
      >
        <SelectTrigger aria-label="Filter by status" className="w-40">
          <SelectValue />
        </SelectTrigger>
        <SelectContent>
          <SelectGroup>
            {statusItems.map((item) => (
              <SelectItem key={item.value} value={item.value}>
                {item.label}
              </SelectItem>
            ))}
          </SelectGroup>
        </SelectContent>
      </Select>
    </div>
  )
}
