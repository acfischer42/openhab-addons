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

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.openhab.core.io.transport.modbus.ModbusFailureCallback;
import org.openhab.core.io.transport.modbus.ModbusWriteCallback;
import org.openhab.core.io.transport.modbus.ModbusWriteFunctionCode;
import org.openhab.core.io.transport.modbus.ModbusWriteRegisterRequestBlueprint;
import org.openhab.core.io.transport.modbus.ModbusWriteRequestBlueprint;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.builder.ThingBuilder;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;

/**
 * Tests control commands at the Modbus write submission boundary.
 *
 * @author Achim Fischer - Initial contribution
 */
@NonNullByDefault
class MarstekVenusHandlerTest {

    private static final ThingUID THING_UID = new ThingUID("modbus:marstek-venus:test:venus");

    private static class TestHandler extends MarstekVenusHandler {
        private final List<ModbusWriteRegisterRequestBlueprint> writes = new ArrayList<>();

        TestHandler() {
            super(ThingBuilder.create(MarstekBindingConstants.THING_TYPE_VENUS, THING_UID).build());
        }

        @Override
        public int getSlaveId() {
            return 1;
        }

        @Override
        public Future<?> submitOneTimeWrite(ModbusWriteRequestBlueprint request, ModbusWriteCallback resultCallback,
                ModbusFailureCallback<ModbusWriteRequestBlueprint> failureCallback) {
            writes.add(assertInstanceOf(ModbusWriteRegisterRequestBlueprint.class, request));
            return CompletableFuture.completedFuture(null);
        }
    }

    static Stream<Arguments> validCommands() {
        return Stream.of(Arguments.of("rs485-control-mode", OnOffType.ON, 42000, 21930),
                Arguments.of("rs485-control-mode", OnOffType.OFF, 42000, 21947),
                Arguments.of("backup-function", OnOffType.ON, 41200, 0),
                Arguments.of("backup-function", OnOffType.OFF, 41200, 1),
                Arguments.of("force-mode", new DecimalType(2), 42010, 2),
                Arguments.of("force-mode", new DecimalType(0), 42010, 0),
                Arguments.of("user-work-mode", new DecimalType(2), 43000, 2),
                Arguments.of("charge-to-soc", new DecimalType(100), 42011, 100),
                Arguments.of("charge-to-soc", QuantityType.valueOf("50 %"), 42011, 50),
                Arguments.of("charge-to-soc", QuantityType.valueOf("0.5 one"), 42011, 50),
                Arguments.of("set-charge-power", new DecimalType(500), 42020, 500),
                Arguments.of("set-discharge-power", QuantityType.valueOf("500 W"), 42021, 500),
                Arguments.of("set-discharge-power", QuantityType.valueOf("0.5 kW"), 42021, 500),
                Arguments.of("set-charge-power", new DecimalType(0), 42020, 0),
                Arguments.of("set-charge-power", new DecimalType(65535), 42020, 65535));
    }

    @ParameterizedTest
    @MethodSource("validCommands")
    void writesExpectedRegister(String channel, Command command, int address, int value) {
        TestHandler handler = new TestHandler();
        handler.handleCommand(new ChannelUID(THING_UID, "control", channel), command);

        assertEquals(1, handler.writes.size());
        ModbusWriteRegisterRequestBlueprint request = handler.writes.getFirst();
        assertEquals(address, request.getReference());
        assertEquals(1, request.getUnitID());
        assertEquals(3, request.getMaxTries());
        assertEquals(ModbusWriteFunctionCode.WRITE_SINGLE_REGISTER, request.getFunctionCode());
        assertEquals(1, request.getRegisters().size());
        assertEquals(value, request.getRegisters().getRegister(0));
    }

    static Stream<Arguments> invalidCommands() {
        return Stream.of(Arguments.of("force-mode", new DecimalType(-1)),
                Arguments.of("force-mode", new DecimalType(3)), Arguments.of("force-mode", new DecimalType("1.5")),
                Arguments.of("user-work-mode", new DecimalType(3)),
                Arguments.of("user-work-mode", QuantityType.valueOf("1 W")),
                Arguments.of("charge-to-soc", new DecimalType(-1)), Arguments.of("charge-to-soc", new DecimalType(101)),
                Arguments.of("charge-to-soc", QuantityType.valueOf("101 %")),
                Arguments.of("charge-to-soc", QuantityType.valueOf("50 W")),
                Arguments.of("set-charge-power", new DecimalType(-500)),
                Arguments.of("set-discharge-power", new DecimalType(65536)),
                Arguments.of("set-discharge-power", new DecimalType("999999999999")),
                Arguments.of("set-discharge-power", QuantityType.valueOf("0.5005 kW")),
                Arguments.of("set-discharge-power", QuantityType.valueOf("500 V")),
                Arguments.of("set-discharge-power", OnOffType.ON),
                Arguments.of("set-discharge-power", new StringType("500")),
                Arguments.of("rs485-control-mode", new DecimalType(1)),
                Arguments.of("backup-function", new DecimalType(0)));
    }

    @ParameterizedTest
    @MethodSource("invalidCommands")
    void rejectsInvalidCommandsWithoutWriting(String channel, Command command) {
        TestHandler handler = new TestHandler();
        handler.handleCommand(new ChannelUID(THING_UID, "control", channel), command);
        assertTrue(handler.writes.isEmpty());
    }

    @Test
    void doesNotWriteForReadOnlyOrWrongGroupChannels() {
        TestHandler handler = new TestHandler();
        handler.handleCommand(new ChannelUID(THING_UID, "control", "max-discharge-power"), new DecimalType(500));
        handler.handleCommand(new ChannelUID(THING_UID, "battery", "set-discharge-power"), new DecimalType(500));
        handler.handleCommand(new ChannelUID(THING_UID, "control", "force-mode"), RefreshType.REFRESH);
        assertTrue(handler.writes.isEmpty());
    }

    @Test
    void submitsExplicitDischargeControlsWithoutImplicitWrites() {
        TestHandler handler = new TestHandler();
        handler.handleCommand(new ChannelUID(THING_UID, "control", "rs485-control-mode"), OnOffType.ON);
        handler.handleCommand(new ChannelUID(THING_UID, "control", "set-discharge-power"),
                QuantityType.valueOf("0.5 kW"));
        handler.handleCommand(new ChannelUID(THING_UID, "control", "force-mode"), new DecimalType(2));

        assertEquals(List.of(42000, 42021, 42010), handler.writes.stream().map(r -> r.getReference()).toList());
        assertEquals(List.of(21930, 500, 2),
                handler.writes.stream().map(r -> r.getRegisters().getRegister(0)).toList());
    }
}
