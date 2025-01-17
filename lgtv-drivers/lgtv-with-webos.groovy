import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import groovy.transform.CompileStatic
import groovy.transform.Field

import hubitat.device.HubAction
import hubitat.device.Protocol

@Field static final String DRIVER_NAME = 'LGTV with webOS'
@Field static final String DRIVER_VERSION = '1.1.0'
@Field static final JsonSlurper JSON_SLURPER = new JsonSlurper()

metadata {
    definition(name:DRIVER_NAME, namespace:'dandanache', author:'Dan Danache', importUrl:'https://raw.githubusercontent.com/dan-danache/hubitat/main/lgtv-drivers/lgtv-with-webos.groovy') {
        capability 'Actuator'
        capability 'Refresh'
        capability 'Switch'
        capability 'AudioVolume'
        capability 'TV'
        capability 'Notification'
        capability 'MediaController'

        attribute 'sessionStatus', 'enum', ['offline', 'online', 'unknown']
        attribute 'channelName', 'string'
        attribute 'screen', 'enum', ['on', 'off', 'standby']
        attribute 'soundOutput', 'string'
    }

    command 'deviceNotification', [
        [name:'Text', type:'STRING', description:'Notification text*'],
        [name:'Type', type:'ENUM', description:'Notification type', constraints:['Toast - Goes away after few seconds', 'Alert - Stays on screen until dismissed']]
    ]
    command 'setChannel', [[name:'Channel number*', type:'NUMBER']]
    command 'screenOn'
    command 'screenOff'

    preferences {
        input(
            name:'logLevel', type:'enum', title:'Log verbosity', required:true,
            description: 'Choose the messages that appear in the "Logs" section',
            options: [
                '1' : 'Debug - log everything',
                '2' : 'Info - log important events',
                '3' : 'Warning - log events that require attention',
                '4' : 'Error - log errors'
            ],
            defaultValue: '1'
        )
        input(
            name:'ipAddr', type:'string', title:'IP address', required:true,
            description: 'Enter the device IP address'
        )
        input(
            name:'useSSL', type:'bool', title:'Enable SSL', required:true,
            description:'Enable SSL when connecting to the TV websocket',
            defaultValue:true
        )
    }
}

// Called when the device is first added
void installed() {
    log_warn 'Installing device ...'

    // Init state
    state.activities = [:]
    utils_sendEvent name:'switch', value:'off', descriptionText:'Power initialized to off', type:'digital'
}

// Called when the "Save Preferences" button is clicked
void updated(boolean auto = false) {
    log_info "Saving preferences${auto ? ' (auto)' : ''} ..."

    if (logLevel == null) {
        logLevel = '1'
        device.updateSetting('logLevel', [value:logLevel, type:'enum'])
    }
    if (logLevel == '1') runIn 1800, 'logsOff'
    log_info "🛠️ logLevel = ${logLevel}"

    if (useSSL == null) {
        useSSL = true
        device.updateSetting('useSSL', [value:useSSL, type:'enum'])
    }
    log_info "🛠️ useSSL = ${useSSL}"

    // Auto-connect
    connect()

    // Start polling
    unschedule()
    schedule '0 0/1 * ? * * *', 'startupPolling'
}

void logsOff() {
    log_info '⏲️ Automatically reverting log level to "Info"'
    device.updateSetting 'logLevel', [value:'2', type:'enum']
}

// ===================================================================================================================
// Implement Capabilities
// ===================================================================================================================

// capability.Refresh
void refresh() {
    utils_sendMessage([type:'request', uri:'ssap://system/getSystemInfo'])
    utils_sendMessage([type:'request', uri:'ssap://com.webos.service.update/getCurrentSWInformation'])
    utils_sendMessage([type:'request', uri:'ssap://audio/getVolume'])
    utils_sendMessage([type:'request', uri:'ssap://tv/getCurrentChannel'])
    utils_sendMessage([type:'request', uri:'ssap://com.webos.service.connectionmanager/getinfo'])
    getAllActivities()

    //utils_sendMessage([type:'request', uri:'ssap://config/getConfigs', payload:[configNames:['tv.model.*']]])
}

// capability.Switch
void on() {
    util_wakeOnLan(getDataValue('wifiMacAddress'))
    util_wakeOnLan(getDataValue('wiredMacAddress'))

    // Start websocket in 7 seconds
    runIn 7, 'connect'
}
void off() {
    utils_sendMessage([type:'request', uri:'ssap://system/turnOff'])
}

// capability.AudioVolume
void mute() {
    utils_sendMessage([type:'request', uri:'ssap://audio/setMute', payload:[mute:true]])
}
void unmute() {
    utils_sendMessage([type:'request', uri:'ssap://audio/setMute', payload:[mute:false]])
}
void volumeUp() {
    utils_sendMessage([type:'request', uri:'ssap://audio/volumeUp'])
}
void volumeDown() {
    utils_sendMessage([type:'request', uri:'ssap://audio/volumeDown'])
}
void setVolume(String level) { setVolume Integer.parseInt(level) }
void setVolume(BigDecimal level) {
    if (level < 0 || level > 100) return
    utils_sendMessage([type:'request', uri:'ssap://audio/setVolume', payload:[volume:level]])
}

// capability.TV
void channelUp() {
    utils_sendMessage([type:'request', uri:'ssap://tv/channelUp'])
}
void channelDown() {
    utils_sendMessage([type:'request', uri:'ssap://tv/channelDown'])
}

// capability.Notification
void deviceNotification(String text, String type = 'Toast') {
    if (type.startsWith('Toast')) {
        utils_sendMessage([type:'request', uri:'"ssap://system.notifications/createToast', payload:[message:"${text}"]])
    } else {
        utils_sendMessage([type:'request', uri:'"ssap://system.notifications/createAlert', payload:[message:"${text}", buttons:[[label:'Dismiss']]]])
    }
}

// capability.MediaController
void getAllActivities() {

    // Remove old activities
    Map<String, String> activities = [:]
    activities['com.webos.app.home'] = 'Home'
    activities['com.webos.app.livetv'] = 'Live TV'
    activities['com.webos.app.miracast'] = 'Miracast'
    state.activities = activities

    utils_sendMessage([type:'request', uri:'ssap://tv/getExternalInputList'])
    utils_sendMessage([type:'request', uri:'ssap://com.webos.applicationManager/listLaunchPoints'])
}
void getCurrentActivity() {
    utils_sendMessage([type:'request', uri:'ssap://com.webos.applicationManager/getForegroundAppInfo'])
}
void startActivity(String activityname) {
    String appId = state.activities.find { it.value == activityname }?.key ?: activityname
    log_debug "Launching ${activityname} (${appId})"
    utils_sendMessage([type:'request', uri:'ssap://system.launcher/launch', payload:[id:appId]])
}

// ===================================================================================================================
// Implement custom commands
// ===================================================================================================================
void setChannel(String channel) { setChannel Integer.parseInt(channel) }
void setChannel(BigDecimal channel) {
    utils_sendMessage([type:'request', uri:'ssap://tv/openChannel', payload:[channelNumber:"${channel}"]])
}
void screenOn() {
    utils_sendMessage([type:'request', uri:'ssap://com.webos.service.tvpower/power/turnOnScreen', payload:[standbyMode:'active']])
}
void screenOff() {
    utils_sendMessage([type:'request', uri:'ssap://com.webos.service.tvpower/power/turnOffScreen', payload:[standbyMode:'active']])
}

// ===================================================================================================================
// Websocket helpers
// ===================================================================================================================

void connect() {
    if (!ipAddr) {
        log_warn 'Device IP address not set in Preferences tab. Make sure you are using DHCP reservation for the TV\'s IP address!'
        return
    }

    log_debug "Connecting to ${ipAddr} ..."
    disconnect()
    interfaces.webSocket.connect(useSSL ? "wss://${ipAddr}:3001/" : "ws://${ipAddr}:3000/", headers: ['Content-Type': 'application/json'], ignoreSSLIssues: true)
}

void disconnect() {
    try { interfaces.webSocket.close() } catch (e) { }
}

void startupPolling() {
    if (!ipAddr || "${device.currentValue('sessionStatus', true)}" == 'online') return
    log_debug 'Trying to connect ...'
    interfaces.webSocket.connect(useSSL ? "wss://${ipAddr}:3001/" : "ws://${ipAddr}:3000/", headers: ['Content-Type': 'application/json'], ignoreSSLIssues: true)
}

void register() {
    utils_sendMessage([
        type: 'register',
        payload: [
            'client-key': state.pk ?: '',
            pairingType: 'PROMPT',
            manifest: [
                appVersion: '1.1',
                manifestVersion: 1,
                permissions: ['LAUNCH', 'LAUNCH_WEBAPP', 'APP_TO_APP', 'CLOSE', 'TEST_OPEN', 'TEST_PROTECTED', 'CONTROL_AUDIO', 'CONTROL_DISPLAY', 'CONTROL_INPUT_JOYSTICK', 'CONTROL_INPUT_MEDIA_RECORDING', 'CONTROL_INPUT_MEDIA_PLAYBACK', 'CONTROL_INPUT_TV', 'CONTROL_POWER', 'READ_APP_STATUS', 'READ_CURRENT_CHANNEL', 'READ_INPUT_DEVICE_LIST', 'READ_NETWORK_STATE', 'READ_RUNNING_APPS', 'READ_TV_CHANNEL_LIST', 'WRITE_NOTIFICATION_TOAST', 'READ_POWER_STATE', 'READ_COUNTRY_INFO', 'READ_SETTINGS', 'CONTROL_TV_SCREEN', 'CONTROL_TV_STANBY', 'CONTROL_FAVORITE_GROUP', 'CONTROL_USER_INFO', 'CHECK_BLUETOOTH_DEVICE', 'CONTROL_BLUETOOTH', 'CONTROL_TIMER_INFO', 'STB_INTERNAL_CONNECTION', 'CONTROL_RECORDING', 'READ_RECORDING_STATE', 'WRITE_RECORDING_LIST', 'READ_RECORDING_LIST', 'READ_RECORDING_SCHEDULE', 'WRITE_RECORDING_SCHEDULE', 'READ_STORAGE_DEVICE_LIST', 'READ_TV_PROGRAM_INFO', 'CONTROL_BOX_CHANNEL', 'READ_TV_ACR_AUTH_TOKEN', 'READ_TV_CONTENT_STATE', 'READ_TV_CURRENT_TIME', 'ADD_LAUNCHER_CHANNEL', 'SET_CHANNEL_SKIP', 'RELEASE_CHANNEL_SKIP', 'CONTROL_CHANNEL_BLOCK', 'DELETE_SELECT_CHANNEL', 'CONTROL_CHANNEL_GROUP', 'SCAN_TV_CHANNELS', 'CONTROL_TV_POWER', 'CONTROL_WOL'],
                signatures: [[
                    signatureVersion: 1,
                    signature: 'eyJhbGdvcml0aG0iOiJSU0EtU0hBMjU2Iiwia2V5SWQiOiJ0ZXN0LXNpZ25pbmctY2VydCIsInNpZ25hdHVyZVZlcnNpb24iOjF9.hrVRgjCwXVvE2OOSpDZ58hR+59aFNwYDyjQgKk3auukd7pcegmE2CzPCa0bJ0ZsRAcKkCTJrWo5iDzNhMBWRyaMOv5zWSrthlf7G128qvIlpMT0YNY+n/FaOHE73uLrS/g7swl3/qH/BGFG2Hu4RlL48eb3lLKqTt2xKHdCs6Cd4RMfJPYnzgvI4BNrFUKsjkcu+WD4OO2A27Pq1n50cMchmcaXadJhGrOqH5YmHdOCj5NSHzJYrsW0HPlpuAx/ECMeIZYDh6RMqaFM2DXzdKX9NmmyqzJ3o/0lkk/N97gfVRLW5hA29yeAwaCViZNCP8iC9aO0q9fQojoa7NQnAtw==',
                ]],
                signed: [
                    created: '20140509',
                    appId: 'com.lge.test',
                    vendorId: 'com.lge',
                    localizedAppNames: ['':'LG Remote App', 'ko-KR': '리모컨 앱', 'zxx-XX': 'ЛГ Rэмotэ AПП'],
                    localizedVendorNames: ['': 'LG Electronics'],
                    permissions: ['TEST_SECURE', 'CONTROL_INPUT_TEXT', 'CONTROL_MOUSE_AND_KEYBOARD', 'READ_INSTALLED_APPS', 'READ_LGE_SDX', 'READ_NOTIFICATIONS', 'SEARCH', 'WRITE_SETTINGS', 'WRITE_NOTIFICATION_ALERT', 'CONTROL_POWER', 'READ_CURRENT_CHANNEL', 'READ_RUNNING_APPS', 'READ_UPDATE_INFO', 'UPDATE_FROM_REMOTE_APP', 'READ_LGE_TV_INPUT_EVENTS', 'READ_TV_CURRENT_TIME'],
                    serial: '2f930e2d2cfe083771f68e4fe7bb07'
                ]
            ]
        ]
    ])
}

// ===================================================================================================================
// Websocket callback
// ===================================================================================================================

void webSocketStatus(String message) {
    log_debug "Websocket status changed: ${message}"

    String sessionStatus = utils_parseStatus message

    // If websocket just opened, say hello (skip state checks)
    if (sessionStatus == 'online') {
        utils_sendMessage([type:'hello', id:'hello'], false)
        return
    }

    // Update "sessionStatus" attribute
    utils_sendEvent name:'sessionStatus', value:sessionStatus, descriptionText:"Websocket status is ${sessionStatus}", type:'physical'
}

void parse(String description) {
    log_debug "▶ Received message: ${description}"

    Map msg = JSON_SLURPER.parseText(description)
    String type = msg.id?.startsWith('hubitat_') || msg.payload?.callerId == 'secondscreen.client' || msg.payload?.callerId == 'com.webos.service.apiadapter' ? 'digital' : 'physical'

    // Empty reponse. Thanks for nothing!
    if (msg.payload.keySet().size() == 1 && msg.payload.returnValue == true) return

    switch (msg.type) {

        // Hello response
        case 'hello':
            log_debug '▶ Proper protocol has been observed. Starting authentication ...'

            // Update "sessionStatus" and "switch" attributes
            utils_sendEvent name:'switch', value:'on', descriptionText:'Power is on', type:'physical'
            utils_sendEvent name:'sessionStatus', value:'online', descriptionText:'Websocket status is online', type:'physical'

            // Start registration
            runIn 1, 'register'
            return

        // Register successfull
        case 'registered':
            log_debug '▶ Authentication complete. Oh yeah!'
            state.pk = msg.payload['client-key']

            // Setup subscriptions
            utils_sendMessage([type:'subscribe', uri:'ssap://audio/getStatus'])
            utils_sendMessage([type:'subscribe', uri:'ssap://tv/getCurrentChannel'])
            utils_sendMessage([type:'subscribe', uri:'ssap://com.webos.applicationManager/getForegroundAppInfo'])
            utils_sendMessage([type:'subscribe', uri:'ssap://com.webos.service.tvpower/power/getPowerState'])
            utils_sendMessage([type:'subscribe', uri:'ssap://com.webos.service.apiadapter/audio/getSoundOutput'])

            // Say hello
            refresh()
            return

        // TV sent an error message
        case 'error':
            log_error "▶ Received error message from TV: ${msg.error}"
            return

        // TV sent a response to a command sent by us
        case 'response':
            Map payload = msg.payload
            switch (payload) {

                // Volume status
                case { contains it, [volumeStatus:null] }:
                    int volume = payload.volumeStatus.volume
                    String mute = payload.volumeStatus.muteStatus ? 'muted' : 'unmuted'
                    utils_sendEvent name:'volume', value:volume, unit:'%', descriptionText:"Sound volume is ${volume}%", type:type
                    utils_sendEvent name:'mute', value:mute, descriptionText:"Sound is ${mute}", type:type
                    return

                // Current channel information
                case { contains it, [channelNumber:null] }:
                    String channel = payload.channelNumber
                    String channelName = payload.channelName ?: 'Unknown'
                    utils_sendEvent name:'channel', value:channel, descriptionText:"Channel is ${channel}", type:type
                    utils_sendEvent name:'channelName', value:channelName, descriptionText:"Channel name is ${channelName}", type:type
                    return

                // Shutdown
                case { contains it, [appId:'', processId:'', windowId:''] }:
                    utils_sendEvent name:'switch', value:'off', descriptionText:'Power is off', type:type
                    disconnect()
                    return

                // Current app changed
                case { contains it, [appId:null] }:
                    utils_sendEvent name:'switch', value:'on', descriptionText:'Power is on', type:type
                    String currentActivity = state.activities?.find { it.key == payload.appId }?.value ?: 'Unknown'
                    utils_sendEvent name:'currentActivity', value:currentActivity, descriptionText:"Current activity is ${currentActivity}", type:type
                    return

                // Supported input list
                case { contains it, [devices:null] }:
                    Map activities = state.activities ?: [:]
                    payload.devices.each { activities[it.appId] = it.label }
                    state.activities = activities
                    runIn 5, 'updateActivities'
                    return

                // Shortcuts configured on the TV home page
                case { contains it, [launchPoints:null] }:
                    Map activities = state.activities ?: [:]
                    payload.launchPoints.each { activities[it.id] = it.title }
                    state.activities = activities
                    runIn 5, 'updateActivities'
                    return

                // System info
                case { contains it, [modelName:null] }:
                    utils_dataValue 'modelName', payload.modelName
                    utils_dataValue 'receiverType', payload.receiverType
                    return

                // Software info
                case { contains it, [sw_type:null] }:
                    utils_dataValue 'fwName', payload.model_name
                    utils_dataValue 'fwVersion', "${payload.product_name} / ${payload.major_ver}.${payload.minor_ver}"
                    return

                // Toast/alert displayed on TV
                case { contains it, [toastId:null] }:
                case { contains it, [alertId:null] }:
                    return

                // Screen status
                case { contains it, [state:'Screen Off'] }:
                    utils_sendEvent name:'screen', value:'off', descriptionText:'Screen is off', type:type
                    return

                case { contains it, [state:'Active'] }:
                    utils_sendEvent name:'screen', value:'on', descriptionText:'Screen is on', type:type
                    return

                case { contains it, [state:'Active Standby'] }:
                    utils_sendEvent name:'screen', value:'standby', descriptionText:'Screen is in active standby', type:type
                    return

                // Grab MAC addresses
                case { contains it, [wifiInfo:null] }:
                case { contains it, [wiredInfo:null] }:
                    String wifiMac = payload.wifiInfo?.macAddress
                    String wiredMac = payload.wiredInfo?.macAddress
                    if (wifiMac) utils_dataValue 'wifiMacAddress', wifiMac
                    if (wiredMac) utils_dataValue 'wiredMacAddress', wiredMac
                    return

                // Pairing prompt
                case { contains it, [pairingType:'PROMPT'] }:
                    log_warn '🙋‍♂️ Go and accept the pairing request on your TV screen. HAUL ASS!'
                    return

                // Sound output
                case { contains it, [soundOutput:null] }:
                    String soundOutput = payload.soundOutput
                    utils_sendEvent name:'soundOutput', value:soundOutput, descriptionText:"Sound output is ${soundOutput}", type:type
                    return

                // Other messages we don't care about
                case { contains it, [muteStatus:null] }:
                case { contains it, [sessionId:null] }:
                case { contains it, [volume:null] }:
                    return
            }
    }

    // Unexpected websocket message
    log_error "🚩 Received unexpected websocket message: description=${description}, msg=${msg}"
}

// ===================================================================================================================
// Logging helpers (something like this should be part of the SDK and not implemented by each driver)
// ===================================================================================================================

private void log_debug(String message) {
    if (logLevel == '1') log.debug "${device.displayName} ${message.uncapitalize()}"
}
private void log_info(String message) {
    if (logLevel <= '2') log.info "${device.displayName} ${message.uncapitalize()}"
}
private void log_warn(String message) {
    if (logLevel <= '3') log.warn "${device.displayName} ${message.uncapitalize()}"
}
private void log_error(String message) {
    log.error "${device.displayName} ${message.uncapitalize()}"
}

// ===================================================================================================================
// Helper methods (keep them simple, keep them dumb)
// ===================================================================================================================

private void utils_sendMessage(Map message, boolean checkState = true) {
    log_debug "◀ Sending websocket messages: ${message}"

    if (!ipAddr) {
        log_warn 'Device IP address not set in Preferences tab. Make sure you are using DHCP reservation for the TV\'s IP Address!'
        return
    }

    if (checkState && "${device.currentValue('switch', true)}" != 'on') {
        log_info 'Device is not switched on. Command not sent.'
        return
    }

    if (checkState && "${device.currentValue('sessionStatus', true)}" != 'online') {
        log_info 'Websocket is not connected. Connecting now ...'
        connect()
        return
    }

    // Send websocket message
    message.id = "hubitat_${now()}"
    message.sourceId = 'hubitat.hub'
    String payload = new JsonBuilder(message)
    interfaces.webSocket.sendMessage(payload)
}

private void utils_sendEvent(Map event) {
    if ("${device.currentValue(event.name, true)}" != "${event.value}") {
        log_info "${event.descriptionText} [${event.type}]"
    } else {
        log_debug "${event.descriptionText} [${event.type}]"
    }
    sendEvent event
}

private void utils_dataValue(String key, String value) {
    if (value == null || value == '') return
    log_debug "Update data value: ${key}=${value}"
    updateDataValue key, value
}

private String utils_parseStatus(String message) {
    switch (message) {
        case 'status: open': return 'online'
        case 'status: closing': return 'offline'
        case ~/^failure: connect timed out.*/: return 'offline'
        case ~/^failure: unexpected end of stream on http:\/\/.*/:
            log_warn 'Failed to establish a stable websocket connection to your TV. You should enable SSL in the Preferences tab. DO IT!'
            return 'offline'
        case ~/^failure: .*/:
            log_warn "Oh snap! ${message}"
            return 'offline'
        default: return 'unknown'
    }
}

private void util_wakeOnLan(String macAddr) {
    if (!macAddr) return
    String cmd = "wake on lan ${macAddr.replaceAll(':', '').toUpperCase()}"
    log_debug "◀ Sending LAN command: ${cmd}"
    sendHubCommand(new HubAction(cmd, Protocol.LAN))
}

void updateActivities() {
    List<String> activities = state.activities*.value.sort()
    utils_sendEvent name:'activities', value:new JsonBuilder(activities), descriptionText:"Supported activities is ${activities}", type:type
}

// switch/case syntactic sugar
@CompileStatic private boolean contains(Map msg, Map spec) {
    return msg.keySet().containsAll(spec.keySet()) && spec.every { it.value == null || it.value == msg[it.key] }
}
