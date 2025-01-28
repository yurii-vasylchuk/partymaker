package org.yvasylchuk.partymaker.config;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component
public class AppContextProvider implements ApplicationContextAware {
    private static ApplicationContext ctx;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        AppContextProvider.ctx = applicationContext;
    }

    public static <T> T getBean(Class<T> clazz) {
        return ctx.getBean(clazz);
    }
}
