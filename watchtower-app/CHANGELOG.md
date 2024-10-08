# Watchtower - Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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
