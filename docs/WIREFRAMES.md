# WIREFRAMES (ASCII, lo-fi on purpose — evolve with implementation)

Visual direction: modern bookstore look (dark academia / warm paper palette), 3D "Shelf Aura"
planned from day one but implemented last (P2). All forms: field-level errors from backend
`validationErrors`, loading/empty/error states, confirm on destructive ops.

## Public / auth

```
LOGIN                            REGISTER                      VERIFY EMAIL
┌──────────────────────┐        ┌──────────────────────┐      ┌──────────────────────┐
│ BookAura      [logo] │        │ BookAura             │      │  ✓ Email verified!   │
│ ┌──────────────────┐ │        │ ┌──────────────────┐ │      │  You can now log in. │
│ │ Email or phone   │ │        │ │ Full name        │ │      │  [ Go to login ]     │
│ │ Password         │ │        │ │ Email            │ │      └──────────────────────┘
│ │ [    Log in   ]  │ │        │ │ Phone (optional) │ │      (error state: link
│ └──────────────────┘ │        │ │ Password + rules │ │       expired → resend)
│ [ Continue w/ Google]│        │ │ [  Register   ]  │ │
│ [ Continue w/Facebook│ (P0-B) │ └──────────────────┘ │
│ Forgot? · Register   │        │ Already have account?│
└──────────────────────┘        └──────────────────────┘
```

## USER

```
CATALOG (public)                       BOOK DETAIL
┌────────────────────────────────────────────┐   ┌───────────────────────────┐
│ nav: Catalog | My Loans | Profile | Logout │   │ Cover | Title             │
│ [search............] [category▾][year][⏷]  │   │       | Authors, ISBN     │
│ ┌────┐ ┌────┐ ┌────┐ ┌────┐  (grid,       │   │       | Category chips    │
│ │card│ │card│ │card│ │card│   cover,       │   │       | ● 3 available     │
│ │    │ │    │ │    │ │    │   title,       │   │ [ Borrow ]  (disabled   │
│ └────┘ └────┘ └────┘ └────┘   availability)│   │  if 0 / already loaned) │
│ ◀ 1 2 3 ▶   (page size ≤ 10)               │   └───────────────────────────┘
└────────────────────────────────────────────┘

MY LOANS / HISTORY                 PROFILE
┌───────────────────────────────┐   ┌───────────────────────────┐
│ [Active] [History] tabs       │   │ Name, email(verified✓),   │
│ ┌───────────────────────────┐ │   │ phone, DoB  [Edit]        │
│ │ Book | due date | [Return]│ │   │ [Change password]         │
│ │ (overdue rows red)        │ │   │ [Change email]→OTP screen │
│ └───────────────────────────┘ │   └───────────────────────────┘
└───────────────────────────────┘
```

## ADMIN

```
BOOKS TABLE                        MEMBERS TABLE
┌────────────────────────────────────────────┐  ┌───────────────────────────────────────────┐
│ [+ New book] [Import CSV]                  │  │ [+ New member]                            │
│ filters: title/isbn/author/category/year/  │  │ filters: name | email/phone | DoB from–to │
│          availability [Apply]              │  │          | borrowed book | status [Apply] │
│ ┌──────────────────────────────────────┐   │  │ ┌───────────────────────────────────────┐ │
│ │ Title | ISBN | Avail | Status | ✏️🗑 │   │  │ │ Name | Email | DoB | Status | Loans ▾ │ │
│ │ (sortable columns, allowlist only)   │   │  │ │ row → detail drawer (loans, edit,     │ │
│ └──────────────────────────────────────┘   │  │ │  disable with confirm)                │ │
│ ◀ 1 2 3 ▶                                  │  │ └───────────────────────────────────────┘ │
└────────────────────────────────────────────┘  └───────────────────────────────────────────┘

CSV IMPORT (modal)                 SYSTEM CONFIG
┌───────────────────────────────┐  ┌───────────────────────────┐
│ Drop .csv (< 5MB)             │  │ Maintenance mode  [ OFF ▾]│
│ [Upload] → per-row errors or  │  │ (confirm dialog; explains │
│ "Imported N rows"             │  │  503 for business APIs)   │
└───────────────────────────────┘  └───────────────────────────┘
```

## MAINTENANCE (all roles)

```
┌──────────────────────────────┐
│      🛠 Under maintenance    │   ← shown on any 503 from API interceptor
│  We'll be back soon.         │
└──────────────────────────────┘
```

## SHELF AURA (P2)

```
┌────────────────────────────────────────────┐
│ Mood [chips] Time [slider] Themes [chips]  │
│ Intensity [light ▾]      [ Find my aura ]  │
│ [3D shelf] [List view]                    │
│ ┌──── WebGL Arcane Opus ───┬─ reading ──┐ │   2D fallback: ranked card list
│ │ physical books + brass   │ score/tags │ │   with score + "why suggested"
│ │ hover lifts + lights     │ top reasons│ │
│ │ click → book detail      │            │ │   reduced motion → list view
│ └──────────────────────────┴────────────┘ │
└────────────────────────────────────────────┘
```
