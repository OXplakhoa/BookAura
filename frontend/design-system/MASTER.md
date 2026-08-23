# BookAura Design System

## Direction

- Product: responsive library discovery and management application.
- Style: editorial minimalism; warm paper surfaces, ink text, restrained teal accent.
- Avoid gradients, excessive glass effects, decorative animation and pill-shaped containers.
- Content hierarchy comes from typography, whitespace and thin borders.

## Tokens

| Role | Value |
|---|---|
| Canvas | `#f5f1e8` |
| Surface | `#fffdf8` |
| Ink | `#192536` |
| Muted ink | `#596577` |
| Primary | `#0f6b62` |
| Primary dark | `#0a514b` |
| Accent | `#b57925` |
| Border | `#d9d2c4` |
| Error | `#b42318` |
| Success | `#16794f` |

- Display type: Georgia / Iowan Old Style fallback.
- Body type: Inter / system sans fallback.
- Spacing: 4/8px scale. Body text minimum 16px.
- Focus ring: 3px primary tint, always visible for keyboard users.
- Touch target: minimum 44px.
- Motion: opacity/transform only, 150–250ms, disabled by `prefers-reduced-motion`.

## Components

- Buttons: 10px radius; one primary CTA per region; clear loading/disabled state.
- Cards: 16px radius, thin border, very subtle elevation only when needed.
- Forms: persistent labels, field-local errors, autofill metadata and 46px controls.
- Navigation: public top bar; authenticated desktop sidebar / mobile top navigation.
- Tables: desktop only where comparison matters; card list fallback on small screens.

## Accessibility

- WCAG AA text contrast.
- Skip link, semantic landmarks and sequential headings.
- Icons are Lucide SVGs; decorative icons use `aria-hidden`.
- Icon-only controls require accessible labels.
- Loading and mutation feedback use live regions without stealing focus.
