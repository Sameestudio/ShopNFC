package com.example.nfcshoppingapp

class MidtransConfig {
    companion object {
        // Replace with your actual Midtrans Client Key (Sandbox or Production)
        const val CLIENT_KEY = "SB-Mid-client-VAIWlgvi70iwpa6l" // <-- REPLACE THIS with your key
        // Merchant URL is typically needed for fetching transaction tokens from your backend.
        // For purely client-side *initiation*, it might not be strictly required,
        // but it's good practice to set it if you have a backend or a default.
        const val MERCHANT_URL = "https://01be-103-226-174-25.ngrok-free.app/midtrans-php-backend/generate-token.php/" // <-- If you have a backend, put its base URL here
        //https://merchant-url-sandbox.com/G265806218/
    }
}