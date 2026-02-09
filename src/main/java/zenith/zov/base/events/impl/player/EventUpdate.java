package zenith.zov.base.events.impl.player;


import com.darkmagician6.eventapi.events.Event;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class EventUpdate implements Event {

    //jeto
    //Данный ивент (событие) срабатывает каждый тик ИГРОКА когда mc.player !=null и mc.world !=null
    //в отличие от события под название EventTick которая срабатывает каждый реальный тик майнкрафта
}
