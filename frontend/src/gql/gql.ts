/* eslint-disable */
import * as types from './graphql';
import type { TypedDocumentNode as DocumentNode } from '@graphql-typed-document-node/core';

/**
 * Map of all GraphQL operations in the project.
 *
 * This map has several performance disadvantages:
 * 1. It is not tree-shakeable, so it will include all operations in the project.
 * 2. It is not minifiable, so the string of a GraphQL query will be multiple times inside the bundle.
 * 3. It does not support dead code elimination, so it will add unused operations.
 *
 * Therefore it is highly recommended to use the babel or swc plugin for production.
 * Learn more about it here: https://the-guild.dev/graphql/codegen/plugins/presets/preset-client#reducing-bundle-size
 */
type Documents = {
    "\n  mutation RequestPasswordReset($input: RequestPasswordResetInput!) {\n    requestPasswordReset(input: $input) {\n      authTokenId\n    }\n  }\n": typeof types.RequestPasswordResetDocument,
    "\n  mutation VerifyPasswordReset($input: VerifyPasswordResetInput!) {\n    verifyPasswordReset(input: $input) {\n      token\n    }\n  }\n": typeof types.VerifyPasswordResetDocument,
    "\n  mutation ResetPassword($input: ResetPasswordInput!) {\n    resetPassword(input: $input) {\n      success\n    }\n  }\n": typeof types.ResetPasswordDocument,
    "\n  mutation Login($input: LoginInput!) {\n    login(input: $input) {\n      id\n    }\n  }\n": typeof types.LoginDocument,
    "\n  mutation RequestEmailVerification($input: RequestEmailVerificationInput!) {\n    requestEmailVerification(input: $input) {\n      authTokenId\n    }\n  }\n": typeof types.RequestEmailVerificationDocument,
    "\n  mutation VerifyEmail($input: VerifyEmailInput!) {\n    verifyEmail(input: $input) {\n      token\n    }\n  }\n": typeof types.VerifyEmailDocument,
    "\n  mutation RegisterUser($input: RegisterUserInput!) {\n    registerUser(input: $input) {\n      id\n    }\n  }\n": typeof types.RegisterUserDocument,
    "\n  query Me {\n    me {\n      id\n      name\n      email\n      profileIconUrl\n    }\n  }\n": typeof types.MeDocument,
    "\n  mutation Logout {\n    logout {\n      success\n    }\n  }\n": typeof types.LogoutDocument,
};
const documents: Documents = {
    "\n  mutation RequestPasswordReset($input: RequestPasswordResetInput!) {\n    requestPasswordReset(input: $input) {\n      authTokenId\n    }\n  }\n": types.RequestPasswordResetDocument,
    "\n  mutation VerifyPasswordReset($input: VerifyPasswordResetInput!) {\n    verifyPasswordReset(input: $input) {\n      token\n    }\n  }\n": types.VerifyPasswordResetDocument,
    "\n  mutation ResetPassword($input: ResetPasswordInput!) {\n    resetPassword(input: $input) {\n      success\n    }\n  }\n": types.ResetPasswordDocument,
    "\n  mutation Login($input: LoginInput!) {\n    login(input: $input) {\n      id\n    }\n  }\n": types.LoginDocument,
    "\n  mutation RequestEmailVerification($input: RequestEmailVerificationInput!) {\n    requestEmailVerification(input: $input) {\n      authTokenId\n    }\n  }\n": types.RequestEmailVerificationDocument,
    "\n  mutation VerifyEmail($input: VerifyEmailInput!) {\n    verifyEmail(input: $input) {\n      token\n    }\n  }\n": types.VerifyEmailDocument,
    "\n  mutation RegisterUser($input: RegisterUserInput!) {\n    registerUser(input: $input) {\n      id\n    }\n  }\n": types.RegisterUserDocument,
    "\n  query Me {\n    me {\n      id\n      name\n      email\n      profileIconUrl\n    }\n  }\n": types.MeDocument,
    "\n  mutation Logout {\n    logout {\n      success\n    }\n  }\n": types.LogoutDocument,
};

/**
 * The graphql function is used to parse GraphQL queries into a document that can be used by GraphQL clients.
 *
 *
 * @example
 * ```ts
 * const query = graphql(`query GetUser($id: ID!) { user(id: $id) { name } }`);
 * ```
 *
 * The query argument is unknown!
 * Please regenerate the types.
 */
export function graphql(source: string): unknown;

/**
 * The graphql function is used to parse GraphQL queries into a document that can be used by GraphQL clients.
 */
export function graphql(source: "\n  mutation RequestPasswordReset($input: RequestPasswordResetInput!) {\n    requestPasswordReset(input: $input) {\n      authTokenId\n    }\n  }\n"): (typeof documents)["\n  mutation RequestPasswordReset($input: RequestPasswordResetInput!) {\n    requestPasswordReset(input: $input) {\n      authTokenId\n    }\n  }\n"];
/**
 * The graphql function is used to parse GraphQL queries into a document that can be used by GraphQL clients.
 */
export function graphql(source: "\n  mutation VerifyPasswordReset($input: VerifyPasswordResetInput!) {\n    verifyPasswordReset(input: $input) {\n      token\n    }\n  }\n"): (typeof documents)["\n  mutation VerifyPasswordReset($input: VerifyPasswordResetInput!) {\n    verifyPasswordReset(input: $input) {\n      token\n    }\n  }\n"];
/**
 * The graphql function is used to parse GraphQL queries into a document that can be used by GraphQL clients.
 */
export function graphql(source: "\n  mutation ResetPassword($input: ResetPasswordInput!) {\n    resetPassword(input: $input) {\n      success\n    }\n  }\n"): (typeof documents)["\n  mutation ResetPassword($input: ResetPasswordInput!) {\n    resetPassword(input: $input) {\n      success\n    }\n  }\n"];
/**
 * The graphql function is used to parse GraphQL queries into a document that can be used by GraphQL clients.
 */
export function graphql(source: "\n  mutation Login($input: LoginInput!) {\n    login(input: $input) {\n      id\n    }\n  }\n"): (typeof documents)["\n  mutation Login($input: LoginInput!) {\n    login(input: $input) {\n      id\n    }\n  }\n"];
/**
 * The graphql function is used to parse GraphQL queries into a document that can be used by GraphQL clients.
 */
export function graphql(source: "\n  mutation RequestEmailVerification($input: RequestEmailVerificationInput!) {\n    requestEmailVerification(input: $input) {\n      authTokenId\n    }\n  }\n"): (typeof documents)["\n  mutation RequestEmailVerification($input: RequestEmailVerificationInput!) {\n    requestEmailVerification(input: $input) {\n      authTokenId\n    }\n  }\n"];
/**
 * The graphql function is used to parse GraphQL queries into a document that can be used by GraphQL clients.
 */
export function graphql(source: "\n  mutation VerifyEmail($input: VerifyEmailInput!) {\n    verifyEmail(input: $input) {\n      token\n    }\n  }\n"): (typeof documents)["\n  mutation VerifyEmail($input: VerifyEmailInput!) {\n    verifyEmail(input: $input) {\n      token\n    }\n  }\n"];
/**
 * The graphql function is used to parse GraphQL queries into a document that can be used by GraphQL clients.
 */
export function graphql(source: "\n  mutation RegisterUser($input: RegisterUserInput!) {\n    registerUser(input: $input) {\n      id\n    }\n  }\n"): (typeof documents)["\n  mutation RegisterUser($input: RegisterUserInput!) {\n    registerUser(input: $input) {\n      id\n    }\n  }\n"];
/**
 * The graphql function is used to parse GraphQL queries into a document that can be used by GraphQL clients.
 */
export function graphql(source: "\n  query Me {\n    me {\n      id\n      name\n      email\n      profileIconUrl\n    }\n  }\n"): (typeof documents)["\n  query Me {\n    me {\n      id\n      name\n      email\n      profileIconUrl\n    }\n  }\n"];
/**
 * The graphql function is used to parse GraphQL queries into a document that can be used by GraphQL clients.
 */
export function graphql(source: "\n  mutation Logout {\n    logout {\n      success\n    }\n  }\n"): (typeof documents)["\n  mutation Logout {\n    logout {\n      success\n    }\n  }\n"];

export function graphql(source: string) {
  return (documents as any)[source] ?? {};
}

export type DocumentType<TDocumentNode extends DocumentNode<any, any>> = TDocumentNode extends DocumentNode<  infer TType,  any>  ? TType  : never;