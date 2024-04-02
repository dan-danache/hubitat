{{!--------------------------------------------------------------------------}}
{{# @definition }}
capability 'ReleasableButton'
{{/ @definition }}
{{!--------------------------------------------------------------------------}}
{{# @implementation }}

// Implementation for capability.ReleasableButton
void release(BigDecimal buttonNumber) {
    String buttonName = BUTTONS.find { it.value[0] == "${buttonNumber}" }?.value?.getAt(1)
    if (buttonName == null) {
        log_warn "Cannot release button ${buttonNumber} because it is not defined"
        return
    }
    utils_sendEvent name:'released', value:buttonNumber, type:'digital', isStateChange:true, descriptionText:"Button ${buttonNumber} (${buttonName}) was released"
}
{{/ @implementation }}
{{!--------------------------------------------------------------------------}}
