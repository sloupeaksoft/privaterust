package com.rustprivate;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.util.ArrayList;
import java.util.List;

/** Server -> client: open / refresh the cupboard menu. */
public class MsgOpen implements IMessage {
    public long pos;
    public boolean authorized;
    public int radius;
    public List<String> names = new ArrayList<String>();
    public List<String> uuids = new ArrayList<String>();

    public MsgOpen() {
    }

    public MsgOpen(long pos, boolean authorized, int radius, List<String> names, List<String> uuids) {
        this.pos = pos;
        this.authorized = authorized;
        this.radius = radius;
        this.names = names;
        this.uuids = uuids;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        pos = buf.readLong();
        authorized = buf.readBoolean();
        radius = buf.readInt();
        int n = buf.readInt();
        for (int i = 0; i < n; i++) {
            uuids.add(ByteBufUtils.readUTF8String(buf));
            names.add(ByteBufUtils.readUTF8String(buf));
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(pos);
        buf.writeBoolean(authorized);
        buf.writeInt(radius);
        buf.writeInt(names.size());
        for (int i = 0; i < names.size(); i++) {
            ByteBufUtils.writeUTF8String(buf, uuids.get(i));
            ByteBufUtils.writeUTF8String(buf, names.get(i));
        }
    }

    public static class Handler implements IMessageHandler<MsgOpen, IMessage> {
        @Override
        public IMessage onMessage(MsgOpen message, MessageContext ctx) {
            RustPrivate.proxy.openCupboard(message);
            return null;
        }
    }
}
