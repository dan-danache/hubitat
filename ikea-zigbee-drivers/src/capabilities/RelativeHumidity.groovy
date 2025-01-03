{{!--------------------------------------------------------------------------}}
{{# @definition }}
capability 'RelativeHumidityMeasurement'
{{/ @definition }}
{{!--------------------------------------------------------------------------}}
{{# @configure }}

// Configuration for capability.RelativeHumidity
cmds += "zdo bind 0x${device.deviceNetworkId} 0x${device.endpointId} 0x01 0x0405 {${device.zigbeeId}} {}" // Relative Humidity Measurement cluster
cmds += "he cr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0405 0x0000 0x21 0x000F 0x0E10 {3200} {}" // Report MeasuredValue (uint16) at most every 15 seconds, at least every 1 hour (Δ = 0.5%)
{{/ @configure }}
{{!--------------------------------------------------------------------------}}
{{# @refresh }}

// Refresh for capability.RelativeHumidity
cmds += zigbee.readAttribute(0x0405, 0x0000) // RelativeHumidity
{{/ @refresh }}
{{!--------------------------------------------------------------------------}}
{{# @events }}

// Events for capability.RelativeHumidity
// ===================================================================================================================

// Report/Read Attributes Reponse: MeasuredValue
case { contains it, [clusterInt:0x0405, commandInt:0x0A, attrInt:0x0000] }:
case { contains it, [clusterInt:0x0405, commandInt:0x01, attrInt:0x0000] }:

    // A MeasuredValue of 0xFFFF indicates that the measurement is invalid
    if (msg.value == 'FFFF') {
        log_warn "Ignored invalid relative humidity value: 0x${msg.value}"
        return
    }

    String humidity = "${Integer.parseInt(msg.value, 16) / 100}"
    utils_sendEvent name:'humidity', value:humidity, unit:'%rh', descriptionText:"Relative humidity is ${humidity}%", type:type
    utils_processedZclMessage "${msg.commandInt == 0x0A ? 'Report' : 'Read'} Attributes Response", "RelativeHumidity=${msg.value}"
    return

// Other events that we expect but are not usefull
case { contains it, [clusterInt:0x0405, commandInt:0x07] }:
    utils_processedZclMessage 'Configure Reporting Response', "attribute=RelativeHumidity, data=${msg.data}"
    return
{{/ @events }}
{{!--------------------------------------------------------------------------}}
