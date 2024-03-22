{{!--------------------------------------------------------------------------}}
{{# @definition }}
capability "Refresh"
{{/ @definition }}
{{!--------------------------------------------------------------------------}}
{{# @implementation }}

// Implementation for capability.Refresh
def refresh(buttonPress = true) {
    if (buttonPress) {
        Log.warn "Refreshing device current state ..."
        if (device.currentValue("powerSource", true) == "battery") {
            Log.warn '[IMPORTANT] Click the "Refresh" button immediately after pushing any button on the device in order to first wake it up!'
        }
    }

    List<String> cmds = []
    {{# params.readAttributes }}
    cmds += zigbee.readAttribute({{ cluster }}, {{ attr }}, [{{# mfgCode }}mfgCode:"{{ mfgCode }}", {{/ mfgCode }}{{# endpoint }}destEndpoint:{{ endpoint }}, {{/ endpoint }}{{^ mfgCode}}{{^ endpoint}}:{{/ endpoint}}{{/ mfgCode}}]) // {{ description }}
    {{/ params.readAttributes }}
    {{# params.writeAttributes }}
    cmds += zigbee.writeAttribute({{ cluster }}, {{ attr }}, {{ type }}, {{ value }}{{# mfgCode }}, [mfgCode:"{{ mfgCode }}"]{{/ mfgCode }}{{# endpoint }}, [destEndpoint:{{ endpoint }}]{{/ endpoint }}{{^ mfgCode}}{{^ endpoint}}:{{/ endpoint}}{{/ mfgCode}}) // {{ description }}
    {{/ params.writeAttributes }}
    {{# params.readGroups }}
    cmds += "he raw 0x${device.deviceNetworkId} 0x01 0x${device.endpointId} 0x0004 {0143 02 00}"  // Get groups membership
    {{/ params.readGroups }}
    {{# params.readBindings }}
    cmds += "he raw 0x${device.deviceNetworkId} 0x00 0x00 0x0033 {57 00} {0x0000}"  // Start querying the Bindings Table
    {{/ params.readBindings }}
    Utils.sendZigbeeCommands cmds
}
{{/ @implementation }}
{{!--------------------------------------------------------------------------}}
