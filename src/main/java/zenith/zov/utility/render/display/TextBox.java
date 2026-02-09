package zenith.zov.utility.render.display;

import lombok.Data;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.Vector2f;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;
import zenith.zov.base.animations.base.Animation;
import zenith.zov.base.animations.base.Easing;
import zenith.zov.base.font.Font;
import zenith.zov.utility.game.other.MouseButton;
import zenith.zov.utility.interfaces.IMinecraft;
import zenith.zov.utility.math.MathUtil;
import zenith.zov.utility.render.display.base.CustomDrawContext;
import zenith.zov.utility.render.display.base.color.ColorRGBA;

@Data
public class TextBox implements IMinecraft {

    private String text = "";
    private boolean selected;
    private boolean selectAll;

    private int cursor;
    private float posX;
    private Font font;
    private Vector2f position;
    private String emptyText;
    private float width;

    private long lastInputTime = System.currentTimeMillis();
    private int maxLength = Integer.MAX_VALUE;
    private CharFilter charFilter = CharFilter.ANY;


    private float scrollOffset = 0;

    public TextBox(Vector2f position, final Font font, final String emptyText, final float width) {
        this.font = font;
        this.emptyText = emptyText;
        this.width = width;
        this.position = position;
    }

    public void render(CustomDrawContext context, float x, float y, final ColorRGBA colorText, final ColorRGBA colorEmpty) {
        this.position = new Vector2f(x, y);
        this.cursor = MathHelper.clamp(this.cursor, 0, this.text.length());
        this.posX = x;

        boolean isEmpty = this.isEmpty();



        float cursorX = 0f;
        if (!isEmpty) {
            String textBeforeCursor = text.substring(0, cursor);
            cursorX = font.width(textBeforeCursor);
        }



        float availableWidth = width;
        int startIndex = 0;


        while (font.width(text.substring(startIndex, cursor)) > availableWidth) {
            startIndex++;
        }


        int endIndex = cursor;
        while (endIndex < text.length() && font.width(text.substring(startIndex, endIndex)) < availableWidth) {
            endIndex++;
        }


        String visibleText = text.substring(startIndex, endIndex);


        if (isEmpty) {
            context.drawText(font, emptyText, x, y, colorEmpty);
        } else {
            context.drawText(font, visibleText, x, y, colorText);


//            if (selected && System.currentTimeMillis() - lastInputTime > 200) {
//                float cursorDrawX = x + font.width(text.substring(startIndex, cursor));
//                context.drawRect(cursorDrawX, y - 1, 1, font.height() + 2,
//                        colorText.mulAlpha(animation.update(animation.getValue()==0.2f?1:animation.getValue()==1?0.2f:animation.getTargetValue())));
//            }
        }



        if (selected && System.currentTimeMillis() - lastInputTime > 200) {
            float cursorDrawX = posX + cursorX - scrollOffset;
            animation.setDuration(250);

            context.drawRect(cursorDrawX, y-1, 1, font.height()+2, colorText.mulAlpha(animation.update(animation.getValue()==0.2f?1:animation.getValue()==1?0.2f:animation.getTargetValue())));
        }
        if(selectAll){
            context.drawRect(x-1, y-1, font.width(visibleText)+2, font.height()+2, colorEmpty.mulAlpha(0.5f));

        }
    }
    Animation animation = new Animation(400,0.2f, Easing.QUAD_IN_OUT);
    public void onMouseClicked(double mouseX, double mouseY, MouseButton button) {
        Vector2f pos = getPosition();
        this.selected = button.getButtonIndex() == 0 && MathUtil.isHovered(mouseX, mouseY, pos.getX(), pos.getY()-1, width, font.height()+2);
        if (selected) {
            selectAll = false;
        }
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!this.selected) return false;

        lastInputTime = System.currentTimeMillis();
        cursor = MathHelper.clamp(cursor, 0, this.text.length());

        if (InputUtil.isKeyPressed(mc.getWindow().getHandle(), GLFW.GLFW_KEY_LEFT_CONTROL)) {
            if (keyCode == GLFW.GLFW_KEY_V) {
                final String clipboard = mc.keyboard.getClipboard();
                if (selectAll) {
                    this.text = "";
                    cursor = 0;
                    selectAll = false;
                }
                this.addText(clipboard, cursor);
                cursor += clipboard.length();
                selectAll = false;
            } else if (keyCode == GLFW.GLFW_KEY_A) {
                selectAll = true;
                cursor = text.length();
            }else if (keyCode == GLFW.GLFW_KEY_C) {
                if (selected && selectAll) {
                    mc.keyboard.setClipboard(this.text);
                }
            }
        } else if (keyCode == GLFW.GLFW_KEY_DELETE && !this.text.isEmpty()) {
            this.removeText(cursor + 1);
            selectAll = false;
        } else if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !this.text.isEmpty()) {
            if (selectAll) {
                this.text = "";
                cursor = 0;
                selectAll = false;
            } else {
                this.removeText(cursor);
                cursor--;
                if (InputUtil.isKeyPressed(mc.getWindow().getHandle(), GLFW.GLFW_KEY_LEFT_CONTROL)) {
                    while (!this.text.isEmpty() && cursor > 0) {
                        this.removeText(cursor);
                        cursor--;
                    }
                }
            }
        } else if (keyCode == GLFW.GLFW_KEY_RIGHT) {
            cursor++;
            if (InputUtil.isKeyPressed(mc.getWindow().getHandle(), GLFW.GLFW_KEY_LEFT_CONTROL)) {
                cursor = text.length();
            }
            selectAll = false;
        } else if (keyCode == GLFW.GLFW_KEY_LEFT) {
            cursor--;
            if (InputUtil.isKeyPressed(mc.getWindow().getHandle(), GLFW.GLFW_KEY_LEFT_CONTROL)) {
                cursor = 0;
            }
            selectAll = false;
        } else if (keyCode == GLFW.GLFW_KEY_END) {
            cursor = text.length();
            selectAll = false;
        } else if (keyCode == GLFW.GLFW_KEY_HOME) {
            cursor = 0;
            selectAll = false;
        }

        cursor = MathHelper.clamp(cursor, 0, this.text.length());
        return true;
    }

    public boolean charTyped(char codePoint, int modifiers) {
        if (!this.selected) return false;

        lastInputTime = System.currentTimeMillis();
        cursor = MathHelper.clamp(cursor, 0, this.text.length());

        if (selectAll) {
            this.text = "";
            cursor = 0;
            selectAll = false;
        }

        this.addText(Character.toString(codePoint), cursor);
        cursor++;
        cursor = MathHelper.clamp(cursor, 0, this.text.length());
        return true;
    }

    private void addText(final String newText, final int position) {
        StringBuilder filteredText = new StringBuilder();
        for (char c : newText.toCharArray()) {
            if (charFilter.isAllowed(c)) {
                filteredText.append(c);
            }
        }

        String filtered = filteredText.toString();
        if (this.text.length() + filtered.length() > maxLength) {
            int available = maxLength - this.text.length();
            if (available <= 0) return;
            filtered = filtered.substring(0, Math.min(available, filtered.length()));
        }

        final StringBuilder newFinalText = new StringBuilder();
        boolean inserted = false;

        for (int i = 0; i < this.text.length(); i++) {
            if (i == position) {
                inserted = true;
                newFinalText.append(filtered);
            }
            newFinalText.append(this.text.charAt(i));
        }

        if (!inserted) newFinalText.append(filtered);
        this.text = newFinalText.toString();
    }


    private void removeText(final int position) {
        final StringBuilder newText = new StringBuilder();
        for (int i = 0; i < this.text.length(); ++i) {
            if (i != position - 1) {
                newText.append(this.text.charAt(i));
            }
        }
        this.text = newText.toString();
    }

    public boolean isEmpty() {
        return this.text.isEmpty();
    }
    public enum CharFilter {
        ANY,
        ENGLISH,
        ENGLISH_NUMBERS,
        CYRILLIC,
        NUMBERS_ONLY;

        public boolean isAllowed(char c) {
            return switch (this) {
                case ANY -> true;
                case ENGLISH -> Character.isLetter(c) && c <= 127 && Character.isAlphabetic(c);
                case ENGLISH_NUMBERS -> Character.isLetterOrDigit(c) && c <= 127;
                case CYRILLIC -> String.valueOf(c).matches("[А-Яа-яЁё]");
                case NUMBERS_ONLY -> Character.isDigit(c);
            };
        }
    }


}
