/**
 * IKEA Vindstyrka Air Quality Sensor (E2112)
 *
 * @see https://dan-danache.github.io/hubitat/ikea-zigbee-drivers/
 * @see https://zigbee.blakadder.com/Ikea_E2112.html
 * @see https://ww8.ikea.com/ikeahomesmart/releasenotes/releasenotes.html
 * @see https://static.homesmart.ikea.com/releaseNotes/
 */
import groovy.time.TimeCategory
import groovy.transform.Field

@Field static final String DRIVER_NAME = "IKEA Vindstyrka Air Quality Sensor (E2112)"
@Field static final String DRIVER_VERSION = "3.8.0"

// Fields for capability.HealthCheck
@Field static final Map<String, String> HEALTH_CHECK = [
    "schedule": "0 0 0/1 ? * * *", // Health will be checked using this cron schedule
    "thereshold": "3600" // When checking, mark the device as offline if no Zigbee message was received in the last 3600 seconds
]

metadata {
    definition(name:DRIVER_NAME, namespace:"dandanache", author:"Dan Danache", importUrl:"https://raw.githubusercontent.com/dan-danache/hubitat/master/ikea-zigbee-drivers/E2112.groovy") {
        capability "Configuration"
        capability "AirQuality"
        capability "TemperatureMeasurement"
        capability "RelativeHumidityMeasurement"
        capability "HealthCheck"
        capability "PowerSource"
        capability "Refresh"
        capability "HealthCheck"

        // For firmware: 1.0.010 (117C-110F-00010010)
        fingerprint profileId:"0104", endpointId:"01", inClusters:"0000,0003,0004,0402,0405,FC57,FC7C,042A,FC7E", outClusters:"0003,0019,0020,0202", model:"VINDSTYRKA", manufacturer:"IKEA of Sweden"
        
        // Attributes for capability.FineParticulateMatter
        attribute "airQuality", "enum", ["good", "moderate", "unhealthy for sensitive groups", "unhealthy", "hazardous"]
        attribute "pm25", "number"
        
        // Attributes for capability.VocIndex
        attribute "vocIndex", "number"
        
        // Attributes for capability.HealthCheck
        attribute "healthStatus", "enum", ["offline", "online", "unknown"]
        
        // Attributes for capability.ZigbeeRouter
        attribute "neighbors", "string"
        attribute "routes", "string"
    }
    
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

    // Configure IKEA Vindstyrka Air Quality Sensor (E2112) specific Zigbee reporting
    // -- No reporting needed

    // Add IKEA Vindstyrka Air Quality Sensor (E2112) specific Zigbee binds
    // -- No binds needed
    
    // Configuration for capability.FineParticulateMatter
    cmds += "zdo bind 0x${device.deviceNetworkId} 0x01 0x01 0x042A {${device.zigbeeId}} {}" // Particulate Matter 2.5 cluster
    cmds += "he cr 0x${device.deviceNetworkId} 0x01 0x042A 0x0000 0x39 0x0000 0x0258 {FFFF0000} {}" // Report MeasuredValue (single) at least every 10 minutes (Δ = ??)
    
    // Configuration for capability.VocIndex
    cmds += "zdo bind 0x${device.deviceNetworkId} 0x01 0x01 0xFC7E {${device.zigbeeId}} {}" // VocIndex Measurement cluster
    cmds += "he cr 0x${device.deviceNetworkId} 0x01 0xFC7E 0x0000 0x39 0x0000 0x0258 {FFFF0000} {117C}" // Report MeasuredValue (single) at least every 10 minutes (Δ = ??)
    
    // Configuration for capability.Temperature
    cmds += "zdo bind 0x${device.deviceNetworkId} 0x01 0x01 0x0402 {${device.zigbeeId}} {}" // Temperature Measurement cluster
    cmds += "he cr 0x${device.deviceNetworkId} 0x01 0x0402 0x0000 0x29 0x0000 0x0258 {6400} {}" // Report MeasuredValue (int16) at least every 10 minutes (Δ = 1°C)
    
    // Configuration for capability.RelativeHumidity
    cmds += "zdo bind 0x${device.deviceNetworkId} 0x01 0x01 0x0405 {${device.zigbeeId}} {}" // Relative Humidity Measurement cluster
    cmds += "he cr 0x${device.deviceNetworkId} 0x01 0x0405 0x0000 0x21 0x0000 0x0258 {6400} {}" // Report MeasuredValue (uint16) at least every 10 minutes (Δ = 1%)
    
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

// Implementation for capability.FineParticulateMatter
private Integer lerp(ylo, yhi, xlo, xhi, cur) {
  return Math.round(((cur - xlo) / (xhi - xlo)) * (yhi - ylo) + ylo);
}
private pm25Aqi(Integer pm25) { // See: https://en.wikipedia.org/wiki/Air_quality_index#United_States
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
    cmds += zigbee.readAttribute(0x0402, 0x0000) // Temperature
    cmds += zigbee.readAttribute(0x0405, 0x0000) // Relative Humidity
    cmds += zigbee.readAttribute(0x042A, 0x0000) // Fine Particulate Matter (PM25)
    cmds += zigbee.readAttribute(0xFC7E, 0x0000, [mfgCode:"0x117C"]) // VOC Index
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
        // Handle IKEA Vindstyrka Air Quality Sensor (E2112) specific Zigbee messages
        // ---------------------------------------------------------------------------------------------------------------

        // No specific events

        // ---------------------------------------------------------------------------------------------------------------
        // Handle capabilities Zigbee messages
        // ---------------------------------------------------------------------------------------------------------------
        
        // Events for capability.FineParticulateMatter
        
        // Report/Read Attributes Reponse: MeasuredValue
        case { contains it, [clusterInt:0x042A, commandInt:0x0A, attrInt:0x0000] }:
        case { contains it, [clusterInt:0x042A, commandInt:0x01, attrInt:0x0000] }:
        
            // A MeasuredValue of 0xFFFFFFFF indicates that the measurement is invalid
            if (msg.value == "FFFFFFFF") return Log.warn("Ignored invalid PM25 value: 0x${msg.value}")
        
            Integer pm25 = Math.round Float.intBitsToFloat(Integer.parseInt(msg.value, 16))
            Utils.sendEvent name:"pm25", value:pm25, unit:"μg/m³", descriptionText:"Fine particulate matter (PM2.5) concentration is ${pm25} μg/m³", type:type
            def aqi = pm25Aqi(pm25)
            Utils.sendEvent name:"airQualityIndex", value:aqi[0], descriptionText:"Calculated Air Quality Index = ${aqi[0]}", type:type
            Utils.sendEvent name:"airQuality", value:"<span style=\"color:${aqi[2]}\">${aqi[1]}</span>", descriptionText:"Calculated Air Quality = ${aqi[1]}", type:type
            return Utils.processedZclMessage("${msg.commandInt == 0x0A ? "Report" : "Read"} Attributes Response", "PM25Measurement=${pm25} μg/m³")
        
        // Other events that we expect but are not usefull for capability.FineParticulateMatter behavior
        case { contains it, [clusterInt:0x042A, commandInt:0x07] }:
            return Utils.processedZclMessage("Configure Reporting Response", "attribute=pm25, data=${msg.data}")
        
        // Events for capability.VocIndex
        
        // Report/Read Attributes Reponse: MeasuredValue
        case { contains it, [clusterInt:0xFC7E, commandInt:0x0A, attrInt:0x0000] }:
        case { contains it, [clusterInt:0xFC7E, commandInt:0x01, attrInt:0x0000] }:
        
            // A MeasuredValue of 0xFFFFFFFF indicates that the measurement is invalid
            if (msg.value == "FFFFFFFF") return Log.warn("Ignored invalid VOC Index value: 0x${msg.value}")
        
            Integer vocIndex = Math.round Float.intBitsToFloat(Integer.parseInt(msg.value, 16))
            Utils.sendEvent name:"vocIndex", value:vocIndex, descriptionText:"Voc index is ${vocIndex} / 500", type:type
            return Utils.processedZclMessage("${msg.commandInt == 0x0A ? "Report" : "Read"} Attributes Response", "VocIndex=${msg.value}")
        
        // Other events that we expect but are not usefull for capability.VocIndex behavior
        case { contains it, [clusterInt:0xFC7E, commandInt:0x07] }:
            return Utils.processedZclMessage("Configure Reporting Response", "attribute=vocIndex, data=${msg.data}")
        
        // Events for capability.Temperature
        
        // Report/Read Attributes Reponse: MeasuredValue
        case { contains it, [clusterInt:0x0402, commandInt:0x0A, attrInt:0x0000] }:
        case { contains it, [clusterInt:0x0402, commandInt:0x01, attrInt:0x0000] }:
        
            // A MeasuredValue of 0x8000 indicates that the temperature measurement is invalid
            if (msg.value == "8000") return Log.warn("Ignored invalid temperature value: 0x${msg.value}")
        
            String temperature = convertTemperatureIfNeeded(Integer.parseInt(msg.value, 16) / 100, "C", 0)
            Utils.sendEvent name:"temperature", value:temperature, unit:"°${location.temperatureScale}", descriptionText:"Temperature is ${temperature} °${location.temperatureScale}", type:type
            return Utils.processedZclMessage("${msg.commandInt == 0x0A ? "Report" : "Read"} Attributes Response", "Temperature=${msg.value}")
        
        // Other events that we expect but are not usefull for capability.Temperature behavior
        case { contains it, [clusterInt:0x0402, commandInt:0x07] }:
            return Utils.processedZclMessage("Configure Reporting Response", "attribute=temperature, data=${msg.data}")
        
        // Events for capability.RelativeHumidity
        
        // Report/Read Attributes Reponse: MeasuredValue
        case { contains it, [clusterInt:0x0405, commandInt:0x0A, attrInt:0x0000] }:
        case { contains it, [clusterInt:0x0405, commandInt:0x01, attrInt:0x0000] }:
        
            // A MeasuredValue of 0xFFFF indicates that the measurement is invalid
            if (msg.value == "FFFF") return Log.warn("Ignored invalid relative humidity value: 0x${msg.value}")
        
            Integer humidity = Math.round(Integer.parseInt(msg.value, 16) / 100)
            Utils.sendEvent name:"humidity", value:humidity, unit:"%rh", descriptionText:"Relative humidity is ${humidity} %", type:type
            return Utils.processedZclMessage("${msg.commandInt == 0x0A ? "Report" : "Read"} Attributes Response", "RelativeHumidity=${msg.value}")
        
        // Other events that we expect but are not usefull for capability.RelativeHumidity behavior
        case { contains it, [clusterInt:0x0405, commandInt:0x07] }:
            return Utils.processedZclMessage("Configure Reporting Response", "attribute=humidity, data=${msg.data}")
        
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
