import { Badge } from "@/components/ui/badge"
import { Checkbox } from "@/components/ui/checkbox"
import { Field, FieldGroup, FieldLabel } from "@/components/ui/field"
import type { Role } from "@/features/roles/roles-query"

type RolePickerProps = {
  roles: readonly Role[]
  value: readonly string[]
  onChange: (roleIds: string[]) => void
}

/**
 * Checkboxes for choosing roles. An inactive role can never be newly picked: it is
 * disabled, unless it is already ticked, so that it can still be taken away.
 */
export function RolePicker({ roles, value, onChange }: RolePickerProps) {
  const selected = new Set(value)

  function toggle(id: string, on: boolean) {
    const next = new Set(selected)
    if (on) next.add(id)
    else next.delete(id)
    onChange([...next])
  }

  return (
    <FieldGroup className="gap-3">
      {roles.map((role) => (
        <Field key={role.id} orientation="horizontal">
          <Checkbox
            id={`role-${role.id}`}
            checked={selected.has(role.id)}
            disabled={!role.active && !selected.has(role.id)}
            onCheckedChange={(checked) => toggle(role.id, checked)}
          />
          <FieldLabel htmlFor={`role-${role.id}`} className="font-normal">
            {role.name}
            {role.builtIn && <Badge variant="secondary">Default</Badge>}
            {!role.active && <Badge variant="outline">Inactive</Badge>}
          </FieldLabel>
        </Field>
      ))}
    </FieldGroup>
  )
}
