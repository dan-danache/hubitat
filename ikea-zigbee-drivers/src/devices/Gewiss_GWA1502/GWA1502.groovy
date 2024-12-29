{{!--------------------------------------------------------------------------}}
{{# @definition }}
capability 'Sensor'
{{/ @definition }}
{{!--------------------------------------------------------------------------}}
{{# @fields }}

// Fields for devices.Gewiss_GWA1502
import com.hubitat.app.ChildDeviceWrapper
import com.hubitat.app.DeviceWrapper

@Field static final Map<String, String> BUTTON_TYPES = [
    'toggle': 'Rocker / Toggle',
    'push': 'Push Button',
]
{{/ @fields }}
{{!--------------------------------------------------------------------------}}
{{# @inputs }}

// Inputs for devices.Gewiss_GWA1502
input(
    name:'buttonType', type:'enum', title:'Button type', required:true,
    description:'Select wired buttons type',
    options:BUTTON_TYPES,
    defaultValue:'toggle'
)
input(
    name:'enableContacts', type:'bool', title:'Use as Contact Sensor', required:true,
    description:'Track open/closed state using two Contact Sensor child devices',
    defaultValue:false
)
{{/ @inputs }}
{{!--------------------------------------------------------------------------}}
{{# @implementation }}

// Implementation for devices.Gewiss_GWA1502
private ChildDeviceWrapper fetchChildDevice(Integer moduleNumber) {
    ChildDeviceWrapper childDevice = getChildDevice("${device.deviceNetworkId}-${moduleNumber}")
    return childDevice ?: addChildDevice('hubitat', 'Generic Component Contact Sensor', "${device.deviceNetworkId}-${moduleNumber}", [name:"${device.displayName} - Contact ${moduleNumber}", label:"Contact ${moduleNumber}", isComponent:true])
}

void componentRefresh(DeviceWrapper childDevice) {
    log_debug "▲ Received Refresh request from ${childDevice.displayName}"
    refresh()
}
{{/ @implementation }}
{{!--------------------------------------------------------------------------}}
{{# @updated }}

// Preferences for devices.Gewiss_GWA1502
if (buttonType == null) {
    buttonType = 'toggle'
    device.updateSetting 'buttonType', [value:buttonType, type:'enum']
}
log_info "🛠️ buttonType = ${BUTTON_TYPES[buttonType]}"

if (enableContacts == null) {
    enableContacts = false
    device.updateSetting 'enableContacts', [value:false, type:'bool']
}
log_info "🛠️ enableContacts = ${enableContacts}"
if (enableContacts != true) {
    ChildDeviceWrapper childDevice = getChildDevice("${device.deviceNetworkId}-1")
    if (childDevice) {
        log_debug "🎬 Removing child device ${childDevice} ..."
        deleteChildDevice("${device.deviceNetworkId}-1")
    }
    childDevice = getChildDevice("${device.deviceNetworkId}-2")
    if (childDevice) {
        log_debug "🎬 Removing child device ${childDevice} ..."
        deleteChildDevice("${device.deviceNetworkId}-2")
    }
}
{{/ @updated }}
{{!--------------------------------------------------------------------------}}
{{# @configure }}

// Configuration for devices.Gewiss_GWA1502
cmds += "zdo bind 0x${device.deviceNetworkId} 0x01 0x01 0x0406 {${device.zigbeeId}} {}" // Occupancy Sensing cluster (ep 0x01)
cmds += "zdo bind 0x${device.deviceNetworkId} 0x02 0x01 0x0406 {${device.zigbeeId}} {}" // Occupancy Sensing cluster (ep 0x02)
cmds += "he cr 0x${device.deviceNetworkId} 0x01 0x0406 0x0000 0x18 0x0000 0x0258 {01} {}" // Report Occupancy (map8) at least every 10 minutes (ep 0x01)
cmds += "he cr 0x${device.deviceNetworkId} 0x02 0x0406 0x0000 0x18 0x0000 0x0258 {01} {}" // Report Occupancy (map8) at least every 10 minutes (ep 0x02)
{{/ @configure }}
{{!--------------------------------------------------------------------------}}
{{# @refresh }}

// Refresh for devices.Gewiss_GWA1502
cmds += zigbee.readAttribute(0x0406, 0x0000, [destEndpoint:0x01]) // Occupancy (ep 01)
cmds += zigbee.readAttribute(0x0406, 0x0000, [destEndpoint:0x02]) // Occupancy (ep 02)
{{/ @refresh }}
{{# @events }}

// Events for devices.Gewiss_GWA1502
// ===================================================================================================================

// Report/Read Attributes Reponse: Occupancy
case { contains it, [clusterInt:0x0406, commandInt:0x0A, attrInt:0x0000] }:
case { contains it, [clusterInt:0x0406, commandInt:0x01, attrInt:0x0000] }:
    String newState = msg.value == '01' ? 'closed' : 'open'

    // Ignore periodic reports with no state change
    if (msg.commandInt == 0x0A && state["lastC${msg.endpointInt}"] == newState) {
        utils_processedZclMessage "${msg.commandInt == 0x0A ? 'Report' : 'Read'} Attributes Response", "Contact=${msg.endpointInt}, State=${newState}"
        return
    }
    state["lastC${msg.endpointInt}"] = newState

    // Send button events only when the device reports any change, not on refresh
    // For push buttons, send events only on contact close
    if (msg.commandInt == 0x0A && (buttonType == 'toggle' || newState == 'closed')) {
        List<String> button = msg.endpointInt == 0x01 ? BUTTONS.ONE : BUTTONS.TWO
        utils_sendEvent name:'pushed', value:button[0], type:'physical', isStateChange:true, descriptionText:"Button ${button[0]} (${button[1]}) was pushed"
    }

    // Send event to module child device (if contacts child devices are enabled)
    if (enableContacts == true) {
        ChildDeviceWrapper childDevice = fetchChildDevice(msg.endpointInt)
        childDevice.parse([[name:'contact', value:newState, descriptionText:"${childDevice.displayName} is ${newState}", type:type]])
        utils_processedZclMessage "${msg.commandInt == 0x0A ? 'Report' : 'Read'} Attributes Response", "Contact=${msg.endpointInt}, State=${newState}"
    }
    return

// Other events that we expect but are not usefull
case { contains it, [clusterInt:0x0406, commandInt:0x07] }:
    utils_processedZclMessage 'Configure Reporting Response', "attribute=Occupancy, data=${msg.data}"
    return
{{/ @events }}
{{!--------------------------------------------------------------------------}}
