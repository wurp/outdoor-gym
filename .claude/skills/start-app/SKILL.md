---
name: start-app
description: Set up a brand-new app from the Vibe Dev template. Use when app.properties still has applicationId=io.github.vibedev.starter, or when the author says they're starting a new app.
---

# Start a new app

The author has just copied the template. Get them from "I have an idea" to an app on their phone as fast as possible. Speak plainly (see `CLAUDE.md`).

## 1. Ask about the idea

First read the repository's description, which the author may have filled in when creating it: `gh api repos/{owner}/{repo} --jq .description`. Treat it, and anything they've said in the chat, as their idea; confirm it briefly instead of asking again. If there's no idea yet, ask what the app should do, in one message. Then ask for a name, suggesting two or three based on the idea. Keep it to one or two rounds of questions. You can refine later.

## 2. Set the app's identity

1. **Find the owner and repo** from the last two parts of `git remote get-url origin`.
2. **Edit `app.properties`:**
   - `appName=` the name they chose.
   - `applicationId=io.github.<owner>.<slug>`:
     - `<owner>` is the GitHub username, lowercased, with `-` changed to `_`.
     - `<slug>` is the app name, lowercased, letters and digits only, starting with a letter.
     - For example: `io.github.jane_doe.plantdiary`.
   - This ID is permanent once the app is shared, so get it right now.
3. **Make a signing key unique to this app.** The template's key is shared by everyone who copies the template, so replace it once, now:
   ```
   rm keystore/debug.keystore
   keytool -genkeypair -keystore keystore/debug.keystore -storepass android -keypass android \
     -alias androiddebugkey -keyalg RSA -keysize 2048 -validity 10000 \
     -dname "CN=<appName> test key"
   ```
   Do this only before the first build. Changing it later stops new versions from installing over old ones.

## 3. Replace the README

`README.md` is the template's setup guide, which this app doesn't need. Replace it with a short README for the app:

```markdown
# <appName>

<One or two plain sentences: what the app does and who it's for.>

## Install

On an Android phone, open this link in Chrome and install the file it downloads:
https://github.com/<owner>/<repo>/releases/download/test-build/app.apk

## Keep building

In the Claude app, open **Code** → **New session**, choose `<repo>` above the message box, and describe what to change. (On a computer, [this link](https://claude.ai/code?repositories=<owner>/<repo>) opens it directly.)

Built with [vibe-dev-template](https://github.com/wurp/vibe-dev-template) and Claude.
```

Keep it current when the app changes in ways the description should mention.

## 4. Build the first version

Replace `StarterScreen` in `MainActivity.kt` with a simple first version of their idea: one screen that does the core thing, even if crudely. Keep `CrashReportDialog()` in `setContent`. Optionally change `Brand` in `ui/theme/Theme.kt` to a color that suits the app.

## 5. Ship it

Follow "After every change" in `CLAUDE.md`. When the build is ready, send the link and, since this is the first time, the short "Installing on the phone" explanation. End by asking what they'd like to change or add next.
