import {
  BuildingIcon,
  LayoutDashboardIcon,
  MailIcon,
  ShieldIcon,
  UsersIcon,
} from "lucide-react"

import { PERMISSIONS, type Permission } from "@/features/access/permissions"

export type NavRoute = {
  path: string
  label: string
  icon: typeof UsersIcon
  /** Needed in the selected company; absent means every signed-in user. */
  permission?: Permission
}

/** The pages behind a permission. The sidebar and the route guards both read this one list. */
export const USER_ROUTES: (NavRoute & { permission: Permission })[] = [
  {
    path: "/users",
    label: "Users",
    icon: UsersIcon,
    permission: PERMISSIONS.tenantUsersRead,
  },
  {
    path: "/users/invitations",
    label: "Invitations",
    icon: MailIcon,
    permission: PERMISSIONS.tenantsInvite,
  },
  {
    path: "/users/roles",
    label: "Roles",
    icon: ShieldIcon,
    permission: PERMISSIONS.tenantRolesRead,
  },
]

export const TOP_ROUTES: NavRoute[] = [
  { path: "/dashboard", label: "Dashboard", icon: LayoutDashboardIcon },
  { path: "/companies", label: "Companies", icon: BuildingIcon },
]
