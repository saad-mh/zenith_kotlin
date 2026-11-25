<!--
Format read by scripts/create-v2-issues.ps1
  ## [ID] Title
  Labels: comma, separated
  <markdown body, may reference other issues as [[ID]]>
Design canvas: https://claude.ai/code/artifact/c611c1a9-7edf-48e1-86be-875a57f1141b
Plan: docs/v2/PLAN.md
-->

## [F1] Build hygiene: clean Gradle config and repo noise before the revamp
Labels: v2, phase:0-foundation, tech-debt

**Why:** v2 adds fonts, charts and tests. A messy build file makes every later PR harder to review.

**Scope**
- Remove the duplicate Hilt entry (`hilt-android-v2571` listed twice, two Hilt versions) and keep one.
- Replace the `x.x.x` mockito-kotlin version with a real one, or drop it if unused.
- Remove the Compose M2 `androidx.compose.material:material` dependency if nothing uses it; stay on Material 3.
- Remove duplicate `material3` declarations from `libs.versions.toml`.
- Delete `app/src/main/java/com/saadm/zenith/exp.txt`.
- Stop tracking `.idea/` and `build_log.txt`; add them to `.gitignore`.
- Make `README.md` truthful: remove libraries and packages that don't exist (Vico, MPAndroidChart, Lottie, `navigation/` package) or mark them as planned.
- Add a short `CLAUDE.md` / contributor note with build and test commands.

**Done when**
- `./gradlew assembleDebug testDebugUnitTest` passes.
- `git ls-files` contains no `.idea/` or `build_log.txt`.

## [F2] Theme: Zenith colour tokens for Light, Dark and Monochrome
Labels: v2, phase:0-foundation, design-system, ui

**Why:** The theme is the Android Studio default purple set. The whole look of v2 comes from tokens.

**Scope** (package `ui/theme`)
- Define the token set from the design: `surface`, `surfaceRaised`, `surfaceSunken`, `border`, `ink`, `inkMuted`, `primary`, `primaryStrong`, `focusRing`, `goldAccent`, `positive`, `negative`, `caution`, `link`, `onStrong`, `scrim`.
- Define avatar tints `t1..t4` (bg + fg) and chart colours `c1..c6` for each theme.
- Expose them through a `ZenithColors` `CompositionLocal` and map the core ones onto the M3 `ColorScheme` so stock components still look right.
- Three palettes: Light (values in `docs/v2/PLAN.md` §2), Dark (derived values in the canvas `zenith.css`), Monochrome (one ink, no hue).
- Brand palette is the default. Dynamic colour is opt-in only (see decision 4 in the plan).
- A `ZenithTheme(mode, palette)` entry point replacing the current `ZenithTheme(darkTheme, dynamicColor)`.

**Done when**
- All three palettes render in a single `@Preview` swatch sheet.
- Every text/background pair used in the design passes 4.5:1 (use a unit test that checks the pairs).

Blocks: [[F3]], [[F4]], [[F5]]

## [F3] Theme: Fraunces + Work Sans typography, shapes and spacing tokens
Labels: v2, phase:0-foundation, design-system, ui

**Scope**
- Add Fraunces (500, 600) and Work Sans (400, 500, 600) as bundled font resources (OFL licensed; add licence notice for the Open-source licences screen).
- Build the type scale: display 40/46, heading 24/30, subheading 18/26, body 15/22, body-strong, numeric (tabular + lining figures via `fontFeatureSettings = "tnum, lnum"`), caption 12/16 muted.
- `Shapes`: small 4dp, medium 6dp (controls), large 12dp (cards, sheets).
- `ZenithSpacing` tokens 4 / 8 / 12 / 16 / 24.
- Wire all of it into `ZenithTheme`.

**Done when**
- A type specimen `@Preview` matches the canvas.
- Numbers in a column align (tabular figures verified in preview).

Depends on: [[F2]]

## [F4] Appearance preferences: theme mode and colour palette in DataStore
Labels: v2, phase:0-foundation, settings, data

**Scope**
- Extend `AppPreferencesStore` with `themeMode` (System / Light / Dark) and `palette` (Colour / Monochrome).
- `MainActivity` collects both and passes them to `ZenithTheme`; no flash of wrong theme on cold start.
- Keep existing transition style/duration preferences working.

**Done when**
- Changing the setting re-themes the app live and survives process death.
- Unit test for the store's defaults and round trip.

Depends on: [[F2]]

## [F5] Component kit: the shared building blocks from the design
Labels: v2, phase:0-foundation, design-system, ui

**Why:** Every screen in the canvas is built from the same ~15 pieces. Build them once.

**Scope** (package `ui/components`, each with light/dark/mono previews)
- `ZenithCard` (raised surface, 1dp border, 12dp radius)
- `PrimaryButton` (min 48dp), `SecondaryButton` (min 44dp), text button
- `FilterChip` (44dp, on/off), `SegmentedControl`, `ZenithSwitch`
- `StatusTag` (icon + word, never colour alone)
- `Avatar` (sizes s/m/xl, tint `t1..t4`) and `AvatarStack`
- `ProgressTrack` with optional even-pace tick (used by budgets)
- `SectionHeader` with trailing link, `ListRow` (leading, title, subtitle, trailing amount + caption)
- `MonthSwitcher` pill (prev / "Oct 2026" / next, next disabled for the current month) — evolves the existing `MonthSelector`
- `ZenithBottomSheet` scaffold (grab handle, 12dp top radius, scrim)
- `EmptyState` (title, body, example row, primary action)
- `AmountText` (₹, muted cents, sign, colour + sign + word; uses [[F8]])
- Rework existing `NumberPad`, `TxnRow`, `AmountDisplay`, `CategoryPicker` to the new look.

**Done when**
- A "component gallery" debug screen or preview page shows every component in all three palettes.
- Every interactive component is at least 44dp and has a role/label for TalkBack.

Depends on: [[F2]], [[F3]], [[F8]]

## [F6] Navigation: floating pill bar, FAB, and a real route graph
Labels: v2, phase:0-foundation, navigation, ui

**Scope**
- Rebuild `NavPillBar` to match the design: 64dp tall, fully rounded, 16dp side margin, 3 tabs (Overview, Insights, Settings) + circular 48dp FAB for Add transaction. The active tab shows icon + label on a tinted pill; inactive tabs are icon only with content descriptions.
- Move the `NavHost` out of `MainActivity.kt` into a `ui/navigation` package with typed routes.
- Routes to register (screens land in later issues): Overview, Insights, Settings, Budgets, Transactions, Payees, PayeeDetails/{id}, Categories, plus the Add sheet.
- Keep the user-selectable transition style/duration from preferences.
- Back stack rules: the bottom bar hides on full-screen flows (Payee details, Add), shows on top-level tabs and Insights sub-pages.
- Replace the stubbed `println` on "add payee" with a real action.

**Done when**
- Every route is reachable from the UI, and deep-back behaves (Budgets → back → Insights).
- The commented-out Transactions route is live.

Depends on: [[F5]]

## [F7] Chart kit: Compose Canvas charts from the chart sheet
Labels: v2, phase:0-foundation, design-system, ui

**Why:** README lists chart libraries that were never added. The design's charts are simple and custom (average lines, projection outline, pace ticks), so draw them with Canvas and avoid a dependency.

**Scope** (package `ui/components/charts`)
- `MonthlyBars` (current month highlighted, dashed average line, dashed projection outline for the partial month)
- `RankedBars`, `StackedBars` (max 4 segments), `GroupedBars`, `NetFlowBars` (around zero, sign always shown)
- `CumulativeLine` (this month vs last month, dashed comparison, end marker) and the compact sparkline variant used on Overview
- `ShareBar` (segmented, 2dp gaps) with legend rows
- `Donut` (centre total), `Gauge` with pace tick, `BudgetRings` (cap at full, label carries the overspend), `SplitPie`
- All charts take plain data classes, use palette tokens `c1..c6`, and expose a `contentDescription` summarising the data for TalkBack.

**Done when**
- Each chart has a preview in all three palettes. In Monochrome the series stay distinguishable (lightness steps, labels).
- Charts don't depend on any specific screen's ViewModel.

Depends on: [[F2]], [[F3]]

## [F8] Money formatting: ₹ with Indian digit grouping, true minus, muted cents
Labels: v2, phase:0-foundation, domain

**Scope**
- One `MoneyFormatter` in `domain` or `util`: `₹1,07,070`, `−₹420` (U+2212, not hyphen), `+₹12,000`, compact `24.3k`.
- Cents hidden when zero, muted when shown.
- Respects the currency/number-format preference (see [[D5]]) but defaults to INR.
- Accessible spoken form: "minus rupees 420", "plus rupees 12,000".

**Done when**
- Table-driven unit tests cover zero, negatives, lakh/crore grouping, rounding, and very large numbers.

## [D1] Data: TransactionRepo implementation and aggregate queries
Labels: v2, phase:1-data, data, domain

**Why:** `TransactionRepo` is only an interface; nothing is bound in `RepositoryModule`. Overview, Insights, Budgets and Transactions all need it.

**Scope**
- Implement and bind `TransactionRepo` (switch DI to `@Binds`).
- Reactive queries (Flow) for: transactions by month with filters (type, owes-you, you-owe, settlements), full-text-ish search across merchant/category/note, grouped-by-day totals.
- Aggregates: month income / personal expense totals, spend by category, cumulative spend by day (this month vs previous month), last-6-months totals + 5-month average + month-end projection, weekly spend by category.
- Use cases in `domain/usecase` so ViewModels stay thin.

**Done when**
- In-memory Room tests cover each aggregate, including month boundaries and the user's "month starts on" setting.

Blocks: [[D2]], [[S1]], [[S2]], [[S4]]

## [D2] Data: budgets repository, month-scoped budgets and status rules
Labels: v2, phase:1-data, data, domain

**Scope**
- `BudgetRepo` (entities/DAO exist; no repo).
- Budgets are **per month**; past months are read-only; unused amounts do not carry over.
- Status rules: On track < 85% of limit, Nearing limit 85–100%, Over budget > 100%. Even-pace marker = day-of-period ÷ days-in-period.
- Suggested budgets from the last 3 months of spend per category, rounded to a sensible step.
- "Copy from last month" when a month has none.
- Overall month total and "left to spend / per-day" derivation.

**Done when**
- Unit tests for thresholds, pace, rounding, and month rollover. Room migration (if needed) is covered in [[Q3]].

Depends on: [[D1]]

## [D3] Data: payee balances, settlements and per-payee running history
Labels: v2, phase:1-data, data, domain

**Scope**
- Extend `PayeeBalanceView` / `PayeeRepoImpl` so a payee exposes: net balance, direction (owes you / you owe / settled), oldest-open date, open item count.
- Settlement as a first-class transaction: "paid you" / "you paid", full or partial, method, date, note.
- Per-payee history with a running balance after each item.
- Overall: owed to you, you owe, net, count of people each way, oldest-first list for Insights.
- Reminder text generation (see decision 6 in the plan), no network.

**Done when**
- Tests: partial settlement, overpayment, settle to zero, interleaved items from both directions.

Blocks: [[S7]]

## [D4] Data: net position across accounts and friends
Labels: v2, phase:1-data, data, domain

**Scope**
- Account balances derived from transactions + opening balance (`AccountEntity`).
- Net position = sum of accounts + net shared balance. Overview's subtitle needs the parts ("₹1,02,340 in 4 accounts + ₹4,730 from friends").
- Accounts and payment methods management data (add/rename/archive), as used by Settings → Financial setup.

**Done when**
- Tests for a mix of accounts, transfers, and settlements.

Blocks: [[S1]]

## [D5] Data: expanded preferences (format, calendar, reminders, defaults)
Labels: v2, phase:1-data, settings, data

**Scope** (DataStore)
- Currency and number format, week start, **month start day** (Budgets and Insights follow it).
- Daily reminder on/off + time; settle-up nudge on/off.
- Defaults for new transactions (default account, default split mode); "fill category from payee" toggle.
- App lock (biometric, grace period) and "hide in recent apps" (`FLAG_SECURE`).
- A `SettingsRepository` exposing these as Flow.

**Done when**
- Round-trip tests; month-start day is consumed by D1/D2 date range helpers.

Blocks: [[S8]]

## [D6] Data: export transactions as CSV or JSON
Labels: v2, phase:1-data, settings, data

**Scope**
- Export all or filtered transactions via the system file picker (`ActivityResultContracts.CreateDocument`); no storage permission.
- CSV and JSON, streaming write so large histories don't spike memory.
- Does not change data.

**Done when**
- A round-trip test imports the JSON back into an empty DB identically.

## [D7] Data: local backup, restore and delete-all-data
Labels: v2, phase:1-data, settings, data, safety

**Scope**
- Back up to a single file the user chooses (system picker). Encrypt with a user passphrase.
- Restore replaces everything on device; requires a confirm step that states what will be lost and prompts a backup first.
- Delete all data: typed confirmation ("DELETE"), clears DB and preferences, returns to the empty Overview.
- Clear cache action reporting the freed size.
- Google Drive / weekly auto-backup are **out of scope** here (decision 3).

**Done when**
- Backup → wipe → restore returns an identical database (test).
- Delete cannot be triggered without the typed confirmation.

Blocks: [[S8]]

## [S1] Screen: Overview
Labels: v2, phase:2-screens, screen, ui

Design: *Overview — where do I stand right now?* (ov-light, ov-dark, ov-mono).

**Scope**
- Header: Zenith mark + wordmark, profile avatar button (opens Settings).
- Net position hero card (display figure + "₹x in N accounts + ₹y from friends").
- Spent-this-month card with comparison to last month at the same point and the cumulative sparkline.
- "Between friends" card: owed to you / you owe, net, and one nudge row with a Remind action (oldest unpaid).
- Recent: exactly five transactions, "See all" goes to Transactions.
- Replace `HomeScreen` / `HomeViewModel` content; keep the file names where sensible.

**Done when**
- Matches the canvas in 3 palettes; scrolls on small phones; empty state is [[S10]].

Depends on: [[F5]], [[F6]], [[F7]], [[D1]], [[D4]]

## [S2] Screen: Insights
Labels: v2, phase:2-screens, screen, ui

Design: ins1/ins2 in light, dark, mono.

**Scope**
- Fixed top bar with title and month switcher; content scrolls under it.
- Cards: net cash flow (+ share of income kept), budget health (worst two + "N more on track" → Budgets), spending trend (6 months, average line, projection), where it went (share bar + legend), shared money (oldest first), and the 4-up entry grid (Budgets, Payees, Categories; hide Review until it is designed — decision 2).
- Replace the existing `InsightsScreen`; decide whether `AnalyticsScreen` is deleted or folded in (it is currently unwired).

Depends on: [[F5]], [[F6]], [[F7]], [[D1]], [[D2]]

## [S3] Screen: Budgets (overview, add/edit, month picker, empty)
Labels: v2, phase:2-screens, screen, ui

Design: bud1–bud4 in light, dark, mono.

**Scope**
- "Left to spend" summary with pace status, days left, per-day figure.
- Categories split into "Needs a look" and "On track", each with progress bar, even-pace tick and status tag (icon + word).
- Month picker sheet (past months read-only, months without budgets show Add).
- Add/edit budget sheet.
- Empty state: suggested start from 3-month averages, "Use suggested budgets" / "Start from scratch", and the "How statuses read" legend.

Depends on: [[F5]], [[F6]], [[D2]]

## [S4] Screen: Transactions (list, filters, search, details sheet)
Labels: v2, phase:2-screens, screen, ui

Design: tx1–tx6 in colour and mono.

**Scope**
- Route the existing, currently unrouted `TransactionsScreen`.
- Month switcher, count, filter chips (All, Income, Expense, You owe, They owe, Settlements).
- Day-grouped list with per-day totals; shared rows show who owes what with a word, not only colour.
- Search (with "Search all months"), no-results state, empty state.
- Details bottom sheet: amount, category, paid with, note, split breakdown per person, Edit and Delete (delete asks for confirmation; deletion is the only permanent action here).

Depends on: [[F5]], [[F6]], [[D1]]

## [S5] Flow: Add transaction (amount → details)
Labels: v2, phase:2-screens, screen, ui

Design: add-color / det-color (+ mono) and the light/dark/mono "Add 1–3" sequence.

**Scope**
- Bottom sheet over any screen. Step 1: type switch (Expense, Income, Owed, You paid), big amount, category chips, date, account, note, numpad, "Save as personal" / "Next · Who's in?".
- Details state: merchant with category suggested from merchant, "Paid with", date chips (Today / Yesterday / Pick date), note, "Split with others", primary "Save expense · ₹650".
- Closing keeps a draft.
- Rework `AddTransactionFlow`, `AddTxnScreen`, `NumberPad`, `AddTxnViewModel`.

Depends on: [[F5]], [[F6]], [[D1]]

## [S6] Flow: Split (who's in, how to split) and group analytics
Labels: v2, phase:2-screens, screen, ui, needs-decision

Design: add2 / add3 (who and split), light-group.

**Scope**
- "Who" step: choose people, optionally a group chip.
- Split step: Equally / By items / Custom, per-person share rows, "Others owe you" total, optional "Send UPI request" (decision 6).
- Group analytics: group spend for the month, spend by category, who paid vs share, "Settle up".

**Needs a decision:** groups are not in the data model (decision 1). Either add a `GroupEntity` + migration here, or ship split-by-people only and move group analytics to v2.1.

Depends on: [[S5]], [[D3]]

## [S7] Screens: Payees and shared balances (list, history, record settlement)
Labels: v2, phase:2-screens, screen, ui

Design: pay1–pay5 in colour and mono.

**Scope**
- Shared balances list: net balance header, search, filter chips (All / Owes you / You owe / Settled), sections with subtotals.
- Payee details (builds on the recent `PayeeDetailsScreen`): net balance, "Record settlement", "Send a gentle reminder", shared history with running balance and a legend ("+ adds to what X owes you").
- Record settlement sheet: direction, full/part amount, method, date, note, live "balance after saving". Saved locally; nothing is sent to the other person.
- Payee add/edit dialog restyled.

Depends on: [[F5]], [[F6]], [[D3]]

## [S8] Screen: Settings (sections, data and privacy, confirm dialogs)
Labels: v2, phase:2-screens, screen, ui, settings

Design: set1–set6 in colour and mono.

**Scope**
- Profile row (name, UPI ID) and the "stored on this device · works offline" banner.
- Sections: App preferences, Financial setup, Data and privacy, Back up, Restore, Local data, About.
- Confirm delete (empty → ready states with typed "DELETE"), confirm restore.
- About: version/build, help, open-source licences (include fonts), privacy, terms.
- Keep the existing navigation-animation settings and move them under App preferences.

Depends on: [[F4]], [[F5]], [[D5]], [[D6]], [[D7]]

## [S9] Screen: Categories (list, rename, merge, add)
Labels: v2, phase:2-screens, screen, ui

The Insights entry grid and Settings both link to "Categories", but there is no board for it. Build it from the existing components.

**Scope**
- List with spend per category for the selected month, add, rename, merge, archive (never delete one that has transactions).
- Reuse the restyled `CategoryPicker`.

Depends on: [[F5]], [[F6]], [[D1]]

## [S10] Empty and first-run states
Labels: v2, phase:2-screens, screen, ui

Design: em-* (Overview, Insights, Budgets, Payees; colour and mono).

**Scope**
- Overview with no transactions ("Start with one entry", example row, "Add your first transaction"); Insights without enough history; Budgets none configured; Payees no shared balances.
- Shared `EmptyState` from [[F5]]; each state teaches what the screen will show.

Depends on: [[F5]], [[S1]], [[S2]], [[S3]], [[S7]]

## [Q1] Accessibility and theme verification pass
Labels: v2, phase:3-quality, a11y

**Scope**
- TalkBack walkthrough of every screen: reading order, labels on icon buttons, chart summaries, state announcements for status tags.
- Contrast audit on all three palettes. Colour is never the only signal.
- 200% font scale, large display size, landscape not clipped.
- Touch targets at least 44dp.
- Fix the findings or file follow-ups.

Depends on: [[S1]], [[S2]], [[S3]], [[S4]], [[S5]], [[S7]], [[S8]]

## [Q2] Tests: use cases, ViewModels, Compose UI and screenshots
Labels: v2, phase:3-quality, testing

**Scope**
- Unit tests for every new use case and ViewModel (Turbine for Flows).
- Compose UI tests for the Add flow, Settings confirm dialogs and filter chips.
- Screenshot tests for key screens in light, dark, mono (Paparazzi or Roborazzi; pick one in the PR).
- Run in CI (a GitHub Actions workflow is part of this issue; none exists today).

## [Q3] Room schema migration to v2 and removal of old UI
Labels: v2, phase:3-quality, data, tech-debt

**Scope**
- Turn on `exportSchema`, and write explicit migrations for all schema changes made by D2/D3/D4/S6 (budget month scoping, settlement fields, groups if accepted). Test with `MigrationTestHelper`.
- No destructive migration for existing users.
- Delete superseded code: old `HomeScreen` content, unwired `AnalyticsScreen`, default template colours, unused components.
- Update `app/DATABASE_REFERENCE.md`.

Depends on: [[D2]], [[D3]], [[D4]]

## [Q4] Release: v2.0 versioning, changelog and smoke test
Labels: v2, phase:3-quality, release

**Scope**
- Bump `versionName` to 2.0.0 and `versionCode`; set `targetSdk` explicitly (currently commented out).
- Changelog / release notes; update README screenshots from the new screens.
- Manual smoke test on a fresh install and on an upgrade from v1 data.
- Tag `v2.0.0`.

Depends on: [[Q1]], [[Q2]], [[Q3]]
