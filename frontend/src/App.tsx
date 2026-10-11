import { BrowserRouter, Navigate, Route, Routes } from "react-router"

import { AcceptInvitationPage } from "@/features/invitations/accept-invitation-page"
import { PublicOnly, RequireAuth } from "@/features/auth/guards"
import { ForgotPasswordPage } from "@/features/auth/forgot-password-page"
import { LoginPage } from "@/features/auth/login-page"
import { RegisterPage } from "@/features/auth/register-page"
import { CompaniesPage } from "@/features/tenants/companies-page"
import { CompanyFormPage } from "@/features/tenants/company-form-page"
import { InvitationsPage } from "@/features/users/invitations-page"
import { RolesPage } from "@/features/roles/roles-page"
import { UsersPage } from "@/features/users/users-page"
import { DashboardLayout } from "@/routes/dashboard-layout"
import { DashboardPage } from "@/routes/dashboard-page"
import { NotFoundPage } from "@/routes/not-found-page"

export function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route element={<PublicOnly />}>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route path="/forgot-password" element={<ForgotPasswordPage />} />
        </Route>
        <Route element={<RequireAuth />}>
          <Route element={<DashboardLayout />}>
            <Route path="/dashboard" element={<DashboardPage />} />
            <Route path="/companies" element={<CompaniesPage />} />
            <Route path="/companies/new" element={<CompanyFormPage />} />
            <Route path="/companies/:id/edit" element={<CompanyFormPage />} />
            <Route path="/users" element={<UsersPage />} />
            <Route path="/users/roles" element={<RolesPage />} />
            <Route path="/users/invitations" element={<InvitationsPage />} />
          </Route>
        </Route>
        {/* Open to signed-in and signed-out visitors, so outside the guards. */}
        <Route path="/accept-invitation" element={<AcceptInvitationPage />} />
        <Route path="/" element={<Navigate to="/dashboard" replace />} />
        <Route path="*" element={<NotFoundPage />} />
      </Routes>
    </BrowserRouter>
  )
}

export default App
