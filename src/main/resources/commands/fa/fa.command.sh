#!/bin/bash

#------------------------------------------------------------------------------
# mog_fa_parse_args()
#------------------------------------------------------------------------------
function mog_fa_parse_args() {
    mog_log_verbose "Config command args: " $@

    options=$(getopt -q -o vshlc: --long list,get: -- "$@")
    eval set -- "$options"
    while true; do
        case "$1" in
        --list)
            shift
            mog_fa_list
            ;;
        --get)
            shift
            mog_fa_get ${1}
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

#------------------------------------------------------------------------------
# mog_fa_get()
#------------------------------------------------------------------------------
function mog_fa_get { 
    mog_fa_name=${1}
    mog_farea=$(mog_functional_area_get ${mog_fa_name})
    mog_fa_print "${mog_farea}"
}

#------------------------------------------------------------------------------
# mog_fa_list()
#------------------------------------------------------------------------------
function mog_fa_list() {

    mog_fa_count=$(mog_json_array_length ${MOG_CONFIG} 'functionalAreas')
    mog_fa_idx=0

    # NOTE: We're doing this the long way because bash arrays and spaces in json values do not get along
    while [ ${mog_fa_idx} -lt ${mog_fa_count} ]; do
        mog_farea=$(mog_json_select ${MOG_CONFIG} ".functionalAreas[${mog_fa_idx}]")
        mog_fa_idx=$((mog_fa_idx+1))
        mog_fa_print "${mog_farea}"
    done

}

#------------------------------------------------------------------------------
# mog_fa_print()
#------------------------------------------------------------------------------
function mog_fa_print() {
    mog_farea="${1}"

    mog_fa_name=$(mog_json_mem_value "${mog_farea}" 'name')
    mog_fa_desc=$(mog_json_mem_value "${mog_farea}" 'description')

    mog_echo "-------------------------------------------------------------------"
    mog_echo "${mog_fa_name} :: ${mog_fa_desc}"
    mog_echo "-------------------------------------------------------------------"

    mog_echo
    mog_echo 'Data Analysts:'
    mog_da_count=$(mog_json_mem_array_length "${mog_farea}" 'dataAnalysts')
    mog_da_idx=0
    while [ ${mog_da_idx} -lt ${mog_da_count} ]; do
        mog_da=$(mog_json_mem_select "${mog_farea}" ".dataAnalysts[${mog_da_idx}]")
        mog_da_idx=$((mog_da_idx+1))
        mog_da_name=$(mog_json_mem_value "${mog_da}" 'name')
        mog_da_email=$(mog_json_mem_value "${mog_da}" 'email')
        mog_echo "- ${mog_da_name} <${mog_da_email}>"
    done

    mog_echo
    mog_echo 'Developers:'
    mog_dev_count=$(mog_json_mem_array_length "${mog_farea}" 'developers')
    mog_dev_idx=0
    while [ ${mog_dev_idx} -lt ${mog_dev_count} ]; do
        mog_dev=$(mog_json_mem_select "${mog_farea}" ".developers[${mog_dev_idx}]")
        mog_dev_idx=$((mog_dev_idx+1))
        mog_dev_name=$(mog_json_mem_value "${mog_dev}" 'name')
        mog_dev_email=$(mog_json_mem_value "${mog_dev}" 'email')
        mog_echo "- ${mog_dev_name} <${mog_dev_email}>"
    done
    mog_echo "-------------------------------------------------------------------"
    mog_echo
}

#==============================================================================
# MAINLINE
#==============================================================================
mog_fa_parse_args $@