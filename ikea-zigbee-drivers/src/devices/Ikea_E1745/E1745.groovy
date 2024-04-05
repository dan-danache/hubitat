{{!--------------------------------------------------------------------------}}
{{# @definition }}
capability 'MotionSensor'
{{/ @definition }}
{{!--------------------------------------------------------------------------}}
{{# @attributes }}

// Attributes for devices.Ikea_E1745
attribute 'requestedBrightness', 'number'            // Syncs with the brightness option on device (◐/⭘)
attribute 'illumination', 'enum', ['dim', 'bright']  // Works only in night mode 🌙
{{/ @attributes }}
{{!--------------------------------------------------------------------------}}
{{# @inputs }}

// Inputs for devices.Ikea_E1745
input(
    name: 'clearMotionPeriod', type: 'enum',
    title: 'Clear motion after',
    description: '<small>Set status inactive if no motion is detected in this period.</small>',
    options: [
         '60': '1 minute',
        '120': '2 minutes',
        '180': '3 minutes',
        '240': '4 minutes',
        '300': '5 minutes',
        '360': '6 minutes',
        '420': '7 minutes',
        '480': '8 minutes',
        '540': '9 minutes',
        '600': '10 minutes'
    ],
    defaultValue: '180',
    required: true
)
// Inputs for devices.Ikea_E1745
input(
    name: 'onlyTriggerInDimLight', type: 'bool',
    title: 'Only detect motion in the dark',
    description: '<small>Select the night mode 🌙 option on device for this to work.</small>',
    defaultValue: false,
    required: true
)
{{/ @inputs }}
{{!--------------------------------------------------------------------------}}
{{# @implementation }}

// Implementation for devices.Ikea_E1745
void clearMotion() {
    utils_sendEvent name:'motion', value:'inactive', type:'digital', descriptionText:'Is inactive'
    return
}
{{/ @implementation }}
{{!--------------------------------------------------------------------------}}
{{# @configure }}

// Configuration for devices.Ikea_E1745
cmds += "zdo bind 0x${device.deviceNetworkId} 0x${device.endpointId} 0x01 0x0006 {${device.zigbeeId}} {}" // On/Off cluster
{{/ @configure }}
{{!--------------------------------------------------------------------------}}
{{# @updated }}

// Preferences for devices.Ikea_E1745
if (clearMotionPeriod == null) {
    clearMotionPeriod = '180'
    device.updateSetting 'clearMotionPeriod', [value:clearMotionPeriod, type:'enum']
}
log_info "🛠️ clearMotionPeriod = ${clearMotionPeriod} seconds"

if (onlyTriggerInDimLight == null) {
    onlyTriggerInDimLight = false
    device.updateSetting 'onlyTriggerInDimLight', [value:onlyTriggerInDimLight, type:'bool']
}
log_info "🛠️ onlyTriggerInDimLight = ${onlyTriggerInDimLight}"
{{/ @updated }}
{{!--------------------------------------------------------------------------}}
{{# @events }}

// Events for devices.Ikea_E1745
// ===================================================================================================================

// OnWithTimedOff := { 08:OnOffControl, 16:OnTime, 16:OffWaitTime }
// OnOffControl := { 01:AcceptOnlyWhenOn, 07:Reserved }
// Example: [01, 08, 07, 00, 00] -> acceptOnlyWhenOn=true, onTime=180, offWaitTime=0
case { contains it, [clusterInt:0x0006, commandInt:0x42] }:
    String illumination = msg.data[0] == '01' ? 'bright' : 'dim'
    utils_sendEvent(name:'illumination', value:illumination, type:'physical', descriptionText:"Illumination is ${illumination}")

    if (illumination == 'bright' && onlyTriggerInDimLight) {
        log_debug 'Ignored detected motion because the "Only detect motion in the dark" option is active and the sensor detected plenty of light'
        return
    }

    runIn Integer.parseInt(clearMotionPeriod), 'clearMotion', [overwrite:true]
    utils_sendEvent name:'motion', value:'active', type:'physical', descriptionText:'Is active'
    return

// MoveToLevelWithOnOff := { 08:Level, 16:TransitionTime }
// Example: [4C, 01, 00] -> level=30%, transitionTime=1/10seconds
// Example: :[FE, 01, 00 -> level=100%, transitionTime=1/10seconds
case { contains it, [clusterInt:0x0008, commandInt:0x04] }:
    Integer requestedBrightness = Math.round(Integer.parseInt(msg.data[0], 16) * 100 / 254)
    utils_sendEvent name:'requestedBrightness', value:requestedBrightness, unit:'%', type:'physical', descriptionText:"Requested brightness set too ${requestedBrightness}%"
    return
{{/ @events }}
{{!--------------------------------------------------------------------------}}
