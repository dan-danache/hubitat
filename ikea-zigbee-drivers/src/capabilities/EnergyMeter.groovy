{{!--------------------------------------------------------------------------}}
{{# @definition }}
capability 'EnergyMeter'
{{/ @definition }}
{{!--------------------------------------------------------------------------}}
{{# @configure }}

// Configuration for capability.EnergyMeter
cmds += "zdo bind 0x${device.deviceNetworkId} 0x${device.endpointId} 0x01 0x0702 {${device.zigbeeId}} {}" // (Metering (Smart Energy) cluster
cmds += "he cr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0702 0x0000 0x25 0x0000 0x4650 {00} {}" // Report CurrentSummationDelivered (uint48) at least every 5 hours (Δ = 0)
{{/ @configure }}
{{!--------------------------------------------------------------------------}}
{{# @refresh }}

// Refresh for capability.EnergyMeter
cmds += zigbee.readAttribute(0x0702, 0x0000) // EnergySumation
cmds += zigbee.readAttribute(0x0702, 0x0301) // EnergyMultiplier
cmds += zigbee.readAttribute(0x0702, 0x0302) // EnergyDivisor
{{/ @refresh }}
{{!--------------------------------------------------------------------------}}
{{# @events }}

// Events for capability.EnergyMeter
// ===================================================================================================================

// Report/Read Attributes Reponse: EnergySummation
case { contains it, [clusterInt:0x0702, commandInt:0x0A, attrInt:0x0000] }:
case { contains it, [clusterInt:0x0702, commandInt:0x01, attrInt:0x0000] }:
    Integer energy = Integer.parseInt(msg.value, 16) * (state.energyMultiplier ?: 1) / (state.energyDivisor ?: 1)
    utils_sendEvent name:'energy', value:energy, unit:'kWh', descriptionText:"Energy is ${energy} kWh", type:type
    utils_processedZclMessage "${msg.commandInt == 0x0A ? 'Report' : 'Read'} Attributes Response", "EnergySummation=${msg.value}"
    return

// Read Attributes Reponse: EnergyMultiplier
case { contains it, [clusterInt:0x0702, commandInt:0x01, attrInt:0x0301] }:
    state.energyMultiplier = Integer.parseInt(msg.value, 16)
    utils_processedZclMessage 'Read Attributes Response', "EnergyMultiplier=${msg.value}"
    return

// Read Attributes Reponse: EnergyDivisor
case { contains it, [clusterInt:0x0702, commandInt:0x01, attrInt:0x0302] }:
    state.energyDivisor = Integer.parseInt(msg.value, 16)
    utils_processedZclMessage 'Read Attributes Response', "EnergyDivisor=${msg.value}"
    return

// Other events that we expect but are not usefull for capability.PowerMeter behavior
case { contains it, [clusterInt:0x0702, commandInt:0x07] }:
    utils_processedZclMessage 'Configure Reporting Response', "attribute=CurrentSummation, data=${msg.data}"
    return
{{/ @events }}
{{!--------------------------------------------------------------------------}}
