package fhw;
import io.quarkiverse.ironjacamar.ResourceAdapterFactory;
import io.quarkiverse.ironjacamar.ResourceAdapterKind;
import io.quarkiverse.ironjacamar.ResourceAdapterTypes;
import io.quarkiverse.ironjacamar.runtime.endpoint.MessageEndpointWrapper;
import io.quarkus.logging.Log;
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
      return new JMSMessageEndpoint(endpoint, (MessageListener) resourceEndpoint);
   }

   private static class JMSMessageEndpoint
      extends MessageEndpointWrapper
      implements MessageListener
   {
      private final MessageListener ml;

      public JMSMessageEndpoint(MessageEndpoint messageEndpoint, MessageListener listener)
      {
         super(messageEndpoint);
         ml = listener;
      }

      @Override
      public void onMessage(Message message)
      {
         ml.onMessage(message);
      }
   }
}
