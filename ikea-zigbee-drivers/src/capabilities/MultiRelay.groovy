{{!--------------------------------------------------------------------------}}
{{# @definition }}
capability 'Actuator'
{{/ @definition }}
{{!--------------------------------------------------------------------------}}
{{# @fields }}

// Fields for capability.MultiRelay
import com.hubitat.app.ChildDeviceWrapper
{{/ @fields }}
{{!--------------------------------------------------------------------------}}
{{# @implementation }}

// Implementation for capability.MultiRelay
private ChildDeviceWrapper fetchChildDevice(Integer moduleNumber) {
    ChildDeviceWrapper childDevice = getChildDevice("${device.deviceNetworkId}-${moduleNumber}")
    return childDevice ?: addChildDevice('hubitat', 'Generic Component Switch', "${device.deviceNetworkId}-${moduleNumber}", [name:"${device.displayName} - Relay L${moduleNumber}", label:"Relay L${moduleNumber}", isComponent:true])
}

void componentOff(ChildDeviceWrapper childDevice) {
    log_debug "▲ Received Off request from ${childDevice.displayName}"
    Integer endpointInt = Integer.parseInt(childDevice.deviceNetworkId.split('-')[1])
    utils_sendZigbeeCommands(["he raw 0x${device.deviceNetworkId} 0x01 0x0${endpointInt} 0x0006 {014300}"])
}

void componentOn(ChildDeviceWrapper childDevice) {
    log_debug "▲ Received On request from ${childDevice.displayName}"
    Integer endpointInt = Integer.parseInt(childDevice.deviceNetworkId.split('-')[1])
    utils_sendZigbeeCommands(["he raw 0x${device.deviceNetworkId} 0x01 0x0${endpointInt} 0x0006 {014301}"])
}

void componentRefresh(ChildDeviceWrapper childDevice) {
    log_debug "▲ Received Refresh request from ${childDevice.displayName}"
    refresh()
}
{{/ @implementation }}
{{!--------------------------------------------------------------------------}}
{{# @configure }}

// Configuration for capability.MultiRelay
{{# params.endpoints }}
cmds += "zdo bind 0x${device.deviceNetworkId} {{ . }} 0x01 0x0006 {${device.zigbeeId}} {}" // On/Off cluster (ep {{ . }})
{{/ params.endpoints }}

{{# params.endpoints }}
cmds += "he cr 0x${device.deviceNetworkId} {{ . }} 0x0006 0x0000 0x10 0x0000 0x0258 {01} {}" // Report OnOff (bool) at least every 10 minutes (ep {{ . }})
{{/ params.endpoints }}
{{/ @configure }}
{{!--------------------------------------------------------------------------}}
{{# @refresh }}

// Refresh for capability.MultiRelay
{{# params.endpoints }}
cmds += zigbee.readAttribute(0x0006, 0x0000, [destEndpoint:{{ . }}]) // OnOff (ep {{ . }})
{{/ params.endpoints }}
{{/ @refresh }}
{{!--------------------------------------------------------------------------}}
{{# @events }}

// Events for capability.MultiRelay
// ===================================================================================================================

// Report/Read Attributes: OnOff
case { contains it, [clusterInt:0x0006, commandInt:0x0A, attrInt:0x0000] }:
case { contains it, [clusterInt:0x0006, commandInt:0x01, attrInt:0x0000] }:
    Integer moduleNumber = msg.endpointInt
    String newState = msg.value == '00' ? 'off' : 'on'

    // Send event to module child device (only if state needs to change)
    ChildDeviceWrapper childDevice = fetchChildDevice(moduleNumber)
    if (newState != childDevice.currentValue('switch', true)) {
        childDevice.parse([[name:'switch', value:newState, descriptionText:"${childDevice.displayName} was turned ${newState}", type:type]])
    }

    utils_processedZclMessage "${msg.commandInt == 0x0A ? 'Report' : 'Read'} Attributes Response", "Module=${moduleNumber}, Switch=${newState}"
    return

// Other events that we expect but are not usefull for capability.MultiRelay behavior
case { contains it, [clusterInt:0x0006, commandInt:0x07] }:
    utils_processedZclMessage 'Configure Reporting Response', "attribute=switch, data=${msg.data}"
    return
{{/ @events }}
{{!--------------------------------------------------------------------------}}
