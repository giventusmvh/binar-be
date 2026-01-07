# Business Data Flow Documentation

> Complete business process flow for Binar Loan Application System

---

## Table of Contents

1. [System Overview](#1-system-overview)
2. [User Types & Roles](#2-user-types--roles)
3. [Customer Journey](#3-customer-journey)
4. [Loan Approval Workflow](#4-loan-approval-workflow)
5. [Entity Relationships](#5-entity-relationships)
6. [State Diagrams](#6-state-diagrams)
7. [Business Rules](#7-business-rules)
8. [API Flow Sequences](#8-api-flow-sequences)

---

## 1. System Overview

Binar Loan Application adalah sistem pengajuan pinjaman dengan fitur:

- **Multi-level approval** (3 tingkat persetujuan)
- **Branch-based restriction** (Marketing & Branch Manager hanya bisa akses cabang sendiri)
- **Plafond/Credit Limit system** (Customer pilih limit kredit sebelum ajukan pinjaman)
- **Dynamic RBAC** (Superadmin bisa atur permissions per role)

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              MOBILE APP (Customer)                          │
│                                                                             │
│  ┌─────────────┐  ┌──────────────┐  ┌───────────────┐  ┌─────────────────┐ │
│  │  Register   │→ │Complete Profile│→│ Select Plafond │→│  Submit Loan    │ │
│  └─────────────┘  └──────────────┘  └───────────────┘  └─────────────────┘ │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                              BACKEND API                                     │
│                                                                             │
│  ┌──────────┐  ┌─────────────┐  ┌──────────────┐  ┌─────────────────────┐  │
│  │   Auth   │  │   Customer  │  │    Loan      │  │      Approval       │  │
│  │ Service  │  │   Service   │  │   Service    │  │      Service        │  │
│  └──────────┘  └─────────────┘  └──────────────┘  └─────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                          WEB DASHBOARD (Internal)                           │
│                                                                             │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐│
│  │  Marketing  │→ │Branch Manager│→ │  Backoffice │→ │    SuperAdmin       ││
│  │  (Level 1)  │  │  (Level 2)   │  │  (Level 3)  │  │   (Manage Users)    ││
│  └─────────────┘  └─────────────┘  └─────────────┘  └─────────────────────┘│
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. User Types & Roles

### User Types

| Type         | Description                            | Access Via    |
| ------------ | -------------------------------------- | ------------- |
| **CUSTOMER** | External user yang mengajukan pinjaman | Mobile App    |
| **INTERNAL** | Staff internal yang memproses pinjaman | Web Dashboard |

### Roles Hierarchy

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              SUPERADMIN                                      │
│                    (Full system access, manage users)                        │
└─────────────────────────────────────────────────────────────────────────────┘
                                    │
        ┌───────────────────────────┼───────────────────────────┐
        ▼                           ▼                           ▼
┌──────────────────┐  ┌──────────────────────┐  ┌──────────────────────────┐
│    BACKOFFICE    │  │   BRANCH_MANAGER     │  │       MARKETING          │
│  (Final Approve) │  │  (Level 2 Approve)   │  │   (Level 1 Approve)      │
│   All Branches   │  │   Branch Restricted  │  │    Branch Restricted     │
└──────────────────┘  └──────────────────────┘  └──────────────────────────┘
                                                            │
                                                            ▼
                                              ┌──────────────────────────┐
                                              │        CUSTOMER          │
                                              │    (Submit Loan Only)    │
                                              └──────────────────────────┘
```

### Role Permissions Summary

| Role           | Can See            | Can Do                           |
| -------------- | ------------------ | -------------------------------- |
| SUPERADMIN     | All data           | Manage users, roles, permissions |
| BACKOFFICE     | All branches loans | Final approve, reject            |
| BRANCH_MANAGER | Own branch loans   | Level 2 approve, reject          |
| MARKETING      | Own branch loans   | Level 1 approve, reject          |
| CUSTOMER       | Own data only      | Submit loan, view status         |

---

## 3. Customer Journey

### Complete Flow

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                           CUSTOMER JOURNEY                                    │
└──────────────────────────────────────────────────────────────────────────────┘

Step 1: REGISTRATION
┌─────────────┐
│  Register   │ ─── POST /api/auth/register
│  (Sign Up)  │     • name, email, password
└──────┬──────┘     • Auto-assigned CUSTOMER role
       │            • Empty profile created
       ▼
Step 2: COMPLETE PROFILE
┌─────────────┐
│  Complete   │ ─── PUT /api/customer/profile
│  Profile    │     • NIK (16 digits)
└──────┬──────┘     • Birthdate
       │            • Phone number
       │            • Address
       │
       │  ⚠️ VALIDATION:
       │  Profile must be COMPLETE before loan submission
       ▼
Step 3: SELECT PLAFOND
┌─────────────┐
│  Select     │ ─── POST /api/customer/plafond
│  Plafond    │     • Choose product (BRONZE/SILVER/GOLD/PLATINUM)
└──────┬──────┘     • Sets credit limit (amount, tenor, rate)
       │            • remainingAmount = product.amount
       │
       │  ⚠️ RULES:
       │  • Only 1 active plafond per customer
       │  • remainingAmount decreases on loan approval
       │  • Plafond becomes inactive when remainingAmount <= 0
       │  • Can select new plafond after old one is inactive
       ▼
Step 4: SUBMIT LOAN
┌─────────────┐
│  Submit     │ ─── POST /api/loans
│  Loan       │     • amount (≤ plafond remainingAmount)
└──────┬──────┘     • tenor (≤ plafond tenor)
       │            • interestRate (≥ plafond rate)
       │            • branchId (processing branch)
       ▼
Step 5: TRACK STATUS
┌─────────────┐
│  Track      │ ─── GET /api/loans
│  Progress   │ ─── GET /api/loans/{id}
└─────────────┘ ─── GET /api/loans/{id}/history
```

### Profile Completeness Check

```java
// UserProfile.isComplete() logic
public boolean isComplete() {
    return nik != null && !nik.isEmpty() &&
           birthdate != null &&
           phoneNumber != null && !phoneNumber.isEmpty() &&
           address != null && !address.isEmpty();
}
```

### Plafond Products

| Product  | Max Amount    | Max Tenor | Min Interest |
| -------- | ------------- | --------- | ------------ |
| BRONZE   | Rp 5,000,000  | 12 months | 12%          |
| SILVER   | Rp 10,000,000 | 24 months | 10%          |
| GOLD     | Rp 25,000,000 | 36 months | 8.5%         |
| PLATINUM | Rp 50,000,000 | 48 months | 7%           |

---

## 4. Loan Approval Workflow

### Multi-Level Approval Process

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                        LOAN APPROVAL WORKFLOW                                 │
└──────────────────────────────────────────────────────────────────────────────┘

                              CUSTOMER
                                 │
                                 │ Submit Loan
                                 ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                           LEVEL 1: MARKETING                                 │
│                        (Branch Restricted)                                   │
├─────────────────────────────────────────────────────────────────────────────┤
│  Expected Status: SUBMITTED                                                  │
│  Actions: Approve → MARKETING_APPROVED                                       │
│           Reject  → MARKETING_REJECTED (Terminal)                            │
└─────────────────────────────────────────────────────────────────────────────┘
                                 │
                                 │ Approved
                                 ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                       LEVEL 2: BRANCH MANAGER                                │
│                        (Branch Restricted)                                   │
├─────────────────────────────────────────────────────────────────────────────┤
│  Expected Status: MARKETING_APPROVED                                         │
│  Actions: Approve → BRANCH_MANAGER_APPROVED                                  │
│           Reject  → BRANCH_MANAGER_REJECTED (Terminal)                       │
└─────────────────────────────────────────────────────────────────────────────┘
                                 │
                                 │ Approved
                                 ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                        LEVEL 3: BACKOFFICE                                   │
│                         (All Branches)                                       │
├─────────────────────────────────────────────────────────────────────────────┤
│  Expected Status: BRANCH_MANAGER_APPROVED                                    │
│  Actions: Approve → APPROVED (Final - Loan Disbursed)                        │
│           Reject  → REJECTED (Terminal)                                      │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Branch Restriction Logic

```java
// ApprovalServiceImpl.getPendingLoans()
if (role == RoleName.BACKOFFICE) {
    // Can see ALL branches
    return loanRepository.findByStatusWithDetails(expectedStatus);
} else {
    // MARKETING & BRANCH_MANAGER - branch restricted
    return loanRepository.findByStatusAndBranchIdWithDetails(
        expectedStatus, approver.getBranch().getId());
}
```

---

## 5. Entity Relationships

### Database Schema (ERD)

```
┌──────────────────┐         ┌──────────────────┐         ┌──────────────────┐
│      BRANCH      │         │       USER       │         │       ROLE       │
├──────────────────┤         ├──────────────────┤         ├──────────────────┤
│ id (PK)          │◄───┐    │ id (PK)          │    ┌───►│ id (PK)          │
│ code             │    │    │ name             │    │    │ name (enum)      │
│ location         │    │    │ email (unique)   │    │    └────────┬─────────┘
└──────────────────┘    │    │ password         │    │             │
                        │    │ user_type (enum) │    │             │ M:N
                        │    │ is_active        │    │             ▼
                        │    │ branch_id (FK)───┘    │    ┌──────────────────┐
                        │    │                  │    │    │   PERMISSION     │
                        │    └────────┬─────────┘    │    ├──────────────────┤
                        │             │ M:N          │    │ id (PK)          │
                        │             └──────────────┘    │ code (unique)    │
                        │                                 │ description      │
                        │    ┌──────────────────┐         └──────────────────┘
                        │    │   USER_PROFILE   │
                        │    ├──────────────────┤
                        │    │ id (PK)          │
                        │    │ user_id (FK) ────┼─── 1:1 with USER
                        │    │ nik              │
                        │    │ birthdate        │
                        │    │ phone_number     │
                        │    │ address          │
                        │    └──────────────────┘
                        │
                        │    ┌──────────────────┐         ┌──────────────────┐
                        │    │   USER_PLAFOND   │         │     PRODUCT      │
                        │    ├──────────────────┤         ├──────────────────┤
                        │    │ id (PK)          │    ┌───►│ id (PK)          │
                        │    │ user_id (FK) ────┼─1:1│    │ name             │
                        │    │ product_id (FK)──┼────┘    │ amount           │
                        │    │ remaining_amount │         │ tenor            │
                        │    │ assigned_at      │         │ interest_rate    │
                        │    │ is_active        │         └──────────────────┘
                        │    └──────────────────┘
                        │
                        │    ┌──────────────────────────────────────────────────┐
                        │    │                 LOAN_APPLICATION                  │
                        │    ├──────────────────────────────────────────────────┤
                        │    │ id (PK)                                          │
                        │    │ customer_id (FK) ────────────────────── USER     │
                        └────┤ branch_id (FK) ──────────────────────── BRANCH   │
                             │ product_id (FK) ─────────────────────── PRODUCT  │
                             │ requested_amount                                 │
                             │ requested_tenor                                  │
                             │ requested_rate                                   │
                             │ customer_name_snapshot    ─┐                     │
                             │ customer_email_snapshot    │                     │
                             │ customer_nik_snapshot      ├─ CUSTOMER SNAPSHOT  │
                             │ customer_phone_snapshot    │  (preserved at      │
                             │ customer_address_snapshot  │   submission time)  │
                             │ customer_birthdate_snapshot┘                     │
                             │ status (enum)                                    │
                             │ created_at, updated_at                           │
                             └─────────────────────┬────────────────────────────┘
                                                   │
                                                   │ 1:N
                                                   ▼
                             ┌──────────────────────────────────────────────────┐
                             │            LOAN_APPLICATION_HISTORY               │
                             ├──────────────────────────────────────────────────┤
                             │ id (PK)                                          │
                             │ loan_application_id (FK)                         │
                             │ approved_by (FK) ──────────────────────── USER   │
                             │ approved_by_role (snapshot)                      │
                             │ approved_by_branch_id (snapshot)                 │
                             │ status (enum)                                    │
                             │ note                                             │
                             │ created_at                                       │
                             └──────────────────────────────────────────────────┘
```

### Snapshot Fields Explanation

The system uses **snapshot** data to preserve information at the time of action:

```
┌─────────────────────────────────────────────────────────────────────────────┐
│  WHY CUSTOMER SNAPSHOT?                                                      │
│                                                                             │
│  Customer profile can change over time. If John Doe submits a loan with    │
│  address "Jl. Sudirman", then moves to "Jl. Thamrin", the loan should      │
│  still show:                                                                │
│                                                                             │
│  ✅ customer_address_snapshot = "Jl. Sudirman" (address at submission)     │
│  ❌ NOT current address = "Jl. Thamrin"                                     │
└─────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────┐
│  WHY APPROVER SNAPSHOT?                                                      │
│                                                                             │
│  User roles can change over time. If Marketing "A" approves a loan,        │
│  then gets promoted to Branch_Manager, history should still show:          │
│                                                                             │
│  ✅ approved_by_role = "MARKETING"  (role at time of action)               │
│  ❌ NOT current role = "BRANCH_MANAGER"                                     │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 6. State Diagrams

### Loan Status State Machine

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         LOAN STATUS STATE MACHINE                           │
└─────────────────────────────────────────────────────────────────────────────┘

                              ┌──────────────┐
                              │  SUBMITTED   │
                              └───────┬──────┘
                                      │
                   ┌──────────────────┴──────────────────┐
                   │                                     │
                   ▼                                     ▼
        ┌─────────────────┐                   ┌────────────────────┐
        │   MARKETING_    │                   │   MARKETING_       │
        │   REJECTED      │                   │   APPROVED         │
        │   (Terminal)    │                   └─────────┬──────────┘
        └─────────────────┘                             │
                                     ┌──────────────────┴──────────────────┐
                                     │                                     │
                                     ▼                                     ▼
                          ┌─────────────────┐               ┌─────────────────┐
                          │ BRANCH_MANAGER_ │               │ BRANCH_MANAGER_ │
                          │ REJECTED        │               │ APPROVED        │
                          │ (Terminal)      │               └────────┬────────┘
                          └─────────────────┘                        │
                                              ┌──────────────────────┴────────────────────┐
                                              │                                           │
                                              ▼                                           ▼
                                   ┌─────────────────┐                         ┌─────────────────┐
                                   │    REJECTED     │                         │    APPROVED     │
                                   │   (Terminal)    │                         │   (Terminal)    │
                                   └─────────────────┘                         └─────────────────┘
```

### Terminal vs Non-Terminal States

| Status                  | Terminal? | Next Action           |
| ----------------------- | --------- | --------------------- |
| SUBMITTED               | ❌        | Marketing review      |
| MARKETING_APPROVED      | ❌        | Branch Manager review |
| MARKETING_REJECTED      | ✅        | None (Rejected)       |
| BRANCH_MANAGER_APPROVED | ❌        | Backoffice review     |
| BRANCH_MANAGER_REJECTED | ✅        | None (Rejected)       |
| APPROVED                | ✅        | None (Loan disbursed) |
| REJECTED                | ✅        | None (Rejected)       |

---

## 7. Business Rules

### Validation Rules

#### Profile Validation

```
┌─────────────────────────────────────────────────────────────────────────────┐
│  PROFILE COMPLETENESS                                                        │
├─────────────────────────────────────────────────────────────────────────────┤
│  ✅ NIK must be 16 digits                                                   │
│  ✅ Birthdate must be provided                                              │
│  ✅ Phone number must be provided                                           │
│  ✅ Address must be provided                                                │
│                                                                             │
│  ⚠️  ALL FIELDS REQUIRED before loan submission                            │
└─────────────────────────────────────────────────────────────────────────────┘
```

#### Plafond Rules

```
┌─────────────────────────────────────────────────────────────────────────────┐
│  PLAFOND RULES                                                               │
├─────────────────────────────────────────────────────────────────────────────┤
│  ✅ One active plafond per customer                                         │
│  ✅ remainingAmount = product.amount initially                              │
│  ✅ remainingAmount decreases when loan is APPROVED (final)                 │
│  ✅ Plafond becomes inactive when remainingAmount <= 0                      │
│  ✅ Can select new plafond after old one is inactive                        │
└─────────────────────────────────────────────────────────────────────────────┘
```

#### Loan Submission Rules

```
┌─────────────────────────────────────────────────────────────────────────────┐
│  LOAN SUBMISSION VALIDATION                                                  │
├─────────────────────────────────────────────────────────────────────────────┤
│  Pre-conditions:                                                            │
│  ✅ Profile must be COMPLETE                                                │
│  ✅ Must have active plafond                                                │
│  ✅ Must NOT have any pending loan applications                             │
│     (SUBMITTED, MARKETING_APPROVED, or BRANCH_MANAGER_APPROVED)             │
│                                                                             │
│  Amount validation:                                                         │
│  ✅ requested_amount ≤ plafond.remainingAmount                              │
│                                                                             │
│  Tenor validation:                                                          │
│  ✅ requested_tenor ≤ plafond.product.tenor                                 │
│                                                                             │
│  Interest rate validation:                                                  │
│  ✅ requested_rate ≥ plafond.product.interest_rate                          │
│                                                                             │
│  Branch validation:                                                         │
│  ✅ branch_id must exist                                                    │
└─────────────────────────────────────────────────────────────────────────────┘
```

#### Approval Rules

```
┌─────────────────────────────────────────────────────────────────────────────┐
│  APPROVAL BUSINESS RULES                                                     │
├─────────────────────────────────────────────────────────────────────────────┤
│  Status matching:                                                           │
│  ✅ MARKETING can only process SUBMITTED loans                              │
│  ✅ BRANCH_MANAGER can only process MARKETING_APPROVED loans                │
│  ✅ BACKOFFICE can only process BRANCH_MANAGER_APPROVED loans               │
│                                                                             │
│  Branch restriction:                                                        │
│  ✅ MARKETING & BRANCH_MANAGER can only see/process their branch            │
│  ✅ BACKOFFICE can see/process ALL branches                                 │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 8. API Flow Sequences

### Complete Loan Submission Sequence

```
Customer                  Backend                    Database
   │                         │                          │
   │  POST /api/auth/login   │                          │
   │────────────────────────►│                          │
   │                         │  findByEmail()           │
   │                         │─────────────────────────►│
   │                         │◄─────────────────────────│
   │  {token, permissions}   │                          │
   │◄────────────────────────│                          │
   │                         │                          │
   │  PUT /api/customer/profile                         │
   │────────────────────────►│                          │
   │                         │  save(profile)           │
   │                         │─────────────────────────►│
   │  {isComplete: true}     │◄─────────────────────────│
   │◄────────────────────────│                          │
   │                         │                          │
   │  POST /api/customer/plafond                        │
   │────────────────────────►│                          │
   │                         │  check existing plafond  │
   │                         │─────────────────────────►│
   │                         │  save(userPlafond)       │
   │                         │─────────────────────────►│
   │  {plafond details}      │◄─────────────────────────│
   │◄────────────────────────│                          │
   │                         │                          │
   │  POST /api/loans        │                          │
   │────────────────────────►│                          │
   │                         │  validate profile        │
   │                         │  validate plafond        │
   │                         │  validate amount/tenor   │
   │                         │  save(loanApplication)   │
   │                         │  save(history)           │
   │                         │─────────────────────────►│
   │  {loan: SUBMITTED}      │◄─────────────────────────│
   │◄────────────────────────│                          │
   │                         │                          │
```

### Complete Approval Sequence

```
Marketing          BranchMgr          Backoffice         Database
   │                  │                   │                  │
   │  GET pending     │                   │                  │
   │──────────────────┼───────────────────┼─────────────────►│
   │  [SUBMITTED      │                   │                  │
   │   branch-filtered]◄─────────────────────────────────────│
   │                  │                   │                  │
   │  POST approve    │                   │                  │
   │──────────────────┼───────────────────┼─────────────────►│
   │                  │                   │  status=MARKETING_APPROVED
   │  ✅ APPROVED     │                   │                  │
   │◄─────────────────┼───────────────────┼──────────────────│
   │                  │                   │                  │
   │                  │  GET pending      │                  │
   │                  │───────────────────┼─────────────────►│
   │                  │  [MARKETING_APPROVED                 │
   │                  │   branch-filtered]◄──────────────────│
   │                  │                   │                  │
   │                  │  POST approve     │                  │
   │                  │───────────────────┼─────────────────►│
   │                  │                   │  status=BM_APPROVED
   │                  │  ✅ APPROVED      │                  │
   │                  │◄──────────────────┼──────────────────│
   │                  │                   │                  │
   │                  │                   │  GET pending     │
   │                  │                   │─────────────────►│
   │                  │                   │  [BM_APPROVED    │
   │                  │                   │   all branches]  │
   │                  │                   │◄─────────────────│
   │                  │                   │                  │
   │                  │                   │  POST approve    │
   │                  │                   │─────────────────►│
   │                  │                   │  status=APPROVED │
   │                  │                   │  ✅ FINAL        │
   │                  │                   │◄─────────────────│
   │                  │                   │                  │
```

---

## Summary

| Component      | Key Points                                       |
| -------------- | ------------------------------------------------ |
| **User Types** | CUSTOMER (mobile), INTERNAL (web)                |
| **Roles**      | 5 roles with hierarchical permissions            |
| **Approval**   | 3-level: Marketing → Branch Manager → Backoffice |
| **Branch**     | MARKETING & BM are branch-restricted             |
| **Plafond**    | Credit limit before loan, 1 per customer         |
| **History**    | Snapshot audit trail for all actions             |

---

_Documentation updated: 2026-01-07_
