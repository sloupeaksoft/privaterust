package com.rustprivate;

import net.minecraft.client.Minecraft;

public class ClientProxy extends CommonProxy {
    @Override
    public void openCupboard(final MsgOpen message) {
        Minecraft.getMinecraft().addScheduledTask(new Runnable() {
            @Override
            public void run() {
                Minecraft.getMinecraft().displayGuiScreen(new GuiCupboard(message));
            }
        });
    }
}
