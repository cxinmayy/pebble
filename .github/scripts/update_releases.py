# .github/scripts/update_releases.py
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
latest_body = (latest.get('body') or '').strip().split('\n')[0][:200] or 'Latest release'

print(f"✅ Latest release: {latest_tag} on {latest_date}")

print("📖 Reading README...")
with open('README.md', 'r', encoding='utf-8') as f:
    content = f.read()
print("✅ README loaded")

# Build the new latest-release block
new_latest_block = (
    f"### 🔥 Latest Release: {latest_tag} ({latest_date})\n\n"
    f"**{latest_body}**\n\n"
    f"[![View {latest_tag}](https://img.shields.io/badge/View_Release-FF6B6B?style=for-the-badge)]"
    f"(https://github.com/cxinmayy/pebble/releases/tag/{latest_tag})\n\n---\n"
)

# Replace the existing Latest Release block (marker -> next '---' horizontal rule)
pattern = r"(### 🔥 Latest Release:.*?)(\n---\n)"
if re.search(pattern, content, flags=re.S):
    content = re.sub(pattern, new_latest_block, content, count=1, flags=re.S)
    print("✅ Replaced Latest Release block using regex")
else:
    # If marker not found, insert the block before the "### All Releases" or before the table
    insert_marker = "### All Releases"
    if insert_marker in content:
        idx = content.find(insert_marker)
        content = content[:idx] + new_latest_block + content[idx:]
        print("⚠️ 'Latest Release' marker not found — inserted new block before 'All Releases'")
    else:
        # fallback: insert near the top after the first '##' heading (safe default)
        first_h2 = content.find("\n## ")
        if first_h2 != -1:
            insert_at = first_h2 + 1
            content = content[:insert_at] + new_latest_block + content[insert_at:]
            print("⚠️ Marker missing — inserted new block near top")

# ================= update version table (unchanged logic, but robust)
start_marker = '<table align="center" width="100%">'
end_marker = '</table>'
start_idx = content.find(start_marker)
end_idx = content.find(end_marker, start_idx)

if start_idx == -1 or end_idx == -1:
    print("❌ ERROR: Table markers not found; aborting table update.")
    with open('README.md', 'w', encoding='utf-8') as f:
        f.write(content)  # still write the latest block changes if any
    raise SystemExit(1)

# find the header end (after the second </tr>)
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

rows = []
for rel in releases:
    tag = rel.get('tag_name', 'unknown')
    color = colors.get(tag, 'CCCCCC')
    pub = rel.get('published_at')
    try:
        date_str = datetime.strptime(pub, '%Y-%m-%dT%H:%M:%SZ').strftime('%B %d, %Y') if pub else ''
    except Exception:
        date_str = pub or ''
    body_text = (rel.get('body') or '').split('\n')[0][:200] or 'Release'
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

# Reconstruct README with new table rows
before = content[:header_end]
after = content[end_idx + len(end_marker):]  # include the closing </table> in replacement
new_table = ''.join(rows) + '\n'
# keep the original table tags by inserting rows between header_end and end_idx
new_content = content[:header_end] + new_table + content[end_idx + len(end_marker):]

with open('README.md', 'w', encoding='utf-8') as f:
    f.write(new_content)

print("✅ README updated successfully!")
