---
feature: animation-performance
status: delivered
updated: 2026-09-09
branch: main
commits: cf5505e..WORKINGTREE # 未提交；base=cf5505e，范围以最终 git diff 为准
---

# 动画卡顿代码级优化

## Report

**What was built** — 以代码级优化消除低端机上的动画卡顿（不做「减少动画」开关）：
1. **转场**：`MotionSprings.appearance()` 改为近临界阻尼中刚度弹簧；NavHost 四向转场改用 `appearanceTween()`（280ms FastOutSlowIn），反向位移收至 `it/4`。
2. **列表入场**：`staggeredAppearance` 只对**整表**前 8 条、延迟 ≤240ms、单 tween 220ms；`hasPlayedListEntry`（rememberSaveable）保证二级页返回不重放；滚动/其余条目短路为零成本。
3. **骨架 shimmer**：全局共享 1 条 InfiniteTransition（`LocalShimmerProgress`），`drawWithCache` 缓存渐变只做 translate；骨架占位收敛为少量块。
4. **Tab 栏**：指示器仅在测量值变化时写 state（打断 onGloballyPositioned→recompose 回环）；去掉 `shadowElevation`/`drawBehind` 自绘，改静态 background+border；图标色不再逐 tab 弹簧。
5. **Pager**：`beyondBoundsPageCount=0`；TopBar 标题改短 fade 无纵向位移。
6. **pressScale**：新增无 composed 的 `Float`/`State<Float>` 重载；FAB 外提 `State` 并在 `graphicsLayer` 内读值，避免逐帧重组。
7. **P2**：Album 展开阴影常量化 8.dp；Statistics 低刚度弹簧改 tween。

**Verification** —
- `./gradlew.bat :app:compileDebugKotlin` → **PASS**（BUILD SUCCESSFUL）
- 独立审查第一轮：2 个 critical（stagger 用组内 index；FAB 组合期 `by` 读 State）→ 已修
- 独立审查复核：Critical 1/2 **FIXED**，无新 critical

**Journey log** —
- 首版 shimmer/pressScale 误用未完成的 `Modifier.Node` API，回退为「短路 + composed 仅热路径」的保守实现。
- 审查指出 `forEachIndexed` 的 `index` 在每日分组重置，stagger 必须用跨分组 `globalTxIndex`。
- `val x by animateFloatAsState` 后再传 Float 给 graphicsLayer **不能**避免重组，必须持 `State` 并在绘制 lambda 内读。
- HomeScreen 同期有并发背景图/卡透明度改动，ComposeposionLocalProvider 包裹时需对齐现有大括号层级。

## [S1] Problem

在部分机型与处理器较差的手机上，页面切换、首页账单列表入场、Tab 滑动、骨架屏等动画出现明显卡顿。

经代码排查，**确认为代码级性能问题**，主要热点集中在组合（recomposition）、无关键帧的弹簧动画、无限循环动画与昂贵图层上。不需要「减少动画」设置也能显著改善；本方案以代码优化为主。

### 检测结论（热点清单）

| 优先级 | 位置 | 问题 | 影响 |
|---|---|---|---|
| P0 | `MotionSystem.staggeredAppearance` | 每条账单 `delay(index*50ms)` + 两个 `StiffnessLow` 弹簧 + `graphicsLayer`；`Modifier.composed` 额外分配 | 首页列表入场/回返回时长串动画，低端机掉帧主因 |
| P0 | `MotionSystem.shimmer` | 每个骨架块独立 `rememberInfiniteTransition` + 动画渐变 + `onGloballyPositioned` | 首页骨架 15+ 同时跑无限动画 |
| P0 | `MotionSprings.appearance()` | `DampingRatioLowBouncy` + `StiffnessLow` 用于 NavHost 进出场 | 转场拖 400–800ms，与页面内容加载叠加 |
| P0 | `HomeScreen` 账单项 | 每项 `remember + LaunchedEffect + staggered` 三层状态 | 列表滚动/recompose 时成倍放大 |
| P1 | `MainScreen` Tab 指示器 | 每个 tab `onGloballyPositioned` 写共享 state → 整条 Tab 行重组；每 tab `animateColorAsState` | 滑动/切页持续重组 |
| P1 | `MainScreen` Tab/FAB | `graphicsLayer.shadowElevation` + `drawBehind` 自绘 | 低端机阴影渲染贵 |
| P1 | `Modifier.pressScale` | 全局 10+ 处 `Modifier.composed` | 每次组合分配、复用差 |
| P2 | `HorizontalPager` | `beyondBoundsPageCount = 1` | 常驻多一页完整屏幕组合 |
| P2 | `AlbumScreen` | 动画 `shadow(elevation)` + 多 `Animatable` 并行 | 展开/收起卡顿 |
| P2 | `StatisticsScreen` 图表 | 多条 spring 进度 + 百分比条动画 | 首次进入统计卡 |
| P2 | TopBar `AnimatedContent` | 标题每次路由切换都跑转场 | 转场叠加、观感顿 |

`frostedGlass` 的 `blur` 已注释，当前只剩背景+描边，**不作为热点**。

## [S2] Design

### 目标

1. 首页账单列表入场、页面导航转场、Tab 切换在中低端机上稳定 60fps（以「不明显掉帧」为准）。
2. **保持现有视觉语言**（毛玻璃卡片、iOS 风格 Tab、弹性反馈），仅去掉「无效且贵」的动画负载。
3. **不做**「减少动画」开关；所有优化对用户透明。

### 合约

#### A. 统一动效入口 `MotionSystem`

| 符号 | 新行为 |
|---|---|
| `MotionSprings.appearance()` | `spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessMedium)`，禁止 `StiffnessLow + LowBouncy` 做全屏转场。 |
| `MotionSprings.appearanceTween()` | `tween(280, FastOutSlowIn)`，NavHost/顶底栏优先使用。 |
| `MotionSprings.interactive()` | 保持 `MediumBouncy + Medium`。 |
| `Modifier.staggeredAppearance` | 见 B。 |
| `Modifier.shimmer` | 见 C。 |
| `Modifier.pressScale` | 提供 `Float` / `State<Float>` 无 composed 重载；`State.value` 仅在 `graphicsLayer` 内读取。 |

#### B. 列表入场 `staggeredAppearance`

- **只在屏幕首次进入时**对**整表前 N=8 条**（跨日分组的全局序号）做 stagger；其余直接显示。
- 单条 delay `min(index*48ms, 240ms)`；单 `tween(220, FastOutSlowIn)`；单 `graphicsLayer`。
- 二级页返回不重放：`hasPlayedListEntry`（rememberSaveable）闸门。
- 非动画路径短路 `return this`，零 composed。

#### C. 骨架屏 `shimmer`

- `LocalShimmerProgress` + `rememberShimmerProgress()` 全局共享 1 条 InfiniteTransition。
- `drawWithCache` 缓存 Brush，每帧只 `translate`；size 变化才重建。
- 首页骨架占位收敛（overview / trend / list-item 各少量块）。

#### D. Tab 指示器（MainScreen）

- `onGloballyPositioned` 仅在 `indicatorCenterX`/`indicatorWidth` **变化时**写 state。
- 图标色直接取值，不做逐 tab `animateColorAsState`。
- Tab/FAB 去掉 `shadowElevation`，改静态 `background + border`。
- 指示器位移改 `tween(FAST)`。

#### E. 转场与 Pager

- NavHost 四向转场使用 `appearanceTween()`；反向位移收至 `it/4`。
- `beyondBoundsPageCount = 0`。
- TopBar 标题 `fadeIn/fadeOut(tween(FAST))`，无纵向位移。

#### F. 次要热点（P2）

- `AlbumScreen`：展开过程 `shadow(elevation)` 常量化（8.dp）。
- `StatisticsScreen`：低刚度弹簧改 tween。
- 账单列表稳定 key 保持/补齐（`tx_${id}` 等）。

#### G. 易测性

- 常量集中在 `MotionStagger`（`VISIBLE_COUNT` / `DELAY_CAP_MS` / `DURATION_MS`）。

### 架构示意

```text
MotionSystem (spec 收紧 + 短路 composed)
    ├── staggeredAppearance  → 全局前 8 条 / 240ms 封顶 / 单 tween
    ├── shimmer              → 共享 InfiniteTransition + drawWithCache
    └── pressScale           → State 在 graphicsLayer 内读
MainScreen
    ├── NavHost transitions  → appearanceTween
    ├── Tab indicator        → 变化才写 state；无阴影
    └── Pager                → beyondBoundsPageCount=0
HomeScreen / Statistics / Album  → P2 热点 + 列表 key
```

## [S3] Out of Scope

- 不做「减少动画 / 性能模式」设置项或 `Settings.Global` 读取。
- 不改业务逻辑、导航结构、Widget、数据层。
- 不重写 `AlbumScreen` 交互本身（只降动画成本）。
- 不做基准测试框架 / CI 性能门禁。
- 不替换 Material / Compose 版本。

## Tasks

- [x] T1: 收紧 `MotionSprings.appearance()` 并统一转场 spec — acceptance: NavHost 四向转场不再使用 `StiffnessLow+LowBouncy`；编译通过 (covers: S2.A, S2.E)
- [x] T2: 重写 `staggeredAppearance` 为首次入场、有上限、单 tween、全局序号 — acceptance: 滚动/返回不重放入场；整表 `index>=8` 无动画；延迟 ≤240ms (covers: S2.B; depends: T1)
- [x] T3: 重写 `shimmer` 为共享 InfiniteTransition + drawWithCache，骨架占位收敛 — acceptance: 全局至多 1 个 infinite transition；骨架块收敛 (covers: S2.C)
- [x] T4: `pressScale` 去 composed 热点路径（State 重载 + FAB 外提）— acceptance: FAB 按压不在组合期读动画值 (covers: S2.A)
- [x] T5: Tab 指示器去 onGloballyPositioned 回环、去阴影 elevation — acceptance: 值变化才写 state；无 `shadowElevation` (covers: S2.D)
- [x] T6: Pager `beyondBoundsPageCount=0` + TopBar 标题短 fade — acceptance: 不预组合邻页；标题切换无纵向位移 (covers: S2.E; depends: T1)
- [x] T7: P2 热点（Album 阴影、Statistics 弹簧进度）— acceptance: 对应 spec 替换完成，编译通过 (covers: S2.F; depends: T1)
- [x] T8: 编译 + 审查回归 — acceptance: `compileDebugKotlin` 通过；critical 修复后复审通过 (covers: S2.G; depends: T1..T7)
