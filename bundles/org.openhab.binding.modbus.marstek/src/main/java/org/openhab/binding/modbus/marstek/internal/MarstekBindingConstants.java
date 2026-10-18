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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.modbus.ModbusBindingConstants;
import org.openhab.core.thing.ThingTypeUID;

/**
 * Constants for the Marstek Modbus binding.
 *
 * @author Achim Fischer - Initial contribution
 */
@NonNullByDefault
public class MarstekBindingConstants {

    public static final ThingTypeUID THING_TYPE_VENUS = new ThingTypeUID(ModbusBindingConstants.BINDING_ID,
            "marstek-venus");
}
