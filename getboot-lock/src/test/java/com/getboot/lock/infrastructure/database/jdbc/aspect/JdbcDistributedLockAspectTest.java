/*
 * Copyright (c) 2026 qiheng. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.getboot.lock.infrastructure.database.jdbc.aspect;

import com.getboot.lock.api.annotation.DistributedLock;
import com.getboot.lock.api.constant.DistributedLockConstants;
import com.getboot.lock.api.exception.DistributedLockException;
import com.getboot.lock.api.properties.LockProperties;
import com.getboot.lock.infrastructure.database.jdbc.support.JdbcDistributedLockRepository;
import com.getboot.lock.spi.DistributedLockAcquireFailureHandler;
import com.getboot.lock.support.DefaultDistributedLockAcquireFailureHandler;
import com.getboot.lock.support.SpelDistributedLockKeyResolver;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdbcDistributedLockAspectTest {

    @Test
    void shouldRejectSecondConcurrentInvocationForSameKey() throws Exception {
        JdbcTemplate jdbcTemplate = createJdbcTemplate();
        JdbcDistributedLockRepository repository = new JdbcDistributedLockRepository(jdbcTemplate, "distributed_lock");

        LockProperties properties = createDatabaseLockProperties();
        JdbcDistributedLockAspect aspect = new JdbcDistributedLockAspect(
                repository,
                new SpelDistributedLockKeyResolver(),
                new DefaultDistributedLockAcquireFailureHandler(),
                properties
        );

        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        BlockingOrderService target = new BlockingOrderService(entered, release);

        AspectJProxyFactory proxyFactory = new AspectJProxyFactory(target);
        proxyFactory.addAspect(aspect);
        BlockingOrderService proxy = proxyFactory.getProxy();

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<String> first = executor.submit(() -> proxy.process("order-1"));
            assertTrue(entered.await(2, TimeUnit.SECONDS));

            Future<String> second = executor.submit(() -> proxy.process("order-1"));
            ExecutionException executionException = assertThrows(ExecutionException.class, second::get);
            assertTrue(executionException.getCause() instanceof DistributedLockException);

            release.countDown();
            assertEquals("order-1", first.get(2, TimeUnit.SECONDS));
            assertEquals(1, target.executions.get());
            assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM distributed_lock", Integer.class));
        } finally {
            release.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void shouldNotProceedWhenFailureHandlerReturnsNormally() {
        JdbcTemplate jdbcTemplate = createJdbcTemplate();
        JdbcDistributedLockRepository repository = new JdbcDistributedLockRepository(jdbcTemplate, "distributed_lock");
        String existingKey = "distributed_lock:order#order-2";
        jdbcTemplate.update(
                "INSERT INTO distributed_lock (lock_key, owner_id, lock_until, created_at, updated_at) VALUES (?, ?, ?, ?, ?)",
                existingKey,
                "holder",
                Timestamp.from(Instant.now().plusSeconds(30)),
                Timestamp.from(Instant.now()),
                Timestamp.from(Instant.now())
        );

        LockProperties properties = createDatabaseLockProperties();
        DistributedLockAcquireFailureHandler noOpHandler = (lockKey, distributedLock) -> {
        };
        JdbcDistributedLockAspect aspect = new JdbcDistributedLockAspect(
                repository,
                new SpelDistributedLockKeyResolver(),
                noOpHandler,
                properties
        );

        SimpleOrderService target = new SimpleOrderService();
        AspectJProxyFactory proxyFactory = new AspectJProxyFactory(target);
        proxyFactory.addAspect(aspect);
        SimpleOrderService proxy = proxyFactory.getProxy();

        assertThrows(DistributedLockException.class, () -> proxy.process("order-2"));
        assertEquals(0, target.executions.get());
    }

    private JdbcTemplate createJdbcTemplate() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        dataSource.setUsername("sa");
        dataSource.setPassword("");

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.execute(
                "CREATE TABLE distributed_lock ("
                        + "lock_key VARCHAR(255) PRIMARY KEY, "
                        + "owner_id VARCHAR(64) NOT NULL, "
                        + "lock_until TIMESTAMP(6) NOT NULL, "
                        + "created_at TIMESTAMP(6) NOT NULL, "
                        + "updated_at TIMESTAMP(6) NOT NULL"
                        + ")"
        );
        return jdbcTemplate;
    }

    private LockProperties createDatabaseLockProperties() {
        LockProperties properties = new LockProperties();
        properties.setType(DistributedLockConstants.LOCK_TYPE_DATABASE);
        properties.getDatabase().setEnabled(true);
        properties.getDatabase().setRetryIntervalMs(25);
        properties.getDatabase().setLeaseMs(500);
        return properties;
    }

    static class BlockingOrderService {

        private final CountDownLatch entered;
        private final CountDownLatch release;
        private final AtomicInteger executions = new AtomicInteger();

        BlockingOrderService(CountDownLatch entered, CountDownLatch release) {
            this.entered = entered;
            this.release = release;
        }

        @DistributedLock(scene = "order", keyExpression = "#orderNo", waitTime = 0, expireTime = 500)
        public String process(String orderNo) {
            executions.incrementAndGet();
            entered.countDown();
            try {
                release.await(2, TimeUnit.SECONDS);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(ex);
            }
            return orderNo;
        }
    }

    static class SimpleOrderService {

        private final AtomicInteger executions = new AtomicInteger();

        @DistributedLock(scene = "order", keyExpression = "#orderNo", waitTime = 0, expireTime = 500)
        public String process(String orderNo) {
            executions.incrementAndGet();
            return orderNo;
        }
    }
}
