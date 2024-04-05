{{!--------------------------------------------------------------------------}}
{{# @configure }}

// Configuration for devices.Ikea_E1810
cmds += "zdo bind 0x${device.deviceNetworkId} 0x${device.endpointId} 0x01 0x0005 {${device.zigbeeId}} {}" // Scenes cluster
cmds += "zdo bind 0x${device.deviceNetworkId} 0x${device.endpointId} 0x01 0x0006 {${device.zigbeeId}} {}" // On/Off cluster
cmds += "zdo bind 0x${device.deviceNetworkId} 0x${device.endpointId} 0x01 0x0008 {${device.zigbeeId}} {}" // Level Control cluster
{{/ @configure }}
{{!--------------------------------------------------------------------------}}
{{# @events }}

// Events for devices.Ikea_E1810
// ===================================================================================================================

// Button Prev/Next was pressed
case { contains it, [clusterInt:0x0005, commandInt:0x07] }:
    List<String> button = msg.data[0] == '00' ? BUTTONS.NEXT : BUTTONS.PREV
    utils_sendEvent name:'pushed', value:button[0], type:'physical', isStateChange:true, descriptionText:"Button ${button[0]} (${button[1]}) was pushed"
    return

// Button Prev/Next was held
case { contains it, [clusterInt:0x0005, commandInt:0x08] }:
    List<String> button = msg.data[0] == '00' ? BUTTONS.NEXT : BUTTONS.PREV
    utils_sendEvent name:'held', value:button[0], type:'physical', isStateChange:true, descriptionText:"Button ${button[0]} (${button[1]}) was held"
    return

// Button Prev/Next was released
case { contains it, [clusterInt:0x0005, commandInt:0x09] }:
    List<String> button = device.currentValue('held', true) == 4 ? BUTTONS.NEXT : BUTTONS.PREV
    utils_sendEvent name:'released', value:button[0], type:'physical', isStateChange:true, descriptionText:"Button ${button[0]} (${button[1]}) was released"
    return

// Power button was pushed
case { contains it, [clusterInt:0x0006, commandInt:0x02] }:
    List<String> button = BUTTONS.POWER
    utils_sendEvent name:'pushed', value:button[0], type:'physical', isStateChange:true, descriptionText:"Button ${button[0]} (${button[1]}) was pushed"
    return

// Plus/Minus button was pushed
case { contains it, [clusterInt:0x0008, commandInt:0x02] }:
case { contains it, [clusterInt:0x0008, commandInt:0x06] }:
    List<String> button = msg.commandInt == 0x02 ? BUTTONS.MINUS : BUTTONS.PLUS
    utils_sendEvent name:'pushed', value:button[0], type:'physical', isStateChange:true, descriptionText:"Button ${button[0]} (${button[1]}) was pushed"
    return

// Plus/Minus button was held
case { contains it, [clusterInt:0x0008, commandInt:0x01] }:
case { contains it, [clusterInt:0x0008, commandInt:0x05] }:
    List<String> button = msg.commandInt == 0x01 ? BUTTONS.MINUS : BUTTONS.PLUS
    utils_sendEvent name:'held', value:button[0], type:'physical', isStateChange:true, descriptionText:"Button ${button[0]} (${button[1]}) was held"
    return

// Plus/Minus button was released
case { contains it, [clusterInt:0x0008, commandInt:0x03] }:
case { contains it, [clusterInt:0x0008, commandInt:0x07] }:
    List<String> button = msg.commandInt == 0x03 ? BUTTONS.MINUS : BUTTONS.PLUS
    utils_sendEvent name:'released', value:button[0], type:'physical', isStateChange:true, descriptionText:"Button ${button[0]} (${button[1]}) was released"
    return
{{/ @events }}
{{!--------------------------------------------------------------------------}}
