# Postman API Testing Guide

> Complete testing flows for Loan Application Backend API

---

## Table of Contents

1. [Setup](#1-setup)
2. [Authentication Flow](#2-authentication-flow)
3. [Customer Flow](#3-customer-flow)
4. [Loan Application Flow](#4-loan-application-flow)
5. [Approval Workflow](#5-approval-workflow)
6. [SuperAdmin Flow](#6-superadmin-flow)

---

## 1. Setup

### Base URL

```
http://localhost:8080
```

### Environment Variables

Create a Postman environment with these variables:

| Variable           | Initial Value           | Description                         |
| ------------------ | ----------------------- | ----------------------------------- |
| `base_url`         | `http://localhost:8080` | API base URL                        |
| `customer_token`   | (empty)                 | Auto-set after customer login       |
| `marketing_token`  | (empty)                 | Auto-set after marketing login      |
| `bm_token`         | (empty)                 | Auto-set after branch manager login |
| `backoffice_token` | (empty)                 | Auto-set after backoffice login     |
| `admin_token`      | (empty)                 | Auto-set after superadmin login     |
| `loan_id`          | (empty)                 | Auto-set after loan submission      |

### Pre-seeded Test Accounts

| Email                    | Password      | Role                  | Branch  |
| ------------------------ | ------------- | --------------------- | ------- |
| `john.doe@email.com`     | `customer123` | CUSTOMER              | -       |
| `jane.smith@email.com`   | `customer123` | CUSTOMER (no profile) | -       |
| `marketing.jkt@loan.com` | `internal123` | MARKETING             | Jakarta |
| `bm.jkt@loan.com`        | `internal123` | BRANCH_MANAGER        | Jakarta |
| `backoffice@loan.com`    | `internal123` | BACKOFFICE            | -       |
| `admin@loan.com`         | `admin123`    | SUPERADMIN            | -       |

---

## 2. Authentication Flow

### 2.1 Register New Customer

**Endpoint:** `POST /api/auth/register`  
**Auth:** None

```json
// Request Body
{
  "name": "Test Customer",
  "email": "test.customer@email.com",
  "password": "password123"
}
```

```json
// Success Response (201 Created)
{
  "success": true,
  "message": "Registration successful",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "userId": 10,
    "email": "test.customer@email.com",
    "name": "Test Customer",
    "roles": ["CUSTOMER"]
  },
  "timestamp": "2025-12-22T10:00:00"
}
```

**Post-request Script (to save token):**

```javascript
if (pm.response.code === 201) {
  var jsonData = pm.response.json();
  pm.environment.set("customer_token", jsonData.data.token);
}
```

---

### 2.2 Login

**Endpoint:** `POST /api/auth/login`  
**Auth:** None

```json
// Request Body - Customer Login
{
  "email": "john.doe@email.com",
  "password": "customer123"
}
```

```json
// Request Body - Marketing Login
{
  "email": "marketing.jkt@loan.com",
  "password": "internal123"
}
```

```json
// Request Body - Branch Manager Login
{
  "email": "bm.jkt@loan.com",
  "password": "internal123"
}
```

```json
// Request Body - Backoffice Login
{
  "email": "backoffice@loan.com",
  "password": "internal123"
}
```

```json
// Request Body - SuperAdmin Login
{
  "email": "admin@loan.com",
  "password": "admin123"
}
```

```json
// Success Response (200 OK)
{
  "success": true,
  "message": "Login successful",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "userId": 1,
    "email": "john.doe@email.com",
    "name": "John Doe",
    "roles": ["CUSTOMER"]
  },
  "timestamp": "2025-12-22T10:00:00"
}
```

---

### 2.3 Change Password

**Endpoint:** `POST /api/auth/change-password`  
**Auth:** Bearer Token (any authenticated user)

```json
// Request Body
{
  "currentPassword": "customer123",
  "newPassword": "newPassword456",
  "confirmPassword": "newPassword456"
}
```

```json
// Success Response (200 OK)
{
  "success": true,
  "message": "Password changed successfully",
  "timestamp": "2025-12-22T10:00:00"
}
```

```json
// Error Response - Wrong current password (400)
{
  "success": false,
  "message": "Current password is incorrect",
  "timestamp": "2025-12-22T10:00:00"
}
```

```json
// Error Response - Passwords don't match (400)
{
  "success": false,
  "message": "New password and confirm password do not match",
  "timestamp": "2025-12-22T10:00:00"
}
```

---

## 3. Customer Flow

### 3.1 Get Profile

**Endpoint:** `GET /api/customer/profile`  
**Auth:** Bearer Token (CUSTOMER)

```json
// Success Response (200 OK)
{
  "success": true,
  "message": "Success",
  "data": {
    "id": 1,
    "name": "John Doe",
    "email": "john.doe@email.com",
    "userType": "CUSTOMER",
    "isActive": true,
    "roles": ["CUSTOMER"],
    "profile": {
      "nik": "3201234567890001",
      "birthdate": "1990-05-15",
      "phoneNumber": "+6281234567890",
      "address": "Jl. Sudirman No. 123, Jakarta",
      "isComplete": true
    }
  },
  "timestamp": "2025-12-22T10:00:00"
}
```

---

### 3.2 Update Profile

**Endpoint:** `PUT /api/customer/profile`  
**Auth:** Bearer Token (CUSTOMER)

```json
// Request Body
{
  "nik": "3201234567890001",
  "birthdate": "1990-05-15",
  "phoneNumber": "+6281234567890",
  "address": "Jl. Sudirman No. 123, Jakarta"
}
```

```json
// Success Response (200 OK)
{
  "success": true,
  "message": "Profile updated successfully",
  "data": {
    "nik": "3201234567890001",
    "birthdate": "1990-05-15",
    "phoneNumber": "+6281234567890",
    "address": "Jl. Sudirman No. 123, Jakarta",
    "isComplete": true
  },
  "timestamp": "2025-12-22T10:00:00"
}
```

---

### 3.3 Get Products (Public)

**Endpoint:** `GET /api/products`  
**Auth:** None

```json
// Success Response (200 OK)
{
  "success": true,
  "message": "Success",
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
  ],
  "timestamp": "2025-12-22T10:00:00"
}
```

---

### 3.4 Get Branches (Public)

**Endpoint:** `GET /api/branches`  
**Auth:** None

```json
// Success Response (200 OK)
{
  "success": true,
  "message": "Success",
  "data": [
    {
      "id": 1,
      "code": "JKT",
      "name": "Jakarta"
    },
    {
      "id": 2,
      "code": "SBY",
      "name": "Surabaya"
    },
    {
      "id": 3,
      "code": "BDG",
      "name": "Bandung"
    }
  ],
  "timestamp": "2025-12-22T10:00:00"
}
```

---

## 4. Loan Application Flow

### 4.1 Submit Loan Application

**Endpoint:** `POST /api/loans`  
**Auth:** Bearer Token (CUSTOMER)

> ⚠️ **Prerequisite:** Customer profile must be complete (NIK, birthdate, phone, address)

```json
// Request Body
{
  "productId": 2,
  "branchId": 1
}
```

```json
// Success Response (201 Created)
{
  "success": true,
  "message": "Loan application submitted successfully",
  "data": {
    "id": 1,
    "customerId": 1,
    "customerName": "John Doe",
    "productId": 2,
    "productName": "SILVER",
    "amount": 10000000,
    "tenor": 24,
    "interestRate": 10.0,
    "branchId": 1,
    "branchName": "Jakarta",
    "status": "SUBMITTED",
    "createdAt": "2025-12-22T10:00:00"
  },
  "timestamp": "2025-12-22T10:00:00"
}
```

```json
// Error Response - Incomplete Profile (400)
{
  "success": false,
  "message": "Profile must be complete before submitting loan application",
  "timestamp": "2025-12-22T10:00:00"
}
```

**Post-request Script:**

```javascript
if (pm.response.code === 201) {
  var jsonData = pm.response.json();
  pm.environment.set("loan_id", jsonData.data.id);
}
```

---

### 4.2 Get My Loans

**Endpoint:** `GET /api/loans`  
**Auth:** Bearer Token (CUSTOMER)

```json
// Success Response (200 OK)
{
  "success": true,
  "message": "Success",
  "data": [
    {
      "id": 1,
      "customerId": 1,
      "customerName": "John Doe",
      "productId": 2,
      "productName": "SILVER",
      "amount": 10000000,
      "tenor": 24,
      "interestRate": 10.0,
      "branchId": 1,
      "branchName": "Jakarta",
      "status": "SUBMITTED",
      "createdAt": "2025-12-22T10:00:00"
    }
  ],
  "timestamp": "2025-12-22T10:00:00"
}
```

---

### 4.3 Get Loan by ID

**Endpoint:** `GET /api/loans/{id}`  
**Auth:** Bearer Token (CUSTOMER - owner only)

```json
// Success Response (200 OK)
{
  "success": true,
  "message": "Success",
  "data": {
    "id": 1,
    "customerId": 1,
    "customerName": "John Doe",
    "productId": 2,
    "productName": "SILVER",
    "amount": 10000000,
    "tenor": 24,
    "interestRate": 10.0,
    "branchId": 1,
    "branchName": "Jakarta",
    "status": "SUBMITTED",
    "createdAt": "2025-12-22T10:00:00"
  },
  "timestamp": "2025-12-22T10:00:00"
}
```

---

### 4.4 Get Loan History

**Endpoint:** `GET /api/loans/{id}/history`  
**Auth:** Bearer Token (CUSTOMER - owner only)

```json
// Success Response (200 OK)
{
  "success": true,
  "message": "Success",
  "data": [
    {
      "id": 1,
      "status": "SUBMITTED",
      "note": "Loan application submitted",
      "approvedBy": "John Doe",
      "approvedByRole": "CUSTOMER",
      "approvedByBranchName": null,
      "createdAt": "2025-12-22T10:00:00"
    },
    {
      "id": 2,
      "status": "MARKETING_APPROVED",
      "note": "Documents verified",
      "approvedBy": "Marketing Jakarta",
      "approvedByRole": "MARKETING",
      "approvedByBranchName": "Jakarta",
      "createdAt": "2025-12-22T10:15:00"
    }
  ],
  "timestamp": "2025-12-22T10:00:00"
}
```

---

## 5. Approval Workflow

### Complete Approval Flow Diagram

```
┌─────────────┐     ┌────────────────────┐     ┌──────────────────────────┐     ┌──────────┐
│  SUBMITTED  │────▶│ MARKETING_APPROVED │────▶│ BRANCH_MANAGER_APPROVED  │────▶│ APPROVED │
└─────────────┘     └────────────────────┘     └──────────────────────────┘     └──────────┘
       │                     │                            │                           │
       │                     │                            │                           │
       ▼                     ▼                            ▼                           │
┌──────────────────┐ ┌──────────────────┐    ┌──────────────────────────┐            │
│ MARKETING_REJECTED│ │ BM_REJECTED     │    │ REJECTED                 │◀───────────┘
└──────────────────┘ └──────────────────┘    └──────────────────────────┘
                                                        │
                                              ┌─────────▼─────────┐
                                              │     RETURNED      │
                                              │ (back to SUBMITTED)│
                                              └───────────────────┘
```

---

### 5.1 Get Pending Loans

**Endpoint:** `GET /api/approval/pending`  
**Auth:** Bearer Token (MARKETING, BRANCH_MANAGER, or BACKOFFICE)

> - **MARKETING** sees `SUBMITTED` loans for their branch
> - **BRANCH_MANAGER** sees `MARKETING_APPROVED` loans for their branch
> - **BACKOFFICE** sees `BRANCH_MANAGER_APPROVED` loans from all branches

```json
// Success Response (200 OK)
{
  "success": true,
  "message": "Success",
  "data": [
    {
      "id": 1,
      "customerId": 1,
      "customerName": "John Doe",
      "productId": 2,
      "productName": "SILVER",
      "amount": 10000000,
      "tenor": 24,
      "interestRate": 10.0,
      "branchId": 1,
      "branchName": "Jakarta",
      "status": "SUBMITTED",
      "createdAt": "2025-12-22T10:00:00"
    }
  ],
  "timestamp": "2025-12-22T10:00:00"
}
```

---

### 5.2 Approve Loan

**Endpoint:** `POST /api/approval/{id}/approve`  
**Auth:** Bearer Token (MARKETING, BRANCH_MANAGER, or BACKOFFICE)

```json
// Request Body (optional)
{
  "note": "All documents verified and approved"
}
```

```json
// Success Response (200 OK) - Marketing Approval
{
  "success": true,
  "message": "Loan approved successfully",
  "data": {
    "id": 1,
    "status": "MARKETING_APPROVED",
    ...
  },
  "timestamp": "2025-12-22T10:00:00"
}
```

```json
// Success Response (200 OK) - Branch Manager Approval
{
  "success": true,
  "message": "Loan approved successfully",
  "data": {
    "id": 1,
    "status": "BRANCH_MANAGER_APPROVED",
    ...
  },
  "timestamp": "2025-12-22T10:00:00"
}
```

```json
// Success Response (200 OK) - Backoffice (Final) Approval
{
  "success": true,
  "message": "Loan approved successfully",
  "data": {
    "id": 1,
    "status": "APPROVED",
    ...
  },
  "timestamp": "2025-12-22T10:00:00"
}
```

---

### 5.3 Reject Loan

**Endpoint:** `POST /api/approval/{id}/reject`  
**Auth:** Bearer Token (MARKETING, BRANCH_MANAGER, or BACKOFFICE)

```json
// Request Body (required)
{
  "note": "Insufficient income documentation"
}
```

```json
// Success Response (200 OK)
{
  "success": true,
  "message": "Loan rejected",
  "data": {
    "id": 1,
    "status": "MARKETING_REJECTED",
    ...
  },
  "timestamp": "2025-12-22T10:00:00"
}
```

---

### 5.4 Return Loan (Backoffice Only)

**Endpoint:** `POST /api/approval/{id}/return`  
**Auth:** Bearer Token (BACKOFFICE only)

```json
// Request Body (required)
{
  "note": "Please verify customer address again"
}
```

```json
// Success Response (200 OK)
{
  "success": true,
  "message": "Loan returned for revision",
  "data": {
    "id": 1,
    "status": "RETURNED",
    ...
  },
  "timestamp": "2025-12-22T10:00:00"
}
```

---

## 6. SuperAdmin Flow

### 6.1 Get All Users

**Endpoint:** `GET /api/admin/users`  
**Auth:** Bearer Token (SUPERADMIN)

```json
// Success Response (200 OK)
{
  "success": true,
  "message": "Success",
  "data": [
    {
      "id": 1,
      "name": "Super Admin",
      "email": "admin@loan.com",
      "userType": "INTERNAL",
      "isActive": true,
      "roles": ["SUPERADMIN"],
      "branchId": null,
      "branchName": null
    },
    {
      "id": 2,
      "name": "Marketing Jakarta",
      "email": "marketing.jkt@loan.com",
      "userType": "INTERNAL",
      "isActive": true,
      "roles": ["MARKETING"],
      "branchId": 1,
      "branchName": "Jakarta"
    }
  ],
  "timestamp": "2025-12-22T10:00:00"
}
```

---

### 6.2 Get User by ID

**Endpoint:** `GET /api/admin/users/{id}`  
**Auth:** Bearer Token (SUPERADMIN)

```json
// Success Response (200 OK)
{
  "success": true,
  "message": "Success",
  "data": {
    "id": 2,
    "name": "Marketing Jakarta",
    "email": "marketing.jkt@loan.com",
    "userType": "INTERNAL",
    "isActive": true,
    "roles": ["MARKETING"],
    "branchId": 1,
    "branchName": "Jakarta"
  },
  "timestamp": "2025-12-22T10:00:00"
}
```

---

### 6.3 Assign Role to User

**Endpoint:** `POST /api/admin/users/{id}/roles`  
**Auth:** Bearer Token (SUPERADMIN)

```json
// Request Body
{
  "roleId": 2
}
```

```json
// Success Response (200 OK)
{
  "success": true,
  "message": "Role assigned successfully",
  "data": {
    "id": 7,
    "name": "Internal User",
    "email": "internal@loan.com",
    "userType": "INTERNAL",
    "isActive": true,
    "roles": ["MARKETING"],
    "branchId": 1,
    "branchName": "Jakarta"
  },
  "timestamp": "2025-12-22T10:00:00"
}
```

---

### 6.4 Remove Role from User

**Endpoint:** `DELETE /api/admin/users/{userId}/roles/{roleId}`  
**Auth:** Bearer Token (SUPERADMIN)

```json
// Success Response (200 OK)
{
  "success": true,
  "message": "Role removed successfully",
  "data": {
    "id": 7,
    "name": "Internal User",
    "email": "internal@loan.com",
    "userType": "INTERNAL",
    "isActive": true,
    "roles": [],
    "branchId": 1,
    "branchName": "Jakarta"
  },
  "timestamp": "2025-12-22T10:00:00"
}
```

---

### 6.5 Get All Roles

**Endpoint:** `GET /api/admin/roles`  
**Auth:** Bearer Token (SUPERADMIN)

```json
// Success Response (200 OK)
{
  "success": true,
  "message": "Success",
  "data": [
    {
      "id": 1,
      "name": "SUPERADMIN",
      "permissions": ["*"]
    },
    {
      "id": 2,
      "name": "MARKETING",
      "permissions": [
        "LOAN_READ_BRANCH",
        "LOAN_APPROVE_MARKETING",
        "LOAN_REJECT"
      ]
    },
    {
      "id": 3,
      "name": "BRANCH_MANAGER",
      "permissions": [
        "LOAN_READ_BRANCH",
        "LOAN_APPROVE_BRANCH_MANAGER",
        "LOAN_REJECT"
      ]
    },
    {
      "id": 4,
      "name": "BACKOFFICE",
      "permissions": [
        "LOAN_READ_ALL",
        "LOAN_APPROVE_BACKOFFICE",
        "LOAN_REJECT",
        "LOAN_RETURN"
      ]
    },
    {
      "id": 5,
      "name": "CUSTOMER",
      "permissions": ["LOAN_CREATE", "LOAN_READ", "PRODUCT_READ", "BRANCH_READ"]
    }
  ],
  "timestamp": "2025-12-22T10:00:00"
}
```

---

### 6.6 Update Role Permissions

**Endpoint:** `PUT /api/admin/roles/{id}/permissions`  
**Auth:** Bearer Token (SUPERADMIN)

```json
// Request Body
{
  "permissionIds": [1, 2, 3, 4]
}
```

```json
// Success Response (200 OK)
{
  "success": true,
  "message": "Role permissions updated successfully",
  "data": {
    "id": 2,
    "name": "MARKETING",
    "permissions": [
      "LOAN_READ_BRANCH",
      "LOAN_APPROVE_MARKETING",
      "LOAN_REJECT",
      "LOAN_CREATE"
    ]
  },
  "timestamp": "2025-12-22T10:00:00"
}
```

---

### 6.7 Get All Permissions

**Endpoint:** `GET /api/admin/permissions`  
**Auth:** Bearer Token (SUPERADMIN)

```json
// Success Response (200 OK)
{
  "success": true,
  "message": "Success",
  "data": [
    { "id": 1, "name": "USER_CREATE" },
    { "id": 2, "name": "USER_READ" },
    { "id": 3, "name": "USER_UPDATE" },
    { "id": 4, "name": "USER_DELETE" },
    { "id": 5, "name": "LOAN_CREATE" },
    { "id": 6, "name": "LOAN_READ" },
    { "id": 7, "name": "LOAN_READ_BRANCH" },
    { "id": 8, "name": "LOAN_READ_ALL" },
    { "id": 9, "name": "LOAN_APPROVE_MARKETING" },
    { "id": 10, "name": "LOAN_APPROVE_BRANCH_MANAGER" },
    { "id": 11, "name": "LOAN_APPROVE_BACKOFFICE" },
    { "id": 12, "name": "LOAN_REJECT" },
    { "id": 13, "name": "LOAN_RETURN" },
    { "id": 14, "name": "PRODUCT_READ" },
    { "id": 15, "name": "BRANCH_READ" },
    { "id": 16, "name": "ROLE_CREATE" },
    { "id": 17, "name": "ROLE_READ" },
    { "id": 18, "name": "ROLE_UPDATE" },
    { "id": 19, "name": "ROLE_DELETE" },
    { "id": 20, "name": "PERMISSION_READ" }
  ],
  "timestamp": "2025-12-22T10:00:00"
}
```

---

## Quick Test Scenarios

### Scenario 1: Complete Loan Approval Flow

1. **Login as Customer**: `john.doe@email.com` → Save token
2. **Submit Loan**: `POST /api/loans` with productId: 2, branchId: 1 → Save loan_id
3. **Login as Marketing**: `marketing.jkt@loan.com` → Save token
4. **Get Pending**: `GET /api/approval/pending` → Should see the loan
5. **Approve**: `POST /api/approval/{loan_id}/approve`
6. **Login as Branch Manager**: `bm.jkt@loan.com` → Save token
7. **Get Pending**: `GET /api/approval/pending` → Should see the loan
8. **Approve**: `POST /api/approval/{loan_id}/approve`
9. **Login as Backoffice**: `backoffice@loan.com` → Save token
10. **Get Pending**: `GET /api/approval/pending` → Should see the loan
11. **Approve**: `POST /api/approval/{loan_id}/approve` → Status becomes APPROVED
12. **Login as Customer**: Verify loan status via `GET /api/loans/{loan_id}`

### Scenario 2: Incomplete Profile Rejection

1. **Login as Jane Smith**: `jane.smith@email.com` (has empty profile)
2. **Try Submit Loan**: `POST /api/loans` → Should get 400 error
3. **Update Profile**: `PUT /api/customer/profile` with complete data
4. **Retry Submit Loan**: Should succeed now

### Scenario 3: Change Password

1. **Login**: Any user
2. **Change Password**: `POST /api/auth/change-password`
3. **Try Login with Old Password**: Should fail (401)
4. **Login with New Password**: Should succeed

---

_Documentation generated: 2025-12-22_
