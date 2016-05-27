# am-i-in-the-geofencing
There are many apps which shows how to create and monitor geofencing, but i couldn't find any resource which tells the app whether user dwells in a geofence. As soon as geofence is created app get to know user is in the fence, but what about after 30mins or 1 hour? 


<h2> What Have I Done Here? </h2>
I am populating the geofence in the main activity when clicked on Add Geofence. This is fork from google sample project. I have edited on top of that. I am using <b> .setLoiteringDelay(1000 * 60 * 5) </b>, which delays notifying for 5mins. It notifies only if user dwells in the geofence for 5mins else it won't. Once i get the notification, i am removing that particular geofence and adding the same geofence with setLoiteringDelay(5mins). By this way i can check if user dwells in the same geofence or not. This is not the right way i feel but i cldn't find any other better solution as of now. 
