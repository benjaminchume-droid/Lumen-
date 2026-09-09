# Lumen Architecture

## Goals
Build a reading OS in the spirit of **Mihon** (manga), **Shosetsu** / **LNReader** (novels), and **iReader**-style ecosystems:

1. **Extension system** — each site is a Source plugin
2. **Universal Aggregator** — merge chapters from many extensions into one series that stays up to date
3. **Smart collapse** — if one source has ≥95% chapter coverage and the highest quality score, use only that source (no unnecessary multi-repo noise)

## Layers

```
┌─────────────────────────────────────────┐
│  UI (Compose / existing React HomeView) │
├─────────────────────────────────────────┤
│  Aggregator (merge + quality ranking)   │
├─────────────────────────────────────────┤
│  Extension Registry (enable / disable)  │
├─────────────────────────────────────────┤
│  Sources (HttpSource per site)          │
└─────────────────────────────────────────┘
```

### Source contract
Every extension implements:
- `getPopular` / `getLatest` / `search`
- `getSeriesDetails`
- `getChapterList`
- `getPageList` (images for manga, text for novels)

### Aggregator rules
- Group chapters by normalized number (`12`, `12.0`, `12.5`)
- Score = `priority * 1000 + qualityScore + recency`
- Primary = highest score mirror
- If single source ≥95% coverage + best quality → collapse to that source only

### Extension repos
Compatible with:
- Mihon / Tachiyomi `index.min.json`
- Shosetsu plugin lists

### Android
- Package: `com.lumen.reader`
- Min SDK 26, Target 35
- Signed release via GitHub Actions + `SIGNING_KEY_STORE_BASE64` / `LUMEN_*` secrets

### CI
Push a tag `v1.0.0` or run **Build & Release APK** workflow manually.
