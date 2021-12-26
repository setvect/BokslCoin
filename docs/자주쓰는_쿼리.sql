-- 투자원금 및 평가금액
SELECT CURRENCY,
       PRICE * (1 + NVL(YIELD, 0)) AS EVL_PRICE,
       PRICE,
       REG_DATE,
       YIELD
FROM BA_ASSET_HISTORY

-- 기간별 투자원금 및 평가금액 합
SELECT REG_DATE,
       SUM(PRICE)                       AS PRICE,
       SUM(PRICE * (1 + NVL(YIELD, 0))) AS EVL_PRICE,
       COUNT(YIELD)                     AS COIN_COUNT
FROM BA_ASSET_HISTORY
GROUP BY REG_DATE
ORDER BY REG_DATE DESC;

-- 기간별 투자원금 및 평가금액 합 + 수익률
SELECT REG_DATE,
       SUM(PRICE)                                             PRICE,
       SUM(PRICE * (1 + NVL(YIELD, 0)))                    AS EVL_PRICE,
       COUNT(YIELD)                                        AS COIN_COUNT,
       (SUM(PRICE * (1 + NVL(YIELD, 0))) / SUM(PRICE) - 1) AS YIELD,
FROM BA_ASSET_HISTORY
GROUP BY REG_DATE
ORDER BY REG_DATE DESC



-- ================ 기하평균 기본 ================
select *
from XB_MABS_TRADE
where BACKTEST_CONDITION_SEQ = 12003429
  and TRADE_TYPE = 'SELL'
  and CANDLE_DATE_TIME_KST between '2017-12-24T00:00:00' and '2018-01-10T23:59:59'
order by TRADE_SEQ;

-- 산술평균, 기하평균
select exp(sum(ln(yield + 1)) / count(yield)) - 1 as 기하평균,
       (exp(avg(ln(yield + 1))) - 1)              as 기하평균,
       avg(yield)                                 as 산술평균,
       (exp(sum(ln(yield + 1))) - 1)              as 누적수익률,
       (exp(sum(ln(yield + 1 - 0.001))) - 1)      as `누적수익률(매매비용 0.1%적용)`
from XB_MABS_TRADE
where BACKTEST_CONDITION_SEQ = 12003429
  and CANDLE_DATE_TIME_KST between '2017-12-24T00:00:00' and '2018-01-10T23:59:59'
  and TRADE_TYPE = 'SELL';


-- ================ 작업 ================
select *
from XB_MABS_TRADE
where BACKTEST_CONDITION_SEQ = 12065151
--   and TRADE_TYPE = 'SELL'
  and CANDLE_DATE_TIME_KST between '2017-10-01T00:00:00' and '2021-06-08T23:59:59' -- 전체기간
order by TRADE_SEQ;

select min(yield)
from XB_MABS_TRADE
where BACKTEST_CONDITION_SEQ = 12065151
--   and TRADE_TYPE = 'SELL'
  and CANDLE_DATE_TIME_KST between '2017-10-01T00:00:00' and '2021-06-08T23:59:59' -- 전체기간
;

select *
from XB_MABS_TRADE
where BACKTEST_CONDITION_SEQ = 12123225
  and yield = (
    select min(yield)
    from XB_MABS_TRADE
    where BACKTEST_CONDITION_SEQ = 12123225
      and CANDLE_DATE_TIME_KST between '2017-10-01T00:00:00' and '2021-06-08T23:59:59'
);

-- 산술평균, 기하평균
select (exp(avg(ln(yield + 1))) - 1)              as 기하평균,
       avg(yield)                                 as 산술평균,
       (exp(sum(ln(yield + 1))) - 1)              as 누적수익률,
       (exp(sum(ln(yield + 1 - 0.0010))) - 1)      as `누적수익률(매매비용 0.10%적용)`,
       (exp(sum(ln(yield + 1 - 0.0012))) - 1)      as `누적수익률(매매비용 0.12%적용)`,
       (exp(sum(ln(yield + 1 - 0.0015))) - 1)      as `누적수익률(매매비용 0.15%적용)`
from XB_MABS_TRADE
where BACKTEST_CONDITION_SEQ = 12065151
  and CANDLE_DATE_TIME_KST between '2018-01-06T00:00:00' and '2018-12-15T23:59:59' -- 하락장4(찐하락장)
  and TRADE_TYPE = 'SELL';


-- 시간별 수익률 계산
select (exp(avg(ln(yield + 1))) - 1)              as 기하평균,
       avg(yield)                                 as 산술평균,
       (exp(sum(ln(yield + 1))) - 1)              as 누적수익률,
       (exp(sum(ln(yield + 1 - 0.0010))) - 1)      as `누적수익률(매매비용 0.10%적용)`,
       (exp(sum(ln(yield + 1 - 0.0012))) - 1)      as `누적수익률(매매비용 0.12%적용)`,
       (exp(sum(ln(yield + 1 - 0.0015))) - 1)      as `누적수익률(매매비용 0.15%적용)`
from XB_MABS_TRADE
where BACKTEST_CONDITION_SEQ = 12284727
--   and CANDLE_DATE_TIME_KST between '2020-11-01T00:00:00' and '2021-04-14T23:59:59' -- 상승장
--   and CANDLE_DATE_TIME_KST between '2021-01-01T00:00:00' and '2021-06-08T23:59:59' -- 상승장 후 하락장
--   and CANDLE_DATE_TIME_KST between '2020-05-07T00:00:00' and '2020-10-20T23:59:59' -- 횡보장1
--   and CANDLE_DATE_TIME_KST between '2020-05-08T00:00:00' and '2020-07-26T23:59:59' -- 횡보장2
--   and CANDLE_DATE_TIME_KST between '2019-06-24T00:00:00' and '2020-03-31T23:59:59' -- 횡보+하락장1
--   and CANDLE_DATE_TIME_KST between '2017-12-24T00:00:00' and '2020-03-31T23:59:59' -- 횡보+하락장2
--   and CANDLE_DATE_TIME_KST between '2018-01-01T00:00:00' and '2020-11-19T23:59:59' -- 횡보장3
--   and CANDLE_DATE_TIME_KST between '2021-04-14T00:00:00' and '2021-06-08T23:59:59' -- 하락장1
--   and CANDLE_DATE_TIME_KST between '2017-12-07T00:00:00' and '2018-02-06T23:59:59' -- 하락장2
--   and CANDLE_DATE_TIME_KST between '2018-01-06T00:00:00' and '2018-02-06T23:59:59' -- 하락장3
--   and CANDLE_DATE_TIME_KST between '2018-01-06T00:00:00' and '2018-12-15T23:59:59' -- 하락장4(찐하락장)
--   and CANDLE_DATE_TIME_KST between '2019-06-27T00:00:00' and '2020-03-17T23:59:59' -- 하락장5
--   and CANDLE_DATE_TIME_KST between '2018-01-06T00:00:00' and '2019-08-15T23:59:59' -- 하락장 이후 약간의 상승장
  and CANDLE_DATE_TIME_KST between '2017-10-01T00:00:00' and '2021-06-08T23:59:59' -- 전체기간
  and TRADE_TYPE = 'SELL';
