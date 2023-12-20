// Button was pushed
case { contains it, [clusterInt:0xFC80, commandInt:0x01] }:
    def button = msg.endpointInt == 0x01 ? BUTTONS.DOT_1 : BUTTONS.DOT_2
    return Utils.sendEvent(name:"pushed", value:button[0], type:"physical", isStateChange:true, descriptionText:"Button ${button[0]} (${button[1]}) was pushed")

// Button was held
case { contains it, [clusterInt:0xFC80, commandInt:0x02] }:
    def button = msg.endpointInt == 0x01 ? BUTTONS.DOT_1 : BUTTONS.DOT_2
    return Utils.sendEvent(name:"held", value:button[0], type:"physical", isStateChange:true, descriptionText:"Button ${button[0]} (${button[1]}) was held")

// IGNORED: Button was short-released
case { contains it, [clusterInt:0x0008, commandInt:0x03] }:
    def button = device.currentValue("held", true) == 1 ? BUTTONS.ON : BUTTONS.OFF
    return Log.debug("Button ${button[0]} (${button[1]}) was short-released (ignored)")

// Button was released
case { contains it, [clusterInt:0x0008, commandInt:0x04] }:
    def button = msg.endpointInt == 0x01 ? BUTTONS.DOT_1 : BUTTONS.DOT_2
    return Utils.sendEvent(name:"released", value:button[0], type:"physical", isStateChange:true, descriptionText:"Button ${button[0]} (${button[1]}) was released")

// Button was double tapped
case { contains it, [clusterInt:0x0008, commandInt:0x06] }:
    def button = msg.endpointInt == 0x01 ? BUTTONS.DOT_1 : BUTTONS.DOT_2
    return Utils.sendEvent(name:"doubleTapped", value:button[0], type:"physical", isStateChange:true, descriptionText:"Button ${button[0]} (${button[1]}) was double tapped")
