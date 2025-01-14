/**
 * IKEA RGB-Only Light
 *
 * @see https://dan-danache.github.io/hubitat/ikea-zigbee-drivers/
 */
import java.math.RoundingMode
import groovy.transform.CompileStatic
import groovy.transform.Field
import com.hubitat.zigbee.DataType

@Field static final String DRIVER_NAME = 'IKEA RGB-Only Light'
@Field static final String DRIVER_VERSION = '5.4.1'

// Fields for capability.HealthCheck
import groovy.time.TimeCategory

@Field static final Map<String, String> HEALTH_CHECK = [
    'schedule': '0 0 0/1 ? * * *', // Health will be checked using this cron schedule
    'thereshold': '3600' // When checking, mark the device as offline if no Zigbee message was received in the last 3600 seconds
]

// Fields for capability.ZigbeeGroups
@Field static final Map<String, String> GROUPS = [
    '9900':'Alfa', '9901':'Bravo', '9902':'Charlie', '9903':'Delta', '9904':'Echo', '9905':'Foxtrot', '9906':'Golf', '9907':'Hotel', '9908':'India', '9909':'Juliett', '990A':'Kilo', '990B':'Lima', '990C':'Mike', '990D':'November', '990E':'Oscar', '990F':'Papa', '9910':'Quebec', '9911':'Romeo', '9912':'Sierra', '9913':'Tango', '9914':'Uniform', '9915':'Victor', '9916':'Whiskey', '9917':'Xray', '9918':'Yankee', '9919':'Zulu'
]

metadata {
    definition(name:DRIVER_NAME, namespace:'dandanache', author:'Dan Danache', importUrl:'https://raw.githubusercontent.com/dan-danache/hubitat/master/ikea-zigbee-drivers/Ikea_RGBO-Light.groovy') {
        capability 'Configuration'
        capability 'Refresh'
        capability 'Actuator'
        capability 'Switch'
        capability 'ColorControl'
        capability 'Light'
        capability 'ChangeLevel'
        capability 'SwitchLevel'
        capability 'HealthCheck'
        capability 'PowerSource'

        fingerprint profileId:'0104', endpointId:'01', inClusters:'0000,0003,0004,0005,0006,0008,0300,1000,FC7C', outClusters:'0019,1000', model:'TRADFRI bulb E27 CWS opal 600lm', manufacturer:'IKEA of Sweden', controllerType:'ZGB' // LED1624G9E27EU: 2.3.093 (117C-2801-23086631)
        fingerprint profileId:'0104', endpointId:'01', inClusters:'0000,0003,0004,0005,0006,0008,0300,1000,FC7C', outClusters:'0019,1000', model:'TRADFRI bulb E14 CWS opal 600lm', manufacturer:'IKEA of Sweden', controllerType:'ZGB' // LED1624G9E14EU: 2.3.093 (117C-2803-23093631)
        
        // Attributes for capability.HealthCheck
        attribute 'healthStatus', 'enum', ['offline', 'online', 'unknown']
    }
    
    // Commands for capability.Switch
    command 'toggle'
    command 'onWithTimedOff', [[name:'On duration*', type:'NUMBER', description:'After how many seconds power will be turned Off [1..6500]']]
    
    // Commands for capability.Brightness
    command 'shiftLevel', [[name:'Direction*', type:'ENUM', constraints: ['up', 'down']]]
    
    // Commands for capability.FirmwareUpdate
    command 'updateFirmware'

    preferences {
        input(
            name:'helpInfo', type:'hidden',
            title:'''
            <div style="min-height:55px; background:transparent url('https://dan-danache.github.io/hubitat/ikea-zigbee-drivers/img/Ikea_RGBO-Light.webp') no-repeat left center;background-size:auto 55px;padding-left:60px">
                IKEA RGB-Only Light <small>v5.4.1</small><br>
                <small><div>
                • <a href="https://dan-danache.github.io/hubitat/ikea-zigbee-drivers/#rgb-only-light" target="_blank">device details</a><br>
                • <a href="https://community.hubitat.com/t/release-ikea-zigbee-drivers/123853" target="_blank">community page</a><br>
                </div></small>
            </div>
            '''
        )
        input(
            name:'logLevel', type:'enum', title:'Log verbosity', required:true,
            description:'Select what messages appear in the "Logs" section',
            options:['1':'Debug - log everything', '2':'Info - log important events', '3':'Warning - log events that require attention', '4':'Error - log errors'],
            defaultValue:'1'
        )
        
        // Inputs for capability.Switch
        input(
            name:'powerOnBehavior', type:'enum', title:'Power On behaviour', required:true,
            description:'Select what happens after a power outage',
            options:['TURN_POWER_ON':'Turn power On', 'TURN_POWER_OFF':'Turn power Off', 'RESTORE_PREVIOUS_STATE':'Restore previous state', 'TOGGLE':'Toggle state'],
            defaultValue:'RESTORE_PREVIOUS_STATE'
        )
        
        // Inputs for capability.Brightness
        input(
            name:'levelStep', type:'enum', title:'Brightness up/down shift', required:true,
            description:'Brightness +/- adjust for the shiftLevel() command',
            options:['1':'1%', '2':'2%', '5':'5%', '10':'10%', '20':'20%', '25':'25%', '33':'33%', '50':'50%'],
            defaultValue:'25'
        )
        input(
            name:'levelChangeRate', type:'enum', title:'Brightness change rate', required:true,
            description:'Brightness +/- adjust for the startLevelChange() command',
            options:[
                 '10': '10% / sec - from 0% to 100% in 10 seconds',
                 '20': '20% / sec - from 0% to 100% in 5 seconds',
                 '33': '33% / sec - from 0% to 100% in 3 seconds',
                 '50': '50% / secs - from 0% to 100% in 2 seconds',
                '100': '100% / sec - from 0% to 100% in 1 seconds',
            ],
            defaultValue:'20'
        )
        input(
            name:'levelTransitionTime', type:'enum', title:'Brightness transition time', required:true,
            description:'Time taken to move to/from the target brightness when device is turned On/Off',
            options:[
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
            defaultValue:'5'
        )
        input(
            name:'turnOnBehavior', type:'enum', title:'Turn On behavior', required:true,
            description:'Select what happens when the device is turned On',
            options:[
                'RESTORE_PREVIOUS_LEVEL': 'Restore previous brightness',
                'FIXED_VALUE': 'Always start with the same fixed brightness'
            ],
            defaultValue:'RESTORE_PREVIOUS_LEVEL'
        )
        if (turnOnBehavior == 'FIXED_VALUE') {
            input(
                name:'onLevelValue', type:'number', title:'Fixed brightness value', required:true,
                description:'Range 1..100',
                range:'1..100',
                defaultValue:50
            )
        }
        input(
            name:'prestaging', type:'bool', title:'Pre-staging', required:true,
            description:'Set brightness level without turning On the device (for later use)',
            defaultValue:false
        )
        
        // Inputs for capability.ZigbeeBindings
        input(
            name:'joinGroup', type:'enum', title:'Join a Zigbee group', required:false,
            description:'Select a Zigbee group you want to join',
            options:['0000':'❌ Leave all Zigbee groups', '----':'- - - -'] + GROUPS,
            defaultValue:'----'
        )
    }
}

// ===================================================================================================================
// Implement default methods
// ===================================================================================================================

// Called when the device is first added
void installed() {
    log_warn 'Installing device ...'
    log_warn '[IMPORTANT] For battery-powered devices, make sure that you keep your device as close as you can (less than 2inch / 5cm) to your Hubitat hub for at least 30 seconds. Otherwise the device will successfully pair but it won\'t work properly!'
    state.lastCx = DRIVER_VERSION
}

// Called when the "Save Preferences" button is clicked
List<String> updated(boolean auto = false) {
    log_info "🎬 Saving preferences${auto ? ' (auto)' : ''} ..."
    List<String> cmds = []

    unschedule()

    if (logLevel == null) {
        logLevel = '1'
        device.updateSetting 'logLevel', [value:logLevel, type:'enum']
    }
    if (logLevel == '1') runIn 1800, 'logsOff'
    log_info "🛠️ logLevel = ${['1':'Debug', '2':'Info', '3':'Warning', '4':'Error'].get(logLevel)}"
    
    // Preferences for capability.Switch
    if (powerOnBehavior == null) {
        powerOnBehavior = 'RESTORE_PREVIOUS_STATE'
        device.updateSetting 'powerOnBehavior', [value:powerOnBehavior, type:'enum']
    }
    log_info "🛠️ powerOnBehavior = ${powerOnBehavior}"
    cmds += zigbee.writeAttribute(0x0006, 0x4003, 0x30, powerOnBehavior == 'TURN_POWER_OFF' ? 0x00 : (powerOnBehavior == 'TURN_POWER_ON' ? 0x01 : (powerOnBehavior == 'TOGGLE' ? 0x02 : 0xFF)))
    
    // Preferences for capability.ColorControl
    cmds += zigbee.writeAttribute(0x0300, 0x000F, 0x18, 0x01)
    
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
    
    // Preferences for capability.HealthCheck
    schedule HEALTH_CHECK.schedule, 'healthCheck'
    
    // Preferences for capability.ZigbeeGroups
    if (joinGroup != null && joinGroup != '----') {
        if (joinGroup == '0000') {
            log_info '🛠️ Leaving all Zigbee groups'
            cmds += "he raw 0x${device.deviceNetworkId} 0x01 0x${device.endpointId} 0x0004 {0143 04}" // Leave all groups
        } else {
            String groupName = GROUPS.getOrDefault(joinGroup, 'Unknown')
            log_info "🛠️ Joining group: ${joinGroup} (${groupName})"
            cmds += "he raw 0x${device.deviceNetworkId} 0x01 0x${device.endpointId} 0x0004 {0143 00 ${utils_payload joinGroup} ${Integer.toHexString(groupName.length()).padLeft(2, '0')}${groupName.bytes.encodeHex()}}"  // Join group
        }
        device.updateSetting 'joinGroup', [value:'----', type:'enum']
        cmds += "he raw 0x${device.deviceNetworkId} 0x01 0x${device.endpointId} 0x0004 {0143 02 00}" // Get groups membership
    }

    if (auto) return cmds
    utils_sendZigbeeCommands cmds
    return []
}

// ===================================================================================================================
// Capabilities helpers
// ===================================================================================================================

// Handler method for scheduled job to disable debug logging
void logsOff() {
    log_info '⏲️ Automatically reverting log level to "Info"'
    device.updateSetting 'logLevel', [value:'2', type:'enum']
}

// Helpers for capability.HealthCheck
void healthCheck() {
    log_debug '⏲️ Automatically running health check'
    String healthStatus = state.lastRx == 0 || state.lastRx == null ? 'unknown' : (now() - state.lastRx < Integer.parseInt(HEALTH_CHECK.thereshold) * 1000 ? 'online' : 'offline')
    utils_sendEvent name:'healthStatus', value:healthStatus, type:'physical', descriptionText:"Health status is ${healthStatus}"
}

// ===================================================================================================================
// Implement Capabilities
// ===================================================================================================================

// capability.Configuration
// Note: This method is also called when the device is initially installed
void configure(boolean auto = false) {
    log_warn "⚙️ Configuring device${auto ? ' (auto)' : ''} ..."
    if (!auto && device.currentValue('powerSource', true) == 'battery') {
        log_warn '[IMPORTANT] Click the "Configure" button immediately after pushing any button on the device in order to first wake it up!'
    }

    // Clear data (keep firmwareMT information though)
    device.data*.key.each { if (it != 'firmwareMT') device.removeDataValue it }

    // Clear state
    state.clear()
    state.lastTx = 0
    state.lastRx = 0
    state.lastCx = DRIVER_VERSION

    // Put device in identifying state (blinking LED)
    List<String> cmds = ["he raw 0x${device.deviceNetworkId} 0x01 0x01 0x0003 {014300 3C00}"]

    // Auto-refresh device state
    cmds += refresh true
    utils_sendZigbeeCommands cmds

    // Apply configuration after the auto-refresh finishes
    runIn(cmds.findAll { !it.startsWith('delay') }.size() + 1, 'configureApply')
}
void configureApply() {
    log_info '⚙️ Finishing device configuration ...'
    List<String> cmds = ["he raw 0x${device.deviceNetworkId} 0x01 0x01 0x0003 {014300 3C00}"]

    // Auto-apply preferences
    cmds += updated true
    
    // Configuration for capability.Switch
    cmds += "zdo bind 0x${device.deviceNetworkId} 0x${device.endpointId} 0x01 0x0006 {${device.zigbeeId}} {}" // On/Off cluster
    cmds += "he cr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0006 0x0000 0x10 0x0000 0x0258 {01} {}" // Report OnOff (bool) at least every 10 minutes
    
    // Configuration for capability.ColorControl
    cmds += "he cr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0300 0x0000 0x20 0x0001 0x0258 {02} {}" // Report CurrentHue (uint8) at least every 10 minutes (Δ = 1%)
    cmds += "he cr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0300 0x0001 0x20 0x0001 0x0258 {02} {}" // Report CurrentSaturation (uint8) at least every 10 minutes (Δ = 1%)
    cmds += "he cr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0300 0x4000 0x21 0x0002 0xFFFE {CB0C} {}" // Report EnhancedCurrentHue (uint16) at most every 2 seconds (Δ = 5%)
    
    // Configuration for capability.Brightness
    cmds += "zdo bind 0x${device.deviceNetworkId} 0x${device.endpointId} 0x01 0x0008 {${device.zigbeeId}} {}" // Level Control cluster
    cmds += "he cr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0008 0x0000 0x20 0x0001 0x0258 {01} {}" // Report CurrentLevel (uint8) at least every 10 minutes (Δ = 1)
    
    // Configuration for capability.HealthCheck
    sendEvent name:'healthStatus', value:'online', descriptionText:'Health status initialized to online'
    sendEvent name:'checkInterval', value:3600, unit:'second', descriptionText:'Health check interval is 3600 seconds'
    
    // Configuration for capability.PowerSource
    sendEvent name:'powerSource', value:'unknown', type:'digital', descriptionText:'Power source initialized to unknown'
    cmds += zigbee.readAttribute(0x0000, 0x0007) // PowerSource

    // Query Basic cluster attributes
    cmds += zigbee.readAttribute(0x0000, [0x0001, 0x0003, 0x0004, 0x4000]) // ApplicationVersion, HWVersion, ManufacturerName, SWBuildID
    cmds += zigbee.readAttribute(0x0000, [0x0005]) // ModelIdentifier
    cmds += zigbee.readAttribute(0x0000, [0x000A]) // ProductCode

    // Stop blinking LED
    cmds += "he raw 0x${device.deviceNetworkId} 0x01 0x01 0x0003 {014300 0000}"
    utils_sendZigbeeCommands cmds
}
/* groovylint-disable-next-line UnusedPrivateMethod */
private void autoConfigure() {
    log_warn "👁️ Detected that this device is not properly configured for this driver version (lastCx != ${DRIVER_VERSION})"
    configure true
}

// capability.Refresh
List<String> refresh(boolean auto = false) {
    if (auto) log_debug '🎬 Refreshing device state (auto) ...'
    else log_info '🎬 Refreshing device state ...'
    if (!auto && device.currentValue('powerSource', true) == 'battery') {
        log_warn '[IMPORTANT] Click the "Refresh" button immediately after pushing any button on the device in order to first wake it up!'
    }

    List<String> cmds = []
    
    // Refresh for capability.Switch
    cmds += zigbee.readAttribute(0x0006, 0x0000) // OnOff
    cmds += zigbee.readAttribute(0x0006, 0x4003) // PowerOnBehavior
    
    // Refresh for capability.ColorControl
    cmds += zigbee.readAttribute(0x0300, [0x0000, 0x0001, 0x0008]) // CurrentHue, CurrentSaturation, ColorMode
    
    // Refresh for capability.Brightness
    cmds += zigbee.readAttribute(0x0008, 0x0000) // CurrentLevel
    
    // Refresh for capability.ZigbeeGroups
    cmds += "he raw 0x${device.deviceNetworkId} 0x01 0x${device.endpointId} 0x0004 {0143 02 00}" // Get groups membership

    if (auto) return cmds
    utils_sendZigbeeCommands cmds
    return []
}

// Implementation for capability.Switch
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

// Implementation for capability.ColorControl
void setColor(Map colormap) {
    Integer newHue = colormap.hue > 100 ? 100 : (colormap.hue < 0 ? 0 : colormap.hue)
    Integer newSaturation = colormap.saturation > 100 ? 100 : (colormap.saturation < 0 ? 0 : colormap.saturation)
    Integer newLevel = colormap.level > 100 ? 100 : (colormap.level < 0 ? 0 : colormap.level)
    log_debug "Setting color to hue=${newHue}, saturation=${newSaturation}, level=${newLevel}"
    newHue = Math.round(newHue * 2.54)
    newSaturation = Math.round(newSaturation * 2.54)
    String payload = "${utils_payload newHue, 2} ${utils_payload newSaturation, 2} 0000 00 00"
    utils_sendZigbeeCommands(["he raw 0x${device.deviceNetworkId} 0x01 0x${device.endpointId} 0x0300 {114306 ${payload}}"]) // Move to Hue and Saturation
    /* groovylint-disable-next-line UnnecessarySetter */
    setLevel newLevel
}
void setHue(String hue) { setHue Integer.parseInt(hue) }
void setHue(BigDecimal hue) {
    Integer newHue = hue > 100 ? 100 : (hue < 0 ? 0 : hue)
    log_debug "Setting color hue to ${newHue}%"
    newHue = Math.round(newHue * 2.54)
    String payload = "${utils_payload newHue, 2} 00 0000 00 00"
    utils_sendZigbeeCommands(["he raw 0x${device.deviceNetworkId} 0x01 0x${device.endpointId} 0x0300 {114300 ${payload}}"]) // Move to Hue
}
void setSaturation(String saturation) { setSaturation Integer.parseInt(saturation) }
void setSaturation(BigDecimal saturation) {
    Integer newSaturation = saturation > 100 ? 100 : (saturation < 0 ? 0 : saturation)
    log_debug "Setting color saturation to ${newSaturation}%"
    newSaturation = Math.round(newSaturation * 2.54)
    String payload = "${utils_payload newSaturation, 2} 0000 00 00"
    utils_sendZigbeeCommands(["he raw 0x${device.deviceNetworkId} 0x01 0x${device.endpointId} 0x0300 {114303 ${payload}}"]) // Move to Saturation
}
private void processMultipleColorAttributes(Map msg, String type) {
    Map<Integer, String> attributes = [:]
    attributes[msg.attrInt] = msg.value
    msg.additionalAttrs?.each { attributes[Integer.parseInt(it.attrId, 16)] = it.value }

    Integer hue = -1
    Integer saturation = -1
    String colorMode = null
    attributes.each {
        switch (it.key) {
            case 0x0000:
                hue = Math.round(Integer.parseInt(it.value, 16) / 2.54)
                hue = hue > 100 ? 100 : (hue < 0 ? 0 : hue)
                break
            case 0x0001:
                saturation = Math.round(Integer.parseInt(it.value, 16) / 2.54)
                saturation = saturation > 100 ? 100 : (saturation < 0 ? 0 : saturation)
                break
            case 0x0008:
            case 0x4001:
                colorMode = it.value == '02' ? 'CT' : 'RGB'
                utils_sendEvent name:'colorMode', value:colorMode, descriptionText:"Color mode is ${colorMode}", type:type
                break
            case 0x4000:
                hue = Math.round(Integer.parseInt(it.value, 16) / 655.34)
                hue = hue > 100 ? 100 : (hue < 0 ? 0 : hue)
        }
    }

    if (hue >= 0) utils_sendEvent name:'hue', value:hue, descriptionText:"Color hue is ${hue}%", type:type
    if (saturation >= 0) utils_sendEvent name:'saturation', value:saturation, descriptionText:"Color saturation is ${saturation}%", type:type

    // Update colorName, if the case
    if ("${colorMode ?: device.currentValue('colorMode', true)}" == 'RGB') {
        Integer colorHue = hue >= 0 ? hue : device.currentValue('hue', true)
        Integer colorSaturation = saturation >= 0 ? saturation : device.currentValue('saturation', true)
        String colorName = convertHueToGenericColorName colorHue, colorSaturation
        utils_sendEvent name:'colorName', value:colorName, descriptionText:"Color name is ${colorName}", type:type
    }
    utils_processedZclMessage "${msg.commandInt == 0x0A ? 'Report' : 'Read'} Attributes Response", "CurrentHue=${hue}%, CurrentSaturation=${saturation}%, ColorMode=${colorMode}"
}

// Implementation for capability.Brightness
void setLevel(String level, String duration = '0') { setLevel(Integer.parseInt(level), Integer.parseInt(duration)) }
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

// Implementation for capability.HealthCheck
void ping() {
    log_warn 'ping ...'
    utils_sendZigbeeCommands(zigbee.readAttribute(0x0000, 0x0000))
    log_debug '🎬 Ping command sent to the device; we\'ll wait 5 seconds for a reply ...'
    runIn 5, 'pingExecute'
}
void pingExecute() {
    if (state.lastRx == 0) {
        log_info 'Did not sent any messages since it was last configured'
        return
    }

    Date now = new Date(Math.round(now() / 1000) * 1000)
    Date lastRx = new Date(Math.round(state.lastRx / 1000) * 1000)
    String lastRxAgo = TimeCategory.minus(now, lastRx).toString().replace('.000 seconds', ' seconds')
    log_info "Sent last message at ${lastRx.format('yyyy-MM-dd HH:mm:ss', location.timeZone)} (${lastRxAgo} ago)"

    Date thereshold = new Date(Math.round(state.lastRx / 1000 + Integer.parseInt(HEALTH_CHECK.thereshold)) * 1000)
    String theresholdAgo = TimeCategory.minus(thereshold, lastRx).toString().replace('.000 seconds', ' seconds')
    log_info "Will be marked as offline if no message is received for ${theresholdAgo} (hardcoded)"

    String offlineMarkAgo = TimeCategory.minus(thereshold, now).toString().replace('.000 seconds', ' seconds')
    log_info "Will be marked as offline if no message is received until ${thereshold.format('yyyy-MM-dd HH:mm:ss', location.timeZone)} (${offlineMarkAgo} from now)"
}

// Implementation for capability.FirmwareUpdate
void updateFirmware() {
    log_info 'Looking for firmware updates ...'
    if (device.currentValue('powerSource', true) == 'battery') {
        log_warn '[IMPORTANT] Click the "Update Firmware" button immediately after pushing any button on the device in order to first wake it up!'
    }
    utils_sendZigbeeCommands zigbee.updateFirmware()
}

// ===================================================================================================================
// Handle incoming Zigbee messages
// ===================================================================================================================

void parse(String description) {
    log_debug "description=[${description}]"

    // Auto-Configure device: configure() was not called for this driver version
    if (state.lastCx != DRIVER_VERSION) {
        state.lastCx = DRIVER_VERSION
        runInMillis 1500, 'autoConfigure'
    }

    // Extract msg
    Map msg = [:]
    if (description.startsWith('zone status')) msg += [clusterInt:0x500, commandInt:0x00, isClusterSpecific:true]
    else if (description.startsWith('enroll request')) msg += [clusterInt:0x500, commandInt:0x01, isClusterSpecific:true]

    msg += zigbee.parseDescriptionAsMap description
    if (msg.containsKey('endpoint')) msg.endpointInt = Integer.parseInt(msg.endpoint, 16)
    if (msg.containsKey('sourceEndpoint')) msg.endpointInt = Integer.parseInt(msg.sourceEndpoint, 16)
    if (msg.containsKey('cluster')) msg.clusterInt = Integer.parseInt(msg.cluster, 16)
    if (msg.containsKey('command')) msg.commandInt = Integer.parseInt(msg.command, 16)
    log_debug "msg=[${msg}]"

    state.lastRx = now()
    
    // Parse for capability.HealthCheck
    if (device.currentValue('healthStatus', true) != 'online') {
        utils_sendEvent name:'healthStatus', value:'online', type:'digital', descriptionText:'Health status changed to online'
    }

    // If we sent a Zigbee command in the last 3 seconds, we assume that this Zigbee event is a consequence of this driver doing something
    // Therefore, we mark this event as "digital"
    String type = state.containsKey('lastTx') && (now() - state.lastTx < 3000) ? 'digital' : 'physical'

    switch (msg) {
        
        // Events for capability.Switch
        // ===================================================================================================================
        
        // Report/Read Attributes: OnOff
        case { contains it, [clusterInt:0x0006, commandInt:0x0A, attrInt:0x0000] }:
        case { contains it, [clusterInt:0x0006, commandInt:0x01, attrInt:0x0000] }:
            String newState = msg.value == '00' ? 'off' : 'on'
            utils_sendEvent name:'switch', value:newState, descriptionText:"Was turned ${newState}", type:type
        
            utils_processedZclMessage "${msg.commandInt == 0x0A ? 'Report' : 'Read'} Attributes Response", "OnOff=${newState}"
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
        
        // Other events that we expect but are not usefull
        case { contains it, [clusterInt:0x0006, commandInt:0x07] }:
            utils_processedZclMessage 'Configure Reporting Response', "attribute=OnOff, data=${msg.data}"
            return
        case { contains it, [clusterInt:0x0006, commandInt:0x04] }: // Write Attribute Response
        case { contains it, [clusterInt:0x0006, commandInt:0x06, isClusterSpecific:false, direction:'01'] }: // Configure Reporting Command
            return
        
        // Events for capability.ColorControl
        // ===================================================================================================================
        
        // Report/Read Attributes Response: CurrentHue
        case { contains it, [clusterInt:0x0300, commandInt:0x0A, attrInt:0x0000] }:
        case { contains it, [clusterInt:0x0300, commandInt:0x01, attrInt:0x0000] }:
        
        // Report/Read Attributes Response: CurrentSaturation
        case { contains it, [clusterInt:0x0300, commandInt:0x0A, attrInt:0x0001] }:
        case { contains it, [clusterInt:0x0300, commandInt:0x01, attrInt:0x0001] }:
        
        // Report/Read Attributes Response: ColorMode
        case { contains it, [clusterInt:0x0300, commandInt:0x0A, attrInt:0x0008] }:
        case { contains it, [clusterInt:0x0300, commandInt:0x01, attrInt:0x0008] }:
        
        // Report/Read Attributes Response: EnhancedColorMode
        case { contains it, [clusterInt:0x0300, commandInt:0x0A, attrInt:0x4001] }:
        case { contains it, [clusterInt:0x0300, commandInt:0x01, attrInt:0x4001] }:
        
        // Report Attributes Response: EnhancedCurrentHue
        case { contains it, [clusterInt:0x0300, commandInt:0x0A, attrInt:0x4000] }:
            processMultipleColorAttributes msg, type
            return
        
        // Other events that we expect but are not usefull
        case { contains it, [clusterInt:0x0300, commandInt:0x07] }:
            utils_processedZclMessage 'Configure Reporting Response', "data=${msg.data}"
            return
        case { contains it, [clusterInt:0x0300, commandInt:0x0A, attrInt:0x0003] }: // Report Attributes Response: CurrentX
        case { contains it, [clusterInt:0x0300, commandInt:0x0A, attrInt:0x0004] }: // Report Attributes Response: CurrentY
        case { contains it, [clusterInt:0x0300, commandInt:0x04] }: // Write Attribute Response (0x04)
            return
        
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
        
        // Events for capability.HealthCheck
        // ===================================================================================================================
        
        case { contains it, [clusterInt:0x0000, attrInt:0x0000] }:
            log_warn '... pong'
            return
        
        // Configuration for capability.PowerSource
        // ===================================================================================================================
        
        // Read Attributes Reponse: PowerSource
        case { contains it, [clusterInt:0x0000, commandInt:0x01, attrInt:0x0007] }:
            String powerSource = 'unknown'
        
            // PowerSource := { 0x00:Unknown, 0x01:MainsSinglePhase, 0x02:MainsThreePhase, 0x03:Battery, 0x04:DC, 0x05:EmergencyMainsConstantlyPowered, 0x06:EmergencyMainsAndTransferSwitch }
            switch (msg.value) {
                case ['01', '02', '05', '06']:
                    powerSource = 'mains'; break
                case '03':
                    powerSource = 'battery'; break
                case '04':
                    powerSource = 'dc'
            }
            utils_sendEvent name:'powerSource', value:powerSource, type:'digital', descriptionText:"Power source is ${powerSource}"
            utils_processedZclMessage 'Read Attributes Response', "PowerSource=${msg.value}"
            return
        
        // Events for capability.ZigbeeGroups
        // ===================================================================================================================
        
        // Get Group Membership Response Command
        case { contains it, [clusterInt:0x0004, commandInt:0x02, direction:'01'] }:
            Integer count = Integer.parseInt msg.data[1], 16
            Set<String> groupNames = []
            for (int pos = 0; pos < count; pos++) {
                String groupId = "${msg.data[pos * 2 + 3]}${msg.data[pos * 2 + 2]}"
                String groupName = GROUPS.containsKey(groupId) ? "<abbr title=\"0x${groupId}\">${GROUPS.get(groupId)}</abbr>" : "0x${groupId}"
                log_debug "Found group membership: ${groupName}"
                groupNames.add groupName
            }
            state.joinGrp = groupNames
            if (state.joinGrp.size() == 0) state.remove 'joinGrp'
            log_info "Current group membership: ${groupNames ?: 'None'}"
            return
        
        // Add Group Response
        case { contains it, [clusterInt:0x0004, commandInt:0x00, direction:'01'] }:
            String status = msg.data[0] == '00' ? 'SUCCESS' : (msg.data[0] == '8A' ? 'ALREADY_MEMBER' : 'FAILED')
            String groupId = "${msg.data[2]}${msg.data[1]}"
            String groupName = GROUPS.containsKey(groupId) ? "<abbr title=\"0x${groupId}\">${GROUPS.get(groupId)}</abbr>" : "0x${groupId}"
            utils_processedZclMessage 'Add Group Response', "Status=${status}, groupId=${groupId}, groupName=${groupName}"
            return
        
        // Leave Group Response
        case { contains it, [clusterInt:0x0004, commandInt:0x03, direction:'01'] }:
            String status = msg.data[0] == '00' ? 'SUCCESS' : (msg.data[0] == '8B' ? 'NOT_A_MEMBER' : 'FAILED')
            String groupId = "${msg.data[2]}${msg.data[1]}"
            String groupName = GROUPS.containsKey(groupId) ? "<abbr title=\"0x${groupId}\">${GROUPS.get(groupId)}</abbr>" : "0x${groupId}"
            utils_processedZclMessage 'Left Group Response', "Status=${status}, groupId=${groupId}, groupName=${groupName}"
            return

        // ---------------------------------------------------------------------------------------------------------------
        // Handle common messages (e.g.: received during pairing when we query the device for information)
        // ---------------------------------------------------------------------------------------------------------------

        // Device_annce: Welcome back! let's sync state.
        case { contains it, [endpointInt:0x00, clusterInt:0x0013, commandInt:0x00] }:
            log_warn '🙋‍♂️ Rejoined the Zigbee mesh. Syncing device state ...'
            utils_sendZigbeeCommands(refresh(true))
            return

        // Report/Read Attributes Response (Basic cluster)
        case { contains it, [clusterInt:0x0000, commandInt:0x01] }:
        case { contains it, [clusterInt:0x0000, commandInt:0x0A] }:
            utils_zigbeeDataValue(msg.attrInt, msg.value)
            msg.additionalAttrs?.each { utils_zigbeeDataValue(it.attrInt, it.value) }
            utils_processedZclMessage "${msg.commandInt == 0x0A ? 'Report' : 'Read'} Attributes Response", "cluster=0x${msg.cluster}, attribute=0x${msg.attrId}, value=${msg.value}"
            return

        // Mgmt_Leave_rsp
        case { contains it, [endpointInt:0x00, clusterInt:0x8034, commandInt:0x00] }:
            log_warn '💀 Device is leaving the Zigbee mesh. See you later, Aligator!'
            return

        // Ignore the following Zigbee messages
        case { contains it, [commandInt:0x0A, isClusterSpecific:false] }:              // ZCL: Attribute report we don't care about (configured by other driver)
        case { contains it, [commandInt:0x0B, isClusterSpecific:false] }:              // ZCL: Default Response
        case { contains it, [clusterInt:0x0003, commandInt:0x01] }:                    // ZCL: Identify Query Command
        case { contains it, [clusterInt:0x0003, commandInt:0x04] }:                    // ZCL: Write Attribute Response (IdentifyTime)
            utils_processedZclMessage 'Ignored', "endpoint=0x${msg.sourceEndpoint ?: msg.endpoint}, manufacturer=0x${msg.manufacturerId ?: '0000'}, cluster=0x${msg.clusterId ?: msg.cluster}, command=0x${msg.command}, data=${msg.data}"
            return

        case { contains it, [endpointInt:0x00, clusterInt:0x8001, commandInt:0x00] }:  // ZDP: IEEE_addr_rsp
        case { contains it, [endpointInt:0x00, clusterInt:0x8004, commandInt:0x00] }:  // ZDP: Simple_Desc_rsp
        case { contains it, [endpointInt:0x00, clusterInt:0x8005, commandInt:0x00] }:  // ZDP: Active_EP_rsp
        case { contains it, [endpointInt:0x00, clusterInt:0x0006, commandInt:0x00] }:  // ZDP: MatchDescriptorRequest
        case { contains it, [endpointInt:0x00, clusterInt:0x801F, commandInt:0x00] }:  // ZDP: Parent_annce_rsp
        case { contains it, [endpointInt:0x00, clusterInt:0x8021, commandInt:0x00] }:  // ZDP: Mgmt_Bind_rsp
        case { contains it, [endpointInt:0x00, clusterInt:0x8022, commandInt:0x00] }:  // ZDP: Mgmt_Unbind_rsp
        case { contains it, [endpointInt:0x00, clusterInt:0x8031, commandInt:0x00] }:  // ZDP: Mgmt_LQI_rsp
        case { contains it, [endpointInt:0x00, clusterInt:0x8032, commandInt:0x00] }:  // ZDP: Mgmt_Rtg_rsp
        case { contains it, [endpointInt:0x00, clusterInt:0x8038, commandInt:0x00] }:  // ZDP: Mgmt_NWK_Update_notify
            utils_processedZdpMessage 'Ignored', "endpoint=0x${msg.sourceEndpoint ?: msg.endpoint}, manufacturer=0x${msg.manufacturerId ?: '0000'}, cluster=0x${msg.clusterId ?: msg.cluster}, command=0x${msg.command}, data=${msg.data}"
            return

        // ---------------------------------------------------------------------------------------------------------------
        // Unexpected Zigbee message
        // ---------------------------------------------------------------------------------------------------------------
        default:
            log_error "🚩 Sent unexpected Zigbee message: description=${description}, msg=${msg}"
    }
}

// ===================================================================================================================
// Logging helpers (something like this should be part of the SDK and not implemented by each driver)
// ===================================================================================================================

private void log_debug(String message) {
    if (logLevel == '1') log.debug "${device.displayName} ${message.uncapitalize()}"
}
private void log_info(String message) {
    if (logLevel <= '2') log.info "${device.displayName} ${message.uncapitalize()}"
}
private void log_warn(String message) {
    if (logLevel <= '3') log.warn "${device.displayName} ${message.uncapitalize()}"
}
private void log_error(String message) {
    log.error "${device.displayName} ${message.uncapitalize()}"
}

// ===================================================================================================================
// Helper methods (keep them simple, keep them dumb)
// ===================================================================================================================

private void utils_sendZigbeeCommands(List<String> cmds) {
    if (cmds.empty) return
    List<String> send = delayBetween(cmds.findAll { !it.startsWith('delay') }, 1000)
    log_debug "◀ Sending Zigbee messages: ${send}"
    state.lastTx = now()
    sendHubCommand new hubitat.device.HubMultiAction(send, hubitat.device.Protocol.ZIGBEE)
}
private void utils_sendEvent(Map event) {
    boolean noInfo = event.remove('noInfo') == true
    if (!noInfo && (device.currentValue(event.name, true) != event.value || event.isStateChange)) {
        log_info "${event.descriptionText} [${event.type}]"
    } else {
        log_debug "${event.descriptionText} [${event.type}]"
    }
    sendEvent event
}
private void utils_dataValue(String key, String value) {
    if (value == null || value == '') return
    log_debug "Update data value: ${key}=${value}"
    updateDataValue key, value
}
private void utils_zigbeeDataValue(Integer attrInt, String value) {
    switch (attrInt) {
        case 0x0001: utils_dataValue 'application', value; return
        case 0x0003: utils_dataValue 'hwVersion', value; return
        case 0x0004: utils_dataValue 'manufacturer', value; return
        case 0x000A: utils_dataValue 'type', "${value ? (value.split('') as List).collate(2).collect { "${Integer.parseInt(it.join(), 16) as char}" }.join() : ''}"; return
        case 0x0005: utils_dataValue 'model', value; return
        case 0x4000: utils_dataValue 'softwareBuild', value; return
    }
}
private void utils_processedZclMessage(String type, String details) {
    log_debug "▶ Processed ZCL message: type=${type}, ${details}"
}
private void utils_processedZdpMessage(String type, String details) {
    log_debug "▶ Processed ZDO message: type=${type}, ${details}"
}
private String utils_payload(String value) {
    return value.replace('0x', '').split('(?<=\\G.{2})').reverse().join('')
}
private String utils_payload(Integer value, Integer size = 4) {
    return utils_payload(Integer.toHexString(value).padLeft(size, '0'))
}

// switch/case syntactic sugar
@CompileStatic private boolean contains(Map msg, Map spec) {
    return msg.keySet().containsAll(spec.keySet()) && spec.every { it.value == msg[it.key] }
}
