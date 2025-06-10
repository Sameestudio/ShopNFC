package com.example.nfcshoppingapp

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val auth = FirebaseAuth.getInstance()

        setContent {
            LoginScreen(auth = auth) {
                // Navigate to MainActivity on successful login
                val intent = Intent(this@LoginActivity, MainActivity::class.java)
                startActivity(intent)
                finish()
            }
        }
    }
}

@Composable
fun LoginScreen(auth: FirebaseAuth, onLoginSuccess: () -> Unit) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    var isRegistering by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val db = FirebaseDatabase.getInstance()
    val userRef = db.getReference("userDetails")
    var registrationError by rememberSaveable { mutableStateOf<String?>(null) }
    var loginError by rememberSaveable { mutableStateOf<String?>(null) }
    var registrationSuccess by rememberSaveable { mutableStateOf(false) } // Added state for success message
    val coroutineScope = rememberCoroutineScope()

    // Google Sign-In
    val oneTapSignInClient = remember {
        Identity.getSignInClient(context)
    }
    val signInRequest = remember {
        BeginSignInRequest.builder()
            .setGoogleIdTokenRequestOptions(
                com.google.android.gms.auth.api.identity.BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    // Your Web client ID from Firebase console (Settings -> General -> Web API key)
                    .setServerClientId(context.getString(R.string.default_web_client_id))
                    .setFilterByAuthorizedAccounts(false)
                    .build()
            )
            .setAutoSelectEnabled(true) // Optional: Auto-select the signed-in account if only one
            .build()
    }

    val signInLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val credential = oneTapSignInClient.getSignInCredentialFromIntent(result.data)
            credential.googleIdToken?.let { idToken ->
                coroutineScope.launch(Dispatchers.IO) {
                    try {
                        val googleCredential = GoogleAuthProvider.getCredential(idToken, null)
                        val authResult = auth.signInWithCredential(googleCredential).await()
                        val firebaseUser = authResult.user
                        firebaseUser?.let {
                            // Check if user exists in your database, if not, add them
                            val userSnapshot = db.getReference("userDetails").child(it.uid).get().await()
                            if (!userSnapshot.exists()) {
                                val userRef = db.getReference("userDetails").child(it.uid)
                                userRef.child("uid").setValue(it.uid).await()
                                userRef.child("username").setValue(it.displayName ?: "").await()
                                userRef.child("email").setValue(it.email ?: "").await()
                                userRef.child("profilePhotoUrl").setValue(it.photoUrl?.toString() ?: "").await()
                                userRef.child("transactionHistory").setValue(emptyList<String>()).await()
                            }
                            withContext(Dispatchers.Main) {
                                onLoginSuccess()
                            }
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Log.e("GoogleSignIn", "Google sign-in failed: ${e.message}")
                            Toast.makeText(context, "Google sign-in failed: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } ?: run {
                Log.e("GoogleSignIn", "Google ID token is null.")
                Toast.makeText(context, "Google sign-in failed: Could not retrieve ID token.", Toast.LENGTH_SHORT).show()
            }
        } else {
            Log.e("GoogleSignIn", "Google sign-in flow cancelled.")
            Toast.makeText(context, "Google sign-in cancelled.", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.logo),
            contentDescription = "App Logo",
            modifier = Modifier
                .size(200.dp)
                .padding(bottom = if (isRegistering) 0.dp else 0.dp)
        )

        Text(
            text = if (isRegistering) "Register" else "Login",
            style = MaterialTheme.typography.headlineMedium,
            fontSize = 18.sp,
            modifier = Modifier.padding(top = if (isRegistering) 2.dp else 2.dp)
        )

        if (registrationError != null || loginError != null) {
            Text(
                text = registrationError ?: loginError ?: "",
                style = TextStyle(color = Color.Red, fontSize = 12.sp),
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        if (registrationSuccess) { // Show success message
            Text(
                text = "Successfully Registered!",
                style = TextStyle(color = Color.Green, fontSize = 16.sp),
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(1.dp))

        if (!isRegistering) {
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email", color = Color.Gray) },
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(vertical = 4.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedIndicatorColor = Color(0xFF66BB6A),
                    unfocusedIndicatorColor = Color.Gray,
                    cursorColor = Color.Black,
                    focusedTextColor = Color.Black
                )
            )

            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password", color = Color.Gray) },
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(vertical = 4.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                visualTransformation = PasswordVisualTransformation(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedIndicatorColor = Color.Gray,
                    cursorColor = Color.Black,
                    focusedTextColor = Color.Black
                )
            )
        } else {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name", color = Color.Gray) },
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(vertical = 4.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedIndicatorColor = Color.Gray,
                    cursorColor = Color.Black,
                    focusedTextColor = Color.Black
                )
            )

            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email", color = Color.Gray) },
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(vertical = 4.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedIndicatorColor = Color.Gray,
                    cursorColor = Color.Black,
                    focusedTextColor = Color.Black
                )
            )

            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password", color = Color.Gray) },
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(vertical = 4.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                visualTransformation = PasswordVisualTransformation(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedIndicatorColor = Color.Gray,
                    cursorColor = Color.Black,
                    focusedTextColor = Color.Black
                )
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                if (isRegistering) {
                    // Handle sign-up
                    if (name.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty()) {
                        coroutineScope.launch(Dispatchers.IO) {
                            try {
                                // Create user in Firebase Authentication
                                val authResult = auth.createUserWithEmailAndPassword(email, password).await()
                                val user = authResult.user

                                //If Authentication was successful, add user details to Realtime Database
                                user?.let {
                                    val uid = it.uid
                                    val userData = mapOf(
                                        "uid" to uid,
                                        "username" to name,
                                        "email" to email,
                                        "profilePhotoUrl" to "",
                                        "transactionHistory" to emptyList<String>()
                                    )
                                    userRef.child(uid).setValue(userData).await()

                                    withContext(Dispatchers.Main) {
                                        registrationError = null
                                        loginError = null
                                        registrationSuccess = true
                                        isRegistering = false
                                        name = ""
                                        email = ""
                                        password = ""
                                        Toast.makeText(context, "Successfully Registered!", Toast.LENGTH_SHORT).show()
                                    }
                                } ?: run {
                                    withContext(Dispatchers.Main) {
                                        registrationError = "Failed to create user."
                                        name = ""
                                        email = ""
                                        password = ""
                                        registrationSuccess = false
                                    }
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    val errorMessage = when (e) {
                                        is com.google.firebase.auth.FirebaseAuthException -> {
                                            when (e.errorCode) {
                                                "ERROR_EMAIL_ALREADY_IN_USE" -> "Email is already in use."
                                                "ERROR_INVALID_EMAIL" -> "Invalid email address."
                                                "ERROR_WEAK_PASSWORD" -> "Password should be at least 6 characters."
                                                else -> "Registration Failed: ${e.message}"
                                            }
                                        }
                                        else -> "Registration Failed: ${e.message}"
                                    }
                                    Log.e("LoginScreen", "Error: ${e.message}")
                                    registrationError = errorMessage
                                    name = ""
                                    email = ""
                                    password = ""
                                    registrationSuccess = false
                                }
                            }
                        }
                    } else {
                        registrationError = "Please enter all fields"
                        registrationSuccess = false
                    }
                } else {
                    // Handle login with email/password
                    if (email.isNotEmpty() && password.isNotEmpty()) {
                        coroutineScope.launch(Dispatchers.IO) {
                            try {
                                val authResult = auth.signInWithEmailAndPassword(email, password).await()
                                val user = authResult.user
                                if (user != null) {
                                    withContext(Dispatchers.Main) {
                                        onLoginSuccess()
                                        loginError = null
                                    }
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    val errorMessage = when (e) {
                                        is com.google.firebase.auth.FirebaseAuthException -> {
                                            when (e.errorCode) {
                                                "ERROR_USER_NOT_FOUND" -> "User not found."
                                                "ERROR_WRONG_PASSWORD" -> "Incorrect password."
                                                else -> "Login Failed: ${e.message}"
                                            }
                                        }
                                        else -> "Login Failed: ${e.message}"
                                    }
                                    Log.e("LoginScreen", "Error: ${e.message}")
                                    loginError = errorMessage
                                    email = ""
                                    password = ""
                                }
                            }
                        }
                    } else {
                        loginError = "Please enter email and password"
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(48.dp)
                .padding(vertical = 4.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF66BB6A),
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(if (isRegistering) "Register" else "Login with Email", fontSize = 14.sp)
        }

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = {
                coroutineScope.launch {
                    try {
                        val result = oneTapSignInClient.beginSignIn(signInRequest).await()
                        val intentSenderRequest = IntentSenderRequest.Builder(result.pendingIntent.intentSender).build()
                        signInLauncher.launch(intentSenderRequest)
                    } catch (e: ApiException) {
                        Log.e("GoogleSignIn", "Google Sign-In failed", e)
                        Toast.makeText(context, "Google Sign-In failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = Color.Black
            ),
            shape = RoundedCornerShape(8.dp),
            elevation = ButtonDefaults.elevatedButtonElevation(defaultElevation = 4.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.google),
                contentDescription = "Google Logo",
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (isRegistering) "Sign up with Google" else "Sign in with Google", fontSize = 14.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (isRegistering) "Already have an account? Login" else "Don't have an account? Register",
            style = TextStyle(
                color = Color.Blue,
                textDecoration = TextDecoration.Underline
            ),
            modifier = Modifier.clickable {
                isRegistering = !isRegistering
                name = ""
                email = ""
                password = ""
                registrationError = null
                loginError = null
                registrationSuccess = false // Clear success message when switching
            },
            fontSize = 14.sp
        )
        if (registrationError != null) {
            Text(
                text = registrationError!!,
                style = TextStyle(color = Color.Red),
                fontSize = 12.sp
            )
        }
        if (loginError != null) {
            Text(
                text = loginError!!,
                style = TextStyle(color = Color.Red),
                fontSize = 12.sp
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewLoginScreen() {
    LoginScreen(auth = FirebaseAuth.getInstance(), onLoginSuccess = {})
}