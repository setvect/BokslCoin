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
where XMT.BACKTEST_CONDITION_SEQ = 13974170
--   and TRADE_TYPE = 'SELL'
  and TRADE_TIME_KST between '2021-11-01T00:00:00' and '2021-12-31T23:59:59' --
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

explain  select XMT.BACKTEST_CONDITION_SEQ,
                XMC.MARKET,
                XMC.TRADE_PERIOD,
                XMC.SHORT_PERIOD,
                XMC.LONG_PERIOD,
                min(TRADE_TIME_KST),
                max(TRADE_TIME_KST),
                count(*)                                         as 매매회숫,
                round(exp(avg(ln(yield + 1))) - 1, 5)            as 기하평균,
                round(avg(yield), 5)                             as 산술평균,
                round(exp(sum(ln(yield + 1))) - 1, 5)            as 누적수익률,
--        round((exp(sum(ln(yield + 1 - 0.0010))) - 1), 5) as `누적수익률(매매비용 0.10%적용)`,
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
--   and TRADE_TIME_KST between '2017-10-01T00:00:00' and '2021-12-31T23:59:59' -- 전체기간(2)
--   and TRADE_TIME_KST between '2021-07-01T00:00:00' and '2021-12-31T23:59:59' --
--   and TRADE_TIME_KST between '2017-01-01T00:00:00' and '2017-12-31T23:59:59' --
--   and TRADE_TIME_KST between '2018-01-01T00:00:00' and '2018-12-31T23:59:59' --
--   and TRADE_TIME_KST between '2019-01-01T00:00:00' and '2019-12-31T23:59:59' --
--   and TRADE_TIME_KST between '2020-01-01T00:00:00' and '2020-12-31T23:59:59' --
           and TRADE_TIME_KST between '2021-01-01T00:00:00' and '2021-12-31T23:59:59' --
--   and MARKET = 'KRW-XRP'
         group by XMT.BACKTEST_CONDITION_SEQ
         order by MARKET, XMT.BACKTEST_CONDITION_SEQ
;
