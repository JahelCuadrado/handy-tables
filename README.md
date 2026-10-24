# Handy Tables

Use your crafting table, stonecutter, loom, cartography table, smithing table, grindstone and anvil
straight from your inventory: middle-click the item and it opens, no need to place it.

Fabric mod for Minecraft 26.3.

## Features

- **Seven stations in your pocket:** crafting table, stonecutter, loom, cartography table, smithing
  table, grindstone and anvil. Middle-click one in your inventory and its screen opens, exactly as
  if you had placed it: no more carrying a table around to put down and pick up again.
- **From any screen:** your inventory, a chest, a barrel, a furnace or another station. Jump from
  the crafting table to the stonecutter without closing anything.
- **Creative too:** in creative, where middle click copies items, hold `Ctrl` (`Cmd` on macOS) while
  middle-clicking. It follows your "Pick Block" mouse button if you moved it.
- **Back where you were:** middle-click the station again and you return to the chest (or barrel,
  furnace...) you opened it from, or to your inventory.
- **Your station stays put:** while you use it, a small padlock keeps it in its slot so a stray
  click or key can't move or drop it. Carrying a stack? Only the one in use is locked: the spares
  can be moved, dropped or added to as usual.
- **The recipe book plays along:** it never uses the station in your hand as an ingredient.
- **A real grindstone and anvil:** the grindstone gives you the experience from removing
  enchantments, and the anvil wears out just like a placed one: chipped, damaged, then broken.
- **Sounds right:** each station plays its placing sound when it opens, for you only. You can turn
  it off in the settings (Mod Menu's "Configure" button, or `config/handytables.json`).
- **Works with Shulker Toolkit:** craft with the materials inside your shulker boxes and keep their
  windows open next to every station.
- **Lightweight:** no cost while you play; it only does something when you click.
- **Languages:** English, Español (España), Español (México), Português (Brasil), Português (Portugal),
  Français, Deutsch, Italiano, Nederlands, Polski, Türkçe, Русский, Українська, 简体中文, 繁體中文,
  日本語 and 한국어.

## Servers

Install it on both client and server: the server is the one that opens the stations
(singleplayer works out of the box). On servers without the mod, middle click stays vanilla. Server owners can turn stations off for other players in
`config/handytables-server.json` (the singleplayer/LAN host is never limited):

```json
{
  "schemaVersion": 1,
  "allowCraftingTable": true,
  "allowStonecutter": true,
  "allowLoom": true,
  "allowCartographyTable": true,
  "allowSmithingTable": true,
  "allowGrindstone": true,
  "allowAnvil": true
}
```

## Requirements

Minecraft 26.3 · Fabric Loader 0.19.5+ · Fabric API · Java 25. Optional: Mod Menu, for the
settings screen.

## Development

Requires JDK 25. `./gradlew runClient` · `./gradlew build` (compile, format check, JUnit, GameTests)
· `./gradlew spotlessApply`.

## License

MIT.
