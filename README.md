# PvP Macro (Fabric, client-side, Minecraft 1.21.11)

Tahap 1: **MacroRunner** + **Spear Attribute Swap**. Fitur lain belum dibuat (tidak ada placeholder).

## Build
Syarat: JDK 21, Gradle 9.2.1 (atau lebih baru yang didukung Loom), internet.

1. Cek `gradle.properties` terhadap https://fabricmc.net/develop (pilih 1.21.11):
   `loader_version`, `loom_version`, `fabric_api_version`. Nilai di file ini berasal dari
   setup komunitas dan **belum saya verifikasi** terhadap Maven Fabric.
2. Buat wrapper (jar wrapper tidak bisa saya buat tanpa jaringan):
   `gradle wrapper --gradle-version 9.2.1`
3. `./gradlew build` -> jar ada di `build/libs/pvpmacro-0.1.0.jar`
   Test dev: `./gradlew runClient`
4. Taruh jar di `mods/` bersama Fabric API 1.21.11 dan Fabric Loader.

Error kompilasi yang paling mungkin adalah nama API Yarn. Tempatnya ditandai `// VERIFY`
di `mc/MinecraftEnvironment.java` dan `mc/InputGuard.java`. Kirim error-nya, akan saya perbaiki.

## Test logika (tanpa Minecraft)
```
java -m jdk.compiler/com.sun.tools.javac.Main -d /tmp/out \
  src/main/java/dev/pvpmacro/core/*.java src/main/java/dev/pvpmacro/config/*.java \
  src/main/java/dev/pvpmacro/macro/spear/*.java src/test/java/dev/pvpmacro/SelfTest.java
java -cp /tmp/out dev.pvpmacro.SelfTest
```
(Atau `javac` biasa.) Test ini hanya memeriksa penjadwalan MacroRunner dan logika Attribute Swap,
bukan perilaku asli Minecraft/server.

## Cara pakai
- Tombol **Spear Attribute Swap** (default `V`, ubah di Options > Controls > PvP Macro).
- Siapkan di hotbar: spear ber-Lunge, dan item swap (tangan kosong / steak / wind charge).
- Tahan tombol: park di item swap -> [spear -> jab -> item swap] diulang -> lepas tombol = kembali ke slot awal.
- Perintah: `/pvpmacro reload | toggle | status`. Tombol "Toggle all macros" tidak punya default key.
- Macro dibatalkan (slot dikembalikan) saat GUI/chat terbuka, window kehilangan fokus, game pause, atau mati.

## Config: `config/pvpmacro.json`
Edit lalu `/pvpmacro reload`. Nilai di luar batas dikoreksi otomatis. File rusak disimpan sebagai `.bad`.

| Field | Default | Arti |
|---|---|---|
| `masterEnabled` | true | saklar semua macro |
| `feedback` | true | pesan action bar |
| `attributeSwap.enabled` | true | |
| `spearSlot` | 0 | 0 = auto (spear Lunge tertinggi), 1-9 = slot tetap |
| `carrierSlot` | 0 | 0 = auto dari `carrierItems`, 1-9 = slot tetap |
| `carrierItems` | air, cooked_beef, wind_charge | prioritas item swap; `minecraft:air` = slot kosong |
| `requireLunge` | true | hanya spear ber-Lunge |
| `holdToRepeat` | true | false = satu jab per tekan |
| `parkTicks` | 2 | tick di item swap sebelum jab pertama (1-20) |
| `repeatIntervalTicks` | 5 | jeda antar jab (2-100) |
| `jabDelayTicks` | 0 | jeda pilih spear -> jab (0 = tick yang sama) |
| `returnDelayTicks` | 0 | jeda jab -> kembali ke item swap |
| `swapBackToOriginal` | true | kembali ke slot awal di akhir |
| `restoreDelayTicks` | 1 | tick di item swap sebelum kembali ke slot awal (1-20) |
| `maxJabs` | 0 | 0 = tanpa batas |
| `minFoodLevel` | 0 | berhenti jika lapar di bawah ini (Lunge menguras hunger) |

## Checklist uji in-game (BELUM dilakukan)
1. Mod load tanpa crash; kategori "PvP Macro" muncul di Controls.
2. Tanpa spear di hotbar: muncul pesan, tidak ada input.
3. Satu jab (`holdToRepeat=false`): slot berpindah item swap -> spear + jab -> item swap -> slot awal.
4. Lunge benar-benar dash pada jab pertama dan jab berulang? Bandingkan dengan klik biasa.
5. Apakah cooldown Lunge benar-benar terlewati? Jika tidak, coba naikkan `parkTicks`,
   ganti item swap (tangan kosong/wind charge), atau atur `jabDelayTicks`/`returnDelayTicks`.
6. Buka chat/inventory/alt-tab saat macro jalan: macro berhenti dan slot kembali.
7. Cek di server dengan anti-cheat: perilaku ini bisa ditandai.
