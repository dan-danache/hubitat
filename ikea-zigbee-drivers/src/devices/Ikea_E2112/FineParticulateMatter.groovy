{{!--------------------------------------------------------------------------}}
{{# @definition }}
capability 'AirQuality'
{{/ @definition }}
{{!--------------------------------------------------------------------------}}
{{# @attributes }}

// Attributes for E2112.FineParticulateMatter
attribute 'airQuality', 'enum', ['good', 'moderate', 'unhealthy for sensitive groups', 'unhealthy', 'hazardous']
attribute 'pm25', 'number'
{{/ @attributes }}
{{!--------------------------------------------------------------------------}}
{{# @configure }}

// Configuration for E2112.FineParticulateMatter
cmds += "zdo bind 0x${device.deviceNetworkId} 0x01 0x01 0x042A {${device.zigbeeId}} {}" // Particulate Matter 2.5 cluster
cmds += "he cr 0x${device.deviceNetworkId} 0x01 0x042A 0x0000 0x39 0x000F 0x0E10 {40000000} {}" // Report MeasuredValue (single) at most every 15 seconds, at least every 1 hour (Δ = ??)
{{/ @configure }}
{{!--------------------------------------------------------------------------}}
{{# @refresh }}

// Refresh for E2112.FineParticulateMatter
cmds += zigbee.readAttribute(0x042A, 0x0000) // Fine Particulate Matter (PM25)
{{/ @refresh }}
{{!--------------------------------------------------------------------------}}
{{# @implementation }}

// Implementation for E2112.FineParticulateMatter
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
{{# @events }}

// Events for E2112.FineParticulateMatter
// ===================================================================================================================

// Report/Read Attributes Reponse: MeasuredValue
case { contains it, [clusterInt:0x042A, commandInt:0x0A, attrInt:0x0000] }:
case { contains it, [clusterInt:0x042A, commandInt:0x01, attrInt:0x0000] }:

    // A MeasuredValue of 0xFFFFFFFF indicates that the measurement is invalid
    if (msg.value == 'FFFFFFFF') {
        log_warn "Ignored invalid PM25 value: 0x${msg.value}"
        return
    }

    Integer pm25 = Math.round Float.intBitsToFloat(Integer.parseInt(msg.value, 16))
    utils_sendEvent name:'pm25', value:pm25, unit:'μg/m³', descriptionText:"Fine particulate matter (PM2.5) concentration is ${pm25} μg/m³", type:type
    List aqi = pm25Aqi pm25
    utils_sendEvent name:'airQualityIndex', value:aqi[0], descriptionText:"Calculated Air Quality Index = ${aqi[0]}", type:type
    utils_sendEvent name:'airQuality', value:"<span style=\"color:${aqi[2]}\">${aqi[1]}</span>", descriptionText:"Calculated Air Quality = ${aqi[1]}", type:type
    utils_processedZclMessage "${msg.commandInt == 0x0A ? 'Report' : 'Read'} Attributes Response", "PM25Measurement=${pm25} μg/m³"
    return

// Other events that we expect but are not usefull
case { contains it, [clusterInt:0x042A, commandInt:0x07] }:
    utils_processedZclMessage 'Configure Reporting Response', "attribute=PM25, data=${msg.data}"
    return
{{/ @events }}
{{!--------------------------------------------------------------------------}}
