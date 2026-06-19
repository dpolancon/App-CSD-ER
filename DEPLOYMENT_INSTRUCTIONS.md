# Deployment Instructions for **Club Social & Deportivo Estrella Roja**

## Prerequisites

- **Java Development Kit (JDK) 17** (or the version specified in `gradle.properties`).
- **Android Studio** with the **Android SDK** installed.
- **Android Virtual Device (AVD)** or a physical Android device with **USB debugging** enabled.
- **Git** (optional, for pulling the repository).
- **Gradle** wrapper is included in the repo (`./gradlew`). No global Gradle installation is required.
- **SQLite JDBC driver** is bundled in the desktop module, no extra installation needed.

---

## 1. Building the Project

Open a terminal at the root of the repository (`C:\ReposGitHub\App-CSD-ER`). Then run:

```bash
# Ensure you are using the Gradle wrapper
./gradlew clean
```

This will clean previous build artefacts.

### 1.1 Android App

```bash
# Assemble a debug APK (this also runs the Room seeder)
./gradlew :app:assembleDebug
```

The generated APK will be located at:
`app/build/outputs/apk/debug/app-debug.apk`

### 1.2 Desktop Admin App (Compose for Desktop)

```bash
# Build and run the desktop application
./gradlew :desktop:run
```

The desktop app will launch automatically after the build finishes.

---

## 2. Running the Android App

### 2.1 Using an Android Emulator (AVD)
1. Open **Android Studio** → **Tools** → **AVD Manager**.
2. Create a new Virtual Device (e.g., **Pixel 5**, API 33) if you don't have one.
3. Start the AVD.
4. In the terminal, install and launch the app:
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   adb shell am start -n com.example.app/.MainActivity
   ```
   - `-r` replaces an existing installation.
   - The activity name may differ; check `AndroidManifest.xml` for the exact launch activity.

### 2.2 Using a Physical Device
1. Enable **Developer Options** → **USB Debugging** on your phone.
2. Connect the phone via USB.
3. Verify the device is recognized:
   ```bash
   adb devices
   ```
   You should see your device listed.
4. Install and launch the app with the same commands as above.

---

## 3. Running the Desktop Admin Application

The desktop module is a standalone JVM application that starts a window with the admin UI.

```bash
./gradlew :desktop:run
```

If you prefer to run the compiled JAR directly:
```bash
# After a successful build
java -jar desktop/build/libs/desktop.jar
```

---

## 4. Verifying Data Synchronisation

Both the Android app and the desktop admin app share the same SQLite database located at:
`app/src/main/assets/club_social_futbol_db` (Android) and `desktop/src/main/resources/club_social_futbol_db` (Desktop). When you run the apps on the same machine, they will read/write to the same file.

1. **Seed the database** – The first launch of the Android app automatically runs `DatabaseSeeder` to import `padron_socios.csv`.
2. **Open the Desktop Admin** – You should see the list of members populated.
3. **In the Android app** – Navigate to the *Consulta de Saldos* screen (the new tab you created). The list of paid / unpaid installments should reflect the same data.
4. **Modify a member’s status** in the desktop app and refresh the Android view (pull‑to‑refresh or restart the app) – the change should be visible, confirming the shared SQLite file works.

---

## 5. Common Issues & Troubleshooting

| Symptom | Likely Cause | Fix |
|---|---|---|
| `adb: command not found` | Android SDK platform‑tools not in PATH | Add `<android-sdk>/platform-tools` to your system `PATH` or use the full path (`C:\Users\<user>\AppData\Local\Android\Sdk\platform-tools\adb.exe`). |
| App crashes on launch | Database file not found or permission issue | Ensure the app has read/write permission to `filesDir`. On the emulator, the DB is copied from assets on first launch. |
| Desktop UI shows blank screen | Missing `Compose` runtime libraries | Run `./gradlew :desktop:run` again; the wrapper will download the required dependencies. |
| Gradle task not found (`:desktop:run`) | Submodule not registered | Verify `settings.gradle.kts` contains `include(":desktop")`. |

---

## 6. Clean Build & Re‑seed

If you need to start from a fresh database:
```bash
# Remove the existing DB (Android side)
rm -f app/src/main/assets/club_social_futbol_db
# Remove the Desktop copy
rm -f desktop/src/main/resources/club_social_futbol_db
# Re‑run the Android app – the seeder will recreate the DB from CSV.
./gradlew :app:assembleDebug && adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 7. Next Steps

- Explore the **Member Dashboard** → **Consulta de Saldos** tab on Android.
- Use the **Desktop Admin** UI to add, edit, or delete members and verify the changes instantly on Android.
- When ready, you can generate a signed release APK (`./gradlew :app:assembleRelease`) and distribute it via the Play Store or side‑load it on devices.

---

**Happy testing!** If you encounter any issues, feel free to ask for clarification.
