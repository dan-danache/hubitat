# IKEA Zigbee drivers - Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [5.0.1] - 2024-05-22

### Fixed
- Fix lights usage in Room Lighting - `@tray_e`

## [5.0.0] - 2024-05-21

### Added
- Add driver for Dimmable Lights devices, including LED drivers
- Add driver for White Spectrum Lights devices (CT)
- Add driver for Color White Spectrum Lights devices (RGB+CT)
- Add driver for RGB-Only Lights devices (RGB)
- Add "lastBattery" attribute representing the time the last battery report was received
- Add Scenes support using Zigbee Bindings for E1810 and E2002
- Put all devices in "identifying mode" while "configure()" is running - `@UncleAlias`
- E2006: Move "Dark Mode" preference to "setIndicatorStatus()" command and "indicatorStatus" attribute - `@userLP24`

### Changed
- Remove ICPSHC24 driver; LED drivers must use the new Dimmable Lights driver

### Fixed
- Legrand Connected Outlet: Fix blue light is ON when the device is OFF and OFF when its ON - `@BorrisTheCat`

## [4.1.2] - 2024-04-27

### Fixed
- Fix errors when pressing a dashboard tile assigned to "push" one of the buttons - `@OldChicagoPete`

## [4.1.1] - 2024-04-25

### Fixed
- E2134, E2123: Fix bindings to use the correct endpoints - `@alexandruy2k`

## [4.1.0] - 2024-04-06

### Added
- Ikea E2204: Add option to disable LED indicators on the device, ensuring total darkness
- Legrand 741811: Add option to configure LED indicators: always On, always Off, follow device power state

## [4.0.0] - 2024-04-05

### Changed
- E2013: App option to swap "open" / "closed" value for the "contact" attribute
- Refactor the build system & change file naming scheme to support other manufacturers

### Added
- Add driver for IKEA Tretakt Smart Plug (E2204)
- Add Zigbee Bindings control for devices that support this feature
- Add Zigbee Groups membership control for mains-powered devices
- Add driver for Aqara Dual Relay Module T2 (DCM-K01)
- Add driver for Legrand Connected Outlet (741811)
- Add driver for Philips Hue Wall Switch Module (RDM001)
- Add driver for Philips Hue Dimmer Switch (RWL022)
- Add driver for Swann One Key Fob (SWO-KEF1PA)

### Fixed
- Ignore "Mgmt_Rtg_rsp" and "Parent_annce_rsp" messages that are generated when the Hub restarts - reported by `@a.mcdear`, `@hubitrep`

## [3.9.0] - 2024-03-14

### Added
- Add driver for Badring Water Leakage Sensor (E2202)

### Changed
- ICPSHC24: Add fingerprint for Silverglans LED Driver

## [3.8.0] - 2024-01-10

### Added
- Add driver for Vindstyrka Air Quality Sensor (E2112)

## [3.7.0] - 2024-01-03
### Added
- Add "Start Zigbee Pairing" command on all devices with Zigbee routing capability

## [3.6.1] - 2023-12-29
### Fixed
- E2213: Fix fingerprint

## [3.6.0] - 2023-12-29

### Added
- Add driver for Vallhorn Motion Sensor (E2134)
- Add driver for Somrig Shortcut Button (E2213)
- Add driver for Parasoll Door/Window Sensor (E2013)

## [3.5.1] - 2023-12-12

### Fixed
- E2006: Display filter usage (%) with no decimals
- All: Adjusted some log levels (info -> warn)

### Added
- E2006: Add "airQuality" (enum) attribute based on the reported PM 2.5 value (US AQI standard)
- E2006: Add "airQualityIndex" (0..500) attribute based on the reported PM 2.5 value (US AQI standard)

## [3.5.0] - 2023-12-11

### Added
- Add driver for Starkvind Air Purifier (E2006, E2007)

## [3.4.3] - 2023-12-05

### Fixed
- ICPSHC24: Brightness level pre-staging is now disabled by default - reported by `@denwood`

## [3.4.2] - 2023-11-25

### Changed
- All: Change battery percentage reporting interval from 11 hours to 5 hours

### Fixed
- All: Set default values for null values in "updated()" - reported by `@leo_charles`
- All: Ignore attribute reports we don't care about (configured by other driver) - reported by `@ymerj`

## [3.4.1] - 2023-11-11

### Fixed
- Fix auto-configure loop

## [3.4.0] - 2023-11-09

### Added
- All: Add "Refresh" command for all battery-powered devices to interrogate, on demand, the battery percentage
- All: Add feature to auto-configure device if user did not click the "Configure" button after switching to this driver

### Fixed
- All: Fix `GroovyRuntimeException on line 104` method `healthCheck` - reported by `@PunchCardPgmr`
- All: Fix `NullPointerException on line 197` method `pingExecute` - reported by `@rhodesda`
- All: Fix `NumberFormatException on line 274` method `parse` - reported by `@SorenDK`

### Changed
- All: Cleanup some unused code

## [3.3.0] - 2023-10-23

### Added
- Add driver for Tradfri Open/Close Remote (E1766) - contribution from `@zcorneli.iit`
- All: Add "Update Firmware" command to all devices

## [3.2.0] - 2023-10-20

### Added
- E1745: Add option to specify the period of time for the motion to become "inactive" (from 1 to 10 minutes,
  default 3 minutes)
- E1745: Add option to only detect motion in the dark
- E1745: Add "requestedBrightness" and "illumination" attributes
 
### Changed
- Instruct mains-powered devices to skip sending the (useless) DefaultResponse Zigbee message after executing
  received commands (reduce some Zigbee traffic)

### Fixed
- Make health check logging less verbose (info -> debug)
- Drivers will now refuse to (digitally) push/hold/release/double-tap buttons that are not present on the
   device (e.g push button 10)

## [3.1.0] - 2023-10-11

### Added
- Add driver for Askvader On/Off Switch (E1836)
- Add driver for Tradfri LED Driver (ICPSHC24)

### Fixed
- E1603: `OnWithTimedOff` command now works without the need to add a temporary timer (runIn)

## [3.0.0] - 2023-09-20
### Changed
- Breaking Change: Remove `Switch` and `SwitchLevel` capabilities for button devices

### Added
- E1603: Add OnWithTimedOff command (max 6500 seconds)
- All: Determine power source automatically

### Fixed
- E1603: Ignore IEEE Address Response cluster:0x8001 - reported by `@tom7`
- E1603: Add Switch capability for RM usage - reported by `@a.mcdear`

## [2.3.0] - 2023-09-16
### Added
- Add driver for IKEA Tradfri Motion Sensor (E1745)

## [2.2.0] - 2023-09-15
### Added
- E1603: Configure what happens after a power outage (Turn On, Turn Off, Restore previous state)

## [2.1.0] - 2023-09-15
### Added
- E2002: Add support for firmware "2.4.5"

## [2.0.1] - 2023-09-14
### Fixed
- Fix drivers version in the UI

## [2.0.0] - 2023-09-14
### Changed
- Major rewrite using {{ Mustache }} templates

### Added
- Add driver for Tradfri Control Outlet (E1603)
- Add driver for Tradfri On/Off Switch (E1743)
- Add driver for Tradfri Shortcut Button (E1812)
- Add driver for Styrbar Remote Control N2 (E2002)
- Add driver for Rodret Dimmer (E2201)

## [1.3.0] - 2023-09-06
### Added
- Add `healthStatus` attribute

## [1.2.0] - 2023-09-05
### Changed
- Cleanup drivers code

## [1.1.0] - 2023-09-03
### Added
- E1810: Add driver for Tradfri Remote Control (E1810)
- E2123: Add "ReleasableButton" capability for the dots buttons

## [1.0.1] - 2023-09-02
### Fixed
- Fix battery reporting on firmware 1.0.012
- Fix logging automatically reverting back to "Info"

## [1.0.0] - 2023-09-01
### Added
- Initial driver for IKEA Symfonisk Sound Remote Gen2 (E2123)
