# Zenith Room database

Database class: `src/main/java/com/saadm/zenith/data/db/AppDatabase.kt`, file `zenith.db`, version **8**.

The exact SQL of every table, index and view is in the exported schemas under
`schemas/com.saadm.zenith.data.db.AppDatabase/`. This page covers what the SQL does not say.

## Tables

| Table | Holds | Removal |
|---|---|---|
| `transactions` | Every money movement, in paise | Hard delete from the app |
| `payees` | People you share money with | `isDeleted` |
| `categories` | Spending and income categories; `emoji` stores the icon key | `isDeleted` |
| `accounts` | Cash, UPI, cards, bank; `openingBalance` in paise | `isDeleted` |
| `budgets` | One row per category per month; a null `categoryId` is the overall budget | `isActive` |

View `payee_balances` nets each payee's dues against settlements.

Foreign keys exist only on `transactions`: `payeeId` and `accountId` set null, `categoryId` restricts.

## Transaction types

`INCOME`, `EXPENSE`, `DUE_TO` (they owe you), `DUE_FROM` (you owe them), `SETTLEMENT_FROM` (they paid you),
`SETTLEMENT_TO` (you paid them), `TRANSFER_IN`, `TRANSFER_OUT`. Only income and expense count as such;
dues move no money, so they are left out of account balances.

A **split** has no column of its own: it is one `EXPENSE` and its `DUE_TO` rows written together,
sharing `createdAt` and `transactedAt`.

## Changing the schema

1. Raise `version` and write a `Migration` in `data/db/migrations/Migrations.kt`; register it in
   `DatabaseProvider`. Never use destructive migration.
2. Build, and commit the new JSON under `schemas/`.
3. Extend `MigrationTest`. A view must be created with exactly the SQL Room expects, trimmed.

| Migration | Change |
|---|---|
| 1 to 6 | Early development; in `AppDatabase`. No exported schemas, so not covered by tests |
| 6 to 7 | `payee_balances` nets settlements |
| 7 to 8 | `accounts.openingBalance` |
