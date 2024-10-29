package com.awstraining.backend.business.measurements.controller;

import static java.lang.System.currentTimeMillis;

import java.util.List;

import com.awstraining.backend.api.rest.v1.DeviceIdApi;
import com.awstraining.backend.api.rest.v1.model.Measurement;
import com.awstraining.backend.api.rest.v1.model.Measurements;
import com.awstraining.backend.business.measurements.MeasurementDO;
import com.awstraining.backend.business.measurements.MeasurementService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("device/v1")
class DeviceController implements DeviceIdApi {
    private static final Logger LOGGER = LogManager.getLogger(DeviceController.class);

    private final MeasurementService service;
    private final MeterRegistry registry;

    @Autowired
    public DeviceController(final MeasurementService service, MeterRegistry registry) {
        this.service = service;
        this.registry = registry;
    }

    @Override
    public ResponseEntity<Measurement> publishMeasurements(final String deviceId, final Measurement measurement) {
        Counter counter = registry.counter("publishMeasurements.counter", "key: method value: publishMeasurements");

        LOGGER.info("Publishing measurement for device '{}'", deviceId);
        final MeasurementDO measurementDO = fromMeasurement(deviceId, measurement);
        service.saveMeasurement(measurementDO);
        counter.increment();
        return ResponseEntity.ok(measurement);
    }
    @Override
    public ResponseEntity<Measurements> retrieveMeasurements(final String deviceId) {
        Counter counter = registry.counter("retrieveMeasurements.counter", "key: method value: retrieveMeasurements");
        LOGGER.info("Retrieving all measurements for device '{}'", deviceId);
        final List<Measurement> measurements = service.getMeasurements()
                .stream()
                .map(this::toMeasurement)
                .toList();
        final Measurements measurementsResult = new Measurements();
        measurementsResult.measurements(measurements);
        LOGGER.info("Measurements size '{}'", measurements.size());
        counter.increment();
        return ResponseEntity.ok(measurementsResult);
    }

    private Measurement toMeasurement(final MeasurementDO measurementDO) {
        final Measurement measurement = new Measurement();
        measurement.setTimestamp(measurementDO.getCreationTime());
        measurement.setType(measurementDO.getType());
        measurement.setValue(measurementDO.getValue());
        return measurement;
    }

    private MeasurementDO fromMeasurement(final String deviceId, final Measurement measurement) {
        final MeasurementDO measurementDO = new MeasurementDO();
        measurementDO.setDeviceId(deviceId);
        measurementDO.setType(measurement.getType());
        measurementDO.setValue(measurement.getValue());
        final Long creationTime = measurement.getTimestamp();
        measurementDO.setCreationTime(creationTime == null ? currentTimeMillis() : creationTime);
        return measurementDO;
    }
}
