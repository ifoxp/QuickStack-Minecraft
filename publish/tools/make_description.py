"""Assembles the CurseForge description (Markdown) from publish/curseforge/i18n/*.md.

    python publish/tools/make_description.py   # writes publish/curseforge/description.md

The English text comes first with the animations (`![...](GIF:<clip>)` placeholders become links to the WebP files
in the GitHub repository); every translation follows under its own heading, without images.
"""
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
I18N = ROOT / 'publish/curseforge/i18n'
OUT = ROOT / 'publish/curseforge/description.md'
MEDIA_URL = 'https://raw.githubusercontent.com/ifoxp/QuickStack-Minecraft/main/publish/curseforge/media/{}.webp'
REPO_URL = 'https://github.com/ifoxp/QuickStack-Minecraft'

# file stem -> native language name, in the order they appear on the page
LANGUAGES = [
    ('uk', 'Українська'), ('de', 'Deutsch'), ('fr', 'Français'), ('es', 'Español'), ('pt_br', 'Português (Brasil)'),
    ('it', 'Italiano'), ('pl', 'Polski'), ('cs', 'Čeština'), ('tr', 'Türkçe'), ('nl', 'Nederlands'), ('ja', '日本語'),
    ('ko', '한국어'), ('zh_cn', '简体中文'), ('zh_tw', '繁體中文'), ('kk', 'Қазақша'), ('uz', 'Oʻzbekcha'),
    ('ky', 'Кыргызча'), ('az', 'Azərbaycanca'), ('hy', 'Հայերեն'), ('ka', 'ქართული'), ('lt', 'Lietuvių'),
    ('lv', 'Latviešu'), ('et', 'Eesti'), ('ro', 'Română'), ('mn', 'Монгол'),
]


def main():
    english = (I18N / 'en.md').read_text(encoding='utf-8')
    english = re.sub(r'\(GIF:([a-z_]+)\)', lambda m: '(' + MEDIA_URL.format(m.group(1)) + ')', english)
    parts = [english.rstrip(), '', f'Source code, issues and translations: [{REPO_URL}]({REPO_URL})', '', '---', '',
             '## 🌍 Other languages', '', ' · '.join(name for _, name in LANGUAGES), '']
    for stem, name in LANGUAGES:
        text = (I18N / f'{stem}.md').read_text(encoding='utf-8').strip()
        # each translation starts with its own "## QuickStack — ..." heading; demote it under the language name
        body = re.sub(r'^## .*\n', '', text, count=1)
        body = re.sub(r'^### ', '#### ', body, flags=re.M)
        parts += ['---', '', f'### {name}', '', body.strip(), '']
    OUT.write_text('\n'.join(parts) + '\n', encoding='utf-8', newline='\n')
    print(f'{OUT} ({OUT.stat().st_size / 1000:.0f} kB)')


if __name__ == '__main__':
    main()
