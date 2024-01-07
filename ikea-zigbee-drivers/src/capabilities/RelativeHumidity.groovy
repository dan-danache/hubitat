{{!--------------------------------------------------------------------------}}
{{# @definition }}
capability "RelativeHumidityMeasurement"
{{/ @definition }}
{{!--------------------------------------------------------------------------}}
{{# @configure }}

// Configuration for capability.RelativeHumidity
cmds += "zdo bind 0x${device.deviceNetworkId} 0x01 0x01 0x0405 {${device.zigbeeId}} {}" // Relative Humidity Measurement cluster
cmds += "he cr 0x${device.deviceNetworkId} 0x01 0x0405 0x0000 0x21 0x0000 0x0258 {6400} {}" // Report MeasuredValue (uint16) at least every 10 minutes (Δ = 1%)
{{/ @configure }}
{{!--------------------------------------------------------------------------}}
{{# @events }}

// Events for capability.RelativeHumidity

// Report/Read Attributes Reponse: MeasuredValue
case { contains it, [clusterInt:0x0405, commandInt:0x0A, attrInt:0x0000] }:
case { contains it, [clusterInt:0x0405, commandInt:0x01, attrInt:0x0000] }:

    // A MeasuredValue of 0xFFFF indicates that the measurement is invalid
    if (msg.value == "FFFF") return Log.warn("Ignored invalid relative humidity value: 0x${msg.value}")

    Integer humidity = Math.round(Integer.parseInt(msg.value, 16) / 100)
    Utils.sendEvent name:"humidity", value:humidity, unit:"%rh", descriptionText:"Relative humidity is ${humidity} %", type:type
    return Utils.processedZclMessage("${msg.commandInt == 0x0A ? "Report" : "Read"} Attributes Response", "RelativeHumidity=${msg.value}")

// Other events that we expect but are not usefull for capability.RelativeHumidity behavior
case { contains it, [clusterInt:0x0405, commandInt:0x07] }:
    return Utils.processedZclMessage("Configure Reporting Response", "attribute=humidity, data=${msg.data}")
{{/ @events }}
{{!--------------------------------------------------------------------------}}
