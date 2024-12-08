var defaultOptions = {
    callbacks: {
        afterZoom: function() {}
    }
}

export default {
    id: 'crosshair',

    afterInit: function(chart) {
        if (!chart.config.options.scales.x) return

        var xScaleType = chart.config.options.scales.x.type
        if (xScaleType !== 'linear' && xScaleType !== 'time' && xScaleType !== 'category' && xScaleType !== 'logarithmic') return
        if (chart.options.plugins.crosshair === undefined) chart.options.plugins.crosshair = defaultOptions

        chart.crosshair = {
            enabled: chart.options.plugins.crosshair?.enabled !== false,
            x: null,
            originalXRange: null,
            button: null,
            dragStarted: false,
            dragStartX: null,
            dragEndX: null,
            ignoreNextEvents: 0,
            canStartDrag: false,
            timeout: null,
            resetZoom: () => this.resetZoom(chart),
            panZoom: direction => this.panZoom(chart, direction)
        }

        if (!chart.crosshair.enabled) return

        // Listen to incoming sync messages
        window.addEventListener('crosshair', event => this.syncCrosshair(chart, event.detail))
    },

    syncCrosshair(chart, {id, time}) {

        // Ignore messages from myself
        if (chart.id === id) return

        var xScale = this.getXScale(chart)
        if (!xScale) return

        // Stop drawing crosshair
        if (time === null) {
            if (chart.crosshair.x === null) return
            chart.crosshair.x = null
            chart.draw()
            return
        }

        // Draw crosshair
        time = Math.min(Math.max(time, xScale.min), xScale.max)
        const lineX = Math.round(xScale.getPixelForValue(time))
        if (chart.crosshair.x == lineX) return
        chart.crosshair.x = lineX
        chart.draw()
    },

    panZoom: function(chart, direction) {

        // Chart is not zoomed in
        if (chart.crosshair.originalXRange === null) return
        const {min, max} = chart.crosshair.originalXRange
        const start = chart.options.scales.x.min
        const end = chart.options.scales.x.max

        // Short-circuit
        if (start === min && direction === 'left') return
        if (end === max && direction === 'right') return

        const diff = end - start
        const increment = diff / 10
        if (direction === 'left') {
            chart.options.scales.x.min = Math.max(start - increment, min)
            chart.options.scales.x.max = chart.options.scales.x.min + diff
        } else {
            chart.options.scales.x.max = Math.min(end + increment, max)
            chart.options.scales.x.min = chart.options.scales.x.max - diff
        }
        chart.update()
    },

    getOption: function(chart, category, name) {
        const retVal = chart.options.plugins.crosshair[category] ? chart.options.plugins.crosshair[category][name] : undefined
        return retVal !== undefined ? retVal : defaultOptions[category][name]
    },

    getXScale: function(chart) {
        return chart.data.datasets.length ? chart.scales[chart.getDatasetMeta(0).xAxisID] : null
    },

    getYScale: function(chart) {
        return chart.scales[chart.getDatasetMeta(0).yAxisID]
    },

    afterEvent: function(chart, args) {
        if (!chart.crosshair.enabled || chart.config.options.scales.x.length == 0) return false

        // var xScaleType = chart.config.options.scales.x.type
        // if (xScaleType !== 'linear' && xScaleType !== 'time' && xScaleType !== 'category' && xScaleType !== 'logarithmic') return false

        var xScale = this.getXScale(chart)
        if (!xScale) return false

        if (chart.crosshair.ignoreNextEvents > 0) {
            chart.crosshair.ignoreNextEvents -= 1
            return false
        }

        const minX = xScale.getPixelForValue(xScale.min)
        const maxX = xScale.getPixelForValue(xScale.max)

        const e = args.event
        e.x = Math.round(Math.min(Math.max(e.x, minX), maxX))
        let time = Math.round(xScale.getValueForPixel(e.x))

        // Enable drag on mobile phones on second quick touch
        if (e.type === 'click') {
            chart.crosshair.canStartDrag = true
            chart.crosshair.dragStarted = false
            clearTimeout(chart.crosshair.timeout)
            chart.crosshair.timeout = setTimeout(() => chart.crosshair.canStartDrag = false, 1000)
            return
        }

        // Start drag
        if (e.type === 'mousedown' && !chart.crosshair.dragStarted) {
            if (e.native.type === 'mousedown') chart.crosshair.canStartDrag = true
            if (!chart.crosshair.canStartDrag) return
            chart.crosshair.x = e.x
            chart.crosshair.dragStartX = e.x
            chart.crosshair.dragStarted = true
            chart.draw()
            return
        }

        // Stop drag
        if (chart.crosshair.dragStarted && e.type === 'mouseup') {
            chart.crosshair.dragStarted = false

            // This is actually a click; reset drag
            if (Math.abs(chart.crosshair.dragStartX - e.x) < 3) {
                chart.draw()
                return false
            }

            // Fix touch event
            if (isNaN(e.x) && e.native.type === 'touchend') {
                e.x = e.native?.changedTouches[0]?.clientX
                time = Math.round(xScale.getValueForPixel(e.x))
            }

            var start = xScale.getValueForPixel(chart.crosshair.dragStartX)
            this.doZoom(chart, start, time)

            // Sync zoom
            if (e.native.ctrlKey) setTimeout(() => Chart.helpers.each(Chart.instances, instance => {
                if (instance.id == chart.id) return
                this.doZoom(instance, start, time)
            }), 0)
            return
        }

        // Remove drag box and trace line if out of bounds
        if (e.type === 'mouseout') {
            chart.crosshair.x = null
            chart.crosshair.canStartDrag = false
            chart.crosshair.dragStarted = false
            chart.draw()
            window.dispatchEvent(new CustomEvent('crosshair', { detail: {id: chart.id, time: null }}))
            return
        }

        // Draw crosshair
        if (chart.crosshair.x == e.x) return false
        chart.crosshair.x = e.x
        chart.draw()

        // Sync other crosshairs
        window.dispatchEvent(new CustomEvent('crosshair', { detail: {id: chart.id, time }}))
    },

    afterDraw: function(chart) {
        if (!chart.crosshair?.enabled || chart.crosshair.x === null) return false

        if (chart.crosshair.dragStarted) this.drawZoombox(chart)
        else this.drawTraceLine(chart)

        return true
    },

    resetZoom: function(chart, sync = false) {
        if (chart.crosshair.originalXRange === null) return

        chart.options.scales.x.min = chart.crosshair.originalXRange.min
        chart.options.scales.x.max = chart.crosshair.originalXRange.max

        if (chart.crosshair.button && chart.crosshair.button.parentNode) {
            chart.crosshair.button.parentNode.removeChild(chart.crosshair.button)
            chart.crosshair.button = null
        }

        chart.update()
        this.getOption(chart, 'callbacks', 'afterZoom')({chart})

        // Sync other charts
        if (sync) setTimeout(() => Chart.helpers.each(Chart.instances, instance => {
            if (instance.id != chart.id) this.resetZoom(instance)
        }), 0)
    },

    doZoom: function(chart, start, end) {
        chart.crosshair.dragStarted = false
        if (start == end) return false

        // Store orginal bounds
        if (chart.crosshair.originalXRange === null) chart.crosshair.originalXRange = {
            min: chart.scales.x.min,
            max: chart.scales.x.max,
        }

        // Swap start/end if user dragged from right to left
        if (start > end) [start, end] = [end, start]

        // Check bounds
        start = Math.max(start, chart.crosshair.originalXRange.min)
        end = Math.min(end, chart.crosshair.originalXRange.max)
        if (start > end) return false

        // Add restore zoom button
        if (chart.crosshair.button == null) {
            var button = document.createElement('button')
            var buttonLabel = document.createTextNode('◀•••▶')
            button.appendChild(buttonLabel)
            button.className = 'reset-zoom'
            button.setAttribute('title', 'Reset Zoom')
            button.addEventListener('click', event => this.resetZoom(chart, event.ctrlKey))
            chart.canvas.parentNode.appendChild(button)
            chart.crosshair.button = button
        }

        // Update chart bounds
        chart.options.scales.x.min = start
        chart.options.scales.x.max = end
        chart.update()

        // Ignore next 2 events to prevent starting a new zoom action after updating the chart
        chart.crosshair.ignoreNextEvents = 2

        this.getOption(chart, 'callbacks', 'afterZoom')(start, end)
    },

    drawZoombox: function(chart) {
        chart.ctx.beginPath()
        chart.ctx.rect(
            chart.crosshair.dragStartX,
            chart.chartArea.top,
            chart.crosshair.x - chart.crosshair.dragStartX,
            chart.chartArea.bottom - chart.chartArea.top,
        )
        chart.ctx.lineWidth = 1
        chart.ctx.strokeStyle = '#48F'
        chart.ctx.fillStyle = 'rgba(66,133,244,0.2)'
        chart.ctx.fill()
        chart.ctx.fillStyle = ''
        chart.ctx.stroke()
        chart.ctx.closePath()
    },

    drawTraceLine: function(chart) {
        //console.log(`Chart #${chart.id}: drawTraceLine()`, chart.crosshair.x)
        if (chart.crosshair.x == null) return

        chart.ctx.beginPath()
        chart.ctx.setLineDash([])
        chart.ctx.moveTo(chart.crosshair.x, chart.chartArea.top)
        chart.ctx.lineWidth = 1
        chart.ctx.strokeStyle = '#dc322f'
        chart.ctx.lineTo(chart.crosshair.x, chart.chartArea.bottom)
        chart.ctx.stroke()
        chart.ctx.setLineDash([])
    }
}
