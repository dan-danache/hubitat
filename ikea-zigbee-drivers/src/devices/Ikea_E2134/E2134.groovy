{{!--------------------------------------------------------------------------}}
{{# @configure }}

// Configuration for devices.Ikea_E2134
cmds += "zdo bind 0x${device.deviceNetworkId} 0x${device.endpointId} 0x01 0x0400 {${device.zigbeeId}} {}" // Illuminance Measurement cluster
cmds += "he cr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0400 0x0000 0x18 0x0000 0x4650 {01} {}" // Report Illuminance/MeasuredValue (uint16) at least every 5 hours (Δ = 0)

cmds += "zdo bind 0x${device.deviceNetworkId} 0x${device.endpointId} 0x01 0x0406 {${device.zigbeeId}} {}" // Occupancy Sensing cluster
cmds += "he cr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0406 0x0000 0x21 0x0000 0x4650 {01} {}" // Report Illuminance/MeasuredValue (uint16) at least every 5 hours (Δ = 0)
{{/ @configure }}
{{!--------------------------------------------------------------------------}}
{{# @events }}

// Events for devices.Ikea_E2134
// ===================================================================================================================

// Report/Read Attributes Reponse: Occupancy/MeasuredValue
case { contains it, [clusterInt:0x0406, commandInt:0x0A, attrInt:0x0000] }:
case { contains it, [clusterInt:0x0406, commandInt:0x01, attrInt:0x0000] }:
    String motion = msg.value == '01' ? 'active' : 'inactive'
    utils_sendEvent(name:'motion', value:motion, type:'physical', descriptionText:"Is ${motion}")
    utils_processedZclMessage "${msg.commandInt == 0x0A ? 'Report' : 'Read'} Attributes Response", "Occupancy/MeasuredValue=${msg.value}"
    return

// Report/Read Attributes Reponse: Illuminance/MeasuredValue
case { contains it, [clusterInt:0x0400, commandInt:0x0A, attrInt:0x0000] }:
case { contains it, [clusterInt:0x0400, commandInt:0x01, attrInt:0x0000] }:
    Integer illuminance = Integer.parseInt(msg.value, 16)

    // 0xFFFF represents an invalid illuminance value, so we just ignore it
    if (illuminance == 0xFFFF) {
        log_warn 'Ignored invalid reported illuminance value: 0xFFFF'
        return
    }

    // Transform raw value to lux
    if (illuminance != 0) {
        illuminance = Math.pow(10, (illuminance - 1) / 10000)
    }
    utils_sendEvent name:'illuminance', value:illuminance, unit:'lx', descriptionText:"Illuminance is ${illuminance} lux", type:type
    utils_processedZclMessage "${msg.commandInt == 0x0A ? 'Report' : 'Read'} Attributes Response", "Illuminance/MeasuredValue=${msg.value}"
    return

// Ignore Configure Reporting Response for attribute Occupancy/MeasuredValue
case { contains it, [clusterInt:0x0406, commandInt:0x07] }:
    utils_processedZclMessage 'Configure Reporting Response', "attribute=Occupancy/MeasuredValue, data=${msg.data}"
    return

// Ignore Configure Reporting Response for attribute Illuminance/MeasuredValue
case { contains it, [clusterInt:0x0400, commandInt:0x07] }:
    utils_processedZclMessage 'Configure Reporting Response', "attribute=Illuminance/MeasuredValue, data=${msg.data}"
    return

// Ignore report for attribute Illuminance/MinMeasuredValue
case { contains it, [clusterInt:0x0400, commandInt:0x0A, attrInt:0x0001] }:
    utils_processedZclMessage 'Report Attributes Response', "Illuminance/MinMeasuredValue=${msg.value}"
    return
{{/ @events }}
{{!--------------------------------------------------------------------------}}
