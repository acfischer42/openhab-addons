# Marstek Binding

<img src="doc/logo.png" alt="Marstek Logo" width="25%">

Disclaimer -- (mostly) AI generated document --

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
| operatingMode           | String       | R          | Current Operating Mode            |

### Energy System Control Channels

| Channel              | Type              | Read/Write | Description                                    |
|----------------------|-------------------|------------|------------------------------------------------|
| modeSelect           | String            | W          | Select operating mode (Auto/AI/UPS/Manual/Passive)            |
| passivePower         | Number:Power      | W          | Passive mode target power (0-10000W)          |
| passiveCountdown     | Number:Time       | W          | Passive mode countdown (0-86400s)             |
| passiveActivate      | Switch            | W          | Activate passive mode with configured settings |
| manualActivate       | Switch            | W          | Activate manual mode with configured periods   |
| timePeriod1_enabled  | Switch            | W          | Enable time period 1                          |
| timePeriod1_start    | String            | W          | Period 1 start time (HH:MM)                   |
| timePeriod1_end      | String            | W          | Period 1 end time (HH:MM)                     |
| timePeriod1_weekdays | String            | W          | Period 1 active days                          |
| timePeriod1_power    | Number:Power      | W          | Period 1 power setting (-10000 to 10000W)     |
| timePeriod2_*        | Various           | W          | Time period 2 configuration (same as period 1) |
| timePeriod3_*        | Various           | W          | Time period 3 configuration (same as period 1) |
| timePeriod4_*        | Various           | W          | Time period 4 configuration (same as period 1) |

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

// Energy System Control
String ModeSelect "Mode Selection" { channel="marstek:battery:mydevice:modeSelect" }
Number:Power PassivePower "Passive Power [%.0f W]" { channel="marstek:battery:mydevice:passivePower" }
Number:Time PassiveCountdown "Passive Countdown [%d s]" { channel="marstek:battery:mydevice:passiveCountdown" }
Switch PassiveActivate "Activate Passive Mode" { channel="marstek:battery:mydevice:passiveActivate" }
Switch ManualActivate "Activate Manual Mode" { channel="marstek:battery:mydevice:manualActivate" }
Switch TimePeriod1Enabled "Period 1 Enabled" { channel="marstek:battery:mydevice:timePeriod1_enabled" }
String TimePeriod1Start "Period 1 Start [%s]" { channel="marstek:battery:mydevice:timePeriod1_start" }
String TimePeriod1End "Period 1 End [%s]" { channel="marstek:battery:mydevice:timePeriod1_end" }
String TimePeriod1Weekdays "Period 1 Weekdays [%s]" { channel="marstek:battery:mydevice:timePeriod1_weekdays" }
Number:Power TimePeriod1Power "Period 1 Power [%.0f W]" { channel="marstek:battery:mydevice:timePeriod1_power" }

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
    
    Frame label="Mode Control" {
        Selection item=ModeSelect mappings=["Auto"="Auto", "AI"="AI", "UPS"="UPS"]
        Switch item=PassiveActivate
        Setpoint item=PassivePower minValue=0 maxValue=10000 step=100
        Setpoint item=PassiveCountdown minValue=0 maxValue=3600 step=60
        Switch item=ManualActivate
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

## Operating Modes

The Marstek device supports multiple operating modes that can be controlled via the binding:

### Auto Mode

Automatic mode lets the device manage power flow based on its internal algorithms.

**Usage:**
```java
sendCommand(ModeSelect, "Auto")
```

### AI Mode

AI-powered mode uses advanced algorithms to optimize energy usage.

**Usage:**
```java
sendCommand(ModeSelect, "AI")
```

### UPS Mode

UPS (Uninterruptible Power Supply) mode configures the device to charge at maximum power (2500W) continuously, 24/7. This ensures the battery is always fully charged and ready to provide backup power.

**Usage:**
```java
sendCommand(ModeSelect, "UPS")
```

**What it does:**
- Charges battery at full power (-2500W from grid)
- Active 24 hours a day, 7 days a week
- Ensures maximum backup power availability

### Passive Mode

Passive mode allows you to manually control the battery power for a specific duration. Use positive values to discharge (export to grid) or negative values to charge (import from grid).

**Usage:**
```java
// Set power to 100W (discharge to grid)
sendCommand(PassivePower, 100)

// Set countdown to 300 seconds (5 minutes)
sendCommand(PassiveCountdown, 300)

// Activate passive mode
sendCommand(PassiveActivate, ON)
```

**Examples:**
- Charge at 100W for 5 minutes: `PassivePower=-100, PassiveCountdown=300`
- Discharge at 500W for 10 minutes: `PassivePower=500, PassiveCountdown=600`
- Charge at 2500W for 1 hour: `PassivePower=-2500, PassiveCountdown=3600`

### Manual Mode

Manual mode allows you to configure up to 4 time periods with specific power settings and weekday schedules.

**Usage:**
```java
// Configure Period 1: Charge at 2000W during off-peak hours, weekdays only
sendCommand(TimePeriod1Enabled, ON)
sendCommand(TimePeriod1Start, "00:00")
sendCommand(TimePeriod1End, "06:00")
sendCommand(TimePeriod1Weekdays, "Mon-Fri")
sendCommand(TimePeriod1Power, -2000)  // Negative = charge from grid

// Configure Period 2: Discharge at 1000W during peak hours
sendCommand(TimePeriod2Enabled, ON)
sendCommand(TimePeriod2Start, "17:00")
sendCommand(TimePeriod2End, "21:00")
sendCommand(TimePeriod2Weekdays, "Daily")
sendCommand(TimePeriod2Power, 1000)   // Positive = discharge to grid

// Activate manual mode with configured periods
sendCommand(ManualActivate, ON)
```

**Weekday Format:**
- `Daily` - All days
- `Mon-Fri` - Monday through Friday
- `Sat-Sun` - Saturday and Sunday
- Individual days: `Mon`, `Tue`, `Wed`, `Thu`, `Fri`, `Sat`, `Sun`
- Custom combinations: `Mon,Wed,Fri`

**Power Values:**
- **Negative values** (-10000 to -1): Charge from grid
- **Positive values** (1 to 10000): Discharge to grid
- **Zero**: No power transfer

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

**Query Methods (Read):**
- `Marstek.GetDevice` - Device information
- `Bat.GetStatus` - Battery status
- `PV.GetStatus` - Photovoltaic status
- `ES.GetStatus` - Energy system status
- `ES.GetMode` - Operating mode
- `EM.GetStatus` - Energy meter status
- `Wifi.GetStatus` - WiFi status

**Control Methods (Write):**
- `ES.SetMode` - Set operating mode (Auto, AI, UPS, Passive, Manual)

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
