{{!--------------------------------------------------------------------------}}
{{# @attributes }}

// Attributes for E2112.VocIndex
attribute "vocIndex", "number"
{{/ @attributes }}
{{!--------------------------------------------------------------------------}}
{{# @configure }}

// Configuration for E2112.VocIndex
cmds += "zdo bind 0x${device.deviceNetworkId} 0x01 0x01 0xFC7E {${device.zigbeeId}} {}" // VocIndex Measurement cluster
cmds += "he cr 0x${device.deviceNetworkId} 0x01 0xFC7E 0x0000 0x39 0x000A 0x0258 {40000000} {117C}" // Report MeasuredValue (single) at least every 10 minutes (Δ = ??)
{{/ @configure }}
{{!--------------------------------------------------------------------------}}
{{# @events }}

// Events for E2112.VocIndex

// Report/Read Attributes Reponse: MeasuredValue
case { contains it, [clusterInt:0xFC7E, commandInt:0x0A, attrInt:0x0000] }:
case { contains it, [clusterInt:0xFC7E, commandInt:0x01, attrInt:0x0000] }:

    // A MeasuredValue of 0xFFFFFFFF indicates that the measurement is invalid
    if (msg.value == "FFFFFFFF") return Log.warn("Ignored invalid VOC Index value: 0x${msg.value}")

    Integer vocIndex = Math.round Float.intBitsToFloat(Integer.parseInt(msg.value, 16))
    Utils.sendEvent name:"vocIndex", value:vocIndex, descriptionText:"Voc index is ${vocIndex} / 500", type:type
    return Utils.processedZclMessage("${msg.commandInt == 0x0A ? "Report" : "Read"} Attributes Response", "VocIndex=${msg.value}")

// Other events that we expect but are not usefull for E2112.VocIndex behavior
case { contains it, [clusterInt:0xFC7E, commandInt:0x07] }:
    return Utils.processedZclMessage("Configure Reporting Response", "attribute=vocIndex, data=${msg.data}")
{{/ @events }}
{{!--------------------------------------------------------------------------}}
