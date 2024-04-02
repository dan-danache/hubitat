{{!--------------------------------------------------------------------------}}
{{# @definition }}
capability 'PushableButton'
{{/ @definition }}
{{!--------------------------------------------------------------------------}}
{{# @fields }}

// Fields for capability.PushableButton
@Field static final Map<String, List<String>> BUTTONS = [
    {{# params.buttons }}
    '{{ id }}': ['{{ number }}', '{{ name }}'],
    {{/ params.buttons }}
]
{{/ @fields }}
{{!--------------------------------------------------------------------------}}
{{# @configure }}

// Configuration for capability.PushableButton
Integer numberOfButtons = BUTTONS.count { true }
sendEvent name:'numberOfButtons', value:numberOfButtons, descriptionText:"Number of buttons is ${numberOfButtons}"
{{/ @configure }}
{{!--------------------------------------------------------------------------}}
{{# @implementation }}

// Implementation for capability.PushableButton
void push(BigDecimal buttonNumber) {
    String buttonName = BUTTONS.find { it.value[0] == "${buttonNumber}" }?.value?.getAt(1)
    if (buttonName == null) {
        log_warn "Cannot push button ${buttonNumber} because it is not defined"
        return
    }
    utils_sendEvent name:'pushed', value:buttonNumber, type:'digital', isStateChange:true, descriptionText:"Button ${buttonNumber} (${buttonName}) was pressed"
}
{{/ @implementation }}
{{!--------------------------------------------------------------------------}}
