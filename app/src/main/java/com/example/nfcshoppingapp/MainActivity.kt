package com.example.nfcshoppingapp

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.tech.Ndef
import android.nfc.NdefMessage
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.nfcshoppingapp.ui.theme.NFCShoppingAppTheme
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var nfcAdapter: NfcAdapter? = null
    private var navControllerState = mutableStateOf<NavController?>(null)
    private val navController get() = navControllerState.value

    // State to hold the cart items across navigation
    private val _cartItems = mutableStateListOf<CartItem>()
    val cartItems: SnapshotStateList<CartItem> = _cartItems // Expose as State

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val auth = FirebaseAuth.getInstance()
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        setContent {
            NFCShoppingAppTheme {
                val navController = rememberNavController()
                navControllerState.value = navController
                var isLoggedIn by remember { mutableStateOf(auth.currentUser != null) }

                DisposableEffect(auth) {
                    val listener = FirebaseAuth.AuthStateListener {
                        isLoggedIn = it.currentUser != null
                    }
                    auth.addAuthStateListener(listener)
                    onDispose { auth.removeAuthStateListener(listener) }
                }

                NavHost(
                    navController = navController,
                    startDestination = if (isLoggedIn) "scan" else "login"
                ) {
                    composable("login") {
                        LoginScreen(auth = auth) {
                            isLoggedIn = true
                            navController.navigate("scan") { popUpTo("login") { inclusive = true } }
                        }
                    }
                    composable("scan") { MainScreen(navController) }
                    composable("discounts") { DiscountsScreen(navController) }
                    composable("cart/{prodId}") { backStackEntry ->
                        val prodId = backStackEntry.arguments?.getString("prodId")
                        CartScreen(
                            navController = navController,
                            scannedProdId = prodId,
                            cartItems = cartItems,
                            onAddToCart = ::addToCart,
                            onRemoveFromCart = ::removeFromCart,
                            onUpdateCartItemQuantity = ::updateCartItemQuantity
                        )
                    }
                    composable("cart") {
                        CartScreen(
                            navController = navController,
                            scannedProdId = null,
                            cartItems = cartItems,
                            onAddToCart = ::addToCart,
                            onRemoveFromCart = ::removeFromCart,
                            onUpdateCartItemQuantity = ::updateCartItemQuantity
                        )
                    }
                    composable("profile") { ProfileScreen(navController) }
                }
            }
        }
    }

    private fun addToCart(item: CartItem) {
        val existingItemIndex = _cartItems.indexOfFirst { it.id == item.id }
        if (existingItemIndex != -1) {
            // If item exists, update quantity
            val existingItem = _cartItems[existingItemIndex]
            _cartItems[existingItemIndex] = existingItem.copy(quantity = existingItem.quantity + 1)
        } else {
            _cartItems.add(item)
        }
    }

    private fun removeFromCart(itemId: String) {
        _cartItems.removeAll { it.id == itemId }
    }

    private fun updateCartItemQuantity(itemId: String, newQuantity: Int) {
        val index = _cartItems.indexOfFirst { it.id == itemId }
        if (index != -1) {
            _cartItems[index] = _cartItems[index].copy(quantity = newQuantity)
        }
    }

    override fun onResume() {
        super.onResume()
        val intent = Intent(this, javaClass).addFlags(Intent.FLAG_RECEIVER_REPLACE_PENDING)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_MUTABLE)
        val filters = arrayOf(IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED))
        nfcAdapter?.enableForegroundDispatch(this, pendingIntent, filters, null)
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableForegroundDispatch(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        if (intent.action == NfcAdapter.ACTION_TAG_DISCOVERED) {
            val tag: Tag? = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
            val ndef = tag?.let { Ndef.get(it) }

            if (ndef != null) {
                try {
                    ndef.connect()
                    val message: NdefMessage? = ndef.ndefMessage
                    val payload = message?.records?.get(0)?.payload
                    val text = payload?.decodeToString()?.removePrefix("\u0002")
                    ndef.close()

                    text?.let { scannedText ->
                        if (scannedText.startsWith("enProdID: ")) {
                            val prodId = scannedText.substringAfter("enProdID: ").trim()
                            // Navigate to cart, CartScreen will handle fetching and adding
                            navController?.navigate("cart/$prodId")
                        } else {
                            Toast.makeText(this, "Scanned NFC: $scannedText", Toast.LENGTH_LONG).show()
                        }
                    } ?: run {
                        Toast.makeText(this, "Empty NFC tag", Toast.LENGTH_SHORT).show()
                    }

                } catch (e: Exception) {
                    Toast.makeText(this, "Error reading NFC tag", Toast.LENGTH_SHORT).show()
                }
            } else {
                val tagId = tag?.id?.joinToString("") { "%02X".format(it) }
                Toast.makeText(this, "Tag ID: $tagId", Toast.LENGTH_LONG).show()
            }
        }
    }
}