{{!--------------------------------------------------------------------------}}
{{# @fields }}

// Fields for capability.ZigbeeGroupMember
@Field static final Map<String, String> GROUPS = [
    "9900":"Alfa", "9901":"Bravo", "9902":"Charlie", "9903":"Delta", "9904":"Echo", "9905":"Foxtrot", "9906":"Golf", "9907":"Hotel", "9908":"India", "9909":"Juliett", "990A":"Kilo", "990B":"Lima", "990C":"Mike", "990D":"November", "990E":"Oscar", "990F":"Papa", "9910":"Quebec", "9911":"Romeo", "9912":"Sierra", "9913":"Tango", "9914":"Uniform", "9915":"Victor", "9916":"Whiskey", "9917":"Xray", "9918":"Yankee", "9919":"Zulu"
]
{{/ @fields }}
{{!--------------------------------------------------------------------------}}
{{# @inputs }}
// Inputs for capability.ZigbeeBindings

input(
    name: "joinGroup",
    type: "enum",
    title: "Join a Zigbee group",
    description: "<small>Select a Zigbee group you want to join.</small>",
    options: [ "0000":"❌ Leave all Zigbee groups", "----":"- - - -" ] + GROUPS,
    defaultValue: "----",
    required: false
)
{{/ @inputs }}
{{!--------------------------------------------------------------------------}}
{{# @updated }}

// Preferences for capability.ZigbeeGroupMember

if (joinGroup != null && joinGroup != "----") {
    if (joinGroup == "0000") {
        Log.info "🛠️ Leaving all Zigbee groups"
        cmds += "he raw 0x${device.deviceNetworkId} 0x01 0x${device.endpointId} 0x0004 {0143 04}"  // Leave all groups
    } else {
        String groupName = GROUPS.getOrDefault(joinGroup, "Unknown")
        Log.info "🛠️ Joining group: ${joinGroup} (${groupName})"
        cmds += "he raw 0x${device.deviceNetworkId} 0x01 0x${device.endpointId} 0x0004 {0143 00 ${Utils.payload joinGroup} ${Integer.toHexString(groupName.length()).padLeft(2, "0")}${groupName.getBytes().encodeHex().toString()}}"  // Join group
    }

    device.updateSetting("joinGroup", [value:"----", type:"enum"])
    cmds += "he raw 0x${device.deviceNetworkId} 0x01 0x${device.endpointId} 0x0004 {0143 02 00}"  // Get groups membership
}
{{/ @updated }}
{{!--------------------------------------------------------------------------}}
{{# @events }}

// Events for capability.ZigbeeGroupMember

// Get Group Membership Response Command
case { contains it, [clusterInt:0x0004, commandInt:0x02, direction:"01"] }:
    Integer count = Integer.parseInt msg.data[1], 16
    Set<String> groupNames = []
    for (int pos = 0; pos < count; pos++) {
        String groupId = "${msg.data[pos*2 + 3]}${msg.data[pos*2 + 2]}"
        String groupName = GROUPS.getOrDefault(groupId, "Unknown (${groupId})")
        Log.debug "Found group membership: ${groupName}"
        groupNames.add(groupName)
    }
    state.joinGrp = groupNames.findAll { !it.startsWith("Unknown") }
    if (state.joinGrp.size() == 0) state.remove "joinGrp"
    return Log.info("Current group membership: ${groupNames ?: "None"}")

// Add Group Response
case { contains it, [clusterInt:0x0004, commandInt:0x00, direction:"01"] }:
    String status = msg.data[0] == "00" ? "SUCCESS" : (msg.data[0] == "8A" ? "ALREADY_MEMBER" : "FAILED")
    String groupId = "${msg.data[2]}${msg.data[1]}"
    String groupName = GROUPS.getOrDefault(groupId, "Unknown (${groupId})")
    return Utils.processedZclMessage("Add Group Response", "Status=${status}, groupId=${groupId}, groupName=${groupName}")

// Leave Group Response
case { contains it, [clusterInt:0x0004, commandInt:0x03, direction:"01"] }:
    String status = msg.data[0] == "00" ? "SUCCESS" : (msg.data[0] == "8B" ? "NOT_A_MEMBER" : "FAILED")
    String groupId = "${msg.data[2]}${msg.data[1]}"
    String groupName = GROUPS.getOrDefault(groupId, "Unknown (${groupId})")
    return Utils.processedZclMessage("Left Group Response", "Status=${status}, groupId=${groupId}, groupName=${groupName}")
{{/ @events }}
{{!--------------------------------------------------------------------------}}
