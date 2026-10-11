# メンテナー向け手順

## 公開前の確認

1. `scripts/prepare-dependencies.ps1` と `scripts/test.ps1 -Suite Unit` を確認します。
2. 明示的に実施する通信・サービス・実クライアントの確認を行います。通常の CI はこれらを起動しません。
3. 製品の版を `scripts/package-manager.ps1`、`installer/tinywin-nfs-server.iss`、README、CHANGELOG で揃えます。
4. `dependencies/components.json` の版、取得元、ハッシュ、対応ソースとライセンスを確認します。
5. `scripts/package-installer.ps1` で配布物を作成し、exe と `TinyWinNfsSources.zip` が揃ったことを確認します。
6. インストーラーへの署名はユーザーが手動で行います。署名処理はスクリプトや CI に追加しません。
7. 署名後に `Get-AuthenticodeSignature` と `Get-FileHash -Algorithm SHA256` で確認します。ソース ZIP のハッシュも記録します。
8. 動作確認・署名完了・公開指示を受けてから、commit、push、tag、release と asset の公開を行います。

インストーラーと対応ソース ZIP は同じリリースに添付し、リリースノートには双方のファイル名・SHA-256・runtime の版を日本語で記載します。
対応ソースが欠ける場合は公開を見送ります。app image の配布では `licenses`、`runtime/legal`、`sources` を維持してください。
過去の公開済み署名ファイルを再ビルドで置換せず、必要な修正は新しい版として確認・署名します。

## GitHub の設定

ローカル修正が確認され、リモート反映の明示指示がある場合に設定します。以下は設定案であり、自動で反映するスクリプトではありません。

| 項目 | 設定案 |
|---|---|
| リポジトリ説明 | Java 21で実装したWindows向けNFSv2/NFSv3サーバー。複数共有、読み書き、SWT管理ツール、Windowsサービスに対応。 |
| topics | `nfs`、`nfs-server`、`windows`、`java`、`swt`、`qnx` |
| 非公開脆弱性報告 | Settings の Advanced Security で Private vulnerability reporting を有効化 |
| secret scanning / push protection | 有効状態を維持 |
| CI | workflow 反映後にビルドと Unit テストが通ることを確認 |

非公開報告の有効化後、`SECURITY.md` の未有効化の説明を実際の状態に更新します。
参考サンプルは業務実装を含まない書式見本へ置き換えています。元の内容は過去の Git 履歴とタグに残るため、公開許諾の確認が必要な場合はメンテナーが判断してください。履歴を書き換える作業は別途明示的な合意のもとで行います。

## 保守

原則として最新の安定リリースを対象に、ベストエフォートで報告・修正を扱います。旧版の修正、応答期限、SLA は保証しません。
対応する機能と検証状況は [サポート範囲](support_scope.md)、脆弱性の報告は [SECURITY.md](../SECURITY.md)、依存の更新は [依存関係の管理](dependency_maintenance.md) を参照してください。
