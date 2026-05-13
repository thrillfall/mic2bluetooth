# Releasing

This project is set up to be distributed via **F-Droid** (built from source on F-Droid's infrastructure) and **IzzyOnDroid** (which mirrors signed APKs from GitHub Releases).

## One-time setup

### 1. Push the project to GitHub

The recipes and metadata in this repo assume the repo lives at `https://github.com/thrillfall/mic2bluetooth`. If you choose a different URL, update:

- `metadata/io.github.thrillfall.mic2bluetooth.yml` — `SourceCode`, `IssueTracker`, `Changelog`, `Repo`
- `README.md` if it grows any links

### 2. GitHub secrets for the release signing key

The release workflow signs APKs (which IzzyOnDroid picks up) using the same key/secret naming convention as the AntennaPod project, so an existing keystore + secrets configured at the org/account level can be reused without changes.

In the GitHub repo → Settings → Secrets and variables → Actions, the following secrets must be available (org-level secrets work too):

| Secret | Value |
|---|---|
| `RELEASE_KEYSTORE_BASE64` | `base64 -w 0 release.jks` |
| `RELEASE_STORE_PASSWORD` | the keystore password |
| `RELEASE_KEY_ALIAS` | the key alias inside the keystore |
| `RELEASE_KEY_PASSWORD` | the key password |

**Keep the keystore safe — losing or rotating it forces every user on the IzzyOnDroid channel to uninstall and reinstall.** F-Droid signs its own APKs separately, so its users are unaffected by this key.

To build a signed release locally (e.g., for testing the workflow output):

```sh
./gradlew :app:assembleRelease \
  -PreleaseStoreFile=/absolute/path/to/release.jks \
  -PreleaseStorePassword=... \
  -PreleaseKeyAlias=... \
  -PreleaseKeyPassword=...
```

Without those `-P` properties, `assembleRelease` produces an unsigned APK (useful for reproducible-build verification).

## Cutting a release

1. Bump `versionCode` and `versionName` in `app/build.gradle.kts`.
2. Add a new `fastlane/metadata/android/en-US/changelogs/<versionCode>.txt`.
3. Update `CHANGELOG.md`.
4. Commit, tag, and push:

   ```sh
   git tag 1.0.0
   git push origin 1.0.0
   ```

5. The `.github/workflows/release.yml` workflow builds, signs, and attaches `mic2bluetooth-X.Y.Z.apk` to the GitHub Release.

## Submitting to IzzyOnDroid

After the first GitHub Release exists:

1. Open an issue at <https://gitlab.com/IzzyOnDroid/repo/-/issues/new> using the "App inclusion" template.
2. Provide the GitHub repo URL. IzzyOnDroid auto-detects new tagged releases via the GitHub API.
3. Once accepted, the app appears at `https://apt.izzysoft.de/fdroid/index/apk/io.github.thrillfall.mic2bluetooth`.

Updates: every new GitHub Release with an attached APK is picked up automatically; no further action required.

## Submitting to F-Droid (main repo)

F-Droid builds from source — it does **not** consume your signed APK.

1. Fork <https://gitlab.com/fdroid/fdroiddata>.
2. Copy `metadata/io.github.thrillfall.mic2bluetooth.yml` from this repo into the fork's `metadata/` directory.
3. Verify locally if you have `fdroidserver` installed:

   ```sh
   fdroid lint io.github.thrillfall.mic2bluetooth
   fdroid build --on-server -v -l io.github.thrillfall.mic2bluetooth
   ```

4. Open a merge request against `fdroiddata`.
5. Reviewer feedback usually arrives within days to a couple of weeks. Once merged, the app appears on f-droid.org after the next index run.

Updates: with `AutoUpdateMode: Version` and `UpdateCheckMode: Tags`, every new git tag is auto-picked up by F-Droid's bot — no further MRs are needed unless metadata changes.

## Reproducibility note

To support F-Droid's reproducible builds verification (optional but recommended), keep the build:

- Free of timestamps in resources / code.
- Pinned to specific dependency versions (no dynamic ranges).
- Free of native code that embeds build paths.

The current build satisfies these. If you add native libraries later, see <https://f-droid.org/docs/Reproducible_Builds/>.
