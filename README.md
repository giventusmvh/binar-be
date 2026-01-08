# Loan Application & Multi-Level Approval System

A production-ready Spring Boot REST API for managing loan applications with multi-level approval workflow, role-based access control, and branch-based restrictions.

## Table of Contents

- [Project Overview](#project-overview)
- [Technology Stack](#technology-stack)
- [ERD Explanation](#erd-explanation)
- [Approval Flow](#approval-flow)
- [API Endpoints](#api-endpoints)
- [Authentication](#authentication)
- [How to Run](#how-to-run)
- [Postman Testing Guide](#postman-testing-guide)
- [Default Users](#default-users)

## Project Overview

This system provides:

- **Customer mobile app access** - Customers can register, browse products, and submit loan applications
- **Internal web dashboard** - Internal users manage and approve loan applications
- **Branch-based approval** - Marketing and Branch Managers are restricted to their branch
- **Role-based access control (RBAC)** - Fine-grained permission system
- **Approval history tracking** - Complete audit trail with snapshot data

## Technology Stack

| Technology      | Version     |
| --------------- | ----------- |
| Java            | 21          |
| Spring Boot     | 4.0.1       |
| Spring Security | 6.x         |
| Spring Data JPA | Hibernate   |
| SQL Server      | 2022        |
| JWT             | jjwt 0.12.3 |
| Build Tool      | Maven       |
| Lombok          | Latest      |

## ERD Explanation

### Core Entities

| Entity                       | Description                                |
| ---------------------------- | ------------------------------------------ |
| `users`                      | All users (customers and internal staff)   |
| `user_profiles`              | Customer personal information              |
| `user_plafonds`              | Customer credit limits/plafond selection   |
| `branch`                     | Physical branch locations                  |
| `roles`                      | System roles (SUPERADMIN, MARKETING, etc.) |
| `permissions`                | Granular access permissions                |
| `products`                   | Loan products (BRONZE to PLATINUM)         |
| `loan_applications`          | Customer loan requests                     |
| `loan_application_histories` | Approval audit trail                       |

### Relationships

```
users ──────────┬───────── 1:1 ─────────── user_profiles
                │
                ├───────── N:1 ─────────── branch
                │
                ├───────── N:M ─────────── roles
                │                            │
                │                            └──── N:M ── permissions
                │
                └───────── 1:N ─────────── loan_applications
                                                │
                                                ├──── N:1 ── products
                                                ├──── N:1 ── branch
                                                └──── 1:N ── loan_application_histories
```

### Key Constraints

- `users.email` - Unique
- `users.branch_id` - Nullable (customers don't belong to branches)
- `user_profiles.user_id` - Unique (one-to-one with users)
- Loan applications are always tied to a specific branch

## Approval Flow

```
┌─────────────────────────────────────────────────────────────────────┐
│                        LOAN APPROVAL WORKFLOW                        │
└─────────────────────────────────────────────────────────────────────┘

Customer submits loan
         │
         ▼
   ┌───────────┐
   │ SUBMITTED │
   └─────┬─────┘
         │
    MARKETING reviews (branch-restricted)
         │
    ┌────┴────┐
    │         │
    ▼         ▼
 APPROVE   REJECT ──────────────────────────────────┐
    │                                               │
    ▼                                               │
┌───────────────────┐                               │
│ MARKETING_APPROVED│                               │
└─────────┬─────────┘                               │
          │                                         │
    BRANCH_MANAGER reviews (branch-restricted)      │
          │                                         │
     ┌────┴────┐                                    │
     │         │                                    │
     ▼         ▼                                    │
  APPROVE   REJECT ─────────────────────────────────┤
     │                                              │
     ▼                                              │
┌──────────────────────────┐                        │
│ BRANCH_MANAGER_APPROVED  │                        │
└────────────┬─────────────┘                        │
             │                                      │
    BACKOFFICE reviews (all branches)               │
             │                                      │
        ┌────┴────┐                                 │
        │         │                                 │
        ▼         ▼                                 │
    APPROVE    REJECT ──────────────────────────────┤
        │                                           │
        ▼                                           │
┌──────────┐                                REJECTED│
│ APPROVED │                                        │
└──────────┘                                        │
```

### Approval Levels

| Level | Role           | Scope             | Actions                           |
| ----- | -------------- | ----------------- | --------------------------------- |
| 1     | MARKETING      | Branch-restricted | Approve → MARKETING_APPROVED      |
| 2     | BRANCH_MANAGER | Branch-restricted | Approve → BRANCH_MANAGER_APPROVED |
| 3     | BACKOFFICE     | All branches      | Approve → APPROVED                |

## API Endpoints

### Authentication (Public)

| Method | Endpoint             | Description             |
| ------ | -------------------- | ----------------------- |
| POST   | `/api/auth/register` | Register new customer   |
| POST   | `/api/auth/login`    | Login and get JWT token |

### Products & Branches (Public)

| Method | Endpoint        | Description            |
| ------ | --------------- | ---------------------- |
| GET    | `/api/products` | List all loan products |
| GET    | `/api/branches` | List all branches      |

### Customer (Requires CUSTOMER role)

| Method | Endpoint                  | Description                     |
| ------ | ------------------------- | ------------------------------- |
| GET    | `/api/customer/profile`   | Get current profile             |
| PUT    | `/api/customer/profile`   | Update profile                  |
| POST   | `/api/customer/plafond`   | Select plafond (credit limit)   |
| GET    | `/api/customer/plafond`   | Get my plafond                  |
| POST   | `/api/loans`              | Submit loan (amount/tenor/rate) |
| GET    | `/api/loans`              | Get my loans                    |
| GET    | `/api/loans/{id}`         | Get loan details                |
| GET    | `/api/loans/{id}/history` | Get approval history            |

### Approval (Requires MARKETING, BRANCH_MANAGER, or BACKOFFICE role)

| Method | Endpoint                     | Description       |
| ------ | ---------------------------- | ----------------- |
| GET    | `/api/approval/pending`      | Get pending loans |
| POST   | `/api/approval/{id}/approve` | Approve loan      |
| POST   | `/api/approval/{id}/reject`  | Reject loan       |

### Admin (Requires SUPERADMIN role)

| Method | Endpoint                                   | Description             |
| ------ | ------------------------------------------ | ----------------------- |
| GET    | `/api/admin/users`                         | List all users          |
| GET    | `/api/admin/users/{id}`                    | Get user details        |
| PUT    | `/api/admin/users/{id}`                    | Update user data        |
| PATCH  | `/api/admin/users/{id}/status`             | Update user status      |
| POST   | `/api/admin/users/{id}/roles`              | Assign role             |
| DELETE | `/api/admin/users/{userId}/roles/{roleId}` | Remove role             |
| GET    | `/api/admin/roles`                         | List all roles          |
| PUT    | `/api/admin/roles/{id}/permissions`        | Update role permissions |
| GET    | `/api/admin/permissions`                   | List all permissions    |

## Authentication

### JWT Token

All protected endpoints require a Bearer token in the Authorization header:

```
Authorization: Bearer <your_jwt_token>
```

### Login Flow

1. Call `POST /api/auth/login` with email and password
2. Receive JWT token in response
3. Include token in subsequent requests

### Response Format

All responses follow this format:

```json
{
  "success": true,
  "message": "Operation successful",
  "data": { ... },
  "timestamp": "2024-01-01T12:00:00"
}
```

Error responses:

```json
{
  "success": false,
  "message": "Error description",
  "errors": ["Validation error 1", "Validation error 2"],
  "timestamp": "2024-01-01T12:00:00"
}
```

## How to Run

### Prerequisites

1. **Java 21** - Ensure JDK 21 is installed
2. **SQL Server 2022** - Running on localhost:1433
3. **Maven** - For building the project

### Database Setup

1. Create a database named `binar-be`:

```sql
CREATE DATABASE [binar-be];
```

2. Update `application.properties` with your SQL Server credentials:

```properties
spring.datasource.url=jdbc:sqlserver://localhost:1433;databaseName=binar-be;encrypt=true;trustServerCertificate=true
spring.datasource.username=sa
spring.datasource.password=YourPassword123!
```

### Build & Run

```bash
# Clone the repository (if applicable)
cd binar-be

# Build the project
./mvnw clean compile

# Run the application
./mvnw spring-boot:run
```

The application will:

1. Start on port 8080
2. Auto-create all database tables (ddl-auto=update)
3. Seed initial data (branches, roles, permissions, users, products)

## Postman Testing Guide

### 1. Browse Products (No Auth)

```
GET http://localhost:8080/api/products
```

### 2. Register Customer

```
POST http://localhost:8080/api/auth/register
Content-Type: application/json

{
  "name": "John Doe",
  "email": "john@example.com",
  "password": "password123"
}
```

### 3. Login

```
POST http://localhost:8080/api/auth/login
Content-Type: application/json

{
  "email": "john@example.com",
  "password": "password123"
}
```

Save the token from the response.

### 4. Complete Profile (Required for Loan)

```
PUT http://localhost:8080/api/customer/profile
Authorization: Bearer <customer_token>
Content-Type: application/json

{
  "birthdate": "1990-01-15",
  "phone": "081234567890",
  "address": "Jl. Sudirman No. 123, Jakarta",
  "nik": "3174012345678901"
}
```

### 5. Select Plafond (Required before Loan)

```
POST http://localhost:8080/api/customer/plafond
Authorization: Bearer <customer_token>
Content-Type: application/json

{
  "productId": 1
}
```

### 6. Submit Loan Application

```
POST http://localhost:8080/api/loans
Authorization: Bearer <customer_token>
Content-Type: application/json

{
  "branchId": 1,
  "amount": 3000000,
  "tenor": 6,
  "interestRate": 12.00
}
```

> Note: amount ≤ plafond max, tenor ≤ plafond max, rate ≥ plafond min

### 6. Login as Marketing (Jakarta)

```
POST http://localhost:8080/api/auth/login
Content-Type: application/json

{
  "email": "marketing.jkt@loan.com",
  "password": "marketing123"
}
```

### 7. View Pending Loans

```
GET http://localhost:8080/api/approval/pending
Authorization: Bearer <marketing_token>
```

### 8. Approve as Marketing

```
POST http://localhost:8080/api/approval/{loanId}/approve
Authorization: Bearer <marketing_token>
Content-Type: application/json

{
  "note": "Documents verified"
}
```

### 9. Continue with Branch Manager and Backoffice

Repeat the login and approval steps with:

- Branch Manager: `bm.jkt@loan.com` / `bm123`
- Backoffice: `backoffice@loan.com` / `backoffice123`

### 10. Admin Operations

```
POST http://localhost:8080/api/auth/login
Content-Type: application/json

{
  "email": "admin@loan.com",
  "password": "admin123"
}
```

Then use the admin token to manage users and roles.

## Default Users

### Internal Users

| Role           | Email                  | Password      | Branch   |
| -------------- | ---------------------- | ------------- | -------- |
| SUPERADMIN     | admin@loan.com         | admin123      | -        |
| BACKOFFICE     | backoffice@loan.com    | backoffice123 | -        |
| MARKETING      | marketing.jkt@loan.com | marketing123  | Jakarta  |
| MARKETING      | marketing.sby@loan.com | marketing123  | Surabaya |
| BRANCH_MANAGER | bm.jkt@loan.com        | bm123         | Jakarta  |
| BRANCH_MANAGER | bm.sby@loan.com        | bm123         | Surabaya |
| (No Role)      | internal@loan.com      | internal123   | Jakarta  |

### Customer Users

| Email                | Password    | Profile Status | Can Submit Loan?                    |
| -------------------- | ----------- | -------------- | ----------------------------------- |
| john.doe@email.com   | customer123 | Complete       | ✅ Yes                              |
| jane.smith@email.com | customer123 | Empty          | ❌ No (must complete profile first) |

## Default Products

| Product  | Amount     | Tenor     | Interest Rate |
| -------- | ---------- | --------- | ------------- |
| BRONZE   | 5,000,000  | 12 months | 12.00%        |
| SILVER   | 10,000,000 | 24 months | 10.00%        |
| GOLD     | 25,000,000 | 36 months | 8.50%         |
| PLATINUM | 50,000,000 | 48 months | 7.00%         |
