import type { CodegenConfig } from "@graphql-codegen/cli"

// The backend's schema files are the source of truth; there is no copy.
const config: CodegenConfig = {
  schema: "../src/main/resources/schema/*.graphqls",
  documents: ["src/**/*.{ts,tsx}", "!src/gql/**"],
  ignoreNoDocuments: true,
  generates: {
    "./src/gql/": {
      preset: "client",
      config: {
        useTypeImports: true,
        scalars: { UUID: "string", DateTime: "string" },
      },
    },
  },
}

export default config
