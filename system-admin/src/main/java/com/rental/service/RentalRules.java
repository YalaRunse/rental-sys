package com.rental.service;

import com.rental.common.BusinessException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/** 独立的领域规则可直接单元测试，避免在 Controller 或 SQL 中分散业务条件。 */
public final class RentalRules {
    private RentalRules() { }
    public static LocalDate date(String value) {
        try { return LocalDate.parse(value); }
        catch (DateTimeParseException | NullPointerException error) { throw BusinessException.bad("日期必须为有效的 yyyy-MM-dd"); }
    }
    public static LocalDate endDate(LocalDate start,int months) {
        if (months<1 || months>24 || start.getYear()<2000 || start.getYear()>2100) throw BusinessException.bad("起租年份为 2000—2100，租期为 1—24 个月");
        return start.plusMonths(months).minusDays(1);
    }
    public static BigDecimal received(BigDecimal amount,BigDecimal paid,BigDecimal incoming) {
        if (incoming==null || incoming.signum()<=0) throw BusinessException.bad("收款金额必须大于零");
        try { incoming=incoming.setScale(2,RoundingMode.UNNECESSARY); }
        catch (ArithmeticException error) { throw BusinessException.bad("金额最多两位小数"); }
        BigDecimal total=paid.add(incoming);
        if (total.compareTo(amount)>0) throw BusinessException.conflict("收款金额超过账单剩余应收");
        return total;
    }
}
