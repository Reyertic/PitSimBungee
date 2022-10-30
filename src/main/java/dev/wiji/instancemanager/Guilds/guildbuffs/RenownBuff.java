package dev.wiji.instancemanager.Guilds.guildbuffs;

import dev.wiji.instancemanager.Guilds.controllers.BuffManager;
import dev.wiji.instancemanager.Guilds.controllers.objects.DummyItemStack;
import dev.wiji.instancemanager.Guilds.controllers.objects.Guild;
import dev.wiji.instancemanager.Guilds.controllers.objects.GuildBuff;
import dev.wiji.instancemanager.Misc.ALoreBuilder;
import dev.wiji.instancemanager.Misc.AUtil;
import net.md_5.bungee.api.ChatColor;

import java.util.Map;

public class RenownBuff extends GuildBuff {
	public RenownBuff() {
		super(ChatColor.YELLOW + "Renown Buff", "renown",
				new ALoreBuilder("&7Increased passive renown").getLore(), ChatColor.YELLOW);
	}

	@Override
	public DummyItemStack getDisplayItem(Guild guild, int level) {
		ALoreBuilder lore = new ALoreBuilder();
		if(level != 0) {
			lore.addLore("&7Tier: &a" + AUtil.toRoman(level), "");
			Map<SubBuff, Double> buffMap = BuffManager.getAllBuffs(level).get(this);
			for(Map.Entry<SubBuff, Double> entry : buffMap.entrySet()) {
				lore.addLore(chatColor + entry.getKey().getDisplayString(entry.getValue()));
			}
		}

		DummyItemStack itemStack = new DummyItemStack("BEACON");
		itemStack.setDisplayName(displayName);
		itemStack.setLore(lore.getLore());
		return itemStack;
	}

	@Override
	public void addBuffs() {
		SubBuff renownSub = new SubBuff("renown", "&e+%amount%% &7more passive renown");

		addSubBuff(1, renownSub, 12.5);
		addSubBuff(2, renownSub, 12.5);
		addSubBuff(3, renownSub, 12.5);
		addSubBuff(4, renownSub, 12.5);
		addSubBuff(5, renownSub, 12.5);
		addSubBuff(6, renownSub, 12.5);
		addSubBuff(7, renownSub, 12.5);
		addSubBuff(8, renownSub, 12.5);
	}
}
