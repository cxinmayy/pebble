import requests
from datetime import datetime

print("🔍 Fetching releases...")
resp = requests.get('https://api.github.com/repos/cxinmayy/pebble/releases')
releases = resp.json()
print(f"✅ Found {len(releases)} releases")

if not releases:
    print("❌ No releases found!")
    exit(1)

# Get latest release
latest = releases[0]
latest_tag = latest['tag_name']
latest_date = datetime.strptime(latest['published_at'], '%Y-%m-%dT%H:%M:%SZ').strftime('%B %d, %Y')
latest_body = latest['body'].split('\n')[0][:80] if latest['body'] else 'Latest Release'

print(f"✅ Latest release: {latest_tag} on {latest_date}")

print("📖 Reading README...")
with open('README.md', 'r', encoding='utf-8') as f:
    content = f.read()
print("✅ README loaded")

# ============== UPDATE LATEST RELEASE SECTION ==============
old_latest = f"### 🔥 Latest Release: {latest_tag} ({latest_date})"
new_latest_section = f"""### 🔥 Latest Release: {latest_tag} ({latest_date})

**{latest_body}**

[![View {latest_tag}](https://img.shields.io/badge/View_Release-FF6B6B?style=for-the-badge)](https://github.com/cxinmayy/pebble/releases/tag/{latest_tag})"""

# Find and replace latest release marker
latest_marker_start = content.find("### 🔥 Latest Release:")
if latest_marker_start != -1:
    latest_marker_end = content.find("---", latest_marker_start) - 1
    old_section = content[latest_marker_start:latest_marker_end].strip()
    content = content.replace(old_section, new_latest_section.strip())
    print("✅ Updated Latest Release section")
else:
    print("⚠️ Latest Release section not found")

# ============== UPDATE VERSION TABLE ==============
start_marker = '<table align="center" width="100%">'
end_marker = '</table>'
start_idx = content.find(start_marker)
end_idx = content.find(end_marker, start_idx)

if start_idx == -1:
    print("❌ ERROR: Table marker not found!")
    exit(1)

# Find header end (after 2nd </tr>)
tr_count = 0
header_end = start_idx
while tr_count < 2:
    pos = content.find('</tr>', header_end)
    if pos == -1:
        print("❌ ERROR: Could not find table header!")
        exit(1)
    header_end = pos + len('</tr>')
    tr_count += 1

print(f"✅ Table found, building rows...")

# Colors
colors = {
    'v2.7.1': 'FF6B6B', 'v2.7': '00D4FF', 'v2.6': '4CAF50', 'v2.5': 'FFD700',
    'v2.4.2': '9C27B0', 'v2.4.1': 'FF9800', 'v2.3': '3498DB', 'v2.2.1': 'E74C3C',
    'v2.2': '1ABC9C', 'v2.1': '34495E', 'v2.0': '2ECC71', 'v1.0': '27AE60'
}

# Build rows
rows = []
for rel in releases:
    tag = rel['tag_name']
    color = colors.get(tag, 'CCCCCC')
    date_str = datetime.strptime(rel['published_at'], '%Y-%m-%dT%H:%M:%SZ').strftime('%B %d, %Y')
    body_text = rel['body'].split('\n')[0][:80] if rel['body'] else 'Release'
    
    row = f'\n  <tr bgcolor="#F5F5F5">\n    <td align="center"><img src="https://img.shields.io/badge/{tag}-{color}?style=flat" /></td>\n    <td>{body_text}</td>\n    <td align="center">{date_str}</td>\n    <td align="center"><a href="https://github.com/cxinmayy/pebble/releases/tag/{tag}">📖</a></td>\n  </tr>'
    rows.append(row)

print(f"✅ Built {len(rows)} rows")

# Write table
before = content[:header_end]
after = content[end_idx:]
new_content = before + ''.join(rows) + '\n' + after

with open('README.md', 'w', encoding='utf-8') as f:
    f.write(new_content)

print("✅ README updated successfully!")
