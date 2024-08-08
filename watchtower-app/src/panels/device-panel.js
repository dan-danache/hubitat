import { html, css, LitElement, nothing } from '../vendor/vendor.min.js';

import { DatastoreHelper } from '../helpers/datastore-helper.js';
import { ColorHelper } from '../helpers/color-helper.js'
import { ChartHelper } from '../helpers/chart-helper.js'

export class DevicePanel extends LitElement {
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
    `;

    static properties = {
        config: { type: Object, reflect: true },
        mobileView: { type: Boolean, state: true },
        chart: { type: Object, state: true },
        nodata: { type: Boolean, state: true },
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
        const data = await DatastoreHelper.fetchDeviceData(this.config.dev, this.config.attr1, this.config.attr2, this.config.precision)
        const colors = ColorHelper.colors()
        this.nodata = data.attr1.length == 0

        const datasets = [{
            label: ChartHelper.prettyName(this.config.attr1),
            data: data.attr1,
            pointStyle: false,
            backgroundColor: colors.Green + '44',
            borderColor: colors.Green,
            borderWidth: 1.2,
            tension: 0.5,
            fill: 'start',
            yAxisID: 'attr1',
            unit: supportedAttributes[this.config.attr1].unit
        }]

        this.chart.options.scales.attr1 = {
            position: 'left',
            display: true,
            title: {
                display: true,
                text: `${datasets[0].label} ${supportedAttributes[this.config.attr1].unit}`,
                color: colors.Green
            },
            ticks: { color: colors.TextColorDarker },
            grid: { color: colors.TextColorDarker + '33' },
        }
        this.attr1Min = supportedAttributes[this.config.attr1].min
        this.attr1Max = supportedAttributes[this.config.attr1].max
        if (this.scale === 'fixed') {
            if (this.attr1Min !== undefined) this.chart.options.scales.attr1.suggestedMin = this.attr1Min
            if (this.attr1Max !== undefined) this.chart.options.scales.attr1.suggestedMax = this.attr1Max
        }

        if (this.config.attr2 !== undefined) {
            datasets.push({
                label: ChartHelper.prettyName(this.config.attr2),
                data: data.attr2,
                pointStyle: false,
                backgroundColor: colors.Blue + '44',
                borderColor: colors.Blue,
                borderWidth: 1.2,
                tension: 0.5,
                fill: 'start',
                yAxisID: 'attr2',
                unit: supportedAttributes[this.config.attr2].unit
            })

            this.chart.options.scales.attr2 = {
                position: 'right',
                display: true,
                title: {
                    display: true,
                    text: `${datasets[1].label} ${supportedAttributes[this.config.attr2].unit}`,
                    color: colors.Blue
                },
                ticks: { color: colors.TextColorDarker },
                grid: { drawOnChartArea: false },
            }
            this.attr2Min = supportedAttributes[this.config.attr2].min
            this.attr2Max = supportedAttributes[this.config.attr2].max
            if (this.scale === 'fixed') {
                if (this.attr2Min !== undefined) this.chart.options.scales.attr2.suggestedMin = this.attr2Min
                if (this.attr2Max !== undefined) this.chart.options.scales.attr2.suggestedMax = this.attr2Max
            }
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
        const data = await DatastoreHelper.fetchDeviceData(this.config.dev, this.config.attr1, this.config.attr2, this.config.precision)
        console.log('refresh data', data)
        this.nodata = data.attr1.length == 0
        this.chart.data.datasets[0].data = data.attr1
        if (this.config.attr2 !== undefined) this.chart.data.datasets[1].data = data.attr2
        this.chart.config.type = data.attr1.length < 10 ? 'bar' : 'line'
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
            delete this.chart.options.scales.attr1.suggestedMin
            delete this.chart.options.scales.attr1.suggestedMax
            if (this.config.attr2 !== undefined) {
                delete this.chart.options.scales.attr2.suggestedMin
                delete this.chart.options.scales.attr2.suggestedMax
            }
        } else {
            if (this.attr1Min !== undefined) this.chart.options.scales.attr1.suggestedMin = this.attr1Min
            if (this.attr1Max !== undefined) this.chart.options.scales.attr1.suggestedMax = this.attr1Max
            if (this.attr2Min !== undefined) this.chart.options.scales.attr2.suggestedMin = this.attr2Min
            if (this.attr2Max !== undefined) this.chart.options.scales.attr2.suggestedMax = this.attr2Max
        }
        this.chart.update()
    }
}

export class DevicePanelConfig extends LitElement {
    static properties = {
        devices: { type: Object, state: true },
        attributes: { type: Object, state: true },

        device: { type: String, state: true },
        attr1: { type: String, state: true },
        attr2: { type: String, state: true },
    }

    constructor() {
        super()
        this.devices = undefined
        this.attributes = undefined

        this.dev = undefined
        this.attr1 = undefined
        this.attr2 = undefined
    }

    render() {
        return html`
            <label for="device">Select device:</label>
            ${this.devices ? this.renderDevicesSelect() : html`<aside class="spinner">Loading devices ...</aside>`}
            ${this.attributes ? this.renderAttributesSelect() : nothing }
        `
    }

    connectedCallback() {
        super.connectedCallback()
        DatastoreHelper.fetchMonitoredDevices().then(devices => {
            this.devices = devices
        })
    }

    createRenderRoot() {
        return this
    }

    renderDevicesSelect() {
        setTimeout(() => this.renderRoot.querySelector('#device').focus(), 0)
        return html`
            <section>
                <select id="device" .value=${this.dev} @change=${this.onDeviceSelect} required="true">
                    <option value=""></option>
                    ${this.devices.map(device => html`
                        <option value="${device.id}" .selected=${this.dev === device.id}>${device.name}</option>
                    `
                    )}
                </select>
            </section>
        `
    }

    renderAttributesSelect() {
        setTimeout(() => this.renderRoot.querySelector('#attr1').focus(), 0)
        return html`
            <section>
                <label for="attr1">Select attribute to chart:</label>
                <select id="attr1" .value=${this.attr1} @change=${event => this.attr1 = event.target.value} required="true">
                    <option value=""></option>
                    ${this.attributes.filter(attribute => attribute != this.attr2).map(attribute => html`
                        <option value="${attribute}" .selected=${this.attr1 === attribute}>${attribute}</option>
                    `
                    )}
                </select>
            </section>
            ${ this.attr1 !== undefined && this.attributes.length > 1 ? this.renderOptionalAttributesSelect() : nothing }
        `
    }

    renderOptionalAttributesSelect() {
        setTimeout(() => this.renderRoot.querySelector('#attr2').focus(), 0)
        return html`
            <section>
                <label for="attr2">Select additional attribute:</label>
                <select id="attr2" .value=${this.attr2} @change=${event => this.attr2 = event.target.value}>
                    <option value="">[optional]</option>
                    ${this.attributes.filter(attribute => attribute != this.attr1).map(attribute => html`
                        <option value="${attribute}" .selected=${this.attr2 === attribute}>${attribute}</option>
                    `
                    )}
                </select>
            </section>
        `
    }

    onDeviceSelect(event) {
        this.dev = event.target.value !== '' ? event.target.value : undefined
        if (this.dev === undefined) {
            this.attributes = undefined
            return
        }

        const device = this.devices.find(device => device.id == this.dev)
        this.attributes = device.attrs.sort()
        this.dispatchEvent(new CustomEvent('suggestTitle', { detail: device.name }))
    }

    decorateConfig(config) {
        return { ...config, dev: this.dev, attr1: this.attr1, attr2: this.attr2, precision: '5m' }
    }
}
