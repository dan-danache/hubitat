import { html, css, LitElement } from '../vendor/vendor.min.js';

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
            touch-action: pan-y;
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
        nav {
            position: absolute;
            top: 0px; left: 2px;
            cursor: pointer;
            visibility: hidden;
        }
    `;

    static properties = {
        config: { type: Object, reflect: true },
        yScale: { type: String, reflect: true },
        mobileView: { type: Boolean, state: true },
        chart: { type: Object, state: true },
        nodata: { type: Boolean, state: true },
    }

    render() {
        return html`
            <canvas tabindex='1' @keydown=${this.onKeyDown}></canvas>
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
        await this.initChart()
    }

    onKeyDown(event) {
        if (this.chart === undefined) return
        if (event.keyCode === 37) this.chart.crosshair.panZoom('left')
        else if(event.keyCode === 39) this.chart.crosshair.panZoom('right')
    }

    async initChart() {
        const chartConfig = ChartHelper.lineConfig()

        this.classList.add('spinner')
        const supportedAttributes = await DatastoreHelper.fetchSupportedAttributes()
        const colors = ColorHelper.colors()

        const data = await DatastoreHelper.fetchDeviceData(this.config)
        this.nodata = data.attr1.length == 0

        const attr1Label = ChartHelper.prettyName(this.config.attr1)
        const attr1Unit = supportedAttributes[this.config.attr1].unit
        const datasets = [{
            label: attr1Label,
            data: data.attr1,
            pointStyle: false,
            backgroundColor: colors.Blue + '44',
            borderColor: colors.Blue,
            borderWidth: 1.2,
            tension: 0.5,
            fill: this.config.mm1 !== true && this.config.mm2 !== true,
            yAxisID: 'attr1',
            unit: attr1Unit,
        }]

        if (this.config.mm1) {
            datasets.push({
                label: `${attr1Label} min`,
                data: data.min1,
                pointStyle: false,
                backgroundColor: colors.Blue + '22',
                borderColor: colors.Blue,
                borderWidth: 1.2,
                borderDash: [2, 2],
                tension: 0.5,
                fill: '+1',
                yAxisID: 'attr1',
                unit: attr1Unit,
            })
            datasets.push({
                label: `${attr1Label} max`,
                data: data.max1,
                pointStyle: false,
                borderColor: colors.Blue,
                backgroundColor: colors.Blue + '22',
                borderWidth: 1.2,
                borderDash: [2, 2],
                tension: 0.5,
                fill: false,
                yAxisID: 'attr1',
                unit: attr1Unit,
            })
        }

        chartConfig.options.scales.attr1 = {
            position: 'left',
            display: true,
            title: {
                display: true,
                text: `${attr1Label} ${attr1Unit}`,
                color: colors.Blue
            },
            ticks: { color: colors.TextColorDarker, precision: 0 },
            grid: { color: colors.TextColorDarker + '33' },
        }
        this.attr1Min = supportedAttributes[this.config.attr1].min
        this.attr1Max = supportedAttributes[this.config.attr1].max
        if (this.yScale === 'fixed') {
            if (this.attr1Min !== undefined) chartConfig.options.scales.attr1.suggestedMin = this.attr1Min
            if (this.attr1Max !== undefined) chartConfig.options.scales.attr1.suggestedMax = this.attr1Max
        }

        if (this.config.attr2 !== undefined) {
            const attr2Label = ChartHelper.prettyName(this.config.attr2)
            const attr2Unit = supportedAttributes[this.config.attr2].unit
            datasets.push({
                label: attr2Label,
                data: data.attr2,
                pointStyle: false,
                backgroundColor: colors.Green + '44',
                borderColor: colors.Green,
                borderWidth: 1.2,
                tension: 0.5,
                fill: this.config.mm1 !== true && this.config.mm2 !== true,
                yAxisID: 'attr2',
                unit: attr2Unit,
            })
            if (this.config.mm2) {
                datasets.push({
                    label: `${attr2Label} min`,
                    data: data.min2,
                    pointStyle: false,
                    backgroundColor: colors.Green + '22',
                    borderColor: colors.Green,
                    borderWidth: 1.2,
                    borderDash: [2, 2],
                    tension: 0.5,
                    fill: '+1',
                    yAxisID: 'attr2',
                    unit: attr2Unit,
                })
                datasets.push({
                    label: `${attr2Label} max`,
                    data: data.max2,
                    pointStyle: false,
                    borderColor: colors.Green,
                    backgroundColor: colors.Green + '22',
                    borderWidth: 1.2,
                    borderDash: [2, 2],
                    tension: 0.5,
                    fill: false,
                    yAxisID: 'attr2',
                    unit: attr2Unit,
                })
            }

            chartConfig.options.scales.attr2 = {
                position: 'right',
                display: true,
                title: {
                    display: true,
                    text:  `${attr2Label} ${attr2Unit}`,
                    color: colors.Green
                },
                ticks: { color: colors.TextColorDarker, precision: 0 },
                grid: { drawOnChartArea: false },
            }
            this.attr2Min = supportedAttributes[this.config.attr2].min
            this.attr2Max = supportedAttributes[this.config.attr2].max
            if (this.yScale === 'fixed') {
                if (this.attr2Min !== undefined) chartConfig.options.scales.attr2.suggestedMin = this.attr2Min
                if (this.attr2Max !== undefined) chartConfig.options.scales.attr2.suggestedMax = this.attr2Max
            }
        }

        chartConfig.data = { datasets }

        if (this.chart !== undefined) this.chart.destroy()
        this.chart = new Chart(this.renderRoot.querySelector('canvas'), chartConfig)
        this.chart.precision = this.config.precision
        ChartHelper.updateChartType(this.chart)
        setTimeout(() => this.classList.remove('empty', 'spinner'), 200)
    }

    async changePrecision(event) {
        this.chart.crosshair.resetZoom()
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
        config: { type: Object, reflect: true },
        devices: { type: Object, state: true },
        supportedAttributes: { type: Object, state: true },
        attributes: { type: Object, state: true },
    }

    constructor() {
        super()
        this.config = {}
        this.devices = undefined
        this.supportedAttributes = undefined
        this.attributes = undefined
    }

    render() {
        return html`
            <fieldset>
                <section>
                    <label for="device">Select device:</label>
                    ${this.devices && this.supportedAttributes ? this.renderDevicesSelect() : html`<aside class="spinner">Loading devices ...</aside>`}
                    ${this.attributes ? this.renderAttributesSelect() : '' }
                </section>
            </fieldset>
            ${ this.config.attr1 !== undefined && this.attributes.length > 1 ? this.renderOptionalAttributesSelect() : '' }
        `
    }

    async connectedCallback() {
        super.connectedCallback()
        await DatastoreHelper.fetchMonitoredDevices().then(devices => { this.devices = devices })
        await DatastoreHelper.fetchSupportedAttributes().then(attributes => { this.supportedAttributes = attributes })
        if (this.config.dev === undefined) return

        // Pre-load attributes
        const device = this.devices.find(device => device.id == this.config.dev)
        this.attributes = device.attrs.sort()
    }

    createRenderRoot() {
        return this
    }

    renderDevicesSelect() {
        return html`
            <section>
                <select id="device" @change=${this.onDeviceSelect} required="true">
                    <option></option>
                    ${this.devices.map(device => html`
                        <option value="${device.id}" .selected=${this.config.dev == device.id}>${device.name}</option>
                    `
                    )}
                </select>
            </section>
        `
    }

    renderAttributesSelect() {
        return html`
            <section>
                <label for="attr1">Select attribute to chart:</label>
                <select id="attr1" .value=${this.config.attr1} @change=${this.onAttribute1Select} required="true">
                    <option value=""></option>
                    ${this.attributes.filter(attribute => attribute != this.config.attr2).map(attribute => html`
                        <option value="${attribute}" .selected=${this.config.attr1 === attribute}>${attribute}</option>
                    `
                    )}
                </select>
                ${ this.config.attr1 ? html`
                    <div>
                        ${ this.supportedAttributes[this.config.attr1].minMax == true ? html`
                            <div class="checkbox">
                                <input type="checkbox" id="mm1"
                                    .checked="${this.config.mm1}"
                                    @change=${ event => this.config.mm1 = event.target.checked }
                                >
                                <label for="mm1">Chart ${this.config.attr1} min/max</label>
                            </div>
                         ` : ''}
                        <div class="checkbox">
                            <input type="checkbox" id="z1"
                                .checked="${this.config.z1}"
                                @change=${ event => this.config.z1 = event.target.checked }
                            >
                            <label for="z1">Render zero for missing values</label>
                        </div>
                    </div>
                ` : ''}
            </section>
        `
    }

    renderOptionalAttributesSelect() {
        return html`
            <fieldset>
                <section>
                    <label for="attr2">Select additional attribute:</label>
                    <select id="attr2" .value=${this.config.attr2} @change=${this.onAttribute2Select}>
                        <option value="">[optional]</option>
                        ${this.attributes.filter(attribute => attribute != this.config.attr1).map(attribute => html`
                            <option value="${attribute}" .selected=${this.config.attr2 === attribute}>${attribute}</option>
                        `
                        )}
                    </select>
                    ${ this.config.attr2 ? html`
                        <div>
                            ${ this.supportedAttributes[this.config.attr2].minMax == true ? html`
                                <div class="checkbox">
                                    <input type="checkbox" id="mm2"
                                        .checked="${this.config.mm2}"
                                        @change=${ event => this.config.mm2 = event.target.checked }
                                    >
                                    <label for="mm2">Chart ${this.config.attr2} min/max</label>
                                </div>
                            ` : ''}
                            <div class="checkbox">
                                <input type="checkbox" id="z2"
                                    .checked="${this.config.z2}"
                                    @change=${ event => this.config.z2 = event.target.checked }
                                >
                                <label for="z2">Render zero for missing values</label>
                            </div>
                        </div>
                    ` : ''}
                </section>
            </fieldset>
        `
    }

    onDeviceSelect(event) {
        const dev = event.target.value !== '' ? event.target.value : undefined
        if (dev == this.config.dev) return

        this.config = { ...this.config,
            dev,
            attr1: undefined,
            attr2: undefined,
            mm1: false,
            mm2: false,
            z1: false,
            z2: false
        }

        if (this.config.dev === undefined) {
            this.attributes = undefined
            return
        }

        const device = this.devices.find(device => device.id == dev)
        this.attributes = device.attrs.sort()
        this.dispatchEvent(new CustomEvent('suggestTitle', { detail: device.name }))

        setTimeout(() => {
            const elm = this.renderRoot.querySelector('#attr1')
            elm.scrollIntoView({behavior: 'smooth', block: 'center'});
            elm.focus({preventScroll: true});
        }, 0)
    }

    onAttribute1Select(event) {
        const attr1 = event.target.value !== '' ? event.target.value : undefined
        if (attr1 == this.config.attr1) return
        this.config = { ...this.config,
            attr1,
            mm1: false,
            z1: false,
        }
        setTimeout(() => {
            const elm = this.renderRoot.querySelector('#attr1')
            elm.scrollIntoView({behavior: 'smooth', block: 'center'});
            elm.focus({preventScroll: true});
        }, 0)
    }

    onAttribute2Select(event) {
        const attr2 = event.target.value !== '' ? event.target.value : undefined
        if (attr2 == this.config.attr2) return
        this.config = { ...this.config,
            attr2,
            mm2: false,
            z2: false,
        }
        setTimeout(() => {
            const elm = this.renderRoot.querySelector('#attr2')
            elm.scrollIntoView({behavior: 'smooth', block: 'center'});
            elm.focus({preventScroll: true});
        }, 0)
    }

    decorateConfig(config) {
        return {
            ...config,
            dev: parseInt(this.config.dev),
            precision: '5m',
            attr1: this.config.attr1,
            attr2: this.config.attr2,
            mm1: this.config.mm1 === true ? true : undefined,
            mm2: this.config.mm2 === true ? true : undefined,
            z1: this.config.z1 === true ? true : undefined,
            z2: this.config.z2 === true ? true : undefined,
        }
    }
}
