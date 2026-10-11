import { EyeIcon, PencilIcon, TriangleAlertIcon } from "lucide-react"

import type { PermissionLevel } from "@/gql/graphql"

/** In order of increasing impact. Each level includes everything before it. */
export const LEVELS = [
  "VIEW",
  "EDIT",
  "SENSITIVE",
] as const satisfies readonly PermissionLevel[]

/**
 * How a level looks. `pressed` colours the level's button when it is on; it is written out
 * in full so Tailwind can see the classes, and the colours are deliberately fixed (blue,
 * orange, red) because they carry meaning, not theme.
 */
export const LEVEL_INFO: Record<
  PermissionLevel,
  { label: string; icon: typeof EyeIcon; pressed: string }
> = {
  VIEW: {
    label: "View",
    icon: EyeIcon,
    pressed:
      "aria-pressed:bg-sky-100 aria-pressed:text-sky-700 aria-pressed:hover:bg-sky-200 dark:aria-pressed:bg-sky-950 dark:aria-pressed:text-sky-300",
  },
  EDIT: {
    label: "Edit",
    icon: PencilIcon,
    pressed:
      "aria-pressed:bg-orange-100 aria-pressed:text-orange-700 aria-pressed:hover:bg-orange-200 dark:aria-pressed:bg-orange-950 dark:aria-pressed:text-orange-300",
  },
  SENSITIVE: {
    label: "Sensitive actions",
    icon: TriangleAlertIcon,
    pressed:
      "aria-pressed:bg-red-100 aria-pressed:text-red-700 aria-pressed:hover:bg-red-200 dark:aria-pressed:bg-red-950 dark:aria-pressed:text-red-300",
  },
}

type Leveled = { code: string; level: PermissionLevel }

export function levelRank(level: PermissionLevel) {
  return LEVELS.indexOf(level)
}

/**
 * Whether the button of `level` is on: the area has permissions of that level and every
 * permission up to it is selected. This also holds when the boxes were ticked by hand.
 */
export function isLevelOn(
  permissions: readonly Leveled[],
  level: PermissionLevel,
  selected: ReadonlySet<string>
) {
  const upTo = permissions.filter((p) => levelRank(p.level) <= levelRank(level))
  return (
    upTo.some((p) => p.level === level) &&
    upTo.every((p) => selected.has(p.code))
  )
}

/**
 * The codes selected in an area after clicking `level`: exactly the permissions up to that
 * level. Clicking the highest level that is already on steps one level down instead, which
 * is how an area is cleared.
 */
export function selectLevel(
  permissions: readonly Leveled[],
  level: PermissionLevel,
  selected: ReadonlySet<string>
) {
  const highestOn = [...LEVELS]
    .reverse()
    .find((candidate) => isLevelOn(permissions, candidate, selected))
  const target = levelRank(level) - (highestOn === level ? 1 : 0)
  return permissions
    .filter((p) => levelRank(p.level) <= target)
    .map((p) => p.code)
}
