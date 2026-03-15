package aporia.su.util.user.render.web;

public record WebRendererConfig(String initialUrl, boolean transparent, boolean debugGui) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String initialUrl = "https://example.org/";
        private boolean transparent;
        private boolean debugGui;

        public Builder url(String url) {
            this.initialUrl = url;
            return this;
        }

        public Builder transparent(boolean value) {
            this.transparent = value;
            return this;
        }

        public Builder debugGui(boolean value) {
            this.debugGui = value;
            return this;
        }

        public WebRendererConfig build() {
            return new WebRendererConfig(initialUrl, transparent, debugGui);
        }
    }
}
