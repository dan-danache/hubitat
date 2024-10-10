{{!--------------------------------------------------------------------------}}
{{# @definition }}
capability 'EnergyMeter'
capability 'PowerMeter'
{{/ @definition }}
{{!--------------------------------------------------------------------------}}
{{# @configure }}

// Configuration for capability.EnergyMeter
cmds += "zdo bind 0x${device.deviceNetworkId} 0x${device.endpointId} 0x01 0x0702 {${device.zigbeeId}} {}" // (Metering (Smart Energy) cluster
//cmds += "he cr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0702 0x0000 0x25 0x0000 0x0E10 {640000000000} {}" // Report CurrentSummationDelivered (uint48) at least every 1 hour (Δ = 0.1kWh)
//cmds += "he cr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0702 0x0400 0x2A 0x0000 0x0E10 {320000} {}" // Report InstantaneousDemand (int24) at least every 1 hour (Δ = 50W)
{{/ @configure }}
{{!--------------------------------------------------------------------------}}
{{# @inputs }}

// Inputs for capability.EnergyMeter
input(
    name: 'energyReportDelta', type: 'enum',
    title: 'Energy report frequency',
    description: '<small>Configure when device reports total consumed energy.</small>',
    options: [
         '100':'Report changes of +/- 0.1kWh',
         '500':'Report changes of +/- 0.5kWh',
        '1000':'Report changes of +/- 1.0kWh',
    ],
    defaultValue: '100',
    required: true
)
input(
    name: 'powerReportDelta', type: 'enum',
    title: 'Power report frequency',
    description: '<small>Configure when device reports current power demand.</small>',
    options: [
          '2':'Report changes of +/- 2W',
         '10':'Report changes of +/- 10W',
         '50':'Report changes of +/- 50W',
        '100':'Report changes of +/- 100W',
    ],
    defaultValue: '50',
    required: true
)
{{/ @inputs }}
{{!--------------------------------------------------------------------------}}
{{# @updated }}

// Preferences for capability.EnergyMeter
if (energyReportDelta == null) {
    energyReportDelta = '100'
    device.updateSetting 'energyReportDelta', [value:energyReportDelta, type:'enum']
}
log_info "🛠️ Energy report frequency = +/- ${energyReportDelta}kWh"
cmds += "he cr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0702 0x0000 0x25 0x0000 0x0E10 {${utils_payload Integer.parseInt(energyReportDelta), 12}} {}"

if (powerReportDelta == null) {
    powerReportDelta = '50'
    device.updateSetting 'powerReportDelta', [value:powerReportDelta, type:'enum']
}
log_info "🛠️ Power report frequency = +/- ${powerReportDelta}W"
cmds += "he cr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0702 0x0400 0x2A 0x0000 0x0E10 {${utils_payload Integer.parseInt(powerReportDelta), 6}} {}"
{{/ @updated }}
{{!--------------------------------------------------------------------------}}
{{# @refresh }}

// Refresh for capability.EnergyMeter
cmds += zigbee.readAttribute(0x0702, 0x0301) // Multiplier
cmds += zigbee.readAttribute(0x0702, 0x0302) // Divisor
cmds += zigbee.readAttribute(0x0702, 0x0000) // EnergySumation
cmds += zigbee.readAttribute(0x0702, 0x0400) // InstantaneousDemand
{{/ @refresh }}
{{!--------------------------------------------------------------------------}}
{{# @events }}

// Events for capability.EnergyMeter
// ===================================================================================================================

// Report/Read Attributes Reponse: EnergySummation
case { contains it, [clusterInt:0x0702, commandInt:0x0A, attrInt:0x0000] }:
case { contains it, [clusterInt:0x0702, commandInt:0x01, attrInt:0x0000] }:
    Long energy = Long.parseLong(msg.value, 16) * (state.multiplier ?: 1) / (state.divisor ?: 1000)
    utils_sendEvent name:'energy', value:energy, unit:'kWh', descriptionText:"Total consumed energy is ${energy} kWh", type:type
    utils_processedZclMessage "${msg.commandInt == 0x0A ? 'Report' : 'Read'} Attributes Response", "EnergySummation=${msg.value} (${energy}kWh)"
    return

// Report/Read Attributes Reponse: InstantaneousDemand
case { contains it, [clusterInt:0x0702, commandInt:0x0A, attrInt:0x0400] }:
case { contains it, [clusterInt:0x0702, commandInt:0x01, attrInt:0x0400] }:
    Integer power = Integer.parseInt(msg.value, 16) * 1000 * (state.multiplier ?: 1) / (state.divisor ?: 1000)
    utils_sendEvent name:'power', value:power, unit:'Watt', descriptionText:"Current power demand is ${power} W", type:type
    utils_processedZclMessage "${msg.commandInt == 0x0A ? 'Report' : 'Read'} Attributes Response", "Power=${msg.value} (${power}W)"
    return

// Read Attributes Reponse: Multiplier
case { contains it, [clusterInt:0x0702, commandInt:0x01, attrInt:0x0301] }:
    state.multiplier = Integer.parseInt(msg.value, 16)
    utils_processedZclMessage 'Read Attributes Response', "Multiplier=${msg.value} (${state.multiplier})"
    return

// Read Attributes Reponse: Divisor
case { contains it, [clusterInt:0x0702, commandInt:0x01, attrInt:0x0302] }:
    state.divisor = Integer.parseInt(msg.value, 16)
    utils_processedZclMessage 'Read Attributes Response', "Divisor=${msg.value} (${state.divisor})"
    return

// Other events that we expect but are not usefull
case { contains it, [clusterInt:0x0702, commandInt:0x07] }:
    utils_processedZclMessage 'Configure Reporting Response', "attribute=CurrentSummation/InstantaneousDemand, data=${msg.data}"
    return
{{/ @events }}
{{!--------------------------------------------------------------------------}}
