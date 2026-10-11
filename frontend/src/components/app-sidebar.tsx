import { ChevronRightIcon, UsersIcon } from "lucide-react"
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
import { TOP_ROUTES, USER_ROUTES } from "@/features/access/routes"
import { useCan } from "@/features/access/use-can"
import { useSession } from "@/features/auth/session"

export function AppSidebar({ ...props }: React.ComponentProps<typeof Sidebar>) {
  const { user } = useSession()
  const { pathname } = useLocation()
  const can = useCan()
  const userRoutes = USER_ROUTES.filter(({ permission }) => can(permission))
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
            {TOP_ROUTES.map(({ path, label, icon: Icon }) => (
              <SidebarMenuItem key={path}>
                <NavLink to={path}>
                  {({ isActive }) => (
                    <SidebarMenuButton
                      isActive={isActive}
                      tooltip={label}
                      render={<span />}
                    >
                      <Icon />
                      <span>{label}</span>
                    </SidebarMenuButton>
                  )}
                </NavLink>
              </SidebarMenuItem>
            ))}
            {userRoutes.length > 0 && (
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
                    {userRoutes.map(({ path, label }) => (
                      <SidebarMenuSubItem key={path}>
                        <SidebarMenuSubButton
                          isActive={
                            path === "/users"
                              ? pathname === path
                              : pathname.startsWith(path)
                          }
                          render={<NavLink to={path} end={path === "/users"} />}
                        >
                          <span>{label}</span>
                        </SidebarMenuSubButton>
                      </SidebarMenuSubItem>
                    ))}
                  </SidebarMenuSub>
                </CollapsibleContent>
              </Collapsible>
            )}
          </SidebarMenu>
        </SidebarGroup>
      </SidebarContent>
      <SidebarFooter>{user && <NavUser user={user} />}</SidebarFooter>
      <SidebarRail />
    </Sidebar>
  )
}
