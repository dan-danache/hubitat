/**
 * IKEA Rodret Dimmer (E2201)
 *
 * @see https://dan-danache.github.io/hubitat/ikea-zigbee-drivers/
 * @see https://zigbee.blakadder.com/Ikea_E2201.html
 * @see https://ww8.ikea.com/ikeahomesmart/releasenotes/releasenotes.html
 * @see https://static.homesmart.ikea.com/releaseNotes/
 */
import groovy.time.TimeCategory
import groovy.transform.Field

@Field static final String DRIVER_NAME = "IKEA Rodret Dimmer (E2201)"
@Field static final String DRIVER_VERSION = "4.0.0"

// Fields for capability.HealthCheck
@Field static final Map<String, String> HEALTH_CHECK = [
    "schedule": "0 0 0/1 ? * * *", // Health will be checked using this cron schedule
    "thereshold": "43200" // When checking, mark the device as offline if no Zigbee message was received in the last 43200 seconds
]

// Fields for capability.PushableButton
@Field static final Map<String, List<String>> BUTTONS = [
    "ON": ["1", "On"],
    "OFF": ["2", "Off"],
]

// Fields for capability.ZigbeeBindings
@Field static final Map<String, String> GROUPS = [
    "9900":"Alfa", "9901":"Bravo", "9902":"Charlie", "9903":"Delta", "9904":"Echo", "9905":"Foxtrot", "9906":"Golf", "9907":"Hotel", "9908":"India", "9909":"Juliett", "990A":"Kilo", "990B":"Lima", "990C":"Mike", "990D":"November", "990E":"Oscar", "990F":"Papa", "9910":"Quebec", "9911":"Romeo", "9912":"Sierra", "9913":"Tango", "9914":"Uniform", "9915":"Victor", "9916":"Whiskey", "9917":"Xray", "9918":"Yankee", "9919":"Zulu"
]

metadata {
    definition(name:DRIVER_NAME, namespace:"dandanache", author:"Dan Danache", importUrl:"https://raw.githubusercontent.com/dan-danache/hubitat/master/ikea-zigbee-drivers/Ikea_E2201.groovy") {
        capability "Configuration"
        capability "Battery"
        capability "HealthCheck"
        capability "HoldableButton"
        capability "PowerSource"
        capability "PushableButton"
        capability "Refresh"
        capability "ReleasableButton"

        // For firmware: 1.0.47 (117C-11CD-01000047)
        fingerprint profileId:"0104", endpointId:"01", inClusters:"0000,0001,0003,0020,1000,FC7C", outClusters:"0003,0004,0006,0008,0019,1000", model:"RODRET Dimmer", manufacturer:"IKEA of Sweden"
        
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
        
        // Inputs for capability.ZigbeeBindings
        input(
            name: "controlDevice",
            type: "enum",
            title: "Control Zigbee device",
            description: "<small>Select the target Zigbee device that will be <b title=\"Without involving the Hubitat hub\" style=\"cursor:help\">directly controlled</b>* by this device.</small>",
            options: [ "0000":"❌ Stop controlling all Zigbee devices", "----":"- - - -" ] + getSwitchDevices(),
            defaultValue: "----",
            required: false
        )
        input(
            name: "controlGroup",
            type: "enum",
            title: "Control Zigbee group",
            description: "<small>Select the target Zigbee group that will be <b title=\"Without involving the Hubitat hub\" style=\"cursor:help\">directly controlled</b>* by this device.</small>",
            options: [ "0000":"❌ Stop controlling all Zigbee groups", "----":"- - - -" ] + GROUPS,
            defaultValue: "----",
            required: false
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
    
    // Preferences for capability.HealthCheck
    schedule HEALTH_CHECK.schedule, "healthCheck"
    
    // Preferences for capability.ZigbeeBindings
    if (controlDevice != null && controlDevice != "----") {
        if (controlDevice == "0000") {
            Log.info "🛠️ Clearing all device bindings"
            state.stopControlling = "devices"
        } else {
            Log.info "🛠️ Adding binding to device #${controlDevice} for clusters [0x0006 0x0008]"
            
            cmds += "he raw 0x${device.deviceNetworkId} 0x00 0x00 0x0021 {49 ${Utils.payload "${device.zigbeeId}"} ${Utils.payload "${device.endpointId}"} ${Utils.payload "0x0006"} 03 ${Utils.payload "${controlDevice}"} 01} {0x0000}" // Add device binding for cluster 0x0006
            cmds += "he raw 0x${device.deviceNetworkId} 0x00 0x00 0x0021 {49 ${Utils.payload "${device.zigbeeId}"} ${Utils.payload "${device.endpointId}"} ${Utils.payload "0x0008"} 03 ${Utils.payload "${controlDevice}"} 01} {0x0000}" // Add device binding for cluster 0x0008
        }
    
        device.updateSetting("controlDevice", [value:"----", type:"enum"])
        cmds += "he raw 0x${device.deviceNetworkId} 0x00 0x00 0x0033 {57 00} {0x0000}"
    }
    
    if (controlGroup != null && controlGroup != "----") {
        if (controlGroup == "0000") {
            Log.info "🛠️ Clearing all group bindings"
            state.stopControlling = "groups"
        } else {
            Log.info "🛠️ Adding binding to group ${controlGroup} for clusters [0x0006 0x0008]"
            cmds += "he raw 0x${device.deviceNetworkId} 0x00 0x00 0x0021 {49 ${Utils.payload "${device.zigbeeId}"} ${Utils.payload "${device.endpointId}"} ${Utils.payload "0x0006"} 01 ${Utils.payload "${controlGroup}"}} {0x0000}" // Add group binding for cluster 0x0006
            cmds += "he raw 0x${device.deviceNetworkId} 0x00 0x00 0x0021 {49 ${Utils.payload "${device.zigbeeId}"} ${Utils.payload "${device.endpointId}"} ${Utils.payload "0x0008"} 01 ${Utils.payload "${controlGroup}"}} {0x0000}" // Add group binding for cluster 0x0008
        }
    
        device.updateSetting("controlGroup", [value:"----", type:"enum"])
        cmds += "he raw 0x${device.deviceNetworkId} 0x00 0x00 0x0033 {57 00} {0x0000}"
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
    List<String> cmds = []
    cmds += updated(true)

    // Clear data (keep firmwareMT information though)
    device.getData()?.collect { it.key }.each { if (it != "firmwareMT") device.removeDataValue it }

    // Clear state
    state.clear()
    state.lastTx = 0
    state.lastRx = 0
    state.lastCx = DRIVER_VERSION

    // Configure IKEA Rodret Dimmer (E2201) specific Zigbee reporting
    // -- No reporting needed

    // Add IKEA Rodret Dimmer (E2201) specific Zigbee binds
    cmds += "zdo bind 0x${device.deviceNetworkId} 0x01 0x01 0x0006 {${device.zigbeeId}} {}" // On/Off cluster
    cmds += "zdo bind 0x${device.deviceNetworkId} 0x01 0x01 0x0008 {${device.zigbeeId}} {}" // Level Control cluster

    // Remove IKEA Rodret Dimmer (E2201) specific Zigbee binds
    // -- No unbinds needed
    
    // Configuration for capability.Battery
    cmds += "zdo bind 0x${device.deviceNetworkId} 0x${device.endpointId} 0x01 0x0001 {${device.zigbeeId}} {}" // Power Configuration cluster
    cmds += "he cr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0001 0x0021 0x20 0x0000 0x4650 {02} {}" // Report BatteryPercentage (uint8) at least every 5 hours (Δ = 1%)
    
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

    Log.info "Configuration done; refreshing device current state in 7 seconds ..."
    runIn 7, "tryToRefresh"
}
private autoConfigure() {
    Log.warn "Detected that this device is not properly configured for this driver version (lastCx != ${DRIVER_VERSION})"
    configure true
}

// Implementation for capability.HealthCheck
def ping() {
    Log.warn "ping ..."
    Utils.sendZigbeeCommands(zigbee.readAttribute(0x0000, 0x0000))
    Log.debug "Ping command sent to the device; we'll wait 5 seconds for a reply ..."
    runIn 5, "pingExecute"
}

def pingExecute() {
    if (state.lastRx == 0) return Log.info("Did not sent any messages since it was last configured")

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
    cmds += zigbee.readAttribute(0x0001, 0x0021, [:]) // BatteryPercentage
    cmds += "he raw 0x${device.deviceNetworkId} 0x00 0x00 0x0033 {57 00} {0x0000}"  // Start querying the Bindings Table
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

// Implementation for capability.ZigbeeBindings
private Map<String, String> getSwitchDevices() {
    try {
        List<Integer> switchDeviceIds = httpGet([ uri:"http://127.0.0.1:8080/device/listJson?capability=capability.switch" ]) { it.data.collect { it.id } }
        httpGet([ uri:"http://127.0.0.1:8080/hub/zigbeeDetails/json" ]) { response ->
            response.data.devices
                .findAll { switchDeviceIds.contains(it.id) }
                .sort { it.name }
                .collectEntries { [(it.zigbeeId): it.name] }
        }
    } catch (Exception ex) {
        return ["ZZZZ": "Exception: ${ex}"]
    }
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
    
    // Parse for capability.HealthCheck
    if (device.currentValue("healthStatus", true) != "online") {
        Utils.sendEvent name:"healthStatus", value:"online", type:"digital", descriptionText:"Health status changed to online"
    }

    // If we sent a Zigbee command in the last 3 seconds, we assume that this Zigbee event is a consequence of this driver doing something
    // Therefore, we mark this event as "digital"
    String type = state.containsKey("lastTx") && (now() - state.lastTx < 3000) ? "digital" : "physical"

    switch (msg) {

        // ---------------------------------------------------------------------------------------------------------------
        // Handle IKEA Rodret Dimmer (E2201) specific Zigbee messages
        // ---------------------------------------------------------------------------------------------------------------

        // On/Off button was pushed
        case { contains it, [clusterInt:0x0006, commandInt:0x00] }:
        case { contains it, [clusterInt:0x0006, commandInt:0x01] }:
            def button = msg.commandInt == 0x00 ? BUTTONS.OFF : BUTTONS.ON
            return Utils.sendEvent(name:"pushed", value:button[0], type:"physical", isStateChange:true, descriptionText:"Button ${button[0]} (${button[1]}) was pushed")
        
        // On/Off button was held
        case { contains it, [clusterInt:0x0008, commandInt:0x01] }:
        case { contains it, [clusterInt:0x0008, commandInt:0x05] }:
            def button = msg.commandInt == 0x01 ? BUTTONS.OFF : BUTTONS.ON
            return Utils.sendEvent(name:"held", value:button[0], type:"physical", isStateChange:true, descriptionText:"Button ${button[0]} (${button[1]}) was held")
        
        // On/Off button was released
        case { contains it, [clusterInt:0x0008, commandInt:0x07] }:
            def button = device.currentValue("held", true) == 1 ? BUTTONS.ON : BUTTONS.OFF
            return Utils.sendEvent(name:"released", value:button[0], type:"physical", isStateChange:true, descriptionText:"Button ${button[0]} (${button[1]}) was released")

        // ---------------------------------------------------------------------------------------------------------------
        // Handle capabilities Zigbee messages
        // ---------------------------------------------------------------------------------------------------------------
        
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
        
        // Events for capability.ZigbeeBindings
        
        // Mgmt_Bind_rsp := { 08:Status, 08:BindingTableEntriesTotal, 08:StartIndex, 08:BindingTableEntriesIncluded, 112/168*n:BindingTableList }
        // BindingTableList: { 64:SrcAddr, 08:SrcEndpoint, 16:ClusterId, 08:DstAddrMode, 16/64:DstAddr, 0/08:DstEndpoint }
        // Example: [71, 00, 01, 00, 01,  C6, 9C, FE, FE, FF, F9, E3, B4,  01,  06, 00,  03,  E9, A6, C9, 17, 00, 6F, 0D, 00,  01]
        case { contains it, [endpointInt:0x00, clusterInt:0x8033] }:
            if (msg.data[1] != "00") return Utils.processedZdpMessage("Mgmt_Bind_rsp", "Status=FAILED, data=${msg.data}")
            Integer totalEntries = Integer.parseInt msg.data[2], 16
            Integer startIndex = Integer.parseInt msg.data[3], 16
            Integer includedEntries = Integer.parseInt msg.data[4], 16
            if (startIndex == 0) {
                state.remove "ctrlDev"
                state.remove "ctrlGrp"
            }
            if (includedEntries == 0) return Utils.processedZdpMessage("Mgmt_Bind_rsp", "totalEntries=${totalEntries}, startIndex=${startIndex}, includedEntries=${includedEntries}")
        
            Integer pos = 5
            Integer deleted = 0
            List<List<String>> bindings = []
            Map<String, String> allDevices = getSwitchDevices()
            Set<String> devices = []
            Set<String> groups = []
            List<String> cmds = []
            for (int idx = 0; idx < includedEntries; idx++) {
                String srcDeviceId = msg.data[(pos)..(pos + 7)].reverse().join()
                String srcEndpoint = msg.data[pos + 8]
                String cluster = msg.data[(pos + 9)..(pos + 10)].reverse().join()
                String dstAddrMode = msg.data[pos + 11]
                if (dstAddrMode != "01" && dstAddrMode != "03") continue
        
                // Found device binding
                if (dstAddrMode == "03") {
                    String dstDeviceId = msg.data[(pos + 12)..(pos + 19)].reverse().join()
                    String dstEndpoint = msg.data[pos + 20]
                    String dstDeviceName = allDevices.getOrDefault(dstDeviceId, "Unknown (${dstDeviceId})")
                    pos += 21
        
                    // Remove all binds that are not targeting the hub
                    if (state.stopControlling == "devices") {
                        if (dstDeviceId != "${location.hub.zigbeeEui}") {
                            Log.debug "Removing binding for device ${dstDeviceName} on cluster 0x${cluster}"
                            cmds += "he raw 0x${device.deviceNetworkId} 0x00 0x00 0x0022 {49 ${Utils.payload srcDeviceId} ${srcEndpoint} ${Utils.payload cluster} 03 ${Utils.payload dstDeviceId} ${dstEndpoint}} {0x0000}"
                            deleted++
                        }
                        continue
                    }
        
                    Log.debug "Found binding for device ${dstDeviceName} on cluster 0x${cluster}"
                    devices.add(dstDeviceName)
                    continue
                }
        
                // Found group binding
                String dstGroupId = msg.data[(pos + 12)..(pos + 13)].reverse().join()
                String dstGroupName = GROUPS.getOrDefault(dstGroupId, "Unknown (${dstGroupId})")
                pos += 14
        
                // Remove all group bindings
                if (state.stopControlling == "groups") {
                    Log.debug "Removing binding for group ${dstGroupName} on cluster 0x${cluster}"
                    cmds += "he raw 0x${device.deviceNetworkId} 0x00 0x00 0x0022 {49 ${Utils.payload srcDeviceId} ${srcEndpoint} ${Utils.payload cluster} 01 ${Utils.payload dstGroupId}} {0x0000}"
                    deleted++
                    continue
                }
        
                Log.debug "Found binding for group ${dstGroupName} on cluster 0x${cluster}"
                groups.add(dstGroupName)
            }
        
            Set<String> ctrlDev = (state.ctrlDev ?: []).toSet()
            ctrlDev.addAll (devices.findAll { !it.startsWith("Unknown") })
            if (ctrlDev.size() > 0) state.ctrlDev = ctrlDev.unique()
        
            Set<String> ctrlGrp = (state.ctrlGrp ?: []).toSet()
            ctrlGrp.addAll(groups.findAll { !it.startsWith("Unknown") })
            if (ctrlGrp.size() > 0) state.ctrlGrp = ctrlGrp.unique()
            // Get next batch
            if (startIndex + includedEntries < totalEntries) {
                cmds += "he raw 0x${device.deviceNetworkId} 0x00 0x00 0x0033 {57 ${Integer.toHexString(startIndex + includedEntries - deleted).padLeft(2, "0")}} {0x0000}"
            } else {
                Log.info "Current device bindings: ${state.ctrlDev ?: "None"}"
                Log.info "Current group bindings: ${state.ctrlGrp ?: "None"}"
                state.remove "stopControlling"
            }
            Utils.sendZigbeeCommands cmds
            return Utils.processedZdpMessage("Mgmt_Bind_rsp", "totalEntries=${totalEntries}, startIndex=${startIndex}, devices=${devices}, groups=${groups}")

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
        case { contains it, [endpointInt:0x00, clusterInt:0x8021, commandInt:0x00] }:  // ZDP: Mgmt_Bind_rsp
        case { contains it, [endpointInt:0x00, clusterInt:0x8022, commandInt:0x00] }:  // ZDP: Mgmt_Unbind_rsp
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
