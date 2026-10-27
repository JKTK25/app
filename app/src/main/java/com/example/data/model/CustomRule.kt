package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class RuleType {
    BLOCK,
    ALLOW
}

enum class RuleCategory(val displayName: String) {
    DOMAIN("Domain / Host"),
    KEYWORD("Keyword / Phrase"),
    URL_PATH("URL Path / Pattern")
}

@Entity(tableName = "custom_rules")
data class CustomRule(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val pattern: String,
    val ruleType: RuleType = RuleType.BLOCK,
    val category: RuleCategory = RuleCategory.DOMAIN,
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
