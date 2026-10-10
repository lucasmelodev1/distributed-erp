import type { CombinedError } from "urql"

export type ApiErrorCode =
  "UNAUTHORIZED" | "FORBIDDEN" | "BAD_REQUEST" | "NOT_FOUND" | "INTERNAL_ERROR"

export function errorCode(error?: CombinedError): ApiErrorCode | undefined {
  return error?.graphQLErrors[0]?.extensions?.code as ApiErrorCode | undefined
}

/** The server's message for the first error, or a generic one for network failures. */
export function errorMessage(error?: CombinedError): string | undefined {
  if (!error) return undefined
  if (error.networkError) return "Could not reach the server. Try again."
  const code = errorCode(error)
  if (code === "INTERNAL_ERROR") return "Something went wrong. Try again."
  return error.graphQLErrors[0]?.message ?? error.message
}
