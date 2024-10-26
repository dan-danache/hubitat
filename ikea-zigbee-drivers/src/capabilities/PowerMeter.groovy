{{!--------------------------------------------------------------------------}}
{{# @definition }}
capability 'PowerMeter'
{{/ @definition }}
{{!--------------------------------------------------------------------------}}
{{# @configure }}

// Configuration for capability.PowerMeter
{{^ params.skipClusterBind}}
cmds += "zdo bind 0x${device.deviceNetworkId} 0x${device.endpointId} 0x01 0x0B04 {${device.zigbeeId}} {}" // Electrical Measurement cluster
{{/ params.skipClusterBind}}
cmds += "he cr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0B04 0x0604 0x21 0x0000 0x0000 {0100} {}" // Report ACPowerMultiplier (uint16) (Δ = 1)
cmds += "he cr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0B04 0x0605 0x21 0x0000 0x0000 {0100} {}" // Report ACPowerDivisor (uint16) (Δ = 1)
{{/ @configure }}
{{!--------------------------------------------------------------------------}}
{{# @inputs }}

// Inputs for capability.PowerMeter
input(
    name:'powerReportDelta', type:'enum', title:'Power report frequency', required:true,
    description:'<small>Configure when device reports current power demand.</small>',
    options:[
          '1':'Report changes of +/- 1 watt',
          '2':'Report changes of +/- 2 watts',
          '5':'Report changes of +/- 5 watts',
         '10':'Report changes of +/- 10 watts',
         '20':'Report changes of +/- 20 watts',
         '50':'Report changes of +/- 50 watts',
        '100':'Report changes of +/- 100 watts',
        '200':'Report changes of +/- 200 watts',
        '500':'Report changes of +/- 500 watts',
    ],
    defaultValue:'1'
)
{{/ @inputs }}
{{!--------------------------------------------------------------------------}}
{{# @updated }}

// Preferences for capability.PowerMeter
if (powerReportDelta == null) {
    powerReportDelta = '1'
    device.updateSetting 'powerReportDelta', [value:powerReportDelta, type:'enum']
}
log_info "🛠️ powerReportDelta = +/- ${powerReportDelta} watts"
Integer powerReportDeltaAdjusted = Math.max(Integer.parseInt(powerReportDelta) * (state.powerDivisor ?: 1) / (state.powerMultiplier ?: 1), 1.00)
cmds += "he cr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0B04 0x050B 0x29 0x0000 0x0000 {${utils_payload powerReportDeltaAdjusted, 4}} {}" // Report ActivePower (int16)
{{/ @updated }}
{{!--------------------------------------------------------------------------}}
{{# @refresh }}

// Refresh for capability.PowerMeter
cmds += zigbee.readAttribute(0x0B04, 0x0604) // ACPowerMultiplier
cmds += zigbee.readAttribute(0x0B04, 0x0605) // ACPowerDivisor
cmds += zigbee.readAttribute(0x0B04, 0x050B) // ActivePower
{{/ @refresh }}
{{!--------------------------------------------------------------------------}}
{{# @events }}

// Events for capability.PowerMeter
// ===================================================================================================================

// Report/Read Attributes Reponse: ActivePower
case { contains it, [clusterInt:0x0B04, commandInt:0x0A, attrInt:0x050B] }:
case { contains it, [clusterInt:0x0B04, commandInt:0x01, attrInt:0x050B] }:

    // A ActivePower of 0xFFFF indicates that the power measurement is invalid
    if (msg.value == '8000') {
        log_warn "Ignored invalid power value: 0x${msg.value}"
        return
    }

    String power = new BigDecimal(Integer.parseInt(msg.value, 16) * (state.powerMultiplier ?: 1) / (state.powerDivisor ?: 1)).setScale(2, RoundingMode.HALF_UP).toPlainString()
    utils_sendEvent name:'power', value:power, unit:'W', descriptionText:"Power is ${power} W", type:type
    utils_processedZclMessage "${msg.commandInt == 0x0A ? 'Report' : 'Read'} Attributes Response", "ActivePower=${msg.value} (${power} W)"
    return

// Read Attributes Reponse: ACPowerMultiplier
case { contains it, [clusterInt:0x0B04, commandInt:0x01, attrInt:0x0604] }:
case { contains it, [clusterInt:0x0B04, commandInt:0x0A, attrInt:0x0604] }:
    state.powerMultiplier = Integer.parseInt(msg.value, 16)
    utils_processedZclMessage 'Read Attributes Response', "ACPowerMultiplier=${msg.value}"
    return

// Read Attributes Reponse: ACPowerDivisor
case { contains it, [clusterInt:0x0B04, commandInt:0x01, attrInt:0x0605] }:
case { contains it, [clusterInt:0x0B04, commandInt:0x0A, attrInt:0x0605] }:
    state.powerDivisor = Integer.parseInt(msg.value, 16)
    utils_processedZclMessage 'Read Attributes Response', "ACPowerDivisor=${msg.value}"
    return

// Other events that we expect but are not usefull
case { contains it, [clusterInt:0x0B04, commandInt:0x07] }:
    utils_processedZclMessage 'Configure Reporting Response', "attribute=ActivePower, data=${msg.data}"
    return
case { contains it, [clusterInt:0x0B04, commandInt:0x06, isClusterSpecific:false, direction:'01'] }: // Configure Reporting Response
    return
{{/ @events }}
{{!--------------------------------------------------------------------------}}
