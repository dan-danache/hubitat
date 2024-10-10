# Lumos

Control lights using motion and contact sensors.

## Installation

To install the Lumos app using the Hubitat Package Manager (and receive automatic updates), follow these steps:

1. Go to the **Apps** menu in the Hubitat interface.
2. Select **Hubitat Package Manager** from the list of apps.
3. Click **Install** and then **Search by Keywords**.
4. Type **Lumos** in the search box and click **Next**.
5. Choose **Lumos by Dan Danache** and click **Next**.
6. Read the license agreement and click **Next**.
7. Wait for the installation to complete and click **Next**.

## Usage

To use the Lumos app, follow these steps:

1. Go to the **Apps** menu in the Hubitat interface.
2. Select **Lumos** from the list of apps.

### Lumos (parent app)

From the Lumos parent app you can add and edit your Lumos Automations child instances.

![Lumos](img/lumos.png "Lumos parent app")

### Lumos Automation (child instance)

Use a Lumos Automation child instance to control one or more lights based on input from the selected motion and contact sensors.

![Lumos Automation](img/lumos-automation.png "Lumos child instance")

## How it Works

To control the room lights, place contact sensors on each door and one or more motion sensors in the room.

1. When the lights are off and a door is opened, the lights will immediately turn on for 5 minutes, illuminating the room even before motion is detected.

1. If any motion sensor detects movement, the lights will turn on for 5 minutes. Each motion detection resets the 5-minute timer.

1. If motion is detected and all doors are closed, the lights will remain on until a door is opened. In this scenario, motion events are ignored, and opening a door will immediately turn off the lights.

These rules make the app ideal for bathrooms, pantries, and basements, but less suitable for bedrooms or living rooms where multiple people use the doors.

---
[<img src="https://cdn.buymeacoffee.com/buttons/v2/default-yellow.png" alt="Buy Me A Coffee" style="height: 40px !important;width: 162px !important">](https://www.buymeacoffee.com/dandanache)
