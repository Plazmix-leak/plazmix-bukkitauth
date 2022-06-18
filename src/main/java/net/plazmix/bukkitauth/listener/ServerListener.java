package net.plazmix.bukkitauth.listener;

import com.comphenix.protocol.utility.MinecraftReflection;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.plazmix.bukkitauth.PlazmixBukkitAuthPlugin;
import net.plazmix.coreconnector.CoreConnector;
import net.plazmix.coreconnector.core.auth.AuthManager;
import net.plazmix.coreconnector.core.network.NetworkManager;
import net.plazmix.coreconnector.direction.bukkit.event.PlayerAuthCompleteEvent;
import net.plazmix.coreconnector.utility.server.ServerMode;
import net.plazmix.holographic.ProtocolHolographic;
import net.plazmix.holographic.impl.SimpleHolographic;
import net.plazmix.holographic.updater.SimpleHolographicUpdater;
import net.plazmix.scoreboard.BaseScoreboardBuilder;
import net.plazmix.scoreboard.BaseScoreboardScope;
import net.plazmix.utility.player.PlazmixUser;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
public final class ServerListener implements Listener {

    private final PlazmixBukkitAuthPlugin plugin;

    private final Map<Player, SimpleHolographic> playerHolographicMap = new HashMap<>();
    private final Map<Player, BukkitRunnable> playerTaskMap = new HashMap<>();

    private final Location holoLocation = new Location(Bukkit.getWorld("world"), 107.5, 58, -96.5);

    @EventHandler
    public void onAuthComplete(PlayerAuthCompleteEvent event) {
        if (!ServerMode.isCurrentTyped(ServerMode.AUTH)) {
            return;
        }

        Player player = event.getBukkitPlayer();

        if (player != null) {
            BukkitRunnable runnable = playerTaskMap.remove(player);

            if (runnable != null) {
                runnable.cancel();
            }

            player.setExp(0);
            player.setLevel(0);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            player.setHealth(2);
            player.setMaxHealth(2);

            player.setLevel(0);
            player.setExp(0);

            player.setFoodLevel(20);

            try {
                MinecraftReflection.getCraftPlayerClass().getMethod("updateScaledHealth").invoke(player);
            } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException ignored) {
            }

        }, 30);

        // Server typing check
        ServerMode serverMode = ServerMode.getMode(CoreConnector.getInstance().getServerName());

        switch (serverMode) {
            case AUTH: {
                int playerId = NetworkManager.INSTANCE.getPlayerId(player.getName());
                AuthManager.INSTANCE.getAuthPlayerMap().remove(playerId);


                createHolographic(player, holoLocation);

                BaseScoreboardBuilder scoreboardBuilder = BaseScoreboardBuilder.newScoreboardBuilder();
                scoreboardBuilder.scoreboardScope(BaseScoreboardScope.PROTOTYPE);

                scoreboardBuilder.scoreboardDisplay("§d§lAUTH");

                scoreboardBuilder.scoreboardLine(11, "");
                scoreboardBuilder.scoreboardLine(10, "Вы находитесь на ");
                scoreboardBuilder.scoreboardLine(9, "сервере для авторизации.");
                scoreboardBuilder.scoreboardLine(8, "");

                if (!AuthManager.INSTANCE.hasPlayerAccount(playerId)) {
                    scoreboardBuilder.scoreboardLine(7, "Для регистрации используйте:");
                    scoreboardBuilder.scoreboardLine(6, "§e/reg [Ваш пароль] [Пароль]");
                    scoreboardBuilder.scoreboardLine(5, "");
                } else {
                    scoreboardBuilder.scoreboardLine(7, "Для авторизации используйте:");
                    scoreboardBuilder.scoreboardLine(6, "§a/l [пароль]");
                    scoreboardBuilder.scoreboardLine(5, "");
                }

                int vkId = AuthManager.INSTANCE.getAuthPlayer(player.getName()).getVkId();
                if (vkId <= 0) {
                    scoreboardBuilder.scoreboardLine(4, "Привяжите свой аккаунт!");
                    scoreboardBuilder.scoreboardLine(3, "§dvk.me/plazmixnetwork");
                    scoreboardBuilder.scoreboardLine(2, "!привязать [ник] [пароль]");
                } else {
                    scoreboardBuilder.scoreboardLine(4, "У вас привязан аккаунт");
                    scoreboardBuilder.scoreboardLine(3, "Подтвердите свой вход");
                    scoreboardBuilder.scoreboardLine(2, "§dvk.me/plazmixnetwork");
                }

                scoreboardBuilder.scoreboardLine(1, "");
                scoreboardBuilder.scoreboardLine(0, "§d§lwww.Plazmix.net");

                scoreboardBuilder.build().setScoreboardToPlayer(player);


                if (AuthManager.INSTANCE.hasAuthSession(playerId)) {
                    return;
                }

                BukkitRunnable bukkitRunnable = new BukkitRunnable() {
                    private int secondsLeft = 120;

                    @Override
                    public void run() {

                        // Title announce
                        player.sendTitle("§d§lPlazmix", "§fДля начала игры необходимо пройти авторизацию", 0, 100, 0);

                        // Experience timer
                        if (secondsLeft <= 0) {
                            return;
                        }

                        player.setLevel(secondsLeft);
                        player.setExp(secondsLeft / 120f);

                        if (secondsLeft < 60 && secondsLeft < 30 && secondsLeft < 15) {
                            player.sendTitle("§d§lPlazmix", "§fОсталось §c" + secondsLeft + " §fсекунд для авторизации");
                        }

                        secondsLeft--;

                        // Sounds
                        player.playSound(player.getLocation(), Sound.BLOCK_DISPENSER_DISPENSE, 1, 2);
                    }
                };

                playerTaskMap.put(player, bukkitRunnable);
                bukkitRunnable.runTaskTimer(plugin, 0, 20);
                break;
            }

            case LIMBO: {
                BaseScoreboardBuilder scoreboardBuilder = BaseScoreboardBuilder.newScoreboardBuilder();
                scoreboardBuilder.scoreboardScope(BaseScoreboardScope.PROTOTYPE);

                scoreboardBuilder.scoreboardDisplay("§6§lLIMBO");

                scoreboardBuilder.scoreboardLine(8, "");
                scoreboardBuilder.scoreboardLine(7, "Вы находитесь на лимбо-сервере");
                scoreboardBuilder.scoreboardLine(6, "Обычно игроки здесь находятся");
                scoreboardBuilder.scoreboardLine(5, "из-за того, что сервер, что");
                scoreboardBuilder.scoreboardLine(4, "разорвал соединение с ними");
                scoreboardBuilder.scoreboardLine(3, "");
                scoreboardBuilder.scoreboardLine(2, "Вернуться на сервер - §e/hub");
                scoreboardBuilder.scoreboardLine(1, "");
                scoreboardBuilder.scoreboardLine(0, "§d§lwww.Plazmix.net");

                scoreboardBuilder.build().setScoreboardToPlayer(player);
                break;
            }
        }
    }

    private void createHolographic(@NonNull Player player, @NonNull Location location) {
        PlazmixUser plazmixUser = PlazmixUser.of(player);
        AuthManager.INSTANCE.getAuthPlayerMap().remove(plazmixUser.getPlayerId());

        int vkId = AuthManager.INSTANCE.getAuthPlayer(player.getName()).getVkId();

        SimpleHolographic simpleHolographic = playerHolographicMap.computeIfAbsent(player, f -> new SimpleHolographic(location));
        simpleHolographic.setHolographicUpdater(20, new SimpleHolographicUpdater(simpleHolographic) {

            @Override
            public void accept(ProtocolHolographic protocolHolographic) {

                if (!AuthManager.INSTANCE.hasPlayerAccount(plazmixUser.getPlayerId())) {

                    protocolHolographic.setOriginalHolographicLine(0, "§cРегистрация");
                    protocolHolographic.setEmptyHolographicLine(1);
                    protocolHolographic.setOriginalHolographicLine(2, "§fДля регистрации Вам нужно ввести команду:");
                    protocolHolographic.setOriginalHolographicLine(3, "§c/reg [Ваш пароль] [Пароль]");
                    protocolHolographic.setEmptyHolographicLine(4);
                    protocolHolographic.setOriginalHolographicLine(5, "§fНе забудьте привязать свой аккаунт к §9группе ВК");
                    protocolHolographic.setOriginalHolographicLine(6, "§dvk.me/plazmixnetwork");
                    protocolHolographic.setOriginalHolographicLine(7, "§Команда §6!привязать [ник] [пароль]");

                } else {
                    protocolHolographic.setOriginalHolographicLine(0, "§aАвторизация");
                    protocolHolographic.setEmptyHolographicLine(1);
                    protocolHolographic.setOriginalHolographicLine(2, "§fДля авторизации Вам нужно ввести команду:");
                    protocolHolographic.setOriginalHolographicLine(3, "§a/login [Ваш пароль]");
                    protocolHolographic.setEmptyHolographicLine(4);
                    protocolHolographic.setOriginalHolographicLine(5, "§fНе забудьте привязать свой аккаунт к §9Группе ВК");
                    protocolHolographic.setOriginalHolographicLine(6, "§dvk.me/plazmixnetwork");
                    protocolHolographic.setOriginalHolographicLine(7, "§fКоманда !привязать [ник] [пароль]");

                    if (vkId <= 0) {
                        protocolHolographic.setOriginalHolographicLine(5, "§fНе забудьте привязать свой аккаунт к §9Группе ВК");
                        protocolHolographic.setOriginalHolographicLine(6, "§dvk.me/plazmixnetwork");
                        protocolHolographic.setOriginalHolographicLine(7, "§fКоманда !привязать [ник] [пароль]");

                    } else {
                        protocolHolographic.setOriginalHolographicLine(5, "§fУ вас привязан аккаунт к §cдвухэтапной аунтификации");
                        protocolHolographic.setOriginalHolographicLine(6, "§fПодтвердите свой вход в диалоге §9ВК:");
                        protocolHolographic.setOriginalHolographicLine(7, "§dvk.me/plazmixnetwork");
                    }
                }
            }
        });

        simpleHolographic.addReceivers(player);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {

        // Player Task
        BukkitRunnable bukkitRunnable = playerTaskMap.remove(event.getPlayer());

        if (bukkitRunnable != null) {
            bukkitRunnable.cancel();
        }

        // Player holographic
        SimpleHolographic simpleHolographic = playerHolographicMap.get(event.getPlayer());

        if (simpleHolographic != null) {
            simpleHolographic.remove();
        }
    }

}