package com.emireminder.app.ui.screens.smsdashboard

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.emireminder.app.domain.model.TransactionCategory
import com.emireminder.app.ui.theme.*

internal data class CatMeta(val label: String, val icon: ImageVector, val color: Color)

internal val catMeta: Map<TransactionCategory, CatMeta> = mapOf(
    TransactionCategory.INCOME          to CatMeta("Income",        Icons.Default.TrendingUp,     SafeGreen),
    TransactionCategory.EMI_AND_LOANS   to CatMeta("EMI & Loans",   Icons.Default.CreditCard,     Indigo600),
    TransactionCategory.FOOD_AND_DINING to CatMeta("Food & Dining", Icons.Default.Restaurant,     Color(0xFFEF6C00)),
    TransactionCategory.TRANSPORT       to CatMeta("Transport",      Icons.Default.DirectionsCar,  Cyan700),
    TransactionCategory.UTILITIES       to CatMeta("Utilities",      Icons.Default.ElectricBolt,   Amber700),
    TransactionCategory.SHOPPING        to CatMeta("Shopping",       Icons.Default.ShoppingBag,    Violet600),
    TransactionCategory.HEALTH          to CatMeta("Health",         Icons.Default.LocalHospital,  UrgentRed),
    TransactionCategory.ENTERTAINMENT   to CatMeta("Entertainment",  Icons.Default.Movie,          Color(0xFFEC4899)),
    TransactionCategory.INVESTMENTS     to CatMeta("Investments",    Icons.Default.ShowChart,      Color(0xFF0D9488)),
    TransactionCategory.INSURANCE       to CatMeta("Insurance",      Icons.Default.Security,       Color(0xFF2563EB)),
    TransactionCategory.CREDIT_CARD     to CatMeta("Credit Card",    Icons.Default.Payment,        Slate800),
    TransactionCategory.ATM_AND_CASH    to CatMeta("ATM / Cash",     Icons.Default.LocalAtm,       Color(0xFF92400E)),
    TransactionCategory.BANK_CHARGES    to CatMeta("Bank Charges",   Icons.Default.AccountBalance, Color(0xFF6B7280)),
    TransactionCategory.UNCATEGORISED   to CatMeta("Uncategorised",  Icons.Default.Category,       Color(0xFF9CA3AF)),
)

internal fun TransactionCategory.meta(): CatMeta =
    catMeta[this] ?: CatMeta(name, Icons.Default.Category, Color(0xFF9CA3AF))
