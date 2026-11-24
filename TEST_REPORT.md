# Brainless 16/19/19 0.1.0 verification

**Environment.** Windows x64, Microsoft OpenJDK 17.0.21.1, Java 11 bytecode, official RuneLite 1.13.8 development API, and RuneLite 1.13.8 client libraries from the installed launcher cache.

## Automated checks

The Windows cached build compiles the production and test sources separately. It runs the checks with assertions enabled before packaging either JAR. Test doubles, synthetic observations and the development entry point are outside the shipped plugin JAR.

| Check | Evidence |
| --- | --- |
| T01, T13 – lifecycle | Synthetic game ticks enter a solo instance, leave it when another player appears, start again, and clear the target on logout. Mid-run attachment with points stays inactive. |
| T02, T12, T16 – rendering | Pixel checks cover an irregular shape with a hole, a thin extension, viewport and UI clipping, and an opaque configurable fill. A synthetic shrine changes screen position between renders. NPC model hit areas match RuneLite’s own pick calculation in twelve combinations of camera angle, CPU/GPU projection and model picking mode. |
| T03, T04 – input | Real RuneLite menu-event objects delivered on the event bus hide a matching mark immediately. Wrong NPC instance, option, world view or object tile fails to acknowledge it. The event remains unconsumed. |
| T05–T08 – action tracking | Sustained travel and activity keep a clicked target hidden. Idle timeout, changed task, replacement entity and despawn clear the acknowledgement or allow a retry. |
| T09, T10 – inventory | Opening cook at eight, load at sixteen, three later loads of nineteen, partial loads, omitted clicks, mismatched points, dropped fish, delayed score or inventory, and a double catch near the quota. |
| T11 – hazards and pool intervals | Tethered state suspends routine targets. The resource state waits through the pool interval and resumes after its observed close. Source review found no hazard predictor, automatic safe-tile marker or pool prompt. |
| T14 – packaging and initialization | The real RuneLite 1.13.8 plugin manager loads the class, injects dependencies, starts it, installs its overlay, stops it and removes the overlay against an offline client fixture. Production source and the packaged class list were checked for input generation, game action invocation, external communications and test-only code. |

## Manual validation

**T15 is pending.** No live game was played. The normal RuneLite development entry point started using a separate temporary home. The build environment could not resolve the Jagex configuration host or RuneLite world service, so that launch did not reach the login screen. The Gradle distribution download was also blocked in this environment. The shipped binaries and automated test results came from the cached build using the official API.

1. Start the launcher. Enable the plugin before a fresh solo run. Check normal supplies and your actual harpoon or barehanded setup.
2. Hover over the middle and edges of the filled fishing spot, shrine and ammunition crate. Check the native left-click action at each location. Rotate and zoom, including an overlap with the inventory or another interactable. Record the target and camera angle if an unclickable patch is colored or an exposed patch is missing.
3. Click the fill. Confirm it disappears immediately and stays hidden through a long approach. Try **Examine**, a second similar target, a despawn and an interrupted action.
4. Complete several west-boat / north-island runs, checking the actual loads and manual pool intervals. Note any missing or early prompt, score mismatch, interruption, or drop. Verify the next run starts cleanly.

Real game animations, menu-entry swaps, specific fishing-spot models, crate representations, overlapping scenery, click priority and frame rate still require in-game confirmation. The overlap mask is conservative when multiple entities project onto the same pixels.
