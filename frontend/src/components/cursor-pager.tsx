import { ChevronLeftIcon, ChevronRightIcon } from "lucide-react"

import { Button } from "@/components/ui/button"
import {
  Select,
  SelectContent,
  SelectGroup,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"

export const PAGE_SIZES = [10, 20, 50]

const PAGE_SIZE_ITEMS = PAGE_SIZES.map((size) => ({
  value: String(size),
  label: String(size),
}))

type CursorPagerProps = {
  pageNumber: number
  hasPrevious: boolean
  hasNext: boolean
  pageSize: number
  onPageSizeChange: (size: number) => void
  onPrevious: () => void
  onNext: () => void
  disabled?: boolean
}

/** Footer of a table backed by a Relay cursor connection. */
export function CursorPager({
  pageNumber,
  hasPrevious,
  hasNext,
  pageSize,
  onPageSizeChange,
  onPrevious,
  onNext,
  disabled,
}: CursorPagerProps) {
  return (
    <div className="flex flex-wrap items-center justify-end gap-4">
      <div className="flex items-center gap-2 text-sm text-muted-foreground">
        <span id="page-size-label">Rows per page</span>
        <Select
          items={PAGE_SIZE_ITEMS}
          value={String(pageSize)}
          onValueChange={(value) => onPageSizeChange(Number(value))}
        >
          <SelectTrigger size="sm" aria-labelledby="page-size-label">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            <SelectGroup>
              {PAGE_SIZE_ITEMS.map((item) => (
                <SelectItem key={item.value} value={item.value}>
                  {item.label}
                </SelectItem>
              ))}
            </SelectGroup>
          </SelectContent>
        </Select>
      </div>
      <span className="text-sm text-muted-foreground">Page {pageNumber}</span>
      <div className="flex items-center gap-2">
        <Button
          variant="outline"
          size="sm"
          disabled={disabled || !hasPrevious}
          onClick={onPrevious}
        >
          <ChevronLeftIcon data-icon="inline-start" />
          Previous
        </Button>
        <Button
          variant="outline"
          size="sm"
          disabled={disabled || !hasNext}
          onClick={onNext}
        >
          Next
          <ChevronRightIcon data-icon="inline-end" />
        </Button>
      </div>
    </div>
  )
}
