# CRYSTAL.BIN — Sugar Bag Counter

Offline Android application for counting sugar bags from the live camera feed using an ONNX object-detection model.

## Current project

- Package: `com.vea.cementbagcounter`
- Android compile/target SDK: 35
- Minimum SDK: 26
- Java/Kotlin target: 17
- CameraX + ONNX Runtime Android
- Offline device-bound activation

## Build

### Android Studio
1. Open this project folder.
2. Allow Gradle sync to complete.
3. Select **Build > Build APK(s)** for a test/installable debug APK.
4. The debug APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.

### GitHub Actions
The workflow in `.github/workflows/build-apk.yml` builds both debug and release variants and uploads them as an artifact. The debug APK is installable for testing. The release APK is signed only if these GitHub Actions secrets are configured: `CRYSTAL_KEYSTORE_BASE64`, `CRYSTAL_KEYSTORE_PASSWORD`, `CRYSTAL_KEY_ALIAS`, and `CRYSTAL_KEY_PASSWORD`. Otherwise, the release APK is unsigned.

Create a release keystore locally (never in the repository) and save the four values as repository Actions secrets to enable stable release signing. Keep a secure backup of the keystore and passwords; losing them prevents future signed updates.

## License issuer

Use the separate CRYSTAL.BIN License Issuer tool. The old VEA generator in this project was incompatible and has been removed. The RSA private key must stay outside this repository.

## Important validation limits

The model is bundled as `app/src/main/assets/best.onnx`. Build success alone does not validate its detection accuracy. Test the final APK with representative factory/conveyor video and on the target phone before deployment.
