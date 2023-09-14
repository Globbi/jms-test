package de.globbi.codefactory.core;

import lombok.RequiredArgsConstructor;
import oracle.jdbc.pool.OracleConnectionCacheManager;
import oracle.ucp.jdbc.PoolDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static de.globbi.codefactory.config.PersistenceConfiguration.CONNECTION_CACHE_NAME;

@RequiredArgsConstructor
public class PoolMonitor {

    private final String monitorPrefix;

    private final CacheInfoSupplier freeConnSupplier;

    private final CacheInfoSupplier userConnSupplier;

    @Scheduled(fixedDelay = 1000, timeUnit = TimeUnit.MILLISECONDS)
    public void checkCache() throws SQLException {
        System.out.printf("(%s) Free: %d -- Used: %d%n",
                monitorPrefix,
                freeConnSupplier.get(),
                userConnSupplier.get());
    }

    public interface CacheInfoSupplier {
        int get() throws SQLException;
    }

}
