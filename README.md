# Zenith - Personal Finance Tracker

A native Android finance management app built with Kotlin and Jetpack Compose. Track income, expenses, and dues with designated payees. Designed for individuals and small groups (flatmates, friend circles) who need a clear picture of shared and personal money flow.

---

## Features

- **Four transaction types** - Income, Expense, Due-To (you owe), Due-From (they owe you)
- **Payee management** - Designated contacts with running net balances, settlement tracking, and transaction history per person
- **Settlement flow** - Record full or partial payments; net balance updates automatically with no manual bookkeeping
- **Analytics** - Income vs expense bar charts, category donut breakdown, spending trend line, monthly comparisons, and due balance overview
- **Budgets** - Set per-category or overall monthly/weekly limits; progress tracked in real time
- **Accounts** - Tag transactions to Cash, UPI, Debit, Credit, or Bank Transfer
- **Categories** - Default set with emoji + color; fully customizable and reorderable
- **Swipe actions** - Swipe right to edit, swipe left to delete (with Snackbar undo)
- **Month selector** - Horizontal scroll pill row synced to a paged transaction list
- **Offline first** - All data local by default; CSV/JSON export built in

---

## Tech Stack

| Layer | Choice |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Database | Room (SQLite) |
| Reactive state | Kotlin Coroutines + StateFlow + Flow |
| Dependency injection | Hilt |
| Navigation | Compose Navigation |
| Charts | Planned for v2: custom Compose Canvas charts (no chart library) |
| Animations | `Animatable`, `animateFloatAsState` |
| Reminders | Planned (WorkManager, not yet added) |

---

## Architecture

Clean Architecture with three layers inside a single `app` module:

```
ui/          → Compose screens + ViewModels (MVVM)
domain/      → Models, repository interfaces, use cases
data/        → Room entities, DAOs, repository implementations
di/          → Hilt modules wiring everything together
```

**Single source of truth:** Transactions are the only stored data. All derived values - payee net balances, budget consumption, monthly totals, settlement status - are computed via Room `@Query` and `@DatabaseView`, never stored as redundant fields. The `payee_balances` view recomputes automatically whenever the transactions table changes.

---

## Data Model

Five tables, one database view.

| Table | Purpose |
|---|---|
| `transactions` | Every financial event (income, expense, due, settlement) |
| `payees` | Named contacts linked to due/settlement transactions |
| `categories` | Emoji + color tags applied to transactions |
| `accounts` | Payment methods (cash, UPI, card, bank) |
| `budgets` | Spending limits per category or overall |
| `payee_balances` *(view)* | Derived net balance per payee from transaction aggregation |

**Amount storage:** All monetary values stored as `Long` in paise (smallest INR unit). Never `Float` or `Double`. Displayed by dividing by 100 via a `Money` value class.

**Transaction types:**

```kotlin
enum class TxnType {
    INCOME, EXPENSE,
    DUE_FROM,        // someone owes you  → +payee balance
    DUE_TO,          // you owe someone   → -payee balance
    SETTLEMENT_FROM, // they paid you     → reduces +balance
    SETTLEMENT_TO    // you paid them     → reduces -balance
}
```

**Payee balance formula:**

```sql
SUM(CASE
    WHEN type IN ('DUE_FROM', 'SETTLEMENT_TO')   THEN  amount
    WHEN type IN ('DUE_TO',   'SETTLEMENT_FROM') THEN -amount
    ELSE 0
END)
-- result > 0: they owe you | < 0: you owe them | = 0: settled
```

---

## Project Structure

```
app/src/main/
├── data/
│   ├── db/
│   │   ├── AppDatabase.kt
│   │   ├── DatabaseProvider.kt
│   │   ├── dao/
│   │   │    ├── TransactionDao.kt
│   │   │    ├── PayeeDao.kt
│   │   │    ├── CategoryDao.kt
│   │   │    ├── AccountDao.kt
│   │   │    └── BudgetDao.kt
│   │   └── converter/
│   │       └── DbTypeConverters.kt
│   ├── preferences/
│   │   └── AppPreferencesStore.kt
│   └── entity/
│       ├── DbEnums.kt
│       ├── TransactionEntity.kt
│       ├── PayeeEntity.kt
│       ├── CategoryEntity.kt
│       ├── AccountEntity.kt
│       └── BudgetEntity.kt
├── domain/
│   ├── model/
│   │   ├── Transaction.kt
│   │   ├── Payee.kt
│   │   └── Money.kt
│   ├── repository/
│   │   ├── TransactionRepo.kt
│   │   └── PayeeRepo.kt
│   └── usecase/
│       ├── AddTransactionUC.kt
│       └── GetPayeeBalance.kt
├── ui/
│   ├── home/
│   ├── transactions/
│   ├── add/
│   ├── people/
│   ├── analytics/
│   ├── settings/
│   ├── components/
│   │   ├── NavPillBar.kt
│   │   ├── PrimaryActionButton.kt
│   │   ├── TxnRow.kt
│   │   ├── MonthSelector.kt
│   │   ├── AmountDisplay.kt
│   │   └── CategoryPicker.kt
│   └── theme/
│       ├── Color.kt
│       ├── Theme.kt
│       └── Type.kt
├── di/
│   ├── DatabaseModule.kt
│   └── RepositoryModule.kt
└── MainActivity.kt
```

---

## Key Design Decisions

**No soft deletes.** Deleted transactions are hard-deleted immediately. Undo is handled in the ViewModel via a short-lived `recentlyDeleted` reference backed by a Snackbar timer. Keeps all queries clean with no `WHERE isDeleted = 0` boilerplate.

**No `linkedTxnId` in balance math.** The optional `linkedTxnId` field on a transaction is UI-only context (e.g. "this settlement pays off that due from Tuesday"). It is never used in any balance calculation. All payee math is pure aggregation.

**Rolling payee balance, not invoice matching.** Dues and settlements contribute a signed amount to a payee's running balance. There is no concept of "closing" a specific due transaction. This handles partial payments and multiple concurrent dues cleanly without any stored mutable state.

**Amounts always positive.** Sign is encoded entirely in `TxnType`, never in the `amount` field. This prevents double-negation bugs and keeps queries straightforward.

---

## Screens

| Screen             | Route                                                                                  |
|--------------------|----------------------------------------------------------------------------------------|
| Home (Tab)         | Dashboard - net balance, pending dues summary, recent transactions, spending sparkline |
| Transactions (Tab) | Full ledger - filtered by type, grouped by date with day totals, month selector        |
| People (Tab)       | Payee grid with net balances - tap to view shared history and settle up                |
| Add Transaction    | Number pad entry - form morphs based on type (payee picker appears only for due types) |
| Analytics (Tab)    | Charts - bar, donut, trend line, category ranking, due breakdown                       |
| Settings           | Profile, categories, accounts, budgets, data export                                    |

Navigation: bottom bar with 3 tabs. Add is in the PrimaryActionButton, on all screens except People. 

[//]: # (Settings screen is accessed via the Home screen header icon, not the nav bar.)

---

## Getting Started

### Prerequisites

- Android Studio (recent stable release)
- Kotlin 2.2
- Minimum SDK 31 (Android 12)
- Compile SDK 36 (target SDK is not set yet)

### Setup

```bash
git clone https://github.com/saad-mh/zenith_kotlin.git
cd zenith_kotlin
```

Open the `zenith_kotlin` project in Android Studio, let Gradle sync, and run on a device or emulator (Android 12+).

No API keys, no network configuration required - the app is fully offline.

### Build

```bash
# Debug build
./gradlew assembleDebug

# Release build (requires signing config)
./gradlew assembleRelease
```

---

## Roadmap

- [ ] Google Drive backup (opt-in)
- [ ] Multi-device sync via (???)
- [ ] Recurring transaction templates
- [ ] Widget - home screen balance summary
- [ ] UPI deep link on payee settle-up (im stupid don't expect all these)
- [ ] Multi-currency support

---

## License

(I'll think about it)