import { cacheExchange, Client, fetchExchange, mapExchange } from "urql"

import { errorCode } from "@/lib/errors"

type UnauthorizedListener = () => void
const listeners = new Set<UnauthorizedListener>()

/** Called when any operation comes back UNAUTHORIZED (e.g. the session expired). */
export function onUnauthorized(listener: UnauthorizedListener) {
  listeners.add(listener)
  return () => {
    listeners.delete(listener)
  }
}

// The API always answers HTTP 200; failures are in `errors[].extensions.code`.
export function createClient() {
  return new Client({
    url: "/graphql",
    exchanges: [
      cacheExchange,
      mapExchange({
        onError(error) {
          if (errorCode(error) === "UNAUTHORIZED") {
            listeners.forEach((listener) => listener())
          }
        },
      }),
      fetchExchange,
    ],
    fetchOptions: { credentials: "include" },
  })
}
