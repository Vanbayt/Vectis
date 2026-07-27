package io.nekohasekai.sfa.utils

import java.util.Locale

object LocationLocalizer {
    data class LocationInfo(
        val flag: String,
        val nameRu: String,
        val nameEn: String,
    )

    fun getLocationInfo(tag: String): LocationInfo {
        val lower = tag.lowercase(Locale.ROOT)
        return when {
            lower.contains("holland") || lower.contains("netherlands") || lower.contains("-nl") || lower.startsWith("nl-") || lower == "nl" ->
                LocationInfo("🇳🇱", "Нидерланды", "Netherlands")
            lower.contains("germany") || lower.contains("-de") || lower.startsWith("de-") || lower == "de" ->
                LocationInfo("🇩🇪", "Германия", "Germany")
            lower.contains("finland") || lower.contains("-fi") || lower.startsWith("fi-") || lower == "fi" ->
                LocationInfo("🇫🇮", "Финляндия", "Finland")
            lower.contains("sweden") || lower.contains("-se") || lower.startsWith("se-") || lower == "se" ->
                LocationInfo("🇸🇪", "Швеция", "Sweden")
            lower.contains("turkey") || lower.contains("-tr") || lower.startsWith("tr-") || lower == "tr" ->
                LocationInfo("🇹🇷", "Турция", "Turkey")
            lower.contains("kazakhstan") || lower.contains("-kz") || lower.startsWith("kz-") || lower == "kz" ->
                LocationInfo("🇰🇿", "Казахстан", "Kazakhstan")
            lower.contains("usa") || lower.contains("united states") || lower.contains("-us") || lower.startsWith("us-") || lower == "us" ->
                LocationInfo("🇺🇸", "США", "United States")
            lower.contains("uk") || lower.contains("england") || lower.contains("-gb") || lower.startsWith("gb-") || lower == "gb" ->
                LocationInfo("🇬🇧", "Великобритания", "United Kingdom")
            lower.contains("singapore") || lower.contains("-sg") || lower.startsWith("sg-") || lower == "sg" ->
                LocationInfo("🇸🇬", "Сингапур", "Singapore")
            lower.contains("japan") || lower.contains("-jp") || lower.startsWith("jp-") || lower == "jp" ->
                LocationInfo("🇯🇵", "Япония", "Japan")
            lower.contains("russia") || lower.contains("-ru") || lower.startsWith("ru-") || lower == "ru" ->
                LocationInfo("🇷🇺", "Россия", "Russia")
            else ->
                LocationInfo("🌐", "Сервер", "Server")
        }
    }

    fun formatLocation(tag: String, locale: Locale = Locale.getDefault()): String {
        if (tag.isBlank()) return "—"
        val info = getLocationInfo(tag)
        val countryName = if (locale.language == "ru") info.nameRu else info.nameEn
        val nodeNum = Regex("""\b(\d+)\b""").find(tag)?.groupValues?.get(1)
        val nodeSuffix = if (nodeNum != null) " #$nodeNum" else ""
        return "${info.flag} $countryName$nodeSuffix"
    }
}
