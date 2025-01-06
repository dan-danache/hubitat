{{!--------------------------------------------------------------------------}}
{{# @fields }}

// Fields for devices.Xiaomi_LYWSD03MMC
import java.math.RoundingMode
{{/ @fields }}
{{!--------------------------------------------------------------------------}}
{{# @inputs }}

// Inputs for devices.Xiaomi_LYWSD03MMC
input(
    name:'temperatureCalibration', type:'number', title:'Temperature calibration', required:true,
    description:'Temperature calibration offset (range -10.00°C .. 10.00°C)',
    range:'-10..10',
    defaultValue:0.00
)
input(
    name:'humidityCalibration', type:'number', title:'Humidity calibration', required:true,
    description:'Humidity calibration offset (range -10.00% .. 10.00%)',
    range:'-10..10',
    defaultValue:0.00
)
input(
    name:'enableDisplay', type:'bool', title:'Enable device display', required:true,
    description:'Keep device display on',
    defaultValue:true
)
input (
    name:'measurementInterval', type:'number', title:'Measurement interval', required:true,
    description:'Measurement interval (range 3 - 255 seconds, default 10 seconds)',
    range:'3..255',
    defaultValue: 10
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
cmds += zigbee.writeAttribute(0x0402, 0x0010, DataType.INT16, temperatureDelta)  // devbis/z03mmc
cmds += zigbee.writeAttribute(0x0204, 0x0100, DataType.INT16, temperatureDelta)  // pvvx/ZigbeeTLc

if (humidityCalibration == null) {
    humidityCalibration = '0.00'
    device.updateSetting 'humidityCalibration', [value:humidityCalibration, type:'number']
}
log_info "🛠️ humidityCalibration = ${humidityCalibration}"
Integer humidityDelta = (new BigDecimal(humidityCalibration) * 100).intValue()
cmds += zigbee.writeAttribute(0x0405, 0x0010, DataType.INT16, humidityDelta)  // devbis/z03mmc
cmds += zigbee.writeAttribute(0x0204, 0x0101, DataType.INT16, humidityDelta)  // pvvx/ZigbeeTLc

if (enableDisplay == null) {
    enableDisplay = true
    device.updateSetting 'enableDisplay', [value:enableDisplay, type:'bool']
}
log_info "🛠️ enableDisplay = ${enableDisplay}"
cmds += zigbee.writeAttribute(0x0204, 0x0011, DataType.BOOLEAN, enableDisplay ? 0x01 : 0x00)  // devbis/z03mmc
cmds += zigbee.writeAttribute(0x0204, 0x0106, DataType.ENUM8, enableDisplay ? 0x00 : 0x01)    // pvvx/ZigbeeTLc

if (showSmiley == null) {
    showSmiley = true
    device.updateSetting 'showSmiley', [value:showSmiley, type:'bool']
}
log_info "🛠️ showSmiley = ${showSmiley}"
cmds += zigbee.writeAttribute(0x0204, 0x0010, DataType.BOOLEAN, showSmiley ? 0x01 : 0x00)  // devbis/z03mmc
cmds += zigbee.writeAttribute(0x0204, 0x0002, DataType.ENUM8, showSmiley ? 0x00 : 0x01)    // pvvx/ZigbeeTLc

if (measurementInterval == null) {
    measurementInterval = 10
    device.updateSetting 'measurementInterval', [value:measurementInterval, type:'number']
}
log_info "🛠️️ measurementInterval = ${measurementInterval}"
Integer measurementInterval = new BigDecimal(measurementInterval).intValue()
cmds += zigbee.writeAttribute(0x0204, 0x0107, DataType.UINT8, measurementInterval)  // pvvx/ZigbeeTLc
{{/ @updated }}
{{!--------------------------------------------------------------------------}}
{{# @refresh }}

// Refresh for devices.Xiaomi_LYWSD03MMC
cmds += zigbee.readAttribute(0x0402, 0x0010) // TemperatureCalibration (devbis/z03mmc)
cmds += zigbee.readAttribute(0x0405, 0x0010) // HumidityCalibration (devbis/z03mmc)
cmds += zigbee.readAttribute(0x0204, 0x0011) // EnableDisplay (devbis/z03mmc)
cmds += zigbee.readAttribute(0x0204, 0x0010) // ShowSmiley (devbis/z03mmc)

cmds += zigbee.readAttribute(0x0204, 0x0100) // TemperatureCalibration (pvvx/ZigbeeTLc)
cmds += zigbee.readAttribute(0x0204, 0x0101) // HumidityCalibration (pvvx/ZigbeeTLc)
cmds += zigbee.readAttribute(0x0204, 0x0106) // EnableDisplay (pvvx/ZigbeeTLc)
cmds += zigbee.readAttribute(0x0204, 0x0002) // ShowSmiley (pvvx/ZigbeeTLc)
cmds += zigbee.readAttribute(0x0204, 0x0107) // MeasurementInterval (pvvx/ZigbeeTLc)
{{/ @refresh }}
{{!--------------------------------------------------------------------------}}
{{# @events }}

// Events for devices.Xiaomi_LYWSD03MMC
// ===================================================================================================================

// Read Attributes: TemperatureCalibration
case { contains it, [clusterInt:0x0402, commandInt:0x01, attrInt:0x0010] }:  // devbis/z03mmc
case { contains it, [clusterInt:0x0204, commandInt:0x01, attrInt:0x0100] }:  // pvvx/ZigbeeTLc
    temperatureCalibration = new BigDecimal(Integer.parseInt(msg.value, 16) / 100.0d).setScale(2, RoundingMode.HALF_UP).toPlainString()
    device.updateSetting 'temperatureCalibration', [value:temperatureCalibration, type:'number']
    utils_processedZclMessage 'Read Attributes Response', "TemperatureCalibration=${msg.value} (${temperatureCalibration}°C)"
    return

// Read Attributes: HumidityCalibration
case { contains it, [clusterInt:0x0405, commandInt:0x01, attrInt:0x0010] }:  // devbis/z03mmc
case { contains it, [clusterInt:0x0204, commandInt:0x01, attrInt:0x0101] }:  // pvvx/ZigbeeTLc
    humidityCalibration = new BigDecimal(Integer.parseInt(msg.value, 16) / 100.0d).setScale(2, RoundingMode.HALF_UP).toPlainString()
    device.updateSetting 'humidityCalibration', [value:humidityCalibration, type:'number']
    utils_processedZclMessage 'Read Attributes Response', "HumidityCalibration=${msg.value} (${humidityCalibration}%)"
    return

// Read Attributes: EnableDisplay
case { contains it, [clusterInt:0x0204, commandInt:0x01, attrInt:0x0011] }:  // devbis/z03mmc
case { contains it, [clusterInt:0x0204, commandInt:0x01, attrInt:0x0106] }:  // pvvx/ZigbeeTLc
    enableDisplay = msg.value == (attrInt == 0x0011 ? '01' : '00')
    device.updateSetting 'enableDisplay', [value:enableDisplay, type:'bool']
    utils_processedZclMessage 'Read Attributes Response', "EnableDisplay=${msg.value} (${enableDisplay})"
    return

// Read Attributes: ShowSmiley
case { contains it, [clusterInt:0x0204, commandInt:0x01, attrInt:0x0010] }:  // devbis/z03mmc
case { contains it, [clusterInt:0x0204, commandInt:0x01, attrInt:0x0002] }:  // pvvx/ZigbeeTLc
    showSmiley = msg.value == (attrInt == 0x0010 ? '01' : '00')
    device.updateSetting 'showSmiley', [value:showSmiley, type:'bool']
    utils_processedZclMessage 'Read Attributes Response', "ShowSmiley=${msg.value} (${showSmiley})"
    return

// Read Attributes: MeasurementInterval (pvvx/ZigbeeTLc only)
case { contains it, [clusterInt:0x0204, commandInt:0x01, attrInt:0x0107] }:
    device.updateSetting 'measurementInterval', [value:measurementInterval, type:'number']
    utils_processedZclMessage 'Read Attributes Response', "MeasurementInterval=${msg.value} (${measurementInterval})"
    return

// Write Attributes Response
case { contains it, [endpointInt:0x01, commandInt:0x04, isClusterSpecific:false] }:
    utils_processedZclMessage 'Write Attributes Response', "cluster=0x${msg.clusterId}"
    return

// Read Attributes Response (invalid attribute)
case { contains it, [endpointInt:0x01, commandInt:0x01, isClusterSpecific:false] }:
    utils_processedZclMessage 'Read Attributes Response', "cluster=0x${msg.clusterId}"
    return
{{/ @events }}
{{!--------------------------------------------------------------------------}}
