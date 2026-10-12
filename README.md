# TeamPing

Postaw znacznik, a każdy z twojej ekipy na tym samym kanale 
zobaczy go na ekranie z nazwą, główką i odległością.

Obsługiwane wersje Minecraft: **1.21.4, 1.21.8, 1.21.11** (osobny .jar na każdą).

## Budowanie

Wymaga JDK 21.

```
cp gradle-1.21.8.properties gradle.properties   # albo 1.21.4 / 1.21.11
./gradlew build
```

Gotowy jar: `dist/teamping-<wersja_mc>-<wersja>.jar`.

## Użycie

Wrzuć jar odpowiedni dla swojej wersji gry **oraz [Fabric API](https://modrinth.com/mod/fabric-api)**
do folderu `mods/`. W grze:

- `V` — postaw ping,
- `0` — otwórz gui.

Ustawienia zapisują się w `config/teamping/config.json`.

## Licencja

Prawa zastrzeżone — patrz [LICENSE](LICENSE). Kod jest publiczny do wglądu; modyfikacja
i redystrybucja wyłącznie za zgodą autorów.
