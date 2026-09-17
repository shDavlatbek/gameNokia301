"""Deterministic level generator for Vanguard Zero.

A level is a row of sectors separated by solid bulkheads. Each sector holds a
braided maze with a few open rooms. Consecutive sectors are joined by exactly
one doorway, so a locked door there is a real gate: its keycard is always
placed in an earlier sector, which makes every level completable in order.
"""
import random, collections, sys

SOLID = set("#%=&")


def n4(x, y):
    yield x + 1, y
    yield x - 1, y
    yield x, y + 1
    yield x, y - 1


class Map:
    def __init__(self, w, h):
        self.w, self.h = w, h
        self.g = [["#"] * w for _ in range(h)]

    def __getitem__(self, p):
        return self.g[p[1]][p[0]]

    def __setitem__(self, p, v):
        self.g[p[1]][p[0]] = v

    def rows(self):
        return ["".join(r) for r in self.g]

    def inside(self, p):
        return 0 <= p[0] < self.w and 0 <= p[1] < self.h

    def open_cells(self):
        return [(x, y) for y in range(self.h) for x in range(self.w)
                if self.g[y][x] not in SOLID]

    # ---- carving -------------------------------------------------------
    def maze(self, rng, x0, y0, x1, y1):
        """Randomised DFS maze inside the inclusive box, on odd offsets."""
        sx = x0 if (x0 % 2 == 1) else x0 + 1
        sy = y0 if (y0 % 2 == 1) else y0 + 1
        if sx > x1 or sy > y1:
            return
        self[sx, sy] = "."
        stack = [(sx, sy)]
        while stack:
            x, y = stack[-1]
            opts = []
            for dx, dy in ((2, 0), (-2, 0), (0, 2), (0, -2)):
                nx, ny = x + dx, y + dy
                if x0 <= nx <= x1 and y0 <= ny <= y1 and self[nx, ny] in SOLID:
                    opts.append((nx, ny, x + dx // 2, y + dy // 2))
            if not opts:
                stack.pop()
                continue
            nx, ny, mx, my = rng.choice(opts)
            self[mx, my] = "."
            self[nx, ny] = "."
            stack.append((nx, ny))

    def braid(self, rng, chance, x0, y0, x1, y1):
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                if self[x, y] in SOLID:
                    continue
                walls = [(nx, ny) for nx, ny in n4(x, y)
                         if x0 <= nx <= x1 and y0 <= ny <= y1 and self[nx, ny] in SOLID]
                if len(walls) == 3 and rng.random() < chance:
                    self[rng.choice(walls)] = "."

    def room(self, x0, y0, w, h, bx0, by0, bx1, by1):
        for y in range(y0, y0 + h):
            for x in range(x0, x0 + w):
                if bx0 <= x <= bx1 and by0 <= y <= by1:
                    self[x, y] = "."

    def bfs(self, start, blocked=()):
        dist = {start: 0}
        q = collections.deque([start])
        while q:
            c = q.popleft()
            for n in n4(*c):
                if n in dist or n in blocked or not self.inside(n):
                    continue
                if self[n] in SOLID:
                    continue
                dist[n] = dist[c] + 1
                q.append(n)
        return dist

    def texture(self, rng, palette, x0, x1):
        for y in range(self.h):
            for x in range(self.w):
                if self[x, y] != "#":
                    continue
                if not (x0 <= x <= x1):
                    continue
                ch = palette[(y * len(palette)) // self.h]
                if rng.random() < 0.22:
                    ch = rng.choice(palette)
                self[x, y] = ch


def build(spec, seed):
    rng = random.Random(seed)
    w, h = spec["w"], spec["h"]
    m = Map(w, h)
    nsec = len(spec["sectors"])

    # sector x ranges (interior), separated by one solid bulkhead column
    inner_w = w - 2 - (nsec - 1)
    widths = []
    for i in range(nsec):
        widths.append(inner_w // nsec + (1 if i < inner_w % nsec else 0))
    bounds = []
    x = 1
    for i in range(nsec):
        bounds.append((x, x + widths[i] - 1))
        x += widths[i] + 1

    for i, (x0, x1) in enumerate(bounds):
        sec = spec["sectors"][i]
        m.maze(rng, x0, 1, x1, h - 2)
        m.braid(rng, sec.get("braid", 0.35), x0, 1, x1, h - 2)
        for (rx, ry, rw, rh) in sec.get("rooms", []):
            m.room(x0 + rx, ry, rw, rh, x0, 1, x1, h - 2)
        # the rooms may have isolated pockets: reconnect everything in the sector
        cells = [(cx, cy) for cy in range(1, h - 1) for cx in range(x0, x1 + 1)
                 if m[cx, cy] == "."]
        seen = m.bfs(cells[0])
        for c in cells:
            if c in seen:
                continue
            # dig toward the known region
            target = min((s for s in seen if x0 <= s[0] <= x1),
                         key=lambda s: abs(s[0] - c[0]) + abs(s[1] - c[1]))
            cx, cy = c
            while (cx, cy) != target:
                if cx != target[0]:
                    cx += 1 if target[0] > cx else -1
                else:
                    cy += 1 if target[1] > cy else -1
                m[cx, cy] = "."
            seen = m.bfs(cells[0])

    # One doorway per bulkhead. The mazes are carved on odd offsets, so the
    # column next to a bulkhead can be solid: dig a short stub on both sides.
    door_cells = []
    for i in range(nsec - 1):
        bx = bounds[i][1] + 1
        cand = [y for y in range(2, h - 2)
                if m[bx - 2, y] == "." and m[bx + 2, y] == "."]
        if not cand:
            cand = [y for y in range(2, h - 2)
                    if m[bx - 2, y] == "." or m[bx + 2, y] == "."]
        if not cand:
            cand = [h // 2]
        y = cand[len(cand) // 2 if i % 2 == 0 else len(cand) // 3]
        for xx in (bx - 1, bx, bx + 1):
            m[xx, y] = "."
        door_cells.append((bx, y))

    # start in the first sector, exit in the last
    first = [c for c in m.bfs(door_cells[0] if door_cells else (bounds[0][0], 1))
             if bounds[0][0] <= c[0] <= bounds[0][1]]
    start = max(first, key=lambda c: (abs(c[0] - bounds[0][0]) + c[1], c[1], c[0]))
    dist = m.bfs(start)
    last = [c for c in dist if bounds[-1][0] <= c[0] <= bounds[-1][1]]
    exit_cell = max(last, key=lambda c: (dist[c], c[1], c[0]))

    used = set([start, exit_cell])
    m[exit_cell] = "E"

    locks = spec["locks"]  # one char per bulkhead: 'D' plain, or R/G/B locked
    for i, dc in enumerate(door_cells):
        m[dc] = locks[i]
        used.add(dc)

    # keycards: the card for bulkhead i lives in a sector before it
    for i, dc in enumerate(door_cells):
        lock = locks[i]
        if lock == "D":
            continue
        region = m.bfs(start, set(door_cells[i:]))
        cands = [c for c in region if m[c] == "." and c not in used and region[c] > 4]
        if not cands:
            cands = [c for c in region if m[c] == "." and c not in used]
        pick = max(cands, key=lambda c: (region[c], c[1], c[0]))
        used.add(pick)
        m[pick] = lock.lower()

    # extra unlocked doors inside sectors, for looks
    plain = []
    for y in range(1, h - 1):
        for x in range(1, w - 1):
            if m[x, y] != "." or (x, y) in used:
                continue
            op = [(nx, ny) for nx, ny in n4(x, y) if m[nx, ny] not in SOLID]
            if len(op) == 2 and (op[0][0] == op[1][0] or op[0][1] == op[1][1]):
                plain.append((x, y))
    rng.shuffle(plain)
    for c in plain[:spec.get("plain_doors", 2)]:
        m[c] = "D"
        used.add(c)

    # content, spread per sector so difficulty ramps with progress
    dist2 = m.bfs(start)
    for i, (x0, x1) in enumerate(bounds):
        floor = [c for c in dist2 if m[c] == "." and c not in used
                 and x0 <= c[0] <= x1 and dist2[c] > 2]
        floor.sort(key=lambda c: (dist2[c], c[1], c[0]))
        items = []
        for ch, n in spec["sectors"][i]["content"]:
            for _ in range(n):
                items.append(ch)
        if len(items) > len(floor):
            raise RuntimeError("sector %d of %s: %d items, %d free cells"
                               % (i, spec["name"], len(items), len(floor)))
        step = len(floor) / float(len(items) + 1)
        for k, ch in enumerate(items):
            idx = min(int((k + 1) * step), len(floor) - 1)
            while floor[idx] in used:
                idx = (idx + 1) % len(floor)
            m[floor[idx]] = ch
            used.add(floor[idx])

    m[start] = "P"
    for i, (x0, x1) in enumerate(bounds):
        m.texture(rng, spec["sectors"][i].get("palette", "#"), x0 - 1, x1 + 1)
    for x in range(w):
        for y in range(h):
            if m[x, y] == "#":
                m[x, y] = spec.get("border", "#")
    return m.rows()


SPECS = [
    dict(name="l1", w=23, h=19, locks="B", plain_doors=3, border="#",
         sectors=[
             dict(braid=0.4, palette="##%", rooms=[(1, 2, 4, 3), (3, 10, 4, 4)],
                  content=[("d", 4), ("m", 1), ("i", 2), ("a", 1), ("W", 1)]),
             dict(braid=0.4, palette="#%=", rooms=[(2, 3, 4, 4), (1, 12, 5, 4)],
                  content=[("d", 4), ("t", 2), ("m", 1), ("i", 1), ("a", 1)]),
         ]),
    dict(name="l2", w=25, h=21, locks="G", plain_doors=3, border="%",
         sectors=[
             dict(braid=0.38, palette="%%=", rooms=[(1, 3, 5, 4), (4, 12, 5, 5)],
                  content=[("d", 3), ("t", 2), ("T", 1), ("m", 2), ("i", 3),
                           ("a", 2), ("W", 1)]),
             dict(braid=0.38, palette="%=&", rooms=[(2, 2, 5, 4), (1, 13, 6, 5)],
                  content=[("d", 2), ("t", 3), ("h", 1), ("T", 1), ("m", 2),
                           ("a", 1), ("Q", 1), ("p", 2)]),
         ]),
    dict(name="l3", w=27, h=21, locks="RG", plain_doors=4, border="=",
         sectors=[
             dict(braid=0.36, palette="=#%", rooms=[(1, 2, 4, 4)],
                  content=[("d", 4), ("t", 2), ("m", 1), ("i", 2), ("a", 1),
                           ("W", 1)]),
             dict(braid=0.36, palette="=%&", rooms=[(1, 8, 5, 5)],
                  content=[("d", 2), ("t", 3), ("h", 1), ("m", 1), ("i", 1),
                           ("p", 1), ("Q", 1)]),
             dict(braid=0.36, palette="=&%", rooms=[(1, 3, 5, 5), (1, 13, 4, 4)],
                  content=[("t", 3), ("h", 2), ("m", 2), ("a", 2), ("i", 1),
                           ("p", 2)]),
         ]),
    dict(name="l4", w=27, h=23, locks="RB", plain_doors=4, border="&",
         sectors=[
             dict(braid=0.34, palette="&%=", rooms=[(1, 3, 4, 4), (2, 13, 4, 5)],
                  content=[("d", 4), ("t", 2), ("m", 1), ("i", 2), ("a", 1),
                           ("W", 1)]),
             dict(braid=0.34, palette="&=%", rooms=[(1, 5, 5, 5), (1, 15, 5, 4)],
                  content=[("d", 2), ("t", 3), ("h", 2), ("m", 2), ("i", 1),
                           ("p", 2), ("a", 1), ("Q", 1)]),
             dict(braid=0.3, palette="&%&", rooms=[(1, 4, 5, 6), (1, 14, 5, 5)],
                  content=[("t", 2), ("h", 2), ("X", 1), ("m", 2), ("a", 2),
                           ("i", 1), ("p", 2)]),
         ]),
]

if __name__ == "__main__":
    for i, spec in enumerate(SPECS):
        rows = build(spec, 2100 + i)
        open(spec["name"] + ".txt", "w").write("\n".join(rows) + "\n")
        print(spec["name"], "%dx%d" % (len(rows[0]), len(rows)))
