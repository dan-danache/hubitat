import { html, css, LitElement, nothing } from '../vendor/vendor.min.js';

import { DatastoreHelper } from '../helpers/datastore-helper.js';
import { ColorHelper } from '../helpers/color-helper.js'
import { ChartHelper } from '../helpers/chart-helper.js'

export class AttributePanel extends LitElement {
    static styles = css`
       :host {
            display: block;
            width: 100%;
            height: 100%;
        }
        canvas {
            width: 100%;
            height: 100%;
        }
        :host(.empty) canvas { visibility: hidden }
        precision-selector {
            position: absolute;
            bottom: 0;
            left: 50%;
            transform: translate(-50%, 0);
            visibility: hidden;
        }
        :host(:hover) precision-selector {
            visibility: visible;
        }
        aside {
            position: absolute;
            top: 50%;
            left: 0;
            transform: translate(0, -50%);
            display: block;
            color: var(--text-color-darker);
            width: 100%;
            text-align: center;
        }
    `

    static properties = {
        config: { type: Object, reflect: true },
        mobileView: { type: Boolean, state: true },
        chart: { type: Object, state: true },
        nodata: { type: Boolean, state: true }
    }

    render() {
        return html`
            <canvas></canvas>
            <precision-selector @change=${this.changePrecision} .precision=${this.config.precision}></precision-selector>
            ${ this.nodata === true ? html`<aside>No data yet</aside>` : nothing}
        `;
    }

    updated(changedProperties) {
        if (changedProperties.mobileView == this.mobileView) return
        this.chart.options.plugins.zoom.pan.enabled = !this.mobileView
        this.chart.options.plugins.zoom.zoom.pinch.enabled = !this.mobileView
    }

    async connectedCallback() {
        super.connectedCallback()

        if (this.config.precision === undefined) this.config.precision = '5m'

        const supportedAttributes = await DatastoreHelper.fetchSupportedAttributes()
        const monitoredDevices = await DatastoreHelper.fetchMonitoredDevices()
        const data = await DatastoreHelper.fetchAttributeData(this.config.attr, this.config.devs, this.config.precision)
        const colors = ColorHelper.colors()
        //this.nodata = data.attr1.length == 0

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
                const color = ColorHelper.chartColors[idx]
                datasets[idx].fill = 'start'
                datasets[idx].borderColor = color
                datasets[idx].backgroundColor = `${color}44`
            }
        }

        const attrLabel = ChartHelper.prettyName(this.config.attr)
        this.chart.options.scales.y = {
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
        if (this.scale === 'fixed') {
            if (this.attrMin !== undefined) this.chart.options.scales.y.suggestedMin = this.attrMin
            if (this.attrMax !== undefined) this.chart.options.scales.y.suggestedMax = this.attrMax
        }

        this.chart.data = { datasets }
        ChartHelper.updateChartType(this.chart)
        this.chart.update('none')
        setTimeout(() => this.classList.remove('empty', 'spinner'), 200)
    }

    firstUpdated() {
        this.chart = new Chart(this.renderRoot.querySelector('canvas'), ChartHelper.defaultConfig(this.mobileView))
        this.chart.canvas.style.touchAction = 'pan-y'
    }

    async changePrecision(event) {
        this.config.precision = event.detail
        await this.refresh()
        this.chart.resetZoom()
    }

    async refresh() {
        this.classList.add('spinner')
        const data = await DatastoreHelper.fetchAttributeData(this.config.attr, this.config.devs, this.config.precision)
        //this.nodata = data.attr1.length == 0
        this.chart.data.datasets.forEach(dataset => dataset.data = data[dataset.ref])
        this.chart.update('none')
        ChartHelper.updateChartType(this.chart)
        this.classList.remove('spinner')
    }

    decorateConfig(config) {
        return { ...config, ...this.config }
    }

    setYScale(scale) {
        this.scale = scale
        if (!this.chart) return
        if (scale == 'auto') {
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
        devices: { type: Object, state: true },
        attributes: { type: Object, state: true },

        attr: { type: String, state: true },
        devs: { type: Object, state: true },
    }

    constructor() {
        super()
        this.devices = undefined
        this.attributes = undefined

        this.attr = undefined
        this.devs = []
    }

    render() {
        return html`
            <label for="device">Select attribute to chart:</label>
            ${this.attributes ? this.renderAttributesSelect() : html`<aside class="spinner">Loading devices ...</aside>`}
            ${this.attr ? this.renderDevicesSelect() : nothing }
        `
    }

    connectedCallback() {
        super.connectedCallback()
        DatastoreHelper.fetchMonitoredDevices().then(devices => {
            this.devices = devices

            const attrs = new Set()
            devices.forEach(device => device.attrs.forEach(attr => attrs.add(attr)))
            this.attributes = [...attrs].sort()
        })
    }

    createRenderRoot() {
        return this
    }

    renderAttributesSelect() {
        setTimeout(() => this.renderRoot.querySelector('#attr').focus(), 0)
        return html`
            <section>
                <select id="attr" .value=${this.attr} @change=${this.onAttributeSelect} required="true">
                    <option value=""></option>
                    ${this.attributes.map(attribute => html`
                        <option value="${attribute}" .selected=${this.attr === attribute}>${attribute}</option>
                    `
                    )}
                </select>
            </section>
        `
    }

    renderDevicesSelect() {
        const devices = this.devices.filter(device => device.attrs.includes(this.attr))
        return html`
            <section>
                <label>Select devices (at least one):</label>
                ${devices.map(device => {
                    return html`<label><input value="${device.id}" type="checkbox"
                        required=${this.devs.length == 0 ? 'yes' : nothing}
                        @change=${this.onDeviceSelect}
                    > ${device.name}</label>`
                })}
            </section>
        `
    }

    onAttributeSelect(event) {
        this.attr = event.target.value !== '' ? event.target.value : undefined
        this.devs = []
        if (this.attr) this.dispatchEvent(new CustomEvent('suggestTitle', { detail: ChartHelper.prettyName(this.attr) }))
    }

    onDeviceSelect() {
        this.devs = [...this.renderRoot.querySelectorAll('input[type="checkbox"]:checked')].map(input => input.value)
    }

    decorateConfig(config) {
        return { ...config, attr: this.attr, devs: this.devs }
    }
}
