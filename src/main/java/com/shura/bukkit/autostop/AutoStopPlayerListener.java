package com.shura.bukkit.autostop;

import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerJoinEvent;

public class AutoStopPlayerListener extends PlayerListener {
  private final AutoStop plugin;

  public AutoStopPlayerListener(AutoStop instance) {
    plugin = instance;
  }

  public void onPlayerQuit(PlayerQuitEvent event) {
    plugin.checkStop();
  }

  public void onPlayerJoin(PlayerJoinEvent event) {
    plugin.deleteStop();
  }
}
