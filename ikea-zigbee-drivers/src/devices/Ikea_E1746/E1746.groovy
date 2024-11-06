{{!--------------------------------------------------------------------------}}
{{# @definition }}
capability 'SignalStrength'
{{/ @definition }}
{{!--------------------------------------------------------------------------}}
{{# @attributes }}

// Attributes for devices.Ikea_E1746
attribute 'resets', 'number'
attribute 'macRxBcast', 'number'
attribute 'macTxBcast', 'number'
attribute 'apsRxBcast', 'number'
attribute 'apsTxBcast', 'number'
attribute 'nwkDropped', 'number'
attribute 'memFailures', 'number'
attribute 'macRetries', 'number'
{{/ @attributes }}
{{!--------------------------------------------------------------------------}}
{{# @updated }}

// Preferences for devices.Ikea_E1746
schedule '0 */10 * ? * *', 'refresh'
{{/ @updated }}
{{!--------------------------------------------------------------------------}}
{{# @refresh }}

// Refresh for devices.Ikea_E1746
cmds += zigbee.readAttribute(0x0B05, [0x0000, 0x0100, 0x0101, 0x0106, 0x0107, 0x0112, 0x0117, 0x011B, 0x011C, 0x011D])
{{/ @refresh }}
{{!--------------------------------------------------------------------------}}
{{# @events }}

// Events for devices.Ikea_E1746
// ===================================================================================================================
case { contains it, [clusterInt:0x0B05, commandInt:0x01, attrInt:0x0000] }:
    Integer resets = Integer.parseInt msg.value, 16
    utils_sendEvent name:'resets', value:resets, descriptionText:"Device resets = ${resets}", type:type
    
    msg.additionalAttrs?.each {
        switch (it.attrInt) {
            case 0x0100:
                Long macRxBcast = Long.parseLong it.value, 16
                utils_sendEvent name:'macRxBcast', value:macRxBcast, descriptionText:"macRxBcast = ${macRxBcast}", type:type
                return
            case 0x0101:
                Long macTxBcast = Long.parseLong it.value, 16
                utils_sendEvent name:'macTxBcast', value:macTxBcast, descriptionText:"macTxBcast = ${macTxBcast}", type:type
                return
            case 0x0106:
                Integer apsRxBcast = Integer.parseInt it.value, 16
                utils_sendEvent name:'apsRxBcast', value:apsRxBcast, descriptionText:"apsRxBcast = ${apsRxBcast}", type:type
                return
            case 0x0107:
                Integer apsTxBcast = Integer.parseInt it.value, 16
                utils_sendEvent name:'apsTxBcast', value:apsTxBcast, descriptionText:"apsTxBcast = ${apsTxBcast}", type:type
                return
            case 0x0112:
                Integer nwkDropped = Integer.parseInt it.value, 16
                utils_sendEvent name:'nwkDropped', value:nwkDropped, descriptionText:"nwkDropped = ${nwkDropped}", type:type
                return
            case 0x0117:
                Integer memFailures = Integer.parseInt it.value, 16
                utils_sendEvent name:'memFailures', value:memFailures, descriptionText:"memFailures = ${memFailures}", type:type
                return
            case 0x011B:
                Integer macRetries = Integer.parseInt it.value, 16
                utils_sendEvent name:'macRetries', value:macRetries, descriptionText:"macRetries = ${macRetries}", type:type
                return
            case 0x011C:
                Integer lqi = Integer.parseInt it.value, 16
                utils_sendEvent name:'lqi', value:lqi, descriptionText:"Signal LQI is ${lqi}", type:type
                return
            case 0x011D:
                byte rssi = (byte) Integer.parseInt(it.value, 16)
                utils_sendEvent name:'rssi', value:rssi, descriptionText:"Signal RSSI is ${rssi}", type:type
                return
        }
    }
    utils_processedZclMessage "Read Attributes Response", "resets=${resets}"
    return
{{/ @events }}
{{!--------------------------------------------------------------------------}}
