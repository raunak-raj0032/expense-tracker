"""Slice the Penny mascot sprite sheets (2x4 grid) into individual emotion PNGs.

Background of source images is near-black; we flood-fill from the four corners
with a tolerance so the piggy's black eyes/nose stay opaque, then tight-crop
to the visible mascot.
"""
from __future__ import annotations

import os
from pathlib import Path
from PIL import Image, ImageDraw

REPO_ROOT = Path(r"d:/Android Work/expense-tracker")
ASSETS = REPO_ROOT / "app/src/main/assets/mascot"
DOWNLOADS = Path(r"C:/Users/HP/Downloads")

FULL_SRC = DOWNLOADS / "ChatGPT Image May 10, 2026, 03_21_28 PM.png"
HALF_SRC = DOWNLOADS / "ChatGPT Image May 10, 2026, 03_23_54 PM.png"

# Mapping: (row, col) -> emotion key. Layout (per the sheets):
#   Row 0: wave, wink, cheer, curious
#   Row 1: surprised, love (worried-cute), sleepy (teary), excited
LAYOUT = {
    (0, 0): "wave",
    (0, 1): "wink",
    (0, 2): "cheer",
    (0, 3): "curious",
    (1, 0): "surprised",
    (1, 1): "love",
    (1, 2): "sleepy",
    (1, 3): "excited",
}

ROWS, COLS = 2, 4


def remove_bg(cell: Image.Image, tolerance: int = 55) -> Image.Image:
    """Flood-fill from corners using PIL to make near-black background transparent."""
    img = cell.convert("RGBA")
    w, h = img.size
    # Work on a copy where the background sample is replaced with a sentinel alpha.
    # Strategy: for each border pixel, if it's near-black, flood-fill to transparent.
    seed_color = (0, 0, 0, 0)
    # PIL ImageDraw.floodfill operates on RGB; we'll do it on a temp RGB then
    # mask. Use a unique magenta sentinel.
    sentinel = (255, 0, 255)
    rgb = img.convert("RGB")
    # Flood-fill from each of the four corners with the sentinel.
    for x, y in [(0, 0), (w - 1, 0), (0, h - 1), (w - 1, h - 1)]:
        ImageDraw.floodfill(rgb, (x, y), sentinel, thresh=tolerance)
    # Also try mid-edge points to catch detached background regions.
    for x, y in [(w // 2, 0), (w // 2, h - 1), (0, h // 2), (w - 1, h // 2)]:
        ImageDraw.floodfill(rgb, (x, y), sentinel, thresh=tolerance)
    # Build alpha mask: pixels equal to sentinel -> 0 alpha, else keep original.
    px_rgb = rgb.load()
    px_out = img.load()
    for y in range(h):
        for x in range(w):
            if px_rgb[x, y] == sentinel:
                px_out[x, y] = seed_color
    return img


def tight_crop(img: Image.Image, padding: int = 6) -> Image.Image:
    bbox = img.getbbox()  # uses non-zero alpha
    if not bbox:
        return img
    left, top, right, bottom = bbox
    left = max(0, left - padding)
    top = max(0, top - padding)
    right = min(img.width, right + padding)
    bottom = min(img.height, bottom + padding)
    return img.crop((left, top, right, bottom))


def slice_sheet(src_path: Path, out_dir: Path) -> dict[str, Path]:
    out_dir.mkdir(parents=True, exist_ok=True)
    sheet = Image.open(src_path).convert("RGBA")
    W, H = sheet.size
    cell_w = W // COLS
    cell_h = H // ROWS
    print(f"Slicing {src_path.name}: {W}x{H}, cell={cell_w}x{cell_h}")
    written: dict[str, Path] = {}
    for (r, c), name in LAYOUT.items():
        box = (c * cell_w, r * cell_h, (c + 1) * cell_w, (r + 1) * cell_h)
        cell = sheet.crop(box)
        cleaned = remove_bg(cell)
        trimmed = tight_crop(cleaned)
        out = out_dir / f"penny_{name}.png"
        trimmed.save(out, format="PNG", optimize=True)
        written[name] = out
        print(f"  -> {out.relative_to(REPO_ROOT)}  {trimmed.size}")
    return written


def main() -> None:
    full_out = ASSETS / "emotions/full"
    half_out = ASSETS / "emotions/half_cut"

    full_files = slice_sheet(FULL_SRC, full_out)
    half_files = slice_sheet(HALF_SRC, half_out)

    # Default mascot uses the cheer pose (full body, both arms up, biggest grin).
    default = full_files.get("cheer") or full_files.get("wave")
    if default:
        target = ASSETS / "penny_mascot.png"
        Image.open(default).save(target, format="PNG", optimize=True)
        print(f"Updated default mascot -> {target.relative_to(REPO_ROOT)}")

    # penny_coin uses the wink/thumbs-up pose since the new art has no
    # dedicated coin shot. penny_happy reuses the wave for a friendly default.
    wink = full_files.get("wink")
    if wink:
        Image.open(wink).save(full_out / "penny_coin.png", format="PNG", optimize=True)
    if "wave" in full_files:
        Image.open(full_files["wave"]).save(full_out / "penny_happy.png", format="PNG", optimize=True)

    print("Done.")


if __name__ == "__main__":
    main()
