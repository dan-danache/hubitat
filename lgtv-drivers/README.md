# LGTV with webOS

Hubitat driver to control LG TV devices using webOS websockets.

Tested devices:
- LG OLED evo C2 55 inch (OLED55C21LA) - webOSTV 7.0 / 23.20.56
- LG OLED evo C2 48 inch (OLED48C21LA) - webOSTV 7.0 / 23.20.56

## Installation
There are two ways to install the drivers: using Hubitat Package Manager (HPM) or manually importing the driver code.

### HPM Installation (Recommended)
HPM is an app that allows you to easily install and update custom drivers and apps on your Hubitat hub. To use HPM, you need to have it installed on your hub first.

Once you have HPM installed, follow these steps to install the "LGTV with webOS" driver:

1. In the Hubitat interface, go to **Apps** and select **Hubitat Package Manager**.
1. Select **Install**, then **Search by Keywords**.
1. Enter **LGTV** in the search box and click **Next**.
1. Select **LGTV with webOS by Dan Danache** and click **Next**.
1. Follow the install instructions.

### Manual Installation
If you don't want to use HPM, you can also install the drivers manually by importing the driver code from GitHub. Follow these steps to do so:

1. In the Hubitat interface, go to **Drivers Code**.
1. Click **Add driver**, then select **Import** from the hamburger menu in the top right.
1. Enter `https://raw.githubusercontent.com/dan-danache/hubitat/main/lgtv-drivers/lgtv-with-webos.groovy` it in the URL field.
1. Click **Import**, then click **OK** and the code should load in the editor.
1. Click **Save** in the top right.

More info about installing custom drivers is available in the [Official Documentation](https://docs2.hubitat.com/en/how-to/install-custom-drivers).

## Create Devices

Follow these steps in order to create a new LG TV device:

### Configure TV
1. Make sure you are near your TV device, you cannot do this remotely.
1. Make sure that the IP address for your LG TV is fixed, using DHCP reservations. This ensures that the IP address for your TV is not changing on every restart.
1. Power on the TV, then open the TV settings.
1. Find and disable the **Always Ready** option (for OLED screens).
1. Find and enable the **Network IP Control** and **Wake on LAN** options.
1. Keep TV on for the following steps.

### Add device
1. In the Hubitat interface, go to **Devices**.
1. Click **Add device**, then select **Virtual**.
1. Select **LGTV with webOS** from the drivers list, then click **Next**.
1. Give your device a name, e.g. **Livingroom TV**, select a room, then click **Next**.
1. Click **View device details** to go to the device page.
1. Select the **Preferences** tab.
1. Enter the TV IP address, then click **Save**.
1. Check your TV screen and approve the notification that pops up.
1. Select the **Commands** tab, then refresh the browser page.
1. If the **Current States** section does not populate with multiple attributes, check the Hubitat logs for clues (debug messages are displayed for 30 minutes).

![TV Attributes](img/attributes.png)

## Control options

The following commands are available:

### Power control
- On / Off

### Sound volume control
- Volume Up / Volume Down
- Set Volume
- Mute / Unmute

### Live TV channel control
- Channel Up / Channel Down
- Set Channel

### Screen control
- Screen On / Screen Off

### App control
- Get All Activities - Populate the **Activities** attribute with available options for the **Start Activity** command
- Get Current Activity - Not actually needed as the current activity (running app) is automatically detected
- Start Activity - Start the specified app on the TV

### Notifications
- Device Notification - Display a toast or an alert to the TV screen

Have fun!

---
[<img src="https://cdn.buymeacoffee.com/buttons/v2/default-yellow.png" alt="Buy Me A Coffee" style="height: 40px !important;width: 162px !important">](https://www.buymeacoffee.com/dandanache)



