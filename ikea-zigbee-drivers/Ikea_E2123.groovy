/**
 * IKEA Symfonisk Sound Remote Gen2 (E2123)
 *
 * @see https://dan-danache.github.io/hubitat/ikea-zigbee-drivers/
 */
import groovy.transform.CompileStatic
import groovy.transform.Field

@Field static final String DRIVER_NAME = 'IKEA Symfonisk Sound Remote Gen2 (E2123)'
@Field static final String DRIVER_VERSION = '4.0.0'

// Fields for capability.HealthCheck
import groovy.time.TimeCategory

@Field static final Map<String, String> HEALTH_CHECK = [
    'schedule': '0 0 0/1 ? * * *', // Health will be checked using this cron schedule
    'thereshold': '43200' // When checking, mark the device as offline if no Zigbee message was received in the last 43200 seconds
]

// Fields for capability.PushableButton
@Field static final Map<String, List<String>> BUTTONS = [
    'PLAY': ['1', 'Play'],
    'PLUS': ['2', 'Plus'],
    'MINUS': ['3', 'Minus'],
    'NEXT': ['4', 'Next'],
    'PREV': ['5', 'Prev'],
    'DOT_1': ['6', '•'],
    'DOT_2': ['7', '••'],
]

// Fields for capability.ZigbeeBindings
@Field static final Map<String, String> GROUPS = [
    '9900':'Alfa', '9901':'Bravo', '9902':'Charlie', '9903':'Delta', '9904':'Echo', '9905':'Foxtrot', '9906':'Golf', '9907':'Hotel', '9908':'India', '9909':'Juliett', '990A':'Kilo', '990B':'Lima', '990C':'Mike', '990D':'November', '990E':'Oscar', '990F':'Papa', '9910':'Quebec', '9911':'Romeo', '9912':'Sierra', '9913':'Tango', '9914':'Uniform', '9915':'Victor', '9916':'Whiskey', '9917':'Xray', '9918':'Yankee', '9919':'Zulu'
]

metadata {
    definition(name:DRIVER_NAME, namespace:'dandanache', author:'Dan Danache', importUrl:'https://raw.githubusercontent.com/dan-danache/hubitat/master/ikea-zigbee-drivers/Ikea_E2123.groovy') {
        capability 'Configuration'
        capability 'Refresh'
        capability 'Battery'
        capability 'DoubleTapableButton'
        capability 'HealthCheck'
        capability 'HoldableButton'
        capability 'PowerSource'
        capability 'PushableButton'
        capability 'ReleasableButton'

        // For firmware: 1.0.012
        fingerprint profileId:'0104', endpointId:'01', inClusters:'0000,0001,0003,0020,1000,FC57', outClusters:'0003,0004,0006,0008,0019,1000,FC7F', model:'SYMFONISK sound remote gen2', manufacturer:'IKEA of Sweden'

        // For firmware: 1.0.35 (117C-110E-01000035)
        fingerprint profileId:'0104', endpointId:'01', inClusters:'0000,0001,0003,0020,1000,FC7C', outClusters:'0003,0004,0006,0008,0019,1000', model:'SYMFONISK sound remote gen2', manufacturer:'IKEA of Sweden'
        
        // Attributes for capability.HealthCheck
        attribute 'healthStatus', 'enum', ['offline', 'online', 'unknown']
    }
    
    // Commands for capability.FirmwareUpdate
    command 'updateFirmware'

    preferences {
        input(
            name: 'helpInfo', type: 'hidden',
            title: '''
            <div style="min-height:55px; background:transparent url('https://dan-danache.github.io/hubitat/ikea-zigbee-drivers/img/Ikea_E2123.webp') no-repeat left center;background-size:auto 55px;padding-left:60px">
                IKEA Symfonisk Sound Remote Gen2 (E2123) <small>v4.0.0</small><br>
                <small><div>
                • <a href="https://dan-danache.github.io/hubitat/ikea-zigbee-drivers/#symfonisk-sound-remote-gen2-e2123" target="_blank">device details</a><br>
                • <a href="https://community.hubitat.com/t/release-ikea-zigbee-drivers/123853" target="_blank">community page</a><br>
                </div></small>
            </div>
            '''
        )
        input(
            name: 'logLevel', type: 'enum',
            title: 'Log verbosity',
            description: '<small>Select what type of messages appear in the "Logs" section.</small>',
            options: [
                '1': 'Debug - log everything',
                '2': 'Info - log important events',
                '3': 'Warning - log events that require attention',
                '4': 'Error - log errors'
            ],
            defaultValue: '1',
            required: true
        )
        
        // Inputs for capability.ZigbeeBindings
        input(
            name: 'controlDevice', type: 'enum',
            title: 'Control Zigbee device',
            description: '<small>Select the target Zigbee device that will be <abbr title="Without involving the Hubitat hub" style="cursor:help">directly controlled</abbr> by this device.</small>',
            options: ['0000':'❌ Stop controlling all Zigbee devices', '----':'- - - -'] + retrieveSwitchDevices(),
            defaultValue: '----',
            required: false
        )
        input(
            name: 'controlGroup', type: 'enum',
            title: 'Control Zigbee group',
            description: '<small>Select the target Zigbee group that will be <abbr title="Without involving the Hubitat hub" style="cursor:help">directly controlled</abbr> by this device.</small>',
            options: ['0000':'❌ Stop controlling all Zigbee groups', '----':'- - - -'] + GROUPS,
            defaultValue: '----',
            required: false
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
}

// Called when the "Save Preferences" button is clicked
List<String> updated(boolean auto = false) {
    log_info "Saving preferences${auto ? ' (auto)' : ''} ..."
    List<String> cmds = []

    unschedule()

    if (logLevel == null) {
        logLevel = '1'
        device.updateSetting 'logLevel', [value:logLevel, type:'enum']
    }
    if (logLevel == '1') runIn 1800, 'logsOff'
    log_info "🛠️ logLevel = ${['1':'Debug', '2':'Info', '3':'Warning', '4':'Error'].get(logLevel)}"
    
    // Preferences for capability.HealthCheck
    schedule HEALTH_CHECK.schedule, 'healthCheck'
    
    // Preferences for capability.ZigbeeBindings
    if (controlDevice != null && controlDevice != '----') {
        if (controlDevice == '0000') {
            log_info '🛠️ Clearing all device bindings'
            state.stopControlling = 'devices'
        } else {
            log_info "🛠️ Adding binding to device #${controlDevice} for clusters [0x0006]"
    
            cmds += "he raw 0x${device.deviceNetworkId} 0x00 0x00 0x0021 {49 ${utils_payload "${device.zigbeeId}"} ${utils_payload "${device.endpointId}"} ${utils_payload '0x0006'} 03 ${utils_payload "${controlDevice}"} 01} {0x0000}" // Add device binding for cluster 0x0006
        }
        device.updateSetting 'controlDevice', [value:'----', type:'enum']
        cmds += "he raw 0x${device.deviceNetworkId} 0x00 0x00 0x0033 {57 00} {0x0000}"
    }
    
    if (controlGroup != null && controlGroup != '----') {
        if (controlGroup == '0000') {
            log_info '🛠️ Clearing all group bindings'
            state.stopControlling = 'groups'
        } else {
            log_info "🛠️ Adding binding to group ${controlGroup} for clusters [0x0006]"
            cmds += "he raw 0x${device.deviceNetworkId} 0x00 0x00 0x0021 {49 ${utils_payload "${device.zigbeeId}"} ${utils_payload "${device.endpointId}"} ${utils_payload '0x0006'} 01 ${utils_payload "${controlGroup}"}} {0x0000}" // Add group binding for cluster 0x0006
        }
        device.updateSetting 'controlGroup', [value:'----', type:'enum']
        cmds += "he raw 0x${device.deviceNetworkId} 0x00 0x00 0x0033 {57 00} {0x0000}"
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
    log_warn "Configuring device${auto ? ' (auto)' : ''} ..."
    if (!auto && device.currentValue('powerSource', true) == 'battery') {
        log_warn '[IMPORTANT] Click the "Configure" button immediately after pushing any button on the device in order to first wake it up!'
    }

    // Apply preferences first
    List<String> cmds = []
    cmds += updated true

    // Clear data (keep firmwareMT information though)
    device.data*.key.each { if (it != 'firmwareMT') device.removeDataValue it }

    // Clear state
    state.clear()
    state.lastTx = 0
    state.lastRx = 0
    state.lastCx = DRIVER_VERSION
    
    // Configuration for devices.Ikea_E2123
    cmds += "zdo bind 0x${device.deviceNetworkId} 0x${device.endpointId} 0x01 0x0006 {${device.zigbeeId}} {}" // On/Off cluster
    cmds += "zdo bind 0x${device.deviceNetworkId} 0x${device.endpointId} 0x01 0x0008 {${device.zigbeeId}} {}" // Level Control cluster
    cmds += "zdo bind 0x${device.deviceNetworkId} 0x${device.endpointId} 0x01 0xFC7F {${device.zigbeeId}} {}" // Unknown 64639 cluster --> For firmware 1.0.012
    cmds += "zdo bind 0x${device.deviceNetworkId} 0x${device.endpointId} 0x01 0xFC80 {${device.zigbeeId}} {}" // Heiman - Specific Scenes cluster --> For firmware 1.0.35
    cmds += "zdo bind 0x${device.deviceNetworkId} 0x${device.endpointId} 0x01 0xFC80 {${device.zigbeeId}} {}" // Heiman - Specific Scenes cluster --> For firmware 1.0.35
    
    // Configuration for capability.Battery
    cmds += "zdo bind 0x${device.deviceNetworkId} 0x${device.endpointId} 0x01 0x0001 {${device.zigbeeId}} {}" // Power Configuration cluster
    cmds += "he cr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0001 0x0021 0x20 0x0000 0x4650 {02} {}" // Report BatteryPercentage (uint8) at least every 5 hours (Δ = 1%)
    
    // Configuration for capability.HealthCheck
    sendEvent name:'healthStatus', value:'online', descriptionText:'Health status initialized to online'
    sendEvent name:'checkInterval', value:3600, unit:'second', descriptionText:'Health check interval is 3600 seconds'
    
    // Configuration for capability.PowerSource
    sendEvent name:'powerSource', value:'unknown', type:'digital', descriptionText:'Power source initialized to unknown'
    cmds += zigbee.readAttribute(0x0000, 0x0007)  // PowerSource
    
    // Configuration for capability.PushableButton
    Integer numberOfButtons = BUTTONS.count { true }
    sendEvent name:'numberOfButtons', value:numberOfButtons, descriptionText:"Number of buttons is ${numberOfButtons}"

    // Query Basic cluster attributes
    cmds += zigbee.readAttribute(0x0000, [0x0001, 0x0003, 0x0004, 0x0005, 0x000A, 0x4000]) // ApplicationVersion, HWVersion, ManufacturerName, ModelIdentifier, ProductCode, SWBuildID
    utils_sendZigbeeCommands cmds

    log_info 'Configuration done; refreshing device current state in 7 seconds ...'
    runIn 7, 'refresh', [data:true]
}
private void autoConfigure() {
    log_warn "Detected that this device is not properly configured for this driver version (lastCx != ${DRIVER_VERSION})"
    configure true
}

// capability.Refresh
void refresh(boolean auto = false) {
    log_warn "Refreshing device state${auto ? ' (auto)' : ''} ..."
    if (!auto && device.currentValue('powerSource', true) == 'battery') {
        log_warn '[IMPORTANT] Click the "Refresh" button immediately after pushing any button on the device in order to first wake it up!'
    }

    List<String> cmds = []
    
    // Refresh for capability.Battery
    cmds += zigbee.readAttribute(0x0001, 0x0021) // BatteryPercentage
    
    // Refresh for capability.ZigbeeBindings
    cmds += "he raw 0x${device.deviceNetworkId} 0x00 0x00 0x0033 {57 00} {0x0000}"  // Start querying the Bindings Table
    utils_sendZigbeeCommands cmds
}

// Implementation for capability.DoubleTapableButton
void doubleTap(BigDecimal buttonNumber) {
    String buttonName = BUTTONS.find { it.value[0] == "${buttonNumber}" }?.value?.getAt(1)
    if (buttonName == null) {
        log_warn "Cannot double tap button ${buttonNumber} because it is not defined"
        return
    }
    utils_sendEvent name:'doubleTapped', value:buttonNumber, type:'digital', isStateChange:true, descriptionText:"Button ${buttonNumber} (${buttonName}) was double tapped"
}

// Implementation for capability.HealthCheck
void ping() {
    log_warn 'ping ...'
    utils_sendZigbeeCommands(zigbee.readAttribute(0x0000, 0x0000))
    log_debug 'Ping command sent to the device; we\'ll wait 5 seconds for a reply ...'
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

// Implementation for capability.HoldableButton
void hold(BigDecimal buttonNumber) {
    String buttonName = BUTTONS.find { it.value[0] == "${buttonNumber}" }?.value?.getAt(1)
    if (buttonName == null) {
        log_warn "Cannot hold button ${buttonNumber} because it is not defined"
        return
    }
    utils_sendEvent name:'held', value:buttonNumber, type:'digital', isStateChange:true, descriptionText:"Button ${buttonNumber} (${buttonName}) was held"
}

// Implementation for capability.PushableButton
void push(BigDecimal buttonNumber) {
    String buttonName = BUTTONS.find { it.value[0] == "${buttonNumber}" }?.value?.getAt(1)
    if (buttonName == null) {
        log_warn "Cannot push button ${buttonNumber} because it is not defined"
        return
    }
    utils_sendEvent name:'pushed', value:buttonNumber, type:'digital', isStateChange:true, descriptionText:"Button ${buttonNumber} (${buttonName}) was pressed"
}

// Implementation for capability.ReleasableButton
void release(BigDecimal buttonNumber) {
    String buttonName = BUTTONS.find { it.value[0] == "${buttonNumber}" }?.value?.getAt(1)
    if (buttonName == null) {
        log_warn "Cannot release button ${buttonNumber} because it is not defined"
        return
    }
    utils_sendEvent name:'released', value:buttonNumber, type:'digital', isStateChange:true, descriptionText:"Button ${buttonNumber} (${buttonName}) was released"
}

// Implementation for capability.ZigbeeBindings
private Map<String, String> retrieveSwitchDevices() {
    try {
        List<Integer> switchDeviceIds = httpGet([uri:'http://127.0.0.1:8080/device/listJson?capability=capability.switch']) { it.data*.id }
        httpGet([uri:'http://127.0.0.1:8080/hub/zigbeeDetails/json']) { response ->
            response.data.devices
                .findAll { switchDeviceIds.contains(it.id) }
                .sort { it.name }
                .collectEntries { [(it.zigbeeId): it.name] }
        }
    } catch (Exception ex) {
        return ['ZZZZ': "Exception: ${ex}"]
    }
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
    if (description.startsWith('enroll request')) msg += [clusterInt:0x500, commandInt:0x01, isClusterSpecific:true]

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
        
        // Events for devices.Ikea_E2123
        // ===================================================================================================================
        
        // Play button was pushed
        case { contains it, [clusterInt:0x0006, commandInt:0x02] }:
            List<String> button = BUTTONS.PLAY
            utils_sendEvent name:'pushed', value:button[0], type:'physical', isStateChange:true, descriptionText:"Button ${button[0]} (${button[1]}) was pushed"
            return
        
        // Plus/Minus button was held
        case { contains it, [clusterInt:0x0008, commandInt:0x01] }:
            List<String> button = msg.data[0] == '00' ? BUTTONS.PLUS : BUTTONS.MINUS
            utils_sendEvent name:'held', value:button[0], type:'physical', isStateChange:true, descriptionText:"Button ${button[0]} (${button[1]}) was held"
            return
        
        // Next/Prev button was pushed
        case { contains it, [clusterInt:0x0008, commandInt:0x02] }:
            List<String> button = msg.data[0] == '00' ? BUTTONS.NEXT : BUTTONS.PREV
            utils_sendEvent name:'pushed', value:button[0], type:'physical', isStateChange:true, descriptionText:"Button ${button[0]} (${button[1]}) was pushed"
            return
        
        // Plus/Minus button was pushed
        case { contains it, [clusterInt:0x0008, commandInt:0x05] }:
            List<String> button = msg.data[0] == '00' ? BUTTONS.PLUS : BUTTONS.MINUS
            utils_sendEvent name:'pushed', value:button[0], type:'physical', isStateChange:true, descriptionText:"Button ${button[0]} (${button[1]}) was pushed"
            return
        
        // Undocumented cluster (0xFC7F) - Used by firmware 1.0.012 (20211214)
        case { contains it, [clusterInt:0xFC7F] }:
            List<String> button = msg.data[0] == '01' ? BUTTONS.DOT_1 : BUTTONS.DOT_2
        
            // 1 Dot / 2 Dots button was pushed
            if (msg.data[1] == '01') {
                utils_sendEvent name:'pushed', value:button[0], type:'physical', isStateChange:true, descriptionText:"Button ${button[0]} (${button[1]}) was pressed"
                return
            }
        
            // 1 Dot / 2 Dots button was double tapped
            if (msg.data[1] == '02') {
                utils_sendEvent name:'doubleTapped', value:button[0], type:'physical', isStateChange:true, descriptionText:"Button ${button[0]} (${button[1]}) was double tapped"
                return
            }
        
            // 1 Dot / 2 Dots button was held
            if (msg.data[1] == '03') {
                utils_sendEvent name:'held', value:button[0], type:'physical', isStateChange:true, descriptionText:"Button ${button[0]} (${button[1]}) was held"
                return
            }
        
        // Undocumented cluster (0xFC80) - Used by firmware 1.0.35 (20230411)
        case { contains it, [clusterInt:0xFC80] }:
            List<String> button = msg.sourceEndpoint == '02' ? BUTTONS.DOT_1 : BUTTONS.DOT_2
        
            switch (msg.commandInt) {
        
                // IGNORED: 1 Dot / 2 Dots button was pressed-down
                case 0x01:
                    log_debug "Button ${button[0]} (${button[1]}) was pressed-down (ignored as we wait for the next message to distinguish between click, double tap and hold)"
                    return
        
                // 1 Dot / 2 Dots button was held
                // Commands are issued in this order: 01 (key-down = ignored) -> 02 (button is held = update "held" attribute) -> 04 (button released = update "released" attribute)
                case 0x02:
                    utils_sendEvent name:'held', value:button[0], type:'physical', isStateChange:true, descriptionText:"Button ${button[0]} (${button[1]}) was held"
                    return
        
                // 1 Dot / 2 Dots button was pushed
                case 0x03:
                    utils_sendEvent name:'pushed', value:button[0], type:'physical', isStateChange:true, descriptionText:"Button ${button[0]} (${button[1]}) was pressed"
                    return
        
                // IGNORED: 1 Dot / 2 Dots button was released
                case 0x04:
                    utils_sendEvent name:'released', value:button[0], type:'physical', isStateChange:true, descriptionText:"Button ${button[0]} (${button[1]}) was released"
                    return
        
                // 1 Dot / 2 Dots button was double tapped
                case 0x06:
                    utils_sendEvent name:'doubleTapped', value:button[0], type:'physical', isStateChange:true, descriptionText:"Button ${button[0]} (${button[1]}) was double tapped"
                    return
            }
        
        // Events for capability.Battery
        // ===================================================================================================================
        
        // Report/Read Attributes Reponse: BatteryPercentage
        case { contains it, [clusterInt:0x0001, commandInt:0x0A, attrInt:0x0021] }:
        case { contains it, [clusterInt:0x0001, commandInt:0x01] }:
        
            // Hubitat fails to parse some Read Attributes Responses
            if (msg.value == null && msg.data != null && msg.data[0] == '21' && msg.data[1] == '00') {
                msg.value = msg.data[2]
            }
        
            // The value 0xff indicates an invalid or unknown reading
            if (msg.value == 'FF') {
                log_warn "Ignored invalid remaining battery percentage value: 0x${msg.value}"
                return
            }
        
            Integer percentage = Integer.parseInt(msg.value, 16)
            percentage =  percentage / 2
            utils_sendEvent name:'battery', value:percentage, unit:'%', descriptionText:"Battery is ${percentage}% full", type:type
            utils_processedZclMessage "${msg.commandInt == 0x0A ? 'Report' : 'Read'} Attributes Response", "BatteryPercentage=${percentage}%"
            return
        
        // Other events that we expect but are not usefull for capability.Battery behavior
        case { contains it, [clusterInt:0x0001, commandInt:0x07] }:
            utils_processedZclMessage 'Configure Reporting Response', "attribute=BatteryPercentage, data=${msg.data}"
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
                case '01':
                case '02':
                case '05':
                case '06':
                    powerSource = 'mains'
                    break
                case '03':
                    powerSource = 'battery'
                    break
                case '04':
                    powerSource = 'dc'
                    break
            }
            utils_sendEvent name:'powerSource', value:powerSource, type:'digital', descriptionText:"Power source is ${powerSource}"
            utils_processedZclMessage 'Read Attributes Response', "PowerSource=${msg.value}"
            return
        
        // Events for capability.ZigbeeBindings
        // ===================================================================================================================
        
        // Mgmt_Bind_rsp := { 08:Status, 08:BindingTableEntriesTotal, 08:StartIndex, 08:BindingTableEntriesIncluded, 112/168*n:BindingTableList }
        // BindingTableList: { 64:SrcAddr, 08:SrcEndpoint, 16:ClusterId, 08:DstAddrMode, 16/64:DstAddr, 0/08:DstEndpoint }
        // Example: [71, 00, 01, 00, 01,  C6, 9C, FE, FE, FF, F9, E3, B4,  01,  06, 00,  03,  E9, A6, C9, 17, 00, 6F, 0D, 00,  01]
        case { contains it, [endpointInt:0x00, clusterInt:0x8033] }:
            if (msg.data[1] != '00') {
                utils_processedZdpMessage 'Mgmt_Bind_rsp', "Status=FAILED, data=${msg.data}"
                return
            }
            Integer totalEntries = Integer.parseInt msg.data[2], 16
            Integer startIndex = Integer.parseInt msg.data[3], 16
            Integer includedEntries = Integer.parseInt msg.data[4], 16
            if (startIndex == 0) {
                state.remove 'ctrlDev'
                state.remove 'ctrlGrp'
            }
            if (includedEntries == 0) {
                utils_processedZdpMessage 'Mgmt_Bind_rsp', "totalEntries=${totalEntries}, startIndex=${startIndex}, includedEntries=${includedEntries}"
                return
            }
        
            Integer pos = 5
            Integer deleted = 0
            Map<String, String> allDevices = retrieveSwitchDevices()
            Set<String> devices = []
            Set<String> groups = []
            List<String> cmds = []
            for (int idx = 0; idx < includedEntries; idx++) {
                String srcDeviceId = msg.data[(pos)..(pos + 7)].reverse().join()
                String srcEndpoint = msg.data[pos + 8]
                String cluster = msg.data[(pos + 9)..(pos + 10)].reverse().join()
                String dstAddrMode = msg.data[pos + 11]
                if (dstAddrMode != '01' && dstAddrMode != '03') continue
        
                // Found device binding
                if (dstAddrMode == '03') {
                    String dstDeviceId = msg.data[(pos + 12)..(pos + 19)].reverse().join()
                    String dstEndpoint = msg.data[pos + 20]
                    String dstDeviceName = allDevices.getOrDefault(dstDeviceId, "Unknown (${dstDeviceId})")
                    pos += 21
        
                    // Remove all binds that are not targeting the hub
                    if (state.stopControlling == 'devices') {
                        if (dstDeviceId != "${location.hub.zigbeeEui}") {
                            log_debug "Removing binding for device ${dstDeviceName} on cluster 0x${cluster}"
                            cmds += "he raw 0x${device.deviceNetworkId} 0x00 0x00 0x0022 {49 ${utils_payload srcDeviceId} ${srcEndpoint} ${utils_payload cluster} 03 ${utils_payload dstDeviceId} ${dstEndpoint}} {0x0000}"
                            deleted++
                        }
                        continue
                    }
        
                    log_debug "Found binding for device ${dstDeviceName} on cluster 0x${cluster}"
                    devices.add dstDeviceName
                    continue
                }
        
                // Found group binding
                String dstGroupId = msg.data[(pos + 12)..(pos + 13)].reverse().join()
                String dstGroupName = GROUPS.getOrDefault(dstGroupId, "Unknown (${dstGroupId})")
        
                // Remove all group bindings
                if (state.stopControlling == 'groups') {
                    log_debug "Removing binding for group ${dstGroupName} on cluster 0x${cluster}"
                    cmds += "he raw 0x${device.deviceNetworkId} 0x00 0x00 0x0022 {49 ${utils_payload srcDeviceId} ${srcEndpoint} ${utils_payload cluster} 01 ${utils_payload dstGroupId}} {0x0000}"
                    deleted++
                } else {
                    log_debug "Found binding for group ${dstGroupName} on cluster 0x${cluster}"
                    groups.add dstGroupName
                }
                pos += 14
            }
        
            Set<String> ctrlDev = (state.ctrlDev ?: []).toSet()
            ctrlDev.addAll(devices.findAll { !it.startsWith('Unknown') })
            if (ctrlDev.size() > 0) state.ctrlDev = ctrlDev.unique()
        
            Set<String> ctrlGrp = (state.ctrlGrp ?: []).toSet()
            ctrlGrp.addAll(groups.findAll { !it.startsWith('Unknown') })
            if (ctrlGrp.size() > 0) state.ctrlGrp = ctrlGrp.unique()
            // Get next batch
            if (startIndex + includedEntries < totalEntries) {
                cmds += "he raw 0x${device.deviceNetworkId} 0x00 0x00 0x0033 {57 ${Integer.toHexString(startIndex + includedEntries - deleted).padLeft(2, '0')}} {0x0000}"
            } else {
                log_info "Current device bindings: ${state.ctrlDev ?: 'None'}"
                log_info "Current group bindings: ${state.ctrlGrp ?: 'None'}"
                state.remove 'stopControlling'
            }
            utils_sendZigbeeCommands cmds
            utils_processedZdpMessage 'Mgmt_Bind_rsp', "totalEntries=${totalEntries}, startIndex=${startIndex}, devices=${devices}, groups=${groups}"
            return

        // ---------------------------------------------------------------------------------------------------------------
        // Handle common messages (e.g.: received during pairing when we query the device for information)
        // ---------------------------------------------------------------------------------------------------------------

        // Device_annce: Welcome back! let's sync state.
        case { contains it, [endpointInt:0x00, clusterInt:0x0013, commandInt:0x00] }:
            log_warn 'Rejoined the Zigbee mesh; refreshing device state in 3 seconds ...'
            runIn 3, 'refresh'
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
            log_warn 'Device is leaving the Zigbee mesh. See you later, Aligator!'
            return

        // Ignore the following Zigbee messages
        case { contains it, [commandInt:0x0A, isClusterSpecific:false] }:              // ZCL: Attribute report we don't care about (configured by other driver)
        case { contains it, [commandInt:0x0B, isClusterSpecific:false] }:              // ZCL: Default Response
        case { contains it, [clusterInt:0x0003, commandInt:0x01] }:                    // ZCL: Identify Query Command
            utils_processedZclMessage 'Ignored', "endpoint=${msg.endpoint}, cluster=0x${msg.clusterId}, command=0x${msg.command}, data=${msg.data}"
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
            utils_processedZdpMessage 'Ignored', "cluster=0x${msg.clusterId}, command=0x${msg.command}, data=${msg.data}"
            return

        // ---------------------------------------------------------------------------------------------------------------
        // Unexpected Zigbee message
        // ---------------------------------------------------------------------------------------------------------------
        default:
            log_error "Sent unexpected Zigbee message: description=${description}, msg=${msg}"
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
    if (device.currentValue(event.name, true) != event.value || event.isStateChange) {
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

// switch/case syntactic sugar
@CompileStatic private boolean contains(Map msg, Map spec) {
    return msg.keySet().containsAll(spec.keySet()) && spec.every { it.value == msg[it.key] }
}
