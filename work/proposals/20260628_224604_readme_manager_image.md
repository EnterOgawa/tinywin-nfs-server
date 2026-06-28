# README管理画面画像差し替え 提案書

## 変更対象

| ファイル | 変更内容 |
|---|---|
| `docs/images/tinywin-nfs-manager-ja.png` | README掲載用の日本語管理画面スクリーンショットを v2.0.0 相当の画面に差し替え |

## 変更内容

README の画像参照先は既存のまま維持します。

```diff
![TinyWinNFS Server マネージャー](docs/images/tinywin-nfs-manager-ja.png)
```

画像の内容を、診断タブと許可クライアント列が反映された新しい管理画面へ更新します。

## 確認方針

- `README.md` の画像参照先が変わっていないこと。
- `docs/images/tinywin-nfs-manager-ja.png` が更新されていること。
- `git diff --check` でテキスト差分に問題がないこと。
