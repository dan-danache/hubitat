{{!--------------------------------------------------------------------------}}
{{# @fields }}

// Fields for devices.RDM001
@Field static final Map<Integer, String> RDM001_SWITCH_STYLE = [
    "00": "Single Rocker",
    "01": "Single Push Button",
    "02": "Dual Rocker",
    "03": "Dual Push Button"
]
{{/ @fields }}
{{!--------------------------------------------------------------------------}}
{{# @inputs }}

// Inputs for devices.RDM001
input(
    name: "switchStyle",
    type: "enum",
    title: "Switch Style",
    description: "<small>Select physical switch button configuration</small>",
    options: RDM001_SWITCH_STYLE,
    defaultValue: "02",
    required: true
)
{{/ @inputs }}
{{!--------------------------------------------------------------------------}}
{{# @configure }}

// Configuration for devices.RDM001
cmds += "he cr 0x${device.deviceNetworkId} 0x01 0x0001 0x0021 0x20 0x0000 0x4650 {01} {}" // Report battery at least every 5 hours
cmds += "zdo bind 0x${device.deviceNetworkId} 0x01 0x01 0x0001 {${device.zigbeeId}} {}" // Power Configuration cluster
cmds += zigbee.readAttribute(0x0001, 0x0021)  // BatteryPercentage

cmds += "he cr 0x${device.deviceNetworkId} 0x01 0x0000 0x0034 0x30 0x0000 0x0000 {} {0x100B}" // Report switch style whenever it changes
cmds += "zdo bind 0x${device.deviceNetworkId} 0x01 0x01 0x0000 {${device.zigbeeId}} {}" // Basic cluster

cmds += zigbee.writeAttribute(0x0000, 0x0031, 0x19, 0x0B00, [mfgCode: "0x100B"]) // Write Philips magic attribute
{{/ @configure }}
{{!--------------------------------------------------------------------------}}
{{# @updated }}

// Preferences for devices.RDM001
if (switchStyle == null) {
    switchStyle = "02"
    device.updateSetting("switchStyle", [value:switchStyle, type:"enum"])
}
Log.info "🛠️ switchStyle = ${switchStyle} (${RDM001_SWITCH_STYLE[switchStyle]})"
Utils.sendZigbeeCommands(["he raw ${device.deviceNetworkId} 0x01 0x01 0x0000 {040B104302 3400 30 ${switchStyle}}"])

Integer numberOfButtons = (switchStyle == "00" || switchStyle == "01") ? 1 : 2
sendEvent name:"numberOfButtons", value:numberOfButtons, descriptionText:"Number of buttons is ${numberOfButtons}"
Log.info "🛠️ numberOfButtons = ${numberOfButtons}"
{{/ @updated }}
{{!--------------------------------------------------------------------------}}
{{# @events }}

// Events for devices.RDM001 (Write Attributes Response)
case { contains it, [endpointInt:0x01, clusterInt:0x0000, commandInt:0x04, isClusterSpecific:false, isManufacturerSpecific:true, manufacturerId:"100B"] }:
    return Log.info("Switch Style successfully configured!")

// Events for devices.RDM001 (Read Attributes Response)
case { contains it, [endpointInt:0x01, clusterInt:0x0000, commandInt:0x01, attrInt:0x0034] }:
case { contains it, [endpointInt:0x01, clusterInt:0x0000, commandInt:0x0A, attrInt:0x0034] }:
    device.clearSetting "switchStyle"
    device.removeSetting "vswitchStyle"
    device.updateSetting "switchStyle", msg.value

    Integer numberOfButtons = msg.value == "01" || msg.value == "02" ? 1 : 2
    sendEvent name:"numberOfButtons", value:numberOfButtons, descriptionText:"Number of buttons is ${numberOfButtons}"
    Log.info "🛠️ numberOfButtons = ${numberOfButtons}"
    return

// Other events that we expect but are not usefull for devices.RDM001 behavior
case { contains it, [clusterInt:0x0000, commandInt:0x07] }:  // ConfigureReportingResponse
    return
{{/ @events }}
{{!--------------------------------------------------------------------------}}
