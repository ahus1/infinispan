package org.infinispan.configuration.cache;

import static org.infinispan.commons.configuration.AbstractTypedPropertiesConfiguration.PROPERTIES;
import static org.infinispan.configuration.cache.InterceptorConfiguration.AFTER;
import static org.infinispan.configuration.cache.InterceptorConfiguration.BEFORE;
import static org.infinispan.configuration.cache.InterceptorConfiguration.INDEX;
import static org.infinispan.configuration.cache.InterceptorConfiguration.INTERCEPTOR;
import static org.infinispan.configuration.cache.InterceptorConfiguration.INTERCEPTOR_CLASS;
import static org.infinispan.configuration.cache.InterceptorConfiguration.POSITION;
import static org.infinispan.util.logging.Log.CONFIG;

import java.util.Properties;

import org.infinispan.commons.CacheConfigurationException;
import org.infinispan.commons.configuration.Builder;
import org.infinispan.commons.configuration.attributes.Attribute;
import org.infinispan.commons.configuration.attributes.AttributeSet;
import org.infinispan.commons.util.TypedProperties;
import org.infinispan.configuration.cache.InterceptorConfiguration.Position;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.interceptors.AsyncInterceptor;
import org.infinispan.interceptors.BaseCustomAsyncInterceptor;

/**
 * This builder defines details of a specific custom interceptor.
 *
 * @deprecated Since 10.0, custom interceptors support will be removed and only modules will be able to define interceptors
 */
@Deprecated
public class InterceptorConfigurationBuilder extends AbstractCustomInterceptorsConfigurationChildBuilder implements Builder<InterceptorConfiguration> {
   private final AttributeSet attributes;

   InterceptorConfigurationBuilder(CustomInterceptorsConfigurationBuilder builder) {
      super(builder);
      attributes = InterceptorConfiguration.attributeDefinitionSet();
   }

   @Override
   public AttributeSet attributes() {
      return attributes;
   }

   /**
    * Dictates that the custom interceptor appears immediately <i>after</i> the specified interceptor. If the specified
    * interceptor is not found in the interceptor chain, a {@link CacheConfigurationException} will be thrown when the
    * cache starts.
    *
    * @param after the class of the interceptor to look for
    */
   public InterceptorConfigurationBuilder after(Class<? extends AsyncInterceptor> after) {
      attributes.attribute(AFTER).set(after);
      return this;
   }

   /**
    * Dictates that the custom interceptor appears immediately <i>before</i> the specified interceptor. If the specified
    * interceptor is not found in the interceptor chain, a {@link CacheConfigurationException} will be thrown when the
    * cache starts.
    *
    * @param before the class of the interceptor to look for
    */
   public InterceptorConfigurationBuilder before(Class<? extends AsyncInterceptor> before) {
      attributes.attribute(BEFORE).set(before);
      return this;
   }

   /**
    * Class of the new custom interceptor to add to the configuration.
    * @param interceptorClass an instance of {@link AsyncInterceptor}
    */
   public InterceptorConfigurationBuilder interceptorClass(Class<? extends AsyncInterceptor> interceptorClass) {
      attributes.attribute(INTERCEPTOR_CLASS).set(interceptorClass);
      return this;
   }

   /**
    * An instance of the new custom interceptor to add to the configuration.
    * Warning: if you use this configuration for multiple caches, the interceptor instance will
    * be shared, which will corrupt interceptor stack. Use {@link #interceptorClass} instead.
    *
    * @param interceptor an instance of {@link AsyncInterceptor}
    */
   public InterceptorConfigurationBuilder interceptor(AsyncInterceptor interceptor) {
      attributes.attribute(INTERCEPTOR).set(interceptor);
      return this;
   }

   /**
    * Specifies a position in the interceptor chain to place the new interceptor.  The index starts at 0 and goes up to
    * the number of interceptors in a given configuration.  An {@link IllegalArgumentException} is thrown if the index is
    * less than 0 or greater than the maximum number of interceptors in the chain.
    *
    * @param i positional index in the interceptor chain to place the new interceptor.
    */
   public InterceptorConfigurationBuilder index(int i) {
      if (i < 0) throw new IllegalArgumentException("Index cannot be negative");
      attributes.attribute(INDEX).set(i);
      return this;
   }

   /**
    * Specifies a position, denoted by the {@link Position} enumeration, where to place the new interceptor.
    *
    * @param p position to place the new interceptor
    */
   public InterceptorConfigurationBuilder position(Position p) {
      attributes.attribute(POSITION).set(p);
      return this;
   }

   /**
    * Sets interceptor properties
    *
    * @return this InterceptorConfigurationBuilder
    */
   public InterceptorConfigurationBuilder withProperties(Properties properties) {
      attributes.attribute(PROPERTIES).set(TypedProperties.toTypedProperties(properties));
      return this;
   }

   /**
    * Clears the interceptor properties
    *
    * @return this InterceptorConfigurationBuilder
    */
   public InterceptorConfigurationBuilder clearProperties() {
      TypedProperties properties = attributes.attribute(PROPERTIES).get();
      properties.clear();
      attributes.attribute(PROPERTIES).set(TypedProperties.toTypedProperties(properties));
      return this;
   }

   public InterceptorConfigurationBuilder addProperty(String key, String value) {
      TypedProperties properties = attributes.attribute(PROPERTIES).get();
      properties.put(key, value);
      attributes.attribute(PROPERTIES).set(TypedProperties.toTypedProperties(properties));
      return this;
   }

   public InterceptorConfigurationBuilder removeProperty(String key) {
      TypedProperties properties = attributes.attribute(PROPERTIES).get();
      properties.remove(key);
      attributes.attribute(PROPERTIES).set(TypedProperties.toTypedProperties(properties));
      return this;
   }

   @Override
   public void validate() {
      Attribute<Class> interceptorClassAttribute = attributes.attribute(INTERCEPTOR_CLASS);
      Attribute<AsyncInterceptor> interceptorAttribute = attributes.attribute(INTERCEPTOR);


      if (!interceptorClassAttribute.isNull() && !interceptorAttribute.isNull()) {
         throw CONFIG.interceptorClassAndInstanceDefined(interceptorClassAttribute.get().getName(), interceptorAttribute.get().toString());
      } else if (interceptorClassAttribute.isNull() && interceptorAttribute.isNull()) {
         throw CONFIG.customInterceptorMissingClass();
      }
      Class<? extends AsyncInterceptor> interceptorClass = interceptorClassAttribute.get();
      if (interceptorClass == null) {
         interceptorClass = interceptorAttribute.get().getClass();
      }

      if (!BaseCustomAsyncInterceptor.class.isAssignableFrom(interceptorClass)) {
         final String className = interceptorClass.getName();
         //Suppress noisy warnings if the interceptor is one of our own (like one of those from Query):
         if (! className.startsWith("org.infinispan.")) {
            CONFIG.suggestCustomInterceptorInheritance(className);
         }
      }

      // Make sure more than one 'position' isn't picked.
      int positions = 0;

      if (!attributes.attribute(BEFORE).isNull()) positions++;
      if (!attributes.attribute(AFTER).isNull()) positions++;
      if (attributes.attribute(INDEX).get() > -1) positions++;
      if (attributes.attribute(POSITION).isModified()) positions++;

      switch (positions) {
         case 0:
            throw CONFIG.missingCustomInterceptorPosition(interceptorClass.getName());
         case 1:
            break;
         default:
            throw CONFIG.multipleCustomInterceptorPositions(interceptorClass.getName());
      }
   }

   @Override
   public void validate(GlobalConfiguration globalConfig) {
   }

   @Override
   public InterceptorConfiguration create() {
      return new InterceptorConfiguration(attributes.protect());
   }

   @Override
   public InterceptorConfigurationBuilder read(InterceptorConfiguration template) {
      attributes.read(template.attributes());
      return this;
   }

   @Override
   public String toString() {
      return "InterceptorConfigurationBuilder [attributes=" + attributes + "]";
   }
}
