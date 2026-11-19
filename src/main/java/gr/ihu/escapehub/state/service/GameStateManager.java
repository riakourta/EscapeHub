package gr.ihu.escapehub.state.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class GameStateManager {

    private final AtomicInteger remainingTime;

    public GameStateManager(@Value("${game.state.manager.remaining-time}") int duration) {
        this.remainingTime = new AtomicInteger(duration);
    }

    public int getRemainingTime() {
        return remainingTime.get();
    }

    public void decrementTime(){
        if(remainingTime.get() > 0){
            remainingTime.decrementAndGet();
        }
    }
}
