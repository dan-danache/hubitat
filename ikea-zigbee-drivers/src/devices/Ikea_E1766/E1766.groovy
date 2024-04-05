{{!--------------------------------------------------------------------------}}
{{# @configure }}

// Configuration for devices.Ikea_E1743
cmds += "zdo bind 0x${device.deviceNetworkId} 0x${device.endpointId} 0x01 0x0102 {${device.zigbeeId}} {}" // Window Covering cluster
{{/ @configure }}
{{!--------------------------------------------------------------------------}}
{{# @events }}

// Events for devices.Ikea_E1743
// ===================================================================================================================

// I/O button was pressed
case { contains it, [clusterInt:0x0102, commandInt:0x00] }:
case { contains it, [clusterInt:0x0102, commandInt:0x01] }:
    List<String> button = msg.commandInt == 0x00 ? BUTTONS.OPEN : BUTTONS.CLOSE
    utils_sendEvent name:'pushed', value:button[0], type:'physical', isStateChange:true, descriptionText:"Button ${button[0]} (${button[1]}) was pushed"
    return

// I/O button was released
case { contains it, [clusterInt:0x0102, commandInt:0x02] }:
    List<String> button = device.currentValue('pushed', true) == 1 ? BUTTONS.OPEN : BUTTONS.CLOSE
    utils_sendEvent name:'released', value:button[0], type:'physical', isStateChange:true, descriptionText:"Button ${button[0]} (${button[1]}) was released"
    return
{{/ @events }}
{{!--------------------------------------------------------------------------}}
