package lab;

import java.util.List;

public final class DeploymentPolicy {
    private final List<String> approvedRegions;

    public DeploymentPolicy(List<String> approvedRegions) {
        this.approvedRegions = List.copyOf(approvedRegions);
    }

    public boolean permits(String region) {
        return approvedRegions.contains(region);
    }

    public List<String> approvedRegions() {
        return approvedRegions;
    }
}
