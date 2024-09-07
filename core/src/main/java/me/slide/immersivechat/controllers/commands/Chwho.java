package me.slide.immersivechat.controllers.commands;

import com.google.inject.Inject;
import com.massivecraft.factions.entity.MPlayer;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Resident;
import me.slide.immersivechat.controllers.PluginMessageController;
import me.slide.immersivechat.initiators.application.ImmersiveChat;
import me.slide.immersivechat.localization.LocalizedMessage;
import me.slide.immersivechat.model.ChatChannel;
import me.slide.immersivechat.model.IImmersiveChatPlayer;
import me.slide.immersivechat.model.UniversalCommand;
import me.slide.immersivechat.service.ConfigService;
import me.slide.immersivechat.service.PlayerApiService;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;

import static me.slide.immersivechat.utilities.FormatUtils.LINE_LENGTH;

public class Chwho extends UniversalCommand {
	@Inject
    private ImmersiveChat plugin;
	@Inject
	private PluginMessageController pluginMessageController;
	@Inject
	private PlayerApiService playerApiService;
	@Inject
	private ConfigService configService;
	
	@Inject
	public Chwho(String name) {
		super(name);
	}

    @Override
    public void executeCommand(CommandSender sender, String command, String[] args) {
        String playerlist = "";
        if (sender.hasPermission("venturechat.chwho")) {
            if (args.length > 0) {
                ChatChannel channel = configService.getChannel(args[0]);
                if (channel != null) {
                    if (channel.isPermissionRequired()) {
                        if (!sender.hasPermission(channel.getPermission())) {
                            IImmersiveChatPlayer mcp = playerApiService.getOnlineImmersiveChatPlayer(((Player) sender));
                            mcp.getListening().remove(channel.getName());
                            mcp.getPlayer().sendMessage(LocalizedMessage.CHANNEL_NO_PERMISSION_VIEW.toString());
                            return;
                        }
                    }

                    if (channel.isBungeeEnabled() && sender instanceof Player) {
                        IImmersiveChatPlayer mcp = playerApiService.getOnlineImmersiveChatPlayer((Player) sender);
                        ByteArrayOutputStream byteOutStream = new ByteArrayOutputStream();
                        DataOutputStream out = new DataOutputStream(byteOutStream);
                        try {
                            out.writeUTF("Chwho");
                            out.writeUTF("Get");
                            out.writeUTF(mcp.getUuid().toString());
                            out.writeUTF(channel.getName());
                            pluginMessageController.sendPluginMessage(byteOutStream);
                            out.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        return;
                    }

                    PluginManager pluginManager = plugin.getServer().getPluginManager();
                    long linecount = LINE_LENGTH;
                    for (IImmersiveChatPlayer p : playerApiService.getOnlineMineverseChatPlayers()) {
                        if (p.getListening().contains(channel.getName())) {
                            if (sender instanceof Player) {
                                if (!((Player) sender).canSee(p.getPlayer())) {
                                    continue;
                                }
                            }
                            if (channel.getDistance() > 0 && sender instanceof Player) {
                                if (!this.isPlayerWithinDistance((Player) sender, p.getPlayer(), channel.getDistance())) {
                                    continue;
                                }
                            }
                            if (plugin.getConfig().getBoolean("enable_towny_channel") && pluginManager.isPluginEnabled("Towny") && sender instanceof Player) {
                                try {
                                    TownyUniverse towny = TownyUniverse.getInstance();
                                    if (channel.getName().equalsIgnoreCase("Town")) {
                                        Resident r = towny.getResident(p.getName());
                                        Resident pp = towny.getResident(((Player) sender).getName());
                                        if (!pp.hasTown()) {
                                            if (playerlist.length() + p.getName().length() > linecount) {
                                                playerlist += "\n";
                                                linecount = linecount + LINE_LENGTH;
                                            }
                                            if (!p.getMutes().containsKey(channel.getName())) {
                                                playerlist += ChatColor.WHITE + p.getName();
                                            } else {
                                                playerlist += ChatColor.RED + p.getName();
                                            }
                                            playerlist += ChatColor.WHITE + ", ";
                                            break;
                                        } else if (!r.hasTown()) {
                                            continue;
                                        } else if (!(r.getTown().getName().equals(pp.getTown().getName()))) {
                                            continue;
                                        }
                                    }
                                    if (channel.getName().equalsIgnoreCase("Nation")) {
                                        Resident r = towny.getResident(p.getName());
                                        Resident pp = towny.getResident(((Player) sender).getName());
                                        if (!pp.hasNation()) {
                                            if (playerlist.length() + p.getName().length() > linecount) {
                                                playerlist += "\n";
                                                linecount = linecount + LINE_LENGTH;
                                            }
                                            if (!p.getMutes().containsKey(channel.getName())) {
                                                playerlist += ChatColor.WHITE + p.getName();
                                            } else {
                                                playerlist += ChatColor.RED + p.getName();
                                            }
                                            playerlist += ChatColor.WHITE + ", ";
                                            break;
                                        } else if (!r.hasNation()) {
                                            continue;
                                        } else if (!(r.getTown().getNation().getName().equals(pp.getTown().getNation().getName()))) {
                                            continue;
                                        }
                                    }
                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                }
                            }
                            if (plugin.getConfig().getBoolean("enable_factions_channel") && pluginManager.isPluginEnabled("Factions") && sender instanceof Player) {
                                try {
                                    if (channel.getName().equalsIgnoreCase("Faction")) {
                                        MPlayer mplayer = MPlayer.get(p.getPlayer());
                                        MPlayer mplayerp = MPlayer.get((Player) sender);
                                        if (!mplayerp.hasFaction()) {
                                            if (playerlist.length() + p.getName().length() > linecount) {
                                                playerlist += "\n";
                                                linecount = linecount + LINE_LENGTH;
                                            }
                                            if (!p.getMutes().containsKey(channel.getName())) {
                                                playerlist += ChatColor.WHITE + p.getName();
                                            } else {
                                                playerlist += ChatColor.RED + p.getName();
                                            }
                                            playerlist += ChatColor.WHITE + ", ";
                                            break;
                                        } else if (!mplayerp.hasFaction()) {
                                            continue;
                                        } else if (!(mplayer.getFactionName().equals(mplayerp.getFactionName()))) {
                                            continue;
                                        }
                                    }
                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                }
                            }
                            if (playerlist.length() + p.getName().length() > linecount) {
                                playerlist += "\n";
                                linecount = linecount + LINE_LENGTH;
                            }
                            if (!p.getMutes().containsKey(channel.getName())) {
                                playerlist += ChatColor.WHITE + p.getName();
                            } else {
                                playerlist += ChatColor.RED + p.getName();
                            }
                            playerlist += ChatColor.WHITE + ", ";
                        }
                    }
                    if (playerlist.length() > 2) {
                        playerlist = playerlist.substring(0, playerlist.length() - 2);
                    }
                    sender.sendMessage(LocalizedMessage.CHANNEL_PLAYER_LIST_HEADER.toString()
                            .replace("{channel_color}", (channel.getColor()).toString())
                            .replace("{channel_name}", channel.getName()));
                    sender.sendMessage(playerlist);
                    return;
                } else {
                    sender.sendMessage(LocalizedMessage.INVALID_CHANNEL.toString()
                            .replace("{args}", args[0]));
                    return;
                }
            } else {
                sender.sendMessage(LocalizedMessage.COMMAND_INVALID_ARGUMENTS.toString()
                        .replace("{command}", "/chwho")
                        .replace("{args}", "[channel]"));
                return;
            }
        } else {
            sender.sendMessage(LocalizedMessage.COMMAND_NO_PERMISSION.toString());
            return;
        }
    }

    private boolean isPlayerWithinDistance(Player p1, Player p2, double Distance) {
        Double chDistance = Distance;
        Location locreceip;
        Location locsender = p1.getLocation();
        Location diff;
        if (chDistance > (double) 0) {
            locreceip = p2.getLocation();
            if (locreceip.getWorld() == p1.getWorld()) {
                diff = locreceip.subtract(locsender);
                if (Math.abs(diff.getX()) > chDistance || Math.abs(diff.getZ()) > chDistance || Math.abs(diff.getY()) > chDistance) {
                    return false;
                }
            } else {
                return false;
            }
        }
        return true;
    }
}
