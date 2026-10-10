import { BrowserRouter, Navigate, Route, Routes } from "react-router"

import { PublicOnly, RequireAuth } from "@/features/auth/guards"
import { ForgotPasswordPage } from "@/features/auth/forgot-password-page"
import { LoginPage } from "@/features/auth/login-page"
import { RegisterPage } from "@/features/auth/register-page"
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
          </Route>
        </Route>
        <Route path="/" element={<Navigate to="/dashboard" replace />} />
        <Route path="*" element={<NotFoundPage />} />
      </Routes>
    </BrowserRouter>
  )
}

export default App
