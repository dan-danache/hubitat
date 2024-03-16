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

POSTFIX += $(POSTFIX.$(UNAME_S).$(UNAME_M))
$(info Postfix  : $(POSTFIX))
$(info ------------------------------------)

.PHONY: all clean

all: DCM-K01.groovy  E1603.groovy  E1743.groovy  E1745.groovy  E1766.groovy  E1810.groovy  E1812.groovy  E1836.groovy  E2002.groovy  E2006.groovy  E2013.groovy  E2112.groovy E2123.groovy  E2134.groovy  E2201.groovy  E2202.groovy  E2213.groovy  ICPSHC24.groovy  RDM001.groovy RWL022.groovy SWO-KEF1PA.groovy

%.groovy: src/devices/%/config.yaml src/common.yaml src/blueprint.groovy
	tools/yaml-merge$(POSTFIX) src/common.yaml $< | tools/mustache$(POSTFIX) src/blueprint.groovy > $@

clean:
	rm -rf *.groovy
