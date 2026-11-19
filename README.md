# mMove

Keeps your PC awake and Teams green by moving the real mouse cursor every 20 s. No admin rights needed.

## Run

**Windows (no Java needed):** unzip `mMove-windows.zip` and double-click `mMove\mMove.exe`. A window opens with sliders for distance and interval, and a Start/Stop button.

**Any OS with Java 11+:**

```
java MMove.java            # test mode: 200 px, every 20 s
java MMove.java 1 20       # minimal: 1 px, every 20 s
```

Arguments: `[pixels] [intervalSeconds]`. On macOS, allow Accessibility for your terminal on first run.

## Build the exe (Windows)

```
javac -d build\classes MMove.java
jar cfm build\in\mmove.jar manifest.txt -C build\classes .
jpackage --type app-image --name mMove --input build\in --main-jar mmove.jar --main-class MMove --icon logo.ico --dest dist --add-modules java.desktop
```

`manifest.txt` contains `Main-Class: MMove`.


