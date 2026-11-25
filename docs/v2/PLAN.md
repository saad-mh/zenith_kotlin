# Zenith v2 — "Circles" revamp plan

Source of truth for visuals: the design canvas (direction **B · Circles**, in Colour light, Colour dark and Monochrome).
Design canvas: https://claude.ai/code/artifact/c611c1a9-7edf-48e1-86be-875a57f1141b

## 1. What v2 is

A visual and structural revamp of the app around three ideas from the design:

1. **People-first money.** Shared balances sit next to personal spending. Net position = accounts + what friends owe you.
2. **Calm, editorial type.** Fraunces (serif) for figures and headings, Work Sans for everything else, tabular figures for amounts.
3. **Never colour alone.** Every positive/negative/status is also a sign, a word or an icon. A monochrome theme must stay fully usable.

Everything stays offline-first and on-device. There is no sign-in and no bank link.

## 2. Design summary (what the canvas specifies)

| Area | Spec |
|---|---|
| Colour | Light: surface `#faf9f7`, raised `#ffffff`, sunken `#f1eee9`, border `#e1dce6`, ink `#241f2b`, muted `#6d6877`, primary `#777fcf`, primary-strong `#5a5fb0`, gold `#8a6a2e`, positive `#1f7a4f`, negative `#c1493f`, caution `#9c5a0a`. Dark is derived (surface `#17131c`, raised `#211c28`, primary `#8a91dc`, ...). Mono is one ink, hierarchy by weight, sign and words. |
| Extra palettes | Avatar tints `t1..t4` (bg/fg pairs) and chart colours `c1..c6`, per theme. |
| Type | display 40/46 serif 500, heading 24/30 serif 500, subheading 18/26 sans 600, body 15/22, body-strong 15/22 600, numeric 15/22 600 tabular, caption 12/16 500 muted. |
| Shape / space | Radii 4 / 6 / 12. Spacing 4, 8, 12, 16, 24. Touch targets at least 44dp. |
| Navigation | Floating pill bar (64dp, fully rounded): Overview, Insights, Settings + circular FAB for "Add transaction". Active tab expands to icon + label on a tinted pill. |
| Screens | Overview, Insights, Budgets (+ empty, month picker), Transactions (list, search, no results, empty, details sheet), Add transaction (amount, details, who, split), Shared balances / Payees (list, filter, history, record settlement), Group analytics, Settings (+ confirm delete, confirm restore), Chart kit, 4 empty states. |
| Charts | Bars (monthly with average line and projection, ranked categories, stacked weekly, grouped income vs spending, net cash flow), budget progress with even-pace tick, lines (cumulative vs last month), share bar, donut/ring, gauge, budget rings, split pie. |

## 3. Current state (from the codebase)

- Single module, Compose + Material 3, Hilt, Room (v1, `exportSchema = false`), DataStore.
- Theme is the Android Studio default purple palette plus dynamic colour; no `Shapes`, no custom fonts.
- Nav is inline in `MainActivity`; `NavPillBar` already exists and is the seed for the new pill bar.
- Transactions screen exists but is **not routed**; Analytics screen exists but is **not wired**.
- Only `PayeeRepo` has an implementation; `TransactionRepo` is an interface with no binding. Budgets and accounts have entities and DAOs but no repository or UI.
- Build hygiene problems: duplicate Hilt versions, mockito-kotlin pinned to `x.x.x`, M2 `material` dependency alongside M3, committed `.idea/` and `build_log.txt`, README describes libraries and packages that don't exist.

The consequence: v2 is not a re-skin. Roughly half of the screens need data plumbing that doesn't exist yet. The plan therefore runs data work in parallel with the design system.

## 4. Phases and parallelism

```
Phase 0  Foundation (everything else depends on F1–F4)
  F1 build hygiene ─┐
  F2 theme tokens ──┼─► F3 type + shapes ─► F5 component kit ─► F6 pill nav + routes
  F4 appearance prefs ┘                              └────────► F7 chart kit
  F8 money formatting (independent)

Phase 1  Data (parallel to Phase 0 after F1)
  D1 transaction repo + aggregates ─► D2 budgets ─► D3 payee balances + settlements
  D4 net position/accounts, D5 preferences, D6 export, D7 backup/restore/delete

Phase 2  Screens (each needs F5/F6 + its data issue)
  S1 Overview, S2 Insights, S3 Budgets, S4 Transactions, S5 Add flow,
  S6 Split + groups, S7 Payees + settlement, S8 Settings, S9 Categories, S10 Empty states

Phase 3  Quality and release
  Q1 a11y + theme verification, Q2 tests, Q3 migration + cleanup, Q4 release
```

Suggested ownership lanes if more than one person (or session) works at once:

- **Lane A, design system:** F2 → F3 → F5 → F6 → F7. Touches `ui/theme`, `ui/components`.
- **Lane B, data:** D1 → D2 → D3, D4, D5. Touches `data/`, `domain/`, `di/`. No UI files.
- **Lane C, screens:** starts once Lane A has F5 and F6. One screen per PR; keeps file ownership to a single package under `ui/`.
- **Lane D, settings and safety:** D6, D7, S8. Mostly independent.

Rule of thumb: two PRs must not edit the same package at once. `MainActivity.kt` nav edits go through F6 only.

## 5. Decisions needed (flagged on the issues)

1. **Groups ("Flat 4B").** The design shows group analytics and a group chip on the split step. The data model has no group concept. Needs a Room schema change. Proposed: ship in v2.0 as S6, or defer group analytics to v2.1 and keep split-by-people only.
2. **Review (October).** The Insights entry grid has a "Review" tile but no Review screen is designed. Proposed: hide the tile in v2.0, design later.
3. **Google Drive backup.** Settings shows encrypted Drive backup and weekly auto-backup. This pulls in Play Services auth and WorkManager. Proposed: v2.0 ships local file backup/restore via the system file picker; Drive is v2.1.
4. **Dynamic colour.** The current theme prefers Material You. The design is brand-led. Proposed: brand palette is the default; dynamic colour becomes an opt-in.
5. **Reminders and settle-up nudges.** Need notification permission and WorkManager. Proposed: v2.0 includes them behind settings toggles, or defer with Drive.
6. **Send UPI request / Remind.** Proposed: Android share/intent with a prefilled message and `upi://` link; no payments inside the app.

## 6. Definition of done for any v2 screen

- Matches the canvas in light, dark and monochrome.
- Amounts: ₹, Indian digit grouping, U+2212 minus, tabular figures, cents muted.
- No status conveyed by colour alone. Contrast at least 4.5:1 for text.
- Touch targets at least 44dp, TalkBack labels on icon-only buttons and charts.
- Works at 200% font scale without clipping.
- `@Preview` for light, dark and mono, plus the empty state.
- ViewModel logic covered by unit tests.
