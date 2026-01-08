# File Upload Implementation Guide

This document explains the technical implementation of the multi-file upload feature for user profiles.

## 1. Overview

The feature allows users to upload specific identity documents (KTP, KK, NPWP) as part of their profile update.

- **Method**: `PUT`
- **Content-Type**: `multipart/form-data`
- **Storage**: Local filesystem (`uploads/` directory)
- **Database**: File paths stored directly in `user_profiles` table columns (`ktp_path`, `kk_path`, `npwp_path`).

## 2. Architecture & Code Flow

The implementation follows a layered architecture: `Controller` -> `Service` -> `Repository`.

### A. Entity: `UserProfile`

**File**: `src/main/java/com/gvn/binarbe/entity/UserProfile.java`

This entity stores personal information and paths to uploaded documents.

- `ktpPath`: Path to the stored KTP image.
- `kkPath`: Path to the stored KK image.
- `npwpPath`: Path to the stored NPWP image.

### B. Controller Layer

**File**: `src/main/java/com/gvn/binarbe/controller/CustomerController.java`

The `updateProfile` endpoint handles `multipart/form-data` with specific parts for each document.

```java
@PutMapping(value = "/api/customer/profile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
public ResponseEntity<ApiResponse<UserProfileResponse>> updateProfile(
    @AuthenticationPrincipal UserDetails userDetails,
    @RequestPart("data") @Valid UpdateProfileRequest request, // <--- JSON Data
    @RequestPart(value = "ktp", required = false) MultipartFile ktp, // <--- Individual File
    @RequestPart(value = "kk", required = false) MultipartFile kk,   // <--- Individual File
    @RequestPart(value = "npwp", required = false) MultipartFile npwp // <--- Individual File
) { ... }
```

- Uses `@RequestPart("data")` to map the JSON part of the request to the DTO.
- Uses named `@RequestPart` for each document type (`ktp`, `kk`, `npwp`).

### C. Service Layer

**File**: `src/main/java/com/gvn/binarbe/service/impl/CustomerServiceImpl.java`

The service handles the business logic:

1.  **Update Profile Data**: Updates text fields (phone, birthdate, etc.).
2.  **Process Files**:
    - Checks if each file (`ktp`, `kk`, `npwp`) is present.
    - Calls `fileStorageService.storeFile(file)` to save the file physically.
    - Updates the corresponding field in `UserProfile` (`setKtpPath`, etc.) with the generated filename.

**File**: `src/main/java/com/gvn/binarbe/service/impl/FileStorageServiceImpl.java`

- Generates a unique filename using `UUID` to prevent overwriting.
- `UUID.randomUUID().toString() + fileExtension`
- Copies the file input stream to the target location (`uploads/UUID.ext`).

### D. Security & File Access

**File**: `src/main/java/com/gvn/binarbe/controller/FileController.java`

- Endpoint: `GET /uploads/{filename}`
- **Security Check**:
  - **Staff Access**: Users with roles `MARKETING`, `BRANCH_MANAGER`, `BACKOFFICE`, or `SUPERADMIN` can access any file.
  - **Owner Access**: Customers can only access files linked to their own `UserProfile`. The system checks if the requested filename matches `ktpPath`, `kkPath`, or `npwpPath` of a profile owned by the authenticated user.

## 3. How to Test with Postman

To test this feature in Postman, follow these exact steps:

1.  **Create Request**:

    - Set Method to **PUT**.
    - Set URL to `http://localhost:8080/api/customer/profile`.

2.  **Authorization**:

    - Type: **Bearer Token**
    - Token: (Paste your Customer JWT Token)

3.  **Body Configuration**:

    - Select **Body** tab.
    - Choose **form-data**.

4.  **Add Fields**:

    | Key    | Type     | Value                                                   | Content-Type (Important!) |
    | :----- | :------- | :------------------------------------------------------ | :------------------------ |
    | `data` | **Text** | `{"nik": "1234567890123456", "phone": "08123...", ...}` | `application/json`        |
    | `ktp`  | **File** | (Select your KTP image file)                            |                           |
    | `kk`   | **File** | (Select your KK image file)                             |                           |
    | `npwp` | **File** | (Select your NPWP image file)                           |                           |

    > **IMPORTANT**: For the `data` key, you **MUST** manually set the Content-Type to `application/json`.

5.  **Send Request**:
    - Click **Send**.
    - You should receive a `200 OK` response with the updated profile and file URLs (e.g., `ktpUrl`, `kkUrl`, `npwpUrl`).

## 4. Key Takeaways

- We use `multipart/form-data` to send JSON + Binary Files.
- Specific named parameters (`ktp`, `kk`, `npwp`) are used instead of a generic list.
- Files are secured so only owners or staff can view them.
