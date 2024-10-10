export default {
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
