"""Builds the mod page GIFs from the frames QuickStackShowcase records.

    ./gradlew :fabric:runClientGameTest -Pshowcase      # writes fabric/build/run/clientGameTest/showcase/<clip>/
    python publish/tools/make_gifs.py [clip...]         # writes publish/curseforge/media/<clip>.gif

Each clip folder holds f0000.png... and frames.jsonl with the cursor position (window pixels, -1 when hidden) and
the key badge to show on every frame. Pillow draws the cursor and the badge (screenshots contain neither); ffmpeg
encodes the GIF with one palette built from the whole clip and error-diffusion dithering, which keeps the game's
colours far better than a per-frame 256-colour reduction.
"""
import json
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[2]
FRAMES = ROOT / 'fabric/build/run/clientGameTest/showcase'
OUT = ROOT / 'publish/curseforge/media'

UI_CROP = (300, 50, 1100, 670)
# crop box in window pixels (1280x720) or None, and the output width
CLIPS = {
    'quick_stack': (None, 720),
    'lock_slots': (None, 720),
    'search': (None, 720),
    'chest_buttons': (UI_CROP, 640),
    'sort': (UI_CROP, 640),
    'shift_sweep': (UI_CROP, 640),
    'wheel': (UI_CROP, 640),
    'crafting': ((140, 90, 1140, 650), 720),
}
FPS = 20               # one frame per game tick
HOLD_LAST_S = 1.2      # pause on the last frame before the loop restarts
MAX_BYTES = 8_000_000  # if a GIF is bigger, it is re-encoded smaller (see ATTEMPTS)
# (width factor, fps) tried in order until the GIF fits MAX_BYTES
ATTEMPTS = [(1.0, 20), (0.85, 20), (0.85, 15), (0.7, 15)]


def font(size):
    for name in ('segoeuib.ttf', 'arialbd.ttf', 'DejaVuSans-Bold.ttf'):
        try:
            return ImageFont.truetype(name, size)
        except OSError:
            continue
    return ImageFont.load_default()


def draw_cursor(img, x, y, scale):
    """A white arrow pointer with a black outline, tip at (x, y)."""
    shape = [(0, 0), (0, 17), (4, 13), (7, 20), (10, 19), (7, 12), (12, 12)]
    points = [(x + px * scale, y + py * scale) for px, py in shape]
    ImageDraw.Draw(img).polygon(points, fill=(255, 255, 255), outline=(0, 0, 0), width=max(1, round(scale)))


def draw_badge(img, text, scale):
    if not text:
        return
    f = font(round(22 * scale))
    d = ImageDraw.Draw(img, 'RGBA')
    pad_x, pad_y = 14 * scale, 8 * scale
    box = d.textbbox((0, 0), text, font=f)
    w, h = box[2] - box[0], box[3] - box[1]
    x0, y0 = 16 * scale, img.height - h - 2 * pad_y - 16 * scale
    d.rounded_rectangle((x0, y0, x0 + w + 2 * pad_x, y0 + h + 2 * pad_y), radius=8 * scale,
                        fill=(20, 20, 24, 215), outline=(255, 214, 90, 255), width=max(2, round(2 * scale)))
    d.text((x0 + pad_x - box[0], y0 + pad_y - box[1]), text, font=f, fill=(255, 255, 255, 255))


def annotate(clip, crop, width, workdir):
    """Writes the cropped frames with cursor and badge to workdir as a numbered PNG sequence."""
    folder = FRAMES / clip
    meta = [json.loads(line) for line in (folder / 'frames.jsonl').read_text(encoding='utf-8').splitlines() if line.strip()]
    for i, m in enumerate(meta):
        img = Image.open(folder / m['file']).convert('RGB')
        ox, oy = (crop[0], crop[1]) if crop else (0, 0)
        if crop:
            img = img.crop(crop)
        if m['x'] >= 0:
            draw_cursor(img, m['x'] - ox, m['y'] - oy, 1.6)
        # badge sized for the final width, drawn at full resolution
        draw_badge(img, m['badge'], img.width / width * 0.8)
        img.save(workdir / f'a{i:04d}.png')
    return len(meta)


def encode(workdir, out, width, fps):
    filters = (
        f'fps={fps},scale={width}:-2:flags=lanczos,tpad=stop_mode=clone:stop_duration={HOLD_LAST_S},'
        'split[a][b];[a]palettegen=max_colors=256:stats_mode=full[p];'
        '[b][p]paletteuse=dither=sierra2_4a:diff_mode=rectangle'
    )
    subprocess.run(['ffmpeg', '-y', '-loglevel', 'error', '-framerate', str(FPS), '-i', str(workdir / 'a%04d.png'),
                    '-vf', filters, '-loop', '0', str(out)], check=True)
    return out.stat().st_size


def main():
    if shutil.which('ffmpeg') is None:
        sys.exit('ffmpeg is required (winget install Gyan.FFmpeg)')
    OUT.mkdir(parents=True, exist_ok=True)
    for clip in sys.argv[1:] or list(CLIPS):
        crop, width = CLIPS[clip]
        out = OUT / f'{clip}.gif'
        with tempfile.TemporaryDirectory() as tmp:
            workdir = Path(tmp)
            frames = annotate(clip, crop, width, workdir)
            for factor, fps in ATTEMPTS:
                w = round(width * factor / 2) * 2
                size = encode(workdir, out, w, fps)
                if size <= MAX_BYTES:
                    break
        print(f'{clip}: {frames} frames, {w}px, {fps} fps, {size / 1e6:.2f} MB -> {out}')


if __name__ == '__main__':
    main()
