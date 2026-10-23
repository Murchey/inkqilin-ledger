package com.inkqilin.ledger.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.inkqilin.ledger.ui.motion.LocalShimmerProgress
import com.inkqilin.ledger.ui.motion.rememberShimmerProgress
import com.inkqilin.ledger.ui.motion.shimmer
import com.inkqilin.ledger.ui.theme.Corners
import com.inkqilin.ledger.ui.theme.InkQilinLedgerTheme
import com.inkqilin.ledger.ui.theme.Space

/**
 * 统计页首屏骨架。
 *
 * 统计数据需要「读库 → 聚合 → 建列表」三步才可渲染，首帧直接渲染真实内容会出现
 * 先空列表、后满列表的闪断，并让聚合逻辑跑两遍。这里在 [TransactionViewModel.dataLoaded]
 * 为 false 时整页只渲染骨架，聚合与列表构建整体后移一帧。
 *
 * 骨架块共享同一个 shimmer 进度（[rememberShimmerProgress]），只重绘不新建动画。
 */
@Composable
internal fun StatisticsLoadingSkeleton(modifier: Modifier = Modifier) {
    val progress = rememberShimmerProgress()
    CompositionLocalProvider(LocalShimmerProgress provides progress) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(top = Space.Sm)
        ) {
            // 周期标签（周 / 月 / 年 / 自定义）
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Space.Xl),
                horizontalArrangement = Arrangement.spacedBy(Space.Xs)
            ) {
                repeat(4) {
                    SkeletonBlock(
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp),
                        corner = 10.dp
                    )
                }
            }

            Spacer(Modifier.height(Space.Lg))

            // 支出 / 收入切换
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Space.Xl),
                horizontalArrangement = Arrangement.spacedBy(Space.Xs)
            ) {
                repeat(2) {
                    SkeletonBlock(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp),
                        corner = 12.dp
                    )
                }
            }

            Spacer(Modifier.height(Space.Xl))

            // 汇总卡（总额 / 环比）
            SkeletonCard(modifier = Modifier.height(132.dp))

            Spacer(Modifier.height(Space.Lg))

            // 图表卡（趋势 / 占比）
            SkeletonCard(modifier = Modifier.height(176.dp))

            Spacer(Modifier.height(Space.Lg))

            // 分类排行行
            Column(
                modifier = Modifier.padding(horizontal = Space.Xl),
                verticalArrangement = Arrangement.spacedBy(Space.Sm)
            ) {
                repeat(5) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .clip(Corners.Md)
                            .background(skeletonBase()),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SkeletonBlock(
                            modifier = Modifier
                                .padding(start = Space.Lg)
                                .size(40.dp),
                            corner = 12.dp
                        )
                        Spacer(Modifier.width(Space.Md))
                        Column(verticalArrangement = Arrangement.spacedBy(Space.Xs)) {
                            SkeletonBlock(
                                modifier = Modifier
                                    .width(96.dp)
                                    .height(14.dp),
                                corner = 4.dp
                            )
                            SkeletonBlock(
                                modifier = Modifier
                                    .width(56.dp)
                                    .height(10.dp),
                                corner = 4.dp
                            )
                        }
                        Spacer(Modifier.weight(1f))
                        SkeletonBlock(
                            modifier = Modifier
                                .padding(end = Space.Lg)
                                .width(72.dp)
                                .height(16.dp),
                            corner = 4.dp
                        )
                    }
                }
            }
        }
    }
}

/** 搜索 / 分类明细等列表页的首屏骨架：只铺列表项，标题区由页面自己渲染。 */
@Composable
internal fun TransactionListSkeleton(
    itemCount: Int = 6,
    modifier: Modifier = Modifier
) {
    val progress = rememberShimmerProgress()
    CompositionLocalProvider(LocalShimmerProgress provides progress) {
        Column(modifier = modifier.fillMaxSize()) {
            repeat(itemCount) {
                TransactionItemSkeleton()
            }
        }
    }
}

@Composable
private fun skeletonBase(): Color =
    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)

@Composable
private fun SkeletonCard(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Space.Xl)
            .clip(Corners.Lg)
            .background(skeletonBase())
            .shimmer()
    )
}

@Composable
private fun SkeletonBlock(modifier: Modifier = Modifier, corner: Dp = 8.dp) {
    val shape = RoundedCornerShape(corner)
    Box(
        modifier = modifier
            .clip(shape)
            .background(skeletonBase())
            .shimmer()
    )
}

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun StatisticsLoadingSkeletonPreview() {
    InkQilinLedgerTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            StatisticsLoadingSkeleton()
        }
    }
}

@Preview(showBackground = true, heightDp = 600)
@Composable
private fun TransactionListSkeletonPreview() {
    InkQilinLedgerTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            TransactionListSkeleton(itemCount = 5)
        }
    }
}
