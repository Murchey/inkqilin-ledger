# InkQilin Ledger

A personal finance Android app built with Jetpack Compose and Material 3. Supports dark/light themes, multi-currency management, bill tracking, category statistics, Excel import/export, and a social gift ledger.

> **中文文档请参阅 [README_CN.md](README_CN.md)**

## Features

### Core

- **Transaction Management** — Quickly add income/expense entries with custom amounts, notes, categories, and dates
- **Amount Keypad Arithmetic** — Edit and add-transaction amount input supports `+ − × ÷` and parentheses (e.g. `(20+5)×2`), with backspace, clear, and live preview
- **Multi-Currency Support** — Manage multiple currencies with customizable card colors, symbols, and names; switch default currency anytime
- **Monthly Overview Card** — Displays total income, expense, and net balance for the selected month with a year-month picker
- **7-Day Trend Chart** — Visual bar chart of daily spending over the past week
- **Category System** — Built-in categories for income and expenses; create custom categories with icons and colors
- **Bill Editing** — Swipe left on any transaction to reveal edit/delete options; modify amount, note, type, category, and date
- **Statistics** — View spending by week, month, year, or custom date range (inclusive of both start and end dates); drill down by category; period-over-period comparison with daily average
- **Asset Management** — Track assets across 8 types (real estate, stocks, funds, bonds, deposits, insurance, crypto, other); view totals and breakdowns by type
- **Search** — Search transactions by keyword

### AI & Lab Features

- **AI Financial Analysis** (Smart Mode only) — Automated daily analysis of spending patterns; displays a financial score (0-100) and up to 3 consumption alerts on the home screen
- **OCR Batch Recognition** — Upload bill images (batch supported) for AI vision model recognition; auto-extracts date, amount, category, and note for one-tap import
- **Separate API Configuration** — Independent API settings for OCR and analysis, with a one-tap sync button between them
- **Auto Bookkeeping** — Monitors Alipay, WeChat Pay, and UnionPay notifications; auto-parses and records transactions with deduplication
- **Smart Keyword Classification** — 50+ built-in keyword rules (e.g. "takeout" -> Dining, "Didi" -> Transport); add/edit/delete custom rules
- **Ad Filtering** — Automatically detects and filters non-transaction notifications (ads, marketing, coupons) from payment apps
- **Zero-Amount Filtering** — Ignores 0-amount notifications to avoid invalid records

### Social Gift Ledger (RenQing)

- **Event Recording** — Track weddings, funerals, birthdays, housewarming, graduations, baby showers, and more
- **Direction Tracking** — Mark gifts as "received" or "given"; auto-calculates net amounts
- **Contact Management** — Maintain a contact list with relationship categories (family, friend, colleague)
- **Tag System** — Custom tags (e.g. family, school, workplace) with icons and colors
- **Annual Dashboard** — Yearly income/expense/net summary with monthly breakdowns
- **Tag & Contact Analytics** — View gift distribution by tag and contact rankings
- **Excel Export** — Export gift records and contacts by time range

### Data Management & Backup

- **Flexible Export** — Export transactions by "All / This Year / Custom Range" for both ledgers
- **Excel Export** — One-tap export to Excel with custom save location
- **Template Download** — Download a pre-formatted template for bulk import
- **Smart Import** — Import from Excel; auto-creates missing categories
- **Local Backup** — Pack the Room database into a zip under the app-private directory; export to system storage, restore, or delete from history
- **Cloud Backup (Tencent COS)** — Private read/write COS: configure SecretId/SecretKey and bucket URL, then upload/list/restore/delete backups
- **Optional Password Encryption** — Backups can be encrypted with AES-256-GCM (key via PBKDF2-HmacSHA256); encrypted restores require the password
- **Hard Delete** — Local backups are overwritten before unlink; cloud deletes are verified as gone; home-screen bills use hard delete with SQLite `secure_delete` to reduce residual recovery from the DB file
- **Safe Exit After Restore** — After restore, a dialog offers one-tap force-close so the app reloads the new database

### General

- **App Mode Toggle** — Basic Mode (privacy-focused, local-only) and Smart Mode (AI-powered analysis, financial scoring, consumption alerts)
- **Theme Customization** — Light mode (#715CFF purple) / Dark mode (#51B4FF blue); follows system setting
- **Custom Colors** — Customize income/expense display colors across all views
- **Immersive UI** — Full-screen layout with transparent status and navigation bars
- **Update Checker** — Optional startup check against the latest Gitee Release, plus a manual “Check for updates” action in Settings; view changelog and download via Gitee / GitHub / proxy
- **Configurable Update Repos** — Point the update checker at any Gitee repository and set the GitHub download repository (`owner/repo` or full URL); both settings are persisted
- **Settings State Kept** — Expanded/collapsed settings sections survive navigation to secondary screens and back
- **About (Top-Level)** — Dedicated settings section with version info, Gitee/GitHub links, and a usage guide

## Tech Stack

| Technology | Purpose |
|------------|---------|
| Kotlin | Language |
| Jetpack Compose | Declarative UI framework |
| Material 3 | Design system |
| Room | Local database |
| Navigation Compose | Screen navigation |
| DataStore | Preferences storage |
| Apache POI | Excel read/write |
| OkHttp | Network requests (update check, AI API) |

## Requirements

- **Android Studio** Hedgehog (2023.1.1) or later
- **JDK** 8+
- **Android SDK** compileSdk 34
- **Minimum device** Android 8.0 (API 26)

## Quick Start

### 1. Clone

```bash
git clone <repo-url>
cd inkqilin-ledger
```

### 2. Build & Run

```bash
# Debug build
./gradlew assembleDebug

# Install to device
./gradlew installDebug
```

Or open the project in Android Studio and click **Run**.

> For users in China: the project includes Alibaba Cloud Maven mirror configuration in `settings.gradle.kts`.

## Theme Colors

| Mode | Color | Description |
|------|-------|-------------|
| Light | `#715CFF` | Purple theme |
| Dark | `#51B4FF` | Blue theme |

Income/expense display colors are customizable in Settings and apply globally to all transaction views and charts.

## Privacy Policy

> Full text in the app: **Settings → About → Privacy Policy** (also shown on first launch). Summary below matches the in-app policy.

**Who we are** — An open-source Android ledger (GPL-3.0). Source is public and auditable; no ad SDK, no analytics, no account system.

**Where data lives** — Transactions, social ledger, assets, categories, settings, and album photos stay **on your device** (app-private files / Room / DataStore). In **Basic Mode**, data never leaves the phone.

**What we collect** — **Nothing** is collected, uploaded, or analyzed on developer servers—no personal identity data and no ledger contents.

**Permissions & optional network** (all user-initiated; off means no network):

| Capability | Purpose |
|------------|---------|
| Camera / storage | System camera capture, gallery pick, Excel export |
| Notification listener | Auto-bookkeeping: parse Alipay/WeChat/UnionPay notifications into local entries; revoke anytime in system settings |
| Network | ① App update check (Gitee/GitHub) ② Smart-mode AI/OCR (**your** API endpoint) ③ Cloud backup (**your** COS keys & bucket) |

**AI & cloud backup** — Only when you enable and configure them; requests go to endpoints/buckets you own. Developers cannot access your keys or payloads. Backups support optional AES-256-GCM encryption.

**Your control** — Export / import / restore / delete backups; hard-deleted bills (`SQLite secure_delete`); uninstall removes app data.

**Contact** — File issues on the repos: [Gitee](https://gitee.com/Murchey/inkqinlin-ledger) · [GitHub](https://github.com/Murchey/inkqilin-ledger)

## Recent Updates

### 2026-09

- **Privacy policy** — First-launch disclosure dialog (remembered after accept); re-readable under Settings → About
- **HarmonyOS (ZhuoyiTong) album crash fix** — CameraX removed; pull-to-shoot uses the **system camera** only (avoids native abort on the compat layer); capture URI survives process death
- **Auto-bookkeeping on HarmonyOS** — Full Throwable guards; safe disable + “not supported” hint in compat mode
- **Album viewer** — Swipe left/right to change photos (system-gallery gesture), with page counter
- **Bottom nav bar** — Fixed disappearing forever after system back from album
- **Motion performance** — Lighter list entry, skeletons, nav transitions, tab indicator for low-end devices
- **Amount keypad** — Live result preview, `00` key, grouped operators, invalid input keeps keypad open
- **Edit-bill return** — Restores month + scroll position on the home list
- **Color picker** — Drag to pick value/hue
- **Home background sheet** — Height hugs content; action buttons visible on first open
- **Repo links** — GitHub unified to [github.com/Murchey/inkqilin-ledger](https://github.com/Murchey/inkqilin-ledger)
- **Home widgets** — Visual refresh (ink cards / segmented actions)

## License

This project is licensed under the [GNU General Public License v3.0](LICENSE).
