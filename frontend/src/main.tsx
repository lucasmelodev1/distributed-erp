import { StrictMode } from "react"
import { createRoot } from "react-dom/client"
import { Provider } from "urql"

import "./index.css"
import App from "./App.tsx"
import { ThemeProvider } from "@/components/theme-provider.tsx"
import { Toaster } from "@/components/ui/toast.tsx"
import { TooltipProvider } from "@/components/ui/tooltip.tsx"
import { SessionProvider } from "@/features/auth/session.tsx"
import { createClient } from "@/lib/graphql.ts"

const client = createClient()

createRoot(document.getElementById("root")!).render(
  <StrictMode>
    <ThemeProvider>
      <Provider value={client}>
        <SessionProvider>
          <TooltipProvider>
            <Toaster>
              <App />
            </Toaster>
          </TooltipProvider>
        </SessionProvider>
      </Provider>
    </ThemeProvider>
  </StrictMode>
)
