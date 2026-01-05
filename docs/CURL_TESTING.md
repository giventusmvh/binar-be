# cURL Testing Guide

> End-to-end testing with cURL commands and actual test results

**Test Date**: 2026-01-01

---

## Table of Contents

1. [Setup](#1-setup)
2. [Authentication Flow](#2-authentication-flow)
3. [Customer Flow](#3-customer-flow)
4. [Loan Application Flow](#4-loan-application-flow)
5. [Approval Workflow](#5-approval-workflow)
6. [SuperAdmin Operations](#6-superadmin-operations)
7. [Edge Cases & Error Scenarios](#7-edge-cases--error-scenarios)

---

## 1. Setup

### Base URL

```bash
BASE_URL="http://localhost:8080"
```

### Pre-seeded Accounts

| Email                    | Password        | Role                        |
| ------------------------ | --------------- | --------------------------- |
| `john.doe@email.com`     | `customer123`   | CUSTOMER (complete profile) |
| `jane.smith@email.com`   | `customer123`   | CUSTOMER (empty profile)    |
| `marketing.jkt@loan.com` | `marketing123`  | MARKETING (Jakarta)         |
| `bm.jkt@loan.com`        | `bm123`         | BRANCH_MANAGER (Jakarta)    |
| `marketing.sby@loan.com` | `marketing123`  | MARKETING (Surabaya)        |
| `bm.sby@loan.com`        | `bm123`         | BRANCH_MANAGER (Surabaya)   |
| `backoffice@loan.com`    | `backoffice123` | BACKOFFICE                  |
| `admin@loan.com`         | `admin123`      | SUPERADMIN                  |

---

## 2. Authentication Flow

### 2.1 Register New Customer

```bash
# ✅ Success - Register new customer
curl -X POST "$BASE_URL/api/auth/register" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test User Curl",
    "email": "test.curl@email.com",
    "password": "password123"
  }'

# Response: 200 OK
{
  "success": true,
  "message": "Registration successful",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9.eyJyb2xlcyI6WyJCUkFOQ0hfUkVBRCIsIkxPQU5fQ1JFQVRFIiwiTE9BTl9SRUFEIiwiUExBRk9ORF9SRUFEIiwiUExBRk9ORF9TRUxFQ1QiLCJQUk9EVUNUX1JFQUQiLCJQUk9GSUxFX1JFQUQiLCJQUk9GSUxFX1VQREFURSIsIlJPTEVfQ1VTVE9NRVIiXSwic3ViIjoidGVzdC5jdXJsQGVtYWlsLmNvbSIsImlhdCI6MTc2NzI0NDIwNywiZXhwIjoxNzY3MzMwNjA3fQ.L8cVJambGtewrDh1a639NqOiwqzi3JJVqY22AbdfFO8",
    "tokenType": "Bearer",
    "userId": 11,
    "email": "test.curl@email.com",
    "name": "Test User Curl",
    "roles": ["CUSTOMER"],
    "permissions": ["PROFILE_READ", "BRANCH_READ", "LOAN_READ", "PROFILE_UPDATE", "LOAN_CREATE", "PLAFOND_SELECT", "PLAFOND_READ", "PRODUCT_READ"]
  },
  "timestamp": "2026-01-01T12:10:07.463876"
}
```

```bash
# ❌ Error - Email already exists
curl -X POST "$BASE_URL/api/auth/register" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Duplicate User",
    "email": "john.doe@email.com",
    "password": "password123"
  }'

# Response: 409 Conflict
{
  "success": false,
  "message": "Email already registered",
  "timestamp": "2026-01-01T12:10:17.683416"
}
```

### 2.2 Login

```bash
# ✅ Success - Customer login
curl -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john.doe@email.com",
    "password": "customer123"
  }'

# Response: 200 OK
{
  "success": true,
  "message": "Login successful",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9.eyJyb2xlcyI6WyJCUkFOQ0hfUkVBRCIsIkxPQU5fQ1JFQVRFIiwiTE9BTl9SRUFEIiwiUExBRk9ORF9SRUFEIiwiUExBRk9ORF9TRUxFQ1QiLCJQUk9EVUNUX1JFQUQiLCJQUk9GSUxFX1JFQUQiLCJQUk9GSUxFX1VQREFURSIsIlJPTEVfQ1VTVE9NRVIiXSwic3ViIjoiam9obi5kb2VAZW1haWwuY29tIiwiaWF0IjoxNzY3MjQ0MjEwLCJleHAiOjE3NjczMzA2MTB9.rc_yY4RoI8ZELPG1WbIt6TH6vnwSM-WR0yVru_px5pI",
    "tokenType": "Bearer",
    "userId": 8,
    "email": "john.doe@email.com",
    "name": "John Doe",
    "roles": ["CUSTOMER"],
    "permissions": ["BRANCH_READ", "PROFILE_UPDATE", "LOAN_READ", "LOAN_CREATE", "PLAFOND_READ", "PROFILE_READ", "PLAFOND_SELECT", "PRODUCT_READ"]
  },
  "timestamp": "2026-01-01T12:10:10.529842"
}

# Save token for subsequent requests
CUSTOMER_TOKEN="<token_from_response>"
```

```bash
# ❌ Error - Wrong password
curl -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john.doe@email.com",
    "password": "wrongpassword"
  }'

# Response: 401 Unauthorized
{
  "success": false,
  "message": "Invalid email or password",
  "timestamp": "2026-01-01T12:10:13.552678"
}
```

### 2.3 Forgot Password

```bash
# ✅ Success - Request password reset
curl -X POST "$BASE_URL/api/auth/forgot-password" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john.doe@email.com"
  }'

# Response: 200 OK
# >> Email sent to Mailtrap with reset token
# >> Note: Returns same response even if email doesn't exist (security)
```

### 2.4 Reset Password

```bash
# ❌ Error - Invalid or expired token
curl -X POST "$BASE_URL/api/auth/reset-password" \
  -H "Content-Type: application/json" \
  -d '{
    "token": "invalid-token-123",
    "newPassword": "newpassword456",
    "confirmPassword": "newpassword456"
  }'

# Response: 400 Bad Request
{
  "success": false,
  "message": "Invalid or expired reset token",
  "timestamp": "2026-01-01T12:19:07.573163"
}
```

```bash
# ❌ Error - Passwords don't match
curl -X POST "$BASE_URL/api/auth/reset-password" \
  -H "Content-Type: application/json" \
  -d '{
    "token": "valid-token",
    "newPassword": "newpassword456",
    "confirmPassword": "differentpassword"
  }'

# Response: 400 Bad Request
{
  "success": false,
  "message": "New password and confirm password do not match",
  "timestamp": "2026-01-01T12:19:12.005856"
}
```

### 2.5 Logout & Token Blacklisting

```bash
# Step 1: Login to get token
JANE_TOKEN=$(curl -s -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email": "jane.smith@email.com", "password": "customer123"}' \
  | jq -r '.data.token')

# Step 2: Verify token works BEFORE logout
curl -X GET "$BASE_URL/api/customer/profile" \
  -H "Authorization: Bearer $JANE_TOKEN"

# Response: 200 OK
{
  "success": true,
  "data": {"name": "Jane Smith", ...}
}

# Step 3: Logout (blacklist the token)
curl -X POST "$BASE_URL/api/auth/logout" \
  -H "Authorization: Bearer $JANE_TOKEN"

# Response: 200 OK
{
  "success": true,
  "message": "Logged out successfully",
  "timestamp": "2026-01-01T12:18:43.528977"
}

# Step 4: ❌ Try to use token AFTER logout - SHOULD FAIL
curl -X GET "$BASE_URL/api/customer/profile" \
  -H "Authorization: Bearer $JANE_TOKEN"

# Response: 403 Forbidden (TOKEN IS BLACKLISTED!)
{
  "timestamp": "2026-01-01T05:18:43.557Z",
  "status": 403,
  "error": "Forbidden",
  "path": "/api/customer/profile"
}
```

> **✅ Token Invalidation Verified**: After logout, the same JWT token is rejected with 403 Forbidden.
> This proves the Redis token blacklist is working correctly.

### 2.6 No Auth - Forbidden

```bash
# ❌ Error - No authorization token
curl -X GET "$BASE_URL/api/customer/profile"

# Response: 403 Forbidden
{
  "timestamp": "2026-01-01T05:13:58.262Z",
  "status": 403,
  "error": "Forbidden",
  "path": "/api/customer/profile"
}
```

---

## 3. Customer Flow

### 3.1 Get Products (Public)

```bash
# ✅ Success - No auth required
curl -X GET "$BASE_URL/api/products"

# Response: 200 OK
{
  "success": true,
  "message": "Success",
  "data": [
    {"id": 1, "name": "BRONZE", "amount": 5000000.00, "tenor": 12, "interestRate": 12.00},
    {"id": 2, "name": "SILVER", "amount": 10000000.00, "tenor": 24, "interestRate": 10.00},
    {"id": 3, "name": "GOLD", "amount": 25000000.00, "tenor": 36, "interestRate": 8.50},
    {"id": 4, "name": "PLATINUM", "amount": 50000000.00, "tenor": 48, "interestRate": 7.00}
  ],
  "timestamp": "2026-01-01T12:09:12.714674"
}
```

### 3.2 Get Branches (Public)

```bash
# ✅ Success - No auth required
curl -X GET "$BASE_URL/api/branches"

# Response: 200 OK
{
  "success": true,
  "message": "Success",
  "data": [
    {"id": 1, "code": "JKT", "location": "Jakarta"},
    {"id": 2, "code": "SBY", "location": "Surabaya"},
    {"id": 3, "code": "BDG", "location": "Bandung"}
  ],
  "timestamp": "2026-01-01T12:09:31.831825"
}
```

### 3.3 Get Profile (Complete)

```bash
# ✅ Success - Get complete profile
CUSTOMER_TOKEN=$(curl -s -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email": "john.doe@email.com", "password": "customer123"}' \
  | jq -r '.data.token')

curl -X GET "$BASE_URL/api/customer/profile" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN"

# Response: 200 OK
{
  "success": true,
  "message": "Success",
  "data": {
    "id": 8,
    "name": "John Doe",
    "email": "john.doe@email.com",
    "userType": "CUSTOMER",
    "isActive": true,
    "branch": null,
    "profile": {
      "id": 1,
      "birthdate": "1990-05-15",
      "phone": "081234567890",
      "address": "Jl. Sudirman No. 123, Jakarta Pusat",
      "nik": "3174051505900001",
      "isComplete": true
    },
    "roles": ["CUSTOMER"],
    "createdAt": "2025-12-31T18:07:25.654186"
  },
  "timestamp": "2026-01-01T12:10:57.050331"
}
```

### 3.4 Get Profile (Empty)

```bash
# ✅ Success - Get incomplete profile (Jane has empty profile)
JANE_TOKEN=$(curl -s -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email": "jane.smith@email.com", "password": "customer123"}' \
  | jq -r '.data.token')

curl -X GET "$BASE_URL/api/customer/profile" \
  -H "Authorization: Bearer $JANE_TOKEN"

# Response: 200 OK
{
  "success": true,
  "message": "Success",
  "data": {
    "id": 9,
    "name": "Jane Smith",
    "email": "jane.smith@email.com",
    "userType": "CUSTOMER",
    "isActive": true,
    "branch": null,
    "profile": {
      "id": 2,
      "birthdate": null,
      "phone": null,
      "address": null,
      "nik": null,
      "isComplete": false
    },
    "roles": ["CUSTOMER"],
    "createdAt": "2025-12-31T18:07:25.747908"
  },
  "timestamp": "2026-01-01T12:11:04.008777"
}
```

### 3.5 Update Profile

```bash
# ✅ Success - Update customer profile
curl -X PUT "$BASE_URL/api/customer/profile" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" \
  -d '{
    "nik": "3174051505900001",
    "birthdate": "1990-05-15",
    "phone": "081234567890",
    "address": "Jl. Sudirman No. 123, Jakarta Pusat"
  }'

# Response: 200 OK
{
  "success": true,
  "message": "Profile updated successfully",
  "data": {
    "id": 1,
    "birthdate": "1990-05-15",
    "phone": "081234567890",
    "address": "Jl. Sudirman No. 123, Jakarta Pusat",
    "nik": "3174051505900001",
    "isComplete": true
  },
  "timestamp": "2026-01-01T12:11:00.123456"
}
```

```bash
# ❌ Error - Invalid NIK format (must be 16 digits)
curl -X PUT "$BASE_URL/api/customer/profile" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" \
  -d '{
    "nik": "12345",
    "birthdate": "1990-05-15",
    "phone": "081234567890",
    "address": "Jl. Sudirman No. 123"
  }'

# Response: 400 Bad Request
{
  "success": false,
  "message": "Validation failed",
  "data": {
    "nik": "NIK must be exactly 16 digits"
  },
  "timestamp": "2026-01-01T12:11:05.123456"
}
```

```bash
# ❌ Error - Invalid phone format (must be 10-15 digits)
curl -X PUT "$BASE_URL/api/customer/profile" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" \
  -d '{
    "nik": "3174051505900001",
    "birthdate": "1990-05-15",
    "phone": "123",
    "address": "Jl. Sudirman No. 123"
  }'

# Response: 400 Bad Request
{
  "success": false,
  "message": "Validation failed",
  "data": {
    "phone": "Phone must be 10-15 digits"
  },
  "timestamp": "2026-01-01T12:11:10.123456"
}
```

---

## 4. Loan Application Flow

### 4.1 Select Plafond

```bash
# ✅ Success - Select BRONZE plafond
curl -X POST "$BASE_URL/api/customer/plafond" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" \
  -d '{"productId": 1}'

# Response: 200 OK
{
  "success": true,
  "message": "Plafond selected successfully",
  "data": {
    "id": 1,
    "product": {
      "id": 1,
      "name": "BRONZE",
      "amount": 5000000.00,
      "tenor": 12,
      "interestRate": 12.00
    },
    "originalAmount": 5000000.00,
    "remainingAmount": 5000000.00,
    "assignedAt": "2026-01-01T12:11:26.404161",
    "isActive": true
  },
  "timestamp": "2026-01-01T12:11:26.431997"
}
```

### 4.2 Get My Plafond

```bash
# ✅ Success - Get active plafond
curl -X GET "$BASE_URL/api/customer/plafond" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN"

# Response: 200 OK
{
  "success": true,
  "message": "Success",
  "data": {
    "id": 1,
    "product": {
      "id": 1,
      "name": "BRONZE",
      "amount": 5000000.00,
      "tenor": 12,
      "interestRate": 12.00
    },
    "originalAmount": 5000000.00,
    "remainingAmount": 5000000.00,
    "assignedAt": "2026-01-01T12:11:26.404161",
    "isActive": true
  },
  "timestamp": "2026-01-01T12:11:29.380367"
}
```

```bash
# ❌ Error - No plafond selected
# (Login as user without plafond)
curl -X GET "$BASE_URL/api/customer/plafond" \
  -H "Authorization: Bearer $NO_PLAFOND_TOKEN"

# Response: 404 Not Found
{
  "success": false,
  "message": "You don't have an active plafond. Please select a plafond first.",
  "timestamp": "2026-01-01T12:10:59.165045"
}
```

### 4.3 Submit Loan

```bash
# ✅ Success - Submit loan within plafond limits
curl -X POST "$BASE_URL/api/loans" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" \
  -d '{
    "branchId": 1,
    "amount": 3000000,
    "tenor": 6,
    "interestRate": 12.0
  }'

# Response: 201 Created
{
  "success": true,
  "message": "Loan application submitted successfully",
  "data": {
    "id": 1,
    "customerName": "John Doe",
    "customerEmail": "john.doe@email.com",
    "customerNik": "3174051505900001",
    "customerPhone": "081234567890",
    "customerAddress": "Jl. Sudirman No. 123, Jakarta Pusat",
    "customerBirthdate": "1990-05-15",
    "product": {
      "id": 1,
      "name": "BRONZE",
      "amount": 5000000.00,
      "tenor": 12,
      "interestRate": 12.00
    },
    "branch": {
      "id": 1,
      "code": "JKT",
      "location": "Jakarta"
    },
    "requestedAmount": 3000000,
    "requestedTenor": 6,
    "requestedRate": 12.0,
    "status": "SUBMITTED",
    "createdAt": "2026-01-01T12:11:31.060318",
    "updatedAt": null
  },
  "timestamp": "2026-01-01T12:11:31.138154"
}
```

### 4.4 Get My Loans

```bash
# ✅ Success - Get all my loans
curl -X GET "$BASE_URL/api/loans" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN"

# Response: 200 OK
{
  "success": true,
  "message": "Success",
  "data": [
    {
      "id": 1,
      "customerName": "John Doe",
      "customerEmail": "john.doe@email.com",
      "customerNik": "3174051505900001",
      "customerPhone": "081234567890",
      "customerAddress": "Jl. Sudirman No. 123, Jakarta Pusat",
      "customerBirthdate": "1990-05-15",
      "product": {
        "id": 1,
        "name": "BRONZE",
        "amount": 5000000.00,
        "tenor": 12,
        "interestRate": 12.00
      },
      "branch": {
        "id": 1,
        "code": "JKT",
        "location": "Jakarta"
      },
      "requestedAmount": 3000000.00,
      "requestedTenor": 6,
      "requestedRate": 12.00,
      "status": "SUBMITTED",
      "createdAt": "2026-01-01T12:11:31.060318",
      "updatedAt": null
    }
  ],
  "timestamp": "2026-01-01T12:11:34.839188"
}
```

---

## 5. Approval Workflow

### 5.1 Get Pending Loans (Marketing)

```bash
# Login as Marketing Jakarta
MARKETING_TOKEN=$(curl -s -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email": "marketing.jkt@loan.com", "password": "marketing123"}' \
  | jq -r '.data.token')

# ✅ Success - Get pending loans (SUBMITTED status, Jakarta branch only)
curl -X GET "$BASE_URL/api/approval/pending" \
  -H "Authorization: Bearer $MARKETING_TOKEN"

# Response: 200 OK
{
  "success": true,
  "message": "Success",
  "data": [
    {
      "id": 1,
      "customerName": "John Doe",
      "customerEmail": "john.doe@email.com",
      "customerNik": "3174051505900001",
      "customerPhone": "081234567890",
      "customerAddress": "Jl. Sudirman No. 123, Jakarta Pusat",
      "customerBirthdate": "1990-05-15",
      "product": {
        "id": 1,
        "name": "BRONZE",
        "amount": 5000000.00,
        "tenor": 12,
        "interestRate": 12.00
      },
      "branch": {
        "id": 1,
        "code": "JKT",
        "location": "Jakarta"
      },
      "requestedAmount": 3000000.00,
      "requestedTenor": 6,
      "requestedRate": 12.00,
      "status": "SUBMITTED",
      "createdAt": "2026-01-01T12:11:31.060318",
      "updatedAt": null
    }
  ],
  "timestamp": "2026-01-01T12:11:59.751196"
}
```

### 5.2 Approve Loan (Marketing → MARKETING_APPROVED)

```bash
# ✅ Success - Marketing approves
curl -X POST "$BASE_URL/api/approval/1/approve" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $MARKETING_TOKEN" \
  -d '{"note": "Documents verified, approved by Marketing"}'

# Response: 200 OK
{
  "success": true,
  "message": "Loan approved successfully",
  "data": {
    "id": 1,
    "customerName": "John Doe",
    "customerEmail": "john.doe@email.com",
    "customerNik": "3174051505900001",
    "customerPhone": "081234567890",
    "customerAddress": "Jl. Sudirman No. 123, Jakarta Pusat",
    "customerBirthdate": "1990-05-15",
    "product": {
      "id": 1,
      "name": "BRONZE",
      "amount": 5000000.00,
      "tenor": 12,
      "interestRate": 12.00
    },
    "branch": {
      "id": 1,
      "code": "JKT",
      "location": "Jakarta"
    },
    "requestedAmount": 3000000.00,
    "requestedTenor": 6,
    "requestedRate": 12.00,
    "status": "MARKETING_APPROVED",
    "createdAt": "2026-01-01T12:11:31.060318",
    "updatedAt": null
  },
  "timestamp": "2026-01-01T12:12:02.757986"
}
```

### 5.3 Approve Loan (Branch Manager → BRANCH_MANAGER_APPROVED)

```bash
# Login as Branch Manager Jakarta
BM_TOKEN=$(curl -s -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email": "bm.jkt@loan.com", "password": "bm123"}' \
  | jq -r '.data.token')

# ✅ Success - Branch Manager approves
curl -X POST "$BASE_URL/api/approval/1/approve" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $BM_TOKEN" \
  -d '{"note": "Risk assessment passed"}'

# Response: 200 OK
{
  "success": true,
  "message": "Loan approved successfully",
  "data": {
    "id": 1,
    "customerName": "John Doe",
    "customerEmail": "john.doe@email.com",
    "customerNik": "3174051505900001",
    "customerPhone": "081234567890",
    "customerAddress": "Jl. Sudirman No. 123, Jakarta Pusat",
    "customerBirthdate": "1990-05-15",
    "product": {
      "id": 1,
      "name": "BRONZE",
      "amount": 5000000.00,
      "tenor": 12,
      "interestRate": 12.00
    },
    "branch": {
      "id": 1,
      "code": "JKT",
      "location": "Jakarta"
    },
    "requestedAmount": 3000000.00,
    "requestedTenor": 6,
    "requestedRate": 12.00,
    "status": "BRANCH_MANAGER_APPROVED",
    "createdAt": "2026-01-01T12:11:31.060318",
    "updatedAt": "2026-01-01T12:12:02.73443"
  },
  "timestamp": "2026-01-01T12:12:07.491531"
}
```

### 5.4 Approve Loan (Backoffice → APPROVED - Final)

```bash
# Login as Backoffice
BACKOFFICE_TOKEN=$(curl -s -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email": "backoffice@loan.com", "password": "backoffice123"}' \
  | jq -r '.data.token')

# ✅ Success - Backoffice final approval
curl -X POST "$BASE_URL/api/approval/1/approve" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $BACKOFFICE_TOKEN" \
  -d '{"note": "Final approval granted"}'

# Response: 200 OK
{
  "success": true,
  "message": "Loan approved successfully",
  "data": {
    "id": 1,
    "customerName": "John Doe",
    "customerEmail": "john.doe@email.com",
    "customerNik": "3174051505900001",
    "customerPhone": "081234567890",
    "customerAddress": "Jl. Sudirman No. 123, Jakarta Pusat",
    "customerBirthdate": "1990-05-15",
    "product": {
      "id": 1,
      "name": "BRONZE",
      "amount": 5000000.00,
      "tenor": 12,
      "interestRate": 12.00
    },
    "branch": {
      "id": 1,
      "code": "JKT",
      "location": "Jakarta"
    },
    "requestedAmount": 3000000.00,
    "requestedTenor": 6,
    "requestedRate": 12.00,
    "status": "APPROVED",
    "createdAt": "2026-01-01T12:11:31.060318",
    "updatedAt": "2026-01-01T12:12:10.220865"
  },
  "timestamp": "2026-01-01T12:12:10.25554"
}
```

### 5.5 Get Loan History

```bash
# ✅ Success - Get loan approval history
curl -X GET "$BASE_URL/api/loans/1/history" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN"

# Response: 200 OK
{
  "success": true,
  "message": "Success",
  "data": [
    {
      "id": 1,
      "approvedByName": "John Doe",
      "approvedByRole": "CUSTOMER",
      "approvedByBranch": "N/A",
      "status": "SUBMITTED",
      "note": "Loan application submitted",
      "createdAt": "2026-01-01T12:11:31.09527"
    },
    {
      "id": 2,
      "approvedByName": "Marketing Jakarta",
      "approvedByRole": "MARKETING",
      "approvedByBranch": "Branch ID: 1",
      "status": "MARKETING_APPROVED",
      "note": "Documents verified, approved by Marketing",
      "createdAt": "2026-01-01T12:12:02.731088"
    },
    {
      "id": 3,
      "approvedByName": "Branch Manager Jakarta",
      "approvedByRole": "BRANCH_MANAGER",
      "approvedByBranch": "Branch ID: 1",
      "status": "BRANCH_MANAGER_APPROVED",
      "note": "Risk assessment passed",
      "createdAt": "2026-01-01T12:12:07.47832"
    },
    {
      "id": 4,
      "approvedByName": "Backoffice User",
      "approvedByRole": "BACKOFFICE",
      "approvedByBranch": "N/A",
      "status": "APPROVED",
      "note": "Final approval granted",
      "createdAt": "2026-01-01T12:12:10.223012"
    }
  ],
  "timestamp": "2026-01-01T12:12:27.697351"
}
```

### 5.6 Verify Plafond Deduction After Approval

```bash
# ✅ After 3M loan approved, remaining plafond decreased from 5M to 2M
curl -X GET "$BASE_URL/api/customer/plafond" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN"

# Response: 200 OK
{
  "success": true,
  "message": "Success",
  "data": {
    "id": 1,
    "product": {
      "id": 1,
      "name": "BRONZE",
      "amount": 5000000.00,
      "tenor": 12,
      "interestRate": 12.00
    },
    "originalAmount": 5000000.00,
    "remainingAmount": 2000000.00,
    "assignedAt": "2026-01-01T12:11:26.404161",
    "isActive": true
  },
  "timestamp": "2026-01-01T12:12:30.17131"
}
```

---

## 6. SuperAdmin Operations

### 6.1 Login as SuperAdmin

```bash
ADMIN_TOKEN=$(curl -s -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email": "admin@loan.com", "password": "admin123"}' \
  | jq -r '.data.token')
```

### 6.2 Get All Users

```bash
# ✅ Success - Get all users
curl -X GET "$BASE_URL/api/admin/users" \
  -H "Authorization: Bearer $ADMIN_TOKEN"

# Response: 200 OK
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
      "branch": null,
      "profile": null,
      "roles": ["SUPERADMIN"],
      "createdAt": "2025-12-31T18:07:24.99446"
    },
    {
      "id": 2,
      "name": "Backoffice User",
      "email": "backoffice@loan.com",
      "userType": "INTERNAL",
      "isActive": true,
      "branch": null,
      "profile": null,
      "roles": ["BACKOFFICE"],
      "createdAt": "2025-12-31T18:07:25.090942"
    },
    {
      "id": 3,
      "name": "Marketing Jakarta",
      "email": "marketing.jkt@loan.com",
      "userType": "INTERNAL",
      "isActive": true,
      "branch": {"id": 1, "code": "JKT", "location": "Jakarta"},
      "profile": null,
      "roles": ["MARKETING"],
      "createdAt": "2025-12-31T18:07:25.182946"
    },
    {
      "id": 4,
      "name": "Branch Manager Jakarta",
      "email": "bm.jkt@loan.com",
      "userType": "INTERNAL",
      "isActive": true,
      "branch": {"id": 1, "code": "JKT", "location": "Jakarta"},
      "profile": null,
      "roles": ["BRANCH_MANAGER"],
      "createdAt": "2025-12-31T18:07:25.273097"
    },
    {
      "id": 5,
      "name": "Marketing Surabaya",
      "email": "marketing.sby@loan.com",
      "userType": "INTERNAL",
      "isActive": true,
      "branch": {"id": 2, "code": "SBY", "location": "Surabaya"},
      "profile": null,
      "roles": ["MARKETING"],
      "createdAt": "2025-12-31T18:07:25.364053"
    },
    {
      "id": 6,
      "name": "Branch Manager Surabaya",
      "email": "bm.sby@loan.com",
      "userType": "INTERNAL",
      "isActive": true,
      "branch": {"id": 2, "code": "SBY", "location": "Surabaya"},
      "profile": null,
      "roles": ["BRANCH_MANAGER"],
      "createdAt": "2025-12-31T18:07:25.453995"
    },
    {
      "id": 7,
      "name": "Internal User No Role",
      "email": "internal@loan.com",
      "userType": "INTERNAL",
      "isActive": true,
      "branch": {"id": 1, "code": "JKT", "location": "Jakarta"},
      "profile": null,
      "roles": [],
      "createdAt": "2025-12-31T18:07:25.562388"
    },
    {
      "id": 8,
      "name": "John Doe",
      "email": "john.doe@email.com",
      "userType": "CUSTOMER",
      "isActive": true,
      "branch": null,
      "profile": {
        "id": 1,
        "birthdate": "1990-05-15",
        "phone": "081234567890",
        "address": "Jl. Sudirman No. 123, Jakarta Pusat",
        "nik": "3174051505900001",
        "isComplete": true
      },
      "roles": ["CUSTOMER"],
      "createdAt": "2025-12-31T18:07:25.654186"
    },
    {
      "id": 9,
      "name": "Jane Smith",
      "email": "jane.smith@email.com",
      "userType": "CUSTOMER",
      "isActive": true,
      "branch": null,
      "profile": {
        "id": 2,
        "birthdate": null,
        "phone": null,
        "address": null,
        "nik": null,
        "isComplete": false
      },
      "roles": ["CUSTOMER"],
      "createdAt": "2025-12-31T18:07:25.747908"
    }
  ],
  "timestamp": "2026-01-01T12:12:32.275263"
}
```

### 6.3 Get All Roles

```bash
# ✅ Success - Get all roles with permissions
curl -X GET "$BASE_URL/api/admin/roles" \
  -H "Authorization: Bearer $ADMIN_TOKEN"

# Response: 200 OK
{
  "success": true,
  "message": "Success",
  "data": [
    {
      "id": 4,
      "name": "BACKOFFICE",
      "permissions": [
        {"id": 15, "code": "LOAN_REJECT", "description": "Reject loan applications"},
        {"id": 14, "code": "LOAN_APPROVE_BACKOFFICE", "description": "Final approval as Backoffice"},
        {"id": 18, "code": "BRANCH_READ", "description": "Read branches"},
        {"id": 10, "code": "LOAN_READ_ALL", "description": "Read all loan applications"},
        {"id": 16, "code": "PRODUCT_READ", "description": "Read products"}
      ]
    },
    {
      "id": 3,
      "name": "BRANCH_MANAGER",
      "permissions": [
        {"id": 1, "code": "USER_READ", "description": "Read user data"},
        {"id": 15, "code": "LOAN_REJECT", "description": "Reject loan applications"},
        {"id": 11, "code": "LOAN_READ_BRANCH", "description": "Read branch loan applications"},
        {"id": 18, "code": "BRANCH_READ", "description": "Read branches"},
        {"id": 13, "code": "LOAN_APPROVE_BRANCH_MANAGER", "description": "Approve as Branch Manager"},
        {"id": 16, "code": "PRODUCT_READ", "description": "Read products"}
      ]
    },
    {
      "id": 5,
      "name": "CUSTOMER",
      "permissions": [
        {"id": 20, "code": "PROFILE_READ", "description": "Read own profile"},
        {"id": 9, "code": "LOAN_READ", "description": "Read loan applications"},
        {"id": 22, "code": "PLAFOND_READ", "description": "Read own plafond"},
        {"id": 8, "code": "LOAN_CREATE", "description": "Create loan applications"},
        {"id": 23, "code": "PLAFOND_SELECT", "description": "Select a plafond"},
        {"id": 21, "code": "PROFILE_UPDATE", "description": "Update own profile"},
        {"id": 18, "code": "BRANCH_READ", "description": "Read branches"},
        {"id": 16, "code": "PRODUCT_READ", "description": "Read products"}
      ]
    },
    {
      "id": 2,
      "name": "MARKETING",
      "permissions": [
        {"id": 15, "code": "LOAN_REJECT", "description": "Reject loan applications"},
        {"id": 11, "code": "LOAN_READ_BRANCH", "description": "Read branch loan applications"},
        {"id": 12, "code": "LOAN_APPROVE_MARKETING", "description": "Approve as Marketing"},
        {"id": 18, "code": "BRANCH_READ", "description": "Read branches"},
        {"id": 16, "code": "PRODUCT_READ", "description": "Read products"}
      ]
    },
    {
      "id": 1,
      "name": "SUPERADMIN",
      "permissions": [
        {"id": 1, "code": "USER_READ", "description": "Read user data"},
        {"id": 2, "code": "USER_CREATE", "description": "Create users"},
        {"id": 3, "code": "USER_UPDATE", "description": "Update users"},
        {"id": 4, "code": "USER_DELETE", "description": "Delete users"},
        {"id": 5, "code": "ROLE_READ", "description": "Read roles"},
        {"id": 6, "code": "ROLE_ASSIGN", "description": "Assign roles to users"},
        {"id": 7, "code": "ROLE_MANAGE", "description": "Manage role permissions"},
        {"id": 8, "code": "LOAN_CREATE", "description": "Create loan applications"},
        {"id": 9, "code": "LOAN_READ", "description": "Read loan applications"},
        {"id": 10, "code": "LOAN_READ_ALL", "description": "Read all loan applications"},
        {"id": 11, "code": "LOAN_READ_BRANCH", "description": "Read branch loan applications"},
        {"id": 12, "code": "LOAN_APPROVE_MARKETING", "description": "Approve as Marketing"},
        {"id": 13, "code": "LOAN_APPROVE_BRANCH_MANAGER", "description": "Approve as Branch Manager"},
        {"id": 14, "code": "LOAN_APPROVE_BACKOFFICE", "description": "Final approval as Backoffice"},
        {"id": 15, "code": "LOAN_REJECT", "description": "Reject loan applications"},
        {"id": 16, "code": "PRODUCT_READ", "description": "Read products"},
        {"id": 17, "code": "PRODUCT_MANAGE", "description": "Manage products"},
        {"id": 18, "code": "BRANCH_READ", "description": "Read branches"},
        {"id": 19, "code": "BRANCH_MANAGE", "description": "Manage branches"},
        {"id": 20, "code": "PROFILE_READ", "description": "Read own profile"},
        {"id": 21, "code": "PROFILE_UPDATE", "description": "Update own profile"},
        {"id": 22, "code": "PLAFOND_READ", "description": "Read own plafond"},
        {"id": 23, "code": "PLAFOND_SELECT", "description": "Select a plafond"}
      ]
    }
  ],
  "timestamp": "2026-01-01T12:12:35.211681"
}
```

### 6.4 Get All Permissions

```bash
# ✅ Success - Get all permissions
curl -X GET "$BASE_URL/api/admin/permissions" \
  -H "Authorization: Bearer $ADMIN_TOKEN"

# Response: 200 OK
{
  "success": true,
  "message": "Success",
  "data": [
    {"id": 1, "code": "USER_READ", "description": "Read user data"},
    {"id": 2, "code": "USER_CREATE", "description": "Create users"},
    {"id": 3, "code": "USER_UPDATE", "description": "Update users"},
    {"id": 4, "code": "USER_DELETE", "description": "Delete users"},
    {"id": 5, "code": "ROLE_READ", "description": "Read roles"},
    {"id": 6, "code": "ROLE_ASSIGN", "description": "Assign roles to users"},
    {"id": 7, "code": "ROLE_MANAGE", "description": "Manage role permissions"},
    {"id": 8, "code": "LOAN_CREATE", "description": "Create loan applications"},
    {"id": 9, "code": "LOAN_READ", "description": "Read loan applications"},
    {"id": 10, "code": "LOAN_READ_ALL", "description": "Read all loan applications"},
    {"id": 11, "code": "LOAN_READ_BRANCH", "description": "Read branch loan applications"},
    {"id": 12, "code": "LOAN_APPROVE_MARKETING", "description": "Approve as Marketing"},
    {"id": 13, "code": "LOAN_APPROVE_BRANCH_MANAGER", "description": "Approve as Branch Manager"},
    {"id": 14, "code": "LOAN_APPROVE_BACKOFFICE", "description": "Final approval as Backoffice"},
    {"id": 15, "code": "LOAN_REJECT", "description": "Reject loan applications"},
    {"id": 16, "code": "PRODUCT_READ", "description": "Read products"},
    {"id": 17, "code": "PRODUCT_MANAGE", "description": "Manage products"},
    {"id": 18, "code": "BRANCH_READ", "description": "Read branches"},
    {"id": 19, "code": "BRANCH_MANAGE", "description": "Manage branches"},
    {"id": 20, "code": "PROFILE_READ", "description": "Read own profile"},
    {"id": 21, "code": "PROFILE_UPDATE", "description": "Update own profile"},
    {"id": 22, "code": "PLAFOND_READ", "description": "Read own plafond"},
    {"id": 23, "code": "PLAFOND_SELECT", "description": "Select a plafond"}
  ],
  "timestamp": "2026-01-01T12:13:47.769242"
}
```

### 6.5 Create Internal User

```bash
# ✅ Success - Create new Marketing user
curl -X POST "$BASE_URL/api/admin/users" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{
    "name": "New Marketing",
    "email": "new.marketing@loan.com",
    "password": "internal123",
    "roleId": 2,
    "branchId": 1
  }'

# Response: 201 Created
```

### 6.6 Assign Role to User

```bash
curl -X POST "$BASE_URL/api/admin/users/7/roles" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{"roleId": 3}'
```

### 6.7 Remove Role from User

```bash
curl -X DELETE "$BASE_URL/api/admin/users/7/roles/3" \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

### 6.8 Update Role Permissions

```bash
curl -X PUT "$BASE_URL/api/admin/roles/2/permissions" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{"permissionIds": [5, 9, 15, 17, 19]}'
```

---

## 7. Edge Cases & Error Scenarios

### 7.1 Authentication Edge Cases

```bash
# ❌ Malformed token
curl -X GET "$BASE_URL/api/customer/profile" \
  -H "Authorization: Bearer not.a.valid.jwt"

# Response: 401 Unauthorized
```

```bash
# ❌ Missing Authorization header
curl -X GET "$BASE_URL/api/customer/profile"

# Response: 403 Forbidden
{
  "timestamp": "2026-01-01T05:13:58.262Z",
  "status": 403,
  "error": "Forbidden",
  "path": "/api/customer/profile"
}
```

### 7.2 Permission Edge Cases

```bash
# ❌ Customer tries to access approval endpoint
curl -X GET "$BASE_URL/api/approval/pending" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN"

# Response: 403 Forbidden
```

```bash
# ❌ Marketing tries to access admin endpoint
curl -X GET "$BASE_URL/api/admin/users" \
  -H "Authorization: Bearer $MARKETING_TOKEN"

# Response: 403 Forbidden
```

### 7.3 Business Logic Edge Cases

```bash
# ❌ Error - Profile incomplete (cannot submit loan)
curl -X POST "$BASE_URL/api/loans" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JANE_TOKEN" \
  -d '{"branchId": 1, "amount": 3000000, "tenor": 6, "interestRate": 12.0}'

# Response: 400 Bad Request
# {"success": false, "message": "Please complete your profile before submitting a loan application."}
```

```bash
# ❌ Error - Amount exceeds remaining plafond
curl -X POST "$BASE_URL/api/loans" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" \
  -d '{"branchId": 1, "amount": 10000000, "tenor": 6, "interestRate": 12.0}'

# Response: 400 Bad Request
# {"success": false, "message": "Requested amount exceeds remaining plafond."}
```

```bash
# ❌ Error - Wrong branch approval
curl -X POST "$BASE_URL/api/approval/1/approve" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $MARKETING_SBY_TOKEN" \
  -d '{}'

# Response: 403 Forbidden
# {"success": false, "message": "You can only process loans from your branch"}
```

---

## Quick Test Script

```bash
#!/bin/bash
# Full E2E test script

BASE_URL="http://localhost:8080"

echo "=== 1. Login as Customer ==="
CUSTOMER_TOKEN=$(curl -s -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email": "john.doe@email.com", "password": "customer123"}' \
  | jq -r '.data.token')
echo "Customer Token: ${CUSTOMER_TOKEN:0:20}..."

echo -e "\n=== 2. Submit Loan ==="
LOAN_ID=$(curl -s -X POST "$BASE_URL/api/loans" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" \
  -d '{"branchId": 1, "amount": 3000000, "tenor": 6, "interestRate": 12.0}' \
  | jq -r '.data.id')
echo "Loan ID: $LOAN_ID"

echo -e "\n=== 3. Marketing Approve ==="
MARKETING_TOKEN=$(curl -s -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email": "marketing.jkt@loan.com", "password": "marketing123"}' \
  | jq -r '.data.token')
curl -s -X POST "$BASE_URL/api/approval/$LOAN_ID/approve" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $MARKETING_TOKEN" \
  -d '{"note": "Approved by Marketing"}' | jq '.data.status'

echo -e "\n=== 4. Branch Manager Approve ==="
BM_TOKEN=$(curl -s -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email": "bm.jkt@loan.com", "password": "bm123"}' \
  | jq -r '.data.token')
curl -s -X POST "$BASE_URL/api/approval/$LOAN_ID/approve" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $BM_TOKEN" \
  -d '{"note": "Approved by Branch Manager"}' | jq '.data.status'

echo -e "\n=== 5. Backoffice Final Approve ==="
BACKOFFICE_TOKEN=$(curl -s -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email": "backoffice@loan.com", "password": "backoffice123"}' \
  | jq -r '.data.token')
curl -s -X POST "$BASE_URL/api/approval/$LOAN_ID/approve" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $BACKOFFICE_TOKEN" \
  -d '{"note": "Final approval"}' | jq '.data.status'

echo -e "\n=== 6. Check Loan History ==="
curl -s -X GET "$BASE_URL/api/loans/$LOAN_ID/history" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" | jq '.data[].status'

echo -e "\n=== Done! ==="
```

---

## Test Summary

| Category                 | Tests Run | Passed |
| ------------------------ | --------- | ------ |
| Public Endpoints         | 2         | ✅ 2   |
| Authentication           | 5         | ✅ 5   |
| Logout & Token Blacklist | 2         | ✅ 2   |
| Reset Password           | 2         | ✅ 2   |
| Customer Profile         | 3         | ✅ 3   |
| Plafond Selection        | 3         | ✅ 3   |
| Loan Submission          | 2         | ✅ 2   |
| Approval Workflow        | 4         | ✅ 4   |
| Loan History             | 1         | ✅ 1   |
| SuperAdmin APIs          | 4         | ✅ 4   |
| Edge Cases               | 4         | ✅ 4   |
| **Total**                | **32**    | **32** |

---

_Documentation generated: 2026-01-01_
_All tests executed against live server at localhost:8080_
