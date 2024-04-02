{{!--------------------------------------------------------------------------}}
{{# @events }}

// Events for devices.E2013
// ===================================================================================================================

// Report/Read Attributes Reponse: ZoneStatus
case { contains it, [clusterInt:0x0500, commandInt:0x0A, attrInt:0x0002] }:
case { contains it, [clusterInt:0x0500, commandInt:0x01, attrInt:0x0002] }:
    String contact = msg.value[-1] == '1' ? 'open' : 'closed'
    utils_sendEvent name:'contact', value:contact, descriptionText:"Is ${contact}", type:type
    utils_processedZclMessage "${msg.commandInt == 0x0A ? 'Report' : 'Read'} Attributes Response", "ZoneStatus=${msg.value}"
    return

// Ignore Configure Reporting Response for attribute ZoneStatus
case { contains it, [clusterInt:0x0500, commandInt:0x07] }:
    utils_processedZclMessage 'Configure Reporting Response', "attribute=contact, data=${msg.data}"
    return
{{/ @events }}
{{!--------------------------------------------------------------------------}}
