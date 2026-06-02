# Quick Start — GitHub only (no Android Studio needed)

You said GitHub is comfortable, so here's the whole path in 4 steps.

## 1. Create a repo and push these files
On github.com: click **New repository**, name it (e.g. `boarding-buddy`), create it.
Then either drag-and-drop all these files into the web uploader, **or** from a
terminal in this folder:

```
git init
git add .
git commit -m "Boarding Buddy"
git branch -M main
git remote add origin https://github.com/YOUR_USERNAME/boarding-buddy.git
git push -u origin main
```

## 2. The build starts automatically
Pushing triggers the workflow in `.github/workflows/build.yml`. It will:
- generate the Gradle wrapper for you (so you don't need Android Studio),
- run the unit tests,
- build the APK.

## 3. Download the APK
Go to the **Actions** tab in your repo → click the latest run → scroll to
**Artifacts** at the bottom → download **boarding-buddy-debug-apk**.
Unzip it to get `app-debug.apk`.

## 4. Install on your Android phone
Transfer `app-debug.apk` to your phone (email it to yourself, Google Drive,
or USB). Tap it. Android will ask to allow installing from this source — allow it.
Open **Boarding Buddy** and point the camera at a boarding pass barcode.

---

### If the build ever fails
Open the failed step in the Actions log. The most common cause is the very first
build needing to download dependencies — just re-run the job (top-right
**Re-run jobs** button). Everything needed is already in the repo.
