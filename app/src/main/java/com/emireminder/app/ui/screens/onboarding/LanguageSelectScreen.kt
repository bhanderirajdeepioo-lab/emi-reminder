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

private data class AppLanguage(
    val flag: String,
    val nativeName: String,
    val englishName: String,
    val code: String,
)

private val LANGUAGES = listOf(
    AppLanguage("🇺🇸", "English", "English", "en"),
    AppLanguage("🇨🇳", "中文 (普通话)", "Chinese (Mandarin)", "zh"),
    AppLanguage("🇮🇳", "हिन्दी", "Hindi", "hi"),
    AppLanguage("🇪🇸", "Español", "Spanish", "es"),
    AppLanguage("🇫🇷", "Français", "French", "fr"),
    AppLanguage("🇸🇦", "العربية", "Arabic", "ar"),
    AppLanguage("🇧🇩", "বাংলা", "Bengali", "bn"),
    AppLanguage("🇷🇺", "Русский", "Russian", "ru"),
    AppLanguage("🇧🇷", "Português", "Portuguese", "pt"),
    AppLanguage("🇵🇰", "اردو", "Urdu", "ur"),
    AppLanguage("🇲🇾", "Bahasa Melayu", "Malay / Indonesian", "ms"),
    AppLanguage("🇩🇪", "Deutsch", "German", "de"),
    AppLanguage("🇯🇵", "日本語", "Japanese", "ja"),
    AppLanguage("🇰🇪", "Kiswahili", "Swahili", "sw"),
    AppLanguage("🇮🇳", "मराठी", "Marathi", "mr"),
    AppLanguage("🇮🇳", "తెలుగు", "Telugu", "te"),
    AppLanguage("🇹🇷", "Türkçe", "Turkish", "tr"),
    AppLanguage("🇮🇳", "தமிழ்", "Tamil", "ta"),
    AppLanguage("🇰🇷", "한국어", "Korean", "ko"),
    AppLanguage("🇻🇳", "Tiếng Việt", "Vietnamese", "vi"),
    AppLanguage("🇮🇹", "Italiano", "Italian", "it"),
    AppLanguage("🇳🇬", "Hausa", "Hausa", "ha"),
    AppLanguage("🇹🇭", "ภาษาไทย", "Thai", "th"),
    AppLanguage("🇮🇳", "ગુજરાતી", "Gujarati", "gu"),
    AppLanguage("🇮🇳", "ಕನ್ನಡ", "Kannada", "kn"),
    AppLanguage("🇮🇷", "فارسی", "Persian", "fa"),
    AppLanguage("🇵🇱", "Polski", "Polish", "pl"),
    AppLanguage("🇮🇳", "ਪੰਜਾਬੀ", "Punjabi", "pa"),
    AppLanguage("🇺🇦", "Українська", "Ukrainian", "uk"),
    AppLanguage("🇨🇳", "粵語", "Cantonese", "yue"),
)

@Composable
fun LanguageSelectScreen(onContinue: (languageCode: String) -> Unit) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var selectedCode by remember { mutableStateOf("en") }

    val filtered = remember(query) {
        if (query.isBlank()) LANGUAGES
        else LANGUAGES.filter {
            it.englishName.contains(query, ignoreCase = true) ||
                it.nativeName.contains(query, ignoreCase = true)
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
            Text(
                "Welcome to EMI Reminder",
                fontSize = 13.sp,
                color = Color(0xFF818CF8),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Choose Your Language",
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                textAlign = TextAlign.Center,
                letterSpacing = (-0.5).sp,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Select the language you're most comfortable with",
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
                    Text("Search languages...", fontSize = 14.sp, color = Color(0xFF6B7280))
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
            items(filtered, key = { it.code }) { lang ->
                val isSelected = lang.code == selectedCode
                LanguageItem(
                    lang = lang,
                    isSelected = isSelected,
                    onClick = { selectedCode = lang.code },
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
                    context.getSharedPreferences("emi_prefs", Context.MODE_PRIVATE)
                        .edit().putString("selected_language", selectedCode).apply()
                    onContinue(selectedCode)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
            ) {
                Text(
                    "Continue",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun LanguageItem(
    lang: AppLanguage,
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
        Text(lang.flag, fontSize = 24.sp)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                lang.nativeName,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
            )
            Text(
                lang.englishName,
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
