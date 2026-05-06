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

# Ensure releases are sorted by published_at (newest first)
releases = sorted(
    [r for r in releases if 'published_at' in r and r.get('published_at')],
    key=lambda r: r['published_at'],
    reverse=True
)

latest = releases[0]
latest_tag = latest.get('tag_name', 'unknown')
latest_date = datetime.strptime(latest['published_at'], '%Y-%m-%dT%H:%M:%SZ').strftime('%B %d, %Y')

# Strip leading markdown heading symbols (e.g. "# V2.7.2" → "V2.7.2")
raw_latest_body = (latest.get('body') or '').strip().split('\n')[0][:200]
latest_body = re.sub(r'^#+\s*', '', raw_latest_body).strip() or 'Latest release'

print(f"✅ Latest release: {latest_tag} on {latest_date}")

print("📖 Reading README...")
with open('README.md', 'r', encoding='utf-8') as f:
    content = f.read()
print("✅ README loaded")

# ================= Update Latest Release block =================
new_latest_block = (
    f"### 🔥 Latest Release: {latest_tag} ({latest_date})\n\n"
    f"**{latest_body}**\n\n"
    f"[![View {latest_tag}](https://img.shields.io/badge/View_Release-FF6B6B?style=for-the-badge)]"
    f"(https://github.com/cxinmayy/pebble/releases/tag/{latest_tag})\n\n---\n"
)

pattern = r"### 🔥 Latest Release:.*?\n(?:.*\n)*?\s*---\s*\n"
m = re.search(pattern, content, flags=re.IGNORECASE)
if m:
    print("ℹ️ Found existing Latest Release block; replacing it.")
    content = re.sub(pattern, new_latest_block, content, count=1, flags=re.IGNORECASE)
    print("✅ Replaced Latest Release block using flexible regex")
else:
    insert_marker = "### All Releases"
    if insert_marker in content:
        idx = content.find(insert_marker)
        content = content[:idx] + new_latest_block + content[idx:]
        print("⚠️ 'Latest Release' marker not found — inserted new block before 'All Releases'")
    else:
        first_h2 = content.find("\n## ")
        if first_h2 != -1:
            insert_at = first_h2 + 1
            content = content[:insert_at] + new_latest_block + content[insert_at:]
            print("⚠️ Marker missing — inserted new block near top")
        else:
            content = new_latest_block + content
            print("⚠️ Marker missing — prepended Latest Release block to the top")

# ================= Update version table =================
start_marker = '<table align="center" width="100%">'
end_marker = '</table>'

start_idx = content.find(start_marker)
if start_idx == -1:
    print("❌ ERROR: Opening <table> tag not found; aborting table update.")
    with open('README.md', 'w', encoding='utf-8') as f:
        f.write(content)
    raise SystemExit(1)

end_idx = content.find(end_marker, start_idx)

if end_idx == -1:
    # RECOVERY: </table> was eaten by the previous bug — patch it back in after the last </tr>
    print("⚠️ WARNING: </table> missing (previous bug) — recovering...")
    last_tr_end = content.rfind('</tr>', start_idx)
    if last_tr_end == -1:
        print("❌ ERROR: Could not find any </tr> tags; aborting.")
        with open('README.md', 'w', encoding='utf-8') as f:
            f.write(content)
        raise SystemExit(1)
    insert_pos = last_tr_end + len('</tr>')
    content = content[:insert_pos] + '\n' + end_marker + content[insert_pos:]
    end_idx = content.find(end_marker, start_idx)
    print("✅ Recovered missing </table> tag")

# Find the header end (after the second </tr>)
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

# Known colors; new/unknown tags cycle through palette automatically
colors = {
    'v2.7.1': 'FF6B6B', 'v2.7': '00D4FF', 'v2.6': '4CAF50', 'v2.5': 'FFD700',
    'v2.4.2': '9C27B0', 'v2.4.1': 'FF9800', 'v2.3': '3498DB', 'v2.2.1': 'E74C3C',
    'v2.2': '1ABC9C', 'v2.1': '34495E', 'v2.0': '2ECC71', 'v1.0': '27AE60'
}
palette = [
    'FF6B6B', '00D4FF', '4CAF50', 'FFD700', '9C27B0',
    'FF9800', '3498DB', 'E74C3C', '1ABC9C', '34495E', '2ECC71', '27AE60'
]

rows = []
for i, rel in enumerate(releases):
    tag = rel.get('tag_name', 'unknown')
    color = colors.get(tag, palette[i % len(palette)])
    pub = rel.get('published_at')
    try:
        date_str = datetime.strptime(pub, '%Y-%m-%dT%H:%M:%SZ').strftime('%B %d, %Y') if pub else ''
    except Exception:
        date_str = pub or ''

    # Strip markdown heading symbols from body text
    raw_body = (rel.get('body') or '').split('\n')[0][:200]
    body_text = re.sub(r'^#+\s*', '', raw_body).strip() or 'Release'

    row = (
        f'\n  <tr bgcolor="#F5F5F5">\n'
        f'    <td align="center"><img src="https://img.shields.io/badge/{tag}-{color}?style=flat" /></td>\n'
        f'    <td>{body_text}</td>\n'
        f'    <td align="center">{date_str}</td>\n'
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
