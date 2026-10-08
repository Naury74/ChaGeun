#!/usr/bin/env bash
# AAB/APK 안의 네이티브 라이브러리(.so)가 16KB 페이지 크기 기기에서 로드되는지 확인한다.
# Android 15+ 16KB 기기에서는 LOAD 세그먼트 정렬이 16KB(2**14) 이상이어야 한다.
set -euo pipefail

archive="${1:?usage: check-16kb-alignment.sh <app.aab|app.apk>}"
work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT

unzip -q "$archive" '*.so' -d "$work" 2>/dev/null || true
mapfile -t libs < <(find "$work" -name '*.so' | sort)
if [ "${#libs[@]}" -eq 0 ]; then
  echo "네이티브 라이브러리 없음: 16KB 정렬 검사 통과"
  exit 0
fi

failed=0
for lib in "${libs[@]}"; do
  # 32비트 ABI는 16KB 기기 대상이 아니므로 64비트만 본다.
  case "$lib" in *"/arm64-v8a/"* | *"/x86_64/"*) ;; *) continue ;; esac
  # readelf는 정렬 값을 16진수로 내므로 숫자로 바꿔 가장 작은 값을 본다.
  min_align=$(readelf -lW "$lib" | awk '$1 == "LOAD" { print $NF }' | while read -r a; do echo $((a)); done | sort -n | head -n 1)
  if [ -z "$min_align" ] || [ "$min_align" -lt 16384 ]; then
    echo "정렬 부족 ($min_align): ${lib#"$work"/}"
    failed=1
  fi
done

if [ "$failed" -ne 0 ]; then
  echo "16KB 페이지 크기에 맞지 않는 라이브러리가 있습니다."
  exit 1
fi
echo "64비트 네이티브 라이브러리 ${#libs[@]}개 중 정렬 부족 없음"
