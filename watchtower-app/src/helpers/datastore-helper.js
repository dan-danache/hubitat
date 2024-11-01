export class DatastoreHelper {
    static CACHE = {}

    static accessToken() {
        return new URLSearchParams(window.location.search).get('access_token')
    }

    static async fetchGridLayout(name) {
        try {
            const response = await fetch(new Request(`./grid-layout.json?name=${encodeURIComponent(name)}&access_token=${this.accessToken()}`), { cache: 'no-store' })
            if (!response.ok) {
                throw new Error(`DatastoreHelper.fetchGridLayout() - HTTP error, status = ${response.status}`)
            }
            const text = await response.text()
            const json = JSON.parse(text)
            if (json.status === false) {
                throw new Error(`Dashboard "${name}" does not exist.`)
            }
            return json
        } catch (ex) {
            console.error(ex)
            alert(ex.message)
        }
    }

    static async saveGridLayout(name, layout) {
        try {
            const response = await fetch(new Request(`./grid-layout.json?name=${encodeURIComponent(name)}&access_token=${this.accessToken()}`), {
                method: 'PUT',
                body: JSON.stringify(layout),
                cache: 'no-store'
            })
            if (!response.ok) {
                throw new Error(`DatastoreHelper.saveGridLayout() - HTTP error, status = ${response.status}`)
            }
            const text = await response.text()
            const json = JSON.parse(text)
            if (json.status === false) {
                throw new Error(`Dashboard "${name}" does not exist.`)
            }
            alert(`Dashboard "${name}" successfully saved!`)
            return json
        } catch (ex) {
            console.error(ex)
            alert(ex.message)
        }
    }

    static async fetchMonitoredDevices() {
        return this.fetchAndCache('monitored-devices.json')
    }

    static async fetchSupportedAttributes() {
        return this.fetchAndCache('supported-attributes.json')
    }

    static async fetchAndCache(fileName) {
        if (this.CACHE[fileName] !== undefined) return this.CACHE[fileName]
        this.CACHE[fileName] = fetch(new Request(`./${fileName}?access_token=${this.accessToken()}`), { cache: 'no-store' })
            .then(response => {
                if (!response.ok) {
                    throw new Error(`DatastoreHelper.fetchAndCache(${fileName}) - HTTP error, status = ${response.status}`)
                }
                return response.text()
            })
            .then(text => JSON.parse(text))
            .catch((ex) => {
                console.error(ex)
                alert(ex.message)
                reject(ex)
            })
        return this.CACHE[fileName]
    }

    static async fetchDeviceData({dev, precision, attr1, attr2, mm1, mm2, z1, z2}) {
        //console.log('fetchDeviceData', dev, precision, attr1, attr2, mm1, mm2, z1, z2)
        const data = { attr1: [], attr2: [], min1: [], max1: [], min2: [], max2: [] }
        try {
            const response = await fetch(new Request(this.buildCsvUrl(dev, precision)), { cache: 'no-store' })

            // Data not available yet
            if (response.status == 404) return data

            // Data transfer failed
            if (!response.ok) {
                throw new Error(`DatastoreHelper.fetchDeviceData() - HTTP error, status = ${response.status}`)
            }
            const lines = (await response.text()).split("\n")
            const header = lines.shift().split(',')
            const attr1Idx = header.indexOf(attr1)
            const attr2Idx = attr2 == undefined ? -1 : header.indexOf(attr2)
            const min1Idx = header.indexOf(`${attr1}_min`)
            const max1Idx = header.indexOf(`${attr1}_max`)
            const min2Idx = header.indexOf(`${attr2}_min`)
            const max2Idx = header.indexOf(`${attr2}_max`)

            // Add min/max values
            const addMinMax1 = mm1 === true && precision !== '5m' && min1Idx !== -1 && max1Idx !== -1
            const addMinMax2 = mm2 === true && precision !== '5m' && min2Idx !== -1 && max2Idx !== -1
            lines.forEach(line => {
                const vals = line.split(',')
                const x = parseInt(vals[0]) * 1000
                data.attr1.push({x, y: (vals[attr1Idx] === '' || vals[attr1Idx] === '-' ? (z1 === true ? 0 : null) : parseFloat(vals[attr1Idx]))})
                if (attr2Idx !== -1) data.attr2.push({x, y: (vals[attr2Idx] === '' || vals[attr2Idx] === '-' ? (z2 === true ? 0 : null) : parseFloat(vals[attr2Idx]))})

                if (addMinMax1) {
                    data.min1.push({x, y: (vals[min1Idx] === undefined || vals[min1Idx] === '' || vals[min1Idx] === '-' ? (z1 === true ? 0 : null) : parseFloat(vals[min1Idx]))})
                    data.max1.push({x, y: (vals[max1Idx] === undefined || vals[max1Idx] === '' || vals[max1Idx] === '-' ? (z1 === true ? 0 : null) : parseFloat(vals[max1Idx]))})
                }
                if (addMinMax2) {
                    data.min2.push({x, y: (vals[min2Idx] === undefined || vals[min2Idx] === '' || vals[min2Idx] === '-' ? (z2 === true ? 0 : null) : parseFloat(vals[min2Idx]))})
                    data.max2.push({x, y: (vals[max2Idx] === undefined || vals[max2Idx] === '' || vals[max2Idx] === '-' ? (z2 === true ? 0 : null) : parseFloat(vals[max2Idx]))})
                }
            })
            return data

        } catch (ex) {
            console.error(ex)
            alert(ex.message)
        }
    }

    static buildCsvUrl(deviceId, precision) {
        if (window.location.host !== 'cloud.hubitat.com') return `/local/wt_${deviceId}_${precision}.csv`
        return `./watchtower.csv?device=${deviceId}&precision=${precision}&access_token=${this.accessToken()}`
    }

    static async fetchAttributeData({ attr, devs, precision, z }) {
        const data = {}
        for (const dev of devs) {
            const deviceData = await this.fetchDeviceData({
                dev,
                precision,
                attr1: attr,
                z1: z
            })
            data[`dev_${dev}`] = deviceData.attr1
        }
        return data
    }

    static async fetchHubInfo() {
        try {
            const response = await fetch(new Request(`./hub-info.json?access_token=${this.accessToken()}`), { cache: 'no-store' })
            if (!response.ok) {
                throw new Error(`DatastoreHelper.fetchHubInfo() - HTTP error, status = ${response.status}`)
            }
            const text = await response.text()
            return JSON.parse(text)
        } catch (ex) {
            console.error(ex)
            alert(ex.message)
        }
    }

    static async fetchHubData() {
        try {
            const response = await fetch(new Request('/hub2/hubData'), { cache: 'no-store' })
            if (!response.ok) {
                throw new Error(`DatastoreHelper.fetchHubData() - HTTP error, status = ${response.status}`)
            }
            const text = await response.text()
            return JSON.parse(text)
        } catch (ex) {
            console.error(ex)
            alert(ex.message)
        }
    }
}
