# mMove

Keeps your PC awake and Teams green by moving the real mouse cursor every 20 s. No admin rights needed.

## Run

**Windows (no Java needed):** unzip `mMove-windows.zip` and double-click `mMove\mMove.exe`. A window opens with sliders for distance and interval, and a Start/Stop button.

**Any OS with Java 22+:** Run from the project directory so Java can load the logo from `assets/`.

```
java --class-path . src/main/java/io/github/giannisdal/mmove/MMove.java
java --class-path . src/main/java/io/github/giannisdal/mmove/MMove.java 1 20
```

Arguments are `[pixels] [intervalSeconds]` and default to 200 pixels every 20 seconds. On macOS, allow Accessibility for your terminal on first run.

## Build the exe (Windows)

```
mkdir build\in
javac --release 22 -d build\classes --source-path src\main\java src\main\java\io\github\giannisdal\mmove\MMove.java
jar cfm build\in\mmove.jar manifest.txt -C build\classes . -C . assets
jpackage --type app-image --name mMove --input build\in --main-jar mmove.jar --main-class io.github.giannisdal.mmove.MMove --icon assets\logo.ico --dest dist --add-modules java.desktop
```

`manifest.txt` contains `Main-Class: io.github.giannisdal.mmove.MMove`.


