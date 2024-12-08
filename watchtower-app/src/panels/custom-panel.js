import { html, LitElement, nothing } from '../vendor/vendor.min.js';

import { AttributeListMixin } from '../mixins/attribute-list-mixin.js';
import { DatastoreHelper } from '../helpers/datastore-helper.js';
import { UiHelper } from '../helpers/ui-helper.js'
import { ChartHelper } from '../helpers/chart-helper.js'

export class CustomPanel extends LitElement {
    static properties = {
        config: { type: Object, reflect: true },
        chart: { type: Object, state: true },
        nodata: { type: Boolean, state: true },
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
            const attrName = UiHelper.prettyName(attr)
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

export class CustomPanelConfig extends AttributeListMixin(LitElement) {
    static properties = {
        config: { type: Object, reflect: true },
    }

    createRenderRoot() {
        return this
    }

    render() {
        return super.render()
    }

    decorateConfig(config) {
        return super.decorateConfig(config)
    }
}
