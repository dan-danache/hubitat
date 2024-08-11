import { html, css, LitElement } from '../vendor/vendor.min.js';

export class IframePanel extends LitElement {
    static styles = css`
        iframe {
            background-color: transparent;
            position: absolute;
            top: 0;
            left: 0;
            width: 100%;
            height: 100%;
        }
    `

    static properties = {
        config: { type: Object, reflect: true },
    }

    render() {
        return html`<iframe src=${this.config.url} frameborder="0" allowtransparency="true">Failed to load iframe</iframe>`
    }

    firstUpdated() {
        this.classList.remove('spinner')
    }

    decorateConfig(config) {
        return config
    }

    refresh() {
        if (this.config.noRefresh === true) return
        this.renderRoot.querySelector('iframe').src += ''
    }
}

export class IframePanelConfig extends LitElement {
    static properties = {
        url: { type: String, state: true },
        noBorder: { type: Boolean, state: true },
        noRefresh: { type: Boolean, state: true },
    }

    constructor() {
        super()
        this.url = ''
    }

    render() {
        return html`
            <section>
                <label for="url">URL to load:</label>
                <input type="url" id="url" required="true" type="url" pattern="https?://.+" placeholder="Enter URL to load" autocomplete="off" @change=${event => this.url = event.target.value}/>
            </section>
            <section>
                <label><input value="true" type="checkbox"
                    @change=${this.onNoBorderChecked}
                > Hide tile border</label>
                <label><input value="true" type="checkbox"
                    @change=${this.onNoRefreshChecked}
                > Disable auto-refresh</label>
            </section>
        `
    }

    createRenderRoot() {
        return this
    }

    firstUpdated() {
        setTimeout(() => this.renderRoot.querySelector('#url').focus(), 0)
    }

    onNoBorderChecked(event) {
        this.noBorder = event.target.checked
    }
    
    onNoRefreshChecked(event) {
        this.noRefresh = event.target.checked
    }

    decorateConfig(config) {
        return {
            ...config,
            url: this.url,
            noBorder: this.noBorder,
            noRefresh: this.noRefresh
        }
    }
}
