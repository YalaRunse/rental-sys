package com.rental;

import com.rental.common.*;
import com.rental.service.RentalRules;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.time.LocalDate;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

/** 验证领域边界而不是复述实现：月末、闰年、金额精度和超收拒绝。 */
class RentalRulesTest {
    @Test void monthEndLease() {assertEquals(LocalDate.of(2026,2,27),RentalRules.endDate(LocalDate.of(2026,1,31),1));}
    @Test void leapYearLease() {assertEquals(LocalDate.of(2024,2,28),RentalRules.endDate(LocalDate.of(2024,1,31),1));}
    @Test void yearBoundary() {assertEquals(LocalDate.of(2027,1,14),RentalRules.endDate(LocalDate.of(2026,10,15),3));}
    @ParameterizedTest @ValueSource(ints={-1,0,25,100}) void invalidDuration(int months) {assertThrows(BusinessException.class,()->RentalRules.endDate(LocalDate.now(),months));}
    @Test void invalidDate() {assertThrows(BusinessException.class,()->RentalRules.date("2026-02-30"));}
    @Test void exactPartialPayment() {assertEquals(new BigDecimal("100.30"),RentalRules.received(new BigDecimal("200.00"),new BigDecimal("100.10"),new BigDecimal("0.20")));}
    @Test void fullPayment() {assertEquals(new BigDecimal("200.00"),RentalRules.received(new BigDecimal("200.00"),new BigDecimal("100.10"),new BigDecimal("99.90")));}
    @Test void overPayment() {assertThrows(BusinessException.class,()->RentalRules.received(new BigDecimal("100"),new BigDecimal("90"),new BigDecimal("10.01")));}
    @Test void negativePayment() {assertThrows(BusinessException.class,()->RentalRules.received(new BigDecimal("100"),BigDecimal.ZERO,new BigDecimal("-1")));}
    @Test void excessivePrecision() {assertThrows(BusinessException.class,()->RentalRules.received(new BigDecimal("100"),BigDecimal.ZERO,new BigDecimal("0.001")));}
    @Test void invalidPagination() {assertThrows(BusinessException.class,()->Page.offset(0,10));assertThrows(BusinessException.class,()->Page.offset(1,101));}
    @Test void paginationOffset() {assertEquals(20,Page.offset(3,10));}
}
