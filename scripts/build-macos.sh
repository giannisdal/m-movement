#!/bin/sh
set -eu

if [ "$(uname -s)" != Darwin ]; then
    echo "Build mMove.app on macOS with JDK 22 or newer." >&2
    exit 1
fi

project_dir=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
cd "$project_dir"
destination=${1:-"$project_dir/dist"}

mkdir -p build "$destination"
work_dir=$(mktemp -d "$project_dir/build/macos.XXXXXX")
trap 'rm -rf "$work_dir"' EXIT
mkdir -p "$work_dir/input"

main_class=io.github.giannisdal.mmove.MMove
javac --release 22 -Xlint:all -Werror --source-path src/main/java -d "$work_dir/classes" \
    src/main/java/io/github/giannisdal/mmove/MMove.java
jar cfm "$work_dir/input/mmove.jar" manifest.txt -C "$work_dir/classes" . assets/logo.png
java -Djava.awt.headless=true -cp "$work_dir/input/mmove.jar" \
    io.github.giannisdal.mmove.AppIcon "$work_dir/mMove.iconset"
iconutil --convert icns --output "$work_dir/mMove.icns" "$work_dir/mMove.iconset"

jpackage --type app-image --name mMove --mac-package-identifier io.github.giannisdal.mmove \
    --input "$work_dir/input" --main-jar mmove.jar --main-class "$main_class" \
    --icon "$work_dir/mMove.icns" --dest "$destination" --add-modules java.desktop

printf 'Built %s/mMove.app\n' "$destination"
