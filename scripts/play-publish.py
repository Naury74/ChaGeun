#!/usr/bin/env python3
"""Google Play Developer API로 차근을 올린다.

upload-internal: AAB와 R8 mapping을 올리고 Internal 트랙에 넣는다.
promote: Internal에 올린 versionCode를 Production 단계적 배포로 옮긴다. Play는 같은 versionCode를
다시 올릴 수 없으므로 AAB를 재업로드하지 않고 트랙만 바꾼다.

access token은 google-github-actions/auth가 OIDC로 받은 단기 토큰(PLAY_ACCESS_TOKEN)을 쓴다.
"""
import argparse
import json
import os
import urllib.request

API = "https://androidpublisher.googleapis.com/androidpublisher/v3/applications"
UPLOAD_API = "https://androidpublisher.googleapis.com/upload/androidpublisher/v3/applications"


def call(method, url, body=None, raw=None):
    if raw is not None:
        data, content_type = raw, "application/octet-stream"
    else:
        data, content_type = (json.dumps(body).encode() if body is not None else None), "application/json"
    request = urllib.request.Request(url, data=data, method=method)
    request.add_header("Authorization", f"Bearer {os.environ['PLAY_ACCESS_TOKEN']}")
    request.add_header("Content-Type", content_type)
    with urllib.request.urlopen(request) as response:
        text = response.read().decode()
        return json.loads(text) if text else {}


def set_track(package, edit_id, track, release):
    call("PUT", f"{API}/{package}/edits/{edit_id}/tracks/{track}", {"track": track, "releases": [release]})


def upload_internal(args):
    edit_id = call("POST", f"{API}/{args.package}/edits", {})["id"]
    with open(args.bundle, "rb") as bundle:
        version_code = call(
            "POST", f"{UPLOAD_API}/{args.package}/edits/{edit_id}/bundles?uploadType=media", raw=bundle.read()
        )["versionCode"]
    with open(args.mapping, "rb") as mapping:
        call(
            "POST",
            f"{UPLOAD_API}/{args.package}/edits/{edit_id}/apks/{version_code}/deobfuscationFiles/proguard"
            "?uploadType=media",
            raw=mapping.read(),
        )
    set_track(args.package, edit_id, "internal", {
        "name": args.release_name,
        "versionCodes": [str(version_code)],
        "status": "completed",
    })
    call("POST", f"{API}/{args.package}/edits/{edit_id}:commit")
    print(f"internal 업로드: versionCode {version_code}")


def promote(args):
    edit_id = call("POST", f"{API}/{args.package}/edits", {})["id"]
    set_track(args.package, edit_id, "production", {
        "name": args.release_name,
        "versionCodes": [args.version_code],
        "status": "inProgress",
        "userFraction": args.user_fraction,
        "inAppUpdatePriority": args.update_priority,
    })
    call("POST", f"{API}/{args.package}/edits/{edit_id}:commit")
    print(f"production {args.user_fraction:.0%} 배포: versionCode {args.version_code}, 우선순위 {args.update_priority}")


def main():
    parser = argparse.ArgumentParser()
    commands = parser.add_subparsers(dest="command", required=True)
    upload = commands.add_parser("upload-internal")
    upload.add_argument("--bundle", required=True)
    upload.add_argument("--mapping", required=True)
    move = commands.add_parser("promote")
    move.add_argument("--version-code", required=True)
    move.add_argument("--user-fraction", type=float, default=0.05)
    move.add_argument("--update-priority", type=int, default=0)
    for command in (upload, move):
        command.add_argument("--package", required=True)
        command.add_argument("--release-name", required=True)
    args = parser.parse_args()
    upload_internal(args) if args.command == "upload-internal" else promote(args)


if __name__ == "__main__":
    main()
