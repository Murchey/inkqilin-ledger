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

/**
 * 悬浮底部导航 token。
 *
 * 内容会延伸到屏幕底部，导航胶囊再覆盖到内容之上。因此列表只需要在末尾
 * 预留导航高度、导航与安全区之间的间距，以及最后一项与胶囊之间的额外间距。
 */
// 52dp keeps the icon/label stack compact while matching the 44dp FAB visually.
val floatingNavHeight = 52.dp
val floatingBottomGap = 8.dp
val floatingContentBottomInset = floatingNavHeight + floatingBottomGap + 16.dp
val floatingNavBlurSigma = 28.dp
val floatingNavDarkOpacity = 0.46f
val floatingNavLightOpacity = 0.60f
val floatingNavOutlineOpacity = 0.20f
val floatingNavHighlightOpacity = 0.06f
val floatingNavSelectedOpacity = 0.08f
val floatingNavSelectedCornerRadius = 13.dp
val floatingNavShadowElevation = 6.dp

/** 主题层使用的悬浮导航形状，避免选中态重新使用导航胶囊的圆角 token。 */
val floatingNavSelectedShape = RoundedCornerShape(floatingNavSelectedCornerRadius)

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
