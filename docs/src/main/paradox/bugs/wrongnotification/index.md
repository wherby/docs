# Wrong notification

## Issue
 
The client complains that the notification message is send to some internal user but the user login to 
system they can't find pending task

```
Subject: FW: PFIC system notification

Hi Steven,

Please find below feedback from our team member.  Could you please check if there is any update to email notification since we update the setting to approver of mapping rules?

Best regards,
Penny
```


How this bug introduces?

Internal user (reviewer or approver) could see all notification from all engagements at first.
And notification email will send to all internal users.

@@snip[Send notification to role](code/checkByRole.scala)

For the internal user notification, the client want to only engagement user could see the notification.
The UI part of the filter is enabled:

@@snip[Check Notification by user](code/checkNotificationByUser.scala)

While this change applied, only specified user could see the notification, and other user will still get notification by 
email because email send is using role base.

How to fix:

@@snip[Fix by filter](code/fixByUser.scala)



