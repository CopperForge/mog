mog_toolset=$(mog_config_get_value 'toolset')
mog_toolset_type=$(mog_json_mem_value "${mog_toolset}" 'type')
mog_toolset_site=$(mog_json_mem_value "${mog_toolset}" 'site')
mog_toolset_api=$(mog_json_mem_value "${mog_toolset}" 'api')
MOG_GITEA_TOKEN=$(mog_json_mem_value "${mog_toolset}" 'token')
MOG_TOOLS_INSTALL_PATH=""
declare -a MOG_REPOS_TO_PROCESS=()
declare -a MOG_REPOS_TO_EXCLUDE=()

#------------------------------------------------------------------------------
# mog_cmd_tools_parse_args()
#------------------------------------------------------------------------------
function mog_cmd_tools_parse_args() {
    mog_log_verbose "config command args: " $@

    MOG_TOOLS_DETAIL=0
    MOG_TOOLS_INSTALL=0
    MOG_REPOS_LIST=0
    MOG_REPOS_DIFF=0
    options=$(getopt -q -o h --long help,list,detail,repo:,exclude-repo:,install-base:,install,diff -- "$@")
    eval set -- "$options"
    while true; do
        case "$1" in
        -h | --help)
            mog_cmd_tools_show_usage
            exit 0
            ;;
        --list)
            shift
            MOG_REPOS_LIST=1
            ;;
        --detail)
            shift
            MOG_TOOLS_DETAIL=1
            ;;
        --repo)
            shift
            MOG_REPOS_TO_PROCESS+=($1)
            shift
            ;;
        --diff)
            shift
            MOG_REPOS_DIFF=1
            ;;
        --install-base)
            shift
            MOG_TOOLS_INSTALL_PATH=${1}
            shift
            ;;
        --install)
            MOG_TOOLS_INSTALL=1
            shift
            ;;
        --exclude-repo)
            shift
            MOG_REPOS_TO_EXCLUDE+=($1)
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

    if [ ${#MOG_REPOS_TO_PROCESS[@]} -le 0 ]; then
        mog_log_verbose "No repositories specified, assuming all"
        MOG_REPOS_TO_PROCESS=($(mog_json_mem_select "${mog_toolset}" '.repositories[] | .name'))
        mog_log_verbose "Processing ${#MOG_REPOS_TO_PROCESS[@]} repositories"
    fi
}

#------------------------------------------------------------------------------
# mog_cmd_tools_diff_repos()
#------------------------------------------------------------------------------
function mog_cmd_tools_remote_diff() {
    mog_org_install_path=$(mog_json_mem_value "${mog_org}" 'installPath')

    mog_echo "Diffing '${mog_org_name}' ..."
    case ${mog_toolset_type} in
    "gitea")
        declare -a mog_gitea_repos
        mog_gitea_api_list_repos ${mog_toolset_api} ${mog_org_name}

        for mog_gitea_repo in "${mog_gitea_repos[@]}"; do
            repo_name=$(mog_json_mem_value "${mog_gitea_repo}" 'name')
            install_path="${MOG_TOOLS_INSTALL_PATH}${mog_org_install_path}/${repo_name}"

            gitea_diff_resp=$(mog_gitea_remote_diff ${install_path})
            mog_gitea_diff_rc=$?

            if [ ${mog_gitea_diff_rc} != 0 ]; then 
                mog_echo ">>>  ${repo_name} :: ${gitea_diff_resp} ..."
            fi
        done

        mog_echo
        ;;

    *) ;;

    esac
    mog_echo
}

#------------------------------------------------------------------------------
# mog_cmd_tools_install_repos()
#------------------------------------------------------------------------------
function mog_cmd_tools_install_repos() {
    mog_org_install_path=$(mog_json_mem_value "${mog_org}" 'installPath')
    
    if [[ $(mog_json_mem_value "${mog_org}" 'enabled') == 'false' ]]; then
        mog_echo "Skipping '${mog_org_name}', disabled."
        mog_echo
        return
    fi

    if [ -z ${mog_org_install_path} ]; then
        mog_echo "Skipping '${mog_org_name}', no installation path specified in config."
        mog_echo
        return
    fi

    mog_echo "Installing '${mog_org_name}' ..."
    case ${mog_toolset_type} in
    "gitea")
        declare -a mog_gitea_repos
        mog_gitea_api_list_repos ${mog_toolset_api} ${mog_org_name}

        for mog_gitea_repo in "${mog_gitea_repos[@]}"; do
            repo_name=$(mog_json_mem_value "${mog_gitea_repo}" 'name')
            clone_url=$(mog_json_mem_value "${mog_gitea_repo}" 'ssh_url')

            install_path="${MOG_TOOLS_INSTALL_PATH}${mog_org_install_path}"

            mog_echo ">>>  Cloning ${repo_name} to ${install_path} ..."
            mog_gitea_clone ${clone_url} "${install_path}/${repo_name}"
            mog_gitea_clone_rc=$?
        done

        mog_echo
        ;;

    *) ;;

    esac
    mog_echo

}

#------------------------------------------------------------------------------
# mog_cmd_tools_list_repos()
#------------------------------------------------------------------------------
function mog_cmd_tools_list_repos() {
    mog_echo "${mog_org_name} [ ${mog_org_install_path} ] provides :"
    case ${mog_toolset_type} in
    "gitea")
        declare -a mog_gitea_repos
        mog_gitea_api_list_repos ${mog_toolset_api} ${mog_org_name}

        for mog_gitea_repo in "${mog_gitea_repos[@]}"; do
            repo_name=$(mog_json_mem_value "${mog_gitea_repo}" 'name')
            clone_url=$(mog_json_mem_value "${mog_gitea_repo}" 'clone_url')

            mog_echo ">>>  ${repo_name} [ ${clone_url} ]"
            if [ ${MOG_TOOLS_DETAIL} == 1 ]; then
                repo_desc="$(mog_json_mem_value "${mog_gitea_repo}" 'description')"
                mog_echo ${repo_desc}
                mog_echo
            fi
        done

        mog_echo
        ;;

    *) ;;

    esac
}

#==============================================================================
# MAINLINE
#==============================================================================
mog_cmd_tools_parse_args $@

for mog_org_name in ${MOG_REPOS_TO_PROCESS[@]}; do
    if [[ ${MOG_REPOS_TO_EXCLUDE[@]} =~ ${mog_org_name} ]]; then
        mog_log_verbose "Excluding ${mog_org_name}"
        continue
    fi

    mog_org="$(mog_config_get_value 'toolset.repositories[] | select(.name == "'${mog_org_name}'")')"
    mog_org_install_path=$(mog_json_mem_value "${mog_org}" 'installPath')

    if [ ${MOG_TOOLS_INSTALL} == 1 ]; then
        mog_cmd_tools_install_repos
    fi

    if [ ${MOG_REPOS_LIST} == 1 ]; then
        mog_cmd_tools_list_repos
    fi

    if [ ${MOG_REPOS_DIFF} == 1 ]; then
        mog_cmd_tools_remote_diff
    fi

done
