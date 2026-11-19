# mMove

mMove is a small desktop utility that keeps an interactive session active during breaks in keyboard and mouse input. It is intended for situations where you want the desktop to stay available while you step away briefly or watch a long-running task.

**Runs with standard user privileges.** On macOS, mMove uses Accessibility permission to move the pointer.

## How it works

When you press **Start**, mMove reads the current position of the real mouse pointer, moves it to the right, and returns it to where it started. The first movement starts immediately. Each following movement starts after the configured interval has elapsed since the previous one finished.

The goal is to reduce idle time by generating mouse activity. Your operating system and any organization-managed policies still determine when the screen locks or the computer sleeps. Applications such as Microsoft Teams apply their own presence rules.

## Controls

| Control | What it does | Default |
| --- | --- | --- |
| **Distance** | Sets how far the pointer moves on each cycle, from 1 to 400 pixels. | 200 pixels |
| **Interval** | Sets the delay between cycles, from 1 to 120 seconds. | 20 seconds |
| **Start / Stop** | Starts or stops automatic movement. | Stopped |

Set a short distance, such as **1 pixel**, if you want the movement to be less noticeable. Both sliders take effect while the app is running. The status line shows whether automatic movement is active and the local time of the last completed cycle. Press **Stop** or close the window when you are done. Opening mMove again brings the existing window forward.

## Open the app

### macOS

Open **mMove.app** from Finder. You can move the app to **Applications** and launch it from there. Its Dock icon uses the mMove logo, and the bundle includes its own Java runtime.

To enable pointer movement, go to **System Settings → Privacy & Security → Accessibility** and enable **mMove**. If the app is missing from the list, add it from Applications.

### Windows

If you have a packaged build, extract the entire archive and double-click `mMove.exe` inside the extracted `mMove` folder. Keep the extracted folder together so the launcher can find its bundled Java runtime.

### Run from source

With JDK 11 or later, run this command **from the project directory** so Java can find the logo in `assets/`.

```sh
java --class-path . src/main/java/io/github/giannisdal/mmove/MMove.java
```

To start with a distance of 1 pixel and a 20-second interval, pass the values in the order `[pixels] [intervalSeconds]`. Press **Start** in the window to begin moving the pointer.

```sh
java --class-path . src/main/java/io/github/giannisdal/mmove/MMove.java 1 20
```

When running from source on macOS, grant Accessibility access to the terminal app you use to launch mMove.

## Build a native app

Build on the target operating system with **JDK 11 or later**. Both builds bundle a Java runtime.

### macOS app bundle

```sh
./scripts/build-macos.sh
open dist/mMove.app
```

The script compiles the Java sources, generates a macOS icon from the project logo, and uses `jpackage` to create `dist/mMove.app`. You can pass a different output directory as the first argument to the script.

### Windows app image

Run these commands in Command Prompt from the project directory.

```bat
mkdir build\in
javac --release 11 -d build\classes --source-path src\main\java src\main\java\io\github\giannisdal\mmove\MMove.java
jar cfm build\in\mmove.jar manifest.txt -C build\classes . -C . assets
jpackage --type app-image --name mMove --input build\in --main-jar mmove.jar --main-class io.github.giannisdal.mmove.MMove --icon assets\logo.ico --dest dist --add-modules java.desktop
```

The launcher is at `dist\mMove\mMove.exe`. To share the build, zip the entire `dist\mMove` folder.

## Project layout

- `src/main/java/io/github/giannisdal/mmove/` contains the Swing interface, movement scheduler, and icon code. Pointer movement uses the standard Java AWT `Robot` API.
- `assets/` contains the logo and Windows icon.
- `scripts/build-macos.sh` builds the native macOS bundle.
- `manifest.txt` declares the JAR entry point.
