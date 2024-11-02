var defaultOptions = {
    line: {
        color: '#F66',
        width: 1,
        dashPattern: []
    },
    zoom: {
        enabled: true,
        zoomboxBackgroundColor: 'rgba(66,133,244,0.2)',
        zoomboxBorderColor: '#48F',
        zoomButtonText: 'Reset Zoom',
        zoomButtonClass: 'reset-zoom',
    },
    snap: {
        enabled: true,
    },
    callbacks: {
        afterZoom: function(start, end) {}
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
            x: null,
            originalXRange: null,
            button: null,
            dragStarted: false,
            dragStartX: null,
            dragEndX: null,
            suppressTooltips: false,
            ignoreNextEvents: 0,
            canStartDrag: false,
            timeout: null,
            resetZoom: () => this.resetZoom(chart),
            panZoom: direction => this.panZoom(chart, direction)
        }
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

    afterEvent: function(chart, event) {
        if (chart.config.options.scales.x.length == 0) return

        var xScaleType = chart.config.options.scales.x.type
        if (xScaleType !== 'linear' && xScaleType !== 'time' && xScaleType !== 'category' && xScaleType !== 'logarithmic') return

        var xScale = this.getXScale(chart)
        if (!xScale) return

        if (chart.crosshair.ignoreNextEvents > 0) {
            chart.crosshair.ignoreNextEvents -= 1
            return
        }

        const minX = xScale.getPixelForValue(xScale.min)
        const maxX = xScale.getPixelForValue(xScale.max)

        let e = event.event
        e.x = Math.min(Math.max(e.x, minX), maxX)
        //console.log('event', chart.crosshair.dragStarted, chart.crosshair.canStartDrag, e)

        // Suppress tooltips for linked charts
        chart.crosshair.enabled = true //(e.type !== 'mouseout' && (e.x > xScale.getPixelForValue(xScale.min) && e.x < xScale.getPixelForValue(xScale.max)))

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
            return
        }

        // Remove drag box and trace line if out of bounds
        if (e.type === 'mouseout') {
            chart.crosshair.enabled = false
            chart.crosshair.canStartDrag = false
            chart.crosshair.dragStarted = false
            return
        }

        // Stop drag
        if (chart.crosshair.dragStarted && e.type === 'mouseup') {
            chart.crosshair.dragStarted = false

            var start = xScale.getValueForPixel(chart.crosshair.dragStartX)
            var end = xScale.getValueForPixel(chart.crosshair.x)
            this.doZoom(chart, start, end)
        }

        chart.crosshair.x = e.x
        chart.draw()
    },

    afterDraw: function(chart) {
        if (!chart.crosshair.enabled) return
        if (chart.crosshair.dragStarted) this.drawZoombox(chart)
        else this.drawTraceLine(chart)
        return true
    },

    beforeTooltipDraw: function(chart) {

        // Suppress tooltips on dragging
        return !chart.crosshair.dragStarted && !chart.crosshair.suppressTooltips
    },

    resetZoom: function(chart) {
        if (chart.crosshair.originalXRange === null) return
        chart.options.scales.x.min = chart.crosshair.originalXRange.x
        chart.options.scales.x.max = chart.crosshair.originalXRange.y

        if (chart.crosshair.button && chart.crosshair.button.parentNode) {
            chart.crosshair.button.parentNode.removeChild(chart.crosshair.button)
            chart.crosshair.button = null
        }

        chart.update()
        this.getOption(chart, 'callbacks', 'afterZoom')({chart})
    },

    doZoom: function(chart, start, end) {
        chart.crosshair.dragStarted = false
        if (start == end) return

        // Store orginal bounds
        if (chart.crosshair.originalXRange === null) chart.crosshair.originalXRange = {
            min: chart.scales.x.min,
            max: chart.scales.x.max,
        }

        // Swap start/end if user dragged from right to left
        if (start > end) {
            var tmp = start
            start = end
            end = tmp
        }

        // Check bounds
        start = Math.max(start, chart.crosshair.originalXRange.min)
        end = Math.min(end, chart.crosshair.originalXRange.max)

        // Add restore zoom button
        if (chart.crosshair.button == null) {
            var button = document.createElement('button')

            var buttonText = this.getOption(chart, 'zoom', 'zoomButtonText')
            var buttonClass = this.getOption(chart, 'zoom', 'zoomButtonClass')

            var buttonLabel = document.createTextNode(buttonText)
            button.appendChild(buttonLabel)
            button.className = buttonClass
            button.setAttribute('title', 'Reset Zoom')
            button.addEventListener('click', () => this.resetZoom(chart))
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
        var yScale = this.getYScale(chart)

        var borderColor = this.getOption(chart, 'zoom', 'zoomboxBorderColor')
        var fillColor = this.getOption(chart, 'zoom', 'zoomboxBackgroundColor')

        chart.ctx.beginPath()
        chart.ctx.rect(
            chart.crosshair.dragStartX,
            yScale.getPixelForValue(yScale.max),
            chart.crosshair.x - chart.crosshair.dragStartX,
            yScale.getPixelForValue(yScale.min) - yScale.getPixelForValue(yScale.max)
        )
        chart.ctx.lineWidth = 1
        chart.ctx.strokeStyle = borderColor
        chart.ctx.fillStyle = fillColor
        chart.ctx.fill()
        chart.ctx.fillStyle = ''
        chart.ctx.stroke()
        chart.ctx.closePath()
    },

    drawTraceLine: function(chart) {
        var yScale = this.getYScale(chart)
        var lineWidth = this.getOption(chart, 'line', 'width')
        var color = this.getOption(chart, 'line', 'color')
        var dashPattern = this.getOption(chart, 'line', 'dashPattern')
        var snapEnabled = this.getOption(chart, 'snap', 'enabled')

        var lineX = chart.crosshair.x
        if (snapEnabled && chart._active.length) lineX = chart._active[0].element.x

        chart.ctx.beginPath()
        chart.ctx.setLineDash(dashPattern)
        chart.ctx.moveTo(lineX, yScale.getPixelForValue(yScale.max))
        chart.ctx.lineWidth = lineWidth
        chart.ctx.strokeStyle = color
        chart.ctx.lineTo(lineX, yScale.getPixelForValue(yScale.min))
        chart.ctx.stroke()
        chart.ctx.setLineDash([])
    }
}
