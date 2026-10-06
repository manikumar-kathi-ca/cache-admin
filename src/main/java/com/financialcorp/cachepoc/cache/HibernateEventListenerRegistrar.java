package com.financialcorp.cachepoc.cache;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.springframework.stereotype.Component;

@Component
public class HibernateEventListenerRegistrar {

    private final EntityManagerFactory entityManagerFactory;
    private final AccountCacheHibernateListener listener;

    public HibernateEventListenerRegistrar(
            EntityManagerFactory entityManagerFactory, AccountCacheHibernateListener listener) {
        this.entityManagerFactory = entityManagerFactory;
        this.listener = listener;
    }

    @PostConstruct
    public void register() {
        SessionFactoryImplementor sessionFactory = entityManagerFactory.unwrap(SessionFactoryImplementor.class);
        EventListenerRegistry registry = sessionFactory.getServiceRegistry().requireService(EventListenerRegistry.class);
        registry.getEventListenerGroup(EventType.POST_COMMIT_INSERT).appendListener(listener);
        registry.getEventListenerGroup(EventType.POST_COMMIT_UPDATE).appendListener(listener);
        registry.getEventListenerGroup(EventType.POST_COMMIT_DELETE).appendListener(listener);
    }
}
