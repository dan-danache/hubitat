{{!--------------------------------------------------------------------------}}
{{# @commands }}

// Commands for capability.FirmwareUpdate
command "updateFirmware"
{{/ @commands }}
{{!--------------------------------------------------------------------------}}
{{# @implementation }}

// Implementation for capability.FirmwareUpdate
def updateFirmware() {
    Log.info "Looking for firmware updates ..."
    if (device.currentValue("powerSource", true) == "battery") {
        Log.warn '[IMPORTANT] Click the "Update Firmware" button immediately after pushing any button on the device in order to first wake it up!'
    }
    Utils.sendZigbeeCommands(zigbee.updateFirmware())
}
{{/ @implementation }}
{{!--------------------------------------------------------------------------}}
