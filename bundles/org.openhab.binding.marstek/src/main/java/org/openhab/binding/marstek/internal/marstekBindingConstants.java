/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.marstek.internal;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.thing.ThingTypeUID;

/**
 * The {@link marstekBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author acfischer42 - Initial contribution
 */
@NonNullByDefault
public class marstekBindingConstants {

    private static final String BINDING_ID = "marstek";

    // List of all Thing Type UIDs
    public static final ThingTypeUID THING_TYPE_BATTERY = new ThingTypeUID(BINDING_ID, "battery");

    // Battery Component Channels
    public static final String CHANNEL_BATTERY_SOC = "batterySoc";
    public static final String CHANNEL_BATTERY_TEMPERATURE = "batteryTemperature";
    public static final String CHANNEL_BATTERY_CAPACITY = "batteryCapacity";
    public static final String CHANNEL_BATTERY_RATED_CAPACITY = "batteryRatedCapacity";
    public static final String CHANNEL_CHARGING_FLAG = "chargingFlag";
    public static final String CHANNEL_DISCHARGING_FLAG = "dischargingFlag";

    // PV Component Channels
    public static final String CHANNEL_PV_POWER = "pvPower";
    public static final String CHANNEL_PV_VOLTAGE = "pvVoltage";
    public static final String CHANNEL_PV_CURRENT = "pvCurrent";

    // Energy System Channels
    public static final String CHANNEL_ONGRID_POWER = "ongridPower";
    public static final String CHANNEL_OFFGRID_POWER = "offgridPower";
    public static final String CHANNEL_BATTERY_POWER = "batteryPower";
    public static final String CHANNEL_TOTAL_PV_ENERGY = "totalPvEnergy";
    public static final String CHANNEL_TOTAL_GRID_OUTPUT_ENERGY = "totalGridOutputEnergy";
    public static final String CHANNEL_TOTAL_GRID_INPUT_ENERGY = "totalGridInputEnergy";
    public static final String CHANNEL_TOTAL_LOAD_ENERGY = "totalLoadEnergy";
    public static final String CHANNEL_OPERATING_MODE = "operatingMode";

    // Energy System Control Channels (Writable)
    public static final String CHANNEL_MODE_SELECT = "modeSelect";
    public static final String CHANNEL_PASSIVE_POWER = "passivePower";
    public static final String CHANNEL_PASSIVE_COUNTDOWN = "passiveCountdown";
    public static final String CHANNEL_PASSIVE_ACTIVATE = "passiveActivate";
    public static final String CHANNEL_MANUAL_ACTIVATE = "manualActivate";

    // Manual Mode Time Period Channels (4 periods)
    public static final String CHANNEL_GROUP_PERIOD_PREFIX = "timePeriod";
    public static final String CHANNEL_PERIOD_ENABLED = "enabled";
    public static final String CHANNEL_PERIOD_START = "start";
    public static final String CHANNEL_PERIOD_END = "end";
    public static final String CHANNEL_PERIOD_WEEKDAYS = "weekdays";
    public static final String CHANNEL_PERIOD_POWER = "power";

    // Energy Meter Channels
    public static final String CHANNEL_CT_STATE = "ctState";
    public static final String CHANNEL_PHASE_A_POWER = "phaseAPower";
    public static final String CHANNEL_PHASE_B_POWER = "phaseBPower";
    public static final String CHANNEL_PHASE_C_POWER = "phaseCPower";
    public static final String CHANNEL_TOTAL_METER_POWER = "totalMeterPower";

    // WiFi Status Channels
    public static final String CHANNEL_WIFI_RSSI = "wifiRssi";
    public static final String CHANNEL_WIFI_SSID = "wifiSsid";
    public static final String CHANNEL_IP_ADDRESS = "ipAddress";

    // General Channels
    public static final String CHANNEL_LAST_UPDATE = "lastUpdate";
}
