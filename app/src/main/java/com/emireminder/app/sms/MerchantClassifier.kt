package com.emireminder.app.sms

object MerchantClassifier {

    private val vpaKeywords: Map<TransactionCategory, List<String>> = mapOf(
        TransactionCategory.FOOD_AND_DINING to listOf(
            "swiggy", "zomato", "dominos", "mcdonalds", "kfc", "pizzahut",
            "burgerking", "subway", "starbucks", "dunkin", "barbeque", "box8",
            "fasoos", "rebel", "eatfit", "licious", "behrouz", "faasos",
            "ovenst", "chaayos", "ccd",
        ),
        TransactionCategory.TRANSPORT to listOf(
            "olacabs", "ola", "uber", "rapido", "blusmart", "meru", "indrive",
            "irctc", "makemytrip", "mmt", "cleartrip", "easemytrip", "redbus",
            "abhibus", "yatra", "goibibo", "indigo", "spicejet", "airindia",
            "vistara", "akasa", "goair",
        ),
        TransactionCategory.SHOPPING to listOf(
            "amazon", "flipkart", "myntra", "meesho", "ajio", "nykaa",
            "snapdeal", "shopsy", "tatacliq", "jiomart", "bigbasket", "blinkit",
            "zepto", "grofers", "dmart", "croma", "decathlon", "ikea",
            "pepperfry", "urbanladder",
        ),
        TransactionCategory.HEALTH to listOf(
            "apollopharmacy", "apollo247", "pharmeasy", "netmeds", "1mg",
            "practo", "medlife", "medplus", "healthkart", "curefit", "cultfit",
            "lybrate", "mfine",
        ),
    )

    private val bodyKeywords: Map<TransactionCategory, List<String>> = mapOf(
        TransactionCategory.FOOD_AND_DINING to listOf(
            "Swiggy", "Zomato", "Dominos", "McDonald", "KFC", "Pizza Hut",
            "Burger King", "Subway", "Starbucks", "Dunkin", "Barbeque Nation",
            "Box8", "Fasoos", "EatFit", "Licious", "Behrouz", "Faasos",
            "Oven Story", "Chaayos", "Café Coffee Day",
        ),
        TransactionCategory.TRANSPORT to listOf(
            "Ola", "Uber", "Rapido", "BluSmart", "Meru", "InDrive", "IRCTC",
            "MakeMyTrip", "Cleartrip", "EaseMyTrip", "redBus", "Abhibus",
            "Yatra", "GoIbibo", "BMTC", "IndiGo", "SpiceJet", "Air India",
            "Vistara", "Akasa Air",
        ),
        TransactionCategory.SHOPPING to listOf(
            "Amazon", "Flipkart", "Myntra", "Meesho", "Ajio", "Nykaa",
            "Snapdeal", "Shopsy", "Tata CLiQ", "JioMart", "BigBasket",
            "Blinkit", "Zepto", "Swiggy Instamart", "D-Mart", "Reliance Retail",
            "Croma", "Decathlon", "IKEA", "Pepperfry", "Urban Ladder",
        ),
        TransactionCategory.HEALTH to listOf(
            "Apollo Pharmacy", "Apollo 247", "PharmEasy", "Netmeds", "1mg",
            "Practo", "Medlife", "Tata 1mg", "Medplus", "Frank Ross",
            "HealthKart", "CureFit", "Cult.fit", "Lybrate", "mfine",
        ),
        TransactionCategory.BANK_CHARGES to listOf(
            "service charge", "account maintenance", "annual fee",
            "processing fee", "convenience fee", "late payment fee",
            "cheque return", "NACH return", "penalty charge",
            "minimum balance", "non-maintenance", "locker rent",
            "forex markup", "GST on charges",
        ),
    )

    fun classify(
        body: String,
        vpa: String? = null,
        merchantName: String? = null,
    ): TransactionCategory? {
        val vpaLower = vpa?.lowercase()
        if (vpaLower != null) {
            for ((category, keywords) in vpaKeywords) {
                if (keywords.any { vpaLower.contains(it) }) return category
            }
        }

        val merchantLower = merchantName?.lowercase()
        if (merchantLower != null) {
            for ((category, keywords) in bodyKeywords) {
                if (keywords.any { merchantLower.contains(it.lowercase()) }) return category
            }
        }

        val bodyLower = body.lowercase()
        for ((category, keywords) in bodyKeywords) {
            if (keywords.any { bodyLower.contains(it.lowercase()) }) return category
        }

        return null
    }
}
