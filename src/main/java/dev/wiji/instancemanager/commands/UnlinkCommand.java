package dev.wiji.instancemanager.commands;

import dev.wiji.instancemanager.discord.AuthenticationManager;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

public class UnlinkCommand extends Command {

	public UnlinkCommand() {
		super("unlink");
	}

	@Override
	public void execute(CommandSender sender, String[] args) {
		if(!(sender instanceof ProxiedPlayer)) return;
		ProxiedPlayer proxiedPlayer = (ProxiedPlayer) sender;


	}
}
