######################################################################
# Maxeler Technologies: BrainNetwork                                 #
#                                                                    #
# Version: 1.2                                                       #
# Date:    05 July 2013                                              #
#                                                                    #
# App-wide Makefile.                                                 #
#                                                                    #
######################################################################

RUNRULE ?= Vectis

DFE_RULE_DIR=dfe/RunRules/$(RUNRULE)
GUI_DIR=gui

NAME_BIN=compute_correlation
NAME_LIB=libdfe.so
NAME_JAR=brain_network.jar

TARGET_BIN=$(addprefix ./../PLATFORMS/$(RUNRULE)/bin/, $(NAME_BIN)-$(RUNRULE))
ORIG_BIN=$(addprefix $(DFE_RULE_DIR)/binaries/, $(NAME_BIN))

TARGET_LIB=./../PLATFORMS/$(RUNRULE)/lib/libdfe-$(RUNRULE).so
ORIG_LIB=$(addprefix $(DFE_RULE_DIR)/binaries/, $(NAME_LIB))

TARGET_JAR=$(addprefix ./../PLATFORMS/$(RUNRULE)/bin/, $(NAME_JAR))
ORIG_JAR=$(addprefix $(GUI_DIR)/, $(NAME_JAR))

.PHONY : install clean distclean

install : $(TARGET_BIN) $(TARGET_JAR) $(TARGET_LIB)

$(ORIG_BIN) $(ORIG_LIB):
	make -C $(DFE_RULE_DIR)

$(ORIG_JAR):
	@cd $(GUI_DIR); ant build-jar; cd -

$(TARGET_BIN) $(TARGET_LIB): $(ORIG_BIN) $(ORIG_LIB)
	@mkdir -p ./../PLATFORMS/$(RUNRULE)/bin
	@rm -f $(TARGET_BIN)
	cp $(ORIG_BIN) $(TARGET_BIN)
	@mkdir -p ./../PLATFORMS/$(RUNRULE)/lib
	@rm -f $(TARGET_LIB)
	cp $(ORIG_LIB) $(TARGET_LIB)

$(TARGET_JAR): $(ORIG_JAR)
	@mkdir -p ./../PLATFORMS/$(RUNRULE)/bin
	@rm -f $(TARGET_JAR)
	cp $(ORIG_JAR) ./../PLATFORMS/$(RUNRULE)/bin

clean:
	@echo Cleaning RUNRULE=$(RUNRULE)
	make clean -C $(DFE_RULE_DIR)
	cd $(GUI_DIR); ant clean; cd -
	@rm -f $(TARGET_BIN) $(TARGET_JAR) $(TARGET_LIB)
	@{ [ -d bin ] && rmdir --ignore-fail-on-non-empty bin ; } || true
	@{ [ -d lib ] && rmdir --ignore-fail-on-non-empty lib ; } || true

distclean: clean
	make distclean -C $(DFE_RULE_DIR)
