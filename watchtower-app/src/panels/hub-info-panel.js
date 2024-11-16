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
        nav {
            position: absolute;
            top: 0px; left: 2px;
            cursor: pointer;
            visibility: hidden;
        }
        :host(:hover) nav {
            visibility: visible;
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
            <nav title="Edit tile" @click=${this.editPanel}>⚙️</nav>
        `;
    }

    async connectedCallback() {
        super.connectedCallback()
        this.refresh()
    }

    editPanel() {
        this.dispatchEvent(new CustomEvent('edit', { bubbles: true, detail: this.config }))
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
        config: { type: Object, reflect: true },
    }

    constructor() {
        super()
        this.config = { info: [] }
    }

    render() {
        return html`
            <fieldset>
                <label>Select hub details to display:</label>
                <div>
                    ${Object.entries(FIELDS).map(([key, val]) => {
                        return html`
                            <div class="checkbox">
                                <input type="checkbox" value="${key}" id="h${key}"
                                    ?required=${this.config.info.length == 0}
                                    .checked=${this.config.info.includes(key)}
                                    @change=${this.onFieldSelect}
                                >
                                <label for="h${key}">${val.label}</label>
                            </div>
                        `
                    })}
                </div>
            </fieldset>
        `
    }

    connectedCallback() {
        super.connectedCallback()
        if (typeof this.config?.info == 'string') {
            this.config = { ...this.config,
                info: this.config.info.split(''),
            }
        }
        if (this.config?.info === undefined) {
            this.config = { ...this.config,
                info: [],
            }
        }
    }

    createRenderRoot() {
        return this
    }

    firstUpdated() {
        setTimeout(() => this.dispatchEvent(new CustomEvent('suggestTitle', { detail: 'Hub Information' })), 200)
    }

    onFieldSelect(event) {
        if (event.target.checked) this.config = { info: this.config.info.concat([event.target.value]) }
        else this.config = { info: this.config.info.filter(item => item !== event.target.value) }
    }

    decorateConfig(config) {
        return { ...config,
            info: this.config.info.join(''),
        }
    }
}
