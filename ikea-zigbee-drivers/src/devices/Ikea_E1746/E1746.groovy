{{!--------------------------------------------------------------------------}}
{{# @definition }}
capability 'SignalStrength'
{{/ @definition }}
{{!--------------------------------------------------------------------------}}
{{# @attributes }}

// Attributes for devices.Ikea_E1746
attribute 'resets', 'number'
attribute 'macRxBcast', 'number'
attribute 'macTxBcast', 'number'
attribute 'apsRxBcast', 'number'
attribute 'apsTxBcast', 'number'
attribute 'nwkDropped', 'number'
attribute 'memFailures', 'number'
attribute 'macRetries', 'number'
{{/ @attributes }}
{{!--------------------------------------------------------------------------}}
{{# @commands }}

// Commands for devices.Ikea_E1746
command 'gatherNeighborsAndRoutes'
{{/ @commands }}
{{!--------------------------------------------------------------------------}}
{{# @updated }}

// Preferences for devices.Ikea_E1746
schedule('0 */10 * ? * *', 'refresh', [data:true])
{{/ @updated }}
{{!--------------------------------------------------------------------------}}
{{# @refresh }}

// Refresh for devices.Ikea_E1746
cmds += zigbee.readAttribute(0x0B05, [0x0000, 0x0100, 0x0101, 0x0106, 0x0107, 0x0112, 0x0117, 0x011B, 0x011C, 0x011D])
{{/ @refresh }}
{{!--------------------------------------------------------------------------}}
{{# @implementation }}

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

    String data = '<style>.ka_div { margin-left:-40px; width:calc(100% + 40px) } @media (max-width: 840px) { .ka_div { overflow-x:scroll; padding:0 1px; margin-left:-64px; width:calc(100% + 87px) } }</style><div class="ka_div"><pre>'
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
{{/ @implementation }}
{{!--------------------------------------------------------------------------}}
{{# @events }}

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
        neighbor += ['Rel', relationship]

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
        route += ['Next Hop', state.devs[msg.data[(pos + 3)..(pos + 4)].reverse().join()]]

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
        route += ['Route Status', routeStatus]
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
{{/ @events }}
{{!--------------------------------------------------------------------------}}
