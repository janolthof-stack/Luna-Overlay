# Luna Overlay

Android overlay with a prototype for automatic, local ChatGPT UI state detection.

## Automatic prototype

Enable **Luna Overlay – automatische Zustandsanzeige** once in Android accessibility settings. When ChatGPT is the active app, the service creates its own accessibility overlay. Conversation states do not require buttons. The previous manual overlay is stopped when opening Luna's settings.

Only visible control/status labels in `com.openai.chatgpt` are considered. No microphone, network, gestures or conversation logging is used. Only the last detected state/error label is saved locally. Conversation text must not be fed to the detector.

This is **not a verified voice integration**. The label vocabulary is provisional and must be checked against an actual device. A microphone-mute button does not establish that the user is speaking. Missing or conflicting labels yield `UNKNOWN`, with an explicit caption and the waiting picture; states are never advanced by a simulated conversation timer. The overlay hides outside ChatGPT and when the screen is off.

The existing four JPG assets are still static pictures. Real idle/listening/typing/speaking sequences remain unfinished. This prototype does not claim to implement them.

## Validation

`gradle testDebugUnitTest assembleDebug` compiles the APK and checks missing, conflicting, localized, and quoted signals. Tests use synthetic labels, not recordings from ChatGPT. The branch workflow uploads a debug APK for diagnosis; it is not a permanently signed release and must not be represented as an in-place update of a previously installed build.

Before release, verify foreground/background transitions, service restart, device restrictions, and real voice-state labels on Android. The prototype cannot guarantee correct automatic voice states until that device check succeeds.
