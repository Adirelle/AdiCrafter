# Changelog

## [Unreleased]

### Added

### Changed

* The autocrafters are easier to break.

### Deprecated

### Removed

### Fixed

* Configuration reset on every reload.

### Security

## [0.5.0] - 2022-01-25

### Added

* 3 new autocrafter variants:
    * the *redstone* autocrafter consumes redstone dust,
    * the *fueled* autocrafter consumes any fuel that would be accepted by a furnace,
    * the *energized* autocrafter is powered by TeamReborn Energy, as provided
      by [Tech Reborn](https://www.curseforge.com/minecraft/mc-mods/techreborn)
      or [Industrial Revolution](https://www.curseforge.com/minecraft/mc-mods/industrial-revolution); it is only
      available if at least one of these mods is installed.

### Changed

* The "automated crafter" has been renamed to "basic autocrafter".

## [0.4.1] - 2022-01-17

### Fixed

* Did not work on servers (#1)

## [0.4.0] - 2022-01-17

### Added

* A configuration file: adicrafter.json.

### Changed

* Power usage can be configured or disabled in the configuration file.

## [0.3.0] - 2022-01-16

### Added

* The automated crafter now consumes power from an internal storage that automatically refills steadily. When fully
  loaded, this allows to craft 10 times in a row.
* Display which ingredients are missing in the crafting grid.

## [0.2.0] - 2022-01-15

### Added

* [RoughlyEnoughItems](https://www.curseforge.com/minecraft/mc-mods/roughly-enough-items) support

## [0.1.0] - 2022-01-14

### Added

* The automated crafter block.
