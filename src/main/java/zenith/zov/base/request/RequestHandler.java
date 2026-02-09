package zenith.zov.base.request;





import net.minecraft.client.MinecraftClient;
import zenith.zov.client.modules.api.Module;
import java.util.Comparator;
import java.util.concurrent.PriorityBlockingQueue;

public class RequestHandler<T> {

    private int currentTick = 0;

    private final PriorityBlockingQueue<Request<T>> activeRequests =
            new PriorityBlockingQueue<>(11, Comparator.comparingInt((Request<T> r) -> -r.priority));

    public void tick(int deltaTime) {
        currentTick += deltaTime;
    }

    public void tick() {
        tick(1);
    }

    public void request(Request<T> request) {

        activeRequests.removeIf(existing -> existing.provider == request.provider);

        request.expiresIn += currentTick;

        activeRequests.add(request);
    }

    public T getActiveRequestValue() {
        Request<T> top = activeRequests.peek();
        if (top == null) return null;

        if (MinecraftClient.getInstance().isOnThread()) {
            while (top != null && (top.expiresIn <= currentTick || !top.provider.isEnabled())) {
                activeRequests.poll();
                top = activeRequests.peek();
            }
        }

        return top != null ? top.value : null;
    }

    public static class Request<T> {
        public int expiresIn;
        public final int priority;
        public final Module provider;
        public final T value;

        public Request(int expiresIn, int priority, Module provider, T value) {
            this.expiresIn = expiresIn;
            this.priority = priority;
            this.provider = provider;
            this.value = value;
        }
    }
}

