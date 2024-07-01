{{!--------------------------------------------------------------------------}}
{{# @definition }}
capability 'Light'
capability 'ChangeLevel'
capability 'SwitchLevel'
{{/ @definition }}
{{!--------------------------------------------------------------------------}}
{{# @inputs }}

// Inputs for capability.Brightness
input(
    name: 'levelStep', type: 'enum',
    title: 'Brightness up/down shift',
    description: '<small>Brightness +/- adjust for the shiftLevel() command.</small>',
    options: ['1':'1%', '2':'2%', '5':'5%', '10':'10%', '20':'20%', '25':'25%', '33':'33%', '50':'50%'],
    defaultValue: '25',
    required: true
)
input(
    name: 'levelChangeRate', type: 'enum',
    title: 'Brightness change rate',
    description: '<small>Brightness +/- adjust for the startLevelChange() command.</small>',
    options: [
         '10': '10% / sec - from 0% to 100% in 10 seconds',
         '20': '20% / sec - from 0% to 100% in 5 seconds',
         '33': '33% / sec - from 0% to 100% in 3 seconds',
         '50': '50% / secs - from 0% to 100% in 2 seconds',
        '100': '100% / sec - from 0% to 100% in 1 seconds',
    ],
    defaultValue: '20',
    required: true
)
input(
    name: 'levelTransitionTime', type: 'enum',
    title: 'Brightness transition time',
    description: '<small>Time taken to move to/from the target brightness when device is turned On/Off.</small>',
    options: [
         '0': 'Instant',
         '5': '0.5 seconds',
        '10': '1 second',
        '15': '1.5 seconds',
        '20': '2 seconds',
        '30': '3 seconds',
        '40': '4 seconds',
        '50': '5 seconds',
       '100': '10 seconds'
    ],
    defaultValue: '5',
    required: true
)
input(
    name: 'turnOnBehavior', type: 'enum',
    title: 'Turn On behavior',
    description: '<small>Select what happens when the device is turned On.</small>',
    options: [
        'RESTORE_PREVIOUS_LEVEL': 'Restore previous brightness',
        'FIXED_VALUE': 'Always start with the same fixed brightness'
    ],
    defaultValue: 'RESTORE_PREVIOUS_LEVEL',
    required: true
)
if (turnOnBehavior == 'FIXED_VALUE') {
    input(
        name: 'onLevelValue',
        type: 'number',
        title: 'Fixed brightness value',
        description: '<small>Range 1..100</small>',
        defaultValue: 50,
        range: '1..100',
        required: true
    )
}
input(
    name: 'prestaging', type: 'bool',
    title: 'Pre-staging',
    description: '<small>Set brightness level without turning On the device (for later use).</small>',
    defaultValue: false,
    required: true
)
{{/ @inputs }}
{{!--------------------------------------------------------------------------}}
{{# @commands }}

// Commands for capability.Brightness
command 'shiftLevel', [[name:'Direction*', type:'ENUM', constraints: ['up', 'down']]]
{{/ @commands }}
{{!--------------------------------------------------------------------------}}
{{# @implementation }}

// Implementation for capability.Brightness
void setLevel(BigDecimal level, BigDecimal duration = 0) {
    Integer newLevel = level > 100 ? 100 : (level < 0 ? 0 : level)
    Integer lvl = newLevel * 2.54
    Integer dur = (duration == null || duration < 0) ? 0 : (duration > 1800 ? 1800 : duration) // Max transition time = 30 min
    log_debug "🎬 Setting brightness level to ${newLevel}% during ${dur} seconds"
    String command = prestaging == false ? '04' : '00'
    String payload = "${utils_payload lvl, 2} ${utils_payload dur * 10, 4}"
    utils_sendZigbeeCommands(["he raw 0x${device.deviceNetworkId} 0x01 0x${device.endpointId} 0x0008 {1143${command} ${payload}}"])
}
void startLevelChange(String direction) {
    log_debug "🎬 Starting brightness level change ${direction}wards with a rate of ${levelChangeRate}% / second"
    Integer mode = direction == 'up' ? 0x00 : 0x01
    Integer rate = Integer.parseInt(levelChangeRate) * 2.54
    String payload = "${utils_payload mode, 2} ${utils_payload rate, 2}"
    utils_sendZigbeeCommands(["he raw 0x${device.deviceNetworkId} 0x01 0x${device.endpointId} 0x0008 {114301 ${payload}}"])
}
void stopLevelChange() {
    log_debug '🎬 Stopping brightness level change'
    utils_sendZigbeeCommands(["he raw 0x${device.deviceNetworkId} 0x01 0x${device.endpointId} 0x0008 {114303}"])
}
void shiftLevel(String direction) {
    log_debug "🎬 Shifting brightness level ${direction} by ${levelStep}%"
    Integer mode = direction == 'up' ? 0x00 : 0x01
    Integer stepSize = Integer.parseInt(levelStep) * 2.54
    String payload = "${utils_payload mode, 2} ${utils_payload stepSize, 2} 0000"
    utils_sendZigbeeCommands(["he raw 0x${device.deviceNetworkId} 0x01 0x${device.endpointId} 0x0008 {114302 ${payload}}"])
}
{{/ @implementation }}
{{!--------------------------------------------------------------------------}}
{{# @updated }}

// Preferences for capability.Brightness
if (levelStep == null) {
    levelStep = '20'
    device.updateSetting 'levelStep', [value:levelStep, type:'enum']
}
log_info "🛠️ levelStep = ${levelStep}%"

if (levelChangeRate == null) {
    levelChangeRate = '20'
    device.updateSetting 'levelChangeRate', [value:levelChangeRate, type:'enum']
}
log_info "🛠️ levelChangeRate = ${levelChangeRate}% / second"

if (turnOnBehavior == null) {
    turnOnBehavior = 'RESTORE_PREVIOUS_LEVEL'
    device.updateSetting 'turnOnBehavior', [value:turnOnBehavior, type:'enum']
}
log_info "🛠️ turnOnBehavior = ${turnOnBehavior}"
if (turnOnBehavior == 'FIXED_VALUE') {
    Integer onLevelValue = onLevelValue == null ? 50 : onLevelValue.intValue()
    device.updateSetting 'onLevelValue', [value:onLevelValue, type:'number']
    log_info "🛠️ onLevelValue = ${onLevelValue}%"
    Integer lvl = onLevelValue * 2.54
    utils_sendZigbeeCommands zigbee.writeAttribute(0x0008, 0x0011, 0x20, lvl)
} else {
    log_debug 'Disabling OnLevel (0xFF)'
    cmds += zigbee.writeAttribute(0x0008, 0x0011, 0x20, 0xFF)
}

if (levelTransitionTime == null) {
    levelTransitionTime = '5'
    device.updateSetting 'levelTransitionTime', [value:levelTransitionTime, type:'enum']
}
log_info "🛠️ levelTransitionTime = ${Integer.parseInt(levelTransitionTime) / 10} second(s)"
cmds += zigbee.writeAttribute(0x0008, 0x0010, 0x21, Integer.parseInt(levelTransitionTime))

if (prestaging == null) {
    prestaging = false
    device.updateSetting 'prestaging', [value:prestaging, type:'bool']
}
log_info "🛠️ prestaging = ${prestaging}"

// If prestaging is true, enable update of brightness without the need for the device to be turned On
cmds += zigbee.writeAttribute(0x0008, 0x000F, 0x18, prestaging ? 0x01 : 0x00)
{{/ @updated }}
{{!--------------------------------------------------------------------------}}
{{# @configure }}

// Configuration for capability.Brightness
cmds += "zdo bind 0x${device.deviceNetworkId} 0x${device.endpointId} 0x01 0x0008 {${device.zigbeeId}} {}" // Level Control cluster
cmds += "he cr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0008 0x0000 0x20 0x0001 0x0258 {01} {}" // Report CurrentLevel (uint8) at least every 10 minutes (Δ = 1)
{{/ @configure }}
{{!--------------------------------------------------------------------------}}
{{# @refresh }}

// Refresh for capability.Brightness
cmds += zigbee.readAttribute(0x0008, 0x0000) // CurrentLevel
{{/ @refresh }}
{{!--------------------------------------------------------------------------}}
{{# @events }}

// Events for capability.Brightness
// ===================================================================================================================

// Report/Read Attributes Reponse: CurrentLevel
case { contains it, [clusterInt:0x0008, commandInt:0x0A, attrInt:0x0000] }:
case { contains it, [clusterInt:0x0008, commandInt:0x01, attrInt:0x0000] }:
    Integer level = msg.value == '00' ? 0 : Math.ceil(Integer.parseInt(msg.value, 16) / 2.54)
    utils_sendEvent name:'level', value:level, descriptionText:"Brightness is ${level}%", type:'digital'
    utils_processedZclMessage "${msg.commandInt == 0x0A ? 'Report' : 'Read'} Attributes Response", "CurrentLevel=${msg.value} (${level}%)"
    return

// Other events that we expect but are not usefull
case { contains it, [clusterInt:0x0008, commandInt:0x07] }:
    utils_processedZclMessage 'Configure Reporting Response', "attribute=CurrentLevel, data=${msg.data}"
    return
case { contains it, [clusterInt:0x0008, commandInt:0x04] }: // Write Attribute Response (0x04)
    return
{{/ @events }}
{{!--------------------------------------------------------------------------}}
