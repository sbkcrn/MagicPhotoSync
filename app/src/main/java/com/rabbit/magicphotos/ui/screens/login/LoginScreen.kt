package com.rabbit.magicphotos.ui.screens.login

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.rabbit.magicphotos.ui.theme.Beige100
import com.rabbit.magicphotos.ui.theme.RabbitOrange
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.Base64

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: (token: String, expiry: Long, userId: String, email: String) -> Unit,
    onDismiss: () -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    var showWebView by remember { mutableStateOf(true) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sign in to Rabbit Hole") },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (showWebView) {
                LoginWebView(
                    onTokenExtracted = { token, expiry, userId, email ->
                        showWebView = false
                        onLoginSuccess(token, expiry, userId, email)
                    },
                    onLoadingChanged = { isLoading = it }
                )
            }
            
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = RabbitOrange)
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun LoginWebView(
    onTokenExtracted: (token: String, expiry: Long, userId: String, email: String) -> Unit,
    onLoadingChanged: (Boolean) -> Unit
) {
    val cookieManager = CookieManager.getInstance()
    
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.databaseEnabled = true
                
                // Clear existing cookies for fresh login
                cookieManager.removeAllCookies(null)
                
                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        onLoadingChanged(true)
                    }
                    
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        onLoadingChanged(false)
                        
                        // Check if we're on the main page (logged in)
                        if (url?.contains("hole.rabbit.tech") == true && 
                            !url.contains("login") &&
                            !url.contains("auth0")) {
                            
                            // Try to extract token from page
                            extractTokenFromPage(view, onTokenExtracted)
                        }
                    }
                    
                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean {
                        return false
                    }
                }
                
                loadUrl("https://hole.rabbit.tech/")
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

private fun extractTokenFromPage(webView: WebView?, onTokenExtracted: (String, Long, String, String) -> Unit) {
    webView?.evaluateJavascript(
        """
        (function() {
            // Look for accessToken in the page's script tags
            var scripts = document.getElementsByTagName('script');
            for (var i = 0; i < scripts.length; i++) {
                var content = scripts[i].innerHTML;
                // Look for JWT token pattern - match full JWT (3 parts separated by dots)
                // JWT characters: alphanumeric, -, _, .
                var tokenMatch = content.match(/eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+/);
                if (tokenMatch) {
                    return tokenMatch[0];
                }
                // Also try looking for accessToken property
                var accessTokenMatch = content.match(/"accessToken"\s*:\s*"([^"]+)"/);
                if (accessTokenMatch && accessTokenMatch[1].startsWith('eyJ')) {
                    return accessTokenMatch[1];
                }
            }
            return null;
        })();
        """.trimIndent()
    ) { result ->
        val token = result?.trim('"')
        if (!token.isNullOrEmpty() && token != "null") {
            // Decode JWT to get user info
            try {
                val parts = token.split(".")
                if (parts.size >= 2) {
                    val payload = String(Base64.getUrlDecoder().decode(parts[1]))
                    val json = Json.parseToJsonElement(payload).jsonObject
                    
                    val exp = json["exp"]?.jsonPrimitive?.content?.toLongOrNull()?.times(1000) 
                        ?: (System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000)
                    val sub = json["sub"]?.jsonPrimitive?.content ?: ""
                    val email = json["email"]?.jsonPrimitive?.content ?: ""
                    
                    onTokenExtracted(token, exp, sub, email)
                }
            } catch (e: Exception) {
                // If parsing fails, use defaults
                onTokenExtracted(
                    token,
                    System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000,
                    "",
                    ""
                )
            }
        }
    }
}

