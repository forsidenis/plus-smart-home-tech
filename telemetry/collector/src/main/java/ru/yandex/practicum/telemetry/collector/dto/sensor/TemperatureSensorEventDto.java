package ru.yandex.practicum.telemetry.collector.dto.sensor;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter @ToString(callSuper = true)
public class TemperatureSensorEventDto extends SensorEventDto {
    @NotNull
    private Integer temperatureC;
    @NotNull
    private Integer temperatureF;

    @Override
    public SensorEventType getType() {
        return SensorEventType.TEMPERATURE_SENSOR_EVENT;
    }
}