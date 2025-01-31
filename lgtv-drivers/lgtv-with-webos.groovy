import groovy.json.JsonBuilder
import groovy.transform.CompileStatic
import groovy.transform.Field

import hubitat.device.HubAction
import hubitat.device.Protocol
import hubitat.helper.NetworkUtils

@Field static final String DRIVER_NAME = 'LGTV with webOS'
@Field static final String DRIVER_VERSION = '1.6.0'

@Field static final List<String> PICTURE_MODES = ['cinema', 'eco', 'expert1', 'expert2', 'game', 'normal', 'photo', 'sports', 'technicolor', 'vivid', 'hdrEffect', 'filmMaker', 'hdrCinema']
@Field static final List<String> SOUND_MODES = ['aiSoundPlus', 'aiSound', 'standard', 'news', 'music', 'movie', 'sports', 'game']
@Field static final List<String> SOUND_OUTPUT = ['tv_speaker', 'external_arc', 'external_optical', 'bt_soundbar', 'mobile_phone', 'lineout', 'headphone', 'tv_speaker_bluetooth']
@Field static final Integer MAX_FAST_PING = 20

metadata {
    definition(name:DRIVER_NAME, namespace:'dandanache', author:'Dan Danache', importUrl:'https://raw.githubusercontent.com/dan-danache/hubitat/main/lgtv-drivers/lgtv-with-webos.groovy') {
        capability 'Actuator'
        capability 'Initialize'
        capability 'Refresh'
        capability 'Switch'
        capability 'AudioVolume'
        capability 'TV'
        capability 'Notification'
        capability 'MediaController'
        capability 'MediaTransport'
        capability 'ImageCapture'
        capability 'SpeechSynthesis'

        attribute 'websocket', 'enum', ['connecting', 'online', 'offline']
        attribute 'channelName', 'string'
        attribute 'screen', 'enum', ['on', 'off', 'standby', 'screensaver']
        attribute 'pictureMode', 'enum', PICTURE_MODES
        attribute 'soundOutput', 'enum', SOUND_OUTPUT
        attribute 'soundMode', 'enum', SOUND_MODES
    }

    command 'deviceNotification', [
        [name:'Text*', type:'STRING', description:'Notification text*'],
        [name:'Type', type:'ENUM', description:'Notification type', constraints:['Toast - Goes away after few seconds', 'Alert - Stays on screen until dismissed']]
    ]
    command 'setChannel', [[name:'Channel number*', type:'STRING']]
    command 'screenOn'
    command 'screenOff'
    command 'setPictureMode', [[name:'Mode*', type:'ENUM', description:'Select picture mode', constraints:PICTURE_MODES.sort()]]
    command 'setSoundOutput', [[name:'Output*', type:'ENUM', description:'Select sound output', constraints:SOUND_OUTPUT.sort()]]
    command 'startVideo', [[name:'URL*', type:'STRING', description:'URL of video file to play']]
    command 'startWebPage', [[name:'URL*', type:'STRING', description:'URL to open in Web Browser']]
    command 'screenSaverOn'
    command 'screenSaverOff'

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
        input(
            name:'pingInterval', type:'enum', title:'Ping interval', required:true,
            description: 'Choose how often to check if the TV was turned on using the remote',
            options: [
                 '0' : 'Off',
                 '1' : '1 minute',
                 '2' : '2 minutes',
                 '3' : '3 minutes',
                 '5' : '5 minutes',
                '10' : '10 minutes',
                '20' : '20 minutes'
            ],
            defaultValue: '5'
        )
    }
}

// Called when the device is first added
void installed() {
    log_warn 'Installing device ...'

    // Init state
    state.activities = [:]
    utils_sendEvent name:'switch', value:'off', descriptionText:'Power initialized to off', type:'digital'
    utils_sendEvent name:'websocket', value:'offline', descriptionText:'Websocket initialized to offline', type:'digital'
}

// Called when the "Save Preferences" button is clicked
void updated(boolean auto = false) {
    log_info "Saving preferences${auto ? ' (auto)' : ''} ..."
    unschedule()

    if (logLevel == null) {
        logLevel = '1'
        device.updateSetting('logLevel', [value:logLevel, type:'enum'])
    }
    if (logLevel == '1') runIn 1800, 'logsOff'
    log_info "🛠️ logLevel = ${logLevel}"

    // Update device network Id
    if (ipAddr != null) {
        device.deviceNetworkId = ipAddr.tokenize('.').collect { String.format('%02X', it.toInteger()) }.join()
        connect()
    }

    if (useSSL == null) {
        useSSL = true
        device.updateSetting('useSSL', [value:useSSL, type:'enum'])
    }
    log_info "🛠️ useSSL = ${useSSL}"

    if (pingInterval == null) {
        pingInterval = '5'
        device.updateSetting('pingInterval', [value:pingInterval, type:'enum'])
    }
    log_info "🛠️ pingInterval = ${pingInterval} min"

    // Schedule priodic device ping
    if (pingInterval != '0') schedule "0 0/${pingInterval} * ? * * *", 'pingDevice'
}

void pingDevice() {
    if (!ipAddr || "${device.currentValue('websocket', true)}" == 'online') return
    log_debug "Pinging ${ipAddr} ..."
    if (NetworkUtils.ping(ipAddr, 1)?.packetsReceived > 0) connect()
}

void logsOff() {
    log_info '⏲️ Automatically reverting log level to "Info"'
    device.updateSetting 'logLevel', [value:'2', type:'enum']
}

// ===================================================================================================================
// Implement Capabilities
// ===================================================================================================================

// capability.Initialize
// This method will run when the hub starts
void initialize() {
    state.lastTx = 0
    state.lastRx = 0
    connect()
}

// capability.Refresh
void refresh() {
    log_debug '🎬 Refreshing device state ...'

    // We also have subscription for this values
    utils_sendMessage([type:'request', uri:'ssap://audio/getVolume'])
    utils_sendMessage([type:'request', uri:'ssap://tv/getCurrentChannel'])

    // These values rarely change, so we only request them on demand
    utils_sendMessage([type:'request', uri:'ssap://system/getSystemInfo'])
    utils_sendMessage([type:'request', uri:'ssap://com.webos.service.update/getCurrentSWInformation'])
    utils_sendMessage([type:'request', uri:'ssap://settings/getSystemSettings', payload:[category:'network', keys: ['deviceName']]])
    utils_sendMessage([type:'request', uri:'ssap://config/getConfigs', payload:[configNames:['tv.nyx.*']]])

    // https://github.com/JPersson77/LGTVCompanion/blob/master/Docs/Commandline.md
    // https://github.com/chros73/bscpylgtv/blob/master/docs/available_settings_CX.md
    //utils_sendMessage([type:'request', uri:'ssap://settings/getSystemSettings', payload:[category:'picture', keys: ['brightness', 'backlight', 'contrast', 'color', 'pictureMode']]])
    //utils_sendMessage([type:'request', uri:'ssap://settings/getSystemSettings', payload:[category:'sound', keys: ['soundMode']]])
    //utils_sendMessage([type:'request', uri:'ssap://config/getConfigs', payload:[configNames:['audio.*']]])
}

// capability.Switch
void on() {
    log_debug '🎬 Powering on ...'
    util_wakeOnLan(getDataValue('wifiMacAddress'))
    util_wakeOnLan(getDataValue('wiredMacAddress'))
    if (ipAddr) util_wakeOnLan(getMACFromIP(ipAddr))

    // Start fast pinging the IP for a maximum of MAX_FAST_PING times
    runIn 1, 'fastPing', [data:[currentRetry:0]]
}
private void fastPing(Map data) {
    data.currentRetry += 1
    if (!ipAddr || "${device.currentValue('switch', true)}" == 'on' || data.currentRetry > MAX_FAST_PING) {
        log_debug 'Fast ping terminated'
        return
    }

    log_debug "Fast pinging ${ipAddr}: ${data.currentRetry} / ${MAX_FAST_PING} ..."
    if (NetworkUtils.ping(ipAddr, 1)?.packetsReceived > 0) connect()

    // Keep fast-pinging until switch changes to 'on' or MAX_FAST_PING limit is reached
    runIn 1, 'fastPing', [data:data]
}
void off() {
    log_debug '🎬 Powering off ...'
    utils_sendMessage([type:'request', uri:'ssap://system/turnOff'])
    utils_sendEvent name:'switch', value:'off', descriptionText:'Power is off', type:'digital'
}

// capability.AudioVolume
void mute() {
    log_debug '🎬 Muting sound ...'
    utils_sendMessage([type:'request', uri:'ssap://audio/setMute', payload:[mute:true]])
}
void unmute() {
    log_debug '🎬 Unmuting sound ...'
    utils_sendMessage([type:'request', uri:'ssap://audio/setMute', payload:[mute:false]])
}
void volumeUp() {
    log_debug '🎬 Raising volume ...'
    utils_sendMessage([type:'request', uri:'ssap://audio/volumeUp'])
}
void volumeDown() {
    log_debug '🎬 Lowering volume ...'
    utils_sendMessage([type:'request', uri:'ssap://audio/volumeDown'])
}
void setVolume(String level) { setVolume Integer.parseInt(level) }
void setVolume(BigDecimal level) {
    log_debug "🎬 Setting volume to ${level}% ..."
    if (level < 0 || level > 100) return
    utils_sendMessage([type:'request', uri:'ssap://audio/setVolume', payload:[volume:level]])
}

// capability.TV
void channelUp() {
    log_debug '🎬 Changing channel up ...'
    utils_sendMessage([type:'request', uri:'ssap://tv/channelUp'])
}
void channelDown() {
    log_debug '🎬 Changing channel down ...'
    utils_sendMessage([type:'request', uri:'ssap://tv/channelDown'])
}

// capability.Notification
void deviceNotification(String text, String type = 'Toast') {
    log_debug "🎬 Sending ${type} notification ..."
    if (type.startsWith('Alert') || text.startsWith('!')) {
        utils_sendMessage([type:'request', uri:'"ssap://system.notifications/createAlert', payload:[message:text.replaceAll('^!', ''), buttons:[[label:'Dismiss']]]])
    } else {
        utils_sendMessage([type:'request', uri:'"ssap://system.notifications/createToast', payload:[message:text]])
    }
}

// capability.MediaController
void getAllActivities() {
    log_debug '🎬 Getting all activities ...'

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
    log_debug '🎬 Getting current activity ...'
    utils_sendMessage([type:'request', uri:'ssap://com.webos.applicationManager/getForegroundAppInfo'])
}
void startActivity(String activityname) {
    String appId = state.activities.find { it.value == activityname }?.key ?: activityname
    log_debug "🎬 Starting activity: [${activityname}] (${appId}) ..."
    utils_sendMessage([type:'request', uri:'ssap://system.launcher/launch', payload:[id:appId]])
}

// capability.MediaTransport
void play() {
    log_debug '🎬 Play ...'
    utils_sendMessage([type:'request', uri:'ssap://media.controls/play'])
}
void pause() {
    log_debug '🎬 Pause ...'
    utils_sendMessage([type:'request', uri:'ssap://media.controls/pause'])
}
void stop() {
    log_debug '🎬 Stop ...'
    utils_sendMessage([type:'request', uri:'ssap://media.controls/stop'])
}

// capability.ImageCapture
void take() {
    log_debug '🎬 Taking a screenshot ...'
    utils_sendMessage([type:'request', uri:'ssap://tv/executeOneShot', payload:[path:'/tmp/capture.jpg', method:'DISPLAY', format:'JPG']])
}

// capability.SpeechSynthesis
void speak(String text, BigDecimal volume = null, String voice = null) {
    log_debug "🎬 Speaking text [${text}] ..."
    Map result = textToSpeech(text, voice)
    log_debug "Sending TTS file ${result} ..."
    // utils_sendMessage([type:'request', uri:'ssap://com.webos.applicationManager/open', payload:[target:result.uri, mime:'audio/mp3']])
    utils_sendMessage([type:'request', uri:'ssap://com.webos.applicationManager/launch', payload:[
        id: state.activities.find { it.value == 'Media Player' || it.value == 'Music' }?.key ?: 'com.webos.app.mediadiscovery',
        params: [payload: [[
            fullPath:result.uri, mediaType:'MUSIC', deviceType:'DMR', lastPlayPosition:0,
            fileName: "Message from ${location.hub.name}",
            thumbnail: "http://${location.hub.localIP}/ui2/images/apple-touch-icon.png",
            dlnaInfo: [
                protocolInfo: 'http-get:*:audio/mpeg:DLNA.ORG_PN=MP3;DLNA.ORG_OP=01;DLNA.ORG_FLAGS=01500000000000000000000000000000',
                contentLength: '-1',
                duration: result.duration,
                opVal: 1,
                flagVal: 0,
                cleartextSize: '-1'
            ]
        ]]]
    ]])
}

// ===================================================================================================================
// Implement custom commands
// ===================================================================================================================
void setChannel(String channel) {
    log_debug "🎬 Changing channel to [${channel}] ..."
    utils_sendMessage([type:'request', uri:'ssap://tv/openChannel', payload:[channelNumber:channel]])
}
void screenOn() {
    log_debug '🎬 Turning screen on ...'
    utils_sendMessage([type:'request', uri:'ssap://com.webos.service.tvpower/power/turnOnScreen', payload:[standbyMode:'active']])
}
void screenOff() {
    log_debug '🎬 Turning screen off ...'
    utils_sendMessage([type:'request', uri:'ssap://com.webos.service.tvpower/power/turnOffScreen', payload:[standbyMode:'active']])
}
void setPictureMode(String mode) {
    log_debug "🎬 Setting picture mode to: [${mode}] ..."
    utils_sendMessage([type:'request', uri:'ssap://settings/setSystemSettings', payload:[category:'picture', settings: [pictureMode:mode]]])
}
void setSoundOutput(String output) {
    log_debug "🎬 Setting sound output to: [${output}] ..."
    utils_sendMessage([type:'request', uri:'ssap://settings/setSystemSettings', payload:[category:'sound', settings: [soundOutput:output]]])
    //utils_sendMessage([type:'request', uri:'com.webos.service.apiadapter/audio/changeSoundOutput', payload:[output:output]]])
}
void startVideo(String url) {
    log_debug "🎬 Playing video file: [${url}]..."

    // https://gist.github.com/aabytt/bddbb1bcf031a050d89a89aeee3a6737#playling-a-link-with-standard-lg-webos-player
    utils_sendMessage([type:'request', uri:'ssap://com.webos.applicationManager/launch', payload:[
        id: state.activities.find { it.value == 'Media Player' || it.value == 'Photo & Video' }?.key ?: 'com.webos.app.mediadiscovery',
        params: [payload: [[fullPath:url, mediaType:'VIDEO', deviceType:'DMR', lastPlayPosition:0]]]
    ]])
}
void startWebPage(String url) {
    log_debug "🎬 Opening web page: [${url}]..."
    utils_sendMessage([type:'request', uri:'ssap://com.webos.applicationManager/launch', payload:[
        id: 'com.webos.app.browser',
        params: [target:url]
    ]])
}
void screenSaverOn() {
    log_debug '🎬 Starting screen saver ...'
    utils_sendMessage([type:'request', uri:'ssap://system.launcher/launch', payload:[id:'com.webos.app.screensaver']])
}
void screenSaverOff() {
    log_debug '🎬 Stopping screen saver ...'
    utils_sendMessage([type:'request', uri:'ssap://system.launcher/close', payload:[id:'com.webos.app.screensaver']])
}

// ===================================================================================================================
// Websocket helpers
// ===================================================================================================================

void connect() {
    if (!ipAddr) {
        log_warn 'Device IP address not set in Preferences tab. Make sure you are using DHCP reservation for the TV\'s IP address!'
        return
    }

    // A connection is already in progress
    if ("${device.currentValue('websocket')}" == 'connecting') {
        log_debug 'Connection already in progress'
        return
    }

    log_debug "Connecting to ${ipAddr} ..."
    disconnect()
    interfaces.webSocket.connect(useSSL ? "wss://${ipAddr}:3001/" : "ws://${ipAddr}:3000/", headers: ['Content-Type': 'application/json'], ignoreSSLIssues: true)

    // Update "websocket" attribute
    utils_sendEvent name:'websocket', value:'connecting', descriptionText:'Websocket is connecting', type:'digital'
}

void disconnect() {
    try { interfaces.webSocket.close() } catch (e) { }
}

void register() {
    if (state.pk) {
        utils_sendMessage([type:'register', payload:['client-key':state.pk]], false)
        return
    }

    utils_sendMessage([
        type: 'register',
        payload: [
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
                    localizedAppNames: ['':'LG Remote App', 'ko-KR':'리모컨 앱', 'zxx-XX':'ЛГ Rэмotэ AПП'],
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
    log_debug "▶ webSocketStatus(${message})"

    String websocket = utils_parseStatus message

    // If websocket just opened, say hello (skip state checks)
    if (websocket == 'online') {
        utils_sendMessage([type:'hello', id:'hello'], false)
        return
    }

    // Update "websocket" attribute
    utils_sendEvent name:'websocket', value:websocket, descriptionText:"Websocket is ${websocket}", type:'physical'
}

void parse(String description) {
    log_debug "▶ Received message: ${description}"
    state.lastRx = now()

    Map msg = parseJson(description)
    String type = msg.payload?.callerId == 'secondscreen.client' || msg.payload?.callerId == 'com.webos.service.apiadapter' || now() - (state.lastTx ?: 0) < 2000 ? 'digital' : 'physical'

    // Empty reponse. Thanks for nothing!
    if (msg.payload.keySet().size() == 1 && (msg.payload.returnValue == true || msg.payload.subscription == true)) return

    switch (msg.type) {

        // TV sent an error message. Oh noes!
        case 'error':

            // Ignore unsupported config keys and TV bind fails
            if (msg.payload?.errorText?.startsWith('Some keys are not allowed') || msg.payload?.errorText?.startsWith('com.webos.service.utp/bind returns invalid result')) return

            log_error "▶ Received error message: ${msg.payload?.errorText ? msg.payload?.errorText : msg.error}"
            return

        // Hello (request)
        case 'hello':
            log_debug '▶ Proper protocol has been observed. Starting authentication ...'

            // Update "websocket" and "switch" attributes
            utils_sendEvent name:'switch', value:'on', descriptionText:'Power is on', type:'physical'
            utils_sendEvent name:'websocket', value:'online', descriptionText:'Websocket is online', type:'physical'

            // Start registration
            runIn 1, 'register'
            return

        // Register (request)
        case 'registered':
            log_debug '▶ Authentication complete. Oh yeah!'
            state.pk = msg.payload['client-key']

            // Setup subscriptions
            utils_sendMessage([type:'subscribe', uri:'ssap://audio/getStatus'])
            utils_sendMessage([type:'subscribe', uri:'ssap://tv/getCurrentChannel'])
            utils_sendMessage([type:'subscribe', uri:'ssap://com.webos.applicationManager/getForegroundAppInfo'])
            utils_sendMessage([type:'subscribe', uri:'ssap://com.webos.media/getForegroundAppInfo'])
            utils_sendMessage([type:'subscribe', uri:'ssap://com.webos.service.tvpower/power/getPowerState'])
            utils_sendMessage([type:'subscribe', uri:'ssap://settings/getSystemSettings', payload:[category:'picture', keys:['pictureMode']]])
            utils_sendMessage([type:'subscribe', uri:'ssap://settings/getSystemSettings', payload:[category:'sound', keys:['soundMode', 'soundOutput']]])

            // First time with talk with this device
            if (!getDataValue('modelName')) {

                // Enable Wake on LAN
                log_debug 'Enabling Wake On LAN (WOL) ...'
                utils_sendMessage([type:'request', uri:'ssap://settings/setSystemSettings', payload:[category:'network', settings: [wolwowlOnOff:'true']]])

                // Get some device information that never changes
                utils_sendMessage([type:'request', uri:'ssap://system/getSystemInfo'])
                utils_sendMessage([type:'request', uri:'ssap://com.webos.service.connectionmanager/getinfo'])

                // Send a notification on TV
                deviceNotification "<b>Mesage from ${location.hub.name}</b><br>Well done! Configuration is now complete 👍"
            }

            // Auto-sync device state with Hubitat
            refresh()
            return

        // TV sent a response to a command sent by us
        case 'response':
            Map payload = msg.payload
            switch (payload) {

                // ssap://audio/getStatus (subscription)
                case { contains it, [volumeStatus:null] }:
                    int volume = payload.volumeStatus.volume
                    String mute = payload.volumeStatus.muteStatus ? 'muted' : 'unmuted'
                    String soundOutput = payload.volumeStatus.soundOutput
                    utils_sendEvent name:'volume', value:volume, unit:'%', descriptionText:"Sound volume is ${volume}%", type:type
                    utils_sendEvent name:'mute', value:mute, descriptionText:"Sound is ${mute}", type:type
                    if (soundOutput) utils_sendEvent name:'soundOutput', value:soundOutput, descriptionText:"Sound output is ${soundOutput}", type:type
                    return

                // ssap://tv/getCurrentChannel (subscription)
                case { contains it, [channelNumber:null] }:
                    String channel = payload.channelNumber
                    String channelName = payload.channelName ?: 'unknown'
                    utils_sendEvent name:'channel', value:channel, descriptionText:"Channel is ${channel}", type:type
                    utils_sendEvent name:'channelName', value:channelName, descriptionText:"Channel name is ${channelName}", type:type
                    return

                // ssap://com.webos.applicationManager/getForegroundAppInfo (subscription)
                case { contains it, [appId:'', processId:'', windowId:''] }:
                    utils_sendEvent name:'switch', value:'off', descriptionText:'Power is off', type:type
                    disconnect()
                    return

                case { contains it, [appId:null] }:
                    utils_sendEvent name:'switch', value:'on', descriptionText:'Power is on', type:type
                    String currentActivity = state.activities?.find { it.key == payload.appId }?.value ?: 'unknown'
                    utils_sendEvent name:'currentActivity', value:currentActivity, descriptionText:"Current activity is ${currentActivity}", type:type
                    return

                // ssap://settings/getSystemSettings (subscription)
                case { contains it, [category:'picture'] }:
                    String pictureMode = payload.settings?.pictureMode
                    utils_sendEvent name:'pictureMode', value:pictureMode, descriptionText:"Picture mode is ${pictureMode}", type:type
                    return

                case { contains it, [category:'sound'] }:
                    String soundOutput = payload.settings?.soundOutput
                    utils_sendEvent name:'soundOutput', value:soundOutput, descriptionText:"Sound output is ${soundOutput}", type:type

                    String soundMode = payload.settings?.soundMode
                    utils_sendEvent name:'soundMode', value:soundMode, descriptionText:"Sound mode is ${soundMode}", type:type
                    return

                // ssap://com.webos.media/getForegroundAppInfo (subscription)
                case { contains it, [foregroundAppInfo:null] }:
                    String transportStatus = utils_parseForegroundAppInfo(payload.foregroundAppInfo)
                    utils_sendEvent name:'transportStatus', value:transportStatus, descriptionText:"Media transport status is ${transportStatus}", type:type
                    return

                // ssap://tv/getExternalInputList (request)
                case { contains it, [devices:null] }:
                    Map activities = state.activities ?: [:]
                    payload.devices.each { activities[it.appId] = it.label }
                    state.activities = activities
                    runIn 5, 'updateActivities'
                    return

                // ssap://com.webos.applicationManager/listLaunchPoints (request)
                case { contains it, [launchPoints:null] }:
                    Map activities = state.activities ?: [:]
                    payload.launchPoints.each { activities[it.id] = it.title }
                    state.activities = activities
                    runIn 5, 'updateActivities'
                    return

                // ssap://settings/getSystemSettings (request)
                case { contains it, [category:'network'] }:
                    utils_dataValue 'networkName', payload.settings?.deviceName
                    return

                // ssap://com.webos.service.update/getCurrentSWInformation (request)
                case { contains it, [sw_type:null] }:
                    utils_dataValue 'fwVersion', "${payload.product_name} / ${payload.major_ver}.${payload.minor_ver}"
                    return

                // ssap://com.webos.service.tvpower/power/turnOnScreen (request)
                case { contains it, [state:'Screen Off'] }:
                    utils_sendEvent name:'screen', value:'off', descriptionText:'Screen is off', type:type
                    return

                case { contains it, [state:'Active'] }:
                    utils_sendEvent name:'screen', value:'on', descriptionText:'Screen is on', type:type
                    return

                case { contains it, [state:'Screen Saver'] }:
                    utils_sendEvent name:'screen', value:'screensaver', descriptionText:'Screen is in screensaver mode', type:type
                    return

                case { contains it, [state:'Active Standby'] }:
                    utils_sendEvent name:'screen', value:'standby', descriptionText:'Screen is in active standby', type:type
                    return

                // ssap://system/getSystemInfo (request)
                case { contains it, [modelName:null] }:
                    utils_dataValue 'modelName', payload.modelName
                    utils_dataValue 'receiverType', payload.receiverType
                    return

                // ssap://com.webos.service.connectionmanager/getinfo (request)
                case { contains it, [wifiInfo:null] }:
                case { contains it, [wiredInfo:null] }:
                    utils_dataValue 'wifiMacAddress', payload.wifiInfo?.macAddress
                    utils_dataValue 'wiredMacAddress', payload.wiredInfo?.macAddress
                    return

                // ssap://config/getConfigs (request)
                case { contains it, [configs:null] }:
                    utils_dataValue 'platformVersion', payload.configs['tv.nyx.platformVersion']
                    return
                
                // ssap://tv/executeOneShot
                case { contains it, [imageUri:null] }:
                    String image = payload.imageUri
                    utils_sendEvent name:'image', value:image, descriptionText:"Capture URL is ${image}", type:type
                    return

                // Pairing prompt (request)
                case { contains it, [pairingType:'PROMPT'] }:
                    log_warn '🙋‍♂️ Go and accept the pairing request on your TV screen. HAUL ASS!'
                    return

                // ssap://audio/getVolume (request)
                case { contains it, [soundOutput:null] }:
                    String soundOutput = payload.soundOutput
                    utils_sendEvent name:'soundOutput', value:soundOutput, descriptionText:"Sound output is ${soundOutput}", type:type
                    return

                // Other messages we don't care about
                case { contains it, [muteStatus:null] }:
                case { contains it, [sessionId:null] }:
                case { contains it, [volume:null] }:
                case { contains it, [toastId:null] }:
                case { contains it, [alertId:null] }:
                case { contains it, [missingConfigs:null] }:
                case { contains it, [returnValue:true, method:'setSystemSettings'] }:
                    return
            }
    }

    // Unexpected websocket message
    log_error "🚩 Received unexpected websocket message: description=${description}"
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

    if (checkState && "${device.currentValue('switch')}" != 'on') {
        log_info 'Device is not switched on. Command not sent.'
        return
    }

    if (checkState && "${device.currentValue('websocket')}" != 'online') {
        log_info 'Websocket is not connected anymore. Connecting now ...'
        runIn 1, 'connect'
        return
    }

    // Send websocket message
    state.lastTx = now()
    message.id = "hubitat_${now()}"
    message.sourceId = 'hubitat.hub'
    String payload = new JsonBuilder(message)
    interfaces.webSocket.sendMessage(payload)
}

private void utils_sendEvent(Map event) {
    if (event.value == null) return
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
            log_info "Oh snap! ${message}"
            return 'offline'
        default: return 'offline'
    }
}

private String utils_parseForegroundAppInfo(List<Map> foregroundAppInfo) {
    String playState = foregroundAppInfo.find { it.windowId != '' }?.playState
    if (foregroundAppInfo.size() > 1 && (playState == 'loaded' || playState == 'playing')) return 'playing'
    return playState == 'paused' ? 'paused' : 'stopped'
}

private void util_wakeOnLan(String macAddr) {
    if (macAddr == null || macAddr == '') return
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
