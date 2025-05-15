package fhw;
import io.quarkiverse.ironjacamar.ResourceEndpoint;
import io.quarkus.logging.Log;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;

@ApplicationScoped
//@RequestScoped
@ResourceEndpoint(activationSpecConfigKey = "myqueue")
//
// This ^^^ Annotation is a unique helper for Quarkus.
// The 'normal' EE style activation spec annotations
// may just work here, But remains untested.
//
// Note:  remainder of the code is 'vanilla' JMS code.
//
public class EventListener
   implements MessageListener
{
   public EventListener() {}

   @PostConstruct
   void postConstructor()
   {
      Log.debug("EventListener:postConstructor");
   }

   @Override
   public void onMessage(Message message)
   {
      try
      {
         var mId = message.getJMSMessageID();
         Log.debugf("Received message: [%s] with messageId [%s]",message.getBody(String.class), mId);
         Thread.sleep(10*1000);
         Log.debugf("Simulated processing complete for message id %s", mId);
      }
      catch (JMSException | InterruptedException e)
      {
         e.printStackTrace();
      }
   }
}
