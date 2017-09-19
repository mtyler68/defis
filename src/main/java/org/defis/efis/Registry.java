/*
 *  Copyright 2017 DEFIS
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.defis.efis;

import javafx.beans.Observable;

/**
 * Central hub for all telemetry and configuration settings. Back systems are managed by the SystemsManager.
 *
 * Among other things, the Registry creates an intermediary binding between the source and the consumer. This allows for
 * the publisher to unpublish without the subscriber requiring to re-subscribe. When a publisher publishes or
 * unpublishes, subscribers need to be notified so that they can change their in-op status accordingly.
 *
 * TODO: Make a protocol and protocol handler to receive FGFS data asynchronously. Should be faster and less invasive.
 *
 * TODO: Make a module manager that scans all classes at start-up for publisher and subscriber telemetry as well as
 * configuration settings.
 *
 * Do not apply smoothing at the registry level. Smoothing is for display purposes.
 *
 * TODO: Add /redundency directory where telemetry is mapped in priority order to fallback telemetry sources. For
 * example: /redundency/indicatedAirSpeed would be mapped to /pitot[0]/airspeed-kts,/pitot[1]/airspeed-kts
 *
 * /gps[0] = ADS-B In/Out GPS, /gps[1] = EFIS 1 GPS, /gps[2] = EFIS 2 GPS
 *
 * TODO: Backup telemetry and instruments may be referred to as AUX
 *
 * The questions are how to represent instruments when they are likely dynamic, but the programming paradigm likes the
 * static model. For example, one airplane may have a single GPS source while a second airplane has multiple. Or, one
 * airplane may have a single AHRS while another has three. If you use a central registry to provide a single value for
 * telemetry and use a redundancy manager to change out the source when necessary, thats the best solution.
 *
 * An instrument is a source for telemetry. It has configuration, but no representation of telemetry. A Gauge is the
 * actual visual representation. Alarms, Alerts, and Annunciators can also monitor instrument/sensor telemetry and
 * apply rules for reaction. Change the beginning of this paragraph to mean a software Sensor is the interface between
 * the physical instrument or sensor and the core DEFIS.
 *
 * A Fuel Level Sensor
 * The DEFIS component may have built-in A/D converters suitable for reading a fuel level senders. Fuel level '0' can
 * represent the left tank and fuel level '1' can represent the right tank. Two instances of the FuelLevelSensor
 * would exist and could be configured as:
 *
 * /consumables/fuel/tank[0]/name = Left
 * /consumables/fuel/tank[0]/level-gal_us = 20
 * /consumables/fuel/tank[0]/capacity-gal_us = 21 (usable fuel vs. capacity)
 * /consumables/fuel/tank[0]/aio/port = 1D
 * /consumables/fuel/tank[0]/aio/low-ohms = 240
 * /consumables/fuel/tank[0]/aio/high-ohms = 12
 * /consumables/fuel/tank[0]/aio/interpolation = LINEAR
 *
 * /consumables/fuel/tank[1]/name = Right
 * /consumables/fuel/tank[1]/level-gal_us = 20
 * /consumables/fuel/tank[1]/capacity-gal_us = 21
 * /consumables/fuel/tank[1]/aio/port = 1F
 * /consumables/fuel/tank[1]/aio/low-ohms = 240
 * /consumables/fuel/tank[1]/aio/high-ohms = 12
 * /consumables/fuel/tank[1]/aio/interpolation = LINEAR
 *
 * A FuelFlowSensor could be another example. Since any aircraft could have one or many and they can be organized
 * by tank or by engine. So the best approach is to allow configuration to come in create an instance of a
 *
 * FuelFlowSensor
 * and define mapping between the telemetry it produces and where it's mapped into the registry. This offers the most
 * flexibility but also doesn't enforce standards across implementations or configurations. A trade off of one thing
 * for another. The flexibility that is offered by the Gauge model is that a gauge can represent any Telemetry element
 * from the Registry. Published Telemetry also include metadata about the Telemetry (i.e. min, max, ranges, units, etc)
 * that can be used by a Gauge to auto-configure its behavior.
 *
 * A Page descriptor describes how the background is laid out (full, split), the controls, gauges, or clusters that
 * are positioned around the page, and the menus, controls, and annunciators that are placed around the perimeter of the
 * page. The background could be an artificial horizon with the ability to enable or disable the pitch and bank
 * indicators.
 * You could also put a control in the edge menus for enabling the HITS renderer. The background is know as the
 * Enhanced Visualization Space (EVS) and could be a Terrain Awareness System or moving map or moving map with traffic
 * and terrain awareness overlay. The Page can also include tape gauges displaying the air speed, altimeter, and
 * vertical
 * speed as well as indicators displaying Ground Speed and TAS. Each Page is laid out specific to its screen resolution,
 * so page layouts can't be used across various screen resolutions.
 *
 * DEFIS Bootstrap
 *
 * When DEFIS starts, it needs load its sensor, protocols, HID, and page configurations. The sensor configuration
 * creates
 * all the instances necessary to communicate and control the on-board sensors. The protocol configuration stands up
 * and establishes communication protocols (RS-232, CAN, WiFi, Bluetooth, Ethernet) with external controls and
 * sensors (i.e. auto-pilot servos, ECU) as well as the internal instances to map communications. The HID configuration
 * defines Human Input Devices (buttons, knobs, indicators) that provide physical control input to DEFIS.And the page
 * configuration creates the UI the pilot interacts with.
 *
 * A HID can be a single button, rotary dial, indicator light, or an array of buttons, rotary dials, or indicator
 * lights. A HID could be a specialized combination of buttons and rotary dials in the same style as Dynon specialized
 * modules (i.e. auto-pilot control, trim and indicator controls). The array of buttons could be aligned to the EFIS
 * display and correspond to contextualized menu items. HIDs are daisy chained using the CAN bus and provide a HID type
 * for better configuration by the DEFIS manager. Other HID devices like multi-axis HAT switches in a control yoke or
 * switch panels that control system operation as well as anything not yet conceived could be developed.
 *
 * A DEFIS needs to know what internal capabilities it has, which Sensor instances to instantiate, and to some extent,
 * how to interpret those sensor instances. There should be some standard setups for DEFIS, but there should also be
 * some flexibility. A standard DEFIS may be able to internally generate GPS coordinates, measure OAT, measure static
 * and pitot air, measure two fuel senders, and measure fuel flow. It should also be able to communicate over one or
 * more serial ports, CAN busses, and ARINC.
 *
 * DEFIS InternalSensorManager reads the sensor descriptor and instantiates and configures instances that interact with
 * those sensors and produce telemetry that can be consumed elsewhere.
 *
 * The ProtocolManager reads a configuration that may instruct it to create several SerialProtocolDrivers that get
 * coupled with ProtocolInterfaces that map to the Telemetry/Registry systems. It may also be instructed to instantiate
 * a CANaerospace interface that is able to scan the CAN network for devices and provision specific handlers for each
 * CAN module that corresponds to some pre-existing configuration about known modules.
 *
 * VISION FOR DEFIS:
 *
 * A software and hardware avionics solution for experimental aircraft builders. It can be as simple as purchasing
 * pre-built components from a vendor with the software loaded and ready to building it yourself to save a few bucks.
 * Try to establish a standardized configuration around components as well as offer the ability to expand.
 *
 * Perhaps the first version of DEFIS could be a tightly scoped PFD that didn't permit customization. An example would
 * be similar to a Dynon D100 with a single screen that shows an artificial horizon, bank, pitch, turn rate, slip,
 * IAS, altimeter, VSI, GS, TAS, DG or HSI. It could have an on-board AHRS, GPS, OAT, and magnetometer
 * (even though this may not be a good idea). It could have a single rotary dial with push button to actuate the menu
 * for settings. It could be designed to fit within an existing 3 1/2" instrument cut-out with a 400x240 pixel screen.
 *
 * @author Matthew Tyler
 */
public class Registry
{

    public void publish(String path, Observable property) {

    }

    public void unpublish(String path) {

    }

    public Observable subscribe(String path) {
        return null;
    }

}
