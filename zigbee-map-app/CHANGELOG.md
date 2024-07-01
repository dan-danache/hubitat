# Hubitat Zigbee Map - Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [2.2.0] - 2024-07-01

### Added
- Add "Advanced Zigbee pairing" functionality

## [2.1.0] - 2024-05-16

### Changed
- Change "poor" link quality color from yellow to violet - `@Horseflesh`
- Change "good" link quality LQI interval from [150 - 200) to [130 - 200)

### Fixed
- Scrollbars on tab contents appear only when necessary

## [2.0.0] - 2024-03-09

### Changed
- **Breaking change**: Some files were renamed therefore your bookmarks or PWA installs might be broken

### Added
- Add Zigbee routes map - `@Tony`
- Fade-out the nodes tooltip to see better how things are connected - `@danabw`

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
