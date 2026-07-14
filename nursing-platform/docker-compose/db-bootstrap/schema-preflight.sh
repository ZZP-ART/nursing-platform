#!/bin/sh
set -eu

query() {
    mysql -h mysql -u "$2" --password="$3" -Nse "$4"
}

require_baseline() {
    database=$1
    username=$2
    password=$3
    tables=$4
    columns=$5

    existing=$(query "$database" "$username" "$password" "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='${database}' AND table_name <> 'flyway_schema_history'")
    [ "$existing" -eq 0 ] && return

    old_ifs=$IFS
    IFS=,
    for table in $tables; do
        found=$(query "$database" "$username" "$password" "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='${database}' AND table_name='${table}'")
        [ "$found" -eq 1 ] || { echo "${database}: legacy schema is missing baseline table ${table}" >&2; exit 1; }
    done
    IFS=';'
    for requirement in $columns; do
        table=${requirement%%:*}
        names=${requirement#*:}
        IFS=,
        for column in $names; do
            found=$(query "$database" "$username" "$password" "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema='${database}' AND table_name='${table}' AND column_name='${column}'")
            [ "$found" -eq 1 ] || { echo "${database}: legacy schema is missing baseline column ${table}.${column}" >&2; exit 1; }
        done
        IFS=';'
    done
    IFS=$old_ifs
}

require_baseline order_db "$NURSING_ORDER_MIGRATION_DB_USERNAME" "$NURSING_ORDER_MIGRATION_DB_PASSWORD" \
  'user_address,order_header,payment_record,order_operation_log,order_sequence,idempotent_record,event_message' \
  'user_address:id,user_id,receiver_name,receiver_phone,tag,province,city,district,detail_address,latitude,longitude,is_default,is_deleted,create_time,update_time;order_header:id,order_no,user_id,source,version,service_item_id,service_spec_id,service_item_name,category_name,spec_name,spec_price,spec_duration,quantity,catalog_snapshot_version,address_id,receiver_name,receiver_phone,address_detail,service_date,service_time_slot,total_amount,status,slot_occupied,remark,cancel_reason,is_deleted,create_time,update_time;payment_record:id,order_id,order_no,user_id,pay_amount,pay_type,pay_status,trade_no,notify_id,pay_time,refund_time,version,is_deleted,create_time,update_time;order_operation_log:id,order_id,order_no,user_id,operator,action,from_status,to_status,remark,create_time;order_sequence:id,stub;idempotent_record:id,idempotent_key,biz_type,user_id,request_fingerprint,biz_id,status,expire_time,create_time;event_message:id,topic,event_key,payload,status,retry_count,next_execute_time,last_error,create_time'

require_baseline feedback_db "$NURSING_FEEDBACK_MIGRATION_DB_USERNAME" "$NURSING_FEEDBACK_MIGRATION_DB_PASSWORD" \
  'review,review_image,complaint,complaint_track,idempotent_record,event_message,review_eligibility' \
  'review:id,order_id,user_id,service_item_id,rating,content,status,is_deleted,create_time,update_time;review_image:id,review_id,image_url,sort_order,is_deleted;complaint:id,order_id,user_id,type,content,images,status,idempotent_key,request_hash,is_deleted,create_time,update_time;complaint_track:id,complaint_id,operator,content,is_deleted,create_time,update_time;idempotent_record:id,idempotent_key,biz_type,subject_id,request_hash,biz_id,status,expire_time,create_time;event_message:id,topic,event_key,payload,status,retry_count,create_time;review_eligibility:order_id,user_id,service_item_id,paid_time,create_time'

echo 'Flyway baseline schema preflight passed.'
