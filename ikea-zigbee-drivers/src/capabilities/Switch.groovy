{{!--------------------------------------------------------------------------}}
{{# @definition }}
capability "Switch"
{{/ @definition }}
{{!--------------------------------------------------------------------------}}
{{# @inputs }}
{{# params.powerOnBehavior }}

// Inputs for capability.Switch
input(
    name: "powerOnBehavior",
    type: "enum",
    title: "Power On behaviour",
    description: "<small>Select what happens after a power outage.</small>",
    options: [
        "TURN_POWER_ON": "Turn power On",
        "TURN_POWER_OFF": "Turn power Off",
        "RESTORE_PREVIOUS_STATE": "Restore previous state"
    ],
    defaultValue: "RESTORE_PREVIOUS_STATE",
    required: true
)
{{/ params.powerOnBehavior }}
{{/ @inputs }}
{{!--------------------------------------------------------------------------}}
{{# @commands }}

// Commands for capability.Switch
command "toggle"
{{# params.onWithTimedOff }}
command "onWithTimedOff", [[name:"On time*", type:"NUMBER", description:"After how many seconds power will be turned Off [1..6500]"]]
{{/ params.onWithTimedOff }}
{{/ @commands }}
{{!--------------------------------------------------------------------------}}
{{# @implementation }}

// Implementation for capability.Switch
def on() {
    Log.debug "Sending On command"
    Utils.sendZigbeeCommands(["he raw 0x${device.deviceNetworkId} 0x01 0x01 0x0006 {114301}"])
}
def off() {
    Log.debug "Sending Off command"
    Utils.sendZigbeeCommands(["he raw 0x${device.deviceNetworkId} 0x01 0x01 0x0006 {114300}"])
}

def toggle() {
    Log.debug "Sending Toggle command"
    Utils.sendZigbeeCommands(["he raw 0x${device.deviceNetworkId} 0x01 0x01 0x0006 {114302}"])
}
{{# params.onWithTimedOff }}

def onWithTimedOff(onTime = 1) {
    Integer delay = onTime < 1 ? 1 : (onTime > 6500 ? 6500 : onTime)
    Log.debug "Sending OnWithTimedOff command"

    String payload = "00 ${zigbee.swapOctets(zigbee.convertToHexString(delay * 10, 4))} 0000"
    Utils.sendZigbeeCommands(["he raw 0x${device.deviceNetworkId} 0x01 0x01 0x0006 {114342 ${payload}}"])
}
{{/ params.onWithTimedOff }}
{{/ @implementation }}
{{!--------------------------------------------------------------------------}}
{{# @updated }}
{{# params.powerOnBehavior }}

// Preferences for capability.Switch
if (powerOnBehavior == null) {
    powerOnBehavior = "RESTORE_PREVIOUS_STATE"
    device.updateSetting("powerOnBehavior", [value:powerOnBehavior, type:"enum"])
}
Log.info "🛠️ powerOnBehavior = ${powerOnBehavior}"
cmds += zigbee.writeAttribute(0x0006, 0x4003, 0x30, powerOnBehavior == "TURN_POWER_OFF" ? 0x00 : (powerOnBehavior == "TURN_POWER_ON" ? 0x01 : 0xFF))
{{/ params.powerOnBehavior }}
{{/ @updated }}
{{!--------------------------------------------------------------------------}}
{{# @configure }}

// Configuration for capability.Switch
sendEvent name:"switch", value:"on", type:"digital", descriptionText:"Switch initialized to on"
cmds += "zdo bind 0x${device.deviceNetworkId} 0x01 0x01 0x0006 {${device.zigbeeId}} {}" // On/Off cluster
cmds += "he cr 0x${device.deviceNetworkId} 0x01 0x0006 0x0000 0x10 0x0000 0x0258 {01} {}" // Report OnOff (bool) at least every 10 minutes
cmds += zigbee.readAttribute(0x0006, 0x0000) // OnOff
{{/ @configure }}
{{!--------------------------------------------------------------------------}}
{{# @events }}

// Events for capability.Switch

// Report/Read Attributes: OnOff
case { contains it, [clusterInt:0x0006, commandInt:0x0A, attrInt:0x0000] }:
case { contains it, [clusterInt:0x0006, commandInt:0x01, attrInt:0x0000] }:
    String newState = msg.value == "00" ? "off" : "on"
    Utils.sendEvent name:"switch", value:newState, descriptionText:"Was turned ${newState}", type:type

    {{# params.callback}}
    // Execute the configured callback: {{ params.callback }}
    if (device.currentValue("switch", true) != newState) {
        {{ params.callback }}(newState)
    }
    {{/ params.callback}}
    return Utils.processedZclMessage("${msg.commandInt == 0x0A ? "Report" : "Read"} Attributes Response", "OnOff=${newState}")
{{# params.powerOnBehavior }}

// Read Attributes Response: powerOnBehavior
case { contains it, [clusterInt:0x0006, commandInt:0x01, attrInt:0x4003] }:
    String newValue = ""
    switch (Integer.parseInt(msg.value, 16)) {
        case 0x00: newValue = "TURN_POWER_OFF"; break
        case 0x01: newValue = "TURN_POWER_ON"; break
        case 0xFF: newValue = "RESTORE_PREVIOUS_STATE"; break
        default: return Log.warn("Received attribute value: powerOnBehavior=${msg.value}")
    }
    powerOnBehavior = newValue
    device.updateSetting("powerOnBehavior",[ value:newValue, type:"enum" ])

    return Utils.processedZclMessage("Read Attributes Response", "PowerOnBehavior=${newValue}")
{{/ params.powerOnBehavior }}

// Other events that we expect but are not usefull for capability.Switch behavior
case { contains it, [clusterInt:0x0006, commandInt:0x04] }: // Write Attribute Response (0x04)
case { contains it, [clusterInt:0x0006, commandInt:0x07] }: // Configure Reporting Response
    return
{{/ @events }}
{{!--------------------------------------------------------------------------}}
