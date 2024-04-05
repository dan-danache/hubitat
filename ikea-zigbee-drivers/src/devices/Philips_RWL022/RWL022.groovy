{{!--------------------------------------------------------------------------}}
{{# @configure }}

// Configuration for devices.Philips_RWL022
cmds += "zdo bind 0x${device.deviceNetworkId} 0x${device.endpointId} 0x01 0xFC00 {${device.zigbeeId}} {}" // Hue Specific cluster

cmds += "he raw 0x${device.deviceNetworkId} 0x00 0x00 0x0022 {49 ${utils_payload "${device.zigbeeId}"} ${utils_payload '0x01'} ${utils_payload '0x0005'} 03 ${utils_payload "${location.hub.zigbeeEui}"} 01} {0x0000}" // Unbind Scenes cluster
cmds += "he raw 0x${device.deviceNetworkId} 0x00 0x00 0x0022 {49 ${utils_payload "${device.zigbeeId}"} ${utils_payload '0x01'} ${utils_payload '0x0006'} 03 ${utils_payload "${location.hub.zigbeeEui}"} 01} {0x0000}" // Unbind On/Off cluster
cmds += "he raw 0x${device.deviceNetworkId} 0x00 0x00 0x0022 {49 ${utils_payload "${device.zigbeeId}"} ${utils_payload '0x01'} ${utils_payload '0x0008'} 03 ${utils_payload "${location.hub.zigbeeEui}"} 01} {0x0000}" // Unbind Level Control cluster

cmds += zigbee.writeAttribute(0x0000, 0x0031, 0x19, 0x0B00, [mfgCode: '0x100B']) // Write Philips magic attribute
{{/ @configure }}
{{!--------------------------------------------------------------------------}}
{{# @events }}

// Events for devices.Philips_RWL022
// ===================================================================================================================

// Button was pressed := { 16:Button, 08:EventType, 08:NextValueType, 08:Action, 08:NextValueType, 16:DurationRotation}
// EventType := { 0x00:Button, 0x01:Rotary }
// Action := { 0x00:Press, 0x01:Hold/Start, 0x02:Release/Repeat, 0x03:LongRelease }
// [02, 00,  00,  30,  02,  21,  01, 00] -> Button=2(0x0002), EventType=Button(0x00), NextValueType=enum8(0x30), Action=Release(0x02), NextValueType=uint16(0x21), DurationRotation=0x0001
case { contains it, [clusterInt:0xFC00, commandInt:0x00] }:
    List<String> button = BUTTONS[msg.data[0]]

    // Dimmer Mode: Only listen to Release (02), Hold (01) and LongRelease (03)
    switch (msg.data[4]) {
        case '02': utils_sendEvent name:'pushed', value:button[0], type:'physical', isStateChange:true, descriptionText:"Button ${button[0]} (${button[1]}) was pushed"; return
        case '01': utils_sendEvent name:'held', value:button[0], type:'physical', isStateChange:true, descriptionText:"Button ${button[0]} (${button[1]}) was held"; return
        case '03': utils_sendEvent name:'released', value:button[0], type:'physical', isStateChange:true, descriptionText:"Button ${button[0]} (${button[1]}) was released"; return
    }
    return

// Other events that we expect but are not usefull
case { contains it, [clusterInt:0x0000, commandInt:0x04, isClusterSpecific:false] }:
    utils_processedZclMessage 'Write Attribute Response', 'attribute=Philips magic attribute'
    return
{{/ @events }}
{{!--------------------------------------------------------------------------}}
