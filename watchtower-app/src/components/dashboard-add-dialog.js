import { html, LitElement, unsafeHTML, nothing } from '../vendor/vendor.min.js';

export class DashboardAddDialog extends LitElement {
    static panels = {
        'device-panel': { label: 'Single Device', hasUserScript: true },
        'attribute-panel': { label: 'Single Attribute', hasUserScript: true },
        'custom-panel': { label: 'Multi Device/Attribute', hasUserScript: true },
        'statusmap-panel': { label: 'Status Map', hasUserScript: true },
        'stats-panel': { label: 'Stats', hasUserScript: false },
        'byod-panel': { label: 'Bring Your Own Data', hasUserScript: true },
        'text-panel': { label: 'Text', hasUserScript: false },
        'iframe-panel': { label: 'Iframe', hasUserScript: false },
        'hub-info-panel': { label: 'Hub Info', hasUserScript: false },
    }

    static properties = {
        open: { type: Boolean, reflect: true },
        showUserScript: { type: Boolean, state: true },
        config: { type: Object, state: true },
    }

    constructor() {
        super()
        this.open = false
        this.showUserScript = false
        this.resetForm()
    }

    createRenderRoot() {
        return this
    }

    render() {
        setTimeout(() => this.renderRoot.querySelector('.panel-config')?.addEventListener('suggestTitle', event => this.suggestTitle(event)), 0)
        return this.open ? html`
            <article role="dialog" aria-modal="true" aria-labelledby="d-title">
                ${this.showUserScript ? this.renderUserScriptForm() : this.renderPanelForm()}
            </article>
        ` : nothing;
    }

    renderPanelForm() {
        return html`
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
                                    <option value="${key}" .selected=${this.config.type === key}>${val.label}</option>
                                `
                                )}
                            </select>
                        </section>
                    </fieldset>
                    ${this.config.type ? this.renderPanelConfig() : nothing}
                </section>
                <footer>
                        ${this.config.type && DashboardAddDialog.panels[this.config.type]?.hasUserScript == true ? html`
                        <button type="button" @click=${() => this.showUserScript = true} title="Beware: Here be dragons!">&lt;/&gt;</button>
                    ` : nothing}
                    <button type="reset" @click=${this.close}>Cancel</button>
                    <button type="submit">${this.config.id ? 'Update': 'Add'} tile</button>
                </footer>
            </form>
        `
    }

    renderUserScriptForm() {
        setTimeout(() => this.renderRoot.querySelector('#uscript').focus(), 0)
        return html`
            <form @submit=${this.submitUserScript}>
                <header id="d-title">Beware: Here be dragons!</header>
                <section class="modal-body">
                    <section>
                        <label for="title">Chart user script (JavaScript):</label>
                        <textarea id="uscript" name="uscript"
                            .value=${this.config.uscript || ''}
                            @change=${this.onUserScriptChange}
                        ></textarea>
                        📜 <a href="https://dan-danache.github.io/hubitat/watchtower-app/#chart-user-script" target="_blank">Learn more about chart user scripts</a>
                    </section>
                </section>
                <footer>
                    <button type="reset" @click=${() => this.showUserScript = false}>Get me outta here!</button>
                    <button type="submit">Set User Script</button>
                </footer>
            </form>
        `
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
        this.showUserScript = false
        this.config = {
            title: ''
        }
    }

    close() {
        this.resetForm()
        this.open = false
        this.showUserScript = false
    }

    submit(event) {
        event.preventDefault()
        const config = this.renderRoot.querySelector('.panel-config').decorateConfig({
            id: this.config.id,
            title: this.config.title,
            type: this.config.type,
            uscript: this.config.uscript,
        })
        Object.keys(config).forEach(key => config[key] === undefined && delete config[key])
        console.info('**** Adding panel with config', config)

        this.close()
        this.resetForm()
        this.dispatchEvent(new CustomEvent('done', {detail: config}))
    }

    submitUserScript(event) {
        event.preventDefault()
        const uscript = new FormData(event.target).get('uscript').trim() || undefined
        this.config = { ...this.config, uscript }
        this.showUserScript = false
    }
}
