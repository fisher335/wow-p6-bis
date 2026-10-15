# -*- coding: utf-8 -*-
import json, os
OUT = r"D:\Dev\ailua\wowP6\tools\out"
data = json.load(open(os.path.join(OUT, "app_data.json"), encoding="utf-8"))
icon_map = json.load(open(os.path.join(OUT, "icon_map.json"), encoding="utf-8-sig"))
name_map = json.load(open(os.path.join(OUT, "name_map.json"), encoding="utf-8-sig"))

fixed = 0
for k, v in data["items"].items():
    if k in icon_map:
        v["icon"] = icon_map[k]
    if (v.get("name") in (None, "", "未知物品")) and k in name_map:
        v["name"] = name_map[k]
        fixed += 1

json.dump(data, open(os.path.join(OUT, "app_data.json"), "w", encoding="utf-8"),
          ensure_ascii=False)
n_icon = sum(1 for v in data["items"].values() if v.get("icon"))
print("items with icon:", n_icon, "/", len(data["items"]), "names fixed:", fixed)
