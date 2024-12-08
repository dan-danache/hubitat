import { html, LitElement, nothing } from '../vendor/vendor.min.js';

import { AttributeListMixin } from '../mixins/attribute-list-mixin.js';
import { DatastoreHelper } from '../helpers/datastore-helper.js';
import { UiHelper } from '../helpers/ui-helper.js'
import { ChartHelper } from '../helpers/chart-helper.js'

export class StatusmapPanel extends LitElement {
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

        const dbResult = await DatastoreHelper.fetchStatusmapData(this.config)
        this.nodata = Object.entries(dbResult).find(entry => entry[1]?.length > 0) === undefined
        if (this.nodata) {
            if (this.chart !== undefined) this.chart.destroy()
            setTimeout(() => this.classList.remove('empty', 'spinner'), 200)
            return
        }

        const $config = ChartHelper.statusmapConfig()
        const labels = []
        const datasets = []
        let minDate = Infinity, maxDate = 0
        this.config.ds.forEach(({dev, attr}) => {
            const devName = monitoredDevices.find(monitoredDevice => monitoredDevice.id == dev).name
            const attrUnit = supportedAttributes[attr].unit
            const attrName = UiHelper.prettyName(attr)
            const label = `${devName} ${attrName}`
            labels.push(label)

            const data = dbResult[`${dev}_${attr}`].map(record => {return { ...record, y: label}})
            datasets.push({
                label: attrName,
                unit: attrUnit,
                backgroundColor: record => `rgba(133, 153, 0, ${Math.round(Math.min(100, Math.max(0, record.raw?.v || 0)) * 9) / 1000 + 0.1})`,
                borderColor: colors.Green,
                data: [...data],
            })
            minDate = Math.min(minDate, data[0].x[0])
            maxDate = Math.max(maxDate, data.pop().x[1])
        })

        $config.data = { labels, datasets }
        $config.options.scales.x.min = minDate
        $config.options.scales.x.max = maxDate

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

export class StatusmapPanelConfig extends AttributeListMixin(LitElement) {
    static properties = {
        config: { type: Object, reflect: true },
    }

    createRenderRoot() {
        return this
    }

    render() {
        return super.render()
    }

    canSelectAttribute(attr) {
        return this.supportedAttributes[attr].unit.startsWith('% ')
    }

    decorateConfig(config) {
        return super.decorateConfig(config)
    }
}
