import groovy.transform.Field

@Field static final String APP_NAME = 'Lumos'
@Field static final String APP_VERSION = '1.0.0'

definition (
    name: "${APP_NAME} ${APP_VERSION}",
    namespace: 'dandanache',
    author: 'Dan Danache',
    description: 'Control lights using motion and contact sensors.',
    documentationLink: 'https://community.hubitat.com/t/zigbee-visual-render-for-getchildandrouteinfo/119074',
    importUrl: 'https://raw.githubusercontent.com/dan-danache/hubitat/lumos-app_1.0.0/lumos-app/lumos.groovy',
    category: 'Control',
    iconUrl: '',
    iconX2Url: '',
    singleInstance: true,
    installOnOpen: true,
)

preferences {
   page (title:'Control lights using motion and contact sensors.', name:'mainPage', install:true, uninstall:true) {
      section {
         app (name:'childApps', appName:"Lumos Automation ${APP_VERSION}", namespace:'dandanache', title:'+ Create new Lumos Automation ...', multiple:true, width:4)
      }
   }
}
