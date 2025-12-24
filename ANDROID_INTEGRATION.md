# Android Integration Guide (Kotlin)

> Complete guide for integrating Loan Application Backend API with Android app

---

## Table of Contents

1. [Setup](#1-setup)
2. [Authentication](#2-authentication)
3. [Customer Features](#3-customer-features)
4. [Data Models](#4-data-models)
5. [Error Handling](#5-error-handling)
6. [Best Practices](#6-best-practices)

---

## 1. Setup

### 1.1 Dependencies (build.gradle.kts)

```kotlin
// Retrofit + OkHttp
implementation("com.squareup.retrofit2:retrofit:2.9.0")
implementation("com.squareup.retrofit2:converter-gson:2.9.0")
implementation("com.squareup.okhttp3:okhttp:4.12.0")
implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

// Coroutines
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

// Security (for token storage)
implementation("androidx.security:security-crypto:1.1.0-alpha06")
```

### 1.2 API Configuration

```kotlin
object ApiConfig {
    const val BASE_URL = "http://10.0.2.2:8080/" // Android emulator localhost
    // const val BASE_URL = "http://YOUR_SERVER_IP:8080/" // Physical device
}
```

### 1.3 Retrofit Setup

```kotlin
object RetrofitClient {

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val authInterceptor = Interceptor { chain ->
        val token = TokenManager.getToken()
        val request = if (token != null) {
            chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            chain.request()
        }
        chain.proceed(request)
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    val instance: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(ApiConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
```

### 1.4 Token Manager (Encrypted Storage)

```kotlin
object TokenManager {
    private const val PREFS_NAME = "secure_prefs"
    private const val KEY_TOKEN = "jwt_token"

    private lateinit var encryptedPrefs: SharedPreferences

    fun init(context: Context) {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        encryptedPrefs = EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun saveToken(token: String) {
        encryptedPrefs.edit().putString(KEY_TOKEN, token).apply()
    }

    fun getToken(): String? = encryptedPrefs.getString(KEY_TOKEN, null)

    fun clearToken() {
        encryptedPrefs.edit().remove(KEY_TOKEN).apply()
    }

    fun isLoggedIn(): Boolean = getToken() != null
}
```

---

## 2. Authentication

### 2.1 API Service Interface

```kotlin
interface ApiService {

    // ===== AUTH =====
    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<ApiResponse<AuthResponse>>

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<ApiResponse<AuthResponse>>

    @POST("api/auth/change-password")
    suspend fun changePassword(@Body request: ChangePasswordRequest): Response<ApiResponse<Unit>>

    // ===== PROFILE =====
    @GET("api/customer/profile")
    suspend fun getProfile(): Response<ApiResponse<UserResponse>>

    @PUT("api/customer/profile")
    suspend fun updateProfile(@Body request: UpdateProfileRequest): Response<ApiResponse<ProfileResponse>>

    // ===== PLAFOND =====
    @GET("api/customer/plafond")
    suspend fun getPlafond(): Response<ApiResponse<PlafondResponse>>

    @POST("api/customer/plafond")
    suspend fun selectPlafond(@Body request: SelectPlafondRequest): Response<ApiResponse<PlafondResponse>>

    // ===== LOANS =====
    @GET("api/loans")
    suspend fun getLoans(): Response<ApiResponse<List<LoanResponse>>>

    @GET("api/loans/{id}")
    suspend fun getLoan(@Path("id") id: Long): Response<ApiResponse<LoanResponse>>

    @POST("api/loans")
    suspend fun submitLoan(@Body request: SubmitLoanRequest): Response<ApiResponse<LoanResponse>>

    @GET("api/loans/{id}/history")
    suspend fun getLoanHistory(@Path("id") id: Long): Response<ApiResponse<List<LoanHistoryResponse>>>

    // ===== PUBLIC =====
    @GET("api/products")
    suspend fun getProducts(): Response<ApiResponse<List<ProductResponse>>>

    @GET("api/branches")
    suspend fun getBranches(): Response<ApiResponse<List<BranchResponse>>>
}
```

### 2.2 Auth Repository

```kotlin
class AuthRepository(private val api: ApiService) {

    suspend fun login(email: String, password: String): Result<AuthResponse> {
        return try {
            val response = api.login(LoginRequest(email, password))
            if (response.isSuccessful && response.body()?.success == true) {
                val authData = response.body()!!.data!!
                TokenManager.saveToken(authData.token)
                Result.success(authData)
            } else {
                val errorMsg = response.body()?.message ?: "Login failed"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun register(name: String, email: String, password: String): Result<AuthResponse> {
        return try {
            val response = api.register(RegisterRequest(name, email, password))
            if (response.isSuccessful && response.body()?.success == true) {
                val authData = response.body()!!.data!!
                TokenManager.saveToken(authData.token)
                Result.success(authData)
            } else {
                val errorMsg = response.body()?.message ?: "Registration failed"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() {
        TokenManager.clearToken()
    }
}
```

### 2.3 Login ViewModel

```kotlin
class LoginViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _loginState = MutableStateFlow<UiState<AuthResponse>>(UiState.Idle)
    val loginState: StateFlow<UiState<AuthResponse>> = _loginState.asStateFlow()

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _loginState.value = UiState.Loading

            authRepository.login(email, password)
                .onSuccess { _loginState.value = UiState.Success(it) }
                .onFailure { _loginState.value = UiState.Error(it.message ?: "Unknown error") }
        }
    }
}

sealed class UiState<out T> {
    object Idle : UiState<Nothing>()
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}
```

---

## 3. Customer Features

### 3.1 Customer Flow

```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│  Register   │ →  │  Complete   │ →  │   Select    │ →  │   Submit    │ →  │    Track    │
│   / Login   │    │   Profile   │    │   Plafond   │    │    Loan     │    │   Status    │
└─────────────┘    └─────────────┘    └─────────────┘    └─────────────┘    └─────────────┘
```

### 3.2 Profile Fragment

```kotlin
class ProfileFragment : Fragment() {

    private val viewModel: ProfileViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Observe profile state
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.profileState.collect { state ->
                when (state) {
                    is UiState.Loading -> showLoading()
                    is UiState.Success -> {
                        hideLoading()
                        displayProfile(state.data)

                        // Check if profile is complete
                        if (!state.data.profile.isComplete) {
                            showProfileIncompleteWarning()
                        }
                    }
                    is UiState.Error -> showError(state.message)
                    else -> {}
                }
            }
        }

        viewModel.loadProfile()
    }

    private fun displayProfile(user: UserResponse) {
        binding.apply {
            etName.setText(user.name)
            etEmail.setText(user.email)
            etNik.setText(user.profile.nik ?: "")
            etPhone.setText(user.profile.phoneNumber ?: "")
            etAddress.setText(user.profile.address ?: "")
            user.profile.birthdate?.let {
                etBirthdate.setText(it)
            }
        }
    }
}
```

### 3.3 Plafond Selection

```kotlin
class PlafondViewModel(
    private val api: ApiService
) : ViewModel() {

    private val _products = MutableStateFlow<List<ProductResponse>>(emptyList())
    val products: StateFlow<List<ProductResponse>> = _products.asStateFlow()

    private val _currentPlafond = MutableStateFlow<PlafondResponse?>(null)
    val currentPlafond: StateFlow<PlafondResponse?> = _currentPlafond.asStateFlow()

    private val _selectState = MutableStateFlow<UiState<PlafondResponse>>(UiState.Idle)
    val selectState: StateFlow<UiState<PlafondResponse>> = _selectState.asStateFlow()

    fun loadProducts() {
        viewModelScope.launch {
            try {
                val response = api.getProducts()
                if (response.isSuccessful) {
                    _products.value = response.body()?.data ?: emptyList()
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun loadCurrentPlafond() {
        viewModelScope.launch {
            try {
                val response = api.getPlafond()
                if (response.isSuccessful && response.body()?.success == true) {
                    _currentPlafond.value = response.body()?.data
                } else {
                    _currentPlafond.value = null // No plafond selected
                }
            } catch (e: Exception) {
                _currentPlafond.value = null
            }
        }
    }

    fun selectPlafond(productId: Long) {
        viewModelScope.launch {
            _selectState.value = UiState.Loading
            try {
                val response = api.selectPlafond(SelectPlafondRequest(productId))
                if (response.isSuccessful && response.body()?.success == true) {
                    _selectState.value = UiState.Success(response.body()!!.data!!)
                    _currentPlafond.value = response.body()?.data
                } else {
                    _selectState.value = UiState.Error(
                        response.body()?.message ?: "Failed to select plafond"
                    )
                }
            } catch (e: Exception) {
                _selectState.value = UiState.Error(e.message ?: "Network error")
            }
        }
    }
}
```

### 3.4 Plafond Card UI

```kotlin
@Composable
fun PlafondCard(plafond: PlafondResponse) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = plafond.product.name,
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Progress bar showing remaining amount
            val progress = plafond.remainingAmount.toFloat() / plafond.originalAmount.toFloat()
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = if (progress > 0.3f) Color.Green else Color.Red
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Remaining", style = MaterialTheme.typography.labelSmall)
                    Text(
                        "Rp ${NumberFormat.getNumberInstance().format(plafond.remainingAmount)}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Original", style = MaterialTheme.typography.labelSmall)
                    Text(
                        "Rp ${NumberFormat.getNumberInstance().format(plafond.originalAmount)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            if (!plafond.isActive) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Plafond depleted - Select a new one",
                    color = Color.Red,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
```

### 3.5 Loan Submission

```kotlin
class SubmitLoanViewModel(private val api: ApiService) : ViewModel() {

    private val _plafond = MutableStateFlow<PlafondResponse?>(null)
    val plafond: StateFlow<PlafondResponse?> = _plafond.asStateFlow()

    private val _submitState = MutableStateFlow<UiState<LoanResponse>>(UiState.Idle)
    val submitState: StateFlow<UiState<LoanResponse>> = _submitState.asStateFlow()

    // Validation
    fun validateAmount(amount: BigDecimal): String? {
        val remaining = _plafond.value?.remainingAmount ?: return "No plafond selected"
        return if (amount > remaining) {
            "Amount exceeds remaining plafond (Rp ${remaining})"
        } else null
    }

    fun validateTenor(tenor: Int): String? {
        val maxTenor = _plafond.value?.product?.tenor ?: return "No plafond selected"
        return if (tenor > maxTenor) {
            "Tenor exceeds limit ($maxTenor months)"
        } else null
    }

    fun validateRate(rate: BigDecimal): String? {
        val minRate = _plafond.value?.product?.interestRate ?: return "No plafond selected"
        return if (rate < minRate) {
            "Rate cannot be lower than $minRate%"
        } else null
    }

    fun submitLoan(branchId: Long, amount: BigDecimal, tenor: Int, rate: BigDecimal) {
        viewModelScope.launch {
            _submitState.value = UiState.Loading

            try {
                val response = api.submitLoan(
                    SubmitLoanRequest(branchId, amount, tenor, rate)
                )
                if (response.isSuccessful && response.body()?.success == true) {
                    _submitState.value = UiState.Success(response.body()!!.data!!)
                } else {
                    _submitState.value = UiState.Error(
                        response.body()?.message ?: "Failed to submit loan"
                    )
                }
            } catch (e: Exception) {
                _submitState.value = UiState.Error(e.message ?: "Network error")
            }
        }
    }
}
```

### 3.6 Loan Status Timeline

```kotlin
@Composable
fun LoanStatusTimeline(history: List<LoanHistoryResponse>) {
    LazyColumn {
        itemsIndexed(history) { index, item ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Timeline indicator
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(40.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .background(
                                color = getStatusColor(item.status),
                                shape = CircleShape
                            )
                    )
                    if (index < history.size - 1) {
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .height(40.dp)
                                .background(Color.Gray)
                        )
                    }
                }

                // Content
                Column(modifier = Modifier.padding(start = 8.dp)) {
                    Text(
                        text = item.status.replace("_", " "),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = item.note ?: "",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "By: ${item.approvedBy} (${item.approvedByRole})",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                    Text(
                        text = formatDate(item.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

fun getStatusColor(status: String): Color = when {
    status.contains("APPROVED") -> Color.Green
    status.contains("REJECTED") -> Color.Red
    status == "RETURNED" -> Color.Yellow
    else -> Color.Blue
}
```

---

## 4. Data Models

```kotlin
// ===== Request Models =====
data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class ChangePasswordRequest(
    val currentPassword: String,
    val newPassword: String,
    val confirmPassword: String
)

data class UpdateProfileRequest(
    val nik: String,
    val birthdate: String,  // yyyy-MM-dd
    val phoneNumber: String,
    val address: String
)

data class SelectPlafondRequest(
    val productId: Long
)

data class SubmitLoanRequest(
    val branchId: Long,
    val amount: BigDecimal,
    val tenor: Int,
    val interestRate: BigDecimal
)

// ===== Response Models =====
data class ApiResponse<T>(
    val success: Boolean,
    val message: String?,
    val data: T?,
    val errors: List<String>?,
    val timestamp: String
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

data class UserResponse(
    val id: Long,
    val name: String,
    val email: String,
    val userType: String,
    val isActive: Boolean,
    val roles: List<String>,
    val profile: ProfileResponse
)

data class ProfileResponse(
    val nik: String?,
    val birthdate: String?,
    val phoneNumber: String?,
    val address: String?,
    val isComplete: Boolean
)

data class ProductResponse(
    val id: Long,
    val name: String,
    val amount: BigDecimal,
    val tenor: Int,
    val interestRate: BigDecimal
)

data class BranchResponse(
    val id: Long,
    val code: String,
    val location: String
)

data class PlafondResponse(
    val id: Long,
    val product: ProductResponse,
    val originalAmount: BigDecimal,
    val remainingAmount: BigDecimal,
    val assignedAt: String,
    val isActive: Boolean
)

data class LoanResponse(
    val id: Long,
    val customerName: String,
    val customerEmail: String,
    val product: ProductResponse,
    val branch: BranchResponse,
    val requestedAmount: BigDecimal,
    val requestedTenor: Int,
    val requestedRate: BigDecimal,
    val status: String,
    val createdAt: String
)

data class LoanHistoryResponse(
    val id: Long,
    val status: String,
    val note: String?,
    val approvedBy: String,
    val approvedByRole: String,
    val approvedByBranchName: String?,
    val createdAt: String
)
```

---

## 5. Error Handling

```kotlin
sealed class ApiError {
    data class Validation(val errors: List<String>) : ApiError()
    data class Unauthorized(val message: String) : ApiError()
    data class Forbidden(val message: String) : ApiError()
    data class NotFound(val message: String) : ApiError()
    data class Business(val message: String) : ApiError()
    data class Network(val cause: Throwable) : ApiError()
    data class Unknown(val message: String) : ApiError()
}

fun <T> Response<ApiResponse<T>>.toResult(): Result<T> {
    return when {
        isSuccessful && body()?.success == true -> {
            Result.success(body()!!.data!!)
        }
        code() == 400 -> {
            val errors = body()?.errors ?: listOf(body()?.message ?: "Validation error")
            Result.failure(ValidationException(errors))
        }
        code() == 401 -> {
            TokenManager.clearToken() // Clear invalid token
            Result.failure(UnauthorizedException(body()?.message ?: "Unauthorized"))
        }
        code() == 403 -> {
            Result.failure(ForbiddenException(body()?.message ?: "Access denied"))
        }
        code() == 404 -> {
            Result.failure(NotFoundException(body()?.message ?: "Not found"))
        }
        else -> {
            Result.failure(Exception(body()?.message ?: "Unknown error"))
        }
    }
}

// Custom exceptions
class ValidationException(val errors: List<String>) : Exception(errors.joinToString(", "))
class UnauthorizedException(message: String) : Exception(message)
class ForbiddenException(message: String) : Exception(message)
class NotFoundException(message: String) : Exception(message)
```

---

## 6. Best Practices

### 6.1 Network State Handling

```kotlin
// Check network connectivity before API calls
fun Context.isNetworkAvailable(): Boolean {
    val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = connectivityManager.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
    return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
           capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
}
```

### 6.2 Session Management

```kotlin
// Auto-logout on 401
class AuthInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        val request = chain.request()
        val response = chain.proceed(request)

        if (response.code == 401) {
            // Token expired, trigger logout
            TokenManager.clearToken()
            // Navigate to login screen via broadcast or event bus
        }

        return response
    }
}
```

### 6.3 Offline Caching

```kotlin
// Room database for offline loan viewing
@Entity(tableName = "loans")
data class LoanEntity(
    @PrimaryKey val id: Long,
    val customerName: String,
    val amount: BigDecimal,
    val status: String,
    val createdAt: String,
    val syncedAt: Long = System.currentTimeMillis()
)

@Dao
interface LoanDao {
    @Query("SELECT * FROM loans ORDER BY createdAt DESC")
    fun getAllLoans(): Flow<List<LoanEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(loans: List<LoanEntity>)
}
```

---

## Loan Status Reference

| Status                    | Description                      | UI Color |
| ------------------------- | -------------------------------- | -------- |
| `SUBMITTED`               | Waiting for Marketing            | Blue     |
| `MARKETING_APPROVED`      | Waiting for Branch Manager       | Blue     |
| `BRANCH_MANAGER_APPROVED` | Waiting for Backoffice           | Blue     |
| `APPROVED`                | Loan approved, will be disbursed | Green    |
| `MARKETING_REJECTED`      | Rejected by Marketing            | Red      |
| `BRANCH_MANAGER_REJECTED` | Rejected by Branch Manager       | Red      |
| `REJECTED`                | Final rejection                  | Red      |
| `RETURNED`                | Returned for revision            | Yellow   |

---

_Generated: 2025-12-24_
