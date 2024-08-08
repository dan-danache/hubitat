{{!--------------------------------------------------------------------------}}
{{# @definition }}
capability 'Actuator'
capability 'Switch'
capability 'RelaySwitch'
{{/ @definition }}
{{!--------------------------------------------------------------------------}}
{{# @fields }}

// Fields for capabilities.RelaySwitch
@Field static final Map<Integer, String> PULSE_DURATIONS = [
       '0':'Disable Impulse Mode',
     '100':'100 miliseconds',
     '300':'300 miliseconds',
     '500':'500 miliseconds',
    '1000':'1 second',
    '2000':'2 seconds',
]
{{/ @fields }}
{{!--------------------------------------------------------------------------}}
{{# @attributes }}

// Attributes for capabilities.RelaySwitch
attribute 'switchType', 'enum', ['toggle', 'momentary']
{{/ @attributes }}
{{!--------------------------------------------------------------------------}}
{{# @inputs }}

// Inputs for capability.RelaySwitch
input(
    name: 'powerOnBehavior',
    type: 'enum',
    title: 'Power On behaviour',
    description: '<small>Select what happens after a power outage.</small>',
    options: ['TURN_POWER_ON':'Turn power On', 'TURN_POWER_OFF':'Turn power Off', 'RESTORE_PREVIOUS_STATE':'Restore previous state'],
    defaultValue: 'RESTORE_PREVIOUS_STATE',
    required: true
)
input(
    name: 'pulseDuration', type: 'enum',
    title: 'Relay Impulse Mode',
    description: '<small>Disable Inpulse Mode or configure relay pulse duration.</small>',
    options: PULSE_DURATIONS,
    defaultValue: '0',
    required: true
)
{{/ @inputs }}
{{!--------------------------------------------------------------------------}}
{{# @commands }}

// Commands for capability.RelaySwitch
command 'toggle'
command 'onWithTimedOff', [[name:'On duration*', type:'NUMBER', description:'After how many seconds power will be turned Off [1..6500]']]
{{/ @commands }}
{{!--------------------------------------------------------------------------}}
{{# @implementation }}

// Implementation for capability.RelaySwitch
void on() {
    log_debug '🎬 Sending On command'
    utils_sendZigbeeCommands(["he raw 0x${device.deviceNetworkId} 0x01 0x${device.endpointId} 0x0006 {114301}"])
}
void off() {
    log_debug '🎬 Sending Off command'
    utils_sendZigbeeCommands(["he raw 0x${device.deviceNetworkId} 0x01 0x${device.endpointId} 0x0006 {114300}"])
}
void toggle() {
    log_debug '🎬 Sending Toggle command'
    utils_sendZigbeeCommands(["he raw 0x${device.deviceNetworkId} 0x01 0x${device.endpointId} 0x0006 {114302}"])
}
void onWithTimedOff(BigDecimal onTime = 1) {
    Integer delay = onTime < 1 ? 1 : (onTime > 6500 ? 6500 : onTime)
    log_debug '🎬 Sending OnWithTimedOff command'
    Integer dur = delay * 10
    String payload = "00 ${utils_payload dur, 4} 0000"
    utils_sendZigbeeCommands(["he raw 0x${device.deviceNetworkId} 0x01 0x${device.endpointId} 0x0006 {114342 ${payload}}"])
}
{{/ @implementation }}
{{!--------------------------------------------------------------------------}}
{{# @updated }}

// Preferences for capability.RelaySwitch
if (powerOnBehavior == null) {
    powerOnBehavior = 'RESTORE_PREVIOUS_STATE'
    device.updateSetting 'powerOnBehavior', [value:powerOnBehavior, type:'enum']
}
log_info "🛠️ powerOnBehavior = ${powerOnBehavior}"
cmds += zigbee.writeAttribute(0x0006, 0x4003, 0x30, powerOnBehavior == 'TURN_POWER_OFF' ? 0x00 : (powerOnBehavior == 'TURN_POWER_ON' ? 0x01 : 0xFF))

if (pulseDuration == null) {
    pulseDuration = '0'
    device.updateSetting 'pulseDuration', [value:pulseDuration, type:'enum']
}
log_info "🛠️ pulseDuration = ${pulseDuration}ms"
cmds += zigbee.writeAttribute(0x0006, 0x0001, 0x21, Integer.parseInt(pulseDuration), [mfgCode:'0x128B', destEndpoint:0x01])
{{/ @updated }}
{{!--------------------------------------------------------------------------}}
{{# @configure }}

// Configuration for capability.RelaySwitch
cmds += "zdo bind 0x${device.deviceNetworkId} 0x${device.endpointId} 0x01 0x0006 {${device.zigbeeId}} {}" // On/Off cluster
cmds += "he cr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0006 0x0000 0x10 0x0000 0x0258 {01} {}" // Report OnOff (bool) at least every 10 minutes
{{/ @configure }}
{{!--------------------------------------------------------------------------}}
{{# @refresh }}

// Refresh for capability.RelaySwitch
cmds += zigbee.readAttribute(0x0006, 0x0000) // OnOff
cmds += zigbee.readAttribute(0x0006, 0x4003) // PowerOnBehavior
cmds += zigbee.readAttribute(0x0006, 0x0001, [mfgCode: '0x128B']) // TransitionTime
cmds += zigbee.readAttribute(0x0007, 0x0000) // SwitchType
{{/ @refresh }}
{{!--------------------------------------------------------------------------}}
{{# @events }}

// Events for capability.RelaySwitch
// ===================================================================================================================

// Report/Read Attributes: OnOff
case { contains it, [clusterInt:0x0006, commandInt:0x0A, attrInt:0x0000] }:
case { contains it, [clusterInt:0x0006, commandInt:0x01, attrInt:0x0000] }:
    String newState = msg.value == '00' ? 'off' : 'on'
    utils_sendEvent name:'switch', value:newState, descriptionText:"Was turned ${newState}", type:type
    utils_processedZclMessage "${msg.commandInt == 0x0A ? 'Report' : 'Read'} Attributes Response", "OnOff=${newState}"
    return

// Switch was pressed - OnOff cluster Toggle
case { contains it, [clusterInt:0x0006, commandInt:0x02] }:
    List<String> button = BUTTONS.SWITCH
    utils_sendEvent name:'pushed', value:button[0], type:'physical', isStateChange:true, descriptionText:"Button ${button[0]} (${button[1]}) was pushed"
    return

// Read Attributes Response: powerOnBehavior
case { contains it, [clusterInt:0x0006, commandInt:0x01, attrInt:0x4003] }:
    String newValue = ''
    switch (Integer.parseInt(msg.value, 16)) {
        case 0x00: newValue = 'TURN_POWER_OFF'; break
        case 0x01: newValue = 'TURN_POWER_ON'; break
        case 0xFF: newValue = 'RESTORE_PREVIOUS_STATE'; break
        default: log_warn "Received unexpected attribute value: PowerOnBehavior=${msg.value}"; return
    }
    powerOnBehavior = newValue
    device.updateSetting 'powerOnBehavior', [value:newValue, type:'enum']
    utils_processedZclMessage 'Read Attributes Response', "PowerOnBehavior=${newValue}"
    return

// Read Attributes: TransitionTime
case { contains it, [clusterInt:0x0006, commandInt:0x01, attrInt:0x0001] }:
case { contains it, [clusterInt:0x0006, commandInt:0x0A, attrInt:0x0001] }:
    String pulseDuration = Integer.parseInt(msg.value, 16).toString()
    if (!PULSE_DURATIONS.containsKey(pulseDuration)) pulseDuration = '0'
    device.updateSetting 'pulseDuration', [value:pulseDuration, type:'enum']
    utils_processedZclMessage 'Read Attributes Response', "TransitionTime=${msg.value} (${pulseDuration}ms)"
    return

// Read Attributes: SwitchType
case { contains it, [clusterInt:0x0007, commandInt:0x01, attrInt:0x0000] }:
case { contains it, [clusterInt:0x0007, commandInt:0x0A, attrInt:0x0000] }:
    String switchType = msg.value == '00' ? 'toggle' : 'momentary'
    utils_sendEvent name:'switchType', value:switchType, descriptionText:"Switch type is ${switchType}", type:type
    utils_processedZclMessage 'Read Attributes Response', "SwitchType=${msg.value} (${switchType})"
    return

// Other events that we expect but are not usefull
case { contains it, [clusterInt:0x0006, commandInt:0x07] }:
    utils_processedZclMessage 'Configure Reporting Response', "attribute=OnOff, data=${msg.data}"
    return
case { contains it, [clusterInt:0x0006, commandInt:0x04] }: // Write Attributes Response
    return
{{/ @events }}
{{!--------------------------------------------------------------------------}}
