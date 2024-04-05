{{!--------------------------------------------------------------------------}}
{{# @definition }}
capability 'Sensor'
capability 'TemperatureMeasurement'
{{/ @definition }}
{{!--------------------------------------------------------------------------}}
{{# @attributes }}

// Attributes for devices.Aqara_DCM-K01
attribute 'powerOutageCount', 'number'
{{/ @attributes }}
{{!--------------------------------------------------------------------------}}
{{# @inputs }}

// Inputs for devices.Aqara_DCM-K01
input(
    name: 'switchType', type: 'enum',
    title: 'Switch type',
    description: '<small>What type of switches are connected to S1 and S2.</small>',
    options: [
        '1':'Latching switch - toggle/rocker',
        '2':'Momentary switch - push button',
        '3':'Disabled - connected switches are ignored',
    ],
    defaultValue: '1',
    required: true
)
input(
    name: 'operationModeS1', type: 'enum',
    title: 'Operation mode for Switch S1',
    description: '<small>What happens when Switch S1 is used.</small>',
    options: [
        '1':'Standard - Switch S1 controls Relay L1',
        '0':'Decoupled - Switch S1 only sends button events',
    ],
    defaultValue: '1',
    required: true
)
input(
    name: 'operationModeS2', type: 'enum',
    title: 'Operation mode for Switch S2',
    description: '<small>What happens when Switch S2 is used.</small>',
    options: [
        '1':'Standard - Switch S2 controls Relay L2',
        '0':'Decoupled - Switch S2 only sends button events',
    ],
    defaultValue: '1',
    required: true
)
input(
    name: 'relayMode', type: 'enum',
    title: 'Relay mode',
    description: '<small>How Relay L1 and Relay L2 operate.</small>',
    options: [
        '0':'Wet contact - connect L to L1, L2 (jumper wire installed)',
        '3':'Dry contact - connect LOUT to L1, L2 (no jumper wire)',
        '1':'Pulse - temporary connect LOUT to L1, L2 (no jumper wire)',
    ],
    defaultValue: '0',
    required: true
)
if ("${relayMode}" == '1') {
    input(
        name: 'pulseDuration', type: 'number',
        title: 'Pulse duration',
        description: '<small>Only when Relay mode is Pulse. Range 200ms .. 2000ms.</small>',
        defaultValue: 1000,
        range: '200..2000',
        required: true
    )
}
input(
    name: 'interlock', type: 'enum',
    title: 'Interlock',
    description: '<small>Prevent both Relay L1 and Relay L2 being On at the same time.</small>',
    options: [
        '0':'Disabled - control lights and other devices',
        '1':'Enabled - control bi-directional motors',
    ],
    defaultValue: '0',
    required: true
)
input(
    name: 'powerOnBehavior', type: 'enum',
    title: 'Power On behaviour',
    description: '<small>What happens after a power outage.</small>',
    options: [
        '0': 'Turn power On',
        '2': 'Turn power Off',
        '1': 'Restore previous state'
    ],
    defaultValue: '1',
    required: true
)
{{/ @inputs }}
{{!--------------------------------------------------------------------------}}
{{# @configure }}

// Configuration for devices.Aqara_DCM-K01
cmds += "zdo bind 0x${device.deviceNetworkId} 0x01 0x01 0x0012 {${device.zigbeeId}} {}" // Multistate Input cluster (ep 0x01)
cmds += "zdo bind 0x${device.deviceNetworkId} 0x02 0x01 0x0012 {${device.zigbeeId}} {}" // Multistate Input cluster (ep 0x02)
{{/ @configure }}
{{!--------------------------------------------------------------------------}}
{{# @updated }}

// Preferences for devices.Aqara_DCM-K01
if (powerOnBehavior == null) {
    powerOnBehavior = '1'
    device.updateSetting 'powerOnBehavior', [value:powerOnBehavior, type:'enum']
}
log_info "🛠️ powerOnBehavior = ${powerOnBehavior}"
cmds += zigbee.writeAttribute(0xFCC0, 0x0517, 0x20, Integer.parseInt(powerOnBehavior),  [mfgCode:'0x115F', destEndpoint:0x01])

if (operationModeS1 == null) {
    operationModeS1 = '1'
    device.updateSetting 'operationModeS1', [value:operationModeS1, type:'enum']
}
log_info "🛠️ operationModeS1 = ${operationModeS1}"
cmds += zigbee.writeAttribute(0xFCC0, 0x0200, 0x20, Integer.parseInt(operationModeS1), [mfgCode:'0x115F', destEndpoint:0x01])

if (operationModeS2 == null) {
    operationModeS2 = '1'
    device.updateSetting 'operationModeS2', [value:operationModeS2, type:'enum']
}
log_info "🛠️ operationModeS2 = ${operationModeS2}"
cmds += zigbee.writeAttribute(0xFCC0, 0x0200, 0x20, Integer.parseInt(operationModeS2), [mfgCode:'0x115F', destEndpoint:0x02])

if (switchType == null) {
    switchType = '1'
    device.updateSetting 'switchType', [value:switchType, type:'enum']
}
log_info "🛠️ switchType = ${switchType}"
cmds += zigbee.writeAttribute(0xFCC0, 0x000A, 0x20, Integer.parseInt(switchType), [mfgCode:'0x115F', destEndpoint:0x01])

if (interlock == null) {
    interlock = '0'
    device.updateSetting 'interlock', [value:interlock, type:'enum']
}
log_info "🛠️ interlock = ${interlock}"
cmds += zigbee.writeAttribute(0xFCC0, 0x02D0, 0x10, Integer.parseInt(interlock), [mfgCode:'0x115F', destEndpoint:0x01])

if (relayMode == null) {
    relayMode = '0'
    device.updateSetting 'relayMode', [value:relayMode, type:'enum']
}
log_info "🛠️ relayMode = ${relayMode}"
cmds += zigbee.writeAttribute(0xFCC0, 0x0289, 0x20, Integer.parseInt(relayMode), [mfgCode:'0x115F', destEndpoint:0x01])

if (relayMode == '1') {
    Integer pulseDurationInt = pulseDuration == null ? 2000 : pulseDuration.intValue()
    device.updateSetting 'pulseDuration', [value:pulseDurationInt, type:'number']

    log_info "🛠️ pulseDuration = ${pulseDurationInt}"
    cmds += zigbee.writeAttribute(0xFCC0, 0x00EB, 0x21, pulseDurationInt, [mfgCode:'0x115F', destEndpoint:0x01])
}
{{/ @updated }}
{{!--------------------------------------------------------------------------}}
{{# @refresh }}

// Refresh for devices.Aqara_DCM-K01
cmds += zigbee.readAttribute(0xFCC0, 0x00F7, [mfgCode: '0x115F']) // LumiSpecific
{{/ @refresh }}
{{!--------------------------------------------------------------------------}}
{{# @events }}

// Events for devices.Aqara_DCM-K01
// ===================================================================================================================

// Switch was flipped
case { contains it, [clusterInt:0x0012, commandInt:0x0A] }:
    List<String> button = msg.endpointInt == 0x01 ? BUTTONS.S1 : BUTTONS.S2
    utils_sendEvent name:'pushed', value:button[0], type:'physical', isStateChange:true, descriptionText:"Button ${button[0]} (${button[1]}) was pushed"
    return

// LumiSpecific
case { contains it, [clusterInt:0xFCC0, commandInt:0x01, attrInt:0x00F7] }:
case { contains it, [clusterInt:0xFCC0, commandInt:0x0A, attrInt:0x00F7] }:
    // https://github.com/Koenkk/zigbee-herdsman-converters/blob/cae265712bf75be74530d1c0458901f81c1adcc5/src/lib/lumi.ts#L166
    //   0 - 03 28 2A               3 = device_temperature
    //   6 - 05 21 23 00            5 = power_outage_count
    //  14 - 09 21 00 0D            9 = ??
    //  22 - 0A 21 99 84           10 = switch_type
    //  30 - 0C 20 0A              12 = ??
    //  36 - 0D 23 1B 00 00 00     13 = Overwrite version advertised by `genBasic` and `genOta` with correct version:
    //                                  - meta.device.meta.lumiFileVersion = value;
    //                                  - meta.device.softwareBuildID = trv.decodeFirmwareVersionString(value);
    //  48 - 11 23 01 00 00 00     17 = ??
    //  60 - 64 10 00             100 = relay 1 ??
    //  66 - 65 10 00             101 = relay 2 ??
    //  72 - 95 39 C1 CC B6 42    149 = energy / consumption
    //  84 - 96 39 9A A9 08 45    150 = voltage = value * 0.1;
    //  96 - 98 39 00 00 00 00    152 = power
    // 108 - 97 39 00 00 00 00    151 = current = value * 0.001;
    // 120 - 9A 20 00             154 = ??

    // cluster-specific, !manufacturer-specific, server-to-client, !disable-default-response = 09
    // utils_sendZigbeeCommands(["he raw 0x${device.deviceNetworkId} 0x01 0x01 0x0019 {0901 00 00 64}"])
    // utils_sendZigbeeCommands(["he raw 0x${device.deviceNetworkId} 0x01 0x01 0x0019 {0901 00 01 64 5F11}"])
    // utils_sendZigbeeCommands(["he raw 0x${device.deviceNetworkId} 0x01 0x01 0x0019 {0901 00 02 64 5F11 1019}"])
    // utils_sendZigbeeCommands(["he raw 0x${device.deviceNetworkId} 0x01 0x01 0x0019 {0901 00 03 64 5F11 1019 1B000000}"])
    //
    // Read required attribute 0x0000 (UpgradeServerID) or 0x0006 (ImageUpgradeStatus) of cluster 0x0019:
    // - !cluster-specific, !manufacturer-specific, server-to-client, !disable-default-response = 08
    // he raw .addr 0x01 0x01 0x0019 {0800 00 0000}             -- UpgradeServerID
    // he raw .addr 0x01 0x01 0x0019 {0800 00 0400}             -- DownloadedFileVersion (read)
    // he raw .addr 0x01 0x01 0x0019 {0843 02 0400 23 1E000000} -- DownloadedFileVersion (write -> read only!)
    // he raw .addr 0x01 0x01 0x0019 {0800 00 0600}             -- ImageUpgradeStatus
    // he raw .addr 0x01 0x01 0x0019 {0800 00 0700}             -- Manufacturer ID
    // he raw .addr 0x01 0x01 0x0019 {0800 00 0800}             -- Image Type ID
    //
    // Image Notify
    // - cluster-specific, !manufacturer-specific, server-to-client, disable-default-response = 19
    // := payload type = 0x03, jitter = 100 (0x064), mfg = 0x115F, imageType = 0x1910, fileVersion = 0x0000001E (30)
    // he raw .addr 0x01 0x01 0x0019 {0900 00 03 60 5F11 1019 1E000000}
    //
    // Query Next Image Response
    // - cluster-specific, !manufacturer-specific, server-to-client, disable-default-response = 19
    // := status, mfg = 0x115F, imageType = 0x1910, fileVersion = 0x0000001C (28), fileSize = 411786 bytes
    // he raw .addr 0x01 0x01 0x0019 {1900 02 00 5F11 1019 1C000000 8A480600}
    //
    // Image Block Response (status = 0x95:ABORT)
    // - cluster-specific, !manufacturer-specific, server-to-client, disable-default-response = 19
    // he raw .addr 0x01 0x01 0x0019 {1900 05 95}

    if (msg.value.size() != 126) return

    String temperature = convertTemperatureIfNeeded(Integer.parseInt(msg.value[4..5], 16), 'C', 0)
    utils_sendEvent name:'temperature', value:temperature, unit:"°${location.temperatureScale}", descriptionText:"Temperature is ${temperature} °${location.temperatureScale}", type:type

    Integer powerOutageCount = Integer.parseInt(msg.value[12..13] + msg.value[10..11], 16)
    utils_sendEvent name:'powerOutageCount', value:powerOutageCount, descriptionText:"Power outage count is ${powerOutageCount}", type:type

    String softwareBuild = '0.0.0_' + [
        "${Integer.parseInt(msg.value[44..45], 16)}",
        "${Integer.parseInt(msg.value[42..43], 16)}",
        "${Integer.parseInt(msg.value[40..41], 16)}"
    ].join('').padLeft(4, '0')
    utils_dataValue('softwareBuild', softwareBuild)

    Integer energy = Math.round(Float.intBitsToFloat(Integer.parseInt("${msg.value[82..83]}${msg.value[80..81]}${msg.value[78..79]}${msg.value[76..77]}", 16))) / 1000
    //utils_sendEvent name:'energy', value:energy, unit:'kWh', descriptionText:"Energy is ${energy} kWh", type:type

    Integer voltage = Math.round(Float.intBitsToFloat(Integer.parseInt("${msg.value[94..95]}${msg.value[92..93]}${msg.value[90..91]}${msg.value[88..89]}", 16))) / 10
    //utils_sendEvent name:'voltage', value:voltage, unit:'V', descriptionText:"Voltage is ${voltage} V", type:type

    Integer power = Math.round(Float.intBitsToFloat(Integer.parseInt("${msg.value[106..107]}${msg.value[104..105]}${msg.value[102..103]}${msg.value[100..101]}", 16)))
    //utils_sendEvent name:'power', value:power, unit:'W', descriptionText:"Power is ${power} W", type:type

    Integer amperage = Math.round(Float.intBitsToFloat(Integer.parseInt("${msg.value[118..119]}${msg.value[116..117]}${msg.value[114..115]}${msg.value[112..113]}", 16))) / 1000
    //utils_sendEvent name:'amperage', value:amperage, unit:'A', descriptionText:"Amperage is ${amperage} W", type:type

    utils_processedZclMessage "${msg.commandInt == 0x0A ? 'Report' : 'Read'} Attributes Response", "Temperature=${temperature}, PowerOutageCount=${powerOutageCount}, SoftwareBuild=${softwareBuild}, Energy=${energy}kWh, Voltage=${voltage}V, Power=${power}W, Amperage=${amperage}A"
    return

// Other events that we expect but are not usefull for devices.Aqara_DCM-K01 behavior
case { contains it, [clusterInt:0xFCC0, commandInt:0x07] }:
    utils_processedZclMessage 'Configure Reporting Response', "attribute=LumiSpecific, data=${msg.data}"
    return
case { contains it, [clusterInt:0xFCC0, commandInt:0x0A, attrInt:0x0517] }:
    utils_processedZclMessage 'Report Attributes Response', "PowerOnBehavior=${msg.value}"
    return
case { contains it, [clusterInt:0xFCC0, commandInt:0x0A, attrInt:0x0200] }:
    utils_processedZclMessage 'Report Attributes Response', "OperationMode=${msg.value}, Switch=${msg.endpoint}"
    return
case { contains it, [clusterInt:0xFCC0, commandInt:0x0A, attrInt:0x000A] }:
    utils_processedZclMessage 'Report Attributes Response', "switchType=${msg.value}"
    return
case { contains it, [clusterInt:0xFCC0, commandInt:0x0A, attrInt:0x02D0] }:
    utils_processedZclMessage 'Report Attributes Response', "Interlock=${msg.value}"
    return
case { contains it, [clusterInt:0xFCC0, commandInt:0x0A, attrInt:0x0289] }:
    utils_processedZclMessage 'Report Attributes Response', "RelayMode=${msg.value}"
    return
case { contains it, [clusterInt:0xFCC0, commandInt:0x0A, attrInt:0x00EB] }:
    utils_processedZclMessage 'Report Attributes Response', "pulseDuration=${msg.value}"
    return
case { contains it, [clusterInt:0xFCC0, commandInt:0x04] }:  // Write Attribute Response
    return
{{/ @events }}
{{!--------------------------------------------------------------------------}}
