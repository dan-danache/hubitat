{{!--------------------------------------------------------------------------}}
{{# @definition }}
capability "Refresh"
{{/ @definition }}
{{!--------------------------------------------------------------------------}}
{{# @implementation }}

// Implementation for capability.Refresh
def refresh(buttonPress = true) {
    if (buttonPress) {
        Log.info "Refreshing current state ..."
        if (device.currentValue("powerSource", true) == "battery") {
            Log.warn '[IMPORTANT] Click the "Refresh" button immediately after pushing any button on the device in order to first wake it up!'
        }
    }
    List<String> cmds = []
    {{# params.readAttributes }}
    cmds += zigbee.readAttribute({{ cluster }}, {{ attr }}{{# mfgCode }}, [mfgCode: "{{ mfgCode }}"]{{/ mfgCode }}) // {{ description }}
    {{/ params.readAttributes }}
    {{# params.writeAttributes }}
    cmds += zigbee.writeAttribute({{ cluster }}, {{ attr }}, {{ type }}, {{ value }}{{# mfgCode }}, [mfgCode: "{{ mfgCode }}"]{{/ mfgCode }}) // {{ description }}
    {{/ params.writeAttributes }}
    Utils.sendZigbeeCommands cmds
}
{{/ @implementation }}
{{!--------------------------------------------------------------------------}}
