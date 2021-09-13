복슬코인 테이블 설계서
=========================

1. AA_TRADE: 거래내역
    | Column Name | Attribute Name | Key | Type     | Len | Not Null | Description                             |
    | ----------- | -------------- | --- | -------- | --- | -------- | --------------------------------------- |
    | TRADE_SEQ   | 키             | PK  | INTEGER  | 4   | Y        |                                         |
    | MARKET      | 코인 이름      |     | VARCHAR  | 20  | Y        | KRW-BTC, KRW-ETH, ...                   |
    | TRADE_TYPE  | 매수/매도      |     | VARCHAR  | 20  | Y        | BUY, SELL                               |
    | AMOUNT      | 거래 금액      |     | NUMBER   |     | Y        | 매수:매도 금액, 매도: 거래 후 입금 금액 |
    | UNIT_PRICE  | 거래 단가      |     | NUMBER   |     | Y        |                                         |
    | REG_DATE    | 거래 시간      |     | DATETIME |     | Y        |                                         |

1. BA_ASSET_HISTORY:  자산 기록
    | Column Name       | Attribute Name | Key | Type     | Len | Not Null | Description                        |
    | ----------------- | -------------- | --- | -------- | --- | -------- | ---------------------------------- |
    | ASSET_HISTORY_SEQ | 키             | PK  | INTEGER  | 4   | Y        |                                    |
    | CURRENCY          | 자산 종류      |     | VARCHAR  | 20  | Y        | KRW, BTC, ETH, ...                 |
    | PRICE             | 원화 환산 금액 |     | NUMBER   |     | Y        | BUY, SELL                          |
    | YIELD             | 수익률         |     | NUMBER   |     | Y        | 소수로 표현, 1->100%, -0.02 -> -2% |
    | REG_DATE          | 자산 조회 시간 |     | DATETIME |     | Y        |                                    |

1. CANDLE: 캔드 실세 - 백테스트 용도
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


