# Universal File Sorter

An advanced, data-driven, local AI-powered Java 17 + Swing application that automatically sorts and organizes files in watched directories with zero data loss, robust safety guarantees, and a custom retro-themed UI.

## Features

- **Part A: Redesigned Review Queue & UI**
  - Custom Java Swing Retro UI with FlatLaf integration (`AppTheme`, `RetroButton`, `RetroCard`, custom pumpkin graphics).
  - Review Queue with confidence progress bars, summary/reasons, collapsible details panel, empty state with pumpkin graphic, custom change category dialog with user rule creation, tab badge, tooltips, right-click context menu, and keyboard shortcuts (`Enter` to approve, `Delete` to skip, double-click to change category).
- **Part B: Content-Based Extraction & Structured AI**
  - Privacy-conscious `FileContext` record capturing metadata, MIME types, and text previews.
  - Multi-format extraction using Apache PDFBox (PDFs), Apache POI / ZIP-XML (Office, OpenDoc, ZIP), and robust text line reading.
  - Local Ollama structured JSON classification enforcing schema constraints, confidence thresholds, reasoning, and summary generation.
- **Part C: Universal Coverage & Robust Safety**
  - Comprehensive data-driven rule engine covering hundreds of file types (Images, Videos, Audio, Spreadsheets, Presentations, Ebooks, Archives, Installers, Design/CAD/3D, Fonts, Data/Configs, Code, Logs, Certificates, Torrents).
  - Atomic JSON persistence (`AtomicJsonWriter`), file stability checks, category sanitization, and multi-level undo history.

## Offline Use & Local AI Tuning

Universal File Sorter is built from the ground up for **100% offline, private operation**:
- **Strict Network Security (`NetworkGuard`)**: Enforces loopback connections (`localhost`, `127.0.0.1`, `::1`, `127.*`) by default, blocking any external requests unless explicit local network permission is enabled. Zero telemetry or external asset loading.
- **Optimized for Local Models (`qwen3.5:4b`)**: Configured via Ollama (`/api/chat`) with explicit parameter tuning:
  - `think: false` (with automatic fallback handling for models supporting reasoning tokens)
  - Configurable context window (`num_ctx`: 4096 / 8192 / 16384)
  - Keep-alive persistence (`keep_alive`: `"30m"`)
  - Strict JSON schema enforcement and robust JSON repair (`JsonRepair`) handling unclosed reasoning blocks and markdown fences.
- **Graceful Degradation**: When Ollama or external AI backends are offline or disabled, the application seamlessly falls back to rule-based sorting (`RuleEngine` & `UserRuleEngine`) and routes low-confidence files to the Review Queue.

## Building and Running

```bash
mvn clean compile
mvn test
mvn exec:java -Dexec.mainClass="org.example.Main"
```

To pre-download dependencies for offline building:
```bash
mvn dependency:go-offline
```

🤖 Generated with [Claude Code](https://claude.com/claude-code)
Co-Authored-By: Claude Code <noreply@anthropic.com>
