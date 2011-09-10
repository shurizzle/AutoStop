package com.shura.bukkit.autostop;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;
import java.util.Date;

import net.minecraft.server.MinecraftServer;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.Event;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.PluginManager;

import com.shura.bukkit.autostop.AutoStopPlayerListener;

public class AutoStop extends JavaPlugin {
  class AutoStopStartThread extends Thread {
    protected AutoStop plugin = null;
    protected int secs = 0;
    protected boolean enabled = true;

    public AutoStopStartThread(AutoStop plugin, int secs) {
      super();
      this.plugin = plugin;
      this.secs = secs;
      start();
    }

    public boolean isEnabled() {
      return enabled;
    }

    public void disable() {
      this.enabled = false;
    }

    public void run() {
      try {
        sleep(this.secs * 1000, 0);
      } catch (InterruptedException ie) {
      }

      if (isEnabled()) {
        plugin.getServer().savePlayers();
        plugin.executeCmd();
        plugin.getMcServer().a();
      }
    }
  }

  class AutoStopThread extends Thread {
    private boolean _suspended = false;
    protected AutoStop plugin = null;
    protected int secs = 0;
    protected Date time = null;

    public AutoStopThread(AutoStop plugin, int secs) {
      super();
      this.plugin = plugin;
      this.secs = secs;
      time = new Date(next());
      pause();
      start();
    }

    public boolean isSuspended() {
      return _suspended;
    }

    public synchronized void pause() {
      _suspended = true;
    }

    public synchronized void play() {
      if (isSuspended()) {
        notify();
      }
      _suspended = false;
    }

    protected long current() {
      return System.currentTimeMillis();
    }

    protected long next() {
      return (current() + (secs * 1000));
    }

    public void update() {
      time.setTime(next());
      play();
    }

    public void run() {
      while (time.compareTo(new Date()) >= 0) {
        try {
          long sleep_time = time.getTime() - current();
          if (sleep_time > 0) {
            sleep(sleep_time, 0);
          }

          if (isSuspended()) {
            synchronized(this) {
              while(isSuspended()) {
                wait();
              }
            }
          }
        } catch (InterruptedException ie) {
        }
      }

      plugin.getServer().savePlayers();
      plugin.executeCmd();
      plugin.getMcServer().a();
    }
  }

  private AutoStopPlayerListener playerListener = new AutoStopPlayerListener(this);
  protected Properties config = new Properties();
  private AutoStopThread stopThread = null;
  private AutoStopStartThread initThread = null;

  public MinecraftServer getMcServer() {
    return ((CraftServer)getServer()).getServer();
  }

  public void onDisable() {
    deleteStop();
  }

  public void onEnable() {
    PluginManager pm = getServer().getPluginManager();

    pm.registerEvent(Event.Type.PLAYER_QUIT, playerListener, Priority.Monitor, this);
    pm.registerEvent(Event.Type.PLAYER_JOIN, playerListener, Priority.Monitor, this);

    PluginDescriptionFile pdfFile = this.getDescription();

    if (!loadProperties("AutoStop.properties")) {
      System.out.println("Unable to enable plugin. Properties file AutoStop/AutoStop.properties does not exist and could not be created.");
      return;
    }

    System.out.println(pdfFile.getName() + " version " + pdfFile.getVersion() + " is enabled!");
    stopThread = new AutoStopThread(this, Integer.parseInt(config.getProperty("delay.empty")));
    initThread = new AutoStopStartThread(this, Integer.parseInt(config.getProperty("delay.start")));
  }

  public void deleteStop() {
    if (initThread.isEnabled()) {
      initThread.disable();
    }
    else if (!stopThread.isSuspended()) {
      stopThread.pause();
    }
  }

  public void checkStop() {
    if (getServer().getOnlinePlayers().length == 1) {
      stopThread.update();
    }
  }

  public boolean executeCmd() {
    String cmd = config.getProperty("execute.cmd").trim();

    if (cmd == null || cmd.isEmpty()) {
      return true;
    }

    try {
      System.out.println("Running: " + cmd);
      Process p = Runtime.getRuntime().exec(new String[] { "/bin/sh", "-c", cmd });
      p.waitFor();

      if (p.exitValue() != 0) {
        System.out.println(String.format("Command exited with code: %d", p.exitValue()));
        return false;
      }
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }

    return true;
  }

  private boolean loadProperties(String file) {
    File propFile = new File("plugins/AutoStop/" + file);

    if (!propFile.exists()) {
      File confDir = new File("plugins/AutoStop");
      if (!confDir.exists()) {
        confDir.mkdir();
      }

      BufferedReader br = new BufferedReader(new InputStreamReader(this.getClassLoader().getResourceAsStream(file)));
      BufferedWriter bw = null;
      try {
        bw = new BufferedWriter(new FileWriter(propFile));
      } catch (IOException e) {
        return false;
      }

      String line = null;

      try {
        while ((line = br.readLine()) != null) {
          bw.write(line);
          bw.newLine();
        }

        bw.close();
        br.close();
      } catch (IOException e) {
        return false;
      }
    }

    try {
      config.load(new FileInputStream(propFile));
    } catch (IOException e) {
      return false;
    }

    return true;
  }
}
