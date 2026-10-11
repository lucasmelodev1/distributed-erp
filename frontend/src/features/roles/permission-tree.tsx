import { Checkbox } from "@/components/ui/checkbox"
import {
  Field,
  FieldGroup,
  FieldLabel,
  FieldLegend,
  FieldSet,
} from "@/components/ui/field"
import { Separator } from "@/components/ui/separator"
import { ToggleGroup, ToggleGroupItem } from "@/components/ui/toggle-group"
import {
  groupPermissions,
  type PermissionGroup,
  type PermissionInfo,
} from "@/features/roles/permission-labels"
import { PermissionLevelIcon } from "@/features/roles/permission-level-icon"
import {
  isLevelOn,
  LEVEL_INFO,
  LEVELS,
  selectLevel,
} from "@/features/roles/permission-levels"
import type { PermissionLevel } from "@/gql/graphql"

type PermissionTreeProps = {
  permissions: readonly PermissionInfo[]
  value: readonly string[]
  onChange: (codes: string[]) => void
}

/**
 * Permissions grouped by area. Each area has a View / Edit / Sensitive button group that
 * selects every permission up to that level, and each permission can still be ticked by hand.
 */
export function PermissionTree({
  permissions,
  value,
  onChange,
}: PermissionTreeProps) {
  const selected = new Set(value)

  /** Replaces the selection of one area, keeping what is selected in the others. */
  function setGroupCodes(group: PermissionGroup, codes: string[]) {
    const inGroup = new Set(group.permissions.map((p) => p.code))
    onChange([...value.filter((code) => !inGroup.has(code)), ...codes])
  }

  function toggle(group: PermissionGroup, code: string, on: boolean) {
    const next = new Set(selected)
    if (on) next.add(code)
    else next.delete(code)
    setGroupCodes(
      group,
      group.permissions.map((p) => p.code).filter((c) => next.has(c))
    )
  }

  return (
    <FieldGroup>
      {groupPermissions(permissions).map((group) => (
        <FieldSet key={group.key} className="gap-3">
          <div className="flex items-end justify-between gap-2">
            <FieldLegend variant="label" className="mb-0">
              {group.label}
            </FieldLegend>
            <LevelButtons
              group={group}
              selected={selected}
              onSelect={(level) =>
                setGroupCodes(
                  group,
                  selectLevel(group.permissions, level, selected)
                )
              }
            />
          </div>
          <Separator />
          <FieldGroup className="gap-3">
            {group.permissions.map((permission) => (
              <Field key={permission.code} orientation="horizontal">
                <PermissionLevelIcon level={permission.level} />
                <Checkbox
                  id={`permission-${permission.code}`}
                  checked={selected.has(permission.code)}
                  onCheckedChange={(checked) =>
                    toggle(group, permission.code, checked)
                  }
                />
                <FieldLabel
                  htmlFor={`permission-${permission.code}`}
                  className="font-normal"
                >
                  {permission.label}
                </FieldLabel>
              </Field>
            ))}
          </FieldGroup>
        </FieldSet>
      ))}
    </FieldGroup>
  )
}

function LevelButtons({
  group,
  selected,
  onSelect,
}: {
  group: PermissionGroup
  selected: ReadonlySet<string>
  onSelect: (level: PermissionLevel) => void
}) {
  // The buttons only mirror the ticked boxes, so the group is controlled and a click just reports its level.
  const on = LEVELS.filter((level) =>
    isLevelOn(group.permissions, level, selected)
  )
  return (
    <ToggleGroup
      multiple
      variant="outline"
      size="sm"
      spacing={0}
      value={on}
      aria-label={`${group.label} access`}
    >
      {LEVELS.map((level) => {
        const { icon: Icon, label, pressed } = LEVEL_INFO[level]
        return (
          <ToggleGroupItem
            key={level}
            value={level}
            aria-label={label}
            title={label}
            className={pressed}
            disabled={!group.permissions.some((p) => p.level === level)}
            onClick={() => onSelect(level)}
          >
            <Icon />
          </ToggleGroupItem>
        )
      })}
    </ToggleGroup>
  )
}
