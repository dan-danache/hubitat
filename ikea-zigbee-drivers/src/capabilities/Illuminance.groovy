{{!--------------------------------------------------------------------------}}
{{# @definition }}
capability "IlluminanceMeasurement"
{{/ @definition }}
{{!--------------------------------------------------------------------------}}
{{# @configure }}

// Configuration for capability.Illuminance
cmds += "he cr 0x${device.deviceNetworkId} 0x03 0x0400 0x0001 0x21 0x0000 0x4650 {05} {}" // Report MeasuredValue (uint16)
cmds += "zdo bind 0x${device.deviceNetworkId} 0x03 0x01 0x0400 {${device.zigbeeId}} {}" // Occupancy Sensing cluster
cmds += zigbee.readAttribute(0x0406, 0x0000)  // MeasuredValue
{{/ @configure }}
{{!--------------------------------------------------------------------------}}
{{# @events }}

// Events for capability.Illuminance

// Report Attributes, Read Attributes Reponse: MeasuredValue
case { contains it, [clusterInt:0x0400, commandInt:0x0A, attrInt:0x0000] }:
case { contains it, [clusterInt:0x0400, commandInt:0x01, attrInt:0x0000] }:
    Integer illuminance = Integer.parseInt(msg.value, 16)
    return Utils.sendEvent(name:"illuminance", value:illuminance, unit:"lx", type:"physical", descriptionText:"Illuminance is ${illuminance}")

// Other events that we expect but are not usefull for capability.Illuminance behavior
case { contains it, [clusterInt:0x0406, commandInt:0x07] }:  // ConfigureReportingResponse
    return
{{/ @events }}
{{!--------------------------------------------------------------------------}}
