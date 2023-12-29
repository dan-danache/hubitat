{{!--------------------------------------------------------------------------}}
{{# @definition }}
capability "MotionSensor"
{{/ @definition }}
{{!--------------------------------------------------------------------------}}
{{# @configure }}

// Configuration for capability.Occupancy
cmds += "he cr 0x${device.deviceNetworkId} 0x02 0x0406 0x0001 0x18 0x0000 0x4650 {00} {}" // Report Occupancy (map8)
cmds += "zdo bind 0x${device.deviceNetworkId} 0x02 0x01 0x0406 {${device.zigbeeId}} {}" // Occupancy Sensing cluster
{{/ @configure }}
{{!--------------------------------------------------------------------------}}
{{# @events }}

// Events for capability.Occupancy

// Report/Read Attributes Reponse: Occupancy
case { contains it, [clusterInt:0x0406, commandInt:0x0A, attrInt:0x0000] }:
case { contains it, [clusterInt:0x0406, commandInt:0x01, attrInt:0x0000] }:
    String motion = msg.value == "01" ? "active" : "inactive"
    Utils.sendEvent(name:"motion", value:motion, type:"physical", descriptionText:"Is ${motion}")
    return Utils.processedZclMessage("${msg.commandInt == 0x0A ? "Report" : "Read"} Attributes Response", "Occupancy=${msg.value}")

// Other events that we expect but are not usefull for capability.Occupancy behavior
case { contains it, [clusterInt:0x0406, commandInt:0x07] }:  // ConfigureReportingResponse
    return
{{/ @events }}
{{!--------------------------------------------------------------------------}}
