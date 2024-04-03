{{!--------------------------------------------------------------------------}}
{{# @fields }}

// Fields for capability.IAS
import hubitat.zigbee.clusters.iaszone.ZoneStatus
{{/ @fields }}
{{!--------------------------------------------------------------------------}}
{{# @configure }}

// Configuration for capability.IAS
Integer ep0500 = {{# params.endpoint }}{{params.endpoint}}{{/ params.endpoint }}{{^ params.endpoint }}0x01{{/ params.endpoint }}
cmds += "he wattr 0x${device.deviceNetworkId} ${ep0500} 0x0500 0x0010 0xF0 {${utils_payload "${location.hub.zigbeeEui}"}}"
cmds += "he raw 0x${device.deviceNetworkId} 0x01 ${ep0500} 0x0500 {01 23 00 00 00}" // Zone Enroll Response (0x00): status=Success, zoneId=0x00
cmds += "zdo bind 0x${device.deviceNetworkId} ${ep0500} 0x01 0x0500 {${device.zigbeeId}} {}" // IAS Zone cluster
cmds += "he cr 0x${device.deviceNetworkId} ${ep0500} 0x0500 0x0002 0x19 0x0000 0x4650 {00} {}" // Report ZoneStatus (map16) at least every 5 hours (Δ = 0)
{{/ @configure }}
{{!--------------------------------------------------------------------------}}
{{# @attributes }}

// Attributes for capability.IAS
attribute 'ias', 'enum', ['enrolled', 'not enrolled']
{{/ @attributes }}
{{!--------------------------------------------------------------------------}}
{{# @refresh }}

// Refresh for capability.IAS
Integer ep0500 = {{# params.endpoint }}{{params.endpoint}}{{/ params.endpoint }}{{^ params.endpoint }}0x01{{/ params.endpoint }}
cmds += zigbee.readAttribute(0x0500, 0x0000, [destEndpoint: ep0500]) // IAS ZoneState
cmds += zigbee.readAttribute(0x0500, 0x0001, [destEndpoint: ep0500]) // IAS ZoneType
cmds += zigbee.readAttribute(0x0500, 0x0002, [destEndpoint: ep0500]) // IAS ZoneStatus
{{/ @refresh }}
{{!--------------------------------------------------------------------------}}
{{# @events }}

// Events for capability.IAS
// ===================================================================================================================

// Zone Status Change Notification
case { contains it, [clusterInt:0x500, commandInt:0x00, isClusterSpecific:true] }:
    ZoneStatus zs = zigbee.parseZoneStatus(description)
    boolean alarm1             = zs.alarm1Set
    boolean alarm2             = zs.alarm2Set
    boolean tamper             = zs.tamperSet
    boolean lowBattery         = zs.batterySet
    boolean supervisionReports = zs.supervisionReportsSet
    boolean restoreReports     = zs.restoreReportsSet
    boolean trouble            = zs.troubleSet
    boolean mainsFault         = zs.acSet
    boolean testMode           = zs.testSet
    boolean batteryDefect      = zs.batteryDefectSet
    utils_processedZclMessage 'Zone Status Change Notification', "alarm1=${alarm1} alarm2=${alarm2} tamper=${tamper} lowBattery=${lowBattery} supervisionReports=${supervisionReports} restoreReports=${restoreReports} trouble=${trouble} mainsFault=${mainsFault} testMode=${testMode} batteryDefect=${batteryDefect}"
    return

// Enroll Request
case { contains it, [clusterInt:0x500, commandInt:0x01, isClusterSpecific:true] }:
    Integer ep0500 = {{# params.endpoint }}{{params.endpoint}}{{/ params.endpoint }}{{^ params.endpoint }}0x01{{/ params.endpoint }}
    utils_sendZigbeeCommands([
        "he raw 0x${device.deviceNetworkId} 0x01 ${ep0500} 0x0500 {01 23 00 00 00}",  // Zone Enroll Response (0x00): status=Success, zoneId=0x00
        "he raw 0x${device.deviceNetworkId} 0x01 ${ep0500} 0x0500 {01 23 01}",        // Initiate Normal Operation Mode (0x01): no_payload
    ])
    utils_processedZclMessage 'Enroll Request', "description=${description}"
    return

// Read Attributes: ZoneState
case { contains it, [clusterInt:0x0500, commandInt:0x01, attrInt:0x0000] }:
    String status = msg.value == '01' ? 'enrolled' : 'not enrolled'
    utils_sendEvent name:'ias', value:status, descriptionText:"Device IAS status is ${status}", type:'digital'
    utils_processedZclMessage 'Read Attributes Response', "ZoneState=${msg.value == '01' ? 'enrolled' : 'not_enrolled'}"
    return

// Read Attributes: ZoneType
case { contains it, [clusterInt:0x0500, commandInt:0x01, attrInt:0x0001] }:
    utils_processedZclMessage 'Read Attributes Response', "ZoneType=${msg.value}"
    return

// Other events that we expect but are not usefull for capability.IAS behavior
case { contains it, [clusterInt:0x0500, commandInt:0x04, isClusterSpecific:false] }:
    utils_processedZclMessage 'Write Attribute Response', "attribute=IAS_CIE_Address, ZoneType=${msg.data}"
    return
{{/ @events }}
{{!--------------------------------------------------------------------------}}
