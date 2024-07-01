{{!--------------------------------------------------------------------------}}
{{# @commands }}

// Commands for capability.FirmwareUpdate
command 'updateFirmware'
{{/ @commands }}
{{!--------------------------------------------------------------------------}}
{{# @implementation }}

// Implementation for capability.FirmwareUpdate
void updateFirmware() {
    log_info 'Instructing device to check for firmware updates ...'
    utils_sendZigbeeCommands zigbee.updateFirmware()
}
{{/ @implementation }}
{{!--------------------------------------------------------------------------}}
