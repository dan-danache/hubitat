import { html, LitElement } from '../vendor/vendor.min.js';

import { DatastoreHelper } from '../helpers/datastore-helper.js';
import { UiHelper } from '../helpers/ui-helper.js'
import { ChartHelper } from '../helpers/chart-helper.js'

export class AttributePanel extends LitElement {
    static properties = {
        config: { type: Object, reflect: true },
        yScale: { type: String, reflect: true },
        chart: { type: Object, state: true },
        nodata: { type: Boolean, state: true }
    }

    createRenderRoot() {
        return this
    }

    render() {
        return html`
            <canvas tabindex='1' @keydown=${this.onKeyDown}></canvas>
            <precision-selector @change=${this.changePrecision} .precision=${this.config.precision}></precision-selector>
            <nav title="Edit tile" @click=${this.editPanel}>⚙️</nav>
            ${ this.nodata === true ? html`<aside>No data yet</aside>` : '' }
        `;
    }

    async connectedCallback() {
        super.connectedCallback()
        if (this.config.precision === undefined) this.config.precision = '5m'
    }

    async firstUpdated() {
        await this.initChart()
    }

    onKeyDown(event) {
        if (this.chart === undefined) return
        if (event.keyCode === 37) this.chart.crosshair.panZoom('left')
        else if(event.keyCode === 39) this.chart.crosshair.panZoom('right')
    }

    async initChart() {
        this.classList.add('spinner')
        const supportedAttributes = await DatastoreHelper.fetchSupportedAttributes()
        const monitoredDevices = await DatastoreHelper.fetchMonitoredDevices()
        const colors = UiHelper.colors()

        const data = await DatastoreHelper.fetchAttributeData(this.config)
        //this.nodata = data.attr1.length == 0

        const $config = ChartHelper.lineConfig()
        const datasets = []
        for (const deviceId of this.config.devs) {
            datasets.push({
                label: monitoredDevices.find(monitoredDevice => monitoredDevice.id == deviceId).name,
                data: data[`dev_${deviceId}`],
                pointStyle: false,
                borderWidth: 1.2,
                tension: 0.5,
                unit: supportedAttributes[this.config.attr].unit,
                ref: `dev_${deviceId}`
            })
        }

        if (this.config.devs.length <= 2) {
            for (let idx = 0; idx < this.config.devs.length; idx++) {
                const color = UiHelper.chartColors[idx]
                datasets[idx].fill = 'start'
                datasets[idx].borderColor = color
                datasets[idx].backgroundColor = `${color}44`
            }
        }

        const attrLabel = UiHelper.prettyName(this.config.attr)
        $config.options.scales.y = {
            position: 'left',
            display: true,
            title: {
                display: true,
                text: `${attrLabel} ${supportedAttributes[this.config.attr].unit}`,
                color: colors.TextColorDarker
            },
            ticks: { color: colors.TextColorDarker },
            grid: { color: colors.TextColorDarker + '33' }
        }
        this.attrMin = supportedAttributes[this.config.attr].min
        this.attrMax = supportedAttributes[this.config.attr].max
        if (this.yScale === 'fixed') {
            if (this.attrMin !== undefined) $config.options.scales.y.suggestedMin = this.attrMin
            if (this.attrMax !== undefined) $config.options.scales.y.suggestedMax = this.attrMax
        }

        $config.data = { datasets }

        // Apply user script
        ChartHelper.executeUserScript(this.config.uscript, $config)

        if (this.chart !== undefined) this.chart.destroy()
        this.chart = new Chart(this.renderRoot.querySelector('canvas'), $config)
        this.chart.precision = this.config.precision
        ChartHelper.updateChartType(this.chart)
        setTimeout(() => this.classList.remove('empty', 'spinner'), 200)
    }

    async changePrecision(event) {
        this.chart.crosshair.resetZoom()
        this.config.precision = event.detail
        this.config.precision = event.detail
        await this.refresh()
    }

    async refresh() {
        await this.initChart()
    }

    editPanel() {
        this.dispatchEvent(new CustomEvent('edit', { bubbles: true, detail: this.config }))
    }

    decorateConfig(config) {
        return { ...config, ...this.config }
    }

    setYScale(yScale) {
        this.yScale = yScale
        if (!this.chart) return
        if (yScale == 'auto') {
            delete this.chart.options.scales.y.suggestedMin
            delete this.chart.options.scales.y.suggestedMax
        } else {
            this.chart.options.scales.y.suggestedMin = this.attrMin
            this.chart.options.scales.y.suggestedMax = this.attrMax
        }
        this.chart.update()
    }
}

export class AttributePanelConfig extends LitElement {
    static properties = {
        config: { type: Object, reflect: true },
        devices: { type: Object, state: true },
        attributes: { type: Object, state: true },
    }

    constructor() {
        super()
        this.config = {
            devs: []
        }
        this.devices = undefined
        this.attributes = undefined
    }

    createRenderRoot() {
        return this
    }

    render() {
        return html`
            <fieldset>
                <section>
                    <label for="device">Select attribute to chart:</label>
                    ${this.attributes ? this.renderAttributesSelect() : html`<aside class="spinner">Loading devices ...</aside>`}
                </section>
            </fieldset>
            ${this.config.attr && this.devices ? this.renderDevicesSelect() : '' }
        `
    }

    async connectedCallback() {
        super.connectedCallback()
        this.devices = await DatastoreHelper.fetchMonitoredDevices();
        const attrs = new Set()
        this.devices.forEach(device => device.attrs.forEach(attr => attrs.add(attr)))
        this.attributes = [...attrs].sort()
    }

    renderAttributesSelect() {
        return html`
            <section>
                <select id="attr" @change=${this.onAttributeSelect} required="true">
                    <option value=""></option>
                    ${this.attributes.map(attribute => html`
                        <option value="${attribute}" .selected=${this.config.attr === attribute}>${attribute}</option>
                    `
                    )}
                </select>
                ${ this.config.attr ? html`
                    <div>
                        <div class="checkbox">
                            <input type="checkbox" id="z"
                                .checked="${this.config.z}"
                                @change=${ event => this.config.z = event.target.checked }
                            >
                            <label for="z">Render zero for missing values</label>
                        </div>
                    </div>
                ` : ''}
            </section>
        `
    }

    renderDevicesSelect() {
        const devices = this.devices.filter(device => device.attrs.includes(this.config.attr))
        return html`
            <fieldset>
                <section id="devlist">
                    <label>Select devices (at least one):</label>
                    ${devices.map(device => {
                        return html`
                            <div class="checkbox">
                                <input type="checkbox" value="${device.id}" id="d${device.id}"
                                    .checked="${this.config.devs.find(dev => dev == device.id)}"
                                    ?required=${this.config.devs.length == 0}
                                    @change=${this.onDeviceSelect}
                                >
                                <label for="d${device.id}">${device.name}</label>
                            </div>
                        `
                    })}
                </section>
            </fieldset>
        `
    }

    onAttributeSelect(event) {
        const attr = event.target.value !== '' ? event.target.value : undefined
        this.config = { ...this.config,
            attr,
            z: false,
            devs: [],
        }
        if (attr) this.dispatchEvent(new CustomEvent('suggestTitle', { detail: UiHelper.prettyName(attr) }))
    }

    onDeviceSelect() {
        this.config = { ...this.config,
            devs: [...this.renderRoot.querySelectorAll('#devlist input[type="checkbox"]:checked')].map(input => input.value),
        }
    }

    decorateConfig(config) {
        return {
            ...config,
            attr: this.config.attr,
            devs: this.config.devs.map(deviceId => parseInt(deviceId)),
            z: this.config.z === true ? true : undefined,
        }
    }
}
