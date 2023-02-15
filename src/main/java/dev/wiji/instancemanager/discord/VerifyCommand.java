package dev.wiji.instancemanager.discord;

import dev.wiji.instancemanager.BungeeMain;
import dev.wiji.instancemanager.ConfigManager;
import dev.wiji.instancemanager.ProxyRunnable;
import dev.wiji.instancemanager.pitsim.LockdownManager;
import dev.wiji.instancemanager.pitsim.OverworldServerManager;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

//This is disabled
public class VerifyCommand extends DiscordCommand {
	public static List<Long> recentVerificationPlayers = new ArrayList<>();

	public VerifyCommand() {
		super("verify");
	}

	@Override
	public void execute(GuildMessageReceivedEvent event, List<String> args) {

		if(recentVerificationPlayers.contains(event.getAuthor().getIdLong())) {
			event.getChannel().sendMessage("This command has a cooldown of 1 hour. Please try again later.").queue();
			return;
		}

		if(args.size() < 1) {
			event.getChannel().sendMessage("Usage: .verify <ign/uuid>").queue();
			return;
		}
		String nameUUID = args.get(0);

		boolean wasVerified = LockdownManager.removeVerifiedPlayer(event.getAuthor().getIdLong());
		boolean verificationSuccessful = LockdownManager.verify(nameUUID.toLowerCase(), event.getAuthor().getIdLong());

		if(!verificationSuccessful) {
			event.getChannel().sendMessage("That player was not found").queue();
			return;
		}

		if(wasVerified) {
			event.getChannel().sendMessage("Successfully verified! (Your previously verified account was removed)").queue();
		} else {
			event.getChannel().sendMessage("Successfully verified!").queue();
		}

		ProxiedPlayer target;
		try {
			UUID uuid = UUID.fromString(nameUUID);
			target = BungeeMain.INSTANCE.getProxy().getPlayer(uuid);
		} catch(Exception e) {
			target = BungeeMain.INSTANCE.getProxy().getPlayer(nameUUID);
		}
		if(target != null && target.getServer().getInfo().getName().equals(ConfigManager.getLobbyServer())) OverworldServerManager.queue(target, 0, false);

		recentVerificationPlayers.add(event.getAuthor().getIdLong());
		((ProxyRunnable) () -> recentVerificationPlayers.remove(event.getAuthor().getIdLong())).runAfter(1, TimeUnit.HOURS);

		try {
			Objects.requireNonNull(DiscordManager.JDA.getTextChannelById(Constants.VERIFICATION_LOG_CHANNEL))
					.sendMessage("Discord: `" + event.getAuthor().getAsTag() + "`" +
							"\nIGN/UUID: `" + nameUUID.toLowerCase() + "`" +
							"\nWas Verified: " + (wasVerified ? "`Yes`" : "`No`")).queue();
		} catch(Exception ignored) {
			System.out.println("verification channel does not exist");
		}
	}
}
