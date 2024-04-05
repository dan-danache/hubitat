{{!--------------------------------------------------------------------------}}
{{# @configure }}

// Configuration for devices.E1743
cmds += "zdo bind 0x${device.deviceNetworkId} 0x${device.endpointId} 0x01 0x0006 {${device.zigbeeId}} {}" // On/Off cluster
cmds += "zdo bind 0x${device.deviceNetworkId} 0x${device.endpointId} 0x01 0x0008 {${device.zigbeeId}} {}" // Level Control cluster
{{/ @configure }}
{{!--------------------------------------------------------------------------}}
{{# @events }}

// Events for devices.Ikea_E1743
// ===================================================================================================================

// I/O button was pressed
case { contains it, [clusterInt:0x0006, commandInt:0x00] }:
case { contains it, [clusterInt:0x0006, commandInt:0x01] }:
    List<String> button = msg.commandInt == 0x00 ? BUTTONS.OFF : BUTTONS.ON
    utils_sendEvent name:'pushed', value:button[0], type:'physical', isStateChange:true, descriptionText:"Button ${button[0]} (${button[1]}) was pushed"
    return

// I/O button was held
case { contains it, [clusterInt:0x0008, commandInt:0x01] }:
case { contains it, [clusterInt:0x0008, commandInt:0x05] }:
    List<String> button = msg.commandInt == 0x01 ? BUTTONS.OFF : BUTTONS.ON
    utils_sendEvent name:'held', value:button[0], type:'physical', isStateChange:true, descriptionText:"Button ${button[0]} (${button[1]}) was held"
    return

// I/O button was released
case { contains it, [clusterInt:0x0008, commandInt:0x07] }:
    List<String> button = device.currentValue('held', true) == 1 ? BUTTONS.ON : BUTTONS.OFF
    utils_sendEvent name:'released', value:button[0], type:'physical', isStateChange:true, descriptionText:"Button ${button[0]} (${button[1]}) was released"
    return
{{/ @events }}
{{!--------------------------------------------------------------------------}}
