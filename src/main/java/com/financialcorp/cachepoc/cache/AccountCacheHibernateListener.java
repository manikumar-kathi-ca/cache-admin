package com.financialcorp.cachepoc.cache;

import com.financialcorp.cachepoc.entity.Account;
import com.financialcorp.cachepoc.service.CacheSyncService;
import org.hibernate.event.spi.PostCommitDeleteEventListener;
import org.hibernate.event.spi.PostCommitInsertEventListener;
import org.hibernate.event.spi.PostCommitUpdateEventListener;
import org.hibernate.event.spi.PostDeleteEvent;
import org.hibernate.event.spi.PostInsertEvent;
import org.hibernate.event.spi.PostUpdateEvent;
import org.hibernate.persister.entity.EntityPersister;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AccountCacheHibernateListener
        implements PostCommitInsertEventListener, PostCommitUpdateEventListener, PostCommitDeleteEventListener {

    private static final Logger log = LoggerFactory.getLogger(AccountCacheHibernateListener.class);

    private final CacheSyncService cacheSyncService;

    public AccountCacheHibernateListener(CacheSyncService cacheSyncService) {
        this.cacheSyncService = cacheSyncService;
    }

    @Override
    public void onPostInsert(PostInsertEvent event) {
        if (event.getEntity() instanceof Account account) {
            cacheSyncService.putAccount(account);
        }
    }

    @Override
    public void onPostInsertCommitFailed(PostInsertEvent event) {
        log.debug("Insert commit failed; cache left unchanged");
    }

    @Override
    public void onPostUpdate(PostUpdateEvent event) {
        if (event.getEntity() instanceof Account account) {
            cacheSyncService.putAccount(account);
        }
    }

    @Override
    public void onPostUpdateCommitFailed(PostUpdateEvent event) {
        log.debug("Update commit failed; cache left unchanged");
    }

    @Override
    public void onPostDelete(PostDeleteEvent event) {
        if (event.getEntity() instanceof Account account) {
            cacheSyncService.evictAccount(account.getId());
            return;
        }
        if (event.getPersister().getMappedClass() == Account.class) {
            Object id = event.getId();
            if (id instanceof Number number) {
                cacheSyncService.evictAccount(number.longValue());
            }
        }
    }

    @Override
    public void onPostDeleteCommitFailed(PostDeleteEvent event) {
        log.debug("Delete commit failed; cache left unchanged");
    }

    @Override
    public boolean requiresPostCommitHandling(EntityPersister persister) {
        return persister.getMappedClass() == Account.class;
    }
}
