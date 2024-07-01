{{!--------------------------------------------------------------------------}}
{{# @events }}

// Events for devices.NodOn_SIN-4-1-20
// ===================================================================================================================

// Switch was pressed - OnOff cluster Toggle
case { contains it, [clusterInt:0x0006, commandInt:0x02] }:
    List<String> button = msg.endpointInt == 1 ? BUTTONS.SWITCH_1 : BUTTONS.SWITCH_2
    utils_sendEvent name:'pushed', value:button[0], type:'physical', isStateChange:true, descriptionText:"Button ${button[0]} (${button[1]}) was pushed"
    return

// Other events that we expect but are not usefull
case { contains it, [clusterInt:0x0021] }: // ??
case { contains it, [clusterInt:0x1000] }: // ??
    return
{{/ @events }}
{{!--------------------------------------------------------------------------}}
