# Проверка

Юнит-тестов нет. Правку собирают, ставят на эмулятор Android TV и смотрят
скриншот, а для кнопок пульта и производительности — на стике.

## Эмулятор

Нужен AVD Android TV 1080p, API 30 (как стик). Сборка и запуск:

```bash
./gradlew assembleDebug
adb -s emulator-5554 install -r app/build/outputs/apk/debug/app-debug.apk
adb -s emulator-5554 shell am start -n io.github.vivchar.lagoon/.MainActivity
adb -s emulator-5554 exec-out screencap -p > shot.png
```

Пульт с компьютера:

```bash
adb -s emulator-5554 shell input keyevent DPAD_DOWN   # UP, LEFT, RIGHT, DPAD_CENTER, BACK, HOME
```

Если подключён и стик, всегда пиши `-s`.

## Отладочные флаги

Флаги интента `MainActivity` (`testing()`), работают и при повторном запуске.
Только в debug-сборке (`BuildConfig.DEBUG`): activity экспортирована, release
флаги игнорирует. Неверное значение пишется в лог `Home` и пропускается:

| Флаг | Что делает |
|---|---|
| `--es phase morning\|day\|sunset\|night` | Закрепляет время сцены: 7:10, 13:40, 18:05, 22:30 |
| `--es style painted\|pictures\|video` | Стиль сцены |
| `--ez bare true` | Прячет интерфейс, остаётся только сцена |
| `--ef demo 120` | Сутки в 120 раз быстрее, с 05:00 |
| `--ez saver true` | Открывает заставку |
| `--es page scene\|calendar\|places\|telegram` | Сразу открывает страницу настроек |

```bash
adb -s emulator-5554 shell am start -n io.github.vivchar.lagoon/.MainActivity --es phase sunset --es style pictures
```

## Моки для скриншотов

`tools/mock_data.py` кладёт в debug-сборку на эмуляторе выдуманные события
календаря и сообщения-напоминания от «Ани» и «Макса»:

```bash
python3 tools/mock_data.py
```

Скриншоты в `docs/screenshots/` сняты так:

1. `python3 tools/mock_data.py`
2. `am start … --es phase sunset|day|night`, `screencap`
3. `magick shot.png -strip -resize 1600x -quality 82 docs/screenshots/<имя>.jpg`

Перед коммитом скриншота проверь, что на нём нет настоящих данных: имён,
сообщений, названий Wi-Fi и VPN, установленных приложений. Лишнее —
замазать (`magick … -region WxH+X+Y -blur 0x12`).

## Логи

| Тег | Что |
|---|---|
| `LagoonScene` | Сцена и видео, fps |
| `Home` | Главный экран |
| `Weather`, `Cal`, `Saver`, `Telegram` | Сеть и данные |
| `NoteMedia` | Медиа заметок |
| `RemoteKeys` | Кнопки пульта из logcat |

```bash
adb logcat -s LagoonScene Home Weather Telegram Cal Saver NoteMedia RemoteKeys
```

## Что проверить руками

- Фокус: на каждом новом элементе стрелки ведут туда, куда ожидаешь,
  Back возвращает, фокус виден.
- Без интернета: строка «Нет интернета», остаются кэши.
- Время суток: `--ef demo 120` и посмотреть смену подложек и клипов.
- Заставка: «Показать сейчас» в плитке «Заставка» или `--ez saver true`.
- Кнопки Netflix и Prime Video — только на стике.
