$(shell chmod +x tools/*)

$(info ------------------------------------)
UNAME_S := $(shell uname -s)
UNAME_M := $(shell uname -m)
$(info OS       : $(UNAME_S))
$(info Platform : $(UNAME_M))

# System specific tools postfix
POSTFIX.Linux.x86_64 := 
POSTFIX.Darwin.x86_64 := -osx-amd64
POSTFIX.Darwin.arm64 := -osx-arm64
POSTFIX.windows32.i686-pc := .exe

POSTFIX += $(POSTFIX.$(UNAME_S).$(UNAME_M))
$(info Postfix  : $(POSTFIX))
$(info ------------------------------------)

.PHONY: all clean

all: NodOn_SIN-4-1-20.groovy  NodOn_SIN-4-1-21.groovy  NodOn_SIN-4-2-20.groovy  NodOn_SIN-4-FP-21.groovy  NodOn_SIN-4-RS-20.groovy

%.groovy: src/devices/%/config.yaml src/common.yaml src/blueprint.groovy
	tools/yaml-merge$(POSTFIX) src/common.yaml $< | tools/mustache$(POSTFIX) src/blueprint.groovy > $@

clean:
	rm -rf *.groovy
