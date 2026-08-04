#!/usr/bin/env python3
"""Mirror the Error Prone warnings from a build log into a single GitHub issue.

Warnings do not fail the build, so without this they are invisible. One issue is
kept up to date rather than a new one raised per merge; it closes when the build
comes back clean and reopens if warnings return.
"""

import json
import os
import re
import sys
import urllib.request

TITLE = "Error Prone warnings"
LABEL = "code-quality"
ASSIGNEE = "jamiewmeldrum"

WARNING = re.compile(r"^(.*?):(\d+): warning: \[([^\]]+)\] (.*)$")


def api(method, path, payload=None, params=""):
    url = f"https://api.github.com/repos/{os.environ['GITHUB_REPOSITORY']}{path}{params}"
    request = urllib.request.Request(
        url,
        method=method,
        data=json.dumps(payload).encode() if payload else None,
        headers={
            "Authorization": f"Bearer {os.environ['GITHUB_TOKEN']}",
            "Accept": "application/vnd.github+json",
            "Content-Type": "application/json",
            "X-GitHub-Api-Version": "2022-11-28",
        },
    )
    with urllib.request.urlopen(request) as response:
        return json.loads(response.read() or "null")


def parse(log_path):
    warnings = []
    workspace = os.environ.get("GITHUB_WORKSPACE", "")
    with open(log_path, encoding="utf-8", errors="replace") as log:
        for line in log:
            match = WARNING.match(line.rstrip())
            if not match:
                continue
            path, line_no, check, message = match.groups()
            if workspace and path.startswith(workspace):
                path = path[len(workspace) :].lstrip("/")
            warnings.append((path, int(line_no), check, message))
    return sorted(set(warnings))


def run_url():
    return (
        f"{os.environ['GITHUB_SERVER_URL']}/{os.environ['GITHUB_REPOSITORY']}"
        f"/actions/runs/{os.environ['GITHUB_RUN_ID']}"
    )


def render_clean():
    return f"No Error Prone warnings on `main` as of [this run]({run_url()})."


def render(warnings):
    run = run_url()
    lines = [
        f"**{len(warnings)}** warning(s) on `main` as of [this run]({run}).",
        "",
        "| Check | Location | Message |",
        "|---|---|---|",
    ]
    for path, line_no, check, message in warnings:
        check_link = f"[{check}](https://errorprone.info/bugpattern/{check})"
        lines.append(f"| {check_link} | `{path}:{line_no}` | {message} |")
    return "\n".join(lines)


def find_issue():
    # The issues endpoint returns pull requests too; they carry a `pull_request` key.
    issues = api("GET", "/issues", params=f"?labels={LABEL}&state=all&per_page=100")
    for issue in issues:
        if "pull_request" not in issue and issue["title"] == TITLE:
            return issue
    return None


def main():
    warnings = parse(sys.argv[1])
    issue = find_issue()

    if not warnings:
        if issue and issue["state"] == "open":
            api(
                "PATCH",
                f"/issues/{issue['number']}",
                {"body": render_clean(), "state": "closed"},
            )
            print(f"No warnings - closed #{issue['number']}.")
        else:
            print("No warnings.")
        return 0

    body = render(warnings)

    if issue:
        api("PATCH", f"/issues/{issue['number']}", {"body": body, "state": "open"})
        print(f"{len(warnings)} warning(s) - updated #{issue['number']}.")
    else:
        created = api(
            "POST",
            "/issues",
            {
                "title": TITLE,
                "body": body,
                "labels": [LABEL],
                "assignees": [ASSIGNEE],
            },
        )
        print(f"{len(warnings)} warning(s) - created #{created['number']}.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
