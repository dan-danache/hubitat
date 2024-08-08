/**
 * Zigbee Map Helper
 *
 * This driver is used by the Zigbee Map app and its pretty useless otherwise.
 */
import groovy.transform.Field
import hubitat.helper.HexUtils

@Field static final String DRIVER_NAME = "Zigbee Map Helper"
@Field static final String DRIVER_VERSION = "2.2.0"

metadata {
    definition(
        name: DRIVER_NAME,
        namespace: "dandanache",
        author: "Dan Danache",
        importUrl: "https://raw.githubusercontent.com/dan-danache/hubitat/zigbee-map_2.1.0/zigbee-map-app/zigbee-map-helper.groovy"
    ) {
        capability "Actuator"
        attribute "poked", "string"
    }

    preferences {
      input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: false
   }
}

def installed() {
    state.info = "This device is used by the Zigbee Map app and it's pretty useless on its own."
    state.data = "Move along, nothing to see here!"
}

def updated() {
   log.info "Debug logging is: ${logEnable == true}"
   if (logEnable) runIn(1800, "logsOff")
}

String interviewNeighbors(String addr, Integer startIndex = 0) {
    List<String> cmds = ["he raw 0x${addr} 0x00 0x00 0x0031 {40 ${HexUtils.integerToHexString(startIndex, 1)}} {0x0000}"]
    if (logEnable) log.debug "◀ Sending Zigbee messages: ${cmds}"
    sendHubCommand new hubitat.device.HubMultiAction(cmds, hubitat.device.Protocol.ZIGBEE)
    sendEvent name:"poked", value:"${addr}:${startIndex}", descriptionText:"Poking ${addr}:${startIndex} for neighbors"
    return "Done"
}

String interviewRoutes(String addr, Integer startIndex = 0) {
    List<String> cmds = ["he raw 0x${addr} 0x00 0x00 0x0032 {40 ${HexUtils.integerToHexString(startIndex, 1)}} {0x0000}"]
    if (logEnable) log.debug "◀ Sending Zigbee messages: ${cmds}"
    sendHubCommand new hubitat.device.HubMultiAction(cmds, hubitat.device.Protocol.ZIGBEE)
    sendEvent name:"poked", value:"${addr}:${startIndex}", descriptionText:"Poking ${addr}:${startIndex} for routes"
    return "Done"
}

String stopPairingOnAllDevicesButThis(String addr) {
    log.info 'Stopping Zigbee pairing on all devices and waiting 5 seconds ...'
    List<String> cmds = ['he raw 0xFFFC 0x00 0x00 0x0036 {42 0001} {0x0000}']
    if (logEnable) log.debug "◀ Sending Zigbee messages: ${cmds}"
    sendHubCommand new hubitat.device.HubMultiAction(cmds, hubitat.device.Protocol.ZIGBEE)
    runIn(5, 'startDeviceZigbeePairing', [ data:[ addr:"${addr}" ]])
}

private startDeviceZigbeePairing(Map params) {
    log.info "Starting Zigbee pairing only on device ${params.addr} for 90 seconds..."
    List<String> cmds = ["he raw 0x${params.addr} 0x00 0x00 0x0036 {43 5A01} {0x0000}"]
    if (logEnable) log.debug "◀ Sending Zigbee messages: ${cmds}"
    sendHubCommand new hubitat.device.HubMultiAction(cmds, hubitat.device.Protocol.ZIGBEE)
    return 'Done'
}
