package com.emireminder.app.ui.screens.onboarding

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emireminder.app.ui.theme.*

private data class Country(
    val flag: String,
    val name: String,
    val currencyCode: String,
    val currencySymbol: String,
    val code: String,
)

private val COUNTRIES = listOf(
    Country("🇮🇳", "India", "INR", "₹", "IN"),
    Country("🇺🇸", "United States", "USD", "$", "US"),
    Country("🇬🇧", "United Kingdom", "GBP", "£", "GB"),
    Country("🇨🇦", "Canada", "CAD", "CA$", "CA"),
    Country("🇦🇺", "Australia", "AUD", "A$", "AU"),
    Country("🇦🇪", "United Arab Emirates", "AED", "د.إ", "AE"),
    Country("🇸🇦", "Saudi Arabia", "SAR", "﷼", "SA"),
    Country("🇸🇬", "Singapore", "SGD", "S$", "SG"),
    Country("🇲🇾", "Malaysia", "MYR", "RM", "MY"),
    Country("🇵🇰", "Pakistan", "PKR", "₨", "PK"),
    Country("🇧🇩", "Bangladesh", "BDT", "৳", "BD"),
    Country("🇱🇰", "Sri Lanka", "LKR", "Rs", "LK"),
    Country("🇳🇵", "Nepal", "NPR", "Rs", "NP"),
    Country("🇧🇹", "Bhutan", "BTN", "Nu", "BT"),
    Country("🇲🇻", "Maldives", "MVR", "Rf", "MV"),
    Country("🇩🇪", "Germany", "EUR", "€", "DE"),
    Country("🇫🇷", "France", "EUR", "€", "FR"),
    Country("🇮🇹", "Italy", "EUR", "€", "IT"),
    Country("🇪🇸", "Spain", "EUR", "€", "ES"),
    Country("🇵🇹", "Portugal", "EUR", "€", "PT"),
    Country("🇳🇱", "Netherlands", "EUR", "€", "NL"),
    Country("🇸🇪", "Sweden", "SEK", "kr", "SE"),
    Country("🇳🇴", "Norway", "NOK", "kr", "NO"),
    Country("🇨🇭", "Switzerland", "CHF", "Fr", "CH"),
    Country("🇵🇱", "Poland", "PLN", "zł", "PL"),
    Country("🇷🇺", "Russia", "RUB", "₽", "RU"),
    Country("🇺🇦", "Ukraine", "UAH", "₴", "UA"),
    Country("🇹🇷", "Turkey", "TRY", "₺", "TR"),
    Country("🇯🇵", "Japan", "JPY", "¥", "JP"),
    Country("🇨🇳", "China", "CNY", "¥", "CN"),
    Country("🇰🇷", "South Korea", "KRW", "₩", "KR"),
    Country("🇮🇩", "Indonesia", "IDR", "Rp", "ID"),
    Country("🇵🇭", "Philippines", "PHP", "₱", "PH"),
    Country("🇹🇭", "Thailand", "THB", "฿", "TH"),
    Country("🇻🇳", "Vietnam", "VND", "₫", "VN"),
    Country("🇧🇷", "Brazil", "BRL", "R$", "BR"),
    Country("🇲🇽", "Mexico", "MXN", "$", "MX"),
    Country("🇦🇷", "Argentina", "ARS", "$", "AR"),
    Country("🇳🇬", "Nigeria", "NGN", "₦", "NG"),
    Country("🇿🇦", "South Africa", "ZAR", "R", "ZA"),
    Country("🇰🇪", "Kenya", "KES", "KSh", "KE"),
    Country("🇬🇭", "Ghana", "GHS", "₵", "GH"),
    Country("🇮🇱", "Israel", "ILS", "₪", "IL"),
    Country("🇮🇷", "Iran", "IRR", "﷼", "IR"),
    Country("🇳🇿", "New Zealand", "NZD", "NZ$", "NZ"),
    Country("🇮🇪", "Ireland", "EUR", "€", "IE"),
)

@Composable
fun CountrySelectScreen(onContinue: (countryCode: String) -> Unit) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var selectedCode by remember { mutableStateOf("IN") }

    val filtered = remember(query) {
        if (query.isBlank()) COUNTRIES
        else COUNTRIES.filter {
            it.name.contains(query, ignoreCase = true) ||
                it.currencyCode.contains(query, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 32.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Indigo600.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.Public,
                    contentDescription = null,
                    tint = Indigo600,
                    modifier = Modifier.size(28.dp),
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "Almost There!",
                fontSize = 13.sp,
                color = Color(0xFF818CF8),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Select Your Country",
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                textAlign = TextAlign.Center,
                letterSpacing = (-0.5).sp,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "We'll use this for currency and regional settings",
                fontSize = 14.sp,
                color = Color(0xFFB0AEC0),
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
            )
            Spacer(Modifier.height(20.dp))

            TextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                placeholder = {
                    Text("Search countries...", fontSize = 14.sp, color = Color(0xFF6B7280))
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = Color(0xFF818CF8),
                        modifier = Modifier.size(20.dp),
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = DarkCard,
                    unfocusedContainerColor = DarkCard,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = Indigo600,
                ),
            )
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 8.dp),
        ) {
            items(filtered, key = { it.code }) { country ->
                val isSelected = country.code == selectedCode
                CountryItem(
                    country = country,
                    isSelected = isSelected,
                    onClick = { selectedCode = country.code },
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            Button(
                onClick = {
                    val prefs = context.getSharedPreferences("emi_prefs", Context.MODE_PRIVATE)
                    val selected = COUNTRIES.find { it.code == selectedCode }
                    prefs.edit()
                        .putString("selected_country", selectedCode)
                        .putString("selected_currency_code", selected?.currencyCode ?: "INR")
                        .putString("selected_currency_symbol", selected?.currencySymbol ?: "₹")
                        .apply()
                    onContinue(selectedCode)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
            ) {
                Text(
                    "Get Started",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun CountryItem(
    country: Country,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) Indigo600.copy(alpha = 0.15f) else DarkCard)
            .then(
                if (isSelected) Modifier.border(2.dp, Indigo600, RoundedCornerShape(12.dp))
                else Modifier
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(country.flag, fontSize = 24.sp)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                country.name,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
            )
            Text(
                "${country.currencyCode} · ${country.currencySymbol}",
                fontSize = 12.sp,
                color = Color(0xFFB0AEC0),
            )
        }
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Indigo600),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}
