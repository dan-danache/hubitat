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
            scrollbar-width: thin;
            scrollbar-color: transparent transparent;
        }
        iframe:hover {
            scrollbar-color: initial;
        }
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
            <iframe src=${this.config.url} frameborder="0" allowtransparency="true">Failed to load iframe</iframe>
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
        if (this.config.noRefresh === true) return
        this.renderRoot.querySelector('iframe').src += ''
    }
}

export class IframePanelConfig extends LitElement {
    static properties = {
        config: { type: Object, reflect: true },
    }

    constructor() {
        super()
        this.config = {
            url: '',
            noBorder: false,
            noRefresh: true,
        }
    }

    render() {
        return html`
            <fieldset>
                <section>
                    <label for="url">URL to load:</label>
                    <input type="url" id="url" required="true" type="url" pattern="https?://.+" placeholder="Enter URL to load" autocomplete="off"
                        .value=${this.config.url}
                        @change=${event => this.config = {...this.config, url: event.target.value}}
                    />
                </section>
                <section>
                    <div class="checkbox">
                        <input type="checkbox" value="true" id="noBorder"
                            .checked=${this.config.noBorder}
                            @change=${event => this.config = {...this.config, noBorder: event.target.checked}}
                        >
                        <label for="noBorder">Hide tile border</label>
                    </div>
                    <div class="checkbox">
                        <input type="checkbox" value="true" id="noRefresh"
                            .checked=${this.config.noRefresh}
                            @change=${event => this.config = {...this.config, noRefresh: event.target.checked}}
                        >
                        <label for="noRefresh">Disable auto-refresh</label>
                    </div>
                </section>
            </fieldset>
        `
    }

    createRenderRoot() {
        return this
    }

    connectedCallback() {
        super.connectedCallback()
        if (this.config?.url === undefined) {
            this.config = { ...this.config,
                url: '',
            }
        }
    }

    firstUpdated() {
        setTimeout(() => this.renderRoot.querySelector('#url').focus(), 0)
    }

    decorateConfig(config) {
        return { ...config,
            url: this.config.url,
            noBorder: this.config.noBorder === true ? true : undefined,
            noRefresh: this.config.noRefresh === true ? true : undefined,
        }
    }
}
