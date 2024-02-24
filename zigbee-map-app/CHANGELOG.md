# Hubitat Zigbee Map - Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.5.0] - 2024-02-23
### Added
- Add config option to show/hide link colors
- Add graph for hub memory and processor usage history (since last reboot) - `@WarlockWeary`

### Changed
- Make node hover effect (see neighbors) more visible - `@WarlockWeary`

## [1.4.0] - 2024-02-23
### Added
- Color links based on LQI/LQA value - `@Horseflesh`
- Use "Esc" keyboard key to toggle the controls - `@jshimota`

### Changed
- Hide back "duplex" links by default to better see the link colors

## [1.3.0] - 2024-02-21
### Added
- Add option to show/hide "Unknown" devices - `@Tony`
- Add PWA manifest

### Fixed
- Remove the Hub device from the "Devices" tab - `@jimhim`

## [1.2.0] - 2024-02-20
### Added
- Add option to use an image as map background (e.g.: home layout)

## [1.1.0] - 2024-02-19
### Added
- Add "Done" button in the Hubitat app - `@dnickel`
- Click the address of any device in the "Devices" tab to add it to the Interview Queue - `@hubitrep`
- Use relative URL when opening the HTML app - `@jlv`
- Mark devices that failed the Interview  - `@kahn-hubitat`
- Show Interview Queue size
- Show "duplex" links by default

## [1.0.0] - 2024-02-16
### Added
- Initial release
