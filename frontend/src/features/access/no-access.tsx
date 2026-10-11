import { LockIcon } from "lucide-react"

import {
  Empty,
  EmptyDescription,
  EmptyHeader,
  EmptyMedia,
  EmptyTitle,
} from "@/components/ui/empty"

export function NoAccess() {
  return (
    <Empty className="border">
      <EmptyHeader>
        <EmptyMedia variant="icon">
          <LockIcon />
        </EmptyMedia>
        <EmptyTitle>No access</EmptyTitle>
        <EmptyDescription>
          You do not have permission to see this page in the selected company.
          Ask an administrator to change your roles.
        </EmptyDescription>
      </EmptyHeader>
    </Empty>
  )
}
