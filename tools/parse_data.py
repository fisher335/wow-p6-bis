# -*- coding: utf-8 -*-
"""Extract TitanBistooltip data into an app-friendly JSON."""
import re, json, os

SRC = r"C:\Users\Terry\AppData\Local\Temp\opencode\bis_extract"
OUT = r"D:\Dev\ailua\wowP6\tools\out"
os.makedirs(OUT, exist_ok=True)

# ---------------- low level lua-ish helpers ----------------
def matching_brace(s, i):
    assert s[i] == "{"
    depth = 0
    n = len(s)
    while i < n:
        c = s[i]
        if c == '"':
            i += 1
            while i < n and s[i] != '"':
                if s[i] == "\\":
                    i += 1
                i += 1
        elif c == "{":
            depth += 1
        elif c == "}":
            depth -= 1
            if depth == 0:
                return i
        i += 1
    return n - 1

def top_groups(s):
    """Return inner strings of each top-level {...} in s.
    If s itself is a single wrapping table {...}, strip it first."""
    ls = s.lstrip()
    if ls.startswith("{"):
        offset = len(s) - len(ls)
        j = matching_brace(s, offset)
        if s[j + 1:].strip() == "":
            s = s[offset + 1:j]
    res = []
    i = 0
    n = len(s)
    while i < n:
        c = s[i]
        if c == '"':
            i += 1
            while i < n and s[i] != '"':
                if s[i] == "\\":
                    i += 1
                i += 1
        elif c == "{":
            j = matching_brace(s, i)
            res.append(s[i:j + 1])
            i = j
        i += 1
    return res

def key_table(body, key):
    m = re.search(r"\b" + re.escape(key) + r"\s*=\s*\{", body)
    if not m:
        return None
    start = m.end() - 1
    end = matching_brace(body, start)
    return body[start:end + 1]

def after_assign(text, name):
    m = re.search(re.escape(name) + r"\s*=\s*\{", text)
    if not m:
        return None
    start = m.end() - 1
    end = matching_brace(text, start)
    return text[start:end + 1]

def ints(s):
    return [int(x) for x in re.findall(r"\d+", re.sub(r"\[\d+\]\s*=", "", s))]

def qname(group):
    m = re.search(r'name\s*=\s*"([^"]*)"', group)
    return m.group(1) if m else None

def read(fn):
    return open(fn, "r", encoding="utf-8", errors="replace").read()

# ---------------- item database ----------------
def parse_items(raw):
    items = {}
    for m in re.finditer(r"^\t\[(\d+)\]\s*=\s*", raw, re.M):
        iid = int(m.group(1))
        start = raw.index("{", m.end())
        end = matching_brace(raw, start)
        body = raw[start:end + 1]
        link = re.search(r'link\s*=\s*"([^"]*)"', body)
        name = quality = None
        if link:
            l = link.group(1)
            qm = re.search(r"(?:\\124|\|)cff([0-9a-fA-F]{6})", l)
            if qm:
                quality = {"a335ee": 4, "0070dd": 3, "1eff00": 2,
                           "ff8000": 5, "9d9d9d": 1}.get(qm.group(1).lower())
            nm = re.search(r"\[(.+?)\]", l)
            if nm:
                name = nm.group(1)
        def g(k):
            mm = re.search(k + r'\s*=\s*"([^"]*)"', body)
            return mm.group(1) if mm else None
        ilvl = re.search(r"itemLevel\s*=\s*(\d+)", body)
        sck = re.search(r"socketCount\s*=\s*(\d+)", body)
        items[iid] = {
            "name": name, "q": quality,
            "ilvl": int(ilvl.group(1)) if ilvl else None,
            "type": g("itemType"), "sub": g("itemSubType"),
            "loc": g("equipLoc"), "sockets": int(sck.group(1)) if sck else 0,
        }
    return items

# ---------------- bis lists (P6) ----------------
def parse_bis(root, only_phase="P6"):
    result = {}
    for cls in sorted(os.listdir(root)):
        cdir = os.path.join(root, cls)
        if not os.path.isdir(cdir):
            continue
        for f in sorted(os.listdir(cdir)):
            if not f.endswith(".lua"):
                continue
            spec = f[:-4]
            raw = read(os.path.join(cdir, f))
            entries = []
            pat = r'\["' + only_phase + r'"\]\[(\d+)\]\s*=\s*\{(.*?)\}\s*$'
            for m in re.finditer(pat, raw, re.M):
                seg = m.group(2)
                slot = re.search(r'slot_name"\s*\]\s*=\s*"([^"]*)"', seg)
                ids = [int(v) for _, v in re.findall(r"\[(\d+)\]\s*=\s*(-?\d+)", seg)]
                entries.append({"idx": int(m.group(1)),
                                "slot": slot.group(1) if slot else None,
                                "ids": ids})
            entries.sort(key=lambda e: e["idx"])
            result.setdefault(cls, {})[spec] = entries
    return result

# ---------------- source index ----------------
def add(idx, iid, txt):
    idx.setdefault(iid, []).append(txt)

def build_sources(base):
    idx = {}
    d = os.path.join(base, "shuju", "BisItem")

    # raids
    raw = read(os.path.join(d, "kp_bs_raid.lua"))
    outer = after_assign(raw, "KPBisItem.Raid")
    for rt in top_groups(outer):
        rname = qname(rt)
        bt = key_table(rt, "bosses")
        if bt:
            for bg in top_groups(bt):
                boss = qname(bg)
                for lk in ("loot", "lootA", "lootH"):
                    lt = key_table(bg, lk)
                    if lt:
                        for i in ints(lt):
                            add(idx, i, "%s · %s" % (rname, boss))
        et = key_table(rt, "exchange")
        if et:
            for i in ints(et):
                add(idx, i, "%s 兑换" % rname)
        for lk in ("quest", "legendary"):
            lt = key_table(rt, lk)
            if lt:
                for i in ints(lt):
                    add(idx, i, "%s" % rname)

    # party dungeons
    raw = read(os.path.join(d, "kp_bs_party.lua"))
    outer = after_assign(raw, "KPBisItem.Party")
    for pt in top_groups(outer):
        pname = qname(pt)
        bt = key_table(pt, "bosses")
        if bt:
            for bg in top_groups(bt):
                boss = qname(bg)
                for lk in ("loot", "lootN"):
                    lt = key_table(bg, lk)
                    if lt:
                        for i in ints(lt):
                            add(idx, i, "%s · %s" % (pname, boss))

    # world boss
    raw = read(os.path.join(d, "kp_bs_worldboss.lua"))
    outer = after_assign(raw, "KPBisItem.WorldBoss")
    if outer:
        for wb in top_groups(outer):
            wname = qname(wb)
            lt = key_table(wb, "loot")
            if lt:
                for i in ints(lt):
                    add(idx, i, "世界BOSS · %s" % wname)
    outer = after_assign(raw, "KPBisItem.WorldDrop")
    if outer:
        for grp in top_groups(outer):
            for i in ints(grp):
                add(idx, i, "世界掉落")

    # craft
    raw = read(os.path.join(d, "kp_bs_craft.lua"))
    outer = after_assign(raw, "KPBisItem.Craft")
    for grp in top_groups(outer):
        for row in top_groups(grp):
            prof = None
            m = re.search(r'prof\s*=\s*"([^"]*)"', row)
            if m:
                prof = m.group(1)
            lt = key_table(row, "items")
            if lt and prof:
                for i in ints(lt):
                    add(idx, i, "制造 · %s" % prof)

    # token
    raw = read(os.path.join(d, "kp_bs_token.lua"))
    outer = after_assign(raw, "KPBisItem.Token")
    for grp in top_groups(outer):
        for row in top_groups(grp):
            m = re.search(r"id\s*=\s*(\d+)", row)
            if m:
                add(idx, int(m.group(1)), "牌子兑换")

    # reputation
    raw = read(os.path.join(d, "kp_bs_rep.lua"))
    outer = after_assign(raw, "KPBisItem.Rep")
    for grp in top_groups(outer):
        for row in top_groups(grp):
            fac = None
            m = re.search(r'faction\s*=\s*"([^"]*)"', row)
            if m:
                fac = m.group(1)
            for lvl in ("2", "3", "4", "5"):
                lt = key_table(row, lvl)
                if lt:
                    for i in ints(lt):
                        add(idx, i, "声望 · %s" % fac)

    # vendor
    raw = read(os.path.join(d, "kp_bs_vendor.lua"))
    outer = after_assign(raw, "KPBisItem.Vendor")
    for grp in top_groups(outer):
        for row in top_groups(grp):
            m = re.search(r"id\s*=\s*(\d+)", row)
            if m:
                add(idx, int(m.group(1)), "商人")

    # holiday
    raw = read(os.path.join(d, "kp_bs_holiday.lua"))
    outer = after_assign(raw, "KPBisItem.Holiday")
    for row in top_groups(outer):
        hname = qname(row)
        lt = key_table(row, "items")
        if lt:
            for i in ints(lt):
                add(idx, i, "节日 · %s" % hname)
    return idx

PRIORITY = [" · ", "兑换", "世界BOSS", "制造", "牌子", "声望", "商人", "节日", "世界掉落"]

def pick_source(lst):
    if not lst:
        return None
    seen = []
    for x in lst:
        if x not in seen:
            seen.append(x)
    # prefer most specific: raid/party/worldboss first
    order = {"团队": 0}
    def rank(s):
        for i, kw in enumerate(["世界BOSS", "制造", "牌子", "声望", "商人", "节日", "世界掉落", "兑换"]):
            if kw in s:
                return 10 + i
        return 0  # raid / party
    seen.sort(key=rank)
    return seen[0]

# ---------------- localization ----------------
CLASS_CN = {
    "Warrior": "战士", "Paladin": "圣骑士", "Death knight": "死亡骑士",
    "Hunter": "猎人", "Rogue": "盗贼", "Priest": "牧师", "Shaman": "萨满",
    "Mage": "法师", "Warlock": "术士", "Druid": "德鲁伊",
}
SPEC_CN = {
    "Arms": "武器", "Fury": "狂暴", "Protection": "防护",
    "Holy": "神圣", "Retribution": "惩戒", "Balance": "平衡",
    "Feral dps": "野性输出", "Feral tank": "野性坦克", "Restoration": "恢复",
    "Beast mastery": "兽王", "Marksmanship": "射击", "Survival": "生存",
    "Assassination": "刺杀", "Combat": "战斗", "Subtlety": "敏锐",
    "Discipline": "戒律", "Shadow": "暗影", "Elemental": "元素",
    "Enhancement": "增强", "Affliction": "痛苦", "Demonology": "恶魔",
    "Destruction": "毁灭", "Arcane": "奥术", "Fire": "火焰", "Frost": "冰霜",
    "Blood dps": "鲜血输出", "Blood tank": "鲜血坦克", "Unholy": "邪恶",
}
SLOT_CN = {
    "Head": "头部", "Neck": "颈部", "Shoulder": "肩部", "Back": "背部",
    "Chest": "胸部", "Wrist": "手腕", "Hands": "手部", "Waist": "腰部",
    "Legs": "腿部", "Feet": "脚部", "Finger": "戒指", "Trinket": "饰品",
    "Two hand": "双手武器", "Weapon": "武器", "Off hand": "副手",
    "Ranged": "远程", "Relic": "圣物",
}

def main():
    base = os.path.join(SRC, "TitanBistooltip")
    items = parse_items(read(os.path.join(SRC, "TitanBistooltip_WS", "ws_equips.lua")))
    bis = parse_bis(os.path.join(base, "shuju", "bis"))
    srcidx = build_sources(base)

    classes = []
    ref = set()
    for cls in ["Warrior", "Paladin", "Death knight", "Hunter", "Rogue",
                "Priest", "Shaman", "Mage", "Warlock", "Druid"]:
        if cls not in bis:
            continue
        specs = []
        for spec, entries in bis[cls].items():
            slots = []
            for e in entries:
                ids = [v for v in e["ids"] if v > 0]
                if not ids:
                    continue
                main = ids[0]
                alts = ids[1:3]  # two alternatives
                slots.append({"slot": e["slot"], "ids": [main] + alts})
                ref.update([main] + alts)
            if slots:
                specs.append({"key": spec, "name": SPEC_CN.get(spec, spec), "slots": slots})
        if specs:
            classes.append({"key": cls, "name": CLASS_CN.get(cls, cls), "specs": specs})

    out_items = {}
    missing = []
    for i in sorted(ref):
        it = items.get(i)
        if not it:
            missing.append(i)
            out_items[str(i)] = {"name": "未知物品", "q": None, "ilvl": None,
                                 "loc": None, "source": pick_source(srcidx.get(i))}
            continue
        out_items[str(i)] = {
            "name": it["name"], "q": it["q"], "ilvl": it["ilvl"],
            "loc": it["loc"], "type": it["type"], "sub": it["sub"],
            "sockets": it["sockets"], "source": pick_source(srcidx.get(i)),
        }

    app = {"classes": classes, "items": out_items, "slotCN": SLOT_CN}
    json.dump(app, open(os.path.join(OUT, "app_data.json"), "w", encoding="utf-8"),
              ensure_ascii=False)
    json.dump(sorted(ref), open(os.path.join(OUT, "ref_ids.json"), "w", encoding="utf-8"))
    print("classes:", len(classes), "specs:", sum(len(c["specs"]) for c in classes))
    print("referenced items:", len(ref), "missing:", len(missing), missing)
    nsrc = sum(1 for v in out_items.values() if v.get("source"))
    print("with source:", nsrc)

if __name__ == "__main__":
    main()
