# Penjelasan Lengkap Kode - Loan Application System

> Dokumentasi detail yang menjelaskan setiap bagian kode sesuai dengan business flow secara berurutan dan runtut

---

## Daftar Isi

1. [Overview Arsitektur](#1-overview-arsitektur)
2. [Flow 1: Registrasi & Login](#2-flow-1-registrasi--login)
3. [Flow 2: Customer Profile](#3-flow-2-customer-profile)
4. [Flow 3: Plafond Selection](#4-flow-3-plafond-selection)
5. [Flow 4: Loan Submission](#5-flow-4-loan-submission)
6. [Flow 5: Approval Workflow](#6-flow-5-approval-workflow)
7. [Flow 6: SuperAdmin Management](#7-flow-6-superadmin-management)
8. [Data Initializer](#8-data-initializer)

---

## 1. Overview Arsitektur

### 1.1 Layer Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        CLIENT (Angular/Android)                  │
└─────────────────────────────────────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────┐
│                     CONTROLLER LAYER                             │
│  Menerima HTTP Request, validasi input, return HTTP Response     │
│  File: *Controller.java                                         │
└─────────────────────────────────────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────┐
│                      SERVICE LAYER                               │
│  Business logic, validasi bisnis, orkestrasi data                │
│  File: *ServiceImpl.java                                        │
└─────────────────────────────────────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────┐
│                    REPOSITORY LAYER                              │
│  Komunikasi dengan database via JPA                              │
│  File: *Repository.java                                         │
└─────────────────────────────────────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────┐
│                      ENTITY LAYER                                │
│  Representasi tabel database                                     │
│  File: *.java di package entity                                 │
└─────────────────────────────────────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────┐
│                      DATABASE (MsSQL)                            │
└─────────────────────────────────────────────────────────────────┘
```

### 1.2 Package Structure

```
com.gvn.binarbe/
├── config/          → Konfigurasi Spring (Security, Redis, dll)
├── controller/      → REST API endpoints
├── dto/
│   ├── request/     → Data Transfer Object untuk request body
│   └── response/    → Data Transfer Object untuk response body
├── entity/          → JPA Entity (mapping ke tabel DB)
├── enums/           → Enum constants (RoleName, LoanStatus, dll)
├── exception/       → Custom exception & global handler
├── initializer/     → Data seeder saat aplikasi start
├── repository/      → JPA Repository interfaces
├── security/        → JWT, UserDetails, Filter
└── service/
    ├── interface/   → Service interfaces
    └── impl/        → Service implementations
```

### 1.3 Arsitektur & Design Principles

#### 1.3.1 Layered Architecture (N-Tier)

Aplikasi ini menggunakan **Layered Architecture** dengan 4 layer utama:

| Layer          | Responsibility                       | Boleh Akses                | Tidak Boleh Akses           |
| -------------- | ------------------------------------ | -------------------------- | --------------------------- |
| **Controller** | HTTP handling, validasi input        | Service                    | Repository, Entity langsung |
| **Service**    | Business logic, orchestration        | Repository, Other Services | Controller                  |
| **Repository** | Data access, query                   | Entity                     | Service, Controller         |
| **Entity**     | Data model, business rules sederhana | -                          | Semua layer lain            |

**Alasan menggunakan Layered Architecture:**

1. **Separation of Concerns** - Setiap layer punya tanggung jawab spesifik
2. **Testability** - Mudah unit test per layer dengan mocking
3. **Maintainability** - Perubahan di satu layer tidak mempengaruhi layer lain
4. **Reusability** - Service bisa dipanggil dari berbagai Controller

---

#### 1.3.2 SOLID Principles

| Principle                 | Penjelasan                                  | Implementasi di Kode                                                         |
| ------------------------- | ------------------------------------------- | ---------------------------------------------------------------------------- |
| **S**ingle Responsibility | Satu class, satu tanggung jawab             | `AuthService` hanya handle auth, `LoanApplicationService` hanya handle loan  |
| **O**pen/Closed           | Open for extension, closed for modification | Menggunakan interface (`AuthService`) dan implementation (`AuthServiceImpl`) |
| **L**iskov Substitution   | Subclass bisa menggantikan parent           | Semua service implements interface-nya                                       |
| **I**nterface Segregation | Interface yang kecil dan spesifik           | `PlafondService`, `ApprovalService` terpisah, bukan satu `LoanService` besar |
| **D**ependency Inversion  | Depend on abstraction, not concretion       | Controller inject `AuthService` (interface), bukan `AuthServiceImpl`         |

**Contoh Dependency Inversion:**

```java
// ❌ BAD - depend on concrete class
@RequiredArgsConstructor
public class AuthController {
    private final AuthServiceImpl authService;  // Concrete class
}

// ✅ GOOD - depend on interface (yang kita pakai)
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;  // Interface
}
```

---

#### 1.3.3 Design Patterns yang Digunakan

| Pattern                | Lokasi                            | Penjelasan                                                               |
| ---------------------- | --------------------------------- | ------------------------------------------------------------------------ |
| **Repository Pattern** | `*Repository.java`                | Abstraksi akses data, Spring Data JPA sebagai implementasi               |
| **DTO Pattern**        | `dto/request/*`, `dto/response/*` | Memisahkan data transfer dari entity, mencegah expose internal structure |
| **Builder Pattern**    | Lombok `@Builder` di entity       | Membuat object kompleks step-by-step, immutable                          |
| **Factory Pattern**    | `BusinessException.badRequest()`  | Static factory methods untuk create exception dengan HTTP status         |
| **Strategy Pattern**   | `ApprovalServiceImpl`             | Approval behavior berbeda berdasarkan role (Marketing/BM/Backoffice)     |
| **Template Method**    | `GlobalExceptionHandler`          | Handle berbagai exception dengan pattern yang sama                       |
| **Singleton Pattern**  | Spring `@Service`, `@Repository`  | Default scope Spring bean adalah singleton                               |

**Contoh Builder Pattern:**

```java
// Tanpa builder - constructor panjang, mudah salah urutan
User user = new User("John", "john@email.com", password, UserType.CUSTOMER, true, roles, null);

// Dengan builder - readable, self-documenting
User user = User.builder()
    .name("John")
    .email("john@email.com")
    .password(hashedPassword)
    .userType(UserType.CUSTOMER)
    .isActive(true)
    .roles(roles)
    .build();
```

**Contoh Factory Pattern:**

```java
// Factory methods di BusinessException
public class BusinessException extends RuntimeException {

    public static BusinessException badRequest(String message) {
        return new BusinessException(HttpStatus.BAD_REQUEST, message);
    }

    public static BusinessException notFound(String message) {
        return new BusinessException(HttpStatus.NOT_FOUND, message);
    }

    public static BusinessException conflict(String message) {
        return new BusinessException(HttpStatus.CONFLICT, message);
    }
}

// Usage - lebih readable
throw BusinessException.notFound("User not found");
throw BusinessException.badRequest("Invalid input");
```

**Contoh Strategy Pattern (Approval):**

```java
// Behavior berbeda berdasarkan role
private LoanStatus getNextApprovedStatus(String role) {
    return switch (role) {
        case "MARKETING" -> LoanStatus.MARKETING_APPROVED;
        case "BRANCH_MANAGER" -> LoanStatus.BRANCH_MANAGER_APPROVED;
        case "BACKOFFICE" -> LoanStatus.APPROVED;
        default -> throw new IllegalStateException("Unknown role: " + role);
    };
}
```

---

#### 1.3.4 Spring Boot Conventions

| Convention                   | Penjelasan                             | Contoh                                   |
| ---------------------------- | -------------------------------------- | ---------------------------------------- |
| **Constructor Injection**    | `@RequiredArgsConstructor` dari Lombok | Preferred over field injection, testable |
| **Declarative Transactions** | `@Transactional` di service            | Auto rollback on exception               |
| **Bean Validation**          | `@Valid` + JSR-380 annotations         | Validasi otomatis di controller          |
| **AOP Security**             | `@PreAuthorize`                        | Method-level security check              |
| **Auto-configuration**       | `application.yml`                      | Minimal boilerplate config               |

**Contoh Constructor Injection:**

```java
// Lombok akan generate constructor dengan semua final fields
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;      // Injected via constructor
    private final RoleRepository roleRepository;      // Injected via constructor
    private final PasswordEncoder passwordEncoder;    // Injected via constructor
    private final JwtUtil jwtUtil;                   // Injected via constructor
}
```

---

#### 1.3.5 Security Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    HTTP REQUEST                                  │
└─────────────────────────────────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│              JwtAuthenticationFilter                             │
│  - Extract token dari header "Authorization: Bearer xxx"         │
│  - Validate token signature & expiration                         │
│  - Load UserDetails dari database                                │
│  - Set SecurityContext                                           │
└─────────────────────────────────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│              SecurityConfig (HttpSecurity)                       │
│  - Public endpoints: /api/auth/**, /api/products, /api/branches  │
│  - Authenticated: /api/customer/**, /api/loans/**                │
│  - Role-specific: /api/approval/** (MARKETING, BM, BACKOFFICE)   │
│  - Admin only: /api/admin/** (SUPERADMIN)                        │
└─────────────────────────────────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│              @PreAuthorize (Method Security)                     │
│  - Fine-grained permission check                                 │
│  - hasRole(), hasPermission(), @permission-name                  │
└─────────────────────────────────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│                       CONTROLLER                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

#### 1.3.6 Error Handling Strategy

```java
// Hierarchy of exceptions
RuntimeException
    └── BusinessException (custom)
            ├── 400 Bad Request   → BusinessException.badRequest()
            ├── 401 Unauthorized  → Spring Security handles
            ├── 403 Forbidden     → BusinessException.forbidden()
            ├── 404 Not Found     → BusinessException.notFound()
            └── 409 Conflict      → BusinessException.conflict()

// Global Exception Handler - catches all and formats consistently
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        return ResponseEntity
            .status(e.getStatus())
            .body(ApiResponse.error(e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException e) {
        List<String> errors = e.getBindingResult().getFieldErrors().stream()
            .map(err -> err.getField() + ": " + err.getDefaultMessage())
            .toList();
        return ResponseEntity.badRequest().body(ApiResponse.validationError(errors));
    }
}
```

**Consistent Response Format:**

```json
// Success
{"success": true, "message": "...", "data": {...}, "timestamp": "..."}

// Error
{"success": false, "message": "...", "errors": [...], "timestamp": "..."}
```

---

## 2. Flow 1: Registrasi & Login

### 2.1 Business Flow

```
Customer membuka app → Register dengan email/password
                    → System membuat User dengan role CUSTOMER
                    → System generate JWT token
                    → Customer bisa login dengan token tersebut
```

### 2.2 Entity: User.java

**Lokasi:** `entity/User.java`

**Alasan dibuat:**
User adalah entitas utama yang merepresentasikan semua pengguna sistem (Customer dan Internal Staff).

**Kode:**

```java
@Entity
@Table(name = "users")
public class User implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;  // Unique karena dipakai untuk login

    @Column(nullable = false)
    private String password;  // Disimpan dalam bentuk BCrypt hash

    @Enumerated(EnumType.STRING)
    private UserType userType;  // CUSTOMER atau INTERNAL

    @Column(nullable = false)
    private Boolean isActive = true;  // Untuk soft-disable user

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();  // User bisa punya banyak role

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;  // Untuk internal staff, Customer = null
}
```

**Penjelasan field:**
| Field | Tipe | Penjelasan |
|-------|------|------------|
| `id` | Long | Primary key auto-increment |
| `name` | String | Nama lengkap user |
| `email` | String | Email unik untuk login |
| `password` | String | Password ter-hash dengan BCrypt |
| `userType` | Enum | CUSTOMER atau INTERNAL |
| `isActive` | Boolean | Flag untuk disable tanpa delete |
| `roles` | Set<Role> | Many-to-Many ke Role |
| `branch` | Branch | Many-to-One ke Branch (khusus internal) |

**Output (Tabel di DB):**

```
users
├── id (BIGINT, PK)
├── name (VARCHAR)
├── email (VARCHAR, UNIQUE)
├── password (VARCHAR) → "$2a$10$..."
├── user_type (VARCHAR) → "CUSTOMER" / "INTERNAL"
├── is_active (BIT)
└── branch_id (BIGINT, FK, nullable)

user_roles (join table)
├── user_id (BIGINT, FK)
└── role_id (BIGINT, FK)
```

---

### 2.3 DTO: RegisterRequest.java

**Lokasi:** `dto/request/RegisterRequest.java`

**Alasan dibuat:**
Memisahkan data yang diterima dari client dengan entity. Ini memungkinkan validasi terpisah dan tidak expose struktur entity ke client.

**Kode:**

```java
@Data
public class RegisterRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters")
    private String password;
}
```

**Penjelasan anotasi validasi:**
| Anotasi | Fungsi |
|---------|--------|
| `@NotBlank` | Field tidak boleh null atau kosong |
| `@Email` | Format harus valid email |
| `@Size(min=6)` | Minimal 6 karakter |

**Output validasi error (jika gagal):**

```json
{
  "success": false,
  "message": "Validation failed",
  "errors": [
    "email: Email must be valid",
    "password: Password must be between 6 and 100 characters"
  ]
}
```

---

### 2.4 Service: AuthServiceImpl.java - register()

**Lokasi:** `service/impl/AuthServiceImpl.java`

**Alasan dibuat:**
Memisahkan business logic dari controller. Service ini menangani:

1. Cek apakah email sudah terdaftar
2. Hash password dengan BCrypt
3. Assign role CUSTOMER
4. Generate JWT token

**Kode:**

```java
@Override
@Transactional
public AuthResponse register(RegisterRequest request) {
    // STEP 1: Cek apakah email sudah ada
    if (userRepository.existsByEmail(request.getEmail())) {
        throw BusinessException.conflict("Email already registered");
    }

    // STEP 2: Ambil role CUSTOMER dari database
    Role customerRole = roleRepository.findByName(RoleName.CUSTOMER)
        .orElseThrow(() -> BusinessException.notFound("Customer role not found"));

    // STEP 3: Build entity User baru
    User user = User.builder()
        .name(request.getName())
        .email(request.getEmail())
        .password(passwordEncoder.encode(request.getPassword())) // BCrypt hash
        .userType(UserType.CUSTOMER)     // Hardcode CUSTOMER untuk register biasa
        .isActive(true)
        .roles(new HashSet<>(Set.of(customerRole)))
        .build();

    // STEP 4: Simpan ke database
    user = userRepository.save(user);

    // STEP 5: Buat UserProfile kosong
    UserProfile profile = UserProfile.builder()
        .user(user)
        .build();
    userProfileRepository.save(profile);

    // STEP 6: Generate JWT token
    String token = jwtUtil.generateToken(user.getEmail(), getRoleStrings(user));

    // STEP 7: Return response
    return buildAuthResponse(user, token);
}
```

**Penjelasan step-by-step:**

| Step | Aksi                | Alasan                                                    |
| ---- | ------------------- | --------------------------------------------------------- |
| 1    | Cek email exists    | Mencegah duplikasi, karena email adalah identifier unik   |
| 2    | Ambil role CUSTOMER | Role wajib ada, kalau tidak ada berarti konfigurasi salah |
| 3    | Build User          | Membuat objek User dengan semua field yang diperlukan     |
| 4    | Save User           | Persist ke database, ID akan di-generate otomatis         |
| 5    | Buat UserProfile    | Setiap customer harus punya profile (awalnya kosong)      |
| 6    | Generate JWT        | Token untuk autentikasi request berikutnya                |
| 7    | Return response     | Kirim token dan info user ke client                       |

**Output (AuthResponse):**

```json
{
  "success": true,
  "message": "Registration successful",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "userId": 10,
    "email": "customer@email.com",
    "name": "Customer Name",
    "roles": ["CUSTOMER"],
    "permissions": ["LOAN_CREATE", "LOAN_READ", "PROFILE_READ", ...]
  }
}
```

---

### 2.5 Controller: AuthController.java

**Lokasi:** `controller/AuthController.java`

**Alasan dibuat:**
Controller adalah entry point untuk HTTP request. Tugasnya:

1. Menerima request
2. Memanggil service
3. Mengembalikan response dalam format yang konsisten

**Kode:**

```java
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request) {

        AuthResponse response = authService.register(request);

        return ResponseEntity
            .status(HttpStatus.CREATED)  // 201
            .body(ApiResponse.success("Registration successful", response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {

        AuthResponse response = authService.login(request);

        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }
}
```

**Penjelasan anotasi:**

| Anotasi                        | Fungsi                                              |
| ------------------------------ | --------------------------------------------------- |
| `@RestController`              | Kombinasi @Controller + @ResponseBody (return JSON) |
| `@RequestMapping("/api/auth")` | Base URL untuk semua endpoint di controller ini     |
| `@PostMapping("/register")`    | HTTP POST ke /api/auth/register                     |
| `@Valid`                       | Trigger validasi dari anotasi di DTO                |
| `@RequestBody`                 | Parse JSON body ke object                           |

**Output sukses (HTTP 201):**

```json
{
  "success": true,
  "message": "Registration successful",
  "data": {
    /* AuthResponse */
  },
  "timestamp": "2025-12-24T10:00:00"
}
```

---

### 2.6 Security: JWT Token Generation

**Lokasi:** `security/JwtUtil.java`

**Alasan dibuat:**
JWT (JSON Web Token) digunakan untuk authentication stateless. Token berisi informasi user yang di-sign dengan secret key.

**Kode:**

```java
public String generateToken(String email, List<String> roles) {
    Map<String, Object> claims = new HashMap<>();
    claims.put("roles", roles);  // Simpan roles di dalam token

    return Jwts.builder()
        .setClaims(claims)
        .setSubject(email)  // Identifier utama
        .setIssuedAt(new Date())
        .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
        .signWith(getSigningKey(), SignatureAlgorithm.HS256)
        .compact();
}
```

**Struktur JWT Token:**

```
Header.Payload.Signature

eyJhbGciOiJIUzI1NiJ9.         ← Header (algorithm)
eyJyb2xlcyI6WyJDVVNUT01FUiJ...←Payload (claims)
.6ymJXRpeFyotKVposMazrIdwTS... ← Signature (verify)
```

**Payload yang tersimpan di token:**

```json
{
  "sub": "customer@email.com",
  "roles": ["CUSTOMER", "ROLE_CUSTOMER"],
  "iat": 1703404800,
  "exp": 1703491200
}
```

---

### 2.7 Flow: Password Reset & Logout (Redis Token Blacklist)

**Alasan dibuat:**
JWT tokens tidak bisa di-revoke setelah di-issue. Untuk security, kita perlu mekanisme untuk:

1. Blacklist token saat logout
2. Invalidate semua token user saat password reset

**Architecture:**

```
┌─────────────────────────────────────────────────────────────┐
│                      Redis Database                          │
├─────────────────────────────────────────────────────────────┤
│  blacklist:{token}        → "1" (TTL = remaining lifetime)  │
│  password-changed:{email} → timestamp (TTL = jwt.expiration)│
│  password-reset:{token}   → userId (TTL = 30 minutes)        │
└─────────────────────────────────────────────────────────────┘
```

**Service: TokenBlacklistService.java**

```java
public interface TokenBlacklistService {
    boolean isTokenBlacklisted(String token, String email, long issuedAt);
    void blacklistToken(String token, long ttlMillis);
    void invalidateAllUserTokens(String email);
}
```

**Implementasi: TokenBlacklistServiceImpl.java**

```java
@Service
public class TokenBlacklistServiceImpl implements TokenBlacklistService {

    @Override
    public boolean isTokenBlacklisted(String token, String email, long issuedAt) {
        // CHECK 1: Token ada di blacklist?
        if (redisTemplate.hasKey("blacklist:" + token)) {
            return true;
        }

        // CHECK 2: Password changed setelah token di-issue?
        String changedTime = redisTemplate.opsForValue().get("password-changed:" + email);
        if (changedTime != null && issuedAt < Long.parseLong(changedTime)) {
            return true;  // Token issued sebelum password change
        }

        return false;
    }
}
```

**Flow: Logout**

```
User → POST /api/auth/logout
     → AuthController.logout()
     → TokenBlacklistService.blacklistToken(token, remainingTTL)
     → Redis: SET blacklist:{token} "1" EX {remainingSeconds}
     → Response: "Logged out successfully"
```

**Flow: Password Reset**

```
User → POST /api/auth/forgot-password
     → Generate reset token
     → Redis: SET password-reset:{token} {userId} EX 1800
     → Send email with reset link

User → POST /api/auth/reset-password
     → Validate token from Redis
     → Update password
     → TokenBlacklistService.invalidateAllUserTokens(email)
     → Redis: SET password-changed:{email} {timestamp} EX {jwtExpiration}
     → All existing tokens untuk user ini sekarang INVALID
```

**JwtAuthenticationFilter Integration:**

```java
@Override
protected void doFilterInternal(request, response, filterChain) {
    // ... extract token ...

    // CHECK: Is token blacklisted?
    Date issuedAt = jwtUtil.extractIssuedAt(jwt);
    if (tokenBlacklistService.isTokenBlacklisted(jwt, email, issuedAt.getTime())) {
        // Token invalid - don't authenticate
        filterChain.doFilter(request, response);
        return;
    }

    // ... proceed with normal validation ...
}
```

### 2.8 Service: EmailService - Password Reset Email

**Lokasi:** `service/EmailService.java`, `service/impl/EmailServiceImpl.java`

**Alasan dibuat:**
Mengirim email password reset menggunakan SMTP. Dipisahkan dari AuthService untuk separation of concerns dan agar bisa di-mock saat testing.

**Interface:**

```java
public interface EmailService {
    void sendPasswordResetEmail(String toEmail, String resetToken);
}
```

**Implementation (EmailServiceImpl):**

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.password-reset.base-url}")
    private String resetBaseUrl;

    @Override
    public void sendPasswordResetEmail(String toEmail, String resetToken) {
        log.info("Sending password reset email to: {}", toEmail);

        String resetLink = resetBaseUrl + "?token=" + resetToken;

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom("noreply@ehefin.com");
            helper.setTo(toEmail);
            helper.setSubject("🔐 Password Reset Request");
            helper.setText(buildHtmlEmailBody(resetLink, resetToken), true);

            mailSender.send(mimeMessage);
            log.info("Password reset email sent successfully to: {}", toEmail);
        } catch (MessagingException e) {
            log.error("Failed to send password reset email to: {}", toEmail, e);
            throw new RuntimeException("Failed to send password reset email", e);
        }
    }
}
```

**Penjelasan:**

| Component           | Deskripsi                                               |
| ------------------- | ------------------------------------------------------- |
| `JavaMailSender`    | Spring Boot auto-config dari `spring-boot-starter-mail` |
| `MimeMessageHelper` | Untuk mengirim email HTML dengan proper encoding        |
| `resetBaseUrl`      | Environment variable untuk frontend reset page URL      |
| HTML Template       | Email dengan gradient header, button, dan token display |

**Configuration (application.properties):**

```properties
# SMTP (Mailtrap for development)
spring.mail.host=sandbox.smtp.mailtrap.io
spring.mail.port=2525
spring.mail.username=${MAIL_USERNAME}
spring.mail.password=${MAIL_PASSWORD}

# Password Reset
app.password-reset.base-url=http://localhost:4200/reset-password
app.password-reset.token-expiry-minutes=30
```

**Flow Integration dengan AuthService:**

```
POST /api/auth/forgot-password
     → AuthService.forgotPassword(email)
     → Generate UUID token
     → Redis: SET password-reset:{token} {userId} EX 1800
     → EmailService.sendPasswordResetEmail(email, token)  ← Email dikirim!
     → Response: "If the email exists, a password reset link has been sent"
```

---

## 3. Flow 2: Customer Profile

### 3.1 Business Flow

```
Customer login → GET /api/customer/profile → Lihat profile (mungkin kosong)
             → PUT /api/customer/profile → Lengkapi profile (NIK, birthdate, dll)
             → Profile isComplete = true
             → Sekarang bisa submit loan
```

### 3.2 Entity: UserProfile.java

**Lokasi:** `entity/UserProfile.java`

**Alasan dibuat:**
Memisahkan data profile dari User. Ini karena:

1. Profile hanya untuk Customer, bukan Internal
2. Data profile bisa kompleks dan opsional
3. Separation of concerns

**Kode:**

```java
@Entity
@Table(name = "user_profiles")
public class UserProfile implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;  // Satu user, satu profile

    @Column(length = 16)
    private String nik;  // Nomor Induk Kependudukan, 16 digit

    private LocalDate birthdate;

    private String phone;

    @Column(columnDefinition = "TEXT")
    private String address;

    // Method untuk cek kelengkapan profile
    public boolean isComplete() {
        return nik != null && !nik.isEmpty() &&
               birthdate != null &&
               phone != null && !phone.isEmpty() &&
               address != null && !address.isEmpty();
    }
}
```

**Kenapa ada `isComplete()`?**
Karena untuk submit loan, customer WAJIB memiliki profile lengkap. Method ini digunakan untuk validasi di `LoanApplicationServiceImpl`.

---

### 3.3 Service: CustomerServiceImpl.java

**Lokasi:** `service/impl/CustomerServiceImpl.java`

**Kode getProfile():**

```java
@Override
public UserResponse getProfile(String email) {
    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> BusinessException.notFound("User not found"));

    return mapToUserResponse(user);
}
```

**Kode updateProfile():**

```java
@Override
@Transactional
public ProfileResponse updateProfile(String email, UpdateProfileRequest request) {
    // STEP 1: Cari user
    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> BusinessException.notFound("User not found"));

    // STEP 2: Cari atau buat profile
    UserProfile profile = userProfileRepository.findByUserId(user.getId())
        .orElseGet(() -> UserProfile.builder().user(user).build());

    // STEP 3: Update fields
    profile.setNik(request.getNik());
    profile.setBirthdate(request.getBirthdate());
    profile.setPhone(request.getPhoneNumber());
    profile.setAddress(request.getAddress());

    // STEP 4: Save
    profile = userProfileRepository.save(profile);

    // STEP 5: Return response
    return mapToProfileResponse(profile);
}
```

**Output updateProfile:**

```json
{
  "success": true,
  "message": "Profile updated successfully",
  "data": {
    "nik": "3201234567890001",
    "birthdate": "1990-05-15",
    "phoneNumber": "+6281234567890",
    "address": "Jl. Sudirman No. 123, Jakarta",
    "isComplete": true // ← Sekarang true, bisa submit loan
  }
}
```

---

## 4. Flow 3: Plafond Selection

### 4.1 Business Flow

```
Customer dengan profile lengkap
    → GET /api/products → Lihat daftar produk (BRONZE, SILVER, GOLD, PLATINUM)
    → POST /api/customer/plafond → Pilih satu produk sebagai plafond
    → UserPlafond dibuat dengan remainingAmount = product.amount
    → GET /api/customer/plafond → Lihat plafond aktif
```

### 4.2 Entity: Product.java

**Lokasi:** `entity/Product.java`

**Alasan dibuat:**
Product mendefinisikan tier kredit dengan limit berbeda.

**Kode:**

```java
@Entity
@Table(name = "products")
public class Product implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;  // BRONZE, SILVER, GOLD, PLATINUM

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;  // Maksimum pinjaman: 5jt, 10jt, 25jt, 50jt

    @Column(nullable = false)
    private Integer tenor;  // Maksimum tenor: 12, 24, 36, 48 bulan

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal interestRate;  // Minimum bunga: 12%, 10%, 8.5%, 7%
}
```

**Data Product (dari DataInitializer):**

| Name     | Amount     | Tenor | Rate |
| -------- | ---------- | ----- | ---- |
| BRONZE   | 5,000,000  | 12    | 12%  |
| SILVER   | 10,000,000 | 24    | 10%  |
| GOLD     | 25,000,000 | 36    | 8.5% |
| PLATINUM | 50,000,000 | 48    | 7%   |

---

### 4.3 Entity: UserPlafond.java

**Lokasi:** `entity/UserPlafond.java`

**Alasan dibuat:**
UserPlafond merepresentasikan plafond kredit yang dipilih customer. Field `remainingAmount` yang kita tambahkan melacak sisa kredit yang tersedia.

**Kode:**

```java
@Entity
@Table(name = "user_plafonds")
public class UserPlafond implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;  // ManyToOne: Satu user bisa punya banyak plafond (tapi hanya 1 aktif)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;  // Produk yang dipilih

    @Column(name = "remaining_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal remainingAmount;  // ← FIELD BARU: Sisa kredit

    @Column(name = "assigned_at", nullable = false)
    private LocalDateTime assignedAt = LocalDateTime.now();

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;  // False jika remainingAmount = 0
}
```

**Kenapa `@ManyToOne` bukan `@OneToOne`?**

Awalnya adalah `@OneToOne` dengan `unique = true`. Tapi kita ubah ke `@ManyToOne` karena:

1. Ketika plafond habis (`remainingAmount = 0`), `isActive = false`
2. Customer harus bisa pilih plafond BARU
3. Dengan `@OneToOne unique`, customer tidak bisa punya record plafond kedua
4. `@ManyToOne` memungkinkan multiple records (hanya 1 yang `isActive = true`)

---

### 4.4 Service: PlafondServiceImpl.java

**Lokasi:** `service/impl/PlafondServiceImpl.java`

**Kode selectPlafond():**

```java
@Override
@Transactional
public UserPlafondResponse selectPlafond(String email, SelectPlafondRequest request) {
    log.info("Selecting plafond for user: {}", email);

    // STEP 1: Cari user
    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> BusinessException.notFound("User not found"));

    // STEP 2: Cek apakah sudah punya plafond AKTIF
    if (userPlafondRepository.existsByUserIdAndIsActiveTrue(user.getId())) {
        throw BusinessException.badRequest(
            "You already have an active plafond. Cannot select another one.");
    }
    // ↑ Ini mencegah customer punya 2 plafond aktif sekaligus

    // STEP 3: Cari product
    Product product = productRepository.findById(request.getProductId())
        .orElseThrow(() -> BusinessException.notFound("Product not found"));

    // STEP 4: Buat UserPlafond dengan remainingAmount = product.amount
    UserPlafond userPlafond = UserPlafond.builder()
        .user(user)
        .product(product)
        .remainingAmount(product.getAmount())  // ← Inisialisasi penuh
        .isActive(true)
        .build();

    userPlafond = userPlafondRepository.save(userPlafond);

    log.info("Plafond selected: User={}, Product={}", email, product.getName());

    return mapToResponse(userPlafond);
}
```

**Kode mapToResponse():**

```java
private UserPlafondResponse mapToResponse(UserPlafond userPlafond) {
    return UserPlafondResponse.builder()
        .id(userPlafond.getId())
        .product(mapToProductResponse(userPlafond.getProduct()))
        .originalAmount(userPlafond.getProduct().getAmount())  // Dari product
        .remainingAmount(userPlafond.getRemainingAmount())     // Dari plafond
        .assignedAt(userPlafond.getAssignedAt())
        .isActive(userPlafond.getIsActive())
        .build();
}
```

**Output selectPlafond:**

```json
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
    "originalAmount": 5000000, // ← Limit awal dari product
    "remainingAmount": 5000000, // ← Sisa (belum ada loan)
    "assignedAt": "2025-12-24T10:00:00",
    "isActive": true
  }
}
```

---

## 5. Flow 4: Loan Submission

### 5.1 Business Flow

```
Customer dengan plafond aktif
    → POST /api/loans dengan {branchId, amount, tenor, interestRate}
    → Validasi:
        • Profile lengkap?
        • Punya plafond aktif?
        • amount ≤ remainingAmount?
        • tenor ≤ product.tenor?
        • rate ≥ product.interestRate?
        • branch exists?
    → Jika valid: Buat LoanApplication dengan status SUBMITTED
    → Snapshot data customer disimpan di loan
```

### 5.2 Entity: LoanApplication.java

**Lokasi:** `entity/LoanApplication.java`

**Alasan dibuat:**
LoanApplication adalah entitas utama yang merepresentasikan pengajuan pinjaman. Memiliki snapshot data customer karena data customer bisa berubah setelah pengajuan.

**Kode (partial):**

```java
@Entity
@Table(name = "loan_applications")
public class LoanApplication implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    // ===== CUSTOMER SNAPSHOT =====
    // Data customer di-copy pada saat submit, tidak ambil dari relasi
    @Column(name = "customer_name_snapshot", nullable = false)
    private String customerNameSnapshot;

    @Column(name = "customer_email_snapshot", nullable = false)
    private String customerEmailSnapshot;

    @Column(name = "customer_nik_snapshot", length = 16)
    private String customerNikSnapshot;

    @Column(name = "customer_phone_snapshot")
    private String customerPhoneSnapshot;

    @Column(name = "customer_address_snapshot", columnDefinition = "TEXT")
    private String customerAddressSnapshot;

    @Column(name = "customer_birthdate_snapshot")
    private LocalDate customerBirthdateSnapshot;
    // ===== END SNAPSHOT =====

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @Column(name = "requested_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal requestedAmount;  // Jumlah yang diajukan

    @Column(name = "requested_tenor", nullable = false)
    private Integer requestedTenor;  // Tenor yang diajukan (bulan)

    @Column(name = "requested_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal requestedRate;  // Bunga yang diajukan (%)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LoanStatus status;  // SUBMITTED, MARKETING_APPROVED, APPROVED, dll

    @OneToMany(mappedBy = "loanApplication", cascade = CascadeType.ALL)
    private List<LoanApplicationHistory> histories = new ArrayList<>();
}
```

**Kenapa ada Customer Snapshot?**

Contoh skenario:

1. Customer submit loan dengan alamat "Jakarta"
2. Loan dalam proses review
3. Customer update profile, alamat jadi "Surabaya"
4. Reviewer melihat loan → harus tetap lihat "Jakarta" (data saat submit)

Tanpa snapshot, data yang ditampilkan bisa berubah dan menyebabkan inkonsistensi.

---

### 5.3 Service: LoanApplicationServiceImpl.java - submitLoan()

**Lokasi:** `service/impl/LoanApplicationServiceImpl.java`

**Kode lengkap dengan penjelasan:**

```java
@Override
@Transactional
public LoanApplicationResponse submitLoan(String email, SubmitLoanRequest request) {
    log.info("Submitting loan for user: {}", email);

    // ========== STEP 1: Ambil data user ==========
    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> BusinessException.notFound("User not found"));

    // ========== STEP 2: Validasi profile lengkap ==========
    UserProfile profile = userProfileRepository.findByUserId(user.getId())
        .orElseThrow(() -> BusinessException.badRequest(
            "Please complete your profile before submitting a loan application."));

    if (!profile.isComplete()) {
        throw BusinessException.badRequest(
            "Please complete your profile before submitting a loan application. " +
            "Required fields: NIK, birthdate, phone, and address.");
    }
    // ↑ Customer dengan profile kosong tidak boleh submit

    // ========== STEP 3: Validasi plafond aktif ==========
    UserPlafond userPlafond = userPlafondRepository.findByUserIdAndIsActiveTrue(user.getId())
        .orElseThrow(() -> BusinessException.badRequest(
            "Please select a plafond first before submitting a loan application."));
    // ↑ Customer tanpa plafond aktif tidak boleh submit

    Product product = userPlafond.getProduct();

    // ========== STEP 4: Validasi amount terhadap REMAINING AMOUNT ==========
    if (request.getAmount().compareTo(userPlafond.getRemainingAmount()) > 0) {
        throw BusinessException.badRequest(
            "Requested amount exceeds remaining plafond. Remaining: Rp " +
            userPlafond.getRemainingAmount());
    }
    // ↑ BUKAN product.getAmount()! Tapi remainingAmount dari plafond
    // Jika plafond 5jt dan sudah terpakai 3jt, sisa 2jt. Request 3jt = DITOLAK

    // ========== STEP 5: Validasi tenor ==========
    if (request.getTenor() > product.getTenor()) {
        throw BusinessException.badRequest(
            "Requested tenor exceeds plafond limit. Maximum: " +
            product.getTenor() + " months");
    }

    // ========== STEP 6: Validasi interest rate ==========
    if (request.getInterestRate().compareTo(product.getInterestRate()) < 0) {
        throw BusinessException.badRequest(
            "Interest rate cannot be lower than plafond minimum rate. Minimum: " +
            product.getInterestRate() + "%");
    }

    // ========== STEP 7: Validasi branch ==========
    Branch branch = branchRepository.findById(request.getBranchId())
        .orElseThrow(() -> BusinessException.notFound("Branch not found"));

    // ========== STEP 8: Buat LoanApplication dengan SNAPSHOT ==========
    LoanApplication loan = LoanApplication.builder()
        .customer(user)
        // Snapshot data customer saat ini
        .customerNameSnapshot(user.getName())
        .customerEmailSnapshot(user.getEmail())
        .customerNikSnapshot(profile.getNik())
        .customerPhoneSnapshot(profile.getPhone())
        .customerAddressSnapshot(profile.getAddress())
        .customerBirthdateSnapshot(profile.getBirthdate())
        // Data loan
        .product(product)
        .branch(branch)
        .requestedAmount(request.getAmount())
        .requestedTenor(request.getTenor())
        .requestedRate(request.getInterestRate())
        .status(LoanStatus.SUBMITTED)  // Status awal
        .build();

    loan = loanApplicationRepository.save(loan);

    // ========== STEP 9: Buat history entry pertama ==========
    LoanApplicationHistory history = LoanApplicationHistory.builder()
        .loanApplication(loan)
        .status(LoanStatus.SUBMITTED)
        .note("Loan application submitted")
        .approvedBy(user)  // Disubmit oleh customer sendiri
        .build();

    loanHistoryRepository.save(history);

    log.info("Loan submitted: ID={}, Amount={}, Status={}",
        loan.getId(), loan.getRequestedAmount(), loan.getStatus());

    return mapToResponse(loan);
}
```

**Diagram validasi:**

```
Request masuk
    │
    ├─ Has pending loan? ─── YES ──→ 400: "You already have a pending loan application..."
    │
    ├─ Profile lengkap? ─── NO ──→ 400: "Please complete your profile..."
    │
    ├─ Punya plafond aktif? ─ NO ──→ 400: "Please select a plafond first..."
    │
    ├─ amount ≤ remainingAmount? ─ NO ──→ 400: "Requested amount exceeds remaining plafond..."
    │
    ├─ tenor ≤ product.tenor? ─ NO ──→ 400: "Requested tenor exceeds plafond limit..."
    │
    ├─ rate ≥ product.rate? ─ NO ──→ 400: "Interest rate cannot be lower than..."
    │
    ├─ branch exists? ─ NO ──→ 404: "Branch not found"
    │
    └─ ALL VALID ──→ Create LoanApplication ──→ 201 Created
```

**Output sukses:**

```json
{
  "success": true,
  "message": "Loan application submitted successfully",
  "data": {
    "id": 1,
    "customerName": "John Doe",        // Snapshot
    "customerEmail": "john@email.com", // Snapshot
    "customerNik": "3201234567890001", // Snapshot
    "product": {"name": "BRONZE", ...},
    "branch": {"location": "Jakarta"},
    "requestedAmount": 3000000,
    "requestedTenor": 6,
    "requestedRate": 12.0,
    "status": "SUBMITTED",
    "createdAt": "2025-12-24T10:00:00"
  }
}
```

---

## 6. Flow 5: Approval Workflow

### 6.1 Business Flow

```
SUBMITTED
    ↓ Marketing approve
MARKETING_APPROVED
    ↓ Branch Manager approve
BRANCH_MANAGER_APPROVED
    ↓ Backoffice approve
APPROVED ← remainingAmount dikurangi di sini!
         ← Jika remainingAmount = 0, isActive = false
```

### 6.2 Enum: LoanStatus.java

**Lokasi:** `enums/LoanStatus.java`

```java
public enum LoanStatus {
    SUBMITTED,                    // Customer baru submit
    MARKETING_APPROVED,           // Disetujui Marketing
    MARKETING_REJECTED,           // Ditolak Marketing
    BRANCH_MANAGER_APPROVED,      // Disetujui Branch Manager
    BRANCH_MANAGER_REJECTED,      // Ditolak Branch Manager
    APPROVED,                     // Final approval oleh Backoffice
    REJECTED                      // Final rejection
}
```

### 6.3 Service: ApprovalServiceImpl.java - approve()

**Lokasi:** `service/impl/ApprovalServiceImpl.java`

**Kode dengan penjelasan lengkap:**

```java
@Override
@Transactional
public LoanApplicationResponse approve(Long loanId, String approverEmail, ApprovalRequest request) {

    // ========== STEP 1: Ambil data loan dan approver ==========
    LoanApplication loan = loanApplicationRepository.findById(loanId)
        .orElseThrow(() -> BusinessException.notFound("Loan application not found"));

    User approver = userRepository.findByEmail(approverEmail)
        .orElseThrow(() -> BusinessException.notFound("User not found"));

    // ========== STEP 2: Tentukan role approver ==========
    String role = determineApproverRole(approver);
    // Marketing, BranchManager, atau Backoffice

    // ========== STEP 3: Validasi status saat ini ==========
    LoanStatus expectedStatus = getExpectedStatusForRole(role);
    // Marketing expects SUBMITTED
    // BranchManager expects MARKETING_APPROVED
    // Backoffice expects BRANCH_MANAGER_APPROVED

    if (loan.getStatus() != expectedStatus) {
        throw BusinessException.badRequest(
            "Loan is not in the correct status for your approval. Current status: " +
            loan.getStatus() + ", Expected: " + expectedStatus);
    }

    // ========== STEP 4: Validasi branch (untuk Marketing & BM) ==========
    if (!role.equals("BACKOFFICE")) {
        if (approver.getBranch() == null ||
            !approver.getBranch().getId().equals(loan.getBranch().getId())) {
            throw BusinessException.forbidden(
                "You can only process loans from your branch");
        }
    }
    // Backoffice bisa approve dari semua branch

    // ========== STEP 5: Tentukan status baru ==========
    LoanStatus newStatus = getNextApprovedStatus(role);
    // Marketing → MARKETING_APPROVED
    // BranchManager → BRANCH_MANAGER_APPROVED
    // Backoffice → APPROVED

    // ========== STEP 6: Update loan status ==========
    loan.setStatus(newStatus);
    loan = loanApplicationRepository.save(loan);

    // ========== STEP 7: Buat history entry ==========
    createHistoryEntry(loan, approver, role, newStatus, request.getNote());

    // ========== STEP 8: JIKA FINAL APPROVAL, KURANGI PLAFOND ==========
    if (newStatus == LoanStatus.APPROVED) {
        deductPlafondRemainingAmount(loan);
    }

    log.info("Loan {} approved by {} ({}) → Status: {}",
        loanId, approverEmail, role, newStatus);

    return mapToResponse(loan);
}
```

**STEP 8: Deduct Plafond (KUNCI FITUR BARU)**

```java
private void deductPlafondRemainingAmount(LoanApplication loan) {
    // Cari plafond aktif milik customer
    UserPlafond userPlafond = userPlafondRepository
        .findByUserIdAndIsActiveTrue(loan.getCustomer().getId())
        .orElse(null);

    if (userPlafond != null) {
        // Kurangi remainingAmount dengan requestedAmount
        BigDecimal newRemaining = userPlafond.getRemainingAmount()
            .subtract(loan.getRequestedAmount());

        userPlafond.setRemainingAmount(newRemaining);

        // Jika habis, set inactive
        if (newRemaining.compareTo(BigDecimal.ZERO) <= 0) {
            userPlafond.setIsActive(false);
            log.info("Plafond depleted for user {}, setting inactive",
                loan.getCustomer().getEmail());
        }

        userPlafondRepository.save(userPlafond);

        log.info("Plafond updated: remaining={}, isActive={}",
            userPlafond.getRemainingAmount(), userPlafond.getIsActive());
    }
}
```

**Contoh skenario:**

```
Initial: Plafond BRONZE (5jt), remainingAmount = 5jt

Loan 1: 3jt → APPROVED
    → remainingAmount = 5jt - 3jt = 2jt
    → isActive = true

Loan 2: 2jt → APPROVED
    → remainingAmount = 2jt - 2jt = 0
    → isActive = false  ← PLAFOND HABIS!

Customer coba submit Loan 3: 1jt
    → Error: "Please select a plafond first..."
    → Karena tidak ada plafond aktif

Customer pilih plafond baru (SILVER)
    → remainingAmount = 10jt (fresh start)
```

---

## 7. Flow 6: SuperAdmin Management

### 7.1 Business Flow

```
SuperAdmin login
    → GET /api/admin/users → Lihat semua user
    → POST /api/admin/users → Buat internal user baru
    → POST /api/admin/users/{id}/roles → Assign role
    → DELETE /api/admin/users/{id}/roles/{roleId} → Hapus role
    → PUT /api/admin/roles/{id}/permissions → Update permission role
```

### 7.2 Controller: SuperAdminController.java

**Lokasi:** `controller/SuperAdminController.java`

```java
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPERADMIN')")  // ← Hanya SUPERADMIN
public class SuperAdminController {

    private final SuperAdminService superAdminService;

    @PostMapping("/users")
    public ResponseEntity<ApiResponse<UserResponse>> createInternalUser(
            @Valid @RequestBody CreateInternalUserRequest request) {

        UserResponse response = superAdminService.createInternalUser(request);

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("User created successfully", response));
    }

    @PostMapping("/users/{userId}/roles")
    public ResponseEntity<ApiResponse<UserResponse>> assignRole(
            @PathVariable Long userId,
            @Valid @RequestBody AssignRoleRequest request) {

        UserResponse response = superAdminService.assignRole(userId, request.getRoleId());

        return ResponseEntity.ok(ApiResponse.success("Role assigned successfully", response));
    }
}
```

**Kenapa ada `@PreAuthorize("hasRole('SUPERADMIN')")`?**

Ini adalah annotation Spring Security yang memastikan hanya user dengan role SUPERADMIN yang bisa mengakses endpoint ini. Jika user lain mencoba akses:

```json
{
  "success": false,
  "message": "Access Denied",
  "timestamp": "..."
}
```

HTTP Status: 403 Forbidden

---

## 8. Data Initializer

### 8.1 Purpose

**Lokasi:** `initializer/DataInitializer.java`

DataInitializer adalah `CommandLineRunner` yang dijalankan saat aplikasi start. Fungsinya:

1. Membuat data master (Branch, Role, Permission, Product)
2. Membuat user default untuk testing
3. Hanya berjalan jika data belum ada (idempotent)

### 8.2 Flow

```java
@Override
@Transactional
public void run(String... args) {
    log.info("Starting data initialization...");

    initializeBranches();     // JKT, SBY, BDG
    initializePermissions();  // 24 permissions
    initializeRoles();        // SUPERADMIN, MARKETING, BRANCH_MANAGER, BACKOFFICE, CUSTOMER
    initializeUsers();        // Default users untuk testing
    initializeProducts();     // BRONZE, SILVER, GOLD, PLATINUM

    log.info("Data initialization completed!");
}
```

### 8.3 Kenapa Perlu Cek Exist?

```java
private void initializeBranches() {
    if (branchRepository.count() > 0) {
        log.info("Branches already exist, skipping...");
        return;  // ← SKIP jika sudah ada
    }
    // Create branches...
}
```

Ini mencegah:

1. Duplicate data saat restart
2. Error karena unique constraint
3. Data test development masuk ke production

---

## Summary: Complete Business Flow

```
┌────────────────────────────────────────────────────────────────────────────┐
│                           CUSTOMER JOURNEY                                  │
└────────────────────────────────────────────────────────────────────────────┘

1. REGISTER
   POST /api/auth/register → User created → JWT token returned

2. COMPLETE PROFILE
   PUT /api/customer/profile → NIK, birthdate, phone, address → isComplete = true

3. SELECT PLAFOND
   GET /api/products → See BRONZE/SILVER/GOLD/PLATINUM
   POST /api/customer/plafond → Select one → remainingAmount initialized

4. SUBMIT LOAN
   POST /api/loans → Validate against remainingAmount → Status = SUBMITTED

5. TRACK STATUS
   GET /api/loans → See my loans
   GET /api/loans/{id}/history → See approval timeline

┌────────────────────────────────────────────────────────────────────────────┐
│                          INTERNAL STAFF JOURNEY                             │
└────────────────────────────────────────────────────────────────────────────┘

1. LOGIN
   POST /api/auth/login → JWT token with role-based permissions

2. GET PENDING (based on role)
   GET /api/approval/pending

3. REVIEW & ACTION
   POST /api/approval/{id}/approve → Move to next status
   POST /api/approval/{id}/reject → Terminal rejection

4. FINAL APPROVAL (Backoffice)
   → Status = APPROVED
   → remainingAmount -= requestedAmount
   → If remainingAmount = 0 → isActive = false

┌────────────────────────────────────────────────────────────────────────────┐
│                         PLAFOND LIFECYCLE                                   │
└────────────────────────────────────────────────────────────────────────────┘

Customer selects BRONZE (5jt)
    → remainingAmount = 5,000,000
    → isActive = true

Loan 3jt APPROVED
    → remainingAmount = 2,000,000
    → isActive = true

Loan 2jt APPROVED
    → remainingAmount = 0
    → isActive = false  ← DEPLETED!

Customer selects SILVER (10jt)
    → NEW remainingAmount = 10,000,000
    → NEW isActive = true
```

---

_Dokumentasi dibuat: 2026-01-01_
