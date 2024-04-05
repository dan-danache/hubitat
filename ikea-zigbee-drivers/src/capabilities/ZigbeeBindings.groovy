{{!--------------------------------------------------------------------------}}
{{# @fields }}
{{# params.groups }}

// Fields for capability.ZigbeeBindings
@Field static final Map<String, String> GROUPS = [
    '9900':'Alfa', '9901':'Bravo', '9902':'Charlie', '9903':'Delta', '9904':'Echo', '9905':'Foxtrot', '9906':'Golf', '9907':'Hotel', '9908':'India', '9909':'Juliett', '990A':'Kilo', '990B':'Lima', '990C':'Mike', '990D':'November', '990E':'Oscar', '990F':'Papa', '9910':'Quebec', '9911':'Romeo', '9912':'Sierra', '9913':'Tango', '9914':'Uniform', '9915':'Victor', '9916':'Whiskey', '9917':'Xray', '9918':'Yankee', '9919':'Zulu'
]
{{/ params.groups }}
{{/ @fields }}
{{!--------------------------------------------------------------------------}}
{{# @inputs }}

// Inputs for capability.ZigbeeBindings
input(
    name: 'controlDevice', type: 'enum',
    title: 'Control Zigbee device',
    description: '<small>Select the target Zigbee device that will be <abbr title="Without involving the Hubitat hub" style="cursor:help">directly controlled</abbr> by this device.</small>',
    options: ['0000':'❌ Stop controlling all Zigbee devices', '----':'- - - -'] + retrieveSwitchDevices(),
    defaultValue: '----',
    required: false
)
{{# params.groups }}
input(
    name: 'controlGroup', type: 'enum',
    title: 'Control Zigbee group',
    description: '<small>Select the target Zigbee group that will be <abbr title="Without involving the Hubitat hub" style="cursor:help">directly controlled</abbr> by this device.</small>',
    options: ['0000':'❌ Stop controlling all Zigbee groups', '----':'- - - -'] + GROUPS,
    defaultValue: '----',
    required: false
)
{{/ params.groups }}
{{/ @inputs }}
{{!--------------------------------------------------------------------------}}
{{# @implementation }}

// Implementation for capability.ZigbeeBindings
private Map<String, String> retrieveSwitchDevices() {
    try {
        List<Integer> switchDeviceIds = httpGet([uri:'http://127.0.0.1:8080/device/listJson?capability=capability.switch']) { it.data*.id }
        httpGet([uri:'http://127.0.0.1:8080/hub/zigbeeDetails/json']) { response ->
            response.data.devices
                .findAll { switchDeviceIds.contains(it.id) }
                .sort { it.name }
                .collectEntries { [(it.zigbeeId): it.name] }
        }
    } catch (Exception ex) {
        return ['ZZZZ': "Exception: ${ex}"]
    }
}
{{/ @implementation }}
{{!--------------------------------------------------------------------------}}
{{# @updated }}

// Preferences for capability.ZigbeeBindings
if (controlDevice != null && controlDevice != '----') {
    if (controlDevice == '0000') {
        log_info '🛠️ Clearing all device bindings'
        state.stopControlling = 'devices'
    } else {
        log_info "🛠️ Adding binding to device #${controlDevice} for clusters {{params.clusters}}"

        {{# params.clusters }}
        cmds += "he raw 0x${device.deviceNetworkId} 0x00 0x00 0x0021 {49 ${utils_payload "${device.zigbeeId}"} ${utils_payload "${device.endpointId}"} ${utils_payload '{{ . }}'} 03 ${utils_payload "${controlDevice}"} 01} {0x0000}" // Add device binding for cluster {{ . }}
        {{/ params.clusters }}
    }
    device.updateSetting 'controlDevice', [value:'----', type:'enum']
    cmds += "he raw 0x${device.deviceNetworkId} 0x00 0x00 0x0033 {57 00} {0x0000}"
}
{{# params.groups }}

if (controlGroup != null && controlGroup != '----') {
    if (controlGroup == '0000') {
        log_info '🛠️ Clearing all group bindings'
        state.stopControlling = 'groups'
    } else {
        log_info "🛠️ Adding binding to group ${controlGroup} for clusters {{params.clusters}}"
        {{# params.clusters }}
        cmds += "he raw 0x${device.deviceNetworkId} 0x00 0x00 0x0021 {49 ${utils_payload "${device.zigbeeId}"} ${utils_payload "${device.endpointId}"} ${utils_payload '{{ . }}'} 01 ${utils_payload "${controlGroup}"}} {0x0000}" // Add group binding for cluster {{ . }}
        {{/ params.clusters }}
    }
    device.updateSetting 'controlGroup', [value:'----', type:'enum']
    cmds += "he raw 0x${device.deviceNetworkId} 0x00 0x00 0x0033 {57 00} {0x0000}"
}
{{/ params.groups }}
{{/ @updated }}
{{!--------------------------------------------------------------------------}}
{{# @refresh }}

// Refresh for capability.ZigbeeBindings
cmds += "he raw 0x${device.deviceNetworkId} 0x00 0x00 0x0033 {57 00} {0x0000}"  // Start querying the Bindings Table
{{/ @refresh }}
{{!--------------------------------------------------------------------------}}
{{# @events }}

// Events for capability.ZigbeeBindings
// ===================================================================================================================

// Mgmt_Bind_rsp := { 08:Status, 08:BindingTableEntriesTotal, 08:StartIndex, 08:BindingTableEntriesIncluded, 112/168*n:BindingTableList }
// BindingTableList: { 64:SrcAddr, 08:SrcEndpoint, 16:ClusterId, 08:DstAddrMode, 16/64:DstAddr, 0/08:DstEndpoint }
// Example: [71, 00, 01, 00, 01,  C6, 9C, FE, FE, FF, F9, E3, B4,  01,  06, 00,  03,  E9, A6, C9, 17, 00, 6F, 0D, 00,  01]
case { contains it, [endpointInt:0x00, clusterInt:0x8033] }:
    if (msg.data[1] != '00') {
        utils_processedZdpMessage 'Mgmt_Bind_rsp', "Status=FAILED, data=${msg.data}"
        return
    }
    Integer totalEntries = Integer.parseInt msg.data[2], 16
    Integer startIndex = Integer.parseInt msg.data[3], 16
    Integer includedEntries = Integer.parseInt msg.data[4], 16
    if (startIndex == 0) {
        state.remove 'ctrlDev'
        state.remove 'ctrlGrp'
    }
    if (includedEntries == 0) {
        utils_processedZdpMessage 'Mgmt_Bind_rsp', "totalEntries=${totalEntries}, startIndex=${startIndex}, includedEntries=${includedEntries}"
        return
    }

    Integer pos = 5
    Integer deleted = 0
    Map<String, String> allDevices = retrieveSwitchDevices()
    Set<String> devices = []
    Set<String> groups = []
    List<String> cmds = []
    for (int idx = 0; idx < includedEntries; idx++) {
        String srcDeviceId = msg.data[(pos)..(pos + 7)].reverse().join()
        String srcEndpoint = msg.data[pos + 8]
        String cluster = msg.data[(pos + 9)..(pos + 10)].reverse().join()
        String dstAddrMode = msg.data[pos + 11]
        if (dstAddrMode != '01' && dstAddrMode != '03') continue

        // Found device binding
        if (dstAddrMode == '03') {
            String dstDeviceId = msg.data[(pos + 12)..(pos + 19)].reverse().join()
            String dstEndpoint = msg.data[pos + 20]
            String dstDeviceName = allDevices.getOrDefault(dstDeviceId, "Unknown (${dstDeviceId})")
            pos += 21

            // Remove all binds that are not targeting the hub
            if (state.stopControlling == 'devices') {
                if (dstDeviceId != "${location.hub.zigbeeEui}") {
                    log_debug "Removing binding for device ${dstDeviceName} on cluster 0x${cluster}"
                    cmds += "he raw 0x${device.deviceNetworkId} 0x00 0x00 0x0022 {49 ${utils_payload srcDeviceId} ${srcEndpoint} ${utils_payload cluster} 03 ${utils_payload dstDeviceId} ${dstEndpoint}} {0x0000}"
                    deleted++
                }
                continue
            }

            log_debug "Found binding for device ${dstDeviceName} on cluster 0x${cluster}"
            devices.add dstDeviceName
            continue
        }

        // Found group binding
{{# params.groups }}
        String dstGroupId = msg.data[(pos + 12)..(pos + 13)].reverse().join()
        String dstGroupName = GROUPS.getOrDefault(dstGroupId, "Unknown (${dstGroupId})")

        // Remove all group bindings
        if (state.stopControlling == 'groups') {
            log_debug "Removing binding for group ${dstGroupName} on cluster 0x${cluster}"
            cmds += "he raw 0x${device.deviceNetworkId} 0x00 0x00 0x0022 {49 ${utils_payload srcDeviceId} ${srcEndpoint} ${utils_payload cluster} 01 ${utils_payload dstGroupId}} {0x0000}"
            deleted++
        } else {
            log_debug "Found binding for group ${dstGroupName} on cluster 0x${cluster}"
            groups.add dstGroupName
        }
{{/ params.groups }}
        pos += 14
    }

    Set<String> ctrlDev = (state.ctrlDev ?: []).toSet()
    ctrlDev.addAll(devices.findAll { !it.startsWith('Unknown') })
    if (ctrlDev.size() > 0) state.ctrlDev = ctrlDev.unique()

{{# params.groups }}
    Set<String> ctrlGrp = (state.ctrlGrp ?: []).toSet()
    ctrlGrp.addAll(groups.findAll { !it.startsWith('Unknown') })
    if (ctrlGrp.size() > 0) state.ctrlGrp = ctrlGrp.unique()
{{/ params.groups }}
    // Get next batch
    if (startIndex + includedEntries < totalEntries) {
        cmds += "he raw 0x${device.deviceNetworkId} 0x00 0x00 0x0033 {57 ${Integer.toHexString(startIndex + includedEntries - deleted).padLeft(2, '0')}} {0x0000}"
    } else {
        log_info "Current device bindings: ${state.ctrlDev ?: 'None'}"
        log_info "Current group bindings: ${state.ctrlGrp ?: 'None'}"
        state.remove 'stopControlling'
    }
    utils_sendZigbeeCommands cmds
    utils_processedZdpMessage 'Mgmt_Bind_rsp', "totalEntries=${totalEntries}, startIndex=${startIndex}, devices=${devices}, groups=${groups}"
    return
{{/ @events }}
{{!--------------------------------------------------------------------------}}
