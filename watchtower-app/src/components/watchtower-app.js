import { html, LitElement } from '../vendor/vendor.min.js';
import { DatastoreHelper } from '../helpers/datastore-helper.js'

export class WatchtowerApp extends LitElement {
    static properties = {
        halt: { type: Boolean, state: true },
        embedded: { type: Boolean, state: true },
    }

    constructor() {
        super()

        this.menuElm = undefined
        this.gridElm = undefined
        this.dialogElm = undefined

        this.params = new URLSearchParams(window.location.search)
        this.name = this.params.get('name')
        if (this.name === null) {
            this.bailOut('0001')
            throw new Error('Query parameter [name] is missing!')
        }

        if (this.params.get('access_token') === null) {
            this.bailOut('0002')
            throw new Error('Query parameter [access_token] is missing!')
        }

        // Embeddable
        window.addEventListener('load', () => {
            if (window.top === window.self) return
            document.body.classList.add('embedded')
            this.embedded = true
        })

        this.halt = false
    }

    createRenderRoot() {
        return this
    }

    render() {
        return this.halt !== false ? '' : html`
            <dashboard-grid id="grid" name=${this.name}
                class="${this.embedded ? 'embedded' : ''}"
                @edit=${this.editPanel}
            ></dashboard-grid>
            ${this.embedded === true ? '' : html`
                <dashboard-menu id="menu"
                    @add=${this.showAddDialog}
                    @compact=${this.compactPanels}
                    @changeRefreshInterval=${this.applyRefreshInterval}
                    @changeYScale=${this.applyYScale}
                    @changeCellHeight=${this.applyCellHeight}
                    @save=${this.saveDashboard}
                ></dashboard-menu>
                <dashboard-add-dialog id="add-dialog"
                    @done=${this.addDashboardPanel}
                ></dashboard-add-dialog>
            `}
        `
    }

    connectedCallback() {
        super.connectedCallback()
        window.addEventListener('keydown', event => this.onKeyDown(event));
        document.body.classList.remove('spinner')
    }

    async firstUpdated() {
        this.menuElm = this.renderRoot.querySelector('dashboard-menu')
        this.gridElm = this.renderRoot.querySelector('dashboard-grid')
        this.dialogElm = this.renderRoot.querySelector('dashboard-add-dialog')

        const layout = await DatastoreHelper.fetchGridLayout(this.params.get('name'));
        const refreshInterval = layout.refresh ? parseInt(layout.refresh) : 0
        const theme = layout.theme === 'dark' ? 'dark' : 'light'
        const yScale = layout.yScale == 'fixed' ? 'fixed' : 'auto'
        const cellHeight = layout.cellHeight ? parseInt(layout.cellHeight) : 206

        // Show menu if dashboard contains no panels
        if (layout.panels.length === 0) this.menuElm.open = true

        // Init grid
        await this.gridElm.updateComplete
        this.gridElm.init(layout.panels)
        this.gridElm.setRefreshInterval(refreshInterval)
        this.gridElm.setYScale(yScale)
        this.gridElm.setCellHeight(cellHeight)

        // Update menu
        this.menuElm.refreshInterval = refreshInterval
        this.menuElm.yScale = yScale
        this.menuElm.cellHeight = cellHeight
        this.menuElm.setTheme(theme)
    }

    onKeyDown(event) {
        if (event.key !== 'Escape') return
        if (this.dialogElm.open) {
            this.dialogElm.open = false
            return
        }
        this.menuElm.open = !this.menuElm.open
    }

    bailOut(errCode) {
        alert(`
            This file is part of the Watchtower application.

            To load or create a Watchtower dashboard, please got to:
            • Apps -> Watchtower -> Dashboards

            Error Code: #${errCode}
        `)
    }

    async saveDashboard(event) {
        const layout = {
            ...event.detail,
            panels: this.gridElm.getPanelsConfig()
        }
        console.info('Saving dashboard to Hubitat', this.name, layout)
        await DatastoreHelper.saveGridLayout(this.name, layout)
    }

    showAddDialog() {
        this.dialogElm.resetForm()
        this.dialogElm.setAttribute('open', true)
    }

    editPanel(event) {
        this.dialogElm.config = JSON.parse(JSON.stringify(event.detail))
        this.dialogElm.setAttribute('open', true)
    }

    compactPanels() {
        this.gridElm.compact()
    }

    addDashboardPanel(event) {
        this.gridElm.addPanel(event.detail)
    }

    applyRefreshInterval(event) {
        const refreshInterval = parseInt(event.detail)
        this.gridElm.setRefreshInterval(refreshInterval)
    }

    applyYScale(event) {
        this.gridElm.setYScale(event.detail)
    }

    applyCellHeight(event) {
        this.gridElm.setCellHeight(event.detail)
    }
}
