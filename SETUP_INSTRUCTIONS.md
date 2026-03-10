# Quick Setup Instructions

## Problem: Gradle Build Fails

The error "What went wrong: 25" typically means the project needs proper setup.

---

## Solution: Use Android Studio (Recommended)

**This is the EASIEST way to build Android apps:**

### Step 1: Install Android Studio

If not installed:
```bash
# Download from: https://developer.android.com/studio
# Or via Homebrew:
brew install --cask android-studio
```

### Step 2: Open Project

1. Launch Android Studio
2. Click "Open" or "Open Existing Project"
3. Navigate to: `/Users/janet/ovd/project/bms-integration/bms-android-app`
4. Click "Open"

### Step 3: Let Android Studio Do the Work

Android Studio will automatically:
- ✅ Download Gradle wrapper
- ✅ Download all dependencies (40+ libraries)
- ✅ Configure Android SDK
- ✅ Set up build tools
- ✅ Create necessary files

**This takes 5-10 minutes on first sync.**

### Step 4: Build

Once sync completes:
- Click the green hammer icon (Build)
- Or: Build → Make Project
- Or: Run → Run 'app' (builds and deploys)

---

## Alternative: Command Line (Advanced)

If you must use command line, you need:

### 1. Install Android SDK

```bash
# Via Android Studio (recommended), or
# Via command line tools:
brew install android-sdk
```

### 2. Set ANDROID_HOME

```bash
# Add to ~/.zshrc or ~/.bashrc:
export ANDROID_HOME=$HOME/Library/Android/sdk
export PATH=$PATH:$ANDROID_HOME/tools
export PATH=$PATH:$ANDROID_HOME/platform-tools
```

### 3. Accept Licenses

```bash
sdkmanager --licenses
```

### 4. Download Gradle Wrapper JAR

The wrapper script exists, but needs the JAR file:

```bash
cd /Users/janet/ovd/project/bms-integration/bms-android-app

# Download wrapper JAR manually
curl -L https://services.gradle.org/distributions/gradle-8.2-bin.zip -o gradle.zip
unzip gradle.zip
mv gradle-8.2/lib/gradle-wrapper-8.2.jar gradle/wrapper/gradle-wrapper.jar
rm -rf gradle-8.2 gradle.zip
```

### 5. Build

```bash
./gradlew assembleDebug
```

---

## Why Android Studio is Better

| Method | Setup Time | Ease | Dependencies | SDK Management |
|--------|------------|------|--------------|----------------|
| Android Studio | 5-10 min | ⭐⭐⭐⭐⭐ Easy | Automatic | Automatic |
| Command Line | 30-60 min | ⭐⭐ Hard | Manual | Manual |

**Android Studio handles EVERYTHING automatically!**

---

## What Android Studio Does

When you open the project, it:

1. ✅ Reads `build.gradle.kts` files
2. ✅ Downloads Gradle 8.2
3. ✅ Downloads Android SDK 34
4. ✅ Downloads Kotlin 1.9.21
5. ✅ Downloads 40+ libraries:
   - Jetpack Compose
   - Hilt DI
   - Room Database
   - MQTT (Paho)
   - USB Serial
   - Coroutines
   - Testing libraries
6. ✅ Configures build tools
7. ✅ Sets up Kotlin compiler
8. ✅ Configures code analysis
9. ✅ Ready to build!

---

## Common Issues

### "SDK location not found"

**Solution:** Android Studio will prompt you to download the SDK. Click "Download SDK" and it handles everything.

### "Failed to download Gradle"

**Solution:** Check your internet connection. Android Studio retries automatically.

### "License not accepted"

**Solution:** Android Studio shows a dialog. Click "Accept" for all licenses.

---

## After Setup in Android Studio

Once the project syncs successfully:

### Run Tests

```bash
# In terminal (inside Android Studio or external):
./gradlew test

# Or in Android Studio:
# Right-click on test file → Run 'TestName'
```

### Build APK

```bash
# Command line:
./gradlew assembleDebug

# Or in Android Studio:
# Build → Build Bundle(s) / APK(s) → Build APK(s)
```

### Deploy to Device

```bash
# Command line (device connected):
./gradlew installDebug

# Or in Android Studio:
# Click green play button → Select device → Run
```

---

## Expected First Sync

**Console Output:**
```
> Configure project :app
Kotlin version: 1.9.21
AGP version: 8.2.1

> Task :app:preBuild
> Task :app:preDebugBuild
> Task :app:compileDebugKotlin
> Task :app:compileDebugJavaWithJavac
> Task :app:dexBuilderDebug
> Task :app:mergeExtDexDebug
> Task :app:packageDebug

BUILD SUCCESSFUL in 3m 45s
147 actionable tasks: 147 executed
```

**This is NORMAL for first build!**

---

## Summary: Easiest Path

```
1. Install Android Studio
2. Open project: /Users/janet/ovd/project/bms-integration/bms-android-app
3. Wait for sync (5-10 min)
4. Click Build
5. Done! ✅
```

**Android Studio takes care of EVERYTHING. Just open and wait!** 🎉

---

## Files Already Created

Your project already has:
- ✅ 39 source files
- ✅ 4 unit tests
- ✅ Build configuration
- ✅ Android manifest
- ✅ Resources
- ✅ Complete DDD architecture

**Just needs Android Studio to download dependencies!**

---

## Support

If Android Studio sync fails:
1. File → Invalidate Caches → Invalidate and Restart
2. Try again
3. Check Android Studio version (need 2023.1 or later)

---

**Recommended: Use Android Studio. It's designed exactly for this!** 🚀
