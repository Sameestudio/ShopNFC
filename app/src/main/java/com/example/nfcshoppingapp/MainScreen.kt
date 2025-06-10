package com.example.nfcshoppingapp

import android.content.Intent
import android.nfc.NfcAdapter
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.compose.ui.res.colorResource
import androidx.navigation.compose.rememberNavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(navController: NavController) {
    val context = LocalContext.current
    val nfcAdapter: NfcAdapter? = NfcAdapter.getDefaultAdapter(context)
    var nfcStatus by remember { mutableStateOf("") }
    // var manualProdId by remember { mutableStateOf("") } // State for manual ProdID input

    LaunchedEffect(Unit) {
        nfcStatus = when {
            nfcAdapter == null -> "Your device doesn't support NFC"
            !nfcAdapter.isEnabled -> "NFC is disabled. Please enable it in settings."
            else -> "NFC is available and enabled!"
        }
    }

    Scaffold(
        topBar = { AppBar() },
        bottomBar = { BottomNavigationBar(navController, selectedTab = "Scan") } // Pass navController
    ) { padding ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(colorResource(id = R.color.bg)),
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(220.dp)
                    .background(Color.LightGray.copy(alpha = 0.3f), shape = CircleShape)
                    .clickable {
                        if (nfcAdapter?.isEnabled == true) {
                            navController.navigate("cart/SW-001") // Simulate a scan for testing
                        }
                    }
            ) {
                Text("Scan", fontSize = 24.sp, color = Color.Gray)
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (nfcAdapter == null) {
                Text(text = "Your device doesn't support NFC", color = Color.Red, fontSize = 16.sp)
            } else if (!nfcAdapter.isEnabled) {
                Button(onClick = {
                    context.startActivity(Intent(Settings.ACTION_NFC_SETTINGS))
                }) {
                    Text(text = "Enable NFC")
                }
            }

//             Spacer(modifier = Modifier.height(20.dp))

             // Manual ProdID Input
//             OutlinedTextField(
//                 value = manualProdId,
//                 onValueChange = { manualProdId = it },
//                 label = { Text("Enter Product ID") },
//                 modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp)
//             )
//
//             Spacer(modifier = Modifier.height(16.dp))
//
//             Button(
//                 onClick = {
//                     if (manualProdId.isNotBlank()) {
//                         navController.navigate("cart/${manualProdId.trim()}")
//                     }
//                 },
//                 enabled = manualProdId.isNotBlank()
//             ) {
//                 Text("Go to Cart with ID")
//             }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBar() {
    TopAppBar(
        title = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 18.dp), // Adds slight right padding for centering effect
                horizontalArrangement = Arrangement.Center // Ensures proper centering
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = "NFC Logo",
                    modifier = Modifier.size(140.dp)
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = { /* Open drawer */ }) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Menu",
                    tint = Color.DarkGray // Make hamburger icon darker
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.White // Matches bottom bar color
        )
    )
}

@Composable
fun BottomNavigationBar(navController: NavController, selectedTab: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        BottomNavigationItem(navController, "Scan", R.drawable.ic_scan, "scan", selectedTab == "Scan")
        BottomNavigationItem(navController, "Discounts", R.drawable.ic_discounts, "discounts", selectedTab == "Discounts")
        BottomNavigationItem(navController, "Cart", R.drawable.ic_cart, "cart", selectedTab == "Cart")
        BottomNavigationItem(navController, "Profile", R.drawable.ic_user, "profile", selectedTab == "Profile")
    }
}

@Composable
fun BottomNavigationItem(navController: NavController, label: String, iconRes: Int, route: String, isSelected: Boolean) {
    Column(
        modifier = Modifier
            .padding(10.dp)
            .clickable { navController.navigate(route) }, // Navigate to the correct screen
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = iconRes),
            contentDescription = label,
            modifier = Modifier
                .size(24.dp)
                .background(if (isSelected) Color(0xFF00C853) else Color.Transparent, shape = CircleShape),
            contentScale = ContentScale.Fit
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = if (isSelected) Color(0xFF00C853) else Color.Gray
        )
    }
}