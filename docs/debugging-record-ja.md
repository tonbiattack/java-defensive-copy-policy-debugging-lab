# デバッグ記録: unmodifiable view をポリシーのスナップショットと誤認する

## 対象の不具合

許可リージョンを受け取る `DeploymentPolicy` が `Collections.unmodifiableList(input)` を保持していた。呼び出し側が元の可変 `ArrayList` へリージョンを追加すると、ポリシー自身を変更していないにもかかわらず許可範囲が広がった。生成時点の許可リージョンを固定することが契約である。

| 観測点 | 期待値 | バグ状態の実際値 |
| --- | --- | --- |
| 境界出力 | `permits("us-east-1") == false` | `true` |
| 最終状態 | `approvedRegions == [eu-central-1]` | `[eu-central-1, us-east-1]` |
| 保持対象 | `eu-central-1` は許可、未設定リージョンは拒否 | いずれも正常 |

## 再現条件

バグ状態のコミットは `e8f3f82762a1c7e0655f70424c284a16300f0e71` です。

```bash
git checkout e8f3f82
./run-tests.sh
```

```text
Exception in thread "main" java.lang.AssertionError: 生成後に呼び出し側が入力Listを変更しても許可リージョンを増やしてはならない expected=false actual=true
    at lab.DeploymentPolicyTest.assertEquals(DeploymentPolicyTest.java:34)
    at lab.DeploymentPolicyTest.callerMutationDoesNotExpandAnExistingPolicy(DeploymentPolicyTest.java:19)
    at lab.DeploymentPolicyTest.main(DeploymentPolicyTest.java:8)
```

## 調査

| 確認対象 | 観測結果 | 判断 |
| --- | --- | --- |
| 入力 | constructor には可変 `ArrayList([eu-central-1])` を渡し、生成後に `us-east-1` を追加 | 外部から変更可能な参照を保持する再現条件。 |
| 境界出力 | `policy.permits("us-east-1")` が `true` | ポリシーの許可判定が外部変更に影響された。 |
| 最終状態 | `policy.approvedRegions()` は2要素になる | 判定だけでなく、保持している状態が変化した。 |
| 実装 | `Collections.unmodifiableList(approvedRegions)` は入力リストを包む view | constructor が独立コピーを作っていない。 |
| 仕様 | Oracle は `unmodifiableList` の戻り値を view とし、元のリストの変更は view から見えると説明する。`List.copyOf` は、可変の元コレクションから独立した unmodifiable copy を作る。[1] [2] | 直接原因と修正方針を確定。 |

## 原因

「API を通じてポリシーのリストを変更できない」ことと「ポリシーの状態が以後変わらない」ことは異なる。`Collections.unmodifiableList` は直接の変更操作を拒否する view だが、元の `ArrayList` を保持している呼び出し側は変更できる。結果として、外部の `input.add` がポリシーの状態へ伝播した。

## 修正

constructor の代入を `List.copyOf(approvedRegions)` に置き換えた。修正コミットは `37e25f79b738e48895dfa6b3bd71f6f8e6fa9f01` である。`List.copyOf` は可変コレクションから unmodifiable copy を作るため、元のリストに要素を足しても policy が保持するリストは変化しない。[1]

## 回帰確認

```bash
git checkout main
./run-tests.sh
```

```text
PASS: all tests
```

呼び出し側の追加後も `us-east-1` を拒否し、最終状態が `[eu-central-1]` にとどまることを確認した。加えて、生成時の許可リージョンと未設定リージョンの既存判定も成功した。

## 設計上の制約

`List.copyOf` は浅いコピーである。要素オブジェクト自体が可変なら、その内部状態の変更は共有されるため、このラボの防御範囲外である。また、null要素は受け付けない。可変要素やnullを許容する契約が必要なら、要素ごとのコピーや明示的な入力検証を別途設計する必要がある。
