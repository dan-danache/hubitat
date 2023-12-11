/**
 * Knockturn Alley - Simple toolkit driver to help developers peer deep into the guts of Zigbee devices.
 *
 * @version 2.2.0
 * @see https://dan-danache.github.io/hubitat/knockturn-alley-driver/
 * @see https://dan-danache.github.io/hubitat/knockturn-alley-driver/CHANGELOG
 * @see https://community.hubitat.com/t/dev-knockturn-alley/125167
 */
import groovy.time.TimeCategory
import groovy.transform.Field

@Field static final def HEXADECIMAL_PATTERN = ~/\p{XDigit}+/

metadata {
    definition(name:"Knockturn Alley", namespace:"dandanache", singleThreaded:true, author:"Dan Danache", importUrl:"https://raw.githubusercontent.com/dan-danache/hubitat/master/knockturn-alley-driver/knockturn-alley.groovy") {
        capability "Actuator"
        attribute "documentation", "STRING"
    }
        
    command "a01Legilimens", [
        [name: "Manufacturer", description: "Manufacturer Code - hex format (e.g.: 0x117C)", type: "STRING"],
    ]
    command "a02Scourgify", [
        [name: "Raw data", type: "ENUM", constraints: [
            "1 - Remove raw data",
            "2 - Keep raw data",
        ]],
    ]
    command "a03Obliviate", [
        [name: "What to forget", type: "ENUM", constraints: [
            "1 - Our state variables (ka_*) - Restore previous driver state",
            "2 - All state variables",
            "3 - Device data",
            "4 - Scheduled jobs configured by the previous driver",
            "5 - Everything",
        ]],
    ]

    command "b01Accio", [
        [name: "What to retrieve", type: "ENUM", constraints: [
            "1 - Get attribute current value",
            "2 - Check attribute reporting",
        ]],
        [name: "Endpoint*", description: "Endpoint ID - hex format (e.g.: 0x01)", type: "STRING"],
        [name: "Cluster*", description: "Cluster ID - hex format (e.g.: 0x0001)", type: "STRING"],
        [name: "Attribute*", description: "Attribute ID - hex format (e.g.: 0x0001)", type: "STRING"],
        [name: "Manufacturer", description: "Manufacturer Code - hex format (e.g.: 0x117C)", type: "STRING"],
    ]
    command "b02EverteStatum", [
        [name: "Endpoint*", description: "Endpoint ID - hex format (e.g.: 0x01)", type: "STRING"],
        [name: "Cluster*", description: "Cluster ID - hex format (e.g.: 0x0001)", type: "STRING"],
        [name: "Attribute*", description: "Attribute ID - hex format (e.g.: 0x0001)", type: "STRING"],
        [name: "Manufacturer", description: "Manufacturer Code - hex format (e.g.: 0x117C)", type: "STRING"],
        [name: "Data Type*", description: "Attribute data type", type: "ENUM", constraints:
                ZCL_DATA_TYPES.keySet()
                    .findAll { ZCL_DATA_TYPES[it].bytes != "0" && ZCL_DATA_TYPES[it].bytes != "var" }
                    .sort()
                    .collect { "0x${Utils.hex it, 2}: ${ZCL_DATA_TYPES[it].name} (${ZCL_DATA_TYPES[it].bytes} bytes)" }
        ],
        [name: "Value*", description: "Attribute value - hex format (e.g.: 0001 - for uint16)", type: "STRING"],
    ]
    command "b03Oppugno", [
        [name: "Endpoint*", description: "Endpoint ID - hex format (e.g.: 0x01)", type: "STRING"],
        [name: "Cluster*", description: "Cluster ID - hex format (e.g.: 0x0001)", type: "STRING"],
        [name: "Attribute*", description: "Attribute ID - hex format (e.g.: 0x0001)", type: "STRING"],
        [name: "Manufacturer", description: "Manufacturer Code - hex format (e.g.: 0x117C)", type: "STRING"],
        [name: "Data Type*", description: "Attribute data type", type: "ENUM", constraints:
                ZCL_DATA_TYPES.keySet()
                    .findAll { ZCL_DATA_TYPES[it].bytes != "0" && ZCL_DATA_TYPES[it].bytes != "var" }
                    .sort()
                    .collect { "0x${Utils.hex it, 2}: ${ZCL_DATA_TYPES[it].name} (${ZCL_DATA_TYPES[it].bytes} bytes)" }
        ],
        [name: "Min Interval*", description: "Minimum reporting interval between issuing reports - in seconds [0..65534]", type: "NUMBER"],
        [name: "Max Interval*", description: "Maximum reporting interval between issuing reports - in seconds [0..65534], 65535 -> disable reporting", type: "NUMBER"],
        [name: "Reportable Change*", description: "Minimum change to the attribute that will trigger a report - hex format (e.g.: 0001 - for uint16)", type: "STRING"],
    ]
    command "c01Imperio", [
        [name: "Endpoint*", description: "Endpoint ID - hex format (e.g.: 0x01)", type: "STRING"],
        [name: "Cluster*", description: "Cluster ID - hex format (e.g.: 0x0001)", type: "STRING"],
        [name: "Command*", description: "Command ID - hex format (e.g.: 0x01)", type: "STRING"],
        [name: "Manufacturer", description: "Manufacturer Code - hex format (e.g.: 0x117C)", type: "STRING"],
        [name: "Payload", description: "Raw payload - sent as is, spaces are removed", type: "STRING"],
    ]
    command "c02Bombarda", [
        [name: "Zigbee command", description: "Enter raw command to execute (e.g. for toggle on/off: he raw .addr 0x01 0x01 0x0006 {114302})", type: "STRING"]
    ]

    command "d01Revelio", [
        [name: "What to reveal", type: "ENUM", constraints: [
            "1 - Neighbors Table",
            "2 - Routing Table",
            "3 - Bindings Table",
        ]],
    ]
    command "d02UnbreakableVow", [
        [name: "What to do", type: "ENUM", constraints: [
            "1 - Make the Unbreakable Vow",
            "2 - Break the Unbreakable Vow",
        ]],
        [name: "Source Addr*", description: "The IEEE address for the source - 8 bytes", type: "STRING", default: "caca"],
        [name: "Source Endpoint*", description: "Endpoint ID - hex format (e.g.: 0x01)", type: "STRING"],
        [name: "Cluster*", description: "Cluster ID - hex format (e.g.: 0x0001)", type: "STRING"],
        [name: "Addr Mode*", description: "Addressing mode for Destination Address", type: "ENUM", constraints: [
            "0x03 = 64-bit extended address",
            "0x01 = 16-bit group address",
        ]],
        [name: "Destination Addr*", description: "The IEEE/group address for the destination - 8/2 bytes", type: "STRING"],
        [name: "Destination Endpoint", description: "Endpoint ID - hex format (e.g.: 0x01), required when Addr Mode = 0x03", type: "STRING"],
    ]

    preferences {
        input(
            name: "lang",
            type: "enum",
            title: "Language",
            options: [
                "hp-HP":"Harry Potter",
                "en-US":"English",
            ],
            defaultValue: "hp-HP",
            required: true
        )
    }
}

// ===================================================================================================================
// Spells
// ===================================================================================================================

def a01Legilimens(manufacturerHex="0x0000") {
    sendEvent name:"documentation", value:"<a href=\"https://dan-danache.github.io/hubitat/knockturn-alley-driver/\" target=\"_blank\">README</a>", isStateChange:false
    Log.info "🪄 Legilimens: manufacturer=${manufacturerHex}"

    if (!manufacturerHex.startsWith("0x") || manufacturerHex.size() != 6) return Log.error("Invalid Manufacturer Code: ${manufacturerHex}")
    State.setManufacturer(manufacturerHex.substring(2))

    List<String> cmds = []

    // Active_EP_req
    cmds += "he raw 0x${device.deviceNetworkId} 0x00 0x00 0x0005 {42 ${Utils.flip device.deviceNetworkId}} {0x0000}"

    // Node_Desc_req
    cmds += "he raw 0x${device.deviceNetworkId} 0x00 0x00 0x0002 {43 ${Utils.flip device.deviceNetworkId}} {0x0000}"

    // Power_Desc_req
    cmds += "he raw 0x${device.deviceNetworkId} 0x00 0x00 0x0003 {44 ${Utils.flip device.deviceNetworkId}} {0x0000}"

    // Mgmt_Lqi_req
    cmds += "he raw 0x${device.deviceNetworkId} 0x00 0x00 0x0031 {45 00} {0x0000}"

    // Mgmt_Rtg_req
    cmds += "he raw 0x${device.deviceNetworkId} 0x00 0x00 0x0032 {46 00} {0x0000}"

    // Mgmt_Bind_req
    cmds += "he raw 0x${device.deviceNetworkId} 0x00 0x00 0x0033 {47 00} {0x0000}"

    Utils.sendZigbeeCommands(cmds)
}

private String printSeparator(chars = "---") {
    return "${chars * 32}\n"
}

private String printHeader(String header) {
    return "${"===" * 32}\n${header}\n${"---" * 32}\n"
}

private String printList(String header, List<String> list) {
    if (!list) return
    Map<Integer, String> indexedList = list.indexed()
    Integer maxKeyLen = indexedList.inject(0) { s, k, v -> k & 1 ? s : Math.max(s, v.size()) }
    return indexedList.inject(printHeader(header)) { s, k, v -> s + (k & 1 ? " = ${v}\n" : "▸ " + v.padRight(maxKeyLen, " ")) }
}

private String printTable(List<List<String>> rows, Integer columnsNo) {
    if (!rows) return

    // Init columns width
    Map<Integer, Integer> widths = [:]
    (0..(columnsNo-1)).each { widths[it] = 0 }

    // Calculate column widths
    rows.each { row -> widths.each { widths[it.key] = Math.max(it.value, row[it.key]?.size() ?: 0) }}

    // Print table
    return rows.inject("") { ts, row -> ts + widths.inject("▸ ") { s, k, v -> s + (k == 0 ? "" : " | ") + (row[k] ?: "--").padRight(v, " ") } + "\n"}
}

private printWeirdTable(String header, List<List<String>> rows, Integer columnsNo) {
    String data = printHeader(header)
    if (!rows) {
        data += "▸ Could not retrieve data\n"
    } else {
        List<List<String>> table = []
        rows.each { row ->
            List<String> record = []
            (0..((row.size() / 2) - 1)).each { idx -> record += "${row[idx * 2]}:${row[idx * 2 + 1]}" }
            table.add record
        }
        data += printTable(table, columnsNo)
    }
}

def a02Scourgify(operation) {
    sendEvent name:"documentation", value:"<a href=\"https://dan-danache.github.io/hubitat/knockturn-alley-driver/\">README</a>", isStateChange:false
    Log.info "🪄 Scourgify: ${operation}"
    if (!state.ka_endpoints) {
        return Log.warn("Raw data is missing. Maybe you should run Legilimens first if you didn't do that already ...")
    }

    String data = "<style>#ka_div { margin-left:-40px; width:calc(100% + 40px) } @media (max-width: 840px) { #ka_div { overflow-x:scroll; padding:0 1px; margin-left:-64px; width:calc(100% + 87px) } }</style>"
    data += "<script type=text/javascript>function selectCode(el) { if (!window.event.ctrlKey) return; var range = document.createRange(); range.selectNodeContents(el); var sel = window.getSelection(); sel.removeAllRanges(); sel.addRange(range); }</script>"
    data += "<div id=ka_div><pre onclick=\"selectCode(this)\">"

    // Node & Power Descriptors
    data += printList("Node Descriptor", state.ka_nodeDescriptor)
    data += printList("Power Descriptor", state.ka_powerDescriptor)

    // Endpoints
    state.ka_endpoints?.sort().each { endpoint ->
        data += printSeparator("===")
        data += "Endpoint 0x${Utils.hex endpoint, 2}\n"

        state["ka_outClusters_${endpoint}"]?.sort().each { cluster ->
            data += printHeader("Out Cluster: ${"0x${Utils.hex cluster, 4} (${ZCL_CLUSTERS.get(cluster)?.name ?: "Unknown Cluster"})"}")

            // Commands
            Set<Integer> commands = []
            getState()?.each {
                if (it.key.startsWith("ka_outCommand_${endpoint}_${cluster}_")) {
                    commands += Integer.parseInt it.key.split("_").last()
                }
            }

            if (commands.size() == 0) {
                data += "▸ No generated commands\n"
            } else {
                List<List<String>> table = []

                commands.sort().each { command ->
                    def commandSpec = ZCL_CLUSTERS.get(cluster)?.get("commands")?.get(command)

                    String cManufacturer = state["ka_outCommand_${endpoint}_${cluster}_${command}"]
                    String cName = commandSpec?.name ?: "--"
                    String cReq = commandSpec?.req ?: "--"

                    List<String> row = [
                        "0${cManufacturer == "0000" ? "x" : "_"}${Utils.hex command, 2}",
                        "${cName}",
                        "${cReq}"
                    ]
                    table.add row
                }

                data += printTable(table, 3)
            }
        }

        state["ka_inClusters_${endpoint}"]?.sort().each { cluster ->
            data += printHeader("In Cluster: ${"0x${Utils.hex cluster, 4} (${ZCL_CLUSTERS.get(cluster)?.name ?: "Unknown Cluster"})"}")

            // Attributes
            Set<Integer> attributes = []
            getState()?.each {
                if (it.key.startsWith("ka_attribute_${endpoint}_${cluster}_") || it.key.startsWith("ka_attributeValue_${endpoint}_${cluster}_")) {
                    attributes += Integer.parseInt it.key.split("_").last()
                }
            }

            if (attributes.size() == 0) {
                data += "▸ No attributes\n"
            } else {
                List<List<String>> table = []
                attributes.sort().each { attribute ->
                    def attributeSpec = ZCL_CLUSTERS.get(cluster)?.get("attributes")?.get(attribute)

                    // Cluster Revision global attribute
                    if (attribute == 0xFFFD) {
                        attributeSpec = [ type:0x21, req:"req", acc:"r--", name:"Cluster Revision" ]
                        attributeType = ZCL_DATA_TYPES[0x21]
                    }

                    String aName = attributeSpec?.name ?: "--"
                    String aRequired = attributeSpec?.req ?: "--"
                    String aAccess = attributeSpec?.acc ?: "---"

                    List<String> attributeInfo = state["ka_attribute_${endpoint}_${cluster}_${attribute}"]
                    def attributeType = ZCL_DATA_TYPES[Integer.parseInt(attributeInfo[0], 16)]
                    if (aAccess == "---" && attributeInfo[1] != "---") aAccess = attributeInfo[1]

                    String aManufacturer = attributeInfo[2]
                    String aType = attributeType?.name ?: "--"

                    def attributeValue = state["ka_attributeValue_${endpoint}_${cluster}_${attribute}"]
                    def attributeReporting = state["ka_attributeReporting_${endpoint}_${cluster}_${attribute}"]

                    String aReporting = attributeReporting ? "${attributeReporting.min}..${attributeReporting.max}" : "--"

                    // Pretty value
                    String aValue = "${attributeValue?.value ?: "--"}"
                    if (attributeValue?.value) {
                        if (attributeValue?.value && attributeSpec?.constraints) {
                            aValue += " = ${attributeSpec.constraints[Utils.dec(attributeValue.value)]}"
                        } else if (attributeValue?.value && attributeSpec?.decorate) {
                            aValue += " = ${attributeSpec.decorate(attributeValue.value)}"
                        } else if (attributeType?.decorate) {
                            aValue += " = ${attributeType.decorate(attributeValue.value)}"
                        }
                    }

                    List<String> row = [
                        "0${aManufacturer == "0000" ? "x" : "_"}${Utils.hex attribute, 4}",
                        "${aName}",
                        "${aRequired}",
                        "${aAccess}",
                        "${aType}",
                        "${aValue}",
                        "${aReporting}"
                    ]
                    table.add row
                }

                data += printTable(table, 7)
            }
            data += printSeparator()

            // Commands
            Set<Integer> commands = []
            getState()?.each {
                if (it.key.startsWith("ka_inCommand_${endpoint}_${cluster}_")) {
                    commands += Integer.parseInt it.key.split("_").last()
                }
            }

            if (commands.size() == 0) {
                data += "▸ No received commands\n"
            } else {
                List<List<String>> table = []

                commands.sort().each { command ->
                    def commandSpec = ZCL_CLUSTERS.get(cluster)?.get("commands")?.get(command)

                    String cManufacturer = state["ka_inCommand_${endpoint}_${cluster}_${command}"]
                    String cName = commandSpec?.name ?: "--"
                    String cReq = commandSpec?.req ?: "--"

                    List<String> row = [
                        "0${cManufacturer == "0000" ? "x" : "_"}${Utils.hex command, 2}",
                        "${cName}",
                        "${cReq}"
                    ]
                    table.add row
                }

                data += printTable(table, 3)
            }
        }
    }

    // ZDP tables
    data += printWeirdTable("Neighbors Table", state.ka_neighbors, 6)
    data += printWeirdTable("Routing Table", state.ka_routes, 3)
    data += printWeirdTable("Bindings Table", state.ka_bindings, 5)
    data += "</pre></div>"

    // Cleanup raw data?
    if (operation == "1 - Remove raw data") {
       getState()?.findAll { it.key.startsWith("ka_") }?.collect { it.key }.each { state.remove it }
    }

    state.ka_report = data
}

def a03Obliviate(operation) {
    sendEvent name:"documentation", value:"<a href=\"https://dan-danache.github.io/hubitat/knockturn-alley-driver/\" target=\"_blank\">README</a>", isStateChange:false
    Log.info "🪄 Obliviate: ${operation}"

    switch (operation) {
        case { it.startsWith("5 - ") }:
            state.clear()
            device.getData().collect { it.key }.each { device.removeDataValue it }
            return unschedule()

        case { it.startsWith("4 - ") }:
            return unschedule()

        case { it.startsWith("3 - ") }:
            return device.getData()?.collect { it.key }.each { device.removeDataValue it }

        case { it.startsWith("2 - ") }:
            return state.clear()
        
        case { it.startsWith("1 - ") }:
            return getState()?.findAll { it.key.startsWith("ka_") }?.collect { it.key }.each { state.remove it }

        default:
            Log.error "Don't know how to ${operation}"
    }
}

def b01Accio(operation, endpointHex, clusterHex, attributeHex, manufacturerHex="") {
    sendEvent name:"documentation", value:"<a href=\"https://dan-danache.github.io/hubitat/knockturn-alley-driver/\" target=\"_blank\">README</a>", isStateChange:false
    Log.info "🪄 Accio: ${operation} (endpoint=${endpointHex}, cluster=${clusterHex}, attribute=${attributeHex}, manufacturer=${manufacturerHex})"

    if (!endpointHex.startsWith("0x") || endpointHex.size() != 4) return Log.error("Invalid Endpoint ID: ${endpointHex}")
    if (!clusterHex.startsWith("0x") || clusterHex.size() != 6) return Log.error("Invalid Cluster ID: ${clusterHex}")
    if (!attributeHex.startsWith("0x") || attributeHex.size() != 6) return Log.error("Invalid Attribute ID: ${clusterHex}")
    if (manufacturerHex && (!manufacturerHex.startsWith("0x") || manufacturerHex.size() != 6)) return Log.error("Invalid Manufacturer Code: ${manufacturerHex}")

    Integer endpoint = Integer.parseInt endpointHex.substring(2), 16
    Integer cluster = Integer.parseInt clusterHex.substring(2), 16
    Integer attribute = Integer.parseInt attributeHex.substring(2), 16

    Integer manufacturer = manufacturerHex ? Integer.parseInt(manufacturerHex.substring(2), 16) : null
    String frameStart = "1043"
    if (manufacturer != null) {
        frameStart = "04${Utils.payload manufacturer}43"
    }

    switch (operation) {
        case { it.startsWith("1 - ") }:
            String command = "00"
            String payload = "${Utils.payload attribute}"
            return Utils.sendZigbeeCommands(["he raw 0x${device.deviceNetworkId} 0x${Utils.hex endpoint, 2} 0x01 0x${Utils.hex cluster} {${frameStart}${command} ${payload}}"])

        case { it.startsWith("2 - ") }:
            String command = "08"
            String payload = "00 ${Utils.payload attribute}"
            return Utils.sendZigbeeCommands(["he raw 0x${device.deviceNetworkId} 0x${Utils.hex endpoint, 2} 0x01 0x${Utils.hex cluster} {${frameStart}${command} ${payload}}"])
    }
}

def b02EverteStatum(endpointHex, clusterHex, attributeHex, manufacturerHex="", typeStr, valueHex) {
    sendEvent name:"documentation", value:"<a href=\"https://dan-danache.github.io/hubitat/knockturn-alley-driver/\" target=\"_blank\">README</a>", isStateChange:false
    Log.info "🪄 Everte Statum: endpoint=${endpointHex}, cluster=${clusterHex}, attribute=${attributeHex}, manufacturer=${manufacturerHex}, type=${typeStr}, value=${valueHex}"

    if (!endpointHex.startsWith("0x") || endpointHex.size() != 4) return Log.error("Invalid Endpoint ID: ${endpointHex}")
    if (!clusterHex.startsWith("0x") || clusterHex.size() != 6) return Log.error("Invalid Cluster ID: ${clusterHex}")
    if (!attributeHex.startsWith("0x") || attributeHex.size() != 6) return Log.error("Invalid Attribute ID: ${clusterHex}")
    if (manufacturerHex && (!manufacturerHex.startsWith("0x") || manufacturerHex.size() != 6)) return Log.error("Invalid Manufacturer Code: ${manufacturerHex}")
    if (valueHex && !HEXADECIMAL_PATTERN.matcher(valueHex).matches()) return Log.error("Invalid Value: ${valueHex}")

    Integer endpoint = Integer.parseInt endpointHex.substring(2), 16
    Integer cluster = Integer.parseInt clusterHex.substring(2), 16
    Integer attribute = Integer.parseInt attributeHex.substring(2), 16

    Integer manufacturer = manufacturerHex ? Integer.parseInt(manufacturerHex.substring(2), 16) : null
    String frameStart = "1043"
    if (manufacturer != null) {
        frameStart = "04${Utils.payload manufacturer}43"
    }
 
    Integer type = Integer.parseInt typeStr.substring(2, 4), 16
    String value = Utils.flip valueHex
    Integer typeLen = Integer.parseInt ZCL_DATA_TYPES[type].bytes
    if (value.size() != typeLen * 2) return Log.error("Invalid Value: It must have exactly ${typeLen} bytes but you provided ${value.size()}: ${valueHex}")

    // Send zigbee command
    String command = "02"
    String payload = "${Utils.payload attribute} ${typeStr.substring(2, 4)} ${value}"
    Utils.sendZigbeeCommands(["he raw ${device.deviceNetworkId} 0x${Utils.hex endpoint, 2} 0x01 0x${Utils.hex cluster} {${frameStart}${command} ${payload}}"])
}

def b03Oppugno(endpointHex, clusterHex, attributeHex, manufacturerHex="", typeStr, minInterval, maxInterval, reportableChangeHex) {
    sendEvent name:"documentation", value:"<a href=\"https://dan-danache.github.io/hubitat/knockturn-alley-driver/\" target=\"_blank\">README</a>", isStateChange:false
    Log.info "🪄 Oppugno: endpoint=${endpointHex}, cluster=${clusterHex}, attribute=${attributeHex}, manufacturer=${manufacturerHex}, type=${typeStr}, minInterval=${minInterval}, maxInterval=${maxInterval}, reportableChange=${reportableChangeHex}"

    if (!endpointHex.startsWith("0x") || endpointHex.size() != 4) return Log.error("Invalid Endpoint ID: ${endpointHex}")
    if (!clusterHex.startsWith("0x") || clusterHex.size() != 6) return Log.error("Invalid Cluster ID: ${clusterHex}")
    if (!attributeHex.startsWith("0x") || attributeHex.size() != 6) return Log.error("Invalid Attribute ID: ${clusterHex}")
    if (manufacturerHex && (!manufacturerHex.startsWith("0x") || manufacturerHex.size() != 6)) return Log.error("Invalid Manufacturer Code: ${manufacturerHex}")
    if (reportableChangeHex && !HEXADECIMAL_PATTERN.matcher(reportableChangeHex).matches()) return Log.error("Invalid Reportable Change: ${reportableChangeHex}")
    if (minInterval < 0 || minInterval > 65535)  return Log.error("Invalid Min Interval: ${minInterval}")
    if (maxInterval < 0 || maxInterval > 65535)  return Log.error("Invalid Max Interval: ${maxInterval}")

    Integer endpoint = Integer.parseInt endpointHex.substring(2), 16
    Integer cluster = Integer.parseInt clusterHex.substring(2), 16
    Integer attribute = Integer.parseInt attributeHex.substring(2), 16

    Integer manufacturer = manufacturerHex ? Integer.parseInt(manufacturerHex.substring(2), 16) : null
    String frameStart = "1043"
    if (manufacturer != null) {
        frameStart = "04${Utils.payload manufacturer}43"
    }

    Integer type = Integer.parseInt typeStr.substring(2, 4), 16
    String reportableChange = Utils.flip reportableChangeHex
    Integer typeLen = Integer.parseInt ZCL_DATA_TYPES[type].bytes
    if (reportableChange.size() != typeLen * 2) return Log.error("Invalid Reportable Change: It must have exactly ${typeLen} bytes but you provided ${reportableChange.size()}: ${valueHex}")

    // Send zigbee command
    String command = "06"
    String payload = "00 ${Utils.payload attribute} ${typeStr.substring(2, 4)} ${Utils.payload (minInterval as Integer)} ${Utils.payload (maxInterval as Integer)} ${reportableChange}"
    Utils.sendZigbeeCommands(["he raw ${device.deviceNetworkId} 0x${Utils.hex endpoint, 2} 0x01 0x${Utils.hex cluster} {${frameStart}${command} ${payload}} "])
}

def c01Imperio(endpointHex, clusterHex, commandHex, manufacturerHex="", payload="") {
    sendEvent name:"documentation", value:"<a href=\"https://dan-danache.github.io/hubitat/knockturn-alley-driver/\" target=\"_blank\">README</a>", isStateChange:false
    Log.info "🪄 Imperio: endpoint=${endpointHex}, cluster=${clusterHex}, command=${commandHex}, manufacturer=${manufacturerHex}, payload=${payload}"

    if (!endpointHex.startsWith("0x") || endpointHex.size() != 4) return Log.error("Invalid Endpoint ID: ${endpointHex}")
    if (!clusterHex.startsWith("0x") || clusterHex.size() != 6) return Log.error("Invalid Cluster ID: ${clusterHex}")
    if (!commandHex.startsWith("0x") || commandHex.size() != 4) return Log.error("Invalid Command ID: ${commandHex}")
    if (manufacturerHex && (!manufacturerHex.startsWith("0x") || manufacturerHex.size() != 6)) return Log.error("Invalid value for Manufacturer Code: ${manufacturerHex}")
    if (payload && !HEXADECIMAL_PATTERN.matcher(payload).matches()) return Log.error("Invalid Payload: ${payload}")
    
    Integer endpoint = Integer.parseInt endpointHex.substring(2), 16
    Integer cluster = Integer.parseInt clusterHex.substring(2), 16
    Integer command = Integer.parseInt commandHex.substring(2), 16

    Integer manufacturer = manufacturerHex ? Integer.parseInt(manufacturerHex.substring(2), 16) : null
    String frameStart = "0143"
    if (manufacturer != null) {
        frameStart = "05${Utils.payload manufacturer}43"
    }

    // Send zigbee command
    Utils.sendZigbeeCommands(["he raw 0x${device.deviceNetworkId} 0x${Utils.hex endpoint, 2} 0x01 0x${Utils.hex cluster} {${frameStart}${Utils.hex command, 2} ${payload}}"])
}

def c02Bombarda(command) {
    sendEvent name:"documentation", value:"<a href=\"https://dan-danache.github.io/hubitat/knockturn-alley-driver/\" target=\"_blank\">README</a>", isStateChange:false
    Log.info "🪄 Bombarda: command=${command}"

    String cmd = command.replace(".addr", "0x${device.deviceNetworkId}")
    Utils.sendZigbeeCommands([cmd])
}

def d01Revelio(operation) {
    sendEvent name:"documentation", value:"<a href=\"https://dan-danache.github.io/hubitat/knockturn-alley-driver/\" target=\"_blank\">README</a>", isStateChange:false
    Log.info "🪄 Revelio: ${operation}"

    switch (operation) {
        case "1 - Neighbors Table":
            return Utils.sendZigbeeCommands(["he raw 0x${device.deviceNetworkId} 0x00 0x00 0x0031 {55 00} {0x0000}"])

        case "2 - Routing Table":
            return Utils.sendZigbeeCommands(["he raw 0x${device.deviceNetworkId} 0x00 0x00 0x0032 {56 00} {0x0000}"])

        case "3 - Bindings Table":
            return Utils.sendZigbeeCommands(["he raw 0x${device.deviceNetworkId} 0x00 0x00 0x0033 {57 00} {0x0000}"])
    }
}

def d02UnbreakableVow(operation, srcAddrHex, srcEndpointHex, clusterHex, addrModeValue, dstAddrHex, dstEndpointHex="") {
    sendEvent name:"documentation", value:"<a href=\"https://dan-danache.github.io/hubitat/knockturn-alley-driver/\" target=\"_blank\">README</a>", isStateChange:false
    Log.info "🪄 Unbreakable Vow: ${operation}: srcAddr=${srcAddrHex}, srcEndpoint=${srcEndpointHex}, cluster=${clusterHex}, addrMode=${addrModeValue}, dstAddr=${dstAddrHex}, dstEndpoint=${dstEndpointHex}"

    if (!srcAddrHex || srcAddrHex.size() != 16 || !HEXADECIMAL_PATTERN.matcher(srcAddrHex).matches()) return Log.error("Invalid Source Addr: ${srcAddrHex}")
    if (!srcEndpointHex.startsWith("0x") || srcEndpointHex.size() != 4) return Log.error("Invalid Source Endpoint: ${srcEndpointHex}")
    if (!clusterHex.startsWith("0x") || clusterHex.size() != 6) return Log.error("Invalid Cluster ID: ${clusterHex}")
    String addrMode = addrModeValue.substring 2, 4
    if (addrMode == "03") {
        if (!dstAddrHex || dstAddrHex.size() != 16 || !HEXADECIMAL_PATTERN.matcher(dstAddrHex).matches()) return Log.error("Invalid Destination Addr: ${dstAddrHex}")
        if (!dstEndpointHex.startsWith("0x") || dstEndpointHex.size() != 4) return Log.error("Invalid Destination Endpoint: ${dstEndpointHex}")
    } else {
        if (!dstAddrHex || dstAddrHex.size() != 4 || !HEXADECIMAL_PATTERN.matcher(dstAddrHex).matches()) return Log.error("Invalid Destination Addr: ${dstAddrHex}")
    }

    String srcAddr = Utils.flip srcAddrHex
    String srcEndpoint = srcEndpointHex.substring 2
    String cluster = Utils.flip clusterHex.substring(2)
    String dstAddr = Utils.flip dstAddrHex
    String dstEndpoint = dstEndpointHex ? dstEndpointHex.substring(2) : ""

    // Send ZDP command
    String zdpCluster = operation == "1 - Make the Unbreakable Vow" ? "0x0021" : "0x0022"
    String payload = "${srcAddr} ${srcEndpoint} ${cluster} ${addrMode} ${dstAddr}${addrMode == "03" ? " ${dstEndpoint}" : ""}"
    Utils.sendZigbeeCommands(["he raw 0x${device.deviceNetworkId} 0x00 0x00 ${zdpCluster} {49 ${payload}} {0x0000}"])
}

// ===================================================================================================================
// Handle incoming Zigbee messages
// ===================================================================================================================

def parse(String description) {
    Log.debug "description=[${description}]"
    if (description.startsWith("zone status")) return

    // Extract msg
    def msg = zigbee.parseDescriptionAsMap description
    if (msg.containsKey("endpoint")) msg.endpointInt = Integer.parseInt(msg.endpoint, 16)
    if (msg.containsKey("sourceEndpoint")) msg.endpointInt = Integer.parseInt(msg.sourceEndpoint, 16)
    if (msg.clusterInt == null) msg.clusterInt = Integer.parseInt(msg.cluster, 16)
    msg.commandInt = Integer.parseInt(msg.command, 16)

    if (description.startsWith('read attr')) {
        msg.isClusterSpecific = false
    }
    Log.debug "msg=[${msg}]"

    switch (msg) {

        // Read Attribute Response (0x01) & Report attributes (0x0A)
        case { contains it, [isClusterSpecific:false, commandInt:0x01] }:
        case { contains it, [isClusterSpecific:false, commandInt:0x0A] }:
            if (!msg.endpoint) {
                return Utils.failedZclMessage("Read Attribute Response", msg.data[2], msg)
            }
            
            Integer endpoint = Utils.dec msg.endpoint
            Integer cluster = msg.clusterInt

            Map<Integer, Map<String, String>> attributesValues = [:]
            attributesValues[msg.attrInt] = [encoding: msg.encoding, value: msg.value]
            Utils.processedZigbeeMessage "Read Attribute Response", "endpoint=0x${Utils.hex endpoint, 2}, cluster=0x${Utils.hex cluster}, attribute=0x${Utils.hex msg.attrInt}, value=${msg.value}"

            msg.additionalAttrs?.each {
                attributesValues[it.attrInt] = [encoding: it.encoding, value: it.value]
                Utils.processedZigbeeMessage "Additional Attribute", "endpoint=0x${Utils.hex endpoint, 2}, cluster=0x${Utils.hex cluster}, attribute=0x${Utils.hex it.attrInt}, value=${it.value}"
            }
        
            return State.addAttributesValues(endpoint, cluster, attributesValues)

        // DefaultResponse (0x0B) :=  { 08:CommandIdentifier, 08:Status }
        // Example: [00, 80] -> command = 0x00, status = MALFORMED_COMMAND (0x80)
        case { contains it, [isClusterSpecific:false, commandInt:0x0B] }:
            if (msg.data[1] != "00") {
                return Utils.failedZclMessage("Default Response", msg.data[1], msg)
            }
            
            Integer endpoint = Utils.dec msg.sourceEndpoint
            Integer cluster = msg.clusterInt
            Integer command = Utils.dec msg.data[0]
        
            return Utils.processedZigbeeMessage("Default Response", "endpoint=0x${Utils.hex endpoint, 2}, cluster=0x${Utils.hex cluster}, command=0x${Utils.hex command, 2}")

        // Write Attribute Response (0x04)
        case { contains it, [isClusterSpecific:false, commandInt:0x04] }:
            if (msg.data[0] != "00") {
                return Utils.failedZclMessage("Write Attribute Response", msg.data[0], msg)
            }
            return Utils.processedZigbeeMessage("Write Attribute Response", "data=${msg.data}")

        // Configure Reporting Response (0x07)
        case { contains it, [isClusterSpecific:false, commandInt:0x07] }:
            if (msg.data[0] != "00") {
                return Utils.failedZclMessage("Configure Reporting Response", msg.data[0], msg)
            }

            return Utils.processedZigbeeMessage("Configure Reporting Response", "data=${msg.data}")

        // Read Reporting Configuration Response (0x09)
        case { contains it, [isClusterSpecific:false, commandInt:0x09] }:
            if (msg.data[0] != "00") {
                return Utils.failedZclMessage("Read Reporting Configuration Response", msg.data[0], msg)
            }
            
            Integer endpoint = Utils.dec msg.sourceEndpoint
            Integer cluster = msg.clusterInt
            Integer attribute = Utils.dec msg.data[2..3].reverse().join()
            Integer minPeriod = Utils.dec msg.data[5..6].reverse().join()
            Integer maxPeriod = Utils.dec msg.data[7..8].reverse().join()

            State.addAttributeReporting endpoint, cluster, attribute, minPeriod, maxPeriod
            return Utils.processedZigbeeMessage("Read Reporting Configuration Response", "endpoint=0x${Utils.hex endpoint, 2}, cluster=0x${Utils.hex cluster}, attribute=0x${Utils.hex attribute}, minPeriod=${minPeriod}, maxPeriod=${maxPeriod}")

        // DiscoverAttributesResponse := { 08:Complete?, n*24:AttributeInformation }
        // AttributeInformation := { 16:AttributeIdentifier, 08:AttributeDataType }
        // AttributeDataType := @see @Field ZCL_DATA_TYPES
        // Example: [01, 00, 00, 20, 01, 00, 20, 02, 00, 20, 03, 00, 20, 04, 00, 42, 05, 00, 42, 06, 00, 42, 07, 00, 30, 08, 00, 30, 09, 00, 30, 0A, 00, 41, 00, 40, 42, FD, FF, 21]
        //
        // DiscoverAttributesExtendedResponse := { 08:Complete?, n*24:AttributeInformation }
        // AttributeInformation := { 16:AttributeIdentifier, 08:AttributeDataType, 08:AttributeAccessControl }
        // AttributeDataType := @see @Field ZCL_DATA_TYPES
        // Example: [01, 20, 00, 20, 05, 21, 00, 20, 05, FD, FF, 21, 05]
        case { contains it, [isClusterSpecific:false, commandInt:0x0D] }:
        case { contains it, [isClusterSpecific:false, commandInt:0x16] }:
            Integer endpoint = Utils.dec msg.sourceEndpoint
            Integer cluster = msg.clusterInt
            String manufacturer = msg.manufacturerId

            boolean isExtended = msg.commandInt == 0x16
            Integer attrInfoBytes = isExtended ? 4 : 3

            List<String> data = msg.data.drop 1
            Map<Integer, List<String>> attributes = [:]
            Map<Integer, List<String>> varAttributes = [:]
            while (data.size() >= attrInfoBytes) {
                List<String> chunk = data.take attrInfoBytes
                //Log.info("isExtended=${isExtended}, attrInfoBytes=${attrInfoBytes}, chunk=${chunk}")
                Integer attribute = Utils.dec chunk.take(2).reverse().join()
                Integer type = Utils.dec chunk[2]
                String acc = "---"
                if (isExtended) {
                    String octet = Integer.toBinaryString(Integer.parseInt(chunk[3], 16)).padLeft(8, "0").reverse()
                    //acc = "${octet[0] == "1" ? "r" : "-"}${octet[1] == "1" ? "w" : "-"}${octet[2] == "1" ? "p" : "-"}" // Bit 2 (reportable) is always 1
                    acc = "${octet[0] == "1" ? "r" : "-"}${octet[1] == "1" ? "w" : "-"}-"
                }
                data = data.drop attrInfoBytes

                // Ignore trailing AttributeReportingStatus
                if (attribute == 0xFFFE) continue

                def zclType = ZCL_DATA_TYPES[type]
                if (zclType?.bytes == "var") {
                    varAttributes[attribute] = [chunk[2], acc, manufacturer]
                } else {
                    attributes[attribute] = [chunk[2], acc, manufacturer]
                }
            }

            String framestart = manufacturer == "0000" ? "1043" : "04${Utils.flip manufacturer}43"

            List<String> cmds = []
            if (attributes.size() != 0) {
                attributes.keySet().collate(3).each { attrs ->

                    // Read attribute value (use batches of 3 to reduce mesh traffic)
                    String payload = "${attrs.collect { Utils.payload it }.join()}"
                    cmds += "he raw 0x${device.deviceNetworkId} 0x${Utils.hex endpoint, 2} 0x01 0x${Utils.hex cluster} {${framestart}00 ${payload}}"

                    // If attribute is reportable, also inquire its reporting status
                    attrs.each {
                        String acc = ZCL_CLUSTERS.get(cluster)?.get("attributes")?.get(it)?.get("acc") ?: attributes[it][1]
                        if (acc?.endsWith("p")) {
                            cmds += "he raw 0x${device.deviceNetworkId} 0x${Utils.hex endpoint, 2} 0x01 0x${Utils.hex cluster} {${framestart}08 00${Utils.payload it}}"
                        }
                    }
                }
            }

            // Also process var attributes (one-by-one)
            varAttributes.keySet().each { attr ->

                // Read attribute value (use batches of 3 to reduce mesh traffic)
                cmds += "he raw 0x${device.deviceNetworkId} 0x${Utils.hex endpoint, 2} 0x01 0x${Utils.hex cluster} {${framestart}00 ${Utils.payload attr}}"

                // If attribute is reportable, also inquire its reporting status
                if (ZCL_CLUSTERS.get(cluster)?.get("attributes")?.get(attr)?.get("acc")?.endsWith("p")) {
                    cmds += "he raw 0x${device.deviceNetworkId} 0x${Utils.hex endpoint, 2} 0x01 0x${Utils.hex cluster} {${framestart}08 00${Utils.payload attr}}"
                }
            }

            if (cmds.size() > 0) {
                State.addAttributes endpoint, cluster, attributes
                State.addAttributes endpoint, cluster, varAttributes
                Utils.sendZigbeeCommands delayBetween(cmds, 1000)
            }

            return Utils.processedZigbeeMessage("Discover Attributes Response", "endpoint=0x${Utils.hex endpoint, 2}, cluster=0x${Utils.hex cluster}, attributes=${attributes}, varAttributes=${varAttributes}")

        // DiscoverCommandsReceivedResponse := { 08:Complete?, n*08:CommandIdentifier }
        // Example: [01, 00, 01, 40] -> commands: 0x00, 0x01, 0x40
        case { contains it, [isClusterSpecific:false, commandInt:0x12] }:
            Integer endpoint = Utils.dec msg.sourceEndpoint
            Integer cluster = msg.clusterInt
            String manufacturer = msg.manufacturerId

            List<String> data = msg.data.drop 1
            Map<Integer, String> commands = [:]
            data.each { commands[Utils.dec(it)] = manufacturer }

            State.addInCommands endpoint, cluster, commands
            return Utils.processedZigbeeMessage("Discover Commands Received Response", "endpoint=0x${Utils.hex endpoint, 2}, cluster=0x${Utils.hex cluster}, commands=${commands}")

        // DiscoverCommandsGeneratedResponse := { 08:Complete?, n*08:CommandIdentifier }
        // Example: [01, 00, 01, 40] -> commands: 0x00, 0x01, 0x40
        case { contains it, [isClusterSpecific:false, commandInt:0x14] }:
            Integer endpoint = Utils.dec msg.sourceEndpoint
            Integer cluster = msg.clusterInt
            String manufacturer = msg.manufacturerId

            List<String> data = msg.data.drop 1
            Map<Integer, String> commands = [:]
            data.each { commands[Utils.dec(it)] = manufacturer }

            State.addOutCommands endpoint, cluster, commands
            return Utils.processedZigbeeMessage("Discover Commands Generated Response", "endpoint=0x${Utils.hex endpoint, 2}, cluster=0x${Utils.hex cluster}, commands=${commands}")

        // ===================================================================================================================
        // Zigbee Device Profile (ZDP)
        // ===================================================================================================================

        // Node_Desc_rsp := { 08:Status, 16:NWKAddrOfInterest, 03:LogicalType, 01:ComplexDescriptorAvailable, 01:UserDescriptorAvailable, 03:Reserved, 03:APSFlags, 05:FrequencyBand, 08:MACCapabilityFlags, 16:ManufacturerCode, 08:MaximumBufferSize, 16:MaximumIncomingTransferSize, 16:ServerMask, 16:MaximumOutgoingTransferSize, 08:DescriptorCapabilityDield }
        // Example: [75, 00, 1F, B3, 01, 40, 8E, 7C, 11, 52, 52, 00, 00, 2C, 52, 00, 00]
        case { contains it, [endpointInt:0x00, clusterInt:0x8002] }:
            if (msg.data[1] != "00") {
                return Utils.failedZdpMessage("Node Descriptor Response", msg.data[1], msg)
            }

            List<String> nodeDescriptor = []

            // Logical Type
            String octet = Integer.toBinaryString(Integer.parseInt(msg.data[4], 16)).padLeft(8, "0").reverse()
            String logicalTypeBinary = octet.substring(0, 3).reverse()
            String logicalType = null
            switch (logicalTypeBinary) {
                case "000":
                    logicalType = "Zigbee Coordinator"
                    break
                case "001":
                    logicalType = "Zigbee Router"
                    break
                case "010":
                    logicalType = "Zigbee End Device (ZED)"
                    break
                default:
                    logicalType = "Invalid value: ${logicalTypeBinary}"
            }
            nodeDescriptor += ["Logical Type", logicalType]

            // Complex Descriptor Available & User Descriptor Available
            nodeDescriptor += ["Complex Descriptor Available", octet[3] == "1" ? "Yes" : "No"]
            nodeDescriptor += ["User Descriptor Available", octet[4] == "1" ? "Yes" : "No"]

            // Frequency Band
            octet = Integer.toBinaryString(Integer.parseInt(msg.data[5], 16)).padLeft(8, "0").reverse()
            String frequencyBandBinary = octet.substring(3)
            String frequencyBand = "Invalid value: ${frequencyBandBinary}"
            if (frequencyBandBinary[0] == "1") frequencyBand = "868 - 868.6 MHz"
            if (frequencyBandBinary[1] == "1") frequencyBand = "Reserved"
            if (frequencyBandBinary[2] == "1") frequencyBand = "902 - 928 MHz"
            if (frequencyBandBinary[3] == "1") frequencyBand = "2400 - 2483.5 MHz"
            if (frequencyBandBinary[4] == "1") frequencyBand = "Reserved"
            nodeDescriptor += ["Frequency Band", frequencyBand]

            // MAC Capability Flags
            octet = Integer.toBinaryString(Integer.parseInt(msg.data[6], 16)).padLeft(8, "0").reverse()
            Log.debug "MAC Capability Flags = ${octet}"
            nodeDescriptor += ["Alternate PAN Coordinator", octet[0] == "1" ? "Yes" : "No"]
            nodeDescriptor += ["Device Type", octet[1] == "1" ? "Full Function Device (FFD)" : "Reduced Function Device (RFD)"]
            nodeDescriptor += ["Mains Power Source", octet[2] == "1" ? "Yes" : "No"]
            nodeDescriptor += ["Receiver On When Idle", octet[3] == "1" ? "Yes (always on)" : "No (conserve power during idle periods)"]
            nodeDescriptor += ["Security Capability", octet[6] == "1" ? "Yes" : "No"]
            nodeDescriptor += ["Allocate Address", octet[7] == "1" ? "Yes" : "No"]

            // Manufacturer Code
            String manufacturerCode = msg.data[7..8].reverse().join()
            String manufacturerName = ZBE_MANUFACTURERS[Integer.parseInt(manufacturerCode, 16)]
            nodeDescriptor += ["Manufacturer Code", "0x${manufacturerCode}${manufacturerName ? " = ${manufacturerName}" : ""}"]

            // Maximum Buffer Size Field
            nodeDescriptor += ["Maximum Buffer Size", "${Integer.parseInt(msg.data[9], 16)} bytes"]
            nodeDescriptor += ["Maximum Incoming Transfer Size", "${Integer.parseInt(msg.data[10..11].reverse().join(), 16)} bytes"]

            // Server Mask Field
            octet = Integer.toBinaryString(Integer.parseInt(msg.data[12..13].reverse().join(), 16)).padLeft(16, "0")
            Log.debug "Server Mask Field = ${octet}"
            nodeDescriptor += ["Primary Trust Center", octet[0] == "1" ? "Yes" : "No"]
            nodeDescriptor += ["Backup Trust Center", octet[1] == "1" ? "Yes" : "No"]
            nodeDescriptor += ["Primary Binding Table Cache", octet[2] == "1" ? "Yes" : "No"]
            nodeDescriptor += ["Backup Binding Table Cache", octet[3] == "1" ? "Yes" : "No"]
            nodeDescriptor += ["Primary Discovery Cache", octet[4] == "1" ? "Yes" : "No"]
            nodeDescriptor += ["Backup Discovery Cache", octet[5] == "1" ? "Yes" : "No"]
            nodeDescriptor += ["Network Manager", octet[5] == "1" ? "Yes" : "No"]

            // Maximum Outgoing Transfer Size Field & Descriptor Capability Field
            nodeDescriptor += ["Maximum Outgoing Transfer Size", "${Integer.parseInt(msg.data[14..15].reverse().join(), 16)} bytes"]

            octet = Integer.toBinaryString(Integer.parseInt(msg.data[16], 16)).padLeft(8, "0").reverse()
            nodeDescriptor += ["Extended Active Endpoint List Available", octet[0] == "1" ? "Yes" : "No"]
            nodeDescriptor += ["Extended Simple Descriptor List Available", octet[1] == "1" ? "Yes" : "No"]

            return State.setNodeDescriptor(nodeDescriptor)


        // Power_Desc_rsp := { 08:Status, 16:NWKAddrOfInterest, 04:CurrentPowerMode, 04:AvailablePowerSources, 04:CurrentPowerSource, 04:CurrentPowerSourceLevel }
        // Example: [17, 00, 1F, B3, 10, C1]
        case { contains it, [endpointInt:0x00, clusterInt:0x8003] }:
            if (msg.data[1] != "00") {
                return Utils.failedZdpMessage("Power Descriptor Response", msg.data[1], msg)
            }

            List<String> powerDescriptor = []

            // Current Power Mode
            String octet = Integer.toBinaryString(Integer.parseInt(msg.data[4], 16)).padLeft(8, "0").reverse()
            Log.debug "Power Descriptor octet 1 = ${octet}"
            String currentPowerModeBinary = octet.substring(0, 4).reverse()
            String currentPowerMode = null
            switch (currentPowerModeBinary) {
                case "0000":
                    currentPowerMode = "Same as \"Receiver On When Idle\" from \"Node Descriptor\" section above"
                    break
                case "0001":
                    currentPowerMode = "Receiver comes on periodically as defined by the Power Descriptor"
                    break
                case "0010":
                    currentPowerMode = "Receiver comes on when stimulated, for example, by a user pressing a button"
                    break
                default:
                    currentPowerMode = "Invalid value: ${currentPowerModeBinary}"
            }
            powerDescriptor += ["Current Power Mode", currentPowerMode]

            // Available Power Sources
            String availablePowerSourcesBinary = octet.substring(4)
            List<String> availablePowerSources = []
            if (availablePowerSourcesBinary[0] == "1") availablePowerSources += "Constant (mains) power"
            if (availablePowerSourcesBinary[1] == "1") availablePowerSources += "Rechargeable battery"
            if (availablePowerSourcesBinary[2] == "1") availablePowerSources += "Disposable battery"
            if (availablePowerSourcesBinary[3] == "1") availablePowerSources += "Reserved"
            powerDescriptor += ["Available Power Sources", availablePowerSources.toString()]

            // Current Power Sources
            octet = Integer.toBinaryString(Integer.parseInt(msg.data[5], 16)).padLeft(8, "0").reverse()
            Log.debug "Power Descriptor octet 2 = ${octet}"
            String currentPowerSourcesBinary = octet.substring(0, 4)
            List<String> currentPowerSources = []
            if (currentPowerSourcesBinary[0] == "1") currentPowerSources += "Constant (mains) power"
            if (currentPowerSourcesBinary[1] == "1") currentPowerSources += "Rechargeable battery"
            if (currentPowerSourcesBinary[2] == "1") currentPowerSources += "Disposable battery"
            if (currentPowerSourcesBinary[3] == "1") currentPowerSources += "Reserved"
            powerDescriptor += ["Current Power Sources", currentPowerSources.toString()]

            // Current Power Source Level
            String currentPowerSourceLevelBinary = octet.substring(4).reverse()
            String currentPowerSourceLevel = null
            switch (currentPowerSourceLevelBinary) {
                case "0000":
                    currentPowerSourceLevel = "Critical"
                    break
                case "0100":
                    currentPowerSourceLevel = "33%"
                    break
                case "1000":
                    currentPowerSourceLevel = "66%"
                    break
                case "1100":
                    currentPowerSourceLevel = "100%"
                    break
                default:
                    currentPowerSourceLevel = "Reserved value: ${currentPowerSourceLevelBinary}"
            }
            powerDescriptor += ["Current Power Source Level", currentPowerSourceLevel]

            return State.setPowerDescriptor(powerDescriptor)


        // Simple_Desc_rsp := { 08:Status, 16:NWKAddrOfInterest, 08:Length, 08:Endpoint, 16:ApplicationProfileIdentifier, 16:ApplicationDeviceIdentifier, 08:Reserved, 16:InClusterCount, n*16:InClusterList, 16:OutClusterCount, n*16:OutClusterList }
        // Example: [B7, 00, 18, 4A, 14, 03, 04, 01, 06, 00, 01, 03, 00,  00, 03, 00, 80, FC, 03, 03, 00, 04, 00, 80, FC] -> endpointId=03, inClusters=[0000, 0003, FC80], outClusters=[0003, 0004, FC80]
        case { contains it, [endpointInt:0x00, clusterInt:0x8004] }:
            if (msg.data[1] != "00") {
                return Utils.failedZclMessage("Simple Descriptor Response", msg.data[1], msg)
            }

            String manufacturer = Utils.flip state.ka_manufacturer

            Integer endpoint = Utils.dec msg.data[5]
            Integer count = Utils.dec msg.data[11]
            Integer position = 12
            Integer positionCounter = null
            Set<Integer> inClusters = []
            List<String> cmds = []
            if (count > 0) {
                (1..count).each() { b->
                    positionCounter = position + ((b - 1) * 2)
                    Integer cluster = Utils.dec msg.data[positionCounter..positionCounter+1].reverse().join()
                    inClusters += cluster

                    // Discover cluster attributes
                    cmds += "he raw 0x${device.deviceNetworkId} 0x${Utils.hex endpoint, 2} 0x01 0x${Utils.hex cluster} {104315 0000 FF}"
                    if (manufacturer != "0000") {
                        cmds += "he raw 0x${device.deviceNetworkId} 0x${Utils.hex endpoint, 2} 0x01 0x${Utils.hex cluster} {04${manufacturer}4315 0000 FF}"
                    }

                    //cmds += "he raw 0x${device.deviceNetworkId} 0x${Utils.hex endpoint, 2} 0x01 0x${Utils.hex cluster} {10430C 0000 FF}"
                    //cmds += "he raw 0x${device.deviceNetworkId} 0x${Utils.hex endpoint, 2} 0x01 0x${Utils.hex cluster} {04FFFF430C 0000 FF}"

                    // Discover cluster received commands
                    cmds += "he raw 0x${device.deviceNetworkId} 0x${Utils.hex endpoint, 2} 0x01 0x${Utils.hex cluster} {104311 00 FF}"
                    if (manufacturer != "0000") {
                        cmds += "he raw 0x${device.deviceNetworkId} 0x${Utils.hex endpoint, 2} 0x01 0x${Utils.hex cluster} {04${manufacturer}4311 00 FF}"
                    }
                }
                State.addInClusters endpoint, inClusters
            }

            position += count * 2
            count = Utils.dec msg.data[position]
            position += 1
            Set<Integer> outClusters = []
            if (count > 0) {
                (1..count).each() { b->
                    positionCounter = position + ((b - 1) * 2)
                    Integer cluster = Utils.dec msg.data[positionCounter..positionCounter+1].reverse().join()
                    outClusters += cluster

                    // Discover cluster generated commands
                    cmds += "he raw 0x${device.deviceNetworkId} 0x${Utils.hex endpoint, 2} 0x01 0x${Utils.hex cluster} {104313 00 FF}"
                    if (manufacturer != "0000") {
                        cmds += "he raw 0x${device.deviceNetworkId} 0x${Utils.hex endpoint, 2} 0x01 0x${Utils.hex cluster} {04${manufacturer}4313 00 FF}"
                    }
                }
                State.addOutClusters endpoint, outClusters
            }

            if (cmds.size != 0) Utils.sendZigbeeCommands delayBetween(cmds, 1000)
            return Utils.processedZigbeeMessage("Simple Descriptor Response", "endpoint=0x${Utils.hex endpoint, 2}, inClusters=${Utils.hexs inClusters}, outClusters=${Utils.hexs outClusters}")


        // Active_EP_rsp := { 08:Status, 16:NWKAddrOfInterest, 08:ActiveEPCount, n*08:ActiveEPList }
        // Three endpoints example: [83, 00, 18, 4A, 03, 01, 02, 03] -> endpointIds=[01, 02, 03]
        case { contains it, [endpointInt:0x00, clusterInt:0x8005] }:
            if (msg.data[1] != "00") {
                return Utils.failedZdpMessage("Active Endpoints Response", msg.data[1], msg)
            }

            Set<Integer> endpoints = []
            Integer count = Utils.dec msg.data[4]
            List<String> cmds = []
            if (count > 0) {
                (1..count).each() { i ->
                    String endpointStr = msg.data[4 + i]
                    Integer endpoint = Utils.dec endpointStr
                    endpoints += endpoint

                    // Query simple descriptor data
                    cmds += "he raw 0x${device.deviceNetworkId} 0x00 0x00 0x0004 {00 ${zigbee.swapOctets(device.deviceNetworkId)} ${endpointStr}} {0x0000}"
                }
                State.addEndpoints endpoints
                Utils.sendZigbeeCommands delayBetween(cmds, 1000)
                return Utils.processedZigbeeMessage("Active Endpoints Response", "endpoints=${Utils.hexs endpoints, 2}")
            }

        // Bind_rsp := { 08:Status }
        case { contains it, [endpointInt:0x00, clusterInt:0x8021] }:
            if (msg.data[1] != "00") {
                return Utils.failedZdpMessage("Bind Response", msg.data[1], msg)
            }

            return Utils.processedZigbeeMessage("Bind Response", "data=${msg.data}")

        // Unbind_rsp := { 08:Status }
        case { contains it, [endpointInt:0x00, clusterInt:0x8022] }:
            if (msg.data[1] != "00") {
                return Utils.failedZdpMessage("Unbind Response", msg.data[1], msg)
            }

            return Utils.processedZigbeeMessage("Unbind Response", "data=${msg.data}")

        // Mgmt_Lqi_rsp := { 08:Status, 08:NeighborTableEntriesTotal, 08:StartIndex, 08:NeighborTableEntriesIncluded, 176*n:NeighborTableList }
        // NeighborTableList: { 64:ExtendedPANId, 64:IEEEAddress, 16:NetworkAddress, 02:DeviceType, 02:RxOnWhenIdle, 03:Relationship, 01:Reserved, 02:PermitJoining, 06:Reserved, 08:Depth, 08:LQI }
        // Example: [46, 00, 0C, 00, 03, 50, 53, 3A, 0D, 00, DF, 66, 15, E9, A6, C9, 17, 00, 6F, 0D, 00, 00, 00, 04, 02, 00, A3, 50, 53, 3A, 0D, 00, DF, 66, 15, 80, BF, CA, 6B, 6A, 38, C1, A4, 4A, 16, 25, 02, 0F, B8, 50, 53, 3A, 0D, 00, DF, 66, 15, D3, FA, E1, 25, 00, 4B, 12, 00, 64, 17, 25, 02, 0F, 6A]
        case { contains it, [endpointInt:0x00, clusterInt:0x8031] }:
            if (msg.data[1] != "00") {
                return Utils.failedZdpMessage("Neighbors Table Response", msg.data[1], msg)
            }

            Integer totalEntries = Integer.parseInt(msg.data[2], 16)
            Integer includedEntries = Integer.parseInt(msg.data[4], 16)

            List<List<String>> neighbors = []
            Integer pos = 5
            (0..(includedEntries - 1)).each {
                List<String> neighbor = []

                // ExtendedPANId, IEEEAddress, NetworkAddress
                //neighbor += ["PAN Id", msg.data[pos..(pos + 8)].reverse().join()]
                //neighbor += ["IEEE", msg.data[(pos + 8)..(pos + 16)].reverse().join()]
                neighbor += ["Addr", msg.data[(pos + 16)..(pos + 17)].reverse().join()]

                // DeviceType, RxOnWhenIdle, Relationship, Reserved
                String octet = Integer.toBinaryString(Integer.parseInt(msg.data[pos + 18], 16)).padLeft(8, "0").reverse()
                //Log.warn ("Mgmt_Lqi_rsp: octet #1: ${octet}")

                String deviceTypeBinary = octet.substring(0, 2).reverse()
                String deviceType = "Unknown"
                switch (deviceTypeBinary) {
                    case "00":
                        deviceType = "Zigbee Coordinator"
                        break
                    case "01":
                        deviceType = "Zigbee Router"
                        break
                    case "10":
                        deviceType = "Zigbee End-Device"
                        break
                }
                neighbor += ["Type", deviceType]

                String rxOnWhenIdleBinary = octet.substring(2, 4).reverse()
                String rxOnWhenIdle = "Unknown"
                switch (rxOnWhenIdleBinary) {
                    case "00":
                        rxOnWhenIdle = "No"
                        break
                    case "01":
                        rxOnWhenIdle = "Yes"
                        break
                }
                neighbor += ["RxOnWhenIdle", rxOnWhenIdle]

                String relationshipBinary = octet.substring(4, 7).reverse()
                String relationship = "Unknown"
                switch (relationshipBinary) {
                    case "000":
                        relationship = "Parent"
                        break
                    case "001":
                        relationship = "Child"
                        break
                    case "010":
                        relationship = "Sibling"
                        break
                    case "011":
                        relationship = "Unknown"
                        break
                    case "100":
                        relationship = "Previous Child"
                        break
                }
                neighbor += ["Rel", relationship]

                // PermitJoining, Reserved
                octet = Integer.toBinaryString(Integer.parseInt(msg.data[pos + 19], 16)).padLeft(8, "0").reverse()
                //Log.warn ("Mgmt_Lqi_rsp: octet #2: ${octet}")

                String permitJoiningBinary = octet.substring(0, 2).reverse()
                String permitJoining = "Invalid"
                switch (permitJoiningBinary) {
                    case "00":
                        permitJoining = "No"
                        break
                    case "01":
                        permitJoining = "Yes"
                        break
                    case "10":
                        permitJoining = "Unknown"
                        break
                }
                //neighbor += ["Permit Joining", permitJoining]

                // Depth, LQI
                neighbor += ["Depth", Integer.parseInt(msg.data[pos + 20], 16)]
                neighbor += ["LQI", Integer.parseInt(msg.data[pos + 21], 16)]

                neighbors.add neighbor
                pos += 22
            }

            State.addNeighbors neighbors
            return Utils.processedZigbeeMessage("Neighbors Table Response", "totalEntries=${totalEntries}, includedEntries=${includedEntries}, neighbors=${neighbors}")


        // Mgmt_Rtg_rsp := { 08:Status, 08:RoutingTableEntriesTotal, 08:StartIndex, 08:RoutingTableEntriesIncluded, 40*n:RoutingTableList }
        // RoutingTableList: { 16:DestinationAddress, 03:RouteStatus, 01:MemoryConstrained, 01:ManyToOne, 01:RouteRecordRequired, 02:Reserved, 16:NextHopAddress }
        // Example: [24, 00, 0A, 00, 0A, 00, 00, 30, 00, 00, 00, 00, 03, 00, 00, ED, EE, 00, 8B, 72, 31, 98, 00, 8B, 72, AD, 56, 00, AD, 56, 58, 1E, 00, 64, 17, 4A, 16, 00, 4A, 16, 64, 17, 00, 64, 17, 3F, 5F, 00, 8B, 72, 9F, 9B, 00, 8B, 72
        case { contains it, [endpointInt:0x00, clusterInt:0x8032] }:
            if (msg.data[1] != "00") {
                return Utils.failedZdpMessage("Routing Table Response", msg.data[1], msg)
            }

            Integer totalEntries = Integer.parseInt(msg.data[2], 16)
            Integer includedEntries = Integer.parseInt(msg.data[4], 16)

            List<List<String>> routes = []
            Integer pos = 5
            (0..(includedEntries - 1)).each {
                List<String> route = []

                // DestinationAddress
                route += ["Destination", msg.data[pos..(pos + 1)].reverse().join()]

                // NextHopAddress
                route += ["Next Hop", msg.data[(pos + 3)..(pos + 4)].reverse().join()]

                // RouteStatus
                String octet = Integer.toBinaryString(Integer.parseInt(msg.data[pos + 2], 16)).padLeft(8, "0").reverse()
                //Log.warn ("Octet: ${octet}")

                String routeStatusBinary = octet.substring(0, 3).reverse()
                String routeStatus = "Reserved  "
                switch (routeStatusBinary) {
                    case "000":
                        routeStatus = "Active"
                        break
                    case "001":
                        routeStatus = "Discovery underway"
                        break
                    case "010":
                        routeStatus = "Discovery failed"
                        break
                    case "011":
                        routeStatus = "Inactive"
                        break
                    case "100":
                        routeStatus = "Validation underway"
                        break
                }
                route += ["Route Status", routeStatus]

                // MemoryConstrained, ManyToOne, RouteRecordRequired
                //route += ["Memory Constrained", octet[3] == "1" ? "Yes" : "No"]
                //route += ["Many To One", octet[4] == "1" ? "Yes" : "No"]
                //route += ["Route Record Required", octet[5] == "1" ? "Yes" : "No"]

                routes.add route
                pos += 5
            }

            State.addRoutes routes
            return Utils.processedZigbeeMessage("Routing Table Response", "totalEntries=${totalEntries}, includedEntries=${includedEntries}, routes=${routes}")


        // Mgmt_Bind_rsp := { 08:Status, 08:BindingTableEntriesTotal, 08:StartIndex, 08:BindingTableEntriesIncluded, 112/168*n:BindingTableList }
        // BindingTableList: { 64:SrcAddr, 08:SrcEndpoint, 16:ClusterId, 08:DstAddrMode, 16/64:DstAddr, 0/08:DstEndpoint }
        // Example: [71, 00, 01, 00, 01,  C6, 9C, FE, FE, FF, F9, E3, B4,  01,  06, 00,  03,  E9, A6, C9, 17, 00, 6F, 0D, 00,  01]
        case { contains it, [endpointInt:0x00, clusterInt:0x8033] }:
            if (msg.data[1] != "00") {
                return Utils.failedZdpMessage("Binding Table Response", msg.data[1], msg)
            }

            Integer totalEntries = Integer.parseInt(msg.data[2], 16)
            Integer includedEntries = Integer.parseInt(msg.data[4], 16)

            List<List<String>> bindings = []
            Integer pos = 5
            (0..(includedEntries - 1)).each { idx ->
                List<String> binding = []

                // SrcAddr, SrcEndpoint, ClusterId
                binding += ["Src", msg.data[(pos)..(pos + 7)].reverse().join()]
                binding += ["Endpoint", "0x" + msg.data[pos + 8]]
                binding += ["Cluster", "0x" + msg.data[(pos + 9)..(pos + 10)].reverse().join()]

                // DstAddrMode
                String dstAddrMode = msg.data[pos + 11]
                if (dstAddrMode != "01" && dstAddrMode != "03") return

                // 16 bit DstAddr
                if (dstAddrMode == "01") {
                    binding += ["Dest", msg.data[(pos + 12)..(pos + 13)].reverse().join()]
                    pos += 14
                } else {
                    binding += ["Dest", msg.data[(pos + 12)..(pos + 19)].reverse().join()]
                    binding += ["Endpoint", "0x" + msg.data[pos + 20]]
                    pos += 21
                }

                bindings.add binding
            }

            State.addBindings bindings
            return Utils.processedZigbeeMessage("Binding Table Response", "totalEntries=${totalEntries}, includedEntries=${includedEntries}, bindings=${bindings}")

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
    debug: { message -> log.debug "${device.displayName} ${message.uncapitalize()}" },
    info:  { message -> log.info  "${device.displayName} ${message.uncapitalize()}" },
    warn:  { message -> log.warn  "${device.displayName} ${message.uncapitalize()}" },
    error: { message -> log.error "${device.displayName} ${message.uncapitalize()}" },
]

// ===================================================================================================================
// Helper methods (keep them simple, keep them dumb)
// ===================================================================================================================

@Field def Utils = [
    dec: { String value -> Integer.parseInt(value, 16) },
    hex: { Integer value, Integer chars = 4 -> "${zigbee.convertToHexString value, chars}" },
    hexs: { Collection<Integer> values, Integer chars = 4 -> values.collect { "0x${zigbee.convertToHexString it, chars}" } },
    payload: { Integer value -> zigbee.swapOctets(zigbee.convertToHexString(value, 4)) },
    flip: { String value -> (value.replaceAll(" ", "").split("") as List).collate(2).collect { it.join() }.reverse().join() },

    sendZigbeeCommands: { List<String> cmds ->
        if (cmds.size() == 0) return
        List<String> send = delayBetween(cmds.findAll { !it.startsWith("delay") }, 1000)
        Log.debug "◀ Sending Zigbee messages: ${send}"
        sendHubCommand new hubitat.device.HubMultiAction(send, hubitat.device.Protocol.ZIGBEE)
    },

    sendEvent: { Map event ->
        Log.info "${event.descriptionText} [${event.type}]"
        sendEvent event
    },

    dataValue: { String key, String value ->
        Log.debug "Update driver data value: ${key}=${value}"
        updateDataValue key, value
    },

    processedZigbeeMessage: { String type, String details ->
        Log.debug "▶ Processed Zigbee message: type=${type}, status=SUCCESS, ${details}"
    },

    ignoredZigbeeMessage: { String type, Map msg ->
        Log.debug "▶ Ignored Zigbee message: type=${type}, status=SUCCESS, data=${msg.data}"
    },

    failedZclMessage: { String type, String status, Map msg ->
        Log.warn "▶ Received ZCL message: type=${type}, status=${ZCL_STATUS[Integer.parseInt(status, 16)]}, data=${msg.data}"
    },

    failedZdpMessage: { String type, String status, Map msg ->
        Log.warn "▶ Received ZDP message: type=${type}, status=${ZDP_STATUS[Integer.parseInt(status, 16)]}, data=${msg.data}"
    }
]

// switch/case syntactic sugar
private boolean contains(Map msg, Map spec) {
    msg.keySet().containsAll(spec.keySet()) && spec.every { it.value == msg[it.key] }
}

// ===================================================================================================================
// State helpers
// ===================================================================================================================

@Field Map<String, Closure> State = [
    setManufacturer: { String manufacturer ->
        state.ka_manufacturer = manufacturer
    },

    addEndpoints: { Set<Integer> endpoints ->
        state.ka_endpoints = endpoints
    },

    addInClusters: { Integer endpoint, Collection<Integer> clusters ->
        state["ka_inClusters_${endpoint}"] = clusters
    },

    addOutClusters: { Integer endpoint, Collection<Integer> clusters ->
        state["ka_outClusters_${endpoint}"] = clusters
    },

    addAttributes: { Integer endpoint, Integer cluster, Map<Integer, Integer> attributes ->
        attributes.each {
            state["ka_attribute_${endpoint}_${cluster}_${it.key}"] = it.value
        }
    },

    addInCommands: { Integer endpoint, Integer cluster, Map<Integer, String> commands ->
        commands.each {
            state["ka_inCommand_${endpoint}_${cluster}_${it.key}"] = it.value
        }
    },

    addOutCommands: { Integer endpoint, Integer cluster, Map<Integer, String> commands ->
        commands.each {
            state["ka_outCommand_${endpoint}_${cluster}_${it.key}"] = it.value
        }
    },

    addAttributesValues: { Integer endpoint, Integer cluster, Map<Integer, Map<String, String>> attributesValues ->
        attributesValues.each {
            state["ka_attributeValue_${endpoint}_${cluster}_${it.key}"] = it.value
        }
    },
    
    addAttributeReporting: { Integer endpoint, Integer cluster, Integer attribute, Integer minPeriod, Integer maxPeriod ->
        state["ka_attributeReporting_${endpoint}_${cluster}_${attribute}"] = [ min:minPeriod, max:maxPeriod ]
    },

    setNodeDescriptor: { List<String> nodeDescriptor ->
        state["ka_nodeDescriptor"] = nodeDescriptor
    },

    setPowerDescriptor: { List<String> powerDescriptor ->
        state["ka_powerDescriptor"] = powerDescriptor
    },

    addNeighbors: { List<List<String>> neighbors ->
        state["ka_neighbors"] = neighbors
    },

    addRoutes: { List<List<String>> routes ->
        state["ka_routes"] = routes
    },

    addBindings: { List<List<String>> bindings ->
        state["ka_bindings"] = bindings
    }
]

// ===================================================================================================================
// Constants
// ===================================================================================================================

@Field static final Map<Integer, String> ZDP_STATUS = [
    0x00: "SUCCESS",
    0x80: "INV_REQUESTTYPE",
    0x81: "DEVICE_NOT_FOUND",
    0x82: "INVALID_EP",
    0x83: "NOT_ACTIVE",
    0x84: "NOT_SUPPORTED",
    0x85: "TIMEOUT",
    0x86: "NO_MATCH",
    0x88: "NO_ENTRY",
    0x89: "NO_DESCRIPTOR",
    0x8A: "INSUFFICIENT_SPACE",
    0x8B: "NOT_PERMITTED",
    0x8C: "TABLE_FULL",
    0x8D: "NOT_AUTHORIZED",
    0x8E: "DEVICE_BINDING_TABLE_FULL"
]

@Field static final Map<Integer, String> ZCL_STATUS = [
    0x00: "SUCCESS",
    0x01: "FAILURE",
    0x7E: "NOT_AUTHORIZED",
    0x7F: "RESERVED_FIELD_NOT_ZERO",
    0x80: "MALFORMED_COMMAND",
    0x81: "UNSUP_CLUSTER_COMMAND",
    0x82: "UNSUP_GENERAL_COMMAND",
    0x83: "UNSUP_MANUF_CLUSTER_COMMAND",
    0x84: "UNSUP_MANUF_GENERAL_COMMAND",
    0x85: "INVALID_FIELD",
    0x86: "UNSUPPORTED_ATTRIBUTE",
    0x87: "INVALID_VALUE",
    0x88: "READ_ONLY",
    0x89: "INSUFFICIENT_SPACE",
    0x8A: "DUPLICATE_EXISTS",
    0x8B: "NOT_FOUND",
    0x8C: "UNREPORTABLE_ATTRIBUTE",
    0x8D: "INVALID_DATA_TYPE",
    0x8E: "INVALID_SELECTOR",
    0x8F: "WRITE_ONLY",
    0x90: "INCONSISTENT_STARTUP_STATE",
    0x91: "DEFINED_OUT_OF_BAND",
    0x92: "INCONSISTENT",
    0x93: "ACTION_DENIED",
    0x94: "TIMEOUT",
    0x95: "ABORT",
    0x96: "INVALID_IMAGE",
    0x97: "WAIT_FOR_DATA",
    0x98: "NO_IMAGE_AVAILABLE",
    0x99: "REQUIRE_MORE_IMAGE",
    0x9A: "NOTIFICATION_PENDING",
    0xC0: "HARDWARE_FAILURE",
    0xC1: "SOFTWARE_FAILURE",
    0xC2: "CALIBRATION_ERROR",
    0xC3: "UNSUPPORTED_CLUSTER"
]

@Field static final Map<Integer, Map<String, String>> ZCL_DATA_TYPES = [
    0x00: [name:"nodata",    bytes:"0"],
    0x08: [name:"data8",     bytes:"1"],
    0x09: [name:"data16",    bytes:"2"],
    0x0a: [name:"data24",    bytes:"3"],
    0x0b: [name:"data32",    bytes:"4"],
    0x0c: [name:"data40",    bytes:"5"],
    0x0d: [name:"data48",    bytes:"6"],
    0x0e: [name:"data56",    bytes:"7"],
    0x0f: [name:"data64",    bytes:"8"],
    0x10: [name:"bool",      bytes:"1", decorate: { value -> "${value == "00" ? "False" : (value == "01" ? "True" : "Invalid value")}" }],
    0x18: [name:"map8",      bytes:"1"],
    0x19: [name:"map16",     bytes:"2"],
    0x1a: [name:"map24",     bytes:"3"],
    0x1b: [name:"map32",     bytes:"4"],
    0x1c: [name:"map40",     bytes:"5"],
    0x1d: [name:"map48",     bytes:"6"],
    0x1e: [name:"map56",     bytes:"7"],
    0x1f: [name:"map64",     bytes:"8"],
    0x20: [name:"uint8",     bytes:"1"],
    0x21: [name:"uint16",    bytes:"2"],
    0x22: [name:"uint24",    bytes:"3"],
    0x23: [name:"uint32",    bytes:"4"],
    0x24: [name:"uint40",    bytes:"5"],
    0x25: [name:"uint48",    bytes:"6"],
    0x26: [name:"uint56",    bytes:"7"],
    0x27: [name:"uint64",    bytes:"8"],
    0x28: [name:"int8",      bytes:"1"],
    0x29: [name:"int16",     bytes:"2"],
    0x2a: [name:"int24",     bytes:"3"],
    0x2b: [name:"int32",     bytes:"4"],
    0x2c: [name:"int40",     bytes:"5"],
    0x2d: [name:"int48",     bytes:"6"],
    0x2e: [name:"int56",     bytes:"7"],
    0x2f: [name:"int64",     bytes:"8"],
    0x30: [name:"enum8",     bytes:"1"],
    0x31: [name:"enum16",    bytes:"2"],
    0x38: [name:"semi",      bytes:"2"],
    0x39: [name:"single",    bytes:"4"],
    0x3a: [name:"double",    bytes:"8"],
    0x41: [name:"octstr",    bytes:"var", decorate: { value -> "${!value ? "" : (value.split("") as List).collate(2).collect { "${Integer.parseInt(it.join(), 16) as char}" }.join()}" }],
    0x42: [name:"string",    bytes:"var"],
    0x43: [name:"octstr16",  bytes:"var"],
    0x44: [name:"string16",  bytes:"var"],
    0x48: [name:"array",     bytes:"var"],
    0x4c: [name:"struct",    bytes:"var"],
    0x50: [name:"set",       bytes:"var"],
    0x51: [name:"bag",       bytes:"var"],
    0xe0: [name:"ToD",       bytes:"4"],
    0xe1: [name:"date",      bytes:"4"],
    0xe2: [name:"UTC",       bytes:"4"],
    0xe8: [name:"clusterId", bytes:"2"],
    0xe9: [name:"attribId",  bytes:"2"],
    0xea: [name:"bacOID",    bytes:"4"],
    0xf0: [name:"EUI64",     bytes:"8"],
    0xf1: [name:"key128",    bytes:"16"],
    0xff: [name:"unknown",   bytes:"0"],
]

// @see https://github.com/wireshark/wireshark/blob/master/epan/dissectors/packet-zbee.h
@Field static final Map<Integer, String> ZBE_MANUFACTURERS = [
    0x0000: "NONE",
    0x1002: "EMBER",
    0x100B: "PHILIPS",
    0x1011: "VISONIC",
    0x1014: "ATMEL",
    0x1015: "DEVELCO",
    0x101D: "YALE",
    0x101E: "MAXSTREAM",
    0x1021: "VANTAGE",
    0x1021: "LEGRAND",
    0x102E: "LGE",
    0x1037: "JENNIC",
    0x1039: "ALERTME",
    0x104E: "CLS",
    0x104E: "CENTRALITE",
    0x1049: "SI_LABS",
    0x105E: "SCHNEIDER",
    0x1071: "4_NOKS",
    0x1071: "BITRON",
    0x1078: "COMPUTIME",
    0x10EF: "XFINITY",
    0x1262: "AXIS",
    0x1092: "KWIKSET",
    0x109a: "MMB",
    0x109F: "NETVOX",
    0x10B9: "NYCE",
    0x10EF: "UNIVERSAL2",
    0x10F2: "UBISYS",
    0x1337: "DATEK_WIRLESS",
    0x115C: "DANALOCK",
    0x1236: "SCHLAGE",
    0x1105: "BEGA",
    0x110A: "PHYSICAL",
    0x110C: "OSRAM",
    0x1110: "PROFALUX",
    0x1112: "EMBERTEC",
    0x1124: "JASCO",
    0x112E: "BUSCH_JAEGER",
    0x1131: "SERCOMM",
    0x1133: "BOSCH",
    0x1135: "DDEL",
    0x113B: "WAXMAN",
    0x113C: "OWON",
    0x1141: "TUYA",
    0x1144: "LUTRON",
    0x1155: "BOSCH2",
    0x1158: "ZEN",
    0x115B: "KEEN_HOME",
    0x115F: "XIAOMI",
    0x1160: "SENGLED_OPTOELEC",
    0x1166: "INNR",
    0x1168: "LDS",
    0x1172: "PLUGWISE_BV",
    0x1175: "D_LINK",
    0x117A: "INSTA",
    0x117C: "IKEA",
    0x117E: "3A_SMART_HOME",
    0x1185: "STELPRO",
    0x1189: "LEDVANCE",
    0x119C: "SINOPE",
    0x119D: "JIUZHOU",
    0x119D: "PAULMANN",
    0x1209: "BOSCH3",
    0x120B: "HEIMAN",
    0x1214: "CHINA_FIRE_SEC",
    0x121B: "MUELLER",
    0x121C: "AURORA",
    0x1224: "SUNRICHER",
    0x1228: "XIAOYAN",
    0x122A: "XAL",
    0x122D: "ADUROLIGHT",
    0x1233: "THIRD_REALITY",
    0x1234: "DSR",
    0x123B: "HANGZHOU_IMAGIC",
    0x1241: "SAMJIN",
    0x1246: "DANFOSS",
    0x125F: "NIKO_NV",
    0x1268: "KONKE",
    0x126A: "SHYUGJ_TECHNOLOGY",
    0x126E: "XIAOMI2",
    0x1277: "ADEO",
    0x1286: "SHENZHEN_COOLKIT",
    0x1337: "DATEK",
    0xBBAA: "OSRAM_STACK",
    0xC2DF: "C2DF",
    0xFFA0: "PHILIO"
]

// https://github.com/dresden-elektronik/deconz-rest-plugin/blob/master/general.xml
@Field static final def ZCL_CLUSTERS = [
    0x0000: [
        name: "Basic Cluster",
        attributes: [
            0x0000: [ type:0x20, req:"req", acc:"r--", name:"ZCL Version" ],
            0x0001: [ type:0x20, req:"opt", acc:"r--", name:"Application Version" ],
            0x0002: [ type:0x20, req:"opt", acc:"r--", name:"Stack Version" ],
            0x0003: [ type:0x20, req:"opt", acc:"r--", name:"HW Version" ],
            0x0004: [ type:0x42, req:"opt", acc:"r--", name:"Manufacturer Name" ],
            0x0005: [ type:0x42, req:"opt", acc:"r--", name:"Model Identifier" ],
            0x0006: [ type:0x42, req:"req", acc:"r--", name:"Date Code" ],
            0x0007: [ type:0x30, req:"opt", acc:"r--", name:"Power Source", constraints: [
                0x00: "Unknown",
                0x01: "Mains (single phase)",
                0x02: "Mains (3 phase)",
                0x03: "Battery",
                0x04: "DC source",
                0x05: "Emergency mains constantly powered",
                0x06: "Emergency mains and transfer switch"
            ]],
            0x0008: [ type:0x30, req:"opt", acc:"r--", name:"Generic Device Class" ],
            0x0009: [ type:0x30, req:"opt", acc:"r--", name:"Generic Device Type", constraints: [
                0x00: "Incandescent",
                0x01: "Spotlight Halogen",
                0x02: "Halogen Bulb",
                0x03: "CFL",
                0x04: "Linear Fluorescent",
                0x05: "LED Bulb",
                0x06: "Spotlight LED",
                0x07: "LED Strip",
                0x08: "LED Tube",
                0x09: "Generic Indoor Luminaire",
                0x0A: "Generic Outdoor Luminaire",
                0x0B: "Pendant Luminaire",
                0x0C: "Floor Standing Luminaire",
                0xE0: "Generic Controller",
                0xE1: "Wall Switch",
                0xE2: "Portable Remote Controller",
                0xE3: "Motion Sensor / Light Sensor",
                0xF0: "Generic Actuator",
                0xF1: "Wall Socket",
                0xF2: "Gateway / Bridge",
                0xF3: "Plug-in Unit",
                0xF4: "Retrofit Actuator",
                0xFF: "Unspecified"
            ]],
            0x000A: [ type:0x41, req:"opt", acc:"r--", name:"Product Code" ],
            0x000B: [ type:0x42, req:"opt", acc:"r--", name:"Product URL" ],
            0x000C: [ type:0x42, req:"opt", acc:"r--", name:"Manufacturer Version Details" ],
            0x000D: [ type:0x42, req:"opt", acc:"r--", name:"Serial Number" ],
            0x000E: [ type:0x42, req:"opt", acc:"r--", name:"Product Label" ],
            0x0010: [ type:0x42, req:"opt", acc:"rw-", name:"Location Description" ],
            0x0011: [ type:0x30, req:"opt", acc:"rw-", name:"Physical Environment" ],
            0x0012: [ type:0x10, req:"opt", acc:"rw-", name:"Device Enabled", constraints: [
                0x00: "Disabled",
                0x01: "Enabled"
            ]],
            0x0013: [ type:0x18, req:"opt", acc:"rw-", name:"Alarm Mask" ],
            0x0014: [ type:0x18, req:"opt", acc:"rw-", name:"Disable Local Config" ],
            0x4000: [ type:0x42, req:"opt", acc:"r--", name:"SW Build ID" ]
        ],
        commands: [
            0x00: [ req:"opt", name:"Reset to Factory Defaults" ]
        ]
    ],
    0x0001: [
        name: "Power Configuration Cluster",
        attributes: [
            0x0000: [ type:0x21, req:"opt", acc:"r--", name:"Mains Voltage" ],
            0x0001: [ type:0x20, req:"opt", acc:"r--", name:"Mains Frequency" ],
            
            0x0010: [ type:0x18, req:"opt", acc:"rw-", name:"Mains Alarm Mask" ],
            0x0011: [ type:0x21, req:"opt", acc:"rw-", name:"Mains Voltage Min Threshold" ],
            0x0012: [ type:0x21, req:"opt", acc:"rw-", name:"Mains Voltage Max Threshold" ],
            0x0013: [ type:0x21, req:"opt", acc:"rw-", name:"Mains Voltage Dwell Trip Point" ],
            
            0x0020: [ type:0x20, req:"opt", acc:"r--", name:"Battery Voltage", decorate: { value -> value == "00" ? "" : "${Integer.parseInt(value, 16) * 100}mV" } ],
            0x0021: [ type:0x20, req:"opt", acc:"r-p", name:"Battery Percentage Remaining", decorate: { value -> "${Math.round(Integer.parseInt(value, 16) / 2) as Integer}% remaining" } ],

            0x0030: [ type:0x42, req:"opt", acc:"rw-", name:"Battery Manufacturer" ],
            0x0031: [ type:0x30, req:"opt", acc:"rw-", name:"Battery Size", constraints: [
                0x00: "No battery",
                0x01: "Built in",
                0x02: "Other",
                0x03: "AA",
                0x04: "AAA",
                0x05: "C",
                0x06: "D",
                0x07: "CR2 (IEC: CR17355 / ANSI: 5046LC)",
                0x08: "CR123A (IEC: CR17345 / ANSI: 5018LC",
                0xFF: "Battery"
            ]],
            0x0032: [ type:0x21, req:"opt", acc:"rw-", name:"Battery AH Rating" ],
            0x0033: [ type:0x20, req:"opt", acc:"rw-", name:"Battery Quantity" ],
            0x0034: [ type:0x20, req:"opt", acc:"rw-", name:"Battery Rated Voltage" ],
            0x0035: [ type:0x18, req:"opt", acc:"rw-", name:"Battery Alarm Mask" ],
            0x0036: [ type:0x20, req:"opt", acc:"rw-", name:"Battery Voltage Min Threshold" ],
            0x0037: [ type:0x20, req:"opt", acc:"rw-", name:"Battery Voltage Threshold 1" ],
            0x0038: [ type:0x20, req:"opt", acc:"rw-", name:"Battery Voltage Threshold 2" ],
            0x0039: [ type:0x20, req:"opt", acc:"rw-", name:"Battery Voltage Threshold 3" ],
            0x003A: [ type:0x20, req:"opt", acc:"rw-", name:"Battery Percentage Min Threshold" ],
            0x003B: [ type:0x20, req:"opt", acc:"rw-", name:"Battery Percentage Threshold 1" ],
            0x003C: [ type:0x20, req:"opt", acc:"rw-", name:"Battery Percentage Threshold 2" ],
            0x003D: [ type:0x20, req:"opt", acc:"rw-", name:"Battery Percentage Threshold 3" ],
            0x003E: [ type:0x1B, req:"opt", acc:"r--", name:"Battery Alarm State" ]
        ]
    ],
    0x0002: [
        name: "Temperature Configuration Cluster",
        attributes: [
            0x0000: [ type:0x29, req:"req", acc:"r--", name:"Current Temperature" ],
            0x0001: [ type:0x29, req:"opt", acc:"r--", name:"Min Temp Experienced" ],
            0x0002: [ type:0x29, req:"opt", acc:"r--", name:"Max Temp Experienced" ],
            0x0003: [ type:0x21, req:"opt", acc:"r--", name:"Over Temp Total Dwell" ],

            0x0010: [ type:0x18, req:"opt", acc:"rw-", name:"Device Temp Alarm Mask" ],
            0x0011: [ type:0x29, req:"opt", acc:"rw-", name:"Low Temp Threshold" ],
            0x0012: [ type:0x29, req:"opt", acc:"rw-", name:"High Temp Threshold" ],
            0x0013: [ type:0x22, req:"opt", acc:"rw-", name:"Low Temp Dwell Trip Point" ],
            0x0014: [ type:0x22, req:"opt", acc:"rw-", name:"High Temp Dwell Trip Point" ]
        ]
    ],
    0x0003: [
        name: "Identify Cluster",
        attributes: [
            0x0000: [ type:0x21, req:"req", acc:"rw-", name:"Identify Time", decorate: { value -> "${Integer.parseInt(value, 16)} seconds" }],
        ],
        commands: [
            0x00: [ req:"req", name:"Identify" ],
            0x01: [ req:"req", name:"Identify Query" ],
            0x40: [ req:"opt", name:"Trigger Effect" ]
        ]
    ],
    0x0004: [
        name: "Groups Cluster",
        attributes: [
            0x0000: [ type:0x18, req:"req", acc:"r--", name:"Name Support" ]
        ],
        commands: [
            0x00: [ req:"req", name:"Add Group" ],
            0x01: [ req:"req", name:"View Group" ],
            0x02: [ req:"req", name:"Get Group Membership" ],
            0x03: [ req:"req", name:"Remove Group" ],
            0x04: [ req:"req", name:"Remove All Groups" ],
            0x05: [ req:"req", name:"Add Group If Identifying" ]
        ]
    ],
    0x0005: [
        name: "Scenes Cluster",
        attributes: [
            0x0000: [ type:0x20, req:"req", acc:"r--", name:"Scene Count" ],
            0x0001: [ type:0x20, req:"req", acc:"r--", name:"Current Scene" ],
            0x0002: [ type:0x21, req:"req", acc:"r--", name:"Current Group" ],
            0x0003: [ type:0x10, req:"req", acc:"r--", name:"Scene Valid" ],
            0x0004: [ type:0x18, req:"req", acc:"r--", name:"Name Support" ],
            0x0005: [ type:0xf0, req:"opt", acc:"r--", name:"Last Configured By" ]
        ],
        commands: [
            0x00: [ req:"req", name:"Add Scene" ],
            0x01: [ req:"req", name:"View Scene" ],
            0x02: [ req:"req", name:"Remove Scene" ],
            0x03: [ req:"req", name:"Remove All Scenes" ],
            0x04: [ req:"req", name:"Store Scene" ],
            0x05: [ req:"req", name:"Recall Scene" ],
            0x06: [ req:"req", name:"Get Scene Membership" ],
            0x40: [ req:"opt", name:"Enhanced Add Scene" ],
            0x41: [ req:"opt", name:"Enhanced View Scene" ],
            0x42: [ req:"opt", name:"Copy Scene" ]
        ]
    ],
    0x0006: [
        name: "On/Off Cluster",
        attributes: [
            0x0000: [ type:0x10, req:"req", acc:"r-p", name:"On Off", decorate: { value -> value == "00" ? "Off" : (value == "01" ? "On" : "Invalid value") }],
            0x4000: [ type:0x10, req:"opt", acc:"r--", name:"Global Scene Control" ],
            0x4001: [ type:0x21, req:"opt", acc:"rw-", name:"On Time", decorate: { value -> "${((Integer.parseInt(value, 16) / 10) as Integer)} seconds" }],
            0x4002: [ type:0x21, req:"opt", acc:"rw-", name:"Off Wait Time", decorate: { value -> "${((Integer.parseInt(value, 16) / 10) as Integer)} seconds" }],
            0x4003: [ type:0x21, req:"opt", acc:"rw-", name:"Power On Behavior", constraints: [
                0x00: "Turn power Off",
                0x01: "Turn power On",
                0xFF: "Restore previous state"
            ]]
        ],
        commands: [
            0x00: [ req:"req", name:"Off" ],
            0x01: [ req:"req", name:"On" ],
            0x02: [ req:"req", name:"Toggle" ],
            0x40: [ req:"opt", name:"Off With Effect" ],
            0x41: [ req:"opt", name:"On With Recall Global Scene" ],
            0x42: [ req:"opt", name:"On With Timed Off" ]
        ]
    ],
    0x0007: [
        name: "On/Off Switch Configuration Cluster",
        attributes: [
            0x0000: [ type:0x30, req:"req", acc:"r--", name:"Switch Type", constraints: [
                0x00: "Toggle",
                0x01: "Momentary",
                0x02: "Multifunction"
            ]],
            0x0010: [ type:0x30, req:"req", acc:"rw-", name:"Switch Actions", constraints: [
                0x00: "On",
                0x01: "Off",
                0x02: "Toggle"
            ]]
        ]
    ],
    0x0008: [
        name: "Level Control Cluster",
        attributes: [
            0x0000: [ type:0x20, req:"req", acc:"r-p", name:"Current Level", decorate: { value -> value == "FF" ? "Invalid value" : "${((Integer.parseInt(value, 16) * 100 / 0xFE) as Integer)}%" }],
            0x0001: [ type:0x21, req:"opt", acc:"r--", name:"Remaining Time", decorate: { value -> "${((Integer.parseInt(value, 16) / 10) as Integer)} seconds" }],
            0x0010: [ type:0x21, req:"opt", acc:"rw-", name:"On Off Transition Time", decorate: { value -> "${((Integer.parseInt(value, 16) / 10) as Integer)} seconds" }],
            0x0011: [ type:0x20, req:"opt", acc:"rw-", name:"On Level", decorate: { value -> value == "FF" ? "Last level" : "${((Integer.parseInt(value, 16) * 100 / 0xFE) as Integer)}%" }],
            0x0012: [ type:0x21, req:"opt", acc:"rw-", name:"On Transition Time", decorate: { value -> "${((Integer.parseInt(value, 16) / 10) as Integer)} seconds" }],
            0x0013: [ type:0x21, req:"opt", acc:"rw-", name:"Off Transition Time", decorate: { value -> "${((Integer.parseInt(value, 16) / 10) as Integer)} seconds" }],
            0x0014: [ type:0x21, req:"opt", acc:"rw-", name:"Default Move Rate" ],
            0x4000: [ type:0x20, req:"opt", acc:"rw-", name:"StartUp Current Level" ]
        ],
        commands: [
            0x00: [ req:"req", name:"Move To Level" ],
            0x01: [ req:"req", name:"Move" ],
            0x02: [ req:"req", name:"Step" ],
            0x03: [ req:"req", name:"Stop" ],
            0x04: [ req:"req", name:"Move To Level With On/Off" ],
            0x05: [ req:"req", name:"Move With On/Off" ],
            0x06: [ req:"req", name:"Step With On/Off" ],
            0x07: [ req:"req", name:"Stop" ]
        ]
    ],
    0x0009: [
        name: "Alarms Cluster",
        attributes: [
            0x0000: [ type:0x21, req:"opt", acc:"r--", name:"Alarm Count" ]
        ],
        commands: [
            0x00: [ req:"req", name:"Move To Level" ],
            0x01: [ req:"req", name:"Move" ],
            0x02: [ req:"req", name:"Step" ],
            0x03: [ req:"req", name:"Stop" ]
        ]
    ],
    0x000A: [
        name: "Time Cluster",
        attributes: [
            0x0000: [ type:0xE2, req:"req", acc:"rw-", name:"Time" ],
            0x0001: [ type:0x18, req:"req", acc:"rw-", name:"Time Status" ],
            0x0002: [ type:0x2B, req:"opt", acc:"rw-", name:"Time Zone" ],
            0x0003: [ type:0x23, req:"opt", acc:"rw-", name:"Dst Start" ],
            0x0004: [ type:0x23, req:"opt", acc:"rw-", name:"Dst End" ],
            0x0005: [ type:0x2B, req:"opt", acc:"rw-", name:"Dst Shift" ],
            0x0006: [ type:0x23, req:"opt", acc:"r--", name:"Standard Time" ],
            0x0007: [ type:0x23, req:"opt", acc:"r--", name:"Local Time" ],
            0x0008: [ type:0xE2, req:"opt", acc:"r--", name:"Last Set Time" ],
            0x0009: [ type:0xE2, req:"opt", acc:"rw-", name:"Valid Until Time" ]
        ]
    ],
    0x000B: [
        name: "RSSI Cluster",
        attributes: [
            0x0000: [ req:"req", acc:"rw-", name:"Location Type" ],
            0x0001: [ req:"req", acc:"rw-", name:"Location Method" ],
            0x0002: [ req:"opt", acc:"r--", name:"Location Age" ],
            0x0003: [ req:"opt", acc:"r--", name:"Quality Measure" ],
            0x0004: [ req:"opt", acc:"r--", name:"Number Of Devices" ],

            0x0010: [ req:"opt", acc:"rw-", name:"Coordinate 1" ],
            0x0011: [ req:"opt", acc:"rw-", name:"Coordinate 2" ],
            0x0012: [ req:"opt", acc:"rw-", name:"Coordinate 3" ],
            0x0013: [ req:"req", acc:"rw-", name:"Power" ],
            0x0014: [ req:"req", acc:"rw-", name:"Path Loss Exponent" ],
            0x0015: [ req:"opt", acc:"rw-", name:"Reporting Period" ],
            0x0016: [ req:"opt", acc:"rw-", name:"Calculation Period" ],
            0x0017: [ req:"opt", acc:"rw-", name:"Number RSSI Measurements" ]
        ],
        commands: [
            0x00: [ req:"req", name:"Set Absolute Location" ],
            0x01: [ req:"req", name:"Set Device Configuration" ],
            0x02: [ req:"req", name:"Get Device Configuration" ],
            0x03: [ req:"req", name:"Get Location Data" ],
            0x04: [ req:"opt", name:"RSSI Response" ],
            0x05: [ req:"opt", name:"Send Pings" ],
            0x06: [ req:"opt", name:"Anchor Node Announce" ]
        ]
    ],
    0x000C: [
        name: "Analog Input Cluster",
        attributes: [
            0x001C: [ req:"opt", acc:"rw-", name:"Description" ],
            0x0041: [ req:"opt", acc:"rw-", name:"Max Present Value" ],
            0x0045: [ req:"opt", acc:"rw-", name:"Min Present Value" ],
            0x0051: [ req:"req", acc:"rw-", name:"Out Of Service" ],
            0x0055: [ req:"req", acc:"RWP", name:"Present Value" ],
            0x0067: [ req:"opt", acc:"rw-", name:"Reliability" ],
            0x006A: [ req:"opt", acc:"rw-", name:"Resolution" ],
            0x006F: [ req:"req", acc:"r-p", name:"Status Flags" ],
            0x0075: [ req:"opt", acc:"rw-", name:"Engineering Units" ],
            0x0100: [ req:"opt", acc:"r--", name:"Application Type" ]
        ]
    ],
    0x000D: [
        name: "Analog Output Cluster",
        attributes: [
            0x001C: [ req:"opt", acc:"rw-", name:"Description" ],
            0x0041: [ req:"opt", acc:"rw-", name:"Max Present Value" ],
            0x0045: [ req:"opt", acc:"rw-", name:"Min Present Value" ],
            0x0051: [ req:"req", acc:"rw-", name:"Out Of Service" ],
            0x0055: [ req:"req", acc:"RWP", name:"Present Value" ],
            0x0057: [ req:"opt", acc:"rw-", name:"Priority Array" ],
            0x0067: [ req:"opt", acc:"rw-", name:"Reliability" ],
            0x0068: [ req:"opt", acc:"rw-", name:"Relinquish Default" ],
            0x006A: [ req:"opt", acc:"rw-", name:"Resolution" ],
            0x006F: [ req:"req", acc:"r-p", name:"Status Flags" ],
            0x0075: [ req:"opt", acc:"rw-", name:"Engineering Units" ],
            0x0100: [ req:"opt", acc:"r--", name:"Application Type" ]
        ]
    ],
    0x000E: [
        name: "Analog Value Cluster",
        attributes: [
            0x001C: [ req:"opt", acc:"rw-", name:"Description" ],
            0x0051: [ req:"req", acc:"rw-", name:"Out Of Service" ],
            0x0055: [ req:"req", acc:"rw-", name:"Present Value" ],
            0x0057: [ req:"opt", acc:"rw-", name:"Priority Array" ],
            0x0067: [ req:"opt", acc:"rw-", name:"Reliability" ],
            0x0068: [ req:"opt", acc:"rw-", name:"Relinquish Default" ],
            0x006F: [ req:"req", acc:"r--", name:"Status Flags" ],
            0x0075: [ req:"opt", acc:"rw-", name:"Engineering Units" ],
            0x0100: [ req:"opt", acc:"r--", name:"Application Type" ]
        ]
    ],
    0x000F: [
        name: "Binary Input Cluster",
        attributes: [
            0x0004: [ req:"opt", acc:"rw-", name:"Active Text" ],
            0x001C: [ req:"opt", acc:"rw-", name:"Description" ],
            0x002E: [ req:"opt", acc:"rw-", name:"Inactive Text" ],
            0x0051: [ req:"req", acc:"rw-", name:"Out Of Service" ],
            0x0054: [ req:"req", acc:"r--", name:"Polarity" ],
            0x0055: [ req:"req", acc:"rw-", name:"Present Value" ],
            0x0067: [ req:"opt", acc:"rw-", name:"Reliability" ],
            0x006F: [ req:"req", acc:"r--", name:"Status Flags" ],
            0x0100: [ req:"opt", acc:"r--", name:"Application Type" ]
        ]
    ],
    0x0010: [
        name: "Binary Output Cluster",
        attributes: [
            0x0004: [ req:"opt", acc:"rw-", name:"Active Text" ],
            0x001C: [ req:"opt", acc:"rw-", name:"Description" ],
            0x002E: [ req:"opt", acc:"rw-", name:"Inactive Text" ],
            0x0042: [ req:"opt", acc:"rw-", name:"Minimum Off Time" ],
            0x0043: [ req:"opt", acc:"rw-", name:"Minimum On Time" ],
            0x0051: [ req:"req", acc:"rw-", name:"Out Of Service" ],
            0x0054: [ req:"req", acc:"r--", name:"Polarity" ],
            0x0055: [ req:"req", acc:"rw-", name:"Present Value" ],
            0x0057: [ req:"opt", acc:"rw-", name:"Priority Array" ],
            0x0067: [ req:"opt", acc:"rw-", name:"Reliability" ],
            0x0068: [ req:"opt", acc:"rw-", name:"Relinquish Default" ],
            0x006F: [ req:"req", acc:"r--", name:"Status Flags" ],
            0x0100: [ req:"opt", acc:"r--", name:"Application Type" ]
        ]
    ],
    0x0011: [
        name: "Binary Value Cluster",
        attributes: [
            0x0004: [ req:"opt", acc:"rw-", name:"Active Text" ],
            0x001C: [ req:"opt", acc:"rw-", name:"Description" ],
            0x002E: [ req:"opt", acc:"rw-", name:"Inactive Text" ],
            0x0042: [ req:"opt", acc:"rw-", name:"Minimum Off Time" ],
            0x0043: [ req:"opt", acc:"rw-", name:"Minimum On Time" ],
            0x0051: [ req:"req", acc:"rw-", name:"Out Of Service" ],
            0x0055: [ req:"req", acc:"rw-", name:"Present Value" ],
            0x0057: [ req:"opt", acc:"rw-", name:"Priority Array" ],
            0x0067: [ req:"opt", acc:"rw-", name:"Reliability" ],
            0x0068: [ req:"opt", acc:"rw-", name:"Relinquish Default" ],
            0x006F: [ req:"req", acc:"r--", name:"Status Flags" ],
            0x0100: [ req:"opt", acc:"r--", name:"Application Type" ]
        ]
    ],
    0x0012: [
        name: "Multistate Input Cluster",
        attributes: [
            0x000E: [ req:"opt", acc:"rw-", name:"State Text" ],
            0x001C: [ req:"opt", acc:"rw-", name:"Description" ],
            0x004A: [ req:"req", acc:"rw-", name:"Number Of States" ],
            0x0051: [ req:"req", acc:"rw-", name:"Out Of Service" ],
            0x0055: [ req:"req", acc:"rw-", name:"Present Value" ],
            0x0067: [ req:"opt", acc:"rw-", name:"Reliability" ],
            0x006F: [ req:"req", acc:"r--", name:"Status Flags" ],
            0x0100: [ req:"opt", acc:"r--", name:"Application Type" ]
        ]
    ],
    0x0013: [
        name: "Multistate Output Cluster",
        attributes: [
            0x000E: [ req:"opt", acc:"rw-", name:"State Text" ],
            0x001C: [ req:"opt", acc:"rw-", name:"Description" ],
            0x004A: [ req:"req", acc:"rw-", name:"Number Of States" ],
            0x0051: [ req:"req", acc:"rw-", name:"Out Of Service" ],
            0x0055: [ req:"req", acc:"rw-", name:"Present Value" ],
            0x0057: [ req:"opt", acc:"r--", name:"Priority Array" ],
            0x0067: [ req:"opt", acc:"rw-", name:"Reliability" ],
            0x0068: [ req:"opt", acc:"rw-", name:"Relinquish Default" ],
            0x006F: [ req:"req", acc:"r--", name:"Status Flags" ],
            0x0100: [ req:"opt", acc:"r--", name:"Application Type" ]
        ]
    ],
    0x0014: [
        name: "Multistate Value Cluster",
        attributes: [
            0x000E: [ req:"opt", acc:"rw-", name:"State Text" ],
            0x001C: [ req:"opt", acc:"rw-", name:"Description" ],
            0x004A: [ req:"req", acc:"rw-", name:"Number Of States" ],
            0x0051: [ req:"req", acc:"rw-", name:"Out Of Service" ],
            0x0055: [ req:"req", acc:"rw-", name:"Present Value" ],
            0x0057: [ req:"opt", acc:"rw-", name:"Priority Array" ],
            0x0067: [ req:"opt", acc:"rw-", name:"Reliability" ],
            0x0068: [ req:"opt", acc:"rw-", name:"Relinquish Default" ],
            0x006F: [ req:"req", acc:"r--", name:"Status Flags" ],
            0x0100: [ req:"opt", acc:"r--", name:"Application Type" ]
        ]
    ],
    0x0015: [
        name: "Commissioning Cluster",
        attributes: [
            0x0000: [ req:"req", acc:"rw-", name:"Short Address" ],
            0x0001: [ req:"req", acc:"rw-", name:"Extended PAN ID" ],
            0x0002: [ req:"req", acc:"rw-", name:"PAN ID" ],
            0x0003: [ req:"req", acc:"rw-", name:"Channel Mask" ],
            0x0004: [ req:"req", acc:"rw-", name:"Protocol Version" ],
            0x0005: [ req:"req", acc:"rw-", name:"Stack Profile" ],
            0x0006: [ req:"req", acc:"rw-", name:"Startup Control" ],
            
            0x0010: [ req:"req", acc:"rw-", name:"Trust Center Address" ],
            0x0011: [ req:"req", acc:"rw-", name:"Trust Center Master Key" ],
            0x0012: [ req:"req", acc:"rw-", name:"Network Key" ],
            0x0013: [ req:"req", acc:"rw-", name:"Use Insecure Join" ],
            0x0014: [ req:"req", acc:"rw-", name:"Preconfigured Link Key" ],
            0x0015: [ req:"req", acc:"rw-", name:"Network Key Seq Num" ],
            0x0016: [ req:"req", acc:"rw-", name:"Network Key Type" ],
            0x0017: [ req:"req", acc:"rw-", name:"Network Manager Address" ],
            
            0x0020: [ req:"opt", acc:"rw-", name:"Scan Attempts" ],
            0x0021: [ req:"opt", acc:"rw-", name:"Time Between Scans" ],
            0x0022: [ req:"opt", acc:"rw-", name:"Rejoin Interval" ],
            
            0x0030: [ req:"opt", acc:"rw-", name:"Indirect Poll Rate" ],
            0x0031: [ req:"opt", acc:"r--", name:"Parent Retry Threshold" ],
            
            0x0040: [ req:"opt", acc:"rw-", name:"Concentrator Flag" ],
            0x0041: [ req:"opt", acc:"rw-", name:"Concentrator Radius" ],
            0x0042: [ req:"opt", acc:"rw-", name:"Concentrator Discovery Time" ]
        ],
        commands: [
            0x00: [ req:"req", name:"Restart Device" ],
            0x01: [ req:"opt", name:"Save Startup Parameters" ],
            0x02: [ req:"opt", name:"Restore Startup Parameter" ],
            0x03: [ req:"req", name:"Reset Startup Parameters" ]
        ]
    ],
    0x0019: [
        name: "OTA Upgrade Cluster",
        attributes: [
            0x0000: [ req:"req", acc:"r--", name:"Upgrade Server ID" ],
            0x0001: [ req:"opt", acc:"r--", name:"File Offset" ],
            0x0002: [ req:"opt", acc:"r--", name:"Current File Version" ],
            0x0003: [ req:"opt", acc:"r--", name:"Current Zigbee Stack Version" ],
            0x0004: [ req:"opt", acc:"r--", name:"Downloaded File Version" ],
            0x0005: [ req:"opt", acc:"r--", name:"Downloaded Zigbee Stack Version" ],
            0x0006: [ req:"req", acc:"r--", name:"Image Upgrade Status" ],
            0x0007: [ req:"opt", acc:"r--", name:"Manufacturer ID" ],
            0x0008: [ req:"opt", acc:"r--", name:"Image Type ID" ],
            0x0009: [ req:"opt", acc:"r--", name:"Minimum Block Period" ],
            0x000A: [ req:"opt", acc:"r--", name:"Image Stamp" ]
        ],
        commands: [
            0x00: [ req:"opt", name:"Image Notify" ],
            0x01: [ req:"req", name:"Query Next Image Request" ],
            0x02: [ req:"req", name:"Query Next Image Response" ],
            0x03: [ req:"req", name:"Image Block Request" ],
            0x04: [ req:"opt", name:"Image Page Request" ],
            0x05: [ req:"req", name:"Image Block Response" ],
            0x06: [ req:"req", name:"Upgrade End Request" ],
            0x07: [ req:"req", name:"Upgrade End Response" ],
            0x08: [ req:"opt", name:"Query Device Specific File Request" ],
            0x09: [ req:"opt", name:"Query Device Specific File Response" ]
        ]
    ],
    0x0021: [
        name: "Green Power Cluster"
    ],
    0x001A: [
        name: "Power Profile Cluster",
        attributes: [
            0x0000: [ req:"req", acc:"r--", name:"Total Profile Num" ],
            0x0001: [ req:"req", acc:"r--", name:"Multiple Scheduling" ],
            0x0002: [ req:"req", acc:"r--", name:"Energy Formatting" ],
            0x0003: [ req:"req", acc:"r-p", name:"Energy Remote" ],
            0x0004: [ req:"req", acc:"RWP", name:"Schedule Mode" ]
        ],
        commands: [
            0x00: [ req:"req", name:"Power Profile Request" ],
            0x01: [ req:"req", name:"Power Profile State Request" ],
            0x02: [ req:"req", name:"Get Power Profile Price Response" ],
            0x03: [ req:"req", name:"Get Overall Schedule Price Response" ],
            0x04: [ req:"req", name:"Energy Phases Schedule Notification" ],
            0x05: [ req:"req", name:"Energy Phases Schedule Response" ],
            0x06: [ req:"req", name:"Power Profile Schedule Constraints Request" ],
            0x07: [ req:"req", name:"Energy Phases Schedule State Request" ],
            0x08: [ req:"req", name:"Get Power Profile Price Extended Response" ]
        ]
    ],
    0x0020: [
        name: "Poll Cluster",
        attributes: [
            0x0000: [ type:0x23, req:"req", acc:"rw-", name:"Check-in Interval", decorate: { value -> "${((Integer.parseInt(value, 16) / 4) as Integer)} seconds" }],
            0x0001: [ type:0x23, req:"req", acc:"r--", name:"Long Poll Interval", decorate: { value -> "${((Integer.parseInt(value, 16) / 4) as Integer)} seconds" }],
            0x0002: [ type:0x21, req:"req", acc:"r--", name:"Short Poll Interval", decorate: { value -> "${((Integer.parseInt(value, 16) / 4) as Integer)} seconds" }],
            0x0003: [ type:0x21, req:"req", acc:"rw-", name:"Fast Poll Timeout", decorate: { value -> "${((Integer.parseInt(value, 16) / 4) as Integer)} seconds" }],
            0x0004: [ type:0x23, req:"opt", acc:"r--", name:"Check-in Interval Min", decorate: { value -> "${((Integer.parseInt(value, 16) / 4) as Integer)} seconds" }],
            0x0005: [ type:0x23, req:"opt", acc:"r--", name:"Long Poll Interval Min", decorate: { value -> "${((Integer.parseInt(value, 16) / 4) as Integer)} seconds" }],
            0x0006: [ type:0x21, req:"opt", acc:"r--", name:"Fast Poll Timeout Max", decorate: { value -> "${((Integer.parseInt(value, 16) / 4) as Integer)} seconds" }]
        ],
        commands: [
            0x00: [ req:"req", name:"Check-in" ],
            0x01: [ req:"req", name:"Stop" ],
            0x02: [ req:"opt", name:"Set Long Poll Attribute" ],
            0x03: [ req:"opt", name:"Set Short Poll Attribute" ]
        ]
    ],
    0x0100: [
        name: "Shade Configuration Cluster",
        attributes: [
            0x0000: [ req:"opt", acc:"r--", name:"Physical Closed Limit" ],
            0x0001: [ req:"opt", acc:"r--", name:"Motor Step Size" ],
            0x0002: [ req:"req", acc:"rw-", name:"Status" ],
            
            0x0010: [ req:"req", acc:"rw-", name:"Closed Limit" ],
            0x0011: [ req:"req", acc:"rw-", name:"Mode" ]
        ]
    ],
    0x0101: [
        name: "Door Lock Cluster",
        attributes: [
            0x0000: [ req:"req", acc:"r-p", name:"Lock State" ],
            0x0001: [ req:"req", acc:"r--", name:"Lock Type" ],
            0x0002: [ req:"req", acc:"r--", name:"Actuator Enabled" ],
            0x0003: [ req:"opt", acc:"r-p", name:"Door State" ],
            0x0004: [ req:"opt", acc:"rw-", name:"Door Open Events" ],
            0x0005: [ req:"opt", acc:"rw-", name:"Door Closed Events" ],
            0x0006: [ req:"opt", acc:"rw-", name:"Open Period" ],
            
            0x0010: [ req:"opt", acc:"r--", name:"Number Of Log Records Supported" ],
            0x0011: [ req:"opt", acc:"r--", name:"Number Of Total Users Supported" ],
            0x0012: [ req:"opt", acc:"r--", name:"Number Of PIN Users Supported" ],
            0x0013: [ req:"opt", acc:"r--", name:"Number Of RFID Users Supported" ],
            0x0014: [ req:"opt", acc:"r--", name:"Number Of Week Day Schedules Supported Per User" ],
            0x0015: [ req:"opt", acc:"r--", name:"Number Of Year Day Schedules Supported Per User" ],
            0x0016: [ req:"opt", acc:"r--", name:"Number Of Holiday Schedules Supported" ],
            0x0017: [ req:"opt", acc:"r--", name:"Max PIN Code Length" ],
            0x0018: [ req:"opt", acc:"r--", name:"Min PIN Code Length" ],
            0x0019: [ req:"opt", acc:"r--", name:"Max RFID Code Length" ],
            0x001A: [ req:"opt", acc:"r--", name:"Min RFID Code Length" ],
            
            0x0020: [ req:"opt", acc:"RwP", name:"Enable Logging" ],
            0x0021: [ req:"opt", acc:"RwP", name:"Language" ],
            0x0022: [ req:"opt", acc:"RwP", name:"Settings" ],
            0x0023: [ req:"opt", acc:"RwP", name:"Auto Relock Time" ],
            0x0024: [ req:"opt", acc:"RwP", name:"Sound Volume" ],
            0x0025: [ req:"opt", acc:"RwP", name:"Operating Mode" ],
            0x0026: [ req:"opt", acc:"r--", name:"Supported Operating Modes" ],
            0x0027: [ req:"opt", acc:"r-p", name:"Default Configuration Register" ],
            0x0028: [ req:"opt", acc:"RwP", name:"Enable Local Programming" ],
            0x0029: [ req:"opt", acc:"RWP", name:"Enable One Touch Locking" ],
            0x002A: [ req:"opt", acc:"RWP", name:"Enable Inside Status LED" ],
            0x002B: [ req:"opt", acc:"RWP", name:"Enable Privacy Mode Button" ],
            
            0x0030: [ req:"opt", acc:"RwP", name:"Wrong Code Entry Limit" ],
            0x0031: [ req:"opt", acc:"RwP", name:"User Code Temporary Disable Time" ],
            0x0032: [ req:"opt", acc:"RwP", name:"Send PIN Over The Air" ],
            0x0033: [ req:"opt", acc:"RwP", name:"Require PIN For RF Operation" ],
            0x0034: [ req:"opt", acc:"r-p", name:"Zigbee Security Level" ],
            
            0x0040: [ req:"opt", acc:"RWP", name:"Alarm Mask" ],
            0x0041: [ req:"opt", acc:"RWP", name:"Keypad Operation Event Mask" ],
            0x0042: [ req:"opt", acc:"RWP", name:"RF Operation Event Mask" ],
            0x0043: [ req:"opt", acc:"RWP", name:"Manual Operation Event Mask" ],
            0x0044: [ req:"opt", acc:"RWP", name:"RFID Operation Event Mask" ],
            0x0045: [ req:"opt", acc:"RWP", name:"Keypad Programming Event Mask" ],
            0x0046: [ req:"opt", acc:"RWP", name:"RF Programming Event Mask" ],
            0x0047: [ req:"opt", acc:"RWP", name:"RFID Programming Event Mask" ]
        ],
        commands: [
            0x00: [ req:"req", name:"Lock Door" ],
            0x01: [ req:"req", name:"Unlock Door" ],
            0x02: [ req:"opt", name:"Toggle" ],
            0x03: [ req:"opt", name:"Unlock with Timeout" ],
            0x04: [ req:"opt", name:"Get Log Record" ],
            0x05: [ req:"opt", name:"Set PIN Code" ],
            0x06: [ req:"opt", name:"Get PIN Code" ],
            0x07: [ req:"opt", name:"Clear PIN Code" ],
            0x08: [ req:"opt", name:"Clear All PIN Codes" ],
            0x09: [ req:"opt", name:"Set User Status" ],
            0x0A: [ req:"opt", name:"Get User Status" ],
            0x0B: [ req:"opt", name:"Set Weekday Schedule" ],
            0x0C: [ req:"opt", name:"Get Weekday Schedule" ],
            0x0D: [ req:"opt", name:"Clear Weekday Schedule" ],
            0x0E: [ req:"opt", name:"Set Year Day Schedule" ],
            0x0F: [ req:"opt", name:"Get Year Day Schedule" ],
            0x10: [ req:"opt", name:"Clear Year Day Schedule" ],
            0x11: [ req:"opt", name:"Set Holiday Schedule" ],
            0x12: [ req:"opt", name:"Get Holiday Schedule" ],
            0x13: [ req:"opt", name:"Clear Holiday Schedule" ],
            0x14: [ req:"opt", name:"Set User Type" ],
            0x15: [ req:"opt", name:"Get User Type" ],
            0x16: [ req:"opt", name:"Set RFID Code" ],
            0x17: [ req:"opt", name:"Get RFID Code" ],
            0x18: [ req:"opt", name:"Clear RFID Code" ],
            0x19: [ req:"opt", name:"Clear All RFID Codes" ]
        ]
    ],
    0x0102: [
        name: "Window Covering Cluster",
        attributes: [
            0x0000: [ req:"req", acc:"r--", name:"Window Covering Type" ],
            0x0001: [ req:"opt", acc:"r--", name:"Physical Closed Limit – Lift" ],
            0x0002: [ req:"opt", acc:"r--", name:"Physical Closed Limit – Tilt" ],
            0x0003: [ req:"opt", acc:"r--", name:"Current Position – Lift" ],
            0x0004: [ req:"opt", acc:"r--", name:"Current Position – Tilt" ],
            0x0005: [ req:"opt", acc:"r--", name:"Number Of Actuations – Lift" ],
            0x0006: [ req:"opt", acc:"r--", name:"Number Of Actuations – Tilt" ],
            0x0007: [ req:"req", acc:"r--", name:"Config/Status" ],
            0x0008: [ req:"req", acc:"RSP", name:"Current Position Lift Percentage" ],
            0x0009: [ req:"req", acc:"RSP", name:"Current Position Tilt Percentage" ],

            0x0100: [ req:"req", acc:"r--", name:"Installed Open Limit – Lift" ],
            0x0101: [ req:"req", acc:"r--", name:"Installed Closed Limit – Lift" ],
            0x0102: [ req:"req", acc:"r--", name:"Installed Open Limit – Tilt" ],
            0x0103: [ req:"req", acc:"r--", name:"Installed Closed Limit – Tilt" ],
            0x0104: [ req:"opt", acc:"rw-", name:"Velocity – Lift" ],
            0x0105: [ req:"opt", acc:"rw-", name:"Acceleration Time – Lift" ],
            0x0106: [ req:"opt", acc:"rw-", name:"Deceleration Time – Lift" ],
            0x0107: [ req:"req", acc:"rw-", name:"Mode" ],
            0x0108: [ req:"opt", acc:"rw-", name:"Intermediate Setpoints – Lift" ],
            0x0109: [ req:"opt", acc:"rw-", name:"Intermediate Setpoints – Tilt" ]
        ],
        commands: [
            0x00: [ req:"req", name:"Up / Open" ],
            0x01: [ req:"req", name:"Down / Close" ],
            0x02: [ req:"req", name:"Stop" ],
            0x04: [ req:"opt", name:"Go To Lift Value" ],
            0x05: [ req:"opt", name:"Go to Lift Percentage" ],
            0x07: [ req:"opt", name:"Go to Tilt Value" ],
            0x08: [ req:"opt", name:"Go to Tilt Percentage" ]
        ]
    ],
    0x0200: [
        name: "Pump Configuration and Control Cluster",
        attributes: [
            0x0000: [ req:"req", acc:"r--", name:"Max Pressure" ],
            0x0001: [ req:"req", acc:"r--", name:"Max Speed" ],
            0x0002: [ req:"req", acc:"r--", name:"Max Flow" ],
            0x0003: [ req:"opt", acc:"r--", name:"Min Const Pressure" ],
            0x0004: [ req:"opt", acc:"r--", name:"Max Const Pressure" ],
            0x0005: [ req:"opt", acc:"r--", name:"Min Comp Pressure" ],
            0x0006: [ req:"opt", acc:"r--", name:"Max Comp Pressure" ],
            0x0007: [ req:"opt", acc:"r--", name:"Min Const Speed" ],
            0x0008: [ req:"opt", acc:"r--", name:"Max Const Speed" ],
            0x0009: [ req:"opt", acc:"r--", name:"Min Const Flow" ],
            0x000A: [ req:"opt", acc:"r--", name:"Max Const Flow" ],
            0x000B: [ req:"opt", acc:"r--", name:"Min Const Temp" ],
            0x000C: [ req:"opt", acc:"r--", name:"Max Const Temp" ],

            0x0010: [ req:"opt", acc:"r-p", name:"Pump Status" ],
            0x0011: [ req:"req", acc:"r--", name:"Effective Operation Mode" ],
            0x0012: [ req:"req", acc:"r--", name:"Effective Control Mode" ],
            0x0013: [ req:"req", acc:"r-p", name:"Capacity" ],
            0x0014: [ req:"opt", acc:"r--", name:"Speed" ],
            0x0015: [ req:"opt", acc:"rw-", name:"Lifetime Running Hours" ],
            0x0016: [ req:"opt", acc:"rw-", name:"Power" ],
            0x0017: [ req:"opt", acc:"r--", name:"Lifetime Energy Consumed" ],

            0x0020: [ req:"req", acc:"rw-", name:"Operation Mode" ],
            0x0021: [ req:"opt", acc:"rw-", name:"Control Mode" ],
            0x0022: [ req:"opt", acc:"r--", name:"Alarm Mask" ]
        ]
    ],
    0x0201: [
        name: "Thermostat Cluster",
        attributes: [
            0x0000: [ req:"req", acc:"r-p", name:"Local Temperature" ],
            0x0001: [ req:"opt", acc:"r--", name:"Outdoor Temperature" ],
            0x0002: [ req:"opt", acc:"r--", name:"Occupancy" ],
            0x0003: [ req:"opt", acc:"r--", name:"Abs Min Heat Setpoint Limit" ],
            0x0004: [ req:"opt", acc:"r--", name:"Abs Max Heat Setpoint Limit" ],
            0x0005: [ req:"opt", acc:"r--", name:"Abs Min Cool Setpoint Limit" ],
            0x0006: [ req:"opt", acc:"r--", name:"Abs Max Cool Setpoint Limit" ],
            0x0007: [ req:"opt", acc:"r-p", name:"PI Cooling Demand" ],
            0x0008: [ req:"opt", acc:"r-p", name:"PI Heating Demand" ],
            0x0009: [ req:"opt", acc:"rw-", name:"HVAC System Type Configuration" ],

            0x0010: [ req:"opt", acc:"rw-", name:"Local Temperature Calibration" ],
            0x0011: [ req:"req", acc:"rw-", name:"Occupied Cooling Setpoint" ],
            0x0012: [ req:"req", acc:"rws", name:"Occupied Heating Setpoint" ],
            0x0013: [ req:"opt", acc:"rw-", name:"Unoccupied Cooling Setpoint" ],
            0x0014: [ req:"opt", acc:"rw-", name:"Unoccupied Heating Setpoint" ],
            0x0015: [ req:"opt", acc:"rw-", name:"Min Heat Setpoint Limit" ],
            0x0016: [ req:"opt", acc:"rw-", name:"Max Heat Setpoint Limit" ],
            0x0017: [ req:"opt", acc:"rw-", name:"Min Cool Setpoint Limit" ],
            0x0018: [ req:"opt", acc:"rw-", name:"Max Cool Setpoint Limit" ],
            0x0019: [ req:"opt", acc:"rw-", name:"Min Setpoint Dead Band" ],
            0x001A: [ req:"opt", acc:"rw-", name:"Remote Sensing" ],
            0x001B: [ req:"req", acc:"rw-", name:"Control Sequence Of Operation" ],
            0x001C: [ req:"req", acc:"rws", name:"System Mode" ],
            0x001D: [ req:"opt", acc:"r--", name:"Alarm Mask" ],
            0x001E: [ req:"opt", acc:"r--", name:"Thermostat Running Mode" ],

            0x0020: [ req:"opt", acc:"r--", name:"Start Of Week" ],
            0x0021: [ req:"opt", acc:"r--", name:"Number Of Weekly Transitions" ],
            0x0022: [ req:"opt", acc:"r--", name:"Number Of Daily Transitions" ],
            0x0023: [ req:"opt", acc:"rw-", name:"Temperature Setpoint Hold" ],
            0x0024: [ req:"opt", acc:"rw-", name:"Temperature Setpoint Hold Duration" ],
            0x0025: [ req:"opt", acc:"rw-", name:"Thermostat Programmin gOperation Mode" ],
            0x0029: [ req:"opt", acc:"r--", name:"Thermostat Running State" ],
            
            0x0030: [ req:"opt", acc:"r--", name:"Setpoint Change Source" ],
            0x0031: [ req:"opt", acc:"r--", name:"Setpoint Change Amount" ],
            0x0032: [ req:"opt", acc:"r--", name:"Setpoint Change Source Timestamp" ],
            
            0x0040: [ req:"opt", acc:"rw-", name:"AC Type" ],
            0x0041: [ req:"opt", acc:"rw-", name:"AC Capacity" ],
            0x0042: [ req:"opt", acc:"rw-", name:"AC Refrigerant Type" ],
            0x0043: [ req:"opt", acc:"rw-", name:"AC Compressor Type" ],
            0x0044: [ req:"opt", acc:"rw-", name:"AC Error Code" ],
            0x0045: [ req:"opt", acc:"rw-", name:"AC Louver Position" ],
            0x0046: [ req:"opt", acc:"r--", name:"AC Coil Temperature" ],
            0x0047: [ req:"opt", acc:"rw-", name:"AC Capacity Format" ]
        ],
        commands: [
            0x00: [ req:"req", name:"Setpoint Raise/Lower" ],
            0x01: [ req:"opt", name:"Set Weekly Schedule" ],
            0x02: [ req:"opt", name:"Get Weekly Schedule" ],
            0x03: [ req:"opt", name:"Clear Weekly Schedule" ],
            0x04: [ req:"opt", name:"Get Relay Status Log" ]
        ]
    ],
    0x0202: [
        name: "Fan Control Cluster",
        attributes: [
            0x0000: [ req:"req", acc:"rw-", name:"Fan Mode", constraints: [
                0x00: "Off",
                0x01: "Low",
                0x02: "Medium",
                0x03: "High",
                0x04: "On",
                0x05: "Auto (the fan speed is self-regulated)",
                0x06: "Smart (when the heated/cooled space is occupied, the fan is always on)"
            ]],
            0x0001: [ req:"req", acc:"rw-", name:"Fan Mode Sequence", constraints: [
                0x00: "Low/Med/High",
                0x01: "Low/High",
                0x02: "Low/Med/High/Auto",
                0x03: "Low/High/Auto",
                0x04: "On/Auto"
            ]],
        ]
    ],
    0x0203: [
        name: "Dehumidification Control Cluster",
        attributes: [
            0x0000: [ req:"opt", acc:"r--", name:"Relative Humidity" ],
            0x0001: [ req:"req", acc:"r-p", name:"Dehumidificatio nCooling" ],
            
            0x0010: [ req:"req", acc:"rw-", name:"RH Dehumidification Setpoint" ],
            0x0011: [ req:"opt", acc:"rw-", name:"Relative Humidity Mode" ],
            0x0012: [ req:"opt", acc:"rw-", name:"Dehumidification Lockout" ],
            0x0013: [ req:"req", acc:"rw-", name:"Dehumidification Hysteresis" ],
            0x0014: [ req:"req", acc:"rw-", name:"Dehumidification Max Cool" ],
            0x0015: [ req:"opt", acc:"rw-", name:"RelativeHumidity Display" ]
        ]
    ],
    0x0204: [
        name: "Thermostat User Interface Configuration Cluster",
        attributes: [
            0x0000: [ req:"req", acc:"r--", name:"Temperature Display Mode" ],
            0x0001: [ req:"req", acc:"rw-", name:"Keypad Lockout" ],
            0x0002: [ req:"opt", acc:"rw-", name:"Schedule Programming Visibility" ]
        ]
    ],
    0x0300: [
        name: "Color Control Cluster",
        attributes: [
            0x0000: [ req:"req", acc:"r-p", name:"CurrentHue" ],
            0x0001: [ req:"req", acc:"r-p", name:"Current Saturation" ],
            0x0002: [ req:"opt", acc:"r--", name:"Remaining Time" ],
            0x0003: [ req:"req", acc:"r-p", name:"CurrentX" ],
            0x0004: [ req:"req", acc:"r-p", name:"CurrentY" ],
            0x0005: [ req:"opt", acc:"r--", name:"Drift Compensation" ],
            0x0006: [ req:"opt", acc:"r--", name:"Compensation Text" ],
            0x0007: [ req:"req", acc:"r-p", name:"Color Temperature Mireds" ],
            0x0008: [ req:"req", acc:"r--", name:"Color Mode" ],
            
            0x0010: [ req:"req", acc:"r--", name:"Number Of Primaries" ],
            0x0011: [ req:"req", acc:"r--", name:"Primary 1 X" ],
            0x0012: [ req:"opt", acc:"r--", name:"Primary 1 Y" ],
            0x0013: [ req:"req", acc:"r--", name:"Primary 1 Intensity" ],
            0x0015: [ req:"opt", acc:"r--", name:"Primary 2 X" ],
            0x0016: [ req:"opt", acc:"r--", name:"Primary 2 Y" ],
            0x0017: [ req:"req", acc:"r--", name:"Primary 2 Intensity" ],
            0x0019: [ req:"req", acc:"r--", name:"Primary 3 X" ],
            0x001A: [ req:"req", acc:"r--", name:"Primary 3 Y" ],
            0x001B: [ req:"req", acc:"r--", name:"Primary 3 Intensity" ],

            0x0020: [ req:"req", acc:"r--", name:"Primary 4 X" ],
            0x0021: [ req:"req", acc:"r--", name:"Primary 4 Y" ],
            0x0022: [ req:"opt", acc:"r--", name:"Primary 4 Intensity" ],
            0x0024: [ req:"opt", acc:"r--", name:"Primary 2 X" ],
            0x0025: [ req:"opt", acc:"r--", name:"Primary 2 Y" ],
            0x0026: [ req:"req", acc:"r--", name:"Primary 2 Intensity" ],
            0x0028: [ req:"req", acc:"r--", name:"Primary 3 X" ],
            0x0029: [ req:"req", acc:"r--", name:"Primary 3 Y" ],
            0x002A: [ req:"req", acc:"r--", name:"Primary 3 Intensity" ],

            0x0030: [ req:"req", acc:"rw-", name:"White Point X" ],
            0x0031: [ req:"req", acc:"rw-", name:"White Point Y" ],
            0x0032: [ req:"opt", acc:"rw-", name:"Color Point R X" ],
            0x0033: [ req:"opt", acc:"rw-", name:"Color Point R Y" ],
            0x0034: [ req:"opt", acc:"rw-", name:"Color Point R Intensity" ],
            0x0036: [ req:"req", acc:"rw-", name:"Color Point G X" ],
            0x0037: [ req:"req", acc:"rw-", name:"Color Point G Y" ],
            0x0038: [ req:"req", acc:"rw-", name:"Color Point G Intensity" ],
            0x003A: [ req:"req", acc:"rw-", name:"Color Point B X" ],
            0x003B: [ req:"req", acc:"rw-", name:"Color Point B Y" ],
            0x003C: [ req:"req", acc:"rw-", name:"Color Point B Intensity" ],

            0x4000: [ req:"req", acc:"r--", name:"Enhanced Current Hue" ],
            0x4001: [ req:"req", acc:"r--", name:"Enhanced Color Mode" ],
            0x4002: [ req:"req", acc:"r--", name:"Color Loop Active" ],
            0x4003: [ req:"req", acc:"r--", name:"Color Loop Direction" ],
            0x4004: [ req:"req", acc:"r--", name:"Color Loop Time" ],
            0x4005: [ req:"req", acc:"r--", name:"Color Loop Start Enhanced Hue" ],
            0x4006: [ req:"req", acc:"r--", name:"Color Loop Stored Enhanced Hue" ],
            0x400A: [ req:"req", acc:"r--", name:"Color Capabilities" ],
            0x400B: [ req:"req", acc:"r--", name:"Color Temp Physical Min Mireds" ],
            0x400C: [ req:"req", acc:"r--", name:"Color Temp Physical Max Mireds" ],
            0x4010: [ req:"opt", acc:"rw-", name:"StartUp Color Temperature Mireds" ]
        ],
        commands: [
            0x00: [ req:"req", name:"Move to Hue" ],
            0x01: [ req:"req", name:"Move Hue" ],
            0x02: [ req:"req", name:"Step Hue" ],
            0x03: [ req:"req", name:"Move to Saturation" ],
            0x04: [ req:"req", name:"Move Saturation" ],
            0x05: [ req:"req", name:"Step Saturation" ],
            0x06: [ req:"req", name:"Move to Hue and Saturation" ],
            0x07: [ req:"req", name:"Move to Color" ],
            0x08: [ req:"req", name:"Move Color" ],
            0x09: [ req:"req", name:"Step Color" ],
            0x0A: [ req:"req", name:"Move to Color Temperature" ],
            
            0x40: [ req:"req", name:"Enhanced Move to Hue" ],
            0x41: [ req:"req", name:"Enhanced Move Hue" ],
            0x42: [ req:"req", name:"Enhanced Step Hue" ],
            0x43: [ req:"req", name:"Enhanced Move to Hue and Saturation" ],
            0x44: [ req:"req", name:"Color Loop Set" ],
            0x47: [ req:"req", name:"Stop Move Step" ],
            0x4B: [ req:"req", name:"Move Color Temperature" ],
            0x4C: [ req:"req", name:"Step Color Temperature" ]
        ]
    ],
    0x0301: [
        name: "Ballast Configuration Cluster",
        attributes: [
            0x0000: [ req:"opt", acc:"r--", name:"Physical Min Level" ],
            0x0001: [ req:"opt", acc:"r--", name:"Physical Max Level" ],
            0x0002: [ req:"req", acc:"r--", name:"Ballast Status" ],

            0x0010: [ req:"opt", acc:"rw-", name:"Min Level" ],
            0x0011: [ req:"opt", acc:"rw-", name:"Max Level" ],
            0x0012: [ req:"opt", acc:"rw-", name:"Power On Level" ],
            0x0013: [ req:"opt", acc:"rw-", name:"Power On Fade Time" ],
            0x0014: [ req:"opt", acc:"rw-", name:"Intrinsic Ballast Factor" ],
            0x0015: [ req:"opt", acc:"rw-", name:"Ballast Factor Adjustment" ],
            
            0x0020: [ req:"opt", acc:"r--", name:"Lamp Quantity" ],
            
            0x0030: [ req:"opt", acc:"rw-", name:"Lamp Type" ],
            0x0031: [ req:"opt", acc:"rw-", name:"Lamp Manufacturer" ],
            0x0032: [ req:"opt", acc:"rw-", name:"Lamp Rated Hours" ],
            0x0033: [ req:"opt", acc:"rw-", name:"Lamp Burn Hours" ],
            0x0034: [ req:"opt", acc:"rw-", name:"Lamp Alarm Mode" ],
            0x0035: [ req:"opt", acc:"rw-", name:"Lamp Burn Hours Trip Point" ]
        ]
    ],
    0x0400: [
        name: "Illuminance Measurement Cluster",
        attributes: [
            0x0000: [ req:"req", acc:"r--", name:"Measured Value" ],
            0x0001: [ req:"req", acc:"RP-", name:"Min Measured Value" ],
            0x0002: [ req:"req", acc:"r--", name:"Max Measured Value" ],
            0x0003: [ req:"opt", acc:"r--", name:"Tolerance" ],
            0x0004: [ req:"opt", acc:"r--", name:"Light Sensor Type" ]
        ]
    ],
    0x0401: [
        name: "Illuminance Level Sensing Cluster",
        attributes: [
            0x0000: [ req:"req", acc:"r-p", name:"Level Status" ],
            0x0001: [ req:"opt", acc:"r--", name:"Light Sensor Type" ],

            0x0010: [ req:"req", acc:"rw-", name:"Illuminance Target Level" ]
        ]
    ],
    0x0402: [
        name: "Temperature Measurement Cluster",
        attributes: [
            0x0000: [ req:"req", acc:"r-p", name:"MeasuredValue", decorate: { value -> "${(Integer.parseInt(value, 16) / 100)} °C" }],
            0x0001: [ req:"req", acc:"r--", name:"MinMeasuredValue" ],
            0x0002: [ req:"req", acc:"r--", name:"MaxMeasuredValue" ],
            0x0003: [ req:"opt", acc:"r-p", name:"Tolerance" ]
        ]
    ],
    0x0403: [
        name: "Pressure Measurement Cluster",
        attributes: [
            0x0000: [ req:"req", acc:"r-p", name:"Measured Value" ],
            0x0001: [ req:"req", acc:"r--", name:"Min Measured Value" ],
            0x0002: [ req:"req", acc:"r--", name:"Max Measured Value" ],
            0x0003: [ req:"opt", acc:"r-p", name:"Tolerance" ],

            0x0010: [ req:"opt", acc:"r--", name:"Scaled Value" ],
            0x0011: [ req:"opt", acc:"r--", name:"Min Scaled Value" ],
            0x0012: [ req:"opt", acc:"r--", name:"Max Scaled Value" ],
            0x0013: [ req:"opt", acc:"r--", name:"Scaled Tolerance" ],
            0x0014: [ req:"opt", acc:"r--", name:"Scale" ]
        ]
    ],
    0x0404: [
        name: "Flow Measurement Cluster",
        attributes: [
            0x0000: [ req:"req", acc:"r-p", name:"Measured Value" ],
            0x0001: [ req:"req", acc:"r--", name:"Min Measured Value" ],
            0x0002: [ req:"req", acc:"r--", name:"Max Measured Value" ],
            0x0003: [ req:"opt", acc:"r-p", name:"Tolerance" ]
        ]
    ],
    0x0405: [
        name: "Relative Humidity Cluster",
        attributes: [
            0x0000: [ req:"req", acc:"r-p", name:"Measured Value", decorate: { value -> "${(Integer.parseInt(value, 16) / 100)}% RH" }],
            0x0001: [ req:"req", acc:"r--", name:"Min Measured Value" ],
            0x0002: [ req:"req", acc:"r--", name:"Max Measured Value" ],
            0x0003: [ req:"opt", acc:"r-p", name:"Tolerance" ]
        ]
    ],
    0x0406: [
        name: "Occupancy Sensing Cluster",
        attributes: [
            0x0000: [ req:"req", acc:"r-p", name:"Occupancy" ],
            0x0001: [ req:"req", acc:"r--", name:"Occupancy Sensor Type" ],

            0x0010: [ req:"req", acc:"rw-", name:"PIR Occupied To Unoccupied Delay" ],
            0x0011: [ req:"req", acc:"rw-", name:"PIR Unoccupied To Occupied Delay" ],
            0x0012: [ req:"req", acc:"rw-", name:"PIR Unoccupied To Occupied Threshold" ],

            0x0020: [ req:"req", acc:"rw-", name:"Ultrasonic Occupied To Unoccupied Delay" ],
            0x0021: [ req:"req", acc:"rw-", name:"Ultrasonic Unoccupied To Occupied Delay" ],
            0x0022: [ req:"req", acc:"rw-", name:"Ultrasonic Unoccupied To Occupied Threshold" ]
        ]
    ],
    0x0500: [
        name: "IAS Zone Cluster",
        attributes: [
            0x0000: [ req:"req", acc:"r--", name:"Zone State" ],
            0x0001: [ req:"req", acc:"r--", name:"Zone Type" ],
            0x0002: [ req:"req", acc:"r--", name:"Zone Status" ],
            
            0x0010: [ req:"req", acc:"rw-", name:"IAS CIE Address" ],
            0x0011: [ req:"req", acc:"r--", name:"Zone ID" ],
            0x0012: [ req:"opt", acc:"r--", name:"Number Of Zone Sensitivity Levels Supported" ],
            0x0013: [ req:"opt", acc:"rw-", name:"Current Zone Sensitivity Level" ]
        ],
        commands: [
            0x00: [ req:"req", name:"Zone Enroll Response" ],
            0x01: [ req:"opt", name:"Initiate Normal Operation Mode" ],
            0x02: [ req:"opt", name:"Initiate Test Mode" ]
        ]
    ],
    0x0502: [
        name: "IAS WD Cluster",
        attributes: [
            0x0000: [ req:"req", acc:"rw-", name:"Max Duration" ]
        ]
    ],
    0x0B01: [
        name: "Meter Identification Cluster",
        attributes: [
            0x0000: [ req:"req", acc:"r--", name:"Company Name" ],
            0x0001: [ req:"req", acc:"r--", name:"Meter Type ID" ],
            0x0004: [ req:"req", acc:"r--", name:"Data Quality ID" ],
            0x0005: [ req:"opt", acc:"rw-", name:"Customer Name" ],
            0x0006: [ req:"opt", acc:"r--", name:"Model" ],
            0x0007: [ req:"opt", acc:"r--", name:"Part Number" ],
            0x0008: [ req:"opt", acc:"r--", name:"Product Revision" ],
            0x000A: [ req:"req", acc:"r--", name:"Software Revision" ],
            0x000B: [ req:"opt", acc:"r--", name:"Utility Name" ],
            0x000C: [ req:"req", acc:"r--", name:"POD" ],
            0x000D: [ req:"req", acc:"r--", name:"Available Power" ],
            0x000E: [ req:"req", acc:"r--", name:"Power Threshold" ]
        ]
    ],
    0x0B04: [
        name: "Electrical Measurement Cluster",
        attributes: [
            0x0000: [ req:"req", acc:"r--", name:"Measurement Type" ],
            
            0x0100: [ req:"opt", acc:"r--", name:"DC Voltage" ],
            0x0101: [ req:"opt", acc:"r--", name:"DC Voltage Min" ],
            0x0102: [ req:"opt", acc:"r--", name:"DC Voltage Max" ],
            0x0103: [ req:"opt", acc:"r--", name:"DC Current" ],
            0x0104: [ req:"opt", acc:"r--", name:"DC Current Min" ],
            0x0105: [ req:"opt", acc:"r--", name:"DC Current Max" ],
            0x0106: [ req:"opt", acc:"r--", name:"DC Power" ],
            0x0107: [ req:"opt", acc:"r--", name:"DC Power Min" ],
            0x0108: [ req:"opt", acc:"r--", name:"DC Power Max" ],
            
            0x0200: [ req:"opt", acc:"r--", name:"DC Voltage Multiplier" ],
            0x0201: [ req:"opt", acc:"r--", name:"DC Voltage Divisor" ],
            0x0202: [ req:"opt", acc:"r--", name:"DC Current Multiplier" ],
            0x0203: [ req:"opt", acc:"r--", name:"DC Current Divisor" ],
            0x0204: [ req:"opt", acc:"r--", name:"DC Power Multiplier" ],
            0x0205: [ req:"opt", acc:"r--", name:"DC Power Divisor" ],
            
            0x0300: [ req:"opt", acc:"r--", name:"ACFrequency" ],
            0x0301: [ req:"opt", acc:"r--", name:"ACFrequencyMin" ],
            0x0302: [ req:"opt", acc:"r--", name:"ACFrequencyMax" ],
            0x0303: [ req:"opt", acc:"r--", name:"NeutralCurrent" ],
            0x0304: [ req:"opt", acc:"r--", name:"TotalActivePower" ],
            0x0305: [ req:"opt", acc:"r--", name:"TotalReactivePower" ],
            0x0306: [ req:"opt", acc:"r--", name:"TotalApparentPower" ],
            
            0x0400: [ req:"opt", acc:"r--", name:"AC Frequency Multiplier" ],
            0x0401: [ req:"opt", acc:"r--", name:"AC Frequency Divisor" ],
            0x0402: [ req:"opt", acc:"r--", name:"Power Multiplier" ],
            0x0403: [ req:"opt", acc:"r--", name:"Power Divisor" ],
            0x0404: [ req:"opt", acc:"r--", name:"Harmonic Current Multiplier" ],
            0x0405: [ req:"opt", acc:"r--", name:"Phase Harmonic Current Multiplier" ],
            
            0x0500: [ req:"opt", acc:"r--", name:"Reserved" ],
            0x0501: [ req:"opt", acc:"r--", name:"Line Current" ],
            0x0502: [ req:"opt", acc:"r--", name:"Active Current" ],
            0x0503: [ req:"opt", acc:"r--", name:"Reactive Current" ],
            0x0505: [ req:"opt", acc:"r--", name:"RMS Voltage" ],
            0x0506: [ req:"opt", acc:"r--", name:"RMS Voltag eMin" ],
            0x0507: [ req:"opt", acc:"r--", name:"RMS Voltage Max" ],
            0x0508: [ req:"opt", acc:"r--", name:"RMS Current" ],
            0x0509: [ req:"opt", acc:"r--", name:"RMS Current Min" ],
            0x050A: [ req:"opt", acc:"r--", name:"RMS Current Max" ],
            0x050B: [ req:"opt", acc:"r--", name:"Active Power" ],
            0x050C: [ req:"opt", acc:"r--", name:"Active Power Min" ],
            0x050D: [ req:"opt", acc:"r--", name:"Active Power Max" ],
            0x050E: [ req:"opt", acc:"r--", name:"Reactive Power" ],
            0x050F: [ req:"opt", acc:"r--", name:"Apparent Power" ],
            0x0510: [ req:"opt", acc:"r--", name:"Power Factor" ],
            0x0511: [ req:"opt", acc:"rw-", name:"Average RMS Voltage Measurement Period" ],
            0x0512: [ req:"opt", acc:"rw-", name:"Average RMS Over Voltage Counter" ],
            0x0513: [ req:"opt", acc:"rw-", name:"Average RMS Under Voltage Counter" ],
            0x0514: [ req:"opt", acc:"rw-", name:"RMS Extreme Over Voltage Period" ],
            0x0515: [ req:"opt", acc:"rw-", name:"RMS Extreme Under Voltage Period" ],
            0x0516: [ req:"opt", acc:"rw-", name:"RMS Voltage Sag Period" ],
            0x0517: [ req:"opt", acc:"rw-", name:"RMS Voltage Swell Period" ],

            0x0600: [ req:"opt", acc:"r--", name:"AC Voltage Multiplier" ],
            0x0601: [ req:"opt", acc:"r--", name:"AC Voltage Divisor" ],
            0x0602: [ req:"opt", acc:"r--", name:"AC Current Multiplier" ],
            0x0603: [ req:"opt", acc:"r--", name:"AC Current Divisor" ],
            0x0604: [ req:"opt", acc:"r--", name:"AC Power Multiplier" ],
            0x0605: [ req:"opt", acc:"r--", name:"AC Power Divisor" ],

            0x0700: [ req:"opt", acc:"rw-", name:"DC Overload Alarms Mask" ],
            0x0701: [ req:"opt", acc:"r--", name:"DC Voltage Overload" ],
            0x0702: [ req:"opt", acc:"r--", name:"DC Current Overload" ],

            0x0800: [ req:"opt", acc:"rw-", name:"AC Alarms Mask" ],
            0x0801: [ req:"opt", acc:"r--", name:"AC Voltage Overload" ],
            0x0802: [ req:"opt", acc:"r--", name:"AC Current Overload" ],
            0x0803: [ req:"opt", acc:"r--", name:"AC Active Power Overload" ],
            0x0804: [ req:"opt", acc:"r--", name:"AC Reactive Power Overload" ],
            0x0805: [ req:"opt", acc:"r--", name:"Average RMS Over Voltage" ],
            0x0806: [ req:"opt", acc:"r--", name:"Average RMS Under Voltage" ],
            0x0807: [ req:"opt", acc:"rw-", name:"RMS Extreme Over Voltage" ],
            0x0808: [ req:"opt", acc:"rw-", name:"RMS Extreme Unde rVoltage" ],
            0x0809: [ req:"opt", acc:"rw-", name:"RMS Voltage Sag" ],
            0x080A: [ req:"opt", acc:"rw-", name:"RMS Voltage Swell" ],

            0x0901: [ req:"opt", acc:"r--", name:"Line Current PhB" ],
            0x0902: [ req:"opt", acc:"r--", name:"Active Current PhB" ],
            0x0903: [ req:"opt", acc:"r--", name:"Reactive Current PhB" ],
            0x0905: [ req:"opt", acc:"r--", name:"RMS Voltage PhB" ],
            0x0906: [ req:"opt", acc:"r--", name:"RMS Voltage Min PhB" ],
            0x0907: [ req:"opt", acc:"r--", name:"RMS Voltage Max PhB" ],
            0x0908: [ req:"opt", acc:"r--", name:"RMS Current PhB" ],
            0x0909: [ req:"opt", acc:"r--", name:"RMS Current Min PhB" ],
            0x090A: [ req:"opt", acc:"r--", name:"RMS Current Max PhB" ],
            0x090B: [ req:"opt", acc:"r--", name:"Active Power PhB" ],
            0x090C: [ req:"opt", acc:"r--", name:"Active PowerMin PhB" ],
            0x090D: [ req:"opt", acc:"r--", name:"Active PowerMax PhB" ],
            0x090E: [ req:"opt", acc:"r--", name:"Reactive Power PhB" ],
            0x090F: [ req:"opt", acc:"r--", name:"Apparent Power PhB" ],
            0x0910: [ req:"opt", acc:"r--", name:"Power Factor PhB" ],
            0x0911: [ req:"opt", acc:"rw-", name:"Average RMS Voltage Measurement Period PhB" ],
            0x0912: [ req:"opt", acc:"rw-", name:"Average RMS Over Voltage Counter PhB" ],
            0x0913: [ req:"opt", acc:"rw-", name:"Average RMS Under Voltage Counter PhB" ],
            0x0914: [ req:"opt", acc:"rw-", name:"RMS Extreme Over Voltage Period PhB" ],
            0x0915: [ req:"opt", acc:"rw-", name:"RMS Extreme Under Voltage Period PhB" ],
            0x0916: [ req:"opt", acc:"rw-", name:"RMS Voltage Sag Period PhB" ],
            0x0917: [ req:"opt", acc:"rw-", name:"RMS Voltage Swell Period PhB" ],

            0x0A01: [ req:"opt", acc:"r--", name:"Line Current PhC" ],
            0x0A02: [ req:"opt", acc:"r--", name:"Active Current PhC" ],
            0x0A03: [ req:"opt", acc:"r--", name:"Reactive Current PhC" ],
            0x0A05: [ req:"opt", acc:"r--", name:"RMS Voltage PhC" ],
            0x0A06: [ req:"opt", acc:"r--", name:"RMS Voltage Min PhC" ],
            0x0A07: [ req:"opt", acc:"r--", name:"RMS Voltage Max PhC" ],
            0x0A08: [ req:"opt", acc:"r--", name:"RMS Current PhC" ],
            0x0A09: [ req:"opt", acc:"r--", name:"RMS Current Min PhC" ],
            0x0A0A: [ req:"opt", acc:"r--", name:"RMS Current Max PhC" ],
            0x0A0B: [ req:"opt", acc:"r--", name:"Active Power PhC" ],
            0x0A0C: [ req:"opt", acc:"r--", name:"Active Power Min PhC" ],
            0x0A0D: [ req:"opt", acc:"r--", name:"Active Power Max PhC" ],
            0x0A0E: [ req:"opt", acc:"r--", name:"Reactive Power PhC" ],
            0x0A0F: [ req:"opt", acc:"r--", name:"Apparent Power PhC" ],
            0x0A10: [ req:"opt", acc:"r--", name:"Power Factor PhC" ],
            0x0A11: [ req:"opt", acc:"rw-", name:"Average RMS Voltage Measurement Period PhC" ],
            0x0A12: [ req:"opt", acc:"rw-", name:"Average RMS Over Voltage Counter PhC" ],
            0x0A13: [ req:"opt", acc:"rw-", name:"Average RMS Under Voltage Counter PhC" ],
            0x0A14: [ req:"opt", acc:"rw-", name:"RMS Extreme Over Voltage Period PhC" ],
            0x0A15: [ req:"opt", acc:"rw-", name:"RMS Extreme Under Voltage Period PhC" ],
            0x0A16: [ req:"opt", acc:"rw-", name:"RMS Voltage Sag Period PhC" ],
            0x0A17: [ req:"opt", acc:"rw-", name:"RMS Voltage Swell Period PhC" ]
        ],
        commands: [
            0x00: [ req:"opt", name:"Get Profile Info Response" ],
            0x01: [ req:"opt", name:"Get Measurement Profile Response" ]
        ]
    ],
    0x0B05: [
        name: "Diagnostics Cluster",
        attributes: [
            0x0000: [ req:"opt", acc:"r--", name:"Number Of Resets" ],
            0x0001: [ req:"opt", acc:"r--", name:"Persistent Memory Writes" ],

            0x0100: [ req:"opt", acc:"r--", name:"Mac Rx Bcast" ],
            0x0101: [ req:"opt", acc:"r--", name:"Mac Tx Bcast" ],
            0x0102: [ req:"opt", acc:"r--", name:"Mac Rx Ucast" ],
            0x0103: [ req:"opt", acc:"r--", name:"Mac Tx Ucast" ],
            0x0104: [ req:"opt", acc:"r--", name:"Mac Tx Ucast Retry" ],
            0x0105: [ req:"opt", acc:"r--", name:"Mac Tx Ucast Fail" ],
            0x0106: [ req:"opt", acc:"r--", name:"APS Rx Bcast" ],
            0x0107: [ req:"opt", acc:"r--", name:"APS Tx Bcast" ],
            0x0108: [ req:"opt", acc:"r--", name:"APS Rx Ucast" ],
            0x0109: [ req:"opt", acc:"r--", name:"APS Tx Ucast Success" ],
            0x010A: [ req:"opt", acc:"r--", name:"APS Tx Ucast Retry" ],
            0x010B: [ req:"opt", acc:"r--", name:"APS Tx Ucast Fail" ],
            0x010C: [ req:"opt", acc:"r--", name:"Route Disc Initiated" ],
            0x010D: [ req:"opt", acc:"r--", name:"Neighbor Added" ],
            0x010E: [ req:"opt", acc:"r--", name:"Neighbor Removed" ],
            0x010F: [ req:"opt", acc:"r--", name:"Neighbor Stale" ],
            0x0110: [ req:"opt", acc:"r--", name:"Join Indication" ],
            0x0111: [ req:"opt", acc:"r--", name:"Child Moved" ],
            0x0112: [ req:"opt", acc:"r--", name:"NWK FC Failure" ],
            0x0113: [ req:"opt", acc:"r--", name:"APS FC Failure" ],
            0x0114: [ req:"opt", acc:"r--", name:"APS Unauthorized Key" ],
            0x0115: [ req:"opt", acc:"r--", name:"NWK Decrypt Failures" ],
            0x0116: [ req:"opt", acc:"r--", name:"APS Decrypt Failures" ],
            0x0117: [ req:"opt", acc:"r--", name:"Packet Buffer Allocate Failures" ],
            0x0118: [ req:"opt", acc:"r--", name:"Relayed Ucast" ],
            0x0119: [ req:"opt", acc:"r--", name:"Phyto MAC Queue Limit Reached" ],
            0x011A: [ req:"opt", acc:"r--", name:"Packet Validate Drop Count" ],
            0x011B: [ req:"opt", acc:"r--", name:"Average MAC Retry Per APS Message Sent" ],
            0x011C: [ req:"opt", acc:"r--", name:"Last Message LQI" ],
            0x011D: [ req:"opt", acc:"r--", name:"Last Message RSSI" ]
        ]
    ],
    0x1000: [
        name: "ZLL/Touchlink Commissioning Cluster",
        commands: [
            0x00: [ req:"req", name:"Scan" ],
            0x01: [ req:"req", name:"Scan Response" ],
            0x02: [ req:"req", name:"Device Information" ],
            0x03: [ req:"req", name:"Device Information Response" ],
            0x06: [ req:"req", name:"Identify" ],
            0x07: [ req:"req", name:"Reset to Factory New" ],
            0x10: [ req:"req", name:"Network Start Request" ],
            0x11: [ req:"req", name:"Network Start Response" ],
            0x12: [ req:"req", name:"Network Join Router Request" ],
            0x13: [ req:"req", name:"Network Join Router Response" ],
            0x14: [ req:"req", name:"Network Join End-Device Request" ],
            0x15: [ req:"req", name:"Network Join End-Device Response" ],
            0x16: [ req:"req", name:"Network Update" ],
            0x40: [ req:"opt", name:"Endpoint Information" ],
            0x41: [ req:"opt", name:"Get Group Identifiers" ],
            0x42: [ req:"opt", name:"Get Endpoint List" ]
        ]
    ],
    0xFC7D: [
        name: "IKEA Air Purifier - mfg: 0x117C",
        attributes: [
            0x0000: [ req:"req", acc:"r-p", name:"Filter Run Time" ],
            0x0001: [ req:"req", acc:"r-p", name:"Replace Filter" ],
            0x0002: [ req:"req", acc:"rwp", name:"Filter Life Time" ],
            0x0003: [ req:"req", acc:"rwp", name:"Disable Panel Lights" ],
            0x0004: [ req:"req", acc:"r-p", name:"PM 2.5 Measurement" ],
            0x0005: [ req:"req", acc:"rwp", name:"Child Lock" ],
            0x0006: [ req:"req", acc:"rwp", name:"Fan Mode" ],
            0x0007: [ req:"req", acc:"r-p", name:"Fan Speed" ],
            0x0008: [ req:"req", acc:"r-p", name:"Device Run Time" ]
        ]
    ]
]
