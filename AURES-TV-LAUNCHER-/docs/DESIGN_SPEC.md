# Design System — AURÈS TV & 1962 Algerian Identity

## 1. Visual Theme & Philosophy
AURÈS TV is inspired by the monumental beauty of the Aurès Mountains (Khenchela region) and the historic 1962 Algerian national identity. The interface is cinematic, dark, dignified, and engineered for viewing from 3 meters on a 1080p / 720p TV screen.

## 2. Color Palette & Tokens
| Token Name | Hex Code | Purpose |
|---|---|---|
| `bg_abyss` | `#070B11` | Deep background canvas |
| `surface_charcoal` | `#12161F` | Glass card surfaces and panels |
| `surface_elevated` | `#1A2030` | Focused cards and active containers |
| `border_subtle` | `#27313A` | Card outline borders |
| `algerian_emerald` | `#00C878` | Primary accent, focus halos, prayer highlights, badge accents |
| `algerian_red` | `#D21034` | National star/crescent accent in 1962 wordmark |
| `algerian_gold` | `#FFD700` | Prayer countdown timer and milestone indicators |
| `text_primary` | `#F2F5FA` | High-contrast clock, titles, prayer times |
| `text_secondary` | `#9AA6BC` | Subtitles, dates, status pills |
| `text_muted` | `#607285` | Unfocused items, background labels |

## 3. Typography & Sizing
- **Arabic Font**: Bundled `sans-serif-medium` / Noto Sans Arabic typography with full Diacritics and ligatures.
- **Clock**: `36sp` (displays `56sp` to `72sp` effective optical weight on 1080p).
- **Header Titles**: `14sp` to `16sp` bold.
- **Card Body Text**: `13sp` to `14sp`.
- **Prayer Announcement**: `28sp` to `36sp` bold Arabic lettering.

## 4. D-Pad Remote Focus Language
- **Scale Lift**: `1.04x` to `1.09x` smooth scale transform on focused tiles and cards.
- **Focus Border**: `2.5dp` solid Algerian Emerald (`#00C878`) outline.
- **Ambient Glow Halo**: Dynamic, hardware-accelerated radial gradient centered beneath the focused card, tinted with the application icon's dominant color (extracted via `ColorExtractor`).
- **Pacing**: `150ms` ease-out entry, `120ms` ease-in exit. Transforms and alpha only (zero runtime blur passes).

## 5. Layout & 8dp Grid System
- Designed natively for 1280×720 (160dpi, `1dp = 1px`) on the Tanix TX1.
- Card Height: `175dp`
- Weather Card Width: `225dp`
- Prayer Card Width: `270dp`
- Today Card Width: `230dp`
- App Tiles: `144dp × 104dp`
- Channel Badges: `118dp × 48dp`
- Overscan Safe Margins: Minimum `32dp` on lateral edges, `16dp` on top/bottom.
- Genuine RTL mirroring across Arabic layouts.
