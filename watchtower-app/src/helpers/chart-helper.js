import '../vendor/vendor.min.js'
import { UiHelper } from './ui-helper.js'
import CrosshairPlugin from '../plugins/crosshair-plugin.js'
import { precisions } from '../panels/precision-selector.js'

// Register custom plugins
Chart.register(CrosshairPlugin)

// Tooltip positioners
const positioners = Chart.Tooltip.positioners
positioners.custom = function(elements, eventPosition) {
    const pos = positioners.nearest(elements, eventPosition)
    if (pos === false) return false
    const chartArea = this.chart.chartArea
    if (pos.y < chartArea.top + 25) { pos.yAlign = 'top'; pos.xAlign = pos.x < (chartArea.left + chartArea.right) / 2 ? 'left' : 'right' }
    else if (pos.y > chartArea.bottom - 25) { pos.yAlign = 'bottom'; pos.xAlign = pos.x < (chartArea.left + chartArea.right) / 2 ? 'left' : 'right' }
    return pos
}
positioners.mouse = function (elements, eventPosition) {
    if (!elements?.length) return false
    const pos =  eventPosition
    const chartArea = this.chart.chartArea
    if (pos.y < chartArea.top + 25) { pos.yAlign = 'top'; pos.xAlign = pos.x < (chartArea.left + chartArea.right) / 2 ? 'left' : 'right' }
    else if (pos.y > chartArea.bottom - 25) { pos.yAlign = 'bottom'; pos.xAlign = pos.x < (chartArea.left + chartArea.right) / 2 ? 'left' : 'right' }
    return pos
}

const adapter = Chart._adapters._date.prototype
function lineTooltipTitle(time, precision, formats) {
    if (precision === undefined) return adapter.format(time, 'd LLL HH:mm')
    return `${adapter.format(adapter.add(time, 0 - precision.amount, precision.unit), formats[precision.unit])} - ${adapter.format(time, formats[precision.unit])}`
}
function statusmapTooltipTitle(times, precision, formats) {
    return `${adapter.format(times[0], formats[precision.unit])} - ${adapter.format(times[1], formats[precision.unit])}`
}
export class ChartHelper {
    static lineConfig() {
        const colors = UiHelper.colors()
        return {
            type: 'line',
            options: {
                events: ['click', 'mousedown', 'mousemove', 'mouseup', 'mouseout', 'touchstart', 'touchmove', 'touchend'],
                parsing: false,
                normalized: true,
                responsive: true,
                maintainAspectRatio: false,
                onResize: chart => ChartHelper.updateChartType(chart),
                animation: { duration: 500, onComplete: ({ initial, chart }) => (initial ? ChartHelper.updateChartType(chart) : undefined) },
                layout: { padding: { top: 25, bottom: 4, right: 10 }},
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
                                day: 'd LLL',
                                week: 'd LLL'
                            }
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
                        callbacks: {
                            title: context => lineTooltipTitle(context[0].parsed.x, precisions[context[0].chart.precision], context[0].chart.options.scales.x.time.displayFormats),
                            label: context => ` ${context.dataset.label}: ${context.parsed.y}${context.dataset.unit}`,
                        },
                        backgroundColor: colors.BgColorDarker,
                        titleColor: colors.TextColor,
                        bodyColor: colors.TextColorDarker,
                        borderColor: colors.BorderColor,
                        borderWidth: 1,
                        position: 'custom',
                        intersect: false,
                    },
                    crosshair: {
                        line: {
                            color: colors.Red,
                            width: 1,
                        },
                        zoom: { zoomButtonText: '◀•••▶' },
                        callbacks: { afterZoom: start => this.updateChartType(start.chart) },
                    },
                }
            }
        }
    }

    static sparklineConfig() {
        const colors = UiHelper.colors()
        return {
            type: 'line',
            options: {
                normalized: true,
                responsive: true,
                maintainAspectRatio: false,
                layout: { padding: { top: 2 }, autoPadding: false},
                stacked: false,
                pointStyle: false,
                scales: {
                    x: {
                        type: 'time',
                        display: false,
                        ticks: { padding: 0, backdropPadding: 0 },
                    },
                    y: {
                        position: 'left',
                        display: false,
                    }
                },
                plugins: {
                    legend: { display: false },
                    tooltip: { enabled: false },
                    crosshair: { enabled: false },
                }
            }
        }
    }

    static statusmapConfig() {
        const colors = UiHelper.colors()
        return {
            type: 'bar',
            options: {
                events: ['mousedown', 'mousemove', 'mouseup', 'mouseout', 'click', 'touchstart', 'touchmove', 'touchend'],
                barPercentage: 1,
                categoryPercentage: 0.90,
                indexAxis: 'y',
                maintainAspectRatio: false,
                layout: { padding: { top: 25, bottom: 4, right: 10 }},
                scales: {
                    x: {
                        type: 'time',
                        time: {
                            minUnit: 'minute',
                            displayFormats: {
                                minute: 'd LLL HH:mm',
                                hour: 'd LLL HH:mm',
                                day: 'd LLL',
                                week: 'd LLL',
                            }
                        },
                        title: { display: false },
                        ticks: {
                            color: colors.TextColorDarker,
                            maxRotation: 0,
                            autoSkipPadding: 15
                        },
                        grid: { color: colors.TextColorDarker + '44' }
                    },
                    y: {
                        beginAtZero: true,
                        stacked: true,
                    },
                },
                plugins: {
                    legend: { display: false },
                    tooltip: {
                        itemSort: (a, b) => b.raw.y - a.raw.y,
                        callbacks: {
                            title: context => statusmapTooltipTitle(context[0].raw.x, precisions[context[0].chart.precision], context[0].chart.options.scales.x.time.displayFormats),
                            label: context => ` ${context.dataset.label}: ${context.raw.v}${context.dataset.unit}`,
                        },
                        backgroundColor: colors.BgColorDarker,
                        titleColor: colors.TextColor,
                        bodyColor: colors.TextColorDarker,
                        borderColor: colors.BorderColor,
                        borderWidth: 1,
                        position: 'mouse',
                        intersect: true,
                    },
                    crosshair: {
                        callbacks: { afterZoom: start => this.updatePointStyle(start.chart) },
                    },
                }
            }
        }
    }

    static executeUserScript(uscript, $config){
        if (!uscript) return 
        try { eval(uscript) } catch (ex) {
            alert(`User Script:\n---------------\n${uscript}\n---------------\n${ex}`)
        }
    }

    static updateChartType(chart) {
        return this.updatePointStyle(chart)
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
}
