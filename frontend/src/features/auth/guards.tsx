import { Navigate, Outlet, useLocation } from "react-router"

import { Spinner } from "@/components/ui/spinner"
import { useSession } from "@/features/auth/session"

function FullPageSpinner() {
  return (
    <div className="flex min-h-svh items-center justify-center">
      <Spinner />
    </div>
  )
}

/** Authentication only; permissions are checked by `features/access` (RequirePermission). */
export function RequireAuth() {
  const { loading, user } = useSession()
  const location = useLocation()
  if (loading) return <FullPageSpinner />
  if (!user) return <Navigate to="/login" replace state={{ from: location }} />
  return <Outlet />
}

/** Login, registration and recovery make no sense for a signed-in user. */
export function PublicOnly() {
  const { loading, user } = useSession()
  if (loading) return <FullPageSpinner />
  if (user) return <Navigate to="/dashboard" replace />
  return <Outlet />
}
