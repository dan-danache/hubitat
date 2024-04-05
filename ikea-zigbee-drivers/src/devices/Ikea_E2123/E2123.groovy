{{!--------------------------------------------------------------------------}}
{{# @configure }}

// Configuration for devices.Ikea_E2123
cmds += "zdo bind 0x${device.deviceNetworkId} 0x${device.endpointId} 0x01 0x0006 {${device.zigbeeId}} {}" // On/Off cluster
cmds += "zdo bind 0x${device.deviceNetworkId} 0x${device.endpointId} 0x01 0x0008 {${device.zigbeeId}} {}" // Level Control cluster
cmds += "zdo bind 0x${device.deviceNetworkId} 0x${device.endpointId} 0x01 0xFC7F {${device.zigbeeId}} {}" // Unknown 64639 cluster --> For firmware 1.0.012
cmds += "zdo bind 0x${device.deviceNetworkId} 0x${device.endpointId} 0x01 0xFC80 {${device.zigbeeId}} {}" // Heiman - Specific Scenes cluster --> For firmware 1.0.35
cmds += "zdo bind 0x${device.deviceNetworkId} 0x${device.endpointId} 0x01 0xFC80 {${device.zigbeeId}} {}" // Heiman - Specific Scenes cluster --> For firmware 1.0.35
{{/ @configure }}
{{!--------------------------------------------------------------------------}}
{{# @events }}

// Events for devices.Ikea_E2123
// ===================================================================================================================

// Play button was pushed
case { contains it, [clusterInt:0x0006, commandInt:0x02] }:
    List<String> button = BUTTONS.PLAY
    utils_sendEvent name:'pushed', value:button[0], type:'physical', isStateChange:true, descriptionText:"Button ${button[0]} (${button[1]}) was pushed"
    return

// Plus/Minus button was held
case { contains it, [clusterInt:0x0008, commandInt:0x01] }:
    List<String> button = msg.data[0] == '00' ? BUTTONS.PLUS : BUTTONS.MINUS
    utils_sendEvent name:'held', value:button[0], type:'physical', isStateChange:true, descriptionText:"Button ${button[0]} (${button[1]}) was held"
    return

// Next/Prev button was pushed
case { contains it, [clusterInt:0x0008, commandInt:0x02] }:
    List<String> button = msg.data[0] == '00' ? BUTTONS.NEXT : BUTTONS.PREV
    utils_sendEvent name:'pushed', value:button[0], type:'physical', isStateChange:true, descriptionText:"Button ${button[0]} (${button[1]}) was pushed"
    return

// Plus/Minus button was pushed
case { contains it, [clusterInt:0x0008, commandInt:0x05] }:
    List<String> button = msg.data[0] == '00' ? BUTTONS.PLUS : BUTTONS.MINUS
    utils_sendEvent name:'pushed', value:button[0], type:'physical', isStateChange:true, descriptionText:"Button ${button[0]} (${button[1]}) was pushed"
    return

// Undocumented cluster (0xFC7F) - Used by firmware 1.0.012 (20211214)
case { contains it, [clusterInt:0xFC7F] }:
    List<String> button = msg.data[0] == '01' ? BUTTONS.DOT_1 : BUTTONS.DOT_2

    // 1 Dot / 2 Dots button was pushed
    if (msg.data[1] == '01') {
        utils_sendEvent name:'pushed', value:button[0], type:'physical', isStateChange:true, descriptionText:"Button ${button[0]} (${button[1]}) was pressed"
        return
    }

    // 1 Dot / 2 Dots button was double tapped
    if (msg.data[1] == '02') {
        utils_sendEvent name:'doubleTapped', value:button[0], type:'physical', isStateChange:true, descriptionText:"Button ${button[0]} (${button[1]}) was double tapped"
        return
    }

    // 1 Dot / 2 Dots button was held
    if (msg.data[1] == '03') {
        utils_sendEvent name:'held', value:button[0], type:'physical', isStateChange:true, descriptionText:"Button ${button[0]} (${button[1]}) was held"
        return
    }

// Undocumented cluster (0xFC80) - Used by firmware 1.0.35 (20230411)
case { contains it, [clusterInt:0xFC80] }:
    List<String> button = msg.sourceEndpoint == '02' ? BUTTONS.DOT_1 : BUTTONS.DOT_2

    switch (msg.commandInt) {

        // IGNORED: 1 Dot / 2 Dots button was pressed-down
        case 0x01:
            log_debug "Button ${button[0]} (${button[1]}) was pressed-down (ignored as we wait for the next message to distinguish between click, double tap and hold)"
            return

        // 1 Dot / 2 Dots button was held
        // Commands are issued in this order: 01 (key-down = ignored) -> 02 (button is held = update "held" attribute) -> 04 (button released = update "released" attribute)
        case 0x02:
            utils_sendEvent name:'held', value:button[0], type:'physical', isStateChange:true, descriptionText:"Button ${button[0]} (${button[1]}) was held"
            return

        // 1 Dot / 2 Dots button was pushed
        case 0x03:
            utils_sendEvent name:'pushed', value:button[0], type:'physical', isStateChange:true, descriptionText:"Button ${button[0]} (${button[1]}) was pressed"
            return

        // IGNORED: 1 Dot / 2 Dots button was released
        case 0x04:
            utils_sendEvent name:'released', value:button[0], type:'physical', isStateChange:true, descriptionText:"Button ${button[0]} (${button[1]}) was released"
            return

        // 1 Dot / 2 Dots button was double tapped
        case 0x06:
            utils_sendEvent name:'doubleTapped', value:button[0], type:'physical', isStateChange:true, descriptionText:"Button ${button[0]} (${button[1]}) was double tapped"
            return
    }
{{/ @events }}
{{!--------------------------------------------------------------------------}}
