package org.jdbi.v3.core.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.jdbi.v3.core.statement.ColonPrefixSqlParser;
import org.jdbi.v3.core.statement.ParsedSql;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.core.statement.StatementContextAccess;
import org.junit.Test;

public class TestJdbiCache {
    ConfigRegistry config = new ConfigRegistry();
    ColonPrefixSqlParser parser = new ColonPrefixSqlParser();
    int misses = 0;

    @Test
    public void shouldReturnSameInstance() {
        JdbiCache<String, String> cache = JdbiCaches.declare(this::reverse);

        String value = cache.get("foo", config);
        assertThat(value).isEqualTo("oof");
        assertThat(misses).isEqualTo(1);

        assertThat(cache.get("foo", config)).isSameAs(value);
        assertThat(misses).isEqualTo(1);
        System.out.println(cache.stats());
    }

    @Test
    public void shouldEnforceMaximumSize() {
        JdbiCache<String, String> cache = JdbiCaches.declare(this::reverse).maximumSize(3);

        cache.get("foo", config);
        cache.get("bar", config);
        cache.get("baz", config);
        assertThat(misses).isEqualTo(3);

        cache.get("foo", config);
        cache.get("bar", config);
        cache.get("baz", config);
        assertThat(misses).isEqualTo(3);

        assertThat(config.get(JdbiCaches.class).getMap(cache)).hasSize(3);

        cache.get("fizz", config);
        cache.get("buzz", config);

        assertThat(config.get(JdbiCaches.class).getMap(cache)).hasSize(3);

        System.out.println(cache.stats());
    }

    @Test
    public void concurrency() throws InterruptedException, ExecutionException {
        int maximumSize = 950;
        int uniqueKeys = 1000;

        StatementContext ctx = StatementContextAccess.createContext(config);
        JdbiCache<String, ParsedSql> cache = JdbiCaches
            .<String, ParsedSql>declare(template -> parser.parse(template, ctx))
            .maximumSize(maximumSize);

        ExecutorService executorService = Executors.newFixedThreadPool(50);

        Random random = new Random();
        List<Future> futures = new ArrayList<>();
        for (int i = 0; i < 1_000_000; i++) {
            int value = random.nextInt(uniqueKeys);
            futures.add(executorService.submit(() -> {
                String key = "select :foo" + value + "from bar";
                cache.get(key, config);

                assertThat(config.get(JdbiCaches.class).getMap(cache)).hasSizeLessThanOrEqualTo(maximumSize);
            }));
        }

        for (Future future : futures) {
            future.get();
        }
        executorService.shutdown();
        executorService.awaitTermination(5, TimeUnit.SECONDS);

        System.out.println(cache.stats());
    }

    String reverse(String input) {
        misses++; // safe for single-threaded test
        StringBuilder b = new StringBuilder(input.length());
        for (int i = input.length() - 1; i >= 0; i--) {
            b.append(input.charAt(i));
        }
        return b.toString();
    }
}
