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

    static async fetchDeviceData(deviceId, attr1, attr2, precision) {
        const data = { attr1: [], attr2: [] }
        try {
            const response = await fetch(new Request(this.buildCsvUrl(deviceId, precision)), { cache: 'no-store' })

            // Data not available yet
            if (response.status == 404) return data

            // Data transfer failed
            if (!response.ok) {
                throw new Error(`DatastoreHelper.fetchDeviceData() - HTTP error, status = ${response.status}`)
            }
            const lines = (await response.text()).split("\n")
            const header = lines.shift().split(',')
            const attr1Idx = attr1 = header.indexOf(attr1)
            const attr2Idx = attr2 == undefined ? 0 : header.indexOf(attr2)

            lines.forEach(line => {
                const vals = line.split(',')
                data.attr1.push({x: parseInt(vals[0] * 1000), y: parseInt(vals[attr1Idx])})
                if (attr2Idx !== 0) data.attr2.push({x: parseInt(vals[0] * 1000), y: parseInt(vals[attr2Idx])})
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

    static async fetchAttributeData(attribute, deviceIds, precision) {
        const data = {}
        for (const deviceId of deviceIds) {
            const deviceData = await this.fetchDeviceData(deviceId, attribute, undefined, precision)
            data[`dev_${deviceId}`] = deviceData.attr1
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
}
