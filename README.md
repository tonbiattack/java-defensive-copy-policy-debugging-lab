# Java defensive-copy policy debugging lab

`Collections.unmodifiableList` を入力リストへ適用しただけでは、呼び出し側が元の可変リストを変更してポリシーの許可範囲を広げられる不具合を再現します。

## 前提

- Java 21 以上
- 外部ライブラリは不要

## 実行

```bash
./run-tests.sh
```

バグ状態では、生成後に呼び出し側が `us-east-1` を入力リストへ追加すると、既存のポリシーもそのリージョンを許可します。修正後は生成時のスナップショットが保持されます。
