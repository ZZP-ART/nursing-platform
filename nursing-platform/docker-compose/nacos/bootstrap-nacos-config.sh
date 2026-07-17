#!/bin/sh
set -eu

nacos_url="${NACOS_URL:-http://nacos:8848}"
profile="${SPRING_PROFILES_ACTIVE:?SPRING_PROFILES_ACTIVE is required}"
namespace="${NURSING_NACOS_NAMESPACE:-}"

upload() {
    file="$1"
    data_id=$(basename "$file")
    content=$(cat "$file")
    set -- --data-urlencode "dataId=${data_id}" \
        --data-urlencode "group=NURSING" \
        --data-urlencode "type=yaml" \
        --data-urlencode "content=${content}"
    if [ -n "${namespace}" ]; then
        set -- "$@" --data-urlencode "tenant=${namespace}"
    fi
    curl --fail --silent --show-error --retry 5 --retry-connrefused \
        --request POST "${nacos_url}/nacos/v1/cs/configs" \
        "$@" >/dev/null
    verify_args="dataId=${data_id}&group=NURSING"
    if [ -n "${namespace}" ]; then verify_args="${verify_args}&tenant=${namespace}"; fi
    expected_content=$(tr -d '\r' < "$file")
    attempt=1
    while :; do
        remote_content=$(curl --fail --silent --show-error --retry 5 --retry-connrefused \
            "${nacos_url}/nacos/v1/cs/configs?${verify_args}")
        if [ "$(printf '%s' "$remote_content" | tr -d '\r')" = "$expected_content" ]; then
            break
        fi
        if [ "$attempt" -ge 5 ]; then
            echo "Nacos configuration verification failed for ${data_id}" >&2
            exit 1
        fi
        attempt=$((attempt + 1))
        sleep 1
    done
    echo "Imported Nacos configuration ${data_id}"
}

for file in /configs/*.yaml; do
    case "$(basename "$file")" in
        *-"${profile}".yaml)
            upload "$file"
            ;;
        *-dev.yaml|*-test.yaml|*-prod.yaml)
            ;;
        *)
            upload "$file"
            ;;
    esac
done
