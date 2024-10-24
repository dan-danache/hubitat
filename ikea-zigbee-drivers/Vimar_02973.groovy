/**
 * Vimar IoT Dial Thermostat (02973)
 *
 * @see https://dan-danache.github.io/hubitat/ikea-zigbee-drivers/
 */
import java.math.RoundingMode
import groovy.transform.CompileStatic
import groovy.transform.Field
import com.hubitat.zigbee.DataType

@Field static final String DRIVER_NAME = 'Vimar IoT Dial Thermostat (02973)'
@Field static final String DRIVER_VERSION = '5.1.0'
@Field static final Map<String, String> TH_MODES = ['00':'off', '03':'cool', '04':'heat']
@Field static final Map<String, String> TH_STATES = ['00':'idle', '01':'heating', '02':'cooling']

// Fields for capability.HealthCheck
import groovy.time.TimeCategory

@Field static final Map<String, String> HEALTH_CHECK = [
    'schedule': '0 0 0/1 ? * * *', // Health will be checked using this cron schedule
    'thereshold': '3600' // When checking, mark the device as offline if no Zigbee message was received in the last 3600 seconds
]

metadata {
    definition(name:DRIVER_NAME, namespace:'dandanache', author:'Dan Danache', importUrl:'https://raw.githubusercontent.com/dan-danache/hubitat/master/ikea-zigbee-drivers/Vimar_02973.groovy') {
        capability 'Configuration'
        capability 'Refresh'
        capability 'Sensor'
        capability 'TemperatureMeasurement'
        capability 'ThermostatCoolingSetpoint'
        capability 'ThermostatHeatingSetpoint'
        capability 'ThermostatOperatingState'
        capability 'ThermostatMode'
        capability 'HealthCheck'
        capability 'PowerSource'

        fingerprint profileId:'0104', endpointId:'0A', inClusters:'0000,0003,0201', model:'WheelThermostat_v1.0', manufacturer:'Vimar', controllerType:'ZGB' // Firmware: 1.0.0_z
        
        // Attributes for capability.Thermostat
        attribute 'supportedThermostatModes', 'JSON_OBJECT'
        
        // Attributes for capability.HealthCheck
        attribute 'healthStatus', 'enum', ['offline', 'online', 'unknown']
    }
    
    // Commands for capability.Thermostat
    command 'setThermostatMode', [[name:'Thermostat mode*', type:'ENUM', description:'Thermostat mode to set', constraints:TH_MODES.values()]]

    preferences {
        input(
            name:'helpInfo', type:'hidden',
            title:'''
            <div style="min-height:55px; background:transparent url('https://dan-danache.github.io/hubitat/ikea-zigbee-drivers/img/Vimar_02973.webp') no-repeat left center;background-size:auto 55px;padding-left:60px">
                Vimar IoT Dial Thermostat (02973) <small>v5.1.0</small><br>
                <small><div>
                • <a href="https://dan-danache.github.io/hubitat/ikea-zigbee-drivers/#vimar-iot-dial-thermostat-02973" target="_blank">device details</a><br>
                • <a href="https://community.hubitat.com/t/release-ikea-zigbee-drivers/123853" target="_blank">community page</a><br>
                </div></small>
            </div>
            '''
        )
        input(
            name:'logLevel', type:'enum', title:'Log verbosity', required:true,
            description:'<small>Select what type of messages appear in the "Logs" section.</small>',
            options:['1':'Debug - log everything', '2':'Info - log important events', '3':'Warning - log events that require attention', '4':'Error - log errors'],
            defaultValue:'1'
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
    log_info "⚙️ Finishing device configuration ..."
    List<String> cmds = ["he raw 0x${device.deviceNetworkId} 0x01 0x01 0x0003 {014300 3C00}"]

    // Auto-apply preferences
    cmds += updated true
    
    // Configuration for capability.Thermostat
    sendEvent name:'supportedThermostatModes', value:TH_MODES.values(), type:'digital', descriptionText:"Supported thermostat modes initialized to ${TH_MODES.values()}"
    cmds += "zdo bind 0x${device.deviceNetworkId} 0x${device.endpointId} 0x01 0x0201 {${device.zigbeeId}} {}" // Thermostat cluster
    cmds += "he cr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0201 0x0000 0x29 0x000F 0x0E10 {1400} {}" // Report LocalTemperature (int16) at most every 15 seconds, at least every 1 hour (Δ = 0.2°C)
    
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
    log_info "🎬 Refreshing device state${auto ? ' (auto)' : ''} ..."
    if (!auto && device.currentValue('powerSource', true) == 'battery') {
        log_warn '[IMPORTANT] Click the "Refresh" button immediately after pushing any button on the device in order to first wake it up!'
    }

    List<String> cmds = []
    
    // Refresh for capability.Thermostat
    cmds += zigbee.readAttribute(0x0201, 0x0000) // LocalTemperature
    cmds += zigbee.readAttribute(0x0201, 0x0011) // OccupiedCoolingSetpoint
    cmds += zigbee.readAttribute(0x0201, 0x0012) // OccupiedHeatingSetpoint
    cmds += zigbee.readAttribute(0x0201, 0x001C) // SystemMode
    cmds += zigbee.readAttribute(0x0201, 0x0029) // ThermostatRunningState

    if (auto) return cmds
    utils_sendZigbeeCommands cmds
    return []
}

// Implementation for capability.Thermostat
void auto() {
    setThermostatMode 'auto'
}
void cool() {
    setThermostatMode 'cool'
}
void emergencyHeat() {
    setThermostatMode 'emergency heat'
}
void heat() {
    setThermostatMode 'heat'
}
void off() {
    setThermostatMode 'off'
}
void setThermostatMode(String mode) {
    log_debug "🎬 Setting thermostat mode to ${mode}"
    List<String> cmds = []
    switch (mode) {
        case 'off':
            cmds += zigbee.writeAttribute(0x0201, 0x001C, DataType.ENUM8, 0x00)
            break
        case 'cool':
            if (device.currentValue('thermostatMode', true) == 'off') {
                cmds += zigbee.writeAttribute(0x0201, 0x001B, DataType.ENUM8, 0x00)
                cmds += zigbee.writeAttribute(0x0201, 0x001C, DataType.ENUM8, 0x03)
            } else {
                cmds += zigbee.writeAttribute(0x0201, 0x001C, DataType.ENUM8, 0x00)
                cmds += zigbee.writeAttribute(0x0201, 0x001B, DataType.ENUM8, 0x00)
                runIn 5, 'systemMode', [data:0x03]
            }
            break
        case 'heat':
            if (device.currentValue('thermostatMode', true) == 'off') {
                cmds += zigbee.writeAttribute(0x0201, 0x001B, DataType.ENUM8, 0x02)
                cmds += zigbee.writeAttribute(0x0201, 0x001C, DataType.ENUM8, 0x04)
            } else {
                cmds += zigbee.writeAttribute(0x0201, 0x001C, DataType.ENUM8, 0x00)
                cmds += zigbee.writeAttribute(0x0201, 0x001B, DataType.ENUM8, 0x02)
                runIn 5, 'systemMode', [data:0x04]
            }
            break
        default:
            log_warn "Mode \"${mode}\" is not supported"
            return
    }
    utils_sendZigbeeCommands cmds
}
private void systemMode(Integer mode) {
    utils_sendZigbeeCommands(zigbee.writeAttribute(0x0201, 0x001C, DataType.ENUM8, mode))
}
void setCoolingSetpoint(BigDecimal temperature) {
    log_debug "🎬 Setting cooling setpoint to ${temperature}°${location.temperatureScale}"
    BigDecimal setpoint = "${location.temperatureScale}" == 'C' ? temperature : (temperature - 32) * 5 / 9
    utils_sendZigbeeCommands(zigbee.writeAttribute(0x0201, 0x0011, DataType.INT16, Integer.valueOf((setpoint * 100).intValue())))
}
void setHeatingSetpoint(BigDecimal temperature) {
    log_debug "🎬 Setting heating setpoint to ${temperature}°${location.temperatureScale}"
    BigDecimal setpoint = "${location.temperatureScale}" == 'C' ? temperature : (temperature - 32) * 5 / 9
    utils_sendZigbeeCommands(zigbee.writeAttribute(0x0201, 0x0012, DataType.INT16, Integer.valueOf((setpoint * 100).intValue())))
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
        
        // Events for capability.Thermostat
        // ===================================================================================================================
        
        // Report/Read Attributes Reponse: LocalTemperature
        case { contains it, [clusterInt:0x0201, commandInt:0x0A, attrInt:0x0000] }:
        case { contains it, [clusterInt:0x0201, commandInt:0x01, attrInt:0x0000] }:
        
            // https://www.urbandictionary.com/define.php?term=Retard%20Unit
            String temperature = "${location.temperatureScale == 'C' ? Integer.parseInt(msg.value, 16) / 100 : Math.round((Integer.parseInt(msg.value, 16) * 0.018 + 32) * 100) / 100}"
            utils_sendEvent name:'temperature', value:temperature, unit:"°${location.temperatureScale}", descriptionText:"Local temperature is ${temperature}°${location.temperatureScale}", type:type
            utils_processedZclMessage "${msg.commandInt == 0x0A ? 'Report' : 'Read'} Attributes Response", "LocalTemperature=${msg.value}"
            return
        
        // Report/Read Attributes Reponse: OccupiedCoolingSetpoint
        case { contains it, [clusterInt:0x0201, commandInt:0x0A, attrInt:0x0011] }:
        case { contains it, [clusterInt:0x0201, commandInt:0x01, attrInt:0x0011] }:
        
            // https://www.urbandictionary.com/define.php?term=Retard%20Unit
            String coolingSetpoint = "${location.temperatureScale == 'C' ? Integer.parseInt(msg.value, 16) / 100 : Math.round((Integer.parseInt(msg.value, 16) * 0.018 + 32) * 100) / 100}"
            utils_sendEvent name:'coolingSetpoint', value:coolingSetpoint, unit:"°${location.temperatureScale}", descriptionText:"Cooling setpoint is ${coolingSetpoint}°${location.temperatureScale}", type:type
            utils_processedZclMessage "${msg.commandInt == 0x0A ? 'Report' : 'Read'} Attributes Response", "OccupiedCoolingSetpoint=${msg.value}"
        
            utils_sendZigbeeCommands(zigbee.readAttribute(0x0201, 0x001C))
            return
        
        // Report/Read Attributes Reponse: OccupiedHeatingSetpoint
        case { contains it, [clusterInt:0x0201, commandInt:0x0A, attrInt:0x0012] }:
        case { contains it, [clusterInt:0x0201, commandInt:0x01, attrInt:0x0012] }:
        
            // https://www.urbandictionary.com/define.php?term=Retard%20Unit
            String heatingSetpoint = "${location.temperatureScale == 'C' ? Integer.parseInt(msg.value, 16) / 100 : Math.round((Integer.parseInt(msg.value, 16) * 0.018 + 32) * 100) / 100}"
            utils_sendEvent name:'heatingSetpoint', value:heatingSetpoint, unit:"°${location.temperatureScale}", descriptionText:"Heating setpoint is ${heatingSetpoint}°${location.temperatureScale}", type:type
            utils_processedZclMessage "${msg.commandInt == 0x0A ? 'Report' : 'Read'} Attributes Response", "OccupiedHeatingSetpoint=${msg.value}"
        
            utils_sendZigbeeCommands(zigbee.readAttribute(0x0201, 0x001C))
            return
        
        // Report/Read Attributes Reponse: SystemMode
        case { contains it, [clusterInt:0x0201, commandInt:0x0A, attrInt:0x001C] }:
        case { contains it, [clusterInt:0x0201, commandInt:0x01, attrInt:0x001C] }:
            String thermostatMode = TH_MODES[msg.value]
            utils_sendEvent name:'thermostatMode', value:thermostatMode, descriptionText:"Thermostat mode is ${thermostatMode}", type:type
            utils_processedZclMessage "${msg.commandInt == 0x0A ? 'Report' : 'Read'} Attributes Response", "SystemMode=${msg.value}"
            return
        
        // Report/Read Attributes Reponse: ThermostatRunningState
        case { contains it, [clusterInt:0x0201, commandInt:0x0A, attrInt:0x0029] }:
        case { contains it, [clusterInt:0x0201, commandInt:0x01, attrInt:0x0029] }:
            String thermostatOperatingState = TH_STATES[msg.value[-2..-1]]
            utils_sendEvent name:'thermostatOperatingState', value:thermostatOperatingState, descriptionText:"Thermostat operating state is ${thermostatOperatingState}", type:type
            utils_processedZclMessage "${msg.commandInt == 0x0A ? 'Report' : 'Read'} Attributes Response", "ThermostatRunningState=${msg.value}"
            return
        
        // Write Attributes Response
        case { contains it, [clusterInt:0x0201, commandInt:0x04, isClusterSpecific:false] }:
            utils_processedZclMessage "Write Attributes Response", "Status=${msg.data}"
            return
        
        // Other events that we expect but are not usefull
        case { contains it, [clusterInt:0x0201, commandInt:0x07] }:
            utils_processedZclMessage 'Configure Reporting Response', "attribute=LocalTemperature, data=${msg.data}"
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
