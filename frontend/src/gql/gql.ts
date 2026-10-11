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
    "\n  query CheckInvitedUser($code: String!) {\n    checkInvitedUser(code: $code) {\n      nextAction\n      email\n      tenantName\n    }\n  }\n": typeof types.CheckInvitedUserDocument,
    "\n  mutation AcceptTenantInvitation($input: AcceptTenantInvitationInput!) {\n    acceptTenantInvitation(input: $input) {\n      tenant {\n        id\n        name\n      }\n    }\n  }\n": typeof types.AcceptTenantInvitationDocument,
    "\n  mutation RegisterUserFromInvitation(\n    $input: RegisterUserFromInvitationInput!\n  ) {\n    registerUserFromInvitation(input: $input) {\n      id\n    }\n  }\n": typeof types.RegisterUserFromInvitationDocument,
    "\n  mutation InvitationLogin($input: LoginInput!) {\n    login(input: $input) {\n      id\n    }\n  }\n": typeof types.InvitationLoginDocument,
    "\n  query Permissions {\n    permissions {\n      code\n      description\n      level\n    }\n  }\n": typeof types.PermissionsDocument,
    "\n  mutation CreateTenantRole($input: CreateTenantRoleInput!) {\n    createTenantRole(input: $input) {\n      tenantRole {\n        id\n        name\n      }\n    }\n  }\n": typeof types.CreateTenantRoleDocument,
    "\n  mutation UpdateTenantRole($input: UpdateTenantRoleInput!) {\n    updateTenantRole(input: $input) {\n      tenantRole {\n        id\n        name\n      }\n    }\n  }\n": typeof types.UpdateTenantRoleDocument,
    "\n  mutation InactivateTenantRole($input: TenantRoleIdInput!) {\n    inactivateTenantRole(input: $input) {\n      tenantRole {\n        id\n        active\n      }\n    }\n  }\n": typeof types.InactivateTenantRoleDocument,
    "\n  mutation ReactivateTenantRole($input: TenantRoleIdInput!) {\n    reactivateTenantRole(input: $input) {\n      tenantRole {\n        id\n        active\n      }\n    }\n  }\n": typeof types.ReactivateTenantRoleDocument,
    "\n  query TenantRoles($first: Int, $query: String, $active: Boolean) {\n    tenantRoles(first: $first, query: $query, active: $active) {\n      edges {\n        node {\n          id\n          name\n          active\n          builtIn\n          permissions {\n            code\n            description\n            level\n          }\n        }\n      }\n    }\n  }\n": typeof types.TenantRolesDocument,
    "\n  mutation InactivateTenant {\n    inactivateTenant {\n      tenant {\n        id\n        name\n        active\n        inactivatedAt\n      }\n    }\n  }\n": typeof types.InactivateTenantDocument,
    "\n  mutation ReactivateTenant {\n    reactivateTenant {\n      tenant {\n        id\n        name\n        active\n        inactivatedAt\n      }\n    }\n  }\n": typeof types.ReactivateTenantDocument,
    "\n  mutation CreateTenant($input: CreateTenantInput!) {\n    createTenant(input: $input) {\n      tenant {\n        id\n        name\n        active\n        inactivatedAt\n      }\n    }\n  }\n": typeof types.CreateTenantDocument,
    "\n  mutation UpdateTenant($input: UpdateTenantInput!) {\n    updateTenant(input: $input) {\n      tenant {\n        id\n        name\n        active\n        inactivatedAt\n      }\n    }\n  }\n": typeof types.UpdateTenantDocument,
    "\n  query MyTenants {\n    myTenants {\n      id\n      name\n      active\n      inactivatedAt\n      permissions\n    }\n  }\n": typeof types.MyTenantsDocument,
    "\n  query TenantInvitations(\n    $first: Int\n    $after: String\n    $status: TenantInvitationStatus\n    $query: String\n  ) {\n    tenantInvitations(\n      first: $first\n      after: $after\n      status: $status\n      query: $query\n    ) {\n      edges {\n        node {\n          id\n          email\n          status\n          expiresAt\n          roles {\n            id\n            name\n          }\n        }\n      }\n      pageInfo {\n        hasNextPage\n        endCursor\n      }\n    }\n  }\n": typeof types.TenantInvitationsDocument,
    "\n  mutation ResendInvitation($input: ResendInvitationInput!) {\n    resendInvitation(input: $input) {\n      invitation {\n        id\n        status\n        expiresAt\n      }\n    }\n  }\n": typeof types.ResendInvitationDocument,
    "\n  mutation RevokeInvitation($input: RevokeInvitationInput!) {\n    revokeInvitation(input: $input) {\n      invitation {\n        id\n        status\n      }\n    }\n  }\n": typeof types.RevokeInvitationDocument,
    "\n  mutation InviteUserToTenant($input: InviteUserToTenantInput!) {\n    inviteUserToTenant(input: $input) {\n      invitation {\n        id\n        email\n        status\n      }\n    }\n  }\n": typeof types.InviteUserToTenantDocument,
    "\n  mutation UpdateTenantUser($input: UpdateTenantUserInput!) {\n    updateTenantUser(input: $input) {\n      tenantUser {\n        roles {\n          id\n          name\n        }\n      }\n    }\n  }\n": typeof types.UpdateTenantUserDocument,
    "\n  query TenantUsers(\n    $first: Int\n    $after: String\n    $query: String\n    $active: Boolean\n  ) {\n    tenantUsers(first: $first, after: $after, query: $query, active: $active) {\n      edges {\n        node {\n          active\n          user {\n            id\n            name\n            email\n          }\n          roles {\n            id\n            name\n          }\n        }\n      }\n      pageInfo {\n        hasNextPage\n        endCursor\n      }\n    }\n  }\n": typeof types.TenantUsersDocument,
    "\n  mutation InactivateTenantUser($input: TenantUserIdInput!) {\n    inactivateTenantUser(input: $input) {\n      tenantUser {\n        active\n        inactivatedAt\n      }\n    }\n  }\n": typeof types.InactivateTenantUserDocument,
    "\n  mutation ReactivateTenantUser($input: TenantUserIdInput!) {\n    reactivateTenantUser(input: $input) {\n      tenantUser {\n        active\n        inactivatedAt\n      }\n    }\n  }\n": typeof types.ReactivateTenantUserDocument,
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
    "\n  query CheckInvitedUser($code: String!) {\n    checkInvitedUser(code: $code) {\n      nextAction\n      email\n      tenantName\n    }\n  }\n": types.CheckInvitedUserDocument,
    "\n  mutation AcceptTenantInvitation($input: AcceptTenantInvitationInput!) {\n    acceptTenantInvitation(input: $input) {\n      tenant {\n        id\n        name\n      }\n    }\n  }\n": types.AcceptTenantInvitationDocument,
    "\n  mutation RegisterUserFromInvitation(\n    $input: RegisterUserFromInvitationInput!\n  ) {\n    registerUserFromInvitation(input: $input) {\n      id\n    }\n  }\n": types.RegisterUserFromInvitationDocument,
    "\n  mutation InvitationLogin($input: LoginInput!) {\n    login(input: $input) {\n      id\n    }\n  }\n": types.InvitationLoginDocument,
    "\n  query Permissions {\n    permissions {\n      code\n      description\n      level\n    }\n  }\n": types.PermissionsDocument,
    "\n  mutation CreateTenantRole($input: CreateTenantRoleInput!) {\n    createTenantRole(input: $input) {\n      tenantRole {\n        id\n        name\n      }\n    }\n  }\n": types.CreateTenantRoleDocument,
    "\n  mutation UpdateTenantRole($input: UpdateTenantRoleInput!) {\n    updateTenantRole(input: $input) {\n      tenantRole {\n        id\n        name\n      }\n    }\n  }\n": types.UpdateTenantRoleDocument,
    "\n  mutation InactivateTenantRole($input: TenantRoleIdInput!) {\n    inactivateTenantRole(input: $input) {\n      tenantRole {\n        id\n        active\n      }\n    }\n  }\n": types.InactivateTenantRoleDocument,
    "\n  mutation ReactivateTenantRole($input: TenantRoleIdInput!) {\n    reactivateTenantRole(input: $input) {\n      tenantRole {\n        id\n        active\n      }\n    }\n  }\n": types.ReactivateTenantRoleDocument,
    "\n  query TenantRoles($first: Int, $query: String, $active: Boolean) {\n    tenantRoles(first: $first, query: $query, active: $active) {\n      edges {\n        node {\n          id\n          name\n          active\n          builtIn\n          permissions {\n            code\n            description\n            level\n          }\n        }\n      }\n    }\n  }\n": types.TenantRolesDocument,
    "\n  mutation InactivateTenant {\n    inactivateTenant {\n      tenant {\n        id\n        name\n        active\n        inactivatedAt\n      }\n    }\n  }\n": types.InactivateTenantDocument,
    "\n  mutation ReactivateTenant {\n    reactivateTenant {\n      tenant {\n        id\n        name\n        active\n        inactivatedAt\n      }\n    }\n  }\n": types.ReactivateTenantDocument,
    "\n  mutation CreateTenant($input: CreateTenantInput!) {\n    createTenant(input: $input) {\n      tenant {\n        id\n        name\n        active\n        inactivatedAt\n      }\n    }\n  }\n": types.CreateTenantDocument,
    "\n  mutation UpdateTenant($input: UpdateTenantInput!) {\n    updateTenant(input: $input) {\n      tenant {\n        id\n        name\n        active\n        inactivatedAt\n      }\n    }\n  }\n": types.UpdateTenantDocument,
    "\n  query MyTenants {\n    myTenants {\n      id\n      name\n      active\n      inactivatedAt\n      permissions\n    }\n  }\n": types.MyTenantsDocument,
    "\n  query TenantInvitations(\n    $first: Int\n    $after: String\n    $status: TenantInvitationStatus\n    $query: String\n  ) {\n    tenantInvitations(\n      first: $first\n      after: $after\n      status: $status\n      query: $query\n    ) {\n      edges {\n        node {\n          id\n          email\n          status\n          expiresAt\n          roles {\n            id\n            name\n          }\n        }\n      }\n      pageInfo {\n        hasNextPage\n        endCursor\n      }\n    }\n  }\n": types.TenantInvitationsDocument,
    "\n  mutation ResendInvitation($input: ResendInvitationInput!) {\n    resendInvitation(input: $input) {\n      invitation {\n        id\n        status\n        expiresAt\n      }\n    }\n  }\n": types.ResendInvitationDocument,
    "\n  mutation RevokeInvitation($input: RevokeInvitationInput!) {\n    revokeInvitation(input: $input) {\n      invitation {\n        id\n        status\n      }\n    }\n  }\n": types.RevokeInvitationDocument,
    "\n  mutation InviteUserToTenant($input: InviteUserToTenantInput!) {\n    inviteUserToTenant(input: $input) {\n      invitation {\n        id\n        email\n        status\n      }\n    }\n  }\n": types.InviteUserToTenantDocument,
    "\n  mutation UpdateTenantUser($input: UpdateTenantUserInput!) {\n    updateTenantUser(input: $input) {\n      tenantUser {\n        roles {\n          id\n          name\n        }\n      }\n    }\n  }\n": types.UpdateTenantUserDocument,
    "\n  query TenantUsers(\n    $first: Int\n    $after: String\n    $query: String\n    $active: Boolean\n  ) {\n    tenantUsers(first: $first, after: $after, query: $query, active: $active) {\n      edges {\n        node {\n          active\n          user {\n            id\n            name\n            email\n          }\n          roles {\n            id\n            name\n          }\n        }\n      }\n      pageInfo {\n        hasNextPage\n        endCursor\n      }\n    }\n  }\n": types.TenantUsersDocument,
    "\n  mutation InactivateTenantUser($input: TenantUserIdInput!) {\n    inactivateTenantUser(input: $input) {\n      tenantUser {\n        active\n        inactivatedAt\n      }\n    }\n  }\n": types.InactivateTenantUserDocument,
    "\n  mutation ReactivateTenantUser($input: TenantUserIdInput!) {\n    reactivateTenantUser(input: $input) {\n      tenantUser {\n        active\n        inactivatedAt\n      }\n    }\n  }\n": types.ReactivateTenantUserDocument,
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
/**
 * The graphql function is used to parse GraphQL queries into a document that can be used by GraphQL clients.
 */
export function graphql(source: "\n  query CheckInvitedUser($code: String!) {\n    checkInvitedUser(code: $code) {\n      nextAction\n      email\n      tenantName\n    }\n  }\n"): (typeof documents)["\n  query CheckInvitedUser($code: String!) {\n    checkInvitedUser(code: $code) {\n      nextAction\n      email\n      tenantName\n    }\n  }\n"];
/**
 * The graphql function is used to parse GraphQL queries into a document that can be used by GraphQL clients.
 */
export function graphql(source: "\n  mutation AcceptTenantInvitation($input: AcceptTenantInvitationInput!) {\n    acceptTenantInvitation(input: $input) {\n      tenant {\n        id\n        name\n      }\n    }\n  }\n"): (typeof documents)["\n  mutation AcceptTenantInvitation($input: AcceptTenantInvitationInput!) {\n    acceptTenantInvitation(input: $input) {\n      tenant {\n        id\n        name\n      }\n    }\n  }\n"];
/**
 * The graphql function is used to parse GraphQL queries into a document that can be used by GraphQL clients.
 */
export function graphql(source: "\n  mutation RegisterUserFromInvitation(\n    $input: RegisterUserFromInvitationInput!\n  ) {\n    registerUserFromInvitation(input: $input) {\n      id\n    }\n  }\n"): (typeof documents)["\n  mutation RegisterUserFromInvitation(\n    $input: RegisterUserFromInvitationInput!\n  ) {\n    registerUserFromInvitation(input: $input) {\n      id\n    }\n  }\n"];
/**
 * The graphql function is used to parse GraphQL queries into a document that can be used by GraphQL clients.
 */
export function graphql(source: "\n  mutation InvitationLogin($input: LoginInput!) {\n    login(input: $input) {\n      id\n    }\n  }\n"): (typeof documents)["\n  mutation InvitationLogin($input: LoginInput!) {\n    login(input: $input) {\n      id\n    }\n  }\n"];
/**
 * The graphql function is used to parse GraphQL queries into a document that can be used by GraphQL clients.
 */
export function graphql(source: "\n  query Permissions {\n    permissions {\n      code\n      description\n      level\n    }\n  }\n"): (typeof documents)["\n  query Permissions {\n    permissions {\n      code\n      description\n      level\n    }\n  }\n"];
/**
 * The graphql function is used to parse GraphQL queries into a document that can be used by GraphQL clients.
 */
export function graphql(source: "\n  mutation CreateTenantRole($input: CreateTenantRoleInput!) {\n    createTenantRole(input: $input) {\n      tenantRole {\n        id\n        name\n      }\n    }\n  }\n"): (typeof documents)["\n  mutation CreateTenantRole($input: CreateTenantRoleInput!) {\n    createTenantRole(input: $input) {\n      tenantRole {\n        id\n        name\n      }\n    }\n  }\n"];
/**
 * The graphql function is used to parse GraphQL queries into a document that can be used by GraphQL clients.
 */
export function graphql(source: "\n  mutation UpdateTenantRole($input: UpdateTenantRoleInput!) {\n    updateTenantRole(input: $input) {\n      tenantRole {\n        id\n        name\n      }\n    }\n  }\n"): (typeof documents)["\n  mutation UpdateTenantRole($input: UpdateTenantRoleInput!) {\n    updateTenantRole(input: $input) {\n      tenantRole {\n        id\n        name\n      }\n    }\n  }\n"];
/**
 * The graphql function is used to parse GraphQL queries into a document that can be used by GraphQL clients.
 */
export function graphql(source: "\n  mutation InactivateTenantRole($input: TenantRoleIdInput!) {\n    inactivateTenantRole(input: $input) {\n      tenantRole {\n        id\n        active\n      }\n    }\n  }\n"): (typeof documents)["\n  mutation InactivateTenantRole($input: TenantRoleIdInput!) {\n    inactivateTenantRole(input: $input) {\n      tenantRole {\n        id\n        active\n      }\n    }\n  }\n"];
/**
 * The graphql function is used to parse GraphQL queries into a document that can be used by GraphQL clients.
 */
export function graphql(source: "\n  mutation ReactivateTenantRole($input: TenantRoleIdInput!) {\n    reactivateTenantRole(input: $input) {\n      tenantRole {\n        id\n        active\n      }\n    }\n  }\n"): (typeof documents)["\n  mutation ReactivateTenantRole($input: TenantRoleIdInput!) {\n    reactivateTenantRole(input: $input) {\n      tenantRole {\n        id\n        active\n      }\n    }\n  }\n"];
/**
 * The graphql function is used to parse GraphQL queries into a document that can be used by GraphQL clients.
 */
export function graphql(source: "\n  query TenantRoles($first: Int, $query: String, $active: Boolean) {\n    tenantRoles(first: $first, query: $query, active: $active) {\n      edges {\n        node {\n          id\n          name\n          active\n          builtIn\n          permissions {\n            code\n            description\n            level\n          }\n        }\n      }\n    }\n  }\n"): (typeof documents)["\n  query TenantRoles($first: Int, $query: String, $active: Boolean) {\n    tenantRoles(first: $first, query: $query, active: $active) {\n      edges {\n        node {\n          id\n          name\n          active\n          builtIn\n          permissions {\n            code\n            description\n            level\n          }\n        }\n      }\n    }\n  }\n"];
/**
 * The graphql function is used to parse GraphQL queries into a document that can be used by GraphQL clients.
 */
export function graphql(source: "\n  mutation InactivateTenant {\n    inactivateTenant {\n      tenant {\n        id\n        name\n        active\n        inactivatedAt\n      }\n    }\n  }\n"): (typeof documents)["\n  mutation InactivateTenant {\n    inactivateTenant {\n      tenant {\n        id\n        name\n        active\n        inactivatedAt\n      }\n    }\n  }\n"];
/**
 * The graphql function is used to parse GraphQL queries into a document that can be used by GraphQL clients.
 */
export function graphql(source: "\n  mutation ReactivateTenant {\n    reactivateTenant {\n      tenant {\n        id\n        name\n        active\n        inactivatedAt\n      }\n    }\n  }\n"): (typeof documents)["\n  mutation ReactivateTenant {\n    reactivateTenant {\n      tenant {\n        id\n        name\n        active\n        inactivatedAt\n      }\n    }\n  }\n"];
/**
 * The graphql function is used to parse GraphQL queries into a document that can be used by GraphQL clients.
 */
export function graphql(source: "\n  mutation CreateTenant($input: CreateTenantInput!) {\n    createTenant(input: $input) {\n      tenant {\n        id\n        name\n        active\n        inactivatedAt\n      }\n    }\n  }\n"): (typeof documents)["\n  mutation CreateTenant($input: CreateTenantInput!) {\n    createTenant(input: $input) {\n      tenant {\n        id\n        name\n        active\n        inactivatedAt\n      }\n    }\n  }\n"];
/**
 * The graphql function is used to parse GraphQL queries into a document that can be used by GraphQL clients.
 */
export function graphql(source: "\n  mutation UpdateTenant($input: UpdateTenantInput!) {\n    updateTenant(input: $input) {\n      tenant {\n        id\n        name\n        active\n        inactivatedAt\n      }\n    }\n  }\n"): (typeof documents)["\n  mutation UpdateTenant($input: UpdateTenantInput!) {\n    updateTenant(input: $input) {\n      tenant {\n        id\n        name\n        active\n        inactivatedAt\n      }\n    }\n  }\n"];
/**
 * The graphql function is used to parse GraphQL queries into a document that can be used by GraphQL clients.
 */
export function graphql(source: "\n  query MyTenants {\n    myTenants {\n      id\n      name\n      active\n      inactivatedAt\n      permissions\n    }\n  }\n"): (typeof documents)["\n  query MyTenants {\n    myTenants {\n      id\n      name\n      active\n      inactivatedAt\n      permissions\n    }\n  }\n"];
/**
 * The graphql function is used to parse GraphQL queries into a document that can be used by GraphQL clients.
 */
export function graphql(source: "\n  query TenantInvitations(\n    $first: Int\n    $after: String\n    $status: TenantInvitationStatus\n    $query: String\n  ) {\n    tenantInvitations(\n      first: $first\n      after: $after\n      status: $status\n      query: $query\n    ) {\n      edges {\n        node {\n          id\n          email\n          status\n          expiresAt\n          roles {\n            id\n            name\n          }\n        }\n      }\n      pageInfo {\n        hasNextPage\n        endCursor\n      }\n    }\n  }\n"): (typeof documents)["\n  query TenantInvitations(\n    $first: Int\n    $after: String\n    $status: TenantInvitationStatus\n    $query: String\n  ) {\n    tenantInvitations(\n      first: $first\n      after: $after\n      status: $status\n      query: $query\n    ) {\n      edges {\n        node {\n          id\n          email\n          status\n          expiresAt\n          roles {\n            id\n            name\n          }\n        }\n      }\n      pageInfo {\n        hasNextPage\n        endCursor\n      }\n    }\n  }\n"];
/**
 * The graphql function is used to parse GraphQL queries into a document that can be used by GraphQL clients.
 */
export function graphql(source: "\n  mutation ResendInvitation($input: ResendInvitationInput!) {\n    resendInvitation(input: $input) {\n      invitation {\n        id\n        status\n        expiresAt\n      }\n    }\n  }\n"): (typeof documents)["\n  mutation ResendInvitation($input: ResendInvitationInput!) {\n    resendInvitation(input: $input) {\n      invitation {\n        id\n        status\n        expiresAt\n      }\n    }\n  }\n"];
/**
 * The graphql function is used to parse GraphQL queries into a document that can be used by GraphQL clients.
 */
export function graphql(source: "\n  mutation RevokeInvitation($input: RevokeInvitationInput!) {\n    revokeInvitation(input: $input) {\n      invitation {\n        id\n        status\n      }\n    }\n  }\n"): (typeof documents)["\n  mutation RevokeInvitation($input: RevokeInvitationInput!) {\n    revokeInvitation(input: $input) {\n      invitation {\n        id\n        status\n      }\n    }\n  }\n"];
/**
 * The graphql function is used to parse GraphQL queries into a document that can be used by GraphQL clients.
 */
export function graphql(source: "\n  mutation InviteUserToTenant($input: InviteUserToTenantInput!) {\n    inviteUserToTenant(input: $input) {\n      invitation {\n        id\n        email\n        status\n      }\n    }\n  }\n"): (typeof documents)["\n  mutation InviteUserToTenant($input: InviteUserToTenantInput!) {\n    inviteUserToTenant(input: $input) {\n      invitation {\n        id\n        email\n        status\n      }\n    }\n  }\n"];
/**
 * The graphql function is used to parse GraphQL queries into a document that can be used by GraphQL clients.
 */
export function graphql(source: "\n  mutation UpdateTenantUser($input: UpdateTenantUserInput!) {\n    updateTenantUser(input: $input) {\n      tenantUser {\n        roles {\n          id\n          name\n        }\n      }\n    }\n  }\n"): (typeof documents)["\n  mutation UpdateTenantUser($input: UpdateTenantUserInput!) {\n    updateTenantUser(input: $input) {\n      tenantUser {\n        roles {\n          id\n          name\n        }\n      }\n    }\n  }\n"];
/**
 * The graphql function is used to parse GraphQL queries into a document that can be used by GraphQL clients.
 */
export function graphql(source: "\n  query TenantUsers(\n    $first: Int\n    $after: String\n    $query: String\n    $active: Boolean\n  ) {\n    tenantUsers(first: $first, after: $after, query: $query, active: $active) {\n      edges {\n        node {\n          active\n          user {\n            id\n            name\n            email\n          }\n          roles {\n            id\n            name\n          }\n        }\n      }\n      pageInfo {\n        hasNextPage\n        endCursor\n      }\n    }\n  }\n"): (typeof documents)["\n  query TenantUsers(\n    $first: Int\n    $after: String\n    $query: String\n    $active: Boolean\n  ) {\n    tenantUsers(first: $first, after: $after, query: $query, active: $active) {\n      edges {\n        node {\n          active\n          user {\n            id\n            name\n            email\n          }\n          roles {\n            id\n            name\n          }\n        }\n      }\n      pageInfo {\n        hasNextPage\n        endCursor\n      }\n    }\n  }\n"];
/**
 * The graphql function is used to parse GraphQL queries into a document that can be used by GraphQL clients.
 */
export function graphql(source: "\n  mutation InactivateTenantUser($input: TenantUserIdInput!) {\n    inactivateTenantUser(input: $input) {\n      tenantUser {\n        active\n        inactivatedAt\n      }\n    }\n  }\n"): (typeof documents)["\n  mutation InactivateTenantUser($input: TenantUserIdInput!) {\n    inactivateTenantUser(input: $input) {\n      tenantUser {\n        active\n        inactivatedAt\n      }\n    }\n  }\n"];
/**
 * The graphql function is used to parse GraphQL queries into a document that can be used by GraphQL clients.
 */
export function graphql(source: "\n  mutation ReactivateTenantUser($input: TenantUserIdInput!) {\n    reactivateTenantUser(input: $input) {\n      tenantUser {\n        active\n        inactivatedAt\n      }\n    }\n  }\n"): (typeof documents)["\n  mutation ReactivateTenantUser($input: TenantUserIdInput!) {\n    reactivateTenantUser(input: $input) {\n      tenantUser {\n        active\n        inactivatedAt\n      }\n    }\n  }\n"];

export function graphql(source: string) {
  return (documents as any)[source] ?? {};
}

export type DocumentType<TDocumentNode extends DocumentNode<any, any>> = TDocumentNode extends DocumentNode<  infer TType,  any>  ? TType  : never;