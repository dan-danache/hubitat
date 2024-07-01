{{!--------------------------------------------------------------------------}}
{{# @definition }}
capability 'Actuator'
{{/ @definition }}
{{!--------------------------------------------------------------------------}}
{{# @fields }}

// Fields for devices.NodOn_SIN-4-FP-21
@Field static final Map<Integer, String> PILOT_WIRE_MODES = [
    '00':'off',
    '01':'comfort',
    '02':'eco',
    '03':'anti-freeze',
    '04':'comfort-1',
    '05':'comfort-2',
]
{{/ @fields }}
{{!--------------------------------------------------------------------------}}
{{# @attributes }}

// Attributes for capability.PilotWire
attribute 'mode', 'enum', PILOT_WIRE_MODES*.value
{{/ @attributes }}
{{!--------------------------------------------------------------------------}}
{{# @commands }}

// Commands for capability.PilotWire
command 'off'
command 'setComfortMode'
command 'setEcoMode'
command 'setAntiFreezeMode'
command 'setComfort_1Mode'
command 'setComfort_2Mode'
{{/ @commands }}
{{!--------------------------------------------------------------------------}}
{{# @implementation }}

// Implementation for capability.PilotWire
void off() {
    log_debug '🎬 Sending Off command'
    utils_sendZigbeeCommands(["he raw 0x${device.deviceNetworkId} 0x01 0x${device.endpointId} 0xFC00 {058B124300 00}"])
}
void setComfortMode() {
    log_debug '🎬 Sending Comfort Mode command'
    utils_sendZigbeeCommands(["he raw 0x${device.deviceNetworkId} 0x01 0x${device.endpointId} 0xFC00 {058B124300 01}"])
}
void setEcoMode() {
    log_debug '🎬 Sending Comfort Mode command'
    utils_sendZigbeeCommands(["he raw 0x${device.deviceNetworkId} 0x01 0x${device.endpointId} 0xFC00 {058B124300 02}"])
}
void setAntiFreezeMode() {
    log_debug '🎬 Sending Anti-Freeze Mode command'
    utils_sendZigbeeCommands(["he raw 0x${device.deviceNetworkId} 0x01 0x${device.endpointId} 0xFC00 {058B124300 03}"])
}
void setComfort_1Mode() {
    log_debug '🎬 Sending Comfort-1 Mode command'
    utils_sendZigbeeCommands(["he raw 0x${device.deviceNetworkId} 0x01 0x${device.endpointId} 0xFC00 {058B124300 04}"])
}
void setComfort_2Mode() {
    log_debug '🎬 Sending Comfort-2 Mode command'
    utils_sendZigbeeCommands(["he raw 0x${device.deviceNetworkId} 0x01 0x${device.endpointId} 0xFC00 {058B124300 05}"])
}
{{/ @implementation }}
{{!--------------------------------------------------------------------------}}
{{# @configure }}

// Configuration for capability.PilotWire
cmds += "zdo bind 0x${device.deviceNetworkId} 0x${device.endpointId} 0x01 0xFC00 {${device.zigbeeId}} {}" // PilotWire cluster
cmds += "he cr 0x${device.deviceNetworkId} 0x${device.endpointId} 0xFC00 0x0000 0x20 0x0000 0x0258 {01} {0x128B}" // Report PilotWireMode (uint8) at least every 10 minutes (Δ = 1)
{{/ @configure }}
{{!--------------------------------------------------------------------------}}
{{# @refresh }}

// Refresh for capability.PilotWire
cmds += zigbee.readAttribute(0xFC00, 0x0000, [mfgCode: '0x128B']) // PilotWireMode
{{/ @refresh }}
{{!--------------------------------------------------------------------------}}
{{# @events }}

// Events for capability.PilotWire
// ===================================================================================================================

// Report/Read Attributes: PilotWireMode
case { contains it, [clusterInt:0xFC00, commandInt:0x0A, attrInt:0x0000] }:
case { contains it, [clusterInt:0xFC00, commandInt:0x01, attrInt:0x0000] }:
    String mode = PILOT_WIRE_MODES[msg.value]
    utils_sendEvent name:'mode', value:mode, descriptionText:"mode changed to ${mode}", type:type
    utils_processedZclMessage "${msg.commandInt == 0x0A ? 'Report' : 'Read'} Attributes Response", "PilotWireMode=${msg.value} (${mode})"
    return

// Other events that we expect but are not usefull
case { contains it, [clusterInt:0xFC00, commandInt:0x07] }:
    utils_processedZclMessage 'Configure Reporting Response', "attribute=PilotWireMode, data=${msg.data}"
    return
{{/ @events }}
{{!--------------------------------------------------------------------------}}
