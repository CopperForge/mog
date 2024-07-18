declare -a mog_projects_to_process
declare -a mog_projects_to_exclude

#------------------------------------------------------------------------------
# mog_cmd_project_parse_args()
#------------------------------------------------------------------------------
function mog_cmd_project_parse_args() {
    mog_log_verbose "Config command args: " $@
    mog_cmd_project_rpt_path=$(mog_config_get_value 'fasr.file.reportPath')
    mog_proj_get=0
    mog_proj_sync=0
    mog_proj_create=0
    
    options=$(getopt -q -o vshlc: --long help,tag:,tag-message:,project:,list,get,create,synchronize,release:,exclude-project: -- "$@")
    eval set -- "$options"
    while true; do
        case "$1" in
        -h | --help)
            mog_cmd_project_show_usage
            exit 0
            ;;
        --tag)
            shift
            mog_proj_tag="${1}"
            shift
            ;;
        --project)
            shift
            mog_projects_to_process+=(${1})
            shift
            ;;
        --exclude-project)
            shift
            mog_projects_to_exclude+=(${1})
            shift
            ;;
        --get)
            shift
            mog_proj_get=1
            ;;
        --synchronize)
            shift
            mog_proj_sync=1
            ;;
        --release)
            shift
            mog_proj_push=1
            ;;
        --create)
            mog_proj_create=1
            shift
            ;;
        --list)
            mog_cmd_project_list
            shift
            return 0
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

    if [ ${#mog_projects_to_process[@]} -eq 0 ]; then
        mog_project_files=($(mog_projects_list))
        for mogf in ${mog_project_files[@]}; do
            declare -A mog_project
            mog_projects_parse_mogf ${mogf}
            mog_projects_to_process+=(${mog_project[name]})
        done
    fi

}

#------------------------------------------------------------------------------
# mog_cmd_project_list()
#------------------------------------------------------------------------------
function mog_cmd_project_list() {
    mog_echo "Checking ${MOG_PROJECT_HOME} ..."
    declare -a mog_projects=($(mog_projects_list))

    for mogf in "${mog_projects[@]}"; do
        declare -A mog_project
        mog_projects_parse_mogf ${mogf}
        mog_echo "Project: ${mog_project[name]}, Functional Area: ${mog_project[owner]}"
        mog_gitea_get_project ${mog_project[owner]} ${mog_project[repo]}
    done
}


#==============================================================================
# MAINLINE
#==============================================================================
mog_cmd_project_parse_args $@

for mog_proj in ${mog_projects_to_process[@]}; do
    if [[ ${mog_projects_to_exclude[@]} =~ ${mog_proj} ]]; then
        continue
    fi
    
    declare -A mog_project
    mog_log_verbose "Finding project ${mog_proj}..."
    mogf="$(mog_projects_find ${mog_proj})"
    mog_echo "file=${mogf}"
    mogd=$(dirname ${mogf})
    mog_echo "path=${mogd}"
    mogf=$(basename ${mogf})
    mogs="$(mog_projects_get "${mogd}/${mogf}")"
    mog_projects_parse_mogs "${mogs}" 

    if [ ${mog_proj_get} -eq "1" ]; then
        mog_git_status ${mogd}
        mog_git_pull ${mogd} ${mog_project[defaultBranch]} ${mog_project[defaultBranch]}
    fi

    if [ ${mog_proj_sync} -eq "1" ]; then
        # echo "cloneUrl=${mog_project[cloneUrl]}"
        mog_git_execute_cmd ${mogd} "remote set-url origin ${mog_project[cloneUrl]}"
        mog_git_execute_cmd ${mogd} "branch -m ${mog_project[defaultBranch]}"
        mog_git_execute_cmd ${mogd} "add ."
        mog_git_execute_cmd ${mogd} "commit -m \"Added project mog; initialized to new devops environment\" ."
        mog_git_push ${mogd} ${mog_project[defaultBranch]} ${mog_project[defaultBranch]}
    fi

    if [ "${mog_proj_create}" -eq "1" ]; then
        mog_gitea_create_project ${mog_project[owner]} ${mog_project[repo]}
    fi

    if [ ! -z ${mog_proj_tag} ]; then
        mog_echo "Tagging project ${mog_proj} with ${mog_proj_tag} ..."
        mog_projects_tag_project ${mog_proj} ${mog_proj_tag} ${mog_tag_msg}
    fi

    if [ ! -z ${mog_proj_release} ]; then
        mog_echo "Releasing project ${mog_proj} as ${mog_proj_release} ..."
        mog_projects_release_project ${mog_proj} ${mog_proj_release} ${mog_tag_msg}
    fi
done

