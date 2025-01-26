# LGTV with webOS - Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.5.2] - 2025-01-26
### Fixed
- Improve the fast-ping mechanism to avoid overlap connections - `@hubitrep`
- Fix the "Speak" command for webOS 5 - `@hubitrep`

## [1.5.1] - 2025-01-25
### Fixed
- Improve the detection of TV startup using a fast-ping mechanism - `@hubitrep`
- Fix the "Start Video" and "Speak" commands for webOS 5 - `@hubitrep`

## [1.5.0] - 2025-01-25
### Added
- Add "SpeechSynthesis/Speak" command for TTS announcements

### Fixed
- Changed "Set Channel" to accept string channel number - `@hubitrep`

## [1.4.0] - 2025-01-23
### Added
- Add "ImageCapture/Take" command to take a TV screenshot

### Fixed
- Correctly identify event type (digital vs physical)
- Hide errors for unsupported config keys - `@nclark`

## [1.3.0] - 2025-01-23
### Added
- Add "Screen Saver On" and "Screen Saver Off" commands

### Fixed
- Hide errors for unsupported config keys - `@nclark`

## [1.2.0] - 2025-01-21
### Added
- Add "Start Video" command
- Add "Start Web Page" command

## [1.1.2] - 2025-01-21
### Fixed
- Set `switch:off` immediately when the "Off command is sent"

## [1.1.1] - 2025-01-21
### Fixed
- Also use `getMACFromIP()` to determine MAC when sending the WOL message

## [1.1.0] - 2025-01-20
### Changed
- Use the "hello" message to test if the websocket is really open
- Use ping to detect when TV becomes online
- Don't send the big pairing payload when we already have a Pairing Key
- Automatically enable Wake On Lan settings when TV is first paired
- Show a "Well done" toast on TV screen on successfull pairing
- Add "setPictureMode" and "setSoundOutput" commands
- Add ping interval preference

## [1.0.0] - 2025-01-17
### Added
- Initial version
