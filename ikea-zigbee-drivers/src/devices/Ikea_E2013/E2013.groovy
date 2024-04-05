{{!--------------------------------------------------------------------------}}
{{# @inputs }}

// Inputs for devices.Ikea_E2013
input(
    name: 'swapOpenClosed', type: 'bool',
    title: 'Invert contact state',
    description: '<small>Swaps "open" and "closed" status reports.</small>',
    defaultValue: false,
    required: true
)
{{/ @inputs }}
{{!--------------------------------------------------------------------------}}
{{# @updated }}

// Preferences for devices.Ikea_E2013
if (swapOpenClosed == null) {
    swapOpenClosed = false
    device.updateSetting 'swapOpenClosed', [value:swapOpenClosed, type:'bool']
}
log_info "🛠️ swapOpenClosed = ${swapOpenClosed}"
{{/ @updated }}
{{!--------------------------------------------------------------------------}}
{{# @events }}

// Events for devices.Ikea_E2013
// ===================================================================================================================

// Report/Read Attributes Reponse: ZoneStatus
case { contains it, [clusterInt:0x0500, commandInt:0x0A, attrInt:0x0002] }:
case { contains it, [clusterInt:0x0500, commandInt:0x01, attrInt:0x0002] }:
    String contact = msg.value[-1] == '1' ^ (swapOpenClosed == true) ? 'open' : 'closed'
    utils_sendEvent name:'contact', value:contact, descriptionText:"Is ${contact}", type:type
    utils_processedZclMessage "${msg.commandInt == 0x0A ? 'Report' : 'Read'} Attributes Response", "ZoneStatus=${msg.value}"
    return

// Ignore Configure Reporting Response for attribute ZoneStatus
case { contains it, [clusterInt:0x0500, commandInt:0x07] }:
    utils_processedZclMessage 'Configure Reporting Response', "attribute=contact, data=${msg.data}"
    return
{{/ @events }}
{{!--------------------------------------------------------------------------}}
