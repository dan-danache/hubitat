import { WatchtowerApp } from './components/watchtower-app.js'
import { DashboardMenu } from './components/dashboard-menu.js'
import { DashboardAddDialog } from './components/dashboard-add-dialog.js'
import { DashboardGrid } from './components/dashboard-grid.js'
import { PrecisionSelector } from './panels/precision-selector.js';

import { DevicePanel, DevicePanelConfig } from './panels/device-panel.js';
import { AttributePanel, AttributePanelConfig } from './panels/attribute-panel.js';
import { TextPanel, TextPanelConfig } from './panels/text-panel.js';
import { IframePanel, IframePanelConfig } from './panels/iframe-panel.js';
import { HubInfoPanel, HubInfoPanelConfig } from './panels/hub-info-panel.js';

customElements.define('watchtower-app', WatchtowerApp)
customElements.define('dashboard-menu', DashboardMenu)
customElements.define('dashboard-add-dialog', DashboardAddDialog)
customElements.define('dashboard-grid', DashboardGrid)
customElements.define('precision-selector', PrecisionSelector)

customElements.define('device-panel', DevicePanel)
customElements.define('device-panel-config', DevicePanelConfig)

customElements.define('attribute-panel', AttributePanel)
customElements.define('attribute-panel-config', AttributePanelConfig)

customElements.define('text-panel', TextPanel)
customElements.define('text-panel-config', TextPanelConfig)

customElements.define('iframe-panel', IframePanel)
customElements.define('iframe-panel-config', IframePanelConfig)

customElements.define('hub-info-panel', HubInfoPanel)
customElements.define('hub-info-panel-config', HubInfoPanelConfig)
