import { html, LitElement } from '../vendor/vendor.min.js';

export class DashboardMenu extends LitElement {
    static properties = {
        open: { type: Boolean, reflect: true },
        refreshInterval: { type: String, state: true },
        theme: { type: String, state: true },
        yScale: { type: String, state: true },
        cellHeight: { type: Number, state: true },
    }

    constructor() {
        super()
        this.open = false
        this.refreshInterval = '0'
        this.theme = 'light'
        this.yScale = 'auto'
        this.cellHeight = 206
    }

    createRenderRoot() {
        return this
    }

    render() {
        return html`
            <nav>
                <button @click=${this.addTile} title="Add a new dashboard tile"><b>+</b> Add dashboard tile</button>
                <button @click=${this.compactTiles} title="Re-order dashboard tiles to fill any empty space">⋮⋮⋮ Compact space</button>
                <hr>
                <label for="refreshInterval">Auto-refresh</label>
                <select id="refreshInterval" .value=${this.refreshInterval} @change=${this.changeRefreshInterval}>
                    <option value="0">no refresh</option>
                    <option value="5">every 5 minutes</option>
                    <option value="10">every 10 minutes</option>
                    <option value="30">every 30 minutes</option>
                    <option value="60">every hour</option>
                </select>
                <label for="theme">Theme</label>
                <select id="theme" .value=${this.theme} @change=${this.changeTheme}>
                    <option value="light">light</option>
                    <option value="dark">dark</option>
                </select>
                <label>Y-axis scale</label>
                <select id="yScale" .value=${this.yScale} @change=${this.changeYScale}>
                    <option value="auto">auto</option>
                    <option value="fixed">fixed</option>
                </select>
                <label>Cell height</label>
                <form @submit=${this.changeCellHeight}>
                    <input type="text" name="cellHeight" autocomplete="off"
                        pattern="[1-3][0-9]{2}"
                        .value="${this.cellHeight}"
                    >
                    <input type="submit" value="➤" title="Apply cell height">
                </form>
                <hr>
                <button @click=${this.saveDashboard} title="Save current dashboard layout">✓ Save dashboard</button>
                <aside>v2.11.0</aside>
            </nav>
        `;
    }

    connectedCallback() {
        super.connectedCallback();
        window.addEventListener('touchstart', event => this.touchStart(event));
        window.addEventListener('touchend', event => this.touchEnd(event));
    }

    addTile() {
        this.dispatchEvent(new CustomEvent('add'))
    }

    compactTiles() {
        this.dispatchEvent(new CustomEvent('compact'))
    }

    saveDashboard() {
        this.dispatchEvent(new CustomEvent('save', { detail: {
            theme: this.theme,
            refresh: this.refreshInterval,
            yScale: this.yScale,
            cellHeight: this.cellHeight,
        }}))
    }

    changeRefreshInterval(event) {
        this.refreshInterval = event.target.value
        this.dispatchEvent(new CustomEvent('changeRefreshInterval', { detail: this.refreshInterval }))
    }

    changeTheme(event) {
        this.setTheme(event.target.value)
    }

    changeYScale(event) {
        this.yScale = event.target.value
        this.dispatchEvent(new CustomEvent('changeYScale', { detail: this.yScale }))
    }
    changeCellHeight(event) {
        event.preventDefault()
        const formProps = Object.fromEntries(new FormData(event.target))
        this.cellHeight = parseInt(formProps.cellHeight)
        this.dispatchEvent(new CustomEvent('changeCellHeight', { detail: this.cellHeight }))
    }

    setTheme(theme) {
        this.theme = theme
        this.dispatchEvent(new CustomEvent('changeTheme', { detail: this.theme }))

        // Apply theme
        document.documentElement.setAttribute('data-theme', this.theme)

        const params = new URLSearchParams(window.location.search)
        document.querySelector('meta[name="theme-color"]').setAttribute('content', theme == 'dark' ? '#1b1b1b' : '#eee8d5')
        document.querySelector('link[rel="manifest"]').setAttribute(
            'href',
            `./app.webmanifest?access_token=${params.get('access_token')}&name=${encodeURIComponent(params.get('name'))}&theme=${theme}`
        )
    }

    touchStart(event) {
        this.startX = event.changedTouches[0].clientX
    }

    touchEnd(event) {
        if (this.startX == undefined) return
        const endX = event.changedTouches[0].clientX
        const diff = endX - this.startX
        if (!this.open && this.startX < 60 && diff > 50) {
            this.open = true
            return
        }
        if (this.open && endX < 150 && diff < -50) this.open = false
    }
}
