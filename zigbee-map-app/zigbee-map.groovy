/*
 * Zigbee Map app for Hubitat
 *
 * Eye-candy visual render for Zigbee mesh topology.
 *
 * @see https://github.com/dan-danache/hubitat
 */
import groovy.transform.Field
import com.hubitat.app.ChildDeviceWrapper

@Field static final String APP_NAME = 'Zigbee Map'
@Field static final String APP_VERSION = '2.2.0'
@Field static final String NEIGHBORS_FILE_NAME = 'zigbee-neighbors.html'
@Field static final String ROUTES_FILE_NAME = 'zigbee-routes.html'
@Field static final String MEMCPU_FILE_NAME = 'mem-cpu-history.html'
@Field static final def HEXADECIMAL_PATTERN = ~/\p{XDigit}{4}/
@Field static final def URL_PATTERN = ~/^https?:\/\/[^\/]+(.+)/

definition(
    name: APP_NAME,
    namespace: 'dandanache',
    author: 'Dan Danache',
    description: 'Visualize the topology and connectivity of your Zigbee network.',
    documentationLink: 'https://community.hubitat.com/t/release-zigbee-map-app/133888',
    importUrl: 'https://raw.githubusercontent.com/dan-danache/hubitat/zigbee-map_2.1.0/zigbee-map-app/zigbee-map.groovy',
    category: 'Utility',
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
    helper = addChildDevice('dandanache', 'Zigbee Map Helper', deviceId, [
        name: 'Zigbee Map Helper',
        label: 'Zigbee Map Helper',
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
    page name: 'main'
    page name: 'changelog'
    page name: 'pairing'
    page name: 'selectRepeater'
    page name: 'instructions'
    page name: 'start'
}

Map main() {
    def showInstall = app.getInstallationState() == 'INCOMPLETE'
    dynamicPage (
        name: 'main',
        title: "<b>${APP_NAME} v${APP_VERSION}</b>",
        install: true,
        uninstall: !showInstall
    ) {
        if (app.getInstallationState() == 'COMPLETE') {
            if (!state.accessToken) createAccessToken()

            section {
                href (
                    name: 'neighborsLink',
                    title: 'Zigbee Neighbors Map',
                    description: 'Start building the Zigbee neighbors map',
                    url: "${getLocalURL(NEIGHBORS_FILE_NAME)}",
                    style: 'embedded',
                    state: 'complete',
                    required: false,
                )
                href (
                    name: 'routesLink',
                    title: 'Zigbee Routing Map',
                    description: 'Start building the Zigbee routes map',
                    url: "${getLocalURL(ROUTES_FILE_NAME)}",
                    style: 'embedded',
                    state: 'complete',
                    required: false,
                )
                href (
                    name: 'memCpuHistoryLink',
                    title: 'View Hub MEM & CPU history',
                    description: 'Graph hub memory and processor usage',
                    url: "${getLocalURL(MEMCPU_FILE_NAME)}",
                    style: 'embedded',
                    state: 'complete',
                    required: false,
                )
                href (
                    name: 'pairingLink',
                    title: 'Advanced Zigbee pairing',
                    description: 'Start Zigbee pairing only on a specific device',
                    page: 'pairing',
                    required: false,
                )
                href (
                    name: 'changelogLink',
                    title: 'View change log',
                    description: 'See latest application changes',
                    page: 'changelog',
                    required: false,
                )

                // Preferences
                input(
                    name: 'useDarkTheme',
                    type: 'bool',
                    title: 'Use dark theme',
                    defaultValue: false,
                    submitOnChange: true
                )
                input(
                    name: 'logEnable',
                    type: 'bool',
                    title: 'Enable debug logging',
                    defaultValue: false,
                    submitOnChange: true
                )
            }
        } else {
            section {
                paragraph 'Tap the "Done" button to create the application instance.'
            }
        }
    }
}

Map pairing() {
    app.removeSetting 'repeater'
    dynamicPage (title:'<b>Advanced Zigbee Pairing</b>', name:'pairing', install:false, uninstall:false) {
        section {
            paragraph '''\
                <details style="white-space:normal">
                    <summary style="cursor:pointer">Click here to learn about Zigbee pairing</summary>
                        <div style="margin-top:0.7em">
                            When you click the "Start Zigbee pairing" button on the "Add device" page, all Zigbee repeater devices (including the Hubitat hub)
                            enter the Zigbee pairing state. This is beneficial because the new device you want to add can join the Zigbee network using any repeater
                            device already part of the network.
                        </div>
                        <div style="margin-top:0.7em">
                            However, there are situations where you specifically want to join a new device to the Zigbee network using only a particular repeater
                            device or only the Hubitat hub itself. For example:
                        </div>
                        <ul style="margin-top:0.5em; padding-left:1.5em">
                            <li><b>IKEA battery-powered devices</b>: These must join the Zigbee network exclusively through the hub. Otherwise, they may quickly fall off the network.</li>
                            <li><b>Aqara devices</b>: Some Aqara devices require joining the Zigbee network through a Zigbee 3.0 repeater device.</li>
                        </ul>
                    <hr>
                </details>
            '''.trim();

            paragraph '<b>On what device do you want to start Zigbee Pairing?</b>'
            href (
                name: 'hubLink',
                title: 'Hub only',
                description: 'Start Zigbee pairing only on the Hubitat hub (useful for IKEA battery-powered devices)',
                page: 'instructions',
                required: false,
            )
            href (
                name: 'selectRepeaterLink',
                title: 'Repeater device only',
                description: 'Start Zigbee pairing only on a specific repeater device (useful for some Aqara devices)',
                page: 'selectRepeater',
                required: false,
            )
        }
    }
}

Map selectRepeater() {
    dynamicPage (title:'<b>Advanced Zigbee Pairing</b> - Select repeater device', name:'select-repeater', install:false, uninstall:false, nextPage:'instructions') {
        section {
            paragraph '<b>Important</b>: Make sure that the device you select below is a Zigbee repeater. Usually all mains-powered devices act as Zigbee repeaters.'
            input 'repeater', 'capability.switch', title:'Select repeater device:', multiple:false, required:true, showFilter:true
        }
    }
}

Map instructions() {
    dynamicPage (title:'<b>Advanced Zigbee Pairing</b> - Instructions', name:'instructions', install:false, uninstall:false, nextPage:'start') {
        section {
            paragraph """\
                <div style="white-space:normal">
                    <div id="pairing-instructions" style="padding:.8em 1em; color:#0c5460; background-color:#d1ecf1; border:1px #bee5eb solid; border-radius:.3em">
                        You selected to start Zigbee pairing only on ${repeater == null ? 'the Hubitat hub' : repeater }.
                    </div>
                    <div style="margin-top:1em">
                        Review the following instructions before advancing to the next page:
                        <ol style="font-size:1em; margin-top:.5em">
                            <li>Learn the procedure to put the device want to join in pairing more, <b>but wait</b> until you receive further instructions before activating pairing mode.</li>
                            <li>Ensure that the device you wish to join is within the radio range of ${repeater == null ? 'your Hubitat hub' : repeater}, as this is the only device that will accept network join requests.</li>
                        </ol>
                    </div>
                    <script type="text/javascript">
                        const instructionsElm = document.getElementById('pairing-instructions')
                        instructionsElm.focus()

                        document.getElementById('fieldsetAppButtons').insertAdjacentHTML('afterbegin',
                            '<button type="button" onclick="gotoMain()" value="Cancel" class="mdl-button mdl-js-button mdl-button--raised delete btn mdl-button--accent">Cancel</button>'
                        )
                        function gotoMain() {
                            location.href=\'../../main\'
                        }
                    </script>
                </div>
            """.trim();
        }
    }
}

Map start() {
    dynamicPage (title:"<b>Advanced Zigbee Pairing</b> - Pair using ${repeater == null ? 'the Hub' : repeater}", name:'start', install:false, uninstall:false, nextPage:'main') {
        String deviceNetworkId = repeater == null ? '0000' : repeater.deviceNetworkId;
        section {
            paragraph """\
                <div id="pairing-instructions" class="instructions-wait" tabindex="0">
                    Step 1 of 4 - Loading the Zigbee pairing page. Please wait...
                </div>
            """.trim();
        }
        section {
            paragraph """\
            <div style="white-space:normal">
                <iframe id="addDeviceIframe" src="/device/addDevice" style="width:100%; height:400px; border:1px #ccc solid" onload="addDeviceIframeLoaded()"></iframe>
                <script type="text/javascript">

                    function addDeviceIframeLoaded() {
                        const instructionsElm = document.getElementById('pairing-instructions')
                        instructionsElm.focus()

                        const iDoc = document.querySelector('iframe').contentDocument
                        const elementsToRemove = '#divMainUIHeader, #divMainUIMenu, #divMainUIFooter, #divAddDeviceContents > .pb-3 > button, hr, .grid > div:not(:first-child)';
                        iDoc.querySelectorAll(elementsToRemove).forEach(el => el.remove())

                        instructionsElm.innerHTML = '</span>Step 2 of 4 - Clicking UI buttons. Please wait ...'
                        iDoc.querySelectorAll('button[aria-label="Zigbee"]').forEach(el => el.click())
                        window.setTimeout(function() {
                            iDoc.querySelectorAll('.add-device-header, .add-device-text').forEach(el => el.remove())
                            iDoc.querySelectorAll('button[aria-label="Start Zigbee pairing"]').forEach(el => el.click())

                            instructionsElm.innerHTML = 'Step 3 of 4 - Configuring Zigbee repeaters. Please wait ...'
                            window.setTimeout(() => configurePairing(), 3000)
                        }, 2000);
                    }
                    function configurePairing() {
                        console.info('Configuring Zigbee repeaters ...')
                        return fetch(new Request('${getLocalURL("pairing/${deviceNetworkId}")}'), {cache: 'no-store'})
                            .then((response) => {
                                if (!response.ok) {
                                    console.error('fetch() - Bad response (not 200 OK)', response)
                                    throw new Error(`fetch() - HTTP error, status = \${response.status}`)
                                }
                                return response.text()
                            })
                            .then(text => JSON.parse(text))
                            .then(json => {
                                if (json.status !== true) {
                                    console.error('fetch() - Bad status attribute (not true)', json)
                                    throw new Error(`fetch() - JSON error, status = \${json.status}`)
                                }
                                window.setTimeout(function() {
                                    const instructionsElm = document.getElementById('pairing-instructions')
                                    instructionsElm.innerHTML = 'Step 4 of 4 - Set the joining device in pairing mode <b>now</b>, then follow the progress below.'
                                    instructionsElm.className = 'instructions-act'
                                }, 10000)
                            })
                            .catch((ex) => {
                                console.error('fetch() - Failed to fetch data', ex)
                                alert(`fetch() - Failed to fetch data: \${ex.message}`)
                            })
                    }
                </script>
                <style>
                    .instructions-wait {
                        white-space: normal;
                        padding: .8em 1em;
                        color: #856404;
                        background-color: #fff3cd;
                        border: 1px #ffeeba solid;
                        border-radius: .3em;
                    }
                    .instructions-act {
                        white-space: normal;
                        padding: .8em 1em;
                        color: #155724;
                        border-radius: .3em;
                        animation: 0.8s infinite alternate ease-out breathing-color;
                    }
                    @keyframes breathing-color {
                        from {
                            background-color: #d4edda;
                            border: 1px #c3e6cb solid;
                        }
                        to {
                            background-color: #95d2a4;
                            border: 1px #87cd97 solid;
                        }
                    }
                </style>
            </div>
            """.trim();
        }
    }
}

Map changelog() {
    dynamicPage (
        name: 'changelog',
        title: "<b>Change Log</b>",
        install: false,
        uninstall: false
    ) {

        section ('v2.2.0 - 2024-07-01', hideable: true, hidden: false) {
            paragraph '<li>Add "Advanced Zigbee pairing" functionality</li>';
        }

        section ('v2.1.0 - 2024-05-16', hideable: true, hidden: true) {
            paragraph """
                <li>Change "poor" link quality color from yellow to violet - @Horseflesh</li>
                <li>Change "good" link quality LQI interval from [150 - 200) to [130 - 200)</li>
                <li>Scrollbars on tab contents appear only when ncesessary</li>
            """.replaceAll(/\n\s*/, '');
        }

        section ('v2.0.0 - 2024-03-09', hideable: true, hidden: true) {
            paragraph """
                <li><b>Breaking change</b>: Some files were renamed therefore your bookmarks or PWA installs might be broken</li>
                <li>Add Zigbee routes map - @Tony</li>
                <li>Fade-out the nodes tooltip to see better how things are connected - @danabw</li>
            """.replaceAll(/\n\s*/, '');
        }

        section ('v1.5.0 - 2024-02-23', hideable: true, hidden: true) {
            paragraph """
                <li>Add config option to show/hide link colors
                <li>Make node hover effect (see neighbors) more visible - @WarlockWeary</li>
            """.replaceAll(/\n\s*/, '');
        }

        section ('v1.4.0 - 2024-02-23', hideable: true, hidden: true) {
            paragraph """
                <li>Color links based on LQI/LQA value - @Horseflesh</li>
                <li>Hide back <b>duplex</b> links by default to better see the link colors</li>
                <li>Use <b>Esc</b> keyboard key to toggle the controls - @jshimota</li>
            """.replaceAll(/\n\s*/, '');
        }

        section ('v1.3.0 - 2024-02-21', hideable: true, hidden: true) {
            paragraph """
                <li>Add option to show/hide <b>Unknown</b> devices - @Tony</li>
                <li>Remove the Hub device from the <b>Devices</b> tab - @jimhim</li>
                <li>Add PWA manifest</li>
            """.replaceAll(/\n\s*/, '');
        }

        section ('v1.2.0 - 2024-02-20', hideable: true, hidden: true) {
            paragraph '<li>Add option to use an image as map background (e.g.: home layout)</li>'
        }

        section ('v1.1.0 - 2024-02-19', hideable: true, hidden: true) {
            paragraph """
                <li>Add <b>Done</b> button in the Hubitat app - @dnickel</li>
                <li>Click the address of any device in the <b>Devices</b> tab to add it to the Interview Queue - @hubitrep</li>
                <li>Use relative URL when opening the HTML app - @jlv</li>
                <li>Mark devices that failed the Interview  - @kahn-hubitat</li>
                <li>Show Interview Queue size</li>
                <li>Show <b>duplex</b> links by default</li>
            """.replaceAll(/\n\s*/, '');
        }

        section ('v1.0.0 - 2024-02-16', hideable: true, hidden: true) {
            paragraph '<li>Initial release</li>'
        }
    }
}

def getLocalURL(String fileName) {
    String fullURL = "${getFullLocalApiServerUrl()}/${fileName}?access_token=${state.accessToken}&dark=${useDarkTheme == true}";
    return (fullURL =~ URL_PATTERN).findAll()[0][1]
}

// ===================================================================================================================
// Buttons
// ===================================================================================================================

String hrefButton(String btnName, String href, String iconName=null) {
    String output = ""
    output += """<button onClick="location.href='""" + href + """'" class="p-button p-component mr-2 mb-2" type="button" aria-label="hrefButton" data-pc-name="button" data-pc-section="root" data-pd-ripple="true">"""
    if (iconName) output += btnIcon(iconName)
    output += btnName
    output += """<span role="presentation" aria-hidden="true" data-p-ink="true" data-p-ink-active="false" class="p-ink" data-pc-name="ripple" data-pc-section="root"></span></button>"""
    return output
}

void appButtonHandler(String btn) {
}

// ===================================================================================================================
// Implement Mappings
// ===================================================================================================================

mappings {
    path("/${NEIGHBORS_FILE_NAME}") { action:[ GET:'loadNeighborsMapMapping' ]}
    path("/${ROUTES_FILE_NAME}") { action:[ GET:'loadRoutesMapMapping' ]}
    path("/${MEMCPU_FILE_NAME}") { action:[ GET:'loadMemCpuHistoryMapping' ]}
    path('/neighbors.webmanifest') { action:[ GET:'loadNeighborsManifestMapping' ]}
    path('/routes.webmanifest') { action:[ GET:'loadRoutesManifestMapping' ]}
    path('/neighbors/:addr/:startIndex') { action:[ GET:'neighborsMapping' ]}
    path('/routes/:addr/:startIndex') { action:[ GET:'routesMapping' ]}
    path("/pairing/:addr") { action:[ GET:'pairingMapping' ]}
}

def loadNeighborsMapMapping() {
    debug "Proxying ${NEIGHBORS_FILE_NAME} to ${request.HOST} (${request.requestSource})"
    return render(
        status: 200,
        contentType: 'text/html',
        data: new String(downloadHubFile(NEIGHBORS_FILE_NAME), 'UTF-8').replaceAll('\\$\\{access_token\\}', "${state.accessToken}")
    )
}

def loadRoutesMapMapping() {
    debug "Proxying ${ROUTES_FILE_NAME} to ${request.HOST} (${request.requestSource})"
    return render(
        status: 200,
        contentType: 'text/html',
        data: new String(downloadHubFile(ROUTES_FILE_NAME), 'UTF-8').replaceAll('\\$\\{access_token\\}', "${state.accessToken}")
    )
}

def loadMemCpuHistoryMapping() {
    debug "Proxying ${MEMCPU_FILE_NAME} to ${request.HOST} (${request.requestSource})"
    return render(
        status: 200,
        contentType: 'text/html',
        data: new String(downloadHubFile(MEMCPU_FILE_NAME), 'UTF-8').replaceAll('\\$\\{access_token\\}', "${state.accessToken}")
    )
}

def loadNeighborsManifestMapping() {
    return buildManifest(NEIGHBORS_FILE_NAME, 'Neighbors Map', '11ba8718-86f0-4461-ae21-8627001d3e8e')
}

def loadRoutesManifestMapping() {
    return buildManifest(ROUTES_FILE_NAME, 'Routing Map', '5105cab6-2f20-43da-b73c-19c8dfe277a2')
}

def buildManifest(fileName, appName, appId) {
    debug 'Loading PWA manifest'
    String appIcon = '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'
    String maskableIcon = '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'
    return render(
        status: 200,
        contentType: 'application/manifest+json',
        data: """\
        {
            "id": "${appId}",
            "name": "${appName}",
            "short_name": "${appName}",
            "description": "Visualize the topology and connectivity of your Zigbee network.",
            "start_url": "${getLocalURL(fileName)}",
            "icons": [{
                "src": "data:image/png;base64,${maskableIcon}",
                "sizes": "512x512",
                "type": "image/png",
                "purpose": "maskable"
            },{
                "src": "data:image/png;base64,${appIcon}",
                "sizes": "512x512",
                "type": "image/png"
            }],
            "categories": ["utilities"],
            "display": "standalone",
            "orientation": "portrait",
            "theme_color": "${useDarkTheme ? "#073642" : "#eee8d5"}",
            "background_color": "${useDarkTheme ? "#073642" : "#eee8d5"}"
        }
        """
    )
}

def neighborsMapping() {
    String addr = "${params.addr}"
    Integer startIndex = Integer.parseInt(params.startIndex)
    debug "Interview neighbors: ${addr}:${startIndex}"

    // Do some checks to make sure we interview the right thing
    if (!addr || !HEXADECIMAL_PATTERN.matcher(addr).matches()) return render(
        status: 400,
        contentType: 'application/json',
        data: "{\"addr\": \"${addr}\", \"status\": false}"
    )

    // Use the helper device to do the actual interview
    fetchHelper().interviewNeighbors(addr, startIndex)
    return render(
        status: 200,
        contentType: 'application/json',
        data: "{\"addr\": \"${addr}\", \"startIndex\": ${startIndex}, \"status\": true}"
    )
}

def routesMapping() {
    String addr = "${params.addr}"
    Integer startIndex = Integer.parseInt(params.startIndex)
    debug "Interview routes: ${addr}:${startIndex}"

    // Do some checks to make sure we interview the right thing
    if (!addr || !HEXADECIMAL_PATTERN.matcher(addr).matches()) return render(
        status: 400,
        contentType: 'application/json',
        data: "{\"addr\": \"${addr}\", \"status\": false}"
    )

    // Use the helper device to do the actual interview
    fetchHelper().interviewRoutes(addr, startIndex)
    return render(
        status: 200,
        contentType: 'application/json',
        data: "{\"addr\": \"${addr}\", \"startIndex\": ${startIndex}, \"status\": true}"
    )
}

def pairingMapping() {
    String addr = "${params.addr}"
    debug "Stopping Zigbee pairing on all devices but: ${addr}"

    // Do some checks to make sure we interview the right thing
    if (!addr || !HEXADECIMAL_PATTERN.matcher(addr).matches()) return render(
        status: 400,
        contentType: 'application/json',
        data: "{\"addr\": \"${addr}\", \"status\": false}"
    )

    // Use the helper device to do the actual work
    fetchHelper().stopPairingOnAllDevicesButThis(addr)
    return render(
        status: 200,
        contentType: 'application/json',
        data: "{\"addr\": \"${addr}\", \"status\": true}"
    )
}
