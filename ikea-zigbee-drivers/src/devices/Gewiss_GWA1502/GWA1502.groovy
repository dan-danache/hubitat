{{!--------------------------------------------------------------------------}}
{{# @definition }}
capability 'Sensor'
{{/ @definition }}
{{!--------------------------------------------------------------------------}}
{{# @fields }}

// Fields for devices.Gewiss_GWA1502
import com.hubitat.app.ChildDeviceWrapper
import com.hubitat.app.DeviceWrapper

@Field static final Map<String, String> GWA1502_WORK_MODE = [
    'button': 'Button - Track button events',
    'sensor': 'Sensor - Track open/closed state using two Contact Sensor child devices',
]
@Field static final Map<String, String> GWA1502_BUTTON_TYPE = [
    'toggle': 'Rocker / Toggle',
    'push': 'Push Button',
]
{{/ @fields }}
{{!--------------------------------------------------------------------------}}
{{# @inputs }}

// Inputs for devices.Gewiss_GWA1502
input(
    name:'workMode', type:'enum', title:'Device mode', required:true,
    description:'<small>Select device work mode.</small>',
    options:GWA1502_WORK_MODE,
    defaultValue:'button'
)
if (workMode != 'sensor') {
    input(
        name:'buttonType', type:'enum', title:'Button type', required:true,
        description:'<small>Select wired buttons type.</small>',
        options:GWA1502_BUTTON_TYPE,
        defaultValue:'toggle'
    )
}
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
if (workMode == null) {
    workMode = 'button'
    device.updateSetting 'workMode', [value:workMode, type:'enum']
}
log_info "🛠️ workMode = ${GWA1502_WORK_MODE[workMode]}"
if (workMode == 'button') {
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
    if (buttonType == null) {
        buttonType = 'toggle'
        device.updateSetting 'buttonType', [value:buttonType, type:'enum']
    }
    log_info "🛠️ buttonType = ${GWA1502_BUTTON_TYPE[buttonType]}"
}
{{/ @updated }}
{{!--------------------------------------------------------------------------}}
{{# @configure }}

// Configuration for devices.Gewiss_GWA1502
cmds += "zdo bind 0x${device.deviceNetworkId} 0x01 0x01 0x0406 {${device.zigbeeId}} {}" // Occupancy Sensing cluster (ep 0x01)
cmds += "zdo bind 0x${device.deviceNetworkId} 0x02 0x01 0x0406 {${device.zigbeeId}} {}" // Occupancy Sensing cluster (ep 0x02)
{{/ @configure }}
{{!--------------------------------------------------------------------------}}
{{# @refresh }}

// Refresh for devices.Gewiss_GWA1502
cmds += zigbee.readAttribute(0x0406, 0x0000, [destEndpoint:0x01]) // Occupancy (ep 01)
cmds += zigbee.readAttribute(0x0406, 0x0000, [destEndpoint:0x02]) // Occupancy (ep 02)
cmds += "he cr 0x${device.deviceNetworkId} 0x01 0x0406 0x0000 0x18 0x0000 0x0258 {01} {}" // Disable periodic reporting for Occupancy (map8) at least every 10 minutes (ep 0x01)
cmds += "he cr 0x${device.deviceNetworkId} 0x02 0x0406 0x0000 0x18 0x0000 0x0258 {01} {}" // Disable periodic reporting for Occupancy (map8) at least every 10 minutes (ep 0x02)
{{/ @refresh }}
{{# @events }}

// Events for devices.Gewiss_GWA1502
// ===================================================================================================================

// Report/Read Attributes Reponse: Occupancy
case { contains it, [clusterInt:0x0406, commandInt:0x0A, attrInt:0x0000] }:
case { contains it, [clusterInt:0x0406, commandInt:0x01, attrInt:0x0000] }:
    String newState = msg.value == '01' ? 'closed' : 'open'

    // Send event to module child device (if contacts child devices are enabled)
    if (workMode == 'sensor') {
        Integer moduleNumber = msg.endpointInt
        ChildDeviceWrapper childDevice = fetchChildDevice(moduleNumber)
        if (newState != childDevice.currentValue('contact', true)) {
            childDevice.parse([[name:'contact', value:newState, descriptionText:"${childDevice.displayName} is ${newState}", type:type]])
        }
        utils_processedZclMessage "${msg.commandInt == 0x0A ? 'Report' : 'Read'} Attributes Response", "Contact=${moduleNumber}, State=${newState}"
        return
    }

    // Send button events only when the device reports any change, not on refresh
    // Ignore open state for push buttons
    if (msg.commandInt == 0x0A && (buttonType == 'toggle' || newState == 'closed')) {
        List<String> button = msg.endpointInt == 0x01 ? BUTTONS.ONE : BUTTONS.TWO
        utils_sendEvent name:'pushed', value:button[0], type:'physical', isStateChange:true, descriptionText:"Button ${button[0]} (${button[1]}) was pushed"
    }
    return

// Other events that we expect but are not usefull
case { contains it, [clusterInt:0x0406, commandInt:0x07] }:
    utils_processedZclMessage 'Configure Reporting Response', "attribute=Occupancy, data=${msg.data}"
    return
{{/ @events }}
{{!--------------------------------------------------------------------------}}
