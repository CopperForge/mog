#!/bin/bash

#------------------------------------------------------------------------------
# mog_fasr_parse_args()
#------------------------------------------------------------------------------
function mog_fasr_parse_args() {
    mog_log_verbose "Config command args: " $@
    mog_fasr_rpt_path=$(mog_config_get_value 'fasr.file.reportPath')

    options=$(getopt -q -o vshlc: --long help,generate:,path: -- "$@")
    eval set -- "$options"
    while true; do
        case "$1" in
        -h | --help)
            mog_fasr_show_usage
            exit 0
            ;;
        --generate)
            shift
            MOG_FASR_GENERATE=${1}
            shift
            ;;
        --path)
            shift
            mog_fasr_rpt_path=${1}
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
# mog_fasr_configure()
#------------------------------------------------------------------------------
function mog_fasr_configure() {
    mog_fasr_template_path=$(echo $(mog_config_get_value 'fasr.templatePath') | envsubst)
}

#------------------------------------------------------------------------------
# mog_fasr_generate()
#------------------------------------------------------------------------------
function mog_fasr_generate { 
    if [ -z ${MOG_RUNID+x} ]; then
        mog_log_fatal "RUNID is required for FASR generation".
    fi

    mog_farea_name=${1}
    if [ -z ${mog_farea_name+x} ]; then
        mog_log_fatal "Functional area is required for FASR generation".
    fi

    mog_fasr_farea="$(mog_functional_area_get ${mog_farea_name})"
    mog_fasr_manager="$(mog_json_value ${MOG_CONFIG} "manager.name")"

    declare -a mog_fasr_resources="($(mog_json_mem_select "${mog_fasr_farea}" '.dataAnalysts, .developers | flatten | map("\"" + .name + " [" + .email + "]\"") | .[]'))"

    mog_fasr_date_fmt=$(mog_config_get_value 'fasr.dateFormat')
    mog_fasr_rpt_date=$(date "+${mog_fasr_date_fmt}")
    mog_fasr_source_date=$(date "+${mog_fasr_date_fmt}" -d $CONVERSION_DATE)

    mog_fasr_ts_fmt=$(mog_config_get_value 'fasr.file.timestampFormat')
    mog_fasr_rpt_ts=$(date "+${mog_fasr_ts_fmt}")


    mog_fasr_fn_format=$(mog_config_get_value 'fasr.file.filenameFormat')
    mog_fasr_filename=$(echo ${mog_fasr_fn_format} | farea=${mog_farea_name} report_date=${mog_fasr_rpt_ts} runid=${MOG_RUNID} envsubst)
    mog_fasr_file=$(echo ${mog_fasr_rpt_path}/${mog_fasr_filename} | envsubst)
    if [ ${mog_farea_name} == "all" ]; then
        mog_fasr_search_farea="*"
    else
        mog_fasr_search_farea="${mog_farea_name}"
    fi

    mog_fasr_files="$(find ${DATADIR}/${mog_fasr_search_farea}/*/output/${MOG_RUNID} -name fasr.part)"

    # TODO get conversion date from config in base mog ; MOG_CONVERSION_DATE

    # Generate the combined FASR
    mog_fasr_compile ${mog_farea_name} ${MOG_RUNID} > ${mog_fasr_file}
    chmod 666 ${mog_fasr_file}

}

#------------------------------------------------------------------------------
# mog_fasr_compile()
#------------------------------------------------------------------------------
function mog_fasr_compile() {

    # cat workbook preamble
    cat ${mog_fasr_template_path}/workbook.preamble

    mog_fasr_resource_list=""
    for res in "${mog_fasr_resources[@]}"; do
        mog_fasr_resource_list+="${res}"'\&#10;'
    done

    # cat summary worksheet preamble
    cat ${mog_fasr_template_path}/summary.preamble                           | \
        sed -e "s/@CONVERSION_DATE@/$(date "+${mog_fasr_date_fmt}" -d ${CONVERSION_DATE})/g"         \
            -e "s/@CONVERSION_MANAGER@/""${mog_fasr_manager}""/g"   \
            -e "s/@CONVERSION_RESOURCE@/${mog_fasr_resource_list}/g" \
            -e "s/@FUNCTIONAL_AREA@/${mog_fasr_farea_name}/g"         \
            -e "s/@REPORT_DATE@/${mog_fasr_rpt_date}/g"                 \
            -e "s/@RUN_ID@/${MOG_RUNID}/g"                           \
            -e "s/@SOURCE_DATE@/${mog_fasr_source_date}/g"                 \
            -e "s/@TARGET_DATABASE@/${mog_fasr_target_db}/g"         \
            -e "s/@PROJECT_SUMMARY@/${mog_fasr_project_summary}/g"

    # grep out summary page lines
    for f in ${mog_fasr_files} ; do
    if [ -e ${f} ] ; then
        grep "Block=\"SUM\"" $f
    elif [ -e ${f}".gz" ] ; then
        zcat ${f}".gz" | grep "Block=\"SUM\"" 
    fi
    done

    # cat worksheet postamble
    cat ${mog_fasr_template_path}/worksheet.postamble

    # cat source worksheet preamble
    cat ${mog_fasr_template_path}/source.preamble

    # grep out source page lines, TODO group all of these fors
    for f in ${mog_fasr_files} ; do
        if [ -e ${f} ] ; then
            grep "Block=\"SRC\"" ${f}
        elif [ -e ${f}".gz" ] ; then
            zcat ${f}".gz" | grep "Block=\"SRC\""
        fi

        cat ${mog_fasr_template_path}/row.separator
    done

    # cat worksheet postamble
    cat ${mog_fasr_template_path}/worksheet.postamble

    # cat source worksheet preamble
    cat ${mog_fasr_template_path}/target.preamble

    # grep out target page lines
    for f in ${mog_fasr_files} ; do
        if [ -e ${f} ] ; then
            grep "Block=\"TGT\"" $f
        elif [ -e ${f}".gz" ] ; then
            zcat ${f}".gz" | grep "Block=\"TGT\""
        fi
        
        cat ${mog_fasr_template_path}/row.separator
    done

    # cat worksheet postamble
    cat ${mog_fasr_template_path}/worksheet.postamble

    # cat message worksheet preamble
    cat ${mog_fasr_template_path}/message.preamble

    # grep out message page lines
    for f in ${mog_fasr_files} ; do
        if [ -e ${f} ] ; then
            grep "Block=\"MSG\"" $f
        elif [ -e ${f}".gz" ] ; then
            zcat ${f}".gz" | grep "Block=\"MSG\""
        fi
        
        cat ${mog_fasr_template_path}/row.separator
    done

    # cat worksheet postamble
    cat ${mog_fasr_template_path}/worksheet.postamble

    # cat workbook postamble
    cat ${mog_fasr_template_path}/workbook.postamble

}

#------------------------------------------------------------------------------
# mog_fasr_list()
#------------------------------------------------------------------------------
function mog_fasr_list() {

    mog_fasr_count=$(mog_json_array_length ${MOG_CONFIG} 'functionalAreas')
    mog_fasr_idx=0

    # NOTE: We're doing this the long way because bash arrays and spaces in json values do not get along
    while [ ${mog_fasr_idx} -lt ${mog_fasr_count} ]; do
        mog_farea=$(mog_json_select ${MOG_CONFIG} ".functionalAreas[${mog_fasr_idx}]")
        mog_fasr_idx=$((mog_fasr_idx+1))
        mog_fasr_print "${mog_farea}"
    done

}

#------------------------------------------------------------------------------
# mog_fasr_print()
#------------------------------------------------------------------------------
function mog_fasr_print() {
    mog_farea="${1}"

    mog_fasr_name=$(mog_json_mem_value "${mog_farea}" 'name')
    mog_fasr_desc=$(mog_json_mem_value "${mog_farea}" 'description')

    mog_echo "-------------------------------------------------------------------"
    mog_echo "${mog_fasr_name} :: ${mog_fasr_desc}"
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
mog_fasr_configure
mog_fasr_parse_args $@

if [ -n ${MOG_FASR_GENERATE+x} ]; then 
    mog_fasr_generate ${MOG_FASR_GENERATE}
fi