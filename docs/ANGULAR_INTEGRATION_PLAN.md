# Implementation Plan - Backend to Angular Integration

This plan outlines the integration of the existing Spring Boot backend (`binar-be`) with a new Angular frontend application. The goal is to build a Landing Page ("ehefin") and Role-Based Dashboards for Marketing, Branch Manager, Backoffice, and Superadmin.

## User Review Required

> [!IMPORTANT] > **Assumption**: The Angular frontend does not currently exist or will be created/extended in a separate repository (e.g., `binar-fe`). This plan assumes starting fresh or adding major modules to an existing Angular setup.

> [!NOTE] > **Roles & Permissions**: The backed uses RBAC (Role Based Access Control) with specific permissions (e.g., `LOAN_READ_BRANCH`, `LOAN_APPROVE_MARKETING`). The frontend must handle these granular permissions for UI visibility.

## Architecture Overview

**Frontend Stack**: Angular (Latest), Standalone Components, Signals (for state), TailwindCSS (for styling).

### Core Modules

1.  **Auth Module**: Login, Register, Forgot Password.
2.  **Public Module**: Landing Page ("ehefin"), Products.
3.  **Admin Module**: Superadmin User/Role Management.
4.  **Workplace Module**: Shared Dashboard for Marketing, Branch Manager, Backoffice (Approval Workflows).

### Security

- **Guards**: `AuthGuard` (Login check), `RoleGuard` (Role/Permission check).
- **Interceptors**: `JwtInterceptor` (Attach Bearer token), `ErrorInterceptor` (Handle 401/403).

## Proposed Changes (Frontend)

### 1. Project Setup & Configuration

#### [NEW] `src/environments/environment.ts`

- Define `apiUrl` pointing to the Spring Boot backend (e.g., `http://localhost:8080/api`).

#### [NEW] Services

- `AuthService`: Connect to `/api/auth/**`. Handle Token storage (LocalStorage).
- `UserService`: Connect to `/api/admin/users/**`.
- `LoanService`: Connect to `/api/loans/**` and `/api/approval/**`.

### 2. Landing Page ("ehefin")

**Target Audience**: Customers / Public.
**Features**:

- **Hero Section**: Branding.
- **Loan Simulation** (Optional/Static for now).
- **Login/Register Buttons**: Navigate to Auth pages.
- **Dashboard Link**: If logged in, go to `/dashboard`.

### 3. Dashboards Strategy

Since Marketing, Branch Manager, and Backoffice share "Approval" workflows, they will rely on a shared **Workplace Component** that adapts based on permissions.

#### A. Marketing Dashboard

- **Role**: `MARKETING` (inferred).
- **Permission**: `LOAN_APPROVE_MARKETING`.
- **Features**:
  - View Pending Loans (filtered by Marketing stage).
  - Approve/Reject Applicants.

#### B. Branch Manager Dashboard

- **Role**: `BRANCH_MANAGER` (inferred).
- **Permission**: `LOAN_APPROVE_BRANCH_MANAGER`.
- **Features**:
  - View Pending Loans (Branch specific).
  - Higher level approval.

#### C. Backoffice Dashboard

- **Role**: `BACKOFFICE` (inferred).
- **Permission**: `LOAN_APPROVE_BACKOFFICE`.
- **Features**:
  - Final verification/disbursement.

#### D. Superadmin Dashboard

- **Role**: `SUPERADMIN`.
- **Path**: `/admin`.
- **Features**:
  - **User Management**: List users (`GET /api/admin/users`), Create User (`POST /api/admin/users`).
  - **Role Management**: Assign Roles (`POST /api/admin/users/{id}/roles`).

## Implementation Steps

### Phase 1: Foundation

1.  Initialize Angular App (if needed) & Install Tailwind.
2.  Create `AuthService` & `JwtInterceptor`.
3.  Implement Login & Register Pages.

### Phase 2: Superadmin

1.  Create `UserManagementComponent`.
2.  Implement "Create Internal User" form (for creating Marketing/Branch Manager accounts).

### Phase 3: Workflow/Approvals

1.  Create `LoanService`.
2.  Implement `ApprovalQueueComponent`: Table displaying `GET /api/approval/pending`.
3.  Implement `LoanDetailComponent`: Show `GET /api/loans/{id}` details.
4.  Add Action Buttons (Approve/Reject) visible based on `CanActivate` or `*ngIf="hasPermission(...)"`.

### Phase 4: Landing Page

1.  Design "ehefin" Landing Page.
2.  Integrate "Apply Loan" flow for Customers.

## API Reference & Data Models

> [!TIP]
> Use these JSON structures to generate your Angular Interfaces/Types.

### Authentication (`/api/auth`)

#### [POST] `/api/auth/login`

**Request**:

```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

#### [POST] `/api/auth/register`

**Request**:

```json
{
  "name": "John Doe",
  "email": "john@example.com",
  "password": "securePassword"
}
```

#### Auth Response (Login/Register)

```json
{
  "token": "eyJh...",
  "tokenType": "Bearer",
  "userId": 123,
  "email": "user@example.com",
  "name": "User Name",
  "roles": ["ROLE_CUSTOMER"],
  "permissions": ["LOAN_CREATE", "LOAN_READ"]
}
```

### Loan Management (`/api/loans`)

#### [POST] `/api/loans` (Submit Application)

**Request**:

```json
{
  "branchId": 1,
  "amount": 5000000,
  "tenor": 12,
  "interestRate": 2.5
}
```

#### Loan Response

```json
{
  "id": 101,
  "customerName": "John Doe",
  "requestedAmount": 5000000,
  "requestedTenor": 12,
  "status": "PENDING_MARKETING",
  "createdAt": "2023-10-25T10:00:00"
}
```

### Approval Workflow Permissions

The frontend `WorkplaceComponent` should conditionally render buttons based on `user.permissions`:

- **Marketing Approval**: `LOAN_APPROVE_MARKETING`
- **Branch Manager Approval**: `LOAN_APPROVE_BRANCH_MANAGER`
- **Backoffice Approval**: `LOAN_APPROVE_BACKOFFICE`
- **Rejection**: `LOAN_REJECT` (Available to all approvers)

## Verification Plan

### Automated Tests

- **Unit Tests (Jasmine/Karma)**:
  - `AuthService`: Mock HTTP requests to verify Token handling.
  - `RoleGuard`: Verify routing blocks unauthorized users.
- **E2E (Cypress/Playwright - Recommended)**:
  - Flow: Register Customer -> Login -> Apply Loan -> Logout.
  - Flow: Login Superadmin -> Create Marketing User.
  - Flow: Login Marketing User -> Approve Loan.

### Manual Verification

1.  **Customer Flow**:
    - Register as new user on Landing Page.
    - Login.
    - Submit Loan Application.
2.  **Superadmin Flow**:
    - Login as Superadmin.
    - Create a "Marketing" user.
3.  **Approval Flow**:
    - Login as the new Marketing user.
    - Check "Pending Approvals" list.
    - Approve the Customer's loan.
    - Verify status change.
