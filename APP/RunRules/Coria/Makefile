CPU_CODE_DIR="../../CPUCode"

# delegates any target to the CPU Code Makefile
%:
	make -C $(CPU_CODE_DIR) RUNRULE="Coria" $@

# default target
all:
	make -C $(CPU_CODE_DIR) RUNRULE="Coria"

installer_no_dependencies:
	echo "This target cannot be handled by this version of the makefile system" > /dev/stderr
