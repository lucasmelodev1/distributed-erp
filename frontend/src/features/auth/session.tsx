import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
} from "react"
import { useClient } from "urql"

import { graphql } from "@/gql"
import { onUnauthorized } from "@/lib/graphql"

const MeQuery = graphql(`
  query Me {
    me {
      id
      name
      email
      profileIconUrl
    }
  }
`)

const LogoutMutation = graphql(`
  mutation Logout {
    logout {
      success
    }
  }
`)

export type SessionUser = {
  id: string
  name: string
  email: string
  profileIconUrl?: string | null
}

type Session = {
  /** True until the first `me` lookup finishes. */
  loading: boolean
  user: SessionUser | null
  /** Re-reads the session from the server, e.g. after login. */
  refresh: () => Promise<void>
  logout: () => Promise<void>
}

const SessionContext = createContext<Session | null>(null)

/** The session cookie is the source of truth: the user is always read from `me`. */
export function SessionProvider({ children }: { children: React.ReactNode }) {
  const client = useClient()
  const [loading, setLoading] = useState(true)
  const [user, setUser] = useState<SessionUser | null>(null)

  const refresh = useCallback(async () => {
    const result = await client
      .query(MeQuery, {}, { requestPolicy: "network-only" })
      .toPromise()
    setUser(result.data?.me ?? null)
    setLoading(false)
  }, [client])

  const logout = useCallback(async () => {
    await client.mutation(LogoutMutation, {}).toPromise()
    setUser(null)
  }, [client])

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- initial session lookup
    void refresh()
  }, [refresh])

  useEffect(() => onUnauthorized(() => setUser(null)), [])

  const value = useMemo(
    () => ({ loading, user, refresh, logout }),
    [loading, user, refresh, logout]
  )
  return <SessionContext value={value}>{children}</SessionContext>
}

export function useSession() {
  const session = useContext(SessionContext)
  if (!session)
    throw new Error("useSession must be used inside SessionProvider")
  return session
}
