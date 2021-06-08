package com.setvect.bokslcoin.autotrading.date;

import com.setvect.bokslcoin.autotrading.util.ApplicationUtil;
import com.setvect.bokslcoin.autotrading.util.DateRange;
import com.setvect.bokslcoin.autotrading.util.DateUtil;
import org.junit.jupiter.api.Test;

public class DateTimeTestCase {
    @Test
    public void UTC_범위() {
        String fromTimeStr = "00:01:00";
        String toTimeStr = "23:59:00";
        DateRange range = ApplicationUtil.getDateRange(fromTimeStr, toTimeStr);

        System.out.printf("from: %s, to: %s%n", DateUtil.formatDateTime(range.getFrom()), DateUtil.formatDateTime(range.getTo()));
    }

}
