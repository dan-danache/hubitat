{{!--------------------------------------------------------------------------}}
{{# @events }}

// Events for devices.NodOn_SIN-4-RS-20
// ===================================================================================================================

// Switch was pressed - OnOff cluster Toggle
case { contains it, [clusterInt:0x0102, commandInt:0x00] }:
case { contains it, [clusterInt:0x0102, commandInt:0x01] }:
    List<String> button = msg.commandInt == 0x00 ? BUTTONS.UP : BUTTONS.DOWN
    utils_sendEvent name:'pushed', value:button[0], type:'physical', isStateChange:true, descriptionText:"Button ${button[0]} (${button[1]}) was pushed"
    return

// Other events that we expect but are not usefull
case { contains it, [clusterInt:0x0102, commandInt:0x02] }:
    return
{{/ @events }}
{{!--------------------------------------------------------------------------}}
