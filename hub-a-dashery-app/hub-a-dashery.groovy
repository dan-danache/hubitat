/*
 * Hub-a-Dashery app for Hubitat
 *
 * View dashboards for your Hubitat hub metrics.
 *
 * @see https://github.com/dan-danache/hubitat
 */
import groovy.transform.Field
import com.hubitat.app.ChildDeviceWrapper

@Field static final String APP_NAME = "Hub-a-Dashery"
@Field static final String APP_VERSION = "1.3.0"
@Field static final String HTML_FILE_NAME = "hub-a-dashery.html"
@Field static final def URL_PATTERN = ~/^https?:\/\/[^\/]+(.+)/

definition(
    name: APP_NAME,
    namespace: "dandanache",
    author: "Dan Danache",
    description: "View dashboards for your Hubitat hub metrics",
    documentationLink: "https://community.hubitat.com/t/release-hub-a-dashery-app/134375",
    importUrl: "https://raw.githubusercontent.com/dan-danache/hubitat/hub-a-dashery_1.1.0/hub-a-dashery-app/hub-a-dashery.groovy",
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
    log.info "${app?.getLabel()} has been installed"
}

def updated() {
    log.info "${app?.getLabel()} has been updated"
}

def refresh() {
    log.info "${app?.getLabel()} has been refreshed"
}

private void debug(message) {
    if (logEnable) log.debug message
}

// ===================================================================================================================
// Implement Pages
// ===================================================================================================================

preferences {
    page name: "mainpage"
    page name: "changelog"
}

Map mainpage() {
    def showInstall = app.getInstallationState() == "INCOMPLETE"
    dynamicPage (
        name: "mainpage",
        title: "<b>${APP_NAME} v${APP_VERSION}</b>",
        install: true,
        uninstall: !showInstall
    ) {
        if (app.getInstallationState() == "COMPLETE") {
            if (!state.accessToken) createAccessToken()

            section {

                // Links / pages
                href (
                    name: "memCpuHistoryLink",
                    title: "View Memory & CPU history",
                    description: "Graph hub memory and processor usage (since last reboot)",
                    url: "${buildURL(HTML_FILE_NAME)}",
                    style: "embedded",
                    state: "complete",
                    required: false,
                )
                href (
                    name: "changelogLink",
                    title: "View change log",
                    description: "See latest application changes",
                    page: "changelog",
                    required: false,
                )

                // Preferences
                input(
                    name: "useDarkTheme",
                    type: "bool",
                    title: "Use dark theme",
                    defaultValue: false,
                    submitOnChange: true
                )
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
        section ("v1.3.0 - 2024-03-04", hideable: true, hidden: false) {
            paragraph "<li>Add internal temperature chart (refresh every 1 min)</li>" +
                "<li>Add option to stop specific charts from rendering - @amithalp</li>" +
                "<li>Fix memory limit for Hubitat C-8 Pro - @ady624</li>" +
                "<li>Remove background color and shadows when loaded inside an iframe (e.g. Hubitat dashboard)</li>"
        }

        section ("v1.2.1 - 2024-03-01", hideable: true, hidden: true) {
            paragraph "<li>Fix grid layout saving function - @amithalp</li>"
        }

        section ("v1.2.0 - 2024-03-01", hideable: true, hidden: true) {
            paragraph "<li>Add hub information table chart (refresh every 5 min) - @iEnam</li>" +
                "<li>Charts have now a fixed 206px height per \"grid unit\" instead of 25% of the window height</li>" +
                "<li>Restore crosshair vertical marker for line charts - @WarlockWeary</li>"
        }

        section ("v1.1.0 - 2024-02-29", hideable: true, hidden: true) {
            paragraph "<li>Add grid system to move charts around</li>" +
                "<li>Add Free Mem and CPU Load Avg gauge charts (refresh every 1 minute)</li>" +
                "<li>Reset graph zoom on double-click</li>" +
                "<li>Enable data decimation to reduce number of rendered points</li>" +
                "<li>Remove call to non-existing method \"fetchHelper()\" during install - @WarlockWeary</li>" +
                "<li>Fix year reset bug for history charts - @hubitrep</li>"
        }

        section ("v1.0.0 - 2024-02-26", hideable: true, hidden: true) {
            paragraph "<li>Initial release</li>"
        }
    }
}

def buildURL(String fileName) {
    String prefix = useCloudLinks == true
        ? "${getApiServerUrl()}/${hubUID}/apps/${app.id}"
        : "${(getFullLocalApiServerUrl() =~ URL_PATTERN).findAll()[0][1]}"
    
    return "${prefix}/${fileName}?access_token=${state.accessToken}&dark=${useDarkTheme == true}"
}

// ===================================================================================================================
// Implement Mappings
// ===================================================================================================================

mappings {
    path("/${HTML_FILE_NAME}") {action: [GET: "getMemCpuHistoryMapping"]}
    path("/app.webmanifest") {action: [GET: "getAppManifestMapping"]}
    path("/grid-layout.json") {action: [GET: "getGridLayoutMapping", PUT: "setGridLayoutMapping"]}
    path("/hub-info.json") {action: [GET: "getHubInfoMapping"]}
}

def getMemCpuHistoryMapping() {
    debug "Proxying ${HTML_FILE_NAME} to ${request.HOST} (${request.requestSource})"
    return render(
        status: 200,
        contentType: "text/html",
        data: new String(downloadHubFile(HTML_FILE_NAME), "UTF-8").replaceAll('\\$\\{access_token\\}', "${state.accessToken}")
    )
}

def getAppManifestMapping() {
    debug "Returning PWA manifest"
    String appIcon = "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"
    String maskableIcon = "iVBORw0KGgoAAAANSUhEUgAAAgAAAAIABAMAAAAGVsnJAAAAElBMVEVHcExxpQBxpQBwpQBxpgCAvACY5M8+AAAABXRSTlMAJ7Ti/ZKd2DUAAANgSURBVHja7d1BctowGIZhAxcgkANAwwEo4+wbW75AY93/Kl22ixJJaQnw8zx7zTCadz5WHnUAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAwB8WT+esuwew6dN5476L7iXN+bw5fe9i26b8sRS7gUXKJWPoIXiecsl87ALrc9nQxbWYctm87sJaVV3ALvAE5BpvXVgHF5AfewVfH/0Cplzj/dEvYFSAAhSgAAUoQAEKUIACFKAABShAAQpQgAIUoAAFKEABClCAAhSgAAUoQAEKUIACFKAABShAAQpQgAIUoAAFKEABClCAAhSgAAUoQAEKUIACFKAABShAAQpQgAIUoAAFKEABClCAAhSgAAUoQAEKUIACFKAABShAAQq42wI2L306ZzztwxewSanwME3sApbFh2l2oQtY9OUD68gFrGoepolcwCGXDdEKaH+YJm4By6oLOMYqINjDNO0FtF/AKVgB7Q/TxC3gUHdncQt4zTXGWAW0X8D86AXMj15AUoANUIANUIANUIANUIANUIANUIANUIANUIANUIANUIANUIANUIANUIANUIANUIANUIANUMAFFD5m+bYOvwHbNH0gpX3wAjblj1mibUDrxyxD6AK2Uy6Zd5E3oM9lb8EKaP+YJfAGrKouYBe3gOdcYT7G3YBDrvEWq4D2n3aKuwFTrjEEK6D9AsJuwGLKNX6GLWBxSwXMCghwAUE3YFDAl2yAAmyAAmyAAmyAAmyAAmyAAmyAAmyAAmyAAmyAAmyAAmyAAmyAAmyAAmyAAmyAAmyAAmyAAmyAAmyAAmyAAmyAAmyAAmyAAmyAAmyAAmyAAmyAAmyAAmyAAmyAAmyAAmyAAmyAAmyAAmyAAmyAAmyAAmyAAmyAAmyAAmyAAmyAAmyAAmyAAmyAAmyAAmyAAmzArReQ7uSdode6aNqPvIe6gKH9RbPxTt4aO1zqyHDrr819/shz5ZErqPtpx/YjP+7kAla5wrz7x0cql5VHbvV/MLUfmdefOXINffs6HS515E7eHe5W7UeWFUeO3c2+PN1+ZFx/5sh1bGvfHv9teaEjV7uBaT4f5pz2fz0yNR7ZFI9cz+alT+eMp6f/dOSpcAQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAACAu/cLBQBL1l75RF4AAAAASUVORK5CYII="
    return render(
        status: 200,
        contentType: "application/manifest+json",
        data: """\
        {
            "id": "cdbd0ce0-f8f9-4bde-90de-6bc321394871",
            "name": "Hub-a-Dashery",
            "short_name": "Hub-a-Dashery",
            "description": "Collect and visualize Hubitat metrics.",
            "start_url": "${buildURL(HTML_FILE_NAME)}",
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

def getGridLayoutMapping() {
    debug "Returning grid layout"
    String hubVersion = getHubVersion()
    return render(
        status: 200,
        contentType: "application/json",
        data: state.gridLayout != null ? state.gridLayout : "{\"widgets\":{}}"
    )
}

def setGridLayoutMapping() {
     runIn(1, "saveGridLayout", [data: "${request.body}"])
     return render(
         status: 200,
         contentType: "application/json",
         data: "{\"status\": true}"
     )
}

def saveGridLayout(data) {
    state.gridLayout = data
}

def getHubInfoMapping() {
    debug "Returning Hub information"
    return render(
        status: 200,
        contentType: "application/json",
        data: """\
        {
            "name": "${location.hub.name}",
            "ip": "${location.hub.localIP}",
            "uptime": ${location.hub.uptime.toLong()},
            "model": "${getHubVersion()}",
            "fw": "${location.hub.firmwareVersionString}",
            "tscale": "${location.temperatureScale}"
        }
        """
    )
}
