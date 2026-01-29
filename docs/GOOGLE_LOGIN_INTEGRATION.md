# Google Login Integration Guide

This guide explains how to integrate Google Sign-In with the Binar-BE backend for Android applications.

## Overview

The backend now supports Google OAuth 2.0 authentication for Android clients. When a user signs in with Google on their Android device, the client sends the Google ID token to the backend, which verifies it and returns a JWT token for subsequent API calls.

## Backend Implementation

### New Endpoint

**POST** `/api/auth/google-login`

Authenticates a user using a Google ID token from the Android Google Sign-In SDK.

#### Request Body

```json
{
  "idToken": "eyJhbGciOiJSUzI1NiIsImtpZCI6IjE2M...",
  "fcmToken": "optional-fcm-token-for-push-notifications"
}
```

| Field      | Type   | Required | Description                                           |
| ---------- | ------ | -------- | ----------------------------------------------------- |
| `idToken`  | string | Yes      | Google ID token obtained from Google Sign-In SDK      |
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
    "email": "user@gmail.com",
    "name": "John Doe",
    "roles": ["CUSTOMER"],
    "permissions": ["APPLY_LOAN", "VIEW_OWN_LOANS"]
  }
}
```

**Error Responses:**

- `400 Bad Request` - Invalid request body or missing ID token
- `401 Unauthorized` - Invalid Google ID token
- `403 Forbidden` - Account is disabled

### How It Works

1. **Token Verification**: The backend verifies the Google ID token using Google's API client library
2. **User Lookup**: Checks if a user with the email exists in the database
3. **Auto-Registration**: If user doesn't exist, creates a new customer account automatically
4. **JWT Generation**: Generates a JWT token for the user (same as regular login)
5. **FCM Token Update**: Updates FCM token if provided for push notifications

### Configuration

Add your Google OAuth Client ID to the environment:

```bash
# .env file
GOOGLE_OAUTH_CLIENT_ID=your-android-client-id.apps.googleusercontent.com
```

Or set it directly in `application.properties`:

```properties
google.oauth.client-id=your-android-client-id.apps.googleusercontent.com
```

> **Important**: The Client ID must match the one configured in your Android app.

## Android Integration

### 1. Add Dependencies

In your `build.gradle` (app level):

```gradle
dependencies {
    // Google Sign-In
    implementation 'com.google.android.gms:play-services-auth:20.7.0'

    // For API calls to backend
    implementation 'com.squareup.retrofit2:retrofit:2.9.0'
    implementation 'com.squareup.retrofit2:converter-gson:2.9.0'
}
```

### 2. Configure Google Sign-In

```kotlin
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions

class LoginActivity : AppCompatActivity() {

    private lateinit var googleSignInClient: GoogleSignInClient
    private val RC_SIGN_IN = 9001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configure Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.server_client_id)) // Your OAuth Client ID
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
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account?.idToken

            // Send ID token to backend
            idToken?.let {
                loginWithBackend(it)
            }
        } catch (e: ApiException) {
            Log.e(TAG, "Google sign-in failed", e)
        }
    }
}
```

### 3. Send Token to Backend

```kotlin
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

// API Interface
interface AuthApiService {
    @POST("api/auth/google-login")
    suspend fun googleLogin(@Body request: GoogleLoginRequest): Response<ApiResponse<AuthResponse>>
}

data class GoogleLoginRequest(
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
private fun loginWithBackend(idToken: String) {
    val retrofit = Retrofit.Builder()
        .baseUrl("https://your-api-url.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val authService = retrofit.create(AuthApiService::class.java)

    lifecycleScope.launch {
        try {
            val fcmToken = getFcmToken() // Optional: Get FCM token
            val request = GoogleLoginRequest(idToken = idToken, fcmToken = fcmToken)

            val response = authService.googleLogin(request)

            if (response.isSuccessful) {
                val authData = response.body()?.data
                authData?.let {
                    // Save JWT token
                    saveAuthToken(it.token)
                    // Navigate to main screen
                    navigateToMainScreen()
                }
            } else {
                // Handle error
                val errorMsg = response.errorBody()?.string()
                Log.e(TAG, "Backend login failed: $errorMsg")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network error", e)
        }
    }
}
```

### 4. Add to strings.xml

```xml
<resources>
    <!-- Your OAuth 2.0 Client ID from Google Cloud Console -->
    <string name="server_client_id">your-android-client-id.apps.googleusercontent.com</string>
</resources>
```

## Security Considerations

1. **Token Verification**: The backend verifies the Google ID token signature and audience
2. **HTTPS Only**: Always use HTTPS in production to protect tokens in transit
3. **Token Expiry**: Google ID tokens expire after 1 hour; the backend JWT has its own expiry
4. **Client ID**: Keep your OAuth Client ID secure and don't expose it in public repositories

## Testing

### Using Postman

1. Get a Google ID token from your Android app (log it in debug mode)
2. Send POST request to `http://localhost:8080/api/auth/google-login`
3. Body (raw JSON):
   ```json
   {
     "idToken": "your-google-id-token"
   }
   ```

### Using cURL

```bash
curl -X POST http://localhost:8080/api/auth/google-login \
  -H "Content-Type: application/json" \
  -d '{
    "idToken": "your-google-id-token"
  }'
```

## Troubleshooting

### "Invalid Google ID token" Error

- Verify the `GOOGLE_OAUTH_CLIENT_ID` matches your Android app's client ID
- Ensure the ID token hasn't expired (valid for 1 hour)
- Check that the token was obtained from the same project in Google Cloud Console

### "Account is disabled" Error

- The user account exists but has been deactivated by an admin
- Contact your administrator to reactivate the account

### Network Errors

- Ensure your backend is running and accessible
- Check firewall rules if testing from different networks
- Verify the base URL in your Android app is correct

## Google Cloud Console Setup

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project or select existing one
3. Enable the **Google Sign-In API**:
   - Go to "APIs & Services" > "Library"
   - Search for "Google Sign-In"
   - Click "Enable"
4. Create OAuth 2.0 Credentials:
   - Go to "APIs & Services" > "Credentials"
   - Click "Create Credentials" > "OAuth client ID"
   - Select "Android" as application type
   - Enter your package name and SHA-1 certificate fingerprint
   - Save the Client ID
5. Add the Client ID to your `.env` file and Android app

## Additional Resources

- [Google Sign-In for Android Documentation](https://developers.google.com/identity/sign-in/android)
- [Google OAuth 2.0 for Mobile & Desktop Apps](https://developers.google.com/identity/protocols/oauth2/native-app)
- [Verify Google ID Tokens](https://developers.google.com/identity/sign-in/android/backend-auth)
