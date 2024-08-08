/**
 * NodOn Lighting Relay Switch (SIN-4-2-20)
 *
 * @see https://dan-danache.github.io/hubitat/nodon-drivers/
 */
import groovy.transform.CompileStatic
import groovy.transform.Field
import com.hubitat.zigbee.DataType

@Field static final String DRIVER_NAME = 'NodOn Lighting Relay Switch (SIN-4-2-20)'
@Field static final String DRIVER_VERSION = '1.0.0'

// Fields for capability.MultiRelay
import com.hubitat.app.ChildDeviceWrapper
import com.hubitat.app.DeviceWrapper

// Fields for capability.PushableButton
@Field static final Map<String, List<String>> BUTTONS = [
    'SWITCH_1': ['1', 'Switch 1'],
    'SWITCH_2': ['2', 'Switch 2'],
]

// Fields for capability.HealthCheck
import groovy.time.TimeCategory

@Field static final Map<String, String> HEALTH_CHECK = [
    'schedule': '0 0 0/1 ? * * *', // Health will be checked using this cron schedule
    'thereshold': '3600' // When checking, mark the device as offline if no Zigbee message was received in the last 3600 seconds
]

metadata {
    definition(name:DRIVER_NAME, namespace:'dandanache', author:'Dan Danache', importUrl:'https://raw.githubusercontent.com/dan-danache/hubitat/master/nodon-drivers/NodOn_SIN-4-2-20.groovy') {
        capability 'Configuration'
        capability 'Refresh'
        capability 'Actuator'
        capability 'PushableButton'
        capability 'HealthCheck'
        capability 'PowerSource'

        fingerprint profileId:'0104', endpointId:'01', inClusters:'0000,0003,0004,0005,0006,0007,0008,1000,FC57', outClusters:'0003,0006,0019', model:'SIN-4-2-20', manufacturer:'NodOn', controllerType:'ZGB' // Firmware: 128B-0102-00010102
        
        // Attributes for capability.HealthCheck
        attribute 'healthStatus', 'enum', ['offline', 'online', 'unknown']
    }
    
    // Commands for capability.FirmwareUpdate
    command 'updateFirmware'

    preferences {
        input(
            name: 'helpInfo', type: 'hidden',
            title: '''
            <div style="min-height:55px; background:transparent url('https://dan-danache.github.io/hubitat/nodon-drivers/img/NodOn_SIN-4-2-20.webp') no-repeat left center;background-size:auto 55px;padding-left:60px">
                NodOn Lighting Relay Switch (SIN-4-2-20) <small>v1.0.0</small><br>
                <small><div>
                • <a href="https://dan-danache.github.io/hubitat/nodon-drivers/#nodon-lighting-relay-switch-sin-4-2-20" target="_blank">device details</a><br>
                • <a href="https://community.hubitat.com/t/release-nodon-drivers/123853" target="_blank">community page</a><br>
                </div></small>
            </div>
            '''
        )
        input(
            name: 'logLevel', type: 'enum',
            title: 'Log verbosity',
            description: '<small>Select what type of messages appear in the "Logs" section.</small>',
            options: ['1':'Debug - log everything', '2':'Info - log important events', '3':'Warning - log events that require attention', '4':'Error - log errors'],
            defaultValue: '1',
            required: true
        )
    }
}

// ===================================================================================================================
// Implement default methods
// ===================================================================================================================

// Called when the device is first added
void installed() {
    log_warn 'Installing device ...'
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
    
    // Preferences for capability.HealthCheck
    schedule HEALTH_CHECK.schedule, 'healthCheck'

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
    log_warn "🎬 Configuring device${auto ? ' (auto)' : ''} ..."

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
    
    // Configuration for capability.MultiRelay
    cmds += "zdo bind 0x${device.deviceNetworkId} 0x01 0x01 0x0006 {${device.zigbeeId}} {}" // On/Off cluster (ep 0x01)
    cmds += "zdo bind 0x${device.deviceNetworkId} 0x02 0x01 0x0006 {${device.zigbeeId}} {}" // On/Off cluster (ep 0x02)
    
    cmds += "he cr 0x${device.deviceNetworkId} 0x01 0x0006 0x0000 0x10 0x0000 0x0258 {01} {}" // Report OnOff (bool) at least every 10 minutes (ep 0x01)
    cmds += "he cr 0x${device.deviceNetworkId} 0x02 0x0006 0x0000 0x10 0x0000 0x0258 {01} {}" // Report OnOff (bool) at least every 10 minutes (ep 0x02)
    
    // Configuration for capability.PushableButton
    Integer numberOfButtons = BUTTONS.count { true }
    sendEvent name:'numberOfButtons', value:numberOfButtons, descriptionText:"Number of buttons is ${numberOfButtons}"
    
    // Configuration for capability.HealthCheck
    sendEvent name:'healthStatus', value:'online', descriptionText:'Health status initialized to online'
    sendEvent name:'checkInterval', value:3600, unit:'second', descriptionText:'Health check interval is 3600 seconds'
    
    // Configuration for capability.PowerSource
    sendEvent name:'powerSource', value:'unknown', type:'digital', descriptionText:'Power source initialized to unknown'
    cmds += zigbee.readAttribute(0x0000, 0x0007) // PowerSource

    // Query Basic cluster attributes
    cmds += zigbee.readAttribute(0x0000, [0x0001, 0x0003, 0x0004, 0x4000]) // ApplicationVersion, HWVersion, ManufacturerName, SWBuildID
    cmds += zigbee.readAttribute(0x0000, [0x0005]) // ModelIdentifier
    utils_sendZigbeeCommands cmds

    log_info 'Configuration done; refreshing device current state in 7 seconds ...'
    runIn 7, 'refresh', [data:true]
}
/* groovylint-disable-next-line UnusedPrivateMethod */
private void autoConfigure() {
    log_warn "Detected that this device is not properly configured for this driver version (lastCx != ${DRIVER_VERSION})"
    configure true
}

// capability.Refresh
void refresh(boolean auto = false) {
    log_warn "🎬 Refreshing device state${auto ? ' (auto)' : ''} ..."

    List<String> cmds = []
    
    // Refresh for capability.MultiRelay
    cmds += zigbee.readAttribute(0x0006, 0x0000, [destEndpoint:0x01]) // OnOff (ep 0x01)
    cmds += zigbee.readAttribute(0x0006, 0x0000, [destEndpoint:0x02]) // OnOff (ep 0x02)
    utils_sendZigbeeCommands cmds
}

// Implementation for capability.MultiRelay
private ChildDeviceWrapper fetchChildDevice(Integer relayNumber) {
    ChildDeviceWrapper childDevice = getChildDevice("${device.deviceNetworkId}-${relayNumber}")
    return childDevice ?: addChildDevice('dandanache', 'NodOn Component Relay Switch', "${device.deviceNetworkId}-${relayNumber}", [name:"Component Relay Switch", label:"Relay #${relayNumber}", isComponent:true])
}

void componentOff(DeviceWrapper childDevice) {
    log_debug "▲ Received Off request from ${childDevice.displayName}"
    Integer endpointInt = Integer.parseInt(childDevice.deviceNetworkId.split('-')[1])
    log_debug "🎬 Sending Off command for ${childDevice.displayName}"
    utils_sendZigbeeCommands(["he raw 0x${device.deviceNetworkId} 0x01 0x0${endpointInt} 0x0006 {014100}"])
}

void componentOn(DeviceWrapper childDevice) {
    log_debug "▲ Received On request from ${childDevice.displayName}"
    Integer endpointInt = Integer.parseInt(childDevice.deviceNetworkId.split('-')[1])
    log_debug "🎬 Sending On command for ${childDevice.displayName}"
    utils_sendZigbeeCommands(["he raw 0x${device.deviceNetworkId} 0x01 0x0${endpointInt} 0x0006 {014201}"])
}

void componentToggle(DeviceWrapper childDevice) {
    log_debug "▲ Received Toggle request from ${childDevice.displayName}"
    Integer endpointInt = Integer.parseInt(childDevice.deviceNetworkId.split('-')[1])
    log_debug "🎬 Sending Toggle command for ${childDevice.displayName}"
    utils_sendZigbeeCommands(["he raw 0x${device.deviceNetworkId} 0x01 0x0${endpointInt} 0x0006 {014302}"])
}

void componentOnWithTimedOff(DeviceWrapper childDevice, BigDecimal onTime = 1) {
    log_debug "▲ Received OnWithTimedOff request from ${childDevice.displayName}"
    Integer endpointInt = Integer.parseInt(childDevice.deviceNetworkId.split('-')[1])
    log_debug "🎬 Sending OnWithTimedOff command for ${childDevice.displayName}"
    Integer delay = onTime < 1 ? 1 : (onTime > 6500 ? 6500 : onTime)
    Integer dur = delay * 10
    String payload = "00 ${utils_payload dur, 4} 0000"
    utils_sendZigbeeCommands(["he raw 0x${device.deviceNetworkId} 0x01 0x${device.endpointId} 0x0006 {114442 ${payload}}"])
}

void componentRefresh(DeviceWrapper childDevice) {
    log_debug "▲ Received Refresh request from ${childDevice.displayName}"
    refresh()
}

void componentUpdatePowerOnBehavior(DeviceWrapper childDevice, String powerOnBehavior) {
    log_debug "▲ Received UpdatePowerOnBehavior request from ${childDevice.displayName}"
    Integer endpointInt = Integer.parseInt(childDevice.deviceNetworkId.split('-')[1])
    log_debug "🎬 Sending WriteAttributes command for ${childDevice.displayName}"
    Integer attrValue = powerOnBehavior == 'TURN_POWER_OFF' ? 0x00 : (powerOnBehavior == 'TURN_POWER_ON' ? 0x01 : 0xFF)
    utils_sendZigbeeCommands(zigbee.writeAttribute(
        0x0006, 0x4003, 0x30, attrValue, [destEndpoint:endpointInt]
    ))
}

// Implementation for capability.PushableButton
void push(String buttonNumber) { push Integer.parseInt(buttonNumber) }
void push(BigDecimal buttonNumber) {
    String buttonName = BUTTONS.find { it.value[0] == "${buttonNumber}" }?.value?.getAt(1)
    if (buttonName == null) {
        log_warn "Cannot push button ${buttonNumber} because it is not defined"
        return
    }
    utils_sendEvent name:'pushed', value:buttonNumber, type:'digital', isStateChange:true, descriptionText:"Button ${buttonNumber} (${buttonName}) was pressed"
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
    log_info 'Instructing device to check for firmware updates ...'
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
    msg += zigbee.parseDescriptionAsMap description
    if (msg.containsKey('endpoint')) msg.endpointInt = Integer.parseInt msg.endpoint, 16
    if (msg.containsKey('sourceEndpoint')) msg.endpointInt = Integer.parseInt msg.sourceEndpoint, 16
    if (msg.containsKey('cluster')) msg.clusterInt = Integer.parseInt msg.cluster, 16
    if (msg.containsKey('command')) msg.commandInt = Integer.parseInt msg.command, 16
    if (msg.containsKey('manufacturerId')) msg.manufacturerInt = Integer.parseInt msg.manufacturerId, 16
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
        
        // Events for capability.MultiRelay
        // ===================================================================================================================
        
        // Report/Read Attributes: OnOff
        case { contains it, [clusterInt:0x0006, commandInt:0x0A, attrInt:0x0000] }:
        case { contains it, [clusterInt:0x0006, commandInt:0x01, attrInt:0x0000] }:
            Integer relayNumber = msg.endpointInt
            String newState = msg.value == '00' ? 'off' : 'on'
        
            // Send event to module child device (only if state needs to change)
            ChildDeviceWrapper childDevice = fetchChildDevice(relayNumber)
            if (newState != childDevice.currentValue('switch', true)) {
                childDevice.parse([[name:'switch', value:newState, descriptionText:"Was turned ${newState}", type:type]])
            }
        
            utils_processedZclMessage "${msg.commandInt == 0x0A ? 'Report' : 'Read'} Attributes Response", "Relay=${relayNumber}, Switch=${newState}"
            return
        
        // Other events that we expect but are not usefull
        case { contains it, [clusterInt:0x0006, commandInt:0x07] }:
            utils_processedZclMessage 'Configure Reporting Response', "attribute=OnOff, endpoint=${msg.endpointInt}, data=${msg.data}"
            return
        case { contains it, [clusterInt:0x0006, commandInt:0x04] }: // Write Attribute Response
            return
        
        // Events for devices.NodOn_SIN-4-1-20
        // ===================================================================================================================
        
        // Switch was pressed - OnOff cluster Toggle
        case { contains it, [clusterInt:0x0006, commandInt:0x02] }:
            List<String> button = msg.endpointInt == 1 ? BUTTONS.SWITCH_1 : BUTTONS.SWITCH_2
            utils_sendEvent name:'pushed', value:button[0], type:'physical', isStateChange:true, descriptionText:"Button ${button[0]} (${button[1]}) was pushed"
            return
        
        // Other events that we expect but are not usefull
        case { contains it, [clusterInt:0x0021] }: // ??
        case { contains it, [clusterInt:0x1000] }: // ??
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
            utils_processedZdpMessage 'Ignored', "endpoint=0x${msg.sourceEndpoint ?: msg.endpoint} (ZDP), manufacturer=0x${msg.manufacturerId ?: '0000'}, cluster=0x${msg.clusterId ?: msg.cluster}, command=0x${msg.command}, data=${msg.data}"
            return

        case { contains it, [endpointInt:0xF2] }:  // Zigbee Green Power
            utils_processedZdpMessage 'Ignored', "endpoint=0x${msg.sourceEndpoint ?: msg.endpoint} (Zigbee Green Power), manufacturer=0x${msg.manufacturerId ?: '0000'}, cluster=0x${msg.clusterId ?: msg.cluster}, command=0x${msg.command}, data=${msg.data}"
            return

        case { contains it, [commandInt:0x0A, isClusterSpecific:false] }:  // ZCL: Attribute report we don't care about (configured by other driver)
        case { contains it, [commandInt:0x0B, isClusterSpecific:false] }:  // ZCL: Default Response
        case { contains it, [clusterInt:0x0003, commandInt:0x01] }:        // ZCL: Identify Query Command
        case { contains it, [clusterInt:0x0003, commandInt:0x04] }:        // ZCL: Write Attribute Response (IdentifyTime)
            utils_processedZclMessage 'Ignored', "endpoint=0x${msg.sourceEndpoint ?: msg.endpoint} (ZCL), manufacturer=0x${msg.manufacturerId ?: '0000'}, cluster=0x${msg.clusterId ?: msg.cluster}, command=0x${msg.command}, data=${msg.data}"
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
