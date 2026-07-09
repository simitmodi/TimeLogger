# Time Logger (Java Desktop Application)

A premium, feature-rich Java Swing desktop application designed for study tracking, exam prep (optimized for GATE aspirants), and gamified productivity analytics. Built with modern UI aesthetics, asynchronous AI coaching, and deep local statistical analysis.

---

## Key Features

### 🎨 1. Modern UI & Theme Engine
* **Harmonious Palettes**: Seamlessly switch between **Light Mode**, **Dark Mode**, and **High Contrast Mode**.
* **Glassmorphism Design**: Standardized with modern typography, smooth micro-interactions, flat rounded hover components, and custom vector icons.
* **Minimize-to-Tray**: Runs quietly in the system tray when minimized. Double-click the tray icon to restore, or right-click for quick tray actions (Pause, Resume, Stop & Log, Exit).

### 🧮 2. Embedded GATE Virtual Scientific Calculator
* **Automatic Pop-up**: Automatically opens when choosing `Practice Book Questions` or `Previous Year Questions` to assist numerical solving.
* **Strict Postfix Mechanics**: Aligned with the official GATE Virtual Calculator (e.g., enter `30` then click `sin` for postfix execution).
* **High-Precision Output**: Computes and formats answers with **up to 16 decimal places** (e.g. `1/3` yields exactly `0.3333333333333333`) using exact `BigDecimal` scaling.
* **Optimized Hitboxes**: Placed in an expanded **820x540 pixel** frame with right-aligned displays and dynamically scaled bold button fonts (up to size 20) for easy clicking.
* **Security & Re-opening**: Keyboard inputs are completely blocked (mouse-clicks only to prevent accidental editor typing). Can be closed to see the timer and manually reopened using the custom blue vector-icon **Calculator** button.

### 🎮 3. Gamified XP Points Engine (Integer-Only)
* **XP Formula**: Earn **`+1 XP`** for every 1 minute of active study tracked, and deduct **`-0.25 XP`** (calculated using rounded daily sums) for every 1 minute spent on mid-session pauses/breaks.
* **Bento XP Card**: View your net XP, total gained, and break-deductions inside the Analysis tab.
* **Daily XP Ledger & Comparison**: A tabular registry listing your daily performance, study hygiene, and net XP gains in reverse-chronological order.

### 📊 4. Bento Box Analytics Dashboard
* **Responsive Grid Layout**: Refactored using dynamic `GridBagLayout` into a clean, modern dashboard:
  * **Summary Row**: Daily Goal Progress, Total Session Count, Total Tracked Duration, and Net XP.
  * **Period Filtering**: Filter data instantly for Today, Yesterday, This Week, Last 7 Days, This Month, Last 30 Days, All Time, or a Custom Date Range (with visual calendars).
  * **Compact 2-Month Heatmap**: A strict 9-week calendar grid displaying contribution density that completely eliminates empty whitespace.
  * **Weekly Distribution Chart**: Custom-drawn, theme-colored vertical bar chart displaying daily tracked hours.
  * **Session Segment Timelines**: Visual timeline showing daily study segments and pauses with HTML hover tooltips.
  * **Focused Tables**: Aggregated breakdowns by Subject, Chapter, Activity Type, and Revision Topics.

### 🤖 5. Asynchronous AI Study Assistant (OpenRouter)
* **Secure Storage**: Integrates with the OpenRouter API using locally secured keys (`openrouter_api_key.txt`).
* **Context Compiler**: Feeds 7-day averages, burnout risks, focus scores, XP metrics, and historical logs into the prompt.
* **Base64 Line Serialization**: Chat logs are serialized using Base64 to escape special characters, ensuring history load/save persistence across app reboots.
* **Spell-Correction**: Cleans common 4-bit Gemma model quantization typos (e.g., repairing `didn's` to `didn't`).

### 📂 6. Excel-like Data Grid & Exports
* **Log Headers Filters**: Click column headers to quick-sort, or click the dropdown arrow (`▾`) to search and select checklist values (like Excel).
* **Resume Session**: Double-click or select a logged session and click **Resume** to continue tracking from where you left off.
* **Reports Export**: Generates automatic weekly and monthly `.xlsx` files in the app folder, plus custom range exports.

---

## System Architecture & Performance
* **Memory Optimization**: Caps JVM heap space to **16MB** (`-Xmx16m`) and utilizes Serial GC, Metaspace restrictions, and startup garbage collections. Drops RAM usage from **~272MB to under 60MB**.
* **Single-Instance Lock**: Leverages local loopback port socket locks (`54321`) to prevent concurrent instances, automatically bringing the active instance to the foreground if launched again.
* **Data Backups**: Automatically backs up logs on startup to a rolling folder of timestamped ZIP archives (keeping the last 7 backups).

---

## Installation & Build Instructions

### Prerequisites
* **Java Development Kit (JDK) 25** (or later) must be installed.
* Ensure Java is added to your system's PATH.

### 1. Build the Runnable JAR
Run the build script in PowerShell:
```powershell
.\build-jar.ps1
```
This compiles the code and packages resources into `dist\time-logger.jar`.

### 2. Packaging a Standalone Native Distribution (Portable ZIP)
To package the app with a bundled, stripped-down JRE so that users can run the app without installing Java, run the following:
```powershell
# Uses JDK's jpackage and jlink utilities to compile the native image
& "C:\Program Files\Java\jdk-25.0.2\bin\jpackage.exe" `
  --type app-image `
  --dest dist `
  --name TimeLogger `
  --input dist `
  --main-jar time-logger.jar `
  --main-class com.timelogger.Main `
  --icon icon.ico
```
This generates the standalone binary folder at `dist\TimeLogger\`. You can compress this directory into a ZIP archive for portable distribution.

### 3. Packaging a Standalone Windows Installer (`.msi`)
To create a double-clickable Windows installation wizard that configures shortcuts and installs directly to `C:\Program Files`:
1. Install the **WiX Toolset v3.x** on your developer machine:
   ```powershell
   winget install FireGiant.WixToolset.v3
   ```
2. Build the installer using `jpackage`:
   ```powershell
   & "C:\Program Files\Java\jdk-25.0.2\bin\jpackage.exe" `
     --type msi `
     --dest dist `
     --name TimeLogger `
     --input dist `
     --main-jar time-logger.jar `
     --main-class com.timelogger.Main `
     --icon icon.ico `
     --win-menu `
     --win-shortcut `
     --win-dir-chooser
   ```
This will compile and output `dist\TimeLogger.msi`.

---

## How to Run

### Option A: Standard JAR Launch
Double-click `start-time-logger.bat` in the root folder, or execute:
```powershell
java -jar .\dist\time-logger.jar
```
*(Optionally runs with low-memory JVM args: `java -Xms2m -Xmx16m -XX:+UseSerialGC -jar .\dist\time-logger.jar`)*

### Option B: Native Launcher
If you built the standalone distribution, simply run:
```powershell
.\dist\TimeLogger\TimeLogger.exe
```

---

## Local Data Storage
The application stores configuration and logs in the directory where the JAR/executable runs:
* `subjects.txt`: Plaintext list of subjects.
* `sessions.log`: Pipe-delimited session database.
* `question_topics_dpp.txt` / `_practice.txt` / `_pyq.txt`: Type-partitioned stopwatch topics.
* `openrouter_api_key.txt`: Local secure OpenRouter API credential storage.
* `backups/`: Rolling zip archives containing daily database snapshots.
