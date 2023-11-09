/**
 * IKEA Styrbar Remote Control N2 (E2002)
 *
 * @see https://dan-danache.github.io/hubitat/ikea-zigbee-drivers/
 * @see https://zigbee.blakadder.com/Ikea_E2002.html
 * @see https://ww8.ikea.com/ikeahomesmart/releasenotes/releasenotes.html
 * @see https://static.homesmart.ikea.com/releaseNotes/
 */
import groovy.time.TimeCategory
import groovy.transform.Field

@Field static final String DRIVER_NAME = "IKEA Styrbar Remote Control N2 (E2002)"
@Field static final String DRIVER_VERSION = "3.4.0"

// Fields for capability.HealthCheck
@Field static final Map<String, String> HEALTH_CHECK = [
    "schedule": "0 0 0/1 ? * * *", // Health will be checked using this cron schedule
    "thereshold": "43200" // When checking, mark the device as offline if no Zigbee message was received in the last 43200 seconds
]

// Fields for capability.PushableButton
@Field static final Map<String, List<String>> BUTTONS = [
    "PLUS": ["1", "🔆"],
    "MINUS": ["2", "🔅"],
    "NEXT": ["3", "Next"],
    "PREV": ["4", "Prev"],
]

metadata {
    definition(name:DRIVER_NAME, namespace:"dandanache", author:"Dan Danache", importUrl:"https://raw.githubusercontent.com/dan-danache/hubitat/master/ikea-zigbee-drivers/E2002.groovy") {
        capability "Configuration"
        capability "Battery"
        capability "HealthCheck"
        capability "HoldableButton"
        capability "PowerSource"
        capability "PushableButton"
        capability "Refresh"
        capability "ReleasableButton"

        // For firmware: 1.0.024
        fingerprint profileId:"0104", endpointId:"01", inClusters:"0000,0001,0003,0020,1000,FC57", outClusters:"0003,0006,0008,0019,1000", model:"Remote Control N2", manufacturer:"IKEA of Sweden"

        // For firmware: 2.4.5 (117C-11CB-02040005)
        fingerprint profileId:"0104", endpointId:"01", inClusters:"0000,0001,0003,0020,1000,FC57,FC7C", outClusters:"0003,0005,0006,0008,0019,1000", model:"Remote Control N2", manufacturer:"IKEA of Sweden"
        
        // Attributes for capability.HealthCheck
        attribute "healthStatus", "ENUM", ["offline", "online", "unknown"]
    }
    
    // Commands for capability.FirmwareUpdate
    command "updateFirmware"

    preferences {
        input(
            name: "logLevel",
            type: "enum",
            title: "Log verbosity",
            description: "<small>Select what type of messages are added in the \"Logs\" section</small>",
            options: [
                "1":"Debug - log everything",
                "2":"Info - log important events",
                "3":"Warning - log events that require attention",
                "4":"Error - log errors"
            ],
            defaultValue: "2",
            required: true
        )
    }
}

// ===================================================================================================================
// Implement default methods
// ===================================================================================================================

// Called when the device is first added
def installed() {
    Log.info "Installing device ..."
    Log.warn "[IMPORTANT] For battery-powered devices, make sure that you keep your device as close as you can to your Hubitat hub (or any other Zigbee router device) for at least 20 seconds. Otherwise it will successfully pair but it won't work properly!"
}

// Called when the "Save Preferences" button is clicked
def updated(auto = false) {
    Log.info "Saving preferences${auto ? " (auto)" : ""} ..."

    unschedule()
    if (logLevel == "1") runIn 1800, "logsOff"
    Log.info "🛠️ logLevel = ${logLevel}"
    
    // Preferences for capability.HealthCheck
    schedule HEALTH_CHECK.schedule, "healthCheck"
}

// ===================================================================================================================
// Capabilities helpers
// ===================================================================================================================

// Handler method for scheduled job to disable debug logging
def logsOff() {
   Log.info '⏲️ Automatically reverting log level to "Info"'
   device.updateSetting("logLevel",[ value:"2", type:"enum" ])
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
    Log.info "Configuring device${auto ? " (auto)" : ""} ..."
    if (!auto && device.currentValue("powerSource", true) == "battery") {
        Log.warn '[IMPORTANT] Click the "Configure" button immediately after pushing any button on the device in order to first wake it up!'
    }

    // Advertise driver name and value
    updateDataValue "driverName", DRIVER_NAME
    updateDataValue "driverVersion", DRIVER_VERSION

    // Apply preferences first
    updated(true)

    // Clear state
    state.clear()
    state.lastTx = 0
    state.lastRx = 0

    List<String> cmds = []

    // Configure IKEA Styrbar Remote Control N2 (E2002) specific Zigbee reporting
    // -- No reporting needed

    // Add IKEA Styrbar Remote Control N2 (E2002) specific Zigbee binds
    cmds += "zdo bind 0x${device.deviceNetworkId} 0x01 0x01 0x0005 {${device.zigbeeId}} {}" // Scenes cluster
    cmds += "zdo bind 0x${device.deviceNetworkId} 0x01 0x01 0x0006 {${device.zigbeeId}} {}" // On/Off cluster
    cmds += "zdo bind 0x${device.deviceNetworkId} 0x01 0x01 0x0008 {${device.zigbeeId}} {}" // Level Control cluster
    
    // Configuration for capability.Battery
    cmds += "he cr 0x${device.deviceNetworkId} 0x01 0x0001 0x0021 0x20 0x0000 0x9AB0 {01} {}" // Report battery at least every 11 hours
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

    // Query Basic cluster attributes
    cmds += zigbee.readAttribute(0x0000, [0x0001, 0x0003, 0x0004, 0x0005, 0x000A, 0x4000]) // ApplicationVersion, HWVersion, ManufacturerName, ModelIdentifier, ProductCode, SWBuildID

    Utils.sendZigbeeCommands cmds
}

// Implementation for capability.HealthCheck
def ping() {
    Log.info "ping ..."
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
    Log.info "Will me marked as offline if no message is received for ${theresholdAgo} (hardcoded)"

    String offlineMarkAgo = TimeCategory.minus(thereshold, now).toString().replace(".000 seconds", " seconds")
    Log.info "Will me marked as offline if no message is received until ${thereshold.format("yyyy-MM-dd HH:mm:ss", location.timeZone)} (${offlineMarkAgo} from now)"
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
        Log.info "Refreshing current state ..."
        if (device.currentValue("powerSource", true) == "battery") {
            Log.warn '[IMPORTANT] Click the "Refresh" button immediately after pushing any button on the device in order to first wake it up!'
        }
    }
    List<String> cmds = []
    cmds += zigbee.readAttribute(0x0001, 0x0021) // BatteryPercentage
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

    // Auto-Configure device: User switched drivers but did not click the "Configure" button
    if (logLevel == null || state.lastRx == null) {
        configure(true)
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

    switch (msg) {

        // ---------------------------------------------------------------------------------------------------------------
        // Handle IKEA Styrbar Remote Control N2 (E2002) specific Zigbee messages
        // ---------------------------------------------------------------------------------------------------------------

        // Plus/Minus button was pushed
        case { contains it, [clusterInt:0x0006, commandInt:0x00] }:
        case { contains it, [clusterInt:0x0006, commandInt:0x01] }:
            def button = msg.commandInt == 0x00 ? BUTTONS.MINUS : BUTTONS.PLUS
            return Utils.sendEvent(name:"pushed", value:button[0], type:"physical", isStateChange:true, descriptionText:"Button ${button[0]} (${button[1]}) was pushed")
        
        // Plus/Minus button was held
        case { contains it, [clusterInt:0x0008, commandInt:0x01] }:
        case { contains it, [clusterInt:0x0008, commandInt:0x05] }:
            def button = msg.commandInt == 0x01 ? BUTTONS.MINUS : BUTTONS.PLUS
            return Utils.sendEvent(name:"held", value:button[0], type:"physical", isStateChange:true, descriptionText:"Button ${button[0]} (${button[1]}) was held")
        
        // Plus/Minus button was released
        case { contains it, [clusterInt:0x0008, commandInt:0x07] }:
        case { contains it, [clusterInt:0x0008, commandInt:0x03] }:
            def button = device.currentValue("held", true) == 2 || msg.commandInt == 0x03 ? BUTTONS.MINUS : BUTTONS.PLUS
            return Utils.sendEvent(name:"released", value:button[0], type:"physical", isStateChange:true, descriptionText:"Button ${button[0]} (${button[1]}) was released")
        
        // Next/Prev button was pushed
        case { contains it, [clusterInt:0x0005, commandInt:0x07] }:
            def button = msg.data[0] == "00" ? BUTTONS.NEXT : BUTTONS.PREV
            return Utils.sendEvent(name:"pushed", value:button[0], type:"physical", isStateChange:true, descriptionText:"Button ${button[0]} (${button[1]}) was pushed")
        
        /*
        Holding the PREV and NEXT buttons works in a weird way:
        
        When PREV button is held, the following Zigbee messages are received:
        #1. [12:55:35.429] description=[catchall: 0104 0005 01 01 0040 00 926B 01 01 117C 09 00 0000]
        #2. [12:55:35.908] description=[catchall: 0104 0006 01 01 0040 00 926B 01 00 0000 01 00 ]             <-- button 1 (🔆) was pushed [physical]
        #3. [12:55:36.422] description=[catchall: 0104 0005 01 01 0040 00 926B 01 00 0000 05 00 0000000000]
        #4. [12:55:37.411] description=[catchall: 0104 0005 01 01 0040 00 926B 01 01 117C 08 00 010D00]
        
        When PREV button is released, the following Zigbee message is received:
        #5. [on release]   description=[catchall: 0104 0005 01 01 0040 00 926B 01 01 117C 09 00 XXXX]
        
        When NEXT button is held, the following Zigbee messages are received:
        #1. [12:56:59.463] description=[catchall: 0104 0005 01 01 0040 00 926B 01 01 117C 09 00 0000]
        #2. [12:56:59.962] description=[catchall: 0104 0006 01 01 0040 00 926B 01 00 0000 01 00 ]             <-- button 1 (🔆) was pushed [physical]
        #3. [12:57:00.480] description=[catchall: 0104 0005 01 01 0040 00 926B 01 00 0000 05 00 0000000000]
        #4. [12:57:01.466] description=[catchall: 0104 0005 01 01 0040 00 926B 01 01 117C 08 00 000D00]
        
        When NEXT button is released, the following Zigbee message is received:
        #5. [on release]   description=[catchall: 0104 0005 01 01 0040 00 926B 01 01 117C 09 00 XXXX]
        
        There is at least 2 seconds delay between the moment the device figured out that the button is held (not a click)
        and the moment message #4 is received (the moment we can figure out what button was held (010D00 vs 000D00)).
        
        IMHO, this weird behavior makes the use of the hold actions on the PREV and NEXT button unusable.
        */

        // ---------------------------------------------------------------------------------------------------------------
        // Handle capabilities Zigbee messages
        // ---------------------------------------------------------------------------------------------------------------
        
        // Events for capability.Battery
        
        // Report Attributes, Read Attributes Reponse: BatteryPercentage
        case { contains it, [clusterInt:0x0001, commandInt:0x0A, attrInt:0x0021] }:
        case { contains it, [clusterInt:0x0001, commandInt:0x01, attrInt:0x0021] }:
            Integer percentage = Integer.parseInt(msg.value, 16)
        
            // (0xFF) 255 is an invalid value for the battery percentage attribute, so we just ignore it
            if (percentage == 255) {
                return Log.warn("Ignored invalid reported battery percentage value: 0xFF (255)")
            }
        
            percentage =  percentage / 2
            Utils.sendEvent name:"battery", value:percentage, unit:"%", type:"physical", descriptionText:"Battery is ${percentage}% full"
            return Utils.processedZclMessage("Report/Read Attributes Response", "BatteryPercentage=${percentage}%")
        
        // Other events that we expect but are not usefull for capability.Battery behavior
        case { contains it, [clusterInt:0x0001, commandInt:0x07] }:  // ConfigureReportingResponse
            return
        
        // Events for capability.HealthCheck
        case { contains it, [clusterInt:0x0000, attrInt:0x0000] }:
            return Log.info("... pong")
        
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

        // ---------------------------------------------------------------------------------------------------------------
        // Handle common messages (e.g.: received during pairing when we query the device for information)
        // ---------------------------------------------------------------------------------------------------------------

        // Device_annce: Welcome back! let's sync state.
        case { contains it, [endpointInt:0x00, clusterInt:0x0013, commandInt:0x00] }:
            Log.info "Rejoined the Zigbee mesh! Refreshing current state ..."
            return runIn(3, "tryToRefresh")

        // Read Attributes Response (Basic cluster)
        case { contains it, [clusterInt:0x0000, commandInt:0x01] }:
            Utils.processedZclMessage("Read Attributes Response", "cluster=0x${msg.cluster}, attribute=0x${msg.attrId}, value=${msg.value}")
            Utils.zigbeeDataValue(msg.attrInt, msg.value)
            msg.additionalAttrs?.each { Utils.zigbeeDataValue(it.attrInt, it.value) }
            return

        // Mgmt_Leave_rsp
        case { contains it, [endpointInt:0x00, clusterInt:0x8034, commandInt:0x00] }:
            return Log.info("Device is leaving the Zigbee mesh. See you later, Aligator!")

        // Ignore the following Zigbee messages
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
            Log.warn "Sent unexpected Zigbee message: description=${description}, msg=${msg}"
    }
}

// ===================================================================================================================
// Logging helpers (something like this should be part of the SDK and not implemented by each driver)
// ===================================================================================================================

@Field def Map Log = [
    debug: { message -> if (logLevel == "1") log.debug "${device.displayName} ${message.uncapitalize()}" },
    info:  { message -> if (logLevel <= "2") log.info  "${device.displayName} ${message.uncapitalize()}" },
    warn:  { message -> if (logLevel <= "3") log.warn  "${device.displayName} ${message.uncapitalize()}" },
    error: { message -> log.error "${device.displayName} ${message.uncapitalize()}" }
]

// ===================================================================================================================
// Helper methods (keep them simple, keep them dumb)
// ===================================================================================================================

@Field def Utils = [
    sendZigbeeCommands: { List<String> cmds ->
        List<String> send = delayBetween(cmds.findAll { !it.startsWith("delay") }, 2000)
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
