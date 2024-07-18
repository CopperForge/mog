#!/bin/bash

#------------------------------------------------------------------------------
# mog_cmd_context_parse_args()
#------------------------------------------------------------------------------
function mog_cmd_context_parse_args() {
    mog_log_verbose "Config command args: " $@

    options=$(getopt -q -o vshlc: --long help,get:,list,switch: -- "$@")
    eval set -- "$options"
    while true; do
        case "$1" in
        -h | --help)
            mog_cmd_context_show_usage
            exit 0
            ;;
        --list)
            shift
            mog_cmd_context_list
            ;;
        --get)
            shift
            echo $(mog_contexts_get ${1})
            shift
            ;;
        --switch)
            shift
            mog_cmd_context_switch ${1}
            shift
            ;;
        --)
            shift
            break
            ;;
        *)
            echo $1
            shift
            ;;
        esac
    done

}

function mog_cmd_context_list() {
    declare -a mog_contexts=($(mog_contexts_list))
    for mog_context in ${mog_contexts[@]}; do
        mog_ctx_name=$(mog_json_mem_value "${mog_context}" "name")
        mog_ctx_path=$(mog_json_mem_value "${mog_context}" "path")
        declare -a mog_ctx_aliases=($(mog_json_mem_select "${mog_context}" ".aliases[]"))
        printf -v mog_ctx_alias_list "%s," ${mog_ctx_aliases[@]}

        mog_echo "${mog_ctx_name} ($(echo ${mog_ctx_alias_list%,})) [${mog_ctx_path}]"
    done
}

function mog_cmd_context_switch() {
    mog_context=($(mog_contexts_get ${1}))
    
    mog_ctx_name=$(mog_json_mem_value "${mog_context}" "name")
    mog_echo "Switching to context ${mog_ctx_name}"

    # set the convdir
    export CONVDIR=$(mog_json_mem_value "${mog_context}" "path")
    export CONTEXT_NAME=${mog_ctx_name}

    # configure any environment vars
    declare -a mog_ctx_envvars=($(mog_json_mem_select ${mog_context} ".environment[]"))
    for mog_ctx_env in ${mog_ctx_envvars[@]}; do
        mog_ctx_key=$(mog_json_mem_select ${mog_ctx_env} ".key")
        mog_ctx_val=$(mog_json_mem_select ${mog_ctx_env} ".value")
        export ${mog_ctx_key}="${mog_ctx_val}"
    done
}

#==============================================================================
# MAINLINE
#==============================================================================
mog_cmd_context_parse_args $@