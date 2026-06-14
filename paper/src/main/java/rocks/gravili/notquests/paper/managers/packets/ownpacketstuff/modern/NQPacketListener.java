/*
 * NotQuests - A Questing plugin for Minecraft Servers
 * Copyright (C) 2022 Alessio Gravili
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package rocks.gravili.notquests.paper.managers.packets.ownpacketstuff.modern;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.papermc.paper.adventure.PaperAdventure;
import net.kyori.adventure.text.Component;
import net.minecraft.network.protocol.game.ClientboundPlayerChatPacket;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import org.bukkit.entity.Player;
import rocks.gravili.notquests.paper.NotQuests;

public class NQPacketListener extends ChannelDuplexHandler {
    private final NotQuests main;
    private final Player player;

    public NQPacketListener(NotQuests main, final Player player) {
        this.main = main;
        this.player = player;
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception { //TODO: 1.19 check
        super.write(ctx, msg, promise);
        final PacketInjector packetInjector = main.getPacketManager().getModernPacketInjector();
        if (packetInjector == null || !packetInjector.isPacketStuffEnabled()) {
            return;
        }
        if (msg instanceof ClientboundSystemChatPacket clientboundSystemChatPacket) {

            /*CraftPlayer craftPlayer = (CraftPlayer)player;
            RegistryAccess registryAccess = craftPlayer.getHandle().getLevel().registryAccess();
            Registry<ChatType> chatTypeRegistry = registryAccess.registryOrThrow(Registry.CHAT_TYPE_REGISTRY);
            ChatType chatType = clientboundSystemChatPacket.resolveType(chatTypeRegistry);*/

            //main.getLogManager().severe("EE" + clientboundSystemChatPacket.typeId());

            //Seems like action bars arent shown here anyways??

            try {

                /*if (clientboundSystemChatPacket.resolveType(chatTypeRegistry) == ChatType.GAME_INFO) { //Skip actionbar messages
                    return;
                }
                main.getLogManager().debug("Valid chat packet! Type: " + clientboundSystemChatPacket.getType().toString());*/

                handleMainChatHistorySavingLogic(clientboundSystemChatPacket, player);
            } catch (Exception e) {
                if (main.getConfiguration().debug) {
                    e.printStackTrace();
                }
                //main.getLogManager().warn("Disabling packet stuff because there was an error reading chat messages...");
                //main.getPacketManager().getPacketInjector().setPacketStuffEnabled(false);
            }

        }else if(msg instanceof ClientboundPlayerChatPacket clientboundPlayerChatPacket){ //For chat messages sent by players
        }
        else if (msg instanceof ClientboundSectionBlocksUpdatePacket) {
            //player.sendMessage("ClientboundSectionBlocksUpdatePacket");

        }
    }

    public void handleMainChatHistorySavingLogic(final ClientboundSystemChatPacket clientboundSystemChatPacket, final Player player) {
        try {
            if(clientboundSystemChatPacket.overlay()){ //This seems to block out actionbar messages. Not quite sure what else it does, though
                return;
            }

            final Component adventureComponent = toAdventure(clientboundSystemChatPacket);
            if (adventureComponent == null) {
                return;
            }

            //main.getLogManager().info("cspacket overlay: " + clientboundSystemChatPacket.overlay() + " content: " + PlainTextComponentSerializer.plainText().serialize(adventureComponent).replace("§", "").replace("&", "") );

            main.getConversationManager().rememberNonConversationChatMessage(player.getUniqueId(), adventureComponent);


        } catch (Throwable e) {
            if (main.getConfiguration().debug) {
                main.getLogManager().warn("Exception reading chat packet: ");
                e.printStackTrace();
            }

        }
        //if (component != null) {
        //main.getLogManager().log(Level.INFO, "E " + LegacyComponentSerializer.legacyAmpersand().serialize(component));
        //}


    }

    static Component toAdventure(final ClientboundSystemChatPacket clientboundSystemChatPacket) {
        if (clientboundSystemChatPacket.content() == null) {
            return null;
        }
        return PaperAdventure.asAdventure(clientboundSystemChatPacket.content());
    }



    /*@Override
    public void channelRead(ChannelHandlerContext c, Object m) throws Exception {
        main.getLogManager().debug("Reading " + m.getClass().getSimpleName());
        if (m.getClass().getSimpleName().equalsIgnoreCase("PacketPlayInResourcePackStatus")) {
            String s = Reflection.getFieldValueOfObject(m, "b").toString();
            if (s.equals("DECLINED")) {
            }
            if (s.equals("FAILED_DOWNLOAD")) {
            }
            if (s.equals("ACCEPTED")) {
            }
            if (s.equals("SUCCESSFULLY_LOADED")) {
                this.player.sendMessage("You have our texture pack installed");
                return;
            }
        } else {
            super.channelRead(c, m);
        }
    }*/
}
