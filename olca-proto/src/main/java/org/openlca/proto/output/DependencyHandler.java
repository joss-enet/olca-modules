package org.openlca.proto.output;

import org.openlca.core.model.RootEntity;
import org.openlca.core.model.descriptors.Descriptor;

public interface DependencyHandler {

  void push(RootEntity entity);

  void push(Descriptor descriptor);
}
