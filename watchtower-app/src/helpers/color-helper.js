export class ColorHelper {
    static colors() {
        const css = getComputedStyle(document.documentElement)
        return {
            BgColor: css.getPropertyValue('--bg-color'),
            BgColorDarker: css.getPropertyValue('--bg-color-darker'),
            TextColor: css.getPropertyValue('--text-color'),
            TextColorDarker: css.getPropertyValue('--text-color-darker'),
            BorderColor: css.getPropertyValue('--border-color'),
            Yellow: css.getPropertyValue('--Yellow'),
            Orange: css.getPropertyValue('--Orange'),
            Red: css.getPropertyValue('--Red'),
            Magenta: css.getPropertyValue('--Magenta'),
            Violet: css.getPropertyValue('--Violet'),
            Blue: css.getPropertyValue('--Blue'),
            Cyan: css.getPropertyValue('--Cyan'),
            Green: css.getPropertyValue('--Green'),
            Gray: css.getPropertyValue('--Gray')
        }
    }

    static {
        // These are fixed and don't depend on the current theme
        const css = getComputedStyle(document.documentElement)
        ColorHelper.chartColors = [
            css.getPropertyValue('--Red'),
            css.getPropertyValue('--Blue'),
            //css.getPropertyValue('--Cyan'),
            //css.getPropertyValue('--Yellow'),
            //css.getPropertyValue('--Green'),
            //css.getPropertyValue('--Orange'),
            //css.getPropertyValue('--Magenta'),
            //css.getPropertyValue('--Gray')
        ]
    }
}
