# Boarding Buddy (Android)

A senior-friendly boarding companion for **Delhi IGI Terminal 3 (Domestic Departures)**.
Scan a boarding pass with the phone camera, confirm the gate, and follow a large-text,
turn-by-turn walking route to the gate — no wheelchair needed.

## What it does
- **Scan** the PDF417 barcode on any boarding pass (Google ML Kit, on-device, offline).
- **Parse** the IATA BCBP data: name, flight, route, seat.
- **Confirm gate** — the gate isn't in the barcode (it's assigned later), so the
  traveller confirms/types it. The app then picks the right T3 section automatically
  (Gates 27–36 → Section C, 37–62 → Section D).
- **Route** — big-text, one-step-at-a-time walking guidance with landmarks and rest benches.

## Build it without installing anything locally
1. Create a new GitHub repo and push these files.
2. The included GitHub Actions workflow (`.github/workflows/build.yml`) runs on every push:
   it runs the unit tests and builds a debug APK.
3. Open the workflow run → **Artifacts** → download `boarding-buddy-debug-apk`.
4. Copy the APK to your Android phone and install (allow "install from unknown sources").

## Or build locally
Open the folder in **Android Studio** (Giraffe or newer), let it sync, then Run.
Requires JDK 17.

## Tested logic
`./gradlew testDebugUnitTest` verifies the BCBP parser field offsets and the T3
gate→section routing.

## Honest limitations
- Walking distances and individual landmark positions are representative, not surveyed.
  A production version would consume DIAL's official T3 indoor map for exact metres.
- Live gate/boarding-time lookup (via a flight-status API) is intentionally left as a
  later add-on; for now the traveller confirms the gate.
- Only the Delhi T3 domestic concourse is modelled.

## ⚠️ One required first step: generate the Gradle wrapper
This project ships without the `gradlew` binary wrapper (it's a generated binary).
Generate it once, either way below, **before** pushing or the CI build will fail:

**If you have Gradle installed locally:**
```
gradle wrapper --gradle-version 8.7
```

**If you don't:** open the folder in Android Studio once — it generates the wrapper
automatically on first sync. Then push to GitHub.

After that, `gradlew`, `gradlew.bat`, and `gradle/wrapper/gradle-wrapper.jar` will
exist and the GitHub Actions workflow will build the APK on every push.
