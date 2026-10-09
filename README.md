# Luna Overlay

Android overlay with a prototype for automatic, local ChatGPT UI state detection.

## Automatic prototype

Enable **Luna Overlay – automatische Zustandsanzeige** once in Android accessibility settings. When ChatGPT is the active app, the service creates its own accessibility overlay. Conversation states do not require buttons. The previous manual overlay is stopped when opening Luna's settings.

Only visible control/status labels in `com.openai.chatgpt` are considered. No microphone, network, gestures or conversation logging is used. Only the last detected state/error label is saved locally. Conversation text must not be fed to the detector.

This is **not a verified voice integration**. The label vocabulary is provisional and must be checked against an actual device. A microphone-mute button does not establish that the user is speaking. Missing or conflicting labels yield `UNKNOWN`, with an explicit caption and the waiting picture; states are never advanced by a simulated conversation timer. The overlay hides outside ChatGPT and when the screen is off.

The existing four JPG assets are still static pictures. The automatic overlay now supports real looping GIF sequences at `app/src/main/assets/luna/idle.gif`, `listening.gif`, `thinking.gif`, and `speaking.gif`. Each must be an actual multi-frame character animation with a positive duration. The player uses monotonic time, scales and centers the clip, and schedules rendering only while attached and visible. Missing, invalid, or zero-duration media falls back to the existing picture with the explicit caption **Animation fehlt**. No sequence is bundled yet: real idle/listening/typing/speaking assets and visual device verification remain unfinished.

The permanent-signature workflow last failed because the repository secret `LUNA_SIGNING_JSON` was absent. Debug build success does not establish upgrade compatibility. Restore the original signing material before claiming an in-place update; a new key cannot prove compatibility with the installed APK.

## Validation

`gradle testDebugUnitTest assembleDebug` compiles the APK and checks missing, conflicting, localized, and quoted signals. Tests use synthetic labels, not recordings from ChatGPT. The branch workflow uploads a debug APK for diagnosis; it is not a permanently signed release and must not be represented as an in-place update of a previously installed build.

Before release, verify foreground/background transitions, service restart, device restrictions, and real voice-state labels on Android. The prototype cannot guarantee correct automatic voice states until that device check succeeds.
