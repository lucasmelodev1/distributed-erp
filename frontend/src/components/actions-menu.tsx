import { MoreHorizontalIcon } from "lucide-react"

import { Button } from "@/components/ui/button"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuGroup,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"

/**
 * The "..." menu of a table row. Pass `show={false}` when the user may do none of its items, so
 * an empty menu is never rendered.
 */
export function ActionsMenu({
  label,
  size = "icon-sm",
  show = true,
  children,
}: {
  label: string
  size?: "icon" | "icon-sm"
  show?: boolean
  children: React.ReactNode
}) {
  if (!show) return null
  return (
    <DropdownMenu>
      <DropdownMenuTrigger
        render={<Button variant="ghost" size={size} />}
        aria-label={label}
      >
        <MoreHorizontalIcon />
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end">
        <DropdownMenuGroup>{children}</DropdownMenuGroup>
      </DropdownMenuContent>
    </DropdownMenu>
  )
}
