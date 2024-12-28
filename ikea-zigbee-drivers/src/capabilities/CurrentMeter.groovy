{{!--------------------------------------------------------------------------}}
{{# @definition }}
capability 'CurrentMeter'
{{/ @definition }}
{{!--------------------------------------------------------------------------}}
{{# @configure }}

// Configuration for capability.CurrentMeter
{{^ params.skipClusterBind}}
cmds += "zdo bind 0x${device.deviceNetworkId} 0x${device.endpointId} 0x01 0x0B04 {${device.zigbeeId}} {}" // Electrical Measurement cluster
{{/ params.skipClusterBind}}
{{/ @configure }}
{{!--------------------------------------------------------------------------}}
{{# @inputs }}

// Inputs for capability.CurrentMeter
input(
    name:'amperageReportDelta', type:'enum', title:'Amperage report frequency', required:true,
    description:'Configure when device reports current amperage',
    options:[
          '0':'Report all changes',
          '5':'Report changes of +/- 5 milliamperes',
         '10':'Report changes of +/- 10 milliamperes',
         '20':'Report changes of +/- 20 milliamperes',
         '50':'Report changes of +/- 50 milliamperes',
        '100':'Report changes of +/- 100 milliamperes',
        '200':'Report changes of +/- 200 milliamperes',
        '500':'Report changes of +/- 500 milliamperes',
       '1000':'Report changes of +/- 1 ampere',
       '2000':'Report changes of +/- 2 amperes',
       '5000':'Report changes of +/- 5 amperes',
    ],
    defaultValue:'5'
)
{{/ @inputs }}
{{!--------------------------------------------------------------------------}}
{{# @updated }}

// Preferences for capability.CurrentMeter
if (amperageReportDelta == null) {
    amperageReportDelta = '5'
    device.updateSetting 'amperageReportDelta', [value:amperageReportDelta, type:'enum']
}
log_info "🛠️ amperageReportDelta = +/- ${amperageReportDelta} milliamperes"
Integer amperageReportDeltaAdjusted = Math.max(Integer.parseInt(amperageReportDelta) * (state.amperageDivisor ?: 1) / (state.amperageMultiplier ?: 1) / 1000, 1.00)
cmds += "he cr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0B04 0x0508 0x21 0x0000 0x0000 {${utils_payload amperageReportDeltaAdjusted, 4}} {}" // Report RMSCurrent (uint16)
{{/ @updated }}
{{!--------------------------------------------------------------------------}}
{{# @refresh }}

// Refresh for capability.CurrentMeter
cmds += zigbee.readAttribute(0x0B04, 0x0602) // ACCurrentMultiplier
cmds += zigbee.readAttribute(0x0B04, 0x0603) // ACCurrentDivisor
cmds += zigbee.readAttribute(0x0B04, 0x0508) // RMSCurrent
{{/ @refresh }}
{{!--------------------------------------------------------------------------}}
{{# @events }}

// Events for capability.CurrentMeter
// ===================================================================================================================

// Report/Read Attributes Reponse: RMSCurrent
case { contains it, [clusterInt:0x0B04, commandInt:0x0A, attrInt:0x0508] }:
case { contains it, [clusterInt:0x0B04, commandInt:0x01, attrInt:0x0508] }:

    // A RMSCurrent of 0xFFFF indicates that the amperage measurement is invalid
    if (msg.value == 'FFFF') {
        log_warn "Ignored invalid amperage value: 0x${msg.value}"
        return
    }

    String amperage = new BigDecimal(Integer.parseInt(msg.value, 16) * (state.amperageMultiplier ?: 1) / (state.amperageDivisor ?: 1)).setScale(2, RoundingMode.HALF_UP).toPlainString()
    utils_sendEvent name:'amperage', value:amperage, unit:'A', descriptionText:"Amperage is ${amperage} A", type:type
    utils_processedZclMessage "${msg.commandInt == 0x0A ? 'Report' : 'Read'} Attributes Response", "RMSCurrent=${msg.value} (${amperage} A)"
    return

// Read Attributes Reponse: ACCurrentMultiplier
case { contains it, [clusterInt:0x0B04, commandInt:0x01, attrInt:0x0602] }:
    state.amperageMultiplier = Integer.parseInt(msg.value, 16)
    utils_processedZclMessage 'Read Attributes Response', "ACCurrentMultiplier=${msg.value}"
    return

// Read Attributes Reponse: ACCurrentDivisor
case { contains it, [clusterInt:0x0B04, commandInt:0x01, attrInt:0x0603] }:
    state.amperageDivisor = Integer.parseInt(msg.value, 16)
    utils_processedZclMessage 'Read Attributes Response', "ACCurrentDivisor=${msg.value}"
    return

// Other events that we expect but are not usefull
case { contains it, [clusterInt:0x0B04, commandInt:0x07] }:
    utils_processedZclMessage 'Configure Reporting Response', "attribute=RMSCurrent, data=${msg.data}"
    return
case { contains it, [clusterInt:0x0B04, commandInt:0x06, isClusterSpecific:false, direction:'01'] }: // Configure Reporting Response
    return
{{/ @events }}
{{!--------------------------------------------------------------------------}}
