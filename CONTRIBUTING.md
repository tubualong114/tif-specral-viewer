# Contributing

Contributions are welcome.

1. Use QuPath 0.7.0 and a compatible JDK (OpenJDK 25 is recommended).
2. Keep image reads off the JavaFX application thread.
3. Preserve raw pixel values: display LUTs and contrast settings must not change sampled values.
4. Add or update JUnit tests for non-UI logic.
5. Run `./gradlew clean test build` before submitting a change.

Please do not commit private microscopy data, generated build directories, or local JDK/toolchain archives.
