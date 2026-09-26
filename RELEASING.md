# Releasing a New Version

Step-by-step guide for publishing a new release of DecomposeNavigator to Maven Central. This is a
manual, human action by design — no CI job and no assistant runs any step of this on its own
initiative (see `.claude/CLAUDE.md` § 5).

## Prerequisites

Make sure the following **GitHub Secrets** are configured in the repository
(`Settings → Secrets and variables → Actions → Repository secrets`):

| Secret                   | Description                                                                    |
|--------------------------|--------------------------------------------------------------------------------|
| `MAVEN_CENTRAL_USERNAME` | Sonatype Central Portal token username                                         |
| `MAVEN_CENTRAL_PASSWORD` | Sonatype Central Portal token password                                         |
| `GPG_KEY_ID`             | Last 8 characters of your GPG key ID                                           |
| `GPG_KEY_PASSWORD`       | Passphrase for the GPG key                                                     |
| `GPG_KEY`                | Base64-encoded GPG private key (`gpg --export-secret-keys <KEY_ID> \| base64`) |

## Step 1 — Update the version

Every module (`decomposenavigator-core`, `decomposenavigator-testing`, `decomposenavigator-bom`)
reads its version from the single `decomposenavigator.version` property in the root
[`gradle.properties`](gradle.properties). Update it once:

```properties
decomposenavigator.version=0.2.0   # ← new version, propagates to every module and the BOM
```

## Step 2 — Update README installation examples

Update the version in every `implementation(...)` / `platform(...)` snippet in the README so it
matches the new version.

## Step 3 — Commit and push

Commit on a task branch (never directly to `main`/`develop`), following `.claude/CLAUDE.md` § 4, then
push and merge to `develop` and from there to `main` in the normal way.

## Step 4 — Create a GitHub Release

Tag `main` at the release commit and create a GitHub Release for that tag. The `publish.yml` workflow
runs on `release: created` and does the rest.

## Step 5 — Monitor the workflow

Watch the Actions run: it checks the ABI surface, runs Android/iOS/JVM unit tests, and only then
publishes.

## Step 6 — Verify on Maven Central

Confirm the new version resolves from `https://repo1.maven.org/maven2/io/github/jamal-wia/` (may take
a few hours to sync after the Central Portal release).

## Manual publishing (without GitHub Actions)

```bash
export ORG_GRADLE_PROJECT_mavenCentralUsername=...
export ORG_GRADLE_PROJECT_mavenCentralPassword=...
export ORG_GRADLE_PROJECT_signingInMemoryKeyId=...
export ORG_GRADLE_PROJECT_signingInMemoryKeyPassword=...
export ORG_GRADLE_PROJECT_signingInMemoryKey="$(cat private.pgp.asc)"
./gradlew publishAndReleaseToMavenCentral --no-configuration-cache
```

## Troubleshooting

- **Signing fails with a key-format error** — the in-memory key must be the ASCII-armored private
  key block (`-----BEGIN PGP PRIVATE KEY BLOCK-----`), not the raw exported binary.
- **A module is missing from the published BOM** — check `decomposenavigator-bom/build.gradle.kts`'s
  `constraints { }` block lists every published artifact; it is not derived automatically from
  `settings.gradle.kts`.
