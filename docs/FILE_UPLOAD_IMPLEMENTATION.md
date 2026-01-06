# File Upload Implementation Guide

This document explains the technical implementation of the multi-file upload feature for user profiles.

## 1. Overview

The feature allows users to upload multiple identity documents (e.g., KTP, KK) as part of their profile update.

- **Method**: `PUT`
- **Content-Type**: `multipart/form-data`
- **Storage**: Local filesystem (`uploads/` directory)
- **Database**: Metadata stored in `user_documents` table

## 2. Architecture & Code Flow

The implementation follows a layered architecture: `Controller` -> `Service` -> `Repository` & `FileStorage`.

### A. Entity: `UserDocument`

**File**: `src/main/java/com/gvn/binarbe/entity/UserDocument.java`

This entity stores metadata about the uploaded file, linking it to a `User`.

- `fileName`: Original name of the file.
- `filePath`: The generated unique name (UUID) stored on disk.
- `fileType`: MIME type (e.g., `image/jpeg`).

### B. Controller Layer

**File**: `src/main/java/com/gvn/binarbe/controller/CustomerController.java`

The `updateProfile` endpoint was modified to handle `multipart/form-data`.

```java
@PutMapping(value = "/api/customer/profile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
public ResponseEntity<ApiResponse<UserProfileResponse>> updateProfile(
    @AuthenticationPrincipal UserDetails userDetails,
    @RequestPart("data") @Valid UpdateProfileRequest request, // <--- JSON Data
    @RequestPart(value = "files", required = false) List<MultipartFile> files // <--- List of Files
) { ... }
```

- Uses `@RequestPart("data")` to map the JSON part of the request to the DTO.
- Uses `@RequestPart("files")` to accept a list of files.

### C. Service Layer

**File**: `src/main/java/com/gvn/binarbe/service/impl/CustomerServiceImpl.java`

The service handles the business logic:

1.  **Update Profile Data**: Updates text fields (phone, address, etc.) first.
2.  **Process Files**:
    - Iterates through the list of `files`.
    - Calls `fileStorageService.storeFile(file)` to save the file physically.
    - Creates a new `UserDocument` entity for each file.
    - Saves the entity to the database using `UserDocumentRepository`.

**File**: `src/main/java/com/gvn/binarbe/service/impl/FileStorageServiceImpl.java`

- Generates a unique filename using `UUID` to prevent overwriting.
- `UUID.randomUUID().toString() + fileExtension`
- Copies the file input stream to the target location (`uploads/UUID.ext`).

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

    | Key     | Type     | Value                                                   | Content-Type (Important!) |
    | :------ | :------- | :------------------------------------------------------ | :------------------------ |
    | `data`  | **Text** | `{"nik": "1234567890123456", "phone": "08123...", ...}` | `application/json`        |
    | `files` | **File** | (Select your image file, e.g., ktp.jpg)                 |                           |
    | `files` | **File** | (Select another image file, e.g., kk.jpg)               |                           |

    > **IMPORTANT**: For the `data` key, you **MUST** manually set the Content-Type to `application/json`.
    >
    > 1. Hover over the `data` row in Postman.
    > 2. Click the three dots `...` (or enable the "Content-Type" column if hidden).
    > 3. Enter `application/json` in the Content-Type column for that row.

5.  **Send Request**:
    - Click **Send**.
    - You should receive a `200 OK` response with the updated profile and list of documents.

## 4. Key Takeaways

- We use `multipart/form-data` because we are sending **complex data** (JSON + Binary Files) in a single request.
- Using `@RequestPart` is the standard Spring way to handle mixed content types.
- Files are stored physically on the server, while the database keeps a reference (path) to them.
