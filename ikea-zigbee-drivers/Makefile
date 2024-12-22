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

all: Aqara_DCM-K01.groovy  Gewiss_GWA1501.groovy  Gewiss_GWA1502.groovy  Ikea_CWS-Lights.groovy  Ikea_DIM-Lights.groovy Ikea_E1603.groovy  Ikea_E1743.groovy  Ikea_E1745.groovy  Ikea_E1746.groovy  Ikea_E1766.groovy  Ikea_E1810.groovy  Ikea_E1812.groovy  Ikea_E1836.groovy  Ikea_E1841.groovy  Ikea_E1842.groovy  Ikea_E2002.groovy  Ikea_E2006.groovy  Ikea_E2013.groovy  Ikea_E2112.groovy Ikea_E2123.groovy  Ikea_E2134.groovy  Ikea_E2201.groovy  Ikea_E2202.groovy  Ikea_E2204.groovy  Ikea_E2206.groovy  Ikea_E2213.groovy  Ikea_RGBO-Light.groovy  Ikea_WS-Lights.groovy Legrand_741811.groovy  Philips_RDM001.groovy  Philips_RWL022.groovy  Schneider_CCTFR6600.groovy  Swann_SWO-KEF1PA.groovy  Tuya_TS0201.groovy  Vimar_02973.groovy  Xiaomi_LYWSD03MMC.groovy

%.groovy: src/devices/%/config.yaml src/common.yaml src/blueprint.groovy
	tools/yaml-merge$(POSTFIX) src/common.yaml $< | tools/mustache$(POSTFIX) src/blueprint.groovy > $@

clean:
	rm -rf *.groovy
