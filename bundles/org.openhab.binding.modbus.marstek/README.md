# Modbus Marstek Binding

This binding reads telemetry and exposes control settings for Marstek Venus
battery systems over Modbus TCP. It is implemented as a companion to the
openHAB Modbus binding: configure a Modbus TCP bridge first, then add a
`marstek-venus` Thing to that bridge.

The register map in this binding is based on a Marstek Venus E V3 and has been
confirmed by testing on that model. Although the Thing description mentions
Venus A, D, and E systems, register compatibility with other models and
firmware versions has not been established. Check values carefully when
testing another model.

## Prerequisites and setup

1. Install/enable the openHAB Modbus binding.
2. Enable Modbus TCP on the Marstek device and make sure the openHAB host can
   connect to it.
3. Create a Modbus TCP bridge and a `marstek-venus` Thing attached to it. Set
   the bridge `id` to the Modbus unit/slave ID required by the device.

Configuration via the UI is possible.

Also text files can be used, for example, in a `.things` file:

```java
Bridge modbus:tcp:marstekBridge "Marstek Modbus TCP" [
    host="192.168.1.50",
    port=502,
    id=1
] {
    Thing modbus:marstek-venus:marstekBridge:venus "Marstek Venus" [
        pollInterval=10000,
        maxTries=3
    ]
}
```

Replace the host and unit ID with the values for your installation. The
`pollInterval` is in milliseconds (minimum 1000, default 10000); `maxTries`
sets the retry count for failed reads (default 3).

## Channels and items

The Thing provides grouped channels for battery measurements, grid and backup
measurements, temperatures, energy totals, status, and control settings.
Channels whose IDs start with `control#` report register values/read-backs;
some of those channels also accept write commands as described below.

For example, link an Item to a channel using the full Thing UID and the
`group#channel` ID:

```java
Number:Dimensionless MarstekBatterySOC "Battery [%]" {
    channel="modbus:marstek-venus:marstekBridge:venus:battery#battery-soc"
}
Number:Power MarstekBatteryPower "Battery power [%.0f %unit%]" {
    channel="modbus:marstek-venus:marstekBridge:venus:battery#battery-power"
}
```

The channel IDs below can be used to create Items for any of the available
measurements.

## Modbus register to channel mapping

The addresses below are the decimal addresses passed to the Modbus library by
the binding (the implementation treats them as 0-based wire addresses). Each
row is read as a holding register. `INT16`/`UINT16` values occupy one register;
`INT32`/`UINT32` values occupy two consecutive registers. Scaled values are
calculated by multiplying the raw register value by the factor shown.

| Address | Type | Scale | Channel (`group#channel`) | Description / resulting value |
|--------:|------|------:|---------------------------|-------------------------------|
| 30001 | INT16 | 1 | `battery#battery-power` | Battery power (W); positive means charging, negative discharging |
| 30100 | UINT16 | 0.01 | `battery#battery-voltage` | Battery voltage (V) |
| 30101 | INT16 | 0.01 | `battery#battery-current` | Battery current (A) |
| 34002 | UINT16 | 0.1 | `battery#battery-soc` | Battery state of charge (%) |
| 32105 | UINT16 | 0.001 | `battery#battery-stored-energy` | Stored battery energy (kWh) |
| 34003 | UINT16 | 1 | `battery#battery-cycle-count` | Battery cycle count |
| 37007 | UINT16 | 0.001 | `battery#max-cell-voltage` | Maximum cell voltage (V) |
| 37008 | UINT16 | 0.001 | `battery#min-cell-voltage` | Minimum cell voltage (V) |
| 32200 | UINT16 | 0.1 | `grid#ac-voltage` | AC voltage (V) |
| 32204 | INT16 | 0.1 | `grid#ac-frequency` | AC frequency (Hz) |
| 30006 | INT16 | 1 | `grid#ac-power` | Inverter AC power (W) |
| 37004 | INT16 | 1 | `grid#grid-ct-power` | Grid CT power (W); negative means importing, positive exporting |
| 32300 | UINT16 | 0.1 | `grid#offgrid-voltage` | Off-grid/backup output voltage (V) |
| 35000 | INT16 | 0.1 | `temperature#internal-temperature` | Internal temperature (°C) |
| 35001 | INT16 | 0.1 | `temperature#mos1-temperature` | MOS1 temperature (°C; advanced channel) |
| 35002 | INT16 | 0.1 | `temperature#mos2-temperature` | MOS2 temperature (°C; advanced channel) |
| 35010 | INT16 | 0.1 | `temperature#max-cell-temperature` | Maximum cell temperature (°C) |
| 33000–33001 | UINT32 | 0.001 | `energy#total-charge-energy` | Total charge energy (kWh) |
| 33002–33003 | INT32 | 0.001 | `energy#total-discharge-energy` | Total discharge energy (kWh) |
| 33004–33005 | UINT32 | 0.001 | `energy#daily-charge-energy` | Daily charge energy (kWh) |
| 33006–33007 | INT32 | 0.001 | `energy#daily-discharge-energy` | Daily discharge energy (kWh) |
| 33008–33009 | UINT32 | 0.001 | `energy#monthly-charge-energy` | Monthly charge energy (kWh) |
| 33010–33011 | INT32 | 0.001 | `energy#monthly-discharge-energy` | Monthly discharge energy (kWh) |
| 35100 | UINT16 | 1 | `status#inverter-state` | Inverter state: 0 sleep, 1 standby, 2 charge, 3 discharge, 4 backup, 5 OTA, 6 bypass |
| 36000–36001 | UINT32 | 1 | `status#alarm-status` | Alarm status bitfield (advanced channel) |
| 36100–36101 | UINT32 | 1 | `status#fault-status` | Fault status bitfield (advanced channel) |
| 30300 | UINT16 | 1 | `status#wifi-status` | Wi-Fi status (advanced channel) |
| 30303 | UINT16 | -1 | `status#wifi-rssi` | Wi-Fi signal strength (dBm; advanced channel) |
| 42000 | UINT16 | special | `control#rs485-control-mode` | `ON` when raw value is 21930 (RS485 control enabled); otherwise `OFF` |
| 42010 | UINT16 | 1 | `control#force-mode` | Force mode: 0 none, 1 force charge, 2 force discharge |
| 42011 | UINT16 | 1 | `control#charge-to-soc` | Target state of charge for force charging (%) |
| 42020 | UINT16 | 1 | `control#set-charge-power` | Charge power setpoint (W) |
| 42021 | UINT16 | 1 | `control#set-discharge-power` | Discharge power setpoint (W) |
| 43000 | UINT16 | 1 | `control#user-work-mode` | User work mode: 0 self-consumption, 1 anti-feed, 2 trade/AI |
| 41200 | UINT16 | special | `control#backup-function` | `ON` when raw value is 0; `OFF` otherwise |
| 44002 | UINT16 | 1 | `control#max-charge-power` | Maximum charge power (W) |
| 44003 | UINT16 | 1 | `control#max-discharge-power` | Maximum discharge power (W) |

The binding groups nearby addresses into read requests; all entries are polled
at the Thing's configured `pollInterval`. A failed read marks the Thing offline;
a successful read marks it online again.

## Control write support

The handler submits single-register writes for numeric commands sent to these
channels:

| Channel | Register | Values described by the channel |
|---------|---------:|---------------------------------|
| `control#force-mode` | 42010 | 0 none, 1 force charge, 2 force discharge |
| `control#charge-to-soc` | 42011 | Target SOC (0–100) |
| `control#set-charge-power` | 42020 | Charge power (W) |
| `control#set-discharge-power` | 42021 | Discharge power (W) |
| `control#user-work-mode` | 43000 | 0 self-consumption, 1 anti-feed, 2 trade/AI |

**Caution:** Although `control#rs485-control-mode` and
`control#backup-function` are Switch channels and their values are read, the
current handler only processes numeric write commands. Switch commands to
those channels therefore do not write to the device. Do not rely on them to
enable or disable those functions. The binding does not currently validate
numeric command ranges; verify values and device behavior before enabling
automation that writes to the battery.

## Testing notes

The binding uses holding-register reads and control-register writes. Start with
read-only monitoring and compare reported values against the Marstek app or
device display. Register definitions and scaling can differ by model or
firmware; confirm them for your specific device before using write controls.
