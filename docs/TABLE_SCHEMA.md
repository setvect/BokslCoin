복슬코인 테이블 설계서
=========================

- CANDLE: 캔드 실세

| Column Name          | Attribute Name      | Key | Type     | Len | Not Null | Description                                                              |
| -------------------- | ------------------- | --- | -------- | --- | -------- | ------------------------------------------------------------------------ |
| CANDLE_SEQ           | 키                  | PK  | INTEGER  | 4   | Y        |                                                                          |
| MARKET               | 코인 이름           | IDX | VARCHAR  | 20  | Y        | KRW-BTC, KRW-ETH, ...                                                    |
| CANDLE_DATE_TIME_UTC | 시세 기준 날짜(UTC) | IDX | DATETIME |     | Y        |                                                                          |
| CANDLE_DATE_TIME_KST | 시세 기준 날짜(KST) | IDX | DATETIME |     | Y        |                                                                          |
| PERIOD_TYPE          | 기준 기간           | IDX | VARCHAR  | 20  | Y        | PERIOD_1: 1분봉, PERIOD_60: 60분봉, PERIOD_240: 4시간, PERIOD_1440: 하루 |
| OPENING_PRICE        | 시가                |     | NUMBER   |     | Y        |                                                                          |
| HIGH_PRICE           | 고가                |     | NUMBER   |     | Y        |                                                                          |
| LOW_PRICE            | 저가                |     | NUMBER   |     | Y        |                                                                          |
| TRADE_PRICE          | 종가                |     | NUMBER   |     | Y        |                                                                          |

