// Button was pressed := { 16:Button, 08:EventType, 08:NextValueType, 08:Action, 08:NextValueType, 16:DurationRotation}
// EventType := { 0x00:Button, 0x01:Rotary }
// Action := { 0x00:Press, 0x01:Hold/Start, 0x02:Release/Repeat, 0x03:LongRelease }
// [02, 00,  00,  30,  02,  21,  01, 00] -> Button=2(0x0002), EventType=Button(0x00), NextValueType=enum8(0x30), Action=Release(0x02), NextValueType=uint16(0x21), DurationRotation=0x0001
case { contains it, [clusterInt:0xFC00, commandInt:0x00] }:
    def button = BUTTONS[msg.data[0]]

    // Dimmer Mode: Only listen to Release (02), Hold (01) and LongRelease (03)
    switch (msg.data[4]) {
        case "02": return Utils.sendEvent(name:"pushed", value:button[0], type:"physical", isStateChange:true, descriptionText:"Button ${button[0]} (${button[1]}) was pushed")
        case "01": return Utils.sendEvent(name:"held", value:button[0], type:"physical", isStateChange:true, descriptionText:"Button ${button[0]} (${button[1]}) was held")
        case "03": return Utils.sendEvent(name:"released", value:button[0], type:"physical", isStateChange:true, descriptionText:"Button ${button[0]} (${button[1]}) was released")
    }
    return

// Other events that we expect but are not usefull
case { contains it, [clusterInt:0x0000, commandInt:0x04, isClusterSpecific:false] }:
    return Utils.processedZclMessage("Write Attribute Response", "attribute=Philips magic attribute")
