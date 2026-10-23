#!/usr/bin/env python3
"""Fills a debug build on the emulator with made-up calendar events and Telegram notes, for screenshots.

    python3 tools/mock_data.py              # first emulator from `adb devices`
    python3 tools/mock_data.py emulator-5556

Only for an emulator: it overwrites the app's calendar and telegram preferences (run-as works on debug builds).
Times are taken from the device clock, so the events stay "today" and the notes look a few minutes to hours old.
"""
import json
import subprocess
import sys
import tempfile
from datetime import datetime, timedelta, timezone
from xml.sax.saxutils import escape

PKG = "io.github.vivchar.lagoon"


def adb(serial, *args):
    return subprocess.run(["adb", "-s", serial, *args], check=True, capture_output=True, text=True).stdout


def serial_arg():
    if len(sys.argv) > 1:
        return sys.argv[1]
    for line in subprocess.run(["adb", "devices"], capture_output=True, text=True).stdout.splitlines():
        if line.startswith("emulator-") and line.endswith("device"):
            return line.split()[0]
    sys.exit("no emulator running")


def argb(c):
    """Java int from 0xAARRGGBB."""
    return c - (1 << 32) if c >= 1 << 31 else c


def prefs(values):
    rows = []
    for k, v in values.items():
        if isinstance(v, int):
            rows.append(f'    <long name="{k}" value="{v}" />')
        else:
            rows.append(f'    <string name="{k}">{escape(v, {chr(34): "&quot;"})}</string>')
    return "<?xml version='1.0' encoding='utf-8' standalone='yes' ?>\n<map>\n" + "\n".join(rows) + "\n</map>\n"


def main():
    serial = serial_arg()
    now_s = int(adb(serial, "shell", "date", "+%s").strip())
    offset = adb(serial, "shell", "date", "+%z").strip()  # +0700
    tz = timezone(timedelta(hours=int(offset[:3]), minutes=int(offset[0] + offset[3:])))
    now = datetime.fromtimestamp(now_s, tz)
    midnight = now.replace(hour=0, minute=0, second=0, microsecond=0)

    def at(minutes_from_now):
        """An event time later today, at least a few minutes ahead, rounded to a quarter hour."""
        t = now + timedelta(minutes=minutes_from_now)
        t = t.replace(minute=t.minute // 15 * 15, second=0)
        return min(t, midnight + timedelta(hours=23, minutes=30))

    def ms(t):
        return int(t.timestamp() * 1000)

    yoga, call = at(40), at(115)
    events = [
        dict(start=ms(midnight), end=ms(midnight + timedelta(days=1)), allDay=True, important=True,
             title="День рождения Ани", color=argb(0xFFE57399)),
        dict(start=ms(yoga), end=ms(yoga + timedelta(hours=1)), allDay=False, important=True,
             title="Йога на пляже", color=argb(0xFF7FD1AE)),
        dict(start=ms(call), end=ms(call + timedelta(minutes=30)), allDay=False, important=True,
             title="Созвон с родителями", color=argb(0xFF8EC9FF)),
    ]
    sources = [dict(url="https://example.com/family.ics", name="Семья", color=argb(0xFF8EC9FF), shown=True)]
    calendar = prefs({
        "at": now_s * 1000,  # fresh, so the app doesn't try to fetch example.com
        "day": now.strftime("%Y%m%d"),
        "today": json.dumps(events, ensure_ascii=False),
        "sources": json.dumps(sources, ensure_ascii=False),
    })

    notes = [  # oldest first, as the app stores them
        (1, "Аня", 200, "Не забудь: в субботу в 10:00 забираем торт к празднику"),
        (2, "Макс", 48, "Напоминание: завтра вывоз мусора, вынести пакеты до 8 утра"),
        (1, "Аня", 9, "Полить цветы и закрыть окна — обещали грозу ночью"),
    ]
    messages = [dict(id=100 + i, **{"from": who}, date=now_s - ago * 60, name=name, text=text)
                for i, (who, name, ago, text) in enumerate(notes)]
    telegram = prefs({
        "messages": json.dumps(messages, ensure_ascii=False),
        "allowed": json.dumps([dict(id=1, name="Аня"), dict(id=2, name="Макс")], ensure_ascii=False),
    })

    adb(serial, "shell", "am", "force-stop", PKG)
    for name, body in (("calendar", calendar), ("telegram", telegram)):
        with tempfile.NamedTemporaryFile("w", suffix=".xml", delete=False) as f:
            f.write(body)
        adb(serial, "push", f.name, f"/data/local/tmp/{name}.xml")
        adb(serial, "shell", f"run-as {PKG} sh -c 'cat /data/local/tmp/{name}.xml > shared_prefs/{name}.xml'")
    adb(serial, "shell", "am", "start", "-n", f"{PKG}/.MainActivity")
    print(f"{serial}: 3 events, 3 notes")


if __name__ == "__main__":
    main()
