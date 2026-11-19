# mMove

Keeps your PC awake and Teams green by moving the real mouse cursor every 20 s. No admin rights needed.

## Run

**macOS:** Open `mMove.app` from Finder. To build it locally, run the commands below from the project directory with JDK 22 or newer.

```sh
./scripts/build-macos.sh
open dist/mMove.app
```

You can drag `dist/mMove.app` into Applications. The bundle includes its own Java runtime. If macOS asks for permission to move the pointer, enable **mMove** under **System Settings → Privacy & Security → Accessibility**.

**Windows (no Java needed):** unzip `mMove-windows.zip` and double-click `mMove\mMove.exe`. A window opens with sliders for distance and interval, and a Start/Stop button.

**Run from source with Java 22+:** Run from the project directory so Java can load the logo from `assets/`.

```
java --class-path . src/main/java/io/github/giannisdal/mmove/MMove.java
java --class-path . src/main/java/io/github/giannisdal/mmove/MMove.java 1 20
```

Arguments are `[pixels] [intervalSeconds]` and default to 200 pixels every 20 seconds. When running from source on macOS, grant your terminal Accessibility access if prompted.

## Build the exe (Windows)

```
mkdir build\in
javac --release 22 -d build\classes --source-path src\main\java src\main\java\io\github\giannisdal\mmove\MMove.java
jar cfm build\in\mmove.jar manifest.txt -C build\classes . -C . assets
jpackage --type app-image --name mMove --input build\in --main-jar mmove.jar --main-class io.github.giannisdal.mmove.MMove --icon assets\logo.ico --dest dist --add-modules java.desktop
```

`manifest.txt` contains `Main-Class: io.github.giannisdal.mmove.MMove`.


