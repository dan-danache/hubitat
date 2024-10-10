{{!--------------------------------------------------------------------------}}
{{# @fields }}

// Fields for devices.Xiaomi_LYWSD03MMC
import java.math.RoundingMode
{{/ @fields }}
{{!--------------------------------------------------------------------------}}
{{# @inputs }}

// Inputs for devices.Xiaomi_LYWSD03MMC
input(
    name: 'temperatureCalibration', type: 'number',
    title: 'Temperature calibration',
    description: '<small>Temperature calibration offset. Range -10.00°C .. 10.00°C.</small>',
    defaultValue: 0.00,
    range: '-10..10',
    required: true
)
input(
    name: 'humidityCalibration', type: 'number',
    title: 'Humidity calibration',
    description: '<small>Humidity calibration offset. Range -10.00% .. 10.00%.</small>',
    defaultValue: 0.00,
    range: '-10..10',
    required: true
)
input(
    name: 'enableDisplay', type: 'bool',
    title: 'Enable device display',
    description: '<small>Turn device display on.</small>',
    defaultValue: true,
    required: true
)
input(
    name: 'showSmiley', type: 'bool',
    title: 'Show smiley',
    description: '<small>Show the smiley on the device screen.</small>',
    defaultValue: true,
    required: true
)
{{/ @inputs }}
{{!--------------------------------------------------------------------------}}
{{# @updated }}

// Sync device temperature unit with Hubitat configuration
log_info "🛠️ temperatureUnit = °${location.temperatureScale}"
cmds += zigbee.writeAttribute(0x0204, 0x0000, DataType.ENUM8, location.temperatureScale == 'C' ? 0x00 : 0x01)

// Preferences for devices.Xiaomi_LYWSD03MMC
if (temperatureCalibration == null) {
    temperatureCalibration = '0.00'
    device.updateSetting 'temperatureCalibration', [value:temperatureCalibration, type:'number']
}
log_info "🛠️ temperatureCalibration = ${temperatureCalibration}"
Integer temperatureDelta = (new BigDecimal(temperatureCalibration) * 100).intValue()
cmds += zigbee.writeAttribute(0x0402, 0x0010, DataType.INT16, temperatureDelta)

if (humidityCalibration == null) {
    humidityCalibration = '0.00'
    device.updateSetting 'humidityCalibration', [value:humidityCalibration, type:'number']
}
log_info "🛠️ humidityCalibration = ${humidityCalibration}"
Integer humidityDelta = (new BigDecimal(humidityCalibration) * 100).intValue()
cmds += zigbee.writeAttribute(0x0405, 0x0010, DataType.INT16, humidityDelta)

if (enableDisplay == null) {
    enableDisplay = true
    device.updateSetting 'enableDisplay', [value:enableDisplay, type:'bool']
}
log_info "🛠️ enableDisplay = ${enableDisplay}"
cmds += zigbee.writeAttribute(0x0204, 0x0011, DataType.BOOLEAN, enableDisplay ? 0x01 : 0x00)

if (showSmiley == null) {
    showSmiley = true
    device.updateSetting 'showSmiley', [value:showSmiley, type:'bool']
}
log_info "🛠️ showSmiley = ${showSmiley}"
cmds += zigbee.writeAttribute(0x0204, 0x0010, DataType.BOOLEAN, showSmiley ? 0x01 : 0x00)
{{/ @updated }}
{{!--------------------------------------------------------------------------}}
{{# @refresh }}

// Refresh for devices.Xiaomi_LYWSD03MMC
cmds += zigbee.readAttribute(0x0402, 0x0010) // TemperatureCalibration
cmds += zigbee.readAttribute(0x0405, 0x0010) // HumidityCalibration
cmds += zigbee.readAttribute(0x0204, 0x0011) // EnableDisplay
cmds += zigbee.readAttribute(0x0204, 0x0010) // ShowSmiley
{{/ @refresh }}
{{!--------------------------------------------------------------------------}}
{{# @events }}

// Events for devices.Xiaomi_LYWSD03MMC
// ===================================================================================================================

// Read Attributes: TemperatureCalibration
case { contains it, [clusterInt:0x0402, commandInt:0x01, attrInt:0x0010] }:
    temperatureCalibration = new BigDecimal(Integer.parseInt(msg.value, 16) / 100.0d).setScale(2, RoundingMode.HALF_UP).toPlainString()
    device.updateSetting 'temperatureCalibration', [value:temperatureCalibration, type:'number']
    utils_processedZclMessage 'Read Attributes Response', "TemperatureCalibration=${msg.value} (${temperatureCalibration}°C)"
    return

// Read Attributes: HumidityCalibration
case { contains it, [clusterInt:0x0405, commandInt:0x01, attrInt:0x0010] }:
    humidityCalibration = new BigDecimal(Integer.parseInt(msg.value, 16) / 100.0d).setScale(2, RoundingMode.HALF_UP).toPlainString()
    device.updateSetting 'humidityCalibration', [value:humidityCalibration, type:'number']
    utils_processedZclMessage 'Read Attributes Response', "HumidityCalibration=${msg.value} (${humidityCalibration}%)"
    return

// Read Attributes: EnableDisplay
case { contains it, [clusterInt:0x0204, commandInt:0x01, attrInt:0x0011] }:
    enableDisplay = msg.value == '01' ? true : false
    device.updateSetting 'enableDisplay', [value:enableDisplay, type:'bool']
    utils_processedZclMessage 'Read Attributes Response', "EnableDisplay=${msg.value} (${enableDisplay})"
    return

// Read Attributes: ShowSmiley
case { contains it, [clusterInt:0x0204, commandInt:0x01, attrInt:0x0010] }:
    showSmiley = msg.value == '01' ? true : false
    device.updateSetting 'showSmiley', [value:showSmiley, type:'bool']
    utils_processedZclMessage 'Read Attributes Response', "ShowSmiley=${msg.value} (${showSmiley})"
    return

// Write Attributes Response
case { contains it, [endpointInt:0x01, commandInt:0x04, isClusterSpecific:false] }:
    utils_processedZclMessage 'Write Attributes Response', "cluster=0x${msg.clusterId}"
    return
{{/ @events }}
{{!--------------------------------------------------------------------------}}
