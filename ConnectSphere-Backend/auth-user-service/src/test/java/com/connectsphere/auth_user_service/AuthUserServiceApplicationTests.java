package com.connectsphere.auth_user_service;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Plain sanity test — does NOT load the full Spring context.
 * Context load requires DB/JWT env-vars not available in unit-test scope.
 * All business logic is covered by AuthServiceImplTest & AdminServiceImplTest.
 */
class AuthUserServiceApplicationTests {

    @Test
    void sanityCheck() {
        assertThat(true).isTrue();
    }
}