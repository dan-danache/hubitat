import { html, css, LitElement } from '../vendor/vendor.min.js';

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
        :host(:hover) precision-selector,
        :host(:hover) nav {
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
        .reset-zoom {
            position: absolute;
            top: 0px;
            right: 0px;
            border: none;
            background-color: var(--Blue);
            color: var(--Base3);
            border-radius: 0 0 0 5px;
            padding: 5px 10px;
            line-height: 1rem;
            z-index: 100;
            cursor: pointer;
            letter-spacing: 1px;
        }
        aside {
            position: absolute;
            top: 0;
            right: 0;
        }
        nav {
            position: absolute;
            top: 0px; left: 2px;
            cursor: pointer;
            visibility: hidden;
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
            <nav title="Edit tile" @click=${this.editPanel}>⚙️</nav>
            ${ this.nodata === true ? html`<aside>No data yet</aside>` : '' }
        `;
    }

    updated(changedProperties) {
        if (changedProperties.mobileView == this.mobileView) return
    }

    async connectedCallback() {
        super.connectedCallback()
        if (this.config.precision === undefined) this.config.precision = '5m'
    }

    async firstUpdated() {
        const canvas = this.renderRoot.querySelector('canvas')
        this.chart = new Chart(canvas, ChartHelper.defaultConfig(this.mobileView))
        this.chart.canvas.style.touchAction = 'pan-y'
        ChartHelper.setupZoomPan(canvas, this.chart)
        await this.initChart()
    }

    editPanel() {
        this.dispatchEvent(new CustomEvent('edit', { bubbles: true, detail: this.config }))
    }

    async initChart() {
        this.classList.add('spinner')
        const supportedAttributes = await DatastoreHelper.fetchSupportedAttributes()
        const monitoredDevices = await DatastoreHelper.fetchMonitoredDevices()
        const data = await DatastoreHelper.fetchAttributeData(this.config)
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

        this.chart.precision = this.config.precision
        this.chart.data = { datasets }
        this.chart.update('none')
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

    render() {
        return html`
            <label for="device">Select attribute to chart:</label>
            ${this.attributes ? this.renderAttributesSelect() : html`<aside class="spinner">Loading devices ...</aside>`}
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

    createRenderRoot() {
        return this
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
                        <label><input type="checkbox"
                            .checked="${this.config.z}"
                            @change=${ event => this.config.z = event.target.checked }
                        > Render zero for missing values</label>
                    </div>
                ` : ''}
            </section>
        `
    }

    renderDevicesSelect() {
        const devices = this.devices.filter(device => device.attrs.includes(this.config.attr))
        return html`
            <section id="devlist">
                <label>Select devices (at least one):</label>
                ${devices.map(device => {
                    return html`<label><input value="${device.id}" type="checkbox"
                        .checked="${this.config.devs.find(dev => dev == device.id)}"
                        ?required=${this.config.devs.length == 0}
                        @change=${this.onDeviceSelect}
                    > ${device.name}</label>`
                })}
            </section>
        `
    }

    onAttributeSelect(event) {
        const attr = event.target.value !== '' ? event.target.value : undefined
        this.config = { ...this.config,
            attr,
            z: false,
            devs: [],
        }
        if (attr) this.dispatchEvent(new CustomEvent('suggestTitle', { detail: ChartHelper.prettyName(attr) }))
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
