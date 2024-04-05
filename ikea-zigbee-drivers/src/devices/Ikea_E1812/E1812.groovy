{{!--------------------------------------------------------------------------}}
{{# @configure }}

// Configuration for devices.Ikea_E1812
cmds += "zdo bind 0x${device.deviceNetworkId} 0x${device.endpointId} 0x01 0x0006 {${device.zigbeeId}} {}" // On/Off cluster
cmds += "zdo bind 0x${device.deviceNetworkId} 0x${device.endpointId} 0x01 0x0008 {${device.zigbeeId}} {}" // Level Control cluster
{{/ @configure }}
{{!--------------------------------------------------------------------------}}
{{# @events }}

// Events for devices.Ikea_E1812
// ===================================================================================================================

// Button was pushed
case { contains it, [clusterInt:0x0006, commandInt:0x01] }:
    List<String> button = BUTTONS.ONOFF
    utils_sendEvent name:'pushed', value:button[0], type:'physical', isStateChange:true, descriptionText:"Button ${button[0]} (${button[1]}) was pushed"
    return

// Button was double tapped
case { contains it, [clusterInt:0x0006, commandInt:0x00] }:
    List<String> button = BUTTONS.ONOFF
    utils_sendEvent name:'doubleTapped', value:button[0], type:'physical', isStateChange:true, descriptionText:"Button ${button[0]} (${button[1]}) was double tapped"
    return

// Button was held
case { contains it, [clusterInt:0x0008, commandInt:0x05] }:
    List<String> button = BUTTONS.ONOFF
    utils_sendEvent name:'held', value:button[0], type:'physical', isStateChange:true, descriptionText:"Button ${button[0]} (${button[1]}) was held"
    return

// Button was released
case { contains it, [clusterInt:0x0008, commandInt:0x07] }:
    List<String> button = BUTTONS.ONOFF
    utils_sendEvent name:'released', value:button[0], type:'physical', isStateChange:true, descriptionText:"Button ${button[0]} (${button[1]}) was released"
    return
{{/ @events }}
{{!--------------------------------------------------------------------------}}
