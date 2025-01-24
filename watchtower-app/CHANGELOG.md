# Watchtower - Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [2.11.0] - 2025-01-24
### Added
- Add option to modify the attribute list for already monitored devices - `@Kulfsson`
- Add "index" custom attribute behaving like an ever-increasing counter

### Changed
- Refactor the "Remove device" functionality
- Groovy code cleanup

## [2.10.1] - 2024-12-25
### Fixed
- Show attribute name in Stats tiles - `@Alan_F`

## [2.10.0] - 2024-12-20
### Added
- Add Bring Your Own Data tile type - `@hubitrep`

## [2.9.0] - 2024-12-10
### Added
- Add precision selector for the Stats tile - `@Ranchitat`

### Changed
- The Stat tile sparkline renders only the last 10 values for the selected precision

### Fixed
- Disable dashboard scroll when either the left menu or the add tile dialog is opened
- Hide the left menu when the add tile dialog is opened

## [2.8.0] - 2024-12-08
### Added
- Add Stats tile type

### Changed
- Use drag-and-drop to re-order datasets when configuring "Multi Device/Attribute" and "Status Map" tile types
- Tech stack: don't use shadow dom anymore; minify HTML/CSS

### Fixed
- Dashboard tiles can now be resized and moved on touchscreen devices - `@Sebastien`

## [2.7.0] - 2024-11-26
### Added
- Add Multi Device/Attribute tile type - `@user3666`

## [2.6.0] - 2024-11-25
### Added
- Add support for chart chart user scripts

## [2.5.0] - 2024-11-20
### Added
- Sync zoom/reset zoom between all dashboard charts (if CTRL key is pressed) - `@tj4293`

## [2.4.0] - 2024-11-19
### Added
- Sync crosshair between all dashboard charts - `@Sebastien`

## [2.3.1] - 2024-11-18
### Fixed
- Values for non-numeric attributes are capped at 1% - `@tj4293`

## [2.3.0] - 2024-11-16
### Added
- Add Status Map tile type - `@briguy`

## [2.2.0] - 2024-11-02
### Added
- Add load, zoom, and pan transition animations

### Fixed
- Edit device widget does not load attributes for "Hubitat Hub" - `@iEnam`
- Add/edit device widget ignores Y-scale dashboard setting - `@iEnam`

## [2.1.1] - 2024-11-01
### Fixed
- Fix regression bug - `@iEnam`

## [2.1.0] - 2024-11-01
### Added
- Add option to render zero for missing values - `@Kulfsson`
- Add option to edit dashboard tiles - `@briguy`

### Changed
- Change tooltip title to display the datapoint time interval

## [2.0.2] - 2024-10-10
### Fixed
- Fix chart zoom selection when mouse gets out of bounds - `@iEnam`

## [2.0.1] - 2024-09-30
### Fixed
- Fix `IndexOutOfBoundsException` when the last value on a record line is null (empty string) - `@iEnam`

## [2.0.0] - 2024-09-14
### Added
- Add support for aggregated min/max values

### Changed
- Remove bar chart rendering when fewer datapoints are available
- Null values in CSV files are now saved as empty strings instead of using dashes (`-`)

## [1.5.0] - 2024-08-27
### Added
- Add support for null (`-`) values in CSV files
- Mark some attributes values as null when the device also has a "switch" attribute with value "off"
- Add attributes support: Alarm/alarm, CarbonMonoxideDetector/carbonMonoxide, Chime/status, AudioVolume/volume, SwitchLevel/level, UltravioletIndex/ultravioletIndex

## [1.4.1] - 2024-08-25
### Fixed
- Fix "Max records with 5 min accuracy" settings - `@bobbles`

## [1.4.0] - 2024-08-22
### Changed
- Rewrite chart zoom mechanism to no longer rely on mouse scroll

## [1.3.2] - 2024-08-19
### Fixed
- Display decimal points in chart data - `@bobbles`

## [1.3.1] - 2024-08-16
### Fixed
- Fix situations where tooltip is obscured by the time resolution picker or the chart title - `@an39511`

## [1.3.0] - 2024-08-15
### Added
- Hub Info tile: add platform update available and alerts notifications - `@amithalp`
- Add option to configure grid cell height

### Changed
- Hub Info tile: add more hub details and make the widget configurable - `@amithalp`

### Fixed
- Fix Iframe tile margins - `@amithalp`

## [1.2.1] - 2024-08-11
### Fixed
- Fix `crypto.randomUUID()` issue

## [1.2.0] - 2024-08-11
### Fixed
- Ignore device events happening earlier than needed when collecting 5m values - `@iEnam`
- Continue collecting data for other devices even when encountering an error

### Changed
- Desaturate dark theme colors

### Added
- Iframe tile: add options to hide tile border and to disable auto-refresh
- Add non-standard attributes support: cloudiness, healthStatus, pm10, windDirection, windGust, windSpeed
- Add option to disable data collection and/or aggregation - `@PPz`
- Add option to initiate on-demand data collection - `@PPz`

## [1.1.0] - 2024-08-08
### Added
- Add option to set charts y-axis scale to "auto" or "fixed" - `@marktheknife`

## [1.0.0] - 2024-07-31
### Added
- Initial release
