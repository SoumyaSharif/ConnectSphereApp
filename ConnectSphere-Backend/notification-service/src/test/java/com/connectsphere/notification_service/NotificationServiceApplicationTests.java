package com.connectsphere.notification_service;

import com.connectsphere.notification.NotificationServiceApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = NotificationServiceApplication.class, properties = {
		"eureka.client.enabled=false",
		"spring.cloud.discovery.enabled=false",
		"spring.kafka.listener.auto-startup=false"
})
class NotificationServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
