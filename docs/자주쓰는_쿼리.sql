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
