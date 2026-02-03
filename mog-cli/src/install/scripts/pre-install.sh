#!/bin/bash
#!/bin/bash

# create group
groupadd -f mog

# create user
id -u mog &>/dev/null || useradd -g mog -m -c "MOG User" mog
