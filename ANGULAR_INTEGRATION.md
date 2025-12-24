# Angular Integration Guide (Web Dashboard)

> Complete guide for integrating Loan Application Backend API with Angular web app for internal staff

---

## Table of Contents

1. [Setup](#1-setup)
2. [Authentication](#2-authentication)
3. [Role-Based Features](#3-role-based-features)
4. [Approval Flow](#4-approval-flow)
5. [SuperAdmin Features](#5-superadmin-features)
6. [Data Models](#6-data-models)
7. [Error Handling](#7-error-handling)

---

## 1. Setup

### 1.1 Environment Configuration

```typescript
// environment.ts
export const environment = {
  production: false,
  apiUrl: "http://localhost:8080/api",
};

// environment.prod.ts
export const environment = {
  production: true,
  apiUrl: "https://your-server.com/api",
};
```

### 1.2 Install Dependencies

```bash
npm install @angular/common/http
npm install jwt-decode
```

### 1.3 HTTP Client Module

```typescript
// app.config.ts (Angular 17+)
import { provideHttpClient, withInterceptors } from "@angular/common/http";
import { authInterceptor } from "./core/interceptors/auth.interceptor";

export const appConfig: ApplicationConfig = {
  providers: [
    provideHttpClient(withInterceptors([authInterceptor])),
    // ...
  ],
};
```

### 1.4 Auth Interceptor

```typescript
// core/interceptors/auth.interceptor.ts
import { HttpInterceptorFn, HttpErrorResponse } from "@angular/common/http";
import { inject } from "@angular/core";
import { Router } from "@angular/router";
import { catchError, throwError } from "rxjs";
import { AuthService } from "../services/auth.service";

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const token = authService.getToken();

  if (token) {
    req = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`,
      },
    });
  }

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401) {
        authService.logout();
        router.navigate(["/login"]);
      }
      return throwError(() => error);
    })
  );
};
```

---

## 2. Authentication

### 2.1 Auth Service

```typescript
// core/services/auth.service.ts
import { Injectable, signal, computed } from "@angular/core";
import { HttpClient } from "@angular/common/http";
import { Router } from "@angular/router";
import { jwtDecode } from "jwt-decode";
import { Observable, tap, BehaviorSubject } from "rxjs";
import { environment } from "../../environments/environment";

interface AuthResponse {
  success: boolean;
  data: {
    token: string;
    tokenType: string;
    userId: number;
    email: string;
    name: string;
    roles: string[];
    permissions: string[];
  };
}

interface JwtPayload {
  sub: string;
  roles: string[];
  exp: number;
}

@Injectable({ providedIn: "root" })
export class AuthService {
  private readonly TOKEN_KEY = "jwt_token";
  private readonly USER_KEY = "user_data";

  private currentUser = signal<AuthResponse["data"] | null>(null);

  // Computed signals for easy access
  readonly isLoggedIn = computed(() => !!this.currentUser());
  readonly userRoles = computed(() => this.currentUser()?.roles || []);
  readonly userName = computed(() => this.currentUser()?.name || "");

  constructor(private http: HttpClient, private router: Router) {
    this.loadStoredUser();
  }

  private loadStoredUser(): void {
    const token = this.getToken();
    const userData = localStorage.getItem(this.USER_KEY);

    if (token && userData && !this.isTokenExpired(token)) {
      this.currentUser.set(JSON.parse(userData));
    } else {
      this.clearStorage();
    }
  }

  login(email: string, password: string): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${environment.apiUrl}/auth/login`, {
        email,
        password,
      })
      .pipe(
        tap((response) => {
          if (response.success && response.data) {
            localStorage.setItem(this.TOKEN_KEY, response.data.token);
            localStorage.setItem(this.USER_KEY, JSON.stringify(response.data));
            this.currentUser.set(response.data);
          }
        })
      );
  }

  logout(): void {
    this.clearStorage();
    this.currentUser.set(null);
    this.router.navigate(["/login"]);
  }

  getToken(): string | null {
    return localStorage.getItem(this.TOKEN_KEY);
  }

  private clearStorage(): void {
    localStorage.removeItem(this.TOKEN_KEY);
    localStorage.removeItem(this.USER_KEY);
  }

  private isTokenExpired(token: string): boolean {
    try {
      const decoded = jwtDecode<JwtPayload>(token);
      return decoded.exp * 1000 < Date.now();
    } catch {
      return true;
    }
  }

  // Role checks
  hasRole(role: string): boolean {
    return this.userRoles().includes(role);
  }

  isMarketing(): boolean {
    return this.hasRole("MARKETING");
  }
  isBranchManager(): boolean {
    return this.hasRole("BRANCH_MANAGER");
  }
  isBackoffice(): boolean {
    return this.hasRole("BACKOFFICE");
  }
  isSuperAdmin(): boolean {
    return this.hasRole("SUPERADMIN");
  }
}
```

### 2.2 Auth Guard

```typescript
// core/guards/auth.guard.ts
import { inject } from "@angular/core";
import { Router, CanActivateFn } from "@angular/router";
import { AuthService } from "../services/auth.service";

export const authGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isLoggedIn()) {
    return true;
  }

  router.navigate(["/login"]);
  return false;
};

export const roleGuard = (allowedRoles: string[]): CanActivateFn => {
  return () => {
    const authService = inject(AuthService);
    const router = inject(Router);

    const hasRole = allowedRoles.some((role) => authService.hasRole(role));

    if (!hasRole) {
      router.navigate(["/unauthorized"]);
      return false;
    }

    return true;
  };
};
```

### 2.3 Login Component

```typescript
// features/auth/login.component.ts
import { Component, signal } from "@angular/core";
import { FormBuilder, Validators, ReactiveFormsModule } from "@angular/forms";
import { Router } from "@angular/router";
import { AuthService } from "../../core/services/auth.service";

@Component({
  selector: "app-login",
  standalone: true,
  imports: [ReactiveFormsModule],
  template: `
    <div class="login-container">
      <h1>Staff Login</h1>

      @if (error()) {
      <div class="error-alert">{{ error() }}</div>
      }

      <form [formGroup]="loginForm" (ngSubmit)="onSubmit()">
        <div class="form-group">
          <label>Email</label>
          <input type="email" formControlName="email" />
        </div>

        <div class="form-group">
          <label>Password</label>
          <input type="password" formControlName="password" />
        </div>

        <button type="submit" [disabled]="loading()">
          {{ loading() ? "Logging in..." : "Login" }}
        </button>
      </form>
    </div>
  `,
})
export class LoginComponent {
  loading = signal(false);
  error = signal<string | null>(null);

  loginForm = this.fb.group({
    email: ["", [Validators.required, Validators.email]],
    password: ["", Validators.required],
  });

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router
  ) {}

  onSubmit(): void {
    if (this.loginForm.invalid) return;

    this.loading.set(true);
    this.error.set(null);

    const { email, password } = this.loginForm.value;

    this.authService.login(email!, password!).subscribe({
      next: () => {
        this.router.navigate(["/dashboard"]);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err.error?.message || "Login failed");
      },
    });
  }
}
```

---

## 3. Role-Based Features

### 3.1 Routes Configuration

```typescript
// app.routes.ts
import { Routes } from "@angular/router";
import { authGuard, roleGuard } from "./core/guards/auth.guard";

export const routes: Routes = [
  {
    path: "login",
    loadComponent: () =>
      import("./features/auth/login.component").then((m) => m.LoginComponent),
  },

  {
    path: "dashboard",
    canActivate: [authGuard],
    loadComponent: () =>
      import("./features/dashboard/dashboard.component").then(
        (m) => m.DashboardComponent
      ),
  },

  // Approval routes (Marketing, BM, Backoffice)
  {
    path: "approvals",
    canActivate: [
      authGuard,
      roleGuard(["MARKETING", "BRANCH_MANAGER", "BACKOFFICE"]),
    ],
    children: [
      {
        path: "",
        loadComponent: () =>
          import("./features/approval/pending-list.component").then(
            (m) => m.PendingListComponent
          ),
      },
      {
        path: ":id",
        loadComponent: () =>
          import("./features/approval/loan-detail.component").then(
            (m) => m.LoanDetailComponent
          ),
      },
    ],
  },

  // SuperAdmin routes
  {
    path: "admin",
    canActivate: [authGuard, roleGuard(["SUPERADMIN"])],
    children: [
      {
        path: "users",
        loadComponent: () =>
          import("./features/admin/user-list.component").then(
            (m) => m.UserListComponent
          ),
      },
      {
        path: "roles",
        loadComponent: () =>
          import("./features/admin/role-list.component").then(
            (m) => m.RoleListComponent
          ),
      },
    ],
  },

  { path: "", redirectTo: "/dashboard", pathMatch: "full" },
];
```

### 3.2 Navigation Based on Role

```typescript
// shared/components/sidebar.component.ts
import { Component, computed } from "@angular/core";
import { AuthService } from "../../core/services/auth.service";

interface NavItem {
  label: string;
  route: string;
  icon: string;
}

@Component({
  selector: "app-sidebar",
  template: `
    <nav>
      @for (item of navItems(); track item.route) {
      <a [routerLink]="item.route" routerLinkActive="active">
        <i [class]="item.icon"></i>
        {{ item.label }}
      </a>
      }
    </nav>
  `,
})
export class SidebarComponent {
  constructor(private authService: AuthService) {}

  navItems = computed<NavItem[]>(() => {
    const items: NavItem[] = [
      { label: "Dashboard", route: "/dashboard", icon: "icon-home" },
    ];

    // Approval menu for staff
    if (
      this.authService.isMarketing() ||
      this.authService.isBranchManager() ||
      this.authService.isBackoffice()
    ) {
      items.push({
        label: "Pending Approvals",
        route: "/approvals",
        icon: "icon-check",
      });
    }

    // Admin menu
    if (this.authService.isSuperAdmin()) {
      items.push(
        { label: "Users", route: "/admin/users", icon: "icon-users" },
        { label: "Roles", route: "/admin/roles", icon: "icon-shield" }
      );
    }

    return items;
  });
}
```

---

## 4. Approval Flow

### 4.1 Approval Service

```typescript
// core/services/approval.service.ts
import { Injectable } from "@angular/core";
import { HttpClient } from "@angular/common/http";
import { Observable } from "rxjs";
import { environment } from "../../environments/environment";

export interface Loan {
  id: number;
  customerName: string;
  customerEmail: string;
  customerNik: string;
  customerPhone: string;
  product: { id: number; name: string; amount: number };
  branch: { id: number; code: string; location: string };
  requestedAmount: number;
  requestedTenor: number;
  requestedRate: number;
  status: string;
  createdAt: string;
}

export interface LoanHistory {
  id: number;
  status: string;
  note: string;
  approvedBy: string;
  approvedByRole: string;
  createdAt: string;
}

interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
}

@Injectable({ providedIn: "root" })
export class ApprovalService {
  constructor(private http: HttpClient) {}

  getPendingLoans(): Observable<ApiResponse<Loan[]>> {
    return this.http.get<ApiResponse<Loan[]>>(
      `${environment.apiUrl}/approval/pending`
    );
  }

  getLoanDetail(id: number): Observable<ApiResponse<Loan>> {
    return this.http.get<ApiResponse<Loan>>(
      `${environment.apiUrl}/loans/${id}`
    );
  }

  getLoanHistory(id: number): Observable<ApiResponse<LoanHistory[]>> {
    return this.http.get<ApiResponse<LoanHistory[]>>(
      `${environment.apiUrl}/loans/${id}/history`
    );
  }

  approveLoan(id: number, note?: string): Observable<ApiResponse<Loan>> {
    return this.http.post<ApiResponse<Loan>>(
      `${environment.apiUrl}/approval/${id}/approve`,
      { note }
    );
  }

  rejectLoan(id: number, note: string): Observable<ApiResponse<Loan>> {
    return this.http.post<ApiResponse<Loan>>(
      `${environment.apiUrl}/approval/${id}/reject`,
      { note }
    );
  }

  returnLoan(id: number, note: string): Observable<ApiResponse<Loan>> {
    return this.http.post<ApiResponse<Loan>>(
      `${environment.apiUrl}/approval/${id}/return`,
      { note }
    );
  }
}
```

### 4.2 Pending Loans Component

```typescript
// features/approval/pending-list.component.ts
import { Component, OnInit, signal } from "@angular/core";
import { RouterLink } from "@angular/router";
import { ApprovalService, Loan } from "../../core/services/approval.service";
import { AuthService } from "../../core/services/auth.service";

@Component({
  selector: "app-pending-list",
  standalone: true,
  imports: [RouterLink],
  template: `
    <div class="page">
      <h1>Pending Approvals</h1>
      <p class="subtitle">{{ statusDescription() }}</p>

      @if (loading()) {
      <div class="loading">Loading...</div>
      } @if (error()) {
      <div class="error">{{ error() }}</div>
      }

      <table class="loan-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>Customer</th>
            <th>Amount</th>
            <th>Product</th>
            <th>Branch</th>
            <th>Status</th>
            <th>Date</th>
            <th>Action</th>
          </tr>
        </thead>
        <tbody>
          @for (loan of loans(); track loan.id) {
          <tr>
            <td>{{ loan.id }}</td>
            <td>
              <strong>{{ loan.customerName }}</strong
              ><br />
              <small>{{ loan.customerEmail }}</small>
            </td>
            <td>Rp {{ loan.requestedAmount | number }}</td>
            <td>{{ loan.product.name }}</td>
            <td>{{ loan.branch.location }}</td>
            <td>
              <span class="badge" [attr.data-status]="loan.status">{{
                loan.status
              }}</span>
            </td>
            <td>{{ loan.createdAt | date : "short" }}</td>
            <td>
              <a [routerLink]="['/approvals', loan.id]" class="btn-primary"
                >Review</a
              >
            </td>
          </tr>
          } @empty {
          <tr>
            <td colspan="8" class="empty">No pending loans</td>
          </tr>
          }
        </tbody>
      </table>
    </div>
  `,
})
export class PendingListComponent implements OnInit {
  loans = signal<Loan[]>([]);
  loading = signal(false);
  error = signal<string | null>(null);

  constructor(
    private approvalService: ApprovalService,
    private authService: AuthService
  ) {}

  statusDescription(): string {
    if (this.authService.isMarketing())
      return "Showing SUBMITTED loans for your branch";
    if (this.authService.isBranchManager())
      return "Showing MARKETING_APPROVED loans for your branch";
    if (this.authService.isBackoffice())
      return "Showing BRANCH_MANAGER_APPROVED loans from all branches";
    return "";
  }

  ngOnInit(): void {
    this.loadPendingLoans();
  }

  loadPendingLoans(): void {
    this.loading.set(true);
    this.error.set(null);

    this.approvalService.getPendingLoans().subscribe({
      next: (response) => {
        this.loading.set(false);
        this.loans.set(response.data || []);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err.error?.message || "Failed to load loans");
      },
    });
  }
}
```

### 4.3 Loan Detail & Approval Component

```typescript
// features/approval/loan-detail.component.ts
import { Component, OnInit, signal } from "@angular/core";
import { ActivatedRoute, Router } from "@angular/router";
import { FormsModule } from "@angular/forms";
import {
  ApprovalService,
  Loan,
  LoanHistory,
} from "../../core/services/approval.service";
import { AuthService } from "../../core/services/auth.service";

@Component({
  selector: "app-loan-detail",
  standalone: true,
  imports: [FormsModule],
  template: `
    <div class="page">
      @if (loan()) {
      <div class="loan-header">
        <h1>Loan #{{ loan()!.id }}</h1>
        <span class="status-badge" [attr.data-status]="loan()!.status">
          {{ loan()!.status }}
        </span>
      </div>

      <!-- Customer Info -->
      <section class="card">
        <h2>Customer Information</h2>
        <div class="grid">
          <div>
            <label>Name</label>
            <p>{{ loan()!.customerName }}</p>
          </div>
          <div>
            <label>Email</label>
            <p>{{ loan()!.customerEmail }}</p>
          </div>
          <div>
            <label>NIK</label>
            <p>{{ loan()!.customerNik }}</p>
          </div>
          <div>
            <label>Phone</label>
            <p>{{ loan()!.customerPhone }}</p>
          </div>
        </div>
      </section>

      <!-- Loan Details -->
      <section class="card">
        <h2>Loan Details</h2>
        <div class="grid">
          <div>
            <label>Product</label>
            <p>{{ loan()!.product.name }}</p>
          </div>
          <div>
            <label>Amount</label>
            <p>Rp {{ loan()!.requestedAmount | number }}</p>
          </div>
          <div>
            <label>Tenor</label>
            <p>{{ loan()!.requestedTenor }} months</p>
          </div>
          <div>
            <label>Interest Rate</label>
            <p>{{ loan()!.requestedRate }}%</p>
          </div>
          <div>
            <label>Branch</label>
            <p>{{ loan()!.branch.location }}</p>
          </div>
        </div>
      </section>

      <!-- History Timeline -->
      <section class="card">
        <h2>Approval History</h2>
        <div class="timeline">
          @for (item of history(); track item.id) {
          <div class="timeline-item" [attr.data-status]="item.status">
            <div class="timeline-dot"></div>
            <div class="timeline-content">
              <strong>{{ item.status | titlecase }}</strong>
              <p>{{ item.note }}</p>
              <small
                >{{ item.approvedBy }} ({{ item.approvedByRole }}) -
                {{ item.createdAt | date : "medium" }}</small
              >
            </div>
          </div>
          }
        </div>
      </section>

      <!-- Action Panel -->
      @if (canTakeAction()) {
      <section class="card action-panel">
        <h2>Take Action</h2>

        <div class="form-group">
          <label>Note</label>
          <textarea
            [(ngModel)]="note"
            rows="3"
            placeholder="Add a note..."
          ></textarea>
        </div>

        <div class="action-buttons">
          <button
            class="btn-success"
            (click)="approve()"
            [disabled]="processing()"
          >
            ✓ Approve
          </button>
          <button
            class="btn-danger"
            (click)="reject()"
            [disabled]="processing() || !note"
          >
            ✗ Reject
          </button>
          @if (authService.isBackoffice()) {
          <button
            class="btn-warning"
            (click)="returnLoan()"
            [disabled]="processing() || !note"
          >
            ↩ Return for Revision
          </button>
          }
        </div>
      </section>
      } }
    </div>
  `,
})
export class LoanDetailComponent implements OnInit {
  loan = signal<Loan | null>(null);
  history = signal<LoanHistory[]>([]);
  processing = signal(false);
  note = "";

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private approvalService: ApprovalService,
    public authService: AuthService
  ) {}

  ngOnInit(): void {
    const id = +this.route.snapshot.params["id"];
    this.loadLoan(id);
    this.loadHistory(id);
  }

  loadLoan(id: number): void {
    this.approvalService.getLoanDetail(id).subscribe({
      next: (response) => this.loan.set(response.data),
    });
  }

  loadHistory(id: number): void {
    this.approvalService.getLoanHistory(id).subscribe({
      next: (response) => this.history.set(response.data || []),
    });
  }

  canTakeAction(): boolean {
    const status = this.loan()?.status;
    if (this.authService.isMarketing() && status === "SUBMITTED") return true;
    if (this.authService.isBranchManager() && status === "MARKETING_APPROVED")
      return true;
    if (this.authService.isBackoffice() && status === "BRANCH_MANAGER_APPROVED")
      return true;
    return false;
  }

  approve(): void {
    this.processing.set(true);
    this.approvalService.approveLoan(this.loan()!.id, this.note).subscribe({
      next: () => {
        alert("Loan approved successfully");
        this.router.navigate(["/approvals"]);
      },
      error: (err) => {
        this.processing.set(false);
        alert(err.error?.message || "Failed to approve");
      },
    });
  }

  reject(): void {
    if (!this.note) {
      alert("Please add a note for rejection");
      return;
    }

    this.processing.set(true);
    this.approvalService.rejectLoan(this.loan()!.id, this.note).subscribe({
      next: () => {
        alert("Loan rejected");
        this.router.navigate(["/approvals"]);
      },
      error: (err) => {
        this.processing.set(false);
        alert(err.error?.message || "Failed to reject");
      },
    });
  }

  returnLoan(): void {
    if (!this.note) {
      alert("Please add a note for return");
      return;
    }

    this.processing.set(true);
    this.approvalService.returnLoan(this.loan()!.id, this.note).subscribe({
      next: () => {
        alert("Loan returned for revision");
        this.router.navigate(["/approvals"]);
      },
      error: (err) => {
        this.processing.set(false);
        alert(err.error?.message || "Failed to return");
      },
    });
  }
}
```

---

## 5. SuperAdmin Features

### 5.1 Admin Service

```typescript
// core/services/admin.service.ts
import { Injectable } from "@angular/core";
import { HttpClient } from "@angular/common/http";
import { Observable } from "rxjs";
import { environment } from "../../environments/environment";

export interface User {
  id: number;
  name: string;
  email: string;
  userType: string;
  isActive: boolean;
  roles: string[];
  branch?: { id: number; name: string };
}

export interface Role {
  id: number;
  name: string;
  permissions: Permission[];
}

export interface Permission {
  id: number;
  code: string;
  description: string;
}

export interface CreateUserRequest {
  name: string;
  email: string;
  password: string;
  roleId: number;
  branchId?: number;
}

@Injectable({ providedIn: "root" })
export class AdminService {
  constructor(private http: HttpClient) {}

  // Users
  getUsers(): Observable<ApiResponse<User[]>> {
    return this.http.get<ApiResponse<User[]>>(
      `${environment.apiUrl}/admin/users`
    );
  }

  createUser(request: CreateUserRequest): Observable<ApiResponse<User>> {
    return this.http.post<ApiResponse<User>>(
      `${environment.apiUrl}/admin/users`,
      request
    );
  }

  assignRole(userId: number, roleId: number): Observable<ApiResponse<User>> {
    return this.http.post<ApiResponse<User>>(
      `${environment.apiUrl}/admin/users/${userId}/roles`,
      { roleId }
    );
  }

  removeRole(userId: number, roleId: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(
      `${environment.apiUrl}/admin/users/${userId}/roles/${roleId}`
    );
  }

  // Roles
  getRoles(): Observable<ApiResponse<Role[]>> {
    return this.http.get<ApiResponse<Role[]>>(
      `${environment.apiUrl}/admin/roles`
    );
  }

  updateRolePermissions(
    roleId: number,
    permissionIds: number[]
  ): Observable<ApiResponse<Role>> {
    return this.http.put<ApiResponse<Role>>(
      `${environment.apiUrl}/admin/roles/${roleId}/permissions`,
      { permissionIds }
    );
  }

  // Permissions
  getPermissions(): Observable<ApiResponse<Permission[]>> {
    return this.http.get<ApiResponse<Permission[]>>(
      `${environment.apiUrl}/admin/permissions`
    );
  }
}
```

### 5.2 User Management Component

```typescript
// features/admin/user-list.component.ts
import { Component, OnInit, signal } from "@angular/core";
import { AdminService, User, Role } from "../../core/services/admin.service";
import { FormsModule } from "@angular/forms";

@Component({
  selector: "app-user-list",
  standalone: true,
  imports: [FormsModule],
  template: `
    <div class="page">
      <div class="header">
        <h1>User Management</h1>
        <button class="btn-primary" (click)="showCreateModal = true">
          + Create User
        </button>
      </div>

      <table class="data-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>Name</th>
            <th>Email</th>
            <th>Type</th>
            <th>Roles</th>
            <th>Branch</th>
            <th>Status</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          @for (user of users(); track user.id) {
          <tr>
            <td>{{ user.id }}</td>
            <td>{{ user.name }}</td>
            <td>{{ user.email }}</td>
            <td>{{ user.userType }}</td>
            <td>
              @for (role of user.roles; track role) {
              <span class="role-badge">{{ role }}</span>
              }
            </td>
            <td>{{ user.branch?.name || "-" }}</td>
            <td>
              <span [class]="user.isActive ? 'active' : 'inactive'">
                {{ user.isActive ? "Active" : "Inactive" }}
              </span>
            </td>
            <td>
              <button (click)="openRoleModal(user)">Manage Roles</button>
            </td>
          </tr>
          }
        </tbody>
      </table>
    </div>

    <!-- Create User Modal -->
    @if (showCreateModal) {
    <div class="modal-overlay" (click)="showCreateModal = false">
      <div class="modal" (click)="$event.stopPropagation()">
        <h2>Create Internal User</h2>

        <div class="form-group">
          <label>Name</label>
          <input [(ngModel)]="newUser.name" />
        </div>

        <div class="form-group">
          <label>Email</label>
          <input type="email" [(ngModel)]="newUser.email" />
        </div>

        <div class="form-group">
          <label>Password</label>
          <input type="password" [(ngModel)]="newUser.password" />
        </div>

        <div class="form-group">
          <label>Role</label>
          <select [(ngModel)]="newUser.roleId">
            @for (role of roles(); track role.id) { @if (role.name !==
            'CUSTOMER') {
            <option [value]="role.id">{{ role.name }}</option>
            } }
          </select>
        </div>

        <div class="form-group">
          <label>Branch</label>
          <select [(ngModel)]="newUser.branchId">
            <option [value]="null">None (Backoffice/SuperAdmin)</option>
            @for (branch of branches(); track branch.id) {
            <option [value]="branch.id">{{ branch.location }}</option>
            }
          </select>
        </div>

        <div class="modal-actions">
          <button (click)="showCreateModal = false">Cancel</button>
          <button class="btn-primary" (click)="createUser()">Create</button>
        </div>
      </div>
    </div>
    }
  `,
})
export class UserListComponent implements OnInit {
  users = signal<User[]>([]);
  roles = signal<Role[]>([]);
  branches = signal<any[]>([]);

  showCreateModal = false;
  newUser = {
    name: "",
    email: "",
    password: "",
    roleId: 0,
    branchId: null as number | null,
  };

  constructor(private adminService: AdminService) {}

  ngOnInit(): void {
    this.loadUsers();
    this.loadRoles();
    this.loadBranches();
  }

  loadUsers(): void {
    this.adminService.getUsers().subscribe({
      next: (response) => this.users.set(response.data || []),
    });
  }

  loadRoles(): void {
    this.adminService.getRoles().subscribe({
      next: (response) => this.roles.set(response.data || []),
    });
  }

  loadBranches(): void {
    // Load from branch service
  }

  createUser(): void {
    this.adminService.createUser(this.newUser).subscribe({
      next: () => {
        alert("User created successfully");
        this.showCreateModal = false;
        this.loadUsers();
      },
      error: (err) => {
        alert(err.error?.message || "Failed to create user");
      },
    });
  }

  openRoleModal(user: User): void {
    // Open modal to assign/remove roles
  }
}
```

---

## 6. Data Models

```typescript
// core/models/api.models.ts

// Generic API Response
export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  errors?: string[];
  timestamp: string;
}

// Auth
export interface LoginRequest {
  email: string;
  password: string;
}

export interface AuthResponse {
  token: string;
  tokenType: string;
  userId: number;
  email: string;
  name: string;
  roles: string[];
  permissions: string[];
}

// Loan
export interface Loan {
  id: number;
  customerName: string;
  customerEmail: string;
  customerNik: string;
  customerPhone: string;
  customerAddress: string;
  customerBirthdate: string;
  product: Product;
  branch: Branch;
  requestedAmount: number;
  requestedTenor: number;
  requestedRate: number;
  status: LoanStatus;
  createdAt: string;
  updatedAt?: string;
}

export type LoanStatus =
  | "SUBMITTED"
  | "MARKETING_APPROVED"
  | "MARKETING_REJECTED"
  | "BRANCH_MANAGER_APPROVED"
  | "BRANCH_MANAGER_REJECTED"
  | "APPROVED"
  | "REJECTED"
  | "RETURNED";

// Reference Data
export interface Product {
  id: number;
  name: string;
  amount: number;
  tenor: number;
  interestRate: number;
}

export interface Branch {
  id: number;
  code: string;
  location: string;
}

// User
export interface User {
  id: number;
  name: string;
  email: string;
  userType: "CUSTOMER" | "INTERNAL";
  isActive: boolean;
  roles: string[];
  branch?: Branch;
}

export interface Role {
  id: number;
  name: string;
  permissions: Permission[];
}

export interface Permission {
  id: number;
  code: string;
  description: string;
}
```

---

## 7. Error Handling

### Global Error Handler

```typescript
// core/services/error.handler.ts
import { ErrorHandler, Injectable } from "@angular/core";
import { HttpErrorResponse } from "@angular/common/http";

@Injectable()
export class GlobalErrorHandler implements ErrorHandler {
  handleError(error: Error | HttpErrorResponse): void {
    if (error instanceof HttpErrorResponse) {
      // Server error
      console.error("Server Error:", error.status, error.message);

      switch (error.status) {
        case 400:
          // Validation error - show field errors
          const errors = error.error?.errors || [error.error?.message];
          console.error("Validation Errors:", errors);
          break;
        case 401:
          // Unauthorized - handled by interceptor
          break;
        case 403:
          // Forbidden
          console.error("Access Denied:", error.error?.message);
          break;
        case 404:
          console.error("Not Found:", error.error?.message);
          break;
        default:
          console.error("Server Error:", error.error?.message);
      }
    } else {
      // Client error
      console.error("Client Error:", error);
    }
  }
}
```

---

## Role Permission Matrix

| Role           | Pending Status          | Branch Scope    | Can Return |
| -------------- | ----------------------- | --------------- | ---------- |
| MARKETING      | SUBMITTED               | Own branch only | ❌         |
| BRANCH_MANAGER | MARKETING_APPROVED      | Own branch only | ❌         |
| BACKOFFICE     | BRANCH_MANAGER_APPROVED | All branches    | ✅         |
| SUPERADMIN     | -                       | All             | -          |

---

## Test Accounts

| Email                  | Password      | Role                     |
| ---------------------- | ------------- | ------------------------ |
| marketing.jkt@loan.com | marketing123  | MARKETING (Jakarta)      |
| bm.jkt@loan.com        | bm123         | BRANCH_MANAGER (Jakarta) |
| backoffice@loan.com    | backoffice123 | BACKOFFICE               |
| admin@loan.com         | admin123      | SUPERADMIN               |

---

_Generated: 2025-12-24_
