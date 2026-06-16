package com.notquests.paper.managers;

import org.bukkit.Bukkit;
import com.notquests.paper.NotQuests;

import java.text.DecimalFormat;

public class PerformanceManager {
  private final NotQuests main;

  private final boolean accurateTPS = true;

  // Inaccurate way of getting TPS through calculating tick times every time a task runs
  private final float tps = 20;
  private final DecimalFormat format = new DecimalFormat("##.##");
  private long lastMS = 0;
  private float tickCounter = 0;
  private long msPerTick = 50;
  private long msCounter = 0;

  public PerformanceManager(final NotQuests main) {
    this.main = main;

    if (!accurateTPS) {
      startMonitoringInaccurateTPS();
    }
  }

  // value of 0 will get the tps for the last minute, value of 1 will be 5min and 2 would be 15min
  public final double getTPSDouble(int time) {
    double[] tps = Bukkit.getTPS();
    return tps[time];
  }

  public final double getTPS() {
    return getTPSDouble(0);
  }

  // Inaccurate
  public void startMonitoringInaccurateTPS() {
    main.getMain()
        .getServer()
        .getScheduler()
        .scheduleSyncRepeatingTask(
            main.getMain(),
            () -> {
              long timeNow = System.currentTimeMillis();
              if (lastMS != 0) {
                msPerTick = (timeNow - lastMS);

                msCounter += msPerTick;
                tickCounter++;

                if (msCounter >= 1000) {
                  long aboveASecond = msCounter - 1000;

                  // tickCounter = tickCounter - (aboveASecond / tickCounter);

                  // System.out.println("TPS: " + tickCounter);

                  tickCounter = 0;
                  msCounter = 0;
                }
              }

              lastMS = timeNow;
            },
            1,
            1);
  }
}
