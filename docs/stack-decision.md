# Stack decision — native Kotlin, not Expo/React Native

**Context.** An earlier Expo/React Native monorepo for Lifesaver exists at
`OneDrive/Documents/Lifesaver`. This project (`OneDrive/Desktop/Lifesaver app`) is the native
Android app the PRD actually specifies, built fresh from `PRD.md` + `DESIGN.md`.

**Decision.** `PRD.md` is the single source of truth and mandates **Kotlin + Jetpack Compose +
Room + DataStore, minSdk 26**. The RN project does not match that stack. We build native here.
Reusable *logic* from the RN project's Kotlin interceptor module (foreground detection, day-key /
open-count accounting, a raw-View pause screen) informed this implementation but was re-architected
onto Room/DataStore/Compose rather than copied.

**Toolchain (proven on this machine).** Gradle 9.6.1, AGP 9.2.1 (built-in Kotlin overridden to
2.3.10 so the Compose compiler + KSP align), Compose BOM 2026.06, Room 2.8.4. compileSdk 37
(required by core 1.19 / lifecycle 2.11), targetSdk 36, minSdk 26. Build with Android Studio's
bundled JBR (JDK 21) as `JAVA_HOME` and the SDK at `AppData/Local/Android/Sdk` as `ANDROID_HOME`:

```bash
JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" \
ANDROID_HOME="C:/Users/<you>/AppData/Local/Android/Sdk" \
./gradlew :app:assembleDebug
```

**Scope built.** All of PRD v1 (milestones M1–M7). v1.1 (§9: session profiling, integrity layer,
browser blocking, life layer, weekly report, streak insurance, backup/restore) is the next phase;
the Room schema and `DetectionConfig` are already forward-compatible with it. The §9.1 override
(2-day baseline, not §6's 3) is adopted now.

**Detection fragility (§3.4).** Every Instagram/YouTube/browser marker lives in one file —
`detection/DetectionConfig.kt`. When IG/YT change their UI, that file (and the Debug screen's live
view-ID list) is where repairs happen.
