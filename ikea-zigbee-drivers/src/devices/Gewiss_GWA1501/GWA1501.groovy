{{!--------------------------------------------------------------------------}}
{{# @definition }}
capability 'Sensor'
{{/ @definition }}
{{!--------------------------------------------------------------------------}}
{{# @fields }}

// Fields for devices.Gewiss_GWA1501
import com.hubitat.app.ChildDeviceWrapper
import com.hubitat.app.DeviceWrapper

@Field static final Map<Integer, String> GWA1501_SWITCH_STYLE = [
    '00': 'Rocker / Toggle',
    '01': 'Push Button',
]
{{/ @fields }}
{{!--------------------------------------------------------------------------}}
{{# @inputs }}

// Inputs for devices.Gewiss_GWA1501
input(
    name:'switchStyle', type:'enum', title:'Switch Style', required:true,
    description:'<small>Select physical switch button configuration.</small>',
    options: GWA1501_SWITCH_STYLE,
    defaultValue:'00'
)

input(
    name:'enableContacts', type:'bool', title:'Use as Contact Sensor', required:true,
    description:'<small>Track open/closed state using two Contact Sensor child devices.</small>',
    defaultValue:false
)
{{/ @inputs }}
{{!--------------------------------------------------------------------------}}
{{# @implementation }}

// Implementation for devices.Gewiss_GWA1501
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

// Preferences for devices.Gewiss_GWA1501
if (switchStyle == null) {
    switchStyle = '00'
    device.updateSetting 'switchStyle', [value:switchStyle, type:'enum']
}
log_info "🛠️ switchStyle = ${GWA1501_SWITCH_STYLE[switchStyle]}"

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

// Configuration for devices.Gewiss_GWA1501
cmds += "zdo bind 0x${device.deviceNetworkId} 0x01 0x01 0x0406 {${device.zigbeeId}} {}" // Occupancy Sensing cluster (ep 0x01)
cmds += "zdo bind 0x${device.deviceNetworkId} 0x02 0x01 0x0406 {${device.zigbeeId}} {}" // Occupancy Sensing cluster (ep 0x02)
cmds += "he raw 0x${device.deviceNetworkId} 0x00 0x00 0x0022 {49 ${utils_payload "${device.zigbeeId}"} ${utils_payload '0x01'} ${utils_payload '0x0020'} 03 ${utils_payload "${location.hub.zigbeeEui}"} 01} {0x0000}" // Unbind Poll Control cluster
cmds += zigbee.writeAttribute(0x0020, 0x0000, 0x23, 0x00) // Disable periodic polling by the device (to conserve battery)
{{/ @configure }}
{{!--------------------------------------------------------------------------}}
{{# @refresh }}

// Refresh for devices.Gewiss_GWA1501
cmds += zigbee.readAttribute(0x0406, 0x0000, [destEndpoint:0x01]) // Occupancy (ep 01)
cmds += zigbee.readAttribute(0x0406, 0x0000, [destEndpoint:0x02]) // Occupancy (ep 02)
cmds += "he cr 0x${device.deviceNetworkId} 0x01 0x0406 0x0000 0x18 0x0000 0x0000 {01} {}" // Disable periodic reporting for Occupancy (map8) (ep 0x01)
cmds += "he cr 0x${device.deviceNetworkId} 0x02 0x0406 0x0000 0x18 0x0000 0x0000 {01} {}" // Disable periodic reporting for Occupancy (map8) (ep 0x02)
{{/ @refresh }}
{{# @events }}

// Events for devices.Gewiss_GWA1501
// ===================================================================================================================

// Report/Read Attributes Reponse: Occupancy
case { contains it, [clusterInt:0x0406, commandInt:0x0A, attrInt:0x0000] }:
case { contains it, [clusterInt:0x0406, commandInt:0x01, attrInt:0x0000] }:
    String newState = msg.value == '01' ? 'closed' : 'open'

    // Send button events only when the device reports any change, not on refresh
    // Ignore open state for push buttons
    if (msg.commandInt == 0x0A && (switchStyle != '01' || newState == 'closed')) {
        List<String> button = msg.endpointInt == 0x01 ? BUTTONS.ONE : BUTTONS.TWO
        utils_sendEvent name:'pushed', value:button[0], type:'physical', isStateChange:true, descriptionText:"Button ${button[0]} (${button[1]}) was pushed"
    }

    // Send event to module child device (if contacts child devices are enabled)
    if (enableContacts == true) {
        Integer moduleNumber = msg.endpointInt
        ChildDeviceWrapper childDevice = fetchChildDevice(moduleNumber)
        if (newState != childDevice.currentValue('contact', true)) {
            childDevice.parse([[name:'contact', value:newState, descriptionText:"${childDevice.displayName} is ${newState}", type:type]])
        }
        utils_processedZclMessage "${msg.commandInt == 0x0A ? 'Report' : 'Read'} Attributes Response", "Contact=${moduleNumber}, State=${newState}"
    }
    return

// Other events that we expect but are not usefull
case { contains it, [clusterInt:0x0406, commandInt:0x07] }:
    utils_processedZclMessage 'Configure Reporting Response', "attribute=Occupancy, data=${msg.data}"
    return
case { contains it, [clusterInt:0x0020, commandInt:0x04] }: // Write Attribute Response
    return
{{/ @events }}
{{!--------------------------------------------------------------------------}}
