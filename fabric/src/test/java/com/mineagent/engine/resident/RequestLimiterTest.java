package com.mineagent.engine.resident;
import org.junit.jupiter.api.Test;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
class RequestLimiterTest {
    @Test void requestsWaitForCapacityAndCancellationDoesNotLeakSlots() throws Exception {
        var limiter=new RequestLimiter(1,30);var first=limiter.acquire();var entered=new CountDownLatch(1);
        Thread waiting=new Thread(()->{try(var ignored=limiter.acquire()){entered.countDown();}catch(InterruptedException e){Thread.currentThread().interrupt();}});
        waiting.start();assertFalse(entered.await(80,TimeUnit.MILLISECONDS));waiting.interrupt();waiting.join(1000);
        assertEquals(0,limiter.availableSlots());first.close();first.close();assertEquals(1,limiter.availableSlots());
        try(var next=limiter.acquire()){assertEquals(2,limiter.admitted());}
    }
}
