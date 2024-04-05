{{!--------------------------------------------------------------------------}}
{{# @commands }}

// Commands for capability.FirmwareUpdate
command 'updateFirmware'
{{/ @commands }}
{{!--------------------------------------------------------------------------}}
{{# @implementation }}

// Implementation for capability.FirmwareUpdate
void updateFirmware() {
    log_info 'Looking for firmware updates ...'
    if (device.currentValue('powerSource', true) == 'battery') {
        log_warn '[IMPORTANT] Click the "Update Firmware" button immediately after pushing any button on the device in order to first wake it up!'
    }
    utils_sendZigbeeCommands zigbee.updateFirmware()
}
{{/ @implementation }}
{{!--------------------------------------------------------------------------}}
