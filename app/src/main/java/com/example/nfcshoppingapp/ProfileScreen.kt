package com.example.nfcshoppingapp

import android.content.Intent
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import androidx.navigation.NavController
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.json.JSONArray
import org.json.JSONObject
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.saveable.rememberSaveable
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Data class to represent a transaction
data class Transaction(
    val productName: String = "",
    val purchaseDate: String = "",
    val price: Int = 0,
    val quantity: Int = 0
)

@Composable
fun ProfileScreen(navController: NavController) {
    val auth = FirebaseAuth.getInstance()
    val dbRef = FirebaseDatabase.getInstance().getReference("userDetails")

    val currentUser = auth.currentUser
    val uid = currentUser?.uid

    var username by remember { mutableStateOf<String?>(null) }
    var email by remember { mutableStateOf<String?>(null) }
    var profilePhotoUrl by remember { mutableStateOf<String?>(null) }
    var transactions by remember { mutableStateOf<List<Transaction>>(emptyList()) }
    var showTransactions by remember { mutableStateOf(false) } // New state variable
    var viewTransactionsClicked by remember { mutableStateOf(false) }  // Track button click state
    var changePasswordClicked by remember { mutableStateOf(false) }
    var customerServiceClicked by remember { mutableStateOf(false) }
    var showReceiptDialog by remember { mutableStateOf<Transaction?>(null) } // To show dialog

    // State variables for password change
    var currentPassword by rememberSaveable { mutableStateOf("") }
    var newPassword by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var passwordChangeMessage by rememberSaveable { mutableStateOf("") }
    var showCurrentPasswordVisibility by rememberSaveable { mutableStateOf(false) }
    var showNewPasswordVisibility by rememberSaveable { mutableStateOf(false) }
    var showConfirmPasswordVisibility by rememberSaveable { mutableStateOf(false) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Function to fetch user data
    suspend fun fetchUserData(uid: String?): Triple<String?, String?, String?> { // Return Triple
        return withContext(Dispatchers.IO) {
            if (uid == null) {
                Log.w("FirebaseDebug", "UID is null")
                return@withContext Triple("Not logged in", "Not logged in", null) // Return null for photo
            }

            try {
                val snapshot = dbRef.child(uid).get().await()
                if (snapshot.exists()) {
                    val userData = snapshot.value as? Map<String, Any?>
                    Log.d("FirebaseDebug", "userData: $userData")
                    val fetchedUsername = userData?.get("username") as? String ?: "N/A"
                    val fetchedEmail = userData?.get("email") as? String ?: "N/A"
                    val fetchedProfilePhotoUrl = userData?.get("profilePhotoUrl") as? String  // Get photo URL
                    Log.d(
                        "FirebaseDebug",
                        "Fetched username: $fetchedUsername, email: $fetchedEmail, profilePhotoUrl: $fetchedProfilePhotoUrl"
                    )
                    return@withContext Triple(fetchedUsername, fetchedEmail, fetchedProfilePhotoUrl) // Return also photo
                } else {
                    Log.w("FirebaseDebug", "User data not found for UID: $uid")
                    return@withContext Triple("N/A", "N/A", null)
                }
            } catch (e: Exception) {
                Log.e("FirebaseDebug", "Error fetching user data: ${e.message}", e)
                return@withContext Triple("Error", "Error", null)
            }
        }
    }

    // Fetch user profile data (but not transactions) initially
    LaunchedEffect(uid) {
        coroutineScope.launch {
            val (fetchedUsername, fetchedEmail, fetchedProfilePhotoUrl) = fetchUserData(uid) // Get all 3
            username = fetchedUsername
            email = fetchedEmail
            profilePhotoUrl = fetchedProfilePhotoUrl // Update the state
        }
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile", fontWeight = FontWeight.Bold) },
                backgroundColor = Color.White,
                contentColor = Color.Black
            )
        },
        bottomBar = {
            BottomNavigationBar(navController, selectedTab = "Profile")
        }
    ) { padding ->
        // Wrap the entire Column in a vertical Scrollable Column
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFFF9F9F9))
                .verticalScroll(rememberScrollState()), // Make the column scrollable
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val imagePainter = if (profilePhotoUrl.isNullOrEmpty()) {
                    painterResource(id = R.drawable.default_profile)
                } else {
                    rememberAsyncImagePainter(profilePhotoUrl)
                }
                Box(contentAlignment = Alignment.BottomCenter) {

                    Image(
                        painter = imagePainter,
                        contentDescription = "Profile Picture",
                        modifier = Modifier
                            .size(110.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(text = username ?: "Loading...", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = email ?: "Loading...", fontSize = 16.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "UID: ${uid ?: "N/A"}", fontSize = 12.sp, color = Color.Gray)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth()
            ) {
                ActionButton(
                    icon = Icons.Default.Lock,
                    label = "Change\nPassword",
                    onClick = {
                        changePasswordClicked = !changePasswordClicked;
                        viewTransactionsClicked = false;
                        customerServiceClicked = false;
                        showTransactions = false; //hide transaction
                        passwordChangeMessage = ""; // Reset message
                        currentPassword = "";
                        newPassword = "";
                        confirmPassword = "";
                    },
                    isSelected = changePasswordClicked
                )
                ActionButton(
                    icon = Icons.Default.Receipt,
                    label = "View\nTransactions",
                    onClick = {
                        viewTransactionsClicked = !viewTransactionsClicked;
                        changePasswordClicked = false;
                        customerServiceClicked = false;
                        if (viewTransactionsClicked) {
                            if (uid != null) {
                                coroutineScope.launch {
                                    transactions = fetchTransactions(uid)
                                }
                            }
                            showTransactions = true;
                        } else {
                            showTransactions = false;
                            transactions = emptyList(); // Clear the transaction list when hiding
                        }
                    },
                    isSelected = viewTransactionsClicked
                )
                ActionButton(
                    icon = Icons.Default.SupportAgent,
                    label = "Customer\nAssistance",
                    onClick = {
                        customerServiceClicked = !customerServiceClicked
                        changePasswordClicked = false;
                        viewTransactionsClicked = false;
                        showTransactions = false; //hide transactions
                    },
                    isSelected = customerServiceClicked
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Change Password Section
            if (changePasswordClicked) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 18.dp) // Reduced horizontal padding
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Change Password",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // Current Password Field
                    OutlinedTextField(
                        value = currentPassword,
                        onValueChange = { currentPassword = it },
                        label = { Text("Current Password") },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (showCurrentPasswordVisibility) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showCurrentPasswordVisibility = !showCurrentPasswordVisibility }) {
                                Icon(
                                    imageVector = if (showCurrentPasswordVisibility) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = if (showCurrentPasswordVisibility) "Hide password" else "Show password"
                                )
                            }
                        },
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = Color(0xFF66BB6A), // Green focus color
                            unfocusedBorderColor = Color.Gray, // Gray unfocused color
                            focusedLabelColor = Color.Black,  // Label color when focused
                            unfocusedLabelColor = Color.Gray    // Label color when unfocused
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // New Password Field
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("New Password") },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (showNewPasswordVisibility) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showNewPasswordVisibility = !showNewPasswordVisibility }) {
                                Icon(
                                    imageVector = if (showNewPasswordVisibility) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = if (showNewPasswordVisibility) "Hide password" else "Show password"
                                )
                            }
                        },
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = Color(0xFF66BB6A), // Green focus color
                            unfocusedBorderColor = Color.Gray, // Gray unfocused color
                            focusedLabelColor = Color.Black,  // Label color when focused
                            unfocusedLabelColor = Color.Gray    // Label color when unfocused
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Confirm New Password Field
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Confirm New Password") },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (showConfirmPasswordVisibility) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showConfirmPasswordVisibility = !showConfirmPasswordVisibility }) {
                                Icon(
                                    imageVector = if (showConfirmPasswordVisibility) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = if (showConfirmPasswordVisibility) "Hide password" else "Show password"
                                )
                            }
                        },
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = Color(0xFF66BB6A), // Green focus color
                            unfocusedBorderColor = Color.Gray, // Gray unfocused color
                            focusedLabelColor = Color.Black,  // Label color when focused
                            unfocusedLabelColor = Color.Gray    // Label color when unfocused
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp)) // Reduced space

                    Text(
                        text = passwordChangeMessage,
                        color = when {
                            passwordChangeMessage.startsWith("Success") -> Color.Green
                            passwordChangeMessage.startsWith("Error") -> Color.Red
                            else -> Color.Gray
                        },
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Button(
                        onClick = {
                            if (newPassword == confirmPassword && newPassword.length >= 6) {
                                val user = auth.currentUser
                                if (user != null) {
                                    user.updatePassword(newPassword)
                                        .addOnCompleteListener { task ->
                                            if (task.isSuccessful) {
                                                Log.d("PasswordChange", "Password updated successfully")
                                                passwordChangeMessage = "Success: Password changed successfully!"
                                                currentPassword = ""
                                                newPassword = ""
                                                confirmPassword = ""
                                            } else {
                                                Log.e("PasswordChange", "Error updating password", task.exception)
                                                passwordChangeMessage = "Error: ${task.exception?.message}"
                                            }
                                        }
                                } else {
                                    passwordChangeMessage = "Error: No user logged in."
                                }
                            } else if (newPassword.length < 6) {
                                passwordChangeMessage = "Error: New password must be at least 6 characters."
                            } else {
                                passwordChangeMessage = "Error: New passwords do not match."
                            }
                        },
                        modifier = Modifier
                            .wrapContentWidth() // Change to wrapContentWidth()
                            .height(50.dp), // Make height same as logout button
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF66BB6A)),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text("Change Password", color = Color.White, fontSize = 16.sp) //same font size as logout
                    }
                }
            }

            // Transaction History Headline - Conditional Visibility
            if (showTransactions) {
                Text(
                    text = "Transaction History",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (transactions.isEmpty() && showTransactions) {
                Text("No transactions yet.", fontSize = 14.sp, color = Color.Gray)
            } else if (showTransactions) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                ) {
                    transactions.forEach { transaction ->
                        TransactionItem(
                            date = transaction.purchaseDate,
                            productName = transaction.productName,
                            quantity = transaction.quantity,
                            price = "Rs. ${transaction.price}",
                            onViewReceipt = { showReceiptDialog = transaction } // Set dialog content
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    auth.signOut()
                    val intent = Intent(context, LoginActivity::class.java)
                    context.startActivity(intent)
                    if (context is ComponentActivity) context.finish()
                },
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF66BB6A)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .height(50.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("Logout", color = Color.White, fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Show dialog if showReceiptDialog is not null
    if (showReceiptDialog != null) {
        ReceiptDialog(
            transaction = showReceiptDialog!!,
            onDismiss = { showReceiptDialog = null }
        )
    }
}

@Composable
fun ActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit = {},
    isSelected: Boolean = false
) {
    val textColor = if (isSelected) Color.White else Color.Gray
    val iconColor = if (isSelected) Color.White else Color.Gray

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.background( // Apply background color conditionally
                color = if (isSelected) Color(0xFF66BB6A) else Color.Transparent,
                shape = CircleShape
            )
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconColor, // Use the determined iconColor
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            color = if (isSelected) Color(0xFF66BB6A) else textColor
        )
    }
}

@Composable
fun TransactionItem(
    date: String,
    productName: String,
    quantity: Int,
    price: String,
    onViewReceipt: () -> Unit // Add this lambda parameter
) {
    Card(
        backgroundColor = Color.White,
        elevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = date, fontSize = 12.sp, color = Color.Gray)
                Text(text = productName, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = "View Receipt",
                    color = Color.Blue,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 2.dp)
                        .clickable { onViewReceipt() } // Make the text clickable
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(text = "Qty: $quantity", fontSize = 12.sp, color = Color.Gray)
                Text(text = price, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// Function to fetch transaction history from Firebase
suspend fun fetchTransactions(uid: String): List<Transaction> = withContext(Dispatchers.IO) {
    val dbRef = FirebaseDatabase.getInstance().getReference("userDetails")
    try {
        val snapshot = dbRef.child(uid).get().await()
        if (snapshot.exists()) {
            val userData = snapshot.value as? Map<String, Any?>
            val transactionData = userData?.get("transactionHistory")
            Log.d("FirebaseDebug", "transactionData (before processing): $transactionData")

            return@withContext when (transactionData) {
                is List<*> -> {
                    Log.d("FirebaseDebug", "transactionData is a List")
                    transactionData.mapNotNull { item ->
                        if (item is Map<*, *>) {
                            val transaction = Transaction(
                                productName = item["productName"] as? String ?: "",
                                purchaseDate = item["purchaseDate"] as? String ?: "",
                                price = (item["price"] as? Number)?.toInt() ?: (item["price"] as? String)?.toIntOrNull() ?: 0,
                                quantity = (item["quantity"] as? Number)?.toInt() ?: (item["quantity"] as? String)?.toIntOrNull() ?: 0
                            )
                            Log.d("FirebaseDebug", "Processed transaction (from List): $transaction")
                            transaction
                        } else {
                            Log.w("FirebaseDebug", "Skipping invalid transaction item (array): $item")
                            null
                        }
                    }
                }
                is Map<*, *> -> {
                    Log.d("FirebaseDebug", "transactionData is a Map")
                    (transactionData as Map<String, Map<String, Any>>).values.mapNotNull { transaction ->
                        val processedTransaction = Transaction(
                            productName = transaction["productName"] as? String ?: "",
                            purchaseDate = transaction["purchaseDate"] as? String ?: "",
                            price = (transaction["price"] as? Number)?.toInt() ?: (transaction["price"] as? String)?.toIntOrNull() ?: 0,
                            quantity = (transaction["quantity"] as? Number)?.toInt() ?: (transaction["quantity"] as? String)?.toIntOrNull() ?: 0
                        )
                        Log.d("FirebaseDebug", "Processed transaction (from Map): $processedTransaction")
                        processedTransaction
                    }
                }
                else -> {
                    Log.w("FirebaseDebug", "transactionHistory is neither a List nor a Map")
                    emptyList<Transaction>().also { Log.d("FirebaseDebug", "transactions set to emptyList()") }
                }
            }
        } else {
            return@withContext emptyList()
        }
    } catch (e: Exception) {
        Log.e("FirebaseDebug", "Error fetching transaction data: ${e.message}", e)
        return@withContext emptyList()
    }
}

@Composable
fun ReceiptDialog(transaction: Transaction, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface( // Use Surface for the dialog background
            modifier = Modifier.wrapContentSize(),
            shape = RoundedCornerShape(16.dp), // Rounded corners for the paper
            color = Color.White, // Background color
            contentColor = Color.Black,
            elevation = 8.dp // Add elevation for a shadow
        ) {
            Column(
                modifier = Modifier.padding(16.dp).width(300.dp), //width
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Receipt",
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp, // Increased font size
                    fontFamily = FontFamily.Serif, // Use a serif font for "Receipt"
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Divider(color = Color.LightGray, thickness = 1.dp)

                // Address and Tel
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Address: 1234 Lorem Ipsum, Dolor",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Tel: 123-456-7890",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Divider(color = Color.LightGray, thickness = 1.dp)

                // Date and Time
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Date: ${transaction.purchaseDate}",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(text = "10:35", fontSize = 12.sp, color = Color.Gray, fontFamily = FontFamily.Monospace) //hardcoded time
                }
                Spacer(modifier = Modifier.height(8.dp))

                // Product Items
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Product",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Price",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Divider(color = Color.LightGray, thickness = 1.dp)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = transaction.productName, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    Text(text = "Rs. ${transaction.price}", fontSize = 12.sp, fontFamily= FontFamily.Monospace)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Quantity", fontSize = 12.sp, color = Color.Gray, fontFamily = FontFamily.Monospace)
                    Text(text = "${transaction.quantity}", fontSize = 12.sp, color = Color.Gray, fontFamily = FontFamily.Monospace)
                }

                Spacer(modifier = Modifier.height(8.dp))
                Divider(color = Color.LightGray, thickness = 1.dp)

                // Amount
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "AMOUNT", fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Text(text = "Rs. ${transaction.price * transaction.quantity}", fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace) //calculate
                }

                Spacer(modifier = Modifier.height(8.dp))
                Divider(color = Color.LightGray, thickness = 1.dp)

                // Subtotal, Sales Tax, Balance (Hardcoded for demo)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Sub-total", fontSize = 12.sp, color = Color.Gray, fontFamily = FontFamily.Monospace)
                    Text(text = "Rs. ${transaction.price * transaction.quantity}", fontSize = 12.sp, color = Color.Gray, fontFamily = FontFamily.Monospace) //subtotal
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Sales Tax", fontSize = 12.sp, color = Color.Gray, fontFamily = FontFamily.Monospace)
                    Text(text = "Rs. 0.00", fontSize = 12.sp, color = Color.Gray, fontFamily = FontFamily.Monospace) //hardcoded
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Balance", fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Text(text = "Rs. ${transaction.price * transaction.quantity}", fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace) //balance
                }

                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = Color.Black, thickness = 2.dp) // Barcode

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.padding(top = 16.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF66BB6A)),
                ) {
                    Text("Close", color = Color.White, fontSize = 16.sp)
                }
            }
        }
    }
}
