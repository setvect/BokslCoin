package com.setvect.bokslcoin.autotrading;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * Spring bean 객체를 가져옴
 */
@Component
public class ApplicationContextProvider implements ApplicationContextAware {

    /**
     * Spring application context
     */
    private static ApplicationContext ctx = null;

    /**
     * @return Spring application context
     */
    public static ApplicationContext getApplicationContext() {
        return ctx;
    }

    @Override
    public void setApplicationContext(@NotNull final ApplicationContext ctx) throws BeansException {
        ApplicationContextProvider.ctx = ctx;
    }
}
