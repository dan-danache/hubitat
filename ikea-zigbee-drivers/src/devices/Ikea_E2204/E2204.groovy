{{!--------------------------------------------------------------------------}}
{{# @inputs }}

// Inputs for devices.Ikea_E2204
input(
    name: 'childLock', type: 'bool',
    title: 'Child lock',
    description: '<small>Lock physical button, safeguarding against accidental operation.</small>',
    defaultValue: false
)
{{/ @inputs }}
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

// Write Attributes Response
case { contains it, [endpointInt:0x01, clusterInt:0xFC85, commandInt:0x04, isClusterSpecific:false, isManufacturerSpecific:true, manufacturerId:'117C'] }:
    log_info 'Child lock successfully configured!'
    return
{{/ @events }}
{{!--------------------------------------------------------------------------}}
