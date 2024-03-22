// Report/Read Attributes Reponse: Occupancy/MeasuredValue
case { contains it, [clusterInt:0x0406, commandInt:0x0A, attrInt:0x0000] }:
case { contains it, [clusterInt:0x0406, commandInt:0x01, attrInt:0x0000] }:
    String motion = msg.value == "01" ? "active" : "inactive"
    Utils.sendEvent(name:"motion", value:motion, type:"physical", descriptionText:"Is ${motion}")
    return Utils.processedZclMessage("${msg.commandInt == 0x0A ? "Report" : "Read"} Attributes Response", "Occupancy/MeasuredValue=${msg.value}")

// Report/Read Attributes Reponse: Illuminance/MeasuredValue
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

// Ignore Configure Reporting Response for attribute Occupancy/MeasuredValue
case { contains it, [clusterInt:0x0406, commandInt:0x07] }:
    return Utils.processedZclMessage("Configure Reporting Response", "attribute=motion, data=${msg.data}")

// Ignore Configure Reporting Response for attribute Illuminance/MeasuredValue
case { contains it, [clusterInt:0x0400, commandInt:0x07] }:
    return Utils.processedZclMessage("Configure Reporting Response", "attribute=illuminance, data=${msg.data}")
