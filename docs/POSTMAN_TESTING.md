# Postman API Testing Guide

> Complete testing flows for Loan Application Backend API

---

## Table of Contents

### 📱 Mobile App (Customer)

1. [Setup](#1-setup)
2. [Authentication Flow](#2-authentication-flow) - Register, Login, Password Reset
3. [Customer Flow](#3-customer-flow) - Profile Management
4. [Plafond Selection Flow](#4-plafond-selection-flow) - Credit Limit Selection
5. [Loan Application Flow](#5-loan-application-flow) - Submit & Track Loans

### 💻 Website Dashboard (Internal Staff)

6. [Approval Workflow](#6-approval-workflow) - Marketing, BM, Backoffice
7. [SuperAdmin Flow](#7-superadmin-flow) - User & Role Management
8. [Shared Staff Operations](#8-shared-staff-operations)
9. [File Access](#9-file-access)

---

## Platform Overview

> Dokumentasi ini dibagi menjadi 2 bagian berdasarkan platform akses.

### 📱 Mobile App - Customer Only

| Feature            | Endpoints                                           |
| ------------------ | --------------------------------------------------- |
| **Registration**   | `POST /api/auth/register`                           |
| **Login/Logout**   | `POST /api/auth/login`, `/logout`                   |
| **Password Reset** | `POST /api/auth/forgot-password`, `/reset-password` |
| **FCM Token**      | `POST /api/customer/fcm-token`                      |
| **Profile**        | `GET/PUT /api/customer/profile`                     |
| **Plafond**        | `GET/POST /api/customer/plafond`                    |
| **Loans**          | `GET/POST /api/loans`, `/loans/{id}`                |
| **Public Data**    | `GET /api/products`, `/api/branches`                |

### 💻 Website Dashboard - Internal Staff

| Role           | Endpoints                                            |
| -------------- | ---------------------------------------------------- |
| **All Staff**  | `POST /api/auth/login`, `GET /api/approval/*`        |
| **Marketing**  | `POST /api/approval/{id}/approve/reject`             |
| **BM**         | `POST /api/approval/{id}/approve/reject`             |
| **Backoffice** | `POST /api/approval/{id}/approve/reject`             |
| **SuperAdmin** | `/api/admin/*` (Users, Roles, Permissions, Branches) |

---

## 1. Setup

### Base URL

```
http://localhost:8080
```

### Environment Variables

Create a Postman environment with these variables:

**📱 Mobile App Variables:**

| Variable         | Initial Value           | Description               |
| ---------------- | ----------------------- | ------------------------- |
| `base_url`       | `http://localhost:8080` | API base URL              |
| `customer_token` | (empty)                 | Auto-set after login      |
| `loan_id`        | (empty)                 | Auto-set after submission |

**💻 Website Dashboard Variables:**

| Variable           | Initial Value           | Description                         |
| ------------------ | ----------------------- | ----------------------------------- |
| `base_url`         | `http://localhost:8080` | API base URL                        |
| `marketing_token`  | (empty)                 | Auto-set after marketing login      |
| `bm_token`         | (empty)                 | Auto-set after branch manager login |
| `backoffice_token` | (empty)                 | Auto-set after backoffice login     |
| `admin_token`      | (empty)                 | Auto-set after superadmin login     |

### Pre-seeded Test Accounts

**📱 Mobile App (Customer):**

| Email                  | Password      | Role                  |
| ---------------------- | ------------- | --------------------- |
| `john.doe@email.com`   | `customer123` | CUSTOMER              |
| `jane.smith@email.com` | `customer123` | CUSTOMER (no profile) |

**💻 Website Dashboard (Internal Staff):**

| Email                    | Password        | Role           | Branch  |
| ------------------------ | --------------- | -------------- | ------- |
| `marketing.jkt@loan.com` | `marketing123`  | MARKETING      | Jakarta |
| `bm.jkt@loan.com`        | `bm123`         | BRANCH_MANAGER | Jakarta |
| `backoffice@loan.com`    | `backoffice123` | BACKOFFICE     | -       |
| `admin@loan.com`         | `admin123`      | SUPERADMIN     | -       |

---

## 2. Authentication Flow [📱 MOBILE / 💻 WEBSITE]

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
    "roles": ["CUSTOMER"],
    "permissions": [
      "LOAN_CREATE",
      "LOAN_READ",
      "PRODUCT_READ",
      "BRANCH_READ",
      "PROFILE_READ",
      "PROFILE_UPDATE",
      "PLAFOND_READ",
      "PLAFOND_SELECT"
    ]
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
  "password": "marketing123"
}
```

```json
// Request Body - Branch Manager Login
{
  "email": "bm.jkt@loan.com",
  "password": "bm123"
}
```

```json
// Request Body - Backoffice Login
{
  "email": "backoffice@loan.com",
  "password": "backoffice123"
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
    "roles": ["CUSTOMER"],
    "permissions": [
      "LOAN_CREATE",
      "LOAN_READ",
      "PRODUCT_READ",
      "BRANCH_READ",
      "PROFILE_READ",
      "PROFILE_UPDATE",
      "PLAFOND_READ",
      "PLAFOND_SELECT"
    ]
  },
  "timestamp": "2025-12-22T10:00:00"
}
```

---

### 2.3 Forgot Password

**Endpoint:** `POST /api/auth/forgot-password`  
**Auth:** None

> Sends password reset email via Mailtrap. Always returns success for security.

```json
// Request Body
{
  "email": "john.doe@email.com"
}
```

```json
// Success Response (200 OK)
{
  "success": true,
  "message": "If the email exists, a password reset link has been sent",
  "timestamp": "2025-12-29T10:00:00"
}
```

---

### 2.4 Reset Password

**Endpoint:** `POST /api/auth/reset-password`  
**Auth:** None

> Use token from email. Invalidates ALL existing tokens for the user.

```json
// Request Body
{
  "token": "550e8400-e29b-41d4-a716-446655440000",
  "newPassword": "newPassword456",
  "confirmPassword": "newPassword456"
}
```

```json
// Success Response (200 OK)
{
  "success": true,
  "message": "Password reset successfully",
  "timestamp": "2025-12-29T10:00:00"
}
```

```json
// Error Response - Invalid token (400)
{
  "success": false,
  "message": "Invalid or expired reset token",
  "timestamp": "2025-12-29T10:00:00"
}
```

```json
// Error Response - Passwords don't match (400)
{
  "success": false,
  "message": "New password and confirm password do not match",
  "timestamp": "2025-12-29T10:00:00"
}
```

---

### 2.5 Logout

**Endpoint:** `POST /api/auth/logout`  
**Auth:** Bearer Token (any authenticated user)

> Blacklists the current token. Token cannot be used again.

```json
// Success Response (200 OK)
{
  "success": true,
  "message": "Logged out successfully",
  "timestamp": "2025-12-29T10:00:00"
}
```

> **Note:** After logout, calling any protected endpoint with the same token returns 401 Unauthorized.

---

## 3. Customer Flow [📱 MOBILE]

### 3.0 Register FCM Token (Push Notifications)

**Endpoint:** `POST /api/customer/fcm-token`  
**Auth:** Bearer Token (CUSTOMER)

> **Important:** Call this endpoint after login to enable push notifications for loan status updates.

```json
// Request Body
{
  "fcmToken": "firebase-device-token-from-mobile-sdk"
}
```

```json
// Success Response (200 OK)
{
  "success": true,
  "message": "FCM token registered successfully",
  "data": null,
  "timestamp": "2026-01-22T10:00:00"
}
```

**When to call:**

- After successful login
- After app reinstall or data clear (FCM token regenerates)

**Push Notifications sent for:**
| Status | Notification |
|--------|--------------|
| MARKETING_APPROVED | "Pengajuan sedang dalam review Branch Manager" |
| BRANCH_MANAGER_APPROVED | "Pengajuan menunggu persetujuan akhir" |
| DISBURSED | "Selamat! Pengajuan Anda telah disetujui ✅" |
| \*\_REJECTED | "Pengajuan ditolak. Alasan: {note} ❌" |

---

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

**Body Type:** `form-data`

| Key    | Type | Value                                                                                                                                                                                                            | Content-Type (Click ... to add) |
| ------ | ---- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------- |
| `data` | Text | `{"nik": "3201234567890001", "birthdate": "1990-05-15", "phone": "081234567890", "address": "Jl. Sudirman No. 123, Jakarta", "bankName": "BCA", "accountNumber": "1234567890", "accountHolderName": "John Doe"}` | `application/json`              |
| `ktp`  | File | (Select KTP File)                                                                                                                                                                                                |                                 |
| `kk`   | File | (Select KK File)                                                                                                                                                                                                 |                                 |
| `npwp` | File | (Select NPWP File)                                                                                                                                                                                               |                                 |

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
    "ktpUrl": "http://localhost:8080/uploads/uuid-ktp.jpg",
    "kkUrl": "http://localhost:8080/uploads/uuid-kk.jpg",
    "npwpUrl": "http://localhost:8080/uploads/uuid-npwp.jpg",
    "bankName": "BCA",
    "accountNumber": "1234567890",
    "accountHolderName": "John Doe",
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

## 4. Plafond Selection Flow [📱 MOBILE]

> **Important:** Customer must select a plafond (credit limit) before submitting loans.
> Products define the maximum limits for amount, tenor, and minimum interest rate.

### 4.1 Select Plafond

**Endpoint:** `POST /api/customer/plafond`  
**Auth:** Bearer Token (CUSTOMER)

```json
// Request Body
{
  "productId": 1
}
```

```json
// Success Response (200 OK)
{
  "success": true,
  "message": "Plafond selected successfully",
  "data": {
    "id": 1,
    "product": {
      "id": 1,
      "name": "BRONZE",
      "amount": 5000000,
      "tenor": 12,
      "interestRate": 12.0
    },
    "originalAmount": 5000000,
    "remainingAmount": 5000000,
    "assignedAt": "2025-12-23T21:55:00",
    "isActive": true
  },
  "timestamp": "2025-12-23T21:55:00"
}
```

```json
// Error Response - Already has plafond (400)
{
  "success": false,
  "message": "You already have an active plafond. Cannot select another one.",
  "timestamp": "2025-12-23T21:55:00"
}
```

---

### 4.2 Get My Plafond

**Endpoint:** `GET /api/customer/plafond`  
**Auth:** Bearer Token (CUSTOMER)

```json
// Success Response (200 OK)
{
  "success": true,
  "message": "Success",
  "data": {
    "id": 1,
    "product": {
      "id": 1,
      "name": "BRONZE",
      "amount": 5000000,
      "tenor": 12,
      "interestRate": 12.0
    },
    "originalAmount": 5000000,
    "remainingAmount": 5000000,
    "assignedAt": "2025-12-23T21:55:00",
    "isActive": true
  },
  "timestamp": "2025-12-23T21:55:00"
}
```

```json
// Error Response - No plafond (404)
{
  "success": false,
  "message": "You don't have an active plafond. Please select a plafond first.",
  "timestamp": "2025-12-23T21:55:00"
}
```

### 4.3 Plafond Remaining Amount Flow (E2E Test)

> **Testing Lifecycle:** This flow demonstrates how `remainingAmount` decreases and plafond becomes inactive.

| Step | Action                     | Expected Result                                                        |
| ---- | -------------------------- | ---------------------------------------------------------------------- |
| 1    | Select BRONZE plafond (5M) | `originalAmount: 5M, remainingAmount: 5M, isActive: true`              |
| 2    | Submit loan 3M → Approve   | `remainingAmount: 2M`                                                  |
| 3    | Try submit loan 3M         | ❌ "Requested amount exceeds remaining plafond. Remaining: Rp 2000000" |
| 4    | Submit loan 2M → Approve   | `remainingAmount: 0, isActive: false`                                  |
| 5    | Get plafond                | ❌ "You don't have an active plafond"                                  |
| 6    | Select new SILVER plafond  | ✅ Allowed (old plafond is inactive)                                   |

```json
// After loan approval, plafond response shows reduced remaining:
{
  "data": {
    "originalAmount": 5000000.00,
    "remainingAmount": 2000000.00,
    "isActive": true
  }
}

// After plafond is depleted (remainingAmount = 0):
{
  "success": false,
  "message": "You don't have an active plafond. Please select a plafond first."
}
```

---

## 5. Loan Application Flow [📱 MOBILE]

### 5.1 Submit Loan Application

**Endpoint:** `POST /api/loans`  
**Auth:** Bearer Token (CUSTOMER)

> ⚠️ **Prerequisites:**
>
> 1. Customer profile must be complete (NIK, birthdate, phone, address)
> 2. Customer must have selected a plafond first
> 3. Customer must NOT have any pending loan applications

> **Validation Rules:**
>
> - No pending loan in status: `SUBMITTED`, `MARKETING_APPROVED`, or `BRANCH_MANAGER_APPROVED`
> - `amount` must be ≤ plafond **remainingAmount** (not product.amount)
> - `tenor` must be ≤ plafond product tenor
> - `interestRate` must be ≥ plafond product interest rate

```json
// Request Body (NEW FORMAT - product derived from plafond)
{
  "branchId": 1,
  "amount": 3000000,
  "tenor": 6,
  "interestRate": 12.0,
  "latitude": "-6.2088",
  "longitude": "106.8456"
}
```

```json
// Success Response (201 Created)
{
  "success": true,
  "message": "Loan application submitted successfully",
  "data": {
    "id": 1,
    "customerName": "John Doe",
    "customerEmail": "john.doe@email.com",
    "customerNik": "3201234567890001",
    "customerPhone": "+6281234567890",
    "customerAddress": "Jl. Sudirman No. 123, Jakarta",
    "customerBirthdate": "1990-05-15",
    "customerKtpPath": "/uploads/documents/uuid-ktp.jpg",
    "customerKkPath": "/uploads/documents/uuid-kk.jpg",
    "customerNpwpPath": "/uploads/documents/uuid-npwp.jpg",
    "customerBankName": "BCA",
    "customerAccountNumber": "1234567890",
    "customerAccountHolderName": "John Doe",
    "product": {
      "id": 1,
      "name": "BRONZE",
      "amount": 5000000,
      "tenor": 12,
      "interestRate": 12.0
    },
    "branch": {
      "id": 1,
      "code": "JKT",
      "location": "Jakarta"
    },
    "requestedAmount": 3000000,
    "requestedTenor": 6,
    "requestedRate": 12.0,
    "latitude": "-6.2088",
    "longitude": "106.8456",
    "status": "SUBMITTED",
    "createdAt": "2025-12-23T22:00:00"
  },
  "timestamp": "2025-12-23T22:00:00"
}
```

```json
// Error Response - No plafond (400)
{
  "success": false,
  "message": "Please select a plafond first before submitting a loan application.",
  "timestamp": "2025-12-23T22:00:00"
}
```

```json
// Error Response - Amount exceeds limit (400)
{
  "success": false,
  "message": "Requested amount exceeds remaining plafond. Remaining: Rp 5000000",
  "timestamp": "2025-12-23T22:00:00"
}
```

```json
// Error Response - Has pending loan application (400)
{
  "success": false,
  "message": "You already have a pending loan application. Please wait until it is fully approved or rejected before submitting a new one.",
  "timestamp": "2025-12-31T17:50:00"
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
**Auth:** Bearer Token (CUSTOMER, MARKETING, BRANCH_MANAGER, BACKOFFICE, SUPERADMIN)

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
**Auth:** Bearer Token (CUSTOMER, MARKETING, BRANCH_MANAGER, BACKOFFICE, SUPERADMIN)

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

## 5. Approval Workflow [💻 WEBSITE]

### Complete Approval Flow Diagram

```
┌─────────────┐     ┌────────────────────┐     ┌──────────────────────────┐     ┌──────────┐
│  SUBMITTED  │────▶│ MARKETING_APPROVED │────▶│ BRANCH_MANAGER_APPROVED  │────▶│ DISBURSED │
└─────────────┘     └────────────────────┘     └──────────────────────────┘     └──────────┘
       │                     │                            │                           │
       │                     │                            │                           │
       ▼                     ▼                            ▼                           │
┌──────────────────┐ ┌──────────────────┐    ┌──────────────────────────┐            │
│ MARKETING_REJECTED│ │ BM_REJECTED     │    │ REJECTED                 │◀───────────┘
└──────────────────┘ └──────────────────┘    └──────────────────────────┘
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
    "status": "DISBURSED",
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

### 5.4 Get My Approval History

**Endpoint:** `GET /api/approval/my-history`  
**Auth:** Bearer Token (MARKETING, BRANCH_MANAGER, or BACKOFFICE)

> Returns all loans that the logged-in user has approved or rejected.

```json
// Success Response (200 OK)
{
  "success": true,
  "message": "Success",
  "data": [
    {
      "id": 5,
      "loanId": 1,
      "customerName": "John Doe",
      "productName": "BRONZE",
      "loanAmount": 3000000,
      "branchLocation": "Jakarta",
      "actionTaken": "MARKETING_APPROVED",
      "currentStatus": "BRANCH_MANAGER_APPROVED",
      "note": "Documents verified",
      "actionDate": "2026-01-08T14:30:00"
    },
    {
      "id": 3,
      "loanId": 2,
      "customerName": "Jane Smith",
      "productName": "SILVER",
      "loanAmount": 5000000,
      "branchLocation": "Jakarta",
      "actionTaken": "MARKETING_REJECTED",
      "note": "Incomplete documents",
      "actionDate": "2026-01-07T10:15:00"
    }
  ],
  "timestamp": "2026-01-08T15:00:00"
}
```

```json
// Empty Response (200 OK) - No approvals yet
{
  "success": true,
  "message": "Success",
  "data": [],
  "timestamp": "2026-01-08T15:00:00"
}
```

---

## 6. SuperAdmin Flow [💻 WEBSITE]

### 6.1 Create Internal User

**Endpoint:** `POST /api/admin/users`  
**Auth:** Bearer Token (SUPERADMIN)

> Create a new internal user with role and optional branch assignment.
>
> - MARKETING/BRANCH_MANAGER roles require a branch
> - SUPERADMIN/BACKOFFICE roles do not require a branch
> - Cannot assign CUSTOMER role to internal users

```json
// Request Body - Create Marketing User
{
  "name": "New Marketing",
  "email": "new.marketing@loan.com",
  "password": "password123",
  "roleId": 2,
  "branchId": 1
}
```

```json
// Request Body - Create Backoffice User (no branch required)
{
  "name": "New Backoffice",
  "email": "new.backoffice@loan.com",
  "password": "password123",
  "roleId": 4
}
```

```json
// Success Response (201 Created)
{
  "success": true,
  "message": "Internal user created successfully",
  "data": {
    "id": 10,
    "name": "New Marketing",
    "email": "new.marketing@loan.com",
    "userType": "INTERNAL",
    "isActive": true,
    "roles": ["MARKETING"],
    "branch": {
      "id": 1,
      "code": "JKT",
      "location": "Jakarta"
    }
  },
  "timestamp": "2025-12-23T20:00:00"
}
```

```json
// Error Response - Missing branch for MARKETING role (400)
{
  "success": false,
  "message": "Branch is required for MARKETING role",
  "timestamp": "2025-12-23T20:00:00"
}
```

```json
// Error Response - Email already exists (409)
{
  "success": false,
  "message": "Email already exists",
  "timestamp": "2025-12-23T20:00:00"
}
```

```json
// Error Response - Cannot assign CUSTOMER role (400)
{
  "success": false,
  "message": "Cannot assign CUSTOMER role to internal user",
  "timestamp": "2025-12-23T20:00:00"
}
```

---

### 6.2 Get All Users

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

### 6.4 Assign Role to User

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

### 6.5 Remove Role from User

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

### 6.6 Get All Roles

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
      "permissions": ["LOAN_READ_ALL", "LOAN_APPROVE_BACKOFFICE", "LOAN_REJECT"]
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

### 6.7 Update Role Permissions

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

### 6.8 Get All Permissions

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
    { "id": 13, "name": "PRODUCT_READ" },
    { "id": 14, "name": "BRANCH_READ" },
    { "id": 15, "name": "ROLE_CREATE" },
    { "id": 16, "name": "ROLE_READ" },
    { "id": 17, "name": "ROLE_UPDATE" },
    { "id": 18, "name": "ROLE_DELETE" },
    { "id": 19, "name": "PERMISSION_READ" }
  ],
  "timestamp": "2025-12-22T10:00:00"
}
```

---

### 6.9 Update User

**Endpoint:** `PUT /api/admin/users/{id}`  
**Auth:** Bearer Token (SUPERADMIN)

> Update user data (name, email, branch). All fields are optional.

```json
// Request Body
{
  "name": "Updated Name",
  "email": "updated.email@loan.com",
  "branchId": 2
}
```

```json
// Success Response (200 OK)
{
  "success": true,
  "message": "User updated successfully",
  "data": {
    "id": 3,
    "name": "Updated Name",
    "email": "updated.email@loan.com",
    "userType": "INTERNAL",
    "isActive": true,
    "branch": {
      "id": 2,
      "code": "SBY",
      "location": "Surabaya"
    },
    "roles": ["MARKETING"],
    "createdAt": "2025-12-22T10:00:00"
  },
  "timestamp": "2025-12-22T10:00:00"
}
```

```json
// Error Response - Email already exists (409)
{
  "success": false,
  "message": "Email already exists",
  "timestamp": "2025-12-22T10:00:00"
}
```

---

### 6.10 Update User Status

**Endpoint:** `PATCH /api/admin/users/{id}/status`  
**Auth:** Bearer Token (SUPERADMIN)

> Toggle user active/inactive status.

```json
// Request Body - Deactivate user
{
  "isActive": false
}
```

```json
// Success Response (200 OK)
{
  "success": true,
  "message": "User status updated successfully",
  "data": {
    "id": 3,
    "name": "Marketing Jakarta",
    "email": "marketing.jkt@loan.com",
    "userType": "INTERNAL",
    "isActive": false,
    "branch": {
      "id": 1,
      "code": "JKT",
      "location": "Jakarta"
    },
    "roles": ["MARKETING"],
    "createdAt": "2025-12-22T10:00:00"
  },
  "timestamp": "2025-12-22T10:00:00"
}
```

> **Note:** Deactivated users cannot login to the system.

---

## Quick Test Scenarios

### Scenario 1: Complete Loan Approval Flow

1. **Login as Customer**: `john.doe@email.com` / `customer123` → Save token
2. **Submit Loan**: `POST /api/loans` with branchId: 1, amount, tenor → Save loan_id
3. **Login as Marketing**: `marketing.jkt@loan.com` / `marketing123` → Save token
4. **Get Pending**: `GET /api/approval/pending` → Should see the loan
5. **Approve**: `POST /api/approval/{loan_id}/approve`
6. **Login as Branch Manager**: `bm.jkt@loan.com` / `bm123` → Save token
7. **Get Pending**: `GET /api/approval/pending` → Should see the loan
8. **Approve**: `POST /api/approval/{loan_id}/approve`
9. **Login as Backoffice**: `backoffice@loan.com` / `backoffice123` → Save token
10. **Get Pending**: `GET /api/approval/pending` → Should see the loan
11. **Approve**: `POST /api/approval/{loan_id}/approve` → Status becomes DISBURSED
12. **Login as Customer**: Verify loan status via `GET /api/loans/{loan_id}`

### Scenario 2: Plafond Depletion Flow

1. **Login as Customer**: `john.doe@email.com` / `customer123`
2. **Select Plafond**: `POST /api/customer/plafond` with productId: 1 (BRONZE 5M)
3. **Check Plafond**: `GET /api/customer/plafond` → remainingAmount: 5000000
4. **Submit Loan 3M**: `POST /api/loans` → Approve through full flow
5. **Check Plafond**: remainingAmount: 2000000
6. **Try Submit 3M**: Should fail - "exceeds remaining plafond"
7. **Submit Loan 2M**: Approve through full flow
8. **Check Plafond**: Returns 404 - plafond is inactive
9. **Select New Plafond**: `POST /api/customer/plafond` with productId: 2 → Succeeds

### Scenario 3: Incomplete Profile Rejection

1. **Login as Jane Smith**: `jane.smith@email.com` / `customer123` (has empty profile)
2. **Try Submit Loan**: `POST /api/loans` → Should get 400 error
3. **Update Profile**: `PUT /api/customer/profile` with complete data
4. **Select Plafond**: `POST /api/customer/plafond`
5. **Retry Submit Loan**: Should succeed now

---

### 7.5 Create Branch

**Endpoint:** `POST /api/admin/branches`  
**Auth:** Bearer Token (SUPERADMIN)

```json
// Request Body
{
  "code": "YGY",
  "location": "Yogyakarta"
}
```

```json
// Success Response (201 Created)
{
  "success": true,
  "message": "Branch created successfully",
  "data": {
    "id": 4,
    "code": "YGY",
    "location": "Yogyakarta"
  },
  "timestamp": "2025-12-29T10:00:00"
}
```

---

### 7.6 Update Branch

**Endpoint:** `PUT /api/admin/branches/{id}`  
**Auth:** Bearer Token (SUPERADMIN)

```json
// Request Body
{
  "code": "YGY-UPD",
  "location": "Yogyakarta Updated"
}
```

```json
// Success Response (200 OK)
{
  "success": true,
  "message": "Branch updated successfully",
  "data": {
    "id": 4,
    "code": "YGY-UPD",
    "location": "Yogyakarta Updated"
  },
  "timestamp": "2025-12-29T10:00:00"
}
```

---

### 7.7 Delete Branch

**Endpoint:** `DELETE /api/admin/branches/{id}`  
**Auth:** Bearer Token (SUPERADMIN)

> **Note:** Cannot delete branch if it has assigned users.

```json
// Success Response (200 OK)
{
  "success": true,
  "message": "Branch deleted successfully",
  "data": null,
  "timestamp": "2025-12-29T10:00:00"
}
```

```json
// Error Response - Constraint Violation (409)
{
  "success": false,
  "message": "Cannot delete branch that has assigned users",
  "timestamp": "2025-12-29T10:00:00"
}
```

---

## Error Response Reference

| Endpoint                    | Error               | Status | Message                                             |
| --------------------------- | ------------------- | ------ | --------------------------------------------------- |
| **Auth**                    |                     |        |                                                     |
| POST /auth/register         | Duplicate email     | 409    | Email already registered                            |
| POST /auth/register         | Invalid email       | 400    | email: must be a valid email                        |
| POST /auth/register         | Short password      | 400    | password: size must be between 6 and 100            |
| POST /auth/login            | Wrong credentials   | 401    | Invalid email or password                           |
| POST /auth/reset-password   | Invalid token       | 400    | Invalid or expired reset token                      |
| POST /auth/reset-password   | Mismatch            | 400    | New password and confirm password do not match      |
| **Profile**                 |                     |        |                                                     |
| PUT /customer/profile       | Invalid NIK         | 400    | nik: NIK must be exactly 16 digits                  |
| **Plafond**                 |                     |        |                                                     |
| GET /customer/plafond       | No plafond          | 404    | You don't have an active plafond                    |
| POST /customer/plafond      | Already has plafond | 400    | You already have an active plafond                  |
| POST /customer/plafond      | Product not found   | 404    | Product not found                                   |
| **Loan**                    |                     |        |                                                     |
| POST /loans                 | No plafond          | 400    | Please select a plafond first                       |
| POST /loans                 | Incomplete profile  | 400    | Please complete your profile...                     |
| POST /loans                 | Amount exceeds      | 400    | Requested amount exceeds remaining plafond          |
| POST /loans                 | Tenor exceeds       | 400    | Requested tenor exceeds plafond limit               |
| POST /loans                 | Rate too low        | 400    | Interest rate cannot be lower than plafond minimum  |
| POST /loans                 | Branch not found    | 404    | Branch not found                                    |
| GET /loans/{id}             | Not owner           | 403    | You don't have access to this loan application      |
| **Approval**                |                     |        |                                                     |
| POST /approval/{id}/approve | Wrong status        | 400    | Loan is not in the correct status for your approval |
| POST /approval/{id}/approve | Wrong branch        | 403    | You can only process loans from your branch         |
| **Admin**                   |                     |        |                                                     |
| POST /admin/users           | Create customer     | 400    | Cannot create customer via admin endpoint           |
| Any admin endpoint          | Not admin           | 403    | Access Denied                                       |
| **General**                 |                     |        |                                                     |
| Any authenticated           | No token            | 401    | Unauthorized                                        |
| Any authenticated           | Invalid token       | 401    | Unauthorized                                        |
| Any authenticated           | Expired token       | 401    | Unauthorized                                        |

---

## 8. Shared Staff Operations [💻 WEBSITE]

### 8.1 Get User Detail

**Endpoint:** `GET /api/users/{id}`  
**Auth:** Bearer Token (SUPERADMIN, MARKETING, BRANCH_MANAGER, BACKOFFICE)

> Shared endpoint for staff to view user details (customer or internal).
>
> - **SUPERADMIN**: Can view all users.
> - **MARKETING/BRANCH_MANAGER/BACKOFFICE**: Can view details to facilitate loan processing (e.g. checking customer profile).

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

## 9. File Access

### 9.1 Get Uploaded File

**Endpoint:** `GET /uploads/{filename}`  
**Auth:** Bearer Token (any authenticated user)

> Secure file access with role and ownership-based authorization.
>
> - **Staff roles (SUPERADMIN, BACKOFFICE, BRANCH_MANAGER, MARKETING)**: Can access any file.
> - **Customers**: Can only access their own uploaded documents (KTP, KK, NPWP).

**Path Parameters:**

| Parameter  | Description                      |
| ---------- | -------------------------------- |
| `filename` | The filename including extension |

**Example Request:**

```bash
# Staff accessing a customer's document
curl -X GET "http://localhost:8080/uploads/uuid-ktp.jpg" \
  -H "Authorization: Bearer {{marketing_token}}"

# Customer accessing their own document
curl -X GET "http://localhost:8080/uploads/uuid-ktp.jpg" \
  -H "Authorization: Bearer {{customer_token}}"
```

**Success Response:**

Returns the file content with appropriate `Content-Type` header (e.g., `image/jpeg`, `application/pdf`).

**Error Responses:**

```json
// Error Response - Not owner and not staff (403)
{
  "status": 403,
  "error": "Forbidden"
}
```

```json
// Error Response - File not found (404)
{
  "status": 404,
  "error": "Not Found"
}
```

> **Note:** File paths are stored in `UserProfile` as `ktpPath`, `kkPath`, and `npwpPath`. These are set when a customer updates their profile with document uploads.

---

## API Endpoint Summary

| #                           | Method | Endpoint                                   | Description                    | Auth                 |
| --------------------------- | ------ | ------------------------------------------ | ------------------------------ | -------------------- |
| **Authentication**          |        |                                            |                                |                      |
| 1                           | POST   | `/api/auth/register`                       | Register new customer          | None                 |
| 2                           | POST   | `/api/auth/login`                          | Login                          | None                 |
| 3                           | POST   | `/api/auth/forgot-password`                | Request password reset email   | None                 |
| 4                           | POST   | `/api/auth/reset-password`                 | Reset password with token      | None                 |
| 5                           | POST   | `/api/auth/logout`                         | Logout (blacklist token)       | Bearer               |
| **Customer**                |        |                                            |                                |                      |
| 6                           | GET    | `/api/customer/profile`                    | Get customer profile           | Bearer (CUSTOMER)    |
| 7                           | PUT    | `/api/customer/profile`                    | Update profile (multipart)     | Bearer (CUSTOMER)    |
| 8                           | POST   | `/api/customer/plafond`                    | Select a plafond               | Bearer (CUSTOMER)    |
| 9                           | GET    | `/api/customer/plafond`                    | Get active plafond             | Bearer (CUSTOMER)    |
| 10                          | GET    | `/api/products`                            | List all products              | None                 |
| 11                          | GET    | `/api/branches`                            | List all branches              | None                 |
| **Loan Application**        |        |                                            |                                |                      |
| 12                          | POST   | `/api/loans`                               | Submit loan application        | Bearer (CUSTOMER)    |
| 13                          | GET    | `/api/loans`                               | Get my loans                   | Bearer (CUSTOMER)    |
| 14                          | GET    | `/api/loans/{id}`                          | Get loan by ID                 | Bearer (Owner/Staff) |
| 15                          | GET    | `/api/loans/{id}/history`                  | Get loan history               | Bearer (Owner/Staff) |
| **Approval Workflow**       |        |                                            |                                |                      |
| 16                          | GET    | `/api/approval/pending`                    | Get pending loans for approval | Bearer (Staff)       |
| 17                          | POST   | `/api/approval/{id}/approve`               | Approve loan                   | Bearer (Staff)       |
| 18                          | POST   | `/api/approval/{id}/reject`                | Reject loan                    | Bearer (Staff)       |
| **SuperAdmin**              |        |                                            |                                |                      |
| 19                          | POST   | `/api/admin/users`                         | Create internal user           | Bearer (SUPERADMIN)  |
| 20                          | GET    | `/api/admin/users`                         | Get all users                  | Bearer (SUPERADMIN)  |
| 21                          | PUT    | `/api/admin/users/{id}`                    | Update user                    | Bearer (SUPERADMIN)  |
| 22                          | PATCH  | `/api/admin/users/{id}/status`             | Update user status             | Bearer (SUPERADMIN)  |
| 23                          | POST   | `/api/admin/users/{id}/roles`              | Assign role to user            | Bearer (SUPERADMIN)  |
| 24                          | DELETE | `/api/admin/users/{userId}/roles/{roleId}` | Remove role from user          | Bearer (SUPERADMIN)  |
| 25                          | GET    | `/api/admin/roles`                         | Get all roles                  | Bearer (SUPERADMIN)  |
| 26                          | PUT    | `/api/admin/roles/{id}/permissions`        | Update role permissions        | Bearer (SUPERADMIN)  |
| 27                          | GET    | `/api/admin/permissions`                   | Get all permissions            | Bearer (SUPERADMIN)  |
| 28                          | POST   | `/api/admin/branches`                      | Create branch                  | Bearer (SUPERADMIN)  |
| 29                          | PUT    | `/api/admin/branches/{id}`                 | Update branch                  | Bearer (SUPERADMIN)  |
| 30                          | DELETE | `/api/admin/branches/{id}`                 | Delete branch                  | Bearer (SUPERADMIN)  |
| **Shared Staff Operations** |        |                                            |                                |                      |
| 31                          | GET    | `/api/users/{id}`                          | Get user by ID                 | Bearer (Staff)       |
| **File Access**             |        |                                            |                                |                      |
| 32                          | GET    | `/uploads/{filename}`                      | Get uploaded file              | Bearer (Owner/Staff) |

---

_Documentation updated: 2026-01-08_
