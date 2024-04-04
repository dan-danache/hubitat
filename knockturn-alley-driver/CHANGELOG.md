# Knockturn Alley - Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [2.3.0] - 2024-04-03
### Added
- Add possibility to show command names in plain english

### Fixed
- `Revelio` now shows all records, not just the ones that fit in a single Zigbee message

## [2.2.0] - 2023-11-07
### Added
- Add `Bombarda` spell to execute raw Zigbee commands

## [2.1.0] - 2023-11-01
### Added
- `Legilimens` spell now also discovers attributes and commands for a specific manufacturer
- Add `Revelio` spell to retrieve data from Neighbors Table (LQI), Routing Table and Bindings Table
- Add `Unbreakable Vow` spell to add/remove entries to/from the Bindings Table

## [2.0.0] - 2023-10-30
### Changed
- Breaking change: The generated report is now text only (no pretty HTML anymore). The good part is that you can now share it using copy/paste.

## [1.6.0] - 2023-10-28
### Added
- Add `Oppugno` spell to configure attribute reporting

## [1.5.0] - 2023-10-27
### Added
- `Legilimens` spell now also gathers data from Neighbors Table (LQI), Routing Table and Bindings Table

## [1.4.0] - 2023-10-26
### Added
- `Legilimens` spell now also gathers data from Node Descriptor and Node Power Descriptor
- Translate attribute hex value to friendly representations for some known attributes (e.g. Power On Behavior, Temperature, Relative Humidity, etc.)

## [1.3.0] - 2023-10-04
### Added
- Add option to specify manufacturer code when handling Zigbee attributes and when executing Zigbee commands

### Changed
- Change / shuffle some spell names

## [1.2.0] - 2023-09-29
### Added
- Add `Bombarda` spell to execute Zigbee cluster commands

### Changed
- `Legilimens` spell now also discovers all commands that each cluster can receive

## [1.1.0] - 2023-09-28
### Added
- Add `Imperio` spell to write Zigbee cluster atrributes value
- `Scourgify` spell has now the option to remove or keep the raw data

## [1.0.0] - 2023-09-27
### Added
- Initial release
