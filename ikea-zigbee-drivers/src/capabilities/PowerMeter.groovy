{{!--------------------------------------------------------------------------}}
{{# @definition }}
capability 'PowerMeter'
{{/ @definition }}
{{!--------------------------------------------------------------------------}}
{{# @configure }}

// Configuration for capability.PowerMeter
cmds += "zdo bind 0x${device.deviceNetworkId} 0x${device.endpointId} 0x01 0x0B04 {${device.zigbeeId}} {}" // Electrical Measurement cluster
cmds += "he cr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0B04 0x050B 0x21 0x0000 0x4650 {02} {}" // Report ActivePower (uint16) at least every 5 hours (Δ = 0.2W)
{{/ @configure }}
{{!--------------------------------------------------------------------------}}
{{# @refresh }}

// Refresh for capability.PowerMeter
cmds += zigbee.readAttribute(0x0B04, 0x0604) // PowerMultiplier
cmds += zigbee.readAttribute(0x0B04, 0x0605) // PowerDivisor
cmds += zigbee.readAttribute(0x0B04, 0x050B) // ActivePower
{{/ @refresh }}
{{!--------------------------------------------------------------------------}}
{{# @events }}

// Events for capability.PowerMeter
// ===================================================================================================================

// Report/Read Attributes Reponse: ActivePower
case { contains it, [clusterInt:0x0B04, commandInt:0x0A, attrInt:0x050B] }:
case { contains it, [clusterInt:0x0B04, commandInt:0x01, attrInt:0x050B] }:
    Integer power = Integer.parseInt(msg.value, 16) * (state.powerMultiplier ?: 1) / (state.powerDivisor ?: 1)
    utils_sendEvent name:'power', value:power, unit:'W', descriptionText:"Power is ${power} W", type:type
    utils_processedZclMessage "${msg.commandInt == 0x0A ? 'Report' : 'Read'} Attributes Response", "ActivePower=${msg.value} (${power}W)"
    return

// Report/Read Attributes Reponse: ApparentPower
case { contains it, [clusterInt:0x0B04, commandInt:0x0A, attrInt:0x050F] }:
    Integer power = Integer.parseInt(msg.value, 16) * (state.powerMultiplier ?: 1) / (state.powerDivisor ?: 1)
    utils_processedZclMessage "${msg.commandInt == 0x0A ? 'Report' : 'Read'} Attributes Response", "ApparentPower=${msg.value} (${power}W)"
    return

// Read Attributes Reponse: PowerMultiplier
case { contains it, [clusterInt:0x0B04, commandInt:0x01, attrInt:0x0604] }:
    state.powerMultiplier = Integer.parseInt(msg.value, 16)
    utils_processedZclMessage 'Read Attributes Response', "PowerMultiplier=${msg.value}"
    return

// Read Attributes Reponse: PowerDivisor
case { contains it, [clusterInt:0x0B04, commandInt:0x01, attrInt:0x0605] }:
    state.powerDivisor = Integer.parseInt(msg.value, 16)
    utils_processedZclMessage 'Read Attributes Response', "PowerDivisor=${msg.value}"
    return

// Other events that we expect but are not usefull for capability.PowerMeter behavior
case { contains it, [clusterInt:0x0B04, commandInt:0x07] }:
    utils_processedZclMessage 'Configure Reporting Response', "attribute=ActivePower, data=${msg.data}"
    return
case { contains it, [clusterInt:0x0B04, commandInt:0x06, isClusterSpecific:false, direction:'01'] }: // Configure Reporting Command
    return
{{/ @events }}
{{!--------------------------------------------------------------------------}}
