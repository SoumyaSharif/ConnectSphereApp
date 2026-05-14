package com.connectsphere.follow;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"eureka.client.enabled=false",
		"spring.cloud.discovery.enabled=false"
})
class FollowServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
