import type { ComponentProps } from "react"
import { useState } from "react"

import { type CursorPager, PAGE_SIZES } from "@/components/cursor-pager"
import { useCursorPages } from "@/hooks/use-cursor-pages"
import { useDebouncedValue } from "@/hooks/use-debounced-value"

/** The value of the "no filter" item of a status dropdown. */
export const ALL = "ALL"

/** Status dropdown items for lists of things that are either active or inactive. */
export const ACTIVE_STATUS_ITEMS = [
  { value: ALL, label: "All statuses" },
  { value: "ACTIVE", label: "Active" },
  { value: "INACTIVE", label: "Inactive" },
]

/** The API's `active` argument for a value of `ACTIVE_STATUS_ITEMS`. */
export function activeFilter(status: string) {
  if (status === "ACTIVE") return true
  if (status === "INACTIVE") return false
  return null
}

type PageInfo = { hasNextPage: boolean; endCursor?: string | null }

/**
 * State shared by the paged, searchable, filterable lists: debounced search text, status
 * filter, page size and the cursor history. Any change of the three restarts at page 1.
 */
export function useListControls() {
  const [search, setSearch] = useState("")
  const [status, setStatus] = useState(ALL)
  const [pageSize, setPageSize] = useState(PAGE_SIZES[1]!)
  const query = useDebouncedValue(search.trim())
  const pages = useCursorPages(`${query}|${status}|${pageSize}`)

  return {
    status,
    /** True when the list is narrowed by a search or a filter (for the empty state). */
    filtered: query !== "" || status !== ALL,
    /** `first`, `after` and `query` of a connection query. */
    pageVariables: {
      first: pageSize,
      after: pages.after,
      query: query || null,
    },
    /** Props of `ListToolbar`, apart from its labels and items. */
    toolbar: {
      search,
      onSearchChange: setSearch,
      status,
      onStatusChange: setStatus,
    },
    pagerProps: (
      pageInfo: PageInfo | undefined,
      fetching: boolean
    ): ComponentProps<typeof CursorPager> => ({
      pageNumber: pages.pageNumber,
      hasPrevious: pages.hasPrevious,
      hasNext: pageInfo?.hasNextPage ?? false,
      pageSize,
      onPageSizeChange: setPageSize,
      onPrevious: pages.previous,
      onNext: () => pageInfo?.endCursor && pages.next(pageInfo.endCursor),
      disabled: fetching,
    }),
  }
}
