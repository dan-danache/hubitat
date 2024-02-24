/*
 * Zigbee Map app for Hubitat
 *
 * Eye-candy visual render for Zigbee mesh topology.
 *
 * @see https://github.com/dan-danache/hubitat
 */
import groovy.transform.Field
import com.hubitat.app.ChildDeviceWrapper

@Field static final String APP_NAME = "Zigbee Map"
@Field static final String APP_VERSION = "1.5.0"
@Field static final String MAP_FILE_NAME = "zigbee-map.html"
@Field static final String MEMCPU_FILE_NAME = "mem-cpu-history.html"
@Field static final def HEXADECIMAL_PATTERN = ~/\p{XDigit}{4}/
@Field static final def URL_PATTERN = ~/^https?:\/\/[^\/]+(.+)/

definition(
    name: APP_NAME,
    namespace: "dandanache",
    author: "Dan Danache",
    description: "Visualize the topology and connectivity of your Zigbee network.",
    documentationLink: "https://community.hubitat.com/t/release-zigbee-map-app/133888",
    importUrl: "https://raw.githubusercontent.com/dan-danache/hubitat/zigbee-map_${APP_VERSION}/zigbee-map-app/zigbee-map.groovy",
    category: "Utility",
    singleInstance: true,
    installOnOpen: true,
    iconUrl: "",
    iconX2Url: "",
    oauth: true,
)

// ===================================================================================================================
// Standard app methods
// ===================================================================================================================

def installed() {
    fetchHelper()
    log.info "${app?.getLabel()} has been installed"
}

def updated() {
    log.info "${app?.getLabel()} has been updated"
}

def refresh() {
    log.info "${app?.getLabel()} has been refreshed"
}

private ChildDeviceWrapper fetchHelper() {
    String deviceId = "ZMH-${app.id}"
    def helper = getChildDevice(deviceId)
    if (helper) return helper

    // Create helper device
    log.info "Creating helper device with Device Network Id: ${deviceId}"
    helper = addChildDevice("dandanache", "Zigbee Map Helper", deviceId, [
        name: "Zigbee Map Helper",
        label: "Zigbee Map Helper",
        isComponent: true
    ])
    return helper
}

private void debug(message) {
    if (logEnable) log.debug message
}

// ===================================================================================================================
// Implement Pages
// ===================================================================================================================

preferences {
    page name: "zigbeemap"
    page name: "changelog"
}

Map zigbeemap() {
    def showInstall = app.getInstallationState() == "INCOMPLETE"
    dynamicPage (
        name: "zigbeemap",
        title: "<b>${APP_NAME} - v${APP_VERSION}</b>",
        install: true,
        uninstall: !showInstall
    ) {
        if (app.getInstallationState() == "COMPLETE") {
            if (!state.accessToken) createAccessToken()

            section {
                href (
                    name: "localLink",
                    title: "View Zigbee map",
                    description: "Start building the Zigbee map",
                    url: "${getLocalURL(MAP_FILE_NAME)}",
                    style: "embedded",
                    state: "complete",
                    required: false,
                )

                href (
                    name: "memCpuHistoryLink",
                    title: "View MEM & CPU history",
                    description: "Graph hub memory and processor usage (since last reboot)",
                    url: "${getLocalURL(MEMCPU_FILE_NAME)}",
                    style: "embedded",
                    state: "complete",
                    required: false,
                )
                // href (
                //     name: "cloudLink",
                //     title: "View Zigbee map - over the Internet",
                //     description: "Tap here to load the Zigbee map when you are on the go. This option uses the Hubitat Cloud.",
                //     url: "${getCloudURL()}",
                //     style: "embedded",
                //     state: "complete",
                //     required: false,
                // )
                href (
                    name: "changelogLink",
                    title: "View change log",
                    description: "See latest application changes",
                    page: "changelog",
                    required: false,
                )
                // href (
                //     name: "donateLink",
                //     title: "<img src=\"data:image/png;base64,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\" alt=\"Buy me a Coffee\">",
                //     description: "",
                //     url: "https://www.buymeacoffee.com/dandanache",
                //     style: "embedded",
                //     required: false,
                // )

                input(
                    name: "logEnable",
                    type: "bool",
                    title: "Enable debug logging",
                    defaultValue: false,
                    submitOnChange: true
                )
            }
        } else {
            section {
                paragraph "Tap the [Done] button to create the application instance."
            }
        }
    }
}

Map changelog() {
    dynamicPage (
        name: "changelog",
        title: "<b>${APP_NAME} - Change Log</b>",
        install: false,
        uninstall: false
    ) {

        section ("v1.5.0 - 2024-02-23", hideable: true, hidden: false) {
            paragraph "<li>Add config option to show/hide link colors" +
            "<li>Make node hover effect (see neighbors) more visible - @WarlockWeary</li>"
        }

        section ("v1.4.0 - 2024-02-23", hideable: true, hidden: true) {
            paragraph "<li>Color links based on LQI/LQA value - @Horseflesh</li>" +
            "<li>Hide back <b>duplex</b> links by default to better see the link colors</li>" +
            "<li>Use <b>Esc</b> keyboard key to toggle the controls - @jshimota</li>"
        }

        section ("v1.3.0 - 2024-02-21", hideable: true, hidden: true) {
            paragraph "<li>Add option to show/hide <b>Unknown</b> devices - @Tony</li>" +
            "<li>Remove the Hub device from the <b>Devices</b> tab - @jimhim</li>" +
            "<li>Add PWA manifest</li>"
        }

        section ("v1.2.0 - 2024-02-20", hideable: true, hidden: true) {
            paragraph "<li>Add option to use an image as map background (e.g.: home layout)</li>"
        }

        section ("v1.1.0 - 2024-02-19", hideable: true, hidden: true) {
            paragraph "<li>Add <b>Done</b> button in the Hubitat app - @dnickel</li>" +
            "<li>Click the address of any device in the <b>Devices</b> tab to add it to the Interview Queue - @hubitrep</li>" +
            "<li>Use relative URL when opening the HTML app - @jlv</li>" +
            "<li>Mark devices that failed the Interview  - @kahn-hubitat</li>" +
            "<li>Show Interview Queue size</li>" +
            "<li>Show <b>duplex</b> links by default</li>"
        }

        section ("v1.0.0 - 2024-02-16", hideable: true, hidden: true) {
            paragraph "<li>Initial release</li>"
        }
    }
}

def getLocalURL(String fileName) {
    String fullURL = "${getFullLocalApiServerUrl()}/${fileName}?access_token=${state.accessToken}";
    return (fullURL =~ URL_PATTERN).findAll()[0][1]
}

def getCloudURL(String fileName) {
    return "${getApiServerUrl()}/${hubUID}/apps/${app.id}/${fileName}?access_token=${state.accessToken}"
}

// ===================================================================================================================
// Implement Mappings
// ===================================================================================================================

mappings {
    path("/${MAP_FILE_NAME}") {action: [GET: "loadZigbeeMapMapping"]}
    path("/${MEMCPU_FILE_NAME}") {action: [GET: "loadMemCpuHistoryMapping"]}
    path("/zigbee-map.webmanifest") {action: [GET: "loadManifestMapping"]}
    path("/poke/:addr/:startIndex") {action: [GET: "pokeMapping"]}
}

def loadZigbeeMapMapping() {
    debug "Proxying ${MAP_FILE_NAME} to ${request.HOST} (${request.requestSource})"
    return render(
        status: 200,
        contentType: "text/html",
        data: new String(downloadHubFile(MAP_FILE_NAME), "UTF-8").replaceAll('\\$\\{access_token\\}', "${state.accessToken}")
    )
}

def loadMemCpuHistoryMapping() {
    debug "Proxying ${MEMCPU_FILE_NAME} to ${request.HOST} (${request.requestSource})"
    return render(
        status: 200,
        contentType: "text/html",
        data: new String(downloadHubFile(MEMCPU_FILE_NAME), "UTF-8").replaceAll('\\$\\{access_token\\}', "${state.accessToken}")
    )
}

def loadManifestMapping() {
    debug "Loading PWA manifest"
    return render(
        status: 200,
        contentType: "application/manifest+json",
        data: """\
        {
            "id": "11ba8718-86f0-4461-ae21-8627001d3e8e",
            "name": "Hubitat Zigbee Map",
            "short_name": "Zigbee Map",
            "description": "Visualize the topology and connectivity of your Zigbee network.",
            "start_url": "${getLocalURL(MAP_FILE_NAME)}",
            "icons": [
                {
                    "src": "/ui2/images/android-chrome-512x512.png",
                    "sizes": "512x512",
                    "type": "image/png"
                },
                {
                    "src": "/ui2/images/android-chrome-512x512.png",
                    "sizes": "512x512",
                    "type": "image/png",
                    "purpose": "maskable"
                }
            ],
            "categories": ["utilities"],
            "display": "standalone",
            "orientation": "portrait",
            "theme_color": "#002b36",
            "background_color": "#002b36"
        }
        """
    )
}

def pokeMapping() {
    String addr = "${params.addr}"
    Integer startIndex = Integer.parseInt(params.startIndex)
    debug "Poking ${addr}:${startIndex}"

    // Do some checks to make sure we poke the right thing
    if (!addr || !HEXADECIMAL_PATTERN.matcher(addr).matches()) return render(
        status: 400,
        contentType: "application/json",
        data: "{\"addr\": \"${addr}\", \"status\": false}"
    )

    // Use the helper device to do the actual poking
    fetchHelper().poke(addr, startIndex)
    return render(
        status: 200,
        contentType: "application/json",
        data: "{\"addr\": \"${addr}\", \"startIndex\": ${startIndex}, \"status\": true}"
    )
}
