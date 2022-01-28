package com.setvect.bokslcoin.autotrading.record.repository;

import com.querydsl.core.QueryResults;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.setvect.bokslcoin.autotrading.record.model.TradeDto;
import com.setvect.bokslcoin.autotrading.record.model.TradeSearchForm;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

import static com.setvect.bokslcoin.autotrading.record.entity.QTradeEntity.tradeEntity;


@Repository
@RequiredArgsConstructor
public class TradeRepositoryImpl implements TradeRepositoryCustom {
    private final JPAQueryFactory queryFactory;


    @Override
    public Page<TradeDto> pageTrade(TradeSearchForm searchForm, Pageable pageable) {
        QueryResults<TradeDto> result = queryFactory
                .from(tradeEntity)
                .select(Projections.fields(TradeDto.class,
                        tradeEntity.tradeSeq,
                        tradeEntity.market,
                        tradeEntity.tradeType,
                        tradeEntity.amount,
                        tradeEntity.unitPrice,
                        tradeEntity.yield,
                        tradeEntity.regDate
                ))
                .where(
                        eqMarket(searchForm.getMarket()),
                        range(searchForm.getFrom(), searchForm.getTo())
                )
                .orderBy(tradeEntity.tradeSeq.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetchResults();

        return new PageImpl<>(result.getResults(), pageable, result.getTotal());
    }

    private BooleanExpression eqMarket(String market) {
        if (StringUtils.isEmpty(market)) {
            return null;
        }
        return tradeEntity.market.eq(market);
    }

    private BooleanExpression range(LocalDateTime from, LocalDateTime to) {
        if (from == null || to == null) {
            return null;
        }
        return tradeEntity.regDate.between(from, to);
    }
}
