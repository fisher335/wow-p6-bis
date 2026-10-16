# -*- coding: utf-8 -*-
"""Merge wowhead set bonus text into app_data.json."""
import json, os
OUT = r"D:\Dev\ailua\wowP6\tools\out"

data = json.load(open(os.path.join(OUT, "app_data.json"), encoding="utf-8"))
bonus = json.load(open(os.path.join(OUT, "sets_bonus.json"), encoding="utf-8-sig"))
bd = {b["key"]: b for b in bonus}

for k, v in data.get("sets", {}).items():
    b = bd.get(k, {})
    v["b2"] = b.get("b2")
    v["b4"] = b.get("b4")
    v["en"] = b.get("en")
    if not v.get("name") or len(v["name"]) < 2:
        v["name"] = b.get("en") or k

json.dump(data, open(os.path.join(OUT, "app_data.json"), "w", encoding="utf-8"),
          ensure_ascii=False)
n = sum(1 for v in data["sets"].values() if v.get("b2") or v.get("b4"))
print("sets:", len(data["sets"]), "with bonus:", n)
