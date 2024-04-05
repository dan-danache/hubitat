{{!--------------------------------------------------------------------------}}
{{# @configure }}

// Configuration for devices.Swann_SWO-KEF1PA
cmds += "zdo bind 0x${device.deviceNetworkId} 0x${device.endpointId} 0x01 0x0501 {${device.zigbeeId}} {}" // IAS Ancillary Control Equipment cluster
{{/ @configure }}
{{!--------------------------------------------------------------------------}}
{{# @events }}

// Events for devices.Swann_SWO-KEF1PA
// ===================================================================================================================

// Arm := { 16:Button, 08:ArmMode, ??:ArmDisarmCode, 08:ZoneId}
// ArmMode := { 0x00:Disarm, 0x01:Arm Day/Home Zones Only, 0x02:Arm Night/Sleep Zones Only, 0x03:Arm All Zones }
// [00, 00, 00, 00, 00, 00, 00, 00, 00, 00] -> Home button
// [02, 00, 00, 00, 00, 00, 00, 00, 00, 00] -> Night button
// [03, 00, 00, 00, 00, 00, 00, 00, 00, 00] -> Away button
case { contains it, [clusterInt:0x0501, commandInt:0x00, isClusterSpecific:true] }:
    switch (msg.data[0]) {
        case '00':
            List<String> button = BUTTONS.HOME
            utils_sendEvent name:'pushed', value:button[0], type:'physical', isStateChange:true, descriptionText:"Button ${button[0]} (${button[1]}) was pushed"
            return

        case '02':
            List<String> button = BUTTONS.NIGHT
            utils_sendEvent name:'pushed', value:button[0], type:'physical', isStateChange:true, descriptionText:"Button ${button[0]} (${button[1]}) was pushed"
            return

        case '03':
            List<String> button = BUTTONS.AWAY
            utils_sendEvent name:'pushed', value:button[0], type:'physical', isStateChange:true, descriptionText:"Button ${button[0]} (${button[1]}) was pushed"
            return
    }

    log_error "Sent unexpected Zigbee message: description=${description}, msg=${msg}"
    return

// Panic
case { contains it, [clusterInt:0x0501, commandInt:0x04, isClusterSpecific:true] }:
    List<String> button = BUTTONS.PANIC
    utils_sendEvent name:'pushed', value:button[0], type:'physical', isStateChange:true, descriptionText:"Button ${button[0]} (${button[1]}) was pushed"
    return

// Read Attributes: ZoneStatus
case { contains it, [clusterInt:0x0500, commandInt:0x01, attrInt:0x0002] }:
    utils_processedZclMessage 'Read Attributes Response', "ZoneStatus=${msg.value}"
    return
{{/ @events }}
{{!--------------------------------------------------------------------------}}
