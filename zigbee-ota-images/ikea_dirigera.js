/**
 * Download Zigbee OTA images for IKEA devices
 * 
 * @see https://ww8.ikea.com/ikeahomesmart/releasenotes/releasenotes.html
 * @see http://fw.ota.homesmart.ikea.net/feed/version_info.json
 * @see https://github.com/Koenkk/zigbee-OTA/blob/master/lib/ota.js
 * @see https://fw.ota.homesmart.ikea.com/check/update/prod
 */
process.env['NODE_TLS_REJECT_UNAUTHORIZED'] = 0;

const path = require('path');
const fs = require('fs');
const assert = require('assert');
const http = require('http');
const https = require('https');

const upgradeFileIdentifier = Buffer.from([0x1E, 0xF1, 0xEE, 0x0B]);

/**
 * Downloads file from remote HTTP[S] host and puts its contents to the
 * specified location.
 */
async function download(url, filePath) {
    console.log('Downloading', url);
    const proto = !url.charAt(4).localeCompare('s') ? https : http;

    return new Promise((resolve, reject) => {
        const file = fs.createWriteStream(filePath);
        let fileInfo = null;

        const request = proto.get(url, response => {
        if (response.statusCode !== 200) {
            fs.unlink(filePath, () => {
            reject(new Error(`Failed to get '${url}' (${response.statusCode})`));
            });
            return;
        }

        fileInfo = {
            mime: response.headers['content-type'],
            size: parseInt(response.headers['content-length'], 10),
        };

        response.pipe(file);
        });

        // The destination stream is ended by the time it's called
        file.on('finish', () => resolve(fileInfo));

        request.on('error', err => {
            fs.unlink(filePath, () => reject(err));
        });

        file.on('error', err => {
            fs.unlink(filePath, () => reject(err));
        });

        request.end();
    });
}

function parseSubElement(buffer, position) {
    const tagID = buffer.readUInt16LE(position);
    const length = buffer.readUInt32LE(position + 2);
    const data = buffer.slice(position + 6, position + 6 + length);
    return {tagID, length, data};
}

function parseImage(rawBuffer) {
    const start = rawBuffer.indexOf(upgradeFileIdentifier);
    if (start === -1) {
        console.log('ERROR: Could not find OTA file identifier');
        return;
    }
    const buffer = rawBuffer.slice(start);

    const header = {
        otaUpgradeFileIdentifier: buffer.subarray(0, 4),
        otaHeaderVersion: buffer.readUInt16LE(4),
        otaHeaderLength: buffer.readUInt16LE(6),
        otaHeaderFieldControl: buffer.readUInt16LE(8),
        manufacturerCode: buffer.readUInt16LE(10),
        imageType: buffer.readUInt16LE(12),
        fileVersion: buffer.readUInt32LE(14),
        zigbeeStackVersion: buffer.readUInt16LE(18),
        otaHeaderString: buffer.toString('utf8', 20, 52),
        totalImageSize: buffer.readUInt32LE(52),
    };
    let headerPos = 56;
    if (header.otaHeaderFieldControl & 1) {
        header.securityCredentialVersion = buffer.readUInt8(headerPos);
        headerPos += 1;
    }
    if (header.otaHeaderFieldControl & 2) {
        header.upgradeFileDestination = buffer.subarray(headerPos, headerPos + 8);
        headerPos += 8;
    }
    if (header.otaHeaderFieldControl & 4) {
        header.minimumHardwareVersion = buffer.readUInt16LE(headerPos);
        headerPos += 2;
        header.maximumHardwareVersion = buffer.readUInt16LE(headerPos);
        headerPos += 2;
    }

    const raw = buffer.slice(0, header.totalImageSize);

    assert(Buffer.compare(header.otaUpgradeFileIdentifier, upgradeFileIdentifier) === 0, 'Not an OTA file');

    let position = header.otaHeaderLength;
    const elements = [];
    while (position < header.totalImageSize) {
        const element = parseSubElement(buffer, position);
        elements.push(element);
        position += element.data.length + 6;
    }

    assert(position === header.totalImageSize, 'Size mismatch');
    return {header, elements, raw};
}

function parseVersion(fileVersion) {

    // 00010002 -> 1.0.002
    if (fileVersion.startsWith('00')) return `${fileVersion.substring(3, 4)}.${fileVersion.substring(4, 5)}.${fileVersion.substring(5)}`

    // 10021655 -> 1.0.021
    if (fileVersion.startsWith('10')) return `1.0.${fileVersion.substring(2, 5)}`

    // 23087631 -> 2.3.087
    if (fileVersion.startsWith('23')) return `2.3.${fileVersion.substring(2, 5)}`

    // 01000020 -> 1.0.20
    // 03000010 -> 3.0.10
    // 24040005 -> 24.4.5
    return `${parseInt(fileVersion.substring(0, 2))}.${parseInt(fileVersion.substring(2, 4))}.${parseInt(fileVersion.substring(4))}`
}

const main = async () => {
    if (!fs.existsSync('temp')) fs.mkdirSync('temp');
    if (!fs.existsSync('images')) fs.mkdirSync('images');

    const jsonFile = path.resolve('temp/version_info.json');
    await download('https://fw.ota.homesmart.ikea.com/check/update/prod', jsonFile);

    const jsonData = JSON.parse(fs.readFileSync(jsonFile));
    for (const entry of jsonData) {
        const filename = `temp/${entry.fw_binary_url.split('/').pop()}`;

        // Download signed OTA file
        if (!filename.endsWith('.ota')) continue;
        await download(entry.fw_binary_url, filename);

        // Read file data
        const buffer = fs.readFileSync(filename);
        const parsed = parseImage(buffer);
        if (parsed === undefined) continue;
        console.log(parsed.header);

        // Build output file name
        const manufacturerCode = parsed.header.manufacturerCode.toString(16).toUpperCase();
        const imageType = parsed.header.imageType.toString(16).toUpperCase().padStart(4, 0);
        let fileVersion = parsed.header.fileVersion.toString(16).toUpperCase().padStart(8, 0);

        // Who dis?
        if (imageType == '0002') continue;

        // Styrbar fix
        if (imageType == '11CB' && fileVersion == '00000245') {
            fileVersion = '02040005';
        }

        const deviceName = parsed.header.otaHeaderString.replaceAll('EBL ', '').replaceAll('GBL ', '').replaceAll('GBL_', '').replaceAll("\x00", '').replaceAll(' ', '_')
        const outputName = `images/${manufacturerCode}-${imageType}-${fileVersion}-${deviceName}-${parseVersion(fileVersion)}.zigbee`
        console.log(`Writing OTA file: ${outputName}`);

        // Write to output file
        fs.writeFileSync(outputName, parsed.raw);
        delete(parsed.header.otaUpgradeFileIdentifier);
        fs.writeFileSync(`${outputName}.txt`, `Source: ${entry.fw_binary_url}\r\nImage Details: ${JSON.stringify(parsed.header, null, "    ").replace(/\n/g, "\r\n")}`);
    };

    // Cleanup
    fs.rmSync('temp', { recursive: true, force: true });
}

main();
