# Brainless 16/19/19 0.1.0
Feature notes for RuneLite Plugin Hub review.

| Feature | Implementation decision |
| --- | --- |
| Ordinary fishing, shrine cooking, gathering supplies and filling the ammunition crate | Use inventory and observed deposits to follow the documented west-boat / north-island batches. Show one existing interactable and observe the normal manual action. Comparable routine guidance exists in Jam’s reviewed Hub release. |
| Waves, storms, fires, safe tiles and vulnerability prompts | Leave these decisions to the player. The plugin adds no hazard prediction, tether or dodge cue, fire route, or automatic spirit-pool indicator. The game displays its own information. |
| Marker and click handling | Draw on the scene and preserve original hit testing. Observe menu selections without consuming, reordering or invoking them. |
| Data and inputs | In-process RuneLite observations. The shipped plugin performs no file or network I/O and generates no input. |

Jam’s Solo Tempoross 1.0.14 is pinned by the RuneLite Hub at `f593d4ba22122805ff8e14135a7a0728bece6074`. Its settings can restrict the overlay to the current click, hide paths and disable the status panel. Inspection of the entry point and overlay showed tick-based updates, NPC hulls and object outlines. They do not supply the requested full hit-area fill and immediate matching menu acknowledgement. Its README describes the opening cook at eight, the first load of sixteen, then batches of nineteen and the manual pool intervals. This implementation uses that published strategy and its north/west landmarks.

The upstream license permits modifications and redistribution under BSD-2-Clause. Its repository includes agent development guidance. The secondary Solo Helper reference at `0acb42afa3d26b05892867cf6e8323855fb40660` is also BSD-2-Clause. Neither plugin is included as a dependency. The Gradle wrapper comes from the RuneLite example plugin and carries Gradle’s own notices.

- [Jagex third-party client guidelines](https://secure.runescape.com/m=news/third-party-client-guidelines?oldschool=1)
- [Jagex macro and client rules](https://legal.jagex.com/docs/rules/macro-and-client-features-not-permitted)
- [RuneLite rejected features](https://github.com/runelite/runelite/wiki/Rejected-or-Rolled-Back-Features)
- [RuneLite plugin review](https://github.com/runelite/runelite/wiki/Plugin-Hub-Review)
- [RuneLite example project guidelines](https://github.com/runelite/example-plugin/blob/master/AGENTS.md)
- [Jam’s pinned strategy and license](https://github.com/JamsRepos/jams-solo-tempoross/tree/f593d4ba22122805ff8e14135a7a0728bece6074)
- [Secondary reference and license](https://github.com/RyanTamulevicz/tempoross-solo-helper/tree/0acb42afa3d26b05892867cf6e8323855fb40660)
- [OSRS Wiki encounter scoring](https://oldschool.runescape.wiki/w/Tempoross)
