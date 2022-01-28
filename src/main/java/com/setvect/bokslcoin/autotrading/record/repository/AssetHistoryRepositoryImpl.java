package com.setvect.bokslcoin.autotrading.record.repository;

import com.querydsl.core.QueryResults;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.setvect.bokslcoin.autotrading.record.model.AssetHistoryDto;
import com.setvect.bokslcoin.autotrading.record.model.AssetHistorySearchForm;
import com.setvect.bokslcoin.autotrading.record.model.AssetPeriodHistoryDto;
import com.setvect.bokslcoin.autotrading.record.model.AssetPeriodHistorySearchForm;
import com.setvect.bokslcoin.autotrading.util.MathUtil;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

import static com.setvect.bokslcoin.autotrading.record.entity.QAssetHistoryEntity.assetHistoryEntity;


@Repository
@RequiredArgsConstructor
public class AssetHistoryRepositoryImpl implements AssetHistoryRepositoryCustom {
    private final JPAQueryFactory queryFactory;

    @Override
    public Page<AssetHistoryDto> pageAssetHistory(AssetHistorySearchForm searchForm, Pageable pageable) {
        QueryResults<AssetHistoryDto> result = queryFactory
                .from(assetHistoryEntity)
                .select(Projections.fields(AssetHistoryDto.class,
                        assetHistoryEntity.assetHistorySeq,
                        assetHistoryEntity.currency,
                        assetHistoryEntity.price,
                        assetHistoryEntity.yield,
                        assetHistoryEntity.regDate
                ))
                .where(
                        eqCurrency(searchForm.getCurrency()),
                        range(searchForm.getFrom(), searchForm.getTo())
                )
                .orderBy(assetHistoryEntity.assetHistorySeq.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetchResults();

        return new PageImpl<>(result.getResults(), pageable, result.getTotal());
    }

    @Override
    public Page<AssetPeriodHistoryDto> pageAssetPeriodHistory(AssetPeriodHistorySearchForm searchForm, Pageable pageable) {
        QueryResults<AssetPeriodHistoryDto> result = queryFactory
                .from(assetHistoryEntity)
                .select(Projections.fields(AssetPeriodHistoryDto.class,
                        assetHistoryEntity.price.sum().as("price"),
                        ExpressionUtils.as(
                                assetHistoryEntity.price.multiply(
                                        Expressions.asNumber(1.0).add(assetHistoryEntity.yield.coalesce(0.0))
                                ).sum()
                                , "evlPrice"),
                        assetHistoryEntity.yield.count().as("coinCount"),
                        assetHistoryEntity.regDate
                ))
                .where(
                        range(searchForm.getFrom(), searchForm.getTo())
                )
                .groupBy(assetHistoryEntity.regDate)
                .orderBy(assetHistoryEntity.regDate.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetchResults();

        Page<AssetPeriodHistoryDto> pageResult = new PageImpl<>(result.getResults(), pageable, result.getTotal());
        pageResult.toList().forEach(p -> p.setYield(MathUtil.getYield(p.getEvlPrice(), p.getPrice())));
        return pageResult;
    }

    private BooleanExpression eqCurrency(String currency) {
        if (StringUtils.isEmpty(currency)) {
            return null;
        }
        return assetHistoryEntity.currency.eq(currency);
    }

    private BooleanExpression range(LocalDateTime from, LocalDateTime to) {
        if (from == null || to == null) {
            return null;
        }
        return assetHistoryEntity.regDate.between(from, to);
    }
}
