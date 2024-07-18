select 
    RPAD(table_name, 50, ' ')  
        || RPAD('2000     n        n        y       1       -', 55, ' ')
        || RPAD(OWNER, 25, ' ')
        || RPAD(OWNER, 25, ' ') as CTL_ENTRY
from all_tables
where owner = @owner_name
order by table_name
;
