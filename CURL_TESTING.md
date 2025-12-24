# cURL Testing Guide

> End-to-end testing with cURL commands and edge cases

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

| Email                    | Password      | Role                        |
| ------------------------ | ------------- | --------------------------- |
| `john.doe@email.com`     | `customer123` | CUSTOMER (complete profile) |
| `jane.smith@email.com`   | `customer123` | CUSTOMER (empty profile)    |
| `marketing.jkt@loan.com` | `internal123` | MARKETING (Jakarta)         |
| `bm.jkt@loan.com`        | `internal123` | BRANCH_MANAGER (Jakarta)    |
| `marketing.sby@loan.com` | `internal123` | MARKETING (Surabaya)        |
| `bm.sby@loan.com`        | `internal123` | BRANCH_MANAGER (Surabaya)   |
| `backoffice@loan.com`    | `internal123` | BACKOFFICE                  |
| `admin@loan.com`         | `admin123`    | SUPERADMIN                  |

---

## 2. Authentication Flow

### 2.1 Register New Customer

```bash
# ✅ Success - Register new customer
curl -X POST "$BASE_URL/api/auth/register" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test User",
    "email": "test.user@email.com",
    "password": "password123"
  }'

# Response: 201 Created
# {
#   "success": true,
#   "data": {
#     "token": "eyJhbGc...",
#     "roles": ["CUSTOMER"],
#     "permissions": ["LOAN_CREATE", "LOAN_READ", ...]
#   }
# }
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
# {"success": false, "message": "Email already registered"}
```

```bash
# ❌ Error - Invalid email format
curl -X POST "$BASE_URL/api/auth/register" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Invalid Email",
    "email": "not-an-email",
    "password": "password123"
  }'

# Response: 400 Bad Request
# {"success": false, "message": "Validation failed", "errors": ["email: must be a valid email"]}
```

```bash
# ❌ Error - Password too short
curl -X POST "$BASE_URL/api/auth/register" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Short Password",
    "email": "short.pass@email.com",
    "password": "123"
  }'

# Response: 400 Bad Request
# {"success": false, "message": "Validation failed", "errors": ["password: size must be between 6 and 100"]}
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

# Save token for subsequent requests
CUSTOMER_TOKEN="<token_from_response>"
```

```bash
# ✅ Success - Marketing login
curl -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "marketing.jkt@loan.com",
    "password": "internal123"
  }'

MARKETING_TOKEN="<token_from_response>"
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
# {"success": false, "message": "Invalid credentials"}
```

```bash
# ❌ Error - User not found
curl -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "nonexistent@email.com",
    "password": "password123"
  }'

# Response: 401 Unauthorized
```

### 2.3 Change Password

```bash
# ✅ Success - Change password
curl -X POST "$BASE_URL/api/auth/change-password" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" \
  -d '{
    "currentPassword": "customer123",
    "newPassword": "newpassword456",
    "confirmPassword": "newpassword456"
  }'

# Response: 200 OK
# {"success": true, "message": "Password changed successfully"}
```

```bash
# ❌ Error - Wrong current password
curl -X POST "$BASE_URL/api/auth/change-password" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" \
  -d '{
    "currentPassword": "wrongcurrent",
    "newPassword": "newpassword456",
    "confirmPassword": "newpassword456"
  }'

# Response: 400 Bad Request
# {"success": false, "message": "Current password is incorrect"}
```

```bash
# ❌ Error - Passwords don't match
curl -X POST "$BASE_URL/api/auth/change-password" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" \
  -d '{
    "currentPassword": "customer123",
    "newPassword": "newpassword456",
    "confirmPassword": "differentpassword"
  }'

# Response: 400 Bad Request
# {"success": false, "message": "New password and confirm password do not match"}
```

---

## 3. Customer Flow

### 3.1 Get Profile

```bash
# ✅ Success - Get profile (complete)
curl -X GET "$BASE_URL/api/customer/profile" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN"

# Response: 200 OK with profile data
```

```bash
# ❌ Error - No token
curl -X GET "$BASE_URL/api/customer/profile"

# Response: 401 Unauthorized
```

```bash
# ❌ Error - Invalid token
curl -X GET "$BASE_URL/api/customer/profile" \
  -H "Authorization: Bearer invalid_token_here"

# Response: 401 Unauthorized
```

### 3.2 Update Profile

```bash
# ✅ Success - Update profile (complete)
curl -X PUT "$BASE_URL/api/customer/profile" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" \
  -d '{
    "nik": "3201234567890001",
    "birthdate": "1990-05-15",
    "phoneNumber": "+6281234567890",
    "address": "Jl. Sudirman No. 123, Jakarta"
  }'

# Response: 200 OK
# {"success": true, "data": {"isComplete": true}}
```

```bash
# ❌ Error - Invalid NIK (not 16 digits)
curl -X PUT "$BASE_URL/api/customer/profile" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" \
  -d '{
    "nik": "123456",
    "birthdate": "1990-05-15",
    "phoneNumber": "+6281234567890",
    "address": "Jakarta"
  }'

# Response: 400 Bad Request
# {"success": false, "errors": ["nik: NIK must be exactly 16 digits"]}
```

### 3.3 Get Products (Public)

```bash
# ✅ Success - No auth required
curl -X GET "$BASE_URL/api/products"

# Response: 200 OK
# {"success": true, "data": [{"id": 1, "name": "BRONZE", ...}, ...]}
```

### 3.4 Get Branches (Public)

```bash
# ✅ Success - No auth required
curl -X GET "$BASE_URL/api/branches"

# Response: 200 OK
```

---

## 4. Loan Application Flow

### 4.1 Select Plafond

```bash
# ✅ Success - Select plafond (BRONZE product)
curl -X POST "$BASE_URL/api/customer/plafond" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" \
  -d '{
    "productId": 1
  }'

# Response: 200 OK
# {"success": true, "data": {"product": {"name": "BRONZE", "amount": 5000000}}}
```

```bash
# ❌ Error - Already has plafond
curl -X POST "$BASE_URL/api/customer/plafond" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" \
  -d '{
    "productId": 2
  }'

# Response: 400 Bad Request
# {"success": false, "message": "You already have an active plafond. Cannot select another one."}
```

```bash
# ❌ Error - Product not found
curl -X POST "$BASE_URL/api/customer/plafond" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" \
  -d '{
    "productId": 999
  }'

# Response: 404 Not Found
# {"success": false, "message": "Product not found"}
```

### 4.2 Get My Plafond

```bash
# ✅ Success - Get active plafond
curl -X GET "$BASE_URL/api/customer/plafond" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN"
```

```bash
# ❌ Error - No plafond selected
# (Login as jane.smith who has no plafond)
curl -X GET "$BASE_URL/api/customer/plafond" \
  -H "Authorization: Bearer $JANE_TOKEN"

# Response: 404 Not Found
# {"success": false, "message": "You don't have an active plafond. Please select a plafond first."}
```

### 4.3 Submit Loan

```bash
# ✅ Success - Submit loan within limits
# (Plafond BRONZE: max 5M, max 12 months, min 12% rate)
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
# {"success": true, "data": {"id": 1, "status": "SUBMITTED"}}
```

```bash
# ❌ Error - No plafond selected
# (Login as user without plafond)
curl -X POST "$BASE_URL/api/loans" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $NO_PLAFOND_TOKEN" \
  -d '{
    "branchId": 1,
    "amount": 3000000,
    "tenor": 6,
    "interestRate": 12.0
  }'

# Response: 400 Bad Request
# {"success": false, "message": "Please select a plafond first before submitting a loan application."}
```

```bash
# ❌ Error - Profile incomplete
# (Login as jane.smith with empty profile)
curl -X POST "$BASE_URL/api/loans" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JANE_TOKEN" \
  -d '{
    "branchId": 1,
    "amount": 3000000,
    "tenor": 6,
    "interestRate": 12.0
  }'

# Response: 400 Bad Request
# {"success": false, "message": "Please complete your profile before submitting a loan application. Required fields: NIK, birthdate, phone, and address."}
```

```bash
# ❌ Error - Amount exceeds plafond limit
# (BRONZE plafond max 5M, requesting 10M)
curl -X POST "$BASE_URL/api/loans" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" \
  -d '{
    "branchId": 1,
    "amount": 10000000,
    "tenor": 6,
    "interestRate": 12.0
  }'

# Response: 400 Bad Request
# {"success": false, "message": "Requested amount exceeds plafond limit. Maximum: Rp 5000000"}
```

```bash
# ❌ Error - Tenor exceeds plafond limit
# (BRONZE plafond max 12 months, requesting 24)
curl -X POST "$BASE_URL/api/loans" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" \
  -d '{
    "branchId": 1,
    "amount": 3000000,
    "tenor": 24,
    "interestRate": 12.0
  }'

# Response: 400 Bad Request
# {"success": false, "message": "Requested tenor exceeds plafond limit. Maximum: 12 months"}
```

```bash
# ❌ Error - Interest rate below minimum
# (BRONZE plafond min 12%, requesting 10%)
curl -X POST "$BASE_URL/api/loans" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" \
  -d '{
    "branchId": 1,
    "amount": 3000000,
    "tenor": 6,
    "interestRate": 10.0
  }'

# Response: 400 Bad Request
# {"success": false, "message": "Interest rate cannot be lower than plafond minimum rate. Minimum: 12.0%"}
```

```bash
# ❌ Error - Branch not found
curl -X POST "$BASE_URL/api/loans" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" \
  -d '{
    "branchId": 999,
    "amount": 3000000,
    "tenor": 6,
    "interestRate": 12.0
  }'

# Response: 404 Not Found
# {"success": false, "message": "Branch not found"}
```

### 4.4 Get My Loans

```bash
# ✅ Success - Get all loans
curl -X GET "$BASE_URL/api/loans" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN"
```

### 4.5 Get Loan by ID

```bash
# ✅ Success - Get own loan
curl -X GET "$BASE_URL/api/loans/1" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN"
```

```bash
# ❌ Error - Access other user's loan
curl -X GET "$BASE_URL/api/loans/1" \
  -H "Authorization: Bearer $OTHER_CUSTOMER_TOKEN"

# Response: 403 Forbidden
# {"success": false, "message": "You don't have access to this loan application"}
```

### 4.6 Get Loan History

```bash
# ✅ Success - Get loan history
curl -X GET "$BASE_URL/api/loans/1/history" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN"
```

---

## 5. Approval Workflow

### 5.1 Get Pending Loans (Marketing)

```bash
# Login as Marketing Jakarta
curl -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email": "marketing.jkt@loan.com", "password": "internal123"}'

MARKETING_TOKEN="<token>"

# ✅ Success - Get pending loans (SUBMITTED status, Jakarta branch only)
curl -X GET "$BASE_URL/api/approval/pending" \
  -H "Authorization: Bearer $MARKETING_TOKEN"

# Returns loans with status=SUBMITTED and branch=Jakarta
```

### 5.2 Approve Loan (Marketing → MARKETING_APPROVED)

```bash
# ✅ Success - Marketing approves
curl -X POST "$BASE_URL/api/approval/1/approve" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $MARKETING_TOKEN" \
  -d '{
    "note": "Documents verified, approved"
  }'

# Response: 200 OK
# {"success": true, "data": {"status": "MARKETING_APPROVED"}}
```

```bash
# ❌ Error - Wrong status (already approved)
curl -X POST "$BASE_URL/api/approval/1/approve" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $MARKETING_TOKEN" \
  -d '{}'

# Response: 400 Bad Request
# {"success": false, "message": "Loan is not in the correct status for your approval. Current status: MARKETING_APPROVED, Expected: SUBMITTED"}
```

```bash
# ❌ Error - Different branch
# (Login as Marketing Surabaya, try to approve Jakarta loan)
curl -X POST "$BASE_URL/api/approval/1/approve" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $MARKETING_SBY_TOKEN" \
  -d '{}'

# Response: 403 Forbidden
# {"success": false, "message": "You can only process loans from your branch"}
```

### 5.3 Approve Loan (Branch Manager → BRANCH_MANAGER_APPROVED)

```bash
# Login as Branch Manager Jakarta
curl -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email": "bm.jkt@loan.com", "password": "internal123"}'

BM_TOKEN="<token>"

# ✅ Success - Branch Manager sees MARKETING_APPROVED loans
curl -X GET "$BASE_URL/api/approval/pending" \
  -H "Authorization: Bearer $BM_TOKEN"

# ✅ Success - Branch Manager approves
curl -X POST "$BASE_URL/api/approval/1/approve" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $BM_TOKEN" \
  -d '{
    "note": "Risk assessment passed"
  }'

# Response: 200 OK
# {"success": true, "data": {"status": "BRANCH_MANAGER_APPROVED"}}
```

### 5.4 Approve Loan (Backoffice → APPROVED - Final)

```bash
# Login as Backoffice
curl -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email": "backoffice@loan.com", "password": "internal123"}'

BACKOFFICE_TOKEN="<token>"

# ✅ Success - Backoffice sees BRANCH_MANAGER_APPROVED from ALL branches
curl -X GET "$BASE_URL/api/approval/pending" \
  -H "Authorization: Bearer $BACKOFFICE_TOKEN"

# ✅ Success - Backoffice final approval
curl -X POST "$BASE_URL/api/approval/1/approve" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $BACKOFFICE_TOKEN" \
  -d '{
    "note": "Final approval granted. Loan will be disbursed."
  }'

# Response: 200 OK
# {"success": true, "data": {"status": "APPROVED"}}
```

### 5.5 Reject Loan

```bash
# ✅ Success - Reject loan (any approval level)
curl -X POST "$BASE_URL/api/approval/1/reject" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $MARKETING_TOKEN" \
  -d '{
    "note": "Insufficient income documentation"
  }'

# Response: 200 OK
# {"success": true, "data": {"status": "MARKETING_REJECTED"}}
```

### 5.6 Return Loan (Backoffice Only)

```bash
# ✅ Success - Backoffice returns loan for revision
curl -X POST "$BASE_URL/api/approval/1/return" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $BACKOFFICE_TOKEN" \
  -d '{
    "note": "Please verify customer address again"
  }'

# Response: 200 OK
# {"success": true, "data": {"status": "RETURNED"}}
# Loan goes back to Marketing for re-review
```

```bash
# ❌ Error - Non-backoffice tries to return
curl -X POST "$BASE_URL/api/approval/1/return" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $MARKETING_TOKEN" \
  -d '{
    "note": "Cannot return"
  }'

# Response: 403 Forbidden
# {"success": false, "message": "Only Backoffice can return loan applications"}
```

---

## 6. SuperAdmin Operations

### 6.1 Login as SuperAdmin

```bash
curl -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email": "admin@loan.com", "password": "admin123"}'

ADMIN_TOKEN="<token>"
```

### 6.2 Create Internal User

```bash
# ✅ Success - Create Marketing user
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
```

```bash
# ❌ Error - Cannot create CUSTOMER via admin
curl -X POST "$BASE_URL/api/admin/users" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{
    "name": "Customer Via Admin",
    "email": "customer.admin@email.com",
    "password": "password123",
    "roleId": 5
  }'

# Response: 400 Bad Request
# {"success": false, "message": "Cannot create customer via admin endpoint. Use registration."}
```

### 6.3 Get All Users

```bash
curl -X GET "$BASE_URL/api/admin/users" \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

### 6.4 Assign Role to User

```bash
curl -X POST "$BASE_URL/api/admin/users/7/roles" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{
    "roleId": 3
  }'
```

### 6.5 Remove Role from User

```bash
curl -X DELETE "$BASE_URL/api/admin/users/7/roles/3" \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

### 6.6 Update Role Permissions

```bash
# Get current permissions
curl -X GET "$BASE_URL/api/admin/permissions" \
  -H "Authorization: Bearer $ADMIN_TOKEN"

# Update Marketing role permissions
curl -X PUT "$BASE_URL/api/admin/roles/2/permissions" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{
    "permissionIds": [5, 9, 15, 17, 19]
  }'
```

---

## 7. Edge Cases & Error Scenarios

### 7.1 Authentication Edge Cases

```bash
# Expired token
# (Use old token after JWT expiration, default 24h)
curl -X GET "$BASE_URL/api/customer/profile" \
  -H "Authorization: Bearer <expired_token>"

# Response: 401 Unauthorized
```

```bash
# Malformed token
curl -X GET "$BASE_URL/api/customer/profile" \
  -H "Authorization: Bearer not.a.valid.jwt"

# Response: 401 Unauthorized
```

```bash
# Missing Authorization header
curl -X GET "$BASE_URL/api/customer/profile"

# Response: 401 Unauthorized
```

### 7.2 Permission Edge Cases

```bash
# Customer tries to access approval endpoint
curl -X GET "$BASE_URL/api/approval/pending" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN"

# Response: 403 Forbidden
```

```bash
# Marketing tries to access admin endpoint
curl -X GET "$BASE_URL/api/admin/users" \
  -H "Authorization: Bearer $MARKETING_TOKEN"

# Response: 403 Forbidden
```

### 7.3 Data Validation Edge Cases

```bash
# Empty request body
curl -X POST "$BASE_URL/api/auth/register" \
  -H "Content-Type: application/json" \
  -d '{}'

# Response: 400 Bad Request
# {"success": false, "errors": ["name: must not be blank", "email: must not be blank", ...]}
```

```bash
# Missing required field
curl -X POST "$BASE_URL/api/loans" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" \
  -d '{
    "amount": 3000000
  }'

# Response: 400 Bad Request
# {"success": false, "errors": ["branchId: must not be null", "tenor: must not be null", ...]}
```

```bash
# Negative amount
curl -X POST "$BASE_URL/api/loans" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" \
  -d '{
    "branchId": 1,
    "amount": -1000000,
    "tenor": 6,
    "interestRate": 12.0
  }'

# Response: 400 Bad Request
# {"success": false, "errors": ["amount: must be greater than 0"]}
```

### 7.4 Race Condition Scenarios

```bash
# Two approvers try to approve same loan simultaneously
# (Not handled - last one wins, but history is preserved)
```

### 7.5 Disabled User

```bash
# Login with disabled account
# (Admin sets user.isActive = false)
curl -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email": "disabled@email.com", "password": "password123"}'

# Response: 401 Unauthorized
# {"success": false, "message": "User account is disabled"}
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
  -d '{"email": "marketing.jkt@loan.com", "password": "internal123"}' \
  | jq -r '.data.token')
curl -s -X POST "$BASE_URL/api/approval/$LOAN_ID/approve" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $MARKETING_TOKEN" \
  -d '{"note": "Approved by Marketing"}' | jq '.data.status'

echo -e "\n=== 4. Branch Manager Approve ==="
BM_TOKEN=$(curl -s -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email": "bm.jkt@loan.com", "password": "internal123"}' \
  | jq -r '.data.token')
curl -s -X POST "$BASE_URL/api/approval/$LOAN_ID/approve" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $BM_TOKEN" \
  -d '{"note": "Approved by Branch Manager"}' | jq '.data.status'

echo -e "\n=== 5. Backoffice Final Approve ==="
BACKOFFICE_TOKEN=$(curl -s -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email": "backoffice@loan.com", "password": "internal123"}' \
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

_Documentation generated: 2025-12-24_
