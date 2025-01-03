import { html, css, LitElement } from '../vendor/vendor.min.js';

export const precisions = {
    '5m': { amount: 5, unit: 'minute', description: 'View data with 5 minutes precision' },
    '1h': { amount: 1, unit: 'hour', description: 'View data with 1 hour precision' },
    '1d': { amount: 1, unit: 'day', description: 'View data with 1 day precision' },
    '1w': { amount: 1, unit: 'week', description: 'View data with 1 week precision' },
}

export class PrecisionSelector extends LitElement {
    static styles = css`
        ul {
            margin: 0;
            padding: 0;
            list-style: none;
            background-color: var(--bg-color);
            color: var(--text-color);
            border: 1px var(--border-color) solid;
            border-bottom: 0;
            box-shadow: 0 0 5px rgba(0, 0, 0, 0.3);
            border-radius: 5px 5px 0 0;
            display: flex;
            user-select: none;
        }
        ul li {
            list-style-type: none;
            display: inline-block;
            margin: 0;
            padding: .4em 0;
            border-right: 1px var(--border-color) solid;
            cursor: pointer;
            width: 2.85em;
            text-align: center;
        }
        ul li:first-child { border-radius: 4px 0 0 0 }
        ul li:last-child { border: 0; border-radius: 0 4px 0 0 }
        ul li:hover {
            background-color: var(--bg-color-darker);
        }
        ul li[data-selected] {
            background-color: var(--Blue);
            color: var(--Base3);
        }
        .ui-resizable-handle { opacity:0 }
    `;

    static properties = {
        precision: { type: Object, state: true },
    }

    render() {
        return html`
            <nav><ul>
                ${Object.entries(precisions).map(([key, val]) => html`<li @click=${this.selectPrecision} ?data-selected=${this.precision == key} ?inert=${this.precision == key} title="${val.description}">${key}</li>`)}
            </ul></nav>
        `;
    }

    selectPrecision(event) {
        this.precision = event.target.textContent
        this.dispatchEvent(new CustomEvent('change', {detail: this.precision}))
    }
}
