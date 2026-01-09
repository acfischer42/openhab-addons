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

import static org.openhab.binding.marstek.internal.marstekBindingConstants.*;

import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.concurrent.ScheduledFuture;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.library.types.DateTimeType;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.library.unit.SIUnits;
import org.openhab.core.library.unit.Units;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * The {@link marstekHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author acfischer42 - Initial contribution
 */
@NonNullByDefault
public class marstekHandler extends BaseThingHandler {

    private final Logger logger = LoggerFactory.getLogger(marstekHandler.class);
    private final Gson gson = new Gson();

    private @Nullable marstekConfiguration config;
    private @Nullable ScheduledFuture<?> refreshTask;
    private volatile int refreshInterval = 60;
    private volatile boolean disposed = false;

    public marstekHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (command instanceof RefreshType) {
            scheduler.execute(this::refresh);
        }
        // Note: Marstek API is primarily read-only, no write commands implemented
    }

    @Override
    public void initialize() {
        disposed = false;
        config = getConfigAs(marstekConfiguration.class);

        if (config != null && config.refreshInterval > 0) {
            refreshInterval = config.refreshInterval;
        }

        updateStatus(ThingStatus.UNKNOWN);

        scheduler.execute(() -> {
            if (disposed) {
                return;
            }
            
            // Test connectivity
            boolean thingReachable = testConnection();

            if (thingReachable) {
                updateStatus(ThingStatus.ONLINE);
                // Schedule periodic polling
                refreshTask = scheduler.scheduleWithFixedDelay(this::refresh, 0, Math.max(1, refreshInterval),
                        java.util.concurrent.TimeUnit.SECONDS);
            } else {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                        "Could not connect to device");
            }
        });
    }

    @Override
    public void dispose() {
        disposed = true;
        
        ScheduledFuture<?> task = refreshTask;
        if (task != null) {
            task.cancel(true);
            refreshTask = null;
        }
        
        super.dispose();
    }

    private boolean testConnection() {
        try {
            String host = config != null ? config.hostname : "127.0.0.1";
            int port = config != null ? config.port : 30000;

            logger.debug("Testing connection to Marstek device at {}:{}", host, port);
            
            // Send Marstek.GetDevice request with longer timeout for initialization
            String request = "{\"id\":0,\"method\":\"Marstek.GetDevice\",\"params\":{}}";
            byte[] response = MarstekUdpHelper.sendRequest(host, port, request.getBytes(StandardCharsets.UTF_8), 0,
                    5000);

            if (response != null && response.length > 0) {
                logger.debug("Connection test successful, received {} bytes", response.length);
                return true;
            } else {
                logger.warn("Connection test failed: No response from device at {}:{}", host, port);
                return false;
            }
        } catch (Exception e) {
            logger.warn("Connection test failed with exception: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Refresh data from the Marstek device and update channels.
     */
    private void refresh() {
        if (disposed) {
            return;
        }
        
        try {
            String host = config != null ? config.hostname : "127.0.0.1";
            int port = config != null ? config.port : 30000;
            int timeoutMs = 2000;

            // Query all components
            queryBatteryStatus(host, port, timeoutMs);
            queryPvStatus(host, port, timeoutMs);
            queryEnergySystemStatus(host, port, timeoutMs);
            queryEnergySystemMode(host, port, timeoutMs);
            queryEnergyMeterStatus(host, port, timeoutMs);
            queryWifiStatus(host, port, timeoutMs);

            // Update last update timestamp
            safeUpdateState(CHANNEL_LAST_UPDATE, new DateTimeType(ZonedDateTime.now()));
            
            // If we were offline, mark back online
            if (getThing().getStatus() != ThingStatus.ONLINE) {
                updateStatus(ThingStatus.ONLINE);
            }

        } catch (Exception e) {
            logger.debug("Error refreshing Marstek device: {}", e.getMessage());
            // Don't immediately go offline - could be transient network issue
            // Only log the error unless it persists
        }
    }

    private void queryBatteryStatus(String host, int port, int timeoutMs) {
        try {
            String request = "{\"id\":0,\"method\":\"Bat.GetStatus\",\"params\":{\"id\":0}}";
            byte[] response = MarstekUdpHelper.sendRequest(host, port, request.getBytes(StandardCharsets.UTF_8), 0,
                    timeoutMs);

            if (response != null) {
                JsonObject json = gson.fromJson(new String(response, StandardCharsets.UTF_8), JsonObject.class);
                JsonObject result = json.getAsJsonObject("result");

                if (result != null) {
                    updateNumberChannel(CHANNEL_BATTERY_SOC, result, "soc", Units.PERCENT);
                    updateNumberChannel(CHANNEL_BATTERY_TEMPERATURE, result, "bat_temp", SIUnits.CELSIUS);
                    updateNumberChannel(CHANNEL_BATTERY_CAPACITY, result, "bat_capacity", Units.WATT_HOUR);
                    updateNumberChannel(CHANNEL_BATTERY_RATED_CAPACITY, result, "rated_capacity", Units.WATT_HOUR);
                    updateSwitchChannel(CHANNEL_CHARGING_FLAG, result, "charg_flag");
                    updateSwitchChannel(CHANNEL_DISCHARGING_FLAG, result, "dischrg_flag");
                }
            }
        } catch (Exception e) {
            logger.debug("Error querying battery status: {}", e.getMessage());
        }
    }

    private void queryPvStatus(String host, int port, int timeoutMs) {
        try {
            String request = "{\"id\":0,\"method\":\"PV.GetStatus\",\"params\":{\"id\":0}}";
            byte[] response = MarstekUdpHelper.sendRequest(host, port, request.getBytes(StandardCharsets.UTF_8), 0,
                    timeoutMs);

            if (response != null) {
                JsonObject json = gson.fromJson(new String(response, StandardCharsets.UTF_8), JsonObject.class);
                JsonObject result = json.getAsJsonObject("result");

                if (result != null) {
                    updateNumberChannel(CHANNEL_PV_POWER, result, "pv_power", Units.WATT);
                    updateNumberChannel(CHANNEL_PV_VOLTAGE, result, "pv_voltage", Units.VOLT);
                    updateNumberChannel(CHANNEL_PV_CURRENT, result, "pv_current", Units.AMPERE);
                }
            }
        } catch (Exception e) {
            logger.debug("Error querying PV status: {}", e.getMessage());
        }
    }

    private void queryEnergySystemStatus(String host, int port, int timeoutMs) {
        try {
            String request = "{\"id\":0,\"method\":\"ES.GetStatus\",\"params\":{\"id\":0}}";
            byte[] response = MarstekUdpHelper.sendRequest(host, port, request.getBytes(StandardCharsets.UTF_8), 0,
                    timeoutMs);

            if (response != null) {
                JsonObject json = gson.fromJson(new String(response, StandardCharsets.UTF_8), JsonObject.class);
                JsonObject result = json.getAsJsonObject("result");

                if (result != null) {
                    updateNumberChannel(CHANNEL_ONGRID_POWER, result, "ongrid_power", Units.WATT);
                    updateNumberChannel(CHANNEL_OFFGRID_POWER, result, "offgrid_power", Units.WATT);
                    updateNumberChannel(CHANNEL_BATTERY_POWER, result, "bat_power", Units.WATT);
                    updateNumberChannel(CHANNEL_TOTAL_PV_ENERGY, result, "total_pv_energy", Units.WATT_HOUR);
                    updateNumberChannel(CHANNEL_TOTAL_GRID_OUTPUT_ENERGY, result, "total_grid_output_energy",
                            Units.WATT_HOUR);
                    updateNumberChannel(CHANNEL_TOTAL_GRID_INPUT_ENERGY, result, "total_grid_input_energy",
                            Units.WATT_HOUR);
                    updateNumberChannel(CHANNEL_TOTAL_LOAD_ENERGY, result, "total_load_energy", Units.WATT_HOUR);
                }
            }
        } catch (Exception e) {
            logger.debug("Error querying energy system status: {}", e.getMessage());
        }
    }

    private void queryEnergySystemMode(String host, int port, int timeoutMs) {
        try {
            String request = "{\"id\":0,\"method\":\"ES.GetMode\",\"params\":{\"id\":0}}";
            byte[] response = MarstekUdpHelper.sendRequest(host, port, request.getBytes(StandardCharsets.UTF_8), 0,
                    timeoutMs);

            if (response != null) {
                JsonObject json = gson.fromJson(new String(response, StandardCharsets.UTF_8), JsonObject.class);
                JsonObject result = json.getAsJsonObject("result");

                if (result != null && result.has("mode")) {
                    String mode = result.get("mode").getAsString();
                    safeUpdateState(CHANNEL_OPERATING_MODE, new StringType(mode));
                }
            }
        } catch (Exception e) {
            logger.debug("Error querying energy system mode: {}", e.getMessage());
        }
    }

    private void queryEnergyMeterStatus(String host, int port, int timeoutMs) {
        try {
            String request = "{\"id\":0,\"method\":\"EM.GetStatus\",\"params\":{\"id\":0}}";
            byte[] response = MarstekUdpHelper.sendRequest(host, port, request.getBytes(StandardCharsets.UTF_8), 0,
                    timeoutMs);

            if (response != null) {
                JsonObject json = gson.fromJson(new String(response, StandardCharsets.UTF_8), JsonObject.class);
                JsonObject result = json.getAsJsonObject("result");

                if (result != null) {
                    if (result.has("ct_state")) {
                        int ctState = result.get("ct_state").getAsInt();
                        safeUpdateState(CHANNEL_CT_STATE, ctState == 1 ? OnOffType.ON : OnOffType.OFF);
                    }
                    updateNumberChannel(CHANNEL_PHASE_A_POWER, result, "a_power", Units.WATT);
                    updateNumberChannel(CHANNEL_PHASE_B_POWER, result, "b_power", Units.WATT);
                    updateNumberChannel(CHANNEL_PHASE_C_POWER, result, "c_power", Units.WATT);
                    updateNumberChannel(CHANNEL_TOTAL_METER_POWER, result, "total_power", Units.WATT);
                }
            }
        } catch (Exception e) {
            logger.debug("Error querying energy meter status: {}", e.getMessage());
        }
    }

    private void queryWifiStatus(String host, int port, int timeoutMs) {
        try {
            String request = "{\"id\":0,\"method\":\"Wifi.GetStatus\",\"params\":{\"id\":0}}";
            byte[] response = MarstekUdpHelper.sendRequest(host, port, request.getBytes(StandardCharsets.UTF_8), 0,
                    timeoutMs);

            if (response != null) {
                JsonObject json = gson.fromJson(new String(response, StandardCharsets.UTF_8), JsonObject.class);
                JsonObject result = json.getAsJsonObject("result");

                if (result != null) {
                    if (result.has("rssi")) {
                        safeUpdateState(CHANNEL_WIFI_RSSI, new DecimalType(result.get("rssi").getAsInt()));
                    }
                    if (result.has("ssid") && !result.get("ssid").isJsonNull()) {
                        safeUpdateState(CHANNEL_WIFI_SSID, new StringType(result.get("ssid").getAsString()));
                    }
                    if (result.has("sta_ip") && !result.get("sta_ip").isJsonNull()) {
                        safeUpdateState(CHANNEL_IP_ADDRESS, new StringType(result.get("sta_ip").getAsString()));
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("Error querying WiFi status: {}", e.getMessage());
        }
    }

    private void updateNumberChannel(String channelId, JsonObject json, String fieldName,
            javax.measure.Unit<?> unit) {
        if (json.has(fieldName)) {
            JsonElement element = json.get(fieldName);
            if (!element.isJsonNull()) {
                double value = element.getAsDouble();
                safeUpdateState(channelId, new QuantityType<>(value, unit));
            }
        }
    }

    private void updateSwitchChannel(String channelId, JsonObject json, String fieldName) {
        if (json.has(fieldName)) {
            JsonElement element = json.get(fieldName);
            if (!element.isJsonNull()) {
                boolean value = element.getAsBoolean();
                safeUpdateState(channelId, value ? OnOffType.ON : OnOffType.OFF);
            }
        }
    }

    private void safeUpdateState(String channelId, org.openhab.core.types.State state) {
        if (!disposed && getThing().getStatus() == ThingStatus.ONLINE) {
            updateState(channelId, state);
        }
    }
}
