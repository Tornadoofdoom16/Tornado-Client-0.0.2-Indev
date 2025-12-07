#!/bin/bash

# Tornado Client Build Script
# Compiles the Fabric 1.21 mod

echo "=== Tornado Client Build System ==="
echo ""
echo "Building Tornado Client v1.0.0 for Minecraft 1.21..."
echo ""

# Check Java version
JAVA_VERSION=$(java -version 2>&1 | grep -oP 'version "\K[^"]+')
echo "Java Version: $JAVA_VERSION"

# Run Gradle build
echo ""
echo "Starting Gradle build..."
./gradlew build

# Check if build was successful
if [ $? -eq 0 ]; then
    echo ""
    echo "✅ Build successful!"
    echo ""
    echo "Output: build/libs/tornadoclient-1.0.0.jar"
    echo ""
    echo "To install:"
    echo "  1. Open your Minecraft mods folder"
    echo "  2. Copy build/libs/tornadoclient-1.0.0.jar to the mods folder"
    echo "  3. Launch Minecraft with Fabric loader"
    echo ""
else
    echo ""
    echo "❌ Build failed. Check the errors above."
    exit 1
fi
