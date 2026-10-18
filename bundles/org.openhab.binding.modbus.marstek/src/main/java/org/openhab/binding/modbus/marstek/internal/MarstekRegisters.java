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
package org.openhab.binding.modbus.marstek.internal;

import static org.openhab.core.io.transport.modbus.ModbusConstants.ValueType.INT16;
import static org.openhab.core.io.transport.modbus.ModbusConstants.ValueType.INT32;
import static org.openhab.core.io.transport.modbus.ModbusConstants.ValueType.UINT16;
import static org.openhab.core.io.transport.modbus.ModbusConstants.ValueType.UINT32;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.function.Function;

import javax.measure.Unit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.io.transport.modbus.ModbusConstants.ValueType;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.unit.SIUnits;
import org.openhab.core.library.unit.Units;
import org.openhab.core.types.State;

/**
 * Defines the Modbus registers for Marstek Venus battery systems.
 * Register addresses are 0-based (as sent on wire).
 * Based on Venus E V3 register map confirmed via testing.
 *
 * @author Achim Fischer - Initial contribution
 */
@NonNullByDefault
public enum MarstekRegisters {

    // === Battery (fast: power/voltage/current/SOC change constantly) ===
    BATTERY_POWER(30001, INT16, BigDecimal.ONE, quantityFactory(Units.WATT), "battery", true),
    BATTERY_VOLTAGE(30100, UINT16, Scale.S001, quantityFactory(Units.VOLT), "battery", true),
    BATTERY_CURRENT(30101, INT16, Scale.S001, quantityFactory(Units.AMPERE), "battery", true),
    BATTERY_SOC(34002, UINT16, Scale.S01, PercentType::new, "battery", true),
    BATTERY_STORED_ENERGY(32105, UINT16, Scale.S0001, quantityFactory(Units.KILOWATT_HOUR), "battery", false),
    BATTERY_CYCLE_COUNT(34003, UINT16, BigDecimal.ONE, DecimalType::new, "battery", false),
    MAX_CELL_VOLTAGE(37007, UINT16, Scale.S0001, quantityFactory(Units.VOLT), "battery", false),
    MIN_CELL_VOLTAGE(37008, UINT16, Scale.S0001, quantityFactory(Units.VOLT), "battery", false),

    // === AC / Grid (fast: grid values change constantly) ===
    AC_VOLTAGE(32200, UINT16, Scale.S01, quantityFactory(Units.VOLT), "grid", true),
    AC_FREQUENCY(32204, INT16, Scale.S01, quantityFactory(Units.HERTZ), "grid", true),
    AC_POWER(30006, INT16, BigDecimal.ONE, quantityFactory(Units.WATT), "grid", true),
    GRID_CT_POWER(37004, INT16, BigDecimal.ONE, quantityFactory(Units.WATT), "grid", true),

    // === Offgrid / Backup ===
    OFFGRID_VOLTAGE(32300, UINT16, Scale.S01, quantityFactory(Units.VOLT), "grid", true),

    // === Temperatures (fast: temps change with load) ===
    INTERNAL_TEMPERATURE(35000, INT16, Scale.S01, quantityFactory(SIUnits.CELSIUS), "temperature", true),
    MOS1_TEMPERATURE(35001, INT16, Scale.S01, quantityFactory(SIUnits.CELSIUS), "temperature", true),
    MOS2_TEMPERATURE(35002, INT16, Scale.S01, quantityFactory(SIUnits.CELSIUS), "temperature", true),
    MAX_CELL_TEMPERATURE(35010, INT16, Scale.S01, quantityFactory(SIUnits.CELSIUS), "temperature", true),

    // === Energy Totals (slow: only change over minutes/hours) ===
    TOTAL_CHARGE_ENERGY(33000, UINT32, Scale.S001, quantityFactory(Units.KILOWATT_HOUR), "energy", false),
    TOTAL_DISCHARGE_ENERGY(33002, INT32, Scale.S001, quantityFactory(Units.KILOWATT_HOUR), "energy", false),
    DAILY_CHARGE_ENERGY(33004, UINT32, Scale.S001, quantityFactory(Units.KILOWATT_HOUR), "energy", false),
    DAILY_DISCHARGE_ENERGY(33006, INT32, Scale.S001, quantityFactory(Units.KILOWATT_HOUR), "energy", false),
    MONTHLY_CHARGE_ENERGY(33008, UINT32, Scale.S001, quantityFactory(Units.KILOWATT_HOUR), "energy", false),
    MONTHLY_DISCHARGE_ENERGY(33010, INT32, Scale.S001, quantityFactory(Units.KILOWATT_HOUR), "energy", false),

    // === Status (slow: rarely changes) ===
    INVERTER_STATE(35100, UINT16, BigDecimal.ONE, DecimalType::new, "status", false),
    ALARM_STATUS(36000, UINT32, BigDecimal.ONE, DecimalType::new, "status", false),
    FAULT_STATUS(36100, UINT32, BigDecimal.ONE, DecimalType::new, "status", false),

    // === Connectivity (slow) ===
    WIFI_STATUS(30300, UINT16, BigDecimal.ONE, DecimalType::new, "status", false),
    WIFI_RSSI(30303, UINT16, BigDecimal.ONE.negate(), quantityFactory(Units.DECIBEL_MILLIWATTS), "status", false),

    // === Control (slow: read-back of settings) ===
    RS485_CONTROL_MODE(42000, UINT16, BigDecimal.ONE, MarstekRegisters::rs485ControlState, "control", false),
    FORCE_MODE(42010, UINT16, BigDecimal.ONE, DecimalType::new, "control", false),
    CHARGE_TO_SOC(42011, UINT16, BigDecimal.ONE, PercentType::new, "control", false),
    SET_CHARGE_POWER(42020, UINT16, BigDecimal.ONE, quantityFactory(Units.WATT), "control", false),
    SET_DISCHARGE_POWER(42021, UINT16, BigDecimal.ONE, quantityFactory(Units.WATT), "control", false),
    USER_WORK_MODE(43000, UINT16, BigDecimal.ONE, DecimalType::new, "control", false),
    BACKUP_FUNCTION(41200, UINT16, BigDecimal.ONE, MarstekRegisters::backupFunctionState, "control", false),
    MAX_CHARGE_POWER(44002, UINT16, BigDecimal.ONE, quantityFactory(Units.WATT), "control", false),
    MAX_DISCHARGE_POWER(44003, UINT16, BigDecimal.ONE, quantityFactory(Units.WATT), "control", false);

    private static class Scale {
        static final BigDecimal S01 = new BigDecimal(BigInteger.ONE, 1);
        static final BigDecimal S001 = new BigDecimal(BigInteger.ONE, 2);
        static final BigDecimal S0001 = new BigDecimal(BigInteger.ONE, 3);
    }

    private final int registerAddress;
    private final ValueType type;
    private final BigDecimal multiplier;
    private final Function<BigDecimal, State> stateFactory;
    private final String channelGroup;
    private final boolean highPriority;

    MarstekRegisters(int registerAddress, ValueType type, BigDecimal multiplier,
            Function<BigDecimal, State> stateFactory, String channelGroup, boolean highPriority) {
        this.registerAddress = registerAddress;
        this.type = type;
        this.multiplier = multiplier;
        this.stateFactory = stateFactory;
        this.channelGroup = channelGroup;
        this.highPriority = highPriority;
    }

    private static Function<BigDecimal, State> quantityFactory(Unit<?> unit) {
        return (BigDecimal value) -> new QuantityType<>(value, unit);
    }

    private static State backupFunctionState(BigDecimal value) {
        // Inverted: register 0 = outlet ON, register 1 = outlet OFF
        return value.intValue() == 0 ? OnOffType.ON : OnOffType.OFF;
    }

    private static State rs485ControlState(BigDecimal value) {
        // 21930 = RS485 control enabled (binding controls battery), 21947 = disabled
        return value.intValue() == 21930 ? OnOffType.ON : OnOffType.OFF;
    }

    public int getRegisterAddress() {
        return registerAddress;
    }

    public ValueType getType() {
        return type;
    }

    public int getRegisterCount() {
        return this.type.getBits() / 16;
    }

    public String getChannelGroup() {
        return channelGroup;
    }

    public boolean isHighPriority() {
        return highPriority;
    }

    public String getChannelName() {
        return this.name().toLowerCase().replace('_', '-');
    }

    public State createState(DecimalType registerValue) {
        final BigDecimal scaledValue = registerValue.toBigDecimal().multiply(this.multiplier);
        return this.stateFactory.apply(scaledValue);
    }
}
