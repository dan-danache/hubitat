import { html, LitElement, nothing } from '../vendor/vendor.min.js';

import { DatastoreHelper } from '../helpers/datastore-helper.js';
import { UiHelper } from '../helpers/ui-helper.js'
import { ChartHelper } from '../helpers/chart-helper.js'

export class ByodPanel extends LitElement {
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
            <nav title="Edit tile" @click=${this.editPanel}>⚙️</nav>
            ${ this.nodata === true ? html`<aside>No data yet</aside>` : nothing }
        `;
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
        const colors = UiHelper.colors()

        const dbResult = await DatastoreHelper.fetchBringYourOwnData(this.config)
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
        this.config.ds.forEach(({k, l, u}) => {
            datasets.push({
                label: l,
                data: dbResult[`${k}`],
                pointStyle: false,
                borderWidth: 1.2,
                tension: 0.5,
                fill: len < 3,
                yAxisID: `y${idx}`,
                unit: u,
            })

            $config.options.scales[`y${idx}`] = {
                position: idx - 1 < len / 2 ? 'left' : 'right',
                display: true,
                title: {
                    display: true,
                    text: `${l} ${u}`,
                },
                ticks: { color: colors.TextColorDarker, precision: 0 },
                grid: { color: colors.TextColorDarker + '33' },
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

export class ByodPanelConfig extends LitElement {
    static properties = {
        config: { type: Object, reflect: true },
        add: { type: Boolean, state: true },

        file: { type: String, state: true },
        fileFeedback: { type: String, state: true },
        fileDetails: { type: Object, state: true },

        label: { type: String, state: true },
        key: { type: String, state: true },
        unit: { type: String, state: true },
    }

    constructor() {
        super()
        this.add = false

        this.file = undefined
        this.fileFeedback = undefined
        this.fileDetails = undefined

        this.label = undefined
        this.key = undefined
        this.unit = undefined

        this.grid = undefined
    }

    createRenderRoot() {
        return this
    }

    render() {
        return html`
            <fieldset>
                <section>
                    <label for="file">Enter file name (as listed in the <a href="/hub/fileManager" target="_blank">File Manager</a>):</label>
                    <input type="text" id="file" .value=${this.file || ''} autocomplete="off" required
                        @change=${this.onFileChange}
                    >
                    ${this.fileFeedback ? html`<p id="file-feedback">${this.fileFeedback}</p>` : nothing}
                </section>
                ${!this.config.file ? html`
                    <section>
                        <button type="button" @click=${this.onCheckFile}>Check file</button>
                    </section>
                ` : html`
                    <section>
                        <label for="ts">Select ${this.config.fmt === 'json' ? 'JSON key name' : 'CSV column'} that contains the timestamp:</label>
                        <select id="ts" required
                            @change=${this.onTimestampSelect}
                        >
                            <option value=""></option>
                            ${this.fileDetails?.ts?.map(key => html`
                                <option value="${key}" .selected=${`${key}` === `${this.config?.ts}`}>${key}</option>
                            `)}
                        </select>
                    </section>
                `}
            </fieldset>
            <fieldset class="${!this.config.ds?.length ? 'empty' : nothing}">
                <section>Metrics:</section>
                <section>
                    <div class="grid-stack"
                        @item-remove=${this.onItemRemove}
                    ></div>
                </section>
            </fieldset>
            ${this.config.file && this.config.fmt && this.grid && this.config.ds?.length ? this.renderDatasets() : nothing}
            ${this.config.file && this.config.fmt && this.config.ts ? this.renderAddDataset() : nothing}
        `
    }

    renderDatasets() {
        this.grid.removeAll()
        if (!this.config.ds?.length) return

        // Add grid widgets
        this.grid.batchUpdate(true)
        this.config.ds.map(dataset => {
            const text = `${dataset.k} ▸ ${dataset.l} (${dataset.u})`
            const content = `
                <div class="item-move" title="Drag to re-order">☰</div>
                <div class="item-text" title="${text}">${text}</div>
                <div class="item-remove" title="Click to remove"
                    onClick="this.dispatchEvent(new CustomEvent('item-remove', {bubbles: true, detail: {k: '${dataset.k}'}}))"
                >✖</div>
            `
            this.grid.addWidget({noResize:true, content, l:dataset.l, k:dataset.k, u: dataset.u})
        });
        this.grid.batchUpdate(false)
    }

    renderAddDataset() {
        return html`
            <fieldset>
                ${this.add ? html`
                    <section>
                        <label for="key">${this.config.fmt === 'json' ? 'JSON key name' : 'CSV column'}:</label>
                        <select id="key" required
                            .value=${this.key}
                            @change=${event => this.key = event.target.value}
                        >
                            <option value=""></option>
                            ${this.fileDetails.keys.filter(key => key !== this.config.ts).map(key => html`
                                <option value="${key}">${key}</option>
                            `)}
                        </select>
                    </section>
                    <section>
                        <label for="label">Metric name:</label>
                        <input type="text" id="label" autocomplete="off" required placeholder="Example: Outside Temperature"
                            @change=${event => this.label = event.target.value}
                        >
                    </section>
                    <section>
                        <label for="unit">Measurement unit:</label>
                        <input type="text" id="unit" autocomplete="off" required placeholder="Example: °F"
                            @change=${event => this.unit = event.target.value}
                        >
                    </section>
                    <section>
                        <button type="button" @click=${this.addMetric}>Add metric</button>
                        ${this.config.ds?.length ? html`
                            <button type="button" @click=${() => this.add = false}>Cancel</button>
                        ` : nothing}
                    </section>
                ` : html`
                    <section>
                        <button type="button" @click=${this.addAnother}>Add another metric</button>
                    </section>
                `}
            </fieldset>
        `
    }

    async connectedCallback() {
        super.connectedCallback()

        if (this.config.ds === undefined) {
            this.config = { ...this.config, ds: []}
            return
        }

        // Prefill form
        this.file = this.config.file
    }

    firstUpdated() {
        this.grid = GridStack.init({
            column: 1,
            cellHeight: 30,
            margin: 0,
            handle: '.item-move',
            removable: false,
        }, this.renderRoot.querySelector('.grid-stack'))

        this.grid.on('change', () => this.updateConfig({
            ds: this.grid.engine.nodes.map(node => { return {l: node.l, k: node.k, u: node.u} })
        }))

        if (!this.config.file) this.focus('#file', true)
        else this.onCheckFile()
    }

    addMetric() {
        const k = this.key.trim()
        if (k == '') {
            this.focus('#key', true)
            return
        }

        const l = this.label.trim()
        if (l == '') {
            this.focus('#label', true)
            return
        }

        const u = this.unit.trim()
        if (u == '') {
            this.focus('#unit', true)
            return
        }

        // Add metric
        const ds = this.config.ds || []
        this.updateConfig({
            ds: this.config.ds.concat({l, k, u})
        })

        // Reset add inputs
        this.add = false
    }

    addAnother() {
        this.label = ''
        this.key = ''
        this.unit = ''
        this.add = true
        this.focus('#key')
    }

    onFileChange() {
        this.fileDetails = undefined
        this.updateConfig({
            file: undefined,
            fmt: undefined,
            ts: undefined,
            ds: [],
        })
    }

    async onCheckFile() {

        // Reset state
        this.updateConfig({file: undefined})

        const file = this.renderRoot.querySelector('#file').value.trim()
        if (!file) return this.focus('#file', true)

        this.fileFeedback = 'Analyzing file, please wait ...'
        try {
            this.fileDetails = await DatastoreHelper.analyzeFile(file)
            console.log(this.fileDetails)
            this.updateConfig({file, fmt: this.fileDetails.fmt})
            this.fileFeedback = undefined

            if (!this.config.ds?.length) this.addAnother()
        } catch (ex) {
            this.fileFeedback = ex.message
            this.fileDetails = undefined
            return this.focus('#ts', true)
        }
    }

    onTimestampSelect(event) {
        this.updateConfigFromEvent('ts', event)
        if (!this.config.ds?.length) this.addAnother()
    }

    onItemRemove(event) {
        console.log('onItemRemove', event)
        const k = event.detail.k
        this.updateConfig({
            ds: this.config.ds.filter(dataset => dataset.k !== k)
        })
        if (!this.config.ds?.length) this.addAnother()
    }

    focus(querySelector, focus = false) {
        setTimeout(() => {
            const element = this.renderRoot.querySelector(querySelector)
            if (!element) return
            element.scrollIntoView({behavior: 'smooth', block: 'center'});
            if (focus) element.focus({preventScroll: false, focusVisible: true})
        }, 0)
    }

    updateConfigFromEvent(key, event) {
        this.updateConfig({[key]: event.target.value !== '' ? event.target.value : undefined})
    }

    updateConfig(props) {
        this.config = {...this.config, ...props}
    }

    decorateConfig(config) {
        return {...config, ...this.config}
    }
}
