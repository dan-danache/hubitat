/**
 * Swann One Key Fob (SWO-KEF1PA)
 *
 * @see https://dan-danache.github.io/hubitat/ikea-zigbee-drivers/
 */
import groovy.transform.CompileStatic
import groovy.transform.Field

@Field static final String DRIVER_NAME = 'Swann One Key Fob (SWO-KEF1PA)'
@Field static final String DRIVER_VERSION = '5.0.1'

// Fields for capability.IAS
import hubitat.zigbee.clusters.iaszone.ZoneStatus

// Fields for capability.HealthCheck
import groovy.time.TimeCategory

@Field static final Map<String, String> HEALTH_CHECK = [
    'schedule': '0 0 0/1 ? * * *', // Health will be checked using this cron schedule
    'thereshold': '43200' // When checking, mark the device as offline if no Zigbee message was received in the last 43200 seconds
]

// Fields for capability.PushableButton
@Field static final Map<String, List<String>> BUTTONS = [
    'PANIC': ['1', 'Panic'],
    'HOME': ['2', 'Home'],
    'AWAY': ['3', 'Away'],
    'NIGHT': ['4', 'Night'],
]

metadata {
    definition(name:DRIVER_NAME, namespace:'dandanache', author:'Dan Danache', importUrl:'https://raw.githubusercontent.com/dan-danache/hubitat/master/ikea-zigbee-drivers/Swann_SWO-KEF1PA.groovy') {
        capability 'Configuration'
        capability 'Refresh'
        capability 'Battery'
        capability 'HealthCheck'
        capability 'PushableButton'

        fingerprint profileId:'0104', endpointId:'01', inClusters:'0000,0003,0001,0500,0000', outClusters:'0003,0501', model:'SWO-KEF1PA', manufacturer:'SwannONe' // Firmware: TBD
        
        // Attributes for capability.IAS
        attribute 'ias', 'enum', ['enrolled', 'not enrolled']
        
        // Attributes for capability.Battery
        attribute 'lastBattery', 'date'
        
        // Attributes for capability.HealthCheck
        attribute 'healthStatus', 'enum', ['offline', 'online', 'unknown']
    }
    
    // Commands for capability.FirmwareUpdate
    command 'updateFirmware'

    preferences {
        input(
            name: 'helpInfo', type: 'hidden',
            title: '''
            <div style="min-height:55px; background:transparent url('https://dan-danache.github.io/hubitat/ikea-zigbee-drivers/img/Swann_SWO-KEF1PA.webp') no-repeat left center;background-size:auto 55px;padding-left:60px">
                Swann One Key Fob (SWO-KEF1PA) <small>v5.0.1</small><br>
                <small><div>
                • <a href="https://dan-danache.github.io/hubitat/ikea-zigbee-drivers/#swann-one-key-fob-swo-kef1pa" target="_blank">device details</a><br>
                • <a href="https://community.hubitat.com/t/release-ikea-zigbee-drivers/123853" target="_blank">community page</a><br>
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
    log_warn '[IMPORTANT] For battery-powered devices, make sure that you keep your device as close as you can (less than 2inch / 5cm) to your Hubitat hub for at least 30 seconds. Otherwise the device will successfully pair but it won\'t work properly!'
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
    if (!auto && device.currentValue('powerSource', true) == 'battery') {
        log_warn '[IMPORTANT] Click the "Configure" button immediately after pushing any button on the device in order to first wake it up!'
    }

    // Apply preferences first
    List<String> cmds = ["he raw 0x${device.deviceNetworkId} 0x01 0x01 0x0003 {100002 0000213C00}"]
    cmds += updated true

    // Clear data (keep firmwareMT information though)
    device.data*.key.each { if (it != 'firmwareMT') device.removeDataValue it }

    // Clear state
    state.clear()
    state.lastTx = 0
    state.lastRx = 0
    state.lastCx = DRIVER_VERSION
    
    // Configuration for devices.Swann_SWO-KEF1PA
    cmds += "zdo bind 0x${device.deviceNetworkId} 0x${device.endpointId} 0x01 0x0501 {${device.zigbeeId}} {}" // IAS Ancillary Control Equipment cluster
    
    // Configuration for capability.IAS
    Integer ep0500 = 0x01
    cmds += "he wattr 0x${device.deviceNetworkId} ${ep0500} 0x0500 0x0010 0xF0 {${utils_payload "${location.hub.zigbeeEui}"}}"
    cmds += "he raw 0x${device.deviceNetworkId} 0x01 ${ep0500} 0x0500 {01 23 00 00 00}" // Zone Enroll Response (0x00): status=Success, zoneId=0x00
    cmds += "zdo bind 0x${device.deviceNetworkId} ${ep0500} 0x01 0x0500 {${device.zigbeeId}} {}" // IAS Zone cluster
    cmds += "he cr 0x${device.deviceNetworkId} ${ep0500} 0x0500 0x0002 0x19 0x0000 0x4650 {00} {}" // Report ZoneStatus (map16) at least every 5 hours (Δ = 0)
    
    // Configuration for capability.Battery
    cmds += "zdo bind 0x${device.deviceNetworkId} 0x${device.endpointId} 0x01 0x0001 {${device.zigbeeId}} {}" // Power Configuration cluster
    cmds += "he cr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0001 0x0021 0x20 0x0000 0x4650 {02} {}" // Report BatteryPercentage (uint8) at least every 5 hours (Δ = 1%)
    
    // Configuration for capability.HealthCheck
    sendEvent name:'healthStatus', value:'online', descriptionText:'Health status initialized to online'
    sendEvent name:'checkInterval', value:3600, unit:'second', descriptionText:'Health check interval is 3600 seconds'
    
    // Configuration for capability.PushableButton
    Integer numberOfButtons = BUTTONS.count { true }
    sendEvent name:'numberOfButtons', value:numberOfButtons, descriptionText:"Number of buttons is ${numberOfButtons}"

    // Query Basic cluster attributes
    cmds += zigbee.readAttribute(0x0000, [0x0001, 0x0003, 0x0004, 0x4000]) // ApplicationVersion, HWVersion, ManufacturerName, SWBuildID
    cmds += zigbee.readAttribute(0x0000, [0x0005]) // ModelIdentifier
    cmds += zigbee.readAttribute(0x0000, [0x000A]) // ProductCode
    cmds += "he raw 0x${device.deviceNetworkId} 0x01 0x01 0x0003 {100002 0000210000}"
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
    if (!auto && device.currentValue('powerSource', true) == 'battery') {
        log_warn '[IMPORTANT] Click the "Refresh" button immediately after pushing any button on the device in order to first wake it up!'
    }

    List<String> cmds = []
    
    // Refresh for capability.IAS
    Integer ep0500 = 0x01
    cmds += zigbee.readAttribute(0x0500, 0x0000, [destEndpoint: ep0500]) // IAS ZoneState
    cmds += zigbee.readAttribute(0x0500, 0x0001, [destEndpoint: ep0500]) // IAS ZoneType
    cmds += zigbee.readAttribute(0x0500, 0x0002, [destEndpoint: ep0500]) // IAS ZoneStatus
    
    // Refresh for capability.Battery
    cmds += zigbee.readAttribute(0x0001, 0x0021) // BatteryPercentage
    utils_sendZigbeeCommands cmds
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
        
        // Events for devices.Swann_SWO-KEF1PA
        // ===================================================================================================================
        
        // Arm := { 16:Button, 08:ArmMode, ??:ArmDisarmCode, 08:ZoneId}
        // ArmMode := { 0x00:Disarm, 0x01:Arm Day/Home Zones Only, 0x02:Arm Night/Sleep Zones Only, 0x03:Arm All Zones }
        // [00, 00, 00, 00, 00, 00, 00, 00, 00, 00] -> Home button
        // [02, 00, 00, 00, 00, 00, 00, 00, 00, 00] -> Night button
        // [03, 00, 00, 00, 00, 00, 00, 00, 00, 00] -> Away button
        case { contains it, [clusterInt:0x0501, commandInt:0x00, isClusterSpecific:true] }:
            switch (msg.data[0]) {
                case '00':
                    List<String> button = BUTTONS.HOME
                    utils_sendEvent name:'pushed', value:button[0], type:'physical', isStateChange:true, descriptionText:"Button ${button[0]} (${button[1]}) was pushed"
                    return
        
                case '02':
                    List<String> button = BUTTONS.NIGHT
                    utils_sendEvent name:'pushed', value:button[0], type:'physical', isStateChange:true, descriptionText:"Button ${button[0]} (${button[1]}) was pushed"
                    return
        
                case '03':
                    List<String> button = BUTTONS.AWAY
                    utils_sendEvent name:'pushed', value:button[0], type:'physical', isStateChange:true, descriptionText:"Button ${button[0]} (${button[1]}) was pushed"
                    return
            }
        
            log_error "Sent unexpected Zigbee message: description=${description}, msg=${msg}"
            return
        
        // Panic
        case { contains it, [clusterInt:0x0501, commandInt:0x04, isClusterSpecific:true] }:
            List<String> button = BUTTONS.PANIC
            utils_sendEvent name:'pushed', value:button[0], type:'physical', isStateChange:true, descriptionText:"Button ${button[0]} (${button[1]}) was pushed"
            return
        
        // Read Attributes: ZoneStatus
        case { contains it, [clusterInt:0x0500, commandInt:0x01, attrInt:0x0002] }:
            utils_processedZclMessage 'Read Attributes Response', "ZoneStatus=${msg.value}"
            return
        
        // Events for capability.IAS
        // ===================================================================================================================
        
        // Zone Status Change Notification
        case { contains it, [clusterInt:0x500, commandInt:0x00, isClusterSpecific:true] }:
            ZoneStatus zs = zigbee.parseZoneStatus(description)
            boolean alarm1             = zs.alarm1Set
            boolean alarm2             = zs.alarm2Set
            boolean tamper             = zs.tamperSet
            boolean lowBattery         = zs.batterySet
            boolean supervisionReports = zs.supervisionReportsSet
            boolean restoreReports     = zs.restoreReportsSet
            boolean trouble            = zs.troubleSet
            boolean mainsFault         = zs.acSet
            boolean testMode           = zs.testSet
            boolean batteryDefect      = zs.batteryDefectSet
            utils_processedZclMessage 'Zone Status Change Notification', "alarm1=${alarm1} alarm2=${alarm2} tamper=${tamper} lowBattery=${lowBattery} supervisionReports=${supervisionReports} restoreReports=${restoreReports} trouble=${trouble} mainsFault=${mainsFault} testMode=${testMode} batteryDefect=${batteryDefect}"
            return
        
        // Enroll Request
        case { contains it, [clusterInt:0x500, commandInt:0x01, isClusterSpecific:true] }:
            Integer ep0500 = 0x01
            utils_sendZigbeeCommands([
                "he raw 0x${device.deviceNetworkId} 0x01 ${ep0500} 0x0500 {01 23 00 00 00}", // Zone Enroll Response (0x00): status=Success, zoneId=0x00
                "he raw 0x${device.deviceNetworkId} 0x01 ${ep0500} 0x0500 {01 23 01}", // Initiate Normal Operation Mode (0x01): no_payload
            ])
            utils_processedZclMessage 'Enroll Request', "description=${description}"
            return
        
        // Read Attributes: ZoneState
        case { contains it, [clusterInt:0x0500, commandInt:0x01, attrInt:0x0000] }:
            String status = msg.value == '01' ? 'enrolled' : 'not enrolled'
            utils_sendEvent name:'ias', value:status, descriptionText:"Device IAS status is ${status}", type:'digital'
            utils_processedZclMessage 'Read Attributes Response', "ZoneState=${msg.value == '01' ? 'enrolled' : 'not_enrolled'}"
            return
        
        // Read Attributes: ZoneType
        case { contains it, [clusterInt:0x0500, commandInt:0x01, attrInt:0x0001] }:
            utils_processedZclMessage 'Read Attributes Response', "ZoneType=${msg.value}"
            return
        
        // Other events that we expect but are not usefull
        case { contains it, [clusterInt:0x0500, commandInt:0x04, isClusterSpecific:false] }:
            utils_processedZclMessage 'Write Attribute Response', "attribute=IAS_CIE_Address, ZoneType=${msg.data}"
            return
        
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
        
            Integer percentage = Integer.parseInt(msg.value, 16) / 2
            Date lastBattery = new Date()
            utils_sendEvent name:'battery', value:percentage, unit:'%', descriptionText:"Battery is ${percentage}% full", type:type
            utils_sendEvent name:'lastBattery', value:lastBattery, descriptionText:"Last battery report time is ${lastBattery}", type:type
            utils_processedZclMessage "${msg.commandInt == 0x0A ? 'Report' : 'Read'} Attributes Response", "BatteryPercentage=${percentage}%"
            return
        
        // Other events that we expect but are not usefull
        case { contains it, [clusterInt:0x0001, commandInt:0x07] }:
            utils_processedZclMessage 'Configure Reporting Response', "attribute=BatteryPercentage, data=${msg.data}"
            return
        
        // Events for capability.HealthCheck
        // ===================================================================================================================
        
        case { contains it, [clusterInt:0x0000, attrInt:0x0000] }:
            log_warn '... pong'
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
