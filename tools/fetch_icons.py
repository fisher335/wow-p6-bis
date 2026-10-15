# -*- coding: utf-8 -*-
"""Fetch item icons (and fix missing names) from wowhead, store icons by texture name."""
import json, os, time, sys
import requests

OUT = r"D:\Dev\ailua\wowP6\tools\out"
ASSETS = r"D:\Dev\ailua\wowP6\app\src\main\assets"
ICONS = os.path.join(ASSETS, "icons")
os.makedirs(ICONS, exist_ok=True)

with open(os.path.join(OUT, "app_data.json"), encoding="utf-8") as f:
    data = json.load(f)
ref = json.load(open(os.path.join(OUT, "ref_ids.json"), encoding="utf-8"))

s = requests.Session()
s.headers.update({"User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) BisApp/1.0"})

icons = {}       # itemid -> icon texture name
names = {}       # itemid -> name (fallback fixes)
for n, iid in enumerate(ref, 1):
    txt = None
    for attempt in range(3):
        try:
            r = s.get("https://nether.wowhead.com/tooltip/item/%d" % iid, timeout=20)
            if r.status_code == 200:
                txt = r.json()
                break
            elif r.status_code == 404:
                txt = None
                break
        except Exception as e:
            time.sleep(0.5 * (attempt + 1))
    if txt:
        if txt.get("icon"):
            icons[iid] = txt["icon"]
        if txt.get("name"):
            names[iid] = txt["name"]
    if n % 50 == 0:
        print("queried", n, "with icon", len(icons), flush=True)
    time.sleep(0.05)

print("total items", len(ref), "icons found", len(icons), "names found", len(names), flush=True)

# apply names for missing
for iid in ref:
    key = str(iid)
    if data["items"].get(key, {}).get("name") in (None, "未知物品") and iid in names:
        data["items"][key]["name"] = names[iid]

# unique icons -> download
uniq = sorted(set(icons.values()))
print("unique icons", len(uniq), flush=True)
ok = 0
fail = 0
for n, ic in enumerate(uniq, 1):
    dst = os.path.join(ICONS, ic + ".jpg")
    if os.path.exists(dst) and os.path.getsize(dst) > 0:
        ok += 1
        continue
    got = False
    for url in ("https://wow.zamimg.com/images/wow/icons/large/%s.jpg" % ic,
                "https://wow.zamimg.com/images/wow/icons/medium/%s.jpg" % ic):
        try:
            r = s.get(url, timeout=20)
            if r.status_code == 200 and len(r.content) > 200:
                with open(dst, "wb") as fo:
                    fo.write(r.content)
                got = True
                break
        except Exception:
            pass
    if got:
        ok += 1
    else:
        fail += 1
        print("icon fail", ic, flush=True)
    if n % 50 == 0:
        print("icons", n, "ok", ok, "fail", fail, flush=True)
    time.sleep(0.03)

# store icon mapping in items
for iid in ref:
    key = str(iid)
    if iid in icons:
        data["items"][key]["icon"] = icons[iid]

json.dump(data, open(os.path.join(OUT, "app_data.json"), "w", encoding="utf-8"),
          ensure_ascii=False)
print("done. downloaded", ok, "failed", fail, flush=True)
