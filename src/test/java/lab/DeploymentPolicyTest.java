package lab;

import java.util.ArrayList;
import java.util.List;

public final class DeploymentPolicyTest {
    public static void main(String[] args) {
        callerMutationDoesNotExpandAnExistingPolicy();
        configuredRegionsRemainPermitted();
        System.out.println("PASS: all tests");
    }

    static void callerMutationDoesNotExpandAnExistingPolicy() {
        List<String> input = new ArrayList<>(List.of("eu-central-1"));
        DeploymentPolicy policy = new DeploymentPolicy(input);

        input.add("us-east-1");

        assertEquals(false, policy.permits("us-east-1"),
                "生成後に呼び出し側が入力Listを変更しても許可リージョンを増やしてはならない");
        assertEquals(List.of("eu-central-1"), policy.approvedRegions(),
                "ポリシーの最終状態は生成時のスナップショットを保持する");
    }

    static void configuredRegionsRemainPermitted() {
        DeploymentPolicy policy = new DeploymentPolicy(new ArrayList<>(List.of("eu-central-1")));

        assertEquals(true, policy.permits("eu-central-1"), "生成時に設定したリージョンは許可する");
        assertEquals(false, policy.permits("ap-northeast-1"), "設定していないリージョンは許可しない");
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (!expected.equals(actual)) {
            throw new AssertionError(message + " expected=" + expected + " actual=" + actual);
        }
    }
}
