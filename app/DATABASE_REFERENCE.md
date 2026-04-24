# Zenith Room Database Reference

This document describes the Room database elements currently wired through `AppDatabase`.

## 1) Database Overview

- Database class: `app/src/main/java/com/saadm/zenith/data/db/AppDatabase.kt`
- Room version: `1`
- `exportSchema`: `false`
- Registered `@TypeConverters`: `DbTypeConverters`
- Tables:
  - `transactions`
  - `payees`
  - `categories`
  - `accounts`
  - `budgets`

## 2) Type Mapping Rules

Room/Kotlin to SQLite storage mapping used in this project:

- `Long` -> `INTEGER`
- `Int` -> `INTEGER`
- `Boolean` -> `INTEGER` (`0`/`1`)
- `String` -> `TEXT`
- Enum values (`TxnType`, `AccountType`, `BudgetPeriod`) -> `TEXT` via converters (`enum.name`)

## 3) Enum and Converter Reference

Source files:
- `app/src/main/java/com/saadm/zenith/data/entity/DbEnums.kt`
- `app/src/main/java/com/saadm/zenith/data/db/converter/DbTypeConverters.kt`

### Enum values

- `TxnType`: `INCOME`, `EXPENSE`, `DUE_TO`, `DUE_FROM`
- `SettlementStatus`: `PENDING`, `PARTIAL`, `SETTLED` (currently not persisted in an entity)
- `AccountType`: `CASH`, `UPI`, `DEBIT_CARD`, `CREDIT_CARD`, `BANK_TRANSFER`
- `BudgetPeriod`: `MONTHLY`, `WEEKLY`, `CUSTOM`

### Converter behavior

- `TxnType <-> String`
  - write: `value?.name`
  - read: `TxnType.valueOf(value)`
- `AccountType <-> String`
  - write: `value?.name`
  - read: `AccountType.valueOf(value)`
- `BudgetPeriod <-> String`
  - write: `value?.name`
  - read: `BudgetPeriod.valueOf(value)`

## 4) Table Specifications

### `transactions`

Entity: `app/src/main/java/com/saadm/zenith/data/entity/TransactionEntity.kt`

| Field | Kotlin type | SQLite type | Nullable | Default | Notes |
|---|---|---|---|---|---|
| `id` | `Long` | `INTEGER` | No | `0` (auto-gen) | Primary key, `autoGenerate = true` |
| `amount` | `Long` | `INTEGER` | No | - | Money in smallest unit (paise/cents) |
| `type` | `TxnType` | `TEXT` | No | - | Uses converter |
| `transactedAt` | `Long` | `INTEGER` | No | - | Epoch millis |
| `createdAt` | `Long` | `INTEGER` | No | - | Epoch millis |
| `updatedAt` | `Long` | `INTEGER` | No | - | Epoch millis |
| `categoryId` | `Long?` | `INTEGER` | Yes | `null` | FK -> `categories.id` |
| `payeeId` | `Long?` | `INTEGER` | Yes | `null` | FK -> `payees.id` |
| `accountId` | `Long?` | `INTEGER` | Yes | `null` | FK -> `accounts.id` |
| `note` | `String?` | `TEXT` | Yes | `null` | Optional memo |
| `receiptUri` | `String?` | `TEXT` | Yes | `null` | Local URI |
| `currency` | `String` | `TEXT` | No | `"INR"` | ISO currency code |
| `isDeleted` | `Boolean` | `INTEGER` | No | `false` | Soft-delete flag |
| `deletedAt` | `Long?` | `INTEGER` | Yes | `null` | Soft-delete timestamp |

Foreign keys:
- `payeeId` -> `payees.id` (`onDelete = SET_NULL`)
- `accountId` -> `accounts.id` (`onDelete = SET_NULL`)
- `categoryId` -> `categories.id` (`onDelete = SET_NULL`)

Indices:
- `payeeId`
- `accountId`
- `categoryId`
- `transactedAt`
- `isDeleted`

### `payees`

Entity: `app/src/main/java/com/saadm/zenith/data/entity/PayeeEntity.kt`

| Field | Kotlin type | SQLite type | Nullable | Default | Notes |
|---|---|---|---|---|---|
| `id` | `Long` | `INTEGER` | No | `0` (auto-gen) | Primary key |
| `name` | `String` | `TEXT` | No | - | Display name |
| `avatarUri` | `String?` | `TEXT` | Yes | `null` | Optional photo URI |
| `phone` | `String?` | `TEXT` | Yes | `null` | Optional contact number |
| `upiId` | `String?` | `TEXT` | Yes | `null` | Optional UPI ID |
| `createdAt` | `Long` | `INTEGER` | No | - | Epoch millis |
| `isDeleted` | `Boolean` | `INTEGER` | No | `false` | Soft-delete flag |

### `categories`

Entity: `app/src/main/java/com/saadm/zenith/data/entity/CategoryEntity.kt`

| Field | Kotlin type | SQLite type | Nullable | Default | Notes |
|---|---|---|---|---|---|
| `id` | `Long` | `INTEGER` | No | `0` (auto-gen) | Primary key |
| `name` | `String` | `TEXT` | No | - | Category name |
| `emoji` | `String` | `TEXT` | No | - | Visual token |
| `colorHex` | `String` | `TEXT` | No | - | Hex color |
| `applicableTo` | `String` | `TEXT` | No | - | Expected values: `EXPENSE`, `INCOME`, `BOTH` |
| `sortOrder` | `Int` | `INTEGER` | No | - | User ordering |
| `isDefault` | `Boolean` | `INTEGER` | No | `false` | Seeded default marker |
| `isDeleted` | `Boolean` | `INTEGER` | No | `false` | Soft-delete flag |

### `accounts`

Entity: `app/src/main/java/com/saadm/zenith/data/entity/AccountEntity.kt`

| Field | Kotlin type | SQLite type | Nullable | Default | Notes |
|---|---|---|---|---|---|
| `id` | `Long` | `INTEGER` | No | `0` (auto-gen) | Primary key |
| `name` | `String` | `TEXT` | No | - | Account label |
| `type` | `AccountType` | `TEXT` | No | - | Uses converter |
| `colorHex` | `String` | `TEXT` | No | - | Hex color |
| `emoji` | `String` | `TEXT` | No | - | Visual token |
| `sortOrder` | `Int` | `INTEGER` | No | - | User ordering |
| `isDeleted` | `Boolean` | `INTEGER` | No | `false` | Soft-delete flag |

### `budgets`

Entity: `app/src/main/java/com/saadm/zenith/data/entity/BudgetEntity.kt`

| Field | Kotlin type | SQLite type | Nullable | Default | Notes |
|---|---|---|---|---|---|
| `id` | `Long` | `INTEGER` | No | `0` (auto-gen) | Primary key |
| `categoryId` | `Long?` | `INTEGER` | Yes | `null` | Nullable for overall budget; currently no FK annotation |
| `limitAmount` | `Long` | `INTEGER` | No | - | Money in smallest unit |
| `periodType` | `BudgetPeriod` | `TEXT` | No | - | Uses converter |
| `startDate` | `Long?` | `INTEGER` | Yes | `null` | Epoch millis (custom ranges) |
| `endDate` | `Long?` | `INTEGER` | Yes | `null` | Epoch millis (custom ranges) |
| `currency` | `String` | `TEXT` | No | `"INR"` | ISO currency code |
| `isActive` | `Boolean` | `INTEGER` | No | `true` | Active/inactive switch |

## 5) DAO Query Conventions

DAO files:
- `app/src/main/java/com/saadm/zenith/data/db/dao/TransactionDao.kt`
- `app/src/main/java/com/saadm/zenith/data/db/dao/PayeeDao.kt`
- `app/src/main/java/com/saadm/zenith/data/db/dao/CategoryDao.kt`
- `app/src/main/java/com/saadm/zenith/data/db/dao/AccountDao.kt`
- `app/src/main/java/com/saadm/zenith/data/db/dao/BudgetDao.kt`

Common behavior:
- `upsert(...)` uses `@Insert(onConflict = REPLACE)`.
- `update(...)` uses `@Update`.
- Query methods return `Flow<List<...>>`.

Active-row filtering:
- `transactions`: `WHERE isDeleted = 0 ORDER BY transactedAt DESC, id DESC`
- `payees`: `WHERE isDeleted = 0 ORDER BY name COLLATE NOCASE ASC`
- `categories`: `WHERE isDeleted = 0 ORDER BY sortOrder ASC, id ASC`
- `accounts`: `WHERE isDeleted = 0 ORDER BY sortOrder ASC, id ASC`
- `budgets`: `WHERE isActive = 1`

## 6) Referential and Lifecycle Notes

- Only `transactions` currently declares explicit foreign keys.
- `onDelete = SET_NULL` ensures parent deletion does not delete transactions.
- Soft delete is modeled via `isDeleted` and (for transactions) `deletedAt`.
- `budgets` currently uses `isActive` state instead of soft delete fields.
- `SettlementStatus` is available in enums but currently derived at query/domain level, not stored in a table.

