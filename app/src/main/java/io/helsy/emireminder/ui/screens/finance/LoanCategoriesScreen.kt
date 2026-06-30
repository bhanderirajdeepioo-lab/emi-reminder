package io.helsy.emireminder.ui.screens.finance

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.helsy.emireminder.ui.theme.*

private data class LoanCategory(val name: String, val emoji: String, val color: Color, val typicalRate: String)

private val loanCategories = listOf(
    LoanCategory("Home Loan", "🏠", HomeLoanColor, "6.5–9%"),
    LoanCategory("Car Loan", "🚗", CarLoanColor, "8–12%"),
    LoanCategory("Personal Loan", "👤", PersonalLoanColor, "10–24%"),
    LoanCategory("Education Loan", "🎓", Color(0xFF7C3AED), "8–12%"),
    LoanCategory("Business Loan", "💼", Color(0xFF059669), "12–18%"),
    LoanCategory("Gold Loan", "🥇", Color(0xFFD97706), "7–12%"),
    LoanCategory("Bike Loan", "🏍️", Color(0xFF0891B2), "10–15%"),
    LoanCategory("Medical Loan", "🏥", Color(0xFFDC2626), "12–18%"),
    LoanCategory("Consumer Loan", "🛍️", Color(0xFFF97316), "13–18%"),
    LoanCategory("Agricultural", "🌾", Color(0xFF16A34A), "4–9%"),
    LoanCategory("Mortgage Loan", "🏢", Color(0xFF475569), "9–13%"),
    LoanCategory("Credit Card", "💳", Color(0xFF6D28D9), "24–48%"),
)

@Composable
fun LoanCategoriesScreen(
    onCategorySelected: (String) -> Unit = {},
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Loan Categories", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Indigo600, titleContentColor = Color.White, navigationIconContentColor = Color.White),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Text(
                "Select a category to calculate EMI",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(loanCategories) { cat ->
                    CategoryCard(cat = cat, onClick = { onCategorySelected(cat.name) })
                }
            }
        }
    }
}

@Composable
private fun CategoryCard(cat: LoanCategory, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(cat.color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(cat.emoji, fontSize = 24.sp)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                cat.name,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                lineHeight = 15.sp,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(2.dp))
            Text(cat.typicalRate, fontSize = 10.sp, color = cat.color, fontWeight = FontWeight.Medium)
        }
    }
}
