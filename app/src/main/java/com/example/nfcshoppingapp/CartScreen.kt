package com.example.nfcshoppingapp

import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Credentials
import org.json.JSONObject
import java.io.IOException
import java.text.NumberFormat
import java.util.Locale
import coil.compose.AsyncImage
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.ui.res.colorResource

data class CartItem(
    val id: String,
    val name: String,
    val price: Double,
    val imageUrl: String,
    var quantity: Int
)

data class Product(
    val ProdID: String = "",
    val imageUrl: String = "",
    val prodName: String = "",
    val prodPrice: String = ""
)

@Composable
fun CartScreen(
    navController: NavController,
    scannedProdId: String?,
    cartItems: List<CartItem>,
    onAddToCart: (CartItem) -> Unit,
    onRemoveFromCart: (String) -> Unit,
    onUpdateCartItemQuantity: (String, Int) -> Unit
) {
    val totalPrice by remember(cartItems) { derivedStateOf { cartItems.sumOf { it.price * it.quantity } } }
    var isLoading by remember { mutableStateOf(false) }
    var invoiceUrl by remember { mutableStateOf<String?>(null) }
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser
    val database = FirebaseDatabase.getInstance().getReference("products")
    val xenditApiKey = "xnd_development_1uXMSDdSggUde1fqKRRtOx8S2sbS3D339w1jO6JBFXng34puta8jkMZFjYIc1" // Replace with your actual key
    val client = OkHttpClient()
    val localIndonesia = Locale("in", "ID")
    val currencyFormat = NumberFormat.getCurrencyInstance(localIndonesia)

    val isProductInCart = remember(scannedProdId, cartItems) {
        scannedProdId?.let { id -> cartItems.any { it.id == id } } ?: false
    }

    LaunchedEffect(scannedProdId) {
        scannedProdId?.let { id ->
            if (!isProductInCart) {
                isLoading = true
                database.orderByChild("ProdID").equalTo(id)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            if (snapshot.exists()) {
                                for (childSnapshot in snapshot.children) {
                                    val product = childSnapshot.getValue(Product::class.java)
                                    product?.let {
                                        val priceDouble =
                                            it.prodPrice.replace(",", "").toDoubleOrNull() ?: 0.0
                                        val newItem = CartItem(
                                            id = it.ProdID,
                                            name = it.prodName,
                                            price = priceDouble,
                                            imageUrl = it.imageUrl,
                                            quantity = 1
                                        )
                                        onAddToCart(newItem)
                                    }
                                }
                            } else {
                                Log.d("CartScreen", "Product with ID $id not found")
                                Toast.makeText(
                                    navController.context,
                                    "Product not found",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            isLoading = false
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Log.e("CartScreen", "Firebase query failed: ${error.message}")
                            isLoading = false
                            Toast.makeText(
                                navController.context,
                                "Failed to fetch product",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    })
            }
        }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = colorResource(id = R.color.buttonbg))
        }
    } else if (invoiceUrl != null) {
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    webViewClient = WebViewClient()
                    settings.javaScriptEnabled = true
                    loadUrl(invoiceUrl!!)
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Cart",
                        )
                        Icon(
                            painter = painterResource(id = R.drawable.ic_cart),
                            contentDescription = "Cart",
                            modifier = Modifier
                                .size(28.dp)
                                .padding(start = 2.dp)
                        )
                    },
                    backgroundColor = (Color(0xFFF9F9F9)),
                    contentColor = colorResource(id = R.color.text)
                )
            },
            bottomBar = { BottomNavigationBar(navController, selectedTab = "Cart") },
            content = { innerPadding ->
                Column(
                    modifier = Modifier
                        .padding(innerPadding)
                        .padding(16.dp)
                        .fillMaxSize()
                        .background(if (cartItems.isEmpty()) Color.Transparent else Color(0xFFF9F9F9)),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (cartItems.isEmpty()) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_addcart),
                                contentDescription = "Cart Empty",
                                modifier = Modifier.size(120.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Cart Empty",
                                style = MaterialTheme.typography.subtitle1,
                                color = Color.Gray
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(cartItems, key = { it.id }) { item ->
                                CartItemCard(
                                    item = item,
                                    onQuantityChange = { newQty ->
                                        onUpdateCartItemQuantity(item.id, newQty)
                                    },
                                    onRemove = { onRemoveFromCart(item.id) },
                                    currencyFormat = currencyFormat
                                )
                            }
                        }

                        Divider(modifier = Modifier.padding(vertical = 10.dp))

                        Text(
                            "Total: ${currencyFormat.format(totalPrice)}",
                            style = MaterialTheme.typography.h6
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                isLoading = true
                                createInvoice(
                                    cartItems = cartItems,
                                    amount = totalPrice,
                                    userEmail = currentUser?.email,
                                    apiKey = xenditApiKey,
                                    client = client
                                ) { url ->
                                    isLoading = false
                                    if (url != null) {
                                        invoiceUrl = url
                                    } else {
                                        Log.e("Xendit", "Invoice creation failed")
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = colorResource(id = R.color.buttonbg),
                                contentColor = Color.White
                            ),
                            enabled = cartItems.isNotEmpty() && !isLoading
                        ) {
                            Text(
                                text = if (isLoading) "Processing..." else "CheckOut",
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        )
    }
}

@Composable
fun CartItemCard(
    item: CartItem,
    onQuantityChange: (Int) -> Unit,
    onRemove: () -> Unit,
    currencyFormat: NumberFormat
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        elevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(12.dp)) {
            AsyncImage(
                model = item.imageUrl,
                contentDescription = item.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(80.dp)
                    .padding(end = 12.dp)
            )

            Column(Modifier.weight(1f)) {
                Text(item.name, style = MaterialTheme.typography.subtitle1)
                Text(
                    currencyFormat.format(item.price),
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = {
                        if (item.quantity > 1) onQuantityChange(item.quantity - 1)
                    }) {
                        Icon(Icons.Default.Remove, contentDescription = "Decrease")
                    }
                    Text("${item.quantity}")
                    IconButton(onClick = {
                        onQuantityChange(item.quantity + 1)
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Increase")
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    IconButton(onClick = onRemove) {
                        Icon(Icons.Default.Delete, contentDescription = "Remove", tint = Color.Red)
                    }
                }
            }
        }
    }
}

private fun createInvoice(
    cartItems: List<CartItem>,
    amount: Double,
    userEmail: String?,
    apiKey: String,
    client: OkHttpClient,
    onResult: (String?) -> Unit
) {
    val url = "https://api.xendit.co/v2/invoices"
    val externalId = "nfc-shop-${System.currentTimeMillis()}"
    val description = cartItems.joinToString(separator = ", ") { "${it.name} x ${it.quantity}" }

    val jsonBody = JSONObject().apply {
        put("external_id", externalId)
        put("amount", amount)
        put("payer_email", userEmail)
        put("description", "Shopping Cart: $description")
    }

    val requestBody = jsonBody.toString()
        .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

    val credential = Credentials.basic(apiKey, "")
    val request = Request.Builder()
        .url(url)
        .header("Authorization", credential)
        .post(requestBody)
        .build()

    CoroutineScope(Dispatchers.IO).launch {
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("Xendit", "Network error: ${e.localizedMessage}")
                CoroutineScope(Dispatchers.Main).launch {
                    onResult(null)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                Log.d("Xendit", "Response: $body")

                val invoiceUrl = try {
                    JSONObject(body ?: "").optString("invoice_url", null)
                } catch (e: Exception) {
                    null
                }

                CoroutineScope(Dispatchers.Main).launch {
                    onResult(invoiceUrl)
                }
            }
        })
    }
}