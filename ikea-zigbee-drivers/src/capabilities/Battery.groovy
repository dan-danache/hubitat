{{!--------------------------------------------------------------------------}}
{{# @definition }}
capability "Battery"
{{/ @definition }}
{{!--------------------------------------------------------------------------}}
{{# @configure }}

// Configuration for capability.Battery
cmds += "he cr 0x${device.deviceNetworkId} 0x01 0x0001 0x0021 0x20 0x0000 0x4650 {02} {}" // Report battery at least every 5 hours (min 1% change)
cmds += "zdo bind 0x${device.deviceNetworkId} 0x01 0x01 0x0001 {${device.zigbeeId}} {}" // Power Configuration cluster
cmds += zigbee.readAttribute(0x0001, 0x0021)  // BatteryPercentage
{{/ @configure }}
{{!--------------------------------------------------------------------------}}
{{# @events }}

// Events for capability.Battery

// Report Attributes, Read Attributes Reponse: BatteryPercentage
case { contains it, [clusterInt:0x0001, commandInt:0x0A, attrInt:0x0021] }:
case { contains it, [clusterInt:0x0001, commandInt:0x01, attrInt:0x0021] }:
    Integer percentage = Integer.parseInt(msg.value, 16)

    // (0xFF) 255 is an invalid value for the battery percentage attribute, so we just ignore it
    if (percentage == 255) {
        return Log.warn("Ignored invalid reported battery percentage value: 0xFF (255)")
    }

    {{# params.half }}
    percentage =  percentage / 2
    {{/ params.half }}
    Utils.sendEvent name:"battery", value:percentage, unit:"%", type:"physical", descriptionText:"Battery is ${percentage}% full"
    return Utils.processedZclMessage("Report/Read Attributes Response", "BatteryPercentage=${percentage}%")

// Other events that we expect but are not usefull for capability.Battery behavior
case { contains it, [clusterInt:0x0001, commandInt:0x07] }:  // ConfigureReportingResponse
    return
{{/ @events }}
{{!--------------------------------------------------------------------------}}
