# People (Payees) Module Implementation

## Overview

The People module implements a lightweight, backend-first payee management system for the Zenith finance app. It follows clean architecture principles by:

1. **Keeping PayeeEntity minimal** — Only storing generic identity fields (name, phone, UPI ID, avatar URI, timestamps).
2. **Computing all summaries dynamically** — Financial data (outstanding dues, net balance, interaction history) is derived from the transactions dataset via a Room `@DatabaseView`.
3. **Maintaining a single source of truth** — No redundant aggregates stored on PayeeEntity; summaries recompute automatically when transactions change.

## Architecture

```
ui/people/
  ├── PeopleScreen.kt           (top-level screen, if navigated to)
  ├── PeopleManagementContent.kt (composable for Settings cascade)
  ├── PayeeEditorDialog.kt       (create/edit dialog)
  └── PeopleViewModel.kt         (ViewModel for complex state, optional)

domain/
  ├── model/Payee.kt            (domain model with derived fields)
  ├── repository/PayeeRepo.kt    (interface)
  └── usecase/PayeeUseCases.kt   (GetPayees, GetPayeeSummary)

data/
  ├── entity/
  │   ├── PayeeEntity.kt         (minimal stored entity)
  │   └── PayeeBalanceView.kt    (@DatabaseView computing summaries)
  ├── db/dao/
  │   ├── PayeeDao.kt            (CRUD ops on PayeeEntity)
  │   └── PayeeBalanceDao.kt     (query view for summaries)
  └── repository/
      └── PayeeRepoImpl.kt        (implementation combining entity + view)

di/
  └── RepositoryModule.kt        (Hilt bindings)
```

## Key Files

### `PayeeEntity.kt`
Lightweight entity storing only identity info:
```kotlin
@Entity(tableName = "payees")
data class PayeeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val avatarUri: String?,
    val phone: String?,
    val upiId: String?,
    val createdAt: Long,
    val isDeleted: Boolean = false
)
```

### `PayeeBalanceView.kt`
Room `@DatabaseView` that computes financial summaries in SQL:
```sql
SELECT
    p.id, p.name, ... (payee fields) ...
    SUM(CASE WHEN t.type = 'DUE_TO' THEN t.amount ELSE 0 END) as dueToAmount,
    SUM(CASE WHEN t.type = 'DUE_FROM' THEN t.amount ELSE 0 END) as dueFromAmount,
    COUNT(DISTINCT t.id) as transactionCount,
    MAX(t.transactedAt) as lastInteractionAt
FROM payees p
LEFT JOIN transactions t ON p.id = t.payeeId
GROUP BY p.id
```

### `PayeeRepo` Interface
Repository API abstraction:
```kotlin
interface PayeeRepo {
    fun observeAllActive(): Flow<List<Payee>>      // all payees with summaries
    fun observeById(id: Long): Flow<Payee?>        // single payee with summary
    suspend fun upsert(payee: PayeeEntity): Long   // insert or update
    suspend fun update(payee: PayeeEntity)
    suspend fun softDelete(id: Long)               // set isDeleted = 1
}
```

### `Payee` Domain Model
Combines entity + derived fields:
```kotlin
data class Payee(
    val id: Long,
    val name: String,
    val avatarUri: String?,
    val phone: String?,
    val upiId: String?,
    val createdAt: Long,
    val dueToAmount: Long,      // derived
    val dueFromAmount: Long,    // derived
    val netBalance: Long,       // derived
    val transactionCount: Int,  // derived
    val lastInteractionAt: Long?  // derived
)
```

## Integration Points

### 1. Settings Navigation
`SettingsScreen.kt` now includes a "People" entry in the Data section:
```kotlin
SettingsDestination.People -> PeopleManagementContent()
```

### 2. Hilt DI
`RepositoryModule.kt` binds `PayeeRepoImpl` to the `PayeeRepo` interface:
```kotlin
@Provides
@Singleton
fun providePayeeRepo(payeeDao: PayeeDao, payeeBalanceDao: PayeeBalanceDao): PayeeRepo
    = PayeeRepoImpl(payeeDao, payeeBalanceDao)
```

### 3. Application Class
`ZenithApplication.kt` enables Hilt:
```kotlin
@HiltAndroidApp
class ZenithApplication : Application()
```

Register in `AndroidManifest.xml`:
```xml
<application android:name=".ZenithApplication" ...>
```

## Usage Examples

### Fetching All Payees
```kotlin
val payees: List<Payee> = repo.observeAllActive()
    .stateIn(scope, SharingStarted.WhileSubscribed(), emptyList())
    .value
```

### Creating a Payee
```kotlin
val newPayee = PayeeEntity(name = "Alice", phone = "+91...", createdAt = now())
val newId = repo.upsert(newPayee)
```

### Deleting a Payee
```kotlin
repo.softDelete(payeeId)  // Sets isDeleted = 1; transaction history preserved
```

### Checking Balance
```kotlin
val payee: Payee = repo.getById(payeeId)
if (payee.netBalance > 0) {
    println("${payee.name} owes ${payee.balanceText()}")
} else {
    println("You owe ${payee.name} ${payee.balanceText()}")
}
```

## UI Components

### `PeopleManagementContent()`
Composable displayed in Settings under Data → People. Features:
- **List** all payees with identity info + derived balance badge.
- **Create** new payee via `PayeeEditorDialog`.
- **Edit** payee info (non-balance fields).
- **Delete** (soft-delete) payee.

### `PayeeRow()`
Card displaying:
- Payee name (primary)
- Phone / UPI / interaction count (secondary)
- Balance badge (if owes/owed; color: red for owing, purple for owed)

### `PayeeEditorDialog()`
Dialog with fields:
- Name (required)
- Phone (optional)
- UPI ID (optional)
- Avatar URI (optional)

## Testing

Unit tests in `PayeeRepoTest.kt` verify:
- ✅ Fetching all payees with correct balance computation
- ✅ Detection of settled vs. active balances
- ✅ Correct net balance direction (positive = payee owes, negative = user owes)

### Run Tests
```bash
./gradlew test  # or IDE test runner
```

## Future Enhancements

1. **Avatar image picker** — Use system gallery or camera to set avatars.
2. **Full People screen** — Promote from Settings cascade to top-level nav tab.
3. **Settlement tracking** — Log settlement marks on transactions (optional FK to settlement records).
4. **Search/filter** — Find payees by name or outstanding balance.
5. **Export payee history** — Generate statements per payee.
6. **Notifications** — Remind about outstanding dues.

## Design Decisions

### Why @DatabaseView for summaries?
- **Automatic consistency**: View recomputes whenever transactions change.
- **SQL-level correctness**: Aggregations happen in the database, no app-side rounding errors.
- **Performance**: Indexes on `payeeId` and `transactedAt` speed up joins.
- **Simplicity**: No need for manual cache invalidation.

### Why soft-delete?
- Preserves transaction history even if payee is removed.
- Allows "undo" if needed (set `isDeleted = 0`).
- Complies with audit requirements (data not actually deleted).

### Why no stored aggregates on PayeeEntity?
- **Single source of truth**: Only transactions exist; all else is derived.
- **Prevents inconsistency**: No risk of stale balance data.
- **Simpler mutations**: Only CRUD payee identity; balance updates are automatic.

## Known Limitations

1. **No offline support**: Balance views require DB access; no cached snapshots.
2. **No real-time graph**: Transaction history shown as count only; detailed timeline in transactions screen.
3. **No avatar display**: URI stored but not rendered (UI component ready for future image loader).

## Files Created/Modified

**Created:**
- `PayeeBalanceView.kt` — @DatabaseView
- `PayeeBalanceDao.kt` — DAO for view queries
- `PayeeRepoImpl.kt` — Repo implementation
- `RepositoryModule.kt` — Hilt DI
- `PeopleManagementContent.kt` — UI composable
- `PayeeEditorDialog.kt` — Editor dialog
- `PayeeUseCases.kt` — Use cases
- `PayeeRepoTest.kt` — Unit tests
- `ZenithApplication.kt` — Hilt app class

**Modified:**
- `PayeeDao.kt` — Added queries for single payee fetch, soft-delete
- `PayeeRepo.kt` (domain) — Implemented interface
- `Payee.kt` (domain model) — Added derived fields & helpers
- `AppDatabase.kt` — Registered PayeeBalanceView and PayeeBalanceDao
- `SettingsScreen.kt` — Added People navigation
- `AndroidManifest.xml` — Registered ZenithApplication

---

**Questions? Integration issues?** Check the test suite or feel free to refine the implementation.

