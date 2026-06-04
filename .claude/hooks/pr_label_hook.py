import json
import sys
import subprocess
import re

TITLE_LABEL_MAP = {
    'feat':     'feature',
    'fix':      'bug',
    'hotfix':   'hotfix',
    'refactor': 'refactor',
    'docs':     'docs',
    'test':     'test',
    'chore':    'chore',
}

BRANCH_AREA_MAP = {
    'frontend': 'frontend',
    'backend':  'backend',
    'ai':       'AI',
    'infra':    'infra',
}

MILESTONES = ['v0.1.0', 'v0.2.0', 'v0.3.0', 'v1.0.0']


def get_repo_from_git():
    try:
        result = subprocess.run(
            ['git', 'remote', 'get-url', 'origin'],
            capture_output=True, text=True
        )
        url = result.stdout.strip()
        m = re.search(r'github\.com[:/](.+?)(?:\.git)?$', url)
        return m.group(1) if m else None
    except Exception:
        return None


def main():
    raw = sys.stdin.read()
    try:
        payload = json.loads(raw)
    except Exception:
        sys.exit(0)

    if payload.get('tool_name') != 'Bash':
        sys.exit(0)

    command = payload.get('tool_input', {}).get('command', '')
    if 'gh pr create' not in command:
        sys.exit(0)

    tool_response = payload.get('tool_response', '')
    output = tool_response if isinstance(tool_response, str) else json.dumps(tool_response)

    url_match = re.search(r'https://github\.com/\S+/pull/(\d+)', output)
    if not url_match:
        sys.exit(0)

    pr_number = url_match.group(1)
    repo = get_repo_from_git()
    repo_args = ['--repo', repo] if repo else []

    view = subprocess.run(
        ['gh', 'pr', 'view', pr_number, '--json', 'title,headRefName'] + repo_args,
        capture_output=True, text=True
    )
    if view.returncode != 0:
        sys.exit(0)

    pr_info = json.loads(view.stdout)
    title  = pr_info.get('title', '')
    branch = pr_info.get('headRefName', '')

    labels = ['review needed']

    for prefix, label in TITLE_LABEL_MAP.items():
        if title.lower().startswith(prefix + ':'):
            labels.append(label)
            break

    for key, label in BRANCH_AREA_MAP.items():
        if key in branch.lower():
            labels.append(label)
            break

    subprocess.run(
        ['gh', 'pr', 'edit', pr_number, '--add-label', ','.join(labels)] + repo_args,
        capture_output=True
    )

    milestone_list = '  '.join(f'{i+1}) {m}' for i, m in enumerate(MILESTONES))
    print(f'\n✅ 라벨 자동 추가: {", ".join(labels)}')
    print(f'\n📌 마일스톤을 선택해주세요 (번호 입력):')
    print(f'  {milestone_list}')


if __name__ == '__main__':
    main()
