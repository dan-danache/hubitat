{{!--------------------------------------------------------------------------}}
{{# @definition }}
capability 'Actuator'
{{/ @definition }}
{{!--------------------------------------------------------------------------}}
{{# @fields }}

// Fields for capability.MultiRelay
import com.hubitat.app.ChildDeviceWrapper
import com.hubitat.app.DeviceWrapper
{{/ @fields }}
{{!--------------------------------------------------------------------------}}
{{# @implementation }}

// Implementation for capability.MultiRelay
private ChildDeviceWrapper fetchChildDevice(Integer relayNumber) {
    ChildDeviceWrapper childDevice = getChildDevice("${device.deviceNetworkId}-${relayNumber}")
    return childDevice ?: addChildDevice('dandanache', 'NodOn Component Relay Switch', "${device.deviceNetworkId}-${relayNumber}", [name:"Component Relay Switch", label:"Relay #${relayNumber}", isComponent:true])
}

void componentOff(DeviceWrapper childDevice) {
    log_debug "▲ Received Off request from ${childDevice.displayName}"
    Integer endpointInt = Integer.parseInt(childDevice.deviceNetworkId.split('-')[1])
    log_debug "🎬 Sending Off command for ${childDevice.displayName}"
    utils_sendZigbeeCommands(["he raw 0x${device.deviceNetworkId} 0x01 0x0${endpointInt} 0x0006 {014100}"])
}

void componentOn(DeviceWrapper childDevice) {
    log_debug "▲ Received On request from ${childDevice.displayName}"
    Integer endpointInt = Integer.parseInt(childDevice.deviceNetworkId.split('-')[1])
    log_debug "🎬 Sending On command for ${childDevice.displayName}"
    utils_sendZigbeeCommands(["he raw 0x${device.deviceNetworkId} 0x01 0x0${endpointInt} 0x0006 {014201}"])
}

void componentToggle(DeviceWrapper childDevice) {
    log_debug "▲ Received Toggle request from ${childDevice.displayName}"
    Integer endpointInt = Integer.parseInt(childDevice.deviceNetworkId.split('-')[1])
    log_debug "🎬 Sending Toggle command for ${childDevice.displayName}"
    utils_sendZigbeeCommands(["he raw 0x${device.deviceNetworkId} 0x01 0x0${endpointInt} 0x0006 {014302}"])
}

void componentOnWithTimedOff(DeviceWrapper childDevice, BigDecimal onTime = 1) {
    log_debug "▲ Received OnWithTimedOff request from ${childDevice.displayName}"
    Integer endpointInt = Integer.parseInt(childDevice.deviceNetworkId.split('-')[1])
    log_debug "🎬 Sending OnWithTimedOff command for ${childDevice.displayName}"
    Integer delay = onTime < 1 ? 1 : (onTime > 6500 ? 6500 : onTime)
    Integer dur = delay * 10
    String payload = "00 ${utils_payload dur, 4} 0000"
    utils_sendZigbeeCommands(["he raw 0x${device.deviceNetworkId} 0x01 0x${device.endpointId} 0x0006 {114442 ${payload}}"])
}

void componentRefresh(DeviceWrapper childDevice) {
    log_debug "▲ Received Refresh request from ${childDevice.displayName}"
    refresh()
}

void componentUpdatePowerOnBehavior(DeviceWrapper childDevice, String powerOnBehavior) {
    log_debug "▲ Received UpdatePowerOnBehavior request from ${childDevice.displayName}"
    Integer endpointInt = Integer.parseInt(childDevice.deviceNetworkId.split('-')[1])
    log_debug "🎬 Sending WriteAttributes command for ${childDevice.displayName}"
    Integer attrValue = powerOnBehavior == 'TURN_POWER_OFF' ? 0x00 : (powerOnBehavior == 'TURN_POWER_ON' ? 0x01 : 0xFF)
    utils_sendZigbeeCommands(zigbee.writeAttribute(
        0x0006, 0x4003, 0x30, attrValue, [destEndpoint:endpointInt]
    ))
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
    Integer relayNumber = msg.endpointInt
    String newState = msg.value == '00' ? 'off' : 'on'

    // Send event to module child device (only if state needs to change)
    ChildDeviceWrapper childDevice = fetchChildDevice(relayNumber)
    if (newState != childDevice.currentValue('switch', true)) {
        childDevice.parse([[name:'switch', value:newState, descriptionText:"Was turned ${newState}", type:type]])
    }

    utils_processedZclMessage "${msg.commandInt == 0x0A ? 'Report' : 'Read'} Attributes Response", "Relay=${relayNumber}, Switch=${newState}"
    return

// Other events that we expect but are not usefull
case { contains it, [clusterInt:0x0006, commandInt:0x07] }:
    utils_processedZclMessage 'Configure Reporting Response', "attribute=OnOff, endpoint=${msg.endpointInt}, data=${msg.data}"
    return
case { contains it, [clusterInt:0x0006, commandInt:0x04] }: // Write Attribute Response
    return
{{/ @events }}
{{!--------------------------------------------------------------------------}}
