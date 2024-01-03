/**
 * Zigbee Pairing Helper
 *
 * This driver allows you to pair new Zigbee devices using the Zigbee routing device you choose.
 */
import groovy.transform.Field

@Field static final String DRIVER_NAME = "Zigbee Pairing Helper"
@Field static final String DRIVER_VERSION = "1.0.0"

metadata {
    definition(name:DRIVER_NAME, namespace:"dandanache", author:"Dan Danache", importUrl:"https://raw.githubusercontent.com/dan-danache/hubitat/master/zigbee-pairing-helper-driver/zigbee-pairing-helper.groovy") {
        capability "Actuator"
    }
    preferences {
        input(
            name: "deviceNetworkId",
            type: "enum",
            title: "Pairing device",
            description: "<small>Select a mains-powered device that you want to put in pairing mode.</small>",
            options: [ "0000":"👑 Hubitat Hub" ] + getDevices(),
            required: true
        )
    }
}

// Called when the "Save Preferences" button is clicked
def updated() {
    if (deviceNetworkId == null || deviceNetworkId == "ZZZZ") return log.error("Invalid Device Network ID: ${deviceNetworkId}")

    log.info "Stopping Zigbee pairing on all devices. Please wait 5 seconds ..."
    sendHubCommand new hubitat.device.HubMultiAction(["he raw 0xFFFC 0x00 0x00 0x0036 {42 0001} {0x0000}"], hubitat.device.Protocol.ZIGBEE)
    runIn(5, "startDeviceZigbeePairing")
}

private startDeviceZigbeePairing() {
    log.info "Starting Zigbee pairing on device ${deviceNetworkId} for 90 seconds..."
    sendHubCommand new hubitat.device.HubMultiAction(["he raw 0x${deviceNetworkId} 0x00 0x00 0x0036 {43 5A01} {0x0000}"], hubitat.device.Protocol.ZIGBEE)
    log.warn "<b>Now is the right moment to put the device you want to join in pairing mode!</b>"
}

private Map<String, String> getDevices() {
    try {
        httpGet([ uri:"http://127.0.0.1:8080/hub/zigbee/getChildAndRouteInfoJson" ]) { response ->
            if (response?.status != 200) {
                return ["ZZZZ": "Invalid response: ${response}"]
            }
            return response.data.devices
                .sort { it.name }
                .collectEntries { ["${it.zigbeeId}", "${it.name}"] }
        }
    } catch (Exception ex) {
        return ["ZZZZ": "Exception: ${ex}"]
    }
}

def parse(String description) {
    log.debug "description=[${description}]"
}
