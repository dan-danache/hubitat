{{!--------------------------------------------------------------------------}}
{{# @configure }}

// Configuration for devices.Ikea_E2213
cmds += "zdo bind 0x${device.deviceNetworkId} 0x01 0x01 0xFC80 {${device.zigbeeId}} {}" // IKEA Button cluster (ep 01)
cmds += "zdo bind 0x${device.deviceNetworkId} 0x02 0x01 0xFC80 {${device.zigbeeId}} {}" // IKEA Button cluster (ep 02)
{{/ @configure }}
{{!--------------------------------------------------------------------------}}
{{# @events }}

// Events for devices.Ikea_E2213
// ===================================================================================================================

// IGNORED: We don't know yet if this will be a push, double-tap or hold
case { contains it, [clusterInt:0xFC80, commandInt:0x01] }:
    List<String> button = msg.endpointInt == 0x01 ? BUTTONS.DOT_1 : BUTTONS.DOT_2
    log_debug "Button ${button[0]} (${button[1]}) was pressed-down (ignored as we don't know yet if this will be a push, double-tap or hold)"
    return

// Button was held
case { contains it, [clusterInt:0xFC80, commandInt:0x02] }:
    List<String> button = msg.endpointInt == 0x01 ? BUTTONS.DOT_1 : BUTTONS.DOT_2
    utils_sendEvent name:'held', value:button[0], type:'physical', isStateChange:true, descriptionText:"Button ${button[0]} (${button[1]}) was held"
    return

// Button was pushed
case { contains it, [clusterInt:0xFC80, commandInt:0x03] }:
    List<String> button = msg.endpointInt == 0x01 ? BUTTONS.DOT_1 : BUTTONS.DOT_2
    utils_sendEvent name:'pushed', value:button[0], type:'physical', isStateChange:true, descriptionText:"Button ${button[0]} (${button[1]}) was pushed"
    return

// Button was released
case { contains it, [clusterInt:0xFC80, commandInt:0x04] }:
    List<String> button = msg.endpointInt == 0x01 ? BUTTONS.DOT_1 : BUTTONS.DOT_2
    utils_sendEvent name:'released', value:button[0], type:'physical', isStateChange:true, descriptionText:"Button ${button[0]} (${button[1]}) was released"
    return

// Button was double tapped
case { contains it, [clusterInt:0xFC80, commandInt:0x06] }:
    List<String> button = msg.endpointInt == 0x01 ? BUTTONS.DOT_1 : BUTTONS.DOT_2
    utils_sendEvent name:'doubleTapped', value:button[0], type:'physical', isStateChange:true, descriptionText:"Button ${button[0]} (${button[1]}) was double tapped"
    return
{{/ @events }}
{{!--------------------------------------------------------------------------}}
