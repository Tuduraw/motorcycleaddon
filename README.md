# Motorcycle Addon

Tudur's Vehicle Mod (`tudursvehiclemod`:https://github.com/Tuduraw/tudursvehiclemod) のアドオンMODのサンプルです。

二輪車を5ティア分追加し、アドオンから何がどこまでできるのかを実際に
動く形で示すことを目的としています。

**前提MODへのmixinは一切使っていません。** すべて前提MODが公開している
APIとオーバーライド可能なメソッドだけで実装しています。

---

## 1. 収録車両

| Tier | 車両 | 特徴 | 最大バンク角 |
|---|---|---|---|
| 1 | 自転車 | 履帯でチェーン、track_rollerでペダル、燃料不要 | 45° |
| 2 | スクーター | 直立気味、2人乗り | 18° |
| 3 | サイドカー | 3輪・機関銃つき | 0°(傾かない) |
| 4 | スポーツバイク | 同梱モデル、ウィリー可 | **60°** |
| 5 | ホバーバイク | 最速、ダクテッドファン、ウィリー可 | 70° |

全車とも、ハンドル・フロントフォーク・前輪が操舵に合わせて左右に
動きます(`steering_wheel_parts`と`wheel_parts`の`steer_angle`)。

---

## 2. 操作

| 操作 | キー | 内容 |
|---|---|---|
| コーナリングモード切替 | **M長押し** | 直立旋回 ⇔ バンク旋回 |
| アクロバット(ウィリー) | **X押しっぱなし** | 対応車両のみ |

### コーナリングモード

バンクとハンドルの転舵を同時に見せると、車体が内側に倒れながら
フォークが外側に切れて見え、視覚的な違和感が出ます。そこで両立させず、
乗り手が選ぶ形にしました。

- **モード1(直立)** — 傾かないので転舵がはっきり見えます。旋回速度に
  `upright_turn_penalty`が掛かり、**性能は劣ります**
- **モード2(バンク)** — 傾きあり、旋回性能は全開

前提MODの`MANUAL_MODE`(航空機のマニュアルモード)をそのまま流用して
います。同期・永続化・キー割当が既に揃っており、同じ概念に2つ目の
キーを増やすより自然なためです。**マニュアルモードOFFが直立**、
**ONがバンク**です。

傾きとウィリーはどちらも見た目のみで、移動計算には影響しません。
前提MODの`ShipEntity`のロールと同じ考え方です。

---

## 3. 機体ごとの設定

車両JSONの中に`motorcycle`オブジェクトを置きます。

```json
{
  "entity_type": "motorcycleaddon:motorcycle",
  "max_speed": 2.0,

  "motorcycle": {
    "max_lean_degrees": 60.0,
    "enable_acrobatics": true
  }
}
```

前提MODの`VehicleDefinition`は知らないキーを無視するため、**1台の車両を
1ファイルで記述**したまま両者が共存します。アドオン側は同じ
`data/<namespace>/vehicles/*.json`を自前のリロードリスナーで読み、
`motorcycle`キーだけを取り出しています。

| キー | 既定値 | 内容 |
|---|---|---|
| `max_lean_degrees` | 35.0 | 旋回時の最大バンク角。**0で傾き無効** |
| `lean_smoothing` | 0.15 | 傾きの追従の速さ(小さいほど緩慢) |
| `full_lean_speed` | 0.35 | この速度で最大まで傾く(blocks/tick) |
| `upright_turn_penalty` | 0.6 | 直立モード時の旋回速度倍率 |
| `enable_acrobatics` | false | ウィリーを許可するか |
| `wheelie_angle_degrees` | 35.0 | ウィリー時の機首上げ角 |
| `wheelie_rate` | 0.08 | ウィリーの立ち上がりの速さ |

`motorcycle`オブジェクトを持たない車両は既定値で動きます。

> **注意**: この読み込みはリソースパック/データパック側のみを対象と
> します。前提MODの外部`tudursvehiclemod-addons/`フォルダに置いた車両も
> 通常どおり走行できますが、設定は既定値になります。

---

## 4. 機関銃(Tier3)

サイドカーには機関銃を据え付けてあり、**武器ファイルをこのアドオンに
同梱**しています。

```
src/main/resources/assets/motorcycleaddon/weapons/sidecar_mg.txt
```

前提MODのMCHeli形式をそのまま使い、`Type = MachineGun2`(射手の視点に
追従)を指定しています。車両JSON側は`weapon_name`でこのファイル名を
参照します。

弾の飛び方・威力・連射速度などはこのファイルで調整できます。書式は
前提MODの`Readme_Weapon.txt`を参照してください。

### 発射音について

`Sound`は**拡張子・名前空間なしのファイル名**を指定します。このサンプル
には音声を同梱していないため、**そのままでは無音で発射されます**
(エラーにはなりません)。音を付けるには、以下に`ogg`を置いてください。

```
src/main/resources/assets/motorcycleaddon/sounds/sidecar_mg.ogg
```

---

## 5. 入手方法

2通りあります。

1. **クリエイティブタブ「バイク」** から取得
2. **乗り物変換ブロック** でベースアイテム(`vehicle_base_tN`)を変換 —
   ページを送ると「バイク」が選択肢に現れます

スポーンアイテムは**前提MODの`TieredVehicleSpawnerItem`そのもの**です。
右クリックすると前提MODの車両選択画面が開き、そのティア以下のバイクが
一覧・検索できます。選択画面のコードはこのアドオンには一切ありません。

---

## 6. ビルド方法

前提MODを先にローカルへ公開してください。

```bash
# 前提MOD側で
./gradlew publishToMavenLocal
```

その後、このアドオンをビルドします。

```bash
./gradlew build
```

`build/libs/`にjarが生成されます。前提MODのjarと一緒に`mods`フォルダへ
入れてください。

`gradle.properties`のMinecraft・Yarn・Fabric Loaderのバージョンは
**前提MODと完全に一致させる**必要があります。特にYarnマッピングがずれる
と、コンパイルが通っても実行時にメソッドが見つかりません。

---

## 7. 実装上の注意点

### 車両定義JSONのスキーマ

実装と突き合わせて確認した要点です。

- `weight_type`の有効値は `tank` / `car` / `unknown` のみ
- `wheel_parts`は `part`・`pivot_x`・`pivot_y`・`pivot_z` が必須
- `steering_wheel_parts`は同じ4つ＋`axis_*`・`max_angle`
- `weapons`の`offsets`は `x`・`y`・`z`・`mount_yaw`・`mount_pitch`
  (`offset_x`ではありません)
- `seats`は `name`・`offset_x/y/z` が必須(`driver`は任意)

### 変換対象の登録タイミング

`VehicleConverterTargets.register()`は**MOD初期化時のみ**呼んで
ください。変換ブロックは選択中のページを一覧内の位置として保存する
ため、途中で登録内容が変わると保存済みの選択がずれます。

---

## 8. ファイル構成

```
src/main/java/com/example/motorcycleaddon/
  MotorcycleAddon.java            エントリポイント・登録・受信
  asset/MotorcycleSettings.java   機体ごとの設定(レコード)
  asset/MotorcycleSettingsRegistry.java  車両JSONからの読み込み
  entity/MotorcycleEntity.java    バンク・モード・ウィリー
  item/MotorcycleItems.java       スポーンアイテム登録
  item/MotorcycleConverterTarget.java  変換対象の実装
  network/AcrobaticsPayload.java  アクロバットキーの同期

src/client/java/com/example/motorcycleaddon/client/
  MotorcycleAddonClient.java      描画登録・キー割当

src/main/resources/
  data/motorcycleaddon/vehicles/  車両定義5種
  assets/motorcycleaddon/weapons/ 機関銃定義
  assets/motorcycleaddon/models/obj/  モデル5種

```
