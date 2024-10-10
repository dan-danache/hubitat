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
            originalData: [],
            originalXRange: {},
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
        if (chart.crosshair.originalData.length === 0) return
        const diff = chart.crosshair.end - chart.crosshair.start
        var min = chart.crosshair.min
        var max = chart.crosshair.max

        // Short-circuit
        if (chart.crosshair.start === min && direction === 'left') return
        if (chart.crosshair.end === max && direction === 'right') return

        const increment = Math.round(diff / 10)
        if (direction === 'left') {
            chart.crosshair.start = Math.max(chart.crosshair.start - increment, min)
            chart.crosshair.end = chart.crosshair.start === min ? min + diff : chart.crosshair.end - increment
        } else {
            chart.crosshair.end = Math.min(chart.crosshair.end + increment, chart.crosshair.max)
            chart.crosshair.start = chart.crosshair.end === max ? max - diff : chart.crosshair.start + increment
        }
        this.doZoom(chart, chart.crosshair.start, chart.crosshair.end)
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

        let e = event.event
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

        // Remove drag if out of bounds
        if (chart.crosshair.dragStarted && e.type === 'mouseout') {
            console.log('mouseout -> dragStarted = false')
            chart.crosshair.canStartDrag = false
            chart.crosshair.dragStarted = false
            return
        }

        // Stop drag
        if (chart.crosshair.dragStarted && e.type === 'mouseup') {
            chart.crosshair.dragStarted = false

            var start = xScale.getValueForPixel(chart.crosshair.dragStartX)
            var end = xScale.getValueForPixel(chart.crosshair.x)

            if (Math.abs(chart.crosshair.dragStartX - chart.crosshair.x) > 1) {
                this.doZoom(chart, start, end)
            }
            chart.update('none')
        }

        chart.crosshair.x = Math.min(Math.max(e.x, xScale.getPixelForValue(xScale.min)), xScale.getPixelForValue(xScale.max))
        chart.draw()
    },

    afterDraw: function(chart) {
        if (!chart.crosshair.enabled) return
        console.log('afterDraw', chart.crosshair.dragStarted)
        if (chart.crosshair.dragStarted) this.drawZoombox(chart)
        else this.drawTraceLine(chart)

        return true
    },

    beforeTooltipDraw: function(chart) {

        // Suppress tooltips on dragging
        return !chart.crosshair.dragStarted && !chart.crosshair.suppressTooltips
    },

    resetZoom: function(chart) {
        if (chart.crosshair.originalData.length > 0) {

            // Reset original data
            for (var datasetIndex = 0; datasetIndex < chart.data.datasets.length; datasetIndex++) {
                var dataset = chart.data.datasets[datasetIndex]
                dataset.data = chart.crosshair.originalData.shift(0)
            }
        }

        // Reset original xRange
        if (chart.crosshair.originalXRange.min) {
            chart.options.scales.x.min = chart.crosshair.originalXRange.min
            chart.crosshair.originalXRange.min = null
        } else {
            delete chart.options.scales.x.min
        }
        if (chart.crosshair.originalXRange.max) {
            chart.options.scales.x.max = chart.crosshair.originalXRange.max
            chart.crosshair.originalXRange.max = null
        } else {
            delete chart.options.scales.x.max
        }

        if (chart.crosshair.button && chart.crosshair.button.parentNode) {
            chart.crosshair.button.parentNode.removeChild(chart.crosshair.button)
            chart.crosshair.button = false
        }

        chart.update('none')
        this.getOption(chart, 'callbacks', 'afterZoom')({chart})
    },

    doZoom: function(chart, start, end) {
        console.log('doZoom', start, end, chart.options.scales.x.min, chart.options.scales.x.max)

        // Swap start/end if user dragged from right to left
        if (start > end) {
            var tmp = start
            start = end
            end = tmp
        }

        chart.crosshair.dragStarted = false

        if (chart.options.scales.x.min && chart.crosshair.originalData.length === 0) {
            chart.crosshair.originalXRange.min = chart.options.scales.x.min
        }
        if (chart.options.scales.x.max && chart.crosshair.originalData.length === 0) {
            chart.crosshair.originalXRange.max = chart.options.scales.x.max
        }

        // Add restore zoom button
        if (!chart.crosshair.button) {
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

        // Set axis scale
        chart.options.scales.x.min = start
        chart.options.scales.x.max = end

        // Make a copy of the original data for later restoration
        var storeOriginals = (chart.crosshair.originalData.length === 0) ? true : false
        var filterDataset = (chart.config.options.scales.x.type !== 'category')

        if (filterDataset) {
            for (var datasetIndex = 0; datasetIndex < chart.data.datasets.length; datasetIndex++) {
                var newData = []
                var index = 0
                var started = false
                var stop = false
                if (storeOriginals) chart.crosshair.originalData[datasetIndex] = chart.data.datasets[datasetIndex].data
                var sourceDataset = chart.crosshair.originalData[datasetIndex]

                for (var oldDataIndex = 0; oldDataIndex < sourceDataset.length; oldDataIndex++) {

                    var oldData = sourceDataset[oldDataIndex]
                    // var oldDataX = this.getXScale(chart).getRightValue(oldData)
                    var oldDataX = oldData.x !== undefined ? oldData.x : NaN

                    // Append one value outside of bounds
                    if (oldDataX >= start && !started && index > 0) {
                        newData.push(sourceDataset[index - 1])
                        started = true
                    }
                    if (oldDataX >= start && oldDataX <= end) {
                        newData.push(oldData)
                    }
                    if (oldDataX > end && !stop && index < sourceDataset.length) {
                        newData.push(oldData)
                        stop = true
                    }
                    index += 1
                }

                chart.data.datasets[datasetIndex].data = newData
            }
        }

        chart.crosshair.start = Math.round(start)
        chart.crosshair.end = Math.round(end)

        if (storeOriginals) {
            var xAxes = this.getXScale(chart)
            chart.crosshair.min = xAxes.min
            chart.crosshair.max = xAxes.max
        }

        chart.crosshair.ignoreNextEvents = 2 // ignore next 2 events to prevent starting a new zoom action after updating the chart

        chart.update()
        this.getOption(chart, 'callbacks', 'afterZoom')(start, end)
    },

    drawZoombox: function(chart) {
        var yScale = this.getYScale(chart)

        var borderColor = this.getOption(chart, 'zoom', 'zoomboxBorderColor')
        var fillColor = this.getOption(chart, 'zoom', 'zoomboxBackgroundColor')

        chart.ctx.beginPath()
        console.log('drawZoombox',
            chart.crosshair.dragStartX,
            yScale.getPixelForValue(yScale.max),
            chart.crosshair.x - chart.crosshair.dragStartX,
            yScale.getPixelForValue(yScale.min) - yScale.getPixelForValue(yScale.max)
        )
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
