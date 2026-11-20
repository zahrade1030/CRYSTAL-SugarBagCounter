# CRYSTAL.BIN Offline Licensing

The Android app accepts licenses in this format:

`CRYSTAL1.<Base64URL payload>.<Base64URL RSA signature>`

Product ID: `CRYSTAL_BAG_COUNTER`  
Payload version: `1`  
Signature: RSA / SHA-256

Use the separate **CRYSTAL.BIN License Issuer** desktop tool to create licenses. It must use the private key that matches the public key embedded in `LicenseManager.kt`.

The previous VEA license scripts were removed because they generated a different product ID and token format and were not compatible with this app.

**Never commit or share the private signing key.** Do not run a new key-generation script for this app unless the app's embedded public key is deliberately updated and all existing licenses are migrated.
