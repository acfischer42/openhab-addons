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

import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.modbus.handler.BaseModbusThingHandler;
import org.openhab.core.io.transport.modbus.AsyncModbusFailure;
import org.openhab.core.io.transport.modbus.AsyncModbusReadResult;
import org.openhab.core.io.transport.modbus.ModbusBitUtilities;
import org.openhab.core.io.transport.modbus.ModbusReadFunctionCode;
import org.openhab.core.io.transport.modbus.ModbusReadRequestBlueprint;
import org.openhab.core.io.transport.modbus.ModbusRegisterArray;
import org.openhab.core.io.transport.modbus.ModbusWriteRegisterRequestBlueprint;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.unit.Units;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handler for Marstek Venus battery systems via Modbus TCP.
 * Reads holding registers and supports write commands for control registers.
 *
 * @author Achim Fischer - Initial contribution
 */
@NonNullByDefault
public class MarstekVenusHandler extends BaseModbusThingHandler {

    private static final int MAX_REGISTER_SPAN = 10;
    private static final Map<String, Integer> WRITE_REGISTER_MAP = Map.of( //
            "force-mode", 42010, //
            "charge-to-soc", 42011, //
            "set-charge-power", 42020, //
            "set-discharge-power", 42021, //
            "user-work-mode", 43000, //
            "backup-function", 41200, //
            "rs485-control-mode", 42000);

    private static final class ModbusRequest {

        private final Deque<MarstekRegisters> registers;
        private final ModbusReadRequestBlueprint blueprint;

        public ModbusRequest(Deque<MarstekRegisters> registers, int slaveId, int tries) {
            this.registers = registers;
            this.blueprint = initReadRequest(registers, slaveId, tries);
        }

        private ModbusReadRequestBlueprint initReadRequest(Deque<MarstekRegisters> registers, int slaveId, int tries) {
            int firstRegister = registers.getFirst().getRegisterAddress();
            int lastRegister = registers.getLast().getRegisterAddress();
            int length = lastRegister - firstRegister + registers.getLast().getRegisterCount();

            return new ModbusReadRequestBlueprint(slaveId, ModbusReadFunctionCode.READ_MULTIPLE_REGISTERS,
                    firstRegister, length, tries);
        }
    }

    private final Logger logger = LoggerFactory.getLogger(MarstekVenusHandler.class);

    private List<ModbusRequest> modbusRequests = new ArrayList<>();

    public MarstekVenusHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void modbusInitialize() {
        final MarstekConfiguration config = getConfigAs(MarstekConfiguration.class);

        if (config.pollInterval <= 0) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "Invalid poll interval: " + config.pollInterval);
            return;
        }

        if (config.maxTries <= 0) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "Invalid maximum tries: " + config.maxTries);
            return;
        }

        this.updateStatus(ThingStatus.UNKNOWN);
        this.modbusRequests = buildRequests(config.maxTries);

        for (ModbusRequest request : modbusRequests) {
            registerRegularPoll(request.blueprint, config.pollInterval, 0,
                    (AsyncModbusReadResult result) -> this.readSuccessful(request, result), this::readError);
        }
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (command instanceof RefreshType) {
            for (ModbusRequest request : this.modbusRequests) {
                submitOneTimePoll(request.blueprint,
                        (AsyncModbusReadResult result) -> this.readSuccessful(request, result), this::readError);
            }
            return;
        }

        String channelId = channelUID.getIdWithoutGroup();
        Integer registerAddress = WRITE_REGISTER_MAP.get(channelId);
        if (!"control".equals(channelUID.getGroupId()) || registerAddress == null) {
            logger.debug("Ignoring command {} for non-writable channel {}", command, channelUID);
            return;
        }

        final int value;
        try {
            value = commandValue(channelId, command);
        } catch (IllegalArgumentException | ArithmeticException e) {
            logger.warn("Rejected command {} for {}: {}", command, channelUID, e.getMessage());
            return;
        }
        writeRegister(registerAddress, value);
    }

    private static int commandValue(String channelId, Command command) {
        if ("rs485-control-mode".equals(channelId) || "backup-function".equals(channelId)) {
            if (!(command instanceof OnOffType onOff)) {
                throw new IllegalArgumentException("Expected ON or OFF");
            }
            return "rs485-control-mode".equals(channelId) ? (onOff == OnOffType.ON ? 21930 : 21947)
                    : (onOff == OnOffType.ON ? 0 : 1);
        }

        BigDecimal value;
        if (command instanceof QuantityType<?> quantity) {
            QuantityType<?> normalized = switch (channelId) {
                case "set-charge-power", "set-discharge-power" -> quantity.toUnit(Units.WATT);
                case "charge-to-soc" -> quantity.toUnit(Units.PERCENT);
                default -> throw new IllegalArgumentException("Expected a unitless mode number");
            };
            if (normalized == null) {
                throw new IllegalArgumentException("Incompatible command unit");
            }
            value = normalized.toBigDecimal();
        } else if (command instanceof DecimalType decimal) {
            value = decimal.toBigDecimal();
        } else {
            throw new IllegalArgumentException("Expected a numeric command");
        }

        int result = value.intValueExact();
        int maximum = switch (channelId) {
            case "force-mode", "user-work-mode" -> 2;
            case "charge-to-soc" -> 100;
            default -> 65535;
        };
        if (result < 0 || result > maximum) {
            throw new IllegalArgumentException("Expected an integer between 0 and " + maximum);
        }
        return result;
    }

    private void writeRegister(int address, int value) {
        MarstekConfiguration config = getConfigAs(MarstekConfiguration.class);
        ModbusRegisterArray registerArray = new ModbusRegisterArray(value);
        ModbusWriteRegisterRequestBlueprint request = new ModbusWriteRegisterRequestBlueprint(getSlaveId(), address,
                registerArray, false, config.maxTries);

        submitOneTimeWrite(request, result -> {
            logger.debug("Successfully wrote value {} to register {}", value, address);
        }, failure -> {
            logger.warn("Failed to write value {} to register {}: {}", value, address, failure.getCause().getMessage());
        });
    }

    private List<ModbusRequest> buildRequests(int tries) {
        final List<ModbusRequest> requests = new ArrayList<>();
        Deque<MarstekRegisters> currentRequest = new ArrayDeque<>();
        int currentRequestFirstRegister = 0;

        MarstekRegisters[] sorted = MarstekRegisters.values();
        Arrays.sort(sorted, Comparator.comparingInt(MarstekRegisters::getRegisterAddress));

        for (MarstekRegisters register : sorted) {
            if (currentRequest.isEmpty()) {
                currentRequest.add(register);
                currentRequestFirstRegister = register.getRegisterAddress();
            } else {
                int sizeWithRegisterAdded = register.getRegisterAddress() - currentRequestFirstRegister
                        + register.getRegisterCount();
                if (sizeWithRegisterAdded > MAX_REGISTER_SPAN) {
                    requests.add(new ModbusRequest(currentRequest, getSlaveId(), tries));
                    currentRequest = new ArrayDeque<>();
                    currentRequest.add(register);
                    currentRequestFirstRegister = register.getRegisterAddress();
                } else {
                    currentRequest.add(register);
                }
            }
        }

        if (!currentRequest.isEmpty()) {
            requests.add(new ModbusRequest(currentRequest, getSlaveId(), tries));
        }
        logger.debug("Created {} modbus request groups for Marstek Venus.", requests.size());
        return requests;
    }

    private void readSuccessful(ModbusRequest request, AsyncModbusReadResult result) {
        result.getRegisters().ifPresent(registers -> {
            if (getThing().getStatus() != ThingStatus.ONLINE) {
                updateStatus(ThingStatus.ONLINE);
            }

            int firstRegister = request.registers.getFirst().getRegisterAddress();

            for (MarstekRegisters register : request.registers) {
                int index = register.getRegisterAddress() - firstRegister;

                ModbusBitUtilities.extractStateFromRegisters(registers, index, register.getType())
                        .map(register::createState).ifPresent(v -> updateState(createChannelUid(register), v));
            }
        });
    }

    private void readError(AsyncModbusFailure<ModbusReadRequestBlueprint> error) {
        logger.debug("Failed to read Marstek Modbus data", error.getCause());
        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                "Failed to retrieve data: " + error.getCause().getMessage());
    }

    private ChannelUID createChannelUid(MarstekRegisters register) {
        return new ChannelUID(thing.getUID(), register.getChannelGroup(), register.getChannelName());
    }
}
