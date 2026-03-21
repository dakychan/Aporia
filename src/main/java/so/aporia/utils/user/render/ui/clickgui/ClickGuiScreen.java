package so.aporia.utils.user.render.ui.clickgui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import so.aporia.module.Category;
import so.aporia.module.Module;
import so.aporia.module.ModuleManager;
import so.aporia.module.impl.misc.AutoConfig;
import so.aporia.module.settings.*;
import so.aporia.utils.files.FilesManager;
import so.aporia.utils.user.render.animation.Animator;
import so.aporia.utils.user.render.animation.Easing;
import so.aporia.utils.user.render.animation.TypeAnim;
import so.aporia.utils.user.render.color.ColorUtil;
import so.aporia.utils.user.render.core.AporiaRenderer;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * GUI Builder — single-file, two modes: LIVE and EDIT.
 * EDIT: drag/resize widgets, RMB opens property panel with sliders.
 * Supports: MODULE_LIST, SEARCH, ACCOUNT, CLIENT_NAME, RECT, CIRCLE, LINE, TRIANGLE, TEXT, IMAGE.
 */
@OnlyIn(Dist.CLIENT)
public final class ClickGuiScreen extends Screen {

    // ── Widget types ──────────────────────────────────────────────────────────
    public enum WidgetType { MODULE_LIST, SEARCH, ACCOUNT, CLIENT_NAME, RECT, CIRCLE, LINE, TRIANGLE, TEXT, IMAGE, CATEGORY_LIST, BUTTON }
    public enum SettingsMode { INLINE, POPUP }
    public enum GradientDir { NONE, HORIZONTAL, VERTICAL, RADIAL }

    // ── Widget ────────────────────────────────────────────────────────────────
    public static final class Widget {
        public WidgetType   type;
        public float        x, y, w, h;
        // background
        public boolean      blur         = true;
        public float        blurStrength = 14f;
        public boolean      border       = true;
        public int          bgR=255, bgG=255, bgB=255, bgAlpha=18;
        public int          borderR=255, borderG=255, borderB=255, borderAlpha=30;
        public int          borderRadius = 12;
        // title
        public boolean      showTitle    = false;
        public String       titleText    = "";
        // module list
        public SettingsMode settingsMode = SettingsMode.INLINE;
        public String       category     = "";
        public boolean      collapsed    = false;
        public int          cardRadius   = 6;
        public int          cardGap      = 3;
        public int          cardH        = 26;
        public int          cols         = 0;
        public float        fontSize     = 9f;
        public int          cardR=255, cardG=255, cardB=255, cardAlpha=12;
        public int          cardOnR=255, cardOnG=255, cardOnB=255, cardOnAlpha=40;
        // shape (circle/line/triangle)
        public int          shapeR=255, shapeG=255, shapeB=255, shapeAlpha=180;
        public float        lineThickness = 2f;
        // text widget
        public String       customText   = "Текст";
        public String       fontName     = "regular";
        // image
        public String       imagePath    = "";
        public String       imageFit     = "fill";
        // transform
        public float        rotation     = 0f;
        public boolean      locked       = false;
        // gradient
        public GradientDir  gradientDir  = GradientDir.NONE;
        public int          grad2R=255, grad2G=255, grad2B=255, grad2Alpha=0;
        // individual corner radius
        public int          radTL=12, radTR=12, radBL=12, radBR=12;
        // button widget
        public String       buttonAction = "";
        // text variables support (uses customText with {player} etc.)
        public boolean      textVars     = false;

        public Widget(WidgetType t, float x, float y, float w, float h) {
            this.type=t; this.x=x; this.y=y; this.w=w; this.h=h;
        }
        /** Deep copy for undo/duplicate. */
        public Widget copy() {
            Widget c=new Widget(type,x,y,w,h);
            c.blur=blur; c.blurStrength=blurStrength; c.border=border;
            c.bgR=bgR; c.bgG=bgG; c.bgB=bgB; c.bgAlpha=bgAlpha;
            c.borderR=borderR; c.borderG=borderG; c.borderB=borderB; c.borderAlpha=borderAlpha;
            c.borderRadius=borderRadius; c.showTitle=showTitle; c.titleText=titleText;
            c.settingsMode=settingsMode; c.category=category; c.collapsed=collapsed;
            c.cardRadius=cardRadius; c.cardGap=cardGap; c.cardH=cardH; c.cols=cols; c.fontSize=fontSize;
            c.cardR=cardR; c.cardG=cardG; c.cardB=cardB; c.cardAlpha=cardAlpha;
            c.cardOnR=cardOnR; c.cardOnG=cardOnG; c.cardOnB=cardOnB; c.cardOnAlpha=cardOnAlpha;
            c.shapeR=shapeR; c.shapeG=shapeG; c.shapeB=shapeB; c.shapeAlpha=shapeAlpha;
            c.lineThickness=lineThickness; c.customText=customText; c.fontName=fontName;
            c.imagePath=imagePath; c.imageFit=imageFit; c.rotation=rotation; c.locked=locked;
            c.gradientDir=gradientDir; c.grad2R=grad2R; c.grad2G=grad2G; c.grad2B=grad2B; c.grad2Alpha=grad2Alpha;
            c.radTL=radTL; c.radTR=radTR; c.radBL=radBL; c.radBR=radBR;
            c.buttonAction=buttonAction; c.textVars=textVars;
            return c;
        }
    }

    // ── Constants ─────────────────────────────────────────────────────────────
    private static final int   PAD        = 8;
    private static final int   R          = 12;
    private static final int   LINE_H     = 26;
    private static final int   HIT        = 6;
    private static final int   EDIT_BTN_S = 22;
    private static final int   TOPBAR_H   = 28;
    private static final int   COL_MIN_W  = 90;
    private static final long  BOOL_FLASH = 1200;
    private static final long  KEY_FLASH  = 1400;
    private static final Path  CFG_PATH   = FilesManager.ROOT.resolve("layout.apr");

    private static final int C_BG      = ColorUtil.rgba(255,255,255, 18);
    private static final int C_BORDER  = ColorUtil.rgba(255,255,255, 30);
    private static final int C_DIV     = ColorUtil.rgba(255,255,255, 16);
    private static final int C_TXT     = ColorUtil.rgba(255,255,255,180);
    private static final int C_TXT_ON  = ColorUtil.rgba(255,255,255,255);
    private static final int C_TXT_DIM = ColorUtil.rgba(255,255,255, 80);
    private static final int C_CARD    = ColorUtil.rgba(255,255,255, 12);
    private static final int C_CARD_H  = ColorUtil.rgba(255,255,255, 28);
    private static final int C_CARD_ON = ColorUtil.rgba(255,255,255, 40);
    private static final int C_BIND    = ColorUtil.rgba(255,200, 80,255);
    private static final int C_BLUE    = ColorUtil.rgba(100,180,255,200);
    private static final int C_RED     = ColorUtil.rgba(255,100,100,220);
    private static final int C_SEL_BG  = ColorUtil.rgba(255,255,255, 16);
    private static final int C_SEL_H   = ColorUtil.rgba(255,255,255, 35);
    private static final int C_SET_BG  = ColorUtil.rgba(255,255,255, 10);
    private static final int C_SET_H   = ColorUtil.rgba(255,255,255, 22);
    private static final int C_MUL_ON  = ColorUtil.rgba(100,180,255,180);
    private static final int C_MUL_OF  = ColorUtil.rgba(255,255,255, 16);
    private static final int C_TXT_BG  = ColorUtil.rgba(255,255,255, 12);
    private static final int C_TXT_BD  = ColorUtil.rgba(255,255,255, 30);
    private static final int C_TXT_BDF = ColorUtil.rgba(100,180,255,150);
    private static final int C_BTN_BG  = ColorUtil.rgba(100,180,255, 30);
    private static final int C_BTN_H   = ColorUtil.rgba(100,180,255, 60);
    private static final int C_EDIT_S  = ColorUtil.rgba(100,180,255, 60);
    private static final int C_OPT_BG  = ColorUtil.rgba( 18, 20, 32,245);
    private static final int C_OPT_H   = ColorUtil.rgba(255,255,255, 18);
    private static final int C_PROP_BG = ColorUtil.rgba( 14, 16, 28,250);
    private static final int C_PROP_BD = ColorUtil.rgba(100,180,255, 60);
    private static final int C_SLD_BG  = ColorUtil.rgba(255,255,255, 14);
    private static final int C_SLD_FG  = ColorUtil.rgba(100,180,255,200);
    private static final int C_SLD_H   = ColorUtil.rgba(100,180,255,255);

    // ── State ─────────────────────────────────────────────────────────────────
    private final List<Widget> widgets = new ArrayList<>();
    private boolean editMode = false;

    private Widget dragWidget  = null;
    private float  dragOx, dragOy;
    private Widget resizeWidget = null;
    private boolean resL, resR, resT, resB;
    private float   resStartMx, resStartMy, resStartX, resStartY, resStartW, resStartH;

    // property panel
    private Widget  propWidget   = null;
    private float   propScroll   = 0, propScrollT = 0;
    // active slider drag: field name + widget
    private String  sliderField  = null;
    private Widget  sliderWidget = null;
    private float   sliderMinV, sliderMaxV;
    private int     sliderX, sliderW;

    private Widget  addMenuWidget = null;
    private int     addMenuX, addMenuY;

    private Widget  popupWidget  = null;
    private float   popupScroll  = 0, popupScrollT = 0;

    private final WeakHashMap<Module, Animator>      modHover    = new WeakHashMap<>();
    private final WeakHashMap<Module, TypeAnim>      modNameAnim = new WeakHashMap<>();
    private final WeakHashMap<Module, Long>          modKeyFlash = new WeakHashMap<>();
    private final WeakHashMap<Widget,  Float>        widgetScroll  = new WeakHashMap<>();
    private final WeakHashMap<Widget,  Float>        widgetScrollT = new WeakHashMap<>();
    private final WeakHashMap<Widget,  Module>       widgetSelected = new WeakHashMap<>();
    private final WeakHashMap<Setting<?>, TypeAnim>  settingAnim  = new WeakHashMap<>();
    private final WeakHashMap<Setting<?>, Long>      boolFlash    = new WeakHashMap<>();

    private Module  moduleBinding  = null;
    private Module  settingBindMod = null;
    private Field   settingBindFld = null;
    private TextSetting focusedText = null;
    private String  search = "";
    private boolean searchFocused = false;
    private Widget  searchWidget  = null;

    private List<Path> imageFiles    = new ArrayList<>();
    private long       imageScanTime = 0;
    private Widget     imagePickWidget = null;
    private int        imagePickX, imagePickY;

    // inline text editing for TEXT widget / title
    private Widget  editTextWidget = null;
    private boolean editTextIsTitle = false;

    // ── Multi-select ──────────────────────────────────────────────────────────
    private final Set<Widget> selection = new LinkedHashSet<>();
    private boolean boxSelecting = false;
    private float   boxX0, boxY0, boxX1, boxY1;
    private final Map<Widget, float[]> multiDragOffsets = new HashMap<>();

    // ── Undo/Redo ─────────────────────────────────────────────────────────────
    private final Deque<List<Widget>> undoStack = new ArrayDeque<>();
    private final Deque<List<Widget>> redoStack = new ArrayDeque<>();
    private static final int MAX_UNDO = 30;

    // ── Grid snap ─────────────────────────────────────────────────────────────
    private boolean gridSnap    = false;
    private int     gridStep    = 10;
    private boolean showGrid    = false;

    // ── Clipboard ─────────────────────────────────────────────────────────────
    private final List<Widget> clipboard = new ArrayList<>();

    // ── Themes ────────────────────────────────────────────────────────────────
    private static final String[] THEME_NAMES = {"Default","Dark Purple","Carbon","Neon","Minimal"};
    private static final int[][] THEME_BG    = {{255,255,255,18},{120,60,200,25},{30,30,30,40},{0,255,180,20},{255,255,255,8}};
    private static final int[][] THEME_BD    = {{255,255,255,30},{180,100,255,60},{200,200,200,30},{0,255,180,80},{255,255,255,15}};
    private static final int[][] THEME_CARD  = {{255,255,255,12},{120,60,200,18},{50,50,50,30},{0,255,180,12},{255,255,255,6}};

    // ── Layouts ───────────────────────────────────────────────────────────────
    private String currentLayout = "default";
    private boolean layoutMenuOpen = false;
    private List<String> savedLayouts = new ArrayList<>();

    // ── Zoom ──────────────────────────────────────────────────────────────────
    private float zoom = 1f;
    private float panX = 0f, panY = 0f;

    // ── Align / Theme cycle state ─────────────────────────────────────────────
    private int alignIdx  = 0;
    private int themeIdx  = 0;
    private static final String[] ALIGN_DIRS = {"left","right","top","bottom","centerH","centerV"};

    public ClickGuiScreen() { super(Component.literal("ClickGui")); }

    // ── init / close ──────────────────────────────────────────────────────────
    @Override protected void init() {
        if (widgets.isEmpty()) loadLayout();
        AporiaRenderer.INSTANCE.resetDebugFlags();
    }

    @Override public void onClose() {
        saveLayout();
        Module ac = ModuleManager.INSTANCE.get("AutoConfig");
        if (ac instanceof AutoConfig a && a.getAutoSave().isEnabled()) a.save();
        super.onClose();
    }

    // ── render ────────────────────────────────────────────────────────────────
    @Override
    public void render(GuiGraphics gfx, int mx, int my, float delta) {
        AporiaRenderer r = AporiaRenderer.INSTANCE;
        float maxBlur = 4f;
        for (Widget w : widgets) if (w.blur) maxBlur = Math.max(maxBlur, w.blurStrength);
        r.prepareBlur(Minecraft.getInstance(), maxBlur, 0.75f);
        for (Widget w : widgets) renderWidget(r, gfx, w, mx, my);
        if (popupWidget != null) renderPopup(r, gfx, popupWidget, mx, my);
        renderEditButton(r, mx, my);
        if (editMode) {
            renderEditOverlay(r, gfx, mx, my);
            if (propWidget != null) renderPropPanel(r, gfx, mx, my);
            if (addMenuWidget != null) renderAddMenu(r, gfx, mx, my);
            if (imagePickWidget != null) renderImagePicker(r, mx, my);
        }
    }

    /** Dispatches rendering for a single widget by type. */
    private void renderWidget(AporiaRenderer r, GuiGraphics gfx, Widget w, int mx, int my) {
        if (w.collapsed) { renderCollapsed(r, w); return; }
        if (w.rotation != 0f) {
            float cx = w.x + w.w/2f, cy = w.y + w.h/2f;
            gfx.pose().pushMatrix();
            gfx.pose().translate(cx, cy);
            gfx.pose().rotate((float)Math.toRadians(w.rotation));
            gfx.pose().translate(-cx, -cy);
        }
        switch (w.type) {
            case MODULE_LIST  -> renderModuleList(r, gfx, w, mx, my);
            case SEARCH       -> renderSearch(r, gfx, w, mx, my);
            case ACCOUNT      -> renderAccount(r, w);
            case CLIENT_NAME  -> renderClientName(r, w);
            case RECT         -> renderRect(r, w);
            case CIRCLE       -> renderCircle(r, w);
            case LINE         -> renderLine(r, w);
            case TRIANGLE     -> renderTriangle(r, w);
            case TEXT         -> renderText(r, w);
            case IMAGE        -> renderImage(r, w);
            case CATEGORY_LIST-> renderCategoryList(r, gfx, w, mx, my);
            case BUTTON       -> renderButton(r, w, mx, my);
        }
        if (w.rotation != 0f) gfx.pose().popMatrix();
    }

    // ── widget: collapsed ─────────────────────────────────────────────────────
    private void renderCollapsed(AporiaRenderer r, Widget w) {
        int ix=(int)w.x, iy=(int)w.y, iw=(int)w.w;
        drawWidgetBg(r, w, ix, iy, iw, TOPBAR_H);
        r.drawText("bold", widgetTitle(w), ix+PAD, iy+(TOPBAR_H-9)/2f, 9f, C_TXT_ON);
    }

    // ── widget: rect ──────────────────────────────────────────────────────────
    private void renderRect(AporiaRenderer r, Widget w) {
        int ix=(int)w.x, iy=(int)w.y, iw=(int)w.w, ih=(int)w.h;
        drawWidgetBg(r, w, ix, iy, iw, ih);
        if (w.showTitle && !w.titleText.isEmpty()) {
            String title = editTextWidget==w&&editTextIsTitle
                ? w.titleText+((System.currentTimeMillis()/500)%2==0?"|":"") : w.titleText;
            float fs = w.fontSize>0 ? w.fontSize : 9f;
            r.drawText("bold", title, ix+PAD, iy+(TOPBAR_H-fs)/2f, fs, C_TXT_ON);
        }
    }

    // ── widget: circle ────────────────────────────────────────────────────────
    private void renderCircle(AporiaRenderer r, Widget w) {
        float cx = w.x + w.w/2f, cy = w.y + w.h/2f;
        float rad = Math.min(w.w, w.h)/2f - 1;
        r.drawCircle(cx, cy, rad, ColorUtil.rgba(w.shapeR, w.shapeG, w.shapeB, w.shapeAlpha));
    }

    // ── widget: line ──────────────────────────────────────────────────────────
    private void renderLine(AporiaRenderer r, Widget w) {
        r.drawLine(w.x, w.y+w.h/2f, w.x+w.w, w.y+w.h/2f,
            w.lineThickness, ColorUtil.rgba(w.shapeR, w.shapeG, w.shapeB, w.shapeAlpha));
    }

    // ── widget: triangle ──────────────────────────────────────────────────────
    private void renderTriangle(AporiaRenderer r, Widget w) {
        r.drawTriangle(w.x+w.w/2f, w.y+2, w.x+w.w-2, w.y+w.h-2, w.x+2, w.y+w.h-2,
            ColorUtil.rgba(w.shapeR, w.shapeG, w.shapeB, w.shapeAlpha));
    }

    // ── widget: text ──────────────────────────────────────────────────────────
    private void renderText(AporiaRenderer r, Widget w) {
        drawWidgetBg(r, w, (int)w.x, (int)w.y, (int)w.w, (int)w.h);
        String raw = (editTextWidget==w&&!editTextIsTitle)
            ? w.customText+((System.currentTimeMillis()/500)%2==0?"|":"")
            : (w.textVars ? resolveVars(w.customText) : w.customText);
        float fs=w.fontSize>0?w.fontSize:9f;
        r.drawText(w.fontName.isEmpty()?"regular":w.fontName, raw,
            w.x+PAD, w.y+(w.h-fs)/2f, fs, ColorUtil.rgba(w.shapeR,w.shapeG,w.shapeB,w.shapeAlpha));
    }

    // ── widget: image ─────────────────────────────────────────────────────────
    private void renderImage(AporiaRenderer r, Widget w) {
        int ix=(int)w.x, iy=(int)w.y, iw=(int)w.w, ih=(int)w.h;
        drawWidgetBg(r, w, ix, iy, iw, ih);
        if (w.imagePath.isEmpty()) {
            r.drawText("regular", "Нет изображения", ix+PAD, iy+(ih-9)/2f, 9f, C_TXT_DIM); return;
        }
        net.minecraft.resources.Identifier id = r.loadImage(java.nio.file.Paths.get(w.imagePath));
        if (id == null) { r.drawText("regular", "Ошибка", ix+PAD, iy+(ih-9)/2f, 9f, C_RED); return; }
        float dx=ix, dy=iy, dw=iw, dh=ih;
        if (w.imageFit.equals("fit")) {
            float aspect=1f;
            try {
                var tex = Minecraft.getInstance().getTextureManager().getTexture(id);
                if (tex instanceof net.minecraft.client.renderer.texture.DynamicTexture dt && dt.getPixels()!=null) {
                    float tw=dt.getPixels().getWidth(), th=dt.getPixels().getHeight(); aspect=tw/th;
                }
            } catch (Exception ignored) {}
            float ba=(float)iw/ih;
            if (aspect>ba) { dh=iw/aspect; dy=iy+(ih-dh)/2f; }
            else { dw=ih*aspect; dx=ix+(iw-dw)/2f; }
        }
        r.drawImage(dx, dy, dw, dh, id);
    }

    // ── widget: category list ─────────────────────────────────────────────────
    private void renderCategoryList(AporiaRenderer r, GuiGraphics gfx, Widget w, int mx, int my) {
        int ix=(int)w.x, iy=(int)w.y, iw=(int)w.w, ih=(int)w.h;
        drawWidgetBg(r, w, ix, iy, iw, ih);
        int cGap=w.cardGap, cH=w.cardH>0?w.cardH:26;
        float fs=w.fontSize>0?w.fontSize:9f;
        float scroll=widgetScroll.getOrDefault(w,0f);
        float scrollT=widgetScrollT.getOrDefault(w,0f);
        scroll+=(scrollT-scroll)*0.15f; widgetScroll.put(w,scroll);
        gfx.enableScissor(ix+1,iy+1,ix+iw-1,iy+ih);
        int y=iy+PAD-(int)scroll;
        for (Category cat : Category.values()) {
            List<Module> mods = ModuleManager.INSTANCE.getByCategory(cat);
            if (mods.isEmpty()) continue;
            boolean hov=mx>=ix+PAD&&mx<ix+iw-PAD&&my>=y&&my<y+cH;
            r.drawRectBlurred(ix+PAD,y,iw-PAD*2,cH,w.cardRadius,
                hov?ColorUtil.rgba(w.cardR,w.cardG,w.cardB,w.cardAlpha+20):ColorUtil.rgba(w.cardR,w.cardG,w.cardB,w.cardAlpha),8f);
            r.drawText("bold",cat.name(),ix+PAD*2,y+(cH-fs)/2f,fs,C_TXT_ON);
            r.drawText("regular",""+mods.size(),ix+iw-PAD*3,y+(cH-8)/2f,8f,C_TXT_DIM);
            y+=cH+cGap;
        }
        gfx.disableScissor();
    }

    // ── widget: button ────────────────────────────────────────────────────────
    private void renderButton(AporiaRenderer r, Widget w, int mx, int my) {
        int ix=(int)w.x, iy=(int)w.y, iw=(int)w.w, ih=(int)w.h;
        boolean hov=mx>=ix&&mx<ix+iw&&my>=iy&&my<iy+ih;
        int bg=hov?ColorUtil.rgba(w.bgR,w.bgG,w.bgB,Math.min(255,w.bgAlpha+30)):ColorUtil.rgba(w.bgR,w.bgG,w.bgB,w.bgAlpha);
        if (w.blur) r.drawRectBlurred(ix,iy,iw,ih,w.borderRadius,bg,w.blurStrength);
        else r.drawRect(ix,iy,iw,ih,w.borderRadius,bg);
        if (w.border) r.drawStroke(ix,iy,iw,ih,w.borderRadius,1f,1,0f,
            hov?ColorUtil.rgba(w.borderR,w.borderG,w.borderB,Math.min(255,w.borderAlpha+60)):ColorUtil.rgba(w.borderR,w.borderG,w.borderB,w.borderAlpha));
        String label=w.customText.isEmpty()?"Кнопка":w.customText;
        float fs=w.fontSize>0?w.fontSize:9f;
        float tw=r.getTextWidth(w.fontName.isEmpty()?"regular":w.fontName,label,fs);
        r.drawText(w.fontName.isEmpty()?"regular":w.fontName,label,ix+(iw-tw)/2f,iy+(ih-fs)/2f,fs,
            ColorUtil.rgba(w.shapeR,w.shapeG,w.shapeB,w.shapeAlpha));
    }

    /** Resolves text variables like {player}, {fps}, {x}, {y}, {z}, {health}, {time}. */
    private String resolveVars(String text) {
        if (!text.contains("{")) return text;
        Minecraft mc=Minecraft.getInstance();
        text=text.replace("{player}", mc.getUser().getName());
        text=text.replace("{fps}", String.valueOf(mc.getFps()));
        if (mc.player!=null) {
            text=text.replace("{x}", String.valueOf((int)mc.player.getX()));
            text=text.replace("{y}", String.valueOf((int)mc.player.getY()));
            text=text.replace("{z}", String.valueOf((int)mc.player.getZ()));
            text=text.replace("{health}", String.valueOf((int)mc.player.getHealth()));
        }
        text=text.replace("{time}", new java.text.SimpleDateFormat("HH:mm").format(new java.util.Date()));
        return text;
    }

    private void renderClientName(AporiaRenderer r, Widget w) {
        int ix=(int)w.x, iy=(int)w.y, iw=(int)w.w, ih=(int)w.h;
        drawWidgetBg(r, w, ix, iy, iw, ih);
        String label = w.showTitle && !w.titleText.isEmpty() ? w.titleText : "Aporia";
        float fs = w.fontSize>0 ? w.fontSize : 13f;
        r.drawText("bold", label, ix+PAD, iy+(ih-fs)/2f, fs, C_TXT_ON);
    }

    // ── widget: account ───────────────────────────────────────────────────────
    private void renderAccount(AporiaRenderer r, Widget w) {
        int ix=(int)w.x, iy=(int)w.y, iw=(int)w.w, ih=(int)w.h;
        drawWidgetBg(r, w, ix, iy, iw, ih);
        String name = Minecraft.getInstance().getUser().getName();
        float fs = w.fontSize>0 ? w.fontSize : 9f;
        r.drawText("regular", name, ix+PAD, iy+(ih-fs)/2f, fs, C_TXT_ON);
    }

    // ── widget: search ────────────────────────────────────────────────────────
    private void renderSearch(AporiaRenderer r, GuiGraphics gfx, Widget w, int mx, int my) {
        int ix=(int)w.x, iy=(int)w.y, iw=(int)w.w, ih=(int)w.h;
        searchWidget = w;
        drawWidgetBg(r, w, ix, iy, iw, ih);
        int bdr = searchFocused ? C_TXT_BDF : C_TXT_BD;
        r.drawStroke(ix, iy, iw, ih, R, 1f, 1, 0f, bdr);
        gfx.enableScissor(ix+6, iy, ix+iw-6, iy+ih);
        if (search.isEmpty() && !searchFocused)
            r.drawText("regular", "Поиск...", ix+PAD, iy+(ih-9)/2f, 9f, C_TXT_DIM);
        else {
            String cur = search + (searchFocused && (System.currentTimeMillis()/500)%2==0 ? "|" : "");
            r.drawText("regular", cur, ix+PAD, iy+(ih-9)/2f, 9f, C_TXT_ON);
        }
        gfx.disableScissor();
    }

    // ── widget: module list ───────────────────────────────────────────────────
    private void renderModuleList(AporiaRenderer r, GuiGraphics gfx, Widget w, int mx, int my) {
        int ix=(int)w.x, iy=(int)w.y, iw=(int)w.w, ih=(int)w.h;
        drawWidgetBg(r, w, ix, iy, iw, ih);
        int topOff = 0;
        if (w.showTitle && !w.titleText.isEmpty()) {
            String title = editTextWidget==w&&editTextIsTitle
                ? w.titleText+((System.currentTimeMillis()/500)%2==0?"|":"") : w.titleText;
            r.drawRect(ix, iy, iw, TOPBAR_H, w.borderRadius, ColorUtil.rgba(w.bgR,w.bgG,w.bgB,Math.min(255,w.bgAlpha+20)));
            r.drawStroke(ix, iy, iw, TOPBAR_H, w.borderRadius, 1f, 1, 0f, ColorUtil.rgba(w.borderR,w.borderG,w.borderB,w.borderAlpha));
            float fs2 = w.fontSize>0?w.fontSize:9f;
            r.drawText("bold", title, ix+PAD, iy+(TOPBAR_H-fs2)/2f, fs2, C_TXT_ON);
            topOff = TOPBAR_H;
        }
        int cR   = w.cardRadius;
        int cGap = w.cardGap;
        int cH   = w.cardH > 0 ? w.cardH : 26;
        float fs = w.fontSize>0 ? w.fontSize : 9f;
        List<Module> mods = filteredMods(w);
        Module sel = widgetSelected.get(w);
        boolean hasSettings = sel!=null && w.settingsMode==SettingsMode.INLINE;
        int modW = hasSettings ? Math.min(200,(int)(iw*0.45f)) : iw-2;
        int autoCols = Math.max(1, modW/COL_MIN_W);
        int cols = w.cols>0 ? w.cols : autoCols;
        int colW = (modW - PAD*(cols+1)) / cols;
        float scroll  = widgetScroll.getOrDefault(w, 0f);
        float scrollT = widgetScrollT.getOrDefault(w, 0f);
        scroll += (scrollT-scroll)*0.15f;
        widgetScroll.put(w, scroll);
        gfx.enableScissor(ix+1, iy+topOff+1, ix+modW, iy+ih);
        int col=0, rowY=iy+topOff+PAD-(int)scroll;
        int cardBg   = ColorUtil.rgba(w.cardR,   w.cardG,   w.cardB,   w.cardAlpha);
        int cardOn   = ColorUtil.rgba(w.cardOnR, w.cardOnG, w.cardOnB, w.cardOnAlpha);
        int cardHov  = ColorUtil.rgba(w.cardR,   w.cardG,   w.cardB,   Math.min(255, w.cardAlpha+20));
        for (Module m : mods) {
            int cardX = ix+PAD+col*(colW+PAD);
            boolean on=m.isEnabled(), isSel=m==sel;
            boolean hov=mx>=cardX&&mx<cardX+colW&&my>=rowY&&my<rowY+cH;
            boolean binding=m==moduleBinding;
            Animator ha = modHover.computeIfAbsent(m, k->new Animator(180, Easing::cubicOut));
            if (hov  && !ha.isPlaying() && ha.value()<0.99f) ha.play();
            if (!hov && !ha.isPlaying() && ha.value()>0.01f) ha.reverse();
            ha.update();
            r.drawRectBlurred(cardX, rowY, colW, cH, cR, isSel?cardOn:(hov?cardHov:cardBg), 8f);
            int oa=(int)(20+50*ha.value())+(on?60:0)+(binding?40:0);
            r.drawStroke(cardX, rowY, colW, cH, cR, 1f, 1, 0f,
                binding?C_BIND:ColorUtil.rgba(w.borderR,w.borderG,w.borderB,oa));
            TypeAnim na = modNameAnim.computeIfAbsent(m, k->{TypeAnim a=new TypeAnim(50,90);a.snap(m.name());return a;});
            long flashAt = modKeyFlash.getOrDefault(m, 0L);
            boolean kFlash = System.currentTimeMillis()-flashAt<KEY_FLASH;
            String want = binding?"Нажмите клавишу":m.name();
            if (!na.getTarget().equals(want)) na.setTarget(want);
            String nd = na.update();
            if (binding&&!na.isRunning()) nd+=(System.currentTimeMillis()/400)%2==0?" _":"  ";
            r.drawText("regular", nd, cardX+PAD, rowY+(cH-fs)/2f, fs,
                binding?C_BIND:(on?C_TXT_ON:C_TXT));
            if (kFlash&&m.keybind()!=-1) {
                String ks=keyName(m.keybind());
                float kw=r.getTextWidth("regular",ks,8f)+8;
                float prog=Math.min(1f,(float)(System.currentTimeMillis()-flashAt)/KEY_FLASH);
                float kx=cardX+colW-PAD-kw-prog*6;
                int ka=(int)(200*(1f-prog*prog));
                r.drawRect((int)kx-2,rowY+(cH-14)/2,(int)kw,14,3,ColorUtil.rgba(100,180,255,(int)(ka*0.3f)));
                r.drawText("regular",ks,kx+2,rowY+(cH-8)/2f,8f,ColorUtil.rgba(100,180,255,ka));
            }
            col++; if (col>=cols){col=0; rowY+=cH+cGap;}
        }
        gfx.disableScissor();
        int totalH=((mods.size()+cols-1)/cols)*(cH+cGap)+PAD*2+topOff;
        int maxScroll=Math.max(0,totalH-ih+PAD);
        scroll=Math.max(0,Math.min(scroll,maxScroll));
        widgetScroll.put(w,scroll);
        if (totalH>ih) {
            int barH=Math.max(16,ih*ih/totalH);
            int barY=iy+(int)((float)scroll/maxScroll*(ih-barH));
            r.drawRect(ix+iw-4,barY,3,barH,2,ColorUtil.rgba(255,255,255,50));
        }
        if (hasSettings) {
            int setX=ix+modW+1, setW=iw-modW-1;
            r.drawRect(setX,iy,1,ih,0,C_DIV);
            gfx.enableScissor(setX+1,iy+1,setX+setW,iy+ih);
            renderSettingsPanel(r,gfx,sel,setX+1,iy,setW,ih,mx,my);
            gfx.disableScissor();
        }
    }

    // ── settings panel ────────────────────────────────────────────────────────
    private void renderSettingsPanel(AporiaRenderer r, GuiGraphics gfx,
                                     Module mod, int sx, int sy, int sw, int sh, int mx, int my) {
        if (mod==null) return;
        int cx=sx+PAD, cw=sw-PAD*2, y=sy+PAD;
        for (Field f : mod.getClass().getDeclaredFields()) {
            if (!Setting.class.isAssignableFrom(f.getType())) continue;
            f.setAccessible(true);
            try { Setting<?> s=(Setting<?>)f.get(mod); if (!s.isVisible()) continue; y=renderSettingRow(r,s,f,cx,cw,y,mx,my); }
            catch (IllegalAccessException ignored) {}
        }
    }

    private int renderSettingRow(AporiaRenderer r, Setting<?> s, Field field,
                                 int cx, int cw, int y, int mx, int my) {
        boolean hov=mx>=cx&&mx<cx+cw&&my>=y&&my<y+LINE_H;
        r.drawRect(cx,y,cw,LINE_H,4,hov?C_SET_H:C_SET_BG);
        int re=cx+cw-6;
        if (!(s instanceof BooleanSetting)&&!(s instanceof BindSetting))
            r.drawText("regular",s.name(),cx+6,y+(LINE_H-9)/2f,9f,C_TXT);
        if (s instanceof BooleanSetting bs) {
            TypeAnim ta=settingAnim.computeIfAbsent(s,k->{TypeAnim a=new TypeAnim(55,100);a.snap(bs.name());return a;});
            long fa=boolFlash.getOrDefault(s,0L); boolean fl=System.currentTimeMillis()-fa<BOOL_FLASH;
            String want=fl?(bs.isEnabled()?"Включено":"Выключено"):bs.name();
            if (!ta.getTarget().equals(want)) ta.setTarget(want);
            int tc=fl?(bs.isEnabled()?C_BLUE:C_RED):C_TXT;
            r.drawText("regular",ta.update(),cx+6,y+(LINE_H-9)/2f,9f,tc);
        } else if (s instanceof SelectSetting ss) {
            TypeAnim ta=settingAnim.computeIfAbsent(s,k->{TypeAnim a=new TypeAnim(50,90);a.snap(ss.get());return a;});
            if (!ta.getTarget().equals(ss.get())) ta.setTarget(ss.get());
            int selW=Math.min(cw/2,90),selX=re-selW,selY=y+(LINE_H-16)/2;
            boolean sh=mx>=selX&&mx<selX+selW&&my>=selY&&my<selY+16;
            r.drawRect(selX,selY,selW,16,4,sh?C_SEL_H:C_SEL_BG);
            r.drawText("regular",ta.update(),selX+6,selY+(16-9)/2f,9f,C_TXT_ON);
            r.drawText("regular","▼",selX+selW-12,selY+(16-8)/2f,8f,C_TXT);
        } else if (s instanceof MultiSelectSetting ms) {
            int chipH=14,chipPad=4,chipX=re;
            for (int i=ms.getOptions().size()-1;i>=0;i--) {
                String opt=ms.getOptions().get(i);
                int chipW=(int)AporiaRenderer.INSTANCE.getTextWidth("regular",opt,8f)+chipPad*2;
                chipX-=chipW+3; int chipY=y+(LINE_H-chipH)/2; boolean on=ms.isSelected(opt);
                r.drawRect(chipX,chipY,chipW,chipH,3,on?C_MUL_ON:C_MUL_OF);
                r.drawText("regular",opt,chipX+chipPad,chipY+(chipH-8)/2f,8f,on?C_TXT_ON:C_TXT);
                if (chipX<=cx+80) break;
            }
        } else if (s instanceof TextSetting ts) {
            int inW=Math.min(cw/2,100),inX=re-inW,inY=y+(LINE_H-14)/2; boolean foc=focusedText==ts;
            r.drawRect(inX,inY,inW,14,3,C_TXT_BG);
            r.drawStroke(inX,inY,inW,14,3,1f,1,0f,foc?C_TXT_BDF:C_TXT_BD);
            String disp=ts.get()+(foc&&(System.currentTimeMillis()/500)%2==0?"|":"");
            r.drawText("regular",disp,inX+4,inY+(14-8)/2f,8f,C_TXT_ON);
        } else if (s instanceof BindSetting bs) {
            boolean binding=settingBindMod!=null&&settingBindFld==field;
            TypeAnim ta=settingAnim.computeIfAbsent(s,k->{TypeAnim a=new TypeAnim(45,80);a.snap(bs.isBound()?keyName(bs.getKey()):bs.name());return a;});
            String want=binding?"Нажмите клавишу":(bs.isBound()?keyName(bs.getKey()):bs.name());
            if (!ta.getTarget().equals(want)) ta.setTarget(want);
            String disp=ta.update();
            if (binding&&!ta.isRunning()) disp+=(System.currentTimeMillis()/400)%2==0?" _":"  ";
            r.drawText("regular",disp,cx+6,y+(LINE_H-9)/2f,9f,binding?C_BIND:(bs.isBound()?C_TXT_ON:C_TXT));
        } else if (s instanceof ButtonSetting) {
            int btnW=Math.min(cw/3,80),btnX=re-btnW,btnY=y+(LINE_H-14)/2;
            boolean bh=mx>=btnX&&mx<btnX+btnW&&my>=btnY&&my<btnY+14;
            r.drawRect(btnX,btnY,btnW,14,3,bh?C_BTN_H:C_BTN_BG);
            r.drawText("regular","▶ Run",btnX+6,btnY+(14-8)/2f,8f,C_TXT_ON);
        }
        return y+LINE_H+2;
    }

    // ── popup settings window ─────────────────────────────────────────────────
    private void renderPopup(AporiaRenderer r, GuiGraphics gfx, Widget w, int mx, int my) {
        Module mod=widgetSelected.get(w);
        if (mod==null){popupWidget=null;return;}
        int pw=260,ph=300;
        int px=(int)(w.x+w.w/2-pw/2f), py=(int)(w.y+w.h/2-ph/2f);
        px=Math.max(0,Math.min(px,this.width-pw)); py=Math.max(0,Math.min(py,this.height-ph));
        r.drawRectBlurred(px,py,pw,ph,R,ColorUtil.rgba(20,22,35,230),14f);
        r.drawStroke(px,py,pw,ph,R,1f,1,0f,C_BORDER);
        r.drawText("bold",mod.name(),px+PAD,py+(TOPBAR_H-9)/2f,9f,C_TXT_ON);
        r.drawText("regular","✕",px+pw-PAD-8,py+(TOPBAR_H-8)/2f,8f,C_TXT);
        r.drawRect(px,py+TOPBAR_H,pw,1,0,C_DIV);
        int bodyH=ph-TOPBAR_H, totalH=settingsTotalH(mod);
        popupScroll+=(popupScrollT-popupScroll)*0.15f;
        int maxScroll=Math.max(0,totalH-bodyH+PAD);
        popupScroll=Math.max(0,Math.min(popupScroll,maxScroll));
        gfx.enableScissor(px+1,py+TOPBAR_H+1,px+pw-1,py+ph);
        int cx=px+PAD,cw=pw-PAD*2,y=py+TOPBAR_H+PAD-(int)popupScroll;
        for (Field f:mod.getClass().getDeclaredFields()) {
            if (!Setting.class.isAssignableFrom(f.getType())) continue;
            f.setAccessible(true);
            try { Setting<?> s=(Setting<?>)f.get(mod); if (!s.isVisible()) continue; y=renderSettingRow(r,s,f,cx,cw,y,mx,my); }
            catch (IllegalAccessException ignored) {}
        }
        gfx.disableScissor();
        if (totalH>bodyH) {
            int barH=Math.max(16,bodyH*bodyH/totalH);
            int barY=py+TOPBAR_H+(int)((float)popupScroll/maxScroll*(bodyH-barH));
            r.drawRect(px+pw-4,barY,3,barH,2,ColorUtil.rgba(255,255,255,50));
        }
    }

    // ── edit button ───────────────────────────────────────────────────────────
    private void renderEditButton(AporiaRenderer r, int mx, int my) {
        int bx=this.width-EDIT_BTN_S-10, by=this.height-EDIT_BTN_S-10;
        boolean hov=mx>=bx&&mx<bx+EDIT_BTN_S&&my>=by&&my<by+EDIT_BTN_S;
        int bg=editMode?ColorUtil.rgba(100,180,255,200):ColorUtil.rgba(60,60,80,hov?220:180);
        r.drawRect(bx,by,EDIT_BTN_S,EDIT_BTN_S,6,bg);
        r.drawStroke(bx,by,EDIT_BTN_S,EDIT_BTN_S,6,1f,1,0f,editMode?ColorUtil.rgba(100,180,255,255):ColorUtil.rgba(255,255,255,80));
        String label=editMode?"OK":"ED";
        float tw=r.getTextWidth("regular",label,8f);
        r.drawText("regular",label,bx+(EDIT_BTN_S-tw)/2f,by+(EDIT_BTN_S-8)/2f,8f,C_TXT_ON);
    }

    // ── edit overlay ──────────────────────────────────────────────────────────
    private void renderEditOverlay(AporiaRenderer r, GuiGraphics gfx, int mx, int my) {
        // grid
        if (showGrid) {
            int sw=this.width, sh=this.height;
            for (int gx=0;gx<sw;gx+=gridStep) r.drawLine(gx,0,gx,sh,0.5f,ColorUtil.rgba(255,255,255,12));
            for (int gy=0;gy<sh;gy+=gridStep) r.drawLine(0,gy,sw,gy,0.5f,ColorUtil.rgba(255,255,255,12));
        }
        // box select
        if (boxSelecting) {
            float bx=Math.min(boxX0,boxX1), by=Math.min(boxY0,boxY1);
            float bw=Math.abs(boxX1-boxX0), bh=Math.abs(boxY1-boxY0);
            r.drawRect(bx,by,bw,bh,2,ColorUtil.rgba(100,180,255,20));
            r.drawStroke(bx,by,bw,bh,2,1f,1,0f,ColorUtil.rgba(100,180,255,160));
        }
        for (Widget w:widgets) {
            int ix=(int)w.x,iy=(int)w.y,iw=(int)w.w,ih=w.collapsed?TOPBAR_H:(int)w.h;
            boolean sel=selection.contains(w);
            boolean hov=mx>=ix-HIT&&mx<ix+iw+HIT&&my>=iy-HIT&&my<iy+ih+HIT;
            if (w.locked) { r.drawStroke(ix-1,iy-1,iw+2,ih+2,R+1,1f,1,0f,ColorUtil.rgba(255,200,80,60)); continue; }
            if (sel) r.drawStroke(ix-1,iy-1,iw+2,ih+2,R+1,2f,1,0f,ColorUtil.rgba(100,180,255,200));
            else if (hov) { r.drawStroke(ix-1,iy-1,iw+2,ih+2,R+1,1.5f,1,0f,C_EDIT_S); renderResizeHandles(r,ix,iy,iw,ih); }
        }
        renderEditToolbar(r, mx, my);
    }

    private void renderResizeHandles(AporiaRenderer r, int ix, int iy, int iw, int ih) {
        int hs=6;
        int[][] pts={{ix-hs/2,iy-hs/2},{ix+iw/2-hs/2,iy-hs/2},{ix+iw-hs/2,iy-hs/2},
            {ix-hs/2,iy+ih/2-hs/2},{ix+iw-hs/2,iy+ih/2-hs/2},
            {ix-hs/2,iy+ih-hs/2},{ix+iw/2-hs/2,iy+ih-hs/2},{ix+iw-hs/2,iy+ih-hs/2}};
        for (int[] p:pts) r.drawRect(p[0],p[1],hs,hs,2,ColorUtil.rgba(100,180,255,200));
    }

    /** Renders the bottom edit toolbar: +, undo, redo, copy, paste, duplicate, align, grid, theme, layouts. */
    private void renderEditToolbar(AporiaRenderer r, int mx, int my) {
        String[] alignLabels = {"←","→","↑","↓","↔","↕"};
        String[] btns = {"+","↩","↪","⎘","⎙","⧉",alignLabels[alignIdx%alignLabels.length],"⊞",THEME_NAMES[themeIdx%THEME_NAMES.length].substring(0,Math.min(3,THEME_NAMES[themeIdx%THEME_NAMES.length].length())),"≡"};
        int btnW=26, btnH=22, gap=3, totalW=btns.length*(btnW+gap)-gap;
        int tx=this.width/2-totalW/2, ty=this.height-btnH-8;
        r.drawRect(tx-6,ty-4,totalW+12,btnH+8,8,ColorUtil.rgba(14,16,28,220));
        r.drawStroke(tx-6,ty-4,totalW+12,btnH+8,8,1f,1,0f,ColorUtil.rgba(100,180,255,40));
        for (int i=0;i<btns.length;i++) {
            int bx=tx+i*(btnW+gap);
            boolean hov=mx>=bx&&mx<bx+btnW&&my>=ty&&my<ty+btnH;
            r.drawRect(bx,ty,btnW,btnH,5,hov?ColorUtil.rgba(100,180,255,60):ColorUtil.rgba(255,255,255,10));
            float tw=r.getTextWidth("regular",btns[i],9f);
            r.drawText("regular",btns[i],bx+(btnW-tw)/2f,ty+(btnH-9)/2f,9f,C_TXT_ON);
        }
    }

    /** Returns toolbar button index at (mx,my), or -1. */
    private int toolbarHit(int mx, int my) {
        String[] btns = {"+","↩","↪","⎘","⎙","⧉","←→","⊞","◑","≡"};
        int btnW=26, btnH=22, gap=3, totalW=btns.length*(btnW+gap)-gap;
        int tx=this.width/2-totalW/2, ty=this.height-btnH-8;
        if (my<ty||my>ty+btnH) return -1;
        int i=(mx-tx)/(btnW+gap);
        if (i<0||i>=btns.length) return -1;
        int bx=tx+i*(btnW+gap);
        return (mx>=bx&&mx<bx+btnW)?i:-1;
    }

    // ── property panel ────────────────────────────────────────────────────────
    private static final int PROP_W   = 220;
    private static final int PROP_IH  = 22;
    private static final int PROP_SH  = 18;
    private static final int PROP_PAD = 8;

    /** Renders the full property panel for propWidget. */
    private void renderPropPanel(AporiaRenderer r, GuiGraphics gfx, int mx, int my) {
        if (propWidget==null) return;
        List<PropRow> rows = buildPropRows(propWidget);
        int totalH = rows.size()*PROP_IH + PROP_PAD*2 + 24;
        int px = Math.min((int)propWidget.x+(int)propWidget.w+6, this.width-PROP_W-4);
        int py = Math.max(4, Math.min((int)propWidget.y, this.height-totalH-4));
        int ph = Math.min(totalH, this.height-py-4);
        propScroll += (propScrollT-propScroll)*0.15f;
        int maxScroll = Math.max(0, totalH-ph);
        propScroll = Math.max(0, Math.min(propScroll, maxScroll));
        r.drawRectBlurred(px,py,PROP_W,ph,10,C_PROP_BG,12f);
        r.drawStroke(px,py,PROP_W,ph,10,1f,1,0f,C_PROP_BD);
        r.drawText("bold","Свойства: "+widgetTitle(propWidget),px+PROP_PAD,py+6,8f,C_TXT_ON);
        r.drawRect(px,py+20,PROP_W,1,0,C_DIV);
        gfx.enableScissor(px+1,py+22,px+PROP_W-1,py+ph);
        int iy=py+22+PROP_PAD-(int)propScroll;
        for (PropRow row:rows) {
            renderPropRow(r,gfx,row,px,iy,PROP_W,mx,my);
            iy+=PROP_IH;
        }
        gfx.disableScissor();
        if (totalH>ph) {
            int barH=Math.max(12,ph*ph/totalH);
            int barY=py+(int)((float)propScroll/maxScroll*(ph-barH));
            r.drawRect(px+PROP_W-3,barY,2,barH,1,ColorUtil.rgba(255,255,255,60));
        }
    }

    private void renderPropRow(AporiaRenderer r, GuiGraphics gfx, PropRow row,
                               int px, int iy, int pw, int mx, int my) {
        int lw=80, rx=px+lw, rw=pw-lw-PROP_PAD*2;
        r.drawText("regular",row.label,px+PROP_PAD,iy+(PROP_IH-8)/2f,8f,C_TXT_DIM);
        if (row.type==PropRow.Type.TOGGLE) {
            boolean val = getPropBool(propWidget, row.field);
            boolean hov=mx>=rx&&mx<rx+rw&&my>=iy&&my<iy+PROP_IH;
            int bg=val?ColorUtil.rgba(100,180,255,80):ColorUtil.rgba(255,255,255,14);
            r.drawRect(rx,iy+3,rw,PROP_IH-6,4,hov?ColorUtil.rgba(255,255,255,28):bg);
            r.drawText("regular",val?"Вкл":"Выкл",rx+6,iy+(PROP_IH-8)/2f,8f,val?C_TXT_ON:C_TXT_DIM);
        } else if (row.type==PropRow.Type.SLIDER) {
            float val=getPropFloat(propWidget,row.field);
            float t=(val-row.min)/(row.max-row.min);
            int sh=PROP_SH; int sy=iy+(PROP_IH-sh)/2;
            r.drawRect(rx,sy,rw,sh,4,C_SLD_BG);
            r.drawRect(rx,sy,(int)(rw*t),sh,4,C_SLD_FG);
            boolean hov=mx>=rx&&mx<rx+rw&&my>=sy&&my<sy+sh;
            if (hov) r.drawStroke(rx,sy,rw,sh,4,1f,1,0f,C_SLD_H);
            String valStr = row.isInt ? String.valueOf((int)val) : String.format("%.1f",val);
            r.drawText("regular",valStr,rx+rw+4,iy+(PROP_IH-8)/2f,8f,C_TXT_ON);
        } else if (row.type==PropRow.Type.CYCLE) {
            String val=getPropString(propWidget,row.field);
            boolean hov=mx>=rx&&mx<rx+rw&&my>=iy&&my<iy+PROP_IH;
            r.drawRect(rx,iy+3,rw,PROP_IH-6,4,hov?C_OPT_H:ColorUtil.rgba(255,255,255,14));
            r.drawText("regular",val,rx+6,iy+(PROP_IH-8)/2f,8f,C_TXT_ON);
            r.drawText("regular","▶",rx+rw-12,iy+(PROP_IH-8)/2f,8f,C_TXT_DIM);
        } else if (row.type==PropRow.Type.COLOR_PREVIEW) {
            int col=getPropInt(propWidget,row.field);
            r.drawRect(rx,iy+4,rw,PROP_IH-8,4,col);
            r.drawStroke(rx,iy+4,rw,PROP_IH-8,4,1f,1,0f,ColorUtil.rgba(255,255,255,40));
        } else if (row.type==PropRow.Type.ACTION) {
            boolean hov=mx>=rx&&mx<rx+rw&&my>=iy&&my<iy+PROP_IH;
            r.drawRect(rx,iy+3,rw,PROP_IH-6,4,hov?ColorUtil.rgba(255,100,100,80):ColorUtil.rgba(255,100,100,30));
            r.drawText("regular",row.field,rx+6,iy+(PROP_IH-8)/2f,8f,ColorUtil.rgba(255,120,120,220));
        } else if (row.type==PropRow.Type.IMAGE_PICK) {
            String fname=propWidget.imagePath.isEmpty()?"(нет)":java.nio.file.Paths.get(propWidget.imagePath).getFileName().toString();
            boolean hov=mx>=rx&&mx<rx+rw&&my>=iy&&my<iy+PROP_IH;
            r.drawRect(rx,iy+3,rw,PROP_IH-6,4,hov?C_OPT_H:ColorUtil.rgba(255,255,255,14));
            r.drawText("regular",fname,rx+4,iy+(PROP_IH-8)/2f,7f,C_TXT_ON);
        }
    }

    /** A single row in the property panel. */
    private static final class PropRow {
        enum Type { TOGGLE, SLIDER, CYCLE, COLOR_PREVIEW, ACTION, IMAGE_PICK }
        final String label, field;
        final Type type;
        final float min, max;
        final boolean isInt;
        PropRow(String label, String field, Type type, float min, float max, boolean isInt) {
            this.label=label; this.field=field; this.type=type; this.min=min; this.max=max; this.isInt=isInt;
        }
        static PropRow toggle(String l, String f)                    { return new PropRow(l,f,Type.TOGGLE,0,1,true); }
        static PropRow slider(String l, String f, float mn, float mx) { return new PropRow(l,f,Type.SLIDER,mn,mx,false); }
        static PropRow islider(String l, String f, int mn, int mx)    { return new PropRow(l,f,Type.SLIDER,mn,mx,true); }
        static PropRow cycle(String l, String f)                     { return new PropRow(l,f,Type.CYCLE,0,0,false); }
        static PropRow colorPrev(String l, String f)                 { return new PropRow(l,f,Type.COLOR_PREVIEW,0,0,false); }
        static PropRow action(String l, String f)                    { return new PropRow(l,f,Type.ACTION,0,0,false); }
        static PropRow imagePick(String l)                           { return new PropRow(l,"imagePath",Type.IMAGE_PICK,0,0,false); }
    }

    /** Builds the property row list for a widget. */
    private List<PropRow> buildPropRows(Widget w) {
        List<PropRow> rows = new ArrayList<>();
        rows.add(PropRow.toggle("Блюр",       "blur"));
        rows.add(PropRow.slider("Сила блюра", "blurStrength", 0f, 40f));
        rows.add(PropRow.islider("Фон α",     "bgAlpha",      0, 255));
        rows.add(PropRow.islider("Фон R",     "bgR",          0, 255));
        rows.add(PropRow.islider("Фон G",     "bgG",          0, 255));
        rows.add(PropRow.islider("Фон B",     "bgB",          0, 255));
        rows.add(PropRow.colorPrev("Фон цвет","_bgColor"));
        rows.add(PropRow.toggle("Рамка",      "border"));
        rows.add(PropRow.islider("Рамка α",   "borderAlpha",  0, 255));
        rows.add(PropRow.islider("Рамка R",   "borderR",      0, 255));
        rows.add(PropRow.islider("Рамка G",   "borderG",      0, 255));
        rows.add(PropRow.islider("Рамка B",   "borderB",      0, 255));
        rows.add(PropRow.islider("Радиус",    "borderRadius", 0, 30));
        if (w.type==WidgetType.MODULE_LIST) {
            rows.add(PropRow.cycle("Настройки",  "settingsMode"));
            rows.add(PropRow.cycle("Категория",  "category"));
            rows.add(PropRow.islider("Колонки",  "cols",         0, 8));
            rows.add(PropRow.islider("Рад.карт", "cardRadius",   0, 16));
            rows.add(PropRow.islider("Отст.карт","cardGap",      0, 12));
            rows.add(PropRow.islider("Выс.карт", "cardH",        16, 50));
            rows.add(PropRow.slider("Шрифт",     "fontSize",     6f, 16f));
            rows.add(PropRow.islider("Карта α",  "cardAlpha",    0, 255));
            rows.add(PropRow.islider("Карта R",  "cardR",        0, 255));
            rows.add(PropRow.islider("Карта G",  "cardG",        0, 255));
            rows.add(PropRow.islider("Карта B",  "cardB",        0, 255));
            rows.add(PropRow.islider("Вкл α",    "cardOnAlpha",  0, 255));
        }
        if (w.type==WidgetType.CLIENT_NAME||w.type==WidgetType.ACCOUNT||w.type==WidgetType.TEXT) {
            rows.add(PropRow.slider("Шрифт",     "fontSize",     6f, 24f));
        }
        if (w.type==WidgetType.TEXT) {
            rows.add(PropRow.cycle("Шрифт",      "fontName"));
            rows.add(PropRow.islider("Текст R",  "shapeR",       0, 255));
            rows.add(PropRow.islider("Текст G",  "shapeG",       0, 255));
            rows.add(PropRow.islider("Текст B",  "shapeB",       0, 255));
            rows.add(PropRow.islider("Текст α",  "shapeAlpha",   0, 255));
            rows.add(PropRow.toggle("Переменные","textVars"));
            rows.add(PropRow.action("Изменить текст","editText"));
        }
        if (w.type==WidgetType.CIRCLE||w.type==WidgetType.TRIANGLE) {
            rows.add(PropRow.islider("Цвет R",   "shapeR",       0, 255));
            rows.add(PropRow.islider("Цвет G",   "shapeG",       0, 255));
            rows.add(PropRow.islider("Цвет B",   "shapeB",       0, 255));
            rows.add(PropRow.islider("Цвет α",   "shapeAlpha",   0, 255));
        }
        if (w.type==WidgetType.LINE) {
            rows.add(PropRow.islider("Цвет R",   "shapeR",       0, 255));
            rows.add(PropRow.islider("Цвет G",   "shapeG",       0, 255));
            rows.add(PropRow.islider("Цвет B",   "shapeB",       0, 255));
            rows.add(PropRow.islider("Цвет α",   "shapeAlpha",   0, 255));
            rows.add(PropRow.slider("Толщина",   "lineThickness",1f, 20f));
        }
        if (w.type==WidgetType.IMAGE) {
            rows.add(PropRow.imagePick("Файл"));
            rows.add(PropRow.cycle("Подгонка",   "imageFit"));
        }
        if (w.type==WidgetType.CATEGORY_LIST) {
            rows.add(PropRow.islider("Рад.карт", "cardRadius",   0, 16));
            rows.add(PropRow.islider("Отст.карт","cardGap",      0, 12));
            rows.add(PropRow.islider("Выс.карт", "cardH",        16, 50));
            rows.add(PropRow.slider("Шрифт",     "fontSize",     6f, 16f));
            rows.add(PropRow.islider("Карта α",  "cardAlpha",    0, 255));
            rows.add(PropRow.islider("Карта R",  "cardR",        0, 255));
            rows.add(PropRow.islider("Карта G",  "cardG",        0, 255));
            rows.add(PropRow.islider("Карта B",  "cardB",        0, 255));
        }
        if (w.type==WidgetType.BUTTON) {
            rows.add(PropRow.slider("Шрифт",     "fontSize",     6f, 24f));
            rows.add(PropRow.cycle("Шрифт",      "fontName"));
            rows.add(PropRow.islider("Текст R",  "shapeR",       0, 255));
            rows.add(PropRow.islider("Текст G",  "shapeG",       0, 255));
            rows.add(PropRow.islider("Текст B",  "shapeB",       0, 255));
            rows.add(PropRow.islider("Текст α",  "shapeAlpha",   0, 255));
            rows.add(PropRow.action("Изменить текст","editText"));
        }
        rows.add(PropRow.toggle("Заголовок",  "showTitle"));
        rows.add(PropRow.action("Изм. заголовок","editTitle"));
        rows.add(PropRow.slider("Поворот",    "rotation",   -180f, 180f));
        rows.add(PropRow.toggle("Заблокировать","locked"));
        rows.add(PropRow.action(w.collapsed?"Развернуть":"Свернуть","collapse"));
        rows.add(PropRow.action("Удалить","delete"));
        return rows;
    }

    // ── prop field accessors ──────────────────────────────────────────────────
    private boolean getPropBool(Widget w, String f) {
        return switch(f) {
            case "blur"      -> w.blur;
            case "border"    -> w.border;
            case "showTitle" -> w.showTitle;
            case "locked"    -> w.locked;
            case "textVars"  -> w.textVars;
            default -> false;
        };
    }
    private void setPropBool(Widget w, String f, boolean v) {
        switch(f) {
            case "blur"      -> w.blur=v;
            case "border"    -> w.border=v;
            case "showTitle" -> w.showTitle=v;
            case "locked"    -> w.locked=v;
            case "textVars"  -> w.textVars=v;
        }
    }
    private float getPropFloat(Widget w, String f) {
        return switch(f) {
            case "blurStrength"  -> w.blurStrength;
            case "bgAlpha"       -> w.bgAlpha;
            case "bgR"           -> w.bgR;
            case "bgG"           -> w.bgG;
            case "bgB"           -> w.bgB;
            case "borderAlpha"   -> w.borderAlpha;
            case "borderR"       -> w.borderR;
            case "borderG"       -> w.borderG;
            case "borderB"       -> w.borderB;
            case "borderRadius"  -> w.borderRadius;
            case "cardRadius"    -> w.cardRadius;
            case "cardGap"       -> w.cardGap;
            case "cardH"         -> w.cardH;
            case "cols"          -> w.cols;
            case "fontSize"      -> w.fontSize;
            case "cardAlpha"     -> w.cardAlpha;
            case "cardR"         -> w.cardR;
            case "cardG"         -> w.cardG;
            case "cardB"         -> w.cardB;
            case "cardOnAlpha"   -> w.cardOnAlpha;
            case "shapeR"        -> w.shapeR;
            case "shapeG"        -> w.shapeG;
            case "shapeB"        -> w.shapeB;
            case "shapeAlpha"    -> w.shapeAlpha;
            case "lineThickness" -> w.lineThickness;
            case "rotation"      -> w.rotation;
            default -> 0f;
        };
    }
    private void setPropFloat(Widget w, String f, float v) {
        switch(f) {
            case "blurStrength"  -> w.blurStrength=(float)Math.round(v*10)/10f;
            case "bgAlpha"       -> w.bgAlpha=(int)v;
            case "bgR"           -> w.bgR=(int)v;
            case "bgG"           -> w.bgG=(int)v;
            case "bgB"           -> w.bgB=(int)v;
            case "borderAlpha"   -> w.borderAlpha=(int)v;
            case "borderR"       -> w.borderR=(int)v;
            case "borderG"       -> w.borderG=(int)v;
            case "borderB"       -> w.borderB=(int)v;
            case "borderRadius"  -> w.borderRadius=(int)v;
            case "cardRadius"    -> w.cardRadius=(int)v;
            case "cardGap"       -> w.cardGap=(int)v;
            case "cardH"         -> w.cardH=(int)v;
            case "cols"          -> w.cols=(int)v;
            case "fontSize"      -> w.fontSize=(float)Math.round(v*10)/10f;
            case "cardAlpha"     -> w.cardAlpha=(int)v;
            case "cardR"         -> w.cardR=(int)v;
            case "cardG"         -> w.cardG=(int)v;
            case "cardB"         -> w.cardB=(int)v;
            case "cardOnAlpha"   -> w.cardOnAlpha=(int)v;
            case "shapeR"        -> w.shapeR=(int)v;
            case "shapeG"        -> w.shapeG=(int)v;
            case "shapeB"        -> w.shapeB=(int)v;
            case "shapeAlpha"    -> w.shapeAlpha=(int)v;
            case "lineThickness" -> w.lineThickness=v;
            case "rotation"      -> w.rotation=v;
        }
    }
    private int getPropInt(Widget w, String f) {
        return switch(f) {
            case "_bgColor"     -> ColorUtil.rgba(w.bgR,w.bgG,w.bgB,w.bgAlpha);
            case "_borderColor" -> ColorUtil.rgba(w.borderR,w.borderG,w.borderB,w.borderAlpha);
            default -> 0;
        };
    }
    private String getPropString(Widget w, String f) {
        return switch(f) {
            case "settingsMode" -> w.settingsMode==SettingsMode.INLINE?"Внутри":"Попап";
            case "category"     -> w.category.isEmpty()?"Все":w.category;
            case "imageFit"     -> w.imageFit;
            case "fontName"     -> w.fontName;
            default -> "";
        };
    }
    private void cyclePropString(Widget w, String f) {
        switch(f) {
            case "settingsMode" -> w.settingsMode = w.settingsMode==SettingsMode.INLINE?SettingsMode.POPUP:SettingsMode.INLINE;
            case "category" -> {
                Category[] cats=Category.values();
                if (w.category.isEmpty()) { w.category=cats[0].name(); }
                else {
                    int ci=-1;
                    for (int i=0;i<cats.length;i++) if (cats[i].name().equals(w.category)){ci=i;break;}
                    w.category=(ci+1>=cats.length)?"":cats[ci+1].name();
                }
            }
            case "imageFit" -> {
                String[] fits={"fill","fit","stretch"};
                int ci=0; for (int i=0;i<fits.length;i++) if (fits[i].equals(w.imageFit)){ci=i;break;}
                w.imageFit=fits[(ci+1)%fits.length];
            }
            case "fontName" -> {
                String[] fonts={"regular","bold","regularnew"};
                int ci=0; for (int i=0;i<fonts.length;i++) if (fonts[i].equals(w.fontName)){ci=i;break;}
                w.fontName=fonts[(ci+1)%fonts.length];
            }
        }
    }

    // ── add menu ──────────────────────────────────────────────────────────────
    private static final String[] ADD_LABELS = {"Список модулей","Категории","Поиск","Аккаунт","Имя клиента","Прямоугольник","Круг","Линия","Треугольник","Текст","Кнопка","Изображение"};
    private static final WidgetType[] ADD_TYPES = {WidgetType.MODULE_LIST,WidgetType.CATEGORY_LIST,WidgetType.SEARCH,WidgetType.ACCOUNT,WidgetType.CLIENT_NAME,WidgetType.RECT,WidgetType.CIRCLE,WidgetType.LINE,WidgetType.TRIANGLE,WidgetType.TEXT,WidgetType.BUTTON,WidgetType.IMAGE};

    private void renderAddMenu(AporiaRenderer r, GuiGraphics gfx, int mx, int my) {
        int itemH=22, menuW=180, pad=8;
        int menuH=ADD_LABELS.length*itemH+pad;
        int ox=Math.min(addMenuX,this.width-menuW-4), oy=Math.min(addMenuY,this.height-menuH-4);
        r.drawRectBlurred(ox,oy,menuW,menuH,8,C_OPT_BG,10f);
        r.drawStroke(ox,oy,menuW,menuH,8,1f,1,0f,C_BORDER);
        int iy=oy+pad/2;
        for (String s:ADD_LABELS) {
            boolean hov=mx>=ox&&mx<ox+menuW&&my>=iy&&my<iy+itemH;
            if (hov) r.drawRect(ox+2,iy,menuW-4,itemH,4,C_OPT_H);
            r.drawText("regular",s,ox+10,iy+(itemH-9)/2f,9f,C_TXT_ON);
            iy+=itemH;
        }
    }

    // ── image picker ──────────────────────────────────────────────────────────
    private void scanImages() {
        long now=System.currentTimeMillis();
        if (now-imageScanTime<3000) return;
        imageScanTime=now; imageFiles.clear();
        Path imgDir=FilesManager.ROOT.resolve("image");
        if (!Files.exists(imgDir)) { try{Files.createDirectories(imgDir);}catch(IOException ignored){} return; }
        try (var stream=Files.list(imgDir)) {
            stream.filter(p->{String n=p.getFileName().toString().toLowerCase();return n.endsWith(".png")||n.endsWith(".jpg")||n.endsWith(".jpeg");})
                .sorted().forEach(imageFiles::add);
        } catch (IOException ignored) {}
    }

    private void renderImagePicker(AporiaRenderer r, int mx, int my) {
        if (imagePickWidget==null) return;
        scanImages();
        int itemH=22, menuW=220, pad=8;
        int menuH=Math.max(1,imageFiles.size())*itemH+pad+itemH;
        int ox=Math.min(imagePickX,this.width-menuW-4), oy=Math.min(imagePickY,this.height-menuH-4);
        r.drawRect(ox,oy,menuW,menuH,8,C_OPT_BG);
        r.drawStroke(ox,oy,menuW,menuH,8,1f,1,0f,C_BORDER);
        int iy=oy+pad/2;
        r.drawText("bold","Выбор изображения",ox+10,iy+(itemH-9)/2f,9f,C_TXT_ON); iy+=itemH;
        if (imageFiles.isEmpty()) {
            r.drawText("regular","Нет файлов в ~/.apr/image/",ox+10,iy+(itemH-9)/2f,8f,C_TXT_DIM);
        } else {
            for (Path p:imageFiles) {
                boolean hov=mx>=ox&&mx<ox+menuW&&my>=iy&&my<iy+itemH;
                boolean sel=p.toAbsolutePath().toString().equals(imagePickWidget.imagePath);
                if (hov||sel) r.drawRect(ox+2,iy,menuW-4,itemH,4,sel?C_EDIT_S:C_OPT_H);
                r.drawText("regular",p.getFileName().toString(),ox+10,iy+(itemH-9)/2f,8f,C_TXT_ON);
                iy+=itemH;
            }
        }
    }

    // ── undo/redo/clipboard/align helpers ────────────────────────────────────
    private void pushUndo() {
        List<Widget> snap = widgets.stream().map(Widget::copy).collect(Collectors.toList());
        undoStack.push(snap);
        while (undoStack.size()>MAX_UNDO) undoStack.removeLast();
        redoStack.clear();
    }
    private void undo() {
        if (undoStack.isEmpty()) return;
        redoStack.push(widgets.stream().map(Widget::copy).collect(Collectors.toList()));
        List<Widget> snap=undoStack.pop(); widgets.clear(); widgets.addAll(snap); selection.clear();
    }
    private void redo() {
        if (redoStack.isEmpty()) return;
        undoStack.push(widgets.stream().map(Widget::copy).collect(Collectors.toList()));
        List<Widget> snap=redoStack.pop(); widgets.clear(); widgets.addAll(snap); selection.clear();
    }
    private void copySelected() {
        clipboard.clear();
        for (Widget w:selection) clipboard.add(w.copy());
    }
    private void pasteClipboard() {
        if (clipboard.isEmpty()) return;
        pushUndo(); selection.clear();
        for (Widget c:clipboard) { Widget n=c.copy(); n.x+=16; n.y+=16; widgets.add(n); selection.add(n); }
        saveLayout();
    }
    private void duplicateSelected() {
        if (selection.isEmpty()) return;
        pushUndo();
        List<Widget> dups=selection.stream().map(w->{Widget n=w.copy();n.x+=16;n.y+=16;return n;}).collect(Collectors.toList());
        widgets.addAll(dups); selection.clear(); selection.addAll(dups); saveLayout();
    }
    private void alignSelected(String dir) {
        if (selection.size()<2) return;
        pushUndo();
        float minX=Float.MAX_VALUE,minY=Float.MAX_VALUE,maxX=-Float.MAX_VALUE,maxY=-Float.MAX_VALUE;
        for (Widget w:selection){minX=Math.min(minX,w.x);minY=Math.min(minY,w.y);maxX=Math.max(maxX,w.x+w.w);maxY=Math.max(maxY,w.y+w.h);}
        for (Widget w:selection) switch(dir) {
            case "left"   -> w.x=minX;
            case "right"  -> w.x=maxX-w.w;
            case "top"    -> w.y=minY;
            case "bottom" -> w.y=maxY-w.h;
            case "centerH"-> w.x=minX+(maxX-minX)/2f-w.w/2f;
            case "centerV"-> w.y=minY+(maxY-minY)/2f-w.h/2f;
        }
        saveLayout();
    }
    private void applyTheme(int idx) {
        if (idx<0||idx>=THEME_NAMES.length) return;
        pushUndo();
        int[] bg=THEME_BG[idx], bd=THEME_BD[idx], cd=THEME_CARD[idx];
        for (Widget w:widgets) {
            w.bgR=bg[0]; w.bgG=bg[1]; w.bgB=bg[2]; w.bgAlpha=bg[3];
            w.borderR=bd[0]; w.borderG=bd[1]; w.borderB=bd[2]; w.borderAlpha=bd[3];
            w.cardR=cd[0]; w.cardG=cd[1]; w.cardB=cd[2]; w.cardAlpha=cd[3];
        }
        saveLayout();
    }
    private float snap(float v) { return gridSnap ? Math.round(v/(float)gridStep)*gridStep : v; }

    // ── mouse clicked ─────────────────────────────────────────────────────────
    @Override
    public boolean mouseClicked(MouseButtonEvent e, boolean b) {
        int mx=(int)e.x(), my=(int)e.y();

        int editBx=this.width-EDIT_BTN_S-10, editBy=this.height-EDIT_BTN_S-10;
        if (mx>=editBx&&mx<editBx+EDIT_BTN_S&&my>=editBy&&my<editBy+EDIT_BTN_S) {
            editMode=!editMode; propWidget=null; addMenuWidget=null; imagePickWidget=null; editTextWidget=null;
            if (!editMode) saveLayout(); return true;
        }

        if (editMode) {
            // toolbar
            int tbIdx=toolbarHit(mx,my);
            if (tbIdx>=0) {
                switch(tbIdx) {
                    case 0 -> { addMenuWidget=new Widget(WidgetType.RECT,0,0,0,0); addMenuX=mx; addMenuY=my; propWidget=null; }
                    case 1 -> undo();
                    case 2 -> redo();
                    case 3 -> copySelected();
                    case 4 -> pasteClipboard();
                    case 5 -> duplicateSelected();
                    case 6 -> { if (!selection.isEmpty()) { alignSelected(ALIGN_DIRS[alignIdx%ALIGN_DIRS.length]); alignIdx++; } }
                    case 7 -> { gridSnap=!gridSnap; showGrid=gridSnap; }
                    case 8 -> { applyTheme(themeIdx%THEME_NAMES.length); themeIdx++; }
                    case 9 -> layoutMenuOpen=!layoutMenuOpen;
                }
                return true;
            }

            if (addMenuWidget!=null) {
                int itemH=22, menuW=180, pad=8;
                int menuH=ADD_LABELS.length*itemH+pad;
                int ox=Math.min(addMenuX,this.width-menuW-4), oy=Math.min(addMenuY,this.height-menuH-4);
                if (mx>=ox&&mx<ox+menuW&&my>=oy&&my<oy+menuH) {
                    int idx=(my-oy-pad/2)/itemH;
                    if (idx>=0&&idx<ADD_TYPES.length) {
                        WidgetType t=ADD_TYPES[idx];
                        float nw=t==WidgetType.MODULE_LIST?320:t==WidgetType.SEARCH?180:t==WidgetType.LINE?200:140;
                        float nh=t==WidgetType.MODULE_LIST?260:t==WidgetType.LINE?20:36;
                        widgets.add(new Widget(t,this.width/2f-nw/2,this.height/2f-nh/2,nw,nh));
                        saveLayout();
                    }
                }
                addMenuWidget=null; return true;
            }

            if (imagePickWidget!=null) {
                scanImages();
                int itemH=22, menuW=220, pad=8;
                int menuH=Math.max(1,imageFiles.size())*itemH+pad+itemH;
                int ox=Math.min(imagePickX,this.width-menuW-4), oy=Math.min(imagePickY,this.height-menuH-4);
                if (mx>=ox&&mx<ox+menuW&&my>=oy&&my<oy+menuH) {
                    int row=(my-oy-pad/2-itemH)/itemH;
                    if (row>=0&&row<imageFiles.size()) {
                        imagePickWidget.imagePath=imageFiles.get(row).toAbsolutePath().toString();
                        saveLayout();
                    }
                }
                imagePickWidget=null; return true;
            }

            if (propWidget!=null) {
                List<PropRow> rows=buildPropRows(propWidget);
                int totalH=rows.size()*PROP_IH+PROP_PAD*2+24;
                int px=Math.min((int)propWidget.x+(int)propWidget.w+6,this.width-PROP_W-4);
                int py=Math.max(4,Math.min((int)propWidget.y,this.height-totalH-4));
                int ph=Math.min(totalH,this.height-py-4);
                if (mx>=px&&mx<px+PROP_W&&my>=py&&my<py+ph) {
                    int lw=80, rx=px+lw, rw=PROP_W-lw-PROP_PAD*2;
                    int iy=py+22+PROP_PAD-(int)propScroll;
                    for (PropRow row:rows) {
                        if (my>=iy&&my<iy+PROP_IH) {
                            handlePropClick(row, rx, iy, rw, mx, my); saveLayout(); break;
                        }
                        iy+=PROP_IH;
                    }
                    return true;
                }
                propWidget=null;
            }

            if (e.button()==1) {
                for (int i=widgets.size()-1;i>=0;i--) {
                    Widget w=widgets.get(i);
                    int ih=w.collapsed?TOPBAR_H:(int)w.h;
                    if (mx>=(int)w.x&&mx<(int)w.x+(int)w.w&&my>=(int)w.y&&my<(int)w.y+ih) {
                        propWidget=w; propScroll=propScrollT=0; return true;
                    }
                }
            }

            if (e.button()==0) {
                for (int i=widgets.size()-1;i>=0;i--) {
                    Widget w=widgets.get(i);
                    if (w.locked) continue;
                    int ix=(int)w.x,iy=(int)w.y,iw=(int)w.w,ih=w.collapsed?TOPBAR_H:(int)w.h;
                    boolean el=mx>=ix-HIT&&mx<ix+HIT, er=mx>=ix+iw-HIT&&mx<ix+iw+HIT;
                    boolean et=my>=iy-HIT&&my<iy+HIT, eb=my>=iy+ih-HIT&&my<iy+ih+HIT;
                    boolean onEdge=(el||er||et||eb)&&mx>=ix-HIT&&mx<=ix+iw+HIT&&my>=iy-HIT&&my<=iy+ih+HIT;
                    if (onEdge) {
                        pushUndo();
                        resizeWidget=w; resL=el; resR=er; resT=et; resB=eb;
                        resStartMx=mx; resStartMy=my; resStartX=w.x; resStartY=w.y; resStartW=w.w; resStartH=w.h;
                        return true;
                    }
                    if (mx>=ix&&mx<ix+iw&&my>=iy&&my<iy+ih) {
                        boolean ctrl=org.lwjgl.glfw.GLFW.glfwGetKey(Minecraft.getInstance().getWindow().handle(), org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_CONTROL)==1;
                        if (ctrl) { if (selection.contains(w)) selection.remove(w); else selection.add(w); return true; }
                        if (!selection.contains(w)) { selection.clear(); selection.add(w); }
                        pushUndo();
                        dragWidget=w; dragOx=mx-w.x; dragOy=my-w.y;
                        multiDragOffsets.clear();
                        for (Widget s:selection) multiDragOffsets.put(s,new float[]{mx-s.x,mx-s.y});
                        return true;
                    }
                }
                // box select start
                selection.clear(); boxSelecting=true; boxX0=boxX1=mx; boxY0=boxY1=my;
            }
            return true;
        }

        // ── LIVE mode ──
        if (popupWidget!=null) {
            Module mod=widgetSelected.get(popupWidget);
            if (mod!=null) {
                int pw2=260,ph2=300;
                int px2=(int)(popupWidget.x+popupWidget.w/2-pw2/2f), py2=(int)(popupWidget.y+popupWidget.h/2-ph2/2f);
                px2=Math.max(0,Math.min(px2,this.width-pw2)); py2=Math.max(0,Math.min(py2,this.height-ph2));
                if (mx>=px2+pw2-PAD-12&&mx<px2+pw2-PAD&&my>=py2&&my<py2+TOPBAR_H){popupWidget=null;return true;}
                if (mx>=px2&&mx<px2+pw2&&my>=py2&&my<py2+ph2) {
                    handleSettingsClick(mod,px2+PAD,pw2-PAD*2,py2+TOPBAR_H+PAD-(int)popupScroll,mx,my); return true;
                }
            }
            popupWidget=null;
        }

        if (editTextWidget!=null) { editTextWidget=null; }

        if (e.button()==0) {
            for (Widget w:widgets) {
                if (w.type==WidgetType.SEARCH) {
                    int ix=(int)w.x,iy=(int)w.y,iw=(int)w.w,ih=(int)w.h;
                    if (mx>=ix&&mx<ix+iw&&my>=iy&&my<iy+ih){searchFocused=true;searchWidget=w;focusedText=null;return true;}
                }
                if (w.type==WidgetType.BUTTON) {
                    int ix=(int)w.x,iy=(int)w.y,iw=(int)w.w,ih=(int)w.h;
                    if (mx>=ix&&mx<ix+iw&&my>=iy&&my<iy+ih) {
                        if (!w.buttonAction.isEmpty()) {
                            Module m=ModuleManager.INSTANCE.get(w.buttonAction);
                            if (m!=null) m.toggle();
                        }
                        return true;
                    }
                }
            }
            searchFocused=false;
            for (Widget w:widgets) {
                if (w.type!=WidgetType.MODULE_LIST||w.collapsed) continue;
                int ix=(int)w.x,iy=(int)w.y,iw=(int)w.w,ih=(int)w.h;
                if (mx<ix||mx>=ix+iw||my<iy||my>=iy+ih) continue;
                Module sel=widgetSelected.get(w);
                boolean hasSettings=sel!=null&&w.settingsMode==SettingsMode.INLINE;
                int modW=hasSettings?Math.min(200,(int)(iw*0.45f)):iw-2;
                int autoCols=Math.max(1,modW/COL_MIN_W);
                int cols=w.cols>0?w.cols:autoCols;
                int colW=(modW-PAD*(cols+1))/cols;
                int cH=w.cardH>0?w.cardH:26;
                float scroll=widgetScroll.getOrDefault(w,0f);
                int col=0,rowY=iy+PAD-(int)scroll;
                for (Module m:filteredMods(w)) {
                    int cardX=ix+PAD+col*(colW+PAD);
                    if (mx>=cardX&&mx<cardX+colW&&my>=rowY&&my<rowY+cH){m.toggle();return true;}
                    col++; if (col>=cols){col=0;rowY+=cH+w.cardGap;}
                }
                if (hasSettings) {
                    int setX=ix+modW+1,setW=iw-modW-1;
                    handleSettingsClick(sel,setX+PAD,setW-PAD*2,iy+PAD,mx,my);
                }
            }
        }

        if (e.button()==1) {
            for (Widget w:widgets) {
                if (w.type!=WidgetType.MODULE_LIST||w.collapsed) continue;
                int ix=(int)w.x,iy=(int)w.y,iw=(int)w.w,ih=(int)w.h;
                Module sel=widgetSelected.get(w);
                boolean hasSettings=sel!=null&&w.settingsMode==SettingsMode.INLINE;
                int modW=hasSettings?Math.min(200,(int)(iw*0.45f)):iw-2;
                int autoCols=Math.max(1,modW/COL_MIN_W);
                int cols=w.cols>0?w.cols:autoCols;
                int colW=(modW-PAD*(cols+1))/cols;
                int cH=w.cardH>0?w.cardH:26;
                float scroll=widgetScroll.getOrDefault(w,0f);
                int col=0,rowY=iy+PAD-(int)scroll;
                for (Module m:filteredMods(w)) {
                    int cardX=ix+PAD+col*(colW+PAD);
                    if (mx>=cardX&&mx<cardX+colW&&my>=rowY&&my<rowY+cH) {
                        if (w.settingsMode==SettingsMode.POPUP) {
                            widgetSelected.put(w,widgetSelected.get(w)==m?null:m);
                            popupWidget=widgetSelected.get(w)!=null?w:null;
                            popupScroll=popupScrollT=0;
                        } else { widgetSelected.put(w,widgetSelected.get(w)==m?null:m); }
                        return true;
                    }
                    col++; if (col>=cols){col=0;rowY+=cH+w.cardGap;}
                }
            }
        }

        if (e.button()==2) {
            for (Widget w:widgets) {
                if (w.type!=WidgetType.MODULE_LIST||w.collapsed) continue;
                int ix=(int)w.x,iy=(int)w.y,iw=(int)w.w;
                Module sel=widgetSelected.get(w);
                boolean hasSettings=sel!=null&&w.settingsMode==SettingsMode.INLINE;
                int modW=hasSettings?Math.min(200,(int)(iw*0.45f)):iw-2;
                int autoCols=Math.max(1,modW/COL_MIN_W);
                int cols=w.cols>0?w.cols:autoCols;
                int colW=(modW-PAD*(cols+1))/cols;
                int cH=w.cardH>0?w.cardH:26;
                float scroll=widgetScroll.getOrDefault(w,0f);
                int col=0,rowY=iy+PAD-(int)scroll;
                for (Module m:filteredMods(w)) {
                    int cardX=ix+PAD+col*(colW+PAD);
                    if (mx>=cardX&&mx<cardX+colW&&my>=rowY&&my<rowY+cH){moduleBinding=(moduleBinding==m)?null:m;return true;}
                    col++; if (col>=cols){col=0;rowY+=cH+w.cardGap;}
                }
            }
        }
        return super.mouseClicked(e,b);
    }

    /** Handles a click on a property panel row. */
    private void handlePropClick(PropRow row, int rx, int iy, int rw, int mx, int my) {
        if (row.type==PropRow.Type.TOGGLE) {
            setPropBool(propWidget, row.field, !getPropBool(propWidget, row.field));
        } else if (row.type==PropRow.Type.SLIDER) {
            float t=Math.max(0,Math.min(1,(float)(mx-rx)/rw));
            float val=row.min+t*(row.max-row.min);
            setPropFloat(propWidget, row.field, row.isInt?(int)val:val);
            sliderField=row.field; sliderWidget=propWidget;
            sliderMinV=row.min; sliderMaxV=row.max; sliderX=rx; sliderW=rw;
        } else if (row.type==PropRow.Type.CYCLE) {
            cyclePropString(propWidget, row.field);
        } else if (row.type==PropRow.Type.IMAGE_PICK) {
            imagePickWidget=propWidget; imagePickX=rx; imagePickY=iy; imageScanTime=0;
        } else if (row.type==PropRow.Type.ACTION) {
            switch(row.field) {
                case "delete"     -> { widgets.remove(propWidget); propWidget=null; }
                case "collapse"   -> propWidget.collapsed=!propWidget.collapsed;
                case "editTitle"  -> { editTextWidget=propWidget; editTextIsTitle=true; }
                case "editText"   -> { editTextWidget=propWidget; editTextIsTitle=false; }
            }
        }
    }

    // ── settings click ────────────────────────────────────────────────────────
    private void handleSettingsClick(Module mod, int cx, int cw, int startY, int mx, int my) {
        int y=startY, re=cx+cw-6;
        for (Field field:mod.getClass().getDeclaredFields()) {
            if (!Setting.class.isAssignableFrom(field.getType())) continue;
            field.setAccessible(true);
            try {
                Setting<?> s=(Setting<?>)field.get(mod);
                if (!s.isVisible()){y+=LINE_H+2;continue;}
                if (my>=y&&my<y+LINE_H&&mx>=cx&&mx<cx+cw) {
                    if (s instanceof BooleanSetting bs){bs.toggle();boolFlash.put(s,System.currentTimeMillis());}
                    else if (s instanceof SelectSetting ss){int selW=Math.min(cw/2,90),selX=re-selW,selY=y+(LINE_H-16)/2;if(mx>=selX&&mx<selX+selW&&my>=selY&&my<selY+16)ss.setSelectedIndex((ss.getSelectedIndex()+1)%ss.getOptions().size());}
                    else if (s instanceof MultiSelectSetting ms){int chipH=14,chipPad=4,chipX=re;for(int i=ms.getOptions().size()-1;i>=0;i--){String opt=ms.getOptions().get(i);int chipW=(int)AporiaRenderer.INSTANCE.getTextWidth("regular",opt,8f)+chipPad*2;chipX-=chipW+3;int chipY=y+(LINE_H-chipH)/2;if(mx>=chipX&&mx<chipX+chipW&&my>=chipY&&my<chipY+chipH){ms.toggle(opt);break;}if(chipX<=cx+80)break;}}
                    else if (s instanceof TextSetting ts){int inW=Math.min(cw/2,100),inX=re-inW,inY=y+(LINE_H-14)/2;if(mx>=inX&&mx<inX+inW&&my>=inY&&my<inY+14)focusedText=ts;else focusedText=null;}
                    else if (s instanceof BindSetting){settingBindMod=mod;settingBindFld=field;}
                    else if (s instanceof ButtonSetting btn){int btnW=Math.min(cw/3,80),btnX=re-btnW,btnY=y+(LINE_H-14)/2;if(mx>=btnX&&mx<btnX+btnW&&my>=btnY&&my<btnY+14)btn.click();}
                    return;
                }
                y+=LINE_H+2;
            } catch (IllegalAccessException ignored) {}
        }
    }

    // ── drag / release / scroll ───────────────────────────────────────────────
    @Override
    public boolean mouseDragged(MouseButtonEvent e, double dx, double dy) {
        float mx=(float)e.x(), my=(float)e.y();
        if (sliderField!=null&&sliderWidget!=null) {
            float t=Math.max(0,Math.min(1,(mx-sliderX)/sliderW));
            float val=sliderMinV+t*(sliderMaxV-sliderMinV);
            boolean isInt=(sliderMaxV-sliderMinV)==(int)(sliderMaxV-sliderMinV)&&sliderMinV==(int)sliderMinV;
            setPropFloat(sliderWidget, sliderField, isInt?(int)val:val);
            return true;
        }
        if (boxSelecting) {
            boxX1=mx; boxY1=my;
            float bx=Math.min(boxX0,boxX1),by=Math.min(boxY0,boxY1),bw=Math.abs(boxX1-boxX0),bh=Math.abs(boxY1-boxY0);
            selection.clear();
            for (Widget w:widgets) if (w.x>=bx&&w.y>=by&&w.x+w.w<=bx+bw&&w.y+w.h<=by+bh) selection.add(w);
            return true;
        }
        if (dragWidget!=null) {
            float nx=snap(mx-dragOx), ny=snap(my-dragOy);
            float ddx=nx-dragWidget.x, ddy=ny-dragWidget.y;
            for (Widget w:selection) { w.x+=ddx; w.y+=ddy; }
            dragWidget.x=nx; dragWidget.y=ny;
            return true;
        }
        if (resizeWidget!=null) {
            float ddx=mx-resStartMx, ddy=my-resStartMy;
            if (resR) resizeWidget.w=Math.max(30,snap(resStartW+ddx));
            if (resB) resizeWidget.h=Math.max(10,snap(resStartH+ddy));
            if (resL){float nw=Math.max(30,snap(resStartW-ddx));resizeWidget.x=resStartX+(resStartW-nw);resizeWidget.w=nw;}
            if (resT){float nh=Math.max(10,snap(resStartH-ddy));resizeWidget.y=resStartY+(resStartH-nh);resizeWidget.h=nh;}
            return true;
        }
        return super.mouseDragged(e,dx,dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent e) {
        if (boxSelecting) { boxSelecting=false; }
        if (dragWidget!=null||resizeWidget!=null) saveLayout();
        if (sliderField!=null) { saveLayout(); sliderField=null; sliderWidget=null; }
        dragWidget=null; resizeWidget=null;
        return super.mouseReleased(e);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        if (propWidget!=null) {
            List<PropRow> rows=buildPropRows(propWidget);
            int totalH=rows.size()*PROP_IH+PROP_PAD*2+24;
            int ph=Math.min(totalH,this.height-8);
            if (totalH>ph) { propScrollT-=(float)dy*15; return true; }
        }
        if (popupWidget!=null){popupScrollT-=(float)dy*15;return true;}
        for (Widget w:widgets) {
            if (w.type!=WidgetType.MODULE_LIST||w.collapsed) continue;
            int ix=(int)w.x,iy=(int)w.y,iw=(int)w.w,ih=(int)w.h;
            if (mx>=ix&&mx<ix+iw&&my>=iy&&my<iy+ih){float cur=widgetScrollT.getOrDefault(w,0f);widgetScrollT.put(w,cur-(float)dy*15);return true;}
        }
        return super.mouseScrolled(mx,my,dx,dy);
    }

    // ── keyboard ──────────────────────────────────────────────────────────────
    @Override
    public boolean keyPressed(KeyEvent e) {
        if (moduleBinding!=null) {
            if (e.key()==256){moduleBinding=null;return true;}
            moduleBinding.setKeybind(e.key());
            modKeyFlash.put(moduleBinding,System.currentTimeMillis());
            TypeAnim na=modNameAnim.computeIfAbsent(moduleBinding,k->{TypeAnim a=new TypeAnim(50,90);a.snap(moduleBinding.name());return a;});
            na.setTarget(moduleBinding.name()); moduleBinding=null; return true;
        }
        if (settingBindMod!=null&&settingBindFld!=null) {
            if (e.key()==256){settingBindMod=null;settingBindFld=null;return true;}
            try{((BindSetting)settingBindFld.get(settingBindMod)).setKey(e.key());}catch(IllegalAccessException ignored){}
            settingBindMod=null;settingBindFld=null; return true;
        }
        if (focusedText!=null) {
            if (e.key()==256){focusedText=null;return true;}
            if (e.key()==259){focusedText.backspace();return true;}
            return true;
        }
        if (editTextWidget!=null) {
            if (e.key()==256||e.key()==257){editTextWidget=null;return true;}
            if (e.key()==259) {
                if (editTextIsTitle&&!editTextWidget.titleText.isEmpty())
                    editTextWidget.titleText=editTextWidget.titleText.substring(0,editTextWidget.titleText.length()-1);
                else if (!editTextIsTitle&&!editTextWidget.customText.isEmpty())
                    editTextWidget.customText=editTextWidget.customText.substring(0,editTextWidget.customText.length()-1);
                return true;
            }
            return true;
        }
        if (searchFocused) {
            if (e.key()==259&&!search.isEmpty()){search=search.substring(0,search.length()-1);return true;}
            if (e.key()==256){searchFocused=false;return true;}
        }
        if (e.key()==256) {
            if (imagePickWidget!=null){imagePickWidget=null;return true;}
            if (addMenuWidget!=null){addMenuWidget=null;return true;}
            if (propWidget!=null){propWidget=null;return true;}
            if (popupWidget!=null){popupWidget=null;return true;}
            if (editMode){editMode=false;saveLayout();return true;}
        }
        // edit mode shortcuts
        if (editMode) {
            boolean ctrl=org.lwjgl.glfw.GLFW.glfwGetKey(Minecraft.getInstance().getWindow().handle(),org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_CONTROL)==1;
            if (ctrl) {
                if (e.key()==90){undo();return true;}       // Ctrl+Z
                if (e.key()==89){redo();return true;}       // Ctrl+Y
                if (e.key()==67){copySelected();return true;} // Ctrl+C
                if (e.key()==86){pasteClipboard();return true;} // Ctrl+V
                if (e.key()==68){duplicateSelected();return true;} // Ctrl+D
                if (e.key()==65){selection.clear();selection.addAll(widgets);return true;} // Ctrl+A
                if (e.key()==93) { // Ctrl+] bring to front
                    for (Widget w:selection){widgets.remove(w);widgets.add(w);}
                    saveLayout(); return true;
                }
                if (e.key()==91) { // Ctrl+[ send to back
                    List<Widget> sel=new ArrayList<>(selection);
                    for (int i=sel.size()-1;i>=0;i--){Widget w=sel.get(i);widgets.remove(w);widgets.add(0,w);}
                    saveLayout(); return true;
                }
            }
            if (e.key()==261||e.key()==259) { // Delete/Backspace
                if (!selection.isEmpty()){pushUndo();widgets.removeAll(selection);selection.clear();saveLayout();return true;}
            }
        }
        return super.keyPressed(e);
    }

    @Override
    public boolean charTyped(CharacterEvent e) {
        if (editTextWidget!=null) {
            char c=(char)e.codepoint();
            if (c>=32) {
                if (editTextIsTitle) editTextWidget.titleText+=c;
                else editTextWidget.customText+=c;
            }
            return true;
        }
        if (focusedText!=null){char c=(char)e.codepoint();if(c>=32&&c<127)focusedText.appendChar(c);return true;}
        if (searchFocused){search+=(char)e.codepoint();return true;}
        return super.charTyped(e);
    }

    // ── layout persistence ────────────────────────────────────────────────────
    private void saveLayout() {
        try {
            StringBuilder sb=new StringBuilder("v3\n");
            for (Widget w:widgets) {
                sb.append(w.type.name()).append(",")
                  .append((int)w.x).append(",").append((int)w.y).append(",")
                  .append((int)w.w).append(",").append((int)w.h).append(",")
                  .append(w.blur?1:0).append(",").append(w.border?1:0).append(",")
                  .append(w.bgAlpha).append(",").append(w.borderAlpha).append(",")
                  .append(w.borderRadius).append(",").append(w.settingsMode.name()).append(",")
                  .append(w.category).append(",").append(w.collapsed?1:0).append(",")
                  .append(w.cardRadius).append(",").append(w.cardGap).append(",")
                  .append(w.fontSize).append(",").append(w.showTitle?1:0).append(",")
                  .append(w.titleText.replace(",",";")).append(",")
                  .append(w.imagePath.replace(",",";")).append(",")
                  .append(w.imageFit).append(",")
                  .append(w.bgR).append(",").append(w.bgG).append(",").append(w.bgB).append(",")
                  .append(w.borderR).append(",").append(w.borderG).append(",").append(w.borderB).append(",")
                  .append(w.blurStrength).append(",")
                  .append(w.cardH).append(",").append(w.cols).append(",")
                  .append(w.cardR).append(",").append(w.cardG).append(",").append(w.cardB).append(",")
                  .append(w.cardOnAlpha).append(",")
                  .append(w.shapeR).append(",").append(w.shapeG).append(",").append(w.shapeB).append(",").append(w.shapeAlpha).append(",")
                  .append(w.lineThickness).append(",")
                  .append(w.customText.replace(",",";")).append(",")
                  .append(w.fontName).append(",")
                  .append(w.cardAlpha).append(",")
                  .append(w.cardOnR).append(",").append(w.cardOnG).append(",").append(w.cardOnB).append(",")
                  .append(w.rotation).append(",").append(w.locked?1:0).append(",")
                  .append(w.textVars?1:0).append("\n");
            }
            FilesManager.writeApr(CFG_PATH, sb.toString());
        } catch (IOException ignored) {}
    }

    private void loadLayout() {
        if (!FilesManager.exists(CFG_PATH)) return;
        try {
            String data=FilesManager.readApr(CFG_PATH);
            String[] lines=data.split("\n");
            if (lines.length==0) return;
            boolean v3=lines[0].trim().equals("v3");
            boolean v2=lines[0].trim().equals("v2");
            if (!v3&&!v2) return;
            for (int li=1;li<lines.length;li++) {
                String line=lines[li]; if (line.isBlank()) continue;
                String[] p=line.split(",",-1); if (p.length<5) continue;
                WidgetType wt; try{wt=WidgetType.valueOf(p[0]);}catch(Exception ex){continue;}
                Widget w=new Widget(wt,Float.parseFloat(p[1]),Float.parseFloat(p[2]),Float.parseFloat(p[3]),Float.parseFloat(p[4]));
                if (p.length>5)  w.blur=p[5].equals("1");
                if (p.length>6)  w.border=p[6].equals("1");
                if (p.length>7)  w.bgAlpha=parseInt(p[7],18);
                if (p.length>8)  w.borderAlpha=parseInt(p[8],30);
                if (p.length>9)  w.borderRadius=parseInt(p[9],12);
                if (p.length>10) try{w.settingsMode=SettingsMode.valueOf(p[10]);}catch(Exception ignored){}
                if (p.length>11) w.category=p[11];
                if (p.length>12) w.collapsed=p[12].equals("1");
                if (p.length>13) w.cardRadius=parseInt(p[13],6);
                if (p.length>14) w.cardGap=parseInt(p[14],3);
                if (p.length>15) w.fontSize=parseFloat(p[15],9f);
                if (p.length>16) w.showTitle=p[16].equals("1");
                if (p.length>17) w.titleText=p[17].replace(";",",");
                if (p.length>18) w.imagePath=p[18].replace(";",",");
                if (p.length>19) w.imageFit=p[19];
                if (v3) {
                    if (p.length>20) w.bgR=parseInt(p[20],255);
                    if (p.length>21) w.bgG=parseInt(p[21],255);
                    if (p.length>22) w.bgB=parseInt(p[22],255);
                    if (p.length>23) w.borderR=parseInt(p[23],255);
                    if (p.length>24) w.borderG=parseInt(p[24],255);
                    if (p.length>25) w.borderB=parseInt(p[25],255);
                    if (p.length>26) w.blurStrength=parseFloat(p[26],14f);
                    if (p.length>27) w.cardH=parseInt(p[27],26);
                    if (p.length>28) w.cols=parseInt(p[28],0);
                    if (p.length>29) w.cardR=parseInt(p[29],255);
                    if (p.length>30) w.cardG=parseInt(p[30],255);
                    if (p.length>31) w.cardB=parseInt(p[31],255);
                    if (p.length>32) w.cardOnAlpha=parseInt(p[32],40);
                    if (p.length>33) w.shapeR=parseInt(p[33],255);
                    if (p.length>34) w.shapeG=parseInt(p[34],255);
                    if (p.length>35) w.shapeB=parseInt(p[35],255);
                    if (p.length>36) w.shapeAlpha=parseInt(p[36],180);
                    if (p.length>37) w.lineThickness=parseFloat(p[37],2f);
                    if (p.length>38) w.customText=p[38].replace(";",",");
                    if (p.length>39) w.fontName=p[39];
                    if (p.length>40) w.cardAlpha=parseInt(p[40],12);
                    if (p.length>41) w.cardOnR=parseInt(p[41],255);
                    if (p.length>42) w.cardOnG=parseInt(p[42],255);
                    if (p.length>43) w.cardOnB=parseInt(p[43],255);
                    if (p.length>44) w.rotation=parseFloat(p[44],0f);
                    if (p.length>45) w.locked=p[45].equals("1");
                    if (p.length>46) w.textVars=p[46].equals("1");
                }
                widgets.add(w);
            }
        } catch (Exception ignored) {}
    }

    private static int   parseInt(String s, int def)   { try{return Integer.parseInt(s.trim());}catch(Exception e){return def;} }
    private static float parseFloat(String s, float def){ try{return Float.parseFloat(s.trim());}catch(Exception e){return def;} }

    // ── helpers ───────────────────────────────────────────────────────────────
    private void drawWidgetBg(AporiaRenderer r, Widget w, int x, int y, int width, int height) {
        int bg=ColorUtil.rgba(w.bgR,w.bgG,w.bgB,w.bgAlpha);
        int br=w.borderRadius;
        if (w.gradientDir != GradientDir.NONE) {
            int bg2=ColorUtil.rgba(w.grad2R,w.grad2G,w.grad2B,w.grad2Alpha);
            int gdir = w.gradientDir==GradientDir.HORIZONTAL?0:w.gradientDir==GradientDir.VERTICAL?1:2;
            if (w.blur) r.drawRectBlurred(x,y,width,height,br,bg,w.blurStrength);
            r.drawRectGradient(x,y,width,height,br,bg,bg2,gdir);
        } else {
            if (w.blur) r.drawRectBlurred(x,y,width,height,br,bg,w.blurStrength);
            else r.drawRect(x,y,width,height,br,bg);
        }
        if (w.border) r.drawStroke(x,y,width,height,br,1f,1,0f,ColorUtil.rgba(w.borderR,w.borderG,w.borderB,w.borderAlpha));
    }

    private List<Module> filteredMods(Widget w) {
        List<Module> base;
        if (w.category==null||w.category.isEmpty()) {
            base=new ArrayList<>();
            for (Category c:Category.values()) base.addAll(ModuleManager.INSTANCE.getByCategory(c));
        } else {
            try{base=ModuleManager.INSTANCE.getByCategory(Category.valueOf(w.category));}
            catch(Exception e){base=new ArrayList<>();}
        }
        if (search.isEmpty()) return base;
        List<Module> out=new ArrayList<>(); String q=search.toLowerCase();
        for (Module m:base) if (m.name().toLowerCase().contains(q)) out.add(m);
        return out;
    }

    private int settingsTotalH(Module mod) {
        int count=0;
        for (Field f:mod.getClass().getDeclaredFields()) {
            if (!Setting.class.isAssignableFrom(f.getType())) continue;
            f.setAccessible(true);
            try{if(((Setting<?>)f.get(mod)).isVisible())count++;}catch(IllegalAccessException ignored){}
        }
        return count*(LINE_H+2)+PAD*2;
    }

    private String widgetTitle(Widget w) {
        return switch(w.type) {
            case MODULE_LIST   -> w.category.isEmpty()?"Модули":w.category;
            case SEARCH        -> "Поиск";
            case ACCOUNT       -> "Аккаунт";
            case CLIENT_NAME   -> "Aporia";
            case RECT          -> "Блок";
            case CIRCLE        -> "Круг";
            case LINE          -> "Линия";
            case TRIANGLE      -> "Треугольник";
            case TEXT          -> w.customText.isEmpty()?"Текст":w.customText;
            case IMAGE         -> "Изображение";
            case CATEGORY_LIST -> "Категории";
            case BUTTON        -> w.customText.isEmpty()?"Кнопка":w.customText;
        };
    }

    private static String keyName(int key) {
        String name=org.lwjgl.glfw.GLFW.glfwGetKeyName(key,0);
        if (name!=null&&!name.isEmpty()) return name.toUpperCase();
        return switch(key) {
            case 256->"ESC"; case 257->"ENTER"; case 258->"TAB"; case 259->"BKSP";
            case 262->"→"; case 263->"←"; case 264->"↓"; case 265->"↑";
            case 290->"F1"; case 291->"F2"; case 292->"F3"; case 293->"F4";
            case 294->"F5"; case 295->"F6"; case 296->"F7"; case 297->"F8";
            case 298->"F9"; case 299->"F10"; case 300->"F11"; case 301->"F12";
            case 340->"LSHIFT"; case 344->"RSHIFT"; case 341->"LCTRL"; case 345->"RCTRL";
            default->"KEY"+key;
        };
    }

    @Override public boolean isPauseScreen()     { return false; }
    @Override public boolean isAllowedInPortal() { return true; }
    @Override public void renderBackground(GuiGraphics gfx, int mx, int my, float d) {}
}
