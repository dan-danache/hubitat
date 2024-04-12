{{!--------------------------------------------------------------------------}}
{{# @definition }}
capability 'ColorControl'
{{/ @definition }}
{{!--------------------------------------------------------------------------}}
{{# @inputs }}

// Inputs for capability.ColorControl
{{/ @inputs }}
{{!--------------------------------------------------------------------------}}
{{# @commands }}

// Commands for capability.ColorControl
{{/ @commands }}
{{!--------------------------------------------------------------------------}}
{{# @implementation }}

// Implementation for capability.ColorControl
void setColor(Map colormap) {
    Integer newHue = colormap.hue > 100 ? 100 : (colormap.hue < 0 ? 0 : colormap.hue)
    Integer newSaturation = colormap.saturation > 100 ? 100 : (colormap.saturation < 0 ? 0 : colormap.saturation)
    Integer newLevel = colormap.level > 100 ? 100 : (colormap.level < 0 ? 0 : colormap.level)
    log_debug "Setting color to hue=${newHue}, saturation=${newSaturation}, level=${newLevel}"
    newHue = Math.round(newHue * 2.54)
    newSaturation = Math.round(newSaturation * 2.54)
    String payload = "${utils_payload newHue, 2} ${utils_payload newSaturation, 2} 0000 00 00"
    utils_sendZigbeeCommands(["he raw 0x${device.deviceNetworkId} 0x01 0x${device.endpointId} 0x0300 {114306 ${payload}}"])  // Move to Hue and Saturation
    setLevel newLevel
}
void setHue(BigDecimal hue) {
    Integer newHue = hue > 100 ? 100 : (hue < 0 ? 0 : hue)
    log_debug "Setting color hue to ${newHue}%"
    newHue = Math.round(newHue * 2.54)
    String payload = "${utils_payload newHue, 2} 00 0000 00 00"
    utils_sendZigbeeCommands(["he raw 0x${device.deviceNetworkId} 0x01 0x${device.endpointId} 0x0300 {114300 ${payload}}"])  // Move to Hue
}
void setSaturation(BigDecimal saturation) {
    Integer newSaturation = saturation > 100 ? 100 : (saturation < 0 ? 0 : saturation)
    log_debug "Setting color saturation to ${newSaturation}%"
    newSaturation = Math.round(newSaturation * 2.54)
    String payload = "${utils_payload newSaturation, 2} 0000 00 00"
    utils_sendZigbeeCommands(["he raw 0x${device.deviceNetworkId} 0x01 0x${device.endpointId} 0x0300 {114303 ${payload}}"])  // Move to Saturation
}
private void processMultipleColorAttributes(Map msg, String type) {
    Map<Integer, String> attributes = [:]
    attributes[msg.attrInt] = msg.value
    msg.additionalAttrs?.each { attributes[Integer.parseInt(it.attrId, 16)] = it.value }

    Integer hue = -1
    Integer saturation = -1
    String colorMode = null
    attributes.each {
        switch (it.key) {
            case 0x0000:
                hue = Math.round(Integer.parseInt(it.value, 16) / 2.54)
                hue = hue > 100 ? 100 : (hue < 0 ? 0 : hue)
                break

            case 0x0001:
                saturation = Math.round(Integer.parseInt(it.value, 16) / 2.54)
                saturation = saturation > 100 ? 100 : (saturation < 0 ? 0 : saturation)
                break

            case 0x0008:
            case 0x4001:
                colorMode = it.value == '02' ? 'CT' : 'RGB'
                utils_sendEvent name:'colorMode', value:colorMode, descriptionText:"Color mode is ${colorMode}", type:type
                break
        }
    }

    if (hue >= 0) utils_sendEvent name:'hue', value:hue, descriptionText:"Color hue is ${hue}%", type:type
    if (saturation >= 0) utils_sendEvent name:'saturation', value:saturation, descriptionText:"Color saturation is ${saturation}%", type:type

    // Update colorName, if the case
    if ("${colorMode ?: device.currentValue('colorMode', true)}" == 'RGB') {
        Integer colorHue = hue >= 0 ? hue : device.currentValue('hue', true)
        Integer colorSaturation = saturation >= 0 ? saturation : device.currentValue('saturation', true)
        String colorName = convertHueToGenericColorName colorHue, colorSaturation
        utils_sendEvent name:'colorName', value:colorName, descriptionText:"Color name is ${colorName}", type:type
    }

    utils_processedZclMessage "${msg.commandInt == 0x0A ? 'Report' : 'Read'} Attributes Response", "CurrentHue=${hue}%, CurrentSaturation=${saturation}%, ColorMode=${colorMode}"
}
{{/ @implementation }}
{{!--------------------------------------------------------------------------}}
{{# @updated }}

// Preferences for capability.ColorControl
cmds += zigbee.writeAttribute(0x0300, 0x000F, 0x18, 0x01)
{{/ @updated }}
{{!--------------------------------------------------------------------------}}
{{# @configure }}

// Configuration for capability.ColorControl
cmds += "he cr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0300 0x0000 0x20 0x0000 0x0258 {01} {}" // Report CurrentHue (uint8) at least every 10 minutes (Δ = 1)
cmds += "he cr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0300 0x0001 0x20 0x0000 0x0258 {01} {}" // Report CurrentSaturation (uint8) at least every 10 minutes (Δ = 1)
{{/ @configure }}
{{!--------------------------------------------------------------------------}}
{{# @refresh }}

// Refresh for capability.ColorControl
cmds += zigbee.readAttribute(0x0300, [0x0000, 0x0001, 0x0008]) // CurrentHue, CurrentSaturation, ColorMode
{{/ @refresh }}
{{!--------------------------------------------------------------------------}}
{{# @events }}

// Events for capability.ColorControl
// ===================================================================================================================

// Report/Read Attributes Reponse: CurrentHue
case { contains it, [clusterInt:0x0300, commandInt:0x0A, attrInt:0x0000] }:
case { contains it, [clusterInt:0x0300, commandInt:0x01, attrInt:0x0000] }:

// Report/Read Attributes Reponse: CurrentSaturation
case { contains it, [clusterInt:0x0300, commandInt:0x0A, attrInt:0x0001] }:
case { contains it, [clusterInt:0x0300, commandInt:0x01, attrInt:0x0001] }:

// Report/Read Attributes Reponse: ColorMode
case { contains it, [clusterInt:0x0300, commandInt:0x0A, attrInt:0x0008] }:
case { contains it, [clusterInt:0x0300, commandInt:0x01, attrInt:0x0008] }:

// Report/Read Attributes Reponse: EnhancedColorMode
case { contains it, [clusterInt:0x0300, commandInt:0x0A, attrInt:0x4001] }:
case { contains it, [clusterInt:0x0300, commandInt:0x01, attrInt:0x4001] }:
    processMultipleColorAttributes msg, type
    return

{{/ @events }}
{{!--------------------------------------------------------------------------}}