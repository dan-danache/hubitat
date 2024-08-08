import { html, css, LitElement, nothing } from '../vendor/vendor.min.js';
import { DatastoreHelper } from '../helpers/datastore-helper.js'

export class WatchtowerApp extends LitElement {
    static styles = css`
        :host {
            display: block;
            height: calc(100vh - 10px);
        }
    `

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

        this.mobileView = window.innerWidth < 768
        window.addEventListener('resize', () => {
            const newState = window.innerWidth < 768
            if (this.mobileView == newState) return
            this.mobileView = newState
            this.applyMobileView()
        })

        // Embeddable
        window.addEventListener('load', () => {
            if (window.top === window.self) return
            document.body.classList.add('embedded')
            this.embedded = true
        })

        this.halt = false
    }

    render() {
        return this.halt !== false ? nothing : html`
            <dashboard-grid name=${this.name} class="${this.embedded ? 'embedded' : nothing}"></dashboard-grid>
            <dashboard-menu
                @add=${this.showAddDialog}
                @compact=${this.compactPanels}
                @changeRefreshInterval=${this.changeRefreshInterval}
                @changeYScale=${this.changeYScale}
                @save=${this.saveDashboard}
            ></dashboard-menu>
            <dashboard-add-dialog @done=${this.addDashboardPanel}></dashboard-add-dialog>
        `
    }

    connectedCallback() {
        super.connectedCallback()
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

        // Show menu if dashboard contains no panels
        if (layout.panels.length === 0) this.menuElm.open = true

        // Init grid
        await this.gridElm.updateComplete
        this.gridElm.init(layout.panels)
        this.gridElm.setRefreshInterval(refreshInterval)
        this.gridElm.setYScale(yScale)

        // Update menu
        this.menuElm.refreshInterval = refreshInterval
        this.menuElm.yScale = yScale
        this.menuElm.setTheme(theme)

        // Apply mobile view
        this.applyMobileView()
    }

    bailOut(errCode) {
        alert(`
            This file is part of the Watchtower application.

            To load or create a Watchtower dashboard, please got to:
            • Apps -> Watchtower -> Dashbaords

            Error Code: #${errCode}
        `)
    }

    applyMobileView() {
        this.gridElm.applyMobileView(this.mobileView)
        this.menuElm.applyMobileView(this.mobileView)
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
        this.dialogElm.setAttribute('open', true)
    }

    compactPanels() {
        this.gridElm.compact()
    }

    addDashboardPanel(event) {
        this.gridElm.addPanel(event.detail)
    }

    changeRefreshInterval(event) {
        const refreshInterval = parseInt(event.detail)
        this.gridElm.setRefreshInterval(refreshInterval)
    }

    changeYScale(event) {
        this.gridElm.setYScale(event.detail)
    }
}
