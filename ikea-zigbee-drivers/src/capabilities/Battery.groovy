{{!--------------------------------------------------------------------------}}
{{# @definition }}
capability "Battery"
{{/ @definition }}
{{!--------------------------------------------------------------------------}}
{{# @configure }}

// Configuration for capability.Battery
cmds += "zdo bind 0x${device.deviceNetworkId} 0x01 0x01 0x0001 {${device.zigbeeId}} {}" // Power Configuration cluster
cmds += "he cr 0x${device.deviceNetworkId} 0x01 0x0001 0x0021 0x20 0x0000 0x4650 {02} {}" // Report BatteryPercentage (uint8) at least every 5 hours (Δ = 1%)
cmds += zigbee.readAttribute(0x0001, 0x0021)  // BatteryPercentage
{{/ @configure }}
{{!--------------------------------------------------------------------------}}
{{# @events }}

// Events for capability.Battery

// Report/Read Attributes Reponse: BatteryPercentage
case { contains it, [clusterInt:0x0001, commandInt:0x0A, attrInt:0x0021] }:
case { contains it, [clusterInt:0x0001, commandInt:0x01, attrInt:0x0021] }:

    // The value 0xff indicates an invalid or unknown reading
    if (msg.value == "FF") return Log.warn("Ignored invalid remaining battery percentage value: 0x${msg.value}")

    Integer percentage = Integer.parseInt(msg.value, 16)
    {{# params.half }}
    percentage =  percentage / 2
    {{/ params.half }}
    Utils.sendEvent name:"battery", value:percentage, unit:"%", descriptionText:"Battery is ${percentage}% full", type:type
    return Utils.processedZclMessage("${msg.commandInt == 0x0A ? "Report" : "Read"} Attributes Response", "BatteryPercentage=${percentage}%")

// Other events that we expect but are not usefull for capability.Battery behavior
case { contains it, [clusterInt:0x0001, commandInt:0x07] }:
    return Utils.processedZclMessage("Configure Reporting Response", "attribute=battery, data=${msg.data}")
{{/ @events }}
{{!--------------------------------------------------------------------------}}
