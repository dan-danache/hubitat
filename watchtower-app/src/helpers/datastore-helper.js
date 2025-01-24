export class DatastoreHelper {
    static CACHE = {}

    static accessToken() {
        return new URLSearchParams(window.location.search).get('access_token')
    }

    static async fetchGridLayout(name) {
        try {
            const json = JSON.parse(await this.downloadFile(`./grid-layout.json?name=${encodeURIComponent(name)}&access_token=${this.accessToken()}`))
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

    static addVal(target, x, vals, idx, z) {
        if (idx === -1) return
        const y = vals[idx] === '' || vals[idx] === '-' ? null : parseFloat(vals[idx])
        if (target.length === 0 && y === null) return
        target.push({x, y: y === null ? (z ? 0 : null) : y})
    }

    static async fetchDeviceData({dev, precision, attr1, attr2, mm1, mm2, z1, z2}) {
        const data = { attr1: [], attr2: [], min1: [], max1: [], min2: [], max2: [] }
        try {
            const contents = await this.downloadFile(this.buildCsvUrl(dev, precision), true)
            if (!contents) return data

            const lines = contents.split("\n")
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

                this.addVal(data.attr1, x, vals, attr1Idx, z1)
                this.addVal(data.attr2, x, vals, attr2Idx, z2)

                if (addMinMax1) {
                    this.addVal(data.min1, x, vals, min1Idx, z1)
                    this.addVal(data.max1, x, vals, max1Idx, z2)
                }

                if (addMinMax2) {
                    this.addVal(data.min2, x, vals, min2Idx, z1)
                    this.addVal(data.max2, x, vals, max2Idx, z2)
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

    static async fetchCustomData({ds, precision}, sparkline = false) {
        const requests = {}
        const retVal = {}
        ds.forEach(dsr => {
            if (requests[dsr.dev] === undefined) requests[dsr.dev] = new Set()
            requests[dsr.dev].add(dsr.attr)
            retVal[`${dsr.dev}_${dsr.attr}`] = []
        })
        Object.keys(requests).forEach(dev => requests[dev] = Array.from(requests[dev]))

        // Retrieve only records needed to render the sparkline
        let minTime = 0
        if (sparkline) {
            switch (precision) {
                case '5m': minTime = new Date().getTime() - 3600 * 1000; break;                // Last 1 hour
                case '1h': minTime = new Date().getTime() - 10 * 3600 * 1000; break;           // Last 10 hours
                case '1d': minTime = new Date().getTime() - 10 * 24 * 3600 * 1000; break;      // Last 10 days
                case '1w': minTime = new Date().getTime() - 10 * 7 * 24 * 3600 * 1000; break;  // Last 10 weeks
            }
        }

        for (const [dev, attrs] of Object.entries(requests)) {
            const attrsLen = attrs.length
            try {
                const contents = await this.downloadFile(this.buildCsvUrl(dev, precision), true)
                if (!contents) continue

                const lines = contents.split("\n")
                const header = lines.shift().split(',')
                const attrsIdx = attrs.map(attr => header.indexOf(attr))

                // Process all lines
                lines.forEach(line => {
                    const vals = line.split(',')
                    const x = parseInt(vals[0]) * 1000
                    if (x < minTime) return
                    for (let idx = 0; idx < attrsLen; idx++) this.addVal(retVal[`${dev}_${attrs[idx]}`], x, vals, attrsIdx[idx], true)
                })
            } catch (ex) {
                console.error(ex)
                alert(ex.message)
            }
        }
        return retVal
    }

    static async fetchStatusmapData({ds, precision}) {
        const requests = {}
        const retVal = {}
        ds.forEach(dsr => {
            if (requests[dsr.dev] === undefined) requests[dsr.dev] = new Set()
            requests[dsr.dev].add(dsr.attr)
            retVal[`${dsr.dev}_${dsr.attr}`] = []
        })
        Object.keys(requests).forEach(dev => requests[dev] = Array.from(requests[dev]))

        const parseVal = val => val === '' || val === '-' ? null : parseFloat(val)
        for (const [dev, attrs] of Object.entries(requests)) {
            const attrsLen = attrs.length
            try {
                const contents = await this.downloadFile(this.buildCsvUrl(dev, precision), true)
                if (!contents) continue

                const lines = contents.split("\n")
                const header = lines.shift().split(',')
                const attrsIdx = attrs.map(attr => header.indexOf(attr))

                // Init last with first line data
                const firstVals = lines.shift().split(',')
                const last = attrsIdx.map(attrIdx => { return {
                    x: parseInt(firstVals[0]) * 1000,
                    v: parseVal(firstVals[attrIdx]),
                }})

                // Process all remaining lines
                lines.forEach(line => {
                    const vals = line.split(',')
                    const x = parseInt(vals[0]) * 1000

                    for (let idx = 0; idx < attrsLen; idx++) {
                        const attr = attrs[idx]
                        const attrIdx = attrsIdx[idx]
                        const v = parseVal(vals[attrIdx])

                        // Attribute has the same value
                        if (v === last[idx].v) continue

                        // Add to result
                        if (retVal[`${dev}_${attr}`].length !== 0 || last[idx].v !== null) retVal[`${dev}_${attr}`].push({
                            x: [last[idx].x, x],
                            v: last[idx].v === null ? 0 : last[idx].v,
                        })
                        last[idx] = {x, v}
                    }
                })

                // Add last records (if the case)
                const lastVals = lines.pop().split(',')
                attrsIdx.forEach((attrIdx, idx) => {
                    const x = parseInt(lastVals[0]) * 1000
                    const v = parseVal(lastVals[attrIdx])
                    if (x == last[idx].x) return
                    const attr = attrs[idx]
                    retVal[`${dev}_${attr}`].push({
                        x:[last[idx].x, x],
                        v: v === null ? 0 : v,
                    })
                })
            } catch (ex) {
                console.error(ex)
                alert(ex.message)
            }
        }
        return retVal
    }

    static async fetchBringYourOwnData({file, fmt, ts, ds}) {
        const contents = await this.downloadFile(`/local/${file}`)
        switch (fmt) {
            case 'csv': return this.parseBringYourOwnDataCsv(contents, ts, ds)
            case 'json': return this.parseBringYourOwnDataJson(contents, ts, ds)
            default: throw new Error(`Unknown file data type: ${fmt}`)
        }
    }

    static parseBringYourOwnDataCsv(contents, ts, ds) {
        const retVal = {}
        ds.forEach(dataset => retVal[`${dataset.k}`] = [])

        const lines = contents.split("\n")
        let tsIdx = ts
        let attrsIdx = ds.map(dataset => dataset.k)

        // If at least one dataset key is not numeric, then we are dealing with header names
        if (isNaN(parseInt(ts)) || ds.find(dataset => isNaN(parseInt(dataset.k)))) {
            const header = lines.shift().split(',').map(value => value.replace(/^["' ]+|["' ]+$/g, ''))
            tsIdx = header.indexOf(ts)
            attrsIdx = ds.map(dataset => header.indexOf(dataset.k))
        }

        // Process all lines
        const parseVal = val => val === '' || val === '-' ? 0 : parseFloat(val)
        lines.forEach(line => {
            const vals = line.split(',').map(value => value.replace(/^["' ]+|["' ]+$/g, ''))

            // Parse timestamp
            let x
            let tsVal = parseInt(vals[tsIdx])
            if (!isNaN(tsVal)) x = tsVal < 600000000000 ? tsVal * 1000 : tsVal
            else x = new Date(vals[tsIdx]).getTime()

            // Parse metrics
            for (let idx = 0; idx < ds.length; idx++) {
                const y = parseVal(vals[attrsIdx[idx]])
                retVal[`${ds[idx].k}`].push({x, y})
            }
        })

        return retVal
    }

    static parseBringYourOwnDataJson(contents, ts, ds) {
        const retVal = {}
        ds.forEach(dataset => retVal[`${dataset.k}`] = [])

        const parseVal = val => val === '' || val === '-' ? 0 : parseFloat(val)
        const records = JSON.parse(contents)
        records.forEach(record => {

            // Parse timestamp
            let x
            let tsVal = parseInt(record[ts])
            if (!isNaN(tsVal)) x = tsVal < 600000000000 ? tsVal * 1000 : tsVal
            else x = new Date(record[ts]).getTime()

            // Parse metrics
            for (let idx = 0; idx < ds.length; idx++) {
                const y = parseVal(record[ds[idx].k])
                retVal[`${ds[idx].k}`].push({x, y})
            }
        })

        return retVal
    }

    static async fetchHubInfo() {
        try {
            return JSON.parse(await this.downloadFile(`./hub-info.json?access_token=${this.accessToken()}`))
        } catch (ex) {
            console.error(ex)
            alert(ex.message)
        }
    }

    static async fetchHubData() {
        try {
            return JSON.parse(await this.downloadFile('/hub2/hubData'))
        } catch (ex) {
            console.error(ex)
            alert(ex.message)
        }
    }

    static async analyzeFile(fileName) {
        const contents = (await this.downloadFile(`/local/${fileName}`)).trim()

        // Empty file
        if (contents === '') throw new Error('File is empty')

        // JSON Object? Yuck!
        if (contents[0] === '{') throw new Error('JSON file contains a single object, not an array of objects')

        // JSON Array? Yay!
        if (contents[0] === '[') {
            try {
                const json = JSON.parse(contents)
                const entry = json[0]

                // Empty array?
                if (entry == undefined) throw new Error('File contains an empty JSON array')

                // Array of empty objects?
                const keys = Object.keys(entry)
                if (keys.length == 0) throw new Error('File contains a JSON array of empty objects')

                // Detect fields that could represent a timestamp
                const ts = keys.filter(key => this.isTimestamp(entry[key]))
                if (ts.length === 0) throw new Error('JSON objects don\'t contain any timestamp keys')

                return {
                    fmt: 'json',
                    keys: keys.filter(key => this.isNumeric(entry[key])).sort(),
                    ts
                }

            } catch (ex) {
                throw new Error(`Failed to parse file contents as JSON: ${ex.message}`)
            }
        }

        // Suppose it's a CSV
        const lines = contents.split("\n")
        if (lines.length < 3) throw new Error('CSV file should contain at least 2 rows')
        const header = lines[0].split(',').map(value => value.replace(/^["' ]+|["' ]+$/g, ''))

        // Check if this a valid CSV file - at least 2 columns
        if (header.length < 2) throw new Error('Not a valid JSON/CSV file')

        // Check if this a valid CSV file - first 3 rows contains the same number of columns
        const rec1 = lines[1].split(','), rec2 = lines[2].split(',')
        if (header.length !== rec1.length || header.length !== rec2.length) throw new Error('Not a valid JSON/CSV file')

        // Detect fields that could represent a timestamp
        const ts = [...Array(header.length).keys()].filter(idx => this.isTimestamp(rec1[idx]) && this.isTimestamp(rec2[idx]))
        if (ts.length === 0) throw new Error('CSV file does not contain any timestamp columns')

        // Is it a header (aka does not contain any number in row values) ?
        if (!header.find(value => /^-?\d+$/.test(value))) return {
            fmt: 'csv',
            keys: header.filter((_, idx) => this.isNumeric(rec1[idx]) && this.isNumeric(rec2[idx])),
            ts: header.filter((_, idx) => ts.includes(idx)),
        }

        // Does not contain header line, just return column numbers
        return {
            fmt: 'csv',
            keys: [...Array(header.length).keys()].filter(idx => this.isNumeric(rec1[idx]) && this.isNumeric(rec2[idx])),
            ts
        }
    }

    static isTimestamp(value) {
        const trimmedValue = `${value}`.replace(/^["' ]+|["' ]+$/g, '')
        let val = parseInt(trimmedValue)
        if (isNaN(val)) val = trimmedValue
        switch (typeof val) {
            case 'number': return val > 600000000
            case 'string': return !isNaN(new Date(val))
            default: return false
        }
    }

    static isNumeric(value) {
        const trimmedValue = `${value}`.replace(/^["' ]+|["' ]+$/g, '')
        return !isNaN(parseFloat(trimmedValue))
    }

    static async downloadFile(filePath, ignoreNotFound = false) {
        const response = await fetch(new Request(filePath), { cache: 'no-store' })

        // File not found
        if (response.status == 404) {
            if (ignoreNotFound) return undefined
            throw new Error(`File not found: ${filePath}`)
        }

        if (!response.ok) {
            throw new Error(`DatastoreHelper.downloadFile(${filePath}): HTTP error: status = ${response.status}`)
        }
        return await response.text()
    }
}
