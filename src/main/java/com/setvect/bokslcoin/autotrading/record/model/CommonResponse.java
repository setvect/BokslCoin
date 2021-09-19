package com.setvect.bokslcoin.autotrading.record.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
public class CommonResponse<T> {
    private final T result;
}
