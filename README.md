# Vectis

> A modern, high-performance Android proxy and VPN client engineered for seamless network bypass.

---

## • [ Overview ]

Vectis is a native Android client designed to provide secure, fast, and reliable internet access. Built around the robust **sing-box** core, it handles modern proxy protocols with an emphasis on performance and battery efficiency. The application is tailored for a public release, combining advanced traffic routing capabilities with a fluid, system-native user interface.

---

## • [ Key Features ]

* **Advanced Protocol Support:** Comprehensive support for **VLESS** (via XHTTP, TCP, gRPC), **Reality**, and **Hysteria**.
* **System-Level Routing:** Utilizes Android's native `VpnService` for reliable, system-wide traffic capture.
* **Split Tunneling & Rules:** Granular control over application routing, allowing users to define exactly which applications bypass or utilize the VPN tunnel.
* **Dynamic Configurations:** Seamlessly interfaces with a custom API to fetch remote node configurations and connection parameters on the fly.
* **Native Pixel-like Experience:** Deep integration with **Material You** (Material Design 3), featuring dynamic color generation (`dynamicColorColorScheme`) that adapts perfectly to the user's system wallpaper and theme.

---

## • [ Architecture & Tech Stack ]

Vectis is built strictly following modern Android development guidelines to ensure a scalable, maintainable, and crash-resistant codebase.

| Layer | Technology / Pattern | Description |
| --- | --- | --- |
| **UI Framework** | Jetpack Compose | Fully declarative UI, ensuring smooth animations and modern design. |
| **Architecture** | MVVM | Clean separation of business logic and user interface. |
| **State Management** | Kotlin Coroutines | Utilizing `StateFlow` for persistent UI states and `SharedFlow` for one-time events. |
| **Dependency Inj.** | Koin | Lightweight and pragmatic dependency injection for module management. |
| **VPN Core** | sing-box | High-performance universal proxy platform for connection handling. |

> **Architecture Note:** The core integration and background service management take architectural inspiration from the official `sing-box for android` client, ensuring stable background execution and proper lifecycle management within Android's strict battery optimization limits.

---

## • [ Backend Integration ]

Vectis operates in tandem with a custom **Python (FastAPI)** backend infrastructure.

* Node parameters and subscription rules are generated dynamically server-side.
* Client-server communication utilizes secure API endpoints, designed for token-based authorization flows.
* *(Note: The FastAPI backend repository is private and maintained separately).*

---

## • [Building from Source]

1. Clone the repository:
```bash
git clone https://github.com/your-username/Vectis.git
```
2. Open the project in **Android Studio**.
3. Sync Gradle dependencies.
4. Build and run the project on a physical Android device. *(Note: Emulators may not fully support all `VpnService` features and network routing capabilities).*
