/**
 * IKEA Tradfri LED Driver (ICPSHC24)
 *
 * @see https://dan-danache.github.io/hubitat/ikea-zigbee-drivers/
 * @see https://zigbee.blakadder.com/Ikea_ICPSHC24-30EU-IL-1.html
 * @see https://ww8.ikea.com/ikeahomesmart/releasenotes/releasenotes.html
 * @see https://static.homesmart.ikea.com/releaseNotes/
 */
import groovy.time.TimeCategory
import groovy.transform.Field

@Field static final String DRIVER_NAME = "IKEA Tradfri LED Driver (ICPSHC24)"
@Field static final String DRIVER_VERSION = "3.8.0"

// Fields for capability.HealthCheck
@Field static final Map<String, String> HEALTH_CHECK = [
    "schedule": "0 0 0/1 ? * * *", // Health will be checked using this cron schedule
    "thereshold": "3600" // When checking, mark the device as offline if no Zigbee message was received in the last 3600 seconds
]

metadata {
    definition(name:DRIVER_NAME, namespace:"dandanache", author:"Dan Danache", importUrl:"https://raw.githubusercontent.com/dan-danache/hubitat/master/ikea-zigbee-drivers/ICPSHC24.groovy") {
        capability "Configuration"
        capability "Actuator"
        capability "Switch"
        capability "ChangeLevel"
        capability "SwitchLevel"
        capability "HealthCheck"
        capability "PowerSource"
        capability "Refresh"
        capability "HealthCheck"

        // For firmware: 10EU-IL-1/1.2.245 (117C-4101-12245572)
        fingerprint profileId:"0104", endpointId:"01", inClusters:"0000,0003,0004,0005,0006,0008,0B05,1000", outClusters:"0005,0019,0020,1000", model:"TRADFRI Driver 10W", manufacturer:"IKEA of Sweden"

        // For firmware: 10EU-IL-1/2.3.086 (117C-4101-23086631)
        fingerprint profileId:"0104", endpointId:"01", inClusters:"0000,0003,0004,0005,0006,0008,1000,FC7C", outClusters:"0005,0019,0020,1000", model:"TRADFRI Driver 10W", manufacturer:"IKEA of Sweden"

        // For firmware: 30EU-IL-2/1.0.002 (117C-4109-00010002)
        fingerprint profileId:"0104", endpointId:"01", inClusters:"0000,0003,0004,0005,0006,0008,1000,FC57", outClusters:"0019", model:"TRADFRI Driver 30W", manufacturer:"IKEA of Sweden"
        
        // Attributes for capability.HealthCheck
        attribute "healthStatus", "enum", ["offline", "online", "unknown"]
        
        // Attributes for capability.ZigbeeRouter
        attribute "neighbors", "string"
        attribute "routes", "string"
    }
    
    // Commands for capability.Switch
    command "toggle"
    command "onWithTimedOff", [[name:"On time*", type:"NUMBER", description:"After how many seconds power will be turned Off [1..6500]"]]
    
    // Commands for capability.Brightness
    command "levelUp"
    command "levelDown"
    
    // Commands for capability.ZigbeeRouter
    command "requestRoutingData"
    command "startZigbeePairing", [[name:"Router device*", type:"STRING", description:"Enter the Device Network Id (0000 for Hubitat Hub)"]]
    
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
        
        // Inputs for capability.Switch
        input(
            name: "powerOnBehavior",
            type: "enum",
            title: "Power On behaviour",
            description: "<small>Select what happens after a power outage.</small>",
            options: [
                "TURN_POWER_ON": "Turn power On",
                "TURN_POWER_OFF": "Turn power Off",
                "RESTORE_PREVIOUS_STATE": "Restore previous state"
            ],
            defaultValue: "RESTORE_PREVIOUS_STATE",
            required: true
        )
        
        // Inputs for capability.Brightness
        input(
            name: "levelStep",
            type: "enum",
            title: "Brightness up/down step",
            description: "<small>Level adjust when using the levelUp/levelDown commands.</small>",
            options: [
                 "1": "1%",
                 "2": "2%",
                 "5": "5%",
                "10": "10%",
                "20": "20%",
                "25": "25%",
                "33": "33%"
            ],
            defaultValue: "20",
            required: true
        )
        input(
            name: "startLevelChangeRate",
            type: "enum",
            title: "Brightness change rate",
            description: "<small>The rate of brightness change when using the startLevelChange() command.</small>",
            options: [
                 "10": "10% / second : from 0% to 100% in 10 seconds",
                 "20": "20% / second : from 0% to 100% in 5 seconds",
                 "33": "33% / second : from 0% to 100% in 3 seconds",
                 "50": "50% / seconds : from 0% to 100% in 2 seconds",
                "100": "100% / second : from 0% to 100% in 1 seconds",
            ],
            defaultValue: "20",
            required: true
        )
        input(
            name: "turnOnBehavior",
            type: "enum",
            title: "Turn On behavior",
            description: "<small>Select what happens when the device is turned On.</small>",
            options: [
                "RESTORE_PREVIOUS_LEVEL": "Restore previous brightness",
                "FIXED_VALUE": "Always start with the same fixed brightness"
            ],
            defaultValue: "RESTORE_PREVIOUS_LEVEL",
            required: true
        )
        if (turnOnBehavior == "FIXED_VALUE") {
            input(
                name: "onLevelValue",
                type: "number",
                title: "Fixed brightness value",
                description: "<small>Range 1~100</small>",
                defaultValue: 50,
                range: "1..100",
                required: true
            )
        }
        input(
            name: "transitionTime",
            type: "enum",
            title: "On/Off transition time",
            description: "<small>Time taken to move to/from the target brightness when device is turned On/Off.</small>",
            options: [
                 "0": "Instant",
                 "5": "0.5 seconds",
                "10": "1 second",
                "15": "1.5 seconds",
                "20": "2 seconds",
                "30": "3 seconds",
                "40": "4 seconds",
                "50": "5 seconds",
               "100": "10 seconds"
            ],
            defaultValue: "5",
            required: true
        )
        input(
            name: "prestaging",
            type: "bool",
            title: "Pre-staging",
            description: "<small>Set the brightness level without turning On the device (for later use).</small>",
            defaultValue: false,
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
    
    // Preferences for capability.Switch
    if (powerOnBehavior == null) {
        powerOnBehavior = "RESTORE_PREVIOUS_STATE"
        device.updateSetting("powerOnBehavior", [value:powerOnBehavior, type:"enum"])
    }
    Log.info "🛠️ powerOnBehavior = ${powerOnBehavior}"
    cmds += zigbee.writeAttribute(0x0006, 0x4003, 0x30, powerOnBehavior == "TURN_POWER_OFF" ? 0x00 : (powerOnBehavior == "TURN_POWER_ON" ? 0x01 : 0xFF))
    
    // Preferences for capability.Brightness
    if (levelStep == null) {
        levelStep = "20"
        device.updateSetting("levelStep", [value:levelStep, type:"enum"])
    }
    Log.info "🛠️ levelStep = ${levelStep}%"
    
    if (startLevelChangeRate == null) {
        startLevelChangeRate = "20"
        device.updateSetting("startLevelChangeRate", [value:startLevelChangeRate, type:"enum"])
    }
    Log.info "🛠️ startLevelChangeRate = ${startLevelChangeRate}% / second"
    
    if (turnOnBehavior == null) {
        turnOnBehavior = "RESTORE_PREVIOUS_LEVEL"
        device.updateSetting("turnOnBehavior", [value:turnOnBehavior, type:"enum"])
    }
    Log.info "🛠️ turnOnBehavior = ${turnOnBehavior}"
    if (turnOnBehavior == "FIXED_VALUE") {
        Integer lvl = onLevelValue == null ? 50 : onLevelValue.intValue()
        device.updateSetting("onLevelValue",[ value:lvl, type:"number" ])
        Log.info "🛠️ onLevelValue = ${lvl}%"
        setOnLevel(lvl)
    } else {
        Log.debug "Disabling OnLevel (0xFF)"
        cmds += zigbee.writeAttribute(0x0008, 0x0011, 0x20, 0xFF)
    }
    
    if (transitionTime == null) {
        transitionTime = "5"
        device.updateSetting("transitionTime", [value:transitionTime, type:"enum"])
    }
    Log.info "🛠️ transitionTime = ${Integer.parseInt(transitionTime) / 10} second(s)"
    cmds += zigbee.writeAttribute(0x0008, 0x0010, 0x21, Integer.parseInt(transitionTime))
    
    if (prestaging == null) {
        prestaging = false
        device.updateSetting("prestaging", [value:prestaging, type:"bool"])
    }
    Log.info "🛠️ prestaging = ${prestaging}"
    
    // Preferences for capability.HealthCheck
    schedule HEALTH_CHECK.schedule, "healthCheck"

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

    // Configure IKEA Tradfri LED Driver (ICPSHC24) specific Zigbee reporting
    // -- No reporting needed

    // Add IKEA Tradfri LED Driver (ICPSHC24) specific Zigbee binds
    // -- No binds needed
    
    // Configuration for capability.Switch
    sendEvent name:"switch", value:"on", type:"digital", descriptionText:"Switch initialized to on"
    cmds += "zdo bind 0x${device.deviceNetworkId} 0x01 0x01 0x0006 {${device.zigbeeId}} {}" // On/Off cluster
    cmds += "he cr 0x${device.deviceNetworkId} 0x01 0x0006 0x0000 0x10 0x0000 0x0258 {01} {}" // Report OnOff (bool) at least every 10 minutes
    cmds += zigbee.readAttribute(0x0006, 0x0000) // OnOff
    
    // Configuration for capability.Brightness
    sendEvent name:"level", value:"100", type:"digital", descriptionText:"Brightness initialized to 100%"
    cmds += "zdo bind 0x${device.deviceNetworkId} 0x01 0x01 0x0008 {${device.zigbeeId}} {}" // Level Control cluster
    cmds += "he cr 0x${device.deviceNetworkId} 0x01 0x0008 0x0000 0x20 0x0000 0x0258 {01} {}" // Report CurrentLevel (uint8) at least every 10 minutes (Δ = 1)
    cmds += zigbee.readAttribute(0x0008, 0x0000) // CurrentLevel
    
    // Configuration for capability.HealthCheck
    sendEvent name:"healthStatus", value:"online", descriptionText:"Health status initialized to online"
    sendEvent name:"checkInterval", value:3600, unit:"second", descriptionText:"Health check interval is 3600 seconds"
    
    // Configuration for capability.PowerSource
    sendEvent name:"powerSource", value:"unknown", type:"digital", descriptionText:"Power source initialized to unknown"
    cmds += zigbee.readAttribute(0x0000, 0x0007) // PowerSource

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

// Implementation for capability.Switch
def on() {
    Log.debug "Sending On command"
    Utils.sendZigbeeCommands(["he raw 0x${device.deviceNetworkId} 0x01 0x01 0x0006 {114301}"])
}
def off() {
    Log.debug "Sending Off command"
    Utils.sendZigbeeCommands(["he raw 0x${device.deviceNetworkId} 0x01 0x01 0x0006 {114300}"])
}

def toggle() {
    Log.debug "Sending Toggle command"
    Utils.sendZigbeeCommands(["he raw 0x${device.deviceNetworkId} 0x01 0x01 0x0006 {114302}"])
}

def onWithTimedOff(onTime = 1) {
    Integer delay = onTime < 1 ? 1 : (onTime > 6500 ? 6500 : onTime)
    Log.debug "Sending OnWithTimedOff command"

    String payload = "00 ${zigbee.swapOctets(zigbee.convertToHexString(delay * 10, 4))} 0000"
    Utils.sendZigbeeCommands(["he raw 0x${device.deviceNetworkId} 0x01 0x01 0x0006 {114342 ${payload}}"])
}

// Implementation for capability.Brightness
def startLevelChange(direction) {
    Log.debug "Starting brightness change ${direction}wards with a rate of ${startLevelChangeRate}% / second"

    Integer mode = direction == "up" ? 0x00 : 0x01
    Integer rate = Integer.parseInt(startLevelChangeRate) * 2.54

    String payload = "${zigbee.convertToHexString(mode, 2)} ${zigbee.convertToHexString(rate, 2)}"
    Utils.sendZigbeeCommands(["he raw 0x${device.deviceNetworkId} 0x01 0x01 0x0008 {114301 ${payload}}"])
}
def stopLevelChange() {
    Log.debug "Stopping brightness change"
    Utils.sendZigbeeCommands(["he raw 0x${device.deviceNetworkId} 0x01 0x01 0x0008 {114303}"])
}
def levelUp() {
    Log.debug "Moving brightness up by ${levelStep}%"

    Integer stepSize = Integer.parseInt(levelStep) * 2.54
    Integer dur = 0

    String payload = "${zigbee.convertToHexString(0x00, 2)} ${zigbee.convertToHexString(stepSize, 2)} ${zigbee.swapOctets(zigbee.convertToHexString(dur, 4))}"
    Utils.sendZigbeeCommands(["he raw 0x${device.deviceNetworkId} 0x01 0x01 0x0008 {114302 ${payload}}"])
}
def levelDown() {
    Log.debug "Moving brightness down by ${levelStep}%"

    Integer stepSize = Integer.parseInt(levelStep) * 2.54
    Integer dur = 0

    String payload = "${zigbee.convertToHexString(0x01, 2)} ${zigbee.convertToHexString(stepSize, 2)} ${zigbee.swapOctets(zigbee.convertToHexString(dur, 4))}"
    Utils.sendZigbeeCommands(["he raw 0x${device.deviceNetworkId} 0x01 0x01 0x0008 {114302 ${payload}}"])
}
def setLevel(level, duration = 0) {
    Integer newLevel = level > 100 ? 100 : (level < 0 ? 0 : level)
    Log.debug "Setting brightness to ${newLevel}% during ${duration} seconds"

    // Device is On: use the Move To Level command
    if (device.currentValue("switch", true) == "on" || prestaging == false) {
        Integer lvl = newLevel * 2.54
        Integer dur = (duration > 1800 ? 1800 : (duration < 0 ? 0 : duration)) * 10         // Max transition time = 30 min

        String command = prestaging == false ? "04" : "00"
        String payload = "${zigbee.convertToHexString(lvl, 2)} ${zigbee.swapOctets(zigbee.convertToHexString(dur, 4))}"
        return Utils.sendZigbeeCommands(["he raw 0x${device.deviceNetworkId} 0x01 0x01 0x0008 {1143${command} ${payload}}"])
    }

    // Device is Off and onLevel is set to a fixed value: ignore command
    if (turnOnBehavior == "FIXED_VALUE") {
        return Log.info("Ignoring Set Level command because the device is turned Off and \"Turn On behavior\" preference is set to \"Always start with the same fixed brightness\"")
    }

    // Device is Off: keep the device turned Off, use the OnLevel attribute
    Log.debug("Device is turned Off so we pre-stage brightness level for when the device will be turned On")
    setOnLevel(newLevel)
    Utils.sendEvent(name:"level", value:newLevel, descriptionText:"Brightness is ${newLevel}%", type:"digital", isStateChange:true)
}
def setOnLevel(level) {
    Integer newLevel = level > 100 ? 100 : (level < 0 ? 0 : level)
    Log.debug "Setting Turn On brightness to ${newLevel}%"
    Integer lvl = newLevel * 2.54
    Utils.sendZigbeeCommands zigbee.writeAttribute(0x0008, 0x0011, 0x20, lvl)
}
private turnOnCallback(switchState) {
    // Device was just turned on: Read the value of the OnLevel attribute to sync/update its value
    if (switchState == "on") Utils.sendZigbeeCommands zigbee.readAttribute(0x0008, 0x0011)
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

// Implementation for capability.Refresh
def refresh(buttonPress = true) {
    if (buttonPress) {
        Log.warn "Refreshing device current state ..."
        if (device.currentValue("powerSource", true) == "battery") {
            Log.warn '[IMPORTANT] Click the "Refresh" button immediately after pushing any button on the device in order to first wake it up!'
        }
    }
    List<String> cmds = []
    cmds += zigbee.readAttribute(0x0006, 0x0000) // OnOff
    cmds += zigbee.readAttribute(0x0006, 0x4003) // PowerOnBehavior
    cmds += zigbee.readAttribute(0x0008, 0x0000) // CurrentLevel
    Utils.sendZigbeeCommands cmds
}

// Implementation for capability.ZigbeeRouter
def requestRoutingData() {
    Log.info "Asking the device to send its Neighbors Table and the Routing Table data ..."
    Utils.sendZigbeeCommands([
        "he raw 0x${device.deviceNetworkId} 0x00 0x00 0x0031 {40} {0x0000}",
        "he raw 0x${device.deviceNetworkId} 0x00 0x00 0x0032 {41} {0x0000}"
    ])
}
def startZigbeePairing(deviceNetworkId) {
    Log.info "Stopping Zigbee pairing on all devices. Please wait 5 seconds ..."
    Utils.sendZigbeeCommands(["he raw 0xFFFC 0x00 0x00 0x0036 {42 0001} {0x0000}"])
    runIn(5, "singleDeviceZigbeePairing", [data:deviceNetworkId])
}
private singleDeviceZigbeePairing(data) {
    Log.warn "Starting Zigbee pairing on device ${data}. Now is the moment to put the device in pairing mode!"
    Utils.sendZigbeeCommands(["he raw 0x${data} 0x00 0x00 0x0036 {43 5A01} {0x0000}"])
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
        // Handle IKEA Tradfri LED Driver (ICPSHC24) specific Zigbee messages
        // ---------------------------------------------------------------------------------------------------------------

        // No specific events

        // ---------------------------------------------------------------------------------------------------------------
        // Handle capabilities Zigbee messages
        // ---------------------------------------------------------------------------------------------------------------
        
        // Events for capability.Switch
        
        // Report/Read Attributes: OnOff
        case { contains it, [clusterInt:0x0006, commandInt:0x0A, attrInt:0x0000] }:
        case { contains it, [clusterInt:0x0006, commandInt:0x01, attrInt:0x0000] }:
            String newState = msg.value == "00" ? "off" : "on"
            Utils.sendEvent name:"switch", value:newState, descriptionText:"Was turned ${newState}", type:type
        
            // Execute the configured callback: turnOnCallback
            if (device.currentValue("switch", true) != newState) {
                turnOnCallback(newState)
            }
            return Utils.processedZclMessage("${msg.commandInt == 0x0A ? "Report" : "Read"} Attributes Response", "OnOff=${newState}")
        
        // Read Attributes Response: powerOnBehavior
        case { contains it, [clusterInt:0x0006, commandInt:0x01, attrInt:0x4003] }:
            String newValue = ""
            switch (Integer.parseInt(msg.value, 16)) {
                case 0x00: newValue = "TURN_POWER_OFF"; break
                case 0x01: newValue = "TURN_POWER_ON"; break
                case 0xFF: newValue = "RESTORE_PREVIOUS_STATE"; break
                default: return Log.warn("Received attribute value: powerOnBehavior=${msg.value}")
            }
            powerOnBehavior = newValue
            device.updateSetting("powerOnBehavior",[ value:newValue, type:"enum" ])
        
            return Utils.processedZclMessage("Read Attributes Response", "PowerOnBehavior=${newValue}")
        
        // Other events that we expect but are not usefull for capability.Switch behavior
        case { contains it, [clusterInt:0x0006, commandInt:0x07] }:
            return Utils.processedZclMessage("Configure Reporting Response", "attribute=switch, data=${msg.data}")
        case { contains it, [clusterInt:0x0006, commandInt:0x04] }: // Write Attribute Response (0x04)
            return
        
        // Events for capability.Brightness
        
        // Report/Read Attributes Reponse: CurrentLevel
        case { contains it, [clusterInt:0x0008, commandInt:0x0A, attrInt:0x0000] }:
        case { contains it, [clusterInt:0x0008, commandInt:0x01, attrInt:0x0000] }:
            Integer newLevel = msg.value == "00" ? 0 : Math.ceil(Integer.parseInt(msg.value, 16) * 100 / 254)
            if (device.currentValue("level", true) != newLevel) {
                Utils.sendEvent name:"level", value:newLevel, descriptionText:"Brightness is ${newLevel}%", type:"digital"
            }
            return Utils.processedZclMessage("${msg.commandInt == 0x0A ? "Report" : "Read"} Attributes Response", "CurrentLevel=${msg.value}")
        
        // Read Attributes Reponse: OnLevel
        // This value is read immediately after the device is turned On
        // @see turnOnCallback()
        case { contains it, [clusterInt:0x0008, commandInt:0x01, attrInt:0x0011] }:
            Integer onLevel = msg.value == "00" ? 0 : Integer.parseInt(msg.value, 16) * 100 / 254
        
            // Clear OnLevel attribute value (if previously set)
            if (turnOnBehavior != "FIXED_VALUE" && msg.value != "FF") {
                setLevel(device.currentValue("level", true))
                Log.debug "Disabling OnLevel (0xFF)"
                return Utils.sendZigbeeCommands(zigbee.writeAttribute(0x0008, 0x0011, 0x20, 0xFF))
            }
        
            // Set current level to OnLevel
            if (turnOnBehavior == "FIXED_VALUE") {
                setLevel(onLevel)
            }
            return Utils.processedZclMessage("Read Attributes Response", "OnLevel=${msg.value}")
        
        // Other events that we expect but are not usefull for capability.Brightness behavior
        case { contains it, [clusterInt:0x0008, commandInt:0x07] }:
            return Utils.processedZclMessage("Configure Reporting Response", "attribute=level, data=${msg.data}")
        case { contains it, [clusterInt:0x0008, commandInt:0x04] }:  // Write Attribute Response (0x04)
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
        
        // Events for capability.ZigbeeRouter
        
        // Mgmt_Lqi_rsp := { 08:Status, 08:NeighborTableEntries, 08:StartIndex, 08:NeighborTableListCount, n*176:NeighborTableList }
        // NeighborTableList := { 64:ExtendedPanId, 64:IEEEAddress, 16:NetworkAddress, 02:DeviceType, 02:RxOnWhenIdle, 03:Relationship, 01:Reserved, 02:PermitJoining, 06:Reserved, 08:Depth, 08:LQI }
        // Example: [6E, 00, 08, 00, 03, 50, 53, 3A, 0D, 00, DF, 66, 15, E9, A6, C9, 17, 00, 6F, 0D, 00, 00, 00, 24, 02, 00, CF, 50, 53, 3A, 0D, 00, DF, 66, 15, 80, BF, CA, 6B, 6A, 38, C1, A4, 4A, 16, 05, 02, 0F, CD, 50, 53, 3A, 0D, 00, DF, 66, 15, D3, FA, E1, 25, 00, 4B, 12, 00, 64, 17, 25, 02, 0F, 36]
        case { contains it, [endpointInt:0x00, clusterInt:0x8031, commandInt:0x00] }:
            if (msg.data[1] != "00") return Log.warn("Failed to retrieve Neighbors Table: data=${msg.data}")
            Integer entriesCount = Integer.parseInt(msg.data[4], 16)
        
            // Use base64 encoding instead of hex encoding to make the message a bit shorter
            String base64 = msg.data.join().decodeHex().encodeBase64().toString() // Decode test: https://base64.guru/converter/decode/hex
            sendEvent name:"neighbors", value:"${entriesCount} entries", type:"digital", descriptionText:base64
            return Utils.processedZdoMessage("Neighbors Table Response", "entries=${entriesCount}, data=${msg.data}")
        
        // Mgmt_Rtg_rsp := { 08:Status, 08:RoutingTableEntries, 08:StartIndex, 08:RoutingTableListCount, n*40:RoutingTableList }
        // RoutingTableList := { 16:DestinationAddress, 03:RouteStatus, 01:MemoryConstrained, 01:ManyToOne, 01:RouteRecordRequired, 02:Reserved, 16:NextHopAddress }
        // Example: [6F, 00, 0A, 00, 0A, 00, 00, 10, 00, 00, AD, 56, 00, AD, 56, ED, EE, 00, 4A, 16, 00, 00, 03, 00, 00, 00, 00, 03, 00, 00, 00, 00, 03, 00, 00, 00, 00, 03, 00, 00, 00, 00, 03, 00, 00, 00, 00, 03, 00, 00, 00, 00, 03, 00, 00]
        case { contains it, [endpointInt:0x00, clusterInt:0x8032, commandInt:0x00] }:
            if (msg.data[1] != "00") return Log.warn("Failed to retrieve Routing Table: data=${msg.data}")
            Integer entriesCount = Integer.parseInt(msg.data[4], 16)
        
            // Use base64 encoding instead of hex encoding to make the message a bit shorter
            String base64 = msg.data.join().decodeHex().encodeBase64().toString()
            sendEvent name:"routes", value:"${entriesCount} entries", type:"digital", descriptionText:base64
            return Utils.processedZdoMessage("Routing Table Response", "entries=${entriesCount}, data=${msg.data}")
        
        // Mgmt_Permit_Joining_rsp := { 08:Status }
        case { contains it, [endpointInt:0x00, clusterInt:0x8036, commandInt:0x00] }:
            if (msg.data[1] != "00") return Log.warn("Failed to Start Zigbee pairing for 90 seconds")
            return Log.info("Started Zigbee Pairing: data=${msg.data}")
        

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
