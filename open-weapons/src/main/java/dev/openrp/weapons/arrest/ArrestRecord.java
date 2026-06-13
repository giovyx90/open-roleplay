package dev.openrp.weapons.arrest;

import java.time.Instant;
import java.util.UUID;

public class ArrestRecord {
    private final UUID playerUuid;
    private final String playerName;
    private final UUID officerUuid;
    private final String officerName;
    private final String jailRegionId;
    private final String reason;
    private final double jailTimeHours;
    private final double bailAmount;
    private final Instant arrestTime;
    private final Instant releaseTime;

    public ArrestRecord(UUID playerUuid, String playerName, UUID officerUuid, String officerName,
                        String jailRegionId, String reason, double jailTimeHours, double bailAmount) {
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.officerUuid = officerUuid;
        this.officerName = officerName;
        this.jailRegionId = jailRegionId;
        this.reason = reason;
        this.jailTimeHours = jailTimeHours;
        this.bailAmount = bailAmount;
        this.arrestTime = Instant.now();
        this.releaseTime = Instant.now().plusSeconds((long) (jailTimeHours * 3600));
    }

    // Full constructor for DB retrieval
    public ArrestRecord(UUID playerUuid, String playerName, UUID officerUuid, String officerName,
                        String jailRegionId, String reason, double jailTimeHours, double bailAmount,
                        Instant arrestTime, Instant releaseTime) {
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.officerUuid = officerUuid;
        this.officerName = officerName;
        this.jailRegionId = jailRegionId;
        this.reason = reason;
        this.jailTimeHours = jailTimeHours;
        this.bailAmount = bailAmount;
        this.arrestTime = arrestTime;
        this.releaseTime = releaseTime;
    }

    public UUID getPlayerUuid() { return playerUuid; }
    public String getPlayerName() { return playerName; }
    public UUID getOfficerUuid() { return officerUuid; }
    public String getOfficerName() { return officerName; }
    public String getJailRegionId() { return jailRegionId; }
    public String getReason() { return reason; }
    public double getJailTimeHours() { return jailTimeHours; }
    public double getBailAmount() { return bailAmount; }
    public Instant getArrestTime() { return arrestTime; }
    public Instant getReleaseTime() { return releaseTime; }

    public boolean isExpired() {
        return Instant.now().isAfter(releaseTime);
    }

    public long getRemainingSeconds() {
        long remaining = releaseTime.getEpochSecond() - Instant.now().getEpochSecond();
        return Math.max(0, remaining);
    }

    public String getRemainingFormatted() {
        long totalSeconds = getRemainingSeconds();
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        if (hours > 0) {
            return hours + "h " + minutes + "m " + seconds + "s";
        } else if (minutes > 0) {
            return minutes + "m " + seconds + "s";
        } else {
            return seconds + "s";
        }
    }
}
