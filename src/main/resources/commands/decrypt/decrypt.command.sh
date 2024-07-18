#!/bin/bash

#------------------------------------------------------------------------------
# mog_decrypt_parse_args()
#------------------------------------------------------------------------------
function mog_decrypt_parse_args() {
    mog_log_verbose "encrypt command args: " $@

    options=$(getopt -q -o h --long help,value:,pass: -- "$@")
    eval set -- "$options"
    while true; do
        case "$1" in
        -h | --help)
            mog_config_show_usage
            exit 0
            ;;
        --value)
            shift
            mog_decrypt_this=$1
            shift
            ;;
        --pass)
            shift
            mog_decrypt_pass=$1
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

#==============================================================================
# MAINLINE
#==============================================================================
mog_decrypt_parse_args $@

if [ -z ${mog_decrypt_pass} ]; then

    if [ -n "${MOG_PASS}" ]; then
        mog_log_verbose "No password provided, using environment var MOG_PASS"
        mog_decrypt_pass=${MOG_PASS}
    elif [ -f ~/.mogpw ]; then
        mog_log_verbose "No password provided, using ~/.mogpw file"
        source ~/.mogpw
        mog_decrypt_pass=${MOG_PASS}
    else
        mog_log_fatal "No password provided!"
    fi

fi

echo $(mog_encryption_decrypt ${mog_decrypt_pass} "${mog_decrypt_this}")
