import { html, css, LitElement, nothing } from '../vendor/vendor.min.js';

export class DashboardGrid extends LitElement {
    static styles = css`
        .grid-stack{position:relative}.grid-stack-rtl{direction:ltr}.grid-stack-rtl>.grid-stack-item{direction:rtl}.grid-stack-placeholder>.placeholder-content{background-color:rgba(0,0,0,.1);margin:0;position:absolute;width:auto;z-index:0!important}.grid-stack>.grid-stack-item{position:absolute;padding:0}.grid-stack>.grid-stack-item>.grid-stack-item-content{margin:0;position:absolute;width:auto;overflow-x:hidden;overflow-y:auto}.grid-stack>.grid-stack-item.size-to-content:not(.size-to-content-max)>.grid-stack-item-content{overflow-y:hidden}.grid-stack-item>.ui-resizable-handle{position:absolute;font-size:.1px;display:block;-ms-touch-action:none;touch-action:none}.grid-stack-item.ui-resizable-autohide>.ui-resizable-handle,.grid-stack-item.ui-resizable-disabled>.ui-resizable-handle{display:none}.grid-stack-item>.ui-resizable-ne,.grid-stack-item>.ui-resizable-nw,.grid-stack-item>.ui-resizable-se,.grid-stack-item>.ui-resizable-sw{background-image:url('data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" fill="none" stroke="%23666" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" viewBox="0 0 20 20"><path d="m10 3 2 2H8l2-2v14l-2-2h4l-2 2"/></svg>');background-repeat:no-repeat;background-position:center}.grid-stack-item>.ui-resizable-ne{transform:translate(0,10px) rotate(45deg)}.grid-stack-item>.ui-resizable-sw{transform:rotate(45deg)}.grid-stack-item>.ui-resizable-nw{transform:translate(0,10px) rotate(-45deg)}.grid-stack-item>.ui-resizable-se{transform:rotate(-45deg)}.grid-stack-item>.ui-resizable-nw{cursor:nw-resize;width:20px;height:20px;top:0}.grid-stack-item>.ui-resizable-n{cursor:n-resize;height:10px;top:0;left:25px;right:25px}.grid-stack-item>.ui-resizable-ne{cursor:ne-resize;width:20px;height:20px;top:0}.grid-stack-item>.ui-resizable-e{cursor:e-resize;width:10px;top:15px;bottom:15px}.grid-stack-item>.ui-resizable-se{cursor:se-resize;width:20px;height:20px}.grid-stack-item>.ui-resizable-s{cursor:s-resize;height:10px;left:25px;bottom:0;right:25px}.grid-stack-item>.ui-resizable-sw{cursor:sw-resize;width:20px;height:20px}.grid-stack-item>.ui-resizable-w{cursor:w-resize;width:10px;top:15px;bottom:15px}.grid-stack-item.ui-draggable-dragging>.ui-resizable-handle{display:none!important}.grid-stack-item.ui-draggable-dragging{will-change:left,top;cursor:move}.grid-stack-item.ui-resizable-resizing{will-change:width,height}.ui-draggable-dragging,.ui-resizable-resizing{z-index:10000}.ui-draggable-dragging>.grid-stack-item-content,.ui-resizable-resizing>.grid-stack-item-content{box-shadow:1px 4px 6px rgba(0,0,0,.2);opacity:.8}.grid-stack-animate,.grid-stack-animate .grid-stack-item{transition:left .3s,top .3s,height .3s,width .3s}.grid-stack-animate .grid-stack-item.grid-stack-placeholder,.grid-stack-animate .grid-stack-item.ui-draggable-dragging,.grid-stack-animate .grid-stack-item.ui-resizable-resizing{transition:left 0s,top 0s,height 0s,width 0s}.grid-stack>.grid-stack-item[gs-y="0"]{top:0}.grid-stack>.grid-stack-item[gs-x="0"]{left:0}.gs-12>.grid-stack-item{width:8.333%}.gs-12>.grid-stack-item[gs-x="1"]{left:8.333%}.gs-12>.grid-stack-item[gs-w="2"]{width:16.667%}.gs-12>.grid-stack-item[gs-x="2"]{left:16.667%}.gs-12>.grid-stack-item[gs-w="3"]{width:25%}.gs-12>.grid-stack-item[gs-x="3"]{left:25%}.gs-12>.grid-stack-item[gs-w="4"]{width:33.333%}.gs-12>.grid-stack-item[gs-x="4"]{left:33.333%}.gs-12>.grid-stack-item[gs-w="5"]{width:41.667%}.gs-12>.grid-stack-item[gs-x="5"]{left:41.667%}.gs-12>.grid-stack-item[gs-w="6"]{width:50%}.gs-12>.grid-stack-item[gs-x="6"]{left:50%}.gs-12>.grid-stack-item[gs-w="7"]{width:58.333%}.gs-12>.grid-stack-item[gs-x="7"]{left:58.333%}.gs-12>.grid-stack-item[gs-w="8"]{width:66.667%}.gs-12>.grid-stack-item[gs-x="8"]{left:66.667%}.gs-12>.grid-stack-item[gs-w="9"]{width:75%}.gs-12>.grid-stack-item[gs-x="9"]{left:75%}.gs-12>.grid-stack-item[gs-w="10"]{width:83.333%}.gs-12>.grid-stack-item[gs-x="10"]{left:83.333%}.gs-12>.grid-stack-item[gs-w="11"]{width:91.667%}.gs-12>.grid-stack-item[gs-x="11"]{left:91.667%}.gs-12>.grid-stack-item[gs-w="12"]{width:100%}.gs-1>.grid-stack-item{width:100%}
        .gs-2>.grid-stack-item{width:50%}.gs-2>.grid-stack-item[gs-x="1"]{left:50%}.gs-2>.grid-stack-item[gs-w="2"]{width:100%}.gs-3>.grid-stack-item{width:33.333%}.gs-3>.grid-stack-item[gs-x="1"]{left:33.333%}.gs-3>.grid-stack-item[gs-w="2"]{width:66.667%}.gs-3>.grid-stack-item[gs-x="2"]{left:66.667%}.gs-3>.grid-stack-item[gs-w="3"]{width:100%}.gs-4>.grid-stack-item{width:25%}.gs-4>.grid-stack-item[gs-x="1"]{left:25%}.gs-4>.grid-stack-item[gs-w="2"]{width:50%}.gs-4>.grid-stack-item[gs-x="2"]{left:50%}.gs-4>.grid-stack-item[gs-w="3"]{width:75%}.gs-4>.grid-stack-item[gs-x="3"]{left:75%}.gs-4>.grid-stack-item[gs-w="4"]{width:100%}.gs-5>.grid-stack-item{width:20%}.gs-5>.grid-stack-item[gs-x="1"]{left:20%}.gs-5>.grid-stack-item[gs-w="2"]{width:40%}.gs-5>.grid-stack-item[gs-x="2"]{left:40%}.gs-5>.grid-stack-item[gs-w="3"]{width:60%}.gs-5>.grid-stack-item[gs-x="3"]{left:60%}.gs-5>.grid-stack-item[gs-w="4"]{width:80%}.gs-5>.grid-stack-item[gs-x="4"]{left:80%}.gs-5>.grid-stack-item[gs-w="5"]{width:100%}.gs-6>.grid-stack-item{width:16.667%}.gs-6>.grid-stack-item[gs-x="1"]{left:16.667%}.gs-6>.grid-stack-item[gs-w="2"]{width:33.333%}.gs-6>.grid-stack-item[gs-x="2"]{left:33.333%}.gs-6>.grid-stack-item[gs-w="3"]{width:50%}.gs-6>.grid-stack-item[gs-x="3"]{left:50%}.gs-6>.grid-stack-item[gs-w="4"]{width:66.667%}.gs-6>.grid-stack-item[gs-x="4"]{left:66.667%}.gs-6>.grid-stack-item[gs-w="5"]{width:83.333%}.gs-6>.grid-stack-item[gs-x="5"]{left:83.333%}.gs-6>.grid-stack-item[gs-w="6"]{width:100%}.gs-7>.grid-stack-item{width:14.286%}.gs-7>.grid-stack-item[gs-x="1"]{left:14.286%}.gs-7>.grid-stack-item[gs-w="2"]{width:28.571%}.gs-7>.grid-stack-item[gs-x="2"]{left:28.571%}.gs-7>.grid-stack-item[gs-w="3"]{width:42.857%}.gs-7>.grid-stack-item[gs-x="3"]{left:42.857%}.gs-7>.grid-stack-item[gs-w="4"]{width:57.143%}.gs-7>.grid-stack-item[gs-x="4"]{left:57.143%}.gs-7>.grid-stack-item[gs-w="5"]{width:71.429%}.gs-7>.grid-stack-item[gs-x="5"]{left:71.429%}.gs-7>.grid-stack-item[gs-w="6"]{width:85.714%}.gs-7>.grid-stack-item[gs-x="6"]{left:85.714%}.gs-7>.grid-stack-item[gs-w="7"]{width:100%}.gs-8>.grid-stack-item{width:12.5%}.gs-8>.grid-stack-item[gs-x="1"]{left:12.5%}.gs-8>.grid-stack-item[gs-w="2"]{width:25%}.gs-8>.grid-stack-item[gs-x="2"]{left:25%}.gs-8>.grid-stack-item[gs-w="3"]{width:37.5%}.gs-8>.grid-stack-item[gs-x="3"]{left:37.5%}.gs-8>.grid-stack-item[gs-w="4"]{width:50%}.gs-8>.grid-stack-item[gs-x="4"]{left:50%}.gs-8>.grid-stack-item[gs-w="5"]{width:62.5%}.gs-8>.grid-stack-item[gs-x="5"]{left:62.5%}.gs-8>.grid-stack-item[gs-w="6"]{width:75%}.gs-8>.grid-stack-item[gs-x="6"]{left:75%}.gs-8>.grid-stack-item[gs-w="7"]{width:87.5%}.gs-8>.grid-stack-item[gs-x="7"]{left:87.5%}.gs-8>.grid-stack-item[gs-w="8"]{width:100%}.gs-9>.grid-stack-item{width:11.111%}.gs-9>.grid-stack-item[gs-x="1"]{left:11.111%}.gs-9>.grid-stack-item[gs-w="2"]{width:22.222%}.gs-9>.grid-stack-item[gs-x="2"]{left:22.222%}.gs-9>.grid-stack-item[gs-w="3"]{width:33.333%}.gs-9>.grid-stack-item[gs-x="3"]{left:33.333%}.gs-9>.grid-stack-item[gs-w="4"]{width:44.444%}.gs-9>.grid-stack-item[gs-x="4"]{left:44.444%}.gs-9>.grid-stack-item[gs-w="5"]{width:55.556%}.gs-9>.grid-stack-item[gs-x="5"]{left:55.556%}.gs-9>.grid-stack-item[gs-w="6"]{width:66.667%}.gs-9>.grid-stack-item[gs-x="6"]{left:66.667%}.gs-9>.grid-stack-item[gs-w="7"]{width:77.778%}.gs-9>.grid-stack-item[gs-x="7"]{left:77.778%}.gs-9>.grid-stack-item[gs-w="8"]{width:88.889%}.gs-9>.grid-stack-item[gs-x="8"]{left:88.889%}.gs-9>.grid-stack-item[gs-w="9"]{width:100%}.gs-10>.grid-stack-item{width:10%}.gs-10>.grid-stack-item[gs-x="1"]{left:10%}.gs-10>.grid-stack-item[gs-w="2"]{width:20%}.gs-10>.grid-stack-item[gs-x="2"]{left:20%}.gs-10>.grid-stack-item[gs-w="3"]{width:30%}.gs-10>.grid-stack-item[gs-x="3"]{left:30%}.gs-10>.grid-stack-item[gs-w="4"]{width:40%}.gs-10>.grid-stack-item[gs-x="4"]{left:40%}.gs-10>.grid-stack-item[gs-w="5"]{width:50%}.gs-10>.grid-stack-item[gs-x="5"]{left:50%}.gs-10>.grid-stack-item[gs-w="6"]{width:60%}.gs-10>.grid-stack-item[gs-x="6"]{left:60%}.gs-10>.grid-stack-item[gs-w="7"]{width:70%}.gs-10>.grid-stack-item[gs-x="7"]{left:70%}.gs-10>.grid-stack-item[gs-w="8"]{width:80%}.gs-10>.grid-stack-item[gs-x="8"]{left:80%}.gs-10>.grid-stack-item[gs-w="9"]{width:90%}.gs-10>.grid-stack-item[gs-x="9"]{left:90%}.gs-10>.grid-stack-item[gs-w="10"]{width:100%}.gs-11>.grid-stack-item{width:9.091%}.gs-11>.grid-stack-item[gs-x="1"]{left:9.091%}.gs-11>.grid-stack-item[gs-w="2"]{width:18.182%}.gs-11>.grid-stack-item[gs-x="2"]{left:18.182%}.gs-11>.grid-stack-item[gs-w="3"]{width:27.273%}.gs-11>.grid-stack-item[gs-x="3"]{left:27.273%}.gs-11>.grid-stack-item[gs-w="4"]{width:36.364%}.gs-11>.grid-stack-item[gs-x="4"]{left:36.364%}.gs-11>.grid-stack-item[gs-w="5"]{width:45.455%}.gs-11>.grid-stack-item[gs-x="5"]{left:45.455%}.gs-11>.grid-stack-item[gs-w="6"]{width:54.545%}.gs-11>.grid-stack-item[gs-x="6"]{left:54.545%}.gs-11>.grid-stack-item[gs-w="7"]{width:63.636%}.gs-11>.grid-stack-item[gs-x="7"]{left:63.636%}.gs-11>.grid-stack-item[gs-w="8"]{width:72.727%}.gs-11>.grid-stack-item[gs-x="8"]{left:72.727%}.gs-11>.grid-stack-item[gs-w="9"]{width:81.818%}.gs-11>.grid-stack-item[gs-x="9"]{left:81.818%}.gs-11>.grid-stack-item[gs-w="10"]{width:90.909%}.gs-11>.grid-stack-item[gs-x="10"]{left:90.909%}.gs-11>.grid-stack-item[gs-w="11"]{width:100%}

        .grid-stack { min-height: 100% }
        .grid-stack-item-content {
            color: var(--text-color);
            border: 1px var(--border-color) solid;
            border-radius: 5px;
            box-shadow: 0 0 0.3em var(--shadow-color);
        }
        :host(.embedded) .grid-stack-item-content {
            box-shadow: none;
            border: none;
        }
        .panel-container {
            width: 100%;
            height: 100%;
            position: relative;
            background-color: var(--bg-color-darker);
        }
        .panel-title {
            position: absolute;
            top: 0;
            left: 50%;
            transform: translate(-50%, 0);
            background-color: transparent;
            text-align: center;
            padding: 0.2em 1em;
            overflow: hidden;
            white-space: nowrap;
            text-overflow: ellipsis;
            user-select: none;
        }
        .grid-stack:not([mobile-view]) .panel-container:hover .panel-title {
            cursor: move;
            background-color: var(--bg-color);
            box-shadow: 0 0 0.3em var(--shadow-color);
            border: 1px var(--border-color) solid;
            border-radius: 0 0 5px 5px;
            border-top: 0;
        }
        .grid-stack-item-removing .panel-container {
            background-color: rgba(255, 0, 0, 0.3);
        }
        .spinner:after {
            position: absolute;
            top: calc(50% - 32px);
            left: calc(50% - 32px);
            content: ' ';
            display: block;
            width: 64px;
            height: 64px;
            border-radius: 50%;
            border: 1px solid var(--text-color-darker);
            border-color: var(--text-color-darker) transparent var(--text-color-darker) transparent;
            animation: spinner 1.2s linear infinite;
        }
        @keyframes spinner {
            0% { transform: rotate(0deg) }
            100% { transform: rotate(360deg) }
        }
    `;

    static properties = {
        name: { type: String, reflect: true },
        mobileView: { type: Boolean, state: true }
    }

    render() {
        return html`
            <div class="grid-stack spinner" mobile-view=${this.mobileView ? 'true' : nothing}></div>
        `;
    }

    firstUpdated() {
        this.grid = GridStack.init({
            sizeToContent: false,
            margin: 5,
            animate: false,
            float: true,

            // Non-configurable options
            cellHeight: 206,
            column: 8,
            columnOpts: {
                layout: 'move', // moveScale move scale list compact none
                columnMax: 8,
                breakpointForWindow: true,

                // TODO: fiddle with these until it behaves nicely on mobile/tablet/laptop
                breakpoints: [
                    { w: 420, c: 1 },
                    { w: 800, c: 2 },
                    { w: 1024, c: 4 },
                    { w: 1366, c: 6 }
                ]
            },
            handle: '.panel-title',
            removable: true,
            alwaysShowResizeHandle: false
        }, this.renderRoot.querySelector('.grid-stack'))
    }

    async init(panels) {

        // Add panels
        if (panels.length === 0) {
            this.addPanel({
                type: 'text-panel',
                title: 'Quick Instructions',
                message: `
                    <b>😎 Welcome to your new dashboard!</b>
                    <ul>
                        <li>Press the <b>ESC</b> (Escape) key on your keyboard to show or hide the left menu.
                        <li>Click the <b>Add dashboard tile</b> button to add new tiles to your dashboard.
                        <li>Rearrange tiles by dragging their titles. Resize tiles by dragging the bottom-right corner.
                        <li>Remove tiles by dragging them outside the dashboard grid.
                        <li>Tiles cannot be edited; remove and add them again if needed.
                        <li>Remember to click the <b>Save dashboard</b> button when you're happy with the layout.
                    </ul>
                    For more information, refer to the <a href="https://dan-danache.github.io/hubitat/watchtower-app/" target="_blank">official documentation</a>.
                `
            }, 2, 2, 1, 0)
        } else {
            this.grid.batchUpdate(true)
            panels.forEach(panel => {
                this.addPanel(panel.config, panel.w, panel.h, panel.x, panel.y)
            })
            this.grid.batchUpdate(false)
        }

        // Remove spinner and init mobile view
        this.renderRoot.querySelector('.grid-stack').classList.remove('spinner')
    }

    applyMobileView(mobileView) {
        this.mobileView = mobileView
        this.renderRoot.querySelectorAll('.panel').forEach(panel => panel.mobileView = this.mobileView)
        this.grid.enableResize(!this.mobileView)
        this.grid.enableMove(!this.mobileView)
        setTimeout(() => this.grid.setAnimation(!this.mobileView), 1000)
    }

    setRefreshInterval(refreshMinutes) {
        this.interval && clearInterval(this.interval)
        if (refreshMinutes == 0) return
        this.interval = setInterval(() => {
            this.renderRoot.querySelectorAll('.panel').forEach(panel => panel.refresh())
        }, refreshMinutes * 60 * 1000)
        console.info(`Setting auto-refresh timer for ${refreshMinutes} minutes`)
    }

    setYScale(yScale) {
        this.renderRoot.querySelectorAll('device-panel, attribute-panel').forEach(panel => panel.setYScale(yScale))
    }

    addPanel(config, w = 2, h = 1, x = undefined, y = undefined) {
        const content = `
            <div class="panel-container">
                <${config.type} config='${JSON.stringify(config).replace(/'/g, '&apos;')}' class="panel empty spinner"></${config.type}>
                <div class="panel-title">${config.title || '&nbsp' }</div>
            </div>
        `
        this.grid.addWidget({w, h, x, y, config, content})
    }

    compact() {
        this.grid.compact()
    }

    getPanelsConfig() {
        return this.grid.engine.nodes.map(node => {
            const panelElm = node.el.querySelector('.panel')
            return {
                w: node.w,
                h: node.h,
                x: node.x,
                y: node.y,
                config: panelElm.decorateConfig(node.config)
            }
        })
    }
}
