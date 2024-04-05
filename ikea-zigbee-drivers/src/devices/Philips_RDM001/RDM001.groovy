{{!--------------------------------------------------------------------------}}
{{# @fields }}

// Fields for devices.Philips_RDM001
@Field static final Map<Integer, String> RDM001_SWITCH_STYLE = [
    '00': 'Single Rocker',
    '01': 'Single Push Button',
    '02': 'Dual Rocker',
    '03': 'Dual Push Button'
]
{{/ @fields }}
{{!--------------------------------------------------------------------------}}
{{# @inputs }}

// Inputs for devices.Philips_RDM001
input(
    name: 'switchStyle', type: 'enum',
    title: 'Switch Style',
    description: '<small>Select physical switch button configuration</small>',
    options: RDM001_SWITCH_STYLE,
    defaultValue: '02',
    required: true
)
{{/ @inputs }}
{{!--------------------------------------------------------------------------}}
{{# @configure }}

// Configuration for devices.Philips_RDM001
cmds += "zdo bind 0x${device.deviceNetworkId} 0x01 0x01 0xFC00 {${device.zigbeeId}} {}" // Hue Specific cluster

cmds += "zdo bind 0x${device.deviceNetworkId} 0x01 0x01 0x0001 {${device.zigbeeId}} {}" // Power Configuration cluster
cmds += "he cr 0x${device.deviceNetworkId} 0x01 0x0001 0x0021 0x20 0x0000 0x4650 {01} {}" // Report battery at least every 5 hours

cmds += "zdo bind 0x${device.deviceNetworkId} 0x01 0x01 0x0000 {${device.zigbeeId}} {}" // Basic cluster
cmds += "he cr 0x${device.deviceNetworkId} 0x01 0x0000 0x0034 0x30 0x0000 0x0000 {} {0x100B}" // Report switch style whenever it changes

cmds += zigbee.writeAttribute(0x0000, 0x0031, 0x19, 0x0B00, [mfgCode: '0x100B']) // Write Philips magic attribute
{{/ @configure }}
{{!--------------------------------------------------------------------------}}
{{# @updated }}

// Preferences for devices.Philips_RDM001
if (switchStyle == null) {
    switchStyle = '02'
    device.updateSetting 'switchStyle', [value:switchStyle, type:'enum']
}
log_info "🛠️ switchStyle = ${switchStyle} (${RDM001_SWITCH_STYLE[switchStyle]})"
utils_sendZigbeeCommands(["he raw ${device.deviceNetworkId} 0x01 0x01 0x0000 {040B104302 3400 30 ${switchStyle}}"])

Integer numberOfButtons = (switchStyle == '00' || switchStyle == '01') ? 1 : 2
sendEvent name:'numberOfButtons', value:numberOfButtons, descriptionText:"Number of buttons is ${numberOfButtons}"
log_info "🛠️ numberOfButtons = ${numberOfButtons}"
{{/ @updated }}
{{!--------------------------------------------------------------------------}}
{{# @events }}

// Events for devices.Philips_RDM001
// ===================================================================================================================

// Button was pressed := { 16:Button, 08:EventType, 08:NextValueType, 08:Action, 08:NextValueType, 16:DurationRotation}
// EventType := { 0x00:Button, 0x01:Rotary }
// Action := { 0x00:Press, 0x01:Hold/Start, 0x02:Release/Repeat, 0x03:LongRelease }
// [02, 00,  00,  30,  02,  21,  01, 00] -> Button=2(0x0002), EventType=Button(0x00), NextValueType=enum8(0x30), Action=Release(0x02), NextValueType=uint16(0x21), DurationRotation=0x0001
case { contains it, [clusterInt:0xFC00, commandInt:0x00] }:
    List<String> button = msg.data[0] == '01' ? BUTTONS.BUTTON_1 : BUTTONS.BUTTON_2

    // Rocker Mode: Only listen to "Press" (00) actions
    if (switchStyle == '00' || switchStyle == '02') {
        if (msg.data[4] != '00') return
        utils_sendEvent name:'pushed', value:button[0], type:'physical', isStateChange:true, descriptionText:"Button ${button[0]} (${button[1]}) was pushed"
        return
    }

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

// Write Attributes Response
case { contains it, [endpointInt:0x01, clusterInt:0x0000, commandInt:0x04, isClusterSpecific:false, isManufacturerSpecific:true, manufacturerId:'100B'] }:
    log_info 'Switch Style successfully configured!'
    return

// Read/Report Attributes Response
case { contains it, [endpointInt:0x01, clusterInt:0x0000, commandInt:0x01, attrInt:0x0034] }:
case { contains it, [endpointInt:0x01, clusterInt:0x0000, commandInt:0x0A, attrInt:0x0034] }:
    device.clearSetting 'switchStyle'
    device.removeSetting 'switchStyle'
    device.updateSetting 'switchStyle', [value:msg.value, type:'enum']

    Integer numberOfButtons = msg.value == '01' || msg.value == '02' ? 1 : 2
    sendEvent name:'numberOfButtons', value:numberOfButtons, descriptionText:"Number of buttons is ${numberOfButtons}"
    log_info "🛠️ numberOfButtons = ${numberOfButtons}"
    return

// Other events that we expect but are not usefull
case { contains it, [clusterInt:0x0000, commandInt:0x07] }:  // ConfigureReportingResponse
    return
{{/ @events }}
{{!--------------------------------------------------------------------------}}
