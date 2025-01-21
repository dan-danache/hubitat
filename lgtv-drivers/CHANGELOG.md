# LGTV with webOS - Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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
