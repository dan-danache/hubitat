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
        this.renderRoot.querySelector('iframe').src += ''
    }
}

export class IframePanelConfig extends LitElement {
    static properties = {
        url: { type: String, state: true },
    }

    constructor() {
        super()
        this.url = ''
    }

    render() {
        return html`
            <label for="url">URL to load:</label>
            <input type="url" id="url" required="true" type="url" pattern="https?://.+" placeholder="Enter URL to load" autocomplete="off" @change=${event => this.url = event.target.value}/>
        `
    }

    createRenderRoot() {
        return this
    }

    firstUpdated() {
        setTimeout(() => this.renderRoot.querySelector('#url').focus(), 0)
    }

    decorateConfig(config) {
        return { ...config, url: this.url }
    }
}
