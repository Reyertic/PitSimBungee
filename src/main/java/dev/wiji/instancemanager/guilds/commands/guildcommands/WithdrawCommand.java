package dev.wiji.instancemanager.guilds.commands.guildcommands;

import dev.wiji.instancemanager.BungeeMain;
import dev.wiji.instancemanager.guilds.ArcticGuilds;
import dev.wiji.instancemanager.guilds.GuildMessaging;
import dev.wiji.instancemanager.guilds.controllers.GuildManager;
import dev.wiji.instancemanager.guilds.controllers.PermissionManager;
import dev.wiji.instancemanager.guilds.controllers.objects.Guild;
import dev.wiji.instancemanager.guilds.controllers.objects.GuildMember;
import dev.wiji.instancemanager.guilds.controllers.objects.GuildMemberInfo;
import dev.wiji.instancemanager.guilds.events.GuildWithdrawalEvent;
import dev.wiji.instancemanager.misc.ACommand;
import dev.wiji.instancemanager.misc.AMultiCommand;
import dev.wiji.instancemanager.misc.AOutput;
import dev.wiji.instancemanager.misc.Constants;
import dev.wiji.instancemanager.ProxyRunnable;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.List;
import java.util.Map;

public class WithdrawCommand extends ACommand {
	public WithdrawCommand(AMultiCommand base, String executor) {
		super(base, executor);
	}

	@Override
	public void execute(CommandSender sender, List<String> args) {
		if(!(sender instanceof ProxiedPlayer)) return;
		ProxiedPlayer player = (ProxiedPlayer) sender;

		Guild guild = GuildManager.getGuildFromPlayer(player.getUniqueId());
		if(guild == null) {
			AOutput.error(player, "You are not in a guild");
			return;
		}

		Map.Entry<GuildMember, GuildMemberInfo> entry = guild.getMember(player);
		if(!PermissionManager.isAdmin(player)) {
			if(!entry.getValue().rank.isAtLeast(Constants.WITHDRAW_PERMISSION)) {
				AOutput.error(player, "You must be at least " + Constants.WITHDRAW_PERMISSION.displayName + " to do this");
				return;
			}
		}

		if(args.size() < 1) {
			AOutput.error(player, "Usage: /withdraw <amount>");
			return;
		}

		int amount;
		try {
			amount = Integer.parseInt(args.get(0));
			if(amount <= 0) throw new IllegalArgumentException();
		} catch(Exception ignored) {
			AOutput.error(player, "Invalid amount");
			return;
		}

		if(amount > guild.getBalance()) {
			AOutput.error(player, "There is not enough money to do this");
			return;
		}

		GuildWithdrawalEvent event = new GuildWithdrawalEvent(player, guild, amount);
		BungeeMain.INSTANCE.getProxy().getPluginManager().callEvent(event);
		if(event.isCancelled()) return;

		ProxyRunnable success = () -> {
			Guild newGuild = GuildManager.getGuildFromPlayer(player.getUniqueId());
			if(newGuild == null) {
				AOutput.error(player, "You are not in a guild");
				return;
			}
			newGuild.withdraw(amount);

			newGuild.broadcast("&a&lGUILD! &7" + player.getName() + " has withdrawn &6" + ArcticGuilds.decimalFormat.format(amount) + "g");
		};

		ProxyRunnable failure = () -> {};

		GuildMessaging.withdraw(player, amount, success, failure);
	}
}
