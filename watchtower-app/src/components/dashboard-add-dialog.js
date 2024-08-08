import { html, css, LitElement, nothing, unsafeHTML } from '../vendor/vendor.min.js';

export class DashboardAddDialog extends LitElement {
    static styles = css`
        :host {
            display: none;
            position: fixed;
            width: 100%; height: 100%;
            top: 0; left: 0;
            background-color: rgba(0, 0, 0, 0.3);
            font-size: 0.84rem;
        }
        :host([open="true"]) {
            display: block;
        }
        .spinner:after {
            position: absolute;
            top: calc(50% - 0.5em);
            left: 0.5em;
            content: ' ';
            display: block;
            width: 1em;
            height: 1em;
            border-radius: 50%;
            border: 1px solid var(--text-color-darker);
            border-color: var(--text-color-darker) transparent var(--text-color-darker) transparent;
            animation: spinner 1.2s linear infinite;
        }

        @keyframes spinner {
            0% { transform: rotate(0deg) }
            100% { transform: rotate(360deg) }
        }
        article {
            box-sizing: border-box;
            display: block;
            position: absolute;
            top: -50%;
            left: 50%;
            transform: translate(-50%, -50%);
            min-width: 300px;
            background-color: var(--bg-color-darker);
            color: var(--text-color);
            border: 1px var(--border-color) solid;
            box-shadow: 0 0 1em var(--shadow-color);
            border-radius: 5px;
            padding: 0;
        }
        :host([open="true"]) article {
            animation: showme .5s;
            animation-fill-mode: forwards;
        }
        @keyframes showme {
            to { top: 50% }
        }
        header {
            text-align: center;
            font-weight: bold;
            padding: 1em;
        }
        section.modal-body {
            max-height: 70vh;
            overflow-y: auto;
            padding: 0 1em;
        }
        section.modal-body section {
            margin-bottom: 1em;
        }
        footer {
            padding: 1em;
            text-align: right;
        }
        footer button {
            padding: .5em 1em;
            cursor: pointer;
            margin-left: 1em;
            background-color: var(--bg-color);
            color: var(--text-color);
            border: 1px var(--border-color) solid;
            border-radius: 5px;
            box-shadow: 0 0 0.3em var(--shadow-color);
        }
        footer button:hover {
            background-color: var(--Blue);
            color: var(--Base3);
        }
        label { display: block; margin-bottom: .3em }
        select {
            display: block;
        }
        input, select {
            box-sizing: border-box;
            display: block;
            width: 100%;
            background-color: var(--bg-color-darker);
            color: var(--text-color);
            border: 1px var(--border-color) solid;
            border-radius: 5px;
            padding: .5em;
            margin: 0;
        }
        input[type="checkbox"] {
            appearance: none;
            display: inline-block;
            margin: 0;
            background-color: var(--bg-color);
            border: 1px var(--border-color) solid;
            font: inherit;
            width: 1.15em;
            height: 1.15em;
            transform: translate(0, 3px);
            cursor: pointer;
        }
        input[type="checkbox"]:checked {
            background-color: var(--Blue)
        }
        input[type="checkbox"]:checked::before {
            content: "";
            display: block;
            width: calc(100% - 4px);
            height: calc(100% - 4px);
            margin: 2px;
            background-color: var(--Base3);
            clip-path: polygon(14% 44%, 0 65%, 50% 100%, 100% 16%, 80% 0%, 43% 62%);
        }
        input:focus, select:focus, button:focus {
            outline: 1px var(--Blue) solid;
            border-color: var(--Blue)
        }
        input:user-invalid, select:user-invalid {
            border-color: var(--Red) !important;
        }
        aside {
            display: block;
            position: relative;
            background-color: var(--bg-color);
            color: var(--text-color-darker);
            border: 1px var(--border-color) solid;
            padding: .5em .5em .5em 2.3em;
        }
    `;

    static panels = {
        'device-panel': 'Device',
        'attribute-panel': 'Attribute',
        'text-panel': 'Text',
        'iframe-panel': 'Iframe',
        'hub-info-panel': 'Hub Info',
    }

    static properties = {
        open: { type: Boolean, reflect: true },
        title: { type: String, title: true },
        type: { type: String, state: true },
    }

    constructor() {
        super()
        this.title = ''
        this.type = undefined
    }

    render() {
        setTimeout(() => this.renderRoot.querySelector('.panel-config')?.addEventListener('suggestTitle', event => this.suggestTitle(event)), 0)
        return html`
            <article role="dialog" aria-modal="true" aria-labelledby="d-title">
                <form @submit=${this.submit}>
                    <header id="d-title">Add dashboard tile</header>
                    <section class="modal-body">
                        <section>
                            <label for="title">Title title:</label>
                            <input type="text" id="title" .value=${this.title} autocomplete="off" @change=${event => this.title = event.target.value} placeholder="[optional]">
                        </section>
                        <section>
                            <label for="type">Tile type:</label>
                            <select id="type" .value=${this.type} @change=${event => this.type = event.target.value} required="true">
                                <option value=""></option>
                                ${Object.entries(DashboardAddDialog.panels).map(([key, val]) => html`
                                    <option value="${key}" .selected=${this.type === key}>${val}</option>
                                `
                                )}
                            </select>
                        </section>
                        <section>
                            ${this.type ? this.renderPanelConfig() : nothing}
                        </section>
                    </section>
                    <footer>
                        <button type="reset" @click=${this.close}>Cancel</button>
                        <button type="submit">Add tile</button>
                    </footer>
                </form>
            </article>
        `;
    }

    renderPanelConfig() {
        return unsafeHTML(`<${this.type}-config class="panel-config"></${this.type}-config>`)
    }

    connectedCallback() {
        super.connectedCallback()
        window.addEventListener('keydown', event => event.key === 'Escape' && this.close())
    }

    suggestTitle(event) {
        if (this.title == '') this.title = event.detail
    }

    updated(changedProperties) {
        if (changedProperties.size == 0 || changedProperties.open === true) return
        setTimeout(() => this.renderRoot.querySelector('#title').focus(), 0)
    }

    resetForm() {
        this.title = ''
        this.type = undefined
    }

    close() {
        this.resetForm()
        this.open = false
    }

    submit(event) {
        event.preventDefault()
        const config = this.renderRoot.querySelector('.panel-config').decorateConfig({title: this.title, type: this.type})
        console.info('Adding panel with config', config)

        this.close()
        this.resetForm()
        this.dispatchEvent(new CustomEvent('done', {detail: config}))
    }
}
