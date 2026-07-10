package com.ansh9boss.pvpbot.npc;

public class PlayerSessionStats {

    private int hitsLanded = 0;
    private int hitsTaken = 0;
    private int currentStreak = 0;
    private int longestStreak = 0;

    public void incrementHitsLanded() {
        this.hitsLanded++;
        this.currentStreak++;
        if (this.currentStreak > this.longestStreak) {
            this.longestStreak = this.currentStreak;
        }
    }

    public void incrementHitsTaken() {
        this.hitsTaken++;
        this.currentStreak = 0; // Reset streak on being hit
    }

    public int getHitsLanded() {
        return hitsLanded;
    }

    public int getHitsTaken() {
        return hitsTaken;
    }

    public int getCurrentStreak() {
        return currentStreak;
    }

    public int getLongestStreak() {
        return longestStreak;
    }

    public void reset() {
        this.hitsLanded = 0;
        this.hitsTaken = 0;
        this.currentStreak = 0;
        this.longestStreak = 0;
    }
}
