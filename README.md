<!-- Claude: this file is the setup guide for people. Follow CLAUDE.md, not this file. When starting a new app, the start-app skill replaces this file with a README for the app. -->

# Build Android apps from your phone with Claude

## Start a new app (3 steps)

1. **[Create your app's repository](https://github.com/new?template_owner=wurp&template_name=vibe-dev-template&owner=@me&visibility=private).** Name it (for example `plant-diary`), optionally say what the app should do under **Description**, and tap **Create repository**.
2. **[Give Claude access to it](https://github.com/apps/claude/installations/new).** This opens a GitHub web page (there's nothing to install on your phone). Choose your account, then under **Repository access** add the new repository and save. Skip this if Claude can already access all your repositories.
3. **[Open Claude Code](https://claude.ai/code?prompt=Start%20my%20new%20app%20from%20this%20repository%27s%20description.).** Tap the repository name above the message box and **choose your new repository** (Claude opens the one you used last). Then send:

   ```
   Start my new app from this repository's description.
   ```

   Or just describe the app you want.

First time? You'll need a **Claude Pro or Max** plan and a free **GitHub** account, and you connect GitHub to Claude once at [claude.ai/code](https://claude.ai/code). See [Setup in detail](https://github.com/wurp/vibe-dev-template/wiki/Setup-in-detail).

## Install it

When Claude says it's ready, it gives you a link.

- Long-press the link, choose **Copy link**, and paste it into **Chrome**, signed in to GitHub. (Downloads can stall inside the Claude app.)
- If Chrome warns about the file, tap **Download anyway**, then tap the download.
- The first time, Android asks you to allow installs from Chrome, and Google Play Protect asks you to confirm an app from an unknown developer. Both are normal for apps not from the Play Store.
- Play Protect scans each new version before it installs. Just wait for it.

After that, tell Claude what to change. The link stays the same, and the new version installs over the old one.

## If something goes wrong

- **The app crashed:** open it again. It shows a **Copy for Claude** button. Tap it and paste the result to Claude.
- **Anything else:** see [Troubleshooting](https://github.com/wurp/vibe-dev-template/wiki/Troubleshooting).

## More

- [Setup in detail](https://github.com/wurp/vibe-dev-template/wiki/Setup-in-detail): first-time setup, and making the repository public to share your app.
- [Faster checks with one network setting](https://github.com/wurp/vibe-dev-template/wiki/Faster-checks).
- [Using a computer or an API key](https://github.com/wurp/vibe-dev-template/wiki/Using-a-computer-or-an-API-key).
- [How it works](https://github.com/wurp/vibe-dev-template/wiki/How-it-works), for developers.
- [Costs and limits](https://github.com/wurp/vibe-dev-template/wiki/Costs-and-limits).
- Questions and ideas: [Discussions](https://github.com/wurp/vibe-dev-template/discussions).

MIT licensed. Not affiliated with Anthropic.
