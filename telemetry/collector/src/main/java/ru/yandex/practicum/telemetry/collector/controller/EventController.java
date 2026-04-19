package ru.yandex.practicum.telemetry.collector.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.telemetry.collector.dto.hub.HubEventDto;
import ru.yandex.practicum.telemetry.collector.dto.sensor.SensorEventDto;
import ru.yandex.practicum.telemetry.collector.service.EventService;

@Slf4j
@RestController
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @PostMapping("/events/sensors")
    public void collectSensorEvent(@Valid @RequestBody SensorEventDto event) {
        log.info("Received sensor event: {}", event);
        eventService.processSensorEvent(event);
    }

    @PostMapping("/events/hubs")
    public void collectHubEvent(@Valid @RequestBody HubEventDto event) {
        log.info("Received hub event: {}", event);
        eventService.processHubEvent(event);
    }
}