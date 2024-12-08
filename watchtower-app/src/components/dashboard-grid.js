import { html, LitElement } from '../vendor/vendor.min.js';

// v11 fix
GridStack.renderCB = (el, w) => { el.innerHTML = w.content }

export class DashboardGrid extends LitElement {
    static properties = {
        name: { type: String, reflect: true },
    }

    constructor() {
        super()
        this.yScale = undefined
    }

    createRenderRoot() {
        return this
    }

    render() {
        return html`
            <div class="grid-stack spinner" @edit=${this.editPanel}></div>
        `;
    }

    firstUpdated() {
        this.grid = GridStack.init({
            sizeToContent: false,
            margin: 5,
            float: true,

            // Non-configurable options
            cellHeight: 206,
            column: 8,
            columnOpts: {
                layout: 'move', // moveScale move scale list compact none
                columnMax: 8,
                breakpointForWindow: true,

                // TODO: fiddle with these until it behaves nicely on mobile/tablet/laptop
                breakpoints: [
                    { w: 420, c: 1 },
                    { w: 800, c: 2 },
                    { w: 1024, c: 4 },
                    { w: 1366, c: 6 }
                ]
            },
            handle: '.panel-title',
            removable: true,
            alwaysShowResizeHandle: false
        }, this.renderRoot.querySelector('.grid-stack'))
    }

    async init(panels) {
        if (panels.length === 0) {
            this.addPanel({
                type: 'text-panel',
                title: 'Quick Instructions',
                message: `
                    <b>😎 Welcome to your new dashboard!</b>
                    <ul>
                        <li>Press the <b>ESC</b> (Escape) key on your keyboard, or swipe from the left margin, to show or hide the left menu.
                        <li>Click the <b>Add dashboard tile</b> button to add new tiles to your dashboard.
                        <li>Rearrange tiles by dragging their titles. Resize tiles by dragging the bottom-right corner.
                        <li>Remove tiles by dragging them outside the dashboard grid.
                        <li>Remember to click the <b>Save dashboard</b> button when you're happy with the layout.
                    </ul>
                    For more information, refer to the <a href="https://dan-danache.github.io/hubitat/watchtower-app/" target="_blank">official documentation</a>.
                `
            }, 2, 2, 1, 0)
        } else {
            this.grid.batchUpdate(true)
            panels.forEach(panel => this.addPanel(panel.config, panel.w, panel.h, panel.x, panel.y))
            this.grid.batchUpdate(false)
        }

        // Remove spinner
        this.renderRoot.querySelector('.grid-stack').classList.remove('spinner')
    }

    setRefreshInterval(refreshMinutes) {
        this.interval && clearInterval(this.interval)
        if (refreshMinutes == 0) return
        this.interval = setInterval(() => {
            this.renderRoot.querySelectorAll('.panel').forEach(panel => panel.refresh())
        }, refreshMinutes * 60 * 1000)
        console.info(`Setting auto-refresh timer for ${refreshMinutes} minutes`)
    }

    setYScale(yScale) {
        this.yScale = yScale
        this.renderRoot.querySelectorAll('device-panel, attribute-panel').forEach(panel => panel.setYScale(yScale))
    }

    setCellHeight(cellHeight) {
        this.grid.cellHeight(cellHeight, true)
    }

    addPanel(config, ww = 2, wh = 1, wx = undefined, wy = undefined) {
        let w = ww, h = wh, x = wx, y = wy

        // Remove widget, if exists
        const old = this.grid.engine.nodes.find(node => node.config?.id == config.id)
        if (old) {
            w = old.w, h = old.h, x = old.x, y = old.y
            this.grid.removeWidget(old.el)
        } else {
            config.id = this.randomUUID()
        }

        // Add gridstack widget
        const content = `
            <div class="panel-container ${config.type}">
                <${config.type} config='${JSON.stringify(config).replace(/'/g, '&apos;')}' yScale="${this.yScale}" class="panel empty spinner"></${config.type}>
                <div class="panel-title">${config.title || '&nbsp' }</div>
            </div>
        `
        this.grid.addWidget({w, h, x, y, config, content, id:`${config.noBorder === true ? 'tr-' : ''}${config.id}`})
    }

    editPanel(event) {
        this.dispatchEvent(new CustomEvent('edit', { bubbles: true, detail: event.detail }))
    }

    randomUUID() {
        return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
            var r = Math.random()*16|0, v = c == 'x' ? r : (r&0x3|0x8);
            return v.toString(16);
        })
    }

    compact() {
        this.grid.compact()
    }

    getPanelsConfig() {
        return this.grid.engine.nodes.map(node => {
            const panelElm = node.el.querySelector('.panel')
            return {
                w: node.w,
                h: node.h,
                x: node.x,
                y: node.y,
                config: panelElm.decorateConfig(node.config)
            }
        })
    }
}
