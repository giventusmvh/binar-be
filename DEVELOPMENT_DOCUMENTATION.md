# Loan Application Backend - Development Documentation

> Complete coding flow documentation from project initialization to final build

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [Phase 1: Project Configuration](#2-phase-1-project-configuration)
3. [Phase 2: Enums & Entities](#3-phase-2-enums--entities)
4. [Phase 3: Repositories](#4-phase-3-repositories)
5. [Phase 4: DTOs](#5-phase-4-dtos)
6. [Phase 5: Utilities & Exceptions](#6-phase-5-utilities--exceptions)
7. [Phase 6: Security & JWT](#7-phase-6-security--jwt)
8. [Phase 7: Services](#8-phase-7-services)
9. [Phase 8: Controllers](#9-phase-8-controllers)
10. [Phase 9: Data Initializer](#10-phase-9-data-initializer)
11. [Build & Verification](#11-build--verification)
12. [Project Structure Summary](#12-project-structure-summary)

---

## 1. Project Overview

### Initial State

- Fresh Spring Boot 4.0.1 project with Maven
- Java 21, Spring Web, Spring Data JPA, SQL Server driver, Lombok

### Target Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        REST Controllers                          │
│  (AuthController, CustomerController, LoanController, etc.)     │
└────────────────────────────┬────────────────────────────────────┘
                             │
┌────────────────────────────▼────────────────────────────────────┐
│                         Services                                 │
│  (AuthService, CustomerService, LoanApplicationService, etc.)   │
└────────────────────────────┬────────────────────────────────────┘
                             │
┌────────────────────────────▼────────────────────────────────────┐
│                        Repositories                              │
│  (UserRepository, LoanApplicationRepository, etc.)              │
└────────────────────────────┬────────────────────────────────────┘
                             │
┌────────────────────────────▼────────────────────────────────────┐
│                      JPA/Hibernate                               │
│                    (Entity Mapping)                              │
└────────────────────────────┬────────────────────────────────────┘
                             │
┌────────────────────────────▼────────────────────────────────────┐
│                     SQL Server 2022                              │
│                    (Database: binar-be)                         │
└─────────────────────────────────────────────────────────────────┘
```

---

## 2. Phase 1: Project Configuration

### Step 1.1: Update pom.xml

Added dependencies for security, validation, and JWT:

```xml
<!-- Spring Security -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>

<!-- Validation -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>

<!-- JWT (JJWT 0.12.3) -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.3</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.3</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.3</version>
    <scope>runtime</scope>
</dependency>
```

### Step 1.2: Configure application.properties

```properties
spring.application.name=binar-be

# SQL Server Configuration
spring.datasource.url=jdbc:sqlserver://localhost:1433;databaseName=binar-be;encrypt=true;trustServerCertificate=true
spring.datasource.username=sa
spring.datasource.password=StrongPass123!
spring.datasource.driver-class-name=com.microsoft.sqlserver.jdbc.SQLServerDriver

# JPA/Hibernate Configuration
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.SQLServerDialect

# JWT Configuration (Base64 encoded secret)
jwt.secret=YmluYXItYmUtbG9hbi1hcHBsaWNhdGlvbi1zZWNyZXQta2V5LTIwMjQ...
jwt.expiration=86400000

# Logging
logging.level.org.springframework.security=DEBUG
logging.level.com.gvn.binarbe=DEBUG
```

### Step 1.3: Create Configuration Classes

**JpaConfig.java** - Enable JPA auditing:

```java
@Configuration
@EnableJpaAuditing
public class JpaConfig { }
```

**JwtConfig.java** - JWT configuration properties:

```java
@Configuration
@ConfigurationProperties(prefix = "jwt")
@Getter @Setter
public class JwtConfig {
    private String secret;
    private long expiration;
}
```

**SecurityConfig.java** - Main security configuration:

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    // JWT filter chain
    // Role-based access rules
    // AuthenticationManager configuration
    // PasswordEncoder bean
}
```

---

## 3. Phase 2: Enums & Entities

### Step 2.1: Create Enums

**UserType.java**

```java
public enum UserType {
    CUSTOMER,   // External mobile app users
    INTERNAL    // Internal web dashboard users
}
```

**RoleName.java**

```java
public enum RoleName {
    SUPERADMIN,     // Full system access
    MARKETING,      // Branch-restricted loan processing
    BRANCH_MANAGER, // Branch-restricted approval
    BACKOFFICE,     // Final approval (all branches)
    CUSTOMER        // Customer access
}
```

**LoanStatus.java**

```java
public enum LoanStatus {
    SUBMITTED,                  // Initial status
    MARKETING_APPROVED,         // Level 1 approved
    MARKETING_REJECTED,         // Level 1 rejected
    BRANCH_MANAGER_APPROVED,    // Level 2 approved
    BRANCH_MANAGER_REJECTED,    // Level 2 rejected
    APPROVED,                   // Final approved
    REJECTED,                   // Final rejected
    RETURNED                    // Sent back for revision
}
```

### Step 2.2: Create Entities

**Entity Relationship Overview:**

```
┌──────────┐     ┌───────────────┐     ┌──────────┐
│  Branch  │────<│     User      │>────│   Role   │
└──────────┘     └───────────────┘     └──────────┘
                        │                    │
                        │                    │
                 ┌──────▼──────┐      ┌──────▼──────┐
                 │ UserProfile │      │ Permission  │
                 └─────────────┘      └─────────────┘
                        │
           ┌────────────┼────────────┐
           │            │            │
    ┌──────▼──────┐     │     ┌──────▼──────┐
    │   Product   │     │     │   Branch    │
    └──────▼──────┘     │     └──────▼──────┘
           │            │            │
           └────────────┼────────────┘
                        │
                 ┌──────▼──────┐
                 │ LoanApp     │
                 └──────▼──────┘
                        │
                 ┌──────▼──────┐
                 │ LoanHistory │
                 └─────────────┘
```

**Key Entity: User.java**

```java
@Entity
@Table(name = "users")
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String email;      // unique
    private String password;

    @Enumerated(EnumType.STRING)
    private UserType userType;

    private Boolean isActive;

    @ManyToOne
    @JoinColumn(name = "branch_id")
    private Branch branch;     // nullable for customers

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_roles")
    private Set<Role> roles;

    @OneToOne(mappedBy = "user")
    private UserProfile profile;
}
```

**Key Entity: LoanApplication.java**

```java
@Entity
@Table(name = "loan_applications")
public class LoanApplication {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private User customer;

    @ManyToOne
    private Product product;

    @ManyToOne
    private Branch branch;

    @Enumerated(EnumType.STRING)
    private LoanStatus status;

    @OneToMany(mappedBy = "loanApplication")
    private List<LoanApplicationHistory> histories;
}
```

**Key Entity: LoanApplicationHistory.java** (Audit Trail)

```java
@Entity
@Table(name = "loan_application_histories")
public class LoanApplicationHistory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private LoanApplication loanApplication;

    @ManyToOne
    private User approvedBy;

    // Snapshot fields (preserved at time of action)
    private String approvedByRole;
    private Integer approvedByBranchId;

    @Enumerated(EnumType.STRING)
    private LoanStatus status;

    private String note;
    private LocalDateTime createdAt;
}
```

---

## 4. Phase 3: Repositories

Created 8 repository interfaces with custom queries:

```java
// UserRepository - Custom queries for auth
Optional<User> findByEmail(String email);
Optional<User> findByEmailWithRoles(@Param("email") String email);
Optional<User> findByIdWithRolesAndProfile(@Param("id") Long id);

// RoleRepository
Optional<Role> findByName(RoleName name);
Optional<Role> findByIdWithPermissions(@Param("id") Long id);

// LoanApplicationRepository - Complex queries with joins
List<LoanApplication> findByStatusWithDetails(LoanStatus status);
List<LoanApplication> findByStatusAndBranchIdWithDetails(
    LoanStatus status, Long branchId);

// LoanApplicationHistoryRepository
List<LoanApplicationHistory> findByLoanApplicationIdWithApprover(
    Long loanApplicationId);
```

---

## 5. Phase 4: DTOs

### Request DTOs (9 classes)

| DTO                       | Purpose                      | Validations                        |
| ------------------------- | ---------------------------- | ---------------------------------- |
| RegisterRequest           | Customer registration        | @NotBlank, @Email, @Size           |
| LoginRequest              | User login                   | @NotBlank, @Email                  |
| ChangePasswordRequest     | Change password              | @NotBlank, @Size(min=6)            |
| UpdateProfileRequest      | Profile update               | @NotNull, @Pattern (NIK)           |
| LoanApplicationRequest    | Submit loan                  | @NotNull productId, branchId       |
| ApprovalRequest           | Approve/reject               | @Size(max=1000) note               |
| AssignRoleRequest         | Role assignment              | @NotNull roleId                    |
| AssignPermissionRequest   | Permission update            | @NotEmpty permissionIds            |
| CreateInternalUserRequest | Create internal user (admin) | @NotBlank, @Email, @NotNull roleId |

### Response DTOs (9 classes)

| DTO                     | Purpose                   |
| ----------------------- | ------------------------- |
| AuthResponse            | JWT token + user info     |
| UserResponse            | User details + roles      |
| UserProfileResponse     | Profile + isComplete flag |
| ProductResponse         | Product details           |
| BranchResponse          | Branch details            |
| RoleResponse            | Role + permissions        |
| PermissionResponse      | Permission details        |
| LoanApplicationResponse | Loan + status             |
| LoanHistoryResponse     | Approval history entry    |

---

## 6. Phase 5: Utilities & Exceptions

### ApiResponse.java - Generic wrapper

```java
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private List<String> errors;
    private LocalDateTime timestamp;

    // Static factory methods
    public static <T> ApiResponse<T> success(T data) { ... }
    public static <T> ApiResponse<T> error(String message) { ... }
}
```

### ResponseUtil.java - HTTP helpers

```java
public static <T> ResponseEntity<ApiResponse<T>> ok(T data) { ... }
public static <T> ResponseEntity<ApiResponse<T>> created(T data) { ... }
public static <T> ResponseEntity<ApiResponse<T>> badRequest(String msg) { ... }
public static <T> ResponseEntity<ApiResponse<T>> unauthorized(String msg) { ... }
```

### BusinessException.java - Custom exception

```java
public class BusinessException extends RuntimeException {
    private final HttpStatus status;

    public static BusinessException notFound(String message) { ... }
    public static BusinessException badRequest(String message) { ... }
    public static BusinessException forbidden(String message) { ... }
}
```

### GlobalExceptionHandler.java

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ExceptionHandler(AuthenticationException.class)
    @ExceptionHandler(AccessDeniedException.class)
    @ExceptionHandler(Exception.class)
    // ... centralized error handling
}
```

---

## 7. Phase 6: Security & JWT

### JwtUtil.java - Token operations

```java
@Component
public class JwtUtil {
    // Token generation with claims
    public String generateToken(UserDetails userDetails) {
        return Jwts.builder()
            .claims(extraClaims)
            .subject(email)
            .issuedAt(new Date())
            .expiration(new Date(now + expiration))
            .signWith(getSigningKey())
            .compact();
    }

    // Token validation
    public Boolean validateToken(String token, UserDetails userDetails) { ... }

    // Claim extraction
    public String extractEmail(String token) { ... }
}
```

### JwtAuthenticationFilter.java

```java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(request, response, filterChain) {
        // 1. Extract Bearer token from Authorization header
        // 2. Validate token
        // 3. Load UserDetails
        // 4. Set SecurityContext authentication
        // 5. Continue filter chain
    }
}
```

### CustomUserDetailsService.java

```java
@Service
public class CustomUserDetailsService implements UserDetailsService {
    @Override
    public UserDetails loadUserByUsername(String email) {
        User user = userRepository.findByEmailWithRoles(email)
            .orElseThrow(() -> new UsernameNotFoundException(...));

        return new org.springframework.security.core.userdetails.User(
            user.getEmail(),
            user.getPassword(),
            getAuthorities(user)  // ROLE_MARKETING, ROLE_CUSTOMER, etc.
        );
    }
}
```

---

## 8. Phase 7: Services

### AuthServiceImpl.java

```java
@Service
public class AuthServiceImpl implements AuthService {

    public AuthResponse register(RegisterRequest request) {
        // 1. Check email uniqueness
        // 2. Get CUSTOMER role
        // 3. Create User with encoded password
        // 4. Create empty UserProfile
        // 5. Generate JWT token
        // 6. Return AuthResponse
    }

    public AuthResponse login(LoginRequest request) {
        // 1. Authenticate with AuthenticationManager
        // 2. Load user with roles
        // 3. Generate JWT token
        // 4. Return AuthResponse
    }

    public void changePassword(Long userId, ChangePasswordRequest request) {
        // 1. Validate new password matches confirm password
        // 2. Validate new password is different from current
        // 3. Get user by ID
        // 4. Verify current password is correct
        // 5. Encode and save new password
    }
}
```

### LoanApplicationServiceImpl.java

```java
@Service
public class LoanApplicationServiceImpl implements LoanApplicationService {

    public LoanApplicationResponse submitLoan(String email, LoanApplicationRequest request) {
        // 1. Get customer
        // 2. Validate profile is COMPLETE (NIK, birthdate, phone, address)
        // 3. Get product and branch
        // 4. Create LoanApplication with SUBMITTED status
        // 5. Create initial history entry
        // 6. Return response
    }
}
```

### ApprovalServiceImpl.java (Key business logic)

```java
@Service
public class ApprovalServiceImpl implements ApprovalService {

    public List<LoanApplicationResponse> getPendingLoans(String email) {
        User approver = getApprover(email);
        RoleName role = getHighestRole(approver);
        LoanStatus expectedStatus = getExpectedStatus(role);

        if (role == BACKOFFICE) {
            // Can see ALL branches
            return findByStatusWithDetails(expectedStatus);
        } else {
            // MARKETING, BRANCH_MANAGER - branch restricted
            return findByStatusAndBranchIdWithDetails(
                expectedStatus, approver.getBranch().getId());
        }
    }

    public LoanApplicationResponse approve(String email, Long loanId, ApprovalRequest request) {
        // 1. Get approver and role
        // 2. Validate loan is in correct status for this role
        // 3. Validate branch restriction (except BACKOFFICE)
        // 4. Determine new status based on role:
        //    MARKETING -> MARKETING_APPROVED
        //    BRANCH_MANAGER -> BRANCH_MANAGER_APPROVED
        //    BACKOFFICE -> APPROVED
        // 5. Update loan status
        // 6. Create history entry with snapshot data
        // 7. Return response
    }
}
```

---

## 9. Phase 8: Controllers

### Endpoint Summary

```
PUBLIC ENDPOINTS (No Auth):
POST   /api/auth/register     - Customer registration
POST   /api/auth/login        - User login
GET    /api/products          - List products
GET    /api/branches          - List branches

AUTHENTICATED ENDPOINTS (Any logged-in user):
POST   /api/auth/change-password - Change password (requires current password)

CUSTOMER ENDPOINTS (ROLE_CUSTOMER):
GET    /api/customer/profile  - Get profile
PUT    /api/customer/profile  - Update profile
POST   /api/loans             - Submit loan
GET    /api/loans             - My loans
GET    /api/loans/{id}        - Loan details
GET    /api/loans/{id}/history - Approval history

APPROVAL ENDPOINTS (ROLE_MARKETING, ROLE_BRANCH_MANAGER, ROLE_BACKOFFICE):
GET    /api/approval/pending      - Pending loans
POST   /api/approval/{id}/approve - Approve loan
POST   /api/approval/{id}/reject  - Reject loan
POST   /api/approval/{id}/return  - Return (BACKOFFICE only)

ADMIN ENDPOINTS (ROLE_SUPERADMIN):
POST   /api/admin/users               - Create internal user (with role + branch)
GET    /api/admin/users               - List users
GET    /api/admin/users/{id}          - User details
POST   /api/admin/users/{id}/roles    - Assign role
DELETE /api/admin/users/{uid}/roles/{rid} - Remove role
GET    /api/admin/roles               - List roles
PUT    /api/admin/roles/{id}/permissions - Update permissions
GET    /api/admin/permissions         - List permissions
```

### Controller Pattern

```java
@RestController
@RequestMapping("/api/...")
@RequiredArgsConstructor
public class XxxController {

    private final XxxService xxxService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<XxxResponse>>> getAll() {
        List<XxxResponse> response = xxxService.getAll();
        return ResponseUtil.ok(response);
    }

    @PostMapping
    public ResponseEntity<ApiResponse<XxxResponse>> create(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody XxxRequest request) {
        XxxResponse response = xxxService.create(
            userDetails.getUsername(), request);
        return ResponseUtil.created("Created successfully", response);
    }
}
```

---

## 10. Phase 9: Data Initializer

### DataInitializer.java (CommandLineRunner)

Executes on startup to seed initial data:

```java
@Component
public class DataInitializer implements CommandLineRunner {

    @Override
    @Transactional
    public void run(String... args) {
        initializeBranches();    // 3 branches
        initializePermissions(); // 20 permissions
        initializeRoles();       // 5 roles with permissions
        initializeUsers();       // 7 users
        initializeProducts();    // 4 products
    }
}
```

### Seeded Data

**Branches:**
| Code | Location |
|------|----------|
| JKT | Jakarta |
| SBY | Surabaya |
| BDG | Bandung |

**Roles with Permissions:**
| Role | Key Permissions |
|------|-----------------|
| SUPERADMIN | All permissions |
| MARKETING | LOAN_READ_BRANCH, LOAN_APPROVE_MARKETING, LOAN_REJECT |
| BRANCH_MANAGER | LOAN_READ_BRANCH, LOAN_APPROVE_BRANCH_MANAGER, LOAN_REJECT |
| BACKOFFICE | LOAN_READ_ALL, LOAN_APPROVE_BACKOFFICE, LOAN_REJECT, LOAN_RETURN |
| CUSTOMER | LOAN_CREATE, LOAN_READ, PRODUCT_READ, BRANCH_READ |

**Internal Users:**
| Email | Role | Branch |
|-------|------|--------|
| admin@loan.com | SUPERADMIN | - |
| backoffice@loan.com | BACKOFFICE | - |
| marketing.jkt@loan.com | MARKETING | Jakarta |
| bm.jkt@loan.com | BRANCH_MANAGER | Jakarta |
| marketing.sby@loan.com | MARKETING | Surabaya |
| bm.sby@loan.com | BRANCH_MANAGER | Surabaya |
| internal@loan.com | (none) | Jakarta |

**Customer Users:**
| Email | Password | Profile Status | Can Submit Loan? |
|-------|----------|----------------|------------------|
| john.doe@email.com | customer123 | Complete (NIK, phone, address, birthdate) | ✅ Yes |
| jane.smith@email.com | customer123 | Empty | ❌ No (must complete profile first) |

**Products:**
| Name | Amount | Tenor | Interest |
|------|--------|-------|----------|
| BRONZE | 5,000,000 | 12 months | 12% |
| SILVER | 10,000,000 | 24 months | 10% |
| GOLD | 25,000,000 | 36 months | 8.5% |
| PLATINUM | 50,000,000 | 48 months | 7% |

---

## 11. Build & Verification

### Build Command

```bash
./mvnw clean compile -DskipTests
```

### Build Result

```
[INFO] Compiling 62 source files with javac [debug parameters release 21]
[INFO] BUILD SUCCESS
[INFO] Total time: 2.024 s
```

### Files Created Summary

| Category      | Count                            |
| ------------- | -------------------------------- |
| Enums         | 3                                |
| Entities      | 8                                |
| Repositories  | 8                                |
| Request DTOs  | 7                                |
| Response DTOs | 9                                |
| Services      | 5 interfaces + 5 implementations |
| Controllers   | 5                                |
| Security      | 3                                |
| Config        | 3                                |
| Exception     | 2                                |
| Utility       | 2                                |
| Initializer   | 1                                |
| **TOTAL**     | **62 Java files**                |

---

## 12. Project Structure Summary

```
src/main/java/com/gvn/binarbe
│
├── config/
│   ├── SecurityConfig.java
│   ├── JwtConfig.java
│   └── JpaConfig.java
│
├── controller/
│   ├── AuthController.java
│   ├── CustomerController.java
│   ├── LoanApplicationController.java
│   ├── ApprovalController.java
│   └── SuperAdminController.java
│
├── dto/
│   ├── request/
│   │   ├── RegisterRequest.java
│   │   ├── LoginRequest.java
│   │   ├── UpdateProfileRequest.java
│   │   ├── LoanApplicationRequest.java
│   │   ├── ApprovalRequest.java
│   │   ├── AssignRoleRequest.java
│   │   └── AssignPermissionRequest.java
│   └── response/
│       ├── AuthResponse.java
│       ├── UserResponse.java
│       ├── UserProfileResponse.java
│       ├── ProductResponse.java
│       ├── BranchResponse.java
│       ├── RoleResponse.java
│       ├── PermissionResponse.java
│       ├── LoanApplicationResponse.java
│       └── LoanHistoryResponse.java
│
├── entity/
│   ├── User.java
│   ├── UserProfile.java
│   ├── Branch.java
│   ├── Role.java
│   ├── Permission.java
│   ├── Product.java
│   ├── LoanApplication.java
│   └── LoanApplicationHistory.java
│
├── enums/
│   ├── UserType.java
│   ├── LoanStatus.java
│   └── RoleName.java
│
├── exception/
│   ├── BusinessException.java
│   └── GlobalExceptionHandler.java
│
├── initializer/
│   └── DataInitializer.java
│
├── repository/
│   ├── UserRepository.java
│   ├── UserProfileRepository.java
│   ├── RoleRepository.java
│   ├── PermissionRepository.java
│   ├── BranchRepository.java
│   ├── ProductRepository.java
│   ├── LoanApplicationRepository.java
│   └── LoanApplicationHistoryRepository.java
│
├── security/
│   ├── JwtUtil.java
│   ├── JwtAuthenticationFilter.java
│   └── CustomUserDetailsService.java
│
├── service/
│   ├── AuthService.java
│   ├── CustomerService.java
│   ├── LoanApplicationService.java
│   ├── ApprovalService.java
│   ├── SuperAdminService.java
│   └── impl/
│       ├── AuthServiceImpl.java
│       ├── CustomerServiceImpl.java
│       ├── LoanApplicationServiceImpl.java
│       ├── ApprovalServiceImpl.java
│       └── SuperAdminServiceImpl.java
│
├── util/
│   ├── ApiResponse.java
│   └── ResponseUtil.java
│
└── BinarBeApplication.java
```

---

## Key Design Decisions

1. **SOLID Principles**

   - Single Responsibility: Each class has one purpose
   - Open/Closed: Services use interfaces
   - Liskov Substitution: Implementations are interchangeable
   - Interface Segregation: Small, focused interfaces
   - Dependency Inversion: Controllers depend on service interfaces

2. **DTO Pattern**

   - Entities never exposed in responses
   - Request DTOs with validation annotations
   - Response DTOs with clean structure

3. **Centralized Exception Handling**

   - GlobalExceptionHandler catches all exceptions
   - Consistent ApiResponse format for errors

4. **Branch Restrictions**

   - MARKETING/BRANCH_MANAGER: branch-specific filtering
   - BACKOFFICE: cross-branch access

5. **Snapshot History**
   - Role and branch recorded at time of action
   - Preserved even if user role changes later

---

_Documentation generated: 2025-12-21_
