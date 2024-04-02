{{!--------------------------------------------------------------------------}}
{{# @definition }}
capability 'Battery'
{{/ @definition }}
{{!--------------------------------------------------------------------------}}
{{# @configure }}

// Configuration for capability.Battery
cmds += "zdo bind 0x${device.deviceNetworkId} 0x${device.endpointId} 0x01 0x0001 {${device.zigbeeId}} {}" // Power Configuration cluster
cmds += "he cr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0001 0x0021 0x20 0x0000 0x4650 {02} {}" // Report BatteryPercentage (uint8) at least every 5 hours (Δ = 1%)
{{/ @configure }}
{{!--------------------------------------------------------------------------}}
{{# @refresh }}

// Refresh for capability.Battery
cmds += zigbee.readAttribute(0x0001, 0x0021) // BatteryPercentage
{{/ @refresh }}
{{!--------------------------------------------------------------------------}}
{{# @events }}

// Events for capability.Battery
// ===================================================================================================================

// Report/Read Attributes Reponse: BatteryPercentage
case { contains it, [clusterInt:0x0001, commandInt:0x0A, attrInt:0x0021] }:
case { contains it, [clusterInt:0x0001, commandInt:0x01] }:

    // Hubitat fails to parse some Read Attributes Responses
    if (msg.value == null && msg.data != null && msg.data[0] == '21' && msg.data[1] == '00') {
        msg.value = msg.data[2]
    }

    // The value 0xff indicates an invalid or unknown reading
    if (msg.value == 'FF') {
        log_warn "Ignored invalid remaining battery percentage value: 0x${msg.value}"
        return
    }

    Integer percentage = Integer.parseInt(msg.value, 16)
    {{# params.half }}
    percentage =  percentage / 2
    {{/ params.half }}
    utils_sendEvent name:'battery', value:percentage, unit:'%', descriptionText:"Battery is ${percentage}% full", type:type
    utils_processedZclMessage "${msg.commandInt == 0x0A ? 'Report' : 'Read'} Attributes Response", "BatteryPercentage=${percentage}%"
    return

// Other events that we expect but are not usefull for capability.Battery behavior
case { contains it, [clusterInt:0x0001, commandInt:0x07] }:
    utils_processedZclMessage 'Configure Reporting Response', "attribute=BatteryPercentage, data=${msg.data}"
    return
{{/ @events }}
{{!--------------------------------------------------------------------------}}
