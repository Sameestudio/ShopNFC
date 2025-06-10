package com.example.nfcshoppingapp

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter

@Composable
fun DiscountsScreen(
    navController: NavController,
    viewModel: DiscountsViewModel = hiltViewModel(),
    selectedTab: Int = 0
) {
    val discounts by viewModel.discounts.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        bottomBar = { BottomNavigationBar(navController, selectedTab = "Discounts") },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(colorResource(id = R.color.white))
        ) {
            // Top Title Row with Centered Title and Icon
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Absolute.Left
            ) {
                Text(
                    text = "Discounts",
                    style = MaterialTheme.typography.titleLarge,
                    color = colorResource(id = R.color.text)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Icon(
                    painter = painterResource(id = R.drawable.ic_discounts),
                    contentDescription = "Discount Icon",
                    modifier = Modifier.size(24.dp),
                    tint = colorResource(id = R.color.text)
                )

            }

            if (discounts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No Discounts Available",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Gray
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(color = Color(0xFFF9F9F9))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    items(discounts) { discount ->
                        DiscountItem(discount)
                    }
                }
            }
        }
    }
}

@Composable
fun DiscountItem(discount: Discount) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { Log.d("DiscountItem", "Clicked on ${discount.title}") },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Image(
                painter = rememberAsyncImagePainter(discount.imageUrl),
                contentDescription = "Discount Image",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = discount.title,
                style = MaterialTheme.typography.titleMedium,
                color = colorResource(id = R.color.buttonbg)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = discount.description,
                style = MaterialTheme.typography.bodyMedium,
                color = colorResource(id = R.color.grey)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewDiscountItem() {
    DiscountItem(
        Discount(
            id = "1",
            title = "Black Friday Sale",
            description = "50% off on all items!",
            imageUrl = "https://example.com/image.png"
        )
    )
}
