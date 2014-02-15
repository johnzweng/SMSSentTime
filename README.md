SMSSentTime
===========


Android application that appends the time when a SMS was *sent* (not received) to the message. 
https://play.google.com/store/apps/details?id=at.zweng.smssenttimefix


#### Donations:
This app was built (and intensely supported) completly in my free time. If you want to support my work you can do this by [donating](http://johannes.zweng.at/donations_en.html) (but of course you don't need to).


### Short disclaimer: ###
This may not be perfect, or the best way this could be done. This was not to be intended a professional project, but just a small proof-of-concept solution for myself - made in my free time. I also did not follow the latest API releases from Google, so maybe in newer Android versions there may be other or better ways to do this (or it even may be unnecessary).


### Function principle ###
This application registers a broadcast listener for `android.provider.Telephony.SMS_RECEIVED` Intents in the `AndroidManifest.xml` so that it gets notified each time a SMS arrives. The resulting intent which is broadcated by Android when receiving a new SMS also contains the raw PDU (protocol data unit) which in fact contains a timestamp field, filled by the sender.

So this app just parses this raw PDU into an instance of a `SmsMessage` object and reads the sending timestamp. Then it waits until the messages gets inserted into the system SMS database and looks it up (by simply matching the body text and the sender) and appends the sent time information to the body text. This may be not the most elegant way to do this, as there are hardcoded waiting times and so on.. But you get the principle.

### LICENSE ###
Licensed under GPL v3 (see LICENSE file).


