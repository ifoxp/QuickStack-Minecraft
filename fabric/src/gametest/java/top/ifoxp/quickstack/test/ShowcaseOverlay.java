package top.ifoxp.quickstack.test;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

/**
 * Draws a mouse pointer and a key badge into the game picture while the showcase is recorded: the real OS cursor is
 * never moved by the test input, and screen recordings would not show which key was pressed. The pointer glides
 * between tick positions using the frame's partial tick, so 30 fps recordings look smooth.
 */
final class ShowcaseOverlay {

    static volatile boolean active;
    static volatile boolean cursorVisible;
    static volatile String badge = "";
    private static volatile double prevX;
    private static volatile double prevY;
    private static volatile double curX;
    private static volatile double curY;
    private static boolean registered;

    /** 12x19 arrow: 'k' outline, 'w' fill. */
    private static final String[] ARROW = {
            "k...........",
            "kk..........",
            "kwk.........",
            "kwwk........",
            "kwwwk.......",
            "kwwwwk......",
            "kwwwwwk.....",
            "kwwwwwwk....",
            "kwwwwwwwk...",
            "kwwwwwwwwk..",
            "kwwwwwwwwwk.",
            "kwwwwwwkkkkk",
            "kwwwkwwk....",
            "kwwk.kwwk...",
            "kwk..kwwk...",
            "kk....kwwk..",
            "k.....kwwk..",
            ".......kwwk.",
            ".......kkk..",
    };

    private ShowcaseOverlay() {
    }

    static void register() {
        if (registered) {
            return;
        }
        registered = true;
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("quickstack-test", "showcase"), (graphics, delta) -> {
            if (Minecraft.getInstance().gui.screen() == null) {
                draw(graphics, delta.getGameTimeDeltaPartialTick(false));
            }
        });
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) ->
                ScreenEvents.afterExtract(screen).register((s, graphics, mouseX, mouseY, tickProgress) -> draw(graphics, tickProgress)));
    }

    /** Called once per tick by the recorder with the cursor's new GUI position. */
    static void cursorTo(double x, double y, boolean jump) {
        prevX = jump ? x : curX;
        prevY = jump ? y : curY;
        curX = x;
        curY = y;
        cursorVisible = true;
    }

    /** Called once per tick when the cursor did not move, so the glide settles. */
    static void settle() {
        prevX = curX;
        prevY = curY;
    }

    private static void draw(GuiGraphicsExtractor graphics, float partial) {
        if (!active) {
            return;
        }
        graphics.nextStratum();
        if (cursorVisible) {
            double x = prevX + (curX - prevX) * partial;
            double y = prevY + (curY - prevY) * partial;
            graphics.pose().pushMatrix();
            graphics.pose().translate((float) x, (float) y);
            graphics.pose().scale(0.6F, 0.6F);
            for (int row = 0; row < ARROW.length; row++) {
                String line = ARROW[row];
                for (int col = 0; col < line.length(); col++) {
                    char c = line.charAt(col);
                    if (c != '.') {
                        graphics.fill(col, row, col + 1, row + 1, c == 'k' ? 0xFF000000 : 0xFFFFFFFF);
                    }
                }
            }
            graphics.pose().popMatrix();
        }
        String text = badge;
        if (!text.isEmpty()) {
            Font font = Minecraft.getInstance().font;
            float scale = 1.5F;
            int w = font.width(text) + 12;
            graphics.pose().pushMatrix();
            // bottom centre: inside every crop of the clips
            graphics.pose().translate(graphics.guiWidth() / 2.0F - w * scale / 2, graphics.guiHeight() - 6 - 18 * scale);
            graphics.pose().scale(scale, scale);
            graphics.fill(0, 0, w, 18, 0xE0141418);
            graphics.outline(0, 0, w, 18, 0xFFFFD65A);
            graphics.text(font, text, 6, 5, 0xFFFFFFFF, true);
            graphics.pose().popMatrix();
        }
    }
}
