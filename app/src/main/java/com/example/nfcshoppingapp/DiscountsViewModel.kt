package com.example.nfcshoppingapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.database.*
import com.example.nfcshoppingapp.Discount
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class DiscountsViewModel : ViewModel() {
    private val database = FirebaseDatabase.getInstance().getReference("discounts")

    private val _discounts = MutableStateFlow<List<Discount>>(emptyList())
    val discounts: StateFlow<List<Discount>> get() = _discounts

    init {
        fetchDiscounts()
    }

    private fun fetchDiscounts() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val discountList = mutableListOf<Discount>()
                for (discountSnapshot in snapshot.children) {
                    val discount = discountSnapshot.getValue(Discount::class.java)
                    discount?.let { discountList.add(it) }
                }
                _discounts.value = discountList
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }
}