---
name: fix-build
description: Diagnose and fix a failed GitHub Actions build of the app. Use when `gh run watch` reports a failure, or the author says the download link shows an old version.
---

# Fix a failed build

Fix it quietly. The author doesn't need to hear about build failures you can solve.

## 1. Read the error

```
gh run view <run-id> --repo OWNER/REPO --log-failed | tail -80
```

Look for the first line starting with `e:` (a Kotlin error), `error:`, `What went wrong:`, or `FAILURE:`. Later errors are often caused by the first one.

If `gh` can't reach GitHub from this session, and the session start said "In-session build: available," reproduce it locally with `gradle assembleDebug --console=plain`. Otherwise, ask the author to open the repository's **Actions** tab, tap the failed run, and send a screenshot. Use this only as a last resort.

## 2. Common causes

| Symptom | Fix |
|---|---|
| `Unresolved reference` | A missing import, or a misspelled name. |
| `@Composable invocations can only happen from the context of a @Composable function` | Move the call into a composable, or mark the function `@Composable`. |
| `Dependency '...' requires Android Gradle plugin 9...` or `compile against version 37` | A library version is too new. Use an older release (see the comment in `gradle/libs.versions.toml`). |
| `Could not resolve` or `Could not find` for a library | Wrong library name or version. Check it exists on Google Maven or Maven Central. |
| `Duplicate class` | Two libraries provide the same code. Remove one, or use the Compose BOM version. |
| `AAPT: error: resource ... not found` | A missing string, drawable, or other resource, or a typo in its name. |
| `Keystore was tampered with, or password was incorrect` | `keystore/debug.keystore` is corrupt or was replaced with different passwords. Restore it from git history; don't make a new one if the app has been installed. |
| The run was cancelled | A newer push started a fresh build. Not an error; watch the newer run. |

## 3. Push the fix and watch again

Follow "After every change" in `CLAUDE.md`. If the same build fails three times in a row, stop. Tell the author in plain words what you're trying to add and that it's not working yet, and suggest a simpler way to get what they want.
