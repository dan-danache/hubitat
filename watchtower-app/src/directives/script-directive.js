import { Directive, directive, noChange } from '../vendor/vendor.min.js';

/**
 const re = /<script\b[^>]*>[\s\S]*?<\/script\b[^>]*>/g
 const results = this.config.message.match(re)
 */
class ScriptDirective extends Directive {
    static previousValues = new WeakMap();

    static hashCode(s) {
        return [...s].reduce((hash, c) => (Math.imul(31, hash) + c.charCodeAt(0)) | 0, 0)
    }

    supdate(part) {
        console.log(part.parentNode)
        return noChange
    }

    render(values) {
        console.log('render', this)
        if (values == null) return

        values.forEach(value => {
            const hash = ScriptDirective.hashCode(value)
            const previousValue = ScriptDirective.previousValues.get(hash);
            if (previousValue !== undefined) return
        
            let range = document.createRange();
            let documentFragment = range.createContextualFragment(value);
            this.nt.options.host.appendChild(documentFragment);
    
            ScriptDirective.previousValues.set({ hash }, true)
        })
    }
}

// Create the directive function
export const script = directive(ScriptDirective);
