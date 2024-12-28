{{!--------------------------------------------------------------------------}}
{{# @definition }}
capability 'EnergyMeter'
{{/ @definition }}
{{!--------------------------------------------------------------------------}}
{{# @configure }}

// Configuration for capability.EnergyMeter
cmds += "zdo bind 0x${device.deviceNetworkId} 0x${device.endpointId} 0x01 0x0702 {${device.zigbeeId}} {}" // Metering (Smart Energy) cluster
{{/ @configure }}
{{!--------------------------------------------------------------------------}}
{{# @inputs }}

// Inputs for capability.EnergyMeter
input(
    name:'energyReportDelta', type:'enum', title:'Energy report frequency', required:true,
    description:'Configure when device reports total consumed energy',
    options:[
           '0':'Report all changes',
          '10':'Report changes of 10 Wh',
          '20':'Report changes of 20 Wh',
          '50':'Report changes of 50 Wh',
         '100':'Report changes of 100 Wh',
         '200':'Report changes of 200 Wh',
         '500':'Report changes of 500 Wh',
        '1000':'Report changes of 1 kWh',
    ],
    defaultValue:'10'
)
{{/ @inputs }}
{{!--------------------------------------------------------------------------}}
{{# @commands }}

// Commands for capability.EnergyMeter
command 'resetEnergy'
{{/ @commands }}
{{!--------------------------------------------------------------------------}}
{{# @implementation }}
void resetEnergy() {
    log_debug "🎬 Resetting energy counter ..."
    state.resetEnergy = true
    utils_sendZigbeeCommands(zigbee.readAttribute(0x0702, 0x0000))
}
{{/ @implementation }}
{{!--------------------------------------------------------------------------}}
{{# @updated }}

// Preferences for capability.EnergyMeter
if (energyReportDelta == null) {
    energyReportDelta = '10'
    device.updateSetting 'energyReportDelta', [value:energyReportDelta, type:'enum']
}
log_info "🛠️ energyReportDelta = ${energyReportDelta} Wh"
Integer energyReportDeltaAdjusted = Math.max(Integer.parseInt(energyReportDelta) * (state.energyDivisor ?: 1) / (state.energyMultiplier ?: 1) / 1000, 1.00)
cmds += "he cr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0702 0x0000 0x25 0x0000 0x0000 {${utils_payload energyReportDeltaAdjusted, 12}} {}" // Report CurrentSummationDelivered (uint48)
{{/ @updated }}
{{!--------------------------------------------------------------------------}}
{{# @refresh }}

// Refresh for capability.EnergyMeter
cmds += zigbee.readAttribute(0x0702, 0x0301) // EnergyMultiplier
cmds += zigbee.readAttribute(0x0702, 0x0302) // EnergyDivisor
cmds += zigbee.readAttribute(0x0702, 0x0000) // CurrentSummationDelivered
{{/ @refresh }}
{{!--------------------------------------------------------------------------}}
{{# @events }}

// Events for capability.EnergyMeter
// ===================================================================================================================

// Report/Read Attributes Reponse: CurrentSummationDelivered
case { contains it, [clusterInt:0x0702, commandInt:0x0A, attrInt:0x0000] }:
case { contains it, [clusterInt:0x0702, commandInt:0x01, attrInt:0x0000] }:
    String energy = '0.00'
    if (state.resetEnergy == true) {
        state.remove 'resetEnergy'
        state.energyOffset = Long.parseLong(msg.value, 16)
    } else {
        energy = new BigDecimal((Long.parseLong(msg.value, 16) - (state.energyOffset ?: 0)) * (state.energyMultiplier ?: 1) / (state.energyDivisor ?: 1)).setScale(2, RoundingMode.HALF_UP).toPlainString()
    }
    utils_sendEvent name:'energy', value:energy, unit:'kWh', descriptionText:"Energy is ${energy} kWh", type:type
    utils_processedZclMessage "${msg.commandInt == 0x0A ? 'Report' : 'Read'} Attributes Response", "CurrentSummationDelivered=${msg.value} (${energy} kWh)"
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

// Other events that we expect but are not usefull
case { contains it, [clusterInt:0x0702, commandInt:0x07] }:
    utils_processedZclMessage 'Configure Reporting Response', "attribute=CurrentSummation, data=${msg.data}"
    return
{{/ @events }}
{{!--------------------------------------------------------------------------}}
