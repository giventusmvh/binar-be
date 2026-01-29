# Firebase Authentication Integration Guide

This guide explains how to integrate Firebase Authentication with the Binar-BE backend for Android applications.

## Overview

The backend uses Firebase Admin SDK to verify Firebase ID tokens from your Android app. Firebase Authentication handles multiple providers (Google, Email/Password, Facebook, etc.) and provides a unified token system.

## How It Works

1. User signs in on Android using Firebase Authentication (Google, Email/Password, etc.)
2. Android app gets a Firebase ID Token
3. App sends token to backend `POST /api/auth/google-login`
4. Backend verifies token using Firebase Admin SDK
5. If valid, backend creates/updates user and returns JWT token

## Backend Implementation

### Endpoint

**POST** `/api/auth/google-login`

Accepts a Firebase ID token and authenticates the user.

#### Request Body

```json
{
  "idToken": "firebase-id-token-from-android",
  "fcmToken": "optional-fcm-token-for-push-notifications"
}
```

| Field      | Type   | Required | Description                                           |
| ---------- | ------ | -------- | ----------------------------------------------------- |
| `idToken`  | string | Yes      | Firebase ID token from Firebase Auth SDK              |
| `fcmToken` | string | No       | Firebase Cloud Messaging token for push notifications |

#### Response

**Success (200 OK):**

```json
{
  "status": "success",
  "message": "Google login successful",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "userId": 123,
    "email": "user@example.com",
    "name": "John Doe",
    "roles": ["CUSTOMER"],
    "permissions": ["APPLY_LOAN", "VIEW_OWN_LOANS"]
  }
}
```

### Configuration

The backend uses the existing Firebase configuration. Ensure your `firebase-service-account.json` is properly configured:

```properties
# application.properties
firebase.credentials.path=${FIREBASE_CREDENTIALS_PATH:src/main/resources/firebase-service-account.json}
```

**Important**: The service account must have the following permissions:

- Firebase Admin SDK Administrator
- Service Account Token Creator

## Android Integration

### 1. Add Firebase Auth Dependency

In your `build.gradle` (app level):

```gradle
dependencies {
    // Firebase Authentication
    implementation 'com.google.firebase:firebase-auth-ktx:22.3.1'

    // For API calls to backend
    implementation 'com.squareup.retrofit2:retrofit:2.9.0'
    implementation 'com.squareup.retrofit2:converter-gson:2.9.0'

    // Coroutines
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3'
}
```

### 2. Initialize Firebase

In your `Application` class or MainActivity:

```kotlin
import com.google.firebase.FirebaseApp

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
    }
}
```

### 3. Sign In with Google using Firebase

```kotlin
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

class LoginActivity : AppCompatActivity() {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private val RC_SIGN_IN = 9001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        firebaseAuth = FirebaseAuth.getInstance()

        // Configure Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id)) // From google-services.json
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleSignInResult(task)
        }
    }

    private fun handleSignInResult(task: com.google.android.gms.tasks.Task<GoogleSignInAccount>) {
        lifecycleScope.launch {
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account?.idToken

                // Authenticate with Firebase
                idToken?.let {
                    val credential = GoogleAuthProvider.getCredential(it, null)
                    val authResult = firebaseAuth.signInWithCredential(credential).await()

                    // Get Firebase ID Token
                    val firebaseToken = authResult.user?.getIdToken(false)?.await()?.token

                    firebaseToken?.let { token ->
                        loginWithBackend(token)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Sign-in failed", e)
            }
        }
    }
}
```

### 4. Send Firebase Token to Backend

```kotlin
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

// API Interface
interface AuthApiService {
    @POST("api/auth/google-login")
    suspend fun firebaseLogin(@Body request: FirebaseLoginRequest): Response<ApiResponse<AuthResponse>>
}

data class FirebaseLoginRequest(
    val idToken: String,
    val fcmToken: String? = null
)

data class AuthResponse(
    val token: String,
    val tokenType: String,
    val userId: Long,
    val email: String,
    val name: String,
    val roles: List<String>,
    val permissions: List<String>
)

// Login function
private suspend fun loginWithBackend(firebaseToken: String) {
    val retrofit = Retrofit.Builder()
        .baseUrl("https://your-api-url.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val authService = retrofit.create(AuthApiService::class.java)

    try {
        val fcmToken = getFcmToken() // Optional: Get FCM token
        val request = FirebaseLoginRequest(idToken = firebaseToken, fcmToken = fcmToken)

        val response = authService.firebaseLogin(request)

        if (response.isSuccessful) {
            val authData = response.body()?.data
            authData?.let {
                // Save JWT token
                saveAuthToken(it.token)
                // Navigate to main screen
                navigateToMainScreen()
            }
        } else {
            val errorMsg = response.errorBody()?.string()
            Log.e(TAG, "Backend login failed: $errorMsg")
        }
    } catch (e: Exception) {
        Log.e(TAG, "Network error", e)
    }
}
```

### 5. Sign In with Email/Password using Firebase

```kotlin
private fun signInWithEmailPassword(email: String, password: String) {
    lifecycleScope.launch {
        try {
            val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()

            // Get Firebase ID Token
            val firebaseToken = authResult.user?.getIdToken(false)?.await()?.token

            firebaseToken?.let { token ->
                loginWithBackend(token)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Email sign-in failed", e)
            // Handle error (wrong password, user not found, etc.)
        }
    }
}
```

## Firebase Setup

### 1. Create Firebase Project

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Click "Add project" and follow the setup
3. Add your Android app to the project
4. Download `google-services.json` and place it in `app/` directory

### 2. Enable Authentication Providers

1. In Firebase Console, go to "Authentication" > "Sign-in method"
2. Enable providers you want to use:
   - **Google**: Toggle to enabled, configure support email
   - **Email/Password**: Toggle to enabled
   - Other providers as needed

### 3. Get Admin SDK Service Account

1. In Firebase Console, go to Project Settings > Service accounts
2. Click "Generate new private key"
3. Save the JSON file as `firebase-service-account.json`
4. Place it in `src/main/resources/`

## Security Considerations

1. **Token Verification**: Firebase Admin SDK automatically verifies token signature and expiry
2. **HTTPS Only**: Always use HTTPS in production
3. **Token Expiry**: Firebase ID tokens expire after 1 hour; refresh tokens on the client
4. **Service Account**: Keep your service account JSON secure and never commit it to version control

## Testing

### Using Postman

1. Get a Firebase ID token from your Android app:

   ```kotlin
   val user = FirebaseAuth.getInstance().currentUser
   val token = user?.getIdToken(false)?.await()?.token
   Log.d("TOKEN", token) // Copy this token
   ```

2. Send POST request to `http://localhost:8080/api/auth/google-login`
3. Body (raw JSON):
   ```json
   {
     "idToken": "your-firebase-id-token"
   }
   ```

### Using cURL

```bash
curl -X POST http://localhost:8080/api/auth/google-login \
  -H "Content-Type: application/json" \
  -d '{
    "idToken": "your-firebase-id-token"
  }'
```

## Troubleshooting

### "Invalid Firebase ID token" Error

- Verify your `firebase-service-account.json` is correct and valid
- Ensure the token hasn't expired (valid for 1 hour)
- Check that the Firebase project matches between Android app and backend

### "FirebaseApp not initialized" Error

- Ensure `firebase-service-account.json` exists at the configured path
- Check that the service account has proper permissions
- Verify the JSON file is valid and not corrupted

### "Account is disabled" Error

- The user account exists but has been deactivated by an admin
- Contact your administrator to reactivate the account

## Additional Resources

- [Firebase Authentication Documentation](https://firebase.google.com/docs/auth)
- [Firebase Admin SDK Setup](https://firebase.google.com/docs/admin/setup)
- [Verify ID Tokens using Firebase Admin SDK](https://firebase.google.com/docs/auth/admin/verify-id-tokens)
- [Firebase Android Auth Guide](https://firebase.google.com/docs/auth/android/start)
