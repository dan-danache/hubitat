import { html, css, LitElement, unsafeHTML } from '../vendor/vendor.min.js';

export class TextPanel extends LitElement {
    static styles = css`
        :host {
            display: block;
            width: 100%;
            height: 100%;
        }
        article {
            position: absolute;
            top: 50%;
            left: 50%;
            width: calc(100% - 2em);
            transform: translate(-50%, -50%);
            max-height: 100%;
            overflow-y: auto;
            scrollbar-width: thin;
            scrollbar-color: transparent transparent;
        }
        article:hover {
            scrollbar-color: initial;
        }
        article a { color: var(--Blue) }
        nav {
            position: absolute;
            top: 0px; left: 2px;
            cursor: pointer;
            visibility: hidden;
        }
        :host(:hover) nav {
            visibility: visible;
        }
    `

    static properties = {
        config: { type: Object, reflect: true },
    }

    render() {
        return html`
            <article>${unsafeHTML(this.config.message)}</article>
            <nav title="Edit tile" @click=${this.editPanel}>⚙️</nav>
        `
    }

    firstUpdated() {
        this.classList.remove('spinner')
    }

    editPanel() {
        this.dispatchEvent(new CustomEvent('edit', { bubbles: true, detail: this.config }))
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
        config: { type: Object, reflect: true },
    }

    constructor() {
        super()
        this.config = {
            message: ''
        }
    }

    render() {
        return html`
            <fieldset>
                <label for="message">Message to display:</label>
                <input type="text" id="message" required="true" placeholder="Enter text to display" autocomplete="off"
                    .value=${this.config.message}
                    @change=${event => this.config = { message: event.target.value}}
                />
            </fieldset>
        `
    }

    connectedCallback() {
        super.connectedCallback()
        if (this.config?.message === undefined) {
            this.config = { ...this.config,
                message: '',
            }
        }
    }

    createRenderRoot() {
        return this
    }

    firstUpdated() {
        setTimeout(() => this.renderRoot.querySelector('#message').focus(), 0)
    }

    decorateConfig(config) {
        return { ...config,
            message:this.config.message,
        }
    }
}
