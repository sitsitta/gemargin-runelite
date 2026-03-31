# GE Margin — RuneLite Plugin

Free Grand Exchange profit tracker with live margins, session stats, and optional cloud sync to [gemargin.com](https://gemargin.com).

## Features

- **Trade Detection** — Automatically tracks all GE buys and sells
- **Session Tracker** — Running profit/loss panel for your current session
- **GE Margin Overlay** — See live margin, ROI, GP/hr, and tax when browsing the GE
- **Cloud Sync** (optional) — Link your gemargin.com account to sync trades across sessions

## Setup

1. Install from the RuneLite Plugin Hub (search "GE Margin")
2. Plugin works immediately — no account required
3. To enable cloud sync:
   - Log in at [gemargin.com](https://gemargin.com) and go to Settings
   - Generate an API key
   - Paste it into RuneLite Settings → GE Margin → API Key

## Building

```bash
./gradlew build
```

## License

BSD 2-Clause. See [LICENSE](LICENSE).
