package org.jdbi.v3.core;

import java.time.Clock;
import org.jdbi.v3.core.config.JdbiConfig;

public class Time implements JdbiConfig<Time> {
    private Clock clock;

    public Time() {
        clock = Clock.systemUTC();
    }

    private Time(Time time) {
        clock = time.clock;
    }

    public Clock getClock() {
        return clock;
    }

    public void setClock(Clock clock) {
        this.clock = clock;
    }

    @Override
    public Time createCopy() {
        return new Time(this);
    }
}
