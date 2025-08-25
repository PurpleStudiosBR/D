package br.com.purplemc.purpleesconde.map;

import org.bukkit.Location;
import org.bukkit.World;

import java.util.List;

public class GameMap {

    private final String name;
    private final World world;
    private final Location waitingLobby;
    private final Location seekerSpawn;
    private final List<Location> hiderSpawns;
    private final Location barrierMin;
    private final Location barrierMax;

    public GameMap(String name, World world, Location waitingLobby, Location seekerSpawn,
                   List<Location> hiderSpawns, Location barrierMin, Location barrierMax) {
        this.name = name;
        this.world = world;
        this.waitingLobby = waitingLobby;
        this.seekerSpawn = seekerSpawn;
        this.hiderSpawns = hiderSpawns;
        this.barrierMin = barrierMin;
        this.barrierMax = barrierMax;
    }

    public String getName() {
        return name;
    }

    public World getWorld() {
        return world;
    }

    public Location getWaitingLobby() {
        return waitingLobby;
    }

    public Location getSeekerSpawn() {
        return seekerSpawn;
    }

    public List<Location> getHiderSpawns() {
        return hiderSpawns;
    }

    public Location getBarrierMin() {
        return barrierMin;
    }

    public Location getBarrierMax() {
        return barrierMax;
    }

    public boolean isInsideBarrier(Location location) {
        if (barrierMin == null || barrierMax == null) return true;
        if (location == null || location.getWorld() == null) return false;
        if (!location.getWorld().equals(world)) return false;
        if (waitingLobby != null && location.distanceSquared(waitingLobby) < 1) return true;
        double x = location.getX();
        double z = location.getZ();
        return x >= Math.min(barrierMin.getX(), barrierMax.getX())
                && x <= Math.max(barrierMin.getX(), barrierMax.getX())
                && z >= Math.min(barrierMin.getZ(), barrierMax.getZ())
                && z <= Math.max(barrierMin.getZ(), barrierMax.getZ());
    }
}