package com.setvect.bokslcoin.autotrading.record.model;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
public class RangeForm {
    /**
     * 시작 날짜
     */
    private LocalDateTime from;

    /**
     * 종료 날짜
     */
    private LocalDateTime to;
}
