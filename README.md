# Michibiki Compass

[![Download APK](https://img.shields.io/badge/Download-APK-ffffff?style=for-the-badge&logo=android&logoColor=000000)](https://github.com/Maslodium/michibiki-compass/releases/latest/download/Michibiki.apk)

**[Download the latest APK](https://github.com/Maslodium/michibiki-compass/releases/latest/download/Michibiki.apk)** · [English](#english) · [Русский](#русский)

> Early prototype. Michibiki is not yet a turn-by-turn navigator and must not be relied on for safety-critical navigation.

## English

Michibiki is a privacy-first waypoint compass for Nothing phones. Choose a destination and see its direction and straight-line distance in the app, a 4×2 widget, or on the Glyph Matrix—without keeping a full map open.

### Features

- voice or text destination search with a choice between ambiguous results;
- point selection on the built-in map;
- separate controls for GPS and network location;
- independent north marker and destination direction;
- GNSS course while moving, filtered compass while stationary;
- rejection of sudden sensor spikes;
- orientation-aware live home-screen and lock-screen 4×2 widget with manual refresh and an optional foreground location service;
- one visual language across the app, widget, and Glyph Matrix: an A-shaped destination arrow, an arc with `N` for north, and side half-arrows for detours;
- synchronized long-press obstacle/detour mode; Glyph elements use shape—not brightness—to convey meaning;
- native rendering for both supported Glyph Matrix sizes.

### Glyph compatibility

| Device | Matrix | Support |
|---|---:|---|
| Nothing Phone (3) | 25×25 | Interactive Glyph Toy, including long press |
| Nothing Phone (4a) Pro | 13×13 | AOD-compatible display with simplified artwork |
| Phone (1), Phone (2), Phone (2a), Phone (3a) series | LED strips | Not yet supported; these require the classic Glyph SDK |

Obstacle mode draws the boundary near the outer edge and rotates it independently from the destination arrow. Until map/routing geometry is implemented, the wall is estimated as perpendicular to the direct bearing to the destination.

### Privacy

Navigation state is stored locally. Michibiki contains no analytics, advertising, or tracking backend. Android's geocoder, speech recognizer, map tiles, and network-location provider may contact their respective service providers when enabled.

## Русский

Michibiki — приватный компас-навигация для телефонов Nothing. Выберите точку назначения и смотрите направление и расстояние по прямой в приложении, виджете 4×2 или на Glyph Matrix, не открывая каждый раз полноценную карту.

### Возможности

- поиск адреса голосом или текстом с выбором из неоднозначных результатов;
- выбор точки на встроенной карте;
- отдельные переключатели GPS и сетевого местоположения;
- независимые указатели севера и направления к цели;
- курс GNSS при движении и отфильтрованный компас на месте;
- отсечение резких скачков датчика;
- живой виджет 4×2 для рабочего стола и экрана блокировки, учитывающий поворот телефона: ручное обновление и отключаемый foreground-сервис;
- единый визуальный язык в приложении, виджете и Glyph Matrix: A-образная стрелка цели, дуга с `N` для севера и боковые полустрелки обхода;
- синхронизированный режим препятствия/обхода по долгому нажатию; на Glyph элементы различаются формой, а не яркостью;
- нативный рендер для обеих поддерживаемых Glyph Matrix.

### Совместимость Glyph

| Устройство | Матрица | Поддержка |
|---|---:|---|
| Nothing Phone (3) | 25×25 | Интерактивный Glyph Toy, включая долгое нажатие |
| Nothing Phone (4a) Pro | 13×13 | Режим AOD с упрощённой и читаемой графикой |
| Phone (1), Phone (2), Phone (2a), Phone (3a) | Светодиодные полосы | Пока не поддерживаются: для них нужен отдельный классический Glyph SDK |

В режиме препятствия граница рисуется у внешнего края и поворачивается независимо от стрелки к цели. Пока геометрия препятствий из карт или роутинга не подключена, стена считается перпендикулярной прямому направлению к точке.

### Приватность

Состояние навигации хранится локально. В Michibiki нет аналитики, рекламы и собственного трекингового сервера. При включении соответствующих функций системный геокодер Android, распознавание речи, сервер карт и провайдер сетевого местоположения могут обращаться к своим поставщикам.

## Screenshots / Скриншоты

| Compass / Компас | Point picker / Выбор точки | Settings / Настройки |
|---|---|---|
| ![Compass](docs/screenshots/compass.png) | ![Point picker](docs/screenshots/map.png) | ![Settings](docs/screenshots/settings.png) |

![Glyph obstacle preview](docs/screenshots/glyph-obstacle-preview.png)

## Build / Сборка

JDK 17 and Android SDK 35 are required. / Нужны JDK 17 и Android SDK 35.

1. Download `glyph-matrix-sdk-2.0.aar` from Nothing's official [Glyph Matrix Developer Kit](https://github.com/Nothing-Developer-Programme/GlyphMatrix-Developer-Kit).
2. Put it at `app/libs/glyph-matrix-sdk-2.0.aar`.
3. Create `local.properties` with your Android SDK path.
4. Run `./gradlew assembleDebug` or `gradlew.bat assembleDebug` on Windows.

The proprietary SDK AAR is excluded from this repository under Nothing's separate licence. / Проприетарный AAR SDK не включён в репозиторий из-за отдельной лицензии Nothing. See [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).

## Licence / Лицензия

No open-source licence has been granted for Michibiki yet. / Открытая лицензия для Michibiki пока не выбрана.
