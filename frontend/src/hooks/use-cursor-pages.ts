import { useState } from "react"

/**
 * Back/next navigation over a forward-only cursor connection: remembers the `after`
 * cursor of every page visited. Pages restart at the first one when `resetKey` changes
 * (search text, filters, page size).
 */
export function useCursorPages(resetKey: string) {
  const [state, setState] = useState<{
    key: string
    cursors: (string | null)[]
  }>({ key: resetKey, cursors: [null] })
  const cursors = state.key === resetKey ? state.cursors : [null]

  return {
    /** The `after` argument of the current page. */
    after: cursors[cursors.length - 1] ?? null,
    pageNumber: cursors.length,
    hasPrevious: cursors.length > 1,
    next: (endCursor: string) =>
      setState({ key: resetKey, cursors: [...cursors, endCursor] }),
    previous: () => setState({ key: resetKey, cursors: cursors.slice(0, -1) }),
  }
}
