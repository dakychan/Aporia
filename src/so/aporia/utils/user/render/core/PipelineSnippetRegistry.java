package so.aporia.utils.user.render.core;

import net.minecraft.resources.Identifier;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Registry for pipeline snippets — reusable shader/pipeline configurations.
 * Register snippets once, apply them anywhere.
 */
public class PipelineSnippetRegistry {

    public static final PipelineSnippetRegistry INSTANCE = new PipelineSnippetRegistry();

    private final Map<String, PipelineSnippet> snippets = new LinkedHashMap<>();

    public PipelineSnippet register(PipelineSnippet snippet) {
        snippets.put(snippet.getName(), snippet);
        return snippet;
    }

    public Optional<PipelineSnippet> get(String name) {
        return Optional.ofNullable(snippets.get(name));
    }

    public PipelineSnippet require(String name) {
        PipelineSnippet s = snippets.get(name);
        if (s == null) throw new IllegalArgumentException("Snippet not found: " + name);
        return s;
    }

    public boolean has(String name) {
        return snippets.containsKey(name);
    }

    public Map<String, PipelineSnippet> all() {
        return Map.copyOf(snippets);
    }

    public void clear() {
        snippets.clear();
    }
}
