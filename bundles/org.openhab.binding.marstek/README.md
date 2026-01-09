# Marstek Binding
Disclaimer -- AI generated document --

This binding integrates Marstek energy storage systems (Venus C/D/E series) with openHAB.
It allows you to monitor battery status, solar generation, grid power, energy meters, and system operating modes via the Marstek Device Open API (Rev 1.0).

Marstek devices communicate with third-party systems over a Local Area Network (LAN).
Before using this binding, please ensure that your Marstek device is properly connected to your home network and the Open API feature has been enabled via the Marstek mobile app.

Please note that different Marstek models may support only a subset of the commands.
This binding has been tested with the VenusE 3.0 model.

## Supported Things

This binding supports one thing type:

- `battery`: Represents a Marstek energy storage device exposed via the Marstek Device Open API (Rev 1.0)

## Discovery

Discovery is not supported by this binding.
Things must be configured manually.

## Thing Configuration

### `battery` Thing Configuration

| Name            | Type    | Description                           | Default | Required | Advanced |
|-----------------|---------|---------------------------------------|---------|----------|----------|
| hostname        | text    | Hostname or IP address of the device  | N/A     | yes      | no       |
| port            | integer | UDP port number                       | 30000   | no       | no       |
| refreshInterval | integer | Interval the device is polled in sec. | 30      | no       | yes      |

## Channels

### Battery Channels

| Channel              | Type                 | Read/Write | Description                                    |
|----------------------|----------------------|------------|------------------------------------------------|
| batterySoc           | Number:Dimensionless | R          | Battery State of Charge (0-100%)               |
| batteryTemperature   | Number:Temperature   | R          | Battery Temperature                            |
| batteryCapacity      | Number:Energy        | R          | Current Battery Capacity                       |
| batteryRatedCapacity | Number:Energy        | R          | Rated Battery Capacity                         |
| chargingFlag         | Switch               | R          | Battery Charging Status                        |
| dischargingFlag      | Switch               | R          | Battery Discharging Status                     |

### Photovoltaic (PV) Channels

| Channel   | Type              | Read/Write | Description              |
|-----------|-------------------|------------|--------------------------|
| pvPower   | Number:Power      | R          | Photovoltaic Power       |
| pvVoltage | Number:ElectricPotential | R   | Photovoltaic Voltage     |
| pvCurrent | Number:ElectricCurrent   | R   | Photovoltaic Current     |

### Energy System Channels

| Channel                 | Type         | Read/Write | Description                       |
|-------------------------|--------------|------------|-----------------------------------|
| ongridPower             | Number:Power | R          | On-Grid Power                     |
| offgridPower            | Number:Power | R          | Off-Grid Power                    |
| batteryPower            | Number:Power | R          | Battery Power                     |
| totalPvEnergy           | Number:Energy| R          | Total Photovoltaic Energy         |
| totalGridOutputEnergy   | Number:Energy| R          | Total Grid Output Energy          |
| totalGridInputEnergy    | Number:Energy| R          | Total Grid Input Energy           |
| totalLoadEnergy         | Number:Energy| R          | Total Load Energy                 |
| operatingMode           | String       | R          | Operating Mode                    |

### Energy Meter Channels

| Channel         | Type         | Read/Write | Description                |
|-----------------|--------------|------------|----------------------------|
| ctState         | Switch       | R          | CT (Current Transformer) State |
| phaseAPower     | Number:Power | R          | Meter Phase A Power        |
| phaseBPower     | Number:Power | R          | Meter Phase B Power        |
| phaseCPower     | Number:Power | R          | Meter Phase C Power        |
| totalMeterPower | Number:Power | R          | Total Meter Power          |

### WiFi Channels

| Channel   | Type   | Read/Write | Description        |
|-----------|--------|------------|--------------------|
| wifiRssi  | Number | R          | WiFi Signal Strength (RSSI) |
| wifiSsid  | String | R          | WiFi Network Name (SSID) |
| ipAddress | String | R          | IP Address         |

### General Channels

| Channel    | Type             | Read/Write | Description           |
|------------|------------------|------------|-----------------------|
| lastUpdate | DateTime         | R          | Last Update Timestamp |

## Full Example

### Thing Configuration

```java
Thing marstek:battery:mydevice "Marstek Battery" [ hostname="192.168.188.145", port=30000, refreshInterval=30 ]
```

### Item Configuration

```java
// Battery
Number:Dimensionless BatterySoc "Battery SOC [%d %%]" { channel="marstek:battery:mydevice:batterySoc" }
Number:Temperature BatteryTemp "Battery Temperature [%.1f %unit%]" { channel="marstek:battery:mydevice:batteryTemperature" }
Number:Energy BatteryCapacity "Battery Capacity [%.2f kWh]" { channel="marstek:battery:mydevice:batteryCapacity" }
Number:Energy BatteryRatedCapacity "Battery Rated Capacity [%.2f kWh]" { channel="marstek:battery:mydevice:batteryRatedCapacity" }
Switch BatteryCharging "Battery Charging" { channel="marstek:battery:mydevice:chargingFlag" }
Switch BatteryDischarging "Battery Discharging" { channel="marstek:battery:mydevice:dischargingFlag" }

// Photovoltaic
Number:Power PvPower "PV Power [%.0f W]" { channel="marstek:battery:mydevice:pvPower" }
Number:ElectricPotential PvVoltage "PV Voltage [%.1f V]" { channel="marstek:battery:mydevice:pvVoltage" }
Number:ElectricCurrent PvCurrent "PV Current [%.2f A]" { channel="marstek:battery:mydevice:pvCurrent" }

// Energy System
Number:Power OnGridPower "On-Grid Power [%.0f W]" { channel="marstek:battery:mydevice:ongridPower" }
Number:Power OffGridPower "Off-Grid Power [%.0f W]" { channel="marstek:battery:mydevice:offgridPower" }
Number:Power BatteryPower "Battery Power [%.0f W]" { channel="marstek:battery:mydevice:batteryPower" }
Number:Energy TotalPvEnergy "Total PV Energy [%.2f kWh]" { channel="marstek:battery:mydevice:totalPvEnergy" }
Number:Energy TotalGridOutputEnergy "Total Grid Output [%.2f kWh]" { channel="marstek:battery:mydevice:totalGridOutputEnergy" }
Number:Energy TotalGridInputEnergy "Total Grid Input [%.2f kWh]" { channel="marstek:battery:mydevice:totalGridInputEnergy" }
Number:Energy TotalLoadEnergy "Total Load [%.2f kWh]" { channel="marstek:battery:mydevice:totalLoadEnergy" }
String OperatingMode "Operating Mode [%s]" { channel="marstek:battery:mydevice:operatingMode" }

// Energy Meter
Switch CtState "CT State" { channel="marstek:battery:mydevice:ctState" }
Number:Power PhaseAPower "Phase A Power [%.0f W]" { channel="marstek:battery:mydevice:phaseAPower" }
Number:Power PhaseBPower "Phase B Power [%.0f W]" { channel="marstek:battery:mydevice:phaseBPower" }
Number:Power PhaseCPower "Phase C Power [%.0f W]" { channel="marstek:battery:mydevice:phaseCPower" }
Number:Power TotalMeterPower "Total Meter Power [%.0f W]" { channel="marstek:battery:mydevice:totalMeterPower" }

// WiFi
Number WifiRssi "WiFi RSSI [%d dBm]" { channel="marstek:battery:mydevice:wifiRssi" }
String WifiSsid "WiFi SSID [%s]" { channel="marstek:battery:mydevice:wifiSsid" }
String IpAddress "IP Address [%s]" { channel="marstek:battery:mydevice:ipAddress" }

// General
DateTime LastUpdate "Last Update [%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS]" { channel="marstek:battery:mydevice:lastUpdate" }
```

### Sitemap Configuration

```perl
sitemap marstek label="Marstek Energy System" {
    Frame label="Battery" {
        Text item=BatterySoc
        Text item=BatteryTemp
        Text item=BatteryCapacity
        Text item=BatteryRatedCapacity
        Switch item=BatteryCharging
        Switch item=BatteryDischarging
    }
    
    Frame label="Solar Generation" {
        Text item=PvPower
        Text item=PvVoltage
        Text item=PvCurrent
    }
    
    Frame label="Energy System" {
        Text item=OnGridPower
        Text item=OffGridPower
        Text item=BatteryPower
        Text item=OperatingMode
    }
    
    Frame label="Energy Totals" {
        Text item=TotalPvEnergy
        Text item=TotalGridOutputEnergy
        Text item=TotalGridInputEnergy
        Text item=TotalLoadEnergy
    }
    
    Frame label="Energy Meter" {
        Switch item=CtState
        Text item=PhaseAPower
        Text item=PhaseBPower
        Text item=PhaseCPower
        Text item=TotalMeterPower
    }
    
    Frame label="Network" {
        Text item=WifiRssi
        Text item=WifiSsid
        Text item=IpAddress
    }
    
    Frame label="Status" {
        Text item=LastUpdate
    }
}
```

## Prerequisites

1. Connect your Marstek device to your local network via WiFi or Ethernet
2. Enable the Open API feature in the Marstek mobile app:
   - Open the Marstek mobile app
   - Navigate to device settings
   - Enable "Open API" or "Third-party API" option
3. Note your device's IP address from the app or your router

## Communication Protocol

The binding communicates with Marstek devices using JSON-RPC over UDP on port 30000 (default).
The following API methods are supported:

- `Marstek.GetDevice` - Device information
- `Bat.GetStatus` - Battery status
- `PV.GetStatus` - Photovoltaic status
- `ES.GetStatus` - Energy system status
- `ES.GetMode` - Operating mode
- `EM.GetStatus` - Energy meter status
- `Wifi.GetStatus` - WiFi status

## Troubleshooting

### Thing Shows OFFLINE

1. Verify the device is connected to your network
2. Check the IP address is correct
3. Ensure the Open API feature is enabled in the Marstek mobile app
4. Check firewall settings on your openHAB server

### Channels Not Updating

1. Check the refresh interval (default 30 seconds)
2. Verify your device model supports the specific channels (different models have different capabilities)
3. Check openHAB logs for errors or timeouts

### Partial Channel Support

Different Marstek models support different features.
If certain channels remain NULL, your device model may not support those features.
This is normal behavior and does not indicate a problem with the binding.
