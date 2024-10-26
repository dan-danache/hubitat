{{!--------------------------------------------------------------------------}}
{{# @definition }}
capability 'VoltageMeasurement'
{{/ @definition }}
{{!--------------------------------------------------------------------------}}
{{# @configure }}

// Configuration for capability.VoltageMeasurement
{{^ params.skipClusterBind}}
cmds += "zdo bind 0x${device.deviceNetworkId} 0x${device.endpointId} 0x01 0x0B04 {${device.zigbeeId}} {}" // Electrical Measurement cluster
{{/ params.skipClusterBind}}
cmds += "he cr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0B04 0x0600 0x21 0x0000 0x0E10 {0100} {}" // Report ACVoltageMultiplier (uint16) (Δ = 1)
cmds += "he cr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0B04 0x0601 0x21 0x0000 0x0E10 {0100} {}" // Report ACVoltageDivisor (uint16) (Δ = 1)
{{/ @configure }}
{{!--------------------------------------------------------------------------}}
{{# @inputs }}

// Inputs for capability.VoltageMeasurement
input(
    name:'voltageReportDelta', type:'enum', title:'Voltage report frequency', required:true,
    description:'<small>Configure when device reports current voltage.</small>',
    options:[
          '1':'Report changes of +/- 1 volt',
          '2':'Report changes of +/- 2 volts',
          '5':'Report changes of +/- 5 volts',
         '10':'Report changes of +/- 10 volts',
         '20':'Report changes of +/- 20 volts',
         '50':'Report changes of +/- 50 volts',
    ],
    defaultValue:'1'
)
{{/ @inputs }}
{{!--------------------------------------------------------------------------}}
{{# @updated }}

// Preferences for capability.VoltageMeasurement
if (voltageReportDelta == null) {
    voltageReportDelta = '1'
    device.updateSetting 'voltageReportDelta', [value:voltageReportDelta, type:'enum']
}
log_info "🛠️ voltageReportDelta = +/- ${voltageReportDelta} volts"
Integer voltageReportDeltaAdjusted = Math.max(Integer.parseInt(voltageReportDelta) * (state.voltageDivisor ?: 1) / (state.voltageMultiplier ?: 1), 1.00)
cmds += "he cr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0B04 0x0505 0x21 0x0000 0x0000 {${utils_payload voltageReportDeltaAdjusted, 4}} {}" // Report RMSVoltage (uint16)
{{/ @updated }}
{{!--------------------------------------------------------------------------}}
{{!--------------------------------------------------------------------------}}
{{# @refresh }}

// Refresh for capability.VoltageMeasurement
cmds += zigbee.readAttribute(0x0B04, 0x0600) // ACVoltageMultiplier
cmds += zigbee.readAttribute(0x0B04, 0x0601) // ACVoltageDivisor
cmds += zigbee.readAttribute(0x0B04, 0x0505) // RMSVoltage
{{/ @refresh }}
{{!--------------------------------------------------------------------------}}
{{# @events }}

// Events for capability.VoltageMeasurement
// ===================================================================================================================

// Report/Read Attributes Reponse: RMSVoltage
case { contains it, [clusterInt:0x0B04, commandInt:0x0A, attrInt:0x0505] }:
case { contains it, [clusterInt:0x0B04, commandInt:0x01, attrInt:0x0505] }:

    // A RMSVoltage of 0xFFFF indicates that the voltage measurement is invalid
    if (msg.value == 'FFFF') {
        log_warn "Ignored invalid voltage value: 0x${msg.value}"
        return
    }

    String voltage = new BigDecimal(Integer.parseInt(msg.value, 16) * (state.voltageMultiplier ?: 1) / (state.voltageDivisor ?: 1)).setScale(2, RoundingMode.HALF_UP).toPlainString()
    utils_sendEvent name:'voltage', value:voltage, unit:'V', descriptionText:"Voltage is ${voltage} V", type:type
    utils_processedZclMessage "${msg.commandInt == 0x0A ? 'Report' : 'Read'} Attributes Response", "RMSVoltage=${msg.value} (${voltage} V)"
    return

// Read Attributes Reponse: ACVoltageMultiplier
case { contains it, [clusterInt:0x0B04, commandInt:0x01, attrInt:0x0600] }:
case { contains it, [clusterInt:0x0B04, commandInt:0x0A, attrInt:0x0600] }:
    state.voltageMultiplier = Integer.parseInt(msg.value, 16)
    utils_processedZclMessage 'Read Attributes Response', "ACVoltageMultiplier=${msg.value}"
    return

// Read Attributes Reponse: ACVoltageDivisor
case { contains it, [clusterInt:0x0B04, commandInt:0x01, attrInt:0x0601] }:
case { contains it, [clusterInt:0x0B04, commandInt:0x0A, attrInt:0x0601] }:
    state.voltageDivisor = Integer.parseInt(msg.value, 16)
    utils_processedZclMessage 'Read Attributes Response', "ACVoltageDivisor=${msg.value}"
    return

// Other events that we expect but are not usefull
case { contains it, [clusterInt:0x0B04, commandInt:0x07] }:
    utils_processedZclMessage 'Configure Reporting Response', "attribute=RMSVoltage, data=${msg.data}"
    return
case { contains it, [clusterInt:0x0B04, commandInt:0x06, isClusterSpecific:false, direction:'01'] }: // Configure Reporting Response
    return
{{/ @events }}
{{!--------------------------------------------------------------------------}}
