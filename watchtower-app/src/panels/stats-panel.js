import { html, LitElement, nothing } from '../vendor/vendor.min.js';

import { AttributeListMixin } from '../mixins/attribute-list-mixin.js';
import { DatastoreHelper } from '../helpers/datastore-helper.js';
import { UiHelper } from '../helpers/ui-helper.js'
import { ChartHelper } from '../helpers/chart-helper.js'

const BG_COLORS = [ 'Yellow', 'Orange', 'Red', 'Magenta', 'Violet' , 'Blue', 'Cyan', 'Green' ]

export class StatsPanel extends LitElement {
    static properties = {
        monitoredDevices: { type: Object, state: true },
        supportedAttributes: { type: Object, state: true },
        config: { type: Object, reflect: true },
        dbResult: { type: Object, state: true },
    }

    constructor() {
        super()
        this.charts = {}
    }

    createRenderRoot() {
        return this
    }

    render() {
        const ready = this.monitoredDevices && this.supportedAttributes && this.dbResult
        return html`
            <ul class=${this.config.bg ? `stats-container has-bg bg-${this.config.bg}` : 'stats-container'}>
                ${this.config.ds.map(dataset => html`
                    <li>
                        <article>
                            ${ready ? html`
                                <header>${this.monitoredDevices.find(it => it.id == dataset.dev).name}</header>
                                <div>
                                    <!--small>${UiHelper.prettyName(dataset.attr)}</small-->
                                    <b>${this.dbResult[`${dataset.dev}_${dataset.attr}`].pop().y}<span>${this.supportedAttributes[dataset.attr].unit}</span></b>
                                </div>
                            ` : nothing}
                        </article>
                        <output>
                            ${this.config.rk ? html`<canvas id="c${dataset.dev}_${dataset.attr}"></canvas>` : nothing}
                        </output>
                    </li>
                `)}
            </ul>
            <precision-selector @change=${this.changePrecision} .precision=${this.config.precision}></precision-selector>
            <nav title="Edit tile" @click=${this.editPanel}>⚙️</nav>
        `;
    }

    async connectedCallback() {
        super.connectedCallback()
        this.monitoredDevices = await DatastoreHelper.fetchMonitoredDevices()
        this.supportedAttributes = await DatastoreHelper.fetchSupportedAttributes()
    }

    async firstUpdated() {
        await this.initCharts()
    }

    async initCharts() {
        this.classList.add('spinner')
        this.dbResult = await DatastoreHelper.fetchCustomData(this.config, true)
        this.config.ds.forEach(dataset => this.initChart(dataset))
        setTimeout(() => this.classList.remove('empty', 'spinner'), 200)
    }

    initChart({dev, attr}) {
        const data = this.dbResult[`${dev}_${attr}`]
        if (!this.config.rk) return

        // Load sparkline chart
        const $config = ChartHelper.sparklineConfig()
        const colors = UiHelper.colors()

        const color = this.config.bg ? '#FFFFFF' : (this.config.fg ? colors[this.config.fg] : colors.TextColorDarker)
        const datasets = [{
            data,
            pointStyle: false,
            borderWidth: 1.2,
            tension: 0.5,
            borderColor: color + '88',
            backgroundColor: color + '44',
            fill: true,
        }]

        // const min = supportedAttributes[this.config.attr].min
        // const max = supportedAttributes[this.config.attr].max
        // if (min !== undefined) $config.options.scales.y.suggestedMin = min
        // if (max !== undefined) $config.options.scales.y.suggestedMax = max

        $config.data = { datasets }

        if (this.charts[`${dev}_${attr}`] !== undefined) this.charts[`${dev}_${attr}`].destroy()
        this.charts[`${dev}_${attr}`] = new Chart(this.renderRoot.querySelector(`#c${dev}_${attr}`), $config)
    }

    async changePrecision(event) {
        this.config.precision = event.detail
        await this.refresh()
    }

    async refresh() {
        await this.initCharts()
    }

    editPanel() {
        this.dispatchEvent(new CustomEvent('edit', { bubbles: true, detail: this.config }))
    }

    decorateConfig(config) {
        return { ...config, ...this.config }
    }
}

export class StatsPanelConfig extends AttributeListMixin(LitElement) {
    static properties = {
        config: { type: Object, reflect: true },
        setBgColor: { type: Boolean, state: true },
        setFgColor: { type: Boolean, state: true },
    }

    constructor() {
        super()
        this.config = { }
        this.setBgColor = false
        this.setBgColor = false
    }

    createRenderRoot() {
        return this
    }

    render() {
        return html`
            ${super.render()}
            ${this.renderColorSettings()}
        `
    }

    async connectedCallback() {
        super.connectedCallback()
        
        if (this.config.bg) this.setBgColor = true
        else if (this.config.fg) this.setFgColor = true
    }

    renderColorSettings() {
        return html`
            <fieldset>
                <section>
                    <div class="checkbox">
                        <input type="checkbox" id="sbgc"
                            .checked="${this.setBgColor}"
                            @change=${this.onSetBgColor}
                        >
                        <label for="sbgc">Custom background color</label>
                    </div>
                </section>
                ${this.setBgColor ? html`
                    <section>
                        <label for="bg">Select tile background color:</label>
                        <select id="bg" .value=${this.config.attr} @change=${this.onBgColorSelect} required="true">
                            <option value=""></option>
                            ${BG_COLORS.map(color => html`
                                <option value="${color}" .selected=${this.config.bg === color}>${color}</option>
                            `
                            )}
                        </select>
                    </section>
                ` : nothing}

                <section>
                    <div class="checkbox">
                        <input type="checkbox" id="rk"
                            .checked="${this.config.rk}"
                            @change=${this.onSetSparkline}
                        >
                        <label for="rk">Render sparkline (last 10 values)</label>
                    </div>
                </section>

                ${this.config.rk && !this.setBgColor ? html`
                    <section>
                        <div class="checkbox">
                            <input type="checkbox" id="sfgc"
                                .checked="${this.setFgColor}"
                                @change=${this.onSetFgColor}
                            >
                            <label id="sfgcl" for="sfgc">Custom sparkline color</label>
                        </div>
                    </section>
                    ${this.setFgColor ? html`
                        <section>
                            <label for="fg">Select sparkline color:</label>
                            <select id="fg" .value=${this.config.attr} @change=${this.onFgColorSelect} required="true">
                                <option value=""></option>
                                ${BG_COLORS.map(color => html`
                                    <option value="${color}" .selected=${this.config.fg === color}>${color}</option>
                                `
                                )}
                            </select>
                        </section>
                    ` : nothing}
                ` : nothing}
            </fieldset>
        `
    }

    onSetBgColor(event) {
        this.setBgColor = event.target.checked
        if (!this.setBgColor) {
            this.updateConfig({bg: undefined})
        } else {
            this.setFgColor = false
            this.updateConfig({fg: undefined})
        }
        this.focus('#bg')
    }

    onBgColorSelect(event) {
        this.updateConfig({
            bg: event.target.value,
            fg: undefined,
        })
    }

    onSetSparkline(event) {
        this.updateConfig({rk: event.target.checked})
        if (!this.config.rk) this.updateConfig({fg: undefined})
        this.focus('#sfgcl')
    }

    onSetFgColor(event) {
        this.setFgColor = event.target.checked
        if (!this.setFgColor) this.updateConfig({fg: undefined})
        this.focus('#fg')
    }

    onFgColorSelect(event) {
        this.updateConfig({fg: event.target.value})
    }

    decorateConfig(config) {
        return super.decorateConfig({ ...config,
            precision: '5m',
            ds: this.config.ds,
            bg: this.config.bg,
            rk: this.config.rk ? true : undefined,
            fg: this.config.fg,
        })
    }
}
