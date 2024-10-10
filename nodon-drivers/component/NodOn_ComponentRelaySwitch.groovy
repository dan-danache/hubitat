/**
 * NodOn Component Relay Switch
 *
 * @see https://dan-danache.github.io/hubitat/nodon-drivers/
 */
metadata {
    definition(name:'NodOn Component Relay Switch', component:true, namespace:'dandanache', author:'Dan Danache', importUrl:'https://raw.githubusercontent.com/dan-danache/hubitat/master/nodon-drivers/NodOn_ComponentRelaySwitch.groovy') {
        capability 'Actuator'
        capability 'Light'
        capability 'Switch'
        capability 'RelaySwitch'
        capability 'Refresh'
    }

    command 'toggle'
    command 'onWithTimedOff', [[name:'On duration*', type:'NUMBER', description:'After how many seconds power will be turned Off [1..6500]']]

    preferences {
        input(
            name: 'powerOnBehavior',
            type: 'enum',
            title: 'Power On behaviour',
            description: '<small>Select what happens after a power outage.</small>',
            options: ['TURN_POWER_ON':'Turn power On', 'TURN_POWER_OFF':'Turn power Off', 'RESTORE_PREVIOUS_STATE':'Restore previous state'],
            defaultValue: 'RESTORE_PREVIOUS_STATE',
            required: true
        )
    }
}

// Called when the "Save Preferences" button is clicked
List<String> updated(boolean auto = false) {
    log_info "🎬 Saving preferences${auto ? ' (auto)' : ''} ..."

    if (powerOnBehavior == null) {
        powerOnBehavior = 'RESTORE_PREVIOUS_STATE'
        device.updateSetting 'powerOnBehavior', [value:powerOnBehavior, type:'enum']
    }
    log_info "🛠️ powerOnBehavior = ${powerOnBehavior}"
    parent?.componentUpdatePowerOnBehavior(this.device, powerOnBehavior)
}

void parse(String description) { log.warn 'parse(String description) not implemented' }

void parse(List<Map> description) {
    description.each { utils_sendEvent it }
}

void on() {
    parent?.componentOn(this.device)
}

void off() {
    parent?.componentOff(this.device)
}

void toggle() {
    parent?.componentToggle(this.device)
}

void onWithTimedOff(BigDecimal onTime = 1) {
    parent?.componentOnWithTimedOff(this.device, onTime)
}

void refresh() {
    parent?.componentRefresh(this.device)
}

// ===================================================================================================================
// Logging helpers (something like this should be part of the SDK and not implemented by each driver)
// ===================================================================================================================

private void log_debug(String message) {
    if (parent?.logLevel == '1') log.debug "${device.displayName} ${message.uncapitalize()}"
}
private void log_info(String message) {
    if (parent?.logLevel <= '2') log.info "${device.displayName} ${message.uncapitalize()}"
}
private void log_warn(String message) {
    if (parent?.logLevel <= '3') log.warn "${device.displayName} ${message.uncapitalize()}"
}
private void log_error(String message) {
    log.error "${device.displayName} ${message.uncapitalize()}"
}

// ===================================================================================================================
// Helper methods (keep them simple, keep them dumb)
// ===================================================================================================================

private void utils_sendEvent(Map event) {
    if (device.currentValue(event.name, true) != event.value || event.isStateChange) {
        log_info "${event.descriptionText} [${event.type}]"
    } else {
        log_debug "${event.descriptionText} [${event.type}]"
    }
    sendEvent event
}
