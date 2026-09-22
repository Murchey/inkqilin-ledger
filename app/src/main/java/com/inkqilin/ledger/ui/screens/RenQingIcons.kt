package com.inkqilin.ledger.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.inkqilin.ledger.data.RenQingEventType

/**
 * 人情账本图标：统一用 Material 图标，避免满屏 emoji。
 *
 * 标签 icon 字段兼容两种存法：
 *  - 新：语义键，如 "gift" / "favorite"
 *  - 旧：emoji（历史数据），通过映射表仍能正确显示
 */
object RenQingIcons {

    /** 可选图标（供标签编辑选择） */
    val tagIconOptions: List<Pair<String, ImageVector>> = listOf(
        "gift" to Icons.Filled.CardGiftcard,
        "favorite" to Icons.Filled.Favorite,
        "celebration" to Icons.Filled.Celebration,
        "cake" to Icons.Filled.Cake,
        "home" to Icons.Filled.Home,
        "school" to Icons.Filled.School,
        "child" to Icons.Filled.ChildCare,
        "flower" to Icons.Filled.LocalFlorist,
        "ring" to Icons.Filled.Diamond,
        "money" to Icons.Filled.Savings,
        "star" to Icons.Filled.Star,
        "trophy" to Icons.Filled.EmojiEvents,
        "family" to Icons.Filled.FamilyRestroom,
        "friends" to Icons.Filled.People,
        "work" to Icons.Filled.Work,
        "travel" to Icons.Filled.Flight,
        "meal" to Icons.Filled.Restaurant,
        "car" to Icons.Filled.DirectionsCar,
        "shopping" to Icons.Filled.ShoppingCart,
        "photo" to Icons.Filled.PhotoCamera,
        "music" to Icons.Filled.MusicNote,
        "book" to Icons.Filled.MenuBook,
        "package" to Icons.Filled.Inventory2,
        "cafe" to Icons.Filled.LocalCafe
    )

    private val keyToIcon: Map<String, ImageVector> = tagIconOptions.toMap()

    /** 历史 emoji → 图标，兼容旧数据 */
    private val legacyEmojiToIcon: Map<String, ImageVector> = mapOf(
        "\uD83C\uDF81" to Icons.Filled.CardGiftcard,      // 🎁
        "\uD83D\uDC92" to Icons.Filled.Favorite,           // 🐮（婚礼）
        "\uD83D\uDE4F" to Icons.Filled.LocalFlorist,       // 🙏（丧礼）
        "\uD83C\uDF82" to Icons.Filled.Cake,               // 🎂
        "\uD83C\uDFE0" to Icons.Filled.Home,               // 🏠
        "\uD83C\uDF93" to Icons.Filled.School,             // 🎓
        "\uD83D\uDC76" to Icons.Filled.ChildCare,          // 👶
        "\uD83C\uDF89" to Icons.Filled.Celebration,        // 🎉
        "\u2764\uFE0F" to Icons.Filled.Favorite,           // ❤️
        "\uD83D\uDC8D" to Icons.Filled.Diamond,            // 💍
        "\uD83D\uDCB0" to Icons.Filled.Savings,            // 💰
        "\u2B50" to Icons.Filled.Star,                     // ⭐
        "\uD83C\uDFC6" to Icons.Filled.EmojiEvents,        // 🏆
        "\uD83C\uDF7D\uFE0F" to Icons.Filled.Restaurant,   // 🍽️
        "\uD83D\uDCDA" to Icons.Filled.MenuBook,           // 📚
        "\uD83D\uDCE6" to Icons.Filled.Inventory2,         // 📦
        "\uD83D\uDED2" to Icons.Filled.ShoppingCart,       // 🛒
        "\uD83D\uDE97" to Icons.Filled.DirectionsCar,      // 🚗
        "\u2708\uFE0F" to Icons.Filled.Flight,             // ✈️
        "\uD83D\uDCF7" to Icons.Filled.PhotoCamera,        // 📷
        "\uD83C\uDFB5" to Icons.Filled.MusicNote,          // 🎵
        "\u2615" to Icons.Filled.LocalCafe,                // ☕
        "\uD83C\uDF54" to Icons.Filled.LunchDining,        // 🍔
        "\uD83C\uDF70" to Icons.Filled.LocalPizza,         // 🍕
        "\uD83C\uDF1F" to Icons.Filled.Star,               // 🌟
        "\uD83D\uDC8E" to Icons.Filled.Diamond,            // 💎
        "\uD83C\uDFA8" to Icons.Filled.Palette,            // 🎨
        "\uD83C\uDFA4" to Icons.Filled.Mic,                // 🎤
        "\uD83C\uDFA7" to Icons.Filled.Headphones,         // 🎧
        "\uD83C\uDFB6" to Icons.Filled.MusicNote,          // 🎶
        "\u26BD" to Icons.Filled.SportsSoccer,             // ⚽
        "\uD83C\uDFC0" to Icons.Filled.SportsBasketball,   // 🏀
        "\uD83C\uDFBF" to Icons.Filled.SportsTennis,       // 🎾
        "\uD83C\uDFAB" to Icons.Filled.ConfirmationNumber, // 🎫
        "\uD83C\uDF88" to Icons.Filled.Celebration,        // 🎈
        "\uD83C\uDF7A" to Icons.Filled.SportsBar,          // 🍺
        "\uD83C\uDF55" to Icons.Filled.LocalPizza,         // 🍕
        "\uD83C\uDF63" to Icons.Filled.RamenDining,        // 🍣
        "\uD83C\uDF66" to Icons.Filled.Icecream,           // 🍦
        "\uD83D\uDC57" to Icons.Filled.Checkroom,          // 👗
        "\uD83D\uDEAA" to Icons.Filled.MeetingRoom,        // 🚪
        "\uD83E\uDD3E" to Icons.Filled.SelfImprovement     // 🤸
    )

    fun iconForTagIconValue(iconValue: String?): ImageVector {
        val value = iconValue?.trim().orEmpty()
        if (value.isEmpty()) return Icons.Filled.CardGiftcard
        keyToIcon[value]?.let { return it }
        legacyEmojiToIcon[value]?.let { return it }
        return Icons.Filled.CardGiftcard
    }

    fun eventTypeIcon(type: RenQingEventType): ImageVector = when (type) {
        RenQingEventType.WEDDING -> Icons.Filled.Favorite
        RenQingEventType.FUNERAL -> Icons.Filled.LocalFlorist
        RenQingEventType.BIRTHDAY -> Icons.Filled.Cake
        RenQingEventType.MOVING -> Icons.Filled.Home
        RenQingEventType.GRADUATION -> Icons.Filled.School
        RenQingEventType.BABY -> Icons.Filled.ChildCare
        RenQingEventType.OTHER -> Icons.Filled.CardGiftcard
    }
}