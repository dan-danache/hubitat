import { html, css, LitElement, unsafeHTML } from '../vendor/vendor.min.js';

export class TextPanel extends LitElement {
    static styles = css`
        article {
            position: absolute;
            top: 50%;
            left: 50%;
            width: calc(100% - 2em);
            transform: translate(-50%, -50%);
            max-height: 100%;
            overflow-y: auto;
        }
        article a { color: var(--Blue) }
    `

    static properties = {
        config: { type: Object, reflect: true },
    }

    render() {
        return html`<article>${unsafeHTML(this.config.message)}</article>`
    }

    firstUpdated() {
        this.classList.remove('spinner')
    }

    decorateConfig(config) {
        return config
    }

    refresh() {
        // Do nothing!
    }
}

export class TextPanelConfig extends LitElement {
    static properties = {
        message: { type: String, state: true },
    }

    constructor() {
        super()
        this.message = ''
    }

    render() {
        return html`
            <label for="message">Message to display:</label>
            <input type="text" id="message" required="true" placeholder="Enter text to display" autocomplete="off" @change=${event => this.message = event.target.value}/>
        `
    }

    createRenderRoot() {
        return this
    }

    firstUpdated() {
        setTimeout(() => this.renderRoot.querySelector('#message').focus(), 0)
    }

    decorateConfig(config) {
        return { ...config, message: this.message }
    }
}
