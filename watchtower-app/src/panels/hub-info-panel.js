import { html, css, LitElement } from '../vendor/vendor.min.js';
import { DatastoreHelper } from '../helpers/datastore-helper.js';

const FIELDS = {
    a: { label: 'Name', render: (hubInfo, hubData) => html`${hubData.name}${hubData.alerts.runAlertsCounter - (hubData.alerts.platformUpdateAvailable ? 1 : 0) === 0 ? '' : html` <a href="/alerts" target="_blank" title="Click to see hub alerts">🚩</a>`}` },
    b: { label: 'Model', render: (hubInfo, hubData) => html`${hubInfo.model}` },
    c: { label: 'IP', render: (hubInfo, hubData) => html`${hubData.ipAddress}` },
    d: { label: 'Fw Ver', render: (hubInfo, hubData) => html`${hubData.version}${hubData.alerts.platformUpdateAvailable === false ? '' : html` <a href="/hub/platformUpdate" target="_blank" title="Platform update available">🚩</a>`}` },
    e: { label: 'CPU', render: (hubInfo, hubData) => html`${hubInfo.cpu}` },
    f: { label: 'RAM', render: (hubInfo, hubData) => html`${hubInfo.ram}` },
    g: { label: 'Temp', render: (hubInfo, hubData) => html`${hubInfo.temp}` },
    h: { label: 'DB Size', render: (hubInfo, hubData) => html`${hubInfo.db}` },
    i: { label: 'Reboot', render: (hubInfo, hubData) => html`${hubInfo.reboot}` },
}

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
            border-top: 1px color-mix(in srgb, var(--text-color-darker), transparent 80%) solid;
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
        config: { type: Object, reflect: true },
        hubInfo: { type: Object, state: true },
        hubData: { type: Object, state: true },
        mobileView: { type: Boolean, state: true },
    }

    render() {
        return this.hubInfo === undefined || this.hubData === undefined ? '' : html`
            <table>
                <tbody>
                    <tr><td colspan="3"></td></tr>
                    ${Object.entries(FIELDS).map(([key, val]) => {
                        if (!this.config?.info?.includes(key)) return ''
                        return html`<tr><td>${val.label}</td><td colspan="2">${val.render(this.hubInfo, this.hubData)}</td>`
                    })}
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

    static properties = {
        info: { type: Array, state: true },
    }

    constructor() {
        super()
        this.info = []
    }

    render() {
        return html`
            <section>
                <label>Select details to display:</label>
                ${Object.entries(FIELDS).map(([key, val]) => {
                    return html`<label><input value="${key}" type="checkbox"
                        ?required=${this.info.length == 0}
                        @change=${this.onFieldSelect}
                    > ${val.label}</label>`
                })}
            </section>
        `
    }

    createRenderRoot() {
        return this
    }

    firstUpdated() {
        console.log('firstUpdated')
        setTimeout(() => this.dispatchEvent(new CustomEvent('suggestTitle', { detail: 'Hub Information' })), 200)
    }

    onFieldSelect(event) {
        console.log('onFieldSelect', event.target.value, event.target.checked)
        if (event.target.checked) this.info = this.info.concat([event.target.value])
        else this.info = this.info.filter(item => item !== event.target.value)
    }

    decorateConfig(config) {
        return { ...config, info: this.info.join('') }
    }
}
