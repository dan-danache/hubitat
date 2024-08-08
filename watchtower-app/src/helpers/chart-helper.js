import '../vendor/vendor.min.js'
import { ColorHelper } from './color-helper.js'

export class ChartHelper {
    static defaultConfig(mobileView) {
        const colors = ColorHelper.colors()
        return {
            type: 'line',
            options: {
                parsing: false,
                normalized: true,
                responsive: true,
                maintainAspectRatio: false,
                onResize: chart => ChartHelper.updateChartType(chart),
                animation: { duration: 0, onComplete: ({ initial, chart }) => (initial ? ChartHelper.updateChartType(chart) : undefined) },
                layout: { padding: { top: 20, bottom: 3 }},
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
                        borderWidth: 1
                    },
                    decimation: { enabled: true, algorithm: 'lttb' },
                    zoom: {
                        pan: { enabled: mobileView !== true, mode: 'x' },
                        zoom: {
                            wheel: { enabled: true },
                            pinch: { enabled: mobileView !== true },
                            mode: 'x',
                            onZoomComplete: ({ chart }) => ChartHelper.updateChartType(chart)
                        },
                        limits: { x: { min: 'original', max: 'original' }},
                    }
                }
            },
            plugins: [ ChartHelper.crosshairPlugin(), ChartHelper.toggleScaleVisibilityPlugin() ]
        }
    }

    static crosshairPlugin() {
        const colors = ColorHelper.colors()
        return {
            id: 'crosshair',
            afterInit: (chart) => {
                chart.crosshair = {
                    x: 0,
                    y: 0
                }
            },
            afterEvent: (chart, args) => {
                const { inChartArea } = args
                const { type, x, y } = args.event

                chart.crosshair = { x, y, draw: inChartArea }
                chart.draw()
            },
            beforeDatasetsDraw: (chart) => {
                const { ctx } = chart
                const { top, bottom, left, right } = chart.chartArea
                const { x, y, draw } = chart.crosshair
                if (!draw) return

                ctx.save()
                ctx.beginPath()
                ctx.lineWidth = 1
                ctx.strokeStyle = colors.TextColor
                ctx.setLineDash([2, 2])
                ctx.moveTo(x, bottom)
                ctx.lineTo(x, top)
                ctx.stroke()
                ctx.restore()
            }
        }
    }

    static toggleScaleVisibilityPlugin() {
        return {
            id: 'toggleScaleVisibility',
            afterEvent: (chart, event) => {
                const evt = event.event;
                if (event.inChartArea === true || evt.type !== 'click' || chart.data.datasets.length === 1) return
                const { x, y } = evt
                const scale = Object.entries(chart.scales)
                    .filter(([key, value]) => key !== 'y' && value.axis === 'y' && value.top < y && value.bottom > y && value.left < x && value.right > x)
                    .map(entry => entry[0])
                if (scale.length === 0) return
                const datasets = chart.data.datasets
                const yAxisID = scale[0]
                const index = datasets.findIndex(dataset => dataset.yAxisID == yAxisID)

                if (chart.getVisibleDatasetCount() === 1) {
                    datasets.forEach((_, idx) => chart.setDatasetVisibility(idx, true))
                } else {
                    datasets.forEach((_, idx) => chart.setDatasetVisibility(idx, idx !== index))
                }
                chart.update('none')
            }
        }
    }

    static updateChartType(chart) {
        if (chart.scales.x === undefined || chart.data.datasets[0] === undefined) return
        const min = chart.scales.x.min
        const max = chart.scales.x.max
        const data = chart.data.datasets[0].data
        const visibleDatapoints = chart.getZoomLevel() <= 1 ? data.length : data.filter(point => point.x >= min && point.x <= max).length

        const chartType = chart.width / visibleDatapoints > 30 ? 'bar' : 'line'
        chart.options.scales.x.offset = chartType == 'bar'
        if (chart.config.type != chartType) {
            chart.config.type = chartType
            chart.update('none')
        }
    }

    static prettyName(camelCase) {
        return (camelCase[0].toUpperCase() + camelCase.slice(1))
            .replace(/([A-Z])(?=[A-Z][a-z])|([a-z])(?=[A-Z])/g, '$& ')
            .replace('Hub ', '')
    }
}
