/**
 * IKEA Tradfri Signal Repeater (E1746)
 *
 * @see https://dan-danache.github.io/hubitat/ikea-zigbee-drivers/
 */
import java.math.RoundingMode
import groovy.transform.CompileStatic
import groovy.transform.Field
import com.hubitat.zigbee.DataType

@Field static final String DRIVER_NAME = 'IKEA Tradfri Signal Repeater (E1746)'
@Field static final String DRIVER_VERSION = '5.3.0'

// Fields for capability.HealthCheck
import groovy.time.TimeCategory

@Field static final Map<String, String> HEALTH_CHECK = [
    'schedule': '0 0 0/1 ? * * *', // Health will be checked using this cron schedule
    'thereshold': '3600' // When checking, mark the device as offline if no Zigbee message was received in the last 3600 seconds
]

metadata {
    definition(name:DRIVER_NAME, namespace:'dandanache', author:'Dan Danache', importUrl:'https://raw.githubusercontent.com/dan-danache/hubitat/master/ikea-zigbee-drivers/Ikea_E1746.groovy') {
        capability 'Configuration'
        capability 'Refresh'
        capability 'SignalStrength'
        capability 'HealthCheck'
        capability 'PowerSource'

        fingerprint profileId:'0104', endpointId:'01', inClusters:'0000,0003,0009,0B05,1000,FC7C', outClusters:'0019,0020,1000', model:'TRADFRI Signal Repeater', manufacturer:'IKEA of Sweden', controllerType:'ZGB' // Firmware: 2.3.086 (117C-1102-23086631)
        
        // Attributes for devices.Ikea_E1746
        attribute 'resets', 'number'
        attribute 'macRxBcast', 'number'
        attribute 'macTxBcast', 'number'
        attribute 'apsRxBcast', 'number'
        attribute 'apsTxBcast', 'number'
        attribute 'nwkDropped', 'number'
        attribute 'memFailures', 'number'
        attribute 'macRetries', 'number'
        
        // Attributes for capability.HealthCheck
        attribute 'healthStatus', 'enum', ['offline', 'online', 'unknown']
    }
    
    // Commands for devices.Ikea_E1746
    command 'gatherNeighborsAndRoutes'
    
    // Commands for capability.FirmwareUpdate
    command 'updateFirmware'

    preferences {
        input(
            name:'helpInfo', type:'hidden',
            title:'''
            <div style="min-height:55px; background:transparent url('https://dan-danache.github.io/hubitat/ikea-zigbee-drivers/img/Ikea_E1746.webp') no-repeat left center;background-size:auto 55px;padding-left:60px">
                IKEA Tradfri Signal Repeater (E1746) <small>v5.3.0</small><br>
                <small><div>
                • <a href="https://dan-danache.github.io/hubitat/ikea-zigbee-drivers/#tradfri-signal-repeater-e1746" target="_blank">device details</a><br>
                • <a href="https://community.hubitat.com/t/release-ikea-zigbee-drivers/123853" target="_blank">community page</a><br>
                </div></small>
            </div>
            '''
        )
        input(
            name:'logLevel', type:'enum', title:'Log verbosity', required:true,
            description:'Select what type of messages appear in the "Logs" section',
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
    
    // Preferences for devices.Ikea_E1746
    schedule('0 */10 * ? * *', 'refresh', [data:true])
    
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
    log_info '⚙️ Finishing device configuration ...'
    List<String> cmds = ["he raw 0x${device.deviceNetworkId} 0x01 0x01 0x0003 {014300 3C00}"]

    // Auto-apply preferences
    cmds += updated true
    
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
    if (auto) log_debug '🎬 Refreshing device state (auto) ...'
    else log_info '🎬 Refreshing device state ...'
    if (!auto && device.currentValue('powerSource', true) == 'battery') {
        log_warn '[IMPORTANT] Click the "Refresh" button immediately after pushing any button on the device in order to first wake it up!'
    }

    List<String> cmds = []
    
    // Refresh for devices.Ikea_E1746
    cmds += zigbee.readAttribute(0x0B05, [0x0000, 0x0100, 0x0101, 0x0106, 0x0107, 0x0112, 0x0117, 0x011B, 0x011C, 0x011D])

    if (auto) return cmds
    utils_sendZigbeeCommands cmds
    return []
}

// Implementation for devices.Ikea_E1746
void gatherNeighborsAndRoutes() {
    log_info '🎬 Gathering neighbors and routes ...'
    state*.key.findAll { it.startsWith('ka_neighbor_') || it.startsWith('ka_route_') }.each { state.remove it }
    state.devs = ['0000':'🏠 Hubitat Hub'] + retrieveZigbeeDevices()
    state.remove 'neighbors'
    state.remove 'routes'

    utils_sendZigbeeCommands([
        "he raw 0x${device.deviceNetworkId} 0x00 0x00 0x0031 {55 00}} {0x0000}",
        "he raw 0x${device.deviceNetworkId} 0x00 0x00 0x0032 {56 00}} {0x0000}"
    ])
}
private Map<String, String> retrieveZigbeeDevices() {
    try {
         httpGet([uri:'http://127.0.0.1:8080/hub/zigbeeDetails/json']) { response ->
            response.data.devices.collectEntries { [(it.shortZigbeeId): it.name] }
        }
    } catch (Exception ex) {
        return ['ZZZZ': "Exception: ${ex}"]
    }
}
private int strLen(String str) {
    if (str == null) return 0
    String norm = str.replaceAll('\uFE0F', '')
    return norm.codePointCount(0, norm.length()) + norm.findAll(/[^\x00-\x7F]/).size()
}
private String strPad(String str, int width) {
    if (str == null) return '--'.padRight(width, ' ')
    return "${str}${' ' * (width - strLen(str))}"
}
private String printTable(List<List<String>> rows, Integer columnsNo) {
    if (!rows) return

    // Init columns width
    Map<Integer, Integer> widths = [:]
    (0..(columnsNo - 1)).each { widths[it] = 0 }

    // Calculate column widths
    rows.each { row -> widths.each { widths[it.key] = Math.max(it.value, strLen(row[it.key])) } }

    // Print table
    return rows.inject('') { ts, row -> ts + widths.inject('▸ ') { s, k, v -> s + (k == 0 ? '' : ' | ') + strPad(row[k], v) } + '\n' }
}
private String printWeirdTable(String stateKeyName, Integer columnsNo) {
    List<List<String>> rows = []
    int i = 0
    while (true) {
        List<String> row = state["${stateKeyName}_${i++}"]
        if (!row) break
        rows.add row
    }

    String data = '<style>@media (max-width: 840px) { .ka_div { overflow-x:scroll; padding:0 1px }}</style><div class="ka_div"><pre style="margin:0">'
    if (rows.size == 0) {
        data += '▸ Could not retrieve data\n'
    } else {
        List<List<String>> table = []
        rows.each { row ->
            List<String> record = []
            (0..((row.size() / 2) - 1)).each { idx -> record += "${row[idx * 2]}:${row[idx * 2 + 1]}" }
            table.add record
        }
        data += printTable(table, columnsNo)
    }
    return data + '</div></pre>'
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
        
        // Events for devices.Ikea_E1746
        // ===================================================================================================================
        case { contains it, [clusterInt:0x0B05, commandInt:0x01, attrInt:0x0000] }:
            Integer resets = Integer.parseInt msg.value, 16
            utils_sendEvent name:'resets', value:resets, descriptionText:"Device resets = ${resets}", type:type
            
            msg.additionalAttrs?.each {
                switch (it.attrInt) {
                    case 0x0100:
                        Long macRxBcast = Long.parseLong it.value, 16
                        utils_sendEvent name:'macRxBcast', value:macRxBcast, descriptionText:"macRxBcast = ${macRxBcast}", type:type
                        return
                    case 0x0101:
                        Long macTxBcast = Long.parseLong it.value, 16
                        utils_sendEvent name:'macTxBcast', value:macTxBcast, descriptionText:"macTxBcast = ${macTxBcast}", type:type
                        return
                    case 0x0106:
                        Integer apsRxBcast = Integer.parseInt it.value, 16
                        utils_sendEvent name:'apsRxBcast', value:apsRxBcast, descriptionText:"apsRxBcast = ${apsRxBcast}", type:type
                        return
                    case 0x0107:
                        Integer apsTxBcast = Integer.parseInt it.value, 16
                        utils_sendEvent name:'apsTxBcast', value:apsTxBcast, descriptionText:"apsTxBcast = ${apsTxBcast}", type:type
                        return
                    case 0x0112:
                        Integer nwkDropped = Integer.parseInt it.value, 16
                        utils_sendEvent name:'nwkDropped', value:nwkDropped, descriptionText:"nwkDropped = ${nwkDropped}", type:type
                        return
                    case 0x0117:
                        Integer memFailures = Integer.parseInt it.value, 16
                        utils_sendEvent name:'memFailures', value:memFailures, descriptionText:"memFailures = ${memFailures}", type:type
                        return
                    case 0x011B:
                        Integer macRetries = Integer.parseInt it.value, 16
                        utils_sendEvent name:'macRetries', value:macRetries, descriptionText:"macRetries = ${macRetries}", type:type
                        return
                    case 0x011C:
                        Integer lqi = Integer.parseInt it.value, 16
                        utils_sendEvent name:'lqi', value:lqi, descriptionText:"Signal LQI is ${lqi}", type:type
                        return
                    case 0x011D:
                        byte rssi = (byte) Integer.parseInt(it.value, 16)
                        utils_sendEvent name:'rssi', value:rssi, descriptionText:"Signal RSSI is ${rssi}", type:type
                        return
                }
            }
            utils_processedZclMessage "Read Attributes Response", "resets=${resets}"
            return
        
        case { contains it, [endpointInt:0x00, clusterInt:0x8031] }:
            if (msg.data[1] != '00') return
        
            Integer totalEntries = Integer.parseInt msg.data[2], 16
            Integer startIndex = Integer.parseInt msg.data[3], 16
            Integer includedEntries = Integer.parseInt msg.data[4], 16
            if (includedEntries == 0) return
        
            Integer pos = 5
            (0..(includedEntries - 1)).each {
                List<String> neighbor = []
                neighbor += ['Neighbor', state.devs[msg.data[(pos + 16)..(pos + 17)].reverse().join()]]
        
                String octet = Integer.toBinaryString(Integer.parseInt(msg.data[pos + 18], 16)).padLeft(8, '0')
                String deviceType = 'Unknown'
                switch (Integer.parseInt(octet.substring(6, 8), 2)) {
                    case 0x00:
                        deviceType = 'Coordinator'; break
                    case 0x01:
                        deviceType = 'Router'; break
                    case 0x02:
                        deviceType = 'End-Device'; break
                }
                neighbor += ['Type', deviceType]
        
                String relationship = 'Unknown'
                switch (Integer.parseInt(octet.substring(1, 4), 2)) {
                    case 0x00:
                        relationship = 'Parent'; break
                    case 0x01:
                        relationship = 'Child'; break
                    case 0x02:
                        relationship = 'Sibling'; break
                    case 0x03:
                        relationship = 'Unknown'; break
                    case 0x04:
                        relationship = 'Previous Child'; break
                }
                neighbor += ['Relation', relationship]
        
                // Depth, LQI
                neighbor += ['LQI', Integer.parseInt(msg.data[pos + 21], 16)]
                pos += 22
        
                state["ka_neighbor_${startIndex++}"] = neighbor
            }
        
            // Get next batch
            if (startIndex < totalEntries) utils_sendZigbeeCommands(["he raw 0x${device.deviceNetworkId} 0x00 0x00 0x0031 {55 ${utils_payload(startIndex, 2)}} {0x0000}"])
            else {
                state.neighbors = printWeirdTable 'ka_neighbor', 4
                state*.key.findAll { it.startsWith 'ka_neighbor_' }.each { state.remove it }
                if (state.routes) state.remove 'devs'
            }
        
            utils_processedZdpMessage 'Neighbors Table Response', "totalEntries=${totalEntries}, startIndex=${startIndex}, includedEntries=${includedEntries}"
            return
        
        case { contains it, [endpointInt:0x00, clusterInt:0x8032] }:
            if (msg.data[1] != '00') return
        
            Integer totalEntries = Integer.parseInt msg.data[2], 16
            Integer startIndex = Integer.parseInt msg.data[3], 16
            Integer includedEntries = Integer.parseInt msg.data[4], 16
            if (includedEntries == 0) return
        
            Integer pos = 5
            (0..(includedEntries - 1)).each {
                List<String> route = []
                route += ['Destination', state.devs[msg.data[pos..(pos + 1)].reverse().join()]]
                route += ['First Hop', state.devs[msg.data[(pos + 3)..(pos + 4)].reverse().join()]]
        
                String octet = Integer.toBinaryString(Integer.parseInt(msg.data[pos + 2], 16)).padLeft(8, '0').reverse()
                String routeStatusBinary = octet.substring(0, 3).reverse()
                String routeStatus = 'Reserved'
                switch (routeStatusBinary) {
                    case '000':
                        routeStatus = 'Active'
                        break
                    case '001':
                        routeStatus = 'Discovery underway'
                        break
                    case '010':
                        routeStatus = 'Discovery failed'
                        break
                    case '011':
                        routeStatus = 'Inactive'
                        break
                    case '100':
                        routeStatus = 'Validation underway'
                        break
                }
                route += ['Status', routeStatus]
                pos += 5
        
                state["ka_route_${startIndex++}"] = route
            }
        
            // Get next batch
            if (startIndex < totalEntries) utils_sendZigbeeCommands(["he raw 0x${device.deviceNetworkId} 0x00 0x00 0x0032 {56 ${utils_payload(startIndex, 2)}} {0x0000}"])
            else {
                state.routes = printWeirdTable 'ka_route', 3
                state*.key.findAll { it.startsWith 'ka_route_' }.each { state.remove it }
                if (state.neighbors) state.remove 'devs'
            }
            utils_processedZdpMessage 'Routing Table Response', "totalEntries=${totalEntries}, startIndex=${startIndex}, includedEntries=${includedEntries}"
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
