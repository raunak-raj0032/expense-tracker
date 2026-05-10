"""Slice the love/sleepy 2x2 sheet and replace existing variants.

Layout of the source sheet:
  Top-left  = full-body  Penny LOVE   (dreamy, hand on cheek)
  Top-right = full-body  Penny SLEEPY (eyes closed, ZZZs)
  Bottom-left  = half-cut Penny LOVE
  Bottom-right = half-cut Penny SLEEPY
"""
from __future__ import annotations

from pathlib import Path
from PIL import Image, ImageDraw

REPO_ROOT = Path(r"d:/Android Work/expense-tracker")
ASSETS = REPO_ROOT / "app/src/main/assets/mascot"
SRC = Path(r"C:/Users/HP/Downloads/ChatGPT Image May 10, 2026, 03_39_28 PM.png")

LAYOUT = {
    (0, 0): ("full", "love"),
    (0, 1): ("full", "sleepy"),
    (1, 0): ("half_cut", "love"),
    (1, 1): ("half_cut", "sleepy"),
}


def remove_bg(cell: Image.Image, tolerance: int = 55) -> Image.Image:
    img = cell.convert("RGBA")
    w, h = img.size
    sentinel = (255, 0, 255)
    rgb = img.convert("RGB")
    seeds = [
        (0, 0), (w - 1, 0), (0, h - 1), (w - 1, h - 1),
        (w // 2, 0), (w // 2, h - 1), (0, h // 2), (w - 1, h // 2),
    ]
    for x, y in seeds:
        ImageDraw.floodfill(rgb, (x, y), sentinel, thresh=tolerance)
    px_rgb = rgb.load()
    px_out = img.load()
    for y in range(h):
        for x in range(w):
            if px_rgb[x, y] == sentinel:
                px_out[x, y] = (0, 0, 0, 0)
    return img


def tight_crop(img: Image.Image, padding: int = 6) -> Image.Image:
    bbox = img.getbbox()
    if not bbox:
        return img
    left, top, right, bottom = bbox
    return img.crop((
        max(0, left - padding),
        max(0, top - padding),
        min(img.width, right + padding),
        min(img.height, bottom + padding),
    ))


def main() -> None:
    sheet = Image.open(SRC).convert("RGBA")
    W, H = sheet.size
    cell_w = W // 2
    cell_h = H // 2
    print(f"Source: {SRC.name}  {W}x{H}  cell={cell_w}x{cell_h}")
    for (r, c), (variant, name) in LAYOUT.items():
        box = (c * cell_w, r * cell_h, (c + 1) * cell_w, (r + 1) * cell_h)
        cell = sheet.crop(box)
        cleaned = remove_bg(cell)
        trimmed = tight_crop(cleaned)
        out = ASSETS / f"emotions/{variant}/penny_{name}.png"
        out.parent.mkdir(parents=True, exist_ok=True)
        trimmed.save(out, format="PNG", optimize=True)
        print(f"  -> {out.relative_to(REPO_ROOT)}  {trimmed.size}")


if __name__ == "__main__":
    main()
