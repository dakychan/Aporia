package zenith.zov.client.modules.impl.misc;

import zenith.zov.client.modules.api.Module;
import zenith.zov.client.modules.api.ModuleAnnotation;
import zenith.zov.client.modules.api.Category;

@ModuleAnnotation(name = "NoInteract", category = Category.MISC, description = "Не дает открыть контейнера")
public final class NoInteract extends Module {
    public static final NoInteract INSTANCE = new NoInteract();
    
    private NoInteract() {
    }
}
