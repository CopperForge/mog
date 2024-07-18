#!/bin/bash

#------------------------------------------------------------------------------
# mog_encrypt_parse_args()
#------------------------------------------------------------------------------
function mog_encrypt_parse_args() {
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
            mog_encrypt_this=$1
            shift
            ;;
        --pass)
            shift
            mog_encrypt_pass=$1
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
mog_encrypt_parse_args $@

if [ -z ${mog_encrypt_pass} ]; then

    if [ -n "${MOG_PASS}" ]; then
        mog_log_verbose "No password provided, using environment var MOG_PASS"
        mog_encrypt_pass=${MOG_PASS}
    elif [ -f ~/.mogpw ]; then
        mog_log_verbose "No password provided, using ~/.mogpw file"
        source ~/.mogpw
        mog_encrypt_pass=${MOG_PASS}
    else
        mog_log_fatal "No password provided!"
    fi

fi

echo $(mog_encryption_encrypt ${mog_encrypt_pass} "${mog_encrypt_this}")
