package com.marketplace;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class PasswordHashGeneratorTest {

    @Test
    void printBcryptForSeedScript() {
        String hash = new BCryptPasswordEncoder().encode("Password123!");
        System.out.println(hash);
    }
}
