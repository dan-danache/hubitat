{{!--------------------------------------------------------------------------}}
{{# @definition }}
capability "ChangeLevel"
capability "SwitchLevel"
{{/ @definition }}
{{!--------------------------------------------------------------------------}}
{{# @inputs }}

// Inputs for capability.Brightness
input(
    name: "levelStep",
    type: "enum",
    title: "Brightness up/down step",
    description: "<small>Level adjust when using the levelUp/levelDown commands.</small>",
    options: [
         "1": "1%",
         "2": "2%",
         "5": "5%",
        "10": "10%",
        "20": "20%",
        "25": "25%",
        "33": "33%"
    ],
    defaultValue: "20",
    required: true
)
input(
    name: "startLevelChangeRate",
    type: "enum",
    title: "Brightness change rate",
    description: "<small>The rate of brightness change when using the startLevelChange() command.</small>",
    options: [
         "10": "10% / second : from 0% to 100% in 10 seconds",
         "20": "20% / second : from 0% to 100% in 5 seconds",
         "33": "33% / second : from 0% to 100% in 3 seconds",
         "50": "50% / seconds : from 0% to 100% in 2 seconds",
        "100": "100% / second : from 0% to 100% in 1 seconds",
    ],
    defaultValue: "20",
    required: true
)
input(
    name: "turnOnBehavior",
    type: "enum",
    title: "Turn On behavior",
    description: "<small>Select what happens when the device is turned On.</small>",
    options: [
        "RESTORE_PREVIOUS_LEVEL": "Restore previous brightness",
        "FIXED_VALUE": "Always start with the same fixed brightness"
    ],
    defaultValue: "RESTORE_PREVIOUS_LEVEL",
    required: true
)
if (turnOnBehavior == "FIXED_VALUE") {
    input(
        name: "onLevelValue",
        type: "number",
        title: "Fixed brightness value",
        description: "<small>Range 1~100</small>",
        defaultValue: 50,
        range: "1..100",
        required: true
    )
}
input(
    name: "transitionTime",
    type: "enum",
    title: "On/Off transition time",
    description: "<small>Time taken to move to/from the target brightness when device is turned On/Off.</small>",
    options: [
         "0": "Instant",
         "5": "0.5 seconds",
        "10": "1 second",
        "15": "1.5 seconds",
        "20": "2 seconds",
        "30": "3 seconds",
        "40": "4 seconds",
        "50": "5 seconds",
       "100": "10 seconds"
    ],
    defaultValue: "5",
    required: true
)
input(
    name: "prestaging",
    type: "bool",
    title: "Pre-staging",
    description: "<small>Set the brightness level without turning On the device (for later use).</small>",
    defaultValue: false,
    required: true
)
{{/ @inputs }}
{{!--------------------------------------------------------------------------}}
{{# @commands }}

// Commands for capability.Brightness
command "levelUp"
command "levelDown"
{{/ @commands }}
{{!--------------------------------------------------------------------------}}
{{# @implementation }}

// Implementation for capability.Brightness
def startLevelChange(direction) {
    Log.debug "Starting brightness change ${direction}wards with a rate of ${startLevelChangeRate}% / second"

    Integer mode = direction == "up" ? 0x00 : 0x01
    Integer rate = Integer.parseInt(startLevelChangeRate) * 2.54

    String payload = "${zigbee.convertToHexString(mode, 2)} ${zigbee.convertToHexString(rate, 2)}"
    Utils.sendZigbeeCommands(["he raw 0x${device.deviceNetworkId} 0x01 0x01 0x0008 {114301 ${payload}}"])
}
def stopLevelChange() {
    Log.debug "Stopping brightness change"
    Utils.sendZigbeeCommands(["he raw 0x${device.deviceNetworkId} 0x01 0x01 0x0008 {114303}"])
}
def levelUp() {
    Log.debug "Moving brightness up by ${levelStep}%"

    Integer stepSize = Integer.parseInt(levelStep) * 2.54
    Integer dur = 0

    String payload = "${zigbee.convertToHexString(0x00, 2)} ${zigbee.convertToHexString(stepSize, 2)} ${zigbee.swapOctets(zigbee.convertToHexString(dur, 4))}"
    Utils.sendZigbeeCommands(["he raw 0x${device.deviceNetworkId} 0x01 0x01 0x0008 {114302 ${payload}}"])
}
def levelDown() {
    Log.debug "Moving brightness down by ${levelStep}%"

    Integer stepSize = Integer.parseInt(levelStep) * 2.54
    Integer dur = 0

    String payload = "${zigbee.convertToHexString(0x01, 2)} ${zigbee.convertToHexString(stepSize, 2)} ${zigbee.swapOctets(zigbee.convertToHexString(dur, 4))}"
    Utils.sendZigbeeCommands(["he raw 0x${device.deviceNetworkId} 0x01 0x01 0x0008 {114302 ${payload}}"])
}
def setLevel(level, duration = 0) {
    Integer newLevel = level > 100 ? 100 : (level < 0 ? 0 : level)
    Log.debug "Setting brightness to ${newLevel}% during ${duration} seconds"

    // Device is On: use the Move To Level command
    if (device.currentValue("switch", true) == "on" || prestaging == false) {
        Integer lvl = newLevel * 2.54
        Integer dur = (duration > 1800 ? 1800 : (duration < 0 ? 0 : duration)) * 10         // Max transition time = 30 min

        String command = prestaging == false ? "04" : "00"
        String payload = "${zigbee.convertToHexString(lvl, 2)} ${zigbee.swapOctets(zigbee.convertToHexString(dur, 4))}"
        return Utils.sendZigbeeCommands(["he raw 0x${device.deviceNetworkId} 0x01 0x01 0x0008 {1143${command} ${payload}}"])
    }

    // Device is Off and onLevel is set to a fixed value: ignore command
    if (turnOnBehavior == "FIXED_VALUE") {
        return Log.info("Ignoring Set Level command because the device is turned Off and \"Turn On behavior\" preference is set to \"Always start with the same fixed brightness\"")
    }

    // Device is Off: keep the device turned Off, use the OnLevel attribute
    Log.debug("Device is turned Off so we pre-stage brightness level for when the device will be turned On")
    setOnLevel(newLevel)
    Utils.sendEvent(name:"level", value:newLevel, descriptionText:"Brightness is ${newLevel}%", type:"digital", isStateChange:true)
}
def setOnLevel(level) {
    Integer newLevel = level > 100 ? 100 : (level < 0 ? 0 : level)
    Log.debug "Setting Turn On brightness to ${newLevel}%"
    Integer lvl = newLevel * 2.54
    Utils.sendZigbeeCommands zigbee.writeAttribute(0x0008, 0x0011, 0x20, lvl)
}
private turnOnCallback(switchState) {
    // Device was just turned on: Read the value of the OnLevel attribute to sync/update its value
    if (switchState == "on") Utils.sendZigbeeCommands zigbee.readAttribute(0x0008, 0x0011)
}
{{/ @implementation }}
{{!--------------------------------------------------------------------------}}
{{# @updated }}

// Preferences for capability.Brightness
if (levelStep == null) {
    levelStep = "20"
    device.updateSetting("levelStep", [value:levelStep, type:"enum"])
}
Log.info "🛠️ levelStep = ${levelStep}%"

if (startLevelChangeRate == null) {
    startLevelChangeRate = "20"
    device.updateSetting("startLevelChangeRate", [value:startLevelChangeRate, type:"enum"])
}
Log.info "🛠️ startLevelChangeRate = ${startLevelChangeRate}% / second"

if (turnOnBehavior == null) {
    turnOnBehavior = "RESTORE_PREVIOUS_LEVEL"
    device.updateSetting("turnOnBehavior", [value:turnOnBehavior, type:"enum"])
}
Log.info "🛠️ turnOnBehavior = ${turnOnBehavior}"
if (turnOnBehavior == "FIXED_VALUE") {
    Integer lvl = onLevelValue == null ? 50 : onLevelValue.intValue()
    device.updateSetting("onLevelValue",[ value:lvl, type:"number" ])
    Log.info "🛠️ onLevelValue = ${lvl}%"
    setOnLevel(lvl)
} else {
    Log.debug "Disabling OnLevel (0xFF)"
    cmds += zigbee.writeAttribute(0x0008, 0x0011, 0x20, 0xFF)
}

if (transitionTime == null) {
    transitionTime = "5"
    device.updateSetting("transitionTime", [value:transitionTime, type:"enum"])
}
Log.info "🛠️ transitionTime = ${Integer.parseInt(transitionTime) / 10} second(s)"
cmds += zigbee.writeAttribute(0x0008, 0x0010, 0x21, Integer.parseInt(transitionTime))

if (prestaging == null) {
    prestaging = false
    device.updateSetting("prestaging", [value:prestaging, type:"bool"])
}
Log.info "🛠️ prestaging = ${prestaging}"
{{/ @updated }}
{{!--------------------------------------------------------------------------}}
{{# @configure }}

// Configuration for capability.Brightness
sendEvent name:"level", value:"100", type:"digital", descriptionText:"Brightness initialized to 100%"
cmds += "he cr 0x${device.deviceNetworkId} 0x01 0x0008 0x0000 0x20 0x0000 0x0258 {01} {}" // Report CurrentLevel at least every 10 minutes
cmds += "zdo bind 0x${device.deviceNetworkId} 0x01 0x01 0x0008 {${device.zigbeeId}} {}" // Level Control cluster
cmds += zigbee.readAttribute(0x0008, 0x0000) // CurrentLevel
{{/ @configure }}
{{!--------------------------------------------------------------------------}}
{{# @events }}

// Events for capability.Brightness

// Report Attributes: CurrentLevel
// Read Attributes Reponse: CurrentLevel
case { contains it, [clusterInt:0x0008, commandInt:0x0A, attrInt:0x0000] }:
case { contains it, [clusterInt:0x0008, commandInt:0x01, attrInt:0x0000] }:
    Utils.processedZclMessage("Report/Read Attributes Response", "CurrentLevel=${msg.value}")

    Integer newLevel = msg.value == "00" ? 0 : Math.ceil(Integer.parseInt(msg.value, 16) * 100 / 254)
    if (device.currentValue("level", true) != newLevel) {
        Utils.sendEvent name:"level", value:newLevel, descriptionText:"Brightness is ${newLevel}%", type:"digital"
    }
    return

// Read Attributes Reponse: OnLevel
// This value is read immediately after the device is turned On
// @see turnOnCallback()
case { contains it, [clusterInt:0x0008, commandInt:0x01, attrInt:0x0011] }:
    Utils.processedZclMessage("Read Attributes Response", "OnLevel=${msg.value}")
    Integer onLevel = msg.value == "00" ? 0 : Integer.parseInt(msg.value, 16) * 100 / 254

    // Clear OnLevel attribute value (if previously set)
    if (turnOnBehavior != "FIXED_VALUE" && msg.value != "FF") {
        setLevel(device.currentValue("level", true))
        Log.debug "Disabling OnLevel (0xFF)"
        return Utils.sendZigbeeCommands(zigbee.writeAttribute(0x0008, 0x0011, 0x20, 0xFF))
    }

    // Set current level to OnLevel
    if (turnOnBehavior == "FIXED_VALUE") {
        setLevel(onLevel)
    }
    return

// Other events that we expect but are not usefull for capability.Brightness behavior
case { contains it, [clusterInt:0x0008, commandInt:0x04] }:  // Write Attribute Response (0x04)
case { contains it, [clusterInt:0x0008, commandInt:0x07] }:  // Configure Reporting Response
    return
{{/ @events }}
{{!--------------------------------------------------------------------------}}
