package com.rental;

import com.rental.auth.PasswordHasher;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** 随机盐、错误口令、损坏格式与旧明文都必须有明确行为。 */
class PasswordHasherTest {
    private final PasswordHasher hasher=new PasswordHasher();
    @Test void saltedHashesAndVerification() {String first=hasher.hash("DemoPass123!");String second=hasher.hash("DemoPass123!");assertNotEquals(first,second);assertTrue(hasher.matches("DemoPass123!",first));assertFalse(hasher.matches("WrongPass123!",first));}
    @Test void plaintextIsRejected() {assertFalse(hasher.matches("111111","111111"));}
    @Test void malformedHashIsRejected() {assertFalse(hasher.matches("password","pbkdf2$"));assertFalse(hasher.matches("password","pbkdf2$bad$salt$hash"));assertFalse(hasher.matches("password",null));}
}
