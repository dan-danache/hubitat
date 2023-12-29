{{!--------------------------------------------------------------------------}}
{{# @definition }}
capability "ContactSensor"
capability "Sensor"
{{/ @definition }}
{{!--------------------------------------------------------------------------}}
{{# @configure }}

// Configuration for capability.ContactSensor
cmds += "he cr 0x${device.deviceNetworkId} 0x02 0x0500 0x0002 0x19 0x0000 0x4650 {00} {}" // Report ZoneStatus (map16)
cmds += "zdo bind 0x${device.deviceNetworkId} 0x02 0x01 0x0500 {${device.zigbeeId}} {}" // IAS Zone cluster (ep 02)
{{/ @configure }}
{{!--------------------------------------------------------------------------}}
{{# @events }}

// Events for capability.ContactSensor

// Report/Read Attributes Reponse: ZoneStatus
case { contains it, [clusterInt:0x0500, commandInt:0x0A, attrInt:0x0002] }:
case { contains it, [clusterInt:0x0500, commandInt:0x01, attrInt:0x0002] }:
    String contact = msg.value[-1] == "1" ? "open" : "closed"
    Utils.sendEvent(name:"contact", value:contact, type:"physical", descriptionText:"Is ${contact}")
    return Utils.processedZclMessage("${msg.commandInt == 0x0A ? "Report" : "Read"} Attributes Response", "ZoneStatus=${msg.value}")

// Other events that we expect but are not usefull for capability.ContactSensor behavior
case { contains it, [clusterInt:0x0500, commandInt:0x07] }:  // ConfigureReportingResponse
    return
{{/ @events }}
{{!--------------------------------------------------------------------------}}
