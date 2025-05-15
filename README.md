# Example of using a RA for MessageListener/MDB like features in Quarkus. 
---
Simple experiment of using JMS/JCA in Quarkus to simulate MessageListner bean (almost a 'MDB').

Why would one do this when Quarkus has solid MOM patterns (e.g. reactive messaging)?

It is pretty easy (well, it can be) to refactor older MDBs to reactive messaging patterns.   However, sometimes 'other forces' in an Enterprise may not feel it is valuable to replatform mature MOM processors onto newer & modern platforms like Quarkus.  Especially when it is mentioned that refactoring would be required.   So basically, this would be a way to maybe minimize a bit of refactoring of older MessageListiners/MDBs so as to allow replatforming to Quarkus.

## Set up step
You do you, but as a suggestion make an .env file in project root directory with (at least the following):

    QUARKUS_IRONJACAMAR_RA_CONFIG_CONNECTION-PARAMETERS=tcp://localhost:61616
    QUARKUS_IRONJACAMAR_RA_CONFIG_USER=someUser
    QUARKUS_IRONJACAMAR_RA_CONFIG_PASSWORD=???



## Building & Running the app

    ./gradlew quarkusDev

Or use any of the other methods to run a quarkus app.

Toss a message at the queue and see what the app does.   It is underwhelming, this POC is more about the code.



