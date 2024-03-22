// Arm := { 16:Button, 08:ArmMode, ??:ArmDisarmCode, 08:ZoneId}
// ArmMode := { 0x00:Disarm, 0x01:Arm Day/Home Zones Only, 0x02:Arm Night/Sleep Zones Only, 0x03:Arm All Zones }
// [00, 00, 00, 00, 00, 00, 00, 00, 00, 00] -> Home button
// [02, 00, 00, 00, 00, 00, 00, 00, 00, 00] -> Night button
// [03, 00, 00, 00, 00, 00, 00, 00, 00, 00] -> Away button
case { contains it, [clusterInt:0x0501, commandInt:0x00, isClusterSpecific:true] }:
   switch (msg.data[0]) {
        case "00":
            def button = BUTTONS.HOME
            return Utils.sendEvent(name:"pushed", value:button[0], type:"physical", isStateChange:true, descriptionText:"Button ${button[0]} (${button[1]}) was pushed")

        case "02":
            def button = BUTTONS.NIGHT
            return Utils.sendEvent(name:"pushed", value:button[0], type:"physical", isStateChange:true, descriptionText:"Button ${button[0]} (${button[1]}) was pushed")

        case "03":
            def button = BUTTONS.AWAY
            return Utils.sendEvent(name:"pushed", value:button[0], type:"physical", isStateChange:true, descriptionText:"Button ${button[0]} (${button[1]}) was pushed")
    }

    return Log.error("Sent unexpected Zigbee message: description=${description}, msg=${msg}")

// Panic
case { contains it, [clusterInt:0x0501, commandInt:0x04, isClusterSpecific:true] }:
    def button = BUTTONS.PANIC
    return Utils.sendEvent(name:"pushed", value:button[0], type:"physical", isStateChange:true, descriptionText:"Button ${button[0]} (${button[1]}) was pushed")

// Read Attributes: ZoneStatus
case { contains it, [clusterInt:0x0500, commandInt:0x01, attrInt:0x0002] }:
    return Utils.processedZclMessage("Read Attributes Response", "ZoneStatus=${msg.value}")
