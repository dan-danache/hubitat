/**
 * IKEA Vindstyrka Air Quality Sensor (E2112)
 *
 * @see https://dan-danache.github.io/hubitat/ikea-zigbee-drivers/
 */
import groovy.transform.CompileStatic
import groovy.transform.Field

@Field static final String DRIVER_NAME = 'IKEA Vindstyrka Air Quality Sensor (E2112)'
@Field static final String DRIVER_VERSION = '4.0.0'

// Fields for capability.HealthCheck
import groovy.time.TimeCategory

@Field static final Map<String, String> HEALTH_CHECK = [
    'schedule': '0 0 0/1 ? * * *', // Health will be checked using this cron schedule
    'thereshold': '3600' // When checking, mark the device as offline if no Zigbee message was received in the last 3600 seconds
]

metadata {
    definition(name:DRIVER_NAME, namespace:'dandanache', author:'Dan Danache', importUrl:'https://raw.githubusercontent.com/dan-danache/hubitat/master/ikea-zigbee-drivers/Ikea_E2112.groovy') {
        capability 'Configuration'
        capability 'Refresh'
        capability 'Sensor'
        capability 'AirQuality'
        capability 'TemperatureMeasurement'
        capability 'RelativeHumidityMeasurement'
        capability 'HealthCheck'
        capability 'PowerSource'

        // For firmware: 1.0.010 (117C-110F-00010010)
        fingerprint profileId:'0104', endpointId:'01', inClusters:'0000,0003,0004,0402,0405,FC57,FC7C,042A,FC7E', outClusters:'0003,0019,0020,0202', model:'VINDSTYRKA', manufacturer:'IKEA of Sweden'
        
        // Attributes for E2112.FineParticulateMatter
        attribute 'airQuality', 'enum', ['good', 'moderate', 'unhealthy for sensitive groups', 'unhealthy', 'hazardous']
        attribute 'pm25', 'number'
        
        // Attributes for E2112.VocIndex
        attribute 'vocIndex', 'number'
        
        // Attributes for capability.HealthCheck
        attribute 'healthStatus', 'enum', ['offline', 'online', 'unknown']
    }
    
    // Commands for capability.FirmwareUpdate
    command 'updateFirmware'

    preferences {
        input(
            name: 'helpInfo', type: 'hidden',
            title: '''
            <div style="min-height:55px; background:transparent url('https://dan-danache.github.io/hubitat/ikea-zigbee-drivers/img/Ikea_E2112.webp') no-repeat left center;background-size:auto 55px;padding-left:60px">
                IKEA Vindstyrka Air Quality Sensor (E2112) <small>v4.0.0</small><br>
                <small><div>
                • <a href="https://dan-danache.github.io/hubitat/ikea-zigbee-drivers/#vindstyrka-air-quality-sensor-e2112" target="_blank">device details</a><br>
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
    
    // Configuration for E2112.FineParticulateMatter
    cmds += "zdo bind 0x${device.deviceNetworkId} 0x01 0x01 0x042A {${device.zigbeeId}} {}" // Particulate Matter 2.5 cluster
    cmds += "he cr 0x${device.deviceNetworkId} 0x01 0x042A 0x0000 0x39 0x000A 0x0258 {40000000} {}" // Report MeasuredValue (single) at least every 10 minutes (Δ = ??)
    
    // Configuration for E2112.VocIndex
    cmds += "zdo bind 0x${device.deviceNetworkId} 0x01 0x01 0xFC7E {${device.zigbeeId}} {}" // VocIndex Measurement cluster
    cmds += "he cr 0x${device.deviceNetworkId} 0x01 0xFC7E 0x0000 0x39 0x000A 0x0258 {40000000} {117C}" // Report MeasuredValue (single) at least every 10 minutes (Δ = ??)
    
    // Configuration for capability.Temperature
    cmds += "zdo bind 0x${device.deviceNetworkId} 0x${device.endpointId} 0x01 0x0402 {${device.zigbeeId}} {}" // Temperature Measurement cluster
    cmds += "he cr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0402 0x0000 0x29 0x0000 0x0258 {6400} {}" // Report MeasuredValue (int16) at least every 10 minutes (Δ = 1°C)
    
    // Configuration for capability.RelativeHumidity
    cmds += "zdo bind 0x${device.deviceNetworkId} 0x${device.endpointId} 0x01 0x0405 {${device.zigbeeId}} {}" // Relative Humidity Measurement cluster
    cmds += "he cr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0405 0x0000 0x21 0x0000 0x0258 {6400} {}" // Report MeasuredValue (uint16) at least every 10 minutes (Δ = 1%)
    
    // Configuration for capability.HealthCheck
    sendEvent name:'healthStatus', value:'online', descriptionText:'Health status initialized to online'
    sendEvent name:'checkInterval', value:3600, unit:'second', descriptionText:'Health check interval is 3600 seconds'
    
    // Configuration for capability.PowerSource
    sendEvent name:'powerSource', value:'unknown', type:'digital', descriptionText:'Power source initialized to unknown'
    cmds += zigbee.readAttribute(0x0000, 0x0007)  // PowerSource

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
    
    // Refresh for E2112.FineParticulateMatter
    cmds += zigbee.readAttribute(0x042A, 0x0000) // Fine Particulate Matter (PM25)
    
    // Refresh for E2112.VocIndex
    cmds += zigbee.readAttribute(0xFC7E, 0x0000, [mfgCode: '0x117C']) // VOC Index
    
    // Refresh for capability.Temperature
    cmds += zigbee.readAttribute(0x0402, 0x0000) // Temperature
    
    // Refresh for capability.RelativeHumidity
    cmds += zigbee.readAttribute(0x0405, 0x0000) // RelativeHumidity
    utils_sendZigbeeCommands cmds
}

// Implementation for E2112.FineParticulateMatter
private Integer lerp(Integer ylo, Integer yhi, BigDecimal xlo, BigDecimal xhi, Integer cur) {
    return Math.round(((cur - xlo) / (xhi - xlo)) * (yhi - ylo) + ylo)
}
private List pm25Aqi(Integer pm25) { // See: https://en.wikipedia.org/wiki/Air_quality_index#United_States
    if (pm25 <=  12.1) return [lerp(  0,  50,   0.0,  12.0, pm25), 'good', 'green']
    if (pm25 <=  35.5) return [lerp( 51, 100,  12.1,  35.4, pm25), 'moderate', 'gold']
    if (pm25 <=  55.5) return [lerp(101, 150,  35.5,  55.4, pm25), 'unhealthy for sensitive groups', 'darkorange']
    if (pm25 <= 150.5) return [lerp(151, 200,  55.5, 150.4, pm25), 'unhealthy', 'red']
    if (pm25 <= 250.5) return [lerp(201, 300, 150.5, 250.4, pm25), 'very unhealthy', 'purple']
    if (pm25 <= 350.5) return [lerp(301, 400, 250.5, 350.4, pm25), 'hazardous', 'maroon']
    if (pm25 <= 500.5) return [lerp(401, 500, 350.5, 500.4, pm25), 'hazardous', 'maroon']
    return [500, 'hazardous', 'maroon']
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
        
        // Events for E2112.FineParticulateMatter
        // ===================================================================================================================
        
        // Report/Read Attributes Reponse: MeasuredValue
        case { contains it, [clusterInt:0x042A, commandInt:0x0A, attrInt:0x0000] }:
        case { contains it, [clusterInt:0x042A, commandInt:0x01, attrInt:0x0000] }:
        
            // A MeasuredValue of 0xFFFFFFFF indicates that the measurement is invalid
            if (msg.value == 'FFFFFFFF') {
                log_warn "Ignored invalid PM25 value: 0x${msg.value}"
                return
            }
        
            Integer pm25 = Math.round Float.intBitsToFloat(Integer.parseInt(msg.value, 16))
            utils_sendEvent name:'pm25', value:pm25, unit:'μg/m³', descriptionText:"Fine particulate matter (PM2.5) concentration is ${pm25} μg/m³", type:type
            List aqi = pm25Aqi pm25
            utils_sendEvent name:'airQualityIndex', value:aqi[0], descriptionText:"Calculated Air Quality Index = ${aqi[0]}", type:type
            utils_sendEvent name:'airQuality', value:"<span style=\"color:${aqi[2]}\">${aqi[1]}</span>", descriptionText:"Calculated Air Quality = ${aqi[1]}", type:type
            utils_processedZclMessage "${msg.commandInt == 0x0A ? 'Report' : 'Read'} Attributes Response", "PM25Measurement=${pm25} μg/m³"
            return
        
        // Other events that we expect but are not usefull for E2112.FineParticulateMatter behavior
        case { contains it, [clusterInt:0x042A, commandInt:0x07] }:
            utils_processedZclMessage 'Configure Reporting Response', "attribute=pm25, data=${msg.data}"
            return
        
        // Events for E2112.VocIndex
        // ===================================================================================================================
        
        // Report/Read Attributes Reponse: MeasuredValue
        case { contains it, [clusterInt:0xFC7E, commandInt:0x0A, attrInt:0x0000] }:
        case { contains it, [clusterInt:0xFC7E, commandInt:0x01, attrInt:0x0000] }:
        
            // A MeasuredValue of 0xFFFFFFFF indicates that the measurement is invalid
            if (msg.value == 'FFFFFFFF') {
                log_warn "Ignored invalid VOC Index value: 0x${msg.value}"
                return
            }
        
            Integer vocIndex = Math.round Float.intBitsToFloat(Integer.parseInt(msg.value, 16))
            utils_sendEvent name:'vocIndex', value:vocIndex, descriptionText:"Voc index is ${vocIndex} / 500", type:type
            utils_processedZclMessage "${msg.commandInt == 0x0A ? 'Report' : 'Read'} Attributes Response", "VocIndex=${msg.value}"
            return
        
        // Other events that we expect but are not usefull for E2112.VocIndex behavior
        case { contains it, [clusterInt:0xFC7E, commandInt:0x07] }:
            utils_processedZclMessage 'Configure Reporting Response', "attribute=vocIndex, data=${msg.data}"
            return
        
        // Events for capability.Temperature
        // ===================================================================================================================
        
        // Report/Read Attributes Reponse: MeasuredValue
        case { contains it, [clusterInt:0x0402, commandInt:0x0A, attrInt:0x0000] }:
        case { contains it, [clusterInt:0x0402, commandInt:0x01, attrInt:0x0000] }:
        
            // A MeasuredValue of 0x8000 indicates that the temperature measurement is invalid
            if (msg.value == '8000') {
                log_warn "Ignored invalid temperature value: 0x${msg.value}"
                return
            }
        
            String temperature = convertTemperatureIfNeeded(Integer.parseInt(msg.value, 16) / 100, 'C', 0)
            utils_sendEvent name:'temperature', value:temperature, unit:"°${location.temperatureScale}", descriptionText:"Temperature is ${temperature} °${location.temperatureScale}", type:type
            utils_processedZclMessage "${msg.commandInt == 0x0A ? 'Report' : 'Read'} Attributes Response", "Temperature=${msg.value}"
            return
        
        // Other events that we expect but are not usefull for capability.Temperature behavior
        case { contains it, [clusterInt:0x0402, commandInt:0x07] }:
            utils_processedZclMessage 'Configure Reporting Response', "attribute=temperature, data=${msg.data}"
            return
        
        // Events for capability.RelativeHumidity
        // ===================================================================================================================
        
        // Report/Read Attributes Reponse: MeasuredValue
        case { contains it, [clusterInt:0x0405, commandInt:0x0A, attrInt:0x0000] }:
        case { contains it, [clusterInt:0x0405, commandInt:0x01, attrInt:0x0000] }:
        
            // A MeasuredValue of 0xFFFF indicates that the measurement is invalid
            if (msg.value == 'FFFF') {
                log_warn "Ignored invalid relative humidity value: 0x${msg.value}"
                return
            }
        
            Integer humidity = Math.round(Integer.parseInt(msg.value, 16) / 100)
            utils_sendEvent name:'humidity', value:humidity, unit:'%rh', descriptionText:"Relative humidity is ${humidity} %", type:type
            utils_processedZclMessage "${msg.commandInt == 0x0A ? 'Report' : 'Read'} Attributes Response", "RelativeHumidity=${msg.value}"
            return
        
        // Other events that we expect but are not usefull for capability.RelativeHumidity behavior
        case { contains it, [clusterInt:0x0405, commandInt:0x07] }:
            utils_processedZclMessage 'Configure Reporting Response', "attribute=humidity, data=${msg.data}"
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
