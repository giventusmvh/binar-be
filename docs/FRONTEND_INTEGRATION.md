# Frontend API Integration Guide

> API integration documentation for Angular (Internal) and Android/Kotlin (Customer)

---

## Table of Contents

1. [Overview](#1-overview)
2. [Authentication](#2-authentication)
3. [Android App (Customer)](#3-android-app-customer)
4. [Angular Web (Internal Staff)](#4-angular-web-internal-staff)
5. [API Reference](#5-api-reference)

---

## 1. Overview

### Base URL

```
http://localhost:8080
```

### Platforms

| Platform          | Users                                             | Framework |
| ----------------- | ------------------------------------------------- | --------- |
| **Android App**   | Customer                                          | Kotlin    |
| **Web Dashboard** | Marketing, Branch Manager, Backoffice, SuperAdmin | Angular   |

### Authentication

All authenticated endpoints require:

```
Authorization: Bearer <jwt_token>
```

### Response Format

```json
{
  "success": true,
  "message": "Success message",
  "data": { ... },
  "timestamp": "2025-12-24T10:00:00"
}
```

---

## 2. Authentication

### 2.1 Login

**Endpoint:** `POST /api/auth/login`

```json
// Request
{
  "email": "user@email.com",
  "password": "password123"
}

// Response
{
  "success": true,
  "data": {
    "token": "eyJhbGc...",
    "tokenType": "Bearer",
    "userId": 1,
    "email": "user@email.com",
    "name": "User Name",
    "roles": ["CUSTOMER"],
    "permissions": ["LOAN_CREATE", "LOAN_READ", ...]
  }
}
```

**Frontend Implementation:**

- Store `token` securely (Android: EncryptedSharedPreferences, Angular: HttpOnly cookie or localStorage)
- Use `roles` to determine UI navigation
- Use `permissions` for feature flags

### 2.2 Register (Customer Only)

**Endpoint:** `POST /api/auth/register`

```json
// Request
{
  "name": "Customer Name",
  "email": "customer@email.com",
  "password": "password123"
}
```

### 2.3 Change Password

**Endpoint:** `POST /api/auth/change-password`  
**Auth:** Bearer Token

```json
// Request
{
  "currentPassword": "oldPassword",
  "newPassword": "newPassword123",
  "confirmPassword": "newPassword123"
}
```

---

## 3. Android App (Customer)

### User Flow

```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│  Register   │ →  │  Complete   │ →  │   Select    │ →  │   Submit    │ →  │    Track    │
│   Login     │    │   Profile   │    │   Plafond   │    │    Loan     │    │   Status    │
└─────────────┘    └─────────────┘    └─────────────┘    └─────────────┘    └─────────────┘
```

### 3.1 Profile Management

#### Get Profile

**Endpoint:** `GET /api/customer/profile`

```json
// Response
{
  "data": {
    "id": 1,
    "name": "John Doe",
    "email": "john@email.com",
    "profile": {
      "nik": "3201234567890001",
      "birthdate": "1990-05-15",
      "phoneNumber": "+6281234567890",
      "address": "Jl. Sudirman No. 123",
      "isComplete": true
    }
  }
}
```

**Android UI Tip:** Show warning banner if `isComplete = false`

#### Update Profile

**Endpoint:** `PUT /api/customer/profile`

```json
// Request
{
  "nik": "3201234567890001", // 16 digits
  "birthdate": "1990-05-15", // yyyy-MM-dd
  "phoneNumber": "+6281234567890",
  "address": "Jl. Sudirman No. 123, Jakarta"
}
```

**Validation:**

- NIK must be 16 digits
- All fields required for loan submission

---

### 3.2 Plafond (Credit Limit)

#### Get Products

**Endpoint:** `GET /api/products` (Public)

```json
// Response
{
  "data": [
    {
      "id": 1,
      "name": "BRONZE",
      "amount": 5000000,
      "tenor": 12,
      "interestRate": 12.0
    },
    {
      "id": 2,
      "name": "SILVER",
      "amount": 10000000,
      "tenor": 24,
      "interestRate": 10.0
    },
    {
      "id": 3,
      "name": "GOLD",
      "amount": 25000000,
      "tenor": 36,
      "interestRate": 8.5
    },
    {
      "id": 4,
      "name": "PLATINUM",
      "amount": 50000000,
      "tenor": 48,
      "interestRate": 7.0
    }
  ]
}
```

#### Select Plafond

**Endpoint:** `POST /api/customer/plafond`

```json
// Request
{ "productId": 1 }

// Response
{
  "data": {
    "id": 1,
    "product": {"id": 1, "name": "BRONZE", "amount": 5000000, ...},
    "originalAmount": 5000000,
    "remainingAmount": 5000000,
    "isActive": true
  }
}
```

**Android UI Tip:**

- Show product cards with tier benefits
- Disable selection if user already has active plafond

#### Get My Plafond

**Endpoint:** `GET /api/customer/plafond`

```json
// Response (active plafond exists)
{
  "data": {
    "originalAmount": 5000000,
    "remainingAmount": 2000000,  // Decreases after approved loans
    "isActive": true
  }
}

// Response (no active plafond / depleted)
{
  "success": false,
  "message": "You don't have an active plafond. Please select a plafond first."
}
```

**Android UI Tip:**

- Show progress bar: `remainingAmount / originalAmount`
- When `isActive = false` or error, prompt to select new plafond

---

### 3.3 Loan Application

#### Get Branches

**Endpoint:** `GET /api/branches` (Public)

```json
{
  "data": [
    { "id": 1, "code": "JKT", "location": "Jakarta" },
    { "id": 2, "code": "SBY", "location": "Surabaya" },
    { "id": 3, "code": "BDG", "location": "Bandung" }
  ]
}
```

#### Submit Loan

**Endpoint:** `POST /api/loans`

```json
// Request
{
  "branchId": 1,
  "amount": 3000000,
  "tenor": 6,
  "interestRate": 12.0
}

// Success Response
{
  "data": {
    "id": 1,
    "status": "SUBMITTED",
    "requestedAmount": 3000000,
    "requestedTenor": 6,
    "requestedRate": 12.0,
    ...
  }
}
```

**Validation Rules:**
| Field | Rule |
|-------|------|
| `amount` | ≤ `plafond.remainingAmount` |
| `tenor` | ≤ `plafond.product.tenor` |
| `interestRate` | ≥ `plafond.product.interestRate` |

**Error Examples:**

```json
{"message": "Requested amount exceeds remaining plafond. Remaining: Rp 2000000.00"}
{"message": "Please complete your profile before submitting a loan application."}
{"message": "Please select a plafond first before submitting a loan application."}
{"message": "You already have a pending loan application. Please wait until it is fully approved or rejected before submitting a new one."}
```

#### Get My Loans

**Endpoint:** `GET /api/loans`

```json
{
  "data": [
    {
      "id": 1,
      "requestedAmount": 3000000,
      "status": "SUBMITTED",
      "createdAt": "2025-12-24T10:00:00"
    }
  ]
}
```

#### Get Loan Detail

**Endpoint:** `GET /api/loans/{id}`

#### Get Loan History

**Endpoint:** `GET /api/loans/{id}/history`

```json
{
  "data": [
    { "status": "SUBMITTED", "note": "Loan submitted", "createdAt": "..." },
    {
      "status": "MARKETING_APPROVED",
      "approvedBy": "Marketing Jakarta",
      "createdAt": "..."
    }
  ]
}
```

**Android UI Tip:** Show timeline/stepper with status history

---

## 4. Angular Web (Internal Staff)

### Role-Based Navigation

| Role               | Dashboard Features                                          |
| ------------------ | ----------------------------------------------------------- |
| **MARKETING**      | View & approve SUBMITTED loans (own branch)                 |
| **BRANCH_MANAGER** | View & approve MARKETING_APPROVED loans (own branch)        |
| **BACKOFFICE**     | View & approve BRANCH_MANAGER_APPROVED loans (all branches) |
| **SUPERADMIN**     | User management, Role assignment, Permission management     |

### 4.1 Approval Flow

#### Get Pending Loans

**Endpoint:** `GET /api/approval/pending`

Returns loans filtered by role:

- **MARKETING:** `status = SUBMITTED`, own branch
- **BRANCH_MANAGER:** `status = MARKETING_APPROVED`, own branch
- **BACKOFFICE:** `status = BRANCH_MANAGER_APPROVED`, all branches

```json
{
  "data": [
    {
      "id": 1,
      "customerName": "John Doe",
      "customerNik": "3201234567890001",
      "requestedAmount": 3000000,
      "status": "SUBMITTED",
      "branch": { "code": "JKT", "location": "Jakarta" }
    }
  ]
}
```

#### Approve Loan

**Endpoint:** `POST /api/approval/{id}/approve`

```json
// Request (optional)
{ "note": "Documents verified" }

// Response
{
  "data": {
    "status": "MARKETING_APPROVED"  // or BRANCH_MANAGER_APPROVED, APPROVED
  }
}
```

**Status Transitions:**

```
SUBMITTED → MARKETING_APPROVED → BRANCH_MANAGER_APPROVED → APPROVED
```

#### Reject Loan

**Endpoint:** `POST /api/approval/{id}/reject`

```json
// Request
{ "note": "Insufficient documentation" }
```

---

### 4.2 SuperAdmin Features

#### Get All Users

**Endpoint:** `GET /api/admin/users`

#### Create Internal User

**Endpoint:** `POST /api/admin/users`

```json
// Request
{
  "name": "New Marketing",
  "email": "new.marketing@loan.com",
  "password": "password123",
  "roleId": 2,
  "branchId": 1
}
```

#### Assign Role

**Endpoint:** `POST /api/admin/users/{userId}/roles`

```json
{ "roleId": 2 }
```

#### Remove Role

**Endpoint:** `DELETE /api/admin/users/{userId}/roles/{roleId}`

#### Get Roles

**Endpoint:** `GET /api/admin/roles`

#### Update Role Permissions

**Endpoint:** `PUT /api/admin/roles/{roleId}/permissions`

```json
{ "permissionIds": [1, 2, 3, 5, 9] }
```

#### Update User

**Endpoint:** `PUT /api/admin/users/{userId}`

```json
// Request (all fields optional)
{
  "name": "Updated Name",
  "email": "updated@email.com",
  "branchId": 2
}
```

#### Update User Status

**Endpoint:** `PATCH /api/admin/users/{userId}/status`

```json
{ "isActive": false }
```

> **Note:** Deactivated users cannot login.

---

## 5. API Reference

### Status Codes

| Code | Meaning                              |
| ---- | ------------------------------------ |
| 200  | Success                              |
| 201  | Created                              |
| 400  | Bad Request (validation error)       |
| 401  | Unauthorized (invalid/missing token) |
| 403  | Forbidden (no permission)            |
| 404  | Not Found                            |
| 409  | Conflict (duplicate)                 |

### Loan Status Values

| Status                    | Description                               |
| ------------------------- | ----------------------------------------- |
| `SUBMITTED`               | Initial, waiting for Marketing            |
| `MARKETING_APPROVED`      | Approved by Marketing, waiting for BM     |
| `MARKETING_REJECTED`      | Rejected by Marketing (terminal)          |
| `BRANCH_MANAGER_APPROVED` | Approved by BM, waiting for Backoffice    |
| `BRANCH_MANAGER_REJECTED` | Rejected by BM (terminal)                 |
| `APPROVED`                | Final approval, loan disbursed (terminal) |
| `REJECTED`                | Final rejection (terminal)                |

### Test Accounts

| Email                    | Password        | Role           |
| ------------------------ | --------------- | -------------- |
| `john.doe@email.com`     | `customer123`   | CUSTOMER       |
| `marketing.jkt@loan.com` | `marketing123`  | MARKETING      |
| `bm.jkt@loan.com`        | `bm123`         | BRANCH_MANAGER |
| `backoffice@loan.com`    | `backoffice123` | BACKOFFICE     |
| `admin@loan.com`         | `admin123`      | SUPERADMIN     |

---

_Generated: 2025-12-24_
