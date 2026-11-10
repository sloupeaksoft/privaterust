package com.rustprivate;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Client -> server: a button in the cupboard menu was pressed. */
public class MsgAction implements IMessage {
    public static final int AUTHORIZE = 0;
    public static final int DEAUTHORIZE = 1;
    public static final int CLEAR = 2;
    public static final int STORAGE = 3;

    public long pos;
    public int action;
    public String target = "";

    public MsgAction() {
    }

    public MsgAction(long pos, int action, String target) {
        this.pos = pos;
        this.action = action;
        this.target = target == null ? "" : target;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        pos = buf.readLong();
        action = buf.readInt();
        target = ByteBufUtils.readUTF8String(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(pos);
        buf.writeInt(action);
        ByteBufUtils.writeUTF8String(buf, target);
    }

    public static class Handler implements IMessageHandler<MsgAction, IMessage> {
        @Override
        public IMessage onMessage(final MsgAction message, MessageContext ctx) {
            final EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(new Runnable() {
                @Override
                public void run() {
                    PrivateHandler.handleAction(player, message);
                }
            });
            return null;
        }
    }
}
