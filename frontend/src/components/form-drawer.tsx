import { useState } from "react"

import { ConfirmDialog } from "@/components/confirm-dialog"
import { Button } from "@/components/ui/button"
import {
  Sheet,
  SheetContent,
  SheetDescription,
  SheetFooter,
  SheetHeader,
  SheetTitle,
} from "@/components/ui/sheet"
import { Spinner } from "@/components/ui/spinner"

type FormDrawerProps = {
  open: boolean
  onOpenChange: (open: boolean) => void
  title: string
  description: string
  /** Id of the `<form>` rendered by `children`; the submit button in the footer targets it. */
  formId: string
  submitLabel: string
  pending: boolean
  /** True once the user changed something: closing then asks before discarding it. */
  dirty: boolean
  /** Called whenever the drawer is closed, to reset the form. */
  onClosed: () => void
  /** `close` closes without asking, e.g. after a successful save. */
  children: (api: { close: () => void }) => React.ReactNode
}

/** A right-hand drawer around a form that asks before throwing away unsaved input. */
export function FormDrawer({
  open,
  onOpenChange,
  title,
  description,
  formId,
  submitLabel,
  pending,
  dirty,
  onClosed,
  children,
}: FormDrawerProps) {
  const [confirmingClose, setConfirmingClose] = useState(false)

  function close() {
    setConfirmingClose(false)
    onOpenChange(false)
    onClosed()
  }

  function requestClose() {
    if (dirty) setConfirmingClose(true)
    else close()
  }

  return (
    <>
      <Sheet
        open={open}
        onOpenChange={(next) => (next ? onOpenChange(true) : requestClose())}
      >
        <SheetContent className="data-[side=right]:sm:max-w-md">
          <SheetHeader>
            <SheetTitle>{title}</SheetTitle>
            <SheetDescription>{description}</SheetDescription>
          </SheetHeader>
          <div className="flex-1 overflow-y-auto px-4">
            {children({ close })}
          </div>
          <SheetFooter>
            <Button type="submit" form={formId} disabled={pending}>
              {pending && <Spinner data-icon="inline-start" />}
              {submitLabel}
            </Button>
            <Button type="button" variant="outline" onClick={requestClose}>
              Cancel
            </Button>
          </SheetFooter>
        </SheetContent>
      </Sheet>

      <ConfirmDialog
        open={confirmingClose}
        onOpenChange={setConfirmingClose}
        title="Discard your changes?"
        description="What you filled in will be lost."
        confirmLabel="Discard"
        cancelLabel="Keep editing"
        onConfirm={close}
      />
    </>
  )
}
