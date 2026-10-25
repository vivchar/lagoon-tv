# Установка на стик

Xiaomi TV Stick 4K, Android 11. Подключение по сети:
«Настройки» → «Об устройстве» → 7 раз по «Сборка», затем
«Для разработчиков» → «Отладка по USB» и «Отладка по сети».

```bash
adb connect <ip-стика>:5555
adb -s <ip-стика>:5555 install -r app/build/outputs/apk/debug/app-debug.apk
```

## Разрешения через ADB

Выдаются один раз. Без них соответствующая функция показывает диалог
с этими же командами.

```bash
P=io.github.vivchar.lagoon
adb shell pm grant $P android.permission.READ_LOGS               # кнопки пульта (Netflix, Prime Video)
adb shell pm grant $P android.permission.WRITE_SECURE_SETTINGS   # выбрать заставку
adb shell appops set $P WRITE_SETTINGS allow                     # время до заставки
adb shell appops set $P SYSTEM_ALERT_WINDOW allow                # яркость, «Ночь», быстрая панель
adb shell appops set $P GET_USAGE_STATS allow                    # порядок дока по времени использования
```

Без `GET_USAGE_STATS` док сортируется по своим счётчикам запусков.

## Лаунчер по умолчанию

Нажать Home и выбрать Lagoon TV. На прошивках, где выбора нет, стоковый
лаунчер можно отключить:

```bash
adb shell pm disable-user --user 0 com.google.android.tvlauncher
```

Осторожно: на части прошивок от этого ломается «Ok Google»
(см. [launchers.md](launchers.md)). Вернуть — `pm enable`.

## Заставка

После выдачи `WRITE_SECURE_SETTINGS` в плитке «Заставка» есть кнопка
«Сделать заставкой ТВ». Вручную:

```bash
adb shell settings put secure screensaver_components io.github.vivchar.lagoon/.HomeDream
adb shell settings put secure screensaver_enabled 1
```

Фото берутся из общего альбома iCloud: ссылку на альбом отправляют боту.

## Обновление

`install -r` сохраняет настройки и разрешения. Смена `applicationId` —
это новое приложение: разрешения, заставку и лаунчер придётся настроить
заново, а данные (бот, альбом, календарь, город) ввести снова.
