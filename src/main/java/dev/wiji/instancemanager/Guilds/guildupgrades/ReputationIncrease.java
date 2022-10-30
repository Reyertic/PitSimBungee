package dev.wiji.instancemanager.Guilds.guildupgrades;

import dev.wiji.instancemanager.Guilds.controllers.objects.DummyItemStack;
import dev.wiji.instancemanager.Guilds.controllers.objects.Guild;
import dev.wiji.instancemanager.Guilds.controllers.objects.GuildUpgrade;
import dev.wiji.instancemanager.Guilds.events.GuildReputationEvent;
import dev.wiji.instancemanager.Misc.ALoreBuilder;
import dev.wiji.instancemanager.Misc.AUtil;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.event.EventHandler;

public class ReputationIncrease extends GuildUpgrade {
	public ReputationIncrease() {
		super(ChatColor.WHITE + "Increased Reputation", "reputation", 5);
	}

	@EventHandler
	public void onReputation(GuildReputationEvent event) {
		Guild guild = event.getGuild();
		int reputationLevel = guild.getLevel(this);
		if(reputationLevel == 0) return;
		event.addMultiplier(1 + reputationLevel / 5.0);
	}

	@Override
	public int getCost(int level) {
		switch(level) {
			case 1:
				return 1_000_000;
			case 2:
				return 5_000_000;
			case 3:
				return 10_000_000;
			case 4:
				return 15_000_000;
			case 5:
				return 20_000_000;
		}
		return -1;
	}

	@Override
	public DummyItemStack getBaseStack(Guild guild, int level) {
		ALoreBuilder lore = new ALoreBuilder();
		if(level != 0) {
			lore.addLore("&7Current: &f+" + (level * 20) + "% reputation", "&7Tier: &a" + AUtil.toRoman(level), "");
		}
		lore.addLore("&7Each tier:", "&f+20% &7reputation");

		DummyItemStack itemStack = new DummyItemStack("NETHER_STAR");
		itemStack.setDisplayName(displayName);
		itemStack.setLore(lore.getLore());
		return itemStack;
	}
}
