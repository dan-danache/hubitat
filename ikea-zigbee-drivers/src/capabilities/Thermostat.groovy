{{!--------------------------------------------------------------------------}}
{{# @definition }}
capability 'TemperatureMeasurement'
capability 'ThermostatCoolingSetpoint'
capability 'ThermostatHeatingSetpoint'
capability 'ThermostatOperatingState'
capability 'ThermostatMode'
{{/ @definition }}
{{!--------------------------------------------------------------------------}}
{{# @fields }}
@Field static final Map<String, String> TH_MODES = ['00':'off', '03':'cool', '04':'heat']
@Field static final Map<String, String> TH_STATES = ['00':'idle', '01':'heating', '02':'cooling']
{{/ @fields }}
{{!--------------------------------------------------------------------------}}
{{# @attributes }}

// Attributes for capability.Thermostat
attribute 'supportedThermostatModes', 'JSON_OBJECT'
{{/ @attributes }}
{{!--------------------------------------------------------------------------}}
{{# @commands }}

// Commands for capability.Thermostat
command 'setThermostatMode', [[name:'Thermostat mode*', type:'ENUM', description:'Thermostat mode to set', constraints:TH_MODES.values()]]
{{/ @commands }}
{{!--------------------------------------------------------------------------}}
{{# @configure }}

// Configuration for capability.Thermostat
sendEvent name:'supportedThermostatModes', value:TH_MODES.values(), type:'digital', descriptionText:"Supported thermostat modes initialized to ${TH_MODES.values()}"
cmds += "zdo bind 0x${device.deviceNetworkId} 0x${device.endpointId} 0x01 0x0201 {${device.zigbeeId}} {}" // Thermostat cluster
cmds += "he cr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0201 0x0000 0x29 0x000F 0x0E10 {1400} {}" // Report LocalTemperature (int16) at most every 15 seconds, at least every 1 hour (Δ = 0.2°C)
{{/ @configure }}
{{!--------------------------------------------------------------------------}}
{{# @refresh }}

// Refresh for capability.Thermostat
cmds += zigbee.readAttribute(0x0201, 0x0000) // LocalTemperature
cmds += zigbee.readAttribute(0x0201, 0x0011) // OccupiedCoolingSetpoint
cmds += zigbee.readAttribute(0x0201, 0x0012) // OccupiedHeatingSetpoint
cmds += zigbee.readAttribute(0x0201, 0x001C) // SystemMode
cmds += zigbee.readAttribute(0x0201, 0x0029) // ThermostatRunningState
{{/ @refresh }}
{{!--------------------------------------------------------------------------}}
{{# @implementation }}

// Implementation for capability.Thermostat
void auto() {
    setThermostatMode 'auto'
}
void cool() {
    setThermostatMode 'cool'
}
void emergencyHeat() {
    setThermostatMode 'emergency heat'
}
void heat() {
    setThermostatMode 'heat'
}
void off() {
    setThermostatMode 'off'
}
void setThermostatMode(String mode) {
    log_debug "🎬 Setting thermostat mode to ${mode}"
    List<String> cmds = []
    switch (mode) {
        case 'off':
            cmds += zigbee.writeAttribute(0x0201, 0x001C, DataType.ENUM8, 0x00)
            break
        case 'cool':
            if (device.currentValue('thermostatMode', true) == 'off') {
                cmds += zigbee.writeAttribute(0x0201, 0x001B, DataType.ENUM8, 0x00)
                cmds += zigbee.writeAttribute(0x0201, 0x001C, DataType.ENUM8, 0x03)
            } else {
                cmds += zigbee.writeAttribute(0x0201, 0x001C, DataType.ENUM8, 0x00)
                cmds += zigbee.writeAttribute(0x0201, 0x001B, DataType.ENUM8, 0x00)
                runIn 5, 'systemMode', [data:0x03]
            }
            break
        case 'heat':
            if (device.currentValue('thermostatMode', true) == 'off') {
                cmds += zigbee.writeAttribute(0x0201, 0x001B, DataType.ENUM8, 0x02)
                cmds += zigbee.writeAttribute(0x0201, 0x001C, DataType.ENUM8, 0x04)
            } else {
                cmds += zigbee.writeAttribute(0x0201, 0x001C, DataType.ENUM8, 0x00)
                cmds += zigbee.writeAttribute(0x0201, 0x001B, DataType.ENUM8, 0x02)
                runIn 5, 'systemMode', [data:0x04]
            }
            break
        default:
            log_warn "Mode \"${mode}\" is not supported"
            return
    }
    utils_sendZigbeeCommands cmds
}
private void systemMode(Integer mode) {
    utils_sendZigbeeCommands(zigbee.writeAttribute(0x0201, 0x001C, DataType.ENUM8, mode))
}
void setCoolingSetpoint(BigDecimal temperature) {
    log_debug "🎬 Setting cooling setpoint to ${temperature}°${location.temperatureScale}"
    BigDecimal setpoint = "${location.temperatureScale}" == 'C' ? temperature : (temperature - 32) * 5 / 9
    utils_sendZigbeeCommands(zigbee.writeAttribute(0x0201, 0x0011, DataType.INT16, Integer.valueOf((setpoint * 100).intValue())))
}
void setHeatingSetpoint(BigDecimal temperature) {
    log_debug "🎬 Setting heating setpoint to ${temperature}°${location.temperatureScale}"
    BigDecimal setpoint = "${location.temperatureScale}" == 'C' ? temperature : (temperature - 32) * 5 / 9
    utils_sendZigbeeCommands(zigbee.writeAttribute(0x0201, 0x0012, DataType.INT16, Integer.valueOf((setpoint * 100).intValue())))
}
{{/ @implementation }}
{{!--------------------------------------------------------------------------}}
{{# @events }}

// Events for capability.Thermostat
// ===================================================================================================================

// Report/Read Attributes Reponse: LocalTemperature
case { contains it, [clusterInt:0x0201, commandInt:0x0A, attrInt:0x0000] }:
case { contains it, [clusterInt:0x0201, commandInt:0x01, attrInt:0x0000] }:

    // https://www.urbandictionary.com/define.php?term=Retard%20Unit
    String temperature = "${location.temperatureScale == 'C' ? Integer.parseInt(msg.value, 16) / 100 : Math.round((Integer.parseInt(msg.value, 16) * 0.018 + 32) * 100) / 100}"
    utils_sendEvent name:'temperature', value:temperature, unit:"°${location.temperatureScale}", descriptionText:"Local temperature is ${temperature}°${location.temperatureScale}", type:type
    utils_processedZclMessage "${msg.commandInt == 0x0A ? 'Report' : 'Read'} Attributes Response", "LocalTemperature=${msg.value}"
    return

// Report/Read Attributes Reponse: OccupiedCoolingSetpoint
case { contains it, [clusterInt:0x0201, commandInt:0x0A, attrInt:0x0011] }:
case { contains it, [clusterInt:0x0201, commandInt:0x01, attrInt:0x0011] }:

    // https://www.urbandictionary.com/define.php?term=Retard%20Unit
    String coolingSetpoint = "${location.temperatureScale == 'C' ? Integer.parseInt(msg.value, 16) / 100 : Math.round((Integer.parseInt(msg.value, 16) * 0.018 + 32) * 100) / 100}"
    utils_sendEvent name:'coolingSetpoint', value:coolingSetpoint, unit:"°${location.temperatureScale}", descriptionText:"Cooling setpoint is ${coolingSetpoint}°${location.temperatureScale}", type:type
    utils_processedZclMessage "${msg.commandInt == 0x0A ? 'Report' : 'Read'} Attributes Response", "OccupiedCoolingSetpoint=${msg.value}"

    utils_sendZigbeeCommands(zigbee.readAttribute(0x0201, 0x001C))
    return

// Report/Read Attributes Reponse: OccupiedHeatingSetpoint
case { contains it, [clusterInt:0x0201, commandInt:0x0A, attrInt:0x0012] }:
case { contains it, [clusterInt:0x0201, commandInt:0x01, attrInt:0x0012] }:

    // https://www.urbandictionary.com/define.php?term=Retard%20Unit
    String heatingSetpoint = "${location.temperatureScale == 'C' ? Integer.parseInt(msg.value, 16) / 100 : Math.round((Integer.parseInt(msg.value, 16) * 0.018 + 32) * 100) / 100}"
    utils_sendEvent name:'heatingSetpoint', value:heatingSetpoint, unit:"°${location.temperatureScale}", descriptionText:"Heating setpoint is ${heatingSetpoint}°${location.temperatureScale}", type:type
    utils_processedZclMessage "${msg.commandInt == 0x0A ? 'Report' : 'Read'} Attributes Response", "OccupiedHeatingSetpoint=${msg.value}"

    utils_sendZigbeeCommands(zigbee.readAttribute(0x0201, 0x001C))
    return

// Report/Read Attributes Reponse: SystemMode
case { contains it, [clusterInt:0x0201, commandInt:0x0A, attrInt:0x001C] }:
case { contains it, [clusterInt:0x0201, commandInt:0x01, attrInt:0x001C] }:
    String thermostatMode = TH_MODES[msg.value]
    utils_sendEvent name:'thermostatMode', value:thermostatMode, descriptionText:"Thermostat mode is ${thermostatMode}", type:type
    utils_processedZclMessage "${msg.commandInt == 0x0A ? 'Report' : 'Read'} Attributes Response", "SystemMode=${msg.value}"
    return

// Report/Read Attributes Reponse: ThermostatRunningState
case { contains it, [clusterInt:0x0201, commandInt:0x0A, attrInt:0x0029] }:
case { contains it, [clusterInt:0x0201, commandInt:0x01, attrInt:0x0029] }:
    String thermostatOperatingState = TH_STATES[msg.value[-2..-1]]
    utils_sendEvent name:'thermostatOperatingState', value:thermostatOperatingState, descriptionText:"Thermostat operating state is ${thermostatOperatingState}", type:type
    utils_processedZclMessage "${msg.commandInt == 0x0A ? 'Report' : 'Read'} Attributes Response", "ThermostatRunningState=${msg.value}"
    return

// Write Attributes Response
case { contains it, [clusterInt:0x0201, commandInt:0x04, isClusterSpecific:false] }:
    utils_processedZclMessage "Write Attributes Response", "Status=${msg.data}"
    return

// Other events that we expect but are not usefull
case { contains it, [clusterInt:0x0201, commandInt:0x07] }:
    utils_processedZclMessage 'Configure Reporting Response', "attribute=LocalTemperature, data=${msg.data}"
    return
{{/ @events }}
{{!--------------------------------------------------------------------------}}
