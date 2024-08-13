# Watchtower - Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.3.0] - 2024-08-11
### Added
- Hub Info tile: add platform update available and alerts notifications - `@amithalp`

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
