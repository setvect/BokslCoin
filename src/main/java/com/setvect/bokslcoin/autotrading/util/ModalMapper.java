package com.setvect.bokslcoin.autotrading.util;

import org.modelmapper.ModelMapper;
import org.modelmapper.config.Configuration;
import org.modelmapper.convention.MatchingStrategies;

public class ModalMapper {
    private static ModelMapper modelMapper = new ModelMapper();

    public static ModelMapper getMapper() {
        modelMapper.getConfiguration()
                .setFieldAccessLevel(Configuration.AccessLevel.PRIVATE)
                .setMatchingStrategy(MatchingStrategies.STRICT);

        return modelMapper;

    }
}
