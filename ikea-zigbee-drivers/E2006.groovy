/**
 * IKEA Starkvind Air Purifier (E2006)
 *
 * @see https://dan-danache.github.io/hubitat/ikea-zigbee-drivers/
 * @see https://zigbee.blakadder.com/Ikea_E1603.html
 * @see https://ww8.ikea.com/ikeahomesmart/releasenotes/releasenotes.html
 * @see https://static.homesmart.ikea.com/releaseNotes/
 */
import groovy.time.TimeCategory
import groovy.transform.Field

@Field static final String DRIVER_NAME = "IKEA Starkvind Air Purifier (E2006)"
@Field static final String DRIVER_VERSION = "3.6.1"

// Fields for capability.PushableButton
@Field static final List<String> SUPPORTED_FAN_SPEEDS = [
    "auto", "low", "medium-low", "medium", "medium-high", "high", "off"
]

// Fields for capability.HealthCheck
@Field static final Map<String, String> HEALTH_CHECK = [
    "schedule": "0 0 0/1 ? * * *", // Health will be checked using this cron schedule
    "thereshold": "3600" // When checking, mark the device as offline if no Zigbee message was received in the last 3600 seconds
]

metadata {
    definition(name:DRIVER_NAME, namespace:"dandanache", author:"Dan Danache", importUrl:"https://raw.githubusercontent.com/dan-danache/hubitat/master/ikea-zigbee-drivers/E2006.groovy") {
        capability "Configuration"
        capability "AirQuality"
        capability "FanControl"
        capability "FilterStatus"
        capability "Switch"
        capability "Sensor"
        capability "HealthCheck"
        capability "PowerSource"
        capability "Refresh"
        capability "HealthCheck"

        // For firmware: 1.0.033 (117C-110C-00010033)
        fingerprint profileId:"0104", endpointId:"01", inClusters:"0000,0003,0004,0005,0202,FC57,FC7D", outClusters:"0019,0400,042A", model:"STARKVIND Air purifier table", manufacturer:"IKEA of Sweden"

        // For firmware: 1.1.001 (117C-110C-00011001)
        fingerprint profileId:"0104", endpointId:"01", inClusters:"0000,0003,0004,0005,0202,FC57,FC7C,FC7D", outClusters:"0019,0400,042A", model:"STARKVIND Air purifier table", manufacturer:"IKEA of Sweden"
        
        // Attributes for capability.AirPurifier
        attribute "airQuality", "enum", ["good", "moderate", "unhealthy for sensitive groups", "unhealthy", "hazardous"]
        attribute "filterUsage", "number"
        attribute "pm25", "number"
        attribute "auto", "enum", ["on", "off"]
        
        // Attributes for capability.HealthCheck
        attribute "healthStatus", "enum", ["offline", "online", "unknown"]
        
        // Attributes for capability.ZigbeeRouter
        attribute "neighbors", "string"
        attribute "routes", "string"
    }
    
    // Commands for capability.AirPurifier
    command "setSpeed", [[name:"Fan speed*", type:"ENUM", description:"Fan speed to set", constraints:SUPPORTED_FAN_SPEEDS]]
    command "toggle"
    
    // Commands for capability.ZigbeeRouter
    command "requestRoutingData"
    
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
        
        // Inputs for capability.AirPurifier
        input(
            name: "pm25ReportDelta",
            type: "enum",
            title: "Sensor report frequency",
            description: "<small>Adjust how often the device sends its PM 2.5 sensor data.</small>",
            options: [
                "01" : "Very High - report changes of +/- 1μg/m3",
                "02" : "High - report changes of +/- 2μg/m3",
                "03" : "Medium - report changes of +/- 3μg/m3",
                "05" : "Low - report changes of +/- 5μg/m3",
                "10" : "Very Low - report changes of +/- 10μg/m3"
            ],
            defaultValue: "03",
            required: true
        )
        input(
            name: "filterLifeTime",
            type: "enum",
            title: "Filter life time",
            description: "<small>Configure time between filter changes (default 6 months).</small>",
            options: [
                 "90" : "3 months",
                "180" : "6 months",
                "270" : "9 months",
                "360" : "1 year"
            ],
            defaultValue: "180",
            required: true
        )
        input(
            name: "childLock",
            type: "bool",
            title: "Child lock",
            description: "<small>Lock physical controls on the device.</small>",
            defaultValue: false
        )
        input(
            name: "panelIndicator",
            type: "bool",
            title: "LED status",
            description: "<small>Keep the LED indicators on the device constantly lit.</small>",
            defaultValue: true
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
    
    // Preferences for capability.AirPurifier
    if (pm25ReportDelta == null) {
        pm25ReportDelta = "03"
        device.updateSetting("pm25ReportDelta", [value:pm25ReportDelta, type:"enum"])
    }
    Log.info "🛠️ pm25ReportDelta = +/- ${pm25ReportDelta}μg/m3"
    cmds += "he cr 0x${device.deviceNetworkId} 0x01 0xFC7D 0x0004 0x21 0x0000 0x0258 {${pm25ReportDelta}} {117C}"
    
    if (filterLifeTime == null) {
        filterLifeTime = "180"
        device.updateSetting("filterLifeTime", [value:filterLifeTime, type:"enum"])
    }
    Log.info "🛠️ filterLifeTime = ${filterLifeTime} days"
    cmds += zigbee.writeAttribute(0xFC7D, 0x0002, 0x23, Integer.parseInt(filterLifeTime) * 1440, [mfgCode:"0x117C"])
    cmds += zigbee.readAttribute(0xFC7D, 0x0000, [mfgCode:"0x117C"])  // Also trigger the update of the filterStatus value (%)
    
    if (childLock == null) {
        childLock = false
        device.updateSetting("childLock", [value:childLock, type:"bool"])
    }
    Log.info "🛠️ childLock = ${childLock}"
    cmds += zigbee.writeAttribute(0xFC7D, 0x0005, 0x10, childLock ? 0x01 : 0x00, [mfgCode:"0x117C"])
    
    if (panelIndicator == null) {
        panelIndicator = true
        device.updateSetting("panelIndicator", [value:panelIndicator, type:"bool"])
    }
    Log.info "🛠️ panelIndicator = ${panelIndicator}"
    cmds += zigbee.writeAttribute(0xFC7D, 0x0003, 0x10, panelIndicator ? 0x00 : 0x01, [mfgCode:"0x117C"])
    
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

    // Configure IKEA Starkvind Air Purifier (E2006) specific Zigbee reporting
    // -- No reporting needed

    // Add IKEA Starkvind Air Purifier (E2006) specific Zigbee binds
    // -- No binds needed
    
    // Configuration for capability.AirPurifier
    sendEvent name:"switch", value:"on", type:"digital", descriptionText:"Switch initialized to on"
    sendEvent name:"supportedFanSpeeds", value:SUPPORTED_FAN_SPEEDS, type:"digital", descriptionText:"Supported fan speeds initialized to ${SUPPORTED_FAN_SPEEDS}"
    
    cmds += "zdo bind 0x${device.deviceNetworkId} 0x01 0x01 0xFC7D {${device.zigbeeId}} {}"  // IKEA Air Purifier cluster
    cmds += "he cr 0x${device.deviceNetworkId} 0x01 0xFC7D 0x0000 0x23 0x0000 0x0258 {0A} {117C}"  // Report FilterRunTime (uint32) at least every 10 minutes
    cmds += "he cr 0x${device.deviceNetworkId} 0x01 0xFC7D 0x0001 0x20 0x0000 0x0000 {01} {117C}"  // Report ReplaceFilter (uint8)
    cmds += "he cr 0x${device.deviceNetworkId} 0x01 0xFC7D 0x0002 0x23 0x0000 0x0000 {01} {117C}"  // Report FilterLifeTime (uint32)
    //cmds += "he cr 0x${device.deviceNetworkId} 0x01 0xFC7D 0x0003 0x10 0x0000 0x0000 {01} {117C}"  // Report DisablePanelLights (bool)
    //cmds += "he cr 0x${device.deviceNetworkId} 0x01 0xFC7D 0x0004 0x21 0x0000 0x0258 {01} {117C}"  // Report PM25Measurement (uint16)
    cmds += "he cr 0x${device.deviceNetworkId} 0x01 0xFC7D 0x0005 0x10 0x0000 0x0000 {01} {117C}"  // Report ChildLock (bool)
    cmds += "he cr 0x${device.deviceNetworkId} 0x01 0xFC7D 0x0006 0x20 0x0000 0x0000 {01} {117C}"  // Report FanMode (uint8)
    cmds += "he cr 0x${device.deviceNetworkId} 0x01 0xFC7D 0x0007 0x20 0x0000 0x0000 {01} {117C}"  // Report FanSpeed (uint8)
    //cmds += "he cr 0x${device.deviceNetworkId} 0x01 0xFC7D 0x0008 0x23 0x0000 0x0258 {01} {117C}"  // Report DeviceRunTime (uint32)
    
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

// Implementation for capability.AirPurifier
def on() {
    if (device.currentValue("switch", true) == "on") return
    Log.debug "Sending On command"
    Utils.sendZigbeeCommands(zigbee.writeAttribute(0xFC7D, 0x0006, 0x20, 0x01, [mfgCode:"0x117C"]))
}
def off() {
    Log.debug "Sending Off command"
    Utils.sendZigbeeCommands(zigbee.writeAttribute(0xFC7D, 0x0006, 0x20, 0x00, [mfgCode:"0x117C"]))
    Utils.sendEvent name:"switch", value:"off", descriptionText:"Was turned off", type:"digital"
    Utils.sendEvent name:"auto", value:"disabled", descriptionText:"Auto mode is disabled", type:"digital"
    Utils.sendEvent name:"speed", value:"off", descriptionText:"Fan speed is off", type:"digital"
}
def toggle() {
    if (device.currentValue("switch", true) == "on") return off()
    on()
}
def setSpeed(speed) {
    Log.debug "Setting speed to: ${speed}"
    Integer newSpeed = 0x00
    switch (speed) {
        case "on":
        case "auto":
            newSpeed = 1
            break
        case "low":
            newSpeed = 10
            break
        case "medium-low":
            newSpeed = 20
            break
        case "medium":
            newSpeed = 30
            break
        case "medium-high":
            newSpeed = 40
            break
        case "high":
            newSpeed = 50
            break
        case "off":
            newSpeed = 0
            break
        default:
            return Log.warn("Unknown speed: ${speed}")
    }
    Utils.sendZigbeeCommands(zigbee.writeAttribute(0xFC7D, 0x0006, 0x20, newSpeed, [mfgCode:"0x117C"]))
}
def cycleSpeed() {
    String curSpeed = device.currentValue("speed", true)
    Log.debug "Current speed is: ${curSpeed}"
    Integer newSpeed = 0x00
    switch (curSpeed) {
        case "high":
        case "off":
            newSpeed = 10
            break
        case "low":
            newSpeed = 20
            break
        case "medium-low":
            newSpeed = 30
            break
        case "medium":
            newSpeed = 40
            break
        case "medium-high":
            newSpeed = 50
            break
        default:
            return Log.warn("Unknown current speed: ${curSpeed}")
    }

    Log.debug "Cycling speed to: ${newSpeed}"
    Utils.sendZigbeeCommands(zigbee.writeAttribute(0xFC7D, 0x0006, 0x20, newSpeed, [mfgCode:"0x117C"]))
}
private Integer lerp(ylo, yhi, xlo, xhi, cur) {
  return Math.round(((cur - xlo) / (xhi - xlo)) * (yhi - ylo) + ylo);
}

// See: https://en.wikipedia.org/wiki/Air_quality_index#United_States
private pm25Aqi(pm25) {
    if (pm25 <=  12.1) return [lerp(  0,  50,   0.0,  12.0, pm25), "good", "green"]
    if (pm25 <=  35.5) return [lerp( 51, 100,  12.1,  35.4, pm25), "moderate", "gold"]
    if (pm25 <=  55.5) return [lerp(101, 150,  35.5,  55.4, pm25), "unhealthy for sensitive groups", "darkorange"]
    if (pm25 <= 150.5) return [lerp(151, 200,  55.5, 150.4, pm25), "unhealthy", "red"]
    if (pm25 <= 250.5) return [lerp(201, 300, 150.5, 250.4, pm25), "very unhealthy", "purple"]
    if (pm25 <= 350.5) return [lerp(301, 400, 250.5, 350.4, pm25), "hazardous", "maroon"]
    if (pm25 <= 500.5) return [lerp(401, 500, 350.5, 500.4, pm25), "hazardous", "maroon"]
    return [500, "hazardous", "maroon"]
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
    cmds += zigbee.readAttribute(0xFC7D, 0x0000, [mfgCode:"0x117C"]) // FilterRunTime
    cmds += zigbee.readAttribute(0xFC7D, 0x0001, [mfgCode:"0x117C"]) // ReplaceFilter
    cmds += zigbee.readAttribute(0xFC7D, 0x0002, [mfgCode:"0x117C"]) // FilterLifeTime
    cmds += zigbee.readAttribute(0xFC7D, 0x0003, [mfgCode:"0x117C"]) // DisablePanelLights
    cmds += zigbee.readAttribute(0xFC7D, 0x0004, [mfgCode:"0x117C"]) // PM25Measurement
    cmds += zigbee.readAttribute(0xFC7D, 0x0005, [mfgCode:"0x117C"]) // ChildLock
    cmds += zigbee.readAttribute(0xFC7D, 0x0006, [mfgCode:"0x117C"]) // FanMode
    Utils.sendZigbeeCommands cmds
}

// Implementation for capability.ZigbeeRouter
def requestRoutingData() {
    Log.info "Asking the device to send its Neighbors Table and the Routing Table data ..."
    Utils.sendZigbeeCommands([
        "he raw 0x${device.deviceNetworkId} 0x00 0x00 0x0031 {00} {0x00}",
        "he raw 0x${device.deviceNetworkId} 0x00 0x00 0x0032 {00} {0x00}"
    ])
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
        // Handle IKEA Starkvind Air Purifier (E2006) specific Zigbee messages
        // ---------------------------------------------------------------------------------------------------------------

        // No specific events

        // ---------------------------------------------------------------------------------------------------------------
        // Handle capabilities Zigbee messages
        // ---------------------------------------------------------------------------------------------------------------
        
        // Events for capability.AirPurifier
        
        // Report/Read Attributes: PM25
        case { contains it, [clusterInt:0xFC7D, commandInt:0x0A, attrInt:0x0004] }:
        case { contains it, [clusterInt:0xFC7D, commandInt:0x01, attrInt:0x0004] }:
            Integer pm25 = Integer.parseInt(msg.value, 16)
        
            // Tried to read the PM 2.5 value when the device is Off
            if (pm25 == 0xFFFF) return
        
            Utils.sendEvent name:"pm25", value:pm25, unit:"μg/m3", descriptionText:"Fine particulate matter (PM2.5) concentration is ${pm25} μg/m3", type:type
            def aqi = pm25Aqi(pm25)
            Utils.sendEvent name:"airQualityIndex", value:aqi[0], descriptionText:"Calculated Air Quality Index = ${aqi[0]}", type:type
            Utils.sendEvent name:"airQuality", value:"<span style=\"color:${aqi[2]}\">${aqi[1]}</span>", descriptionText:"Calculated Air Quality = ${aqi[1]}", type:type
            return Utils.processedZclMessage("${msg.commandInt == 0x0A ? "Report" : "Read"} Attributes Response", "PM25Measurement=${pm25} μg/m3")
        
        // Report/Read Attributes: FilterRunTime
        case { contains it, [clusterInt:0xFC7D, commandInt:0x0A, attrInt:0x0000] }:
        case { contains it, [clusterInt:0xFC7D, commandInt:0x01, attrInt:0x0000] }:
            Integer runTime = Integer.parseInt(msg.value, 16)
            Integer filterUsage = Math.floor(runTime * 100 / (Integer.parseInt(filterLifeTime) * 1440));
            Utils.sendEvent name:"filterUsage", value:filterUsage, unit:"%", descriptionText:"Filter usage is ${filterUsage}%", type:type
            return Utils.processedZclMessage("${msg.commandInt == 0x0A ? "Report" : "Read"} Attributes Response", "FilterRunTime=${runTime} min, FilterLifeTime=${filterLifeTime} days")
        
        // Report/Read Attributes: FanMode
        case { contains it, [clusterInt:0xFC7D, commandInt:0x0A, attrInt:0x0006] }:
        case { contains it, [clusterInt:0xFC7D, commandInt:0x01, attrInt:0x0006] }:
            String auto = msg.value == "01" ? "enabled" : "disabled"
            Utils.sendEvent name:"auto", value:auto, descriptionText:"Auto mode is ${auto}", type:type
            Utils.sendZigbeeCommands(zigbee.readAttribute(0xFC7D, 0x0007, [mfgCode:"0x117C"]))
            return Utils.processedZclMessage("${msg.commandInt == 0x0A ? "Report" : "Read"} Attributes Response", "FanMode=${msg.value}")
        
        // Report/Read Attributes: FanSpeed
        case { contains it, [clusterInt:0xFC7D, commandInt:0x0A, attrInt:0x0007] }:
        case { contains it, [clusterInt:0xFC7D, commandInt:0x01, attrInt:0x0007] }:
        
            // Fan Speed should vary from 1 to 50
            Integer speed = Integer.parseInt(msg.value, 16)
            if (speed > 50) newSpeed = 50
        
            // Update switch status
            String newState = speed == 0 ? "off" : "on"
            Utils.sendEvent name:"switch", value:newState, descriptionText:"Was turned ${newState}", type:type
        
            String newSpeed = ""
            switch (speed) {
                case 0:
                    newSpeed = "off"
                    break
                case { speed <= 10 }:
                    newSpeed = "low"
                    break
                case { speed <= 20 }:
                    newSpeed = "medium-low"
                    break
                case { speed <= 30 }:
                    newSpeed = "medium"
                    break
                case { speed <= 40 }:
                    newSpeed = "medium-high"
                    break
                default:
                    newSpeed = "high"
            }
            Utils.sendEvent name:"speed", value:newSpeed, descriptionText:"Fan speed is ${newSpeed}", type:type
            return Utils.processedZclMessage("${msg.commandInt == 0x0A ? "Report" : "Read"} Attributes Response", "FanSpeed=${speed}")
        
        // Report/Read Attributes: ReplaceFilter
        case { contains it, [clusterInt:0xFC7D, commandInt:0x0A, attrInt:0x0001] }:
        case { contains it, [clusterInt:0xFC7D, commandInt:0x01, attrInt:0x0001] }:
            String filterStatus = msg.value == "00" ? "normal" : "replace"
            Utils.sendEvent name:"filterStatus", value:filterStatus, descriptionText:"Filter status is ${filterStatus}", type:type
            return Utils.processedZclMessage("${msg.commandInt == 0x0A ? "Report" : "Read"} Attributes Response", "ReplaceFilter=${msg.value}")
        
        // Report/Read Attributes: FilterLifeTime
        case { contains it, [clusterInt:0xFC7D, commandInt:0x0A, attrInt:0x0002] }:
        case { contains it, [clusterInt:0xFC7D, commandInt:0x01, attrInt:0x0002] }:
            Integer lifeTimeDays = Math.ceil(Integer.parseInt(msg.value, 16) / 1440)
            if (lifeTimeDays != 90 && lifeTimeDays != 180 && lifeTimeDays != 270 && lifeTimeDays != 360) {
                Log.warn "Invalid FilterLifeTime value: ${msg.value} (${lifeTimeDays} days). Setting it to default value of 180 days."
                lifeTimeDays = 180
                Utils.sendZigbeeCommands(zigbee.writeAttribute(0xFC7D, 0x0002, 0x23, lifeTimeDays * 1440, [mfgCode:"0x117C"]))
            }
            filterLifeTime = "${filterLifeTime}"
            device.updateSetting("filterLifeTime", [value:filterLifeTime, type:"enum"])
            return Utils.processedZclMessage("${msg.commandInt == 0x0A ? "Report" : "Read"} Attributes Response", "FilterLifeTime=${msg.value} (${lifeTimeDays} days)")
        
        // Read Attributes: DisablePanelLights
        case { contains it, [clusterInt:0xFC7D, commandInt:0x01, attrInt:0x0003] }:
            panelIndicator = msg.value == "00"
            device.updateSetting("panelIndicator", [value:panelIndicator, type:"bool"])
            return Utils.processedZclMessage("Read Attributes Response", "DisablePanelLights=${msg.value}")
        
        // Report/Read Attributes: ChildLock
        case { contains it, [clusterInt:0xFC7D, commandInt:0x0A, attrInt:0x0005] }:
        case { contains it, [clusterInt:0xFC7D, commandInt:0x01, attrInt:0x0005] }:
            childLock = msg.value == "01"
            device.updateSetting("childLock", [value:childLock, type:"bool"])
            return Utils.processedZclMessage("${msg.commandInt == 0x0A ? "Report" : "Read"} Attributes Response", "ChildLock=${msg.value}")
        
        // Other events that we expect but are not usefull for capability.AirPurifier behavior
        case { contains it, [clusterInt:0xFC7D, commandInt:0x04] }: // Write Attribute Response (0x04)
        case { contains it, [clusterInt:0xFC7D, commandInt:0x07] }: // Configure Reporting Response
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
