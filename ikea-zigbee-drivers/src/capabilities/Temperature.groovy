{{!--------------------------------------------------------------------------}}
{{# @definition }}
capability "TemperatureMeasurement"
{{/ @definition }}
{{!--------------------------------------------------------------------------}}
{{# @configure }}

// Configuration for capability.Temperature
cmds += "zdo bind 0x${device.deviceNetworkId} 0x01 0x01 0x0402 {${device.zigbeeId}} {}" // Temperature Measurement cluster
cmds += "he cr 0x${device.deviceNetworkId} 0x01 0x0402 0x0000 0x29 0x0000 0x0258 {6400} {}" // Report MeasuredValue (int16) at least every 10 minutes (Δ = 1°C)
{{/ @configure }}
{{!--------------------------------------------------------------------------}}
{{# @events }}

// Events for capability.Temperature

// Report/Read Attributes Reponse: MeasuredValue
case { contains it, [clusterInt:0x0402, commandInt:0x0A, attrInt:0x0000] }:
case { contains it, [clusterInt:0x0402, commandInt:0x01, attrInt:0x0000] }:

    // A MeasuredValue of 0x8000 indicates that the temperature measurement is invalid
    if (msg.value == "8000") return Log.warn("Ignored invalid temperature value: 0x${msg.value}")

    String temperature = convertTemperatureIfNeeded(Integer.parseInt(msg.value, 16) / 100, "C", 0)
    Utils.sendEvent name:"temperature", value:temperature, unit:"°${location.temperatureScale}", descriptionText:"Temperature is ${temperature} °${location.temperatureScale}", type:type
    return Utils.processedZclMessage("${msg.commandInt == 0x0A ? "Report" : "Read"} Attributes Response", "Temperature=${msg.value}")

// Other events that we expect but are not usefull for capability.Temperature behavior
case { contains it, [clusterInt:0x0402, commandInt:0x07] }:
    return Utils.processedZclMessage("Configure Reporting Response", "attribute=temperature, data=${msg.data}")
{{/ @events }}
{{!--------------------------------------------------------------------------}}
