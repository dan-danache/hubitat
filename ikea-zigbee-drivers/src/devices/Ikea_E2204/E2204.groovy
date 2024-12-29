{{!--------------------------------------------------------------------------}}
{{# @attributes }}

// Attributes for devices.Ikea_E2204
attribute 'indicatorStatus', 'enum', ['on', 'off']
{{/ @attributes }}
{{!--------------------------------------------------------------------------}}
{{# @commands }}

// Commands for devices.Ikea_E2204
command 'setIndicatorStatus', [[name:'Status*', type:'ENUM', description:'Select LED indicator status on the device', constraints:['on', 'off']]]
{{/ @commands }}
{{!--------------------------------------------------------------------------}}
{{# @inputs }}

// Inputs for devices.Ikea_E2204
input(
    name:'childLock', type:'bool', title:'Child lock',
    description:'Lock physical button, safeguarding against accidental operation',
    defaultValue:false
)
{{/ @inputs }}
{{!--------------------------------------------------------------------------}}
{{# @implementation }}

// Implementation for devices.Ikea_E2204
void setIndicatorStatus(String status) {
    log_debug "🎬 Setting status indicator to: ${status}"
    utils_sendZigbeeCommands(zigbee.writeAttribute(0xFC85, 0x0001, 0x10, status == 'off' ? 0x00 : 0x01, [mfgCode:'0x117C']))
    utils_sendEvent name:'indicatorStatus', value:status, descriptionText:"Indicator status turned ${status}", type:'digital'
}
{{/ @implementation }}
{{!--------------------------------------------------------------------------}}
{{# @updated }}

// Preferences for devices.Ikea_E2204
if (childLock == null) {
    childLock = false
    device.updateSetting 'childLock', [value:childLock, type:'bool']
}
log_info "🛠️ childLock = ${childLock}"
cmds += zigbee.writeAttribute(0xFC85, 0x0000, 0x10, childLock ? 0x01 : 0x00, [mfgCode:'0x117C'])
{{/ @updated }}
{{!--------------------------------------------------------------------------}}
{{# @refresh }}

// Refresh for devices.Ikea_E2204
cmds += zigbee.readAttribute(0xFC85, 0x0000, [mfgCode:'0x117C'] ) // ChildLock
cmds += zigbee.readAttribute(0xFC85, 0x0001, [mfgCode:'0x117C'] ) // IndicatorStatus
{{/ @refresh }}
{{!--------------------------------------------------------------------------}}
{{# @events }}

// Events for devices.Ikea_E2204
// ===================================================================================================================

// Read Attributes: ChildLock
case { contains it, [clusterInt:0xFC85, commandInt:0x01, attrInt:0x0000] }:
    childLock = msg.value == '01'
    device.updateSetting 'childLock', [value:childLock, type:'bool']
    utils_processedZclMessage 'Read Attributes Response', "ChildLock=${msg.value}"
    return

// Read Attributes: IndicatorStatus
case { contains it, [clusterInt:0xFC85, commandInt:0x01, attrInt:0x0001] }:
    String indicatorStatus = msg.value == '00' ? 'off' : 'on'
    utils_sendEvent name:'indicatorStatus', value:indicatorStatus, descriptionText:"Indicator status turned ${indicatorStatus}", type:'digital'
    utils_processedZclMessage 'Read Attributes Response', "IndicatorStatus=${indicatorStatus}"
    return

// Write Attributes Response
case { contains it, [endpointInt:0x01, clusterInt:0xFC85, commandInt:0x04, isClusterSpecific:false, isManufacturerSpecific:true, manufacturerId:'117C'] }:
    return
{{/ @events }}
{{!--------------------------------------------------------------------------}}
