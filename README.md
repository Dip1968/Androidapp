# Proximity Alert POC — Android

This is the first foreground BLE proof-of-concept.

## Test
- Phone A: select `દૂધવાળો (Sender)` and tap `START POC`.
- Phone B: select `ઘર (Receiver)` and tap `START POC`.
- Enable Bluetooth and grant the requested Bluetooth permissions.
- Bring the phones close together.
- Phone B should show `દૂધવાળો નજીક છે!` and make a notification beep.

## Build without Android Studio
This repository includes `.github/workflows/build-apk.yml`.
GitHub Actions builds a debug APK and publishes it as an artifact.

## Important
This V1 is intentionally foreground-only. Background/locked-screen detection, Gujarati speech/TTS, accounts, and production hardening are not included yet.
