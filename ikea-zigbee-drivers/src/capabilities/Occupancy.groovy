{{!--------------------------------------------------------------------------}}
{{# @definition }}
capability "MotionSensor"
{{/ @definition }}
{{!--------------------------------------------------------------------------}}
{{# @configure }}

// Configuration for capability.Occupancy
cmds += "he cr 0x${device.deviceNetworkId} 0x02 0x0406 0x0001 0x18 0x0000 0x4650 {00} {}" // Report Occupancy (map8)
cmds += "zdo bind 0x${device.deviceNetworkId} 0x02 0x01 0x0406 {${device.zigbeeId}} {}" // Occupancy Sensing cluster
cmds += zigbee.readAttribute(0x0406, 0x0000)  // Occupancy
{{/ @configure }}
{{!--------------------------------------------------------------------------}}
{{# @events }}

// Events for capability.Occupancy

// Report Attributes, Read Attributes Reponse: Occupancy
case { contains it, [clusterInt:0x0406, commandInt:0x0A, attrInt:0x0000] }:
case { contains it, [clusterInt:0x0406, commandInt:0x01, attrInt:0x0000] }:
    Integer value = Integer.parseInt(msg.value, 16)
    String motion = (value % 2) > 0 ? "active" : "inactive"
    return Utils.sendEvent(name:"motion", value:motion, type:"physical", descriptionText:"Is ${motion}")

// Other events that we expect but are not usefull for capability.Occupancy behavior
case { contains it, [clusterInt:0x0406, commandInt:0x07] }:  // ConfigureReportingResponse
    return
{{/ @events }}
{{!--------------------------------------------------------------------------}}
