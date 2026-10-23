# Чужие лаунчеры и инструменты: идеи для lagoon-tv

Подборка готовых решений для Android TV, чтобы смотреть идеи. Собрано 23.10.2026.
Звёзды и описания взяты со страниц репозиториев на эту дату.

## Мне нравится дизайн

- **AT4K**: https://github.com/avadhesh18/at4k. Минимализм в стиле Apple TV: сетка
  приложений, папки, без рекламы.
- **tv-home, экран «после»** (правая половина картинки):
  https://github.com/mihirgupta0900/tv-home/blob/main/before-after.png
  - Фото природы во весь экран, обои меняются.
  - Слева вверху мелкое приветствие «Good afternoon», крупные тонкие часы, ниже
    дата мелкими разреженными капсами.
  - Справа вверху полупрозрачные «пилюли» Inputs, Wi-Fi, Settings.
  - Внизу док в полупрозрачной скруглённой подложке: пять крупных прямоугольных
    баннеров приложений. У баннера под фокусом обводка.
  - Над доком крупно название приложения под фокусом («Apple TV»).
  - Больше ничего: ни рядов с контентом, ни рекламы.

## Скиллы и плагины для агентов (Claude Code и т. п.)

| Что | Ссылка | Чем интересно |
|---|---|---|
| cleanyourtv | https://github.com/Gathin23/cleanyourtv | Уже клонирован в `../cleanyourtv`. Промпт, debloat-списки для десятков ТВ, лаунчер в стиле Apple TV на 280 КБ, темы без пересборки |
| tv-makeover | https://github.com/paisanos-io/tv-makeover | Плагин Claude Code поверх cleanyourtv: шаблоны лаунчеров (DreamOS, starter, extreme), перенос дизайна из Figma, звуки интерфейса |
| tv-home | https://github.com/mihirgupta0900/tv-home | Один промпт: debloat и лаунчер с доком, сеткой по «вниз» и меняющимися обоями природы. Есть `tools/type.sh` для ввода текста на ТВ с ноутбука |
| TV Box Tools | https://github.com/smartago/tvboxtools-plugin | MCP-сервер: найти приставку в сети, подключиться, поставить лаунчер, сделать HOME, проверить после перезагрузки, скриншот |
| clawtv | https://github.com/PhatStraw/clawtv | CLI над ADB и скилл: Claude смотрит скриншоты ТВ и сам находит нужный сериал |
| android-tv-development | https://github.com/majiayu000/claude-skill-registry/tree/main/skills/other/android-tv-development | Скилл про фокус и D-pad в Compose for TV |
| RNTV skills | https://github.com/react-native-tvos/skills | Скиллы для React Native под ТВ, нам не по стеку |

Статья с готовыми промптами: https://princejain.me/blogs/debloat-android-tv-custom-launcher-claude-code/
Две ловушки оттуда: стоковый лаунчер так просто не заменить, а его отключение
может сломать «Ok Google».

## Open source лаунчеры (код можно читать)

| Что | Ссылка | Стек | Идеи |
|---|---|---|---|
| Tarang | https://github.com/mohitkyadav/tarang-launcher | Kotlin, Compose for TV | В стиле tvOS: стеклянный док через AGSL-шейдер, режим «картины» (слайдшоу и часы), постер под фокусом как обои, три вида анимаций |
| BareLauncher | https://github.com/f102mithunysypdlcjr-pixel/BareLauncherv3 | Чистая Java, без AndroidX | Ближе всего к нашему стеку. 140 КБ, своя «полка» вместо RecyclerView, кэш холодного старта, кнопки пульта на приложения. Лицензия некоммерческая |
| Couchy | https://github.com/conreo/couchy-launcher | — | Один экран, три раскладки (карусель, сетка, стеклянный док), видео-обои и Aerial, экспорт настроек в JSON |
| LTvLauncher | https://github.com/LeanBitLab/LtvLauncher | Flutter, форк FLauncher | Погода, OLED-заставка, переключение входов, «продолжить просмотр» |
| PitchforkLauncher | https://github.com/yoramvandevelde/pitchforklauncher | Flutter, форк FLauncher | Почти весь написан Claude. Перехват Home через Accessibility, кнопки пульта на приложения, переключение входа ТВ долгим «вверх» |
| FLauncher | https://gitlab.com/flauncher/flauncher | Flutter | Родоначальник форков. Форк с фиксами: https://github.com/osrosal/flauncher |
| aLauncher | https://github.com/4v3ngR/aLauncher | Flutter, форк FLauncher | Упрощённый, без Firebase |
| LeanbackLauncher | https://github.com/tsynik/LeanbackLauncher | Java | Стоковый лаунчер Google, вынутый из прошивки |
| AndroidTVLauncher | https://github.com/jackyandroid/androidtvlauncher | Java, Leanback | Старый, 3 тыс. звёзд |
| LauncherCompose | https://github.com/karuhun-developer/LauncherCompose | Kotlin, Compose | Лаунчер для отелей |
| LauncherTV | https://github.com/alescdb/launchertv | Java | Простой, есть в Play |
| LetiHome | https://github.com/pkoretic/LetiHome | Qt | Около 700 строк, для слабых приставок |

## Закрытые, но стоит посмотреть, как выглядят

| Что | Ссылка | Идеи |
|---|---|---|
| Projectivy | https://play.google.com/store/apps/details?id=com.spocky.projengmenu, трекер: https://github.com/spocky/miproja1 | Самый популярный. Видео- и GIF-обои, плагины обоев, цвета от обоев, тестовые таблицы для калибровки |
| Monet | https://github.com/Klevico/Monet-Launcher | Интерфейс красится в цвета обоев, виджеты погоды и «сейчас играет», заставка, профили |
| AT4K | https://github.com/avadhesh18/at4k | В стиле Apple TV, папки, карточки «продолжить» |
| Premium TV Launcher UI | https://pluitv.com/us/compare/ | Стекло поверх обоев, 82 вида погоды, камеры на главном экране. Там же таблица сравнения пяти лаунчеров |
| Wolf Launcher | https://play.google.com/store/apps/details?id=com.wolf.firelauncher | Простой и лёгкий |
| ATV Launcher | https://play.google.com/store/apps/details?id=ca.dstudio.atvlauncher.free | Для слабых приставок |
| Arc, Dispatch, ChillHub, HALauncher, Super Smart TV | ссылок нет | Упоминаются в обзорах. Arc — open source форк LTvLauncher, Dispatch показывает библиотеку Plex, ChillHub похож на Apple TV |

## Обзоры

- https://www.androidauthority.com/install-google-tv-launcher-alternative-3663642/
- https://www.makeuseof.com/best-google-tv-launchers-to-try/
- https://unstore.io/discover/best-apps-for-android-tv-launcher/
- https://cleanyourtv.org/open-source — список open source лаунчеров и блокировщиков рекламы

## Что взять в lagoon-tv

- Режим «картины» и стеклянный док из Tarang.
- Кэш первого кадра и своя лёгкая полка из BareLauncher (тот же стек: Java без библиотек).
- Кнопки пульта на приложения: BareLauncher, PitchforkLauncher.
- Цвета интерфейса от обоев: Monet, Projectivy.
- Ввод текста на ТВ с ноутбука: `tools/type.sh` из tv-home.
