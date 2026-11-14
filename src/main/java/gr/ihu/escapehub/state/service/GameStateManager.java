package gr.ihu.escapehub.state.service;

import org.springframework.stereotype.Component;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class GameStateManager {

    private final AtomicInteger remainingTime = new AtomicInteger(3600);

    public int getRemainingTime() {
        return remainingTime.get();
    }

    public void decrementTime(){
        if(remainingTime.get() > 0){
            remainingTime.decrementAndGet();
        }
    }
}
