"""Builds the animated images for the README and the CurseForge page from the showcase frames.

    ./gradlew :fabric:runClientGameTest -Pshowcase [-Pclips=quick_stack,wheel]
    python tools/make_media.py [clip...]

QuickStackShowcase renders every clip at 30 fps with exact partial ticks into
build/showcase/<clip>/f00000.png..., the pointer and the key badge already drawn by the
game. This script crops each clip to what matters and writes:
- docs/media/<clip>.webp — full colour, 30 fps; UI clips lossless (README, CurseForge description via a link)
- publish/curseforge/media/<clip>.gif — 256 colours, one palette per clip, for places that only take GIF
"""
import shutil
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
FRAMES = ROOT / 'build/showcase'
OUT = ROOT / 'docs/media'                 # WebP for the README (tracked)
GIF_OUT = ROOT / 'publish/curseforge/media'  # GIF for the CurseForge gallery (local only)

FPS = 30
UI_CROP = (220, 20, 840, 700)  # x, y, width, height in the 1280x720 frame
# crop (or None), output width, and whether the WebP is lossless (screens of pixel UI compress best lossless,
# the moving 3D world best lossy)
CLIPS = {
    'quick_stack': (None, 960, False),
    'lock_slots': (None, 960, False),
    'search': (None, 960, False),
    'chest_buttons': ((180, 20, 1100, 700), 920, True),  # wider: its tooltips run off to the right
    'sort': (UI_CROP, 800, True),
    'shift_sweep': (UI_CROP, 800, True),
    'wheel': (UI_CROP, 800, True),
    'crafting': (None, 1000, True),
}
WEBP_QUALITY = 85
GIF_WIDTH_FACTOR = 0.75  # GIFs are heavier; keep them a bit smaller
HOLD_LAST_S = 1.2


def base_filter(crop, width):
    parts = []
    if crop:
        x, y, w, h = crop
        parts.append(f'crop={w}:{h}:{x}:{y}')
    parts.append(f'scale={width}:-2:flags=lanczos')
    parts.append(f'tpad=stop_mode=clone:stop_duration={HOLD_LAST_S}')
    return ','.join(parts)


def run(args):
    subprocess.run(['ffmpeg', '-y', '-loglevel', 'error', '-framerate', str(FPS), *args], check=True)


def main():
    if shutil.which('ffmpeg') is None:
        sys.exit('ffmpeg is required (winget install Gyan.FFmpeg)')
    OUT.mkdir(parents=True, exist_ok=True)
    GIF_OUT.mkdir(parents=True, exist_ok=True)
    for clip in sys.argv[1:] or list(CLIPS):
        crop, width, lossless = CLIPS[clip]
        frames = str(FRAMES / clip / 'f%05d.png')
        webp = OUT / f'{clip}.webp'
        gif = GIF_OUT / f'{clip}.gif'
        # libwebp_anim stores only what changed between frames, which keeps these clips small
        quality = ['-lossless', '1', '-compression_level', '4'] if lossless else ['-quality', str(WEBP_QUALITY), '-compression_level', '6']
        run(['-i', frames, '-vf', base_filter(crop, width), '-c:v', 'libwebp_anim', *quality, '-loop', '0', str(webp)])
        gif_width = round(width * GIF_WIDTH_FACTOR / 2) * 2
        run(['-i', frames, '-vf', base_filter(crop, gif_width) + ',split[a][b];[a]palettegen=max_colors=256:stats_mode=full[p];'
             '[b][p]paletteuse=dither=sierra2_4a:diff_mode=rectangle', '-loop', '0', str(gif)])
        print(f'{clip}: webp {webp.stat().st_size / 1e6:.2f} MB ({width}px), gif {gif.stat().st_size / 1e6:.2f} MB ({gif_width}px)')


if __name__ == '__main__':
    main()
