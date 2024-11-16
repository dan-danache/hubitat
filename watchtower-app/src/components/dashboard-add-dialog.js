import { html, css, LitElement, unsafeHTML, nothing } from '../vendor/vendor.min.js';

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
        section.modal-body fieldset {
            border: 0;
            border-radius: 5px;
            background-color: var(--bg-color);
            padding: 10px;
        }
        section.modal-body fieldset:not(:last-child) {
            margin-bottom: 10px;
        }
        section.modal-body fieldset nav {
            text-align: right;
            border-top: 1px var(--border-color) solid;
            margin-top: 5px;
            padding-top: 5px;
            color: var(--border-color);
        }
        section.modal-body fieldset a {
            color: var(--Blue);
            text-decoration: none;
        }
        fieldset section:not(:last-child) {
            margin-bottom: 10px;
        }
        section.modal-body section > div {
            margin-top: .3em;
        }
        footer {
            padding: 1em;
            text-align: right;
        }
        button {
            padding: .5em 1em;
            cursor: pointer;
            background-color: var(--bg-color);
            color: var(--text-color);
            border: 1px var(--border-color) solid;
            border-radius: 5px;
            box-shadow: 0 0 0.3em var(--shadow-color);
        }
        button:not(:first-child) {
            margin-left: 10px;
        }
        button:hover {
            background-color: var(--Blue);
            color: var(--Base3);
        }
        label { display: block; user-select: none; margin-bottom: 5px }
        select { display: block; line-height: 32px }
        input, select {
            box-sizing: border-box;
            display: block;
            width: 100%;
            height: 32px;
            background-color: var(--bg-color-darker);
            color: var(--text-color);
            border: 1px var(--border-color) solid;
            border-radius: 5px;
            padding: 6px;
            margin: 0;
        }
        .checkbox:not(:last-child) { margin-bottom: 9px }
        .checkbox input { display: none }
        .checkbox label {
            align-items: center;
            cursor: pointer;
            display: flex;
            line-height: 15px;
            height: 15px;
            position: relative;
            magin-bottom: 0;
        }
        .checkbox label::before, .checkbox label::after { content: ''; display: block }
        .checkbox label::before {
            background-color: var(--Base00);
            border-radius: 500px;
            height: 15px;
            margin-right: 8px;
            width: 25px;
        }
        .checkbox input:user-invalid + label::before { background-color: var(--Red) }
        .checkbox label::after {
            background-color: #fff;
            border-radius: 13px;
            height: 13px;
            left: 1px;
            position: absolute;
            top: 1px;
            transition: transform 0.125s ease-out;
            width: 13px;
        }
        .checkbox input:checked + label::before { background-color: var(--Blue) }
        .checkbox input:checked + label::after { transform: translate3d(10px, 0, 0) }
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
        'statusmap-panel': 'Status Map',
        'text-panel': 'Text',
        'iframe-panel': 'Iframe',
        'hub-info-panel': 'Hub Info',
    }

    static properties = {
        open: { type: Boolean, reflect: true },
        config: { type: Object, state: true },
    }

    constructor() {
        super()
        this.open = false
        this.resetForm()
    }

    render() {
        setTimeout(() => this.renderRoot.querySelector('.panel-config')?.addEventListener('suggestTitle', event => this.suggestTitle(event)), 0)
        return this.open ? html`
            <article role="dialog" aria-modal="true" aria-labelledby="d-title">
                <form @submit=${this.submit}>
                    <header id="d-title">${this.config.id ? 'Edit': 'Add'} Dashboard Tile</header>
                    <section class="modal-body">
                        <fieldset>
                            <section>
                                <label for="title">Tile name:</label>
                                <input type="text" id="title" .value=${this.config.title} autocomplete="off" @change=${event => this.config.title = event.target.value} placeholder="[optional]">
                            </section>
                            <section>
                                <label for="type">Tile type:</label>
                                <select id="type" .value=${this.config.type} @change=${this.onTileTypeChange} required="true">
                                    <option value=""></option>
                                    ${Object.entries(DashboardAddDialog.panels).map(([key, val]) => html`
                                        <option value="${key}" .selected=${this.config.type === key}>${val}</option>
                                    `
                                    )}
                                </select>
                            </section>
                        </fieldset>
                        ${this.config.type ? this.renderPanelConfig() : nothing}
                    </section>
                    <footer>
                        <button type="reset" @click=${this.close}>Cancel</button>
                        <button type="submit">${this.config.id ? 'Update': 'Add'} tile</button>
                    </footer>
                </form>
            </article>
        ` : nothing;
    }

    renderPanelConfig() {
        return unsafeHTML(`
            <${this.config.type}-config class="panel-config"
                config='${JSON.stringify(this.config).replace(/'/g, '&apos;')}'></${this.config.type}-config
                @suggestTitle=${this.suggestTitle}
            ></${this.config.type}-config>
        `)
    }

    connectedCallback() {
        super.connectedCallback()
        window.addEventListener('keydown', event => event.key === 'Escape' && this.close(event))
    }

    onTileTypeChange(event) {
        this.config = {
            id: this.config.id,
            title: this.config.title,
            type: event.target.value
        }
    }

    suggestTitle(event) {
        if (this.config.title == '') this.config.title = event.detail
    }

    updated(changedProperties) {
        if (changedProperties.size == 0 || changedProperties.open === true) return
    }

    resetForm() {
        this.config = {
            title: ''
        }
    }

    close() {
        this.resetForm()
        this.open = false
    }

    submit(event) {
        event.preventDefault()
        const config = this.renderRoot.querySelector('.panel-config').decorateConfig({
            id: this.config.id,
            title: this.config.title,
            type: this.config.type
        })
        Object.keys(config).forEach(key => config[key] === undefined && delete config[key])
        console.info('**** Adding panel with config', config)

        this.close()
        this.resetForm()
        this.dispatchEvent(new CustomEvent('done', {detail: config}))
    }
}
