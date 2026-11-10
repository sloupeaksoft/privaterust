package com.rustprivate;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;

import java.io.IOException;

/** Tool cupboard menu: list of authorized players with deauthorize buttons. */
public class GuiCupboard extends GuiScreen {
    private static final int PER_PAGE = 7;
    private final MsgOpen data;
    private int page = 0;
    private int left, top;
    private static final int W = 230, H = 220;

    public GuiCupboard(MsgOpen data) {
        this.data = data;
    }

    private boolean me(int i) {
        return Minecraft.getMinecraft().player != null
                && Minecraft.getMinecraft().player.getUniqueID().toString().equals(data.uuids.get(i));
    }

    private boolean amAuthorized() {
        if (Minecraft.getMinecraft().player == null) return false;
        return data.uuids.contains(Minecraft.getMinecraft().player.getUniqueID().toString());
    }

    @Override
    public void initGui() {
        buttonList.clear();
        left = (width - W) / 2;
        top = (height - H) / 2;
        int pages = Math.max(1, (data.names.size() + PER_PAGE - 1) / PER_PAGE);
        if (page >= pages) page = pages - 1;
        for (int row = 0; row < PER_PAGE; row++) {
            int i = page * PER_PAGE + row;
            if (i >= data.names.size()) break;
            GuiButton b = new GuiButton(100 + i, left + W - 34, top + 44 + row * 18, 24, 16, "\u00a7cX");
            b.enabled = data.authorized;
            buttonList.add(b);
        }
        GuiButton prev = new GuiButton(5, left + 10, top + 172, 20, 16, "<");
        GuiButton next = new GuiButton(6, left + W - 30, top + 172, 20, 16, ">");
        prev.enabled = page > 0;
        next.enabled = page < pages - 1;
        buttonList.add(prev);
        buttonList.add(next);

        int bw = (W - 30) / 2;
        if (amAuthorized()) {
            buttonList.add(new GuiButton(2, left + 10, top + 194, bw, 20, "\u0412\u044b\u043f\u0438\u0441\u0430\u0442\u044c\u0441\u044f"));
        } else {
            buttonList.add(new GuiButton(1, left + 10, top + 194, bw, 20, "\u00a7a\u0410\u0432\u0442\u043e\u0440\u0438\u0437\u043e\u0432\u0430\u0442\u044c\u0441\u044f"));
        }
        GuiButton storage = new GuiButton(4, left + 20 + bw, top + 194, bw, 20, "\u0425\u0440\u0430\u043d\u0438\u043b\u0438\u0449\u0435");
        storage.enabled = data.authorized;
        buttonList.add(storage);
        GuiButton clear = new GuiButton(3, left + 40, top + 172, W - 80, 16, "\u041e\u0447\u0438\u0441\u0442\u0438\u0442\u044c \u0441\u043f\u0438\u0441\u043e\u043a");
        clear.enabled = data.authorized && !data.names.isEmpty();
        buttonList.add(clear);
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        int id = button.id;
        if (id >= 100) {
            int i = id - 100;
            if (i < data.uuids.size()) {
                RustPrivate.NET.sendToServer(new MsgAction(data.pos, MsgAction.DEAUTHORIZE, data.uuids.get(i)));
            }
            return;
        }
        switch (id) {
            case 1:
                RustPrivate.NET.sendToServer(new MsgAction(data.pos, MsgAction.AUTHORIZE, ""));
                break;
            case 2:
                if (Minecraft.getMinecraft().player != null) {
                    RustPrivate.NET.sendToServer(new MsgAction(data.pos, MsgAction.DEAUTHORIZE,
                            Minecraft.getMinecraft().player.getUniqueID().toString()));
                }
                break;
            case 3:
                RustPrivate.NET.sendToServer(new MsgAction(data.pos, MsgAction.CLEAR, ""));
                break;
            case 4:
                RustPrivate.NET.sendToServer(new MsgAction(data.pos, MsgAction.STORAGE, ""));
                break;
            case 5:
                page--;
                initGui();
                break;
            case 6:
                page++;
                initGui();
                break;
            default:
                break;
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawRect(left, top, left + W, top + H, 0xE0201A14);
        drawRect(left, top, left + W, top + 1, 0xFFB5651D);
        drawRect(left, top + H - 1, left + W, top + H, 0xFFB5651D);
        drawCenteredString(fontRenderer, "\u00a76\u00a7l\u0428\u041a\u0410\u0424 (TOOL CUPBOARD)", width / 2, top + 8, 0xFFFFFF);
        drawCenteredString(fontRenderer, "\u00a77\u041f\u0440\u0438\u0432\u0430\u0442: " + data.radius + " \u0431\u043b\u043e\u043a\u043e\u0432 \u0432\u043e \u0432\u0441\u0435 \u0441\u0442\u043e\u0440\u043e\u043d\u044b", width / 2, top + 19, 0xFFFFFF);
        drawString(fontRenderer, "\u00a7f\u0410\u0432\u0442\u043e\u0440\u0438\u0437\u043e\u0432\u0430\u043d\u044b (" + data.names.size() + "):", left + 10, top + 32, 0xFFFFFF);
        if (data.names.isEmpty()) {
            drawString(fontRenderer, "\u00a78\u043d\u0438\u043a\u043e\u0433\u043e", left + 14, top + 48, 0xFFFFFF);
        }
        for (int row = 0; row < PER_PAGE; row++) {
            int i = page * PER_PAGE + row;
            if (i >= data.names.size()) break;
            int y = top + 44 + row * 18;
            drawRect(left + 8, y, left + W - 38, y + 16, 0x60000000);
            String n = (me(i) ? "\u00a7a" : "\u00a7f") + data.names.get(i) + (me(i) ? " \u00a77(\u0442\u044b)" : "");
            drawString(fontRenderer, n, left + 14, y + 4, 0xFFFFFF);
        }
        if (!data.authorized) {
            drawCenteredString(fontRenderer, "\u00a7c\u0422\u044b \u043d\u0435 \u0430\u0432\u0442\u043e\u0440\u0438\u0437\u043e\u0432\u0430\u043d", width / 2, top + 162, 0xFFFFFF);
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
