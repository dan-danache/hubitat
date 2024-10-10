import '../vendor/vendor.min.js'
import { ColorHelper } from './color-helper.js'
//import ToggleScaleVisibilityPlugin from '../plugins/toggle-scale-visibility.js'
import CrosshairPlugin from '../plugins/crosshair.js'

// Register custom plugins
//Chart.register(ToggleScaleVisibilityPlugin)
Chart.register(CrosshairPlugin)

const positioners = Chart.Tooltip.positioners
positioners.custom = function(elements, eventPosition) {
    const pos = positioners.nearest(elements, eventPosition)
    if (pos === false) return false
    const chartArea = this.chart.chartArea
    if (pos.y < chartArea.top + 25) { pos.yAlign = 'top'; pos.xAlign = pos.x < (chartArea.left + chartArea.right) / 2 ? 'left' : 'right' }
    else if (pos.y > chartArea.bottom - 25) { pos.yAlign = 'bottom'; pos.xAlign = pos.x < (chartArea.left + chartArea.right) / 2 ? 'left' : 'right' }
    return pos
}

export class ChartHelper {
    static defaultConfig(mobileView) {
        const colors = ColorHelper.colors()
        return {
            type: 'line',
            options: {
                events: ['mousedown', 'mousemove', 'mouseup', 'mouseout', 'click', 'touchstart', 'touchmove', 'touchend'],
                parsing: false,
                normalized: true,
                responsive: true,
                maintainAspectRatio: false,
                onResize: chart => ChartHelper.updateChartType(chart),
                animation: { duration: 0, onComplete: ({ initial, chart }) => (initial ? ChartHelper.updateChartType(chart) : undefined) },
                layout: { padding: { top: 25, bottom: 4 }},
                stacked: false,
                pointStyle: false,
                scales: {
                    x: {
                        type: 'time',
                        time: {
                            minUnit: 'minute',
                            displayFormats: {
                                minute: 'd LLL HH:mm',
                                hour: 'd LLL HH:mm',
                                day: 'd LLL'
                            },
                            tooltipFormat: 'd LLL HH:mm'
                        },
                        title: { display: false },
                        ticks: {
                            color: colors.TextColorDarker,
                            maxRotation: 0,
                            autoSkipPadding: 15
                        },
                        grid: { color: colors.TextColorDarker + '44' }
                    }
                },
                interaction: {
                    mode: 'nearest',
                    axis: 'x',
                    intersect: false
                },
                plugins: {
                    legend: { display: false },
                    tooltip: {
                        itemSort: (a, b) => b.raw.y - a.raw.y,
                        callbacks: { label: t => ` ${t.dataset.label}: ${t.parsed.y}${t.dataset.unit}` },
                        backgroundColor: colors.BgColorDarker,
                        titleColor: colors.TextColor,
                        bodyColor: colors.TextColorDarker,
                        borderColor: colors.BorderColor,
                        borderWidth: 1,
                        position: 'custom',
                        //mode: 'interpolate',
                        intersect: false,
                    },
                    crosshair: {
                        line: {
                            color: colors.Red,
                            width: 1
                        },
                        zoom: { zoomButtonText: '◀•••▶' },
                        callbacks: { afterZoom: start => this.updateChartType(start.chart) }
                    }
                }
            }
        }
    }

    static updateChartType(chart) {
        return this.updatePointStyle(chart)
        //console.log('1. updateChartType')
        if (chart.scales.x === undefined || chart.data.datasets[0] === undefined) return
        const min = chart.scales.x.min
        const max = chart.scales.x.max
        const data = chart.data.datasets[0].data
        const visibleDatapoints = data.filter(point => point.x >= min && point.x <= max).length
        //console.log('2. visibleDatapoints', visibleDatapoints)

        const chartType = chart.width / visibleDatapoints > 30 ? 'bar' : 'line'
        //console.log('3. chartType', chartType)
        chart.options.scales.x.offset = chartType == 'bar'
        if (chart.config.type != chartType) {
            chart.config.type = chartType
            //console.log('4. updating')
            chart.update('none')
        }
    }

    static updatePointStyle(chart) {
        if (chart.scales.x === undefined || chart.data.datasets[0] === undefined) return
        const min = chart.scales.x.min
        const max = chart.scales.x.max
        const data = chart.data.datasets[0].data
        const visibleDatapoints = data.filter(point => point.x >= min && point.x <= max).length

        const pointStyle = chart.width / visibleDatapoints > 30 ? 'circle' : false
        if (chart.data.datasets[0].pointStyle !== pointStyle) {
            chart.data.datasets.forEach(dataset => dataset.pointStyle = pointStyle)
            chart.update()
        }
    }

    static prettyName(camelCase) {
        return (camelCase[0].toUpperCase() + camelCase.slice(1))
            .replace(/([A-Z])(?=[A-Z][a-z])|([a-z])(?=[A-Z])/g, '$& ')
            .replace('Hub ', '')
    }

    static setupZoomPan(canvas, chart) {
        const old = chart.crosshair.reset
        chart.crosshair.reset = () => {
            console.log('reset() called')
            old()
        }
        canvas.addEventListener('keydown', event => {
            if (event.keyCode === 37) chart.crosshair.panZoom('left')
            else if(event.keyCode === 39) chart.crosshair.panZoom('right')
        })
    }
}
