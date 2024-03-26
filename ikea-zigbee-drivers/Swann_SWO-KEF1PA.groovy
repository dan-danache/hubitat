/**
 * Swann One Key Fob (SWO-KEF1PA)
 *
 * @see https://dan-danache.github.io/hubitat/ikea-zigbee-drivers/
 * @see https://zigbee.blakadder.com/Swann_SWO-KEF1PA.html
 */
import groovy.time.TimeCategory
import groovy.transform.Field

@Field static final String DRIVER_NAME = "Swann One Key Fob (SWO-KEF1PA)"
@Field static final String DRIVER_VERSION = "4.0.0"

// Fields for capability.IAS
import hubitat.zigbee.clusters.iaszone.ZoneStatus

// Fields for capability.PushableButton
@Field static final Map<String, List<String>> BUTTONS = [
    "PANIC": ["1", "Panic"],
    "HOME": ["2", "Home"],
    "AWAY": ["3", "Away"],
    "NIGHT": ["4", "Night"],
]

metadata {
    definition(name:DRIVER_NAME, namespace:"dandanache", author:"Dan Danache", importUrl:"https://raw.githubusercontent.com/dan-danache/hubitat/master/ikea-zigbee-drivers/Swann_SWO-KEF1PA.groovy") {
        capability "Configuration"
        capability "Battery"
        capability "PushableButton"
        capability "Refresh"

        // For firmware: TBD
        fingerprint profileId:"0104", endpointId:"01", inClusters:"0000,0003,0001,0500,0000", outClusters:"0003,0501", model:"SWO-KEF1PA", manufacturer:"SwannONe"
        
        // Attributes for capability.IAS
        attribute "ias", "enum", ["enrolled", "not enrolled"]
    }

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

    // Configure Swann One Key Fob (SWO-KEF1PA) specific Zigbee reporting
    // -- No reporting needed

    // Add Swann One Key Fob (SWO-KEF1PA) specific Zigbee binds
    cmds += "zdo bind 0x${device.deviceNetworkId} 0x01 0x01 0x0501 {${device.zigbeeId}} {}" // IAS Ancillary Control Equipment cluster

    // Remove Swann One Key Fob (SWO-KEF1PA) specific Zigbee binds
    // -- No unbinds needed
    
    // Configuration for capability.IAS
    String ep_0500 = "0x01"
    cmds += "he wattr 0x${device.deviceNetworkId} ${ep_0500} 0x0500 0x0010 0xF0 {${Utils.payload "${location.hub.zigbeeEui}"}}"
    cmds += "he raw 0x${device.deviceNetworkId} 0x01 ${ep_0500} 0x0500 {01 23 00 00 00}" // Zone Enroll Response (0x00): status=Success, zoneId=0x00
    cmds += "zdo bind 0x${device.deviceNetworkId} ${ep_0500} 0x01 0x0500 {${device.zigbeeId}} {}" // IAS Zone cluster
    cmds += "he cr 0x${device.deviceNetworkId} ${ep_0500} 0x0500 0x0002 0x19 0x0000 0x4650 {00} {}" // Report ZoneStatus (map16) at least every 5 hours (Δ = 0)
    
    // Configuration for capability.Battery
    cmds += "zdo bind 0x${device.deviceNetworkId} 0x${device.endpointId} 0x01 0x0001 {${device.zigbeeId}} {}" // Power Configuration cluster
    cmds += "he cr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0001 0x0021 0x20 0x0000 0x4650 {02} {}" // Report BatteryPercentage (uint8) at least every 5 hours (Δ = 1%)
    
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
    cmds += zigbee.readAttribute(0x0001, 0x0021, [:]) // BatteryPercentage
    cmds += zigbee.readAttribute(0x0500, 0x0000, [:]) // IAS ZoneState
    cmds += zigbee.readAttribute(0x0500, 0x0001, [:]) // IAS ZoneType
    cmds += zigbee.readAttribute(0x0500, 0x0002, [:]) // IAS ZoneStatus
    Utils.sendZigbeeCommands cmds
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
        // Handle Swann One Key Fob (SWO-KEF1PA) specific Zigbee messages
        // ---------------------------------------------------------------------------------------------------------------

        // Arm := { 16:Button, 08:ArmMode, ??:ArmDisarmCode, 08:ZoneId}
        // ArmMode := { 0x00:Disarm, 0x01:Arm Day/Home Zones Only, 0x02:Arm Night/Sleep Zones Only, 0x03:Arm All Zones }
        // [00, 00, 00, 00, 00, 00, 00, 00, 00, 00] -> Home button
        // [02, 00, 00, 00, 00, 00, 00, 00, 00, 00] -> Night button
        // [03, 00, 00, 00, 00, 00, 00, 00, 00, 00] -> Away button
        case { contains it, [clusterInt:0x0501, commandInt:0x00, isClusterSpecific:true] }:
           switch (msg.data[0]) {
                case "00":
                    def button = BUTTONS.HOME
                    return Utils.sendEvent(name:"pushed", value:button[0], type:"physical", isStateChange:true, descriptionText:"Button ${button[0]} (${button[1]}) was pushed")
        
                case "02":
                    def button = BUTTONS.NIGHT
                    return Utils.sendEvent(name:"pushed", value:button[0], type:"physical", isStateChange:true, descriptionText:"Button ${button[0]} (${button[1]}) was pushed")
        
                case "03":
                    def button = BUTTONS.AWAY
                    return Utils.sendEvent(name:"pushed", value:button[0], type:"physical", isStateChange:true, descriptionText:"Button ${button[0]} (${button[1]}) was pushed")
            }
        
            return Log.error("Sent unexpected Zigbee message: description=${description}, msg=${msg}")
        
        // Panic
        case { contains it, [clusterInt:0x0501, commandInt:0x04, isClusterSpecific:true] }:
            def button = BUTTONS.PANIC
            return Utils.sendEvent(name:"pushed", value:button[0], type:"physical", isStateChange:true, descriptionText:"Button ${button[0]} (${button[1]}) was pushed")
        
        // Read Attributes: ZoneStatus
        case { contains it, [clusterInt:0x0500, commandInt:0x01, attrInt:0x0002] }:
            return Utils.processedZclMessage("Read Attributes Response", "ZoneStatus=${msg.value}")

        // ---------------------------------------------------------------------------------------------------------------
        // Handle capabilities Zigbee messages
        // ---------------------------------------------------------------------------------------------------------------
        
        // Events for capability.IAS
        
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
            return Utils.processedZclMessage("Zone Status Change Notification", "alarm1=${alarm1} alarm2=${alarm2} tamper=${tamper} lowBattery=${lowBattery} supervisionReports=${supervisionReports} restoreReports=${restoreReports} trouble=${trouble} mainsFault=${mainsFault} testMode=${testMode} batteryDefect=${batteryDefect}")
        
        // Enroll Request
        case { contains it, [clusterInt:0x500, commandInt:0x01, isClusterSpecific:true] }:
            String ep_0500 = "0x01"
            Utils.sendZigbeeCommands([
                "he raw 0x${device.deviceNetworkId} 0x01 ${ep_0500} 0x0500 {01 23 00 00 00}",  // Zone Enroll Response (0x00): status=Success, zoneId=0x00
                "he raw 0x${device.deviceNetworkId} 0x01 ${ep_0500} 0x0500 {01 23 01}",        // Initiate Normal Operation Mode (0x01): no_payload
            ])
            return Utils.processedZclMessage("Enroll Request", "description=${description}")
        
        // Read Attributes: ZoneState
        case { contains it, [clusterInt:0x0500, commandInt:0x01, attrInt:0x0000] }:
            String status = msg.value == "01" ? "enrolled" : "not enrolled"
            Utils.sendEvent name:"ias", value:status, descriptionText:"Device IAS status is ${status}", type:"digital"
            return Utils.processedZclMessage("Read Attributes Response", "ZoneState=${msg.value == "01" ? "enrolled" : "not_enrolled"}")
        
        // Read Attributes: ZoneType
        case { contains it, [clusterInt:0x0500, commandInt:0x01, attrInt:0x0001] }:
            return Utils.processedZclMessage("Read Attributes Response", "ZoneType=${msg.value}")
        
        // Other events that we expect but are not usefull for capability.IAS behavior
        case { contains it, [clusterInt:0x0500, commandInt:0x04, isClusterSpecific:false] }:
            return Utils.processedZclMessage("Write Attribute Response", "attribute=IAS_CIE_Address, ZoneType=${msg.data}")
        
        // Events for capability.Battery
        
        // Report/Read Attributes Reponse: BatteryPercentage
        case { contains it, [clusterInt:0x0001, commandInt:0x0A, attrInt:0x0021] }:
        case { contains it, [clusterInt:0x0001, commandInt:0x01] }:
        
            // Hubitat fails to parse some Read Attributes Responses
            if (msg.value == null && msg.data != null && msg.data[0] == "21" && msg.data[1] == "00") {
                msg.value = msg.data[2]
            }
        
            // The value 0xff indicates an invalid or unknown reading
            if (msg.value == "FF") return Log.warn("Ignored invalid remaining battery percentage value: 0x${msg.value}")
        
            Integer percentage = Integer.parseInt(msg.value, 16)
            percentage =  percentage / 2
            Utils.sendEvent name:"battery", value:percentage, unit:"%", descriptionText:"Battery is ${percentage}% full", type:type
            return Utils.processedZclMessage("${msg.commandInt == 0x0A ? "Report" : "Read"} Attributes Response", "BatteryPercentage=${percentage}%")
        
        // Other events that we expect but are not usefull for capability.Battery behavior
        case { contains it, [clusterInt:0x0001, commandInt:0x07] }:
            return Utils.processedZclMessage("Configure Reporting Response", "attribute=battery, data=${msg.data}")

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
