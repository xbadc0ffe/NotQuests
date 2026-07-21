# NotQuests — Paper 26.2 upgrade notes

Bump of the local fork from the Paper **26.1.x** line to **26.2**
(`26.2.build.62-beta`, the MC version the production server reports).
Everything below is evidenced by disassembly / build output, not inference.

## Symptom

On Paper 26.2, right-clicking a Citizens quest-giver NPC threw:

```
java.lang.NoSuchFieldError: Class net.minecraft.world.item.Items does not have
member field 'net.minecraft.world.item.Item GREEN_CANDLE'
  at rocks.gravili.notquests.paper.shadow.invui.internal.menu.CustomContainerMenu.<clinit>(CustomContainerMenu.java:109)
-> then NoClassDefFoundError: Could not initialize class ...CustomPlainMenu on every subsequent click
via GuiService.showGui -> QuestManager.sendQuestsPreviewOfQuestShownNPCs -> CitizensEvents.onNPCClickEvent
```

Text-based quest interaction worked; only the GUI path failed.

## Root cause

The GUI layer is **InvUI**, shaded and relocated into
`rocks.gravili.notquests.paper.shadow.invui`. InvUI builds a private
`DIRTY_MARKER` sentinel `ItemStack` in a static initializer using a candle item
with a custom `item_model` component (`invui:dirty_marker`).

In **26.2 the candle items were consolidated**: the field
`net.minecraft.world.item.Items.GREEN_CANDLE` (type `Item`) was **removed** and
replaced by `net.minecraft.world.item.Items.DYED_CANDLE` (type
`net.minecraft.world.level.block.ColorCollection`), from which the green variant
is obtained via `ColorCollection.green()`.

- InvUI **2.0.0-RC.1 … 2.1.x** are compiled against the old API — `<clinit>`
  does `getstatic Items.GREEN_CANDLE`. On 26.2 that field is gone → the class
  fails to initialize → `NoSuchFieldError`, then `NoClassDefFoundError` on every
  later use.
- InvUI **2.2.x** is compiled against 26.2: `<clinit>` does
  `getstatic Items.DYED_CANDLE` → `invokevirtual ColorCollection.green()` →
  `checkcast Item`.

Verified in the built jar's shaded class
(`…shadow.invui.internal.menu.CustomContainerMenu`): `GREEN_CANDLE` occurrences
= 0; bytecode now `getstatic Items.DYED_CANDLE:ColorCollection` +
`ColorCollection.green()`.

InvUI 2.x targets **one** Minecraft version per release (it dropped the 1.x
multi-version `inventory-access-rN` modules). So the fix is a version bump, not
an in-tree patch.

## Secondary breakage — Adventure 4 → 5

Paper 26.2 also bundles **Adventure 5.2.0** (26.1.x was on Adventure 4.x), a
major bump that removed two APIs our own code used. Both were fixed with
**verified 1:1 equivalents** (behavior identical, confirmed by disassembling
both versions), nothing touching MiniMessage parsing or component rendering:

| File | Removed in 5.x | Replacement (semantics identical) |
|---|---|---|
| `paper/…/minimessage/AbstractColorChangingTag.java` | `net.kyori.adventure.internal.Internals.toString(this)` (debug `toString`) | `this.examine(StringExaminer.simpleEscaping())` — exactly what `Internals.toString` did |
| `paper/…/managers/LogManager.java` | `GsonComponentSerializer.builder().downsampleColors().build()` | `GsonComponentSerializer.colorDownsamplingGson()` — same static factory, present in both 4.x and 5.x |

All other Adventure serializer call-sites (`legacyAmpersand()`,
`LegacyComponentSerializer.builder().hexColors()`, `plainText()`,
`GsonComponentSerializer.gson().deserialize/serialize`) are 5.x-compatible and
compiled unchanged.

> **Fragility note:** `AbstractColorChangingTag` is a **vendored copy of an
> Adventure-internal MiniMessage class** (base of `SimpleGradientTag`). Because
> it lives against Adventure internals, it is a **known break point for future
> Adventure bumps** — expect to revisit it on the next Adventure major.

## Own NMS code — compiled clean

The plugin's own direct-NMS surface (43 refs across 6 files; notably the 19 in
`managers/packets/ownpacketstuff/modern/PacketInjector.java`, plus
`NQPacketListener`, `AdminCommands`, the `reflection/*` package) **compiled
against 26.2 with no changes required.** The candle change did not ripple into
the handles/packets/holograms/beams layer. That code was deliberately left
untouched (owner review reserved).

## Exact changes

Build scripts (Step 0 reconcile of the mislabeled deployed jar, then the 26.2 bump):

| File:line | 26.2 value |
|---|---|
| `build.gradle.kts` version | `6.3.0` |
| `build.gradle.kts` / `paper` / `plugin` `paperDevBundle` | `26.2.build.62-beta` |
| `build.gradle.kts` / `paper` / `plugin` `minecraftVersion` (runServer) | `26.2` |
| `plugin/build.gradle.kts` `apiVersion` (bukkit + paper) | `26.2` |
| `paper/build.gradle.kts` InvUI | `xyz.xenondevs.invui:invui:2.2.0` |
| `plugin/build.gradle.kts` shadowJar | `archiveBaseName = "notquests"`, `archiveClassifier = "26.2"` → `notquests-<ver>-26.2.jar` |

Source (Adventure 5.x only — see table above): `LogManager.java`,
`AbstractColorChangingTag.java`. **No NMS/packet-layer edits.**

## Deliberately left unchanged

- FlagParser fix, OpenGuiAction fix, `.gitignore`, `npc-available-quests.yml`
  (the existing local patches).
- The entire NMS/packet layer (`modern/*`, `reflection/*`, `AdminCommands`
  NMS block).
- No upstream merge or rebase onto `origin/main`.

## Build

```
./gradlew clean build
```

Output (deployable, mojang-mapped production jar):

```
plugin/build/libs/notquests-6.3.0-26.2.jar
```

`plugin.yml`: `version: 6.3.0`, `api-version: "26.2"`.

## ⚠ Version coupling — InvUI 2.2.0 is 26.2-only

InvUI 2.2.0 is compiled against the 26.2 NMS API (`DYED_CANDLE.green()`). This
jar will **crash symmetrically on 26.1.x** (there `Items.DYED_CANDLE` /
`ColorCollection` don't exist — the mirror of the original `GREEN_CANDLE`
crash). The InvUI version is now hard-pinned to the server's Paper version:
roll the server back to 26.1.x and you must roll InvUI back to 2.1.x too.

## Smoke test after deploying on 26.2

Right-click the Citizens quest-giver NPC → the quest-preview GUI opens with no
`NoSuchFieldError` / `NoClassDefFoundError` in console. Console color output
(the `LogManager` downsample path) still renders on non-truecolor terminals.
