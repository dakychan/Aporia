package zenith.zov.utility.game.other;

import lombok.experimental.UtilityClass;
import net.minecraft.text.MutableText;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import zenith.zov.utility.interfaces.IMinecraft;

import java.util.Locale;

@UtilityClass
public class TextUtil implements IMinecraft {


    public String formatNumber(double number) {
        return String.format(Locale.US, "%.1f", number);
    }
    public Text truncateAfterSecondSpace(Text input, boolean addEllipsis) {
        OrderedText ordered = input.asOrderedText();
        MutableText out = Text.empty();

        StringBuilder run = new StringBuilder();
        final Style[] current = {Style.EMPTY};

        final int[] spaceCount = {0};
        final boolean[] truncated = {false};

        ordered.accept((index, style, codePoint) -> {
            if (spaceCount[0] > 2) {
                truncated[0] = true;
                return false;
            }


            if (!style.equals(current[0])) {
                if (!run.isEmpty()) {
                    out.append(Text.literal(run.toString()).setStyle(current[0]));
                    run.setLength(0);
                }
                current[0] = style;
            }


            run.appendCodePoint(codePoint);
            if (Character.isWhitespace(codePoint)) {
                spaceCount[0]++;
            }

            return true;
        });


        if (run.length() > 0) {
            out.append(Text.literal(run.toString()).setStyle(current[0]));
        }


        if (addEllipsis && truncated[0]) {
            Style tailStyle = current[0] != null ? current[0] : input.getStyle();
            out.append(Text.literal("…").setStyle(tailStyle));
        }

        return out;
    }
    public static MutableText replaceLastChar(Text source, String replacement) {
        OrderedText ordered = source.asOrderedText();

        
        final int[] length = {0};
        ordered.accept((index, style, codePoint) -> { length[0]++; return true; });
        if (length[0] == 0) {
            
            return source.copy();
        }
        final int lastIdx = length[0] - 1;

        
        MutableText out = Text.empty();

        StringBuilder run = new StringBuilder();
        Style[] currentStyle = {null};

        final int[] pos = {0};
        ordered.accept((index, style, codePoint) -> {
            boolean isLast = (pos[0] == lastIdx);

            
            if (currentStyle[0] == null || !currentStyle[0].equals(style)) {
                flushRun(out, run, currentStyle[0]);
                currentStyle[0] = style;
            }

            if (isLast&&false) {
                
                flushRun(out, run, currentStyle[0]);


                out.append(Text.literal(replacement).setStyle(style));


            } else {
                
                run.appendCodePoint(codePoint);
            }

            pos[0]++;
            return true;
        });

        
        flushRun(out, run, currentStyle[0]);
        return out;
    }

    private static void flushRun(MutableText out, StringBuilder run, Style style) {
        if (run.length() == 0) return;
        MutableText chunk = Text.literal(run.toString());
        if (style != null) chunk.setStyle(style);
        out.append(chunk);
        run.setLength(0);
    }
    public static Text truncateAfterSubstring(Text input, String argStr, boolean addEllipsis) {
        if (argStr == null || argStr.isEmpty()) return input;

        OrderedText ordered = input.asOrderedText();
        MutableText out = Text.empty();

        StringBuilder buffer = new StringBuilder();
        final Style[] currentStyle = {Style.EMPTY};

        StringBuilder collected = new StringBuilder();
        final boolean[] truncated = {false};

        ordered.accept((index, style, codePoint) -> {
            String cpStr = new String(Character.toChars(codePoint));
            if (collected.toString().contains(argStr)) {
                truncated[0] = true;
                return false;
            }

            collected.append(cpStr);



            if (!style.equals(currentStyle[0])) {
                if (buffer.length() > 0) {
                    out.append(Text.literal(buffer.toString()).setStyle(currentStyle[0]));
                    buffer.setLength(0);
                }
                currentStyle[0] = style;
            }

            buffer.append(cpStr);
            return true;
        });

        if (buffer.length() > 0) {
            out.append(Text.literal(buffer.toString()).setStyle(currentStyle[0]));
        }

        if (addEllipsis && truncated[0]) {
            Style tailStyle = currentStyle[0] != null ? currentStyle[0] : input.getStyle();
            out.append(Text.literal("…").setStyle(tailStyle));
        }

        return out;
    }
}
