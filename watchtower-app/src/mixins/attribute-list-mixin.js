import { html, nothing } from '../vendor/vendor.min.js';

import { DatastoreHelper } from '../helpers/datastore-helper.js';
import { UiHelper } from '../helpers/ui-helper.js'

export const AttributeListMixin = (superClass) => {
    class AttributeListMixinImplementation extends superClass {
        static properties = {
            devices: { type: Object, state: true },
            supportedAttributes: { type: Object, state: true },
            attributes: { type: Object, state: true },
            dev: { type: Number, state: true },
            add: { type: Boolean, state: true },
        }

        constructor() {
          super()
          this.devices = undefined
          this.supportedAttributes = undefined
          this.attributes = undefined
          this.dev = undefined
          this.add = true
          this.grid = undefined
        }

        render() {
            return html`
                <fieldset class="${this.config.ds?.length === 0 ? 'empty' : nothing}">
                    <section>
                        <div class="grid-stack"
                            @item-remove=${this.onItemRemove}
                        ></div>
                    </section>
                </fieldset>
                ${(this.devices) ? this.renderDatasets() : ''}
                <fieldset>
                    <section>
                        ${this.add ? html`
                            <label for="dev">Select device:</label>
                            ${(this.devices && this.supportedAttributes) ? this.renderDevicesSelect() : html`<aside class="spinner">Loading devices ...</aside>`}
                            ${this.attributes ? this.renderAttributesSelect() : '' }
                        ` : html`
                            <button type="button" @click=${this.addAnother}>Add another device attribute</button>
                        `}
                    </section>
                </fieldset>
            `
        }

        renderDatasets() {
            if (!this.grid) return
            this.grid.removeAll()
            if (!this.config.ds?.length) return

            // Add grid widgets
            this.grid.batchUpdate(true)
            this.config.ds.map(dataset => {
                const text = `${this.devices.find(it => it.id == dataset.dev).name} ${UiHelper.prettyName(dataset.attr)}`
                const content = `
                    <div class="item-move" title="Drag to re-order">☰</div>
                    <div class="item-text" title="${text}">${text}</div>
                    <div class="item-remove" title="Click to remove"
                        onClick="this.dispatchEvent(new CustomEvent('item-remove', {bubbles: true, detail: {dev: ${dataset.dev}, attr: '${dataset.attr}'}}))"
                    >✖</div>
                `
                this.grid.addWidget({noResize:true, content, dev:dataset.dev, attr:dataset.attr})
            });
            this.grid.batchUpdate(false)
        }

        renderDevicesSelect() {
            return html`
                <section>
                    <select id="dev" @change=${this.onDeviceSelect} .required=${!this.config.ds?.length}>
                        <option></option>
                        ${this.devices.map(device => html`
                            <option value="${device.id}">${device.name}</option>
                        `
                        )}
                    </select>
                </section>
            `
        }
    
        renderAttributesSelect() {
            return html`
                <section>
                    <label for="attr">Select attribute to chart:</label>
                    <select id="attr" @change=${this.onAttributeSelect} .required=${!this.config.ds?.length}>
                        <option value=""></option>
                        ${this.attributes.map(attr => html`<option value="${attr}" .disabled=${!this.canSelectAttribute(attr)}>${attr}</option>`)}
                    </select>
                </section>
            `
        }

        async connectedCallback() {
            super.connectedCallback()
            this.devices = await DatastoreHelper.fetchMonitoredDevices()
            this.supportedAttributes = await DatastoreHelper.fetchSupportedAttributes()

            if (this.config.ds === undefined) this.config = { ...this.config, ds: []}
            this.add = !this.config.ds.length
        }

        firstUpdated() {
            this.grid = GridStack.init({
                column: 1,
                cellHeight: 30,
                margin: 0,
                handle: '.item-move',
                removable: false,
            }, this.renderRoot.querySelector('.grid-stack'))
            this.grid.on('change', () => {
                this.config = { ...this.config,
                    ds: this.grid.engine.nodes.map(node => { return {dev: node.dev, attr: node.attr} })
                }
            })
        }

        addAnother() {
            this.add = true
            this.focus('#dev')
        }
    
        canSelectAttribute(attr) {
            return this.config.ds.find(it => it.dev == this.dev && it.attr == attr) === undefined
        }
    
        onItemRemove(event) {
            const {dev, attr} = event.detail
            this.updateConfig({
                ds: this.config.ds.filter(dataset => dataset.dev !== dev || dataset.attr !== attr)
            })
            if (!this.config.ds?.length) this.addAnother()
        }
    
        onDeviceSelect(event) {
            this.dev = event.target.value !== '' ? event.target.value : undefined
            if (this.dev === undefined) {
                this.attributes = undefined
                return
            }
    
            const device = this.devices.find(device => device.id == this.dev)
            this.attributes = device.attrs.sort()
            this.focus('#attr')
        }
    
        onAttributeSelect(event) {
            const attr = event.target.value !== '' ? event.target.value : undefined
            if (attr === undefined) return
    
            const ds = this.config.ds || []
            this.updateConfig({
                ds: this.config.ds.concat({
                    dev: parseInt(this.dev),
                    attr,
                })
            })
    
            // Reset add inputs
            this.dev = undefined
            this.attributes = undefined
            this.add = false
        }

        updateConfig(props) {
            this.config = {...this.config, ...props}
        }
    
        focus(querySelector) {
            setTimeout(() => {
                const element = this.renderRoot.querySelector(querySelector)
                if (!element) return
                element.scrollIntoView({behavior: 'smooth', block: 'center'});
            }, 0)
        }

        decorateConfig(config) {
            return { ...config,
                precision: '5m',
                ds: this.config.ds,
            }
        }
    }
    return AttributeListMixinImplementation;
};
