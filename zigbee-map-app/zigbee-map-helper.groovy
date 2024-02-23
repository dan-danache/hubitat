/**
 * Zigbee Map Helper
 *
 * This driver is used by the Zigbee Map app and its pretty useless otherwise.
 */
import groovy.transform.Field
import hubitat.helper.HexUtils

@Field static final String DRIVER_NAME = "Zigbee Map Helper"
@Field static final String DRIVER_VERSION = "1.5.0"

metadata {
    definition(
        name: DRIVER_NAME,
        namespace: "dandanache",
        author: "Dan Danache",
        importUrl: "https://raw.githubusercontent.com/dan-danache/hubitat/zigbee-map_${DRIVER_VERSION}/zigbee-map-app/zigbee-map-helper.groovy"
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

String poke(String addr, Integer startIndex = 0) {
    List<String> cmds = ["he raw 0x${addr} 0x00 0x00 0x0031 {40 ${HexUtils.integerToHexString(startIndex, 1)}} {0x0000}"]
    if (logEnable) log.debug "◀ Sending Zigbee messages: ${cmds}"
    sendHubCommand new hubitat.device.HubMultiAction(cmds, hubitat.device.Protocol.ZIGBEE)
    sendEvent name:"poked", value:"${addr}:${startIndex}", descriptionText:"Poking ${addr}:${startIndex}"
    return "Done"
}
