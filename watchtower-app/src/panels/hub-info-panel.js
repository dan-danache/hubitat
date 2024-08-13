import { html, css, LitElement, nothing } from '../vendor/vendor.min.js';
import { DatastoreHelper } from '../helpers/datastore-helper.js';

export class HubInfoPanel extends LitElement {
    static styles = css`
        :host {
            display: block;
            width: 100%;
            height: 100%;
        }
        table {
            width: 100%;
            height: 100%;
            border-collapse: collapse;
            color: var(--text-color-darker);
            table-layout: fixed;
        }
        table tr:first-child td {
            height: 20px;
            padding: 0;
            border: 0;
        }
        table td {
            border-top: 1px var(--bg-color) solid;
            padding: 0 0.5em;
            overflow: hidden;
            white-space: nowrap;
            text-overflow: ellipsis;
        }
        table tr td:first-child {
            text-align: right;
            font-weight: bold;
        }
        a {
            text-decoration: none;
            color: red;
            line-height: 1em;
        }
    `;

    static properties = {
        hubInfo: { type: Object, state: true },
        hubData: { type: Object, state: true },
        mobileView: { type: Boolean, state: true },
    }

    render() {
        return this.hubInfo === undefined ? nothing: html`
            <table>
                <tbody>
                    <tr><td colspan="3"></td></tr>
                    <tr><td>Name</td><td colspan="2">${this.hubData.name}${this.hubData.alerts.runAlertsCounter === 0 ? nothing : html` <a href="/alerts" target="_blank" title="${this.hubData.alerts.runAlertsCounter} alerts">🚨</a>`}</td></tr>
                    <tr><td>IP</td><td colspan="2">${this.hubData.ipAddress}</td></tr>
                    <tr><td>FW Ver</td><td colspan="2">${this.hubData.version}${this.hubData.alerts.platformUpdateAvailable === false ? nothing : html` <a href="/hub/platformUpdate" target="_blank" title="Platform update available">🚩</a>`}</td></tr>
                    <tr><td>Model</td><td colspan="2">${this.hubInfo.model}</td></tr>
                    <tr><td>Reboot</td><td colspan="2">${this.hubInfo.reboot}</td></tr>
                </tbody>
            </table>
        `;
    }

    async connectedCallback() {
        super.connectedCallback()
        this.refresh()
    }

    async refresh() {
        this.hubInfo = await DatastoreHelper.fetchHubInfo()
        this.hubData = await DatastoreHelper.fetchHubData()
        const adapter = new Chart._adapters._date({timeZone: 'UTC'});
        const lastReboot = adapter.add(new Date(), -this.hubInfo.uptime, 'second');
        this.hubInfo.reboot = adapter.format(lastReboot, 'Pp').replace(',', '')
        setTimeout(() => this.classList.remove('empty', 'spinner'), 100)
    }

    decorateConfig(config) {
        return config
    }
}

export class HubInfoPanelConfig extends LitElement {

    render() {
        return html``
    }

    createRenderRoot() {
        return this
    }

    firstUpdated() {
        console.log('firstUpdated')
        setTimeout(() => this.dispatchEvent(new CustomEvent('suggestTitle', { detail: 'Hub Information' })), 200)
    }

    decorateConfig(config) {
        return config
    }
}
