{{!--------------------------------------------------------------------------}}
{{# @inputs }}

// Inputs for devices.Legrand_741811
input(
    name: 'ledMode', type: 'enum',
    title: 'LED mode',
    description: 'elect how the LED indicator behaves',
    options: [
        'ALWAYS_ON': 'Always On - LED remains lit at all times, making it easy to find in the dark',
        'ALWAYS_OFF': 'Always Off - LED remains off, ensuring total darkness',
        'OUTLET_STATUS': 'Outlet status - LED indicates the power state of the outlet'
    ],
    defaultValue: 'OUTLET_STATUS',
    required: true
)
{{/ @inputs }}
{{!--------------------------------------------------------------------------}}
{{# @updated }}

// Preferences for devices.Legrand_741811
if (ledMode == null) {
    ledMode = 'OUTLET_STATUS'
    device.updateSetting 'ledMode', [value:ledMode, type:'enum']
}
log_info "🛠️ ledMode = ${ledMode}"
switch (ledMode) {
    case 'ALWAYS_ON':
        cmds += zigbee.writeAttribute(0xFC01, 0x0001, 0x10, 0x01, [mfgCode: '0x1021'])
        cmds += zigbee.writeAttribute(0xFC01, 0x0002, 0x10, 0x01, [mfgCode: '0x1021'])
        break
    case 'ALWAYS_OFF':
        cmds += zigbee.writeAttribute(0xFC01, 0x0001, 0x10, 0x00, [mfgCode: '0x1021'])
        cmds += zigbee.writeAttribute(0xFC01, 0x0002, 0x10, 0x00, [mfgCode: '0x1021'])
        break
    default:
        cmds += zigbee.writeAttribute(0xFC01, 0x0001, 0x10, 0x00, [mfgCode: '0x1021'])
        cmds += zigbee.writeAttribute(0xFC01, 0x0002, 0x10, 0x01, [mfgCode: '0x1021'])
}
{{/ @updated }}
{{!--------------------------------------------------------------------------}}
{{# @events }}

// Events for devices.Legrand_741811
// ===================================================================================================================

// Write Attributes Response
case { contains it, [endpointInt:0x01, clusterInt:0xFC01, commandInt:0x04, isClusterSpecific:false, isManufacturerSpecific:true, manufacturerId:'1021'] }:
    return
{{/ @events }}
{{!--------------------------------------------------------------------------}}
