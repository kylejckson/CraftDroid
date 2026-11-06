package com.mineagent.engine.resident;

import java.util.ArrayDeque;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/** Global resident HTTP admission, including tool continuations/retries. Never used on the server thread. */
public final class RequestLimiter {
    private final Semaphore concurrent;
    private final int perMinute;
    private final ArrayDeque<Long> starts=new ArrayDeque<>();
    private final AtomicLong admitted=new AtomicLong();
    public RequestLimiter(int concurrency,int perMinute) {
        if(concurrency<1||concurrency>8||perMinute<1||perMinute>300)throw new IllegalArgumentException("Invalid request budget");
        concurrent=new Semaphore(concurrency,true);this.perMinute=perMinute;
    }
    public Lease acquire() throws InterruptedException {
        concurrent.acquire();boolean success=false;
        try {
            for(;;) {
                long wait;
                synchronized(starts) {
                    long now=System.nanoTime(),window=TimeUnit.MINUTES.toNanos(1);
                    while(!starts.isEmpty()&&now-starts.peekFirst()>=window)starts.removeFirst();
                    if(starts.size()<perMinute){starts.addLast(now);admitted.incrementAndGet();success=true;return new Lease(concurrent);}
                    wait=window-(now-starts.peekFirst());
                }
                TimeUnit.NANOSECONDS.sleep(Math.min(wait,TimeUnit.MILLISECONDS.toNanos(250)));
            }
        }finally{if(!success)concurrent.release();}
    }
    public long admitted(){return admitted.get();}
    public int availableSlots(){return concurrent.availablePermits();}
    public static Lease unrestricted(){return new Lease(null);}
    public static final class Lease implements AutoCloseable {
        private Semaphore semaphore;
        private Lease(Semaphore semaphore){this.semaphore=semaphore;}
        @Override public synchronized void close(){if(semaphore!=null){semaphore.release();semaphore=null;}}
    }
}
