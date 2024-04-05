{{!--------------------------------------------------------------------------}}
{{# @definition }}
capability 'AirQuality'
capability 'FanControl'
capability 'FilterStatus'
capability 'Switch'
{{/ @definition }}
{{!--------------------------------------------------------------------------}}
{{# @fields }}

// Fields for devices.Ikea_E2006
@Field static final List<String> SUPPORTED_FAN_SPEEDS = [
    'auto', 'low', 'medium-low', 'medium', 'medium-high', 'high', 'off'
]
{{/ @fields }}
{{!--------------------------------------------------------------------------}}
{{# @attributes }}

// Attributes for devices.Ikea_E2006
attribute 'airQuality', 'enum', ['good', 'moderate', 'unhealthy for sensitive groups', 'unhealthy', 'hazardous']
attribute 'filterUsage', 'number'
attribute 'pm25', 'number'
attribute 'auto', 'enum', ['on', 'off']
{{/ @attributes }}
{{!--------------------------------------------------------------------------}}
{{# @commands }}

// Commands for devices.Ikea_E2006
command 'setSpeed', [[name:'Fan speed*', type:'ENUM', description:'Fan speed to set', constraints:SUPPORTED_FAN_SPEEDS]]
command 'toggle'
{{/ @commands }}
{{!--------------------------------------------------------------------------}}
{{# @inputs }}

// Inputs for devices.Ikea_E2006
input(
    name: 'pm25ReportDelta', type: 'enum',
    title: 'Sensor report frequency',
    description: '<small>Adjust how often the device sends its PM 2.5 sensor data.</small>',
    options: [
        '01': 'Very High - report changes of +/- 1μg/m3',
        '02': 'High - report changes of +/- 2μg/m3',
        '03': 'Medium - report changes of +/- 3μg/m3',
        '05': 'Low - report changes of +/- 5μg/m3',
        '10': 'Very Low - report changes of +/- 10μg/m3'
    ],
    defaultValue: '03',
    required: true
)
input(
    name: 'filterLifeTime', type: 'enum',
    title: 'Filter life time',
    description: '<small>Configure time between filter changes (default 6 months).</small>',
    options: [
         '90': '3 months',
        '180': '6 months',
        '270': '9 months',
        '360': '1 year'
    ],
    defaultValue: '180',
    required: true
)
input(
    name: 'childLock', type: 'bool',
    title: 'Child lock',
    description: '<small>Lock physical controls, safeguarding against accidental operation.</small>',
    defaultValue: false
)
input(
    name: 'panelIndicator', type: 'bool',
    title: 'LED status',
    description: '<small>Keep the LED indicators on the device constantly lit.</small>',
    defaultValue: true
)
{{/ @inputs }}
{{!--------------------------------------------------------------------------}}
{{# @implementation }}

// Implementation for devices.Ikea_E2006
void on() {
    if (device.currentValue('switch', true) == 'on') return
    log_debug 'Sending On command'
    utils_sendZigbeeCommands(zigbee.writeAttribute(0xFC7D, 0x0006, 0x20, 0x01, [mfgCode:'0x117C']))
}
void off() {
    log_debug 'Sending Off command'
    utils_sendZigbeeCommands(zigbee.writeAttribute(0xFC7D, 0x0006, 0x20, 0x00, [mfgCode:'0x117C']))
    utils_sendEvent name:'switch', value:'off', descriptionText:'Was turned off', type:'digital'
    utils_sendEvent name:'auto', value:'disabled', descriptionText:'Auto mode is disabled', type:'digital'
    utils_sendEvent name:'speed', value:'off', descriptionText:'Fan speed is off', type:'digital'
}
void toggle() {
    if (device.currentValue('switch', true) == 'on') { off() }
    else { on() }
}
void setSpeed(String speed) {
    log_debug "Setting speed to: ${speed}"
    Integer newSpeed = 0x00
    switch (speed) {
        case 'on':
        case 'auto':
            newSpeed = 1
            break
        case 'low':
            newSpeed = 10
            break
        case 'medium-low':
            newSpeed = 20
            break
        case 'medium':
            newSpeed = 30
            break
        case 'medium-high':
            newSpeed = 40
            break
        case 'high':
            newSpeed = 50
            break
        case 'off':
            newSpeed = 0
            break
        default:
            log_warn "Unknown speed: ${speed}"
            return
    }
    utils_sendZigbeeCommands(zigbee.writeAttribute(0xFC7D, 0x0006, 0x20, newSpeed, [mfgCode:'0x117C']))
}
void cycleSpeed() {
    String curSpeed = device.currentValue('speed', true)
    log_debug "Current speed is: ${curSpeed}"
    Integer newSpeed = 0x00
    switch (curSpeed) {
        case 'high':
        case 'off':
            newSpeed = 10
            break
        case 'low':
            newSpeed = 20
            break
        case 'medium-low':
            newSpeed = 30
            break
        case 'medium':
            newSpeed = 40
            break
        case 'medium-high':
            newSpeed = 50
            break
        default:
            log_warn "Unknown current speed: ${curSpeed}"
            return
    }

    log_debug "Cycling speed to: ${newSpeed}"
    utils_sendZigbeeCommands(zigbee.writeAttribute(0xFC7D, 0x0006, 0x20, newSpeed, [mfgCode:'0x117C']))
}
private Integer lerp(Integer ylo, Integer yhi, BigDecimal xlo, BigDecimal xhi, Integer cur) {
    return Math.round(((cur - xlo) / (xhi - xlo)) * (yhi - ylo) + ylo)
}
private List pm25Aqi(Integer pm25) { // See: https://en.wikipedia.org/wiki/Air_quality_index#United_States
    if (pm25 <=  12.1) return [lerp(  0,  50,   0.0,  12.0, pm25), 'good', 'green']
    if (pm25 <=  35.5) return [lerp( 51, 100,  12.1,  35.4, pm25), 'moderate', 'gold']
    if (pm25 <=  55.5) return [lerp(101, 150,  35.5,  55.4, pm25), 'unhealthy for sensitive groups', 'darkorange']
    if (pm25 <= 150.5) return [lerp(151, 200,  55.5, 150.4, pm25), 'unhealthy', 'red']
    if (pm25 <= 250.5) return [lerp(201, 300, 150.5, 250.4, pm25), 'very unhealthy', 'purple']
    if (pm25 <= 350.5) return [lerp(301, 400, 250.5, 350.4, pm25), 'hazardous', 'maroon']
    if (pm25 <= 500.5) return [lerp(401, 500, 350.5, 500.4, pm25), 'hazardous', 'maroon']
    return [500, 'hazardous', 'maroon']
}
{{/ @implementation }}
{{!--------------------------------------------------------------------------}}
{{# @updated }}

// Preferences for devices.Ikea_E2006
if (pm25ReportDelta == null) {
    pm25ReportDelta = '03'
    device.updateSetting 'pm25ReportDelta', [value:pm25ReportDelta, type:'enum']
}
log_info "🛠️ pm25ReportDelta = +/- ${pm25ReportDelta} μg/m3"
cmds += "he cr 0x${device.deviceNetworkId} 0x01 0xFC7D 0x0004 0x21 0x0000 0x0258 {${pm25ReportDelta}} {117C}"

if (filterLifeTime == null) {
    filterLifeTime = '180'
    device.updateSetting 'filterLifeTime', [value:filterLifeTime, type:'enum']
}
log_info "🛠️ filterLifeTime = ${filterLifeTime} days"
cmds += zigbee.writeAttribute(0xFC7D, 0x0002, 0x23, Integer.parseInt(filterLifeTime) * 1440, [mfgCode:'0x117C'])
cmds += zigbee.readAttribute(0xFC7D, 0x0000, [mfgCode:'0x117C'])  // Also trigger the update of the filterStatus value (%)

if (childLock == null) {
    childLock = false
    device.updateSetting 'childLock', [value:childLock, type:'bool']
}
log_info "🛠️ childLock = ${childLock}"
cmds += zigbee.writeAttribute(0xFC7D, 0x0005, 0x10, childLock ? 0x01 : 0x00, [mfgCode:'0x117C'])

if (panelIndicator == null) {
    panelIndicator = true
    device.updateSetting 'panelIndicator', [value:panelIndicator, type:'bool']
}
log_info "🛠️ panelIndicator = ${panelIndicator}"
cmds += zigbee.writeAttribute(0xFC7D, 0x0003, 0x10, panelIndicator ? 0x00 : 0x01, [mfgCode:'0x117C'])
{{/ @updated }}
{{!--------------------------------------------------------------------------}}
{{# @configure }}

// Configuration for devices.E2006
sendEvent name:'supportedFanSpeeds', value:SUPPORTED_FAN_SPEEDS, type:'digital', descriptionText:"Supported fan speeds initialized to ${SUPPORTED_FAN_SPEEDS}"

cmds += "zdo bind 0x${device.deviceNetworkId} 0x01 0x01 0xFC7D {${device.zigbeeId}} {}"  // IKEA Air Purifier cluster
cmds += "he cr 0x${device.deviceNetworkId} 0x01 0xFC7D 0x0000 0x23 0x0000 0x0258 {0A} {117C}"  // Report FilterRunTime (uint32) at least every 10 minutes
cmds += "he cr 0x${device.deviceNetworkId} 0x01 0xFC7D 0x0001 0x20 0x0000 0x0000 {01} {117C}"  // Report ReplaceFilter (uint8)
cmds += "he cr 0x${device.deviceNetworkId} 0x01 0xFC7D 0x0002 0x23 0x0000 0x0000 {01} {117C}"  // Report FilterLifeTime (uint32)
//cmds += "he cr 0x${device.deviceNetworkId} 0x01 0xFC7D 0x0003 0x10 0x0000 0x0000 {01} {117C}"  // Report DisablePanelLights (bool)
//cmds += "he cr 0x${device.deviceNetworkId} 0x01 0xFC7D 0x0004 0x21 0x0000 0x0258 {01} {117C}"  // Report PM25Measurement (uint16)
cmds += "he cr 0x${device.deviceNetworkId} 0x01 0xFC7D 0x0005 0x10 0x0000 0x0000 {01} {117C}"  // Report ChildLock (bool)
cmds += "he cr 0x${device.deviceNetworkId} 0x01 0xFC7D 0x0006 0x20 0x0000 0x0000 {01} {117C}"  // Report FanMode (uint8)
cmds += "he cr 0x${device.deviceNetworkId} 0x01 0xFC7D 0x0007 0x20 0x0000 0x0000 {01} {117C}"  // Report FanSpeed (uint8)
//cmds += "he cr 0x${device.deviceNetworkId} 0x01 0xFC7D 0x0008 0x23 0x0000 0x0258 {01} {117C}"  // Report DeviceRunTime (uint32)
{{/ @configure }}
{{!--------------------------------------------------------------------------}}
{{# @refresh }}

// Refresh for devices.Ikea_E2006
cmds += zigbee.readAttribute(0xFC7D, 0x0000, [mfgCode: '0x117C']) // FilterRunTime
cmds += zigbee.readAttribute(0xFC7D, 0x0001, [mfgCode: '0x117C']) // ReplaceFilter
cmds += zigbee.readAttribute(0xFC7D, 0x0002, [mfgCode: '0x117C']) // FilterLifeTime
cmds += zigbee.readAttribute(0xFC7D, 0x0003, [mfgCode: '0x117C']) // DisablePanelLights
cmds += zigbee.readAttribute(0xFC7D, 0x0004, [mfgCode: '0x117C']) // PM25Measurement
cmds += zigbee.readAttribute(0xFC7D, 0x0005, [mfgCode: '0x117C']) // ChildLock
cmds += zigbee.readAttribute(0xFC7D, 0x0006, [mfgCode: '0x117C']) // FanMode
{{/ @refresh }}
{{!--------------------------------------------------------------------------}}
{{# @events }}

// Events for devices.Ikea_E2006
// ===================================================================================================================

// Report/Read Attributes: PM25
case { contains it, [clusterInt:0xFC7D, commandInt:0x0A, attrInt:0x0004] }:
case { contains it, [clusterInt:0xFC7D, commandInt:0x01, attrInt:0x0004] }:
    Integer pm25 = Integer.parseInt(msg.value, 16)

    // Tried to read the PM 2.5 value when the device is Off
    if (pm25 == 0xFFFF) return

    utils_sendEvent name:'pm25', value:pm25, unit:'μg/m³', descriptionText:"Fine particulate matter (PM2.5) concentration is ${pm25} μg/m³", type:type
    List aqi = pm25Aqi pm25
    utils_sendEvent name:'airQualityIndex', value:aqi[0], descriptionText:"Calculated Air Quality Index = ${aqi[0]}", type:type
    utils_sendEvent name:'airQuality', value:"<span style=\"color:${aqi[2]}\">${aqi[1]}</span>", descriptionText:"Calculated Air Quality = ${aqi[1]}", type:type
    utils_processedZclMessage "${msg.commandInt == 0x0A ? 'Report' : 'Read'} Attributes Response", "PM25Measurement=${pm25} μg/m³"
    return

// Report/Read Attributes: FilterRunTime
case { contains it, [clusterInt:0xFC7D, commandInt:0x0A, attrInt:0x0000] }:
case { contains it, [clusterInt:0xFC7D, commandInt:0x01, attrInt:0x0000] }:
    Integer runTime = Integer.parseInt(msg.value, 16)
    Integer filterUsage = Math.floor(runTime * 100 / (Integer.parseInt(filterLifeTime) * 1440))
    utils_sendEvent name:'filterUsage', value:filterUsage, unit:'%', descriptionText:"Filter usage is ${filterUsage}%", type:type
    utils_processedZclMessage "${msg.commandInt == 0x0A ? 'Report' : 'Read'} Attributes Response", "FilterRunTime=${runTime} min, FilterLifeTime=${filterLifeTime} days"
    return

// Report/Read Attributes: FanMode
case { contains it, [clusterInt:0xFC7D, commandInt:0x0A, attrInt:0x0006] }:
case { contains it, [clusterInt:0xFC7D, commandInt:0x01, attrInt:0x0006] }:
    String auto = msg.value == '01' ? 'enabled' : 'disabled'
    utils_sendEvent name:'auto', value:auto, descriptionText:"Auto mode is ${auto}", type:type
    utils_sendZigbeeCommands(zigbee.readAttribute(0xFC7D, 0x0007, [mfgCode:'0x117C']))
    utils_processedZclMessage "${msg.commandInt == 0x0A ? 'Report' : 'Read'} Attributes Response", "FanMode=${msg.value}"
    return

// Report/Read Attributes: FanSpeed
case { contains it, [clusterInt:0xFC7D, commandInt:0x0A, attrInt:0x0007] }:
case { contains it, [clusterInt:0xFC7D, commandInt:0x01, attrInt:0x0007] }:

    // Fan Speed should vary from 1 to 50
    Integer speed = Integer.parseInt(msg.value, 16)
    if (speed > 50) newSpeed = 50

    // Update switch status
    String newState = speed == 0 ? 'off' : 'on'
    utils_sendEvent name:'switch', value:newState, descriptionText:"Was turned ${newState}", type:type

    String newSpeed = ''
    switch (speed) {
        case 0:
            newSpeed = 'off'
            break
        case { speed <= 10 }:
            newSpeed = 'low'
            break
        case { speed <= 20 }:
            newSpeed = 'medium-low'
            break
        case { speed <= 30 }:
            newSpeed = 'medium'
            break
        case { speed <= 40 }:
            newSpeed = 'medium-high'
            break
        default:
            newSpeed = 'high'
    }
    utils_sendEvent name:'speed', value:newSpeed, descriptionText:"Fan speed is ${newSpeed}", type:type
    utils_processedZclMessage "${msg.commandInt == 0x0A ? 'Report' : 'Read'} Attributes Response", "FanSpeed=${speed}"
    return

// Report/Read Attributes: ReplaceFilter
case { contains it, [clusterInt:0xFC7D, commandInt:0x0A, attrInt:0x0001] }:
case { contains it, [clusterInt:0xFC7D, commandInt:0x01, attrInt:0x0001] }:
    String filterStatus = msg.value == '00' ? 'normal' : 'replace'
    utils_sendEvent name:'filterStatus', value:filterStatus, descriptionText:"Filter status is ${filterStatus}", type:type
    utils_processedZclMessage "${msg.commandInt == 0x0A ? 'Report' : 'Read'} Attributes Response", "ReplaceFilter=${msg.value}"
    return

// Report/Read Attributes: FilterLifeTime
case { contains it, [clusterInt:0xFC7D, commandInt:0x0A, attrInt:0x0002] }:
case { contains it, [clusterInt:0xFC7D, commandInt:0x01, attrInt:0x0002] }:
    Integer lifeTimeDays = Math.ceil(Integer.parseInt(msg.value, 16) / 1440)
    if (lifeTimeDays != 90 && lifeTimeDays != 180 && lifeTimeDays != 270 && lifeTimeDays != 360) {
        log_warn "Invalid FilterLifeTime value: ${msg.value} (${lifeTimeDays} days). Setting it to default value of 180 days."
        lifeTimeDays = 180
        utils_sendZigbeeCommands(zigbee.writeAttribute(0xFC7D, 0x0002, 0x23, lifeTimeDays * 1440, [mfgCode:'0x117C']))
    }
    filterLifeTime = "${filterLifeTime}"
    device.updateSetting 'filterLifeTime', [value:filterLifeTime, type:'enum']
    utils_processedZclMessage "${msg.commandInt == 0x0A ? 'Report' : 'Read'} Attributes Response", "FilterLifeTime=${msg.value} (${lifeTimeDays} days)"
    return

// Read Attributes: DisablePanelLights
case { contains it, [clusterInt:0xFC7D, commandInt:0x01, attrInt:0x0003] }:
    panelIndicator = msg.value == '00'
    device.updateSetting 'panelIndicator', [value:panelIndicator, type:'bool']
    utils_processedZclMessage 'Read Attributes Response', "DisablePanelLights=${msg.value}"
    return

// Report/Read Attributes: ChildLock
case { contains it, [clusterInt:0xFC7D, commandInt:0x0A, attrInt:0x0005] }:
case { contains it, [clusterInt:0xFC7D, commandInt:0x01, attrInt:0x0005] }:
    childLock = msg.value == '01'
    device.updateSetting 'childLock', [value:childLock, type:'bool']
    utils_processedZclMessage "${msg.commandInt == 0x0A ? 'Report' : 'Read'} Attributes Response", "ChildLock=${msg.value}"
    return

// Other events that we expect but are not usefull for devices.E2006 behavior
case { contains it, [clusterInt:0xFC7D, commandInt:0x04] }: // Write Attribute Response (0x04)
case { contains it, [clusterInt:0xFC7D, commandInt:0x07] }: // Configure Reporting Response
    return
{{/ @events }}
{{!--------------------------------------------------------------------------}}
