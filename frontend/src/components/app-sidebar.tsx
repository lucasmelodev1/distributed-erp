import {
  BuildingIcon,
  ChevronRightIcon,
  LayoutDashboardIcon,
  UsersIcon,
} from "lucide-react"
import { NavLink, useLocation } from "react-router"

import { NavUser } from "@/components/nav-user"
import {
  Collapsible,
  CollapsibleContent,
  CollapsibleTrigger,
} from "@/components/ui/collapsible"
import {
  Sidebar,
  SidebarContent,
  SidebarFooter,
  SidebarGroup,
  SidebarGroupLabel,
  SidebarHeader,
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
  SidebarMenuSub,
  SidebarMenuSubButton,
  SidebarMenuSubItem,
  SidebarRail,
} from "@/components/ui/sidebar"
import { useSession } from "@/features/auth/session"

export function AppSidebar({ ...props }: React.ComponentProps<typeof Sidebar>) {
  const { user } = useSession()
  const { pathname } = useLocation()
  const inUsers = pathname.startsWith("/users")
  return (
    <Sidebar collapsible="icon" {...props}>
      <SidebarHeader>
        <div className="px-2 py-1.5 text-sm font-semibold group-data-[collapsible=icon]:hidden">
          Multi-tenant template
        </div>
      </SidebarHeader>
      <SidebarContent>
        <SidebarGroup>
          <SidebarGroupLabel>Platform</SidebarGroupLabel>
          <SidebarMenu>
            <SidebarMenuItem>
              <NavLink to="/dashboard">
                {({ isActive }) => (
                  <SidebarMenuButton
                    isActive={isActive}
                    tooltip="Dashboard"
                    render={<span />}
                  >
                    <LayoutDashboardIcon />
                    <span>Dashboard</span>
                  </SidebarMenuButton>
                )}
              </NavLink>
            </SidebarMenuItem>
            <SidebarMenuItem>
              <NavLink to="/companies">
                {({ isActive }) => (
                  <SidebarMenuButton
                    isActive={isActive}
                    tooltip="Companies"
                    render={<span />}
                  >
                    <BuildingIcon />
                    <span>Companies</span>
                  </SidebarMenuButton>
                )}
              </NavLink>
            </SidebarMenuItem>
            <Collapsible defaultOpen={inUsers} render={<SidebarMenuItem />}>
              <CollapsibleTrigger
                render={
                  <SidebarMenuButton
                    tooltip="Users"
                    isActive={inUsers}
                    className="group/users"
                  />
                }
              >
                <UsersIcon />
                <span>Users</span>
                <ChevronRightIcon className="ml-auto transition-transform group-data-[panel-open]/users:rotate-90" />
              </CollapsibleTrigger>
              <CollapsibleContent>
                <SidebarMenuSub>
                  <SidebarMenuSubItem>
                    <SidebarMenuSubButton
                      isActive={pathname === "/users"}
                      render={<NavLink to="/users" end />}
                    >
                      <span>Users</span>
                    </SidebarMenuSubButton>
                  </SidebarMenuSubItem>
                  <SidebarMenuSubItem>
                    <SidebarMenuSubButton
                      isActive={pathname.startsWith("/users/invitations")}
                      render={<NavLink to="/users/invitations" />}
                    >
                      <span>Invitations</span>
                    </SidebarMenuSubButton>
                  </SidebarMenuSubItem>
                  <SidebarMenuSubItem>
                    <SidebarMenuSubButton
                      isActive={pathname.startsWith("/users/roles")}
                      render={<NavLink to="/users/roles" />}
                    >
                      <span>Roles</span>
                    </SidebarMenuSubButton>
                  </SidebarMenuSubItem>
                </SidebarMenuSub>
              </CollapsibleContent>
            </Collapsible>
          </SidebarMenu>
        </SidebarGroup>
      </SidebarContent>
      <SidebarFooter>{user && <NavUser user={user} />}</SidebarFooter>
      <SidebarRail />
    </Sidebar>
  )
}
