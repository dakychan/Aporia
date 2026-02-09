package zenith.zov.base.font;

import com.google.gson.Gson;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import zenith.zov.Zenith;
import zenith.zov.utility.interfaces.IMinecraft;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

public final class ResourceProvider implements IMinecraft {

	private static final ResourceManager RESOURCE_MANAGER = mc.getResourceManager();
	private static final Gson GSON = new Gson();
	
	public static Identifier getShaderIdentifier(String name) {
		return Zenith.id("core/" + name);
	}

	public static <T> T fromJsonToInstance(Identifier identifier, Class<T> clazz) {
		return GSON.fromJson(toString(identifier), clazz);
	}

	public static String toString(Identifier identifier) {
		return toString(identifier, "\n");
	}
	
	public static String toString(Identifier identifier, String delimiter) {
		try (InputStream inputStream = RESOURCE_MANAGER.open(identifier);
			BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
			return reader.lines().collect(Collectors.joining(delimiter));
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

}