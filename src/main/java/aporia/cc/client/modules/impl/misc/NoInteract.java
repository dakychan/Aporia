package aporia.cc.client.modules.impl.misc;

import aporia.cc.client.modules.api.Module;
import aporia.cc.client.modules.api.ModuleAnnotation;
import aporia.cc.client.modules.api.Category;

@ModuleAnnotation(name = "NoInteract", category = Category.MISC, description = "Не дает открыть контейнера")
public final class NoInteract extends Module {
    public static final NoInteract INSTANCE = new NoInteract();
    
    private NoInteract() {
    }
}
