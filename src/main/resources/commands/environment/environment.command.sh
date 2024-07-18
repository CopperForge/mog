#!/bin/bash

#------------------------------------------------------------------------------
# mog_environment_parse_args()
#------------------------------------------------------------------------------
function mog_environment_parse_args() {
    mog_log_verbose "Config command args: " $@

    options=$(getopt -q -o vshlc: --long help,get:,list,export -- "$@")
    eval set -- "$options"
    while true; do
        case "$1" in
        -h | --help)
            mog_environment_show_usage
            exit 0
            ;;
        --get)
            shift
            mog_environment_cmd_get $1
            shift
            ;;
        --list)
            shift
            mog_environment_cmd_list
            ;;
        --export)
            shift
            mog_environment_cmd_export
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

#------------------------------------------------------------------------------
# mog_environment_cmd_list()
#------------------------------------------------------------------------------
function mog_environment_cmd_list() { 
    declare -a mog_envvars=($(mog_environment_list))
    for mog_env in ${mog_envvars[@]}; do
        mog_echo "${mog_env}"
    done
}

#------------------------------------------------------------------------------
# mog_environment_cmd_get()
#------------------------------------------------------------------------------
function mog_environment_cmd_get() { 
    envk=${1}
    echo $(mog_environment_get ${envk})
}

#------------------------------------------------------------------------------
# mog_environment_cmd_export()
#------------------------------------------------------------------------------
function mog_environment_cmd_export() { 
    declare -a mog_envvars=($(mog_environment_list))
    for mog_env in ${mog_envvars[@]}; do
        mog_key=$(mog_json_mem_select ${mog_env} ".key")
        mog_val=$(mog_json_mem_select ${mog_env} ".value")
        export ${mog_key}="${mog_val}"
    done
}

#==============================================================================
# MAINLINE
#==============================================================================
mog_environment_parse_args $@