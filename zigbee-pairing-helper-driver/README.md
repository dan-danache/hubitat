# Zigbee Pairing Helper

This driver allows you to pair a new Zigbee device through a specific existing Zigbee device from the mesh.

## Usage

1. Install the "Zigbee Pairing Helper" driver either using HPM or by pasting the code from the following URL in the Hubitat code editor:

   https://raw.githubusercontent.com/dan-danache/hubitat/main/zigbee-pairing-helper-driver/zigbee-pairing-helper.groovy

1. Select a main-powered and change its driver to "Zigbee Pairing Helper".

1. In a new tab, navigate to (as usual) to "Devices" -> "Add Device" -> "Zigbee" -> "Start Zigbee pairing".

1. In the "Preferences" section, select the device you want to put in pairing mode, then click the "Save Preferences" button.

1. Wait for 5 seconds. Also check the Live Logs for further instructions.

1. Put the device you want to join in the pairing mode; for IKEA devices this usually means pushing the pairing button (🔗) 4 times in 5 seconds.

1. The device will now join the Zigbee network by talking directly to the Hubitat Hub, no need to keep the device really close to the Hub anymore.

---
[<img src="https://cdn.buymeacoffee.com/buttons/v2/default-yellow.png" alt="Buy Me A Coffee" style="height: 40px !important;width: 162px !important">](https://www.buymeacoffee.com/dandanache)
