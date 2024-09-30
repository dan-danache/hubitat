import { html, css, LitElement } from '../vendor/vendor.min.js';

export class DashboardMenu extends LitElement {
    static styles = css`
        :host {
            display: block;
            position: fixed;
            top: 0;
            left: 0;
            height: 100%;
        }
        nav {
            box-sizing: border-box;
            position: absolute;
            top: 0;
            left: -15em;
            width: 15em;
            height: 100%;
            padding: 1em;
            background-color: var(--bg-color-darker);
            color: var(--text-color);
            border-right: 1px var(--border-color) solid;
            animation: hideme .3s;
        }
        :host([open]) nav {
            box-shadow: 0 0 1em var(--shadow-color);
            left: 0;
            animation: showme .3s;
        }
        @keyframes hideme {
            from { left: 0 }
            to { left: -15em }
        }
        @keyframes showme {
            from { left: -15em }
            to { left: 0px }
        }
        hr {
            border: 0;
            border-top: 1px var(--separator-color) solid;
            margin: 1em 0;
        }
        button {
            background-color: var(--bg-color);
            color: var(--text-color);
            border: 1px var(--border-color) solid;
            border-radius: 5px;
            margin-bottom: 5px;
            padding: .5em 1em;
            cursor: pointer;
            display: block;
            width: 100%;
            text-align: left;
            box-shadow: 0 0 0.3em var(--shadow-color);
        }
        button:hover {
            background-color: var(--Blue);
            color: var(--Base3);
        }
        label {
            display: block;
            margin: 1em 0 .5em 0;
            font-size: .85rem;
        }
        select {
            display: block;
            width: 100%;
            margin-bottom: 5px;
            height: 30px;
            background-color: var(--bg-color-darker);
            color: var(--text-color);
            border: 1px var(--border-color) solid;
            border-radius: 5px;
        }
        select:focus, button:focus {
            outline: 1px var(--Blue) solid;
            border-color: var(--Blue);
        }
        aside {
            color: var(--text-color-darker);
            position: absolute;
            bottom: .5em;
            left: 50%;
            transform: translateX(-50%);
            user-select: none;
            font-size: .75rem;
        }
        form {
            border: 1px solid grey;
            padding: 0;
            background-color: var(--bg-color-darker);
            color: var(--text-color);
            border: 1px var(--border-color) solid;
            border-radius: 5px;
            position: relative;
            display: block;
            width: 100%;
        }
        form input[type="text"] {
            box-sizing: border-box;
            border: none;
            color: var(--text-color);
            background-color: transparent;
            padding-right: 30px;
            padding-left: 5px;
            width: 100%;
            height: 28px;
        }
        form input:focus {
            outline: none;
        }
        form:focus-within {
            border-color: var(--Blue);
            outline: 1px solid var(--Blue); 
        }
        form:invalid {
            border-color: var(--Red);
            outline: 1px solid var(--Red);
        }
        form input[type="submit"] {
            border: none;
            color: var(--text-color-darker);
            background-color: transparent;
            position: absolute;
            top: 50%;
            right: 0px;
            transform: translateY(-50%);
            cursor: pointer;
            width: 30px;
        }
    `;

    static properties = {
        open: { type: Boolean, reflect: true },
        refreshInterval: { type: String, state: true },
        theme: { type: String, state: true },
        yScale: { type: String, state: true },
        cellHeight: { type: Number, state: true },
        mobileView: { type: Boolean, state: true }
    }

    constructor() {
        super()
        this.open = false
        this.refreshInterval = '0'
        this.theme = 'light'
        this.yScale = 'auto'
        this.cellHeight = 206
    }

    render() {
        return html`
            <nav>
                ${this.mobileView ? '' : html`
                    <button @click=${this.addTile} title="Add a new dashboard tile"><b>+</b> Add dashboard tile</button>
                    <button @click=${this.compactTiles} title="Re-order dashboard tiles to fill any empty space">⋮⋮⋮ Compact space</button>
                    <hr>
                `}
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
                ${this.mobileView ? '' : html`
                    <hr>
                    <button @click=${this.saveDashboard} title="Save current dashboard layout">✓ Save dashboard</button>
                `}
                <aside>v2.0.1</aside>
            </nav>
        `;
    }

    connectedCallback() {
        super.connectedCallback();
        window.addEventListener('keydown', event => event.key === 'Escape' && (this.open = !this.open));
        window.addEventListener('touchstart', event => this.touchStart(event));
        window.addEventListener('touchend', event => this.touchEnd(event));
    }

    applyMobileView(mobileView) {
        this.mobileView = mobileView
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
        if (!this.open && this.startX < 30 && diff > 50) {
            this.open = true
            return
        }
        if (this.open && endX < 30 && diff < -50) this.open = false
    }
}
