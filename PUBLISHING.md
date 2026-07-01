# Publishing (JetBrains Marketplace)

Prerequisites (one-time):
1. Create a JetBrains Marketplace account at https://plugins.jetbrains.com/ and a
   Hub **permanent token** (for `publishPlugin`).
2. (Recommended) Generate a signing certificate/key:
   ```bash
   openssl genpkey -aes-256-cbc -algorithm RSA -out private.pem -pkeyopt rsa_keygen_bits:4096
   openssl req -key private.pem -new -x509 -days 3650 -out chain.crt
   ```
3. Verify the plugin name is available on the Marketplace. The first upload goes
   through moderation.

Publish (CLI):
```bash
export JAVA_HOME="/Users/k/.asdf/installs/java/corretto-17.0.14.7.1"; export PATH="$JAVA_HOME/bin:$PATH"
export CERTIFICATE_CHAIN="$(cat chain.crt)"
export PRIVATE_KEY="$(cat private.pem)"
export PRIVATE_KEY_PASSWORD="<key password>"
export PUBLISH_TOKEN="<marketplace token>"
./gradlew publishPlugin
```
Or upload `build/distributions/*.zip` manually via the Marketplace web UI (unsigned
is accepted for manual upload).

Notes:
- The LSP features require a paid IDE (LSP API), **Node.js**, and the **maude**
  executable on PATH; highlighting/folding work everywhere.
- Bump `version` in `build.gradle.kts` and update `<change-notes>` per release.
