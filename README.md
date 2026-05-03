# Papoter

A lightweight, privacy-first Android chat client for local LLMs via [Ollama](https://ollama.com/). Chat with your own models offline — no cloud, no API keys, no data leaving your network.

Papoter supports streaming responses, built-in tool use (web search, time, location), file and image uploads, conversation history, and multiple model switching — all wrapped in a clean, casual Material Design interface.

---

## Features

| Feature | Description |
|---------|-------------|
| **Local LLM Chat** | Connects to your own Ollama instance. Fully private, no third-party APIs. |
| **Streaming Responses** | Watch the model type its reply in real-time token by token. |
| **Brief & Casual Tone** | System prompt tuned for short, friendly, text-message-like replies. Detailed answers only when explicitly requested. |
| **Built-in Tools** | Automatic tool calling for weather, time, location, news, scores, stock prices, trending topics, and general web search. |
| **File Uploads** | Summarize PDFs and text documents inline. |
| **Image Uploads** | Attach images to conversations (base64-encoded for vision-capable models). |
| **Conversation History** | Persistent SQLite-backed history with per-conversation delete. |
| **Model Switching** | Switch between any model served by your Ollama instance on the fly. |
| **Token Counter** | Track prompt/response tokens and conversation totals. |
| **Server Health Check** | One-tap latency and model count check. |

---

## Architecture

```
Papoter/
├── app/src/main/java/com/papoter/app/
│   ├── MainActivity.kt              # Chat UI, menus, attachment flow
│   ├── ChatViewModel.kt             # Business logic, streaming orchestration
│   ├── SplashActivity.kt            # Entry splash
│   ├── SetupActivity.kt             # First-run server setup
│   ├── data/
│   │   ├── OllamaRepository.kt      # API, database, DataStore abstraction
│   │   ├── ApiService.kt            # Retrofit Ollama API definitions
│   │   ├── OllamaModels.kt          # DTOs / request/response models
│   │   ├── Message.kt               # Room entity
│   │   ├── Conversation.kt          # Room entity
│   │   ├── ConversationDao.kt       # Room DAO
│   │   └── AppDatabase.kt           # Room database
│   ├── tools/
│   │   ├── ToolManager.kt           # Tool schema registry & proactive detection
│   │   ├── WebSearchTool.kt         # DuckDuckGo HTML search scraper
│   │   ├── LocationTool.kt          # Fused location provider wrapper
│   │   ├── TimeDateTool.kt          # Local time/date formatter
│   │   ├── FileUploadTool.kt        # PDF/Text extractor
│   │   └── ImageUploadTool.kt       # Image to base64 encoder
│   └── ui/
│       ├── ChatAdapter.kt           # RecyclerView adapter with streaming diff
│       ├── ConversationHistoryDialog.kt
│       ├── ModelSelectionDialog.kt
│       └── TokenCounterDialog.kt
├── app/src/main/res/                # Layouts, drawables, menus, themes
└── build.gradle.kts                 # Gradle build scripts
```

### Tech Stack

| Layer | Technology |
|-------|------------|
| Language | Kotlin |
| UI | Android SDK + Material Components |
| Architecture | MVVM (ViewModel + StateFlow) |
| Networking | Retrofit 2 + OkHttp |
| Local DB | Room (SQLite) |
| Preferences | DataStore |
| Coroutines | kotlinx-coroutines-android |
| JSON | Gson |
| Markdown | Markwon |
| Location | Google Play Services Location |

---

## Prerequisites

1. **Android Studio** (latest stable) or command-line Android SDK
2. **JDK 17** (Android Gradle Plugin requires it)
3. **An Ollama server** reachable from your device/emulator
   - Install Ollama: [https://ollama.com/download](https://ollama.com/download)
   - Pull a model: `ollama pull llama3.2`
   - Run with: `ollama serve` (default: `http://localhost:11434`)
   - For emulator access, use your machine's LAN IP instead of `localhost`

---

## Build

### Option A: Android Studio (Recommended)

1. **Clone the repository**
   ```bash
   git clone https://github.com/yourusername/papoter.git
   cd papoter
   ```

2. **Open in Android Studio**
   - File → Open → select the `papoter` folder
   - Let Gradle sync finish (this downloads dependencies automatically)

3. **Configure SDK** (if prompted)
   - Android Studio will ask to download the SDK — accept it
   - Or set it manually in `local.properties`:
     ```properties
     sdk.dir=/path/to/your/Android/Sdk
     ```

4. **Run**
   - Select a device/emulator from the toolbar
   - Click the green **Run** button (Shift+F10)

### Option B: Command Line

1. **Clone and enter the project**
   ```bash
   git clone https://github.com/yourusername/papoter.git
   cd papoter
   ```

2. **Set `ANDROID_HOME`** (one-time setup)
   ```bash
   export ANDROID_HOME=$HOME/Android/Sdk
   export PATH=$PATH:$ANDROID_HOME/emulator:$ANDROID_HOME/platform-tools:$ANDROID_HOME/cmdline-tools/latest/bin
   ```
   Add these lines to your shell profile (`.bashrc`, `.zshrc`, etc.) to make them permanent.

3. **Accept SDK licenses** (first time only)
   ```bash
   yes | $ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager --licenses
   ```

4. **Build the debug APK**
   ```bash
   ./gradlew assembleDebug
   ```
   The APK will be at:
   ```
   app/build/outputs/apk/debug/app-debug.apk
   ```

5. **Install to a connected device/emulator**
   ```bash
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

   Or run directly with Gradle:
   ```bash
   ./gradlew installDebug
   ```

---

## Setup & First Run

1. Launch the app
2. On first run, enter your **Ollama server URL**:
   - If running on the same machine as the emulator: `http://10.0.2.2:11434`
   - If on a real device on the same LAN: `http://192.168.1.x:11434`
3. Tap **Test** to verify connectivity
4. Select a model from the dropdown and start chatting

---

## Screenshots


| Chat | History | Settings | Token Counter |
|------|---------|----------|---------------|
| <img width="270" height="585" alt="Screenshot_20260502_213601_Papoter" src="https://github.com/user-attachments/assets/076f1952-6e56-4d6c-8620-f88832a91dc0" /> | <img width="270" height="585" alt="Screenshot_20260502_213632_Papoter" src="https://github.com/user-attachments/assets/94dd3341-6657-4c73-837b-22af49fe76d6" /> | <img width="270" height="585" alt="Screenshot_20260502_213710_Papoter" src="https://github.com/user-attachments/assets/72b72808-6e44-4e17-b1df-e9d82e29793f" /> | <img width="270" height="585" alt="Screenshot_20260502_213733_Papoter" src="https://github.com/user-attachments/assets/4ac5fbae-4bcd-4931-bbf3-537082f89120" />


---

## Roadmap

- [ ] Native tool/function calling via Ollama's `/api/chat` tools field
- [ ] Markdown rendering in chat bubbles
- [ ] Dark mode theme
- [ ] Export conversations to Markdown / JSON
- [ ] Multi-model comparison (side-by-side responses)
- [ ] Voice input
- [ ] Custom system prompt editor

---

## License

MIT License — feel free to fork, modify, and distribute.

---

## Contributing

Pull requests are welcome. For major changes, please open an issue first to discuss what you would like to change.

1. Fork the repository
2. Create your feature branch: `git checkout -b feature/my-feature`
3. Commit your changes: `git commit -am 'Add some feature'`
4. Push to the branch: `git push origin feature/my-feature`
5. Open a Pull Request

---

## Acknowledgements

- [Ollama](https://ollama.com/) — for making local LLMs accessible
- [Retrofit](https://square.github.io/retrofit/) — type-safe HTTP client
- [Room](https://developer.android.com/training/data-storage/room) — abstraction layer over SQLite
- [Markwon](https://noties.io/Markwon/) — markdown rendering for Android
