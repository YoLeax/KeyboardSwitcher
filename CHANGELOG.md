# Changelog

## v4.3.0

Based on commits `25682a8 feat(keyboard): add direct switching and floating button controls`
and `d2ec043 fix(keyboard): stabilize direct input method switching`.

### Added

- Added direct switching between two configured keyboards.
- Added settings for selecting the first and second keyboards used by direct switching.
- Added secure settings permission guidance for ADB and Shizuku.
- Added Shizuku dependencies, provider configuration, and runtime permission flow for granting `WRITE_SECURE_SETTINGS`.
- Added an accessibility-service mode that shows the floating button only while the soft keyboard is open.
- Added floating button background color customization with a visible semi-transparent default background.
- Added a GitHub Actions workflow that builds a signed release APK and publishes it to GitHub Releases when a `v*` tag is pushed.

### Changed

- Refactored floating button creation, dragging, position saving, and click handling into `FloatingButtonController`.
- Updated `KeyboardSwitcherService` to delegate floating button behavior to the shared controller.
- Updated floating button color semantics so icon color and background color are controlled separately.
- Replaced the old cutout-style floating button icon with a normal keyboard foreground glyph.
- Updated the About screen icon to use the app launcher instead of the floating button drawable.
- Updated localized labels for the new direct switching, accessibility, icon color, and background color settings.

### Fixed

- Stabilized direct input method switching by handling secure settings write failures.
- Improved current input method detection on newer Android versions.
- Restored focused editor selection after direct switches when possible to reduce cursor jumps.

## v4.3.1

### Fixed

- Switched Shizuku secure settings grants to a UserService shell command path for Android 14 compatibility.
