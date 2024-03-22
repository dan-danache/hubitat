// Report/Read Attributes Reponse: ZoneStatus
case { contains it, [clusterInt:0x0500, commandInt:0x0A, attrInt:0x0002] }:
case { contains it, [clusterInt:0x0500, commandInt:0x01, attrInt:0x0002] }:
    String water = msg.value[-1] == "1" ? "wet" : "dry"
    Utils.sendEvent name:"water", value:water, descriptionText:"Is ${water}", type:type
    return Utils.processedZclMessage("${msg.commandInt == 0x0A ? "Report" : "Read"} Attributes Response", "ZoneStatus=${msg.value}")

// Ignore Configure Reporting Response for attribute ZoneStatus
case { contains it, [clusterInt:0x0500, commandInt:0x07] }:
    return Utils.processedZclMessage("Configure Reporting Response", "attribute=ZoneStatus, data=${msg.data}")
