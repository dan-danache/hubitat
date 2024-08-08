/**
 * Schneider Wiser UFH (CCTFR6600)
 *
 * @see https://dan-danache.github.io/hubitat/ikea-zigbee-drivers/
 */
import groovy.transform.CompileStatic
import groovy.transform.Field

@Field static final String DRIVER_NAME = 'Schneider Wiser UFH (CCTFR6600)'
@Field static final String DRIVER_VERSION = '5.0.1'

// Fields for capability.HealthCheck
import groovy.time.TimeCategory

@Field static final Map<String, String> HEALTH_CHECK = [
    'schedule': '0 0 0/1 ? * * *', // Health will be checked using this cron schedule
    'thereshold': '3600' // When checking, mark the device as offline if no Zigbee message was received in the last 3600 seconds
]

// Fields for devices.Schneider_CCTFR6600
@Field static final List<String> ZONES = [
    'zone_1', 'zone_2', 'zone_3', 'zone_4', 'zone_5', 'zone_6'
]

metadata {
    definition(name:DRIVER_NAME, namespace:'dandanache', author:'Dan Danache', importUrl:'https://raw.githubusercontent.com/dan-danache/hubitat/master/ikea-zigbee-drivers/Schneider_CCTFR6600.groovy') {
        capability 'Configuration'
        capability 'Refresh'
        capability 'HealthCheck'
        capability 'PowerSource'
        capability 'Actuator'
        capability 'SignalStrength'

        fingerprint profileId:'0104', endpointId:'01', inClusters:'0000,0003,0006,0201,0B05,FE03,FF16', outClusters:'0003,0019', model:'UFH', manufacturer:'Schneider Electric' // Firmware: 9ae667b (105E-0A00-00007D00)
        
        // Attributes for capability.HealthCheck
        attribute 'healthStatus', 'enum', ['offline', 'online', 'unknown']
        
        // Attributes for devices.Schneider_CCTFR6600
        attribute 'zone_1', 'enum', ['on', 'off']
        attribute 'zone_2', 'enum', ['on', 'off']
        attribute 'zone_3', 'enum', ['on', 'off']
        attribute 'zone_4', 'enum', ['on', 'off']
        attribute 'zone_5', 'enum', ['on', 'off']
        attribute 'zone_6', 'enum', ['on', 'off']
        attribute 'pump', 'enum', ['on', 'off']
        attribute 'boiler', 'enum', ['on', 'off']
        attribute 'debug', 'string'
    }
    
    // Commands for devices.Schneider_CCTFR6600
    command 'on', [[name:'Zone*', type:'ENUM', description:'Zone to turn On', constraints:ZONES]]
    command 'off', [[name:'Zone*', type:'ENUM', description:'Zone to turn Off', constraints:ZONES]]
    command 'exec', [[name:'Zigbee command', description:'Enter raw command to execute (e.g. for toggle on/off: he raw .addr 0x01 0x01 0x0006 {114302})', type:'STRING']]

    preferences {
        input(
            name: 'helpInfo', type: 'hidden',
            title: '''
            <div style="min-height:55px; background:transparent url('https://dan-danache.github.io/hubitat/ikea-zigbee-drivers/img/Schneider_CCTFR6600.webp') no-repeat left center;background-size:auto 55px;padding-left:60px">
                Schneider Wiser UFH (CCTFR6600) <small>v5.0.1</small><br>
                <small><div>
                • <a href="https://dan-danache.github.io/hubitat/ikea-zigbee-drivers/#schneider-wiser-ufh-cctrf6600" target="_blank">device details</a><br>
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
    
    // Configuration for capability.HealthCheck
    sendEvent name:'healthStatus', value:'online', descriptionText:'Health status initialized to online'
    sendEvent name:'checkInterval', value:3600, unit:'second', descriptionText:'Health check interval is 3600 seconds'
    
    // Configuration for capability.PowerSource
    sendEvent name:'powerSource', value:'unknown', type:'digital', descriptionText:'Power source initialized to unknown'
    cmds += zigbee.readAttribute(0x0000, 0x0007) // PowerSource
    
    // Configuration for Schneider_CCTFR6600.Switch
    ZONES.each { off it }
    
    cmds += "zdo bind 0x${device.deviceNetworkId} 0x07 0x01 0x0006 {${device.zigbeeId}} {}" // Pump endpoint
    //cmds += "he cr 0x${device.deviceNetworkId} 0x07 0x0006 0x0000 0x10 0x0000 0x0258 {01} {}" // Report pump status at least every 10 minutes
    
    cmds += "zdo bind 0x${device.deviceNetworkId} 0x08 0x01 0x0006 {${device.zigbeeId}} {}" // Boiler endpoint
    //cmds += "he cr 0x${device.deviceNetworkId} 0x08 0x0006 0x0000 0x10 0x0000 0x0258 {01} {}" // Report boiler status at least every 10 minutes
    
    ZONES.each {
        cmds += "he cr 0x${device.deviceNetworkId} 0x0${it.substring(5)} 0x0006 0x0000 0x10 0x0000 0x0258 {01} {}" // Report zones[1-6] status at least every 10 minutes
    }

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
    
    // Refresh for devices.Schneider_CCTFR6600
    cmds += zigbee.readAttribute(0xFE03, 0x0020, [mfgCode: '0x105E']) // Wiser Debug Info
    
    ZONES.each { cmds += zigbee.readAttribute(0x0006, 0x0000, [destEndpoint:Integer.parseInt(it.substring(5))]) } // Zones[1-6] OnOff
    cmds += zigbee.readAttribute(0x0006, 0x0000, [destEndpoint:0x07]) // Pump OnOff
    cmds += zigbee.readAttribute(0x0006, 0x0000, [destEndpoint:0x08]) // Boiler OnOff
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

// Implementation for devices.Schneider_CCTFR6600
void on(String zone) {
    if (!ZONES.contains(zone)) {
        log_error "Invalid zone: ${zone}. Available zones: ${ZONES}"
        return
    }
    utils_sendEvent name:zone, value:'on', descriptionText:"Zone ${zone} was turned on", type:'digital'
    utils_sendZigbeeCommands(["he raw 0x${device.deviceNetworkId} 0x01 0x0${zone.substring(5)} 0x0006 {014301}"])
}

void off(String zone) {
    if (!ZONES.contains(zone)) {
        log_error "Invalid zone: ${zone}. Available zones: ${ZONES}"
        return
    }
    utils_sendEvent name:zone, value:'off', descriptionText:"Zone ${zone} was turned off", type:'digital'
    utils_sendZigbeeCommands(["he raw 0x${device.deviceNetworkId} 0x01 0x0${zone.substring(5)} 0x0006 {014300}"])
}

void exec(String command) {
    log_info "Exec: ${command}"
    String cmd = command.replace '.addr', "0x${device.deviceNetworkId}"
    utils_sendZigbeeCommands([cmd])
}

private String attrValue(Integer attr, Integer type, Integer value, Integer bytes) {
    return "${utils_payload attr, 4}00${utils_payload type, 2}${utils_payload value, bytes}"
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
        
        // Events for devices.Schneider_CCTFR6600
        // ===================================================================================================================
        
        // Read Attributes: ZCL Version
        case { contains it, [clusterInt:0x0000, commandInt:0x00, data:['00', '00']] }:
            Integer frameControl = 0x08
            Integer txSeq = 0x00
            Integer command = 0x01 // Read Attributes Response
            String payload = attrValue(0x0000, 0x20, 0x03, 2) // ZCL Version = 0x03
        
            // Send response only once every 5 minutes
            //if (Calendar.getInstance().get(Calendar.MINUTE) % 5 == 0)
            utils_sendZigbeeCommands(["he raw 0x${device.deviceNetworkId} 0x01 0x${device.endpointId} 0x0000 {${utils_payload frameControl, 2}${utils_payload txSeq, 2}${utils_payload command, 2} ${payload}}"])
            utils_processedZclMessage '😊 Read Attributes (health check)', "endpoint=${msg.sourceEndpoint}, cluster=Basic, attr=0000 (ZCL Version)"
            return
        
        // ▶ Processed ZCL message: type=Read Attributes, endpoint=03, manufacturer=0000, cluster=0006, attrs=[0000]
        case { contains it, [clusterInt:0x0006, commandInt:0x00, data:['00', '00']] }:
            String zone = "zone_${Integer.parseInt msg.sourceEndpoint, 16}"
            boolean status = device.currentValue(zone, true) == 'on'
        
            Integer frameControl = Integer.parseInt('00001000', 2)
            Integer txSeq = 0x00
            Integer command = 0x01 // Read Attributes Response
            String payload = attrValue(0x0000, 0x10, status ? 0x01 : 0x00, 2)
            utils_sendZigbeeCommands(["he raw 0x${device.deviceNetworkId} 0x01 0x${msg.sourceEndpoint} 0x0006 {${utils_payload frameControl, 2}${utils_payload txSeq, 2}${utils_payload command, 2} ${payload}}"])
            utils_processedZclMessage '😊 Read Attributes', "endpoint=${msg.sourceEndpoint}, cluster=OnOff, attr=0000 (OnOff)"
            return
        
        // ▶ Processed ZCL message: type=Read Attributes, endpoint=03, manufacturer=105E, cluster=0006, attrs=[E002]
        case { contains it, [clusterInt:0x0006, commandInt:0x00, data:['02', 'E0']] }:
            Integer frameControl = Integer.parseInt('00001100', 2)
            Integer txSeq = 0x00
            Integer command = 0x01 // Read Attributes Response
            String payload = attrValue(0xE002, 0x10, 0x00, 2)
            Integer mfgCode = 0x105E
            utils_sendZigbeeCommands(["he raw 0x${device.deviceNetworkId} 0x01 0x${msg.sourceEndpoint} 0x0006 {${utils_payload frameControl, 2}${utils_payload mfgCode, 4}${utils_payload txSeq, 2}${utils_payload command, 2} ${payload}}"])
            utils_processedZclMessage '😊 Read Attributes', "endpoint=${msg.sourceEndpoint}, cluster=OnOff, attr=E002 (Unknown)"
            return
        
        // ▶ Processed ZCL message: type=Read Attributes, endpoint=03, manufacturer=105E, cluster=FF16, attrs=[0000, 0001, 0002, 0010, 0011, 0012, 0030]
        case { contains it, [clusterInt:0xFF16, commandInt:0x00, data:['00', '00', '01', '00', '02', '00', '10', '00', '11', '00', '12', '00', '30', '00']] }:
            Integer frameControl = Integer.parseInt('00001100', 2)
            Integer txSeq = 0x00
            Integer command = 0x01 // Read Attributes Response
            String payload = ''
            payload += attrValue(0x0000, 0x20, 0xC8, 2) + ' '   // 0x0000 : 0x64 = 100  (uint8)
            payload += '010086 '                                // 0x0001 : UNSUPPORTED_ATTRIBUTE
            payload += '020086 '                                // 0x0002 : UNSUPPORTED_ATTRIBUTE
            payload += attrValue(0x0010, 0x21, 0x04B0, 4) + ' ' // 0x0010 : 0258 = 600  (uint16)
            payload += attrValue(0x0011, 0x21, 0x0258, 4) + ' ' // 0x0011 : 012C = 300  (uint16)
            payload += attrValue(0x0012, 0x21, 0x1C20, 4) + ' ' // 0x0012 : 0E10 = 3600 (uint16)
            payload += attrValue(0x0030, 0x20, 0x42, 2)         // 0x0030 : 0x21 = 33   (uint8)
        
            Integer mfgCode = 0x105E
            utils_sendZigbeeCommands(["he raw 0x${device.deviceNetworkId} 0x01 0x${msg.sourceEndpoint} 0xFF16 {${utils_payload frameControl, 2}${utils_payload mfgCode, 4}${utils_payload txSeq, 2}${utils_payload command, 2} ${payload}}"])
            utils_processedZclMessage '😊 Read Attributes', "endpoint=${msg.sourceEndpoint}, cluster=FF16, attrs=[0000, 0001, 0002, 0010, 0011, 0012, 0030]"
            return
        
        // ▶ Processed ZCL message: type=Read Attributes, endpoint=01, manufacturer=0000, cluster=0201, attrs=[001C, 0015, 0016, 0017, 0018]
        case { contains it, [clusterInt:0x0201, commandInt:0x00, data:['1C', '00', '15', '00', '16', '00', '17', '00', '18', '00']] }:
            Integer frameControl = Integer.parseInt('00001100', 2)
            Integer txSeq = 0x00
            Integer command = 0x01 // Read Attributes Response
            String payload = ''
            payload += attrValue(0x001C, 0x30, 0x04, 2) + ' '   // 0x0000 : 0x03 = Cool  (enum8)     | 0x00:Off, 0x01:Auto, 0x03:Cool, 0x04:Heat, 0x05:Emergency heating
                                                                //                                   | 0x06:Precooling, 0x07:Fan only, 0x08:Dry, 0x09:Sleep
            payload += attrValue(0x0015, 0x29, 0x954D, 4) + ' ' // 0x0015 : 954D = -54.53°C (int16)  | Min Heat Setpoint Limit
            payload += attrValue(0x0016, 0x29, 0x7FFF, 4) + ' ' // 0x0016 : 7FFF = 327.67°C (int16)  | Max Heat Setpoint Limit
            payload += attrValue(0x0017, 0x29, 0x954D, 4) + ' ' // 0x0017 : 954D = -54.53°C (int16)  | Min Cool Setpoint Limit
            payload += attrValue(0x0038, 0x29, 0x7FFF, 4)       // 0x0018 : 7FFF = 327.67°C (int16)  | Max Cool Setpoint Limit
        
            Integer mfgCode = 0x105E
            utils_sendZigbeeCommands(["he raw 0x${device.deviceNetworkId} 0x01 0x01 0x0201 {${utils_payload frameControl, 2}${utils_payload mfgCode, 4}${utils_payload txSeq, 2}${utils_payload command, 2} ${payload}}"])
            utils_processedZclMessage '😊 Read Attributes', "endpoint=${msg.sourceEndpoint}, cluster=Thermostat, attrs=[001C, 0015, 0016, 0017, 0018]"
            return
        
        // Report/Read Attributes: Pump
        case { contains it, [endpointInt:0x07, clusterInt:0x0006, commandInt:0x0A, attrInt:0x0000] }:
        case { contains it, [endpointInt:0x07, clusterInt:0x0006, commandInt:0x01, attrInt:0x0000] }:
            String pump = msg.value == '00' ? 'off' : 'on'
            utils_sendEvent name:'pump', value:pump, descriptionText:"Pump was turned ${pump}", type:type
            utils_processedZclMessage "${msg.commandInt == 0x0A ? 'Report' : 'Read'} Attributes Response", "Pump=${pump}"
            return
        
        // Report/Read Attributes: Boiler
        case { contains it, [endpointInt:0x08, clusterInt:0x0006, commandInt:0x0A, attrInt:0x0000] }:
        case { contains it, [endpointInt:0x08, clusterInt:0x0006, commandInt:0x01, attrInt:0x0000] }:
            String boiler = msg.value == '00' ? 'off' : 'on'
            utils_sendEvent name:'boiler', value:boiler, descriptionText:"Boiler was turned ${boiler}", type:type
            utils_processedZclMessage "${msg.commandInt == 0x0A ? 'Report' : 'Read'} Attributes Response", "Boiler=${boiler}"
            return
        
        // Read Attributes: Zone 1-6
        case { contains it, [clusterInt:0x0006, commandInt:0x01, attrInt:0x0000] }:
            String zone = "zone_${msg.endpointInt}"
            String status = msg.value == '01' ? 'on' : 'off'
            utils_sendEvent name:zone, value:status, descriptionText:"Zone ${zone} is ${status}", type:'digital'
            utils_processedZclMessage 'Read Attributes Response', "${zone}=${status}"
            return
        
        // Read Attributes Reponse: Debug Info
        case { contains it, [clusterInt:0xFE03, commandInt:0x01, attrInt:0x0020] }:
        case { contains it, [clusterInt:0xFE03, commandInt:0x0A, attrInt:0x0020] }:
            utils_sendEvent name:'debug', value:msg.value, descriptionText:"Debug info is ${msg.value}", type:type
            utils_processedZclMessage "${msg.commandInt == 0x0A ? 'Report' : 'Read'} Attributes Response", "WiserDebugInfo=${msg.value}"
            return
        
        // Report Attributes: LastMessageLQI
        case { contains it, [clusterInt:0x0B05, commandInt:0x0A, attrInt:0x011C] }:
            Integer lqi = Integer.parseInt msg.value, 16
            utils_sendEvent name:'lqi', value:lqi, descriptionText:"Signal LQI is ${lqi}", type:'physical'
            msg.additionalAttrs.each {
                if (it.attrId == '011D') {
                    Integer rssi = Integer.parseInt it.value, 16
                    utils_sendEvent name:'rssi', value:rssi, descriptionText:"Signal RSSI is ${rssi}", type:'physical'
                    utils_processedZclMessage 'Report Attributes Response', "Diagnostics/LastMessageRSSI=${it.value}"
                }
            }
            utils_processedZclMessage 'Report Attributes Response', "Diagnostics/LastMessageLQI=${msg.value}"
            return
        
        // Report Unhandled Attributes
        case { contains it, [commandInt:0x0A] }:
            List<String> attrs = ["${msg.attrId}=${msg.value} (${msg.encoding})"]
            msg.additionalAttrs?.each { attrs += "${it.attrId}=${it.value} (${it.encoding})" }
            utils_processedZclMessage '👿 Report Attributes', "endpoint=${msg.sourceEndpoint ?: msg.endpoint}, manufacturer=${msg.manufacturerId ?: '0000'}, cluster=${msg.clusterId ?: msg.cluster}, attrs=${attrs}"
            return
        
        // Read Unhandled Attributes
        case { contains it, [commandInt:0x00] }:
            List<String> attrs = msg.data.collate(2).collect { "${it.reverse().join()}" }
            utils_processedZclMessage '👿 Read Attributes', "endpoint=${msg.sourceEndpoint ?: msg.endpoint}, manufacturer=${msg.manufacturerId ?: '0000'}, cluster=${msg.clusterId ?: msg.cluster}, attrs=${attrs}"
            return
        
        // Other events that we expect but are not usefull
        case { contains it, [endpointInt:0x07, clusterInt:0x0006, commandInt:0x07] }:
            utils_processedZclMessage 'Configure Reporting Response', "attribute=Pump, data=${msg.data}"
            return
        case { contains it, [endpointInt:0x08, clusterInt:0x0006, commandInt:0x07] }:
            utils_processedZclMessage 'Configure Reporting Response', "attribute=Boiler, data=${msg.data}"
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
