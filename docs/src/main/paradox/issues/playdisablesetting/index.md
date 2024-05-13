# Play disable setting

## Issue

For Rachel want to call a api of AWM from AWM config application in frontend, for AWM and AWM configuration are two different applications.
There are some CORS setting need to be modified.

For we want to open stag enviornment for CORS, so the setting for stag.conf as below

SecuritySetting
: @@snip[Setcurity setting](code/securitysetting.conf)

Stag config
: @@snip[Stage conf](code/stag.conf)


But the CORS setting can't be enabled.

For the structure of stag config, we enable the CORS filter at the end.

The reason is in the Security setting config, we disable the CORS filter, in play setting, if one feature is disabled in setting, you can't enabled again.

Even you set as below, you can't enable CORS filter again.
```
play.filters.enabled += "play.filters.cors.CORSFilter"
play.filters.disabled += "play.filters.cors.CORSFilter"
play.filters.enabled += "play.filters.cors.CORSFilter"
```