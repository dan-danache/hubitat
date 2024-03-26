/**
 * Aqara Dual Relay Module T2 (DCM-K01)
 *
 * @see https://dan-danache.github.io/hubitat/ikea-zigbee-drivers/
 * @see https://zigbee.blakadder.com/Aqara_LLKZMK12LM.html
 */
import groovy.time.TimeCategory
import groovy.transform.Field

@Field static final String DRIVER_NAME = "Aqara Dual Relay Module T2 (DCM-K01)"
@Field static final String DRIVER_VERSION = "4.0.0"

// Fields for capability.MultiRelay
import com.hubitat.app.ChildDeviceWrapper

// Fields for capability.PushableButton
@Field static final Map<String, List<String>> BUTTONS = [
    "S1": ["1", "S1"],
    "S2": ["2", "S2"],
]

metadata {
    definition(name:DRIVER_NAME, namespace:"dandanache", author:"Dan Danache", importUrl:"https://raw.githubusercontent.com/dan-danache/hubitat/master/ikea-zigbee-drivers/Aqara_DCM-K01.groovy") {
        capability "Configuration"
        capability "Sensor"
        capability "TemperatureMeasurement"
        capability "PowerMeter"
        capability "EnergyMeter"
        capability "Actuator"
        capability "PushableButton"
        capability "Refresh"

        // For firmware: Unknown
        fingerprint profileId:"0104", endpointId:"01", inClusters:"0B04,0702,0005,0004,0003,0012,0000,0006,FCC0", outClusters:"0019,000A", model:"lumi.switch.acn047", manufacturer:"Aqara"
        
        // Attributes for devices.Aqara_DCM-K01
        attribute "powerOutageCount", "number"
    }
    
    // Commands for capability.FirmwareUpdate
    command "updateFirmware"

    preferences {
        input(
            name: "logLevel",
            type: "enum",
            title: "Log verbosity",
            description: "<small>Select what type of messages appear in the \"Logs\" section.</small>",
            options: [
                "1" : "Debug - log everything",
                "2" : "Info - log important events",
                "3" : "Warning - log events that require attention",
                "4" : "Error - log errors"
            ],
            defaultValue: "1",
            required: true
        )
        
        // Inputs for devices.Aqara_DCM-K01
        input(
            name: "switchType",
            type: "enum",
            title: "Switch type",
            description: "<small>What type of switches are connected to S1 and S2.</small>",
            options: [
                "1":"Latching switch - toggle/rocker",
                "2":"Momentary switch - push button",
                "3":"Disabled - connected switches are ignored",
            ],,
            defaultValue: "1",
            required: true
        )
        input(
            name: "operationModeS1",
            type: "enum",
            title: "Operation mode for Switch S1",
            description: "<small>What happens when Switch S1 is used.</small>",
            options: [
                "1":"Standard - Switch S1 controls Relay L1",
                "0":"Decoupled - Switch S1 only sends button events",
            ],
            defaultValue: "1",
            required: true
        )
        input(
            name: "operationModeS2",
            type: "enum",
            title: "Operation mode for Switch S2",
            description: "<small>What happens when Switch S2 is used.</small>",
            options: [
                "1":"Standard - Switch S2 controls Relay L2",
                "0":"Decoupled - Switch S2 only sends button events",
            ],
            defaultValue: "1",
            required: true
        )
        input(
            name: "relayMode",
            type: "enum",
            title: "Relay mode",
            description: "<small>How Relay L1 and Relay L2 operate.</small>",
            options: [
                "0":"Wet contact - connect L to L1, L2 (jumper wire installed)",
                "3":"Dry contact - connect LOUT to L1, L2 (no jumper wire)",
                "1":"Pulse - temporary connect LOUT to L1, L2 (no jumper wire)",
            ],
            defaultValue: "0",
            required: true
        )
        if ("${relayMode}" == "1") {
            input(
                name: "pulseLength",
                type: "number",
                title: "Pulse length",
                description: "<small>Only when Relay mode is Pulse. Range 200ms .. 2000ms.</small>",
                defaultValue: 1000,
                range: "200..2000",
                required: true
            )
        }
        input(
            name: "interlock",
            type: "enum",
            title: "Interlock",
            description: "<small>Prevent both Relay L1 and Relay L2 being On at the same time.</small>",
            options: [
                "0":"Disabled - control lights and other devices",
                "1":"Enabled - control bi-directional motors",
            ],,
            defaultValue: "0",
            required: true
        )
        input(
            name: "powerOnBehavior",
            type: "enum",
            title: "Power On behaviour",
            description: "<small>What happens after a power outage.</small>",
            options: [
                "0": "Turn power On",
                "2": "Turn power Off",
                "1": "Restore previous state"
            ],
            defaultValue: "1",
            required: true
        )
    }
}

// ===================================================================================================================
// Implement default methods
// ===================================================================================================================

// Called when the device is first added
def installed() {
    Log.warn "Installing device ..."
    Log.warn "[IMPORTANT] For battery-powered devices, make sure that you keep your device as close as you can (less than 2inch / 5cm) to your Hubitat hub for at least 30 seconds. Otherwise the device will successfully pair but it won't work properly!"
}

// Called when the "Save Preferences" button is clicked
def updated(auto = false) {
    Log.info "Saving preferences${auto ? " (auto)" : ""} ..."
    List<String> cmds = []

    unschedule()

    if (logLevel == null) {
        logLevel = "1"
        device.updateSetting("logLevel", [value:logLevel, type:"enum"])
    }
    if (logLevel == "1") runIn 1800, "logsOff"
    Log.info "🛠️ logLevel = ${["1":"Debug", "2":"Info", "3":"Warning", "4":"Error"].get(logLevel)}"
    
    // Preferences for devices.Aqara_DCM-K01
    if (powerOnBehavior == null) {
        powerOnBehavior = "1"
        device.updateSetting("powerOnBehavior", [value:powerOnBehavior, type:"enum"])
    }
    Log.info "🛠️ powerOnBehavior = ${powerOnBehavior}"
    cmds += zigbee.writeAttribute(0xFCC0, 0x0517, 0x20, Integer.parseInt(powerOnBehavior), [mfgCode:"0x115F", destEndpoint:0x01])
    
    if (operationModeS1 == null) {
        operationModeS1 = "1"
        device.updateSetting("operationModeS1", [value:operationModeS1, type:"enum"])
    }
    Log.info "🛠️ operationModeS1 = ${operationModeS1}"
    cmds += zigbee.writeAttribute(0xFCC0, 0x0200, 0x20, Integer.parseInt(operationModeS1), [mfgCode:"0x115F", destEndpoint:0x01])
    
    if (operationModeS2 == null) {
        operationModeS2 = "1"
        device.updateSetting("operationModeS2", [value:operationModeS2, type:"enum"])
    }
    Log.info "🛠️ operationModeS2 = ${operationModeS2}"
    cmds += zigbee.writeAttribute(0xFCC0, 0x0200, 0x20, Integer.parseInt(operationModeS2), [mfgCode:"0x115F", destEndpoint:0x02])
    
    if (switchType == null) {
        switchType = "1"
        device.updateSetting("switchType", [value:switchType, type:"enum"])
    }
    Log.info "🛠️ switchType = ${switchType}"
    cmds += zigbee.writeAttribute(0xFCC0, 0x000A, 0x20, Integer.parseInt(switchType), [mfgCode:"0x115F", destEndpoint:0x01])
    
    if (interlock == null) {
        interlock = "0"
        device.updateSetting("interlock", [value:interlock, type:"enum"])
    }
    Log.info "🛠️ interlock = ${interlock}"
    cmds += zigbee.writeAttribute(0xFCC0, 0x02D0, 0x10, Integer.parseInt(interlock), [mfgCode:"0x115F", destEndpoint:0x01])
    
    if (relayMode == null) {
        relayMode = "0"
        device.updateSetting("relayMode", [value:relayMode, type:"enum"])
    }
    Log.info "🛠️ relayMode = ${relayMode}"
    cmds += zigbee.writeAttribute(0xFCC0, 0x0289, 0x20, Integer.parseInt(relayMode), [mfgCode:"0x115F", destEndpoint:0x01])
    
    if (relayMode == "1") {
        Integer pulseLengthInt = pulseLength == null ? 2000 : pulseLength.intValue()
        device.updateSetting("pulseLength", [value:pulseLengthInt, type:"number"])
    
        Log.info "🛠️ pulseLength = ${pulseLengthInt}"
        cmds += zigbee.writeAttribute(0xFCC0, 0x00EB, 0x21, pulseLengthInt, [mfgCode:"0x115F", destEndpoint:0x01])
    }
    

    if (auto) return cmds
    Utils.sendZigbeeCommands cmds
}

// ===================================================================================================================
// Capabilities helpers
// ===================================================================================================================

// Handler method for scheduled job to disable debug logging
def logsOff() {
   Log.info '⏲️ Automatically reverting log level to "Info"'
   device.updateSetting("logLevel", [value:"2", type:"enum"])
}

// ===================================================================================================================
// Implement Hubitat Capabilities
// ===================================================================================================================

// capability.Configuration
// Note: This method is also called when the device is initially installed
def configure(auto = false) {
    Log.warn "Configuring device${auto ? " (auto)" : ""} ..."
    if (!auto && device.currentValue("powerSource", true) == "battery") {
        Log.warn '[IMPORTANT] Click the "Configure" button immediately after pushing any button on the device in order to first wake it up!'
    }

    // Apply preferences first
    List<String> cmds = []
    cmds += updated(true)

    // Clear data (keep firmwareMT information though)
    device.getData()?.collect { it.key }.each { if (it != "firmwareMT") device.removeDataValue it }

    // Clear state
    state.clear()
    state.lastTx = 0
    state.lastRx = 0
    state.lastCx = DRIVER_VERSION

    // Configure Aqara Dual Relay Module T2 (DCM-K01) specific Zigbee reporting
    // -- No reporting needed

    // Add Aqara Dual Relay Module T2 (DCM-K01) specific Zigbee binds
    // -- No binds needed

    // Remove Aqara Dual Relay Module T2 (DCM-K01) specific Zigbee binds
    // -- No unbinds needed
    
    // Configuration for devices.Aqara_DCM-K01
    cmds += "zdo bind 0x${device.deviceNetworkId} 0x01 0x01 0x0012 {${device.zigbeeId}} {}" // Multistate Input cluster (Switch 1)
    cmds += "zdo bind 0x${device.deviceNetworkId} 0x02 0x01 0x0012 {${device.zigbeeId}} {}" // Multistate Input cluster (Switch 2)
    
    // Configuration for capability.PowerMeter
    cmds += "zdo bind 0x${device.deviceNetworkId} 0x01 0x01 0x0B04 {${device.zigbeeId}} {}" // Electrical Measurement cluster
    cmds += "he cr 0x${device.deviceNetworkId} 0x01 0x0B04 0x050B 0x21 0x0000 0x4650 {02} {}" // Report ActivePower (uint16) at least every 5 hours (Δ = 0.2W)
    cmds += zigbee.readAttribute(0x0B04, 0x0604, [destEndpoint:0x01]) // PowerMultiplier
    cmds += zigbee.readAttribute(0x0B04, 0x0605, [destEndpoint:0x01]) // PowerDivisor
    
    cmds += "zdo bind 0x${device.deviceNetworkId} 0x01 0x01 0x0702 {${device.zigbeeId}} {}" // (Metering (Smart Energy) cluster
    cmds += "he cr 0x${device.deviceNetworkId} 0x01 0x0702 0x0000 0x25 0x0000 0x4650 {00} {}" // Report CurrentSummationDelivered (uint48) at least every 5 hours (Δ = 0)
    cmds += zigbee.readAttribute(0x0702, 0x0301, [destEndpoint:0x01]) // Multiplier
    cmds += zigbee.readAttribute(0x0702, 0x0302, [destEndpoint:0x01]) // Divisor
    
    // Configuration for capability.MultiRelay
    cmds += "zdo bind 0x${device.deviceNetworkId} 0x01 0x01 0x0006 {${device.zigbeeId}} {}" // On/Off cluster
    cmds += "zdo bind 0x${device.deviceNetworkId} 0x02 0x01 0x0006 {${device.zigbeeId}} {}" // On/Off cluster
    
    cmds += "he cr 0x${device.deviceNetworkId} 0x01 0x0006 0x0000 0x10 0x0000 0x0258 {01} {}" // Report OnOff (bool) at least every 10 minutes
    cmds += "he cr 0x${device.deviceNetworkId} 0x02 0x0006 0x0000 0x10 0x0000 0x0258 {01} {}" // Report OnOff (bool) at least every 10 minutes
    
    cmds += "he raw 0x${device.deviceNetworkId} 0x01 0x01 0x0006 {104300 0000}" // Read OnOff attribute
    cmds += "he raw 0x${device.deviceNetworkId} 0x02 0x01 0x0006 {104400 0000}" // Read OnOff attribute
    
    // Configuration for capability.PushableButton
    Integer numberOfButtons = BUTTONS.count{_ -> true}
    sendEvent name:"numberOfButtons", value:numberOfButtons, descriptionText:"Number of buttons is ${numberOfButtons}"

    // Query Basic cluster attributes
    cmds += zigbee.readAttribute(0x0000, [0x0001, 0x0003, 0x0004, 0x0005, 0x000A, 0x4000]) // ApplicationVersion, HWVersion, ManufacturerName, ModelIdentifier, ProductCode, SWBuildID
    Utils.sendZigbeeCommands cmds

    Log.info "Configuration done; refreshing device current state in 7 seconds ..."
    runIn 7, "tryToRefresh"
}
private autoConfigure() {
    Log.warn "Detected that this device is not properly configured for this driver version (lastCx != ${DRIVER_VERSION})"
    configure true
}

// Implementation for capability.MultiRelay
private ChildDeviceWrapper fetchChildDevice(Integer moduleNumber){
    def childDevice = getChildDevice("${device.deviceNetworkId}-${moduleNumber}")
    if (!childDevice) {
        childDevice = addChildDevice("hubitat", "Generic Component Switch", "${device.deviceNetworkId}-${moduleNumber}", [name:"${device.displayName} - Relay L${moduleNumber}", label:"Relay L${moduleNumber}", isComponent:true])
        childDevice.parse([[name:"switch", value:"off", descriptionText:"Set initial switch value"]])
    }
    return childDevice
}

void componentOff(childDevice) {
    Log.debug "▲ Received Off request from ${childDevice.displayName}"
    Integer endpointInt = Integer.parseInt(childDevice.deviceNetworkId.split('-')[1])
    Utils.sendZigbeeCommands(["he raw 0x${device.deviceNetworkId} 0x01 0x0${endpointInt} 0x0006 {014300}"])
}

void componentOn(childDevice) {
    Log.debug "▲ Received On request from ${childDevice.displayName}"
    Integer endpointInt = Integer.parseInt(childDevice.deviceNetworkId.split('-')[1])
    Utils.sendZigbeeCommands(["he raw 0x${device.deviceNetworkId} 0x01 0x0${endpointInt} 0x0006 {014301}"])
}

void componentRefresh(childDevice) {
    Log.debug "▲ Received Refresh request from ${childDevice.displayName}"
    tryToRefresh()
}

// Implementation for capability.PushableButton
def push(buttonNumber) {
    String buttonName = BUTTONS.find { it.value[0] == "${buttonNumber}" }?.value?.getAt(1)
    if (buttonName == null) return Log.warn("Cannot push button ${buttonNumber} because it is not defined")
    Utils.sendEvent name:"pushed", value:buttonNumber, type:"digital", isStateChange:true, descriptionText:"Button ${buttonNumber} (${buttonName}) was pressed"
}

// Implementation for capability.Refresh
def refresh(buttonPress = true) {
    if (buttonPress) {
        Log.warn "Refreshing device current state ..."
        if (device.currentValue("powerSource", true) == "battery") {
            Log.warn '[IMPORTANT] Click the "Refresh" button immediately after pushing any button on the device in order to first wake it up!'
        }
    }

    List<String> cmds = []
    cmds += zigbee.readAttribute(0x0006, 0x0000, [destEndpoint:0x01, ]) // OnOff - Switch 1
    cmds += zigbee.readAttribute(0x0006, 0x0000, [destEndpoint:0x02, ]) // OnOff - Switch 2
    cmds += zigbee.readAttribute(0x0B04, 0x0604, [destEndpoint:0x01, ]) // PowerMultiplier
    cmds += zigbee.readAttribute(0x0B04, 0x0605, [destEndpoint:0x01, ]) // PowerDivisor
    cmds += zigbee.readAttribute(0x0B04, 0x050B, [destEndpoint:0x01, ]) // ActivePower
    cmds += zigbee.readAttribute(0x0702, 0x0301, [destEndpoint:0x01, ]) // EnergyMultiplier
    cmds += zigbee.readAttribute(0x0702, 0x0302, [destEndpoint:0x01, ]) // EnergyDivisor
    cmds += zigbee.readAttribute(0x0702, 0x0000, [destEndpoint:0x01, ]) // EnergySumation
    cmds += zigbee.readAttribute(0xFCC0, 0x00F7, [mfgCode:"0x115F", destEndpoint:0x01, ]) // LumiSpecific
    Utils.sendZigbeeCommands cmds
}

// Implementation for capability.FirmwareUpdate
def updateFirmware() {
    Log.info "Looking for firmware updates ..."
    if (device.currentValue("powerSource", true) == "battery") {
        Log.warn '[IMPORTANT] Click the "Update Firmware" button immediately after pushing any button on the device in order to first wake it up!'
    }
    Utils.sendZigbeeCommands(zigbee.updateFirmware())
}

// ===================================================================================================================
// Handle incoming Zigbee messages
// ===================================================================================================================

def parse(String description) {
    Log.debug "description=[${description}]"

    // Auto-Configure device: configure() was not called for this driver version
    if (state.lastCx != DRIVER_VERSION) {
        state.lastCx = DRIVER_VERSION
        runInMillis(1500, "autoConfigure")
    }

    // Extract msg
    def msg = [:]
    if (description.startsWith("zone status")) msg += [ clusterInt:0x500, commandInt:0x00, isClusterSpecific:true ]
    if (description.startsWith("enroll request")) msg += [ clusterInt:0x500, commandInt:0x01, isClusterSpecific:true ]

    msg += zigbee.parseDescriptionAsMap description
    if (msg.containsKey("endpoint")) msg.endpointInt = Integer.parseInt(msg.endpoint, 16)
    if (msg.containsKey("sourceEndpoint")) msg.endpointInt = Integer.parseInt(msg.sourceEndpoint, 16)
    if (msg.containsKey("cluster")) msg.clusterInt = Integer.parseInt(msg.cluster, 16)
    if (msg.containsKey("command")) msg.commandInt = Integer.parseInt(msg.command, 16)
    Log.debug "msg=[${msg}]"

    state.lastRx = now()

    // If we sent a Zigbee command in the last 3 seconds, we assume that this Zigbee event is a consequence of this driver doing something
    // Therefore, we mark this event as "digital"
    String type = state.containsKey("lastTx") && (now() - state.lastTx < 3000) ? "digital" : "physical"

    switch (msg) {

        // ---------------------------------------------------------------------------------------------------------------
        // Handle Aqara Dual Relay Module T2 (DCM-K01) specific Zigbee messages
        // ---------------------------------------------------------------------------------------------------------------

        // ---------------------------------------------------------------------------------------------------------------
        // Handle capabilities Zigbee messages
        // ---------------------------------------------------------------------------------------------------------------
        
        // Events for devices.Aqara_DCM-K01
        
        // Switch was flipped
        case { contains it, [clusterInt:0x0012, commandInt:0x0A] }:
            def button = msg.endpointInt == 0x01 ? BUTTONS.S1 : BUTTONS.S2
            return Utils.sendEvent(name:"pushed", value:button[0], type:"physical", isStateChange:true, descriptionText:"Button ${button[0]} (${button[1]}) was pushed")
        
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
            // Utils.sendZigbeeCommands(["he raw 0x${device.deviceNetworkId} 0x01 0x01 0x0019 {0901 00 00 64}"])
            // Utils.sendZigbeeCommands(["he raw 0x${device.deviceNetworkId} 0x01 0x01 0x0019 {0901 00 01 64 5F11}"])
            // Utils.sendZigbeeCommands(["he raw 0x${device.deviceNetworkId} 0x01 0x01 0x0019 {0901 00 02 64 5F11 1019}"])
            // Utils.sendZigbeeCommands(["he raw 0x${device.deviceNetworkId} 0x01 0x01 0x0019 {0901 00 03 64 5F11 1019 1B000000}"])
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
        
            String temperature = convertTemperatureIfNeeded(Integer.parseInt(msg.value[4..5], 16), "C", 0)
            Utils.sendEvent name:"temperature", value:temperature, unit:"°${location.temperatureScale}", descriptionText:"Temperature is ${temperature} °${location.temperatureScale}", type:type
        
            Integer powerOutageCount = Integer.parseInt(msg.value[12..13] + msg.value[10..11], 16);
            Utils.sendEvent name:"powerOutageCount", value:powerOutageCount, descriptionText:"Power outage count is ${powerOutageCount}", type:type
        
            String softwareBuild = "0.0.0_" + [
                "${Integer.parseInt(msg.value[44..45], 16)}",
                "${Integer.parseInt(msg.value[42..43], 16)}",
                "${Integer.parseInt(msg.value[40..41], 16)}"
            ].join("").padLeft(4, "0")
            Utils.dataValue("softwareBuild", softwareBuild)
        
            def energy = Math.round(Float.intBitsToFloat(Integer.parseInt("${msg.value[82..83]}${msg.value[80..81]}${msg.value[78..79]}${msg.value[76..77]}", 16))) / 1000
            //Utils.sendEvent name:"energy", value:energy, unit:"kWh", descriptionText:"Energy is ${energy} kWh", type:type
        
            def voltage = Math.round(Float.intBitsToFloat(Integer.parseInt("${msg.value[94..95]}${msg.value[92..93]}${msg.value[90..91]}${msg.value[88..89]}", 16))) / 10
            //Utils.sendEvent name:"voltage", value:voltage, unit:"V", descriptionText:"Voltage is ${voltage} V", type:type
        
            def power = Math.round(Float.intBitsToFloat(Integer.parseInt("${msg.value[106..107]}${msg.value[104..105]}${msg.value[102..103]}${msg.value[100..101]}", 16)))
            //Utils.sendEvent name:"power", value:power, unit:"W", descriptionText:"Power is ${power} W", type:type
        
            def amperage = Math.round(Float.intBitsToFloat(Integer.parseInt("${msg.value[118..119]}${msg.value[116..117]}${msg.value[114..115]}${msg.value[112..113]}", 16))) / 1000
            //Utils.sendEvent name:"amperage", value:amperage, unit:"A", descriptionText:"Amperage is ${amperage} W", type:type
        
            return Utils.processedZclMessage("${msg.commandInt == 0x0A ? "Report" : "Read"} Attributes Response", "Temperature=${temperature}, PowerOutageCount=${powerOutageCount}, SoftwareBuild=${softwareBuild}, Energy=${energy}kWh, Voltage=${voltage}V, Power=${power}W, Amperage=${amperage}A")
        
        // Other events that we expect but are not usefull for devices.Aqara_DCM-K01 behavior
        case { contains it, [clusterInt:0xFCC0, commandInt:0x07] }:
            return Utils.processedZclMessage("Configure Reporting Response", "attribute=LumiSpecific, data=${msg.data}")
        case { contains it, [clusterInt:0xFCC0, commandInt:0x0A, attrInt:0x0517] }:
             return Utils.processedZclMessage("Report Attributes Response", "PowerOnBehavior=${msg.value}")
        case { contains it, [clusterInt:0xFCC0, commandInt:0x0A, attrInt:0x0200] }:
             return Utils.processedZclMessage("Report Attributes Response", "OperationMode=${msg.value}, Switch=${msg.endpoint}")
        case { contains it, [clusterInt:0xFCC0, commandInt:0x0A, attrInt:0x000A] }:
             return Utils.processedZclMessage("Report Attributes Response", "switchType=${msg.value}")
        case { contains it, [clusterInt:0xFCC0, commandInt:0x0A, attrInt:0x02D0] }:
             return Utils.processedZclMessage("Report Attributes Response", "Interlock=${msg.value}")
        case { contains it, [clusterInt:0xFCC0, commandInt:0x0A, attrInt:0x0289] }:
             return Utils.processedZclMessage("Report Attributes Response", "RelayMode=${msg.value}")
        case { contains it, [clusterInt:0xFCC0, commandInt:0x0A, attrInt:0x00EB] }:
             return Utils.processedZclMessage("Report Attributes Response", "PulseLength=${msg.value}")
        case { contains it, [clusterInt:0xFCC0, commandInt:0x04] }:  // Write Attribute Response
            return
        
        // Events for capability.PowerMeter
        
        // Report/Read Attributes Reponse: ActivePower
        case { contains it, [clusterInt:0x0B04, commandInt:0x0A, attrInt:0x050B] }:
        case { contains it, [clusterInt:0x0B04, commandInt:0x01, attrInt:0x050B] }:
            def power = Integer.parseInt(msg.value, 16) * (state.powerMultiplier ?: 1) / (state.powerDivisor ?: 1)
            Utils.sendEvent name:"power", value:power, unit:"W", descriptionText:"Power is ${power} W", type:type
            return Utils.processedZclMessage("${msg.commandInt == 0x0A ? "Report" : "Read"} Attributes Response", "ActivePower=${msg.value}")
        
        // Report/Read Attributes Reponse: EnergySummation
        case { contains it, [clusterInt:0x0702, commandInt:0x0A, attrInt:0x0000] }:
        case { contains it, [clusterInt:0x0702, commandInt:0x01, attrInt:0x0000] }:
            def energy = Integer.parseInt(msg.value, 16) * (state.energyMultiplier ?: 1) / (state.energyDivisor ?: 1)
            Utils.sendEvent name:"energy", value:energy, unit:"kWh", descriptionText:"Energy is ${energy} kWh", type:type
            return Utils.processedZclMessage("${msg.commandInt == 0x0A ? "Report" : "Read"} Attributes Response", "EnergySummation=${msg.value}")
        
        // Read Attributes Reponse: PowerMultiplier
        case { contains it, [clusterInt:0x0B04, commandInt:0x01, attrInt:0x0604] }:
            state.powerMultiplier = Integer.parseInt(msg.value, 16)
            return Utils.processedZclMessage("Read Attributes Response", "PowerMultiplier=${msg.value}")
        
        // Read Attributes Reponse: PowerDivisor
        case { contains it, [clusterInt:0x0B04, commandInt:0x01, attrInt:0x0605] }:
            state.powerDivisor = Integer.parseInt(msg.value, 16)
            return Utils.processedZclMessage("Read Attributes Response", "PowerDivisor=${msg.value}")
        
        // Read Attributes Reponse: EnergyMultiplier
        case { contains it, [clusterInt:0x0702, commandInt:0x01, attrInt:0x0301] }:
            state.energyMultiplier = Integer.parseInt(msg.value, 16)
            return Utils.processedZclMessage("Read Attributes Response", "EnergyMultiplier=${msg.value}")
        
        // Read Attributes Reponse: EnergyDivisor
        case { contains it, [clusterInt:0x0702, commandInt:0x01, attrInt:0x0302] }:
            state.energyDivisor = Integer.parseInt(msg.value, 16)
            return Utils.processedZclMessage("Read Attributes Response", "EnergyDivisor=${msg.value}")
        
        // Other events that we expect but are not usefull for capability.PowerMeter behavior
        case { contains it, [clusterInt:0x0B04, commandInt:0x07] }:
            return Utils.processedZclMessage("Configure Reporting Response", "attribute=ActivePower, data=${msg.data}")
        case { contains it, [clusterInt:0x0702, commandInt:0x07] }:
            return Utils.processedZclMessage("Configure Reporting Response", "attribute=CurrentSummation, data=${msg.data}")
        
        // Events for capability.MultiRelay
        
        // Report/Read Attributes: OnOff
        case { contains it, [clusterInt:0x0006, commandInt:0x0A, attrInt:0x0000] }:
        case { contains it, [clusterInt:0x0006, commandInt:0x01, attrInt:0x0000] }:
            Integer moduleNumber = msg.endpointInt
            String newState = msg.value == "00" ? "off" : "on"
        
            // Send event to module child device (only if state needs to change)
            def childDevice = fetchChildDevice(moduleNumber)
            if (newState != childDevice.currentValue("switch", true)) {
                childDevice.parse([[name:"switch", value:newState, descriptionText:"${childDevice.displayName} was turned ${newState}", type:type]])
            }
        
            return Utils.processedZclMessage("${msg.commandInt == 0x0A ? "Report" : "Read"} Attributes Response", "Module=${moduleNumber}, Switch=${newState}")
        
        // Other events that we expect but are not usefull for capability.MultiRelay behavior
        case { contains it, [clusterInt:0x0006, commandInt:0x07] }:
            return Utils.processedZclMessage("Configure Reporting Response", "attribute=switch, data=${msg.data}")

        // ---------------------------------------------------------------------------------------------------------------
        // Handle common messages (e.g.: received during pairing when we query the device for information)
        // ---------------------------------------------------------------------------------------------------------------

        // Device_annce: Welcome back! let's sync state.
        case { contains it, [endpointInt:0x00, clusterInt:0x0013, commandInt:0x00] }:
            Log.warn "Rejoined the Zigbee mesh; refreshing device state in 3 seconds ..."
            return runIn(3, "tryToRefresh")

        // Report/Read Attributes Response (Basic cluster)
        case { contains it, [clusterInt:0x0000, commandInt:0x01] }:
        case { contains it, [clusterInt:0x0000, commandInt:0x0A] }:
            Utils.zigbeeDataValue(msg.attrInt, msg.value)
            msg.additionalAttrs?.each { Utils.zigbeeDataValue(it.attrInt, it.value) }
            return Utils.processedZclMessage("${msg.commandInt == 0x0A ? "Report" : "Read"} Attributes Response", "cluster=0x${msg.cluster}, attribute=0x${msg.attrId}, value=${msg.value}")

        // Mgmt_Leave_rsp
        case { contains it, [endpointInt:0x00, clusterInt:0x8034, commandInt:0x00] }:
            return Log.warn("Device is leaving the Zigbee mesh. See you later, Aligator!")

        // Ignore the following Zigbee messages
        case { contains it, [commandInt:0x0A, isClusterSpecific:false] }:              // ZCL: Attribute report we don't care about (configured by other driver)
        case { contains it, [commandInt:0x0B, isClusterSpecific:false] }:              // ZCL: Default Response
        case { contains it, [clusterInt:0x0003, commandInt:0x01] }:                    // ZCL: Identify Query Command
            return Utils.processedZclMessage("Ignored", "endpoint=${msg.endpoint}, cluster=0x${msg.clusterId}, command=0x${msg.command}, data=${msg.data}")

        case { contains it, [endpointInt:0x00, clusterInt:0x8001, commandInt:0x00] }:  // ZDP: IEEE_addr_rsp
        case { contains it, [endpointInt:0x00, clusterInt:0x8004, commandInt:0x00] }:  // ZDP: Simple_Desc_rsp
        case { contains it, [endpointInt:0x00, clusterInt:0x8005, commandInt:0x00] }:  // ZDP: Active_EP_rsp
        case { contains it, [endpointInt:0x00, clusterInt:0x0006, commandInt:0x00] }:  // ZDP: MatchDescriptorRequest
        case { contains it, [endpointInt:0x00, clusterInt:0x801F, commandInt:0x00] }:  // ZDP: Parent_annce_rsp
        case { contains it, [endpointInt:0x00, clusterInt:0x8021, commandInt:0x00] }:  // ZDP: Mgmt_Bind_rsp
        case { contains it, [endpointInt:0x00, clusterInt:0x8022, commandInt:0x00] }:  // ZDP: Mgmt_Unbind_rsp
        case { contains it, [endpointInt:0x00, clusterInt:0x8032, commandInt:0x00] }:  // ZDP: Mgmt_Rtg_rsp
        case { contains it, [endpointInt:0x00, clusterInt:0x8038, commandInt:0x00] }:  // ZDP: Mgmt_NWK_Update_notify
            return Utils.processedZdpMessage("Ignored", "cluster=0x${msg.clusterId}, command=0x${msg.command}, data=${msg.data}")

        // ---------------------------------------------------------------------------------------------------------------
        // Unexpected Zigbee message
        // ---------------------------------------------------------------------------------------------------------------
        default:
            Log.error "Sent unexpected Zigbee message: description=${description}, msg=${msg}"
    }
}

// ===================================================================================================================
// Logging helpers (something like this should be part of the SDK and not implemented by each driver)
// ===================================================================================================================

@Field Map Log = [
    debug: { if (logLevel == "1") log.debug "${device.displayName} ${it.uncapitalize()}" },
    info:  { if (logLevel <= "2") log.info  "${device.displayName} ${it.uncapitalize()}" },
    warn:  { if (logLevel <= "3") log.warn  "${device.displayName} ${it.uncapitalize()}" },
    error: { log.error "${device.displayName} ${it.uncapitalize()}" }
]

// ===================================================================================================================
// Helper methods (keep them simple, keep them dumb)
// ===================================================================================================================

@Field def Utils = [
    sendZigbeeCommands: { List<String> cmds ->
        if (cmds.isEmpty()) return
        List<String> send = delayBetween(cmds.findAll { !it.startsWith("delay") }, 1000)
        Log.debug "◀ Sending Zigbee messages: ${send}"
        state.lastTx = now()
        sendHubCommand new hubitat.device.HubMultiAction(send, hubitat.device.Protocol.ZIGBEE)
    },

    sendEvent: { Map event ->
        if (device.currentValue(event.name, true) != event.value || event.isStateChange) {
            Log.info "${event.descriptionText} [${event.type}]"
        } else {
            Log.debug "${event.descriptionText} [${event.type}]"
        }
        sendEvent event
    },

    dataValue: { String key, String value ->
        if (value == null || value == "") return
        Log.debug "Update data value: ${key}=${value}"
        updateDataValue key, value
    },

    zigbeeDataValue: { Integer attrInt, String value ->
        switch (attrInt) {
            case 0x0001: return Utils.dataValue("application", value)
            case 0x0003: return Utils.dataValue("hwVersion", value)
            case 0x0004: return Utils.dataValue("manufacturer", value)
            case 0x000A: return Utils.dataValue("type", "${!value ? "" : (value.split("") as List).collate(2).collect { "${Integer.parseInt(it.join(), 16) as char}" }.join()}")
            case 0x0005: return Utils.dataValue("model", value)
            case 0x4000: return Utils.dataValue("softwareBuild", value)
        }
    },

    processedZclMessage: { String type, String details ->
        Log.debug "▶ Processed ZCL message: type=${type}, ${details}"
    },

    processedZdpMessage: { String type, String details ->
        Log.debug "▶ Processed ZDO message: type=${type}, ${details}"
    },

    payload: { String value ->
        return value.replace("0x", "").split("(?<=\\G.{2})").reverse().join("")
    }
]

// switch/case syntactic sugar
private boolean contains(Map msg, Map spec) {
    msg.keySet().containsAll(spec.keySet()) && spec.every { it.value == msg[it.key] }
}

// Call refresh() if available
private tryToRefresh() {
    try { refresh(false) } catch(ex) {}
}
