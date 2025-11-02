package seq.sequencermod.client.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import seq.sequencermod.net.SequencerNetworking;
import seq.sequencermod.net.SequencerNetworkingClient;

import java.util.List;

public class SequencerScreen extends Screen {
    private int selectedIndex = -1;

    public SequencerScreen(Text title) {
        super(title);
    }

    @Override
    protected void init() {
        int w = 220;
        int h = 20;
        int x = (width - w) / 2;
        int y = (height / 2) - 60;

        addDrawableChild(ButtonWidget.builder(Text.literal("Play"), b -> {
            if (selectedIndex >= 0 && selectedIndex < SequencerNetworkingClient.CLIENT_SEQUENCES.size()) {
                String id = SequencerNetworkingClient.CLIENT_SEQUENCES.get(selectedIndex);
                play(id);
            }
        }).dimensions(x, y + 80, w, h).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Stop"), b -> stop()).dimensions(x, y + 105, w, h).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Close"), b -> close()).dimensions(x, y + 130, w, h).build());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int w = 220;
        int lineH = 14;
        int x = (width - w) / 2;
        int startY = (height / 2) - 60;

        List<String> list = SequencerNetworkingClient.CLIENT_SEQUENCES;
        for (int i = 0; i < list.size(); i++) {
            int y = startY + i * lineH;
            if (mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + lineH) {
                selectedIndex = i;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        renderBackground(ctx);
        int w = 220;
        int x = (width - w) / 2;
        int y = (height / 2) - 60;
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Sequencer (click to select)"), width / 2, y - 18, 0xFFFFFF);

        int lineH = 14;
        List<String> list = SequencerNetworkingClient.CLIENT_SEQUENCES;
        for (int i = 0; i < list.size(); i++) {
            int yy = y + i * lineH;
            int color = (i == selectedIndex) ? 0xFFFFAA : 0xFFFFFF;
            ctx.drawTextWithShadow(textRenderer, list.get(i), x + 4, yy, color);
        }

        super.render(ctx, mouseX, mouseY, delta);
    }

    private static void play(String id) {
        var buf = net.fabricmc.fabric.api.networking.v1.PacketByteBufs.create();
        buf.writeString(id);
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(SequencerNetworking.C2S_PLAY, buf);
    }

    private static void stop() {
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(
                SequencerNetworking.C2S_STOP,
                net.fabricmc.fabric.api.networking.v1.PacketByteBufs.empty()
        );
    }
}