{{!--------------------------------------------------------------------------}}
{{# @definition }}
capability "IlluminanceMeasurement"
capability "Sensor"
{{/ @definition }}
{{!--------------------------------------------------------------------------}}
{{# @configure }}

// Configuration for capability.Illuminance
cmds += "zdo bind 0x${device.deviceNetworkId} 0x03 0x01 0x0400 {${device.zigbeeId}} {}" // Illuminance Measurement cluster
cmds += "he cr 0x${device.deviceNetworkId} 0x03 0x0400 0x0001 0x21 0x0000 0x4650 {0000} {}" // Report MeasuredValue (uint16) at least every 5 hours (Δ = 0)
{{/ @configure }}
{{!--------------------------------------------------------------------------}}
{{# @events }}

// Events for capability.Illuminance

// Report/Read Attributes Reponse: MeasuredValue
case { contains it, [clusterInt:0x0400, commandInt:0x0A, attrInt:0x0000] }:
case { contains it, [clusterInt:0x0400, commandInt:0x01, attrInt:0x0000] }:
    Integer illuminance = Integer.parseInt(msg.value, 16)

    // 0xFFFF represents an invalid illuminance value, so we just ignore it
    if (illuminance == 0xFFFF) return Log.warn("Ignored invalid reported illuminance value: 0xFFFF")

    // Transform raw value to lux
    if (illuminance != 0) {
        illuminance = Math.pow(10, (illuminance - 1) / 10000)
    }
    Utils.sendEvent name:"illuminance", value:illuminance, unit:"lx", descriptionText:"Illuminance is ${illuminance} lux", type:type
    return Utils.processedZclMessage("${msg.commandInt == 0x0A ? "Report" : "Read"} Attributes Response", "Illuminance/MeasuredValue=${msg.value}")

// Other events that we expect but are not usefull for capability.Illuminance behavior
case { contains it, [clusterInt:0x0400, commandInt:0x07] }:
    return Utils.processedZclMessage("Configure Reporting Response", "attribute=illuminance, data=${msg.data}")
{{/ @events }}
{{!--------------------------------------------------------------------------}}
