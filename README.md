# Brainless 16/19/19

A RuneLite plugin for the west-boat, north-island solo Tempoross method. Load 16 cooked fish on the first trip and 19 on subsequent trips. A solid color marks the current target's clickable area. Selecting its action clears the marker while you walk, fish, cook or load.

## Playing in your regular RuneLite window

The Plugin Hub in RuneLite’s wrench sidebar handles installation. Once RuneLite approves and lists this plugin, search for **Brainless 16/19/19** and click **Install**. You can open RuneLite through the Jagex Launcher as usual. [RuneLite Plugin Hub review](https://github.com/runelite/runelite/wiki/Plugin-Hub-Review)

## Testing in a separate development window

1. Run the Gradle development task below. On Windows with a local RuneLite 1.13.8 installation, you can also use `Launch-Development-Client.cmd` after building both JARs. Either method opens a separate development window.
2. In the wrench sidebar, search for **Brainless 16/19/19** and enable it. Pick your highlight color. Enable **Barehanded fishing** only if you have unlocked that method and want to use it.
3. Turn off **Jam’s Solo Tempoross** and any competing Tempoross target overlays in that client if you want just one color on screen. Start a fresh **Solo-start** run. The plugin waits for the active encounter HUD.
4. Use a harpoon or barehanded fishing, a rope or the complete equipped Spirit Angler outfit, and four buckets. Leave room for nineteen fish to follow the normal batches. The plugin can highlight supply crates and the ship pump when you need them.

The launcher uses RuneLite’s development entry point. Jagex-account authentication for a development client has its own setup instructions. Follow RuneLite’s instructions yourself if prompted. [Using Jagex Accounts](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts)

## What happens during a run

| Situation | What you see |
| --- | --- |
| Opening rotation | Gather supplies, fish eight, cook, then fish and cook enough for the first load of sixteen. |
| Later batches | Fish, cook and load batches of nineteen. A visible double spot gets priority while at least two fish remain. The final single catch uses an ordinary spot where available. |
| Correct selection | Selecting the intended action on the marked entity clears its color by the next render. The plugin checks observed activity before advancing. |
| Missed click or interrupted action | The target can reappear after a short period without movement or matching progress. A despawned target loses its marker. |
| Fire, storm, wave or spirit pool | Follow the game’s cues and make these decisions yourself. Routine guidance resumes from the observed checkpoint. |
| Overlap | The fill clips to the viewport and excludes known competing hitboxes and UI controls. If the actual top menu action under the pointer disagrees, the fill hides. Turning the camera can expose more of the target. |
| Unexpected inventory | Dropped fish do not count as loads. Credit requires a crate interaction, proximity, a matching inventory decrease and the corresponding score increase. Uncertain deposit evidence pauses guidance. If you have more cooked fish than the batch needs, set aside the excess before loading. |

You can enable the plugin before entering the minigame. If you enable it after earning points, rejoin in a fresh solo session. A scene change, logout or another player entering clears the tracked session. The initial build has been tested with synthetic observations. The final check is a manually played run on the current game revision.

## Build and run from source

Use a JDK 17 and the supplied Gradle 8.10 wrapper. Java source targets release 11. From this directory:

```powershell
.\gradlew.bat clean build
.\gradlew.bat run
```

The normal Gradle build resolves RuneLite 1.13.8 through the official RuneLite repository. The build produces the plugin and development entry point in `build/libs`. To reproduce the verified Windows build with cached RuneLite dependencies and the official development API JAR:

```powershell
.\Build-Cached.ps1 -Jdk 'C:\path\to\jdk-17' -ApiJar 'C:\path\to\runelite-api-1.13.8.jar'
```

The development build produces `build/libs/brainless-16-19-19-0.1.0.jar` and `build/libs/brainless-16-19-19-dev.jar`. See [TEST_REPORT.md](TEST_REPORT.md) for test coverage and [COMPLIANCE.md](COMPLIANCE.md) for the features included in the submission.
