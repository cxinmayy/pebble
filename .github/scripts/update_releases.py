import requests
import re
from datetime import datetime

print("🔍 Fetching releases...")
resp = requests.get('https://api.github.com/repos/cxinmayy/pebble/releases')
releases = resp.json()
print(f"✅ Found {len(releases)} releases")

if not releases:
    print("❌ No releases found!")
    raise SystemExit(1)

releases = sorted(
    [r for r in releases if 'published_at' in r and r.get('published_at')],
    key=lambda r: r['published_at'],
    reverse=True
)

latest = releases[0]
latest_tag = latest.get('tag_name', 'unknown')
latest_date = datetime.strptime(latest['published_at'], '%Y-%m-%dT%H:%M:%SZ').strftime('%B %d, %Y')


def extract_summary(body: str, max_chars: int = 200) -> str:
    """Return the first meaningful non-heading line from a release body."""
    if not body:
        return ''
    for line in body.strip().splitlines():
        clean = re.sub(r'^#+\s*', '', line).strip()
        clean = re.sub(r'^\*+|\*+$', '', clean).strip()
        clean = re.sub(r'^>\s*', '', clean).strip()
        if not clean or re.fullmatch(r'[Vv]?\d[\d.]*', clean):
            continue
        return clean[:max_chars]
    return ''


def short_date(pub: str) -> str:
    """Format as 'Apr 21, 2026' for compact table display."""
    try:
        return datetime.strptime(pub, '%Y-%m-%dT%H:%M:%SZ').strftime('%b %d, %Y')
    except Exception:
        return pub or ''


latest_summary = extract_summary(latest.get('body') or '') or f'Release {latest_tag}'
print(f"✅ Latest: {latest_tag} — {latest_date}")

print("📖 Reading README...")
with open('README.md', 'r', encoding='utf-8') as f:
    content = f.read()
print("✅ README loaded")

# ── Version badge in header ──────────────────────────────────────────────────
version_num = latest_tag.lstrip('v')
version_badge_pattern = r'(!\[Version\]\(https://img\.shields\.io/badge/Version-)[\d.]+(-[^)]+\))'
content, n = re.subn(version_badge_pattern, rf'\g<1>{version_num}\2', content)
print(f"{'✅' if n else '⚠️'} Version badge {'updated to ' + latest_tag if n else 'not found — skipping'}")

# ── Latest Release block ─────────────────────────────────────────────────────
new_latest_block = (
    f"### 🔥 Latest Release: {latest_tag} ({latest_date})\n\n"
    f"**{latest_summary}**\n\n"
    f"[![Download {latest_tag}](https://img.shields.io/badge/⬇%EF%B8%8F_Download_{latest_tag}-FF6B6B?style=for-the-badge)]"
    f"(https://github.com/cxinmayy/pebble/releases/tag/{latest_tag})\n"
    f"&nbsp;\n"
    f"[![All Releases](https://img.shields.io/badge/📋_All_Releases-555555?style=for-the-badge)]"
    f"(https://github.com/cxinmayy/pebble/releases)\n\n---\n"
)

pattern = r"### 🔥 Latest Release:.*?\n(?:.*\n)*?\s*---\s*\n"
m = re.search(pattern, content, flags=re.IGNORECASE)
if m:
    content = re.sub(pattern, new_latest_block, content, count=1, flags=re.IGNORECASE)
    print("✅ Replaced Latest Release block")
else:
    insert_marker = "### All Releases"
    if insert_marker in content:
        idx = content.find(insert_marker)
        content = content[:idx] + new_latest_block + content[idx:]
    else:
        first_h2 = content.find("\n## ")
        if first_h2 != -1:
            content = content[:first_h2 + 1] + new_latest_block + content[first_h2 + 1:]
        else:
            content = new_latest_block + content
    print("⚠️ Inserted new Latest Release block")

# ── Version table ─────────────────────────────────────────────────────────────
start_marker = '<table align="center" width="100%">'
end_marker = '</table>'

start_idx = content.find(start_marker)
if start_idx == -1:
    print("❌ ERROR: Opening <table> tag not found; aborting.")
    with open('README.md', 'w', encoding='utf-8') as f:
        f.write(content)
    raise SystemExit(1)

end_idx = content.find(end_marker, start_idx)
if end_idx == -1:
    # RECOVERY: </table> was eaten by a previous bug
    print("⚠️ WARNING: </table> missing — recovering...")
    last_tr_end = content.rfind('</tr>', start_idx)
    if last_tr_end == -1:
        print("❌ ERROR: No </tr> tags found; aborting.")
        with open('README.md', 'w', encoding='utf-8') as f:
            f.write(content)
        raise SystemExit(1)
    insert_pos = last_tr_end + len('</tr>')
    content = content[:insert_pos] + '\n' + end_marker + content[insert_pos:]
    end_idx = content.find(end_marker, start_idx)
    print("✅ Recovered missing </table> tag")

# Find header end (after the second </tr>)
tr_count = 0
header_end = start_idx
while tr_count < 2:
    pos = content.find('</tr>', header_end)
    if pos == -1:
        print("❌ ERROR: Could not find table header boundary")
        raise SystemExit(1)
    header_end = pos + len('</tr>')
    tr_count += 1

print("✅ Table found, building rows...")

colors = {
    'v2.7.1': 'FF6B6B', 'v2.7': '00D4FF', 'v2.6': '4CAF50', 'v2.5': 'FFD700',
    'v2.4.2': '9C27B0', 'v2.4.1': 'FF9800', 'v2.3': '3498DB', 'v2.2.1': 'E74C3C',
    'v2.2': '1ABC9C', 'v2.1': '34495E', 'v2.0': '2ECC71', 'v1.0': '27AE60'
}
palette = [
    'FF6B6B', '00D4FF', '4CAF50', 'FFD700', '9C27B0',
    'FF9800', '3498DB', 'E74C3C', '1ABC9C', '34495E', '2ECC71', '27AE60'
]
# Alternating row background colours for the redesigned README
row_bgs = ['#F5F5F5', '#FAFAFA']

rows = []
for i, rel in enumerate(releases):
    tag = rel.get('tag_name', 'unknown')
    color = colors.get(tag, palette[i % len(palette)])
    pub = rel.get('published_at', '')
    date_str = short_date(pub)
    body_text = extract_summary(rel.get('body') or '') or f'Release {tag}'
    bg = row_bgs[i % 2]

    # Mark the very first row (latest) with a "🆕 Latest" sub-label
    version_cell = (
        f'<img src="https://img.shields.io/badge/{tag}-{color}?style=flat-square" />'
        + ('<br><sub>🆕 Latest</sub>' if i == 0 else '')
    )

    row = (
        f'\n  <tr bgcolor="{bg}">\n'
        f'    <td align="center">{version_cell}</td>\n'
        f'    <td>{body_text}</td>\n'
        f'    <td align="center"><sub>{date_str}</sub></td>\n'
        f'    <td align="center"><a href="https://github.com/cxinmayy/pebble/releases/tag/{tag}">📖</a></td>\n'
        f'  </tr>'
    )
    rows.append(row)

print(f"✅ Built {len(rows)} rows")

# Always explicitly write </table> so it is never dropped
new_content = (
    content[:header_end]
    + ''.join(rows)
    + '\n'
    + end_marker
    + content[end_idx + len(end_marker):]
)

with open('README.md', 'w', encoding='utf-8') as f:
    f.write(new_content)

print("✅ README updated successfully!")
