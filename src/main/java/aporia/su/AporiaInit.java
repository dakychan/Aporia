package aporia.su;

import aporia.su.utils.chat.CommandManager;
import aporia.su.utils.events.api.EventManager;
import aporia.su.utils.events.impl.TabCompleteEvent;

public class AporiaInit {
    public static void init() {
        System.out.println("[Aporia] Command system initialized with prefix: " + CommandManager.INSTANCE.getPrefix());

        EventManager.register(TabCompleteEvent.class, event -> {
            String prefix = event.getPrefix();
            if (prefix.startsWith(CommandManager.INSTANCE.getPrefix())) {
                String[] completions = CommandManager.INSTANCE.getCompletions(prefix);
                if (completions.length > 0) {
                    event.setCompletions(completions);
                }
            }
        });
    }
}
