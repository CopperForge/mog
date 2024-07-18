#!/bin/bash

#------------------------------------------------------------------------------
# mog_dialects_parse_args()
#------------------------------------------------------------------------------
function mog_dialects_parse_args() {
    mog_log_verbose "dialects command args: " $@

    options=$(getopt -q -o h --long help -- "$@")
    eval set -- "$options"
    while true; do
        case "$1" in
        -h | --help)
            mog_config_show_usage
            exit 0
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

#==============================================================================
# MAINLINE
#==============================================================================
mog_dialects_configure
mog_dialects_parse_args $@

declare -a mog_dialects=($(mog_dialects_list))
for dialect in ${mog_dialects[@]}; do
    mog_dialects_describe ${dialect}
done
