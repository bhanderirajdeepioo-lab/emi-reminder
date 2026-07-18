package com.emireminder.app.data.db.converter

import androidx.room.TypeConverter
import com.emireminder.app.domain.model.EmiStatus
import com.emireminder.app.domain.model.TransactionCategory
import com.emireminder.app.domain.model.TransactionDirection
import org.json.JSONObject

class SmsFinanceConverters {

    @TypeConverter
    fun fromTransactionCategory(value: TransactionCategory): String = value.name

    @TypeConverter
    fun toTransactionCategory(value: String): TransactionCategory =
        TransactionCategory.valueOf(value)

    @TypeConverter
    fun fromTransactionDirection(value: TransactionDirection): String = value.name

    @TypeConverter
    fun toTransactionDirection(value: String): TransactionDirection =
        TransactionDirection.valueOf(value)

    @TypeConverter
    fun fromEmiStatus(value: EmiStatus): String = value.name

    @TypeConverter
    fun toEmiStatus(value: String): EmiStatus = EmiStatus.valueOf(value)

    @TypeConverter
    fun fromCategoryMap(value: Map<String, Double>): String {
        val json = JSONObject()
        value.forEach { (k, v) -> json.put(k, v) }
        return json.toString()
    }

    @TypeConverter
    fun toCategoryMap(value: String): Map<String, Double> {
        val json = JSONObject(value)
        return json.keys().asSequence().associateWith { json.getDouble(it) }
    }
}
