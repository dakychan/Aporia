package by.saskkeee.compiler;

import by.saskkeee.annotations.*;
import by.saskkeee.annotations.vmprotect.*;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.tools.Diagnostic;
import java.util.*;

@SupportedAnnotationTypes({
    "by.saskkeee.annotations.CompileToNative",
    "by.saskkeee.annotations.Entrypoint",
    "by.saskkeee.annotations.HttpStage",
    "by.saskkeee.annotations.vmprotect.VMProtect"
})
@SupportedSourceVersion(SourceVersion.RELEASE_21)
public class AnnotationProcessor extends AbstractProcessor {
    
    private final Map<String, Integer> protectionLevels = new HashMap<>();
    
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        
        for (Element element : roundEnv.getElementsAnnotatedWith(CompileToNative.class)) {
            processCompileToNative(element);
        }
        
        for (Element element : roundEnv.getElementsAnnotatedWith(Entrypoint.class)) {
            processEntrypoint(element);
        }
        
        for (Element element : roundEnv.getElementsAnnotatedWith(VMProtect.class)) {
            processVMProtect(element);
        }
        
        for (Element element : roundEnv.getElementsAnnotatedWith(HttpStage.class)) {
            processHttpStage(element);
        }
        
        return true;
    }
    
    private void processCompileToNative(Element element) {
        String name = element.getSimpleName().toString();
        protectionLevels.put(name, 2);
        
        processingEnv.getMessager().printMessage(
            Diagnostic.Kind.NOTE,
            "[CompileToNative] Marked for native compilation: " + name
        );
    }
    
    private void processEntrypoint(Element element) {
        String name = element.getSimpleName().toString();
        protectionLevels.put(name, 3);
        
        processingEnv.getMessager().printMessage(
            Diagnostic.Kind.NOTE,
            "[Entrypoint] Marked as entry point: " + name
        );
    }
    
    private void processVMProtect(Element element) {
        VMProtect annotation = element.getAnnotation(VMProtect.class);
        CompileType type = annotation.type();
        String name = element.getSimpleName().toString();
        
        int level = switch (type) {
            case VIRTUALIZATION -> 1;
            case MUTATION -> 2;
            case ULTRA -> 3;
        };
        
        protectionLevels.put(name, level);
        
        processingEnv.getMessager().printMessage(
            Diagnostic.Kind.NOTE,
            "[VMProtect] Protection level " + type + " for: " + name
        );
    }
    
    private void processHttpStage(Element element) {
        HttpStage annotation = element.getAnnotation(HttpStage.class);
        int stage = annotation.stage();
        String name = element.getSimpleName().toString();
        
        processingEnv.getMessager().printMessage(
            Diagnostic.Kind.NOTE,
            "[HttpStage] Stage " + stage + " for: " + name
        );
    }
}
