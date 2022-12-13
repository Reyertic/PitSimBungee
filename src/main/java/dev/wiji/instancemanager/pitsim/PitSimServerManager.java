package dev.wiji.instancemanager.pitsim;

import dev.wiji.instancemanager.BungeeMain;
import dev.wiji.instancemanager.ConfigManager;
import dev.wiji.instancemanager.ProxyRunnable;
import dev.wiji.instancemanager.ServerManager;
import dev.wiji.instancemanager.commands.LobbiesCommand;
import dev.wiji.instancemanager.guilds.GuildMessaging;
import dev.wiji.instancemanager.objects.PitSimServer;
import dev.wiji.instancemanager.objects.PluginMessage;
import dev.wiji.instancemanager.objects.ServerStatus;
import dev.wiji.instancemanager.storage.EditSessionManager;
import dev.wiji.instancemanager.storage.StorageManager;
import dev.wiji.instancemanager.storage.StorageProfile;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class PitSimServerManager implements Listener {
	public static List<PitSimServer> serverList = new ArrayList<>();

	//	The next server turns on when the player count reaches a multiple of this number
	public static final int NEW_SERVER_THRESHOLD = 10;
	//	When the player count drops this many below a multiple of the number above, that server enabled by hitting
//	that threshold is no longer needed and gets shut down
	public static final int REQUIRED_DROP_FOR_SHUTDOWN = 4;

	public static boolean networkIsShuttingDown = false;

	static {
		((ProxyRunnable) () -> {
			int players = getTotalPlayers();

			if(networkIsShuttingDown) return;

			for(PitSimServer pitSimServer : serverList) {
				if(pitSimServer.status == ServerStatus.STARTING) return;
			}

			for(int i = 0; i < Math.min(players / NEW_SERVER_THRESHOLD + 1, serverList.size()); i++) {
				PitSimServer server = serverList.get(i);
				if(server.status.isOnline()) {
					if(server.status == ServerStatus.SHUTTING_DOWN_INITIAL) {
						server.status = ServerStatus.RUNNING;
						new PluginMessage().writeString("CANCEL SHUTDOWN").addServer(server.getServerInfo().getName()).send();
					}
					continue;
				}

				if(server.isOnStartCooldown) continue;

				server.status = ServerStatus.STARTING;
				server.startUp();
				System.out.println("Turning on server: " + (i + 1));
			}

			for(int i = 1 + (players + REQUIRED_DROP_FOR_SHUTDOWN - 1) / 10; i < serverList.size(); i++) {
				PitSimServer server = serverList.get(i);
				if(server.status.isShuttingDown() || server.status == ServerStatus.OFFLINE || server.status == ServerStatus.SUSPENDED) continue;
				if(server.status == ServerStatus.RESTARTING_INITIAL) {
					server.status = ServerStatus.SHUTTING_DOWN_INITIAL;
					System.out.println("Switching restart to shut down: " + (i + 1));
					continue;
				}

				System.out.println("Shutting down server: " + (i + 1));
				server.shutDown(false);
			}

		}).runAfterEvery(10, 10, TimeUnit.SECONDS);
	}

	@EventHandler
	public void onJoin(PostLoginEvent event) {
		if(!ConfigManager.isDev()) return;
		ProxiedPlayer player = event.getPlayer();
		((ProxyRunnable) () -> queue(player, 0, false)).runAfter(1, TimeUnit.SECONDS);
	}

	public static void init() {
		for(String value : ServerManager.pitSimServers.values()) serverList.add(new PitSimServer(value));

		for(PitSimServer server : serverList) {
			if(serverList.get(0) == server) {

//				if(ConfigManager.isDev()) {
//					server.status = ServerStatus.RUNNING;
//					server.setStartTime(System.currentTimeMillis());
//					continue;
//				}

				server.status = ServerStatus.STARTING;
				ServerManager.restartServer(server.getPteroID());
			}

			server.hardShutDown();
		}
	}

	public static boolean queue(ProxiedPlayer player, int requestedServer, boolean fromDarkzone) {

		if(EditSessionManager.isBeingEdited(player.getUniqueId())) {
			player.sendMessage(new ComponentBuilder("Your player-data is being modified. Please try again in a moment.").color(ChatColor.RED).create());
			return false;
		}

		try {
			LeaderboardCalc.sendLeaderboardPlayerData(player.getUniqueId());
		} catch(Exception e) {
			System.out.println("Player leaderboard data send failed. (Proxy has just started)");
		}

		GuildMessaging.sendGuildData(player);

		PitSimServer previousServer = null;
		for(PitSimServer server : serverList) {
			if(server.getServerInfo() == player.getServer().getInfo()) {
				previousServer = server;
				break;
			}
		}

		if(previousServer != null) {
			if(previousServer.status == ServerStatus.RESTARTING_FINAL || previousServer.status == ServerStatus.SHUTTING_DOWN_FINAL) {
				if(networkIsShuttingDown) {
					player.connect(BungeeMain.INSTANCE.getProxy().getServerInfo(ConfigManager.getLobbyServer()));
				}
			}
		}

		if(getTotalServersOnline() == 0) {
			player.sendMessage(new ComponentBuilder("There are currently no available servers. Please try again later.").color(ChatColor.RED).create());
			return false;
		}

		if(ServerChangeListener.recentlyLeft.contains(player)) {
			player.sendMessage(new ComponentBuilder("You recently left a server. Please wait a few seconds before rejoining.").color(ChatColor.RED).create());
			return false;
		}

		ServerDataManager.sendServerData();

		PitSimServer targetServer = null;

		if(requestedServer != 0) {
			targetServer = serverList.get(requestedServer - 1);
			if(targetServer.status != ServerStatus.RUNNING) {
				player.sendMessage(new ComponentBuilder("This server is currently unavailable!").color(ChatColor.RED).create());
				return false;
			}
		}

		int players = getTotalPlayers();

		if(targetServer == null) {
			for(PitSimServer activeServer : serverList) {
				if(activeServer.status != ServerStatus.RUNNING) continue;

				if(activeServer.getPlayers().size() > players / getTotalServersOnline()) continue;
				else {
					targetServer = activeServer;
					break;
				}
			}
		}

		if(targetServer == null) {
			for(PitSimServer pitSimServer : serverList) {
				if(pitSimServer.status == ServerStatus.RUNNING) {
					targetServer = pitSimServer;
					break;
				}
			}
		}

		if(targetServer == null) {

			if(previousServer != null) {
				if(previousServer.status == ServerStatus.RESTARTING_FINAL || previousServer.status == ServerStatus.SHUTTING_DOWN_FINAL || previousServer.status == ServerStatus.SUSPENDED) {
					player.connect(BungeeMain.INSTANCE.getProxy().getServerInfo(ConfigManager.getLobbyServer()));
					return true;
				}
			}

			player.sendMessage(new ComponentBuilder("There are currently no available servers. Please try again later.").color(ChatColor.RED).create());
			return false;
		}

		StorageProfile profile = StorageManager.getStorage(player.getUniqueId());
		profile.sendToServer(targetServer.getServerInfo());

		player.sendMessage((new ComponentBuilder("Sending you to " + targetServer.getServerInfo().getName()).color(ChatColor.GREEN).create()));

		if(fromDarkzone) new PluginMessage().writeString("DARKZONE JOIN").writeString(player.getUniqueId().toString()).writeBoolean(true).addServer(targetServer.getServerInfo().getName()).send();

		PitSimServer finalTargetServer = targetServer;
		((ProxyRunnable) () -> player.connect(finalTargetServer.getServerInfo())).runAfter(1, TimeUnit.SECONDS);

		return true;
	}

	public static int getTotalPlayers() {
		if(LobbiesCommand.overridePlayers) return 10;
		int total = 0;
		for(PitSimServer server : serverList) {
			total += server.getPlayers().size();
		}
		return total;
	}

	public static int getTotalServersOnline() {
		int total = 0;
		for(PitSimServer server : serverList) {
			if(server.status == ServerStatus.RUNNING || server.status == ServerStatus.RESTARTING_INITIAL) total++;
		}

		System.out.println("Total online servers: " + total);
		return total;
	}

}
