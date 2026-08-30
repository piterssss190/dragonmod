"""
Generuje teksturę 160x272 dopasowaną PIKSEL W PIKSEL do standardowego
rozwinięcia UV (top/bottom/west/north/east/south) używanego przez
Minecraft CubeListBuilder.addBox() dla każdej bryły naszego modelu smoka.

Układ (u, v, dx, dy, dz) musi być IDENTYCZNY z tym, co wpisujemy
w DragonModel.java (texOffs + wymiary addBox) - to jest właśnie ta
"pasująca teksturowo" część.
"""
from PIL import Image, ImageDraw
import random

random.seed(7)

TEX_W, TEX_H = 160, 272
img = Image.new("RGBA", (TEX_W, TEX_H), (255, 0, 255, 255))  # magenta = "błąd/nie pomalowane"
draw = ImageDraw.Draw(img)

# --- Paleta kolorów ---
GREEN_BASE   = (52, 99, 46)
GREEN_DARK   = (34, 70, 30)
GREEN_LIGHT  = (70, 122, 60)
BELLY        = (214, 198, 150)
BELLY_DARK   = (184, 166, 118)
FOOTPAD      = (60, 55, 55)
CLAW         = (30, 28, 28)
MEMBRANE     = (132, 74, 46)
MEMBRANE_DK  = (96, 50, 30)
VEIN         = (70, 34, 20)
EYE          = (255, 205, 40)
EYE_SLIT     = (30, 20, 10)
NOSTRIL      = (25, 45, 22)


def unwrap(u, v, dx, dy, dz):
    """Standardowy rozkład UV Minecrafta dla pudełka (dx,dy,dz) przy origin (u,v)."""
    return {
        "top":    (u + dz, v, dx, dz),
        "bottom": (u + dz + dx, v, dx, dz),
        "west":   (u, v + dz, dz, dy),
        "north":  (u + dz, v + dz, dx, dy),
        "east":   (u + dz + dx, v + dz, dz, dy),
        "south":  (u + dz + dx + dz, v + dz, dx, dy),
    }


def fill_rect(x, y, w, h, color):
    if w <= 0 or h <= 0:
        return
    draw.rectangle([x, y, x + w - 1, y + h - 1], fill=color)


def scale_pattern(x, y, w, h, base, dark, light):
    """Rysuje prosty wzór łusek: naprzemienne rzędy przesuniętych 'kropek'."""
    fill_rect(x, y, w, h, base)
    if w < 4 or h < 4:
        return
    row = 0
    yy = y
    while yy < y + h:
        offset = 2 if row % 2 == 0 else 0
        xx = x + offset
        while xx < x + w:
            c = dark if random.random() < 0.6 else light
            sw = min(3, x + w - xx)
            sh = min(2, y + h - yy)
            if sw > 0 and sh > 0:
                draw.rectangle([xx, yy, xx + sw - 1, yy + sh - 1], fill=c)
            xx += 4
        yy += 3
        row += 1


def paint_creature_box(u, v, dx, dy, dz, belly_on_bottom=True, footpad=False):
    r = unwrap(u, v, dx, dy, dz)
    for face in ("top", "west", "north", "east", "south"):
        x, y, w, h = r[face]
        scale_pattern(x, y, w, h, GREEN_BASE, GREEN_DARK, GREEN_LIGHT)
    x, y, w, h = r["bottom"]
    if footpad:
        fill_rect(x, y, w, h, FOOTPAD)
    elif belly_on_bottom:
        scale_pattern(x, y, w, h, BELLY, BELLY_DARK, BELLY)
    else:
        scale_pattern(x, y, w, h, GREEN_BASE, GREEN_DARK, GREEN_LIGHT)
    return r


def paint_wing(u, v, dx, dy, dz):
    r = unwrap(u, v, dx, dy, dz)
    for face in ("top", "bottom"):
        x, y, w, h = r[face]
        fill_rect(x, y, w, h, MEMBRANE)
        # żyłki skrzydła - promieniście od nasady (lewa krawędź, x)
        n_veins = 5
        for i in range(n_veins):
            vy = y + int((i + 0.5) * h / n_veins)
            draw.line([(x, y + h // 2), (x + w - 2, vy)], fill=VEIN, width=1)
        # ciemniejsza obwódka
        draw.rectangle([x, y, x + w - 1, y + h - 1], outline=MEMBRANE_DK, width=1)
    for face in ("west", "north", "east", "south"):
        x, y, w, h = r[face]
        fill_rect(x, y, w, h, MEMBRANE_DK)
    return r


# ---- BODY: 24,24,56 przy (0,0) ----
BODY_U, BODY_V, BODY_DX, BODY_DY, BODY_DZ = 0, 0, 24, 24, 56
paint_creature_box(BODY_U, BODY_V, BODY_DX, BODY_DY, BODY_DZ, belly_on_bottom=True)

# ---- HEAD: 16,16,20 przy (0,82) ----
HEAD_U, HEAD_V, HEAD_DX, HEAD_DY, HEAD_DZ = 0, 82, 16, 16, 20
r = paint_creature_box(HEAD_U, HEAD_V, HEAD_DX, HEAD_DY, HEAD_DZ, belly_on_bottom=True)
# przód głowy = face "north" (kierunek -Z, w stronę pyska)
fx, fy, fw, fh = r["north"]
eye_y = fy + fh // 3
draw.ellipse([fx + 2, eye_y, fx + 5, eye_y + 3], fill=EYE)
draw.ellipse([fx + fw - 6, eye_y, fx + fw - 3, eye_y + 3], fill=EYE)
draw.line([fx + 3, eye_y + 1, fx + 4, eye_y + 2], fill=EYE_SLIT, width=1)
draw.line([fx + fw - 5, eye_y + 1, fx + fw - 4, eye_y + 2], fill=EYE_SLIT, width=1)
nostril_y = fy + fh - 3
draw.point((fx + fw // 2 - 2, nostril_y), fill=NOSTRIL)
draw.point((fx + fw // 2 + 2, nostril_y), fill=NOSTRIL)

# ---- TAIL: 8,8,40 przy (0,120) ----
paint_creature_box(0, 120, 8, 8, 40, belly_on_bottom=True)

# ---- LEFT WING: 48,2,32 przy (0,170) ----
paint_wing(0, 170, 48, 2, 32)

# ---- RIGHT WING: 48,2,32 przy (0,206) ----
paint_wing(0, 206, 48, 2, 32)

# ---- 4x LEG: 8,20,8, rząd przy v=242, u = 0,34,68,102 ----
for leg_u in (0, 34, 68, 102):
    paint_creature_box(leg_u, 242, 8, 20, 8, belly_on_bottom=False, footpad=True)

out_path = "/home/claude/texture_gen/dragon.png"
img.save(out_path)
print("saved", out_path, img.size)
