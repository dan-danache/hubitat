/**
 * Aqara Dual Relay Module T2 (DCM-K01)
 *
 * @see https://dan-danache.github.io/hubitat/ikea-zigbee-drivers/
 */
import groovy.transform.CompileStatic
import groovy.transform.Field

@Field static final String DRIVER_NAME = 'Aqara Dual Relay Module T2 (DCM-K01)'
@Field static final String DRIVER_VERSION = '4.0.0'

// Fields for capability.MultiRelay
import com.hubitat.app.ChildDeviceWrapper

// Fields for capability.PushableButton
@Field static final Map<String, List<String>> BUTTONS = [
    'S1': ['1', 'S1'],
    'S2': ['2', 'S2'],
]

// Fields for capability.HealthCheck
import groovy.time.TimeCategory

@Field static final Map<String, String> HEALTH_CHECK = [
    'schedule': '0 0 0/1 ? * * *', // Health will be checked using this cron schedule
    'thereshold': '3600' // When checking, mark the device as offline if no Zigbee message was received in the last 3600 seconds
]

metadata {
    definition(name:DRIVER_NAME, namespace:'dandanache', author:'Dan Danache', importUrl:'https://raw.githubusercontent.com/dan-danache/hubitat/master/ikea-zigbee-drivers/Aqara_DCM-K01.groovy') {
        capability 'Configuration'
        capability 'Refresh'
        capability 'Sensor'
        capability 'TemperatureMeasurement'
        capability 'PowerMeter'
        capability 'EnergyMeter'
        capability 'Actuator'
        capability 'PushableButton'
        capability 'HealthCheck'

        // For firmware: Unknown
        fingerprint profileId:'0104', endpointId:'01', inClusters:'0B04,0702,0005,0004,0003,0012,0000,0006,FCC0', outClusters:'0019,000A', model:'lumi.switch.acn047', manufacturer:'Aqara'
        
        // Attributes for devices.Aqara_DCM-K01
        attribute 'powerOutageCount', 'number'
        
        // Attributes for capability.HealthCheck
        attribute 'healthStatus', 'enum', ['offline', 'online', 'unknown']
    }
    
    // Commands for capability.FirmwareUpdate
    command 'updateFirmware'

    preferences {
        input(
            name: 'helpInfo', type: 'hidden',
            title: '''
            <div style="min-height:55px; background:transparent url('https://dan-danache.github.io/hubitat/ikea-zigbee-drivers/img/Aqara_DCM-K01.webp') no-repeat left center;background-size:auto 55px;padding-left:60px">
                Aqara Dual Relay Module T2 (DCM-K01) <small>v4.0.0</small><br>
                <small><div>
                • <a href="https://dan-danache.github.io/hubitat/ikea-zigbee-drivers/#aqara-dual-relay-module-t2-dcm-k01" target="_blank">device details</a><br>
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
        
        // Inputs for devices.Aqara_DCM-K01
        input(
            name: 'switchType', type: 'enum',
            title: 'Switch type',
            description: '<small>What type of switches are connected to S1 and S2.</small>',
            options: [
                '1':'Latching switch - toggle/rocker',
                '2':'Momentary switch - push button',
                '3':'Disabled - connected switches are ignored',
            ],
            defaultValue: '1',
            required: true
        )
        input(
            name: 'operationModeS1', type: 'enum',
            title: 'Operation mode for Switch S1',
            description: '<small>What happens when Switch S1 is used.</small>',
            options: [
                '1':'Standard - Switch S1 controls Relay L1',
                '0':'Decoupled - Switch S1 only sends button events',
            ],
            defaultValue: '1',
            required: true
        )
        input(
            name: 'operationModeS2', type: 'enum',
            title: 'Operation mode for Switch S2',
            description: '<small>What happens when Switch S2 is used.</small>',
            options: [
                '1':'Standard - Switch S2 controls Relay L2',
                '0':'Decoupled - Switch S2 only sends button events',
            ],
            defaultValue: '1',
            required: true
        )
        input(
            name: 'relayMode', type: 'enum',
            title: 'Relay mode',
            description: '<small>How Relay L1 and Relay L2 operate.</small>',
            options: [
                '0':'Wet contact - connect L to L1, L2 (jumper wire installed)',
                '3':'Dry contact - connect LOUT to L1, L2 (no jumper wire)',
                '1':'Pulse - temporary connect LOUT to L1, L2 (no jumper wire)',
            ],
            defaultValue: '0',
            required: true
        )
        if ("${relayMode}" == '1') {
            input(
                name: 'pulseDuration', type: 'number',
                title: 'Pulse duration',
                description: '<small>Only when Relay mode is Pulse. Range 200ms .. 2000ms.</small>',
                defaultValue: 1000,
                range: '200..2000',
                required: true
            )
        }
        input(
            name: 'interlock', type: 'enum',
            title: 'Interlock',
            description: '<small>Prevent both Relay L1 and Relay L2 being On at the same time.</small>',
            options: [
                '0':'Disabled - control lights and other devices',
                '1':'Enabled - control bi-directional motors',
            ],
            defaultValue: '0',
            required: true
        )
        input(
            name: 'powerOnBehavior', type: 'enum',
            title: 'Power On behaviour',
            description: '<small>What happens after a power outage.</small>',
            options: [
                '0': 'Turn power On',
                '2': 'Turn power Off',
                '1': 'Restore previous state'
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
    
    // Preferences for devices.Aqara_DCM-K01
    if (powerOnBehavior == null) {
        powerOnBehavior = '1'
        device.updateSetting 'powerOnBehavior', [value:powerOnBehavior, type:'enum']
    }
    log_info "🛠️ powerOnBehavior = ${powerOnBehavior}"
    cmds += zigbee.writeAttribute(0xFCC0, 0x0517, 0x20, Integer.parseInt(powerOnBehavior),  [mfgCode:'0x115F', destEndpoint:0x01])
    
    if (operationModeS1 == null) {
        operationModeS1 = '1'
        device.updateSetting 'operationModeS1', [value:operationModeS1, type:'enum']
    }
    log_info "🛠️ operationModeS1 = ${operationModeS1}"
    cmds += zigbee.writeAttribute(0xFCC0, 0x0200, 0x20, Integer.parseInt(operationModeS1), [mfgCode:'0x115F', destEndpoint:0x01])
    
    if (operationModeS2 == null) {
        operationModeS2 = '1'
        device.updateSetting 'operationModeS2', [value:operationModeS2, type:'enum']
    }
    log_info "🛠️ operationModeS2 = ${operationModeS2}"
    cmds += zigbee.writeAttribute(0xFCC0, 0x0200, 0x20, Integer.parseInt(operationModeS2), [mfgCode:'0x115F', destEndpoint:0x02])
    
    if (switchType == null) {
        switchType = '1'
        device.updateSetting 'switchType', [value:switchType, type:'enum']
    }
    log_info "🛠️ switchType = ${switchType}"
    cmds += zigbee.writeAttribute(0xFCC0, 0x000A, 0x20, Integer.parseInt(switchType), [mfgCode:'0x115F', destEndpoint:0x01])
    
    if (interlock == null) {
        interlock = '0'
        device.updateSetting 'interlock', [value:interlock, type:'enum']
    }
    log_info "🛠️ interlock = ${interlock}"
    cmds += zigbee.writeAttribute(0xFCC0, 0x02D0, 0x10, Integer.parseInt(interlock), [mfgCode:'0x115F', destEndpoint:0x01])
    
    if (relayMode == null) {
        relayMode = '0'
        device.updateSetting 'relayMode', [value:relayMode, type:'enum']
    }
    log_info "🛠️ relayMode = ${relayMode}"
    cmds += zigbee.writeAttribute(0xFCC0, 0x0289, 0x20, Integer.parseInt(relayMode), [mfgCode:'0x115F', destEndpoint:0x01])
    
    if (relayMode == '1') {
        Integer pulseDurationInt = pulseDuration == null ? 2000 : pulseDuration.intValue()
        device.updateSetting 'pulseDuration', [value:pulseDurationInt, type:'number']
    
        log_info "🛠️ pulseDuration = ${pulseDurationInt}"
        cmds += zigbee.writeAttribute(0xFCC0, 0x00EB, 0x21, pulseDurationInt, [mfgCode:'0x115F', destEndpoint:0x01])
    }
    
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
    
    // Configuration for devices.Aqara_DCM-K01
    cmds += "zdo bind 0x${device.deviceNetworkId} 0x01 0x01 0x0012 {${device.zigbeeId}} {}" // Multistate Input cluster (ep 0x01)
    cmds += "zdo bind 0x${device.deviceNetworkId} 0x02 0x01 0x0012 {${device.zigbeeId}} {}" // Multistate Input cluster (ep 0x02)
    
    // Configuration for capability.PowerMeter
    cmds += "zdo bind 0x${device.deviceNetworkId} 0x${device.endpointId} 0x01 0x0B04 {${device.zigbeeId}} {}" // Electrical Measurement cluster
    cmds += "he cr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0B04 0x050B 0x21 0x0000 0x4650 {02} {}" // Report ActivePower (uint16) at least every 5 hours (Δ = 0.2W)
    
    // Configuration for capability.EnergyMeter
    cmds += "zdo bind 0x${device.deviceNetworkId} 0x${device.endpointId} 0x01 0x0702 {${device.zigbeeId}} {}" // (Metering (Smart Energy) cluster
    cmds += "he cr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0702 0x0000 0x25 0x0000 0x4650 {00} {}" // Report CurrentSummationDelivered (uint48) at least every 5 hours (Δ = 0)
    
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
    
    // Refresh for devices.Aqara_DCM-K01
    cmds += zigbee.readAttribute(0xFCC0, 0x00F7, [mfgCode: '0x115F']) // LumiSpecific
    
    // Refresh for capability.PowerMeter
    cmds += zigbee.readAttribute(0x0B04, 0x0604) // PowerMultiplier
    cmds += zigbee.readAttribute(0x0B04, 0x0605) // PowerDivisor
    cmds += zigbee.readAttribute(0x0B04, 0x050B) // ActivePower
    
    // Refresh for capability.EnergyMeter
    cmds += zigbee.readAttribute(0x0702, 0x0000) // EnergySumation
    cmds += zigbee.readAttribute(0x0702, 0x0301) // EnergyMultiplier
    cmds += zigbee.readAttribute(0x0702, 0x0302) // EnergyDivisor
    
    // Refresh for capability.MultiRelay
    cmds += zigbee.readAttribute(0x0006, 0x0000, [destEndpoint:0x01]) // OnOff (ep 0x01)
    cmds += zigbee.readAttribute(0x0006, 0x0000, [destEndpoint:0x02]) // OnOff (ep 0x02)
    utils_sendZigbeeCommands cmds
}

// Implementation for capability.MultiRelay
private ChildDeviceWrapper fetchChildDevice(Integer moduleNumber) {
    ChildDeviceWrapper childDevice = getChildDevice("${device.deviceNetworkId}-${moduleNumber}")
    return childDevice ?: addChildDevice('hubitat', 'Generic Component Switch', "${device.deviceNetworkId}-${moduleNumber}", [name:"${device.displayName} - Relay L${moduleNumber}", label:"Relay L${moduleNumber}", isComponent:true])
}

void componentOff(ChildDeviceWrapper childDevice) {
    log_debug "▲ Received Off request from ${childDevice.displayName}"
    Integer endpointInt = Integer.parseInt(childDevice.deviceNetworkId.split('-')[1])
    utils_sendZigbeeCommands(["he raw 0x${device.deviceNetworkId} 0x01 0x0${endpointInt} 0x0006 {014300}"])
}

void componentOn(ChildDeviceWrapper childDevice) {
    log_debug "▲ Received On request from ${childDevice.displayName}"
    Integer endpointInt = Integer.parseInt(childDevice.deviceNetworkId.split('-')[1])
    utils_sendZigbeeCommands(["he raw 0x${device.deviceNetworkId} 0x01 0x0${endpointInt} 0x0006 {014301}"])
}

void componentRefresh(ChildDeviceWrapper childDevice) {
    log_debug "▲ Received Refresh request from ${childDevice.displayName}"
    refresh()
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
        
        // Events for devices.Aqara_DCM-K01
        // ===================================================================================================================
        
        // Switch was flipped
        case { contains it, [clusterInt:0x0012, commandInt:0x0A] }:
            List<String> button = msg.endpointInt == 0x01 ? BUTTONS.S1 : BUTTONS.S2
            utils_sendEvent name:'pushed', value:button[0], type:'physical', isStateChange:true, descriptionText:"Button ${button[0]} (${button[1]}) was pushed"
            return
        
        // LumiSpecific
        case { contains it, [clusterInt:0xFCC0, commandInt:0x01, attrInt:0x00F7] }:
        case { contains it, [clusterInt:0xFCC0, commandInt:0x0A, attrInt:0x00F7] }:
            // https://github.com/Koenkk/zigbee-herdsman-converters/blob/cae265712bf75be74530d1c0458901f81c1adcc5/src/lib/lumi.ts#L166
            //   0 - 03 28 2A               3 = device_temperature
            //   6 - 05 21 23 00            5 = power_outage_count
            //  14 - 09 21 00 0D            9 = ??
            //  22 - 0A 21 99 84           10 = switch_type
            //  30 - 0C 20 0A              12 = ??
            //  36 - 0D 23 1B 00 00 00     13 = Overwrite version advertised by `genBasic` and `genOta` with correct version:
            //                                  - meta.device.meta.lumiFileVersion = value;
            //                                  - meta.device.softwareBuildID = trv.decodeFirmwareVersionString(value);
            //  48 - 11 23 01 00 00 00     17 = ??
            //  60 - 64 10 00             100 = relay 1 ??
            //  66 - 65 10 00             101 = relay 2 ??
            //  72 - 95 39 C1 CC B6 42    149 = energy / consumption
            //  84 - 96 39 9A A9 08 45    150 = voltage = value * 0.1;
            //  96 - 98 39 00 00 00 00    152 = power
            // 108 - 97 39 00 00 00 00    151 = current = value * 0.001;
            // 120 - 9A 20 00             154 = ??
        
            // cluster-specific, !manufacturer-specific, server-to-client, !disable-default-response = 09
            // utils_sendZigbeeCommands(["he raw 0x${device.deviceNetworkId} 0x01 0x01 0x0019 {0901 00 00 64}"])
            // utils_sendZigbeeCommands(["he raw 0x${device.deviceNetworkId} 0x01 0x01 0x0019 {0901 00 01 64 5F11}"])
            // utils_sendZigbeeCommands(["he raw 0x${device.deviceNetworkId} 0x01 0x01 0x0019 {0901 00 02 64 5F11 1019}"])
            // utils_sendZigbeeCommands(["he raw 0x${device.deviceNetworkId} 0x01 0x01 0x0019 {0901 00 03 64 5F11 1019 1B000000}"])
            //
            // Read required attribute 0x0000 (UpgradeServerID) or 0x0006 (ImageUpgradeStatus) of cluster 0x0019:
            // - !cluster-specific, !manufacturer-specific, server-to-client, !disable-default-response = 08
            // he raw .addr 0x01 0x01 0x0019 {0800 00 0000}             -- UpgradeServerID
            // he raw .addr 0x01 0x01 0x0019 {0800 00 0400}             -- DownloadedFileVersion (read)
            // he raw .addr 0x01 0x01 0x0019 {0843 02 0400 23 1E000000} -- DownloadedFileVersion (write -> read only!)
            // he raw .addr 0x01 0x01 0x0019 {0800 00 0600}             -- ImageUpgradeStatus
            // he raw .addr 0x01 0x01 0x0019 {0800 00 0700}             -- Manufacturer ID
            // he raw .addr 0x01 0x01 0x0019 {0800 00 0800}             -- Image Type ID
            //
            // Image Notify
            // - cluster-specific, !manufacturer-specific, server-to-client, disable-default-response = 19
            // := payload type = 0x03, jitter = 100 (0x064), mfg = 0x115F, imageType = 0x1910, fileVersion = 0x0000001E (30)
            // he raw .addr 0x01 0x01 0x0019 {0900 00 03 60 5F11 1019 1E000000}
            //
            // Query Next Image Response
            // - cluster-specific, !manufacturer-specific, server-to-client, disable-default-response = 19
            // := status, mfg = 0x115F, imageType = 0x1910, fileVersion = 0x0000001C (28), fileSize = 411786 bytes
            // he raw .addr 0x01 0x01 0x0019 {1900 02 00 5F11 1019 1C000000 8A480600}
            //
            // Image Block Response (status = 0x95:ABORT)
            // - cluster-specific, !manufacturer-specific, server-to-client, disable-default-response = 19
            // he raw .addr 0x01 0x01 0x0019 {1900 05 95}
        
            if (msg.value.size() != 126) return
        
            String temperature = convertTemperatureIfNeeded(Integer.parseInt(msg.value[4..5], 16), 'C', 0)
            utils_sendEvent name:'temperature', value:temperature, unit:"°${location.temperatureScale}", descriptionText:"Temperature is ${temperature} °${location.temperatureScale}", type:type
        
            Integer powerOutageCount = Integer.parseInt(msg.value[12..13] + msg.value[10..11], 16)
            utils_sendEvent name:'powerOutageCount', value:powerOutageCount, descriptionText:"Power outage count is ${powerOutageCount}", type:type
        
            String softwareBuild = '0.0.0_' + [
                "${Integer.parseInt(msg.value[44..45], 16)}",
                "${Integer.parseInt(msg.value[42..43], 16)}",
                "${Integer.parseInt(msg.value[40..41], 16)}"
            ].join('').padLeft(4, '0')
            utils_dataValue('softwareBuild', softwareBuild)
        
            Integer energy = Math.round(Float.intBitsToFloat(Integer.parseInt("${msg.value[82..83]}${msg.value[80..81]}${msg.value[78..79]}${msg.value[76..77]}", 16))) / 1000
            //utils_sendEvent name:'energy', value:energy, unit:'kWh', descriptionText:"Energy is ${energy} kWh", type:type
        
            Integer voltage = Math.round(Float.intBitsToFloat(Integer.parseInt("${msg.value[94..95]}${msg.value[92..93]}${msg.value[90..91]}${msg.value[88..89]}", 16))) / 10
            //utils_sendEvent name:'voltage', value:voltage, unit:'V', descriptionText:"Voltage is ${voltage} V", type:type
        
            Integer power = Math.round(Float.intBitsToFloat(Integer.parseInt("${msg.value[106..107]}${msg.value[104..105]}${msg.value[102..103]}${msg.value[100..101]}", 16)))
            //utils_sendEvent name:'power', value:power, unit:'W', descriptionText:"Power is ${power} W", type:type
        
            Integer amperage = Math.round(Float.intBitsToFloat(Integer.parseInt("${msg.value[118..119]}${msg.value[116..117]}${msg.value[114..115]}${msg.value[112..113]}", 16))) / 1000
            //utils_sendEvent name:'amperage', value:amperage, unit:'A', descriptionText:"Amperage is ${amperage} W", type:type
        
            utils_processedZclMessage "${msg.commandInt == 0x0A ? 'Report' : 'Read'} Attributes Response", "Temperature=${temperature}, PowerOutageCount=${powerOutageCount}, SoftwareBuild=${softwareBuild}, Energy=${energy}kWh, Voltage=${voltage}V, Power=${power}W, Amperage=${amperage}A"
            return
        
        // Other events that we expect but are not usefull for devices.Aqara_DCM-K01 behavior
        case { contains it, [clusterInt:0xFCC0, commandInt:0x07] }:
            utils_processedZclMessage 'Configure Reporting Response', "attribute=LumiSpecific, data=${msg.data}"
            return
        case { contains it, [clusterInt:0xFCC0, commandInt:0x0A, attrInt:0x0517] }:
            utils_processedZclMessage 'Report Attributes Response', "PowerOnBehavior=${msg.value}"
            return
        case { contains it, [clusterInt:0xFCC0, commandInt:0x0A, attrInt:0x0200] }:
            utils_processedZclMessage 'Report Attributes Response', "OperationMode=${msg.value}, Switch=${msg.endpoint}"
            return
        case { contains it, [clusterInt:0xFCC0, commandInt:0x0A, attrInt:0x000A] }:
            utils_processedZclMessage 'Report Attributes Response', "switchType=${msg.value}"
            return
        case { contains it, [clusterInt:0xFCC0, commandInt:0x0A, attrInt:0x02D0] }:
            utils_processedZclMessage 'Report Attributes Response', "Interlock=${msg.value}"
            return
        case { contains it, [clusterInt:0xFCC0, commandInt:0x0A, attrInt:0x0289] }:
            utils_processedZclMessage 'Report Attributes Response', "RelayMode=${msg.value}"
            return
        case { contains it, [clusterInt:0xFCC0, commandInt:0x0A, attrInt:0x00EB] }:
            utils_processedZclMessage 'Report Attributes Response', "pulseDuration=${msg.value}"
            return
        case { contains it, [clusterInt:0xFCC0, commandInt:0x04] }:  // Write Attribute Response
            return
        
        // Events for capability.PowerMeter
        // ===================================================================================================================
        
        // Report/Read Attributes Reponse: ActivePower
        case { contains it, [clusterInt:0x0B04, commandInt:0x0A, attrInt:0x050B] }:
        case { contains it, [clusterInt:0x0B04, commandInt:0x01, attrInt:0x050B] }:
            Integer power = Integer.parseInt(msg.value, 16) * (state.powerMultiplier ?: 1) / (state.powerDivisor ?: 1)
            utils_sendEvent name:'power', value:power, unit:'W', descriptionText:"Power is ${power} W", type:type
            utils_processedZclMessage "${msg.commandInt == 0x0A ? 'Report' : 'Read'} Attributes Response", "ActivePower=${msg.value} (${power}W)"
            return
        
        // Report/Read Attributes Reponse: ApparentPower
        case { contains it, [clusterInt:0x0B04, commandInt:0x0A, attrInt:0x050F] }:
            Integer power = Integer.parseInt(msg.value, 16) * (state.powerMultiplier ?: 1) / (state.powerDivisor ?: 1)
            utils_processedZclMessage "${msg.commandInt == 0x0A ? 'Report' : 'Read'} Attributes Response", "ApparentPower=${msg.value} (${power}W)"
            return
        
        // Read Attributes Reponse: PowerMultiplier
        case { contains it, [clusterInt:0x0B04, commandInt:0x01, attrInt:0x0604] }:
            state.powerMultiplier = Integer.parseInt(msg.value, 16)
            utils_processedZclMessage 'Read Attributes Response', "PowerMultiplier=${msg.value}"
            return
        
        // Read Attributes Reponse: PowerDivisor
        case { contains it, [clusterInt:0x0B04, commandInt:0x01, attrInt:0x0605] }:
            state.powerDivisor = Integer.parseInt(msg.value, 16)
            utils_processedZclMessage 'Read Attributes Response', "PowerDivisor=${msg.value}"
            return
        
        // Other events that we expect but are not usefull for capability.PowerMeter behavior
        case { contains it, [clusterInt:0x0B04, commandInt:0x07] }:
            utils_processedZclMessage 'Configure Reporting Response', "attribute=ActivePower, data=${msg.data}"
            return
        case { contains it, [clusterInt:0x0B04, commandInt:0x06, isClusterSpecific:false, direction:'01'] }: // Configure Reporting Command
            return
        
        // Events for capability.EnergyMeter
        // ===================================================================================================================
        
        // Report/Read Attributes Reponse: EnergySummation
        case { contains it, [clusterInt:0x0702, commandInt:0x0A, attrInt:0x0000] }:
        case { contains it, [clusterInt:0x0702, commandInt:0x01, attrInt:0x0000] }:
            Integer energy = Integer.parseInt(msg.value, 16) * (state.energyMultiplier ?: 1) / (state.energyDivisor ?: 1)
            utils_sendEvent name:'energy', value:energy, unit:'kWh', descriptionText:"Energy is ${energy} kWh", type:type
            utils_processedZclMessage "${msg.commandInt == 0x0A ? 'Report' : 'Read'} Attributes Response", "EnergySummation=${msg.value}"
            return
        
        // Read Attributes Reponse: EnergyMultiplier
        case { contains it, [clusterInt:0x0702, commandInt:0x01, attrInt:0x0301] }:
            state.energyMultiplier = Integer.parseInt(msg.value, 16)
            utils_processedZclMessage 'Read Attributes Response', "EnergyMultiplier=${msg.value}"
            return
        
        // Read Attributes Reponse: EnergyDivisor
        case { contains it, [clusterInt:0x0702, commandInt:0x01, attrInt:0x0302] }:
            state.energyDivisor = Integer.parseInt(msg.value, 16)
            utils_processedZclMessage 'Read Attributes Response', "EnergyDivisor=${msg.value}"
            return
        
        // Other events that we expect but are not usefull for capability.PowerMeter behavior
        case { contains it, [clusterInt:0x0702, commandInt:0x07] }:
            utils_processedZclMessage 'Configure Reporting Response', "attribute=CurrentSummation, data=${msg.data}"
            return
        
        // Events for capability.MultiRelay
        // ===================================================================================================================
        
        // Report/Read Attributes: OnOff
        case { contains it, [clusterInt:0x0006, commandInt:0x0A, attrInt:0x0000] }:
        case { contains it, [clusterInt:0x0006, commandInt:0x01, attrInt:0x0000] }:
            Integer moduleNumber = msg.endpointInt
            String newState = msg.value == '00' ? 'off' : 'on'
        
            // Send event to module child device (only if state needs to change)
            ChildDeviceWrapper childDevice = fetchChildDevice(moduleNumber)
            if (newState != childDevice.currentValue('switch', true)) {
                childDevice.parse([[name:'switch', value:newState, descriptionText:"${childDevice.displayName} was turned ${newState}", type:type]])
            }
        
            utils_processedZclMessage "${msg.commandInt == 0x0A ? 'Report' : 'Read'} Attributes Response", "Module=${moduleNumber}, Switch=${newState}"
            return
        
        // Other events that we expect but are not usefull for capability.MultiRelay behavior
        case { contains it, [clusterInt:0x0006, commandInt:0x07] }:
            utils_processedZclMessage 'Configure Reporting Response', "attribute=switch, data=${msg.data}"
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
