{{!--------------------------------------------------------------------------}}
{{# @definition }}
capability 'HoldableButton'
{{/ @definition }}
{{!--------------------------------------------------------------------------}}
{{# @implementation }}

// Implementation for capability.HoldableButton
void hold(BigDecimal buttonNumber) {
    String buttonName = BUTTONS.find { it.value[0] == "${buttonNumber}" }?.value?.getAt(1)
    if (buttonName == null) {
        log_warn "Cannot hold button ${buttonNumber} because it is not defined"
        return
    }
    utils_sendEvent name:'held', value:buttonNumber, type:'digital', isStateChange:true, descriptionText:"Button ${buttonNumber} (${buttonName}) was held"
}
{{/ @implementation }}
{{!--------------------------------------------------------------------------}}
