# Smart Privacy Scanner

Smart Privacy Scanner is an Android application designed to help users identify and analyze the privacy risks associated with the apps installed on their device. It scans installed applications, analyzes their requested permissions, and provides a risk assessment to ensure better privacy awareness.

## Features

- **App Scanning**: Automatically scans all installed applications on the device.
- **Permission Analysis**: Detailed breakdown of permissions requested by each app.
- **Risk Assessment**: Uses a custom Risk Engine to evaluate the potential privacy impact of apps.
- **Dashboard & Visualization**: Visual representation of privacy data using charts.
- **Modern UI**: Built with Jetpack Compose and Material 3 design principles.

## Tech Stack

- **Language**: Kotlin
- **UI Toolkit**: [Jetpack Compose](https://developer.android.com/jetbrains/compose)
- **Architecture**: MVVM (Model-View-ViewModel)
- **Asynchronous Programming**: [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html) & Flow
- **Dependency Injection**: Manual / Hilt (if applicable, though not explicitly seen in the snippet, assuming standard practices)
- **Local Storage**: [Room Database](https://developer.android.com/training/data-storage/room)
- **Networking**: [Retrofit](https://square.github.io/retrofit/) & [OkHttp](https://square.github.io/okhttp/)
- **Charting**: [MPAndroidChart](https://github.com/PhilJay/MPAndroidChart)

## Getting Started

### Prerequisites

- Android Studio (latest version recommended)
- JDK 11 or higher
- Android device or emulator running Android 7.0 (API level 24) or higher

### Installation

1.  **Clone the repository:**
    ```bash
    git clone https://github.com/bappa8172/Smart-Scanner.git
    ```
2.  **Open in Android Studio:**
    - Launch Android Studio.
    - Select "Open an existing Android Studio project".
    - Navigate to the cloned directory and select it.
3.  **Build and Run:**
    - Wait for Gradle sync to complete.
    - Connect your Android device or start an emulator.
    - Click the "Run" button (green play icon) or press `Shift + F10`.

## Detailed Project Structure

- **ui**: Contains Jetpack Compose screens (`ScannerScreens`, `AppDetailScreen`) and ViewModels (`ScannerViewModel`).
- **analyzer**: Core logic for analyzing permissions and calculating risks (`PermissionAnalyzer`, `RiskEngine`).
- **system**: System-level interactions, such as scanning installed packages (`AppScanner`).
- **data**: (Implied) Data layer handling Room database and Retrofit network calls.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
