{{!--------------------------------------------------------------------------}}
{{# @definition }}
capability 'DoubleTapableButton'
{{/ @definition }}
{{!--------------------------------------------------------------------------}}
{{# @implementation }}

// Implementation for capability.DoubleTapableButton
void doubleTap(BigDecimal buttonNumber) {
    String buttonName = BUTTONS.find { it.value[0] == "${buttonNumber}" }?.value?.getAt(1)
    if (buttonName == null) {
        log_warn "Cannot double tap button ${buttonNumber} because it is not defined"
        return
    }
    utils_sendEvent name:'doubleTapped', value:buttonNumber, type:'digital', isStateChange:true, descriptionText:"Button ${buttonNumber} (${buttonName}) was double tapped"
}
{{/ @implementation }}
{{!--------------------------------------------------------------------------}}
