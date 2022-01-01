
복슬코인 테이블 설계서
=========================

1. AA_TRADE: 거래내역

    | Column Name | Attribute Name | Key | Type     | Len | Not Null | Description                             |
    | ----------- | -------------- | --- | -------- | --- | -------- | --------------------------------------- |
    | TRADE_SEQ   | 일련번호       | PK  | INTEGER  |     | Y        |                                         |
    | MARKET      | 코인 이름      |     | VARCHAR  | 20  | Y        | KRW-BTC, KRW-ETH, ...                   |
    | TRADE_TYPE  | 매수/매도      |     | VARCHAR  | 20  | Y        | BUY, SELL                               |
    | AMOUNT      | 거래 금액      |     | NUMBER   |     | Y        | 매수:매도 금액, 매도: 거래 후 입금 금액 |
    | UNIT_PRICE  | 거래 단가      |     | NUMBER   |     | Y        |                                         |
    | REG_DATE    | 거래 시간      |     | DATETIME |     | Y        |                                         |

2. BA_ASSET_HISTORY:  자산 기록

    | Column Name       | Attribute Name | Key | Type     | Len | Not Null | Description                        |
    | ----------------- | -------------- | --- | -------- | --- | -------- | ---------------------------------- |
    | ASSET_HISTORY_SEQ | 일련번호       | PK  | INTEGER  |     | Y        |                                    |
    | CURRENCY          | 자산 종류      |     | VARCHAR  | 20  | Y        | KRW, BTC, ETH, ...                 |
    | PRICE             | 원화 환산 금액 |     | NUMBER   |     | Y        |                                    |
    | YIELD             | 수익률         |     | NUMBER   |     | Y        | 소수로 표현, 1->100%, -0.02 -> -2% |
    | REG_DATE          | 자산 조회 시간 |     | DATETIME |     | Y        |                                    |

3. WA_CANDLE: 캔드 실세 - 백테스트 용도

    | Column Name          | Attribute Name      | Key | Type     | Len | Not Null | Description                                                              |
    | -------------------- | ------------------- | --- | -------- | --- | -------- | ------------------------------------------------------------------------ |
    | CANDLE_SEQ           | 일련번호            | PK  | INTEGER  |     | Y        |                                                                          |
    | MARKET               | 코인 이름           | IDX | VARCHAR  | 20  | Y        | KRW-BTC, KRW-ETH, ...                                                    |
    | CANDLE_DATE_TIME_UTC | 시세 기준 날짜(UTC) | IDX | DATETIME |     | Y        |                                                                          |
    | CANDLE_DATE_TIME_KST | 시세 기준 날짜(KST) | IDX | DATETIME |     | Y        |                                                                          |
    | PERIOD_TYPE          | 기준 기간           | IDX | VARCHAR  | 20  | Y        | PERIOD_1: 1분봉, PERIOD_60: 60분봉, PERIOD_240: 4시간, PERIOD_1440: 하루 |
    | OPENING_PRICE        | 시가                |     | NUMBER   |     | Y        |                                                                          |
    | HIGH_PRICE           | 고가                |     | NUMBER   |     | Y        |                                                                          |
    | LOW_PRICE            | 저가                |     | NUMBER   |     | Y        |                                                                          |
    | TRADE_PRICE          | 종가                |     | NUMBER   |     | Y        |                                                                          |

4. XA_MABS_CONDITION: 백테스트 조건

    | Column Name            | Attribute Name     | Key | Type     | Len | Not Null | Description              |
    | ---------------------- | ------------------ | --- | -------- | --- | -------- | ------------------------ |
    | BACKTEST_CONDITION_SEQ | 일련번호           | PK  | INTEGER  |     | Y        |                          |
    | MARKET                 | 코인 이름          |     | VARCHAR  | 20  | Y        | KRW-BTC, KRW-ETH,...     |
    | TRADE_PERIOD           | 매매 주기          |     | VARCHAR  | 20  | Y        | P_1440, P_240, P_60, ... |
    | UP_BUY_RATE            | 상승 매수률        |     | NUMBER   |     | Y        |                          |
    | DOWN_BUY_RATE          | 하락 매도률        |     | NUMBER   |     | Y        |                          |
    | SHORT_PERIOD           | 단기 이동평균 기간 |     | INTEGER  |     | Y        |                          |
    | LONG_PERIOD            | 장기 이동평균 기간 |     | INTEGER  |     | Y        |                          |
    | LOSE_STOP_RATE         | 손절 손실율        |     | NUMBER   |     | Y        |                          |
    | COMMENT                | 조건에 대한 설명   |     | VARCHAR  | 20  | N        |                          |
    | REG_DATE               | 등록일             |     | DATETIME |     | Y        |                          |

5. XB_MABS_TRADE: 이평선 돌파 백테스트 매매 건별 정보

    | Column Name            | Attribute Name     | Key | Type     | Len | Not Null | Description       |
    | ---------------------- | ------------------ | --- | -------- | --- | -------- | ----------------- |
    | TRADE_SEQ              | 일련번호           | PK  | INTEGER  |     | Y        |                   |
    | BACKTEST_CONDITION_SEQ | 매매 조건 일련번호 | FK  | INTEGER  |     | Y        | XA_MABS_CONDITION |
    | TRADE_TYPE             | 매수/매도          |     | VARCHAR  | 20  | Y        | BUY, SELL         |
    | HIGH_YIELD             | 최고 수익률        |     | NUMBER   |     | Y        |                   |
    | LOW_YIELD              | 최저 수익률        |     | NUMBER   |     | Y        |                   |
    | MA_SHORT               | 단기 이동평균 가격 |     | NUMBER   |     | Y        |                   |
    | MA_LONG                | 장기 이동평균 가격 |     | NUMBER   |     | Y        |                   |
    | YIELD                  | 매도시 수익률      |     | NUMBER   |     | Y        |                   |
    | UNIT_PRICE             | 거래 단가          |     | NUMBER   |     | Y        |                   |
    | SELL_TYPE              | 매도 이유          |     | VARCHAR  | 20  | N        |                   |
    | TRADE_TIME_KST         | 거래시간(KST 기준) |     | DATETIME |     | Y        |                   |

   - Index
     - TRADE_TIME_KST