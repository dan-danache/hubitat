import groovy.transform.Field

definition (
    name: "Lumos 1.0.0",
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
         app (name:'childApps', appName:"Lumos Automation 1.0.0", namespace:'dandanache', title:'+ Create new Lumos Automation ...', multiple:true, width:4)
      }
   }
}
