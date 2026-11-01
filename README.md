# 🗂️ Universal File Sorter

**Your Downloads folder, finally tamed.** An AI-powered Java desktop app that watches your Downloads folder and instantly sorts every new file into the right place—completely offline, completely private, and with zero user intervention.

> Built for the **Hacktoberfest 2026 "Slow and Steady"** challenge.

---

## ✨ Features

- **Set it and forget it** — runs silently in the background, sorting files the moment they land in your Downloads folder.
- **AI-powered classification** — uses a local Llama 3.2 model to read file content and pick a category (e.g., `Academics`, `Finance_Invoices`, `Code_Projects`).
- **100% offline & private** — everything runs on your machine. No API keys, no cloud calls, no data leaves your PC.
- **Content-aware** — extracts text from PDFs, reads the first lines of code files, and uses filenames for media/binaries to make smart decisions.
- **Retro-themed GUI** — a custom Java Swing interface to view sorting history, review low-confidence decisions, and undo moves.
- **Rule engine fallback** — when the AI model is unavailable, a built-in rule engine keeps sorting files by extension.

---

## 🛠️ Tech Stack

| Layer          | Technology                                          |
| -------------- | --------------------------------------------------- |
| Language       | Java 17                                             |
| Build Tool     | Maven                                               |
| GUI            | Java Swing (custom retro theme)                     |
| AI Model       | Llama 3.2 (3B) via [Ollama](https://ollama.com/)    |
| PDF Extraction | Apache PDFBox 2.0.30                                |
| JSON Parsing   | Gson 2.10.1                                         |

---

## 📋 Prerequisites

Before you begin, make sure you have the following installed:

1. **Java 17+**  
   Verify with: `java -version`

2. **Maven**  
   Verify with: `mvn -version`

3. **Ollama** (for AI-powered sorting)  
   - Install from [ollama.com](https://ollama.com/)
   - Pull the Llama 3.2 model:
     ```bash
     ollama pull llama3.2
     ```
   - Make sure Ollama is running before you start the app:
     ```bash
     ollama serve
     ```

> **💡 Hardware note:** The 3B-parameter model is optimized to run smoothly on mid-range GPUs like a GTX 1660 (6 GB VRAM). It works on CPU too, just slower.

---

## 🚀 Getting Started

```bash
# 1. Clone the repository
git clone https://github.com/navadiadharmik05-hub/universal-sorter.git
cd universal-sorter

# 2. Compile the project
mvn clean compile

# 3. Run the application
mvn exec:java -Dexec.mainClass="org.example.Main"
```

The GUI will launch and immediately begin watching your Downloads folder. New files are sorted automatically—no extra setup needed.

---

## ⚙️ How It Works

```
Downloads folder
       │
       ▼
  ┌──────────────────┐
  │   WatchService    │  ← Background thread detects new files
  └────────┬─────────┘
           │
           ▼
  ┌──────────────────┐
  │    Extractor      │  ← Reads a snippet: first PDF page,
  │                   │     first 20 lines of code, or filename
  └────────┬─────────┘
           │
           ▼
  ┌──────────────────┐
  │   Llama 3.2 (AI) │  ← Local model classifies the file
  │   via Ollama      │     into a category (e.g., "Academics")
  └────────┬─────────┘
           │
           ▼
  ┌──────────────────┐
  │    File Mover     │  ← Moves the file into the right folder
  └──────────────────┘
```

1. **Watch** — A background thread uses Java's `WatchService` to monitor the Windows Downloads folder for new files.
2. **Extract** — It reads a small snippet of the file. For PDFs, it grabs the first page of text. For code files, the first 20 lines. For images, videos, or installers, it uses just the filename.
3. **Classify** — The snippet is sent to the local Llama 3.2 model with a strict prompt that returns exactly one category.
4. **Sort** — The file is instantly moved into the corresponding folder inside Downloads.

---

## 📄 License

This project is open source. See the repository for license details.

---

<p align="center">Made with ☕ and local AI by <a href="https://github.com/navadiadharmik05-hub">@navadiadharmik05-hub</a></p>
