package com.inkqilin.ledger.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * UI 规范 token（developDocs/UI_STANDARD.md）。
 * 新代码优先使用本文件常量，避免魔法数。
 */
object Space {
    val Xxs = 2.dp
    val Xs = 4.dp
    val Sm = 8.dp
    val Md = 12.dp
    val Lg = 16.dp
    val Xl = 20.dp
    val Xxl = 24.dp
    val Xxxl = 32.dp

    /** 页面水平安全边距 */
    val PageHorizontal = 16.dp
    /** 分组 / 区块间距 */
    val Section = 24.dp
    /** BottomSheet 底部留白（含手势区） */
    val SheetBottom = 32.dp
}

object Corners {
    val Xs = RoundedCornerShape(8.dp)
    val Sm = RoundedCornerShape(12.dp)
    val Md = RoundedCornerShape(18.dp)
    val Lg = RoundedCornerShape(24.dp)
    val Xl = RoundedCornerShape(32.dp)
    /** BottomSheet / Modal 顶角 */
    val SheetTop = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
}

object SheetHeights {
    /** 设置类面板最小拉起高度（保证标题+正文+按钮完整可见） */
    val SettingsSheetMin = 520.dp
    /** 可滚动内容区上限 */
    val ScrollContentMax = 560.dp
}
