import { Outlet, useLocation } from "react-router"

import { AppSidebar } from "@/components/app-sidebar"
import { Separator } from "@/components/ui/separator"
import {
  SidebarInset,
  SidebarProvider,
  SidebarTrigger,
} from "@/components/ui/sidebar"
import { CompaniesProvider } from "@/features/tenants/tenant-context"

function titleFor(pathname: string) {
  if (pathname.startsWith("/companies")) return "Companies"
  if (pathname.startsWith("/users/roles")) return "Roles"
  if (pathname.startsWith("/users/invitations")) return "Invitations"
  if (pathname.startsWith("/users")) return "Users"
  return "Dashboard"
}

export function DashboardLayout() {
  const { pathname } = useLocation()
  return (
    <CompaniesProvider>
      <SidebarProvider>
        <AppSidebar />
        <SidebarInset>
          <header className="flex h-16 shrink-0 items-center gap-2">
            <div className="flex items-center gap-2 px-4">
              <SidebarTrigger className="-ml-1" />
              <Separator
                orientation="vertical"
                className="mr-2 data-vertical:h-4 data-vertical:self-auto"
              />
              <span className="text-sm font-medium">{titleFor(pathname)}</span>
            </div>
          </header>
          <div className="flex flex-1 flex-col gap-4 p-4 pt-0">
            <Outlet />
          </div>
        </SidebarInset>
      </SidebarProvider>
    </CompaniesProvider>
  )
}
