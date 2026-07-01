package com.emireminder.app.ui.screens.finance

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
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
import com.emireminder.app.ui.theme.*

private data class LoanCategory(
    val name: String,
    val emoji: String,
    val color: Color,
    val subtitle: String,
    val isPopular: Boolean = false,
)

private val loanCategories = listOf(
    LoanCategory("Home Loan",     "🏠", HomeLoanColor,         "Up to ₹2 Cr",          isPopular = true),
    LoanCategory("Car Loan",      "🚗", CarLoanColor,          "Up to ₹50 L"),
    LoanCategory("Personal",      "👤", PersonalLoanColor,     "Up to ₹40 L"),
    LoanCategory("Education",     "🎓", Color(0xFF7C3AED),     "Up to ₹75 L"),
    LoanCategory("Business",      "💼", Color(0xFF059669),     "Up to ₹5 Cr"),
    LoanCategory("Two-Wheeler",   "🏍️", Color(0xFF0891B2),     "Up to ₹10 L"),
    LoanCategory("Gold Loan",     "🥇", Color(0xFFD97706),     "Quick disbursal"),
    LoanCategory("Medical",       "🏥", Color(0xFFDC2626),     "Emergency loan"),
    LoanCategory("Loan vs Prop",  "🏢", Color(0xFF475569),     "LAP · Up to 70%"),
    LoanCategory("Consumer",      "🛍️", Color(0xFFF97316),     "Electronics/Applia"),
    LoanCategory("CC EMI",        "💳", Color(0xFF6D28D9),     "Credit card convert"),
    LoanCategory("Agriculture",   "🌾", Color(0xFF16A34A),     "Kisan Credit Card"),
)

@Composable
fun LoanCategoriesScreen(
    onCategorySelected: (String) -> Unit = {},
    onCustomLoanType: () -> Unit = {},
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
                "Choose a loan type to calculate or add a reminder",
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
                item(span = { GridItemSpan(maxLineSpan) }) {
                    CustomLoanTypeCard(onClick = onCustomLoanType)
                }
            }
        }
    }
}

@Composable
private fun CategoryCard(cat: LoanCategory, onClick: () -> Unit) {
    Box {
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
                Text(cat.subtitle, fontSize = 10.sp, color = cat.color, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
            }
        }
        if (cat.isPopular) {
            Text(
                "POPULAR",
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = 8.dp, y = (-7).dp)
                    .background(Color(0xFFDC2626), RoundedCornerShape(9.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = 0.3.sp,
            )
        }
    }
}

@Composable
private fun CustomLoanTypeCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(top = 4.dp, bottom = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = Color(0xFF4F46E5),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Custom Loan Type",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B),
                )
                Text(
                    "Add any other loan or credit line",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B),
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = Color(0xFF4F46E5),
            )
        }
    }
}
