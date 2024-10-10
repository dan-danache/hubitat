{{!--------------------------------------------------------------------------}}
{{# @definition }}
capability 'Actuator'
capability 'WindowShade'
{{/ @definition }}
{{!--------------------------------------------------------------------------}}
{{# @fields }}

// Fields for capability.WindowShade
import java.text.DecimalFormat
{{/ @fields }}
{{!--------------------------------------------------------------------------}}
{{# @inputs }}

// Inputs for capability.WindowShade
input(
    name: 'openRunTime', type: 'number',
    title: 'Open run time',
    description: '<small>Set seconds required to go from fully closed to fully open. Range 1s .. 600s.</small>',
    defaultValue: 1.00,
    range: '1..600',
    required: true
)
input(
    name: 'closeRunTime', type: 'number',
    title: 'Close run time',
    description: '<small>Set seconds required to go from fully open to fully closed. Range 1s .. 600s.</small>',
    defaultValue: 1.00,
    range: '1..600',
    required: true
)
{{/ @inputs }}
{{!--------------------------------------------------------------------------}}
{{# @attributes }}

// Attributes for capability.WindowShade
{{/ @attributes }}
{{!--------------------------------------------------------------------------}}
{{# @implementation }}

// Implementation for capability.WindowShade
void open() {
    log_debug '🎬 Sending Open / Up command'
    utils_sendZigbeeCommands(["he raw 0x${device.deviceNetworkId} 0x01 0x${device.endpointId} 0x0102 {114300}"])
}
void close() {
    log_debug '🎬 Sending Close / Down command'
    utils_sendZigbeeCommands(["he raw 0x${device.deviceNetworkId} 0x01 0x${device.endpointId} 0x0102 {114301}"])
}
void startPositionChange(String direction) {
    if (direction == 'open') open()
    else close()
}
void stopPositionChange() {
    log_debug '🎬 Sending Stop command'
    utils_sendZigbeeCommands(["he raw 0x${device.deviceNetworkId} 0x01 0x${device.endpointId} 0x0102 {114302}"])
}
void setPosition(BigDecimal position) {
    Integer pos = position < 0 ? 0 : (position > 100 ? 100 : position)
    log_debug "🎬 Sending Go to Lift Percentage command: ${pos}%"
    utils_sendZigbeeCommands(["he raw 0x${device.deviceNetworkId} 0x01 0x${device.endpointId} 0x0102 {114305 ${utils_payload pos, 2}}"])
}
{{/ @implementation }}
{{!--------------------------------------------------------------------------}}
{{# @updated }}

// Preferences for capability.WindowShade
if (openRunTime == null) {
    openRunTime = '3.00'
    device.updateSetting 'openRunTime', [value:openRunTime, type:'number']
}
log_info "🛠️ openRunTime = ${openRunTime}s"
Integer openTime = (new BigDecimal(openRunTime) * 100).intValue()
cmds += zigbee.writeAttribute(0x0102, 0x0001, DataType.UINT16, openTime, [mfgCode:'0x128B'])

if (closeRunTime == null) {
    closeRunTime = '3.00'
    device.updateSetting 'closeRunTime', [value:closeRunTime, type:'number']
}
log_info "🛠️ closeRunTime = ${closeRunTime}s"
Integer closeTime = (new BigDecimal(closeRunTime) * 100).intValue()
cmds += zigbee.writeAttribute(0x0102, 0x0002, DataType.UINT16, closeTime, [mfgCode:'0x128B'])
{{/ @updated }}
{{!--------------------------------------------------------------------------}}
{{# @configure }}

// Configuration for capability.WindowShade
cmds += "zdo bind 0x${device.deviceNetworkId} 0x${device.endpointId} 0x01 0x0102 {${device.zigbeeId}} {}" // Window Covering cluster
cmds += "he cr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0102 0x0008 0x20 0x0001 0x0258 {01} {}" // Report CurrentPositionLiftPercentage (uint8) at least every 10 minutes (Δ = 1)
cmds += "he cr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0102 0x0009 0x20 0x0000 0xFFFF {00} {}" // Disable reporting for CurrentPositionTiltPercentage (uint8)
cmds += "he cr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0102 0x0001 0x21 0x0001 0x0000 {0100} {128B}" // Report only changes for OpenRunTime (uint16)
cmds += "he cr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0102 0x0002 0x21 0x0001 0x0000 {0100} {128B}" // Report only changes for CloseRunTime (uint16)
cmds += "he cr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0102 0x0003 0x21 0x0000 0xFFFF {0000} {128B}" // Disable reporting for TiltUpRunTime (uint16)
cmds += "he cr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0102 0x0004 0x21 0x0000 0xFFFF {0000} {128B}" // Disable reporting for TiltDownRunTime (uint16)
{{/ @configure }}
{{!--------------------------------------------------------------------------}}
{{# @refresh }}

// Refresh for capability.WindowShade
cmds += zigbee.readAttribute(0x0102, 0x0008) // CurrentPositionLiftPercentage
cmds += zigbee.readAttribute(0x0102, 0x0001, [mfgCode: '0x128B']) // OpenRunTime
cmds += zigbee.readAttribute(0x0102, 0x0002, [mfgCode: '0x128B']) // CloseRunTime
{{/ @refresh }}
{{!--------------------------------------------------------------------------}}
{{# @events }}

// Events for capability.WindowShade
// ===================================================================================================================

// Report/Read Attributes Reponse: CurrentPositionLiftPercentage
case { contains it, [clusterInt:0x0102, commandInt:0x0A, attrInt:0x0008] }:
case { contains it, [clusterInt:0x0102, commandInt:0x01, attrInt:0x0008] }:
    Integer position = Integer.parseInt msg.value, 16
    utils_sendEvent name:'position', value:position, descriptionText:"Position is ${position}%", type:'digital'
    utils_processedZclMessage "${msg.commandInt == 0x0A ? 'Report' : 'Read'} Attributes Response", "CurrentPositionLiftPercentage=${msg.value} (${position}%)"
    return

// Report/Read Attributes Reponse: OpenRunTime
case { contains it, [clusterInt:0x0102, commandInt:0x0A, attrInt:0x0001] }:
case { contains it, [clusterInt:0x0102, commandInt:0x01, attrInt:0x0001] }:
    BigDecimal openTime = new BigDecimal(msg.value == 'FFFF' ? 0 : Integer.parseInt(msg.value, 16) / 100)
    String openRunTime = new DecimalFormat("0.00").format(openTime)
    device.updateSetting 'openRunTime', [value:openRunTime, type:'number']
    utils_processedZclMessage "${msg.commandInt == 0x0A ? 'Report' : 'Read'} Attributes Response", "OpenRunTime=${msg.value} (${openRunTime}s)"
    return

// Report/Read Attributes Reponse: CloseRunTime
case { contains it, [clusterInt:0x0102, commandInt:0x0A, attrInt:0x0002] }:
case { contains it, [clusterInt:0x0102, commandInt:0x01, attrInt:0x0002] }:
    BigDecimal closeTime = new BigDecimal(msg.value == 'FFFF' ? 0 : Integer.parseInt(msg.value, 16) / 100)
    String closeRunTime = new DecimalFormat("0.00").format(closeTime)
    device.updateSetting 'closeRunTime', [value:closeRunTime, type:'number']
    utils_processedZclMessage "${msg.commandInt == 0x0A ? 'Report' : 'Read'} Attributes Response", "CloseRunTime=${msg.value} (${closeRunTime}s)"
    return

// Default Reponse: Device not calibrated yet
case { contains it, [clusterInt:0x0102, commandInt:0x0B, data:['05', '01']] }:
    log_warn "rejected the Go to Lift Percentage command because device is not calibrated yet!"
    return

// Other events that we expect but are not usefull
case { contains it, [clusterInt:0x0102, commandInt:0x07, data:['00']] }:
    utils_processedZclMessage 'Configure Reporting Response', "data=${msg.data}"
    return
case { contains it, [clusterInt:0x0102, commandInt:0x0A, attrInt:0x0003] }: // Report Attributes Reponse: TiltUpRunTime
case { contains it, [clusterInt:0x0102, commandInt:0x0A, attrInt:0x0004] }: // Report Attributes Reponse: TiltDownRunTime
case { contains it, [clusterInt:0x0102, commandInt:0x0A, attrInt:0x0009] }: // Report Attributes Reponse: CurrentPositionTiltPercentage
case { contains it, [clusterInt:0x0102, commandInt:0x04] }: // Write Attributes Response (0x04)
    return

// ===================================================================================================================
{{/ @events }}
{{!--------------------------------------------------------------------------}}
