package fhw;
import io.quarkiverse.ironjacamar.ResourceAdapterFactory;
import io.quarkiverse.ironjacamar.ResourceAdapterKind;
import io.quarkiverse.ironjacamar.ResourceAdapterTypes;
import io.quarkiverse.ironjacamar.runtime.endpoint.MessageEndpointWrapper;
import io.quarkus.logging.Log;
import io.vertx.core.Vertx;
import io.vertx.core.WorkerExecutor;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import jakarta.jms.XAConnectionFactory;
import jakarta.resource.ResourceException;
import jakarta.resource.spi.ActivationSpec;
import jakarta.resource.spi.ManagedConnectionFactory;
import jakarta.resource.spi.ResourceAdapter;
import jakarta.resource.spi.endpoint.MessageEndpoint;
import java.util.Map;
import java.util.concurrent.Callable;
import org.apache.activemq.ra.ActiveMQActivationSpec;
import org.apache.activemq.ra.ActiveMQManagedConnectionFactory;
import org.apache.activemq.ra.ActiveMQResourceAdapter;

@ResourceAdapterKind("ActiveMQClassic")
@ResourceAdapterTypes(connectionFactoryTypes =
{
   ConnectionFactory.class,
   XAConnectionFactory.class
})
public class AMQResourceFactory
   implements ResourceAdapterFactory
{
   private WorkerExecutor executor;

   public AMQResourceFactory()
   {
      Log.debug("Construction");
      Vertx vertx = Vertx.vertx();
      if(null != vertx)
      {
         //
         // Basically, this is number of threads that can
         // conscurrently run 'onMessage()' methods.
         // In longer view, this should be tunnable via a property
         //
         int numWorkers = 10;
         executor =  vertx.createSharedWorkerExecutor("MyMessageListener", numWorkers);
         Log.infof("Message worker pool created with %d workers", numWorkers );
      }
   }

   @Override
   public String getProductName()
   {
      return "ActiveMQClassic";
   }

   @Override
   public String getProductVersion()
   {
      return "6.x";
   }

   @Override
   public ResourceAdapter createResourceAdapter(String id, Map<String, String> config)
           throws ResourceException
   {
      var adapter = new ActiveMQResourceAdapter();
      String brokerUrl = config.get("connection-parameters");
      adapter.setServerUrl(brokerUrl);
      adapter.setUserName(config.get("user"));
      adapter.setPassword(config.get("password"));
      adapter.setClientid(config.get("client-id"));
      return (adapter);
   }

   @Override
   public ManagedConnectionFactory createManagedConnectionFactory(String id, ResourceAdapter adapter)
           throws ResourceException
   {
      var factory = new ActiveMQManagedConnectionFactory();
      factory.setResourceAdapter(adapter);
      return (factory);
   }

   @Override
   public ActivationSpec createActivationSpec(String id, ResourceAdapter adapter, Class<?> type, Map<String, String> config)
      throws ResourceException
   {
      var spec = new ActiveMQActivationSpec();
      spec.setResourceAdapter(adapter);
      spec.setDestinationType(config.get("destination-type"));
      spec.setDestination(config.get("destination"));
      spec.setMaxSessions(config.getOrDefault("max-session", "50"));
      spec.setMessageSelector(config.get("message-selector"));
      spec.setSubscriptionDurability(config.get("durability"));
      spec.setSubscriptionName(config.get("subscription-name"));
      spec.setUseJndi(false);
      return (spec);
   }

   @Override
   public MessageEndpoint wrap(MessageEndpoint endpoint, Object resourceEndpoint)
   {
      Log.debugf("wrap invoked");
      return new BackgroundJMSMessageEndpoint(endpoint, (MessageListener) resourceEndpoint, executor);
   }

   private static class BackgroundJMSMessageEndpoint
           extends MessageEndpointWrapper
           implements MessageListener
   {
      private final MessageListener ml;
      private final WorkerExecutor we;

      public BackgroundJMSMessageEndpoint(MessageEndpoint messageEndpoint, MessageListener listener, WorkerExecutor executor)
      {
         super(messageEndpoint);
         ml = listener;
         we = executor;
      }

      @Override
      public void onMessage(Message message)
      {
         we.executeBlocking(  new Callable <Boolean>()
         {
            //
            // This is just a quick & dirty
            // callable that delegates to a
            // vertx executor.   Some rethinking
            // & refactoring of this callable
            // is needed.   ALSO, not clear
            // of XA concerns and how DI actions
            // will happen.
            //
            @Override
            public Boolean call() throws Exception
            {
               ml.onMessage(message);
               return(Boolean.TRUE);
            }
         });
      }
   }
}
