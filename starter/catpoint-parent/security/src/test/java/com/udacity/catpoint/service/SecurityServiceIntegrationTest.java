package com.udacity.catpoint.service;

import com.udacity.catpoint.data.*;
import com.udacity.catpoint.image.ImageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SecurityServiceIntegrationTest {

    private FakeSecurityRepository securityRepository;
    
    @Mock
    private ImageService imageService;
    
    private SecurityService securityService;
    private Sensor sensor;

    @BeforeEach
    void init() {
        securityRepository = new FakeSecurityRepository();
        securityService = new SecurityService(securityRepository, imageService);
        sensor = new Sensor("Front Door", SensorType.DOOR);
        securityService.addSensor(sensor);
    }

    @Test
    void testArmedAwayAndSensorActivated_transitionsToPending() {
        securityService.setArmingStatus(ArmingStatus.ARMED_AWAY);
        assertEquals(AlarmStatus.NO_ALARM, securityService.getAlarmStatus());

        securityService.changeSensorActivationStatus(sensor, true);
        assertEquals(AlarmStatus.PENDING_ALARM, securityService.getAlarmStatus());
    }

    @Test
    void testArmedAwayAndSensorActivatedTwice_transitionsToAlarm() {
        securityService.setArmingStatus(ArmingStatus.ARMED_AWAY);
        
        securityService.changeSensorActivationStatus(sensor, true);
        assertEquals(AlarmStatus.PENDING_ALARM, securityService.getAlarmStatus());

        securityService.changeSensorActivationStatus(sensor, true);
        assertEquals(AlarmStatus.ALARM, securityService.getAlarmStatus());
    }

    @Test
    void testPendingAlarmAndSensorDeactivated_transitionsToNoAlarm() {
        securityService.setArmingStatus(ArmingStatus.ARMED_AWAY);
        securityService.changeSensorActivationStatus(sensor, true);
        assertEquals(AlarmStatus.PENDING_ALARM, securityService.getAlarmStatus());

        securityService.changeSensorActivationStatus(sensor, false);
        assertEquals(AlarmStatus.NO_ALARM, securityService.getAlarmStatus());
    }

    @Test
    void testAlarmState_sensorChangesDoNotAffectAlarm() {
        securityService.setArmingStatus(ArmingStatus.ARMED_AWAY);
        
        // Trigger alarm
        securityService.changeSensorActivationStatus(sensor, true);
        securityService.changeSensorActivationStatus(sensor, true);
        assertEquals(AlarmStatus.ALARM, securityService.getAlarmStatus());

        // Deactivate sensor
        securityService.changeSensorActivationStatus(sensor, false);
        assertEquals(AlarmStatus.ALARM, securityService.getAlarmStatus());
    }

    @Test
    void testCatDetectedWhenArmedHome_transitionsToAlarm() {
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);
        when(imageService.imageContainsCat(any(BufferedImage.class), anyFloat())).thenReturn(true);

        BufferedImage img = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        securityService.processImage(img);

        assertEquals(AlarmStatus.ALARM, securityService.getAlarmStatus());
    }

    @Test
    void testNoCatDetectedAndSensorsInactive_transitionsToNoAlarm() {
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);
        securityService.changeSensorActivationStatus(sensor, true);
        assertEquals(AlarmStatus.PENDING_ALARM, securityService.getAlarmStatus());

        // Deactivate sensor so all sensors are inactive
        securityService.changeSensorActivationStatus(sensor, false);
        assertEquals(AlarmStatus.NO_ALARM, securityService.getAlarmStatus());

        // Now process image without cat
        when(imageService.imageContainsCat(any(BufferedImage.class), anyFloat())).thenReturn(false);
        BufferedImage img = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        securityService.processImage(img);

        assertEquals(AlarmStatus.NO_ALARM, securityService.getAlarmStatus());
    }
}
