{{!--------------------------------------------------------------------------}}
{{# @fields }}

// Fields for capability.RDM001_SwitchStyle
@Field static final Map<Integer, String> RDM001_SWITCH_STYLE = [
    "00": "Single Rocker",
    "01": "Single Push Button",
    "02": "Dual Rocker",
    "03": "Dual Push Button"
]
{{/ @fields }}
{{!--------------------------------------------------------------------------}}
{{# @inputs }}

// Inputs for capability.RDM001_SwitchStyle
input(
    name: "switchStyle",
    type: "enum",
    title: "Switch Style",
    description: "<small>Configure the button configuration</small>",
    options: RDM001_SWITCH_STYLE,
    defaultValue: "02",
    required: true
)
{{/ @inputs }}
{{!--------------------------------------------------------------------------}}
{{# @updated }}

// Preferences for capability.RDM001_SwitchStyle
if (switchStyle == null) switchStyle = "02"
Log.info "🛠️ switchStyle = ${switchStyle} (${RDM001_SWITCH_STYLE[switchStyle]})"
Utils.sendZigbeeCommands(["he raw ${device.deviceNetworkId} 0x01 0x01 0x0000 {040B104302 3400 30 ${switchStyle}}"])
Integer numberOfButtons = (switchStyle == "00" || switchStyle == "01") ? 1 : 2
sendEvent name:"numberOfButtons", value:numberOfButtons, descriptionText:"Number of buttons is ${numberOfButtons}"
Log.info "🛠️ numberOfButtons = ${numberOfButtons}"

{{/ @updated }}
{{!--------------------------------------------------------------------------}}
{{# @events }}

// Events for capability.RDM001_SwitchStyle (Write Attributes Response)
case { contains it, [endpointInt:0x01, clusterInt:0x0000, commandInt:0x04, isClusterSpecific:false, isManufacturerSpecific:true, manufacturerId:"100B"] }:
    return Log.info("Switch Style successfully configured!")

// Events for capability.RDM001_SwitchStyle (Read Attributes Response)
case { contains it, [endpointInt:0x01, clusterInt:0x0000, commandInt:0x01, attrInt:0x0034] }:
case { contains it, [endpointInt:0x01, clusterInt:0x0000, commandInt:0x0A, attrInt:0x0034] }:
    device.clearSetting "switchStyle"
    device.removeSetting "vswitchStyle"
    device.updateSetting "switchStyle", msg.value

    Integer numberOfButtons = msg.value == "01" || msg.value == "02" ? 1 : 2
    sendEvent name:"numberOfButtons", value:numberOfButtons, descriptionText:"Number of buttons is ${numberOfButtons}"
    Log.info "🛠️ numberOfButtons = ${numberOfButtons}"
    return
{{/ @events }}
{{!--------------------------------------------------------------------------}}
