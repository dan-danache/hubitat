{{!--------------------------------------------------------------------------}}
{{# @definition }}
capability 'ColorTemperature'
capability 'ColorMode'
{{/ @definition }}
{{!--------------------------------------------------------------------------}}
{{# @inputs }}

// Inputs for capability.ColorTemperature
input(
    name: 'colorTemperatureStep', type: 'enum',
    title: 'Color Temperature up/down shift',
    description: '<small>Color Temperature +/- adjust for the shiftColorTemperature() command.</small>',
    options: ['1':'1%', '2':'2%', '5':'5%', '10':'10%', '20':'20%', '25':'25%', '33':'33%', '50':'50%'],
    defaultValue: '25',
    required: true
)
input(
    name: 'colorTemperatureChangeRate', type: 'enum',
    title: 'Color Temperature change rate',
    description: '<small>Color Temperature +/- adjust for the startColorTemperatureChange() command.</small>',
    options: [
         '10': '10% / sec - from hot to cold in 10 seconds',
         '20': '20% / sec - from hot to cold in 5 seconds',
         '33': '33% / sec - from hot to cold in 3 seconds',
         '50': '50% / secs - from hot to cold in 2 seconds',
        '100': '100% / sec - from hot to cold in 1 seconds',
    ],
    defaultValue: '20',
    required: true
)
{{/ @inputs }}
{{!--------------------------------------------------------------------------}}
{{# @commands }}

// Commands for capability.ColorTemperature
command 'startColorTemperatureChange', [[name:'Direction*', type:'ENUM', constraints: ['up', 'down']]]
command 'stopColorTemperatureChange'
command 'shiftColorTemperature', [[name:'Direction*', type:'ENUM', constraints: ['up', 'down']]]
{{/ @commands }}
{{!--------------------------------------------------------------------------}}
{{# @implementation }}

// Implementation for capability.ColorTemperature
void setColorTemperature(BigDecimal colorTemperature, BigDecimal level = -1, BigDecimal duration = 0) {
    Integer mireds = Math.round(1000000 / colorTemperature)
    mireds = mireds < state.minMireds ? state.minMireds : (mireds > state.maxMireds ? state.maxMireds : mireds)
    Integer newColorTemperature = Math.round(1000000 / mireds)
    Integer dur = (duration == null || duration < 0) ? 0 : (duration > 1800 ? 1800 : duration) // Max transition time = 30 min
    log_debug "🎬 Setting color temperature to ${newColorTemperature}k (${mireds} mireds) during ${dur} seconds"
    String payload = "${utils_payload mireds, 4} ${utils_payload dur * 10, 4}"
    utils_sendZigbeeCommands(["he raw 0x${device.deviceNetworkId} 0x01 0x${device.endpointId} 0x0300 {11430A ${payload}}"])
    /* groovylint-disable-next-line UnnecessarySetter */
    if (level > 0 && dur == 0) setLevel level
}
void startColorTemperatureChange(String direction) {
    log_debug "🎬 Starting color temperature change ${direction}wards with a rate of ${colorTemperatureChangeRate}% / second"
    Integer mode = direction == 'up' ? 0x03 : 0x01
    Integer changeRate = (state.maxMireds - state.minMireds) * Integer.parseInt(colorTemperatureChangeRate) / 100
    String payload = "${utils_payload mode, 2} ${utils_payload changeRate, 4} ${utils_payload state.minMireds, 4} ${utils_payload state.maxMireds, 4} 00 00"
    utils_sendZigbeeCommands(["he raw 0x${device.deviceNetworkId} 0x01 0x${device.endpointId} 0x0300 {11434B ${payload}}"])
}
void stopColorTemperatureChange() {
    log_debug '🎬 Stopping color temperature change'
    utils_sendZigbeeCommands(["he raw 0x${device.deviceNetworkId} 0x01 0x${device.endpointId} 0x0300 {114347 00 00}"])
}
void shiftColorTemperature(String direction) {
    log_debug "🎬 Shifting color temperature ${direction} by ${colorTemperatureStep}%"
    Integer mode = direction == 'up' ? 0x03 : 0x01
    Integer stepSize = (state.maxMireds - state.minMireds) * Integer.parseInt(colorTemperatureStep) / 100
    String payload = "${utils_payload mode, 2} ${utils_payload stepSize, 4} 0000 ${utils_payload state.minMireds, 4} ${utils_payload state.maxMireds, 4} 00 00"
    utils_sendZigbeeCommands(["he raw 0x${device.deviceNetworkId} 0x01 0x${device.endpointId} 0x0300 {11434C ${payload}}"])
}
private void processMultipleColorTemperatureAttributes(Map msg, String type) {
    Map<Integer, String> attributes = [:]
    attributes[msg.attrInt] = msg.value
    msg.additionalAttrs?.each { attributes[Integer.parseInt(it.attrId, 16)] = it.value }

    Integer mireds = -1
    Integer temperature = -1
    String colorMode = null
    String colorName = null
    attributes.each {
        switch (it.key) {
            case 0x0007:
                mireds = Integer.parseInt it.value, 16
                temperature = Math.round(1000000 / mireds)
                break
            case 0x0008:
            case 0x4001:
                colorMode = it.value == '02' ? 'CT' : 'RGB'
                utils_sendEvent name:'colorMode', value:colorMode, descriptionText:"Color mode is ${colorMode}", type:type
                break
        }
    }

    if (temperature >= 0) utils_sendEvent name:'colorTemperature', value:temperature, descriptionText:"Color temperature is ${temperature}K", type:type

    // Update colorName, if the case
    if ("${colorMode ?: device.currentValue('colorMode', true)}" == 'CT') {
        Integer colorTemperature = temperature >= 0 ? temperature : device.currentValue('colorTemperature', true)
        colorName = convertTemperatureToGenericColorName colorTemperature
        utils_sendEvent name:'colorName', value:colorName, descriptionText:"Color name is ${colorName}", type:type
    }

    utils_processedZclMessage "${msg.commandInt == 0x0A ? 'Report' : 'Read'} Attributes Response", "ColorTemperatureMireds=${mireds} (${temperature}K), ${colorName}), ColorMode=${colorMode}"
}
{{/ @implementation }}
{{!--------------------------------------------------------------------------}}
{{# @updated }}

// Preferences for capability.ColorTemperature
if (colorTemperatureStep == null) {
    colorTemperatureStep = '20'
    device.updateSetting 'colorTemperatureStep', [value:colorTemperatureStep, type:'enum']
}
log_info "🛠️ colorTemperatureStep = ${colorTemperatureStep}%"

if (colorTemperatureChangeRate == null) {
    colorTemperatureChangeRate = '20'
    device.updateSetting 'colorTemperatureChangeRate', [value:colorTemperatureChangeRate, type:'enum']
}
log_info "🛠️ colorTemperatureChangeRate = ${colorTemperatureChangeRate}% / second"

// Regardless of prestaging, enable update of color temperature without the need for the device to be turned On
cmds += zigbee.writeAttribute(0x0300, 0x000F, 0x18, 0x01)
{{/ @updated }}
{{!--------------------------------------------------------------------------}}
{{# @configure }}

// Configuration for capability.ColorTemperature
cmds += "zdo bind 0x${device.deviceNetworkId} 0x${device.endpointId} 0x01 0x0300 {${device.zigbeeId}} {}" // Color Control Cluster cluster
cmds += "he cr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0300 0x0007 0x21 0x0001 0x0258 {01} {}" // Report ColorTemperatureMireds (uint16) at least every 10 minutes (Δ = 1)
state.minMireds = 200  // Will be updated in refresh()
state.maxMireds = 600  // Will be updated in refresh()
{{/ @configure }}
{{!--------------------------------------------------------------------------}}
{{# @refresh }}

// Refresh for capability.ColorTemperature
cmds += zigbee.readAttribute(0x0300, [0x0007, 0x0008]) // ColorTemperatureMireds, ColorMode
cmds += zigbee.readAttribute(0x0300, [0x400B, 0x400C]) // ColorTemperaturePhysicalMinMireds, ColorTemperaturePhysicalMaxMireds
{{/ @refresh }}
{{!--------------------------------------------------------------------------}}
{{# @events }}

// Events for capability.ColorTemperature
// ===================================================================================================================

// Report/Read Attributes Response: ColorTemperatureMireds
case { contains it, [clusterInt:0x0300, commandInt:0x0A, attrInt:0x0007] }:
case { contains it, [clusterInt:0x0300, commandInt:0x01, attrInt:0x0007] }:

// Report/Read Attributes Response: ColorMode
case { contains it, [clusterInt:0x0300, commandInt:0x0A, attrInt:0x0008] }:
case { contains it, [clusterInt:0x0300, commandInt:0x01, attrInt:0x0008] }:

// Report/Read Attributes Response: EnhancedColorMode
case { contains it, [clusterInt:0x0300, commandInt:0x0A, attrInt:0x4001] }:
case { contains it, [clusterInt:0x0300, commandInt:0x01, attrInt:0x4001] }:
    processMultipleColorTemperatureAttributes msg, type
    return

// Read Attributes Response: ColorTemperaturePhysicalMinMireds, ColorTemperaturePhysicalMaxMireds
case { contains it, [clusterInt:0x0300, commandInt:0x01, attrInt:0x400B] }:
    state.minMireds = Integer.parseInt msg.value, 16
    msg.additionalAttrs?.each { if (it.attrId == '400C') state.maxMireds = Integer.parseInt it.value, 16 }
    utils_processedZclMessage 'Read Attributes Response', "ColorTemperaturePhysicalMinMireds=${msg.value} (${state.minMireds} mireds, ${Math.round(1000000 / state.minMireds)}K), ColorTemperaturePhysicalMaxMireds=${msg.value} (${state.maxMireds} mireds, ${Math.round(1000000 / state.maxMireds)}K)"
    return

// Ignore CurrentX and CurrentY reports



// Other events that we expect but are not usefull
case { contains it, [clusterInt:0x0300, commandInt:0x07] }:
    utils_processedZclMessage 'Configure Reporting Response', "attribute=ColorTemperatureMireds, data=${msg.data}"
    return
case { contains it, [clusterInt:0x0300, commandInt:0x0A, attrInt:0x0003] }: // Report Attributes Response: CurrentX
case { contains it, [clusterInt:0x0300, commandInt:0x0A, attrInt:0x0004] }: // Report Attributes Response: CurrentY
case { contains it, [clusterInt:0x0300, commandInt:0x04] }: // Write Attribute Response (0x04)
    return
{{/ @events }}
{{!--------------------------------------------------------------------------}}
