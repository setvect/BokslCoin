-- ================ 기하평균 기본 ================
delete
from XB_MABS_TRADE
where BACKTEST_CONDITION_SEQ in (13974170, 14088824);

select *
from XB_MABS_TRADE
where BACKTEST_CONDITION_SEQ = 12003429
  and TRADE_TYPE = 'SELL'
  and TRADE_TIME_KST between '2017-12-24T00:00:00' and '2018-01-10T23:59:59'
order by TRADE_SEQ;

-- 산술평균, 기하평균
select count(*)                                         as 매매회숫,
       round(exp(avg(ln(yield + 1))) - 1, 5)            as 기하평균,
       round(avg(yield), 5)                             as 산술평균,
       round(exp(sum(ln(yield + 1))) - 1, 5)            as 누적수익률,
       round((exp(sum(ln(yield + 1 - 0.0010))) - 1), 5) as `누적수익률(매매비용 0.10%적용)`,
       round((exp(sum(ln(yield + 1 - 0.0012))) - 1), 5) as `누적수익률(매매비용 0.12%적용)`,
       round((exp(sum(ln(yield + 1 - 0.0015))) - 1), 5) as `누적수익률(매매비용 0.15%적용)`
from XB_MABS_TRADE
where BACKTEST_CONDITION_SEQ = 12003429
  and TRADE_TIME_KST between '2017-12-24T00:00:00' and '2018-01-10T23:59:59'
  and TRADE_TYPE = 'SELL';


-- ================ 1. 개별 통계 ================
select count(*),
       min(TRADE_TIME_KST),
       max(TRADE_TIME_KST),
       XMC.BACKTEST_CONDITION_SEQ,
       XMC.MARKET,
       XMC.TRADE_PERIOD,
       XMC.SHORT_PERIOD,
       XMC.LONG_PERIOD
from XB_MABS_TRADE XMT
         join XA_MABS_CONDITION XMC on XMT.BACKTEST_CONDITION_SEQ = XMC.BACKTEST_CONDITION_SEQ
group by XMC.BACKTEST_CONDITION_SEQ
order by XMC.BACKTEST_CONDITION_SEQ;

select XMC.MARKET,
       XMC.TRADE_PERIOD,
       XMC.SHORT_PERIOD,
       XMC.LONG_PERIOD,
       XMT.*
from XB_MABS_TRADE XMT
         join XA_MABS_CONDITION XMC on XMT.BACKTEST_CONDITION_SEQ = XMC.BACKTEST_CONDITION_SEQ
where XMT.BACKTEST_CONDITION_SEQ = 27288611
--   and TRADE_TYPE = 'SELL'
  and TRADE_TIME_KST between '2021-11-01T00:00:00' and '2022-12-31T23:59:59' --
order by TRADE_SEQ;

select min(yield)
from XB_MABS_TRADE
where BACKTEST_CONDITION_SEQ = 12065151
--   and TRADE_TYPE = 'SELL'
  and TRADE_TIME_KST between '2017-10-01T00:00:00' and '2021-06-08T23:59:59' -- 전체기간
;

select *
from XB_MABS_TRADE
where BACKTEST_CONDITION_SEQ = 12123225
  and yield = (
    select min(yield)
    from XB_MABS_TRADE
    where BACKTEST_CONDITION_SEQ = 12123225
      and TRADE_TIME_KST between '2017-10-01T00:00:00' and '2021-06-08T23:59:59'
);

-- 산술평균, 기하평균
select count(*)                                         as 매매회숫,
       round(exp(avg(ln(yield + 1))) - 1, 5)            as 기하평균,
       round(avg(yield), 5)                             as 산술평균,
       round(exp(sum(ln(yield + 1))) - 1, 5)            as 누적수익률,
       round((exp(sum(ln(yield + 1 - 0.0010))) - 1), 5) as `누적수익률(매매비용 0.10%적용)`,
       round((exp(sum(ln(yield + 1 - 0.0012))) - 1), 5) as `누적수익률(매매비용 0.12%적용)`,
       round((exp(sum(ln(yield + 1 - 0.0015))) - 1), 5) as `누적수익률(매매비용 0.15%적용)`
from XB_MABS_TRADE
where BACKTEST_CONDITION_SEQ = 12065151
  and TRADE_TIME_KST between '2018-01-06T00:00:00' and '2018-12-15T23:59:59' -- 하락장4(찐하락장)
  and TRADE_TYPE = 'SELL';


-- 시간별 수익률 계산
select count(*)                                         as 매매회숫,
       round(exp(avg(ln(yield + 1))) - 1, 5)            as 기하평균,
       round(avg(yield), 5)                             as 산술평균,
       round(exp(sum(ln(yield + 1))) - 1, 5)            as 누적수익률,
       round((exp(sum(ln(yield + 1 - 0.0010))) - 1), 5) as `누적수익률(매매비용 0.10%적용)`,
       round((exp(sum(ln(yield + 1 - 0.0012))) - 1), 5) as `누적수익률(매매비용 0.12%적용)`,
       round((exp(sum(ln(yield + 1 - 0.0015))) - 1), 5) as `누적수익률(매매비용 0.15%적용)`
from XB_MABS_TRADE
where BACKTEST_CONDITION_SEQ = 12284727
--   and TRADE_TIME_KST between '2020-11-01T00:00:00' and '2021-04-14T23:59:59' -- 상승장
--   and TRADE_TIME_KST between '2021-01-01T00:00:00' and '2021-06-08T23:59:59' -- 상승장 후 하락장
--   and TRADE_TIME_KST between '2020-05-07T00:00:00' and '2020-10-20T23:59:59' -- 횡보장1
--   and TRADE_TIME_KST between '2020-05-08T00:00:00' and '2020-07-26T23:59:59' -- 횡보장2
--   and TRADE_TIME_KST between '2019-06-24T00:00:00' and '2020-03-31T23:59:59' -- 횡보+하락장1
--   and TRADE_TIME_KST between '2017-12-24T00:00:00' and '2020-03-31T23:59:59' -- 횡보+하락장2
--   and TRADE_TIME_KST between '2018-01-01T00:00:00' and '2020-11-19T23:59:59' -- 횡보장3
--   and TRADE_TIME_KST between '2021-04-14T00:00:00' and '2021-06-08T23:59:59' -- 하락장1
--   and TRADE_TIME_KST between '2017-12-07T00:00:00' and '2018-02-06T23:59:59' -- 하락장2
--   and TRADE_TIME_KST between '2018-01-06T00:00:00' and '2018-02-06T23:59:59' -- 하락장3
--   and TRADE_TIME_KST between '2018-01-06T00:00:00' and '2018-12-15T23:59:59' -- 하락장4(찐하락장)
--   and TRADE_TIME_KST between '2019-06-27T00:00:00' and '2020-03-17T23:59:59' -- 하락장5
--   and TRADE_TIME_KST between '2018-01-06T00:00:00' and '2019-08-15T23:59:59' -- 하락장 이후 약간의 상승장
  and TRADE_TIME_KST between '2017-10-01T00:00:00' and '2021-06-08T23:59:59' -- 전체기간
  and TRADE_TYPE = 'SELL';


-- 시간별 수익률 계산
select count(*)                                         as 매매회숫,
       round(exp(avg(ln(yield + 1))) - 1, 5)            as 기하평균,
       round(avg(yield), 5)                             as 산술평균,
       round(exp(sum(ln(yield + 1))) - 1, 5)            as 누적수익률,
       round((exp(sum(ln(yield + 1 - 0.0010))) - 1), 5) as `누적수익률(매매비용 0.10%적용)`,
       round((exp(sum(ln(yield + 1 - 0.0012))) - 1), 5) as `누적수익률(매매비용 0.12%적용)`,
       round((exp(sum(ln(yield + 1 - 0.0015))) - 1), 5) as `누적수익률(매매비용 0.15%적용)`
from XB_MABS_TRADE
where BACKTEST_CONDITION_SEQ = 14283804
--   and TRADE_TIME_KST between '2020-11-01T00:00:00' and '2021-04-14T23:59:59' -- 상승장
--   and TRADE_TIME_KST between '2021-01-01T00:00:00' and '2021-06-08T23:59:59' -- 상승장 후 하락장
--   and TRADE_TIME_KST between '2020-05-07T00:00:00' and '2020-10-20T23:59:59' -- 횡보장1
--   and TRADE_TIME_KST between '2020-05-08T00:00:00' and '2020-07-26T23:59:59' -- 횡보장2
--   and TRADE_TIME_KST between '2019-06-24T00:00:00' and '2020-03-31T23:59:59' -- 횡보+하락장1
--   and TRADE_TIME_KST between '2017-12-24T00:00:00' and '2020-03-31T23:59:59' -- 횡보+하락장2
--   and TRADE_TIME_KST between '2018-01-01T00:00:00' and '2020-11-19T23:59:59' -- 횡보장3
--   and TRADE_TIME_KST between '2021-04-14T00:00:00' and '2021-06-08T23:59:59' -- 하락장1
--   and TRADE_TIME_KST between '2017-12-07T00:00:00' and '2018-02-06T23:59:59' -- 하락장2
--   and TRADE_TIME_KST between '2018-01-06T00:00:00' and '2018-02-06T23:59:59' -- 하락장3
--   and TRADE_TIME_KST between '2018-01-06T00:00:00' and '2018-12-15T23:59:59' -- 하락장4(찐하락장)
--   and TRADE_TIME_KST between '2019-06-27T00:00:00' and '2020-03-17T23:59:59' -- 하락장5
--   and TRADE_TIME_KST between '2018-01-06T00:00:00' and '2019-08-15T23:59:59' -- 하락장 이후 약간의 상승장
  and TRADE_TIME_KST between '2017-10-01T00:00:00' and '2021-06-08T23:59:59' -- 전체기간
  and TRADE_TYPE = 'SELL';

-- ================ 2. 통합 통계 ================

select sum(CASE WHEN YIELD > 0 THEN 1 ELSE 0 END)
from XB_MABS_TRADE;



select XMT.BACKTEST_CONDITION_SEQ,
       XMC.MARKET,
       XMC.TRADE_PERIOD,
       XMC.SHORT_PERIOD,
       XMC.LONG_PERIOD,
       FORMATDATETIME(min(TRADE_TIME_KST), 'yyyyMMdd HHmmss')                  as 시작시간,
       FORMATDATETIME(max(TRADE_TIME_KST), 'yyyyMMdd HHmmss')                  as 종료시간,
       count(*)                                                                as 매매회숫,
       round(exp(avg(ln(yield + 1))) - 1, 5)                                   as 기하평균,
       round(avg(yield), 5)                                                    as 산술평균,
       round(exp(sum(ln(yield + 1))) - 1, 5)                                   as 누적수익률,
       round((exp(sum(ln(yield + 1 - 0.0010))) - 1), 5)                        as `누적수익률(매매비용 0.10%적용)`,
       round(sum(CASE WHEN YIELD > 0 THEN 1 ELSE 0 END) * 100.0 / count(*), 2) as `승률(%)`,
       sum(CASE WHEN YIELD > 0 THEN 1 ELSE 0 END)                              as `승리횟수`,
       sum(CASE WHEN YIELD <= 0 THEN 1 ELSE 0 END)                             as `패배횟수`
--        sum(casewhen(yield = 0, 1, 0))                  as '패배'
--        round((exp(sum(ln(yield + 1 - 0.0012))) - 1), 5) as `누적수익률(매매비용 0.12%적용)`,
--        round((exp(sum(ln(yield + 1 - 0.0015))) - 1), 5) as `누적수익률(매매비용 0.15%적용)`
from XB_MABS_TRADE XMT
         join XA_MABS_CONDITION XMC on XMC.BACKTEST_CONDITION_SEQ = XMT.BACKTEST_CONDITION_SEQ
where TRADE_TYPE = 'SELL'
--   and TRADE_TIME_KST between '2020-11-01T00:00:00' and '2021-04-14T23:59:59' -- 상승장
--   and TRADE_TIME_KST between '2021-01-01T00:00:00' and '2021-06-08T23:59:59' -- 상승장 후 하락장
--   and TRADE_TIME_KST between '2020-05-07T00:00:00' and '2020-10-20T23:59:59' -- 횡보장1
--   and TRADE_TIME_KST between '2020-05-08T00:00:00' and '2020-07-26T23:59:59' -- 횡보장2
--   and TRADE_TIME_KST between '2019-06-24T00:00:00' and '2020-03-31T23:59:59' -- 횡보+하락장1
--   and TRADE_TIME_KST between '2017-12-24T00:00:00' and '2020-03-31T23:59:59' -- 횡보+하락장2
--   and TRADE_TIME_KST between '2018-01-01T00:00:00' and '2020-11-19T23:59:59' -- 횡보장3
--   and TRADE_TIME_KST between '2021-04-14T00:00:00' and '2021-06-08T23:59:59' -- 하락장1
--   and TRADE_TIME_KST between '2017-12-07T00:00:00' and '2018-02-06T23:59:59' -- 하락장2
--   and TRADE_TIME_KST between '2018-01-06T00:00:00' and '2018-02-06T23:59:59' -- 하락장3
--   and TRADE_TIME_KST between '2018-01-06T00:00:00' and '2018-12-15T23:59:59' -- 하락장4(찐하락장)
--   and TRADE_TIME_KST between '2019-06-27T00:00:00' and '2020-03-17T23:59:59' -- 하락장5
--   and TRADE_TIME_KST between '2018-01-06T00:00:00' and '2019-08-15T23:59:59' -- 하락장 이후 약간의 상승장
--   and TRADE_TIME_KST between '2017-10-01T00:00:00' and '2021-06-08T23:59:59' -- 전체기간
--   and TRADE_TIME_KST between '2017-01-01T00:00:00' and '2021-12-31T23:59:59' -- 전체기간(2)
--   and TRADE_TIME_KST between '2021-07-01T00:00:00' and '2021-12-31T23:59:59' --
--   and TRADE_TIME_KST between '2017-01-01T00:00:00' and '2017-12-31T23:59:59' --
--   and TRADE_TIME_KST between '2018-01-01T00:00:00' and '2018-12-31T23:59:59' --
--   and TRADE_TIME_KST between '2019-01-01T00:00:00' and '2019-12-31T23:59:59' --
--   and TRADE_TIME_KST between '2020-01-01T00:00:00' and '2020-12-31T23:59:59' --
--   and TRADE_TIME_KST between '2021-06-14T00:00:00' and '2021-12-31T23:59:59' --
  and MARKET in
      ('KRW-BTC', 'KRW-ETH', 'KRW-XRP', 'KRW-EOS', 'KRW-ETC', 'KRW-ADA', 'KRW-DOT', 'KRW-BCH', 'KRW-BAT', 'KRW-MANA')
  and TRADE_PERIOD = 'P_60'
  and SHORT_PERIOD = 13
group by XMT.BACKTEST_CONDITION_SEQ
order by TRADE_PERIOD desc, SHORT_PERIOD desc, LONG_PERIOD, XMT.BACKTEST_CONDITION_SEQ
;


delete
from XB_MABS_TRADE
where BACKTEST_CONDITION_SEQ in (32327637);

delete
from XA_MABS_CONDITION
where BACKTEST_CONDITION_SEQ in (32327637);

select *
from XA_MABS_CONDITION
where BACKTEST_CONDITION_SEQ not in (select XB_MABS_TRADE.BACKTEST_CONDITION_SEQ from XB_MABS_TRADE);

delete
from XA_MABS_CONDITION
where BACKTEST_CONDITION_SEQ not in (select XB_MABS_TRADE.BACKTEST_CONDITION_SEQ from XB_MABS_TRADE);

select BACKTEST_CONDITION_SEQ, count(*), min(TRADE_TIME_KST), max(TRADE_TIME_KST)
from XB_MABS_TRADE
group by BACKTEST_CONDITION_SEQ;

select MARKET, count(*), min(CANDLE_DATE_TIME_KST), max(CANDLE_DATE_TIME_KST)
from WA_CANDLE
group by MARKET;


select MARKET,
       FORMATDATETIME(CANDLE_DATE_TIME_KST, 'yyyyMM'),
       count(*),
       min(CANDLE_DATE_TIME_KST),
       max(CANDLE_DATE_TIME_KST)
from WA_CANDLE
where PERIOD_TYPE = 'PERIOD_1'
  and CANDLE_DATE_TIME_KST between '2022-01-01' and '2022-01-31'
  and MARKET in
      ('KRW-BTC', 'KRW-ETH', 'KRW-XRP', 'KRW-EOS', 'KRW-ETC', 'KRW-ADA', 'KRW-MANA', 'KRW-BAT', 'KRW-BCH', 'KRW-DOT')
group by MARKET, FORMATDATETIME(CANDLE_DATE_TIME_KST, 'yyyyMM')
order by 1, 2;



select MARKET, FORMATDATETIME(CANDLE_DATE_TIME_KST, 'yyyyMMdd HH') yyyyMMdd_HH, count(*)
from WA_CANDLE
where PERIOD_TYPE = 'PERIOD_1'
  and CANDLE_DATE_TIME_KST between '2022-01-01' and '2022-01-31'
  and MARKET = 'KRW-ELF'
group by MARKET, FORMATDATETIME(CANDLE_DATE_TIME_KST, 'yyyyMMdd HH')
order by 2;



select count(*)
from WA_CANDLE;


-- ========== 짬

select candleenti0_.candle_seq           as candle_s1_2_,
       candleenti0_.candle_date_time_kst as candle_d2_2_,
       candleenti0_.candle_date_time_utc as candle_d3_2_,
       candleenti0_.high_price           as high_pri4_2_,
       candleenti0_.low_price            as low_pric5_2_,
       candleenti0_.market               as market6_2_,
       candleenti0_.opening_price        as opening_7_2_,
       candleenti0_.period_type          as period_t8_2_,
       candleenti0_.trade_price          as trade_pr9_2_
from wa_candle candleenti0_
where (candleenti0_.candle_date_time_utc between '2021-12-31 17:00' and '2021-12-31 17:14:59')
  and candleenti0_.market = 'KRW-BTC'
  and candleenti0_.period_type = 'PERIOD_1'
--   and
order by candleenti0_.candle_date_time_utc


select candleenti0_.candle_seq           as candle_s1_2_,
       candleenti0_.candle_date_time_kst as candle_d2_2_,
       candleenti0_.candle_date_time_utc as candle_d3_2_,
       candleenti0_.high_price           as high_pri4_2_,
       candleenti0_.low_price            as low_pric5_2_,
       candleenti0_.market               as market6_2_,
       candleenti0_.opening_price        as opening_7_2_,
       candleenti0_.period_type          as period_t8_2_,
       candleenti0_.trade_price          as trade_pr9_2_
from wa_candle candleenti0_
where (candleenti0_.candle_date_time_utc between '2021-12-30 17:00' and '2022-12-31 17:14:59')
-- and PERIOD_TYPE <> 'PERIOD_1'
order by candleenti0_.candle_date_time_utc;


select FORMATDATETIME(candle_date_time_utc, 'yyyyMMdd-HH'),
       count(*)
from wa_candle
where candle_date_time_utc between '2021-12-29 17:00' and '2022-12-31 17:14:59'
  and MARKET = 'KRW-BTC'
  and PERIOD_TYPE = 'PERIOD_1'
group by FORMATDATETIME(candle_date_time_utc, 'yyyyMMdd-HH');


-- delete
-- from wa_candle
-- where (candle_date_time_utc between '2021-12-30 17:00' and '2022-12-31 17:14:59');

delete
from XB_MABS_TRADE
where BACKTEST_CONDITION_SEQ in (44343234, 36960734);

delete
from XA_MABS_CONDITION
where BACKTEST_CONDITION_SEQ in (44343234, 36960734);


explain
select candleenti0_.candle_seq           as candle_s1_2_,
       candleenti0_.candle_date_time_kst as candle_d2_2_,
       candleenti0_.candle_date_time_utc as candle_d3_2_,
       candleenti0_.high_price           as high_pri4_2_,
       candleenti0_.low_price            as low_pric5_2_,
       candleenti0_.market               as market6_2_,
       candleenti0_.opening_price        as opening_7_2_,
       candleenti0_.period_type          as period_t8_2_,
       candleenti0_.trade_price          as trade_pr9_2_
from wa_candle candleenti0_
where candleenti0_.market = 'KRW-BTC'
  and candleenti0_.period_type = 'PERIOD_1'
  and (candleenti0_.candle_date_time_utc between '2021-12-17T20:15:00' and '2021-12-17T20:15:00')
order by candleenti0_.candle_date_time_utc;


select candleenti0_.candle_seq           as candle_s1_2_,
       candleenti0_.candle_date_time_kst as candle_d2_2_,
       candleenti0_.candle_date_time_utc as candle_d3_2_,
       candleenti0_.high_price           as high_pri4_2_,
       candleenti0_.low_price            as low_pric5_2_,
       candleenti0_.market               as market6_2_,
       candleenti0_.opening_price        as opening_7_2_,
       candleenti0_.period_type          as period_t8_2_,
       candleenti0_.trade_price          as trade_pr9_2_
from wa_candle candleenti0_
where candleenti0_.market = 'KRW-BTC'
  and candleenti0_.period_type = 'PERIOD_60'
  and candleenti0_.candle_date_time_utc < '2021-12-26T16:00:00'
order by candleenti0_.candle_date_time_utc desc
    limit 64;


explain
select candleenti0_.candle_seq           as candle_s1_2_,
       candleenti0_.candle_date_time_kst as candle_d2_2_,
       candleenti0_.candle_date_time_utc as candle_d3_2_,
       candleenti0_.high_price           as high_pri4_2_,
       candleenti0_.low_price            as low_pric5_2_,
       candleenti0_.market               as market6_2_,
       candleenti0_.opening_price        as opening_7_2_,
       candleenti0_.period_type          as period_t8_2_,
       candleenti0_.trade_price          as trade_pr9_2_
from wa_candle candleenti0_
where candleenti0_.MARKET = 'KRW-BTC'
  and candleenti0_.candle_date_time_utc < '2021-12-28T17:00:00'
  and candleenti0_.period_type = 'PERIOD_60'
order by candleenti0_.candle_date_time_utc desc
    limit 64;

explain
select candleenti0_.candle_seq           as candle_s1_2_,
       candleenti0_.candle_date_time_kst as candle_d2_2_,
       candleenti0_.candle_date_time_utc as candle_d3_2_,
       candleenti0_.high_price           as high_pri4_2_,
       candleenti0_.low_price            as low_pric5_2_,
       candleenti0_.market               as market6_2_,
       candleenti0_.opening_price        as opening_7_2_,
       candleenti0_.period_type          as period_t8_2_,
       candleenti0_.trade_price          as trade_pr9_2_
from wa_candle candleenti0_
where candleenti0_.MARKET = 'KRW-BTC'
  and candleenti0_.candle_date_time_utc > '2020-01-28T17:00:00'
  and candleenti0_.period_type = 'PERIOD_1'
order by candleenti0_.candle_date_time_utc
    limit 1;

create index IDX_CANDLE_DATE_TIME_UTC_ASC
    on WA_CANDLE (CANDLE_DATE_TIME_UTC ASC);


create unique index IDX_CANDLE_ENTITY_MARKET
    on WA_CANDLE (MARKET, CANDLE_DATE_TIME_UTC desc, PERIOD_TYPE);

drop index IDX_CANDLE_DATE_TIME_UTC_DESC;