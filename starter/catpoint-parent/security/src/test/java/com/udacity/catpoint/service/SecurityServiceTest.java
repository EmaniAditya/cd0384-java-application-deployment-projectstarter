package com.udacity.catpoint.service;

import com.udacity.catpoint.application.StatusListener;
import com.udacity.catpoint.data.*;
import com.udacity.catpoint.image.ImageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SecurityServiceTest {

    @Mock
    private SecurityRepository securityRepository;

    @Mock
    private ImageService imageService;

    @Mock
    private StatusListener statusListener;

    private SecurityService securityService;
    private Sensor sensor;

    @BeforeEach
    void init() {
        securityService = new SecurityService(securityRepository, imageService);
        sensor = new Sensor("Main Door", SensorType.DOOR);
    }

    // 1. If alarm is armed and a sensor becomes activated, put the system into pending alarm status.
    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    void testArmedAndSensorActivated_pendingAlarmStatus(ArmingStatus status) {
        when(securityRepository.getArmingStatus()).thenReturn(status);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);
        sensor.setActive(false);

        securityService.changeSensorActivationStatus(sensor, true);

        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }

    // 2. If alarm is armed and a sensor becomes activated and the system is already pending alarm, set off the alarm.
    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    void testArmedAndSensorActivatedAndPending_alarmStatus(ArmingStatus status) {
        when(securityRepository.getArmingStatus()).thenReturn(status);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        sensor.setActive(false);

        securityService.changeSensorActivationStatus(sensor, true);

        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

    // 3. If pending alarm and all sensors are inactive, return to no alarm state.
    @Test
    void testPendingAndAllSensorsInactive_noAlarmState() {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        sensor.setActive(true);

        Set<Sensor> sensors = new HashSet<>();
        sensors.add(sensor);
        when(securityRepository.getSensors()).thenReturn(sensors);

        securityService.changeSensorActivationStatus(sensor, false);

        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    @Test
    void testPendingAndSomeSensorsActive_staysPendingAlarm() {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        sensor.setActive(true);

        Sensor secondSensor = new Sensor("Window", SensorType.WINDOW);
        secondSensor.setActive(true);

        Set<Sensor> sensors = new HashSet<>();
        sensors.add(sensor);
        sensors.add(secondSensor);
        when(securityRepository.getSensors()).thenReturn(sensors);

        securityService.changeSensorActivationStatus(sensor, false);

        verify(securityRepository, never()).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    // 4. If alarm is active, change in sensor state should not affect the alarm state.
    @Test
    void testAlarmActive_sensorChangeDoesNotAffectAlarmState() {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
        sensor.setActive(false);

        securityService.changeSensorActivationStatus(sensor, true);
        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));

        sensor.setActive(true);
        securityService.changeSensorActivationStatus(sensor, false);
        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }

    // 5. If a sensor is activated while already active and the system is in pending state, change it to alarm state.
    @Test
    void testSensorActivatedWhileAlreadyActiveAndPending_alarmState() {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        sensor.setActive(true);

        securityService.changeSensorActivationStatus(sensor, true);

        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

    // 6. If a sensor is deactivated while already inactive, make no changes to the alarm state.
    @Test
    void testSensorDeactivatedWhileAlreadyInactive_noChanges() {
        sensor.setActive(false);

        securityService.changeSensorActivationStatus(sensor, false);

        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }

    // 7. If the camera image contains a cat while the system is armed-home, put the system into alarm status.
    // 11. If the system is armed-home while the camera shows a cat, set the alarm status to alarm.
    @Test
    void testCatDetectedAndArmedHome_alarmStatus() {
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(imageService.imageContainsCat(any(BufferedImage.class), anyFloat())).thenReturn(true);

        BufferedImage img = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        securityService.processImage(img);

        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

    // 8. If the camera image does not contain a cat, change the status to no alarm as long as the sensors are not active.
    @Test
    void testNoCatDetectedAndSensorsInactive_noAlarm() {
        when(imageService.imageContainsCat(any(BufferedImage.class), anyFloat())).thenReturn(false);
        when(securityRepository.getSensors()).thenReturn(new HashSet<>()); // Empty means no active sensors

        BufferedImage img = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        securityService.processImage(img);

        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    @Test
    void testNoCatDetectedAndSensorsActive_noChanges() {
        when(imageService.imageContainsCat(any(BufferedImage.class), anyFloat())).thenReturn(false);

        sensor.setActive(true);
        Set<Sensor> sensors = new HashSet<>();
        sensors.add(sensor);
        when(securityRepository.getSensors()).thenReturn(sensors);

        BufferedImage img = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        securityService.processImage(img);

        verify(securityRepository, never()).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    // 9. If the system is disarmed, set the status to no alarm.
    @Test
    void testSystemDisarmed_noAlarmStatus() {
        securityService.setArmingStatus(ArmingStatus.DISARMED);

        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
        verify(securityRepository, times(1)).setArmingStatus(ArmingStatus.DISARMED);
    }

    // 10. If the system is armed, reset all sensors to inactive.
    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    void testSystemArmed_resetSensorsToInactive(ArmingStatus status) {
        Sensor s1 = new Sensor("S1", SensorType.DOOR);
        s1.setActive(true);
        Sensor s2 = new Sensor("S2", SensorType.WINDOW);
        s2.setActive(false);

        Set<Sensor> sensors = new HashSet<>();
        sensors.add(s1);
        sensors.add(s2);
        when(securityRepository.getSensors()).thenReturn(sensors);

        securityService.setArmingStatus(status);

        assertFalse(s1.getActive());
        assertFalse(s2.getActive());
        verify(securityRepository, times(2)).updateSensor(any(Sensor.class));
        verify(securityRepository, times(1)).setArmingStatus(status);
    }

    // Coverage & Helper methods tests
    @Test
    void testAddAndRemoveSensor() {
        securityService.addSensor(sensor);
        verify(securityRepository, times(1)).addSensor(sensor);

        securityService.removeSensor(sensor);
        verify(securityRepository, times(1)).removeSensor(sensor);
    }

    @Test
    void testGetters() {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
        assertEquals(AlarmStatus.ALARM, securityService.getAlarmStatus());

        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        assertEquals(ArmingStatus.ARMED_HOME, securityService.getArmingStatus());

        Set<Sensor> sensors = Set.of(sensor);
        when(securityRepository.getSensors()).thenReturn(sensors);
        assertEquals(sensors, securityService.getSensors());
    }

    @Test
    void testStatusListeners() {
        securityService.addStatusListener(statusListener);
        
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(imageService.imageContainsCat(any(BufferedImage.class), anyFloat())).thenReturn(true);

        BufferedImage img = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        securityService.processImage(img);

        verify(statusListener, times(1)).catDetected(true);
        verify(statusListener, times(1)).notify(AlarmStatus.ALARM);

        securityService.removeStatusListener(statusListener);
    }

    @Test
    void testSensorDeactivatedWhenDisarmed() {
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.DISARMED);
        sensor.setActive(false);

        securityService.changeSensorActivationStatus(sensor, true);

        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }
}
