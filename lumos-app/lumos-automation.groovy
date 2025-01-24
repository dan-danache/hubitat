import groovy.transform.Field

definition (
    name: "Lumos Automation 1.1.0",
    namespace: 'dandanache',
    author: 'Dan Danache',
    description: 'Control lights using motion and contact sensors.',
    documentationLink: 'https://community.hubitat.com/t/zigbee-visual-render-for-getchildandrouteinfo/119074',
    importUrl: 'https://raw.githubusercontent.com/dan-danache/hubitat/lumos-app_1.0.0/lumos-app/lumos-automation.groovy',
    category: 'Control',
    iconUrl: '',
    iconX2Url: '',
    parent: "dandanache:Lumos 1.1.0",
)

preferences {
    page name: 'mainPage'
}

def mainPage() {
    dynamicPage (name:'mainPage', install:true, uninstall:true) {
        section {
            label description:'Enter a name for this automation', required:true
        }

        section (title:'Sensors that will be used to control the lights') {
            input 'motionSensors', 'capability.motionSensor', title:'Select motion sensors:', multiple:true, required:true, width:4
            paragraph ''
            input 'contactSensors', 'capability.contactSensor', title:'Select contact sensors:', multiple:true, required:true, width:4
        }

        section (title:'Lights to control') {
            input 'lights', 'capability.switch', title:'Select lights:', multiple:true, required:true, width:4
            input 'disableLightsOut', 'bool', title:'Keep lights on when door is opened', defaultValue:false, submitOnChange:true
        }

        section {
            input(
                name: 'logging',
                type: 'enum',
                title: 'Logging',
                options: ['Events', 'Triggers', 'Actions'],
                multiple: true,
                submitOnChange: true,
                width: 4
            )
        }
    }
}

void installed() {
    info 'installed()'
    initialize()
}

void updated() {
    info 'updated()'
    initialize()
}

void initialize() {
    info 'initialize()'
    state.presence = 'not present'

    unschedule()
    unsubscribe()

    subscribe(motionSensors, 'motion', 'motionHandler')
    subscribe(contactSensors, 'contact', 'contactHandler')

    turnOff()
}

void motionHandler(evt) {
    if ('Events' in logging) info "Event: ${evt.device.displayName} motion is ${evt.value}"

    // Motion was detected
    if (evt.value == 'active') {

        if ('Triggers' in logging) info 'Triggered: Person detected -> Turning on all lights'
        turnOn()

        if (contactSensors.every { it.currentState('contact').value == 'closed' }) {
            if ('Triggers' in logging) info 'Triggered: Person detected and all doors are closed -> Keeping all lights on until a door is opened'
            state.presence = 'present'
        }
        return
    }

    // Motion is inactive, but presence is 'present' -> Do nothing
    if (state.presence == 'present') {
        if ('Triggers' in logging) info 'Triggered: Person not detected anymore, but presence is active -> Keeping all lights on until a door is opened'
        return
    }

    // No more motion detected, but lights are already OFF -> Do nothing
    if (lights.every { it.currentState('switch').value == 'off' }) {
        if ('Triggers' in logging) info 'Triggered: Person not detected anymore, but all lights are already OFF -> Do nothing'
        return
    }

    // Bail out if other sensors still detect motion
    if (motionSensors.any { it.currentState('motion').value == 'active' }) {
         if ('Triggers' in logging) info 'Triggered: Person not detected anymore, but at least one other motion sensor is still active -> Do nothing'
         return
    }

    if ('Triggers' in logging) info 'Triggered: Person not detected anymore -> Turning off all lights after 5 minutes'
    unschedule()
    runIn 240, 'turnOff'
}

void contactHandler(evt) {
    if ('Events' in logging) info "Event: ${evt.device.displayName} contact is ${evt.value}"

    // A door was opened
    if (evt.value == 'open') {
        if (state.presence == 'present') {
            if (disableLightsOut != true) {
                if ('Triggers' in logging) info 'Triggered: Person is leaving the room -> Turning off the lights'
                turnOff()
            } else {
                if ('Triggers' in logging) info 'Triggered: Person is leaving the room -> Leaving lights unchanged'
            }
            state.presence = 'not present'
            return
        }

        if ('Triggers' in logging) info 'Triggered: Person is entering the room -> Turning on the lights for 5 minutes'
        turnOn()
        runIn 240, 'turnOff'
        return
    }
}

private void turnOn() {
    unschedule()
    if (lights.every { it.currentState('switch').value == 'on' }) return

    lights.on()
    if ('Actions' in logging) info "Action: All lights turned on: ${lights}"
}

private void turnOff() {
    unschedule()
    if (lights.every { it.currentState('switch').value == 'off' }) return

    lights.off()
    if ('Actions' in logging) info "Action: All lights turned off: ${lights}"
}

private void info(String message) {
    log.info "${app.label} ▸ ${message}"
}
