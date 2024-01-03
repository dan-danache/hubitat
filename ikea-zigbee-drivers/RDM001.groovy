/**
 * Philips Wall Switch Module (RDM001)
 *
 * @see https://dan-danache.github.io/hubitat/ikea-zigbee-drivers/
 * @see https://zigbee.blakadder.com/Philips_RDM001.html
 */
import groovy.time.TimeCategory
import groovy.transform.Field

@Field static final String DRIVER_NAME = "Philips Wall Switch Module (RDM001)"
@Field static final String DRIVER_VERSION = "3.7.0"

// Fields for capability.HealthCheck
@Field static final Map<String, String> HEALTH_CHECK = [
    "schedule": "0 0 0/1 ? * * *", // Health will be checked using this cron schedule
    "thereshold": "43200" // When checking, mark the device as offline if no Zigbee message was received in the last 43200 seconds
]

// Fields for capability.PushableButton
@Field static final Map<String, List<String>> BUTTONS = [
    "BUTTON_1": ["1", "Left"],
    "BUTTON_2": ["2", "Right"],
]

// Fields for capability.RDM001_SwitchStyle
@Field static final Map<Integer, String> RDM001_SWITCH_STYLE = [
    "00": "Single Rocker",
    "01": "Single Push Button",
    "02": "Dual Rocker",
    "03": "Dual Push Button"
]

metadata {
    definition(name:DRIVER_NAME, namespace:"dandanache", author:"Dan Danache", importUrl:"https://raw.githubusercontent.com/dan-danache/hubitat/master/ikea-zigbee-drivers/RDM001.groovy") {
        capability "Configuration"
        capability "Battery"
        capability "HealthCheck"
        capability "PowerSource"
        capability "HoldableButton"
        capability "PushableButton"
        capability "Refresh"
        capability "ReleasableButton"

        // For firmware: 1.0.5 (100B-011C-0000041A)
        fingerprint profileId:"0104", endpointId:"01", inClusters:"0000,0001,0003,FC00", outClusters:"0003,0004,0006,0008,0019", model:"RDM001", manufacturer:"Signify Netherlands B.V."
        
        // Attributes for capability.HealthCheck
        attribute "healthStatus", "enum", ["offline", "online", "unknown"]
    }
    
    // Commands for capability.FirmwareUpdate
    command "updateFirmware"

    preferences {
        input(
            name: "logLevel",
            type: "enum",
            title: "Log verbosity",
            description: "<small>Choose the kind of messages that appear in the \"Logs\" section.</small>",
            options: [
                "1" : "Debug - log everything",
                "2" : "Info - log important events",
                "3" : "Warning - log events that require attention",
                "4" : "Error - log errors"
            ],
            defaultValue: "1",
            required: true
        )
        
        // Inputs for capability.RDM001_SwitchStyle
        input(
            name: "switchStyle",
            type: "enum",
            title: "Switch Style",
            description: "<small>Select physical switch button configuration</small>",
            options: RDM001_SWITCH_STYLE,
            defaultValue: "02",
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
    Log.info "🛠️ logLevel = ${logLevel}"
    
    // Preferences for capability.HealthCheck
    schedule HEALTH_CHECK.schedule, "healthCheck"
    
    // Preferences for capability.RDM001_SwitchStyle
    if (switchStyle == null) {
        switchStyle = "02"
        device.updateSetting("switchStyle", [value:switchStyle, type:"enum"])
    }
    Log.info "🛠️ switchStyle = ${switchStyle} (${RDM001_SWITCH_STYLE[switchStyle]})"
    Utils.sendZigbeeCommands(["he raw ${device.deviceNetworkId} 0x01 0x01 0x0000 {040B104302 3400 30 ${switchStyle}}"])
    
    Integer numberOfButtons = (switchStyle == "00" || switchStyle == "01") ? 1 : 2
    sendEvent name:"numberOfButtons", value:numberOfButtons, descriptionText:"Number of buttons is ${numberOfButtons}"
    Log.info "🛠️ numberOfButtons = ${numberOfButtons}"

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

// Helpers for capability.HealthCheck
def healthCheck() {
    Log.debug '⏲️ Automatically running health check'
    String healthStatus = state.lastRx == 0 || state.lastRx == null ? "unknown" : (now() - state.lastRx < Integer.parseInt(HEALTH_CHECK.thereshold) * 1000 ? "online" : "offline")
    Utils.sendEvent name:"healthStatus", value:healthStatus, type:"physical", descriptionText:"Health status is ${healthStatus}"
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
    updated(true)

    // Clear data (keep firmwareMT information though)
    device.getData()?.collect { it.key }.each { if (it != "firmwareMT") device.removeDataValue it }

    // Clear state
    state.clear()
    state.lastTx = 0
    state.lastRx = 0
    state.lastCx = DRIVER_VERSION

    List<String> cmds = []

    // Configure Philips Wall Switch Module (RDM001) specific Zigbee reporting
    // -- No reporting needed

    // Add Philips Wall Switch Module (RDM001) specific Zigbee binds
    cmds += "zdo bind 0x${device.deviceNetworkId} 0x01 0x01 0xFC00 {${device.zigbeeId}} {}" // Hue Button
    
    // Configuration for capability.Battery
    cmds += "he cr 0x${device.deviceNetworkId} 0x01 0x0001 0x0021 0x20 0x0000 0x4650 {02} {}" // Report BatteryPercentage (uint8) at least every 5 hours (min 1% change)
    cmds += "zdo bind 0x${device.deviceNetworkId} 0x01 0x01 0x0001 {${device.zigbeeId}} {}" // Power Configuration cluster
    cmds += zigbee.readAttribute(0x0001, 0x0021)  // BatteryPercentage
    
    // Configuration for capability.HealthCheck
    sendEvent name:"healthStatus", value:"online", descriptionText:"Health status initialized to online"
    sendEvent name:"checkInterval", value:3600, unit:"second", descriptionText:"Health check interval is 3600 seconds"
    
    // Configuration for capability.PowerSource
    sendEvent name:"powerSource", value:"unknown", type:"digital", descriptionText:"Power source initialized to unknown"
    cmds += zigbee.readAttribute(0x0000, 0x0007) // PowerSource
    
    // Configuration for capability.PushableButton
    Integer numberOfButtons = BUTTONS.count{_ -> true}
    sendEvent name:"numberOfButtons", value:numberOfButtons, descriptionText:"Number of buttons is ${numberOfButtons}"
    
    // Configuration for capability.RDM001_SwitchStyle
    cmds += "he cr 0x${device.deviceNetworkId} 0x01 0x0001 0x0021 0x20 0x0000 0x4650 {01} {}" // Report battery at least every 5 hours
    cmds += "zdo bind 0x${device.deviceNetworkId} 0x01 0x01 0x0001 {${device.zigbeeId}} {}" // Power Configuration cluster
    cmds += zigbee.readAttribute(0x0001, 0x0021)  // BatteryPercentage
    
    cmds += "he cr 0x${device.deviceNetworkId} 0x01 0x0000 0x0034 0x30 0x0000 0x0000 {} {0x100B}" // Report switch style whenever it changes
    cmds += "zdo bind 0x${device.deviceNetworkId} 0x01 0x01 0x0000 {${device.zigbeeId}} {}" // Basic cluster
    
    cmds += zigbee.writeAttribute(0x0000, 0x0031, 0x19, 0x0B00, [mfgCode: "0x100B"]) // Write Philips magic attribute

    // Query Basic cluster attributes
    cmds += zigbee.readAttribute(0x0000, [0x0001, 0x0003, 0x0004, 0x0005, 0x000A, 0x4000]) // ApplicationVersion, HWVersion, ManufacturerName, ModelIdentifier, ProductCode, SWBuildID
    Utils.sendZigbeeCommands cmds

    Log.info "Configuration done; refreshing device current state in 10 seconds ..."
    runIn(10, "tryToRefresh")
}
private autoConfigure() {
    Log.warn "Detected that this device is not properly configured for this driver version (lastCx != ${DRIVER_VERSION})"
    configure(true)
}

// Implementation for capability.HealthCheck
def ping() {
    Log.warn "ping ..."
    Utils.sendZigbeeCommands(zigbee.readAttribute(0x0000, 0x0000))
    Log.debug "Ping command sent to the device; we'll wait 5 seconds for a reply ..."
    runIn 5, "pingExecute"
}

def pingExecute() {
    if (state.lastRx == 0) {
        return Log.info("Did not sent any messages since it was last configured")
    }

    Date now = new Date(Math.round(now() / 1000) * 1000)
    Date lastRx = new Date(Math.round(state.lastRx / 1000) * 1000)
    String lastRxAgo = TimeCategory.minus(now, lastRx).toString().replace(".000 seconds", " seconds")
    Log.info "Sent last message at ${lastRx.format("yyyy-MM-dd HH:mm:ss", location.timeZone)} (${lastRxAgo} ago)"

    Date thereshold = new Date(Math.round(state.lastRx / 1000 + Integer.parseInt(HEALTH_CHECK.thereshold)) * 1000)
    String theresholdAgo = TimeCategory.minus(thereshold, lastRx).toString().replace(".000 seconds", " seconds")
    Log.info "Will be marked as offline if no message is received for ${theresholdAgo} (hardcoded)"

    String offlineMarkAgo = TimeCategory.minus(thereshold, now).toString().replace(".000 seconds", " seconds")
    Log.info "Will be marked as offline if no message is received until ${thereshold.format("yyyy-MM-dd HH:mm:ss", location.timeZone)} (${offlineMarkAgo} from now)"
}

// Implementation for capability.HoldableButton
def hold(buttonNumber) {
    String buttonName = BUTTONS.find { it.value[0] == "${buttonNumber}" }?.value?.getAt(1)
    if (buttonName == null) return Log.warn("Cannot hold button ${buttonNumber} because it is not defined")
    Utils.sendEvent name:"held", value:buttonNumber, type:"digital", isStateChange:true, descriptionText:"Button ${buttonNumber} (${buttonName}) was held"
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
    cmds += zigbee.readAttribute(0x0001, 0x0021) // BatteryPercentage
    cmds += zigbee.writeAttribute(0x0000, 0x0031, 0x19, 0x0B00, [mfgCode:"0x100B"]) // Philips magic attribute
    Utils.sendZigbeeCommands cmds
}

// Implementation for capability.ReleasableButton
def release(buttonNumber) {
    String buttonName = BUTTONS.find { it.value[0] == "${buttonNumber}" }?.value?.getAt(1)
    if (buttonName == null) return Log.warn("Cannot release button ${buttonNumber} because it is not defined")
    Utils.sendEvent name:"released", value:buttonNumber, type:"digital", isStateChange:true, descriptionText:"Button ${buttonNumber} (${buttonName}) was released"
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
    def msg = zigbee.parseDescriptionAsMap description
    if (msg.containsKey("endpoint")) msg.endpointInt = Integer.parseInt(msg.endpoint, 16)
    if (msg.containsKey("sourceEndpoint")) msg.endpointInt = Integer.parseInt(msg.sourceEndpoint, 16)
    if (msg.clusterInt == null) msg.clusterInt = Integer.parseInt(msg.cluster, 16)
    msg.commandInt = Integer.parseInt(msg.command, 16)
    Log.debug "msg=[${msg}]"

    state.lastRx = now()
    
    // Parse for capability.HealthCheck
    if (device.currentValue("healthStatus", true) != "online") {
        Utils.sendEvent name:"healthStatus", value:"online", type:"digital", descriptionText:"Health status changed to online"
    }

    // If we sent a Zigbee command in the last 3 seconds, we assume that this Zigbee event is a consequence of this driver doing something
    // Therefore, we mark this event as "digital"
    String type = state.containsKey("lastTx") && (now() - state.lastTx < 3000) ? "digital" : "physical"

    switch (msg) {

        // ---------------------------------------------------------------------------------------------------------------
        // Handle Philips Wall Switch Module (RDM001) specific Zigbee messages
        // ---------------------------------------------------------------------------------------------------------------

        // Button was pressed := { 16:Button, 08:EventType, 08:NextValueType, 08:Action, 08:NextValueType, 16:DurationRotation}
        // EventType := { 0x00:Button, 0x01:Rotary }
        // Action := { 0x00:Press, 0x01:Hold/Start, 0x02:Release/Repeat, 0x03:LongRelease }
        // [02, 00,  00,  30,  02,  21,  01, 00] -> Button=2(0x0002), EventType=Button(0x00), NextValueType=enum8(0x30), Action=Release(0x02), NextValueType=uint16(0x21), DurationRotation=0x0001
        case { contains it, [clusterInt:0xFC00, commandInt:0x00] }:
            def button = msg.data[0] == "01" ? BUTTONS.BUTTON_1 : BUTTONS.BUTTON_2
        
            // Rocker Mode: Only listen to "Press" (00) actions
            if (switchStyle == "00" || switchStyle == "02") {
                if (msg.data[4] != "00") return
                return Utils.sendEvent(name:"pushed", value:button[0], type:"physical", isStateChange:true, descriptionText:"Button ${button[0]} (${button[1]}) was pushed")
            }
        
            // Dimmer Mode: Only listen to Release (02), Hold (01) and LongRelease (03)
            switch (msg.data[4]) {
                case "02": return Utils.sendEvent(name:"pushed", value:button[0], type:"physical", isStateChange:true, descriptionText:"Button ${button[0]} (${button[1]}) was pushed")
                case "01": return Utils.sendEvent(name:"held", value:button[0], type:"physical", isStateChange:true, descriptionText:"Button ${button[0]} (${button[1]}) was held")
                case "03": return Utils.sendEvent(name:"released", value:button[0], type:"physical", isStateChange:true, descriptionText:"Button ${button[0]} (${button[1]}) was released")
            }
            return

        // ---------------------------------------------------------------------------------------------------------------
        // Handle capabilities Zigbee messages
        // ---------------------------------------------------------------------------------------------------------------
        
        // Events for capability.Battery
        
        // Report/Read Attributes Reponse: BatteryPercentage
        case { contains it, [clusterInt:0x0001, commandInt:0x0A, attrInt:0x0021] }:
        case { contains it, [clusterInt:0x0001, commandInt:0x01, attrInt:0x0021] }:
            Integer percentage = Integer.parseInt(msg.value, 16)
        
            // 0xFF represents an invalid battery percentage value, so we just ignore it
            if (percentage == 0xFF) return Log.warn("Ignored invalid reported battery percentage value: 0xFF")
        
            percentage =  percentage / 2
            Utils.sendEvent name:"battery", value:percentage, unit:"%", type:"physical", descriptionText:"Battery is ${percentage}% full"
            return Utils.processedZclMessage("${msg.commandInt == 0x0A ? "Report" : "Read"} Attributes Response", "BatteryPercentage=${percentage}%")
        
        // Other events that we expect but are not usefull for capability.Battery behavior
        case { contains it, [clusterInt:0x0001, commandInt:0x07] }:  // ConfigureReportingResponse
            return
        
        // Events for capability.HealthCheck
        case { contains it, [clusterInt:0x0000, attrInt:0x0000] }:
            return Log.warn("... pong")
        
        // Read Attributes Reponse: PowerSource
        case { contains it, [clusterInt:0x0000, commandInt:0x01, attrInt:0x0007] }:
            String powerSource = "unknown"
        
            // PowerSource := { 0x00:Unknown, 0x01:MainsSinglePhase, 0x02:MainsThreePhase, 0x03:Battery, 0x04:DC, 0x05:EmergencyMainsConstantlyPowered, 0x06:EmergencyMainsAndTransferSwitch }
            switch (msg.value) {
                case "01":
                case "02":
                case "05":
                case "06":
                    powerSource = "mains"
                    break
                case "03":
                    powerSource = "battery"
                    break
                case "04":
                    powerSource = "dc"
                    break
            }
            Utils.sendEvent name:"powerSource", value:powerSource, type:"digital", descriptionText:"Power source is ${powerSource}"
            return Utils.processedZclMessage("Read Attributes Response", "PowerSource=${msg.value}")
        
        // Events for capability.RDM001_SwitchStyle (Write Attributes Response)
        case { contains it, [endpointInt:0x01, clusterInt:0x0000, commandInt:0x04, isClusterSpecific:false, isManufacturerSpecific:true, manufacturerId:"100B"] }:
            return Log.info("Switch Style successfully configured!")
        
        // Events for capability.RDM001_SwitchStyle (Read Attributes Response)
        case { contains it, [endpointInt:0x01, clusterInt:0x0000, commandInt:0x01, attrInt:0x0034] }:
        case { contains it, [endpointInt:0x01, clusterInt:0x0000, commandInt:0x0A, attrInt:0x0034] }:
            device.clearSetting "switchStyle"
            device.removeSetting "vswitchStyle"
            device.updateSetting "switchStyle", msg.value
        
            Integer numberOfButtons = msg.value == "01" || msg.value == "02" ? 1 : 2
            sendEvent name:"numberOfButtons", value:numberOfButtons, descriptionText:"Number of buttons is ${numberOfButtons}"
            Log.info "🛠️ numberOfButtons = ${numberOfButtons}"
            return
        
        // Other events that we expect but are not usefull for capability.RDM001_SwitchStyle behavior
        case { contains it, [clusterInt:0x0000, commandInt:0x07] }:  // ConfigureReportingResponse
            return

        // ---------------------------------------------------------------------------------------------------------------
        // Handle common messages (e.g.: received during pairing when we query the device for information)
        // ---------------------------------------------------------------------------------------------------------------

        // Device_annce: Welcome back! let's sync state.
        case { contains it, [endpointInt:0x00, clusterInt:0x0013, commandInt:0x00] }:
            Log.warn "Rejoined the Zigbee mesh; refreshing device state in 3 seconds ..."
            return runIn(3, "tryToRefresh")

        // Read Attributes Response (Basic cluster)
        case { contains it, [clusterInt:0x0000, commandInt:0x01] }:
            Utils.processedZclMessage("Read Attributes Response", "cluster=0x${msg.cluster}, attribute=0x${msg.attrId}, value=${msg.value}")
            Utils.zigbeeDataValue(msg.attrInt, msg.value)
            msg.additionalAttrs?.each { Utils.zigbeeDataValue(it.attrInt, it.value) }
            return

        // Mgmt_Leave_rsp
        case { contains it, [endpointInt:0x00, clusterInt:0x8034, commandInt:0x00] }:
            return Log.warn("Device is leaving the Zigbee mesh. See you later, Aligator!")

        // Ignore the following Zigbee messages
        case { contains it, [commandInt:0x0A] }:                                       // ZCL: Attribute report we don't care about (configured by other driver)
        case { contains it, [clusterInt:0x0003, commandInt:0x01] }:                    // ZCL: Identify Query Command
        case { contains it, [endpointInt:0x00, clusterInt:0x8001, commandInt:0x00] }:  // ZDP: IEEE_addr_rsp
        case { contains it, [endpointInt:0x00, clusterInt:0x8005, commandInt:0x00] }:  // ZDP: Active_EP_rsp
        case { contains it, [endpointInt:0x00, clusterInt:0x0006, commandInt:0x00] }:  // ZDP: MatchDescriptorRequest
        case { contains it, [endpointInt:0x00, clusterInt:0x8021, commandInt:0x00] }:  // ZDP: Mgmt_Bind_rsp
        case { contains it, [endpointInt:0x00, clusterInt:0x8038, commandInt:0x00] }:  // ZDP: Mgmt_NWK_Update_notify
            return

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

@Field def Map Log = [
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
        Log.debug "▶ Processed ZCL message: type=${type}, status=SUCCESS, ${details}"
    },

    processedZdoMessage: { String type, String details ->
        Log.debug "▶ Processed ZDO message: type=${type}, status=SUCCESS, ${details}"
    }
]

// switch/case syntactic sugar
private boolean contains(Map msg, Map spec) {
    msg.keySet().containsAll(spec.keySet()) && spec.every { it.value == msg[it.key] }
}

// Call refresh() if available
private tryToRefresh() {
    try { refresh(false) } catch(e) {}
}
