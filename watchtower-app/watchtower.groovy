/*
 * Watchtower app for Hubitat
 *
 * Data-driven insights for a smarter home.
 *
 * @see https://github.com/dan-danache/hubitat
 */
import groovy.json.JsonBuilder
import groovy.transform.CompileStatic
import groovy.transform.Field

import java.math.RoundingMode
import java.nio.file.NoSuchFileException
import java.time.DayOfWeek
import java.time.ZonedDateTime
import java.time.ZoneId
import java.util.Collections

import com.hubitat.app.DeviceWrapper
import com.hubitat.hub.domain.Event

@Field static final String APP_NAME = 'Watchtower'
@Field static final String APP_VERSION = '1.1.0'
@Field static final def URL_PATTERN = ~/^https?:\/\/[^\/]+(.+)/
@Field static final String APP_ICON = 'iVBORw0KGgoAAAANSUhEUgAAAgAAAAIABAMAAAAGVsnJAAAAJ1BMVEVHcEyAugB6tACAuwB5sgB/uwB4sgB/uwB2rQCAvABypgB3rgCAvADOha9yAAAADHRSTlMAGTtVbYaeudPl8vuPQkR9AAAV4UlEQVR42uzWMWsUQRjG8WfmLv2ht3ftBI3XLkQ8061BUMHiFkEs0mwlgTS5gFpsIRfstjMkCPtxkt0sPB9KMjuFg50w7sK8v2ZgmuE/uwwvhBBCCCGEEEIIIYQQQvyzJx+/fjlGtOYlyTYFsI8I6U8k2RqoF1UBKEQmqVz/vOxy4NAgKuotH3SpWvM+w+PPkfVPyr4/02dsU/WmNWvEJKlp5UnNxhxUrXlJg3gs2CvWZPO8ZGtW7AyisWTv4tQutp/n8X3/m+qP/t3TVwhgko63v2PvPl2Q16csEEKSY2QS+ro0IbuKO4SRXGBU9mp6uuxhp+YtoF/vI4Dk2mA8pn/19zvtDLokf5gQZ96kGAtd0dNtJrVdU9tPfkcA+luGcVAlfXmfzY3rb2YIQf/KgUcY3gl95+5Gdm40DjYMLZlDHWJoR/Rt1ZldbzGp3I8QyrO6gPqAYS3pu1Qn7B9A9zQWCEcfDH4DU/qu8M4NAq5/G/j8AvoYw9ElPXc4opW7/ksEluRQGM57eprZitbW9V8huEWO4azoac2C1k+vP7DlZiwPQJfu0Wrmff/dDP/DKhvJBLiZ1r+ZsX/WtoEwjuPfk2x5vQ6N3E2FQtF2dKohg0oDbSGDQ2m7eIihzZQhspt/4CUt3jTJQxa9HMeyQS8qnLCHC3iz7vR5Bz94nnueeyqtfGc1P3yJWvEATPyi0jYnxfZBwBLxS7o5ARgWu4nwZ1sHEdZ41xLrvMIcgK8uq9qN/fzgn7tugPLtqKo9usj/yfkKvFHfqtraRX6OJ64bYDwwToKlwiIPr0ipCUcNsDDrYa2wyftPWCXUfkisCMwH4PWLfsCueMKolGh+7uIIdFIY+RMsE7MzL1tSixMsiI3888xtfujoLTxFE7eSxvlmA5jlcIYDYSnDbed1cstXwE3mPj8MlgxWEm2gaFjPSPxiHuLIKBWXOZqY0yyRVXvluCLulV8M0YKURr2p9nrCHf9RHq0jtK/S2g5oKiMc6uYcL9G8BeDRkONqrwSn4qHIUrR+AmJoYQSaUtwStzLYRGgXwJFsfASalrjW2TUBnRTEGITkwLrVPmuJc3EisnO0jxLCCMYc2KzaZ4h74oaglADePxBTCKLmdiBTTht0HjjN0cIEegomlgpgRTvEyisUgLgG/gp6igMKWzgBTWJO/wmtm0Dve8S0oTOA6YGW8N9fMRuyc/GBUFkogBVtIeZ3sluyE/xWYmrhF6RojfAu5/SKnZ+f6SsOpN/SCWC636hn6uyeRWogjAP4f5LN5ezGQ2EDFuEUdWGLcCoopDgVwSLFInIgWAQ8C2GLLCoqbHMHIsIVzvrW7KfJbjYb+H8omcB1t4WXWZj5ddMl8Mw8b34tpZDQgreH4hRmiDkvVktYJOASg+LT1a9oHR2hH285AHJYJePI+xvNohNoveNcnG43BSxgF581ovc8aGJoD56hL7dZBDYxLDPkRPxNF8MZNP9lIibbLAInsI03b2T/mPeaGNq9p7gjtxcAK9hnQCV+Z4t0hlaQeBN0Nt7YBNpH6BB4yYNGovUGd9DVjhMv4LmISvzKFqlCK0y8eFuDoBiwNASi+7y1lhLaezxDN73NNaC1IfAzK6dFD1o/DkboJN08BrM2BAY3+agWeXs87dgVe5tToL0h4M3S1by4Da0v+/EWlkE1rOXpENgnV37eHguhttAH57DXgCe+GpKjI2gHeGi+CKpgMY9r+WqPXO6M0Apy40XQIWw25GTnZEgmH4HWB9PLkCWs5rPG5z2yjJ7E0HZjwzkwgd0yFmGRstmrFDShzObABSwXsBbffFKlXyS0u2YnQQlsN2YeFSnrHgtovdzkIGAB64WsxB+fLMY1Wscmn8AE1hNnPBzkr1mFzKGF0twTuIQDIi691Q75+KzCpQkHa4BzHpmk+RkXEQ8NtwEVnJCy7NUpuc/S8Cx0BCcEZDz+Tqq0kddMTkJWcMSUs12S6+sswhiXcYMXKeCIiM1VkiymladwGXNeYA1XCFJlJKtdxs+lsWGwgjNS1iFJPp6rsDBVBDQSzgjIFyRZpivx29RGvIRDpqymJJvrTIaJoVFQAodEZEtNq0CZWYdUcIkgW3XEk09mJgEFnJKSrTesoxH+z65lObBTN7sY86A0cQMUHHNGtgZUr6SBRjCG5uAz+JNVmHe/AUu4xidbqyn3S/yPzKptUPe1xpTqXecb0Eg45wrPrbO8axVUwj3eJb8/c7cK3PwnTccbsIIbNsfyqFsn/AP/2juf5ziKK473/FiNltJhwQJFjg5rmxQ4NYdN7ECZ2sMa20VR2YP44WDIHJQYlxO8B5FgFw57UFJFQmwd5JBAUjUHDA6pknXhEELJOngj7a4WvT+K4gaj+U7PTk9rpp/2Wxw4qKztN59+7/tet2bNEO7peiKtfGACDN8DQ6Vp6I4wU16GLOaCPsjwPZB+F88DE2D8HugrNELbwlR5Yz9FS78Nth6Nkx7IrLFX4eneAbN/eBCv9ctCg4JxzWBT8w6orG8REe3Rt/rO/xLtbX4m8tf0uIVwVe8OsG4R1qaGjtMe08y4mnfA0S1K0Oe6euL0xbyqeQfcpiR93dI0F0q/kECvC3I2KFF3tb4BdzfjiVCOO+ARSta/9cxG07vhiuY+4MeUrK/1vv1mLdOtgBWRn34uC0BNqxmUJ4FAcyd8TRaAulYzOJL+rO5Z0G1ZABp6C2EjQwq4oyEhQe21hNZCuJYhBbQ0BECzEcCPdXv8FDASxhMgQuwE5C5gWxhIAJ6KNMZuBJYZEFDFy5E3AnUGBLhpZwJtUASNIwAngeG4lyN7ggEBop2OaAdcjjafgCoa7sjHgTUWBLjpkPbBzTjzCRBhqhV1gA82nwARAKalnVCLCQELabJaBfhgDgRU0vRDVZACOBBgpfH2TZACOBAguin6oS5wARwIEL7cCtngRIQHAdPyfmgKpABDCFC3QlXQCPAgwApxf4dbwSWTCcBWKH0OrPMhQPjQ3WEfOBSMCPBkJz0VcD2YCwF4KoRz4AonAvBUCPfCLbMJwL1uLWUvLFgR4EsMbghsEBsCpkFDiOeBPV4EuKAhxPPAZV4EWKC+42PRBgMCgNOrpRkGjAQDAsASW2mM8EAwI2AmMQsCH8iJgCmwNlQEVrgR4AC6URFocSNAJOW3GXAoxoQA+WWpJuiFWRHQTPA4HXA1xmwCMOYP5Z1AjxEB8iujNpgH8iLAwRu8AowwLwIs3OpPgyLAiwDRhc93ARQBXgSINtzhTVAEmBGwAMtABxQBZgRU4QMOwTSEGQEVNO+zQCfAjQAbdQMuKALcCBBogR4YB3EjQHQB4lUwEmdHQBskOR+Mg9gR4IOpWJuiesiTgCp4xB1QBdkR4IFNHoIqyI4AN+L1sQ2o8yTAiq+DDqiCTAgArDeSpgFDwYUAbAQWk3zQDkcCsBGYATaAIQF+bKX3gQ1gSMBM5CEjH7TMlQAvss2RD1rkSkAlkujRp6pzJcCJLfVgGsCMAOyEbPAWcWYEYCfkgMshzAjATqgCfBBLAoKYUucBH8SEAPxSITwQ6zEiAFvBHnbCa5wIwFYQO+ElvgRMx2S6JjCCLAmYiql1AUXV4EgAHop1gRNmQgD2wjgAojQEPHY8Vsdy9cIhcMLFE2C/tbker68yk7IfdAtMBIsnwL61BQO13lL/BA3UC/VLQsBzW4T1eW7NgANagcIJcDYSf3Ipr2bABa1A4QQcpUTdU58Lo2awVw4CrlGiNvM6IPbATLhoAqwNWayUb4qhAKyVggB3QxKrFeVuCHXDy6UgwNuiZH2ifFUOdcNLpSBgmiT6IqeTgQXQDBZNwCMk0aeq7WAfjQNaBRKgMwDRojdAAWhwJAD3w03QDbMhAPfD6Gi0xpWAaOuDBkKCIwF4IBCAC0LMCMD3hTsgAMwIwBORLpiH8CEAH4+CAHAjAN8Yjw/AgCUBeCS0CgLAlYBOtOkJwUiQDwF4JgYCwJsAeQB2mBGAh4Jr8QHYZkCAWgB4E9CMDgWJOwE4AD0QAN4E+NGpKDgXYUCAQgBYExBlPTxkBCyAAPAlAAdg57ATsHPYCegfdgL6h52AQeQzsSYAB6B7eAkYggDwJkAegO1DRkAHBYA/AbsgAMx7gegRUJDHSGz2N7fW4/TBm7WSEIAD0M4hAE+tb8XrweZ/6mUnoKk+FXbvJyzpXtkmQhoC8Jb0TmvxBOAA+GonQ/IbffdKNhWOBmBBOQBHJYuqlfJkCAdgqPCd+mAPlOpsMBqAGeUAbMivdBZPAA5AVfWGiL0h+6RlvR8wApelx82BW5Sse8USgM3obj4BqMgC8N9SERBGAzClek9wShaAL8tEgLUv21Uoqrq+ABRPgL0vAA5F1eBIAA6ATVG1OBPg7rN8FkW1yJmAyn7PS1EtcSbA29/2hRTRMmcCpiOTj7ip6BpXAqLOfxu8SuwhZwIWIgGIm4n1OBPg7z8BaYKTEZ4EtPc/aR9MRXkS0Ins9biBQJ8zAav7s30VTER4EhDzfXIeRbTLmACbKMULBBgT4MRYXhcMBFgSUInp+mzQD7MkYDqyTtAO8iVgJm7yE4J2kCMBfmSrg26ILwHtuPl/G3RDHAnoxvkdHzQDHAkI4w5BF0AzwJAAO7JM4IUHbAlwY0GfAl6YIQFeZO6BvDBbAqqRYoesYJ0rAX6kF0JWsMWVgHbE8EIryJWAbnzL0wFWkB8BoOkFc2F+BNiRVA+t4DZTAipg8jcDnBA7Ajww+wVTQX4ELEQwx06oxpOAJkh0YCjGj4BOzFAcXBHgSUAI1ghmQuwIsCDlATACzAhwQZ4D56P8CPDAjWBwYZofATNwiR5FxZIAH0AObovyIyCAk18bHA4xI2AVz/5DUAdZEWAlXAbsgDrIigAXTL3A387xI8ADWQ7UQX4EzMTYAFwHa/wIaCaMPFwwGGZFQCdmGoAn48v8CAhjsjz+ZD12BDgEpgHgLQLsCJgCTg/0g0N2BMwkev0qKAOGE4C9DuADMMKCgC4AHLVDa9wIwCkOvFKMGQEuKHKwHRowI8ADfMMyMGJGgA8yHC4DDV4EBKAK4jKwzIuAEOCNy0CPFQEOGnfgMtBnRcA0KnG4GxixIsCXmhwfZEEmBHSlNncaZEEeBFiSYw/w7bN8CKjgGzB4KDRgRMA87gTwJYFRjQ8BQYoK3wSDURYEhCC7JZvhNTYEuJL6Bo6It9kQUAUOR5IFd9kQ0AYeV5IFqc6FgBDkQFkWXGZCgJtuWVVghcwnoApyoDQLDpkQ0AQ5UJoFqc6DgBDkQOlglJZYEOCiSY+8I+6xIKCKnqq8Ix6wICBIu68dkAQMJ8AKU2f2EDgBswmopDf4beAEzCZgIX2LNw+cgNkEdNI3+RWQBIwmwALD7pRWaCXXFyu/JwtAI30A7ma6GkM7ybRkmAm4G+lfrX1NFoB6tCZjfZHpRIDuSPMFJkb95erPyXLAGGB9kulEgBblh+gwZ6i/Xv+HlKyv0sd1Dxg6mbmpyf+6GCAD9UsJ1t+JuSd5rJ+BuILdksEH92W8QDes/hUb8se6tyJSb5f/ZfHB8u7Gz1AInY3UT1W8IXuqaWnZu5upCNKitGZkcMNvpN0BQngbqWNl3aYEbabcAR467EnvBLYVv2gpEqwtglr//qK8+wlh/TRbEeyn8o0gaFhPbD6gPdqK+W/zy1okzb53f2sr9mcfrL8kvq+n1x/E/6t7m/dESoXjpvQFynJn9MjVD2L1l8u1fYw98278z17ZD/Us+Gf//HLGYRC1UhhH0BGaqXls69IngV1hrjoon2EFwAwaKRu0dmMmgZ4wVdUMY36XwFTERAVZFhLy2QN2JpSbfPaABwp6sjw+eyAAlk7GDZc9YANTL1OHyx6Yznj/fZ6LFwpAEcxQCGmRwQ7oq3xB9v8Z7IA7Ga6WgwRqhjqZX5A2RWAuZJSc7LXcijGDO+Z1wgqVrElgNmqSugpexiNwPmCQKipu1iLz7bCvVMfaxlsBK1Ry8x6B8bgx8tS8rEWmp8FAsZtpG54GHdV+1jM8Dfrgsyt5IVoyKAUq09skk92gR6gPUOoHqGFqHzRQgMjEpriSR/72ydymuJlHBa9QjP5ozihIPXl1ja2EJ/MZZsyToXMRK8xn59oUo4E5kxD13N0mI3tCaxXbYHU7TH3jhsE0VNhLBiJgdfMrXQtkoB+ey9G8OBSnljEAqNvXwDwE5nJ9YB6BLGBQCRio/XOmITCfs3WbNwwBO8QpMD83SH1zugD6m3pjadBszM1/huMSGdQUBhpg7VCc3jahBGJW1Svhbq2UbbAWVLtEhhySnNEzvvgBxWlUL18GhKCqg2XCUanV1cXpPBnhhk5qG2LboQml0AHHGBpiW84ReaDxPN+m8ufBOa1pyicqb1eIt2lDw0lLWVuCtuY61aRy+8E5AgDoRmC7LBtA+2fzS70J2gAADTEuZSWYO4gMfbKcmwA/nJaG31LSyUBwMA9mntAmKOMGoIaWgXsZJ6ROCADQFmkwdy1IVufgsOxSCWvhmQN8JlNUvjTwBNEBXmYLCKhfK0MC0F+ZHEL6sEwJgIY1jVOnUrmBFylWyzrPnoFGLXHwevrg6/IcIe3WC0uA2ATrz4N44+mXGxZhSxyC+nsBBaCAIc1JKkcpsLsFpWOrS1C/O+BjoGKu8lYI61cHagCKusN3pvgIWEGRjZm1SlCjlwp4/hozIG6KsCEqcv20VEALChjQy3/BA0prtQAGQP0r5pymQoUx4HSpgA0A7BCSxgg4CfBtF9CIA/1aaNLjIRWwAaATx/q90KKnKEFLBU/j9XdG1ouUoA8LmcZgDeq5p/9O4u+rFXMrDWt0VuSq2dXE39YobCKLdVnkqGcpUW8XOJPH+qiRG/6vK6Qc7ZYYa5QTBD8CsBUwjgOuHGp4VsPjV3Df2nw51vW6hsePE0Bxk1msd1RCcOS3BAQcQAGJUCMF9iUCKvJ+AnanWO//JMvyXyC5hvVS/ZkG1kfPj7v810KSa9Qo2x0lrOErtTH2/muUSosCqMDWGGt0Pd1OsH52k6BwAShRMcT6WIqB9eTVkFLqT6Jo4TEN1o1zOG89dhqsXqEAFtgXYQ6unDsmIrJOnL76VwJS6ACKNkRY/7xx5eL58+dPnT7/6sWr7/6LkEp0MwmrEpKiTFo/HlfyXz/WrK4I4BEYgwgweP4gE2rWP8D6GUTAkPqHD+60Cx+7GOCKFSU/eDOoM5ILHz6XWtbrpFXDhii7nmWe/uV6nLTpsjBCs6ua8D8rDJGtJRG8L8GfeSIYvSyM0mw358dfF4bJeiHP3S8p/twheKcmjJT1TEgKAtcMjJJ9SX35ptQ+fLqroo+fF8bryZvcly+n4FK2yvdTwUZHwEkv1vDNuuAjfN6JD1EZyk4Xg93rDNDHJ7+/SAzC6MYrxwR3WScuXL0ZxpwUXjwFFs9Sjx4/cfrChYvf6tVzp44/KiaaaKKJJppoookmmmiiiSaa6MD0DQrj3ghNx49LAAAAAElFTkSuQmCC'
@Field static final String MASKABLE_ICON = 'iVBORw0KGgoAAAANSUhEUgAAAgAAAAIABAMAAAAGVsnJAAAAElBMVEVHcExxpQBxpQBwpQBxpgCAvACY5M8+AAAABXRSTlMAJ7Ti/ZKd2DUAAANgSURBVHja7d1BctowGIZhAxcgkANAwwEo4+wbW75AY93/Kl22ixJJaQnw8zx7zTCadz5WHnUAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAwB8WT+esuwew6dN5476L7iXN+bw5fe9i26b8sRS7gUXKJWPoIXiecsl87ALrc9nQxbWYctm87sJaVV3ALvAE5BpvXVgHF5AfewVfH/0Cplzj/dEvYFSAAhSgAAUoQAEKUIACFKAABShAAQpQgAIUoAAFKEABClCAAhSgAAUoQAEKUIACFKAABShAAQpQgAIUoAAFKEABClCAAhSgAAUoQAEKUIACFKAABShAAQpQgAIUoAAFKEABClCAAhSgAAUoQAEKUIACFKAABShAAQq42wI2L306ZzztwxewSanwME3sApbFh2l2oQtY9OUD68gFrGoepolcwCGXDdEKaH+YJm4By6oLOMYqINjDNO0FtF/AKVgB7Q/TxC3gUHdncQt4zTXGWAW0X8D86AXMj15AUoANUIANUIANUIANUIANUIANUIANUIANUIANUIANUIANUIANUIANUIANUIANUIANUIANUMAFFD5m+bYOvwHbNH0gpX3wAjblj1mibUDrxyxD6AK2Uy6Zd5E3oM9lb8EKaP+YJfAGrKouYBe3gOdcYT7G3YBDrvEWq4D2n3aKuwFTrjEEK6D9AsJuwGLKNX6GLWBxSwXMCghwAUE3YFDAl2yAAmyAAmyAAmyAAmyAAmyAAmyAAmyAAmyAAmyAAmyAAmyAAmyAAmyAAmyAAmyAAmyAAmyAAmyAAmyAAmyAAmyAAmyAAmyAAmyAAmyAAmyAAmyAAmyAAmyAAmyAAmyAAmyAAmyAAmyAAmyAAmyAAmyAAmyAAmyAAmyAAmyAAmyAAmyAAmyAAmyAAmyAAmyAAmzArReQ7uSdode6aNqPvIe6gKH9RbPxTt4aO1zqyHDrr819/shz5ZErqPtpx/YjP+7kAla5wrz7x0cql5VHbvV/MLUfmdefOXINffs6HS515E7eHe5W7UeWFUeO3c2+PN1+ZFx/5sh1bGvfHv9teaEjV7uBaT4f5pz2fz0yNR7ZFI9cz+alT+eMp6f/dOSpcAQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAACAu/cLBQBL1l75RF4AAAAASUVORK5CYII='
@Field static final String HUB_NAME = '🏠 Hubitat Hub'
@Field static final List<String> HUB_ATTRIBUTES = ['hubCPU', 'hubRAM', 'hubTemperature', 'hubDatabaseSize']

@Field static final Map SUPPORTED_ATTRIBUTES = [
    hubCPU: [min:0, max:50, unit:'%', probe:{ device, state, events, begin, end -> state.hubCPU ?: '0' }],
    hubRAM: [min:0, unit:'MB free', probe:{ device, state, events, begin, end -> state.hubRAM ?: '0' }],
    hubTemperature: [min:0, unit:'°', probe:{ device, state, events, begin, end -> state.hubTemperature ?: '0' }],
    hubDatabaseSize: [min:0, unit:'MB', probe:{ device, state, events, begin, end -> state.hubDatabaseSize ?: '0' }],

    acceleration: [min:0, max:100, unit:'% active', probe:{ device, state, events, begin, end -> calc5minValue(device, 'acceleration', ['active'], events, begin, end) }],
    airQualityIndex: [min:0, max:500, unit:'', probe:{ device, state, events, begin, end -> calc5minAverage(device, 'airQualityIndex', events) }],
    amperage: [min:0, unit:'A', probe:{ device, state, events, begin, end -> calc5minAverage(device, 'amperage', events) }],
    battery: [min:0, max:100, unit:'%', probe:{ device, state, events, begin, end -> "${device.currentValue('battery') ?: 0}" }],
    camera: [min:0, max:100, unit:'% on', probe:{ device, state, events, begin, end -> calc5minValue(device, 'camera', ['on'], events, begin, end) }],
    carbonDioxide: [min:0, unit:'ppm', probe:{ device, state, events, begin, end -> calc5minAverage(device, 'carbonDioxide', events) }],
    contact: [min:0, max:100, unit:'% open', probe:{ device, state, events, begin, end -> calc5minValue(device, 'contact', ['open'], events, begin, end) }],
    coolingSetpoint: [min:0, unit:'°', probe:{ device, state, events, begin, end -> calc5minAverage(device, 'coolingSetpoint', events) }],
    door: [min:0, max:100, unit:'% open', probe:{ device, state, events, begin, end -> calc5minValue(device, 'door', ['open'], events, begin, end) }],
    energy: [min:0, unit:'kWh', probe:{ device, state, events, begin, end -> calc5minIncrease(device, 'energy', state) }],
    filterStatus: [min:0, max:100, unit:'% normal', probe:{ device, state, events, begin, end -> "${device.currentValue('filterStatus')}" == 'normal' ? 100 : 0 }],
    frequency: [unit:'Hz', probe:{ device, state, events, begin, end -> calc5minAverage(device, 'frequency', events) }],
    goal: [min:0, unit:'steps', probe:{ device, state, events, begin, end -> calc5minAverage(device, 'goal', events) }],
    heatingSetpoint: [min:0, unit:'°', probe:{ device, state, events, begin, end -> calc5minAverage(device, 'heatingSetpoint', events) }],
    humidity: [min:0, max:100, unit:'%', probe:{ device, state, events, begin, end -> calc5minAverage(device, 'humidity', events) }],
    illuminance: [min:0, unit:'lx', probe:{ device, state, events, begin, end -> calc5minAverage(device, 'illuminance', events) }],
    lock: [unit:'% locked', probe:{ device, state, events, begin, end -> calc5minValue(device, 'lock', ['locked'], events, begin, end) }],
    lqi: [min:0, max:255, unit:'lqi', probe:{ device, state, events, begin, end -> calc5minAverage(device, 'lqi', events) }],
    motion: [min:0, max:100, unit:'% active', probe:{ device, state, events, begin, end -> calc5minValue(device, 'motion', ['active'], events, begin, end) }],
    naturalGas: [min:0, max:100, unit:'% detected', probe:{ device, state, events, begin, end -> calc5minValue(device, 'naturalGas', ['detected'], events, begin, end) }],
    networkStatus: [min:0, max:100, unit:'% online', probe:{ device, state, events, begin, end -> calc5minValue(device, 'networkStatus', ['online'], events, begin, end) }],
    pH: [unit:'pH', probe:{ device, state, events, begin, end -> calc5minAverage(device, 'ph', events) }],
    power: [min:0, unit:'W', probe:{ device, state, events, begin, end -> calc5minAverage(device, 'power', events) }],
    presence: [min:0, max:100, unit:'% present', probe:{ device, state, events, begin, end -> calc5minValue(device, 'presence', ['present'], events, begin, end) }],
    pressure: [min:0, unit:'psi', probe:{ device, state, events, begin, end -> calc5minAverage(device, 'pressure', events) }],
    rate: [min:0, unit:'LPM', probe:{ device, state, events, begin, end -> calc5minAverage(device, 'rate', events) }],
    rssi: [min:0, max:255, unit:'rssi', probe:{ device, state, events, begin, end -> calc5minAverage(device, 'rssi', events) }],
    securityKeypad: [min:0, max:100, unit:'% armed', probe:{ device, state, events, begin, end -> calc5minValue(device, 'securityKeypad', ['armed home', 'armed away'], events, begin, end) }],
    sessionStatus: [min:0, max:100, unit:'% running', probe:{ device, state, events, begin, end -> calc5minValue(device, 'sessionStatus', ['running'], events, begin, end) }],
    shock: [min:0, max:100, unit:'% detected', probe:{ device, state, events, begin, end -> calc5minValue(device, 'shock', ['detected'], events, begin, end) }],
    sleeping: [min:0, max:100, unit:'% sleeping', probe:{ device, state, events, begin, end -> calc5minValue(device, 'sleeping', ['sleeping'], events, begin, end) }],
    smoke: [min:0, max:100, unit:'% detected', probe:{ device, state, events, begin, end -> calc5minValue(device, 'smoke', ['detected'], events, begin, end) }],
    sound: [min:0, max:100, unit:'% detected', probe:{ device, state, events, begin, end -> calc5minValue(device, 'sound', ['detected'], events, begin, end) }],
    soundPressureLevel: [unit:'dB', probe:{ device, state, events, begin, end -> calc5minAverage(device, 'soundPressureLevel', events) }],
    steps: [unit:'steps', probe:{ device, state, events, begin, end -> calc5minAverage(device, 'steps', events) }],
    'switch': [min:0, max:100, unit:'% on', probe:{ device, state, events, begin, end -> calc5minValue(device, 'switch', ['on'], events, begin, end) }],
    tamper: [min:0, max:100, unit:'% detected', probe:{ device, state, events, begin, end -> calc5minValue(device, 'tamper', ['detected'], events, begin, end) }],
    temperature: [min:0, unit:'°', probe:{ device, state, events, begin, end -> calc5minAverage(device, 'temperature', events) }],
    transportStatus: [min:0, max:100, unit:'% playing', probe:{ device, state, events, begin, end -> calc5minValue(device, 'transportStatus', ['playing'], events, begin, end) }],
    valve: [min:0, max:100, unit:'% open', probe:{ device, state, events, begin, end -> calc5minValue(device, 'valve', ['open'], events, begin, end) }],
    water: [min:0, max:100, unit:'% wet', probe:{ device, state, events, begin, end -> calc5minValue(device, 'water', ['wet'], events, begin, end) }],
    windowBlind: [min:0, max:100, unit:'% open', probe:{ device, state, events, begin, end -> calc5minValue(device, 'windowBlind', ['opening', 'partially open', 'open'], events, begin, end) }],
    windowShade: [min:0, max:100, unit:'% open', probe:{ device, state, events, begin, end -> calc5minValue(device, 'windowShade', ['opening', 'partially open', 'open'], events, begin, end) }],
    voltage: [min:0, unit:'V', probe:{ device, state, events, begin, end -> calc5minAverage(device, 'voltage', events) }],

    // Non-standard attributes
    pm25: [unit:'μg/m3', probe:{ device, state, events, begin, end -> calc5minAverage(device, 'pm25', events) }],
    vocIndex: [unit:'', probe:{ device, state, events, begin, end -> calc5minAverage(device, 'vocIndex', events) }],
]

@CompileStatic
static String calc5minValue(DeviceWrapper device, String attribute, List<String> onValues, List<Event> events, long beginTimestamp, long endTimestamp) {
    String currentValue = device.currentValue(attribute)
    if (currentValue == null) return '0'
    long onTime = 0
    long lastOnTimestamp = beginTimestamp
    for (int i = 0; i < events.size(); i++) {
        Event event = events.get(i)
        if (event.name != attribute || !event.isStateChange) continue
        if (onValues.contains(event.value)) {
            lastOnTimestamp = event.getDate().getTime()
            continue
        } else {
            onTime += event.getDate().getTime() - lastOnTimestamp
        }
    }
    if (onValues.contains(currentValue)) onTime += endTimestamp - lastOnTimestamp
    return new BigDecimal(onTime / 3000.0d).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
}

@CompileStatic
static String calc5minIncrease(DeviceWrapper device, String attribute, Map state) {
    String currentValue = device.currentValue(attribute)
    if (currentValue == null) return '0'
    String stateKey = String.format('v.%s_%s', device.getId(), attribute)
    String lastValue = state.get(stateKey) ?: currentValue
    state.put(stateKey, currentValue)
    return (new BigDecimal(currentValue) - new BigDecimal(lastValue)).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
}

@CompileStatic
static String calc5minAverage(DeviceWrapper device, String attribute, List<Event> events) {
    String currentValue = device.currentValue(attribute)
    if (currentValue == null) return '0'
    BigDecimal sum = BigDecimal.ZERO
    int count = 0
    for (int i = 0; i < events.size(); i++) {
        Event event = events.get(i)
        if (event.name != attribute || !event.isStateChange) continue
        sum += new BigDecimal(event.value)
        count++
    }
    sum += new BigDecimal(currentValue)
    return (sum / (count + 1)).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
}

definition(
    name: APP_NAME,
    namespace: 'dandanache',
    author: 'Dan Danache',
    description: 'Data-Driven Insights for a Smarter Home',
    documentationLink: 'https://community.hubitat.com/t/release-watchtower-app/141505',
    importUrl: 'https://raw.githubusercontent.com/dan-danache/hubitat/main/watchtower-app/watchtower.groovy',
    category: 'Utility',
    singleInstance: true,
    installOnOpen: true,
    iconUrl: '',
    iconX2Url: '',
    oauth: true,
)

// ===================================================================================================================
// Standard app methods
// ===================================================================================================================

def installed() {
    log.info "${app.getLabel()} has been installed"
    unschedule()
    schedule '0 0/5 * ? * * *', 'collectDeviceMetrics'
    schedule '0 4/5 * ? * * *', 'collectHubMetrics'
}
def updated() {
    log.info "${app.getLabel()} has been updated"
}
def refresh() {
    log.info "${app.getLabel()} has been refreshed"
}
private void debug(message) {
    if (logEnable) log.debug "${APP_NAME} ▸ ${message}"
}
private void info(message) {
    log.info "${APP_NAME} ▸ ${message}"
}
private void warn(message) {
    log.warn "${APP_NAME} ▸ ${message}"
}

// ===================================================================================================================
// Button handler
// ===================================================================================================================

def appButtonHandler(String buttonName) {
    List<String> dashboardList = collectDashboards()

    if (buttonName == 'addDashboard') {
        app.removeSetting 'dashboardName'
        state.remove 'dashboardName'
        state.action = 'add'
        return
    }

    if (buttonName.startsWith('editDashboard_')) {
        state.dashboardName = buttonName.substring(14)
        app.updateSetting('dashboardName', state.dashboardName)
        state.action = 'edit'
        return
    }

    if (buttonName == 'saveDashboard') {
        String newDashboardName = "${app.getSetting('dashboardName')}".trim()
        if (!dashboardList.contains(newDashboardName)) {
            String stateEntryName = "g.${state.dashboardName}"
            state["g.${newDashboardName}"] = state[stateEntryName] ?: '{"panels":[]}'
            state.remove stateEntryName
        }
        app.removeSetting 'dashboardName'
        state.remove 'dashboardName'
        state.action = 'list'
        return
    }

    if (buttonName.startsWith('removeDashboard_')) {
        state.dashboardName = buttonName.substring(16)
        state.action = 'confirm'
        return
    }

    if (buttonName == 'removeDashboard') {
        String stateEntryName = "g.${state.dashboardName}"
        state.remove stateEntryName
        state.remove 'dashboardName'
        state.action = 'list'
        return
    }

    if (buttonName == 'addDevice') {

        // Find next empty position for insertion
        int nextPosition = 1
        while (app.getSetting("d.${nextPosition}") != null && app.getSetting("a.${nextPosition}") != null) nextPosition++
        state.position = nextPosition

        // Clear form
        app.removeSetting "d.${nextPosition}"
        app.removeSetting "a.${nextPosition}"

        // Update action
        state.action = 'add'
        return
    }

    if (buttonName.startsWith('viewDevice_')) {
        state.position = Integer.parseInt(buttonName.substring(11))
        state.highlightPosition = state.position
        state.action = 'view'
        return
    }

    if (buttonName == 'removeDevice') {

        // Remove CSV files
        deleteDataFiles(app.getSetting("d.${state.position}"))

        // Cleanup settings
        app.removeSetting "d.${state.position}"
        app.removeSetting "a.${state.position}"

        // Reset lastPosition; to be calculated on next list render
        state.remove 'lastPosition'

        // Update action
        state.remove 'position'
        state.action = 'list'
        return
    }

    if (buttonName == 'cancel') {

        // Clear form
        if (state.action == 'add') {
            app.removeSetting "d.${state.position}"
            app.removeSetting "a.${state.position}"
        }

        // Update action
        state.remove 'position'
        state.action = 'list'
        return
    }

    if (buttonName == 'saveDevice' || buttonName == 'close') {

        // Update lastPosition && highlightPosition
        state.lastPosition = Math.max(state.position, state.lastPosition)
        state.highlightPosition = state.position

        // Update action
        state.remove 'position'
        state.action = 'list'
    }
}

// ===================================================================================================================
// Implement Pages
// ===================================================================================================================

preferences {
    page name: 'main'
    page name: 'devices'
    page name: 'dashboards'
    page name: 'settings'
    page name: 'changelog'
}

Map main() {
    def showInstall = app.getInstallationState() == 'INCOMPLETE'
    dynamicPage(name:'main', install:true, uninstall:!showInstall) {

        if (app.getInstallationState() != 'COMPLETE') {
            section {
                paragraph 'Click the "Done" button to complete the app installation.'
            }
        } else {
            if (!state.accessToken) createAccessToken()

            // Cleanup devices page state
            state.remove 'position'
            state.action = 'list'

            section {
                href(name:'devicesLink', title:'Devices', description:'Select devices to monitor', page:'devices', required:false)
                href(name:'dashboardsLink', title:'Dashboards', description:'Manage dashboards', page:'dashboards', required:false)
                href(name:'settingsLink', title:'Settings', description:'Configure metrics storage limits', page:'settings', required:false)
                href(name:'changelogLink', title:'Change log', description:'See latest application changes', page:'changelog', required:false)

                // Preferences
                input(name:'logEnable', type:'bool', title:'Enable debug logging', defaultValue:false, submitOnChange:true)
                if (logEnable) {
                    paragraph '<b>Warning</b>: Enabling debug logging will significantly increase the number of log entries, potentially impacting hub\'s performance.'
                }
            }
        }
    }
}

Map devices() {
    dynamicPage(name:'devices', title:'Devices', install:false, uninstall:false) {

        // Highlight and clear last added/viewed position
        Integer highlightPosition = null
        if (state.action != 'add') {
            highlightPosition = state.highlightPosition
            state.remove 'highlightPosition'
        }

        // Render table
        List devices = collectDeviceConfiguration()
        String table = '<div style="overflow-x:auto; border: 1px rgba(0,0,0,.12) solid"><table id="app-table" class="mdl-data-table tstat-col"><tbody>'
        devices.each {
            String deviceName = it[1]?.displayName ?: HUB_NAME
            table += """
                <tr${highlightPosition == it[0] ? ' id="highlighted-row"' : ''}>
                    <td>${renderButton("viewDevice_${it[0]}", "${deviceName}<div class=\"text-600\">${it[2].join(', ')}</div>", 'View device configuration', 'view-btn')}</td>
                </tr>
            """
        }
        table += '</tbody></table></div>'

        section {
            paragraph """\
                ${renderCommonStyle()}${table}
                <script type="text/javascript">
                    if (document.getElementById('highlighted-row')) document.getElementById('highlighted-row').scrollIntoView({
                        behavior: 'smooth',
                        block: 'center'
                    })
                </script>
            """
        }

        // Render add button
        section {
            input(name:'addDevice', title:'➕ Add device configuration&nbsp;&nbsp;', type:'button')
        }

        // Render add/view page
        if (state.position != null) {
            DeviceWrapper device = app.getSetting("d.${state.position}")
            String deviceId = device?.id ?: '0'
            String deviceName = device?.displayName ?: HUB_NAME

            List<String> attributes = device ? app.getSetting("a.${state.position}") : HUB_ATTRIBUTES

            // Render add page
            if (state.action == 'add') {
                section {
                    input(
                        name: "d.${state.position}",
                        title: 'Select device',
                        type: 'capability.*',
                        multiple: false,
                        showFilter: true,
                        required: true,
                        submitOnChange: true
                    )

                    if (device != null) {
                        List<String> allAttr = device.supportedAttributes
                            .collect { it.name }
                            .findAll { SUPPORTED_ATTRIBUTES.containsKey(it) }
                            .unique()
                            .sort()

                        // Check for device with unsupported attributes
                        if (allAttr.size == 0) {
                            disableSaveButton = true
                            paragraph renderInfoBox("<b>${deviceName}</b> contains no supported attributes. Please select another device!")

                        // Check for duplicate device
                        } else if (devices.any { deviceId == it[1]?.id } ) {
                            disableSaveButton = true
                            paragraph renderInfoBox("<b>${deviceName}</b> is already configured. Please select another device!")
                        } else {

                            // Device changed; clear attributes
                            if (attributes != null && !allAttr.containsAll(attributes)) {
                                app.removeSetting "a.${state.position}"
                            }

                            input(
                                name: "a.${state.position}",
                                title: 'Select attributes',
                                type: 'enum',
                                options: allAttr,
                                multiple: true,
                                required: true,
                                submitOnChange: true
                            )
                        }
                    }
                }
            }

            // Render view page
            if (state.action == 'view') {
                section {
                    paragraph """
                        <b>${deviceName}</b>
                        <div class="text-600">${attributes.join(', ')}</div>
                        <hr>
                        Data files
                        <ul>
                            <li><a href="/local/wt_${deviceId}_5m.csv" target="_blank">wt_${deviceId}_5m.csv <i class="pi pi-external-link"></i></a></li>
                            <li><a href="/local/wt_${deviceId}_1h.csv" target="_blank">wt_${deviceId}_1h.csv <i class="pi pi-external-link"></i></a></li>
                            <li><a href="/local/wt_${deviceId}_1d.csv" target="_blank">wt_${deviceId}_1d.csv <i class="pi pi-external-link"></i></a></li>
                            <li><a href="/local/wt_${deviceId}_1w.csv" target="_blank">wt_${deviceId}_1w.csv <i class="pi pi-external-link"></i></a></li>
                        </ul>
                        ${deviceId == '0' ? '' : renderInfoBox('Removing this device configuration will also remove the data files')}
                    """
                }
            }

            section {
                boolean disableSaveButton = app.getSetting("d.${state.position}") == null || app.getSetting("a.${state.position}") == null
                paragraph """\
                    <div class="p-dialog-mask" style="display:${state.action == 'add' || state.action == 'view' ? 'flex' : 'none'}; position: fixed; height: 100%; width: 100%; left: 0px; top: 0px; justify-content: center; align-items: center; pointer-events: none; z-index: 3203;" data-pc-section="mask">
                        <div class="p-dialog p-component" style="min-width: 30vw; display: flex; flex-direction: column; pointer-events: auto;" role="dialog" data-pc-name="dialog" data-pc-section="root" data-pd-focustrap="true">
                            <div class="p-dialog-header" data-pc-section="header">
                                <span class="p-dialog-title" data-pc-section="title">${state.action == 'add' ? 'Add' : 'View'} device configuration</span>
                            </div>
                            <div id="dialog-body" class="p-dialog-content" data-pc-section="content"></div>
                            <div id="dialog-footer" class="p-dialog-footer" data-pc-section="footer">
                                ${ state.action != 'view' ? '' : """
                                    ${deviceId == '0' ? '' : renderButton('removeDevice', '🗑️&nbsp;&nbsp;Remove', 'Remove device configuration', 'mdl-button mdl-js-button mdl-button--accent mdl-button--raised', 'dialog-btn btn-remove')}
                                    ${renderButton('close', '✖&nbsp;&nbsp;Close', 'Close view', 'mdl-button mdl-js-button mdl-button--raised', 'dialog-btn btn-close')}
                                """}
                                ${ state.action != 'add' ? '' : """
                                    ${renderButton('cancel', '✖&nbsp;&nbsp;Cancel', 'Cancel add action', 'mdl-button mdl-js-button mdl-button--raised', 'dialog-btn')}
                                    ${renderButton(disableSaveButton, 'saveDevice', '✔&nbsp;&nbsp;Save', 'Save device configuration', 'mdl-button mdl-button--primary mdl-js-button mdl-button--raised', 'dialog-btn')}
                                """}
                            </div>
                        </div>
                    </div>
                    <script type="text/javascript">
                        document.getElementById('dialog-body').prepend(document.querySelectorAll('div[style="margin-bottom:15px;"]')[2])
                    </script>
                """
            }
        }
    }
}

Map dashboards() {
    List<String> dashboardList = collectDashboards().sort()
    dynamicPage(name:'dashboards', title:'Dashboards', install:false, uninstall:false) {
        String table = renderInfoBox('Click the button below to add your first dashboard')
        if (dashboardList.size != 0) {
            table = '<div style="overflow-x:auto; border: 1px rgba(0,0,0,.12) solid"><table id="app-table" class="mdl-data-table tstat-col"><tbody>'
            dashboardList.each {
                table += """
                    <tr>
                        <td><a href="${buildDashboardURL(it)}" target="_blank">${it} <i class="pi pi-external-link"></i></a></td>
                        <td class="tbl-icon">${renderButton("editDashboard_${it}", '✏️', 'Rename dashboard', 'view-btn')}</td>
                        <td class="tbl-icon">${renderButton("removeDashboard_${it}", '🗑️', 'Remove dashboard', 'view-btn')}</td>
                    </tr>
                """
            }
            table += '</tbody></table></div>'
        }

        section {
            paragraph "${renderCommonStyle()}${table}"
            // input(name:'useCloudLinks', type:'bool', title:'Use cloud links', defaultValue:false, submitOnChange:true)
            // if (useCloudLinks) {
            //     paragraph '<b>Warning</b>: Using cloud links will impose a <u>substantial load</u> on your hub resources and the Hubitat cloud services. This is due to metrics data files from the File Manager being proxied through the cloud infrastructure.'
            // }
        }

        // Render add button
        section {
            input(name:'addDashboard', title:'➕ Add dashboard&nbsp;&nbsp;&nbsp;', type:'button')
        }

        // Render add/view page
        if (state.action == 'add' || state.action == 'edit') {
            section {
                input(
                    name: 'dashboardName',
                    title: 'Dashboard name',
                    type: 'text',
                    required: true,
                    submitOnChange: true
                )
            }

            section {
                boolean disableSaveButton = app.getSetting('dashboardName') == null || app.getSetting('dashboardName').trim() == '' || dashboardList.contains(app.getSetting('dashboardName'))
                paragraph """\
                    <div class="p-dialog-mask" style="display:${state.action == 'add' || state.action == 'edit' ? 'flex' : 'none'}; position: fixed; height: 100%; width: 100%; left: 0px; top: 0px; justify-content: center; align-items: center; pointer-events: none; z-index: 3203;" data-pc-section="mask">
                        <div class="p-dialog p-component" style="display: flex; flex-direction: column; pointer-events: auto;" role="dialog" data-pc-name="dialog" data-pc-section="root" data-pd-focustrap="true">
                            <div class="p-dialog-header" data-pc-section="header">
                                <span class="p-dialog-title" data-pc-section="title">${state.action == 'add' ? 'Add' : 'Rename'} dashboard</span>
                            </div>
                            <div id="dialog-body" class="p-dialog-content" data-pc-section="content"></div>
                            <div id="dialog-footer" class="p-dialog-footer" data-pc-section="footer">
                                ${renderButton('cancel', '✖&nbsp;&nbsp;Cancel', 'Cancel action', 'mdl-button mdl-js-button mdl-button--raised', 'dialog-btn')}
                                ${renderButton(disableSaveButton, 'saveDashboard', '✔&nbsp;&nbsp;Save', 'Save dashboard', 'mdl-button mdl-button--primary mdl-js-button mdl-button--raised', 'dialog-btn')}
                            </div>
                        </div>
                    </div>
                    <script type="text/javascript">
                        document.getElementById('dialog-body').prepend(document.querySelectorAll('div[style="margin-bottom:15px;"]')[2])
                    </script>
                """
            }
        }

        // Render confirm delete page
        if (state.action == 'confirm') {
            section {
                paragraph """\
                    <div class="p-dialog-mask" style="display:flex; position: fixed; height: 100%; width: 100%; left: 0px; top: 0px; justify-content: center; align-items: center; pointer-events: none; z-index: 3203;" data-pc-section="mask">
                        <div class="p-dialog p-component" style="display: flex; flex-direction: column; pointer-events: auto;" role="dialog" data-pc-name="dialog" data-pc-section="root" data-pd-focustrap="true">
                            <div class="p-dialog-header" data-pc-section="header">
                                <span class="p-dialog-title" data-pc-section="title">Confirm</span>
                            </div>
                            <div id="dialog-body" class="p-dialog-content" data-pc-section="content">
                                Remove the <b>${state.dashboardName}</b> dashboard now?<br><br>
                            </div>
                            <div id="dialog-footer" class="p-dialog-footer" data-pc-section="footer">
                                ${renderButton('cancel', 'No', 'Cancel remove action', 'mdl-button mdl-js-button mdl-button--raised', 'dialog-btn')}
                                ${renderButton('removeDashboard', 'Yes', 'Remove dashboard', 'mdl-button mdl-js-button mdl-button--accent mdl-button--raised', 'dialog-btn')}
                            </div>
                        </div>
                    </div>
                """
            }
        }
    }
}

Map settings() {
    dynamicPage(name:'settings', title:'Settings', install:false, uninstall:false) {
        section('How data collection works', hideable:true, hidden:true) {
            paragraph '''\
                The application utilizes a fixed-size database, similar in design and purpose to an RRD (Round-Robin Database). This setup allows for high-resolution data (minutes per point) to gradually degrade into lower resolutions for long-term retention of historical data.
                <br><br>

                The following time resolution are used:
                <ul>
                    <li><b>5 minutes</b>: Attribute value in the last 5 minutes</li>
                    <li><b>1 hour</b>: Average attribute value over the last hour</li>
                    <li><b>1 day</b>: Average attribute value over the last day</li>
                    <li><b>1 week</b>: Average attribute value over the last week</li>
                </ul>
                <br>
                How it works:
                <ul>
                    <li><b>Every 5 minutes</b>: The application reads the current value for all configured device attributes and stores this data in the File Manager using CSV files named <code>wt_${device_id}_5m.csv</code>, one file per configured device. Only devices configured in the <b>Devices</b> screen are queried.</li>
                    <li><b>At the start of every hour</b>: The application reads the data from each device's <code>wt_${device_id}_5m.csv</code> file, selects records from the last hour, calculates the averages, and saves them in CSV files named <code>wt_${device_id}_1h.csv</code>.</li>
                    <li><b>At midnight daily</b>: The application reads the data from each device's <code>wt_${device_id}_5m.csv</code> file, selects records from the last day (00:00 - 23:59), calculates the averages, and saves them in CSV files named <code>wt_${device_id}_1d.csv</code>.</li>
                    <li><b>At midnight every Sunday</b>: The application reads the data from each device's <code>wt_${device_id}_1h.csv</code> file, selects records from the last week (Monday 00:00 - Sunday 23:59), calculates the averages, and saves them in CSV files named <code>wt_${device_id}_1w.csv</code>.</li>
                </ul>
                <br>
                To maintain a fixed file size, old records are discarded during each save, as specified below.
                <style>
                    .mdl-cell > div { white-space:normal !important }
                    ul { margin:0; padding-left:1em }
                    code { background:#e7e7e7; padding:.1rem .4rem; border-radius:.2rem }
                </style>
            '''
        }
        section {
            input(
                name: 'conf_5MinMaxLines',
                title: 'Max records with 5 min accuracy<br><span class="text-600" style="font-size:.85em">default 864 (3 days), min 288 (1 day)</span>',
                type: 'number',
                required: true,
                defaultValue: 864,
                range: '288..10000',
                width: 6,
            )
            input(
                name: 'conf_1HourMaxLines',
                title: 'Max records with 1 hour accuracy<br><span class="text-600" style="font-size:.85em">default 744 (1 month), min 168 (1 week)</span>',
                type: 'number',
                required: true,
                defaultValue: 744,
                range: '168..10000',
                width: 6,
            )
            input(
                name: 'conf_1DayMaxLines',
                title: 'Max records with 1 day accuracy<br><span class="text-600" style="font-size:.85em">default 732 (2 years), min 366 (1 year)</span>',
                type: 'number',
                required: true,
                defaultValue: 732,
                range: '366..10000',
                width: 6,
            )
            input(
                name: 'conf_1WeekMaxLines',
                title: 'Max records with 1 week accuracy<br><span class="text-600" style="font-size:.85em">default 522 (10 years), min 105 (2 years)</span>',
                type: 'number',
                required: true,
                defaultValue: 522,
                range: '105..2600',
                width: 6,
            )
        }
    }
}

Map changelog() {
    dynamicPage(name:'changelog', title:'Change log', install:false, uninstall:false) {
        section('v1.1.0 - 2024-08-08', hideable:true, hidden:false) {
            paragraph '''\
                <ul>
                    <li>Add option to set charts y-axis scale to "auto" or "fixed" - @marktheknife</li>
                </ul>
            '''
        }
        section('v1.0.0 - 2024-07-31', hideable:true, hidden:true) {
            paragraph '''\
                <ul>
                    <li>Initial release</li>
                </ul>
                <style>
                    .mdl-cell > div { white-space:normal !important }
                    ul { margin:0; padding-left:.5em }
                </style>
            '''
        }
    }
}

// ===================================================================================================================
// Helper functions
// ===================================================================================================================

String renderCommonStyle() {
    return '''
    <style>
        .mdl-cell > div { white-space:normal !important }
        .mdl-grid { padding: 0 !important }

        #app-table {
            background-color: inherit;
            border: 1px rgba(0,0,0,.12) solid;
            font-size: 15px;
            border-collapse: collapse;
            width: 100%;
        }
        #app-table tr:hover { background-color: transparent }
        #app-table th {
            font-weight: bold;
            border-bottom: 2px rgba(0,0,0,.12) solid;
        }
        #app-table td {
            text-align: left;
            padding: 0;
            white-space: nowrap;
            text-overflow: ellipsis;
        }
        #app-table td.tbl-icon { width: 1em }
        .view-btn {
            border: 0;
            padding: .5em;
            margin: 0;
            background-color: transparent;
            color: #1a77c9;
            cursor: pointer;
            font-size: 1rem;
            text-align: left;
            width: 100%;
        }
        .view-btn:hover { background-color: var(--gray-300) }
        .view-btn > div { margin-top: .2em; cursor: default }
        #highlighted-row td { animation: background-fade 10s forwards }
        #app-table a { margin-left:.5em }
        @keyframes background-fade { 0% { background-color: #FFDAA3 }}

        #dialog-body { padding-bottom: 0 !important }
        #dialog-body > div { margin-bottom: 0 !important }
        #dialog-footer { display: block; text-align: right }
        #dialog-footer > div.form-group { display: none }
        #dialog-footer > div.dialog-btn { display: inline-block; margin-left: 1em }
        #dialog-footer > div.btn-remove { margin: 0; float: left }
        #dialog-footer button { position: static }
        #dialog-body hr { margin: .5em 0 }
        #dialog-body i { font-size: .85em }

        .form-warning {
            padding: .8em 1em;
            color: #856404;
            background-color: #fff3cd;
            border: 1px #ffeeba solid;
            border-radius: .3em;
            margin-left: -8px;
            margin-right: -8px;
            font-size: .85em;
        }
    </style>
    '''
}

String renderButton(String name, String label, String tooltip=null, String buttonClass=null, String containerClass=null) {
    return renderButton(false, name, label, tooltip, buttonClass, containerClass)
}

String renderButton(Boolean disabled, String name, String label, String tooltip=null, String buttonClass=null, String containerClass=null) {
    return """
    <div class="form-group">
        <input type="hidden" name="${name}.type" value="button">
        <input type="hidden" name="${name}.multiple" value="false">
    </div>
    <div${containerClass != null ? " class=\"${containerClass}\"": ''}>
        <button type="button" id="settings[${name}]" class="submitOnChange${buttonClass != null ? " ${buttonClass}": ''}" value="button"${tooltip != null ? "title=\"${tooltip}\"" : ''}
        ${ disabled ? 'disabled="true"' : '' }
        >${label}</button>
        <input type="hidden" name="settings[${name}]" value="">
    </div>
    """
}

String renderInfoBox(String message) {
    return "<div class=\"form-warning\">${message}</div>"
}

def collectDeviceConfiguration() {
    List retVal = []
    Integer lastPosition = state.lastPosition ?: 500
    for (int position = 1; position <= lastPosition; position++) {

        // Skip current entry that is in the add form right now
        if (state.action == 'add' && state.position == position) continue

        // Skip uncomplete/broken entries
        if (app.getSetting("d.${position}") == null || app.getSetting("a.${position}") == null) continue
        retVal.add([position, app.getSetting("d.${position}"), app.getSetting("a.${position}")])
    }
    state.lastPosition = retVal.size == 0 ? 0 : retVal.last()[0]

    List hubEntry = [0, null, HUB_ATTRIBUTES]
    return retVal.sort { it[1].label }.plus(0, [hubEntry])
}

List<String> collectDashboards() {
    List<String> retVal = []
    state.each { key, val -> if (key.startsWith('g.')) retVal.add(key.substring(2)) }
    return retVal
}

def buildURL(String fileName) {
    String prefix = useCloudLinks == true ? "${getApiServerUrl()}/${hubUID}/apps/${app.id}" : "${(getFullLocalApiServerUrl() =~ URL_PATTERN).findAll()[0][1]}"
    return "${prefix}/${fileName}?access_token=${state.accessToken}"
}

def buildDashboardURL(String dashboardName) {
    String prefix = useCloudLinks == true ? "${getApiServerUrl()}/${hubUID}/apps/${app.id}" : "${(getFullLocalApiServerUrl() =~ URL_PATTERN).findAll()[0][1]}"
    return "${prefix}/watchtower.html?name=${java.net.URLEncoder.encode(dashboardName, 'UTF-8')}&access_token=${state.accessToken}"
}


// ===================================================================================================================
// Metrics handlers
// ===================================================================================================================

void collectHubMetrics() {
    debug 'Start saving hub metrics to state (to be collected in 1 minute) ...'
    state.remove 'hubTemperature'
    state.remove 'hubDatabaseSize'
    state.remove 'hubRAM'
    state.remove 'hubCPU'
    fetchHubUrl('/hub/advanced/internalTempCelsius', { state.hubTemperature = convertTemperatureIfNeeded(new BigDecimal(it), 'C', 1) })
    fetchHubUrl('/hub/advanced/databaseSize', { state.hubDatabaseSize = new BigDecimal(it).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString() })
    fetchHubUrl('/hub/advanced/freeOSMemoryLast', {
        String[] bits = it.split(',')
        state.hubRAM= (new BigDecimal(bits[3]) / 1024).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
        state.hubCPU = (new BigDecimal(bits[4]) * 25).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
    })
}

void fetchHubUrl(String path, Closure func) {
    httpGet("http://127.0.0.1:8080${path}", {
        if (it.status != 200) {
            warn "Failed to fetch hub resource: ${path}"
            return
        }
        func("${it.data}")
    })
}

void collectDeviceMetrics() {
    ZonedDateTime now = ZonedDateTime.now(ZoneId.of(location.timeZone.ID)).withSecond(0).withNano(0)
    debug "Collecting metrics: now=${now}, epoch=${now.toEpochSecond()}, dow=${now.getDayOfWeek()}, hour=${now.getHour()}, min=${now.getMinute()}"
    collectDeviceConfiguration().each { conf ->
        DeviceWrapper device = conf[1]
        List<String> attrs = conf[2]
        String deviceId = device?.id ?: '0'

        update5MinData(now, device, attrs)

        if (now.getMinute() != 0) return
        update1HourData(now, deviceId, attrs)

        if (now.getHour() != 0) return
        update1DayData(now, deviceId, attrs)

        if (now.getDayOfWeek() != DayOfWeek.MONDAY) return
        update1WeekData(now, deviceId, attrs)
    }
}

void update5MinData(ZonedDateTime now, DeviceWrapper device, List<String> attrs) {
    String deviceId = device?.id ?: '0'
    Date beginDate = Date.from(now.minusMinutes(5).toInstant())
    List<Event> events = device?.eventsSince(beginDate) ?: []
    debug "Updating 5 min metrics for device #${deviceId}, events=${events} ..."

    // Compute and save a new CSV record
    List<String> newCsvRecord = ["${now.toEpochSecond()}"]
    attrs.each { newCsvRecord.add(SUPPORTED_ATTRIBUTES[it].probe(device, state, events, beginDate.getTime(), now.toInstant().toEpochMilli())) }
    appendDataRecord("wt_${deviceId}_5m.csv", newCsvRecord, attrs, conf_5minMaxLines ?: 864)
}

void update1HourData(ZonedDateTime now, String deviceId, List<String> attrs) {
    debug "Updating 1 hour metrics device #${deviceId} ..."

    // Compute averages from lower interval file
    String lowerFileName = "wt_${deviceId}_5m.csv"
    Long onlyAfter = now.minusHours(1).toEpochSecond()
    try {
        String lowerFileContents = new String(downloadHubFile(lowerFileName), 'UTF-8')
        BigDecimal[] averages = computeAverages(lowerFileContents, attrs, onlyAfter)
        if (averages == null) {
            warn 'Could not compute 1 hour averages'
            return
        }

        // Create and save a new CSV record
        List<String> newCsvRecord = ["${now.toEpochSecond()}"]
        for (int i = 0; i < averages.length; i++) newCsvRecord.add("${averages[i].setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()}")
        appendDataRecord("wt_${deviceId}_1h.csv", newCsvRecord, attrs, conf_1HourMaxLines ?: 744)
    } catch (NoSuchFileException ex) {
        warn "update1HourData: File not found: ${lowerFileName}"
    }
}

void update1DayData(ZonedDateTime now, String deviceId, List<String> attrs) {
    debug "Updating 1 day metrics device #${deviceId} ..."

    // Compute averages from lower interval file
    String lowerFileName = "wt_${deviceId}_5m.csv"
    Long onlyAfter = now.minusDays(1).toEpochSecond()
    try {
        String lowerFileContents = new String(downloadHubFile(lowerFileName), 'UTF-8')
        BigDecimal[] averages = computeAverages(lowerFileContents, attrs, onlyAfter)
        if (averages == null) {
            warn 'Could not compute 1 day averages'
            return
        }

        // Create and save a new CSV record
        List<String> newCsvRecord = ["${now.toEpochSecond()}"]
        for (int i = 0; i < averages.length; i++) newCsvRecord.add("${averages[i].setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()}")
        appendDataRecord("wt_${deviceId}_1d.csv", newCsvRecord, attrs, conf_1DayMaxLines ?: 732)
    } catch (NoSuchFileException ex) {
        warn "update1DayData: File not found: ${lowerFileName}"
    }
}

void update1WeekData(ZonedDateTime now, String deviceId, List<String> attrs) {
    debug "Updating 1 week metrics device #${deviceId} ..."

    // Compute averages from lower interval file
    String lowerFileName = "wt_${deviceId}_1h.csv"
    Long onlyAfter = now.minusWeeks(1).toEpochSecond()
    try {
        String lowerFileContents = new String(downloadHubFile(lowerFileName), 'UTF-8')
        BigDecimal[] averages = computeAverages(lowerFileContents, attrs, onlyAfter)
        if (averages == null) {
            warn 'Could not compute 1 week averages'
            return
        }

        // Create and save a new CSV record
        List<String> newCsvRecord = ["${now.toEpochSecond()}"]
        for (int i = 0; i < averages.length; i++) newCsvRecord.add("${averages[i].setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()}")
        appendDataRecord("wt_${deviceId}_1w.csv", newCsvRecord, attrs, conf_1WeekMaxLines ?: 522)
    } catch (NoSuchFileException ex) {
        warn "update1DayData: File not found: ${lowerFileName}"
    }
}

// ===================================================================================================================
// Datastore helpers
// ===================================================================================================================

void appendDataRecord(String fileName, List<String> csvRecord, List<String> attrs, Long maxLines) {
    debug "appendDataRecord(fileName=${fileName}, csvRecord=${csvRecord}, attrs=${attrs}, maxLines=${maxLines})"
    List<String> csvLines = []
    csvLines.add("timestamp,${ attrs.join(',') }")
    csvLines.addAll(loadDataLines(fileName, maxLines - 1))
    csvLines.add("${csvRecord.join(',')}")
    uploadHubFile(fileName, csvLines.join("\n").getBytes())
}

List<String> loadDataLines(String fileName, Long maxLines) {
    try {
        return new String(downloadHubFile(fileName), 'UTF-8').trim().split("\n").drop(1).takeRight((int) maxLines)
    } catch (NoSuchFileException ex) {
        warn "Creating data file: ${fileName}"
        return []
    }
}

@CompileStatic
BigDecimal[] computeAverages(String fileContents, List<String> attrs, Long onlyAfter) {
    if (fileContents == null) return null

    String[] fileLines = fileContents.trim().split('\n')
    if (fileLines.length < 2) return null

    // Sanity check; do we need this?
    int attrsCount = attrs.size()
    if (fileLines[0].split(',').length != attrsCount + 1) {
        warn "Sanity check failed: ${fileLines[0].split(',').length} != ${attrsCount + 1}"
        return null
    }

    // Init result
    BigDecimal[] retVal = new BigDecimal[attrsCount]
    for (int i = 0; i < attrsCount; i++) retVal[i] = BigDecimal.ZERO

    // Parse only records of interest (starting from the end of the file, and skipping first line) and calculate their sums
    int onlyAfterRecordsCount = 0
    for (int i = fileLines.length - 1; i > 0; i--) {
        String[] rowElements = fileLines[i].split(',')
        if (Long.parseLong(rowElements[0]) <= onlyAfter) break
        for (int j = 0; j < attrsCount; j++) retVal[j] += new BigDecimal(rowElements[j + 1])
        onlyAfterRecordsCount++
    }
    debug "Found ${onlyAfterRecordsCount} records of interest"
    if (onlyAfterRecordsCount == 0) return null

    // Divide by onlyAfterRecordsCount to get the averages; exception is the 'energy' attribute, that si a counter not a gauge
    for (int i = 0; i < attrsCount; i++) {
        if (attrs.get(i) == 'energy') continue
        retVal[i] = retVal[i] / onlyAfterRecordsCount
    }
    return retVal
}

void deleteDataFiles(DeviceWrapper device) {
    if (device == null) return
    warn "Deleting data files for ${device} (${device.id})"
    try { deleteHubFile("wt_${device.id}_5m.csv") } catch (NoSuchFileException ex) { }
    try { deleteHubFile("wt_${device.id}_1h.csv") } catch (NoSuchFileException ex) { }
    try { deleteHubFile("wt_${device.id}_1d.csv") } catch (NoSuchFileException ex) { }
    try { deleteHubFile("wt_${device.id}_1w.csv") } catch (NoSuchFileException ex) { }

    // Also cleanup app state entries used for counter attributes (e.g. energy)
    // @see calc5minIncrease()
    state.entrySet().removeIf { it.key.startsWith("v.${device.id}_") }
}

// ===================================================================================================================
// Implement Mappings
// ===================================================================================================================

mappings {
    path('/watchtower.html') { action:[GET:'getDashboardHtmlMapping']}
    path('/watchtower.js') { action:[GET:'getDashboardJsMapping']}
    path('/watchtower.csv') { action:[GET:'getDashboardCsvMapping']}
    path('/icon.png') { action:[GET:'getIconMapping']}
    path('/app.webmanifest') { action:[GET:'getAppManifestMapping']}
    path('/grid-layout.json') { action:[GET:'getGridLayoutMapping', PUT:'setGridLayoutMapping']}
    path('/monitored-devices.json') { action:[GET:'getMonitoredDevicesMapping']}
    path('/supported-attributes.json') { action:[GET:'getSupportedAttributesMapping']}
    path("/hub-info.json") { action:[GET:'getHubInfoMapping']}
}

def getDashboardHtmlMapping() {
    debug "Proxying watchtower.html to ${request.HOST} (${request.requestSource})"
    if (params.name == null) throw new RuntimeException('Missing "name" query param')
    return render(status:200, contentType:'text/html',
        data: new String(downloadHubFile('watchtower.html'), 'UTF-8')
            .replaceAll('\\$\\{access_token\\}', "${state.accessToken}")
            .replaceAll('\\$\\{dashboard_name\\}', "${params.name}")
    )
}

def getDashboardJsMapping() {
    debug "Proxying watchtower.js to ${request.HOST} (${request.requestSource})"
    // if (request.requestSource == 'cloud') return render(status:301,
    //     headers: [location:'https://dan-danache.github.io/hubitat/watchtower-app/watchtower.js']
    // )
    return render(status:200, contentType:'text/javascript', data:new String(downloadHubFile('watchtower.js'), 'UTF-8'))
}

def getDashboardCsvMapping() {
    debug "Proxying CSV data file: device=${params.device}, precision=${params.precision}"
    if (params.device == null) throw new RuntimeException('Missing "device" query param')
    if (params.precision == null) throw new RuntimeException('Missing "precision" query param')
    return render(status:200, contentType:'text/csv',
        data: new String(downloadHubFile("wt_${params.device}_${params.precision}.csv"), 'UTF-8')
    )
}

def getIconMapping() {
    return render(status:200, contentType:'image/png', data:APP_ICON.decodeBase64())
}

def getAppManifestMapping() {
    debug "Returning PWA manifest for dashboard: ${params.name}"
    if (params.name == null) throw new RuntimeException('Missing "name" query param')
    return render(status:200, contentType:'application/manifest+json',
        data: """\
        {
            "id": "${java.util.UUID.nameUUIDFromBytes(params.name.getBytes())}",
            "name": "${params.name}",
            "short_name": "${params.name}",
            "description": "View metrics for your smart devices.",
            "start_url": "${buildDashboardURL(params.name)}",
            "icons": [{
                "src": "data:image/png;base64,${MASKABLE_ICON}",
                "sizes": "512x512",
                "type": "image/png",
                "purpose": "maskable"
            },{
                "src": "data:image/png;base64,${APP_ICON}",
                "sizes": "512x512",
                "type": "image/png"
            }],
            "categories": ["utilities"],
            "display": "standalone",
            "orientation": "any",
            "theme_color": "${params.theme == 'dark' ? "#002b36" : "#eee8d5"}",
            "background_color": "${params.theme == 'dark' ? "#002b36" : "#eee8d5"}"
        }
        """
    )
}

def getGridLayoutMapping() {
    debug "Returning grid layout for dashboard: ${params.name}"
    if (params.name == null) throw new RuntimeException('Missing "name" query param')

    String stateEntryName = "g.${params.name}"
    String layout = state[stateEntryName]
    if (layout == null) return render(status:200, contentType:'application/json', data:'{"status": false}')
    return render(status:200, contentType:'application/json', data:layout)
}

def setGridLayoutMapping() {
    debug "Saving grid layout for dashboard: ${params.name}"
    if (params.name == null) throw new RuntimeException('Missing "name" query param')

    String stateEntryName = "g.${params.name}"
    if (!state[stateEntryName]) return render(status:200, contentType:'application/json', data:'{"status": false}')

    runIn(1, 'saveGridLayout', [data: [stateEntryName:stateEntryName, json:"${request.body}"]])
    return render(status:200, contentType:'application/json', data:'{"status": true}')
}

def saveGridLayout(data) {
    state[data.stateEntryName] = data.json
}

def getMonitoredDevicesMapping() {
    debug 'Returning monitored devices list'
    List devices = collectDeviceConfiguration().collect { return [id:it[1]?.id ?: '0', name:it[1]?.displayName ?: HUB_NAME, attrs:it[2]] }
    return render(status:200, contentType:'application/json', data:new JsonBuilder(devices).toString())
}

def getSupportedAttributesMapping() {
    debug 'Returning supported attributes list'
    Map attributes = SUPPORTED_ATTRIBUTES.collectEntries{ key, val -> [key, val.findAll { k, v -> k != 'probe' }] }
    attributes.coolingSetpoint.unit = "°${location.temperatureScale}"
    attributes.heatingSetpoint.unit = "°${location.temperatureScale}"
    attributes.temperature.unit = "°${location.temperatureScale}"
    attributes.hubTemperature.unit = "°${location.temperatureScale}"
    return render(status:200, contentType:'application/json', data:new JsonBuilder(attributes).toString())
}

def getHubInfoMapping() {
    debug 'Returning Hub information'
    return render(status:200, contentType:'application/json', data: """\
        {
            "name": "${location.hub.name}",
            "ip": "${location.hub.localIP}",
            "uptime": ${location.hub.uptime.toLong()},
            "model": "${getHubVersion()}",
            "fw": "${location.hub.firmwareVersionString}"
        }
        """
    )
}
