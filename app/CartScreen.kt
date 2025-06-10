package com.example.nfcshoppingapp

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

data class CartItem(
    val id: String,
    val name: String,
    val price: Double,
    val imageRes: Int,
    var quantity: Int
)

@Composable
fun CartScreen(navController: NavController) {
    var cartItems by remember {
        mutableStateOf(
            listOf(
                CartItem("1", "Wireless Headphones", 49.99, R.drawable.sample_product, 1),
                CartItem("2", "Smart Watch", 89.99, R.drawable.sample_product, 2)
            )
        )
    }

    val totalPrice = cartItems.sumOf { it.price * it.quantity }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Your Cart") }, backgroundColor = MaterialTheme.colors.primary)
        },
        content = {
            Column(Modifier.padding(16.dp)) {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(cartItems, key = { it.id }) { item ->
                        CartItemCard(item,
                            onQuantityChange = { newQty ->
                                cartItems = cartItems.map {
                                    if (it.id == item.id) it.copy(quantity = newQty) else it
                                }
                            },
                            onRemove = {
                                cartItems = cartItems.filterNot { it.id == item.id }
                            }
                        )
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 10.dp))

                Text("Total: $${"%.2f".format(totalPrice)}", style = MaterialTheme.typography.h6)

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { /* TODO: Checkout logic */ },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Proceed to Checkout")
                }
            }
        }
    )
}

@Composable
fun CartItemCard(
    item: CartItem,
    onQuantityChange: (Int) -> Unit,
    onRemove: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        elevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(12.dp)) {
            Image(
                painter = painterResource(id = item.imageRes),
                contentDescription = item.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(80.dp)
                    .padding(end = 12.dp)
            )

            Column(Modifier.weight(1f)) {
                Text(item.name, style = MaterialTheme.typography.subtitle1)
                Text("$${item.price}", color = Color.Gray)

                Spacer(modifier = Modifier.height(6.dp))

                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
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
