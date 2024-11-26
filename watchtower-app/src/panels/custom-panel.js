import { html, css, LitElement, nothing } from '../vendor/vendor.min.js';

import { DatastoreHelper } from '../helpers/datastore-helper.js';
import { ColorHelper } from '../helpers/color-helper.js'
import { ChartHelper } from '../helpers/chart-helper.js'

export class CustomPanel extends LitElement {
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
        this.classList.add('spinner')
        const supportedAttributes = await DatastoreHelper.fetchSupportedAttributes()
        const monitoredDevices = await DatastoreHelper.fetchMonitoredDevices()
        const colors = ColorHelper.colors()

        const dbResult = await DatastoreHelper.fetchCustomData(this.config)
        this.nodata = Object.entries(dbResult).find(entry => entry[1]?.length > 0) === undefined
        if (this.nodata) {
            if (this.chart !== undefined) this.chart.destroy()
            setTimeout(() => this.classList.remove('empty', 'spinner'), 200)
            return
        }

        const $config = ChartHelper.lineConfig()
        const datasets = []
        const len = this.config.ds.length
        let idx = 1
        this.config.ds.forEach(({dev, attr}) => {
            const devName = monitoredDevices.find(monitoredDevice => monitoredDevice.id == dev).name
            const {unit, min, max} = supportedAttributes[attr]
            const attrName = ChartHelper.prettyName(attr)
            const attrLabel = `${devName} ${attrName}`

            datasets.push({
                label: attrLabel,
                data: dbResult[`${dev}_${attr}`],
                pointStyle: false,
                borderWidth: 1.2,
                tension: 0.5,
                fill: len < 3,
                yAxisID: `y${idx}`,
                unit,
            })

            $config.options.scales[`y${idx}`] = {
                position: idx - 1 < len / 2 ? 'left' : 'right',
                display: true,
                title: {
                    display: true,
                    text: `${attrLabel} ${unit}`,
                },
                ticks: { color: colors.TextColorDarker, precision: 0 },
                grid: { color: colors.TextColorDarker + '33' },
            }
            if (this.yScale === 'fixed') {
                if (min !== undefined) $config.options.scales[`y${idx}`].suggestedMin = min
                if (max !== undefined) $config.options.scales[`y${idx}`].suggestedMax = max
            }
            idx++
        })

        $config.data = { datasets }

        // Apply user script
        ChartHelper.executeUserScript(this.config.uscript, $config)

        if (this.chart !== undefined) this.chart.destroy()
        this.chart = new Chart(this.renderRoot.querySelector('canvas'), $config)
        this.chart.precision = this.config.precision
        setTimeout(() => this.classList.remove('empty', 'spinner'), 200)
    }

    async changePrecision(event) {
        this.chart?.crosshair.resetZoom()
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
}

export class CustomPanelConfig extends LitElement {
    static properties = {
        config: { type: Object, reflect: true },
        devices: { type: Object, state: true },
        supportedAttributes: { type: Object, state: true },
        attributes: { type: Object, state: true },
        dev: { type: Number, state: true },
        add: { type: Boolean, state: true },
    }

    constructor() {
        super()
        this.config = { ds: [] }
        this.devices = undefined
        this.supportedAttributes = undefined
        this.attributes = undefined
        this.dev = undefined
        this.add = true
    }

    render() {
        return html`
            ${(this.devices && this.config.ds?.length) ? this.renderDatasets() : ''}
            <fieldset>
                <section>
                    ${this.add ? html`
                        <label for="dev">Select device:</label>
                        ${(this.devices && this.supportedAttributes) ? this.renderDevicesSelect() : html`<aside class="spinner">Loading devices ...</aside>`}
                        ${this.attributes ? this.renderAttributesSelect() : '' }
                    ` : html`
                        <button @click=${this.addAnother}>Add another device attribute</button>
                    `}
                </section>
            </fieldset>
        `
    }

    async connectedCallback() {
        super.connectedCallback()
        this.devices = await DatastoreHelper.fetchMonitoredDevices()
        this.supportedAttributes = await DatastoreHelper.fetchSupportedAttributes()

        if (this.config.ds === undefined) this.config = { ...this.config, ds: []}
        this.add = !this.config.ds.length
    }

    createRenderRoot() {
        return this
    }

    renderDatasets() {
        return this.config.ds.map((dataset, idx) => html`
            <fieldset>
                <section>
                    ${this.devices.find(it => it.id == dataset.dev).name}: ${dataset.attr}
                    <nav>
                        ${idx > 0 ? html`<a href="javascript:" @click=${() => this.moveUp(idx)} title="Move up">▲</a> | ` : ''}
                        ${idx < this.config.ds.length - 1 ? html`<a href="javascript:" @click=${() => this.moveDown(idx)} title="Move down">▼</a> | ` : ''}
                        <a href="javascript:" @click=${() => this.remove(idx)} title="Remove">✖</a>
                    </nav>
                </section>
            </fieldset>
        `)
    }

    renderDevicesSelect() {
        return html`
            <section>
                <select id="dev" @change=${this.onDeviceSelect} .required=${!this.config.ds?.length}>
                    <option></option>
                    ${this.devices.map(device => html`
                        <option value="${device.id}">${device.name}</option>
                    `
                    )}
                </select>
            </section>
        `
    }

    renderAttributesSelect() {
        return html`
            <section>
                <label for="attr">Select attribute to chart:</label>
                <select id="attr" @change=${this.onAttributeSelect} .required=${!this.config.ds?.length}>
                    <option value=""></option>
                    ${this.attributes.map(attr => html`<option value="${attr}" .disabled=${this.isDisabled(attr)}>${attr}</option>`)}
                </select>
            </section>
        `
    }

    addAnother() {
        this.add = true
        setTimeout(() => {
            const elm = this.renderRoot.querySelector('#dev')
            elm.scrollIntoView({behavior: 'smooth', block: 'center'});
            elm.focus({preventScroll: true});
        }, 0)
    }

    isDisabled(attr) {
        return this.config.ds.find(it => it.dev == this.dev && it.attr == attr) !== undefined
    }

    moveUp(idx) {
        const ds = this.config.ds
        const tmp = ds[idx - 1]
        ds[idx - 1] = ds[idx]
        ds[idx] = tmp
        this.config = { ...this.config, ds }
    }

    moveDown(idx) {
        const ds = this.config.ds
        const tmp = ds[idx + 1]
        ds[idx + 1] = ds[idx]
        ds[idx] = tmp
        this.config = { ...this.config, ds }
    }

    remove(index) {
        this.config = { ...this.config,
            ds: this.config.ds.filter((_, idx) => idx !== index)
        }
        if (!this.config.ds?.length) this.addAnother()
    }

    onDeviceSelect(event) {
        this.dev = event.target.value !== '' ? event.target.value : undefined
        if (this.dev === undefined) {
            this.attributes = undefined
            return
        }

        const device = this.devices.find(device => device.id == this.dev)
        this.attributes = device.attrs.sort()
        setTimeout(() => {
            const elm = this.renderRoot.querySelector('#attr')
            elm.scrollIntoView({behavior: 'smooth', block: 'center'});
            elm.focus({preventScroll: true});
        }, 0)
    }

    onAttributeSelect(event) {
        const attr = event.target.value !== '' ? event.target.value : undefined
        if (attr === undefined) return

        const ds = this.config.ds || []
        this.config = { ...this.config,
            ds: this.config.ds.concat({
                dev: parseInt(this.dev),
                attr,
            })
        }

        // Reset add inputs
        this.dev = undefined
        this.attributes = undefined
        this.add = false
    }

    decorateConfig(config) {
        return {
            ...config,
            precision: '5m',
            ds: this.config.ds,
        }
    }
}
