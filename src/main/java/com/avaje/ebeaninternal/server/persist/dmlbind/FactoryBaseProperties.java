package com.avaje.ebeaninternal.server.persist.dmlbind;

import com.avaje.ebeaninternal.server.deploy.BeanDescriptor;
import com.avaje.ebeaninternal.server.deploy.BeanProperty;
import com.avaje.ebeaninternal.server.persist.dml.DmlMode;

import java.util.List;

/**
 * Add base properties to the BindableList for a bean type.
 * <p>
 * This excludes unique embedded and associated properties.
 * </p>
 */
public class FactoryBaseProperties {

  private final FactoryProperty factoryProperty;


  public FactoryBaseProperties(boolean bindEncryptDataFirst) {
    factoryProperty = new FactoryProperty(bindEncryptDataFirst);
  }

  /**
   * Add Bindable for the base properties to the list.
   */
  public void create(List<Bindable> list, BeanDescriptor<?> desc, DmlMode mode, boolean withLobs) {

    for (BeanProperty prop : desc.propertiesBaseScalar()) {
      Bindable item = factoryProperty.create(prop, mode, withLobs);
      if (item != null) {
        list.add(item);
      }
    }
  }

}
